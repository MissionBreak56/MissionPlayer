package com.missionbreak56.musicplayer.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.missionbreak56.musicplayer.BuildConfig
import com.missionbreak56.musicplayer.data.Library
import com.missionbreak56.musicplayer.data.Song
import com.missionbreak56.musicplayer.online.Candidate
import com.missionbreak56.musicplayer.online.CoverArtClient
import com.missionbreak56.musicplayer.online.CoverOption
import com.missionbreak56.musicplayer.online.ItunesCoverClient
import com.missionbreak56.musicplayer.data.LyricsFetcher
import com.missionbreak56.musicplayer.tags.LyricsText
import com.missionbreak56.musicplayer.online.ImportedMetadata
import com.missionbreak56.musicplayer.online.LocalInfo
import com.missionbreak56.musicplayer.online.MatchQuality
import com.missionbreak56.musicplayer.online.MusicBrainzClient
import com.missionbreak56.musicplayer.online.OnlineError
import com.missionbreak56.musicplayer.online.OnlineException
import com.missionbreak56.musicplayer.online.SearchOutcome
import com.missionbreak56.musicplayer.online.UrlConnectionFetcher
import com.missionbreak56.musicplayer.tags.AudioTagFormat
import com.missionbreak56.musicplayer.tags.CoverChange
import com.missionbreak56.musicplayer.tags.CoverImage
import com.missionbreak56.musicplayer.tags.InvalidImageException
import com.missionbreak56.musicplayer.tags.PreparedCover
import com.missionbreak56.musicplayer.tags.TagError
import com.missionbreak56.musicplayer.tags.TagException
import com.missionbreak56.musicplayer.tags.TagField
import com.missionbreak56.musicplayer.tags.TagSnapshot
import com.missionbreak56.musicplayer.tags.TagStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** What happens to the cover when the user saves. */
sealed class CoverUi {
    object Keep : CoverUi()
    object Remove : CoverUi()
    class Replace(val bytes: ByteArray, val mime: String, val preview: Bitmap?, val label: String) : CoverUi()
}

data class EditorState(
    val loading: Boolean = true,
    val loadError: String? = null,
    val format: AudioTagFormat = AudioTagFormat.OTHER,
    /** Bumped whenever the fields were changed by code (load, import, save) and the inputs must be refilled. */
    val fieldsRevision: Int = 0,
    val importedFields: Set<TagField> = emptySet(),
    val cover: CoverUi = CoverUi.Keep,
    val coverOptions: List<CoverOption> = emptyList(),
    val coverNote: String? = null,
    val busy: Boolean = false,
    val saving: Boolean = false,
    val status: String? = null,
    val statusIsError: Boolean = false,
    val dirty: Boolean = false
)

sealed class EditorEvent {
    class PickCandidate(val outcome: SearchOutcome, val weak: Boolean) : EditorEvent()
    class ConfirmReplaceEdits(val meta: ImportedMetadata, val conflicts: List<TagField>) : EditorEvent()
    class Toast(val text: String) : EditorEvent()
    class ConfirmReplaceLyrics(val text: String) : EditorEvent()
    object Saved : EditorEvent()
    class SaveFailed(val error: TagError) : EditorEvent()
}

/**
 * State of the metadata editor. It keeps three things apart: the tags as read from the file, the
 * values currently typed (or imported), and which of those the user typed by hand, so a new online
 * search never overwrites manual edits without asking.
 */
class MetadataEditorViewModel(private val app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state

    private val _events = MutableSharedFlow<EditorEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<EditorEvent> = _events

    var song: Song? = null
        private set
    private var snapshot: TagSnapshot? = null

    /** Current value of every field (what Save writes). */
    val edited = LinkedHashMap<TagField, String>()
    private val touched = HashSet<TagField>()

    private val http = UrlConnectionFetcher(
        "MissionPlayer/${BuildConfig.VERSION_NAME} ( https://github.com/MissionBreak56/MissionPlayer )"
    )
    private val musicBrainz = MusicBrainzClient(http)
    private val coverArt = CoverArtClient(http)
    private val itunes = ItunesCoverClient(http)
    private var searchJob: Job? = null
    private var coverJob: Job? = null
    private val thumbCache = HashMap<String, Bitmap>()

    fun thumbnailOf(option: CoverOption): Bitmap? = thumbCache[option.thumbUrl]

    suspend fun loadThumbnail(option: CoverOption): Bitmap? {
        thumbCache[option.thumbUrl]?.let { return it }
        return try {
            val bytes = coverArt.download(option.thumbUrl)
            val bmp = withContext(Dispatchers.Default) { decodePreview(bytes, 320) }
            if (bmp != null) thumbCache[option.thumbUrl] = bmp
            bmp
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            null
        }
    }

    val snapshotFormat: AudioTagFormat get() = snapshot?.format ?: AudioTagFormat.OTHER
    fun original(field: TagField): String = snapshot?.values?.get(field).orEmpty()

    // ----- Loading -----

    fun load(songId: Long) {
        if (song != null) return
        val s = Library.song(songId)
        if (s == null) {
            _state.update { it.copy(loading = false, loadError = tr("Música não encontrada na biblioteca.", "Song not found in the library.")) }
            return
        }
        song = s
        viewModelScope.launch {
            try {
                val snap = withContext(Dispatchers.IO) {
                    TagStore.cleanLeftovers(app)
                    TagStore.read(app, s.uri, null)
                }
                snapshot = snap
                edited.clear()
                edited.putAll(snap.values)
                _state.update { it.copy(loading = false, format = snap.format, fieldsRevision = it.fieldsRevision + 1) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: TagException) {
                _state.update { it.copy(loading = false, loadError = readError(e)) }
            }
        }
    }

    private fun readError(e: TagException): String = when (e.error) {
        TagError.UNREADABLE -> tr("Não foi possível ler as tags deste arquivo.", "The tags of this file could not be read.")
        else -> tr("Falha ao abrir o arquivo.", "Could not open the file.")
    }

    // ----- Fields -----

    fun setField(field: TagField, text: String) {
        if (edited[field] == text) return
        edited[field] = text
        touched += field
        _state.update { it.copy(importedFields = it.importedFields - field, dirty = dirtyFor(it.cover)) }
    }

    private fun dirtyFor(cover: CoverUi): Boolean {
        val snap = snapshot ?: return false
        val changed = TagField.values().any {
            snap.format.supports(it) && edited[it].orEmpty().trim() != snap.values[it].orEmpty().trim()
        }
        return changed || cover !is CoverUi.Keep
    }

    fun hasChanges(): Boolean = dirtyFor(_state.value.cover)

    /** Puts every field and the cover back as they are in the file. */
    fun discardChanges() {
        val snap = snapshot ?: return
        searchJob?.cancel()
        coverJob?.cancel()
        edited.clear()
        edited.putAll(snap.values)
        touched.clear()
        _state.update {
            it.copy(
                fieldsRevision = it.fieldsRevision + 1, importedFields = emptySet(), cover = CoverUi.Keep,
                coverOptions = emptyList(), coverNote = null, busy = false, status = null, dirty = false
            )
        }
    }

    // ----- Online search -----

    private fun setStatus(text: String?, error: Boolean = false) {
        _state.update { it.copy(status = text, statusIsError = error) }
    }

    fun search() {
        val snap = snapshot ?: return
        val s = song ?: return
        val st = _state.value
        if (st.busy || st.saving) return
        val local = LocalInfo(
            title = edited[TagField.TITLE].orEmpty().trim(),
            artist = edited[TagField.ARTIST].orEmpty().trim(),
            album = edited[TagField.ALBUM].orEmpty().trim(),
            durationMs = snap.audio?.lengthMs?.toLong() ?: s.durationMs,
            year = edited[TagField.DATE].orEmpty().take(4).toIntOrNull()
        )
        if (local.title.isBlank()) {
            setStatus(tr("Preencha o título para pesquisar.", "Fill in the title to search."), true)
            return
        }
        searchJob?.cancel()
        _state.update { it.copy(busy = true, status = tr("Pesquisando no MusicBrainz…", "Searching MusicBrainz…"), statusIsError = false) }
        searchJob = viewModelScope.launch {
            try {
                val outcome = musicBrainz.search(local)
                when (outcome.quality) {
                    MatchQuality.NONE -> setStatus(
                        tr("Nenhum resultado encontrado. Confira título e artista e pesquise de novo.", "No results. Check the title and artist and search again."), true
                    )
                    MatchQuality.WEAK -> {
                        setStatus(
                            tr("Não foi possível identificar a música com segurança. Nada foi alterado.", "The song could not be identified with confidence. Nothing was changed."), true
                        )
                        _events.emit(EditorEvent.PickCandidate(outcome, weak = true))
                    }
                    MatchQuality.AMBIGUOUS -> {
                        setStatus(tr("Vários resultados possíveis: escolha o correto.", "Several possible results: pick the right one."))
                        _events.emit(EditorEvent.PickCandidate(outcome, weak = false))
                    }
                    MatchQuality.CLEAR -> {
                        applyCandidate(outcome.candidates[0])
                        return@launch
                    }
                }
                _state.update { it.copy(busy = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: OnlineException) {
                _state.update { it.copy(busy = false, status = onlineMessage(e), statusIsError = true) }
            } catch (e: Throwable) {
                _state.update { it.copy(busy = false, status = tr("Erro inesperado na pesquisa.", "Unexpected search error."), statusIsError = true) }
            }
        }
    }

    /** Downloads the details of [candidate] and fills the fields (asking first if that would replace manual edits). */
    fun applyCandidate(candidate: Candidate) {
        val snap = snapshot ?: return
        if (_state.value.saving) return
        searchJob?.cancel()
        _state.update { it.copy(busy = true, status = tr("Obtendo os dados do lançamento…", "Getting the release data…"), statusIsError = false) }
        searchJob = viewModelScope.launch {
            try {
                val meta = musicBrainz.details(candidate)
                val conflicts = meta.fields.filter { (f, v) ->
                    snap.format.supports(f) && f in touched && edited[f].orEmpty().trim() != v.trim()
                }.keys.toList()
                if (conflicts.isEmpty()) {
                    applyImported(meta, keepManual = true)
                } else {
                    _state.update { it.copy(busy = false, status = null) }
                    _events.emit(EditorEvent.ConfirmReplaceEdits(meta, conflicts))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: OnlineException) {
                _state.update { it.copy(busy = false, status = onlineMessage(e), statusIsError = true) }
            } catch (e: Throwable) {
                _state.update { it.copy(busy = false, status = tr("Erro inesperado ao obter os dados.", "Unexpected error getting the data."), statusIsError = true) }
            }
        }
    }

    /** Copies the imported values into the editor. Nothing is written to the file here. */
    fun applyImported(meta: ImportedMetadata, keepManual: Boolean) {
        val fmt = snapshot?.format ?: return
        val done = HashSet<TagField>()
        for ((f, v) in meta.fields) {
            if (!fmt.supports(f)) continue
            if (keepManual && f in touched) continue
            if (edited[f].orEmpty().trim() == v.trim()) continue
            edited[f] = v
            touched -= f
            done += f
        }
        val album = meta.releaseTitle ?: tr("lançamento", "release")
        _state.update {
            it.copy(
                busy = false, fieldsRevision = it.fieldsRevision + 1, importedFields = done, dirty = dirtyFor(it.cover),
                status = tr(
                    "${done.size} campo(s) preenchido(s) com dados de “$album” (MusicBrainz). Revise e toque em Salvar.",
                    "${done.size} field(s) filled from “$album” (MusicBrainz). Review them and tap Save."
                ),
                statusIsError = false
            )
        }
        loadCovers(meta)
    }

    private fun loadCovers(meta: ImportedMetadata) {
        coverJob?.cancel()
        _state.update { it.copy(coverOptions = emptyList(), coverNote = null) }
        val releaseId = meta.releaseId ?: return
        coverJob = viewModelScope.launch {
            try {
                var options: List<CoverOption> = emptyList()
                var note: String? = null
                if (meta.hasFrontCover) options = coverArt.forRelease(releaseId)
                if (options.isEmpty() && meta.releaseGroupId != null) {
                    options = coverArt.forReleaseGroup(meta.releaseGroupId)
                    if (options.isNotEmpty()) {
                        note = tr(
                            "Este lançamento não tem capa. As imagens abaixo são de outras edições do mesmo álbum e podem ser diferentes.",
                            "This release has no cover. The pictures below belong to other editions of the same album and may differ."
                        )
                    }
                }
                if (options.isEmpty()) {
                    note = tr("Nenhuma capa encontrada. A capa atual foi mantida.", "No cover found. The current cover was kept.")
                } else if (note == null) {
                    note = tr("Toque em uma capa para usá-la (só será gravada ao salvar).", "Tap a cover to use it (it is only written when you save).")
                }
                _state.update { it.copy(coverOptions = options, coverNote = note) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: OnlineException) {
                _state.update { it.copy(coverNote = tr("Não foi possível buscar capas agora.", "Could not look for covers right now.")) }
            }
        }
    }

    // ----- Lyrics -----

    /** Looks the lyrics up with the same sources the app already uses, using the title/artist currently typed. */
    fun searchLyrics() {
        val s = song ?: return
        val st = _state.value
        if (st.busy || st.saving) return
        val title = edited[TagField.TITLE].orEmpty().trim()
        if (title.isEmpty()) {
            setStatus(tr("Preencha o título para buscar a letra.", "Fill in the title to search for the lyrics."), true)
            return
        }
        val probe = s.copy(
            title = title,
            artist = edited[TagField.ARTIST].orEmpty().trim(),
            album = edited[TagField.ALBUM].orEmpty().trim()
        )
        _state.update { it.copy(busy = true, status = tr("Buscando letra…", "Searching for lyrics…"), statusIsError = false) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            try {
                val found = withContext(Dispatchers.IO) { LyricsFetcher.lookup(probe) }
                val plain = found.text?.let { LyricsText.toPlain(it) }.orEmpty()
                when {
                    plain.isNotEmpty() -> {
                        _state.update { it.copy(status = null) }
                        if (edited[TagField.LYRICS].orEmpty().isBlank()) applyLyrics(plain)
                        else _events.emit(EditorEvent.ConfirmReplaceLyrics(plain))
                    }
                    found.outcome == LyricsFetcher.Outcome.RATE_LIMITED -> setStatus(
                        tr("As fontes de letra pediram para esperar. Tente de novo em instantes.", "The lyrics sources asked to wait. Try again shortly."), true
                    )
                    found.outcome == LyricsFetcher.Outcome.ERROR -> setStatus(
                        tr("Falha de conexão ao buscar a letra.", "Connection failed while searching for the lyrics."), true
                    )
                    else -> setStatus(tr("Letra não encontrada para esta música.", "No lyrics found for this song."), true)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                setStatus(tr("Erro inesperado ao buscar a letra.", "Unexpected error searching for lyrics."), true)
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }

    /** Puts [text] in the lyrics field. Nothing is written to the file until Save. */
    fun applyLyrics(text: String) {
        edited[TagField.LYRICS] = text
        touched -= TagField.LYRICS
        _state.update {
            it.copy(
                fieldsRevision = it.fieldsRevision + 1, importedFields = it.importedFields + TagField.LYRICS,
                dirty = dirtyFor(it.cover),
                status = tr("Letra encontrada (texto simples). Revise e toque em Salvar.", "Lyrics found (plain text). Review them and tap Save."),
                statusIsError = false
            )
        }
    }

    // ----- Cover search (independent of the metadata search) -----

    /** Looks for covers by the typed title/artist/album: Cover Art Archive (via MusicBrainz) and iTunes. */
    fun searchCovers() {
        val snap = snapshot ?: return
        val s = song ?: return
        val st = _state.value
        if (st.busy || st.saving) return
        val title = edited[TagField.TITLE].orEmpty().trim()
        val artist = edited[TagField.ARTIST].orEmpty().trim()
        val album = edited[TagField.ALBUM].orEmpty().trim()
        if (title.isEmpty() && album.isEmpty()) {
            setStatus(tr("Preencha o título ou o álbum para buscar capas.", "Fill in the title or album to search for covers."), true)
            return
        }
        coverJob?.cancel()
        _state.update { it.copy(busy = true, status = tr("Buscando capas online…", "Searching covers online…"), statusIsError = false, coverOptions = emptyList(), coverNote = null) }
        coverJob = viewModelScope.launch {
            try {
                val found = ArrayList<CoverOption>()
                var failure: OnlineException? = null
                try {
                    val local = LocalInfo(title.ifEmpty { album }, artist, album, snap.audio?.lengthMs?.toLong() ?: s.durationMs, edited[TagField.DATE].orEmpty().take(4).toIntOrNull())
                    val outcome = musicBrainz.search(local)
                    val releases = outcome.candidates.filter { it.score >= 0.45 }.mapNotNull { it.release }.distinctBy { it.id }.take(4)
                    for (r in releases) found += coverArt.forRelease(r.id).filter { it.isFront }.take(2)
                } catch (e: OnlineException) {
                    failure = e
                }
                try {
                    found += itunes.search(listOf(artist, album.ifEmpty { title }).filter { it.isNotBlank() }.joinToString(" "))
                } catch (e: OnlineException) {
                    if (failure == null) failure = e
                }
                val options = found.distinctBy { it.fullUrl }.take(20)
                val note = when {
                    options.isNotEmpty() -> tr("Toque em uma capa para usá-la (só será gravada ao salvar).", "Tap a cover to use it (it is only written when you save).")
                    failure != null -> onlineMessage(failure)
                    else -> tr("Nenhuma capa encontrada. A capa atual foi mantida.", "No cover found. The current cover was kept.")
                }
                _state.update { it.copy(coverOptions = options, coverNote = note, status = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _state.update { it.copy(coverNote = tr("Erro inesperado ao buscar capas.", "Unexpected error searching covers."), status = null) }
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }

    // ----- Cover -----

    fun chooseCover(option: CoverOption) {
        if (_state.value.busy || _state.value.saving) return
        _state.update { it.copy(busy = true, status = tr("Baixando capa…", "Downloading cover…"), statusIsError = false) }
        viewModelScope.launch {
            try {
                val bytes = coverArt.download(option.fullUrl)
                val prepared = withContext(Dispatchers.Default) { CoverImage.fromBytes(bytes) }
                setCover(
                    prepared,
                    if (option.fromOtherEdition) tr("Capa de outra edição do álbum (Cover Art Archive)", "Cover of another edition of the album (Cover Art Archive)")
                    else tr("Capa do lançamento (Cover Art Archive)", "Release cover (Cover Art Archive)")
                )
                setStatus(null)
            } catch (e: CancellationException) {
                throw e
            } catch (e: OnlineException) {
                setStatus(onlineMessage(e), true)
            } catch (e: InvalidImageException) {
                setStatus(tr("A imagem baixada não é válida.", "The downloaded image is not valid."), true)
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }

    fun pickLocalCover(uri: Uri) {
        _state.update { it.copy(busy = true, status = tr("Preparando imagem…", "Preparing image…"), statusIsError = false) }
        viewModelScope.launch {
            try {
                val prepared = withContext(Dispatchers.IO) { CoverImage.fromUri(app, uri) }
                setCover(prepared, tr("Imagem escolhida no aparelho", "Image picked from the device"))
                setStatus(null)
            } catch (e: CancellationException) {
                throw e
            } catch (e: InvalidImageException) {
                setStatus(tr("Imagem inválida: escolha um arquivo JPEG, PNG ou WebP.", "Invalid image: pick a JPEG, PNG or WebP file."), true)
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }

    private fun setCover(p: PreparedCover, label: String) {
        val preview = decodePreview(p.bytes, 900)
        _state.update {
            val c = CoverUi.Replace(p.bytes, p.mime, preview, label)
            it.copy(cover = c, dirty = dirtyFor(c))
        }
    }

    fun removeCover() {
        _state.update { it.copy(cover = CoverUi.Remove, dirty = dirtyFor(CoverUi.Remove)) }
    }

    fun undoCover() {
        _state.update { it.copy(cover = CoverUi.Keep, dirty = dirtyFor(CoverUi.Keep)) }
    }

    private fun decodePreview(bytes: ByteArray, size: Int): Bitmap? {
        val b = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, b)
        var sample = 1
        while (b.outWidth / (sample * 2) >= size && b.outHeight / (sample * 2) >= size) sample *= 2
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    // ----- Saving -----

    /** Writes the tags. The caller has already made sure the app may write to the file. */
    fun save() {
        val snap = snapshot ?: return
        val s = song ?: return
        val cover = _state.value.cover
        if (_state.value.saving) return
        val change = when (cover) {
            is CoverUi.Keep -> CoverChange.Keep
            is CoverUi.Remove -> CoverChange.Remove
            is CoverUi.Replace -> CoverChange.Replace(cover.bytes, cover.mime)
        }
        val values = LinkedHashMap(edited)
        val lyricsChanged = snap.format.supports(TagField.LYRICS) &&
            values[TagField.LYRICS].orEmpty().replace("\r\n", "\n").trim() != snap.values[TagField.LYRICS].orEmpty().replace("\r\n", "\n").trim()
        searchJob?.cancel()
        _state.update { it.copy(saving = true, busy = false, status = tr("Salvando…", "Saving…"), statusIsError = false) }
        // Not tied to the screen: a save that started must also finish its bookkeeping if the user leaves.
        saveScope.launch {
            try {
                withContext(Dispatchers.IO) { TagStore.save(app, s.uri, snap, values, change) }
                SongRefresh.afterSave(app, s, cover !is CoverUi.Keep, lyricsChanged)
                val fresh = withContext(Dispatchers.IO) { TagStore.read(app, s.uri, null) }
                snapshot = fresh
                edited.clear()
                edited.putAll(fresh.values)
                touched.clear()
                song = Library.song(s.id) ?: s
                _state.update {
                    it.copy(
                        saving = false, cover = CoverUi.Keep, coverOptions = emptyList(), coverNote = null,
                        importedFields = emptySet(), fieldsRevision = it.fieldsRevision + 1, dirty = false,
                        status = tr("Metadados salvos no arquivo.", "Metadata saved to the file."), statusIsError = false
                    )
                }
                _events.emit(EditorEvent.Saved)
            } catch (e: TagException) {
                _state.update { it.copy(saving = false, status = null) }
                _events.emit(EditorEvent.SaveFailed(e.error))
            } catch (e: Throwable) {
                _state.update { it.copy(saving = false, status = null) }
                _events.emit(EditorEvent.SaveFailed(TagError.WRITE_FAILED))
            }
        }
    }

    fun onlineMessage(e: OnlineException): String = when (e.error) {
        OnlineError.NETWORK -> tr("Falha de conexão. Verifique a internet e tente de novo.", "Connection failed. Check your internet and try again.")
        OnlineError.RATE_LIMITED -> tr("O serviço pediu para esperar. Tente novamente em instantes.", "The service asked to wait. Try again in a moment.")
        OnlineError.NOT_FOUND -> tr("Nada encontrado.", "Nothing found.")
        OnlineError.INVALID_RESPONSE -> tr("O serviço respondeu algo inesperado. Nada foi alterado.", "The service sent an unexpected answer. Nothing was changed.")
        OnlineError.HTTP -> tr("O serviço retornou um erro (${e.httpCode}). Tente mais tarde.", "The service returned an error (${e.httpCode}). Try later.")
    }

    override fun onCleared() {
        searchJob?.cancel()
        coverJob?.cancel()
        super.onCleared()
    }

    private companion object {
        val saveScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    }
}
