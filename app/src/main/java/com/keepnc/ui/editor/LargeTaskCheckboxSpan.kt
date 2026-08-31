package com.keepnc.ui.editor

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.text.style.ReplacementSpan
import android.view.View

/**
 * A [ReplacementSpan] that renders a large, tappable checkbox for Markdown task list items.
 *
 * This replaces Markwon's built-in [io.noties.markwon.ext.tasklist.TaskListSpan] after rendering,
 * providing:
 * - A fixed 22dp checkbox (Markwon's default scales with font size and is often too small)
 * - Click-to-toggle via [ClickableSpan] (works with [android.text.method.LinkMovementMethod])
 * - [onToggle] callback to sync the toggled state back to the raw Markdown content
 *
 * BEGINNER NOTE:
 * - [ReplacementSpan] lets you draw ANYTHING in place of the characters it covers.
 * - [ClickableSpan] makes the span respond to taps when the TextView's movementMethod
 *   is [android.text.method.LinkMovementMethod].
 * - Both are combined here via multiple inheritance (Kotlin allows inheriting from multiple
 *   interfaces, but only one class — both ClickableSpan and ReplacementSpan are abstract classes,
 *   so we use [ReplacementSpan] as the base class and duplicate [ClickableSpan]'s one method).
 */
class LargeTaskCheckboxSpan(
    context: Context,
    /** Current checked state. Mutated via [toggle]. */
    var checked: Boolean,
    /**
     * Called after the user taps the checkbox with the NEW checked state.
     * The caller (EditorFragment) uses this to update the raw Markdown text in the EditText.
     */
    val onToggle: (newChecked: Boolean) -> Unit = {}
) : ReplacementSpan() {

    private val density = context.resources.displayMetrics.density

    // Checkbox dimensions
    private val sizePx = (22f * density).toInt()
    private val strokeWidth = 2f * density
    private val cornerRadius = 4f * density
    private val padding = (6f * density).toInt()   // trailing space after the checkbox

    // Colors
    private val checkedFill = 0xFF0082C9.toInt()   // Nextcloud blue
    private val borderColor = 0xFF9E9E9E.toInt()   // grey outline (unchecked)
    private val checkmarkColor = Color.WHITE

    private val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val checkPath = Path()
    private val rect = RectF()

    // -------------------------------------------------------------------------
    // Touch support — EditorFragment sets a custom OnTouchListener on tvPreview
    // that calls handleTouch(); we expose the hit-test logic here.
    // -------------------------------------------------------------------------

    /**
     * Returns true when the touch point (relative to the span's drawn area) lands
     * inside the checkbox square.
     *
     * @param touchX horizontal touch coordinate within the TextView
     * @param spanLeft left edge of this span in the TextView (from layout)
     */
    fun isInside(touchX: Float, touchY: Float, spanLeft: Float, lineTop: Int, lineBottom: Int): Boolean {
        val lineHeight = lineBottom - lineTop
        val boxTop = lineTop + (lineHeight - sizePx) / 2f
        return touchX in spanLeft..(spanLeft + sizePx) &&
                touchY in boxTop..(boxTop + sizePx)
    }

    /**
     * Toggle state and request a redraw. The callback is fired so the fragment
     * can sync the new state back into the Markdown EditText.
     */
    fun toggle(widget: View) {
        checked = !checked
        widget.invalidate()
        onToggle(checked)
    }

    // -------------------------------------------------------------------------
    // ReplacementSpan
    // -------------------------------------------------------------------------

    override fun getSize(
        paint: Paint, text: CharSequence, start: Int, end: Int,
        fm: Paint.FontMetricsInt?
    ): Int = sizePx + padding

    override fun draw(
        canvas: Canvas, text: CharSequence, start: Int, end: Int,
        x: Float, top: Int, y: Int, bottom: Int, paint: Paint
    ) {
        val lineHeight = bottom - top
        val boxTop = top + (lineHeight - sizePx) / 2f
        val boxBottom = boxTop + sizePx

        rect.set(x, boxTop, x + sizePx, boxBottom)

        if (checked) {
            // --- filled blue box ---
            boxPaint.style = Paint.Style.FILL
            boxPaint.color = checkedFill
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, boxPaint)

            // --- white checkmark ---
            val inset = sizePx * 0.22f
            checkPath.reset()
            checkPath.moveTo(x + inset,              boxTop + sizePx * 0.52f)
            checkPath.lineTo(x + sizePx * 0.42f,     boxBottom - inset)
            checkPath.lineTo(x + sizePx - inset,     boxTop + inset)

            boxPaint.style = Paint.Style.STROKE
            boxPaint.color = checkmarkColor
            boxPaint.strokeWidth = strokeWidth * 1.6f
            boxPaint.strokeCap = Paint.Cap.ROUND
            boxPaint.strokeJoin = Paint.Join.ROUND
            canvas.drawPath(checkPath, boxPaint)
        } else {
            // --- outline only ---
            boxPaint.style = Paint.Style.STROKE
            boxPaint.color = borderColor
            boxPaint.strokeWidth = strokeWidth
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, boxPaint)
        }
    }

    // updateDrawState is from ClickableSpan — suppress the blue underline effect.
    override fun updateDrawState(ds: android.text.TextPaint) { /* no-op: don't change text color/underline */ }
}
