package com.example.dailyhabittracker.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class AdManager(context: Context) {
    private var rewardedAd: RewardedAd? = null

    init {
        MobileAds.initialize(context)
    }

    fun loadBanner(adView: AdView, onFailed: (String) -> Unit = {}) {
        val request = AdRequest.Builder().build()
        adView.adListener = object : AdListener() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.w("AdManager", "Banner ad failed: ${adError.message}")
                onFailed(adError.message)
            }
        }
        adView.loadAd(request)
    }

    fun loadRewarded(
        context: Context,
        adUnitId: String,
        onLoaded: () -> Unit = {},
        onFailed: (String) -> Unit = {}
    ) {
        val request = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            adUnitId,
            request,
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    rewardedAd = null
                    Log.w("AdManager", "Rewarded ad failed: ${adError.message}")
                    onFailed(adError.message)
                }

                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    onLoaded()
                }
            }
        )
    }

    fun showRewarded(activity: Activity, onReward: (RewardItem) -> Unit, onClosed: () -> Unit) {
        val ad = rewardedAd
        if (ad == null) {
            onClosed()
            return
        }

        ad.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                onClosed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                rewardedAd = null
                onClosed()
            }

            override fun onAdShowedFullScreenContent() {
                rewardedAd = null
            }
        }

        ad.show(activity) { reward ->
            onReward(reward)
        }
    }
}
