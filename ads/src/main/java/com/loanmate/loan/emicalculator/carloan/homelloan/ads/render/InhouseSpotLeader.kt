package com.loanmate.loan.emicalculator.carloan.homelloan.ads.render
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
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.entity.InhouseSpotSlab
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.policy.SpotOptionsChest
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.R
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.analytics.Stats2
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.host.SpotCues

/** [place] = the RC place key this custom ad runs for (`ad` events). */
class InhouseSpotLeader(private val place: String = SpotCues.place) {
    companion object {
        private var cachedAds: List<InhouseSpotSlab>? = null

        fun clearCache() {
            cachedAds = null
        }
    }

    enum class InhouseSpotStyle {
        BIG_NATIVE,
        MID_NATIVE,
        BANNER,
        FULLSCREEN_NATIVE
    }

    fun obtainInhouseSpot(
        context: Context,
        container: FrameLayout,
        type: InhouseSpotStyle,
        backupImage: ImageView? = null,
        backupLayout: LinearLayout? = null,
        onFail: (() -> Unit)? = null
    ) {
        if (context.parentActivityGone()) {
            fallback(container, backupImage, backupLayout, onFail)
            return
        }

        val pref = SpotOptionsChest.getInstance(context)

        if (!pref.getBoolean("IsCustomADS")) {
            fallback(container, backupImage, backupLayout, onFail)
            return
        }

        val ads: List<InhouseSpotSlab> = if (cachedAds != null) {
            cachedAds!!
        } else {
            val json = pref.getString("CUSTOM_ADS", null)
            if (json.isNullOrEmpty()) {
                fallback(container, backupImage, backupLayout, onFail)
                return
            }
            try {
                val parsed: List<InhouseSpotSlab> = Gson().fromJson(json, object : TypeToken<List<InhouseSpotSlab>>() {}.type)
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
            InhouseSpotStyle.BIG_NATIVE -> R.layout.house_spot_slab_a
            InhouseSpotStyle.MID_NATIVE -> R.layout.house_spot_slab_b
            InhouseSpotStyle.BANNER -> R.layout.house_spot_strip
            InhouseSpotStyle.FULLSCREEN_NATIVE -> R.layout.house_spot_fullpage
        }
        val view = try {
            LayoutInflater.from(container.context).inflate(layoutId, container, false)
        } catch (e: Exception) {
            fallback(container, backupImage, backupLayout, onFail)
            return
        }

        applyTheme(context, view, pref)
        wireFacts( view, ad)
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
            SpotCues.send(SpotCues.CUSTOM, "show", place)

            backupImage?.visibility = View.GONE
            backupLayout?.visibility = View.GONE
        }
    }

    private fun applyTheme(context: Context, view: View, pref: SpotOptionsChest) {
        try {
            val bgColor = pref.getString("NativeBgColor")
            val btnColor = pref.getString("NativebtnColor")
            val txtColor = pref.getString("NativetxtColor")
            val btnTxtColor = pref.getString("NativebtntxtColor")

            SpotFrameStyling.paintSlabBackground(context, view.findViewById<LinearLayout?>(R.id.mainlnr), bgColor)

            val ctaText = SpotFrameStyling.ctaCaptionColor(context, btnTxtColor)
            view.findViewById<TextView?>(R.id.btntext)?.let {
                SpotFrameStyling.kitPaintCtaBackground(context, it, btnColor)
                it.setTextColor(ctaText)
            }
            view.findViewById<TextView?>(R.id.only_banner_button)?.let {
                SpotFrameStyling.kitPaintCtaBackground(context, it, btnColor)
                it.setTextColor(ctaText)
            }

            val headline = SpotFrameStyling.headlineColor2(context, txtColor)
            val body = SpotFrameStyling.kitBodyColor(context, txtColor)
            view.findViewById<TextView?>(R.id.titileText)?.setTextColor(headline)
            view.findViewById<TextView?>(R.id.Texttext)?.setTextColor(body)

            view.findViewById<TextView?>(R.id.only_banner_title)?.setTextColor(headline)
            view.findViewById<TextView?>(R.id.only_banner_desc)?.setTextColor(body)
        } catch (_: Exception) {
        }
    }

    private fun wireFacts(view: View, ad: InhouseSpotSlab) {
        view.findViewById<TextView?>(R.id.titileText)?.text = ad.title
        view.findViewById<TextView?>(R.id.Texttext)?.text = ad.description
        view.findViewById<TextView?>(R.id.btntext)?.text = ad.buttonText

        view.findViewById<TextView?>(R.id.only_banner_title)?.text = ad.title
        view.findViewById<TextView?>(R.id.only_banner_desc)?.text = ad.description
        view.findViewById<TextView?>(R.id.only_banner_button)?.text = ad.buttonText

        view.findViewById<ImageView?>(R.id.gif_image)?.let { pullBadgeSafely(it, ad.icon) }
        view.findViewById<ImageView?>(R.id.only_banner_logo)?.let { pullBadgeSafely(it, ad.icon) }
        view.findViewById<ImageView?>(R.id.custom_native)?.let { pullBadgeSafely(it, ad.bannerImage) }
    }

    private fun Context.locateParentActivity(): Activity? {
        var ctx: Context = this
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    private fun Context.parentActivityGone(): Boolean {
        val act = this as? Activity ?: locateParentActivity() ?: return false
        if (act.isFinishing) return true
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && act.isDestroyed
    }

    private fun pullBadgeSafely(imageView: ImageView, url: String?) {
        if (url.isNullOrEmpty()) return
        if (imageView.context.parentActivityGone()) return
        try {
            Glide.with(imageView).load(url).into(imageView)
        } catch (_: IllegalArgumentException) {
        }
    }

    private fun kitWireClicks(view: View) {
        val click = View.OnClickListener {
            Stats2.click("house_ad_cta")
            SpotCues.send(SpotCues.CUSTOM, "click", place)
            pushPatronLink(view.context)
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
