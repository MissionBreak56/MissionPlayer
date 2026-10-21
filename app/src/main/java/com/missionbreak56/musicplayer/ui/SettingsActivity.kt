package com.missionbreak56.musicplayer.ui

import android.content.Intent
import android.net.Uri
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.missionbreak56.musicplayer.BuildConfig
import com.missionbreak56.musicplayer.data.LyricsWorker
import com.missionbreak56.musicplayer.data.Prefs
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Settings screen: everything that used to live in the floating menu of the main screen.
 */
class SettingsActivity : BaseActivity() {

    private lateinit var body: LinearLayout

    // The system file picker lets the user choose the folder (and the name) of the backup file.
    private val createBackup = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri -> if (uri != null) runBackup(uri) }

    private val openBackup = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) confirmRestore(uri) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(MaterialColors.getColor(this, android.R.attr.colorBackground, 0))
        root.addView(screenHeader(this, tr("Configurações", "Settings")) { finish() })

        val scroll = ScrollView(this)
        body = LinearLayout(this)
        body.orientation = LinearLayout.VERTICAL
        body.setPadding(dp(16), dp(4), dp(16), dp(32))
        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)

        build()
    }

    private fun build() {
        body.removeAllViews()

        header(tr("Aparência", "Appearance"))
        entry(
            tr("Personalização", "Customization"),
            tr("Modo claro/escuro, ícone, cor de destaque, fonte e fundo", "Light/dark mode, icon, accent color, font and background")
        ) { open(CustomizeActivity::class.java) }

        header(tr("Biblioteca", "Library"))
        entry(
            tr("Organizar abas", "Arrange tabs"),
            tr("Muda a ordem das abas da tela inicial", "Changes the order of the home screen tabs")
        ) {
            Dialogs.reorderTabs(this, Prefs.tabOrder()) { order -> Prefs.setTabOrder(order) }
        }
        entry(
            tr("Itens ocultos", "Hidden items"),
            tr("Mostra de novo músicas e pastas que você ocultou", "Show songs and folders you hid again")
        ) { Dialogs.manageHidden(this) {} }

        header(tr("Áudio e letras", "Audio and lyrics"))
        entry(
            tr("Equalizador", "Equalizer"),
            tr("Presets, bandas, graves, surround e aumento de volume", "Presets, bands, bass, surround and volume boost")
        ) { open(EqualizerActivity::class.java) }
        switchEntry(
            tr("Letras automáticas", "Automatic lyrics"),
            tr("Baixa em segundo plano as letras que faltam (só com internet)", "Downloads missing lyrics in the background (online only)"),
            Prefs.autoLyrics
        ) { on ->
            Prefs.autoLyrics = on
            if (on) LyricsWorker.schedule(this) else LyricsWorker.cancel(this)
            Toast.makeText(
                this,
                if (on) tr("Letras automáticas ligadas", "Auto lyrics on")
                else tr("Letras automáticas desligadas", "Auto lyrics off"),
                Toast.LENGTH_SHORT
            ).show()
        }

        header(tr("Backup", "Backup"))
        entry(
            tr("Fazer backup", "Back up"),
            tr(
                "Salva favoritas, playlists, fotos, estatísticas e ajustes em um arquivo; você escolhe a pasta",
                "Saves favorites, playlists, photos, statistics and settings to a file; you choose the folder"
            )
        ) {
            val stamp = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            createBackup.launch("MissionPlayer-backup-$stamp.zip")
        }
        entry(
            tr("Restaurar backup", "Restore backup"),
            tr(
                "Volta tudo para o que está em um arquivo de backup",
                "Brings everything back from a backup file"
            )
        ) { openBackup.launch(arrayOf("*/*")) }

        header(tr("Mais", "More"))
        entry(
            tr("Estatísticas", "Statistics"),
            tr("Reproduções, tempo ouvido e mais ouvidos", "Plays, listening time and top items")
        ) { open(StatsActivity::class.java) }
        entry(
            tr("Sobre", "About"),
            tr("Versão, desenvolvedor e repositório", "Version, developer and repository")
        ) { open(AboutActivity::class.java) }
    }

    private fun runBackup(uri: Uri) {
        Toast.makeText(this, tr("Salvando backup…", "Saving backup…"), Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            try {
                val out = contentResolver.openOutputStream(uri, "w")
                    ?: throw java.io.IOException("cannot open file")
                val summary = out.use {
                    Backup.create(this@SettingsActivity, it, BuildConfig.VERSION_NAME)
                }
                MaterialAlertDialogBuilder(this@SettingsActivity)
                    .setTitle(tr("Backup salvo", "Backup saved"))
                    .setMessage(
                        tr(
                            "${summary.favorites} favoritas, ${summary.playlists} playlists e ${summary.files} arquivos (fotos e letras) foram salvos, além dos ajustes e estatísticas.",
                            "${summary.favorites} favorites, ${summary.playlists} playlists and ${summary.files} files (photos and lyrics) were saved, plus settings and statistics."
                        )
                    )
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            } catch (e: Exception) {
                Toast.makeText(
                    this@SettingsActivity,
                    tr("Não foi possível salvar o backup", "Could not save the backup"),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun confirmRestore(uri: Uri) {
        MaterialAlertDialogBuilder(this)
            .setTitle(tr("Restaurar backup?", "Restore backup?"))
            .setMessage(
                tr(
                    "Favoritas, playlists, fotos, estatísticas e ajustes atuais serão substituídos pelos do arquivo. Ao terminar, o app fecha para aplicar tudo: abra-o de novo.",
                    "Current favorites, playlists, photos, statistics and settings will be replaced by the ones in the file. When it finishes the app closes to apply everything: open it again."
                )
            )
            .setPositiveButton(tr("Restaurar", "Restore")) { _, _ -> runRestore(uri) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun runRestore(uri: Uri) {
        Toast.makeText(this, tr("Restaurando…", "Restoring…"), Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            try {
                val input = contentResolver.openInputStream(uri)
                    ?: throw java.io.IOException("cannot open file")
                val result = input.use { Backup.restore(this@SettingsActivity, it) }
                IconManager.apply(this@SettingsActivity, Prefs.iconVariant)
                MaterialAlertDialogBuilder(this@SettingsActivity)
                    .setTitle(tr("Backup restaurado", "Backup restored"))
                    .setMessage(
                        if (result.missing == 0) {
                            tr(
                                "Tudo foi restaurado. O app vai fechar; abra-o de novo para ver as mudanças.",
                                "Everything was restored. The app will close; open it again to see the changes."
                            )
                        } else {
                            tr(
                                "Restaurado. ${result.missing} músicas do backup não foram encontradas neste aparelho e ficaram de fora. O app vai fechar; abra-o de novo para ver as mudanças.",
                                "Restored. ${result.missing} songs from the backup were not found on this device and were left out. The app will close; open it again to see the changes."
                            )
                        }
                    )
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        finishAffinity()
                        Runtime.getRuntime().exit(0)
                    }
                    .show()
            } catch (e: Exception) {
                Toast.makeText(
                    this@SettingsActivity,
                    tr("Este arquivo não é um backup válido do MissionPlayer", "This file is not a valid MissionPlayer backup"),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun open(cls: Class<*>) = startActivity(Intent(this, cls))

    private fun header(title: String) {
        val t = TextView(this)
        t.text = title
        t.textSize = 14f
        t.setTypeface(t.typeface, Typeface.BOLD)
        t.setTextColor(MaterialColors.getColor(this, androidx.appcompat.R.attr.colorPrimary, 0))
        body.addView(t, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(22)
            bottomMargin = dp(4)
            marginStart = dp(4)
        })
    }

    private fun texts(title: String, subtitle: String): LinearLayout {
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        val t = TextView(this)
        t.text = title
        t.textSize = 16f
        col.addView(t, LinearLayout.LayoutParams(-1, -2))
        val s = TextView(this)
        s.text = subtitle
        s.textSize = 13f
        s.alpha = 0.7f
        col.addView(s, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(2) })
        return col
    }

    private fun rowContainer(): LinearLayout {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(dp(8), dp(12), dp(8), dp(12))
        val attrs = intArrayOf(android.R.attr.selectableItemBackground)
        val ta = obtainStyledAttributes(attrs)
        row.foreground = ta.getDrawable(0)
        ta.recycle()
        row.isClickable = true
        row.isFocusable = true
        return row
    }

    private fun entry(title: String, subtitle: String, onClick: () -> Unit) {
        val row = rowContainer()
        row.addView(texts(title, subtitle), LinearLayout.LayoutParams(0, -2, 1f))
        val chevron = TextView(this)
        chevron.text = "›"
        chevron.textSize = 24f
        chevron.alpha = 0.5f
        row.addView(chevron, LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(8) })
        row.setOnClickListener { onClick() }
        body.addView(row, LinearLayout.LayoutParams(-1, -2))
    }

    private fun switchEntry(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
        val row = rowContainer()
        row.addView(texts(title, subtitle), LinearLayout.LayoutParams(0, -2, 1f))
        val sw = MaterialSwitch(this)
        sw.isChecked = checked
        sw.setOnCheckedChangeListener { _, on -> onChange(on) }
        row.addView(sw, LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(8) })
        row.setOnClickListener { sw.toggle() }
        body.addView(row, LinearLayout.LayoutParams(-1, -2))
    }
}
