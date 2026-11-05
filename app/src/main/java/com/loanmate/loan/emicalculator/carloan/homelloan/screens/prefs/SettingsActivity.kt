package com.loanmate.loan.emicalculator.carloan.homelloan.screens.prefs

import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.wireViewSpot
import com.loanmate.loan.emicalculator.carloan.adkit.PromoPack
import androidx.activity.addCallback
import com.loanmate.loan.emicalculator.carloan.adkit.Spots
import com.loanmate.loan.emicalculator.carloan.adkit.metrics.MetricsTwo
import com.loanmate.loan.emicalculator.carloan.adkit.metrics.Views
import com.loanmate.loan.emicalculator.carloan.homelloan.LoanTwo
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.welcome.setupPhases
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.loanmate.loan.emicalculator.carloan.homepanel.Launcher
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.kitEnableDarkEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import com.loanmate.loan.emicalculator.carloan.homelloan.R
import com.loanmate.loan.emicalculator.carloan.homelloan.databinding.ScreenOptionsBinding
import com.loanmate.loan.emicalculator.carloan.homelloan.databinding.RowToolBinding
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.toolViewForm
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.kitCollectWhenStarted
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.padForOsBars
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.listAd
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.insertInto

/** Settings: language, plus share, rate, privacy policy and terms. */
class SettingsActivity : AppCompatActivity() {
    private val viewModel by toolViewForm { SettingsScreenModel(settingsRepository) }
    private lateinit var languageRow: RowToolBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        kitEnableDarkEdgeToEdge()
        val b = ScreenOptionsBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.root.padForOsBars()
        // As the reference: leaving Settings (arrow or Back) runs its `screens.settings` exit ad first.
        val close = { Spots.exit(this, "screens.settings") { finish() } }
        b.crest.backVw.setOnClickListener { close() }
        onBackPressedDispatcher.addCallback(this) { close() }
        b.crest.titleVw.setText(R.string.settings)

        // launcher.default_home_banner: offer "Set as default home" while another launcher is the default.
        val launcher = (application as LoanTwo).container.config.value.launcher
        if (launcher.on && launcher.defaultHomeBanner && !Launcher.isDefaultHome(this)) {
            row(b.appearanceVw, R.drawable.glyph_settings, R.string.set_default_home, null) {
                Launcher.requestDefaultHome(this) { granted -> if (granted) recreate() }
            }
        }
        languageRow = row(b.appearanceVw, R.drawable.glyph_language, R.string.language, "") {
            startActivity(Intent(this, LanguageActivity::class.java))
        }

        // screens.settings list_ad: right under the Tongue row.
        listAd("screens.settings")?.insertInto(
            b.appearanceVw, resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._7sdp),
            at = b.appearanceVw.indexOfChild(languageRow.root) + 1,
        )

        profileStrip(b.profileVw, R.drawable.glyph_person, R.string.step_gender, "OB_2")
        profileStrip(b.profileVw, R.drawable.glyph_cake, R.string.step_age, "OB_3")

        // Remote Config `settings`: each of these four rows can be turned off per track (and country).
        val rows = (application as LoanTwo).container.config.value.settingsRows
        if (rows.share) row(b.aboutVw, R.drawable.glyph_share, R.string.share_app, getString(R.string.share_app_sub)) { share() }
        if (rows.rate) row(b.aboutVw, R.drawable.glyph_star, R.string.rate_app, getString(R.string.rate_app_sub)) { rate() }
        if (rows.feedback) row(b.aboutVw, R.drawable.glyph_mail, R.string.feedback, getString(R.string.feedback_sub)) { feedback() }
        if (rows.update) row(b.aboutVw, R.drawable.glyph_update, R.string.update_app, getString(R.string.update_app_sub)) {
            open(base.updateLink.ifBlank { storeUrl() })
        }
        row(b.aboutVw, R.drawable.glyph_shield, R.string.privacy_policy, null) { open(base.privacyLink.ifBlank { getString(R.string.privacy_policy_url) }) }
        row(b.aboutVw, R.drawable.glyph_document, R.string.terms, null) { open(base.termsLink.ifBlank { getString(R.string.terms_url) }) }
        // Spot consent (UMP): users in regions that require it can change their choice here.
        if (PromoPack.secrecyPicksRequired()) {
            row(b.aboutVw, R.drawable.glyph_shield, R.string.privacy_options, null) { PromoPack.presentSecrecyPicks(this) {} }
        }

        b.versionVw.text = getString(R.string.settings_version, packageManager.getPackageInfo(packageName, 0).versionName)

        kitCollectWhenStarted(viewModel.state) { state ->
            languageRow.subVw.text = "${state.language.flag}  ${getString(state.language.nativeName)}"
        }
        wireViewSpot("screens.settings", b.adInlineVw, b.adSlotVw)
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
        Views.show("settings")
    }

    private fun row(parent: ViewGroup, @DrawableRes icon: Int, @StringRes name: Int, sub: String?, onClick: () -> Unit): RowToolBinding {
        val r = RowToolBinding.inflate(layoutInflater, parent, true)
        r.iconVw.setImageResource(icon)
        r.nameVw.setText(name)
        r.subVw.text = sub
        r.subVw.visibility = if (sub == null) View.GONE else View.VISIBLE
        r.root.setOnClickListener {
            MetricsTwo.click(resources.getResourceEntryName(name))
            onClick()
        }
        return r
    }

    /** Gender / age: the onboarding answer, editable here (single choice from the same options). */
    private fun profileStrip(parent: ViewGroup, @DrawableRes icon: Int, @StringRes name: Int, stepKey: String) {
        val settings = (application as LoanTwo).container.settingsRepository
        val step = setupPhases(settings.languages).first { it.key == stepKey }
        val labels = step.opts.map { getString(it.name) }
        fun current() = settings.answer(stepKey)?.firstOrNull() ?: step.defaultSel.firstOrNull()
        lateinit var r: RowToolBinding
        r = row(parent, icon, name, current()?.let(labels::getOrNull).orEmpty()) {
            MaterialAlertDialogBuilder(this)
                .setTitle(step.title)
                .setSingleChoiceItems(labels.toTypedArray(), current() ?: -1) { dialog, which ->
                    settings.setAnswer(stepKey, setOf(which))
                    r.subVw.text = labels[which]
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

    private val base get() = (application as LoanTwo).container.config.value.base
}
