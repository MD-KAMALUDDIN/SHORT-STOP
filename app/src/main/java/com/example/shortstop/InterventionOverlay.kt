package com.example.shortstop

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat

class InterventionOverlay(context: Context, private val onDismiss: () -> Unit) : FrameLayout(context) {
    
    private val messageView: TextView
    private val cancelButton: Button

    init {
        // Blur-like background with gradient
        setBackgroundColor(Color.parseColor("#F0FFFFFF")) // Light blur effect
        alpha = 0.95f
        
        // Make the entire overlay intercept touches
        isClickable = true
        isFocusable = true
        setOnTouchListener { _, _ -> true } // Consume all touch events

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER
            }
            setBackgroundColor(Color.parseColor("#FFFFFF"))
            setPadding(80, 80, 80, 80)
            // Add rounded corners
            background = context.getDrawable(android.R.drawable.dialog_holo_light_frame)
        }

        messageView = TextView(context).apply {
            textSize = 28f
            setTextColor(Color.parseColor("#333333"))
            gravity = Gravity.CENTER
            text = "Cool Down\n\nYou've been seeing me long time, have a break"
            setPadding(0, 0, 0, 40)
        }

        cancelButton = Button(context).apply {
            text = "Continue"
            visibility = View.GONE
            setOnClickListener { 
                onDismiss()
            }
            // Make button touchable
            isClickable = true
            isFocusable = true
        }

        // Show button after 3 seconds (no countdown)
        postDelayed({
            // Don't show continue button - force user to wait full 10 seconds
            // cancelButton.visibility = View.VISIBLE
        }, 3000)

        container.addView(messageView)
        container.addView(cancelButton)
        addView(container)
    }

    fun updateMessage(text: String, points: Int) {
        messageView.text = text
    }

    fun showCancelButton() {
        cancelButton.visibility = View.VISIBLE
    }
}
