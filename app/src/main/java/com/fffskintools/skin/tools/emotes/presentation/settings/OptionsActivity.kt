package com.fffskintools.skin.tools.emotes.presentation.settings

import com.fffskintools.skin.tools.emotes.presentation.common.wireViewSpot
import com.fffskintools.skin.tools.ads.SpotKit
import androidx.activity.addCallback
import com.fffskintools.skin.tools.ads.Spots
import com.fffskintools.skin.tools.ads.analytics.Stats2
import com.fffskintools.skin.tools.ads.analytics.Views
import com.fffskintools.skin.tools.emotes.Tool2
import com.fffskintools.skin.tools.emotes.presentation.onboarding.setupPhases
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.fffskintools.skin.tools.launcher.Launcher
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.fffskintools.skin.tools.emotes.presentation.common.kitEnableDarkEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import com.fffskintools.skin.tools.emotes.R
import com.fffskintools.skin.tools.emotes.databinding.PageOptionsBinding
import com.fffskintools.skin.tools.emotes.databinding.CellToolBinding
import com.fffskintools.skin.tools.emotes.presentation.common.toolViewForm
import com.fffskintools.skin.tools.emotes.presentation.common.kitCollectWhenStarted
import com.fffskintools.skin.tools.emotes.presentation.common.padForOsBars
import com.fffskintools.skin.tools.emotes.presentation.common.listAd
import com.fffskintools.skin.tools.emotes.presentation.common.insertInto

/** Settings: language, plus share, rate, privacy policy and terms. */
class OptionsActivity : AppCompatActivity() {
    private val viewModel by toolViewForm { OptionsViewForm(settingsRepository) }
    private lateinit var languageRow: CellToolBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        kitEnableDarkEdgeToEdge()
        val b = PageOptionsBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.root.padForOsBars()
        // As the reference: leaving Settings (arrow or Back) runs its `screens.settings` exit ad first.
        val close = { Spots.exit(this, "screens.settings") { finish() } }
        b.header.back.setOnClickListener { close() }
        onBackPressedDispatcher.addCallback(this) { close() }
        b.header.title.setText(R.string.settings)

        // launcher.default_home_banner: offer "Set as default home" while another launcher is the default.
        val launcher = (application as Tool2).container.config.value.launcher
        if (launcher.on && launcher.defaultHomeBanner && !Launcher.isDefaultHome(this)) {
            row(b.appearance, R.drawable.ic_settings, R.string.set_default_home, null) {
                Launcher.requestDefaultHome(this) { granted -> if (granted) recreate() }
            }
        }
        languageRow = row(b.appearance, R.drawable.ic_language, R.string.language, "") {
            startActivity(Intent(this, TongueActivity::class.java))
        }

        // screens.settings list_ad: right under the Tongue row.
        listAd("screens.settings")?.insertInto(
            b.appearance, resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._7sdp),
            at = b.appearance.indexOfChild(languageRow.root) + 1,
        )

        profileStrip(b.profile, R.drawable.ic_person, R.string.step_gender, "OB_2")
        profileStrip(b.profile, R.drawable.ic_cake, R.string.step_age, "OB_3")

        // Remote Config `settings`: each of these four rows can be turned off per track (and country).
        val rows = (application as Tool2).container.config.value.settingsRows
        if (rows.share) row(b.about, R.drawable.ic_share, R.string.share_app, getString(R.string.share_app_sub)) { share() }
        if (rows.rate) row(b.about, R.drawable.ic_star, R.string.rate_app, getString(R.string.rate_app_sub)) { rate() }
        if (rows.feedback) row(b.about, R.drawable.ic_mail, R.string.feedback, getString(R.string.feedback_sub)) { feedback() }
        if (rows.update) row(b.about, R.drawable.ic_update, R.string.update_app, getString(R.string.update_app_sub)) {
            open(base.updateLink.ifBlank { storeUrl() })
        }
        row(b.about, R.drawable.ic_shield, R.string.privacy_policy, null) { open(base.privacyLink.ifBlank { getString(R.string.privacy_policy_url) }) }
        row(b.about, R.drawable.ic_document, R.string.terms, null) { open(base.termsLink.ifBlank { getString(R.string.terms_url) }) }
        // Spot consent (UMP): users in regions that require it can change their choice here.
        if (SpotKit.secrecyPicksRequired()) {
            row(b.about, R.drawable.ic_shield, R.string.privacy_options, null) { SpotKit.presentSecrecyPicks(this) {} }
        }

        b.version.text = getString(R.string.settings_version, packageManager.getPackageInfo(packageName, 0).versionName)

        kitCollectWhenStarted(viewModel.state) { state ->
            languageRow.sub.text = "${state.language.flag}  ${getString(state.language.nativeName)}"
        }
        wireViewSpot("screens.settings", b.adInline, b.adSlot)
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
        Views.show("settings")
    }

    private fun row(parent: ViewGroup, @DrawableRes icon: Int, @StringRes name: Int, sub: String?, onClick: () -> Unit): CellToolBinding {
        val r = CellToolBinding.inflate(layoutInflater, parent, true)
        r.icon.setImageResource(icon)
        r.name.setText(name)
        r.sub.text = sub
        r.sub.visibility = if (sub == null) View.GONE else View.VISIBLE
        r.root.setOnClickListener {
            Stats2.click(resources.getResourceEntryName(name))
            onClick()
        }
        return r
    }

    /** Gender / age: the onboarding answer, editable here (single choice from the same options). */
    private fun profileStrip(parent: ViewGroup, @DrawableRes icon: Int, @StringRes name: Int, stepKey: String) {
        val settings = (application as Tool2).container.settingsRepository
        val step = setupPhases(settings.languages).first { it.key == stepKey }
        val labels = step.opts.map { getString(it.name) }
        fun current() = settings.answer(stepKey)?.firstOrNull() ?: step.defaultSel.firstOrNull()
        lateinit var r: CellToolBinding
        r = row(parent, icon, name, current()?.let(labels::getOrNull).orEmpty()) {
            MaterialAlertDialogBuilder(this)
                .setTitle(step.title)
                .setSingleChoiceItems(labels.toTypedArray(), current() ?: -1) { dialog, which ->
                    settings.setAnswer(stepKey, setOf(which))
                    r.sub.text = labels[which]
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun feedback() {
        val email = getString(R.string.feedback_email)
        if (email.isBlank()) return open("")
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))
            .putExtra(Intent.EXTRA_SUBJECT, "${getString(R.string.store_name)} ${packageManager.getPackageInfo(packageName, 0).versionName}")
        Spots.kitMarkOwnLaunch()
        runCatching { startActivity(intent) }.onFailure {
            Spots.resetOwnLaunch()
            Toast.makeText(this, R.string.link_unavailable, Toast.LENGTH_SHORT).show()
        }
    }

    private fun share() {
        val text = getString(R.string.share_message, getString(R.string.store_name), storeUrl())
        Spots.kitMarkOwnLaunch() // no resume ad when the user comes back from the share sheet
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), null))
    }

    private fun rate() {
        try {
            Spots.kitMarkOwnLaunch()
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
        } catch (_: ActivityNotFoundException) {
            open(storeUrl())
        }
    }

    private fun open(url: String) {
        if (url.isBlank()) {
            Toast.makeText(this, R.string.link_unavailable, Toast.LENGTH_SHORT).show()
            return
        }
        try {
            Spots.kitMarkOwnLaunch()
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: ActivityNotFoundException) {
            Spots.resetOwnLaunch()
            Toast.makeText(this, R.string.link_unavailable, Toast.LENGTH_SHORT).show()
        }
    }

    private fun storeUrl() = base.storeLink.ifBlank { "https://play.google.com/store/apps/details?id=$packageName" }

    private val base get() = (application as Tool2).container.config.value.base
}
