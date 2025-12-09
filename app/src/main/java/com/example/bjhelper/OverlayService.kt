package com.example.bjhelper

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

class OverlayService : AccessibilityService() {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private lateinit var resultText: TextView
    private lateinit var playerInput: EditText
    private lateinit var dealerInput: EditText

    override fun onServiceConnected() {
        super.onServiceConnected()
        
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        overlayView = LayoutInflater.from(this).inflate(R.layout.floating_layout, null)
        
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        resultText = overlayView.findViewById(R.id.txt_result)
        playerInput = overlayView.findViewById(R.id.input_player)
        dealerInput = overlayView.findViewById(R.id.input_dealer)
        
        overlayView.findViewById<Button>(R.id.btn_calculate).setOnClickListener {
            calculateMove()
        }
        overlayView.findViewById<Button>(R.id.btn_close).setOnClickListener {
            disableSelf()
        }

        try {
            windowManager.addView(overlayView, params)
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun calculateMove() {
        val pStr = playerInput.text.toString()
        val dStr = dealerInput.text.toString()

        if (pStr.isEmpty() || dStr.isEmpty()) {
            resultText.text = "ENTER DATA"
            return
        }

        try {
            val dealerCard = dStr.toInt()
            val playerCards = pStr.split(",").map { it.trim().toInt() }
            resultText.text = getBestMove(playerCards, dealerCard)
        } catch (e: Exception) {
            resultText.text = "INVALID INPUT"
        }
    }

    private fun getBestMove(playerHand: List<Int>, dealerUpCard: Int): String {
        val total = calculateTotal(playerHand)
        val isSoft = hasSoftAce(playerHand)
        val isPair = playerHand.size == 2 && playerHand[0] == playerHand[1]

        if (isPair) {
            val card = playerHand[0]
            if (card == 11 || card == 8) return "SPLIT"
            if (card == 10 || card == 5) return "DOUBLE"
            if (card == 9 && dealerUpCard !in listOf(7, 10, 11)) return "SPLIT"
            if (card == 6 && dealerUpCard in 2..6) return "SPLIT"
            if (card == 7 || card == 3 || card == 2) {
                 if(dealerUpCard in 2..7) return "SPLIT"
            }
        }

        if (isSoft) {
            if (total >= 19) return "STAND"
            if (total == 18) return if (dealerUpCard in 2..8) "DOUBLE" else "HIT"
            if (total in 13..17 && dealerUpCard in 4..6) return "DOUBLE"
            return "HIT"
        }

        if (total >= 17) return "STAND"
        if (total in 13..16 && dealerUpCard in 2..6) return "STAND"
        if (total == 12 && dealerUpCard in 4..6) return "STAND"
        if (total == 11) return "DOUBLE"
        if (total == 10 && dealerUpCard < 10) return "DOUBLE"
        if (total == 9 && dealerUpCard in 3..6) return "DOUBLE"

        return "HIT"
    }

    private fun calculateTotal(hand: List<Int>): Int {
        var sum = 0
        var aces = 0
        for (card in hand) {
            val v = if (card > 10) 10 else card
            if (v == 1) aces++
            sum += if (v == 1) 11 else v
        }
        while (sum > 21 && aces > 0) { sum -= 10; aces-- }
        return sum
    }
    
    private fun hasSoftAce(hand: List<Int>): Boolean {
        var sum = 0
        var aces = 0
        for (card in hand) {
            val v = if (card > 10) 10 else card
            if (v == 1) aces++
            sum += if (v == 1) 11 else v
        }
        return sum <= 21 && aces > 0
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
}
