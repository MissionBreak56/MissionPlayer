package com.missionbreak56.musicplayer.ui

import android.Manifest
import android.app.RecoverableSecurityException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.MediaStore
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.missionbreak56.musicplayer.R
import com.missionbreak56.musicplayer.online.Candidate
import com.missionbreak56.musicplayer.tags.TagError
import com.missionbreak56.musicplayer.tags.TagField
import kotlinx.coroutines.launch

/**
 * Edits the tags of one song: manual fields, automatic search (MusicBrainz + Cover Art Archive), cover
 * replacement/removal and lyrics. Nothing reaches the file until the user taps Save.
 */
class MetadataEditorActivity : BaseActivity() {

    private lateinit var vm: MetadataEditorViewModel
    private val inputs = HashMap<TagField, TextInputEditText>()
    private val layouts = HashMap<TagField, TextInputLayout>()
    private var populating = false
    private var shownRevision = -1
    private var renderedCover: Any? = null
    private var renderedOptions: Any? = null
    private var initialActionDone = false

    private lateinit var progress: LinearProgressIndicator
    private lateinit var statusView: TextView
    private lateinit var readOnlyNote: TextView
    private lateinit var coverView: ImageView
    private lateinit var coverInfo: TextView
    private lateinit var coverNote: TextView
    private lateinit var optionsScroll: HorizontalScrollView
    private lateinit var optionsRow: LinearLayout
    private lateinit var searchBtn: MaterialButton
    private lateinit var pickBtn: MaterialButton
    private lateinit var removeBtn: MaterialButton
    private lateinit var undoBtn: MaterialButton
    private lateinit var cancelBtn: MaterialButton
    private lateinit var saveBtn: MaterialButton
    private lateinit var form: LinearLayout

    private val pickImage = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.pickLocalCover(uri)
    }

    private val writeAccess = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == RESULT_OK) vm.save() else showAccessDenied()
    }

    private val legacyPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.save() else showAccessDenied()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vm = ViewModelProvider(this)[MetadataEditorViewModel::class.java]

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(MaterialColors.getColor(this, android.R.attr.colorBackground, 0))
        root.addView(screenHeader(this, tr("Editar metadados", "Edit metadata")) { handleBack() })

        progress = LinearProgressIndicator(this).apply {
            isIndeterminate = true
            visibility = View.GONE
        }
        root.addView(progress, LinearLayout.LayoutParams(-1, -2))

        val scroll = ScrollView(this)
        scroll.isFillViewport = false
        form = LinearLayout(this)
        form.orientation = LinearLayout.VERTICAL
        form.setPadding(dp(16), dp(4), dp(16), dp(24))
        scroll.addView(form)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(buildBottomBar())
        setContentView(root)

        buildForm()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = handleBack()
        })

        vm.load(intent.getLongExtra(EXTRA_ID, -1L))
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { vm.state.collect { render(it) } }
                launch { vm.events.collect { handleEvent(it) } }
            }
        }
    }

    // ----- Layout -----

    private fun sectionTitle(text: String) {
        val tv = TextView(this)
        tv.text = text
        tv.textSize = 15f
        tv.setTypeface(tv.typeface, Typeface.BOLD)
        tv.setTextColor(MaterialColors.getColor(tv, com.google.android.material.R.attr.colorPrimary))
        tv.setPadding(0, dp(20), 0, dp(6))
        tv.accessibilityHeading = true
        form.addView(tv)
    }

    private fun labelOf(f: TagField): String = when (f) {
        TagField.TITLE -> tr("Título", "Title")
        TagField.ARTIST -> tr("Artista", "Artist")
        TagField.ALBUM_ARTIST -> tr("Artista do álbum", "Album artist")
        TagField.ALBUM -> tr("Álbum", "Album")
        TagField.GENRE -> tr("Gênero", "Genre")
        TagField.DATE -> tr("Ano ou data (AAAA ou AAAA-MM-DD)", "Year or date (YYYY or YYYY-MM-DD)")
        TagField.TRACK -> tr("Faixa", "Track")
        TagField.TRACK_TOTAL -> tr("Total de faixas", "Track total")
        TagField.DISC -> tr("Disco", "Disc")
        TagField.DISC_TOTAL -> tr("Total de discos", "Disc total")
        TagField.COMPOSER -> tr("Compositor", "Composer")
        TagField.LYRICIST -> tr("Letrista", "Lyricist")
        TagField.LABEL -> tr("Gravadora", "Label")
        TagField.ORIGINAL_ALBUM -> tr("Álbum ou lançamento original", "Original album")
        TagField.COMMENT -> tr("Comentários", "Comments")
        TagField.LYRICS -> tr("Letra (texto simples, sem sincronia)", "Lyrics (plain text, not synced)")
    }

    private fun field(f: TagField, multiline: Boolean = false, numeric: Boolean = false): TextInputLayout {
        val til = TextInputLayout(this, null, com.google.android.material.R.attr.textInputOutlinedStyle)
        til.hint = labelOf(f)
        val et = TextInputEditText(til.context)
        et.inputType = when {
            numeric -> InputType.TYPE_CLASS_NUMBER
            multiline -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            else -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
        }
        if (multiline) {
            et.minLines = 3
            et.gravity = Gravity.TOP or Gravity.START
        } else {
            et.setSingleLine()
        }
        et.doAfterTextChanged { if (!populating) vm.setField(f, it?.toString().orEmpty()) }
        til.addView(et, LinearLayout.LayoutParams(-1, -2))
        inputs[f] = et
        layouts[f] = til
        return til
    }

    private fun addField(f: TagField, multiline: Boolean = false) {
        form.addView(field(f, multiline), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
    }

    private fun addPair(a: TagField, b: TagField) {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.addView(field(a, numeric = true), LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(4) })
        row.addView(field(b, numeric = true), LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(4) })
        form.addView(row, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
    }

    private fun buildForm() {
        readOnlyNote = TextView(this).apply {
            textSize = 13f
            visibility = View.GONE
            setPadding(0, dp(8), 0, 0)
        }
        form.addView(readOnlyNote)

        statusView = TextView(this).apply {
            textSize = 14f
            visibility = View.GONE
            setPadding(0, dp(8), 0, dp(4))
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        form.addView(statusView)

        searchBtn = MaterialButton(this).apply {
            text = tr("Buscar informações automaticamente", "Find information automatically")
            setOnClickListener { vm.search() }
        }
        form.addView(searchBtn, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })

        sectionTitle(tr("Informações principais", "Main information"))
        addField(TagField.TITLE)
        addField(TagField.ARTIST)

        sectionTitle(tr("Álbum e lançamento", "Album and release"))
        addField(TagField.ALBUM)
        addField(TagField.ALBUM_ARTIST)
        addField(TagField.DATE)
        addPair(TagField.TRACK, TagField.TRACK_TOTAL)
        addPair(TagField.DISC, TagField.DISC_TOTAL)
        addField(TagField.GENRE)
        addField(TagField.LABEL)
        addField(TagField.ORIGINAL_ALBUM)

        sectionTitle(tr("Créditos", "Credits"))
        addField(TagField.COMPOSER)
        addField(TagField.LYRICIST)

        sectionTitle(tr("Capa do álbum", "Album cover"))
        coverView = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            roundedBackground(0x22888888, 12)
            clipToOutline = true
            contentDescription = tr("Capa atual da música", "Current cover of the song")
        }
        form.addView(coverView, LinearLayout.LayoutParams(dp(180), dp(180)).apply { gravity = Gravity.CENTER_HORIZONTAL })
        coverInfo = TextView(this).apply {
            textSize = 13f
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, dp(6), 0, dp(6))
        }
        form.addView(coverInfo)
        val coverButtons = LinearLayout(this)
        coverButtons.orientation = LinearLayout.HORIZONTAL
        coverButtons.gravity = Gravity.CENTER
        pickBtn = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = tr("Escolher imagem", "Choose image")
            setOnClickListener { launchPicker() }
        }
        removeBtn = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = tr("Remover capa", "Remove cover")
            setOnClickListener { confirmRemoveCover() }
        }
        undoBtn = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = tr("Manter a atual", "Keep current")
            setOnClickListener { vm.undoCover() }
        }
        coverButtons.addView(pickBtn, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(4) })
        coverButtons.addView(removeBtn, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(4) })
        coverButtons.addView(undoBtn, LinearLayout.LayoutParams(0, -2, 1f))
        form.addView(coverButtons, LinearLayout.LayoutParams(-1, -2))
        coverNote = TextView(this).apply {
            textSize = 13f
            visibility = View.GONE
            setPadding(0, dp(8), 0, dp(4))
        }
        form.addView(coverNote)
        optionsRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        optionsScroll = HorizontalScrollView(this).apply {
            visibility = View.GONE
            isHorizontalScrollBarEnabled = false
            addView(optionsRow)
        }
        form.addView(optionsScroll, LinearLayout.LayoutParams(-1, -2))

        sectionTitle(tr("Letras", "Lyrics"))
        addField(TagField.LYRICS, multiline = true)

        sectionTitle(tr("Outras informações", "Other information"))
        addField(TagField.COMMENT, multiline = true)
    }

    private fun buildBottomBar(): LinearLayout {
        val bar = LinearLayout(this)
        bar.orientation = LinearLayout.HORIZONTAL
        bar.setPadding(dp(16), dp(8), dp(16), dp(8))
        cancelBtn = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = tr("Cancelar", "Cancel")
            setOnClickListener { handleBack() }
        }
        saveBtn = MaterialButton(this).apply {
            text = tr("Salvar", "Save")
            setOnClickListener { beginSave() }
        }
        bar.addView(cancelBtn, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(8) })
        bar.addView(saveBtn, LinearLayout.LayoutParams(0, -2, 1f))
        return bar
    }

    // ----- Rendering -----

    private fun populate() {
        populating = true
        for ((f, et) in inputs) {
            val v = vm.edited[f].orEmpty()
            if (et.text?.toString() != v) et.setText(v)
        }
        populating = false
    }

    private fun render(s: EditorState) {
        val working = s.busy || s.saving || s.loading
        progress.visibility = if (working) View.VISIBLE else View.GONE

        if (s.loadError != null) {
            form.visibility = View.VISIBLE
            statusView.visibility = View.VISIBLE
            statusView.text = s.loadError
            statusView.setTextColor(MaterialColors.getColor(statusView, com.google.android.material.R.attr.colorError))
            for (v in listOf(searchBtn, pickBtn, removeBtn, undoBtn, saveBtn)) v.isEnabled = false
            return
        }
        if (!s.loading && s.fieldsRevision != shownRevision) {
            shownRevision = s.fieldsRevision
            populate()
        }
        val writable = s.format.writable
        for ((f, til) in layouts) {
            val supported = s.format.supports(f) || s.loading
            til.visibility = if (supported) View.VISIBLE else View.GONE
            til.isEnabled = writable && !s.saving
            til.helperText = if (f in s.importedFields) tr("Importado do MusicBrainz", "Imported from MusicBrainz") else null
        }
        if (!s.loading && !writable) {
            readOnlyNote.visibility = View.VISIBLE
            readOnlyNote.text = tr(
                "Este formato de arquivo só permite leitura aqui: as tags não podem ser gravadas com segurança.",
                "This file format is read-only here: its tags cannot be written safely."
            )
        } else {
            readOnlyNote.visibility = View.GONE
        }

        if (s.status.isNullOrBlank()) {
            statusView.visibility = View.GONE
        } else {
            statusView.visibility = View.VISIBLE
            statusView.text = s.status
            statusView.setTextColor(
                if (s.statusIsError) MaterialColors.getColor(statusView, com.google.android.material.R.attr.colorError)
                else MaterialColors.getColor(statusView, com.google.android.material.R.attr.colorOnSurface)
            )
        }

        val canAct = !working && writable
        searchBtn.isEnabled = canAct
        searchBtn.text = if (s.importedFields.isNotEmpty() || s.coverOptions.isNotEmpty()) tr("Pesquisar novamente", "Search again")
        else tr("Buscar informações automaticamente", "Find information automatically")
        pickBtn.isEnabled = canAct
        removeBtn.isEnabled = canAct && s.cover !is CoverUi.Remove
        undoBtn.visibility = if (s.cover is CoverUi.Keep) View.GONE else View.VISIBLE
        undoBtn.isEnabled = !s.saving
        saveBtn.isEnabled = canAct && s.dirty
        cancelBtn.isEnabled = !s.saving

        renderCover(s)
        renderOptions(s)

        if (!s.loading && !initialActionDone && writable) {
            initialActionDone = true
            when (intent.getStringExtra(EXTRA_ACTION)) {
                ACTION_SEARCH -> vm.search()
                ACTION_REPLACE_COVER -> launchPicker()
                ACTION_REMOVE_COVER -> vm.removeCover()
            }
        }
    }

    private fun renderCover(s: EditorState) {
        val key: Any = if (s.cover is CoverUi.Keep) "keep#${s.fieldsRevision}" else s.cover
        if (key == renderedCover) return
        renderedCover = key
        when (val c = s.cover) {
            is CoverUi.Replace -> {
                coverView.setImageBitmap(c.preview)
                coverInfo.text = c.label + " — " + tr("será gravada ao salvar", "will be written when you save")
            }
            is CoverUi.Remove -> {
                coverView.setImageResource(R.drawable.art_placeholder)
                coverInfo.text = tr("A capa incorporada será removida ao salvar.", "The embedded cover will be removed when you save.")
            }
            is CoverUi.Keep -> {
                coverInfo.text = tr("Capa atual", "Current cover")
                val song = vm.song
                if (song == null) {
                    coverView.setImageResource(R.drawable.art_placeholder)
                } else {
                    lifecycleScope.launch {
                        val bmp = ArtLoader.bitmap(applicationContext, song, 600)
                        if (renderedCover == key) {
                            if (bmp != null) coverView.setImageBitmap(bmp) else {
                                coverView.setImageResource(R.drawable.art_placeholder)
                                coverInfo.text = tr("Esta música não tem capa", "This song has no cover")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun renderOptions(s: EditorState) {
        if (s.coverNote.isNullOrBlank()) coverNote.visibility = View.GONE else {
            coverNote.visibility = View.VISIBLE
            coverNote.text = s.coverNote
        }
        if (renderedOptions === s.coverOptions) return
        renderedOptions = s.coverOptions
        optionsRow.removeAllViews()
        optionsScroll.visibility = if (s.coverOptions.isEmpty()) View.GONE else View.VISIBLE
        for (option in s.coverOptions) {
            val iv = ImageView(this)
            iv.scaleType = ImageView.ScaleType.CENTER_CROP
            iv.roundedBackground(0x22888888, 8)
            iv.clipToOutline = true
            iv.contentDescription = tr("Capa encontrada: toque para usar", "Cover found: tap to use")
            iv.setOnClickListener { vm.chooseCover(option) }
            optionsRow.addView(iv, LinearLayout.LayoutParams(dp(96), dp(96)).apply { marginEnd = dp(8) })
            vm.thumbnailOf(option)?.let { iv.setImageBitmap(it) } ?: lifecycleScope.launch {
                vm.loadThumbnail(option)?.let { iv.setImageBitmap(it) }
            }
        }
    }

    // ----- Actions -----

    private fun launchPicker() {
        pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun confirmRemoveCover() {
        MaterialAlertDialogBuilder(this)
            .setTitle(tr("Remover a capa?", "Remove the cover?"))
            .setMessage(
                tr(
                    "A imagem da capa será retirada das tags do arquivo ao salvar (e a capa personalizada do app, se houver). A música não é apagada.",
                    "The cover picture will be removed from the file's tags when you save (and the app's custom cover, if any). The song is not deleted."
                )
            )
            .setPositiveButton(tr("Remover", "Remove")) { _, _ -> vm.removeCover() }
            .setNegativeButton(tr("Cancelar", "Cancel"), null)
            .show()
    }

    private fun handleBack() {
        if (vm.state.value.saving) {
            Toast.makeText(this, tr("Aguarde: salvando…", "Please wait: saving…"), Toast.LENGTH_SHORT).show()
            return
        }
        if (!vm.hasChanges()) {
            finish()
            return
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(tr("Descartar alterações?", "Discard changes?"))
            .setMessage(tr("As alterações ainda não foram salvas no arquivo.", "The changes have not been saved to the file yet."))
            .setPositiveButton(tr("Descartar", "Discard")) { _, _ ->
                vm.discardChanges()
                finish()
            }
            .setNegativeButton(tr("Continuar editando", "Keep editing"), null)
            .show()
    }

    // ----- Saving: write access first -----

    private fun beginSave() {
        val s = vm.state.value
        val song = vm.song ?: return
        if (s.saving || s.busy || !vm.hasChanges()) return
        val uri = song.uri
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> requestWriteR(uri)
            Build.VERSION.SDK_INT == Build.VERSION_CODES.Q -> requestWriteQ(uri)
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED -> vm.save()
            else -> legacyPermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun requestWriteR(uri: Uri) {
        val granted = checkUriPermission(uri, Process.myPid(), Process.myUid(), Intent.FLAG_GRANT_WRITE_URI_PERMISSION) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) {
            vm.save()
        } else {
            val sender = MediaStore.createWriteRequest(contentResolver, listOf(uri)).intentSender
            writeAccess.launch(IntentSenderRequest.Builder(sender).build())
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun requestWriteQ(uri: Uri) {
        try {
            contentResolver.openFileDescriptor(uri, "rw")?.close()
            vm.save()
        } catch (e: RecoverableSecurityException) {
            writeAccess.launch(IntentSenderRequest.Builder(e.userAction.actionIntent.intentSender).build())
        } catch (e: SecurityException) {
            showAccessDenied()
        } catch (e: java.io.IOException) {
            showAccessDenied()
        }
    }

    private fun showAccessDenied() {
        MaterialAlertDialogBuilder(this)
            .setTitle(tr("Permissão necessária", "Permission needed"))
            .setMessage(
                tr(
                    "O Android exige a sua autorização para o app modificar este arquivo de áudio. Nada foi gravado. Toque em “Tentar de novo” e permita, ou cancele para manter o arquivo como está.",
                    "Android requires your approval before the app can modify this audio file. Nothing was written. Tap “Try again” and allow it, or cancel to leave the file as it is."
                )
            )
            .setPositiveButton(tr("Tentar de novo", "Try again")) { _, _ -> beginSave() }
            .setNegativeButton(tr("Cancelar", "Cancel"), null)
            .show()
    }

    // ----- Events -----

    private fun handleEvent(e: EditorEvent) {
        when (e) {
            is EditorEvent.Toast -> Toast.makeText(this, e.text, Toast.LENGTH_SHORT).show()
            is EditorEvent.Saved -> {
                setResult(RESULT_OK)
                Toast.makeText(this, tr("Metadados salvos", "Metadata saved"), Toast.LENGTH_SHORT).show()
            }
            is EditorEvent.SaveFailed -> showSaveFailed(e.error)
            is EditorEvent.PickCandidate -> {
                if (e.weak) {
                    MaterialAlertDialogBuilder(this)
                        .setTitle(tr("Correspondência fraca", "Weak match"))
                        .setMessage(
                            tr(
                                "Nenhum resultado parece ser esta música com segurança. Nada foi alterado. Deseja ver os resultados mesmo assim?",
                                "No result looks like this song with confidence. Nothing was changed. Do you want to see the results anyway?"
                            )
                        )
                        .setPositiveButton(tr("Ver resultados", "Show results")) { _, _ -> showCandidates(e.outcome.candidates) }
                        .setNegativeButton(tr("Fechar", "Close"), null)
                        .show()
                } else {
                    showCandidates(e.outcome.candidates)
                }
            }
            is EditorEvent.ConfirmReplaceEdits -> {
                val names = e.conflicts.joinToString(", ") { labelOf(it).substringBefore(" (") }
                MaterialAlertDialogBuilder(this)
                    .setTitle(tr("Substituir o que você editou?", "Replace what you edited?"))
                    .setMessage(
                        tr(
                            "Você já alterou estes campos: $names. A pesquisa trouxe outros valores.",
                            "You already changed these fields: $names. The search returned different values."
                        )
                    )
                    .setPositiveButton(tr("Manter minhas edições", "Keep my edits")) { _, _ -> vm.applyImported(e.meta, keepManual = true) }
                    .setNegativeButton(tr("Substituir", "Replace")) { _, _ -> vm.applyImported(e.meta, keepManual = false) }
                    .setNeutralButton(tr("Cancelar", "Cancel"), null)
                    .show()
            }
        }
    }

    private fun showCandidates(list: List<Candidate>) {
        if (list.isEmpty()) return
        val items = list.map { c ->
            val pct = (c.score * 100).toInt()
            val r = c.release
            val where = listOfNotNull(
                r?.title?.ifBlank { null }, (r?.date?.ifBlank { null } ?: c.firstReleaseDate.ifBlank { null }),
                r?.country, r?.status, c.disambiguation.ifBlank { null }
            ).joinToString(" · ")
            "$pct%  ${c.title} — ${c.artist}\n$where"
        }.toTypedArray()
        MaterialAlertDialogBuilder(this)
            .setTitle(tr("Escolha o resultado", "Choose a result"))
            .setItems(items) { _, which -> vm.applyCandidate(list[which]) }
            .setNegativeButton(tr("Cancelar", "Cancel"), null)
            .show()
    }

    private fun showSaveFailed(error: TagError) {
        if (error == TagError.NO_WRITE_ACCESS) {
            showAccessDenied()
            return
        }
        val text = when (error) {
            TagError.UNSUPPORTED_FORMAT -> tr("Este formato não permite gravar tags com segurança.", "This format cannot be written safely.")
            TagError.NO_SPACE -> tr("Sem espaço livre para a cópia de segurança. O arquivo não foi alterado.", "Not enough free space for the safety copy. The file was not changed.")
            TagError.VERIFY_FAILED -> tr(
                "O arquivo gravado não passou na verificação. O original foi restaurado.",
                "The written file did not pass verification. The original was restored."
            )
            TagError.UNREADABLE -> tr("Não foi possível ler o arquivo. Nada foi alterado.", "The file could not be read. Nothing was changed.")
            else -> tr("Falha ao gravar. O arquivo original foi preservado ou restaurado.", "Writing failed. The original file was kept or restored.")
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(tr("Não foi possível salvar", "Could not save"))
            .setMessage(text)
            .setPositiveButton("OK", null)
            .show()
    }

    companion object {
        private const val EXTRA_ID = "song_id"
        private const val EXTRA_ACTION = "action"
        const val ACTION_SEARCH = "search"
        const val ACTION_REPLACE_COVER = "replace_cover"
        const val ACTION_REMOVE_COVER = "remove_cover"

        fun start(context: Context, songId: Long, action: String? = null) {
            context.startActivity(
                Intent(context, MetadataEditorActivity::class.java)
                    .putExtra(EXTRA_ID, songId)
                    .putExtra(EXTRA_ACTION, action)
            )
        }
    }
}
