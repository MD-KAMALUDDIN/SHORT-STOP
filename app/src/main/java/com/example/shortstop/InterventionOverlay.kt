package com.example.shortstop

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

class InterventionOverlay(context: Context, private val onDismiss: () -> Unit, private val onEmergencyExit: () -> Unit) : FrameLayout(context) {
    
    private val messageView: TextView

    init {
        val isDarkMode = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        
        val bgColor = if (isDarkMode) "#C0FFFFFF" else "#C0000000"
        val textColor = if (isDarkMode) "#333333" else "#FFFFFF"
        val containerBg = if (isDarkMode) "#FFFFFF" else "#1E1E1E"
        
        setBackgroundColor(Color.parseColor(bgColor))
        alpha = 1f
        
        isClickable = true
        isFocusable = true
        setOnTouchListener { _, _ -> true }
        
        // Haptic feedback when overlay appears
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

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER
            }
            setBackgroundColor(Color.parseColor(containerBg))
            setPadding(80, 80, 80, 80)
            if (isDarkMode) {
                background = context.getDrawable(android.R.drawable.dialog_holo_light_frame)
            } else {
                background = context.getDrawable(android.R.drawable.dialog_holo_dark_frame)
            }
        }

        messageView = TextView(context).apply {
            textSize = 28f
            setTextColor(Color.parseColor(textColor))
            gravity = Gravity.CENTER
            text = getRandomMessage()
            setPadding(0, 0, 0, 40)
        }

        val emergencyButton = Button(context).apply {
            text = "🚨 Emergency Exit"
            setBackgroundColor(Color.parseColor("#FF5252"))
            setTextColor(Color.WHITE)
            textSize = 16f
            setPadding(40, 20, 40, 20)
            setOnClickListener {
                onEmergencyExit()
            }
        }

        container.addView(messageView)
        container.addView(emergencyButton)
        addView(container)
        
        ValueAnimator.ofFloat(1f, 0f).apply {
            duration = 30000
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                alpha = animation.animatedValue as Float
            }
            start()
        }
    }
    
    private fun getRandomMessage(): String {
        val messages = listOf(
            "Break the loop.\nPut the phone down.",
            "Is this really how you\nwant to spend your time?",
            "You've been here a while.\nTake a break.",
            "Cool down.\nYour goals are waiting.",
            "Step away.\nYour future self will thank you.",
            "Pause.\nWhat matters more right now?",
            "Time to refocus.\nYou've got this."
        )
        return messages.random()
    }
}