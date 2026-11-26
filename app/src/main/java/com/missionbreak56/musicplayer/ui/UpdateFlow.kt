package com.missionbreak56.musicplayer.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.missionbreak56.musicplayer.BuildConfig
import com.missionbreak56.musicplayer.update.AppUpdater
import com.missionbreak56.musicplayer.update.ReleaseInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The screens of an in-app update: the offer, the permission hint, the download progress and the hand-off to Android. */
object UpdateFlow {
    private const val PREFS = "updates"
    private const val KEY_SKIPPED = "skipped_version"

    fun isSkipped(context: Context, version: String): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_SKIPPED, null) == version

    private fun skip(context: Context, version: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_SKIPPED, version).apply()
    }

    /** Release notes without the markdown marks, short enough for a dialog. */
    fun cleanNotes(raw: String): String {
        val text = raw.replace("\r\n", "\n").lines()
            .map { it.replace(Regex("^#+\\s*"), "").replace("**", "").replace("`", "").trimEnd() }
            .joinToString("\n").trim()
        return if (text.length > 700) text.take(700).trimEnd() + "…" else text
    }

    /** Asks the user whether to update. [auto] adds "skip this version" (used by the check at app start). */
    fun offer(activity: AppCompatActivity, info: ReleaseInfo, auto: Boolean) {
        val notes = cleanNotes(info.notes)
        val b = MaterialAlertDialogBuilder(activity)
            .setTitle(tr("Atualização disponível", "Update available"))
            .setMessage(
                tr("Versão ${info.version} (instalada: ${BuildConfig.VERSION_NAME})", "Version ${info.version} (installed: ${BuildConfig.VERSION_NAME})") +
                    if (notes.isNotEmpty()) "\n\n$notes" else ""
            )
            .setPositiveButton(tr("Atualizar agora", "Update now")) { _, _ -> start(activity, info) }
            .setNegativeButton(tr("Depois", "Later"), null)
        if (auto) b.setNeutralButton(tr("Ignorar esta versão", "Skip this version")) { _, _ -> skip(activity, info.version) }
        b.show()
    }

    /** Downloads and installs [info] without leaving the app. */
    fun start(activity: AppCompatActivity, info: ReleaseInfo) {
        if (info.apkUrl == null) {
            openPage(activity, info)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !activity.packageManager.canRequestPackageInstalls()) {
            MaterialAlertDialogBuilder(activity)
                .setTitle(tr("Permitir instalar atualizações", "Allow installing updates"))
                .setMessage(
                    tr(
                        "Para atualizar dentro do app, o Android precisa da sua permissão para o MissionPlayer instalar aplicativos. Toque em Abrir ajustes, ative a opção e volte para tocar em Atualizar agora de novo.",
                        "To update inside the app, Android needs your permission for MissionPlayer to install apps. Tap Open settings, turn the option on and come back to tap Update now again."
                    )
                )
                .setPositiveButton(tr("Abrir ajustes", "Open settings")) { _, _ ->
                    try {
                        activity.startActivity(
                            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${activity.packageName}"))
                        )
                    } catch (_: Exception) {
                        openPage(activity, info)
                    }
                }
                .setNegativeButton(tr("Cancelar", "Cancel"), null)
                .show()
            return
        }

        val bar = LinearProgressIndicator(activity).apply { max = 1000; isIndeterminate = true }
        val label = TextView(activity).apply { text = tr("Baixando…", "Downloading…"); gravity = Gravity.CENTER_HORIZONTAL }
        val box = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(activity.dp(24), activity.dp(16), activity.dp(24), 0)
            addView(label, LinearLayout.LayoutParams(-1, -2))
            addView(bar, LinearLayout.LayoutParams(-1, -2).apply { topMargin = activity.dp(12) })
        }
        var job: Job? = null
        val dialog = MaterialAlertDialogBuilder(activity)
            .setTitle(tr("Atualizando para ${info.version}", "Updating to ${info.version}"))
            .setView(box)
            .setCancelable(false)
            .setNegativeButton(tr("Cancelar", "Cancel")) { _, _ -> job?.cancel() }
            .show()

        job = activity.lifecycleScope.launch {
            try {
                var lastShown = -1
                val apk = AppUpdater.download(activity, info) { done, total ->
                    if (total > 0) {
                        val permille = (done * 1000 / total).toInt()
                        if (permille != lastShown) {
                            lastShown = permille
                            activity.runOnUiThread {
                                bar.isIndeterminate = false
                                bar.progress = permille
                                label.text = tr("Baixando… ${permille / 10}%", "Downloading… ${permille / 10}%")
                            }
                        }
                    }
                }
                label.text = tr("Abrindo o instalador…", "Opening the installer…")
                withContext(Dispatchers.IO) { AppUpdater.install(activity, apk) }
                dialog.dismiss()
            } catch (e: CancellationException) {
                dialog.dismiss()
                throw e
            } catch (e: Exception) {
                dialog.dismiss()
                MaterialAlertDialogBuilder(activity)
                    .setTitle(tr("Não foi possível atualizar", "Could not update"))
                    .setMessage(
                        tr(
                            "O download ou a verificação falhou. Confira a internet e tente de novo, ou baixe a versão pela página da release.",
                            "The download or its verification failed. Check your internet and try again, or get the version from the release page."
                        )
                    )
                    .setPositiveButton(tr("Tentar de novo", "Try again")) { _, _ -> start(activity, info) }
                    .setNeutralButton(tr("Abrir a página", "Open the page")) { _, _ -> openPage(activity, info) }
                    .setNegativeButton(tr("Fechar", "Close"), null)
                    .show()
            }
        }
    }

    private fun openPage(activity: AppCompatActivity, info: ReleaseInfo) {
        try {
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(info.pageUrl)))
        } catch (_: Exception) {
            Toast.makeText(activity, tr("Não foi possível abrir o link.", "Could not open the link."), Toast.LENGTH_SHORT).show()
        }
    }
}
