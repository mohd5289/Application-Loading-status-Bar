package com.udacity

import android.animation.AnimatorSet
import android.view.View
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart

fun AnimatorSet.disableViewDuringAnimation(view: View) = apply {
    doOnStart { view.isEnabled = false }
    doOnEnd { view.isEnabled = true }
}