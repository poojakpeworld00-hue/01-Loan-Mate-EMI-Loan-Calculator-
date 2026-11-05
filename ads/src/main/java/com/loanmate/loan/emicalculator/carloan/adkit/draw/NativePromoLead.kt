package com.loanmate.loan.emicalculator.carloan.adkit.draw
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.loanmate.loan.emicalculator.carloan.adkit.model.NativePromoTile
import com.loanmate.loan.emicalculator.carloan.adkit.rules.PromoSettingsBox
import com.loanmate.loan.emicalculator.carloan.adkit.R
import com.loanmate.loan.emicalculator.carloan.adkit.metrics.MetricsTwo
import com.loanmate.loan.emicalculator.carloan.adkit.bridge.PromoSignals

/** [place] = the RC place key this custom ad runs for (`ad` events). */
class NativePromoLead(private val place: String = PromoSignals.place) {
    companion object {
        private var cachedAds: List<NativePromoTile>? = null

        fun wipeStore() {
            cachedAds = null
        }
    }

    enum class NativePromoLook {
        BIG_NATIVE,
        MID_NATIVE,
        BANNER,
        FULLSCREEN_NATIVE
    }

    fun obtainNativePromo(
        context: Context,
        container: FrameLayout,
        type: NativePromoLook,
        backupImage: ImageView? = null,
        backupLayout: LinearLayout? = null,
        onFail: (() -> Unit)? = null
    ) {
        if (context.parentActivityGone()) {
            fallback(container, backupImage, backupLayout, onFail)
            return
        }

        val pref = PromoSettingsBox.getInstance(context)

        if (!pref.getBoolean("IsCustomADS")) {
            fallback(container, backupImage, backupLayout, onFail)
            return
        }

        val ads: List<NativePromoTile> = if (cachedAds != null) {
            cachedAds!!
        } else {
            val json = pref.getString("CUSTOM_ADS", null)
            if (json.isNullOrEmpty()) {
                fallback(container, backupImage, backupLayout, onFail)
                return
            }
            try {
                val parsed: List<NativePromoTile> = Gson().fromJson(json, object : TypeToken<List<NativePromoTile>>() {}.type)
                cachedAds = parsed
                parsed
            } catch (e: Exception) {
                fallback(container, backupImage, backupLayout, onFail)
                return
            }
        }

        if (ads.isEmpty()) {
            fallback(container, backupImage, backupLayout, onFail)
            return
        }

        val ad = ads.random()

        val layoutId = when (type) {
            NativePromoLook.BIG_NATIVE -> R.layout.inh_spot_slab_a
            NativePromoLook.MID_NATIVE -> R.layout.inh_spot_slab_b
            NativePromoLook.BANNER -> R.layout.inh_spot_strip
            NativePromoLook.FULLSCREEN_NATIVE -> R.layout.inh_spot_fullpage
        }
        val view = try {
            LayoutInflater.from(container.context).inflate(layoutId, container, false)
        } catch (e: Exception) {
            fallback(container, backupImage, backupLayout, onFail)
            return
        }

        commitTheme(context, view, pref)
        wireNotes( view, ad)
        kitWireClicks(view)

        container.post {
            if (context.parentActivityGone()) {
                container.visibility = View.GONE
                return@post
            }

            container.findFocus()?.clearFocus()
            container.removeAllViews()
            container.addView(view)
            container.visibility = View.VISIBLE
            PromoSignals.send(PromoSignals.CUSTOM, "show", place)

            backupImage?.visibility = View.GONE
            backupLayout?.visibility = View.GONE
        }
    }

    private fun commitTheme(context: Context, view: View, pref: PromoSettingsBox) {
        try {
            val bgColor = pref.getString("NativeBgColor")
            val btnColor = pref.getString("NativebtnColor")
            val txtColor = pref.getString("NativetxtColor")
            val btnTxtColor = pref.getString("NativebtntxtColor")

            PromoFrameLooks.paintTileBackground(context, view.findViewById<LinearLayout?>(R.id.mainlnr), bgColor)

            val ctaText = PromoFrameLooks.ctaLabelColor(context, btnTxtColor)
            view.findViewById<TextView?>(R.id.btntext)?.let {
                PromoFrameLooks.kitPaintCtaBackground(context, it, btnColor)
                it.setTextColor(ctaText)
            }
            view.findViewById<TextView?>(R.id.only_banner_button)?.let {
                PromoFrameLooks.kitPaintCtaBackground(context, it, btnColor)
                it.setTextColor(ctaText)
            }

            val headline = PromoFrameLooks.headlineColor2(context, txtColor)
            val body = PromoFrameLooks.kitBodyColor(context, txtColor)
            view.findViewById<TextView?>(R.id.titileText)?.setTextColor(headline)
            view.findViewById<TextView?>(R.id.Texttext)?.setTextColor(body)

            view.findViewById<TextView?>(R.id.only_banner_title)?.setTextColor(headline)
            view.findViewById<TextView?>(R.id.only_banner_desc)?.setTextColor(body)
        } catch (_: Exception) {
        }
    }

    private fun wireNotes(view: View, ad: NativePromoTile) {
        view.findViewById<TextView?>(R.id.titileText)?.text = ad.title
        view.findViewById<TextView?>(R.id.Texttext)?.text = ad.description
        view.findViewById<TextView?>(R.id.btntext)?.text = ad.buttonText

        view.findViewById<TextView?>(R.id.only_banner_title)?.text = ad.title
        view.findViewById<TextView?>(R.id.only_banner_desc)?.text = ad.description
        view.findViewById<TextView?>(R.id.only_banner_button)?.text = ad.buttonText

        view.findViewById<ImageView?>(R.id.gif_image)?.let { pullMarkSafely(it, ad.icon) }
        view.findViewById<ImageView?>(R.id.only_banner_logo)?.let { pullMarkSafely(it, ad.icon) }
        view.findViewById<ImageView?>(R.id.custom_native)?.let { pullMarkSafely(it, ad.bannerImage) }
    }

    private fun Context.locateHostActivity(): Activity? {
        var ctx: Context = this
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    private fun Context.parentActivityGone(): Boolean {
        val act = this as? Activity ?: locateHostActivity() ?: return false
        if (act.isFinishing) return true
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && act.isDestroyed
    }

    private fun pullMarkSafely(imageView: ImageView, url: String?) {
        if (url.isNullOrEmpty()) return
        if (imageView.context.parentActivityGone()) return
        try {
            Glide.with(imageView).load(url).into(imageView)
        } catch (_: IllegalArgumentException) {
        }
    }

    private fun kitWireClicks(view: View) {
        val click = View.OnClickListener {
            MetricsTwo.click("house_ad_cta")
            PromoSignals.send(PromoSignals.CUSTOM, "click", place)
            pushSponsorUrl(view.context)
        }

        // Only the CTA opens the sponsor link (no whole-card click area).
        view.findViewById<View?>(R.id.btntext)?.setOnClickListener(click)
        view.findViewById<View?>(R.id.only_banner_button)?.setOnClickListener(click)
    }

    private fun fallback(
        container: FrameLayout,
        img: ImageView?,
        layout: LinearLayout?,
        onFail: (() -> Unit)?
    ) {
        container.post {
            if (container.context.parentActivityGone()) {
                container.visibility = View.GONE
                return@post
            }

            container.findFocus()?.clearFocus()
            container.removeAllViews()
            container.visibility = View.GONE

            img?.visibility = View.VISIBLE
            layout?.visibility = View.VISIBLE

            onFail?.invoke()
        }
    }
}
