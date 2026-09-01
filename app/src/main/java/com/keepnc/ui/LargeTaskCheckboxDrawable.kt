package com.keepnc.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.keepnc.R

/**
 * Custom [Drawable] for Markdown checklist items in the note editor.
 *
 * Renders 16dp checkboxes with Google Keep styling:
 * - Unchecked: clean rounded outline box (1.5dp stroke, 3dp corner radius)
 * - Checked: filled Nextcloud Blue box with white checkmark
 */
class LargeTaskCheckboxDrawable(
    context: Context,
    private val sizeDp: Float = 16f
) : Drawable() {

    private val density = context.resources.displayMetrics.density
    private val sizePx = (sizeDp * density).toInt()
    private val strokeWidth = 1.5f * density
    private val cornerRadius = 3f * density

    private val checkedColor = ContextCompat.getColor(context, R.color.nextcloud_blue)
    private val uncheckedBorderColor = 0xFF757575.toInt()
    private val checkmarkColor = Color.WHITE

    private var isChecked = false

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val rect = RectF()

    override fun isStateful(): Boolean = true

    override fun onStateChange(state: IntArray): Boolean {
        val checked = state.contains(android.R.attr.state_checked)
        if (isChecked != checked) {
            isChecked = checked
            invalidateSelf()
            return true
        }
        return super.onStateChange(state)
    }

    override fun getIntrinsicWidth(): Int = sizePx
    override fun getIntrinsicHeight(): Int = sizePx

    override fun draw(canvas: Canvas) {
        val b = bounds
        val boxLeft = b.left.toFloat()

        // b.top is computed by Markwon's TaskListSpan as (lineTop + (lineHeight - sizePx) / 2).
        // Since tv_preview has lineSpacingExtra (which adds extra spacing at the bottom of each line),
        // we shift the box slightly up (-1.5dp) to match the visual text baseline perfectly across all devices.
        val lineSpacingCorrection = 1.5f * density
        val boxTop = b.top.toFloat() - lineSpacingCorrection
        val boxRight = boxLeft + sizePx
        val boxBottom = boxTop + sizePx

        rect.set(boxLeft, boxTop, boxRight, boxBottom)

        if (isChecked) {
            // Fill box
            paint.style = Paint.Style.FILL
            paint.color = checkedColor
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)

            // Draw white checkmark
            val inset = sizePx * 0.22f
            path.reset()
            path.moveTo(boxLeft + inset, boxTop + sizePx * 0.52f)
            path.lineTo(boxLeft + sizePx * 0.42f, boxBottom - inset)
            path.lineTo(boxRight - inset, boxTop + inset)

            paint.style = Paint.Style.STROKE
            paint.color = checkmarkColor
            paint.strokeWidth = strokeWidth * 1.3f
            paint.strokeCap = Paint.Cap.ROUND
            paint.strokeJoin = Paint.Join.ROUND
            canvas.drawPath(path, paint)
        } else {
            // Draw outline box
            paint.style = Paint.Style.STROKE
            paint.color = uncheckedBorderColor
            paint.strokeWidth = strokeWidth
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
        }
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
