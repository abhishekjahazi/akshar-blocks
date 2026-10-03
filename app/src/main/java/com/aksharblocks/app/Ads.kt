package com.aksharblocks.app

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.ViewGroup
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration

/**
 * Google AdMob, set up for a children's app: every request is child-directed and under the
 * age of consent, ads are limited to the "G" rating (so they are never personalized), and the
 * app has no advertising ID (removed in the manifest). The only ad is a banner on Home, away
 * from the games; there are no full-screen ads.
 *
 * The banner's ad unit comes from the build (`banner_ad_unit`): debug builds use Google's test
 * unit, and release builds show no ad until a real unit is set in build.gradle.kts.
 */
object Ads {

    private var started = false

    /** Applies the children's settings, then starts the SDK in the background. */
    fun start(context: Context) {
        if (started) return
        started = true
        MobileAds.setRequestConfiguration(childSafe())
        Thread {
            try {
                MobileAds.initialize(context.applicationContext) {}
            } catch (e: RuntimeException) {
                Log.w(TAG, "Ads could not start", e)
            }
        }.start()
    }

    fun childSafe(): RequestConfiguration =
        RequestConfiguration.Builder()
            .setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
            .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE)
            .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G)
            .build()

    /** True when this build has a banner ad unit to show. */
    fun hasBanner(context: Context) = context.getString(R.string.banner_ad_unit).isNotBlank()

    /** A banner the width of the screen (not loading yet, see [load]); null when this build has no ad unit. */
    fun banner(activity: Activity): AdView? {
        if (!hasBanner(activity)) return null
        val metrics = activity.resources.displayMetrics
        val widthDp = (metrics.widthPixels / metrics.density).toInt()
        return AdView(activity).apply {
            adUnitId = activity.getString(R.string.banner_ad_unit)
            setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, widthDp))
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    /**
     * Asks for an ad. The first one starts a web view, which holds up the screen for a moment, so
     * call it once Home has settled rather than as it appears.
     */
    fun load(banner: AdView) {
        try {
            banner.loadAd(AdRequest.Builder().build())
        } catch (e: RuntimeException) {
            Log.w(TAG, "Banner could not load", e)
        }
    }

    private const val TAG = "Ads"
}
