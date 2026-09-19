package com.velune.launcher

import android.app.AlertDialog
import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Shows a dark, rounded card-styled action menu instead of Android's plain
 * default list dialog. Used for every long-press menu across the app so they
 * all feel consistent with the rest of Velune's design instead of looking
 * like a stock Android popup.
 */
fun showPremiumMenu(context: Context, title: String, options: List<Pair<String, () -> Unit>>) {
    val density = context.resources.displayMetrics.density
    fun dp(v: Int) = (v * density).toInt()

    val rippleBackground = TypedValue().also {
        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, it, true)
    }.resourceId

    val container = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundResource(R.drawable.card_background)
    }

    container.addView(TextView(context).apply {
        text = title
        setTextColor(context.getColor(R.color.white))
        textSize = 16f
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        setPadding(dp(20), dp(18), dp(20), dp(10))
    })

    val dialog = AlertDialog.Builder(context).setView(container).create()
    dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

    options.forEachIndexed { index, (label, action) ->
        container.addView(TextView(context).apply {
            text = label
            setTextColor(context.getColor(R.color.white))
            textSize = 15f
            setPadding(dp(20), dp(15), dp(20), dp(15))
            setBackgroundResource(rippleBackground)
            isClickable = true
            setOnClickListener {
                dialog.dismiss()
                action()
            }
        })

        if (index != options.lastIndex) {
            container.addView(View(context).apply {
                setBackgroundColor(context.getColor(R.color.card_border))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
            })
        }
    }

    dialog.show()
}
