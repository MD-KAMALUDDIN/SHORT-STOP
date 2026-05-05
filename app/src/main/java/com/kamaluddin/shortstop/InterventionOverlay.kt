package com.kamaluddin.shortstop

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Gravity
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat

class InterventionOverlay(
    context: Context,
    private val canAffordExit: Boolean,
    private val onDismiss: () -> Unit,
    private val onEmergencyExit: () -> Unit
) : FrameLayout(context) {

    init {
        val isDarkMode = (context.resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES

        val bgColor = if (isDarkMode) "#C0FFFFFF" else "#C0000000"
        val textColor = if (isDarkMode) "#333333" else "#FFFFFF"

        setBackgroundColor(Color.parseColor(bgColor))
        isClickable = true
        isFocusable = true

        vibrate(context)

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER
            }
            setPadding(80, 80, 80, 80)
            background = ContextCompat.getDrawable(
                context,
                if (isDarkMode) android.R.drawable.dialog_holo_light_frame
                else android.R.drawable.dialog_holo_dark_frame
            )
        }

        val messageView = TextView(context).apply {
            textSize = 28f
            setTextColor(Color.parseColor(textColor))
            gravity = Gravity.CENTER
            text = getRandomMessage()
            setPadding(0, 0, 0, 40)
        }

        val emergencyButton = Button(context).apply {
            text = if (canAffordExit) "🚨 Emergency Exit (-50 pts)" else "🚫 Need 50 pts to exit early"
            setBackgroundColor(Color.parseColor(if (canAffordExit) "#FF5252" else "#888888"))
            setTextColor(Color.WHITE)
            textSize = 16f
            setPadding(40, 20, 40, 20)
            isEnabled = canAffordExit
            alpha = if (canAffordExit) 1f else 0.5f
            setOnClickListener { if (canAffordExit) onEmergencyExit() }
        }

        container.addView(messageView)
        container.addView(emergencyButton)
        addView(container)

        ValueAnimator.ofFloat(1f, 0f).apply {
            duration = 30000
            interpolator = LinearInterpolator()
            addUpdateListener { alpha = it.animatedValue as Float }
            start()
        }
    }

    private fun vibrate(context: Context) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(500)
        }
    }

    private fun getRandomMessage(): String = listOf(
        "Break the loop.\nPut the phone down.",
        "Is this really how you\nwant to spend your time?",
        "You've been here a while.\nTake a break.",
        "Cool down.\nYour goals are waiting.",
        "Step away.\nYour future self will thank you.",
        "Pause.\nWhat matters more right now?",
        "Time to refocus.\nYou've got this."
    ).random()
}
