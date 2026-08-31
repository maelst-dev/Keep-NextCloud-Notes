package com.keepnc.ui

import android.content.Context
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonVisitor
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.linkify.LinkifyPlugin
import org.commonmark.node.SoftLineBreak

/**
 * Factory for creating configured [Markwon] instances.
 *
 * Provides:
 * - [createForEditor]: Used in note editor preview with 16dp checkboxes and a 26dp Google Keep-style gap.
 * - [createForCard]: Used in note cards on main screen with compact default checkboxes.
 */
object MarkwonFactory {

    /** Creates Markwon instance for the note editor with 16dp checkboxes and generous text spacing. */
    fun createForEditor(context: Context): Markwon {
        val density = context.resources.displayMetrics.density
        return Markwon.builder(context)
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TablePlugin.create(context))
            .usePlugin(TaskListPlugin.create(LargeTaskCheckboxDrawable(context, 16f)))
            .usePlugin(LinkifyPlugin.create())
            .usePlugin(object : AbstractMarkwonPlugin() {
                override fun configureTheme(builder: MarkwonTheme.Builder) {
                    // blockMargin sets the indentation before list text:
                    // 32dp total - 16dp checkbox = 16dp clean gap between checkbox and text
                    builder.blockMargin((32f * density).toInt())
                }

                override fun configureVisitor(builder: MarkwonVisitor.Builder) {
                    builder.on(SoftLineBreak::class.java) { visitor, _ ->
                        visitor.ensureNewLine()
                    }
                }
            })
            .build()
    }

    /** Creates Markwon instance for note cards in the grid with compact checkboxes. */
    fun createForCard(context: Context): Markwon = Markwon.builder(context)
        .usePlugin(StrikethroughPlugin.create())
        .usePlugin(TablePlugin.create(context))
        .usePlugin(TaskListPlugin.create(context))
        .usePlugin(LinkifyPlugin.create())
        .usePlugin(object : AbstractMarkwonPlugin() {
            override fun configureVisitor(builder: MarkwonVisitor.Builder) {
                builder.on(SoftLineBreak::class.java) { visitor, _ ->
                    visitor.ensureNewLine()
                }
            }
        })
        .build()

    /** Default factory method (delegates to editor). */
    fun create(context: Context): Markwon = createForEditor(context)
}
