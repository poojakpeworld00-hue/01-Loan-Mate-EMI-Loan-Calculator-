package com.fffskintools.skin.tools.ads.policy

import android.app.Activity
import android.content.Context
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentForm.OnConsentFormDismissedListener
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentInformation.PrivacyOptionsRequirementStatus
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform

class AgreeRouteBarrier private constructor(context: Context) {
    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(context)

    fun interface AgreeReadyListener {
        fun agreeRouteDone(error: FormError?)
    }

    fun canRequestAds(): Boolean = consentInformation.canRequestAds()

    val isPrivacyOptionsRequired: Boolean
        get() = consentInformation.privacyOptionsRequirementStatus ==
            PrivacyOptionsRequirementStatus.REQUIRED

    fun queryAgree(
        activity: Activity,
        onConsentGatheringCompleteListener: AgreeReadyListener
    ) {
        val debugSettings = ConsentDebugSettings.Builder(activity).build()
        val params = ConsentRequestParameters.Builder()
            .setConsentDebugSettings(debugSettings)
            .build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    onConsentGatheringCompleteListener.agreeRouteDone(formError)
                }
            },
            { requestConsentError ->
                onConsentGatheringCompleteListener.agreeRouteDone(requestConsentError)
            }
        )
    }

    fun enterSecrecyPicks(
        activity: Activity,
        onConsentFormDismissedListener: OnConsentFormDismissedListener
    ) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity, onConsentFormDismissedListener)
    }

    companion object {
        @Volatile
        private var instance: AgreeRouteBarrier? = null

        fun getInstance(context: Context): AgreeRouteBarrier =
            instance ?: synchronized(this) {
                instance ?: AgreeRouteBarrier(context.applicationContext).also { instance = it }
            }
    }
}
