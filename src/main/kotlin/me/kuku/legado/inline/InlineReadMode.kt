package me.kuku.legado.inline

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseEventArea
import com.intellij.openapi.editor.event.EditorMouseListener
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.Disposer
import com.intellij.ui.JBColor
import com.intellij.util.ui.UIUtil
import me.kuku.legado.dao.CurrentReadData
import me.kuku.legado.state.SettingsService
import me.kuku.legado.toolwindow.IndexUI
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Rectangle
import java.awt.RenderingHints
import java.awt.font.FontRenderContext
import javax.swing.SwingUtilities

/**
 * 行内隐蔽阅读：把当前章节按可配置字数切分（默认 80），
 * 在编辑器光标右侧用 Inlay 显示。
 *
 * 单击逻辑：
 * - 当前没有显示 → 显示当前段
 * - 已显示且仍在同一行 → 下一段
 * - 已显示但点到了另一行 → 只换位置显示，不切段
 */
object InlineReadMode {

    private const val DEFAULT_CHUNK_SIZE = 80
    private const val PROP_ENABLED = "me.kuku.legado.inlineRead.enabled"

    /** 每段字数：读取设置，非法值回退默认 80 */
    @JvmStatic
    val chunkSize: Int
        get() {
            return try {
                val n = SettingsService.getInstance().state.inlineReadChunkSize
                if (n in 1..500) n else DEFAULT_CHUNK_SIZE
            } catch (_: Exception) {
                DEFAULT_CHUNK_SIZE
            }
        }

    @Volatile
    private var enabledInternal: Boolean =
        PropertiesComponent.getInstance().getBoolean(PROP_ENABLED, false)

    private var chunkIndex: Int = 0

    @Volatile
    private var pendingJumpToLastChunk: Boolean = false

    private val parentDisposable = Disposer.newDisposable("legado-inline-read")
    private var listenerInstalled = false

    /** 当前显示的 inlay */
    private var activeInlay: Inlay<*>? = null
    private var activeEditor: Editor? = null
    /** 上次显示时所在的文档行号；用于判断是否“同一行再点” */
    private var lastShowLine: Int = -1
    private var lastShowEditor: Editor? = null

    private val mouseListener = object : EditorMouseListener {
        override fun mouseClicked(event: EditorMouseEvent) {
            if (!enabled) return
            val me = event.mouseEvent
            if (!SwingUtilities.isLeftMouseButton(me)) return
            // 只处理单击，忽略双击/三击后续事件
            if (me.clickCount != 1) return
            if (event.area != EditorMouseEventArea.EDITING_AREA) return
            val editor = event.editor
            if (editor.isDisposed) return
            FileDocumentManager.getInstance().getFile(editor.document) ?: return
            handleClick(editor)
        }
    }

    @JvmStatic
    var enabled: Boolean
        get() = enabledInternal
        set(value) {
            if (enabledInternal == value) {
                if (value) ensureListener()
                return
            }
            enabledInternal = value
            PropertiesComponent.getInstance().setValue(PROP_ENABLED, value, false)
            if (value) {
                ensureListener()
            } else {
                clearInlay()
            }
        }

    @JvmStatic
    fun toggle(): Boolean {
        enabled = !enabled
        return enabled
    }

    @JvmStatic
    fun resetToFirstChunk() {
        if (!pendingJumpToLastChunk) {
            chunkIndex = 0
        }
    }

    @JvmStatic
    fun afterBodyLoaded() {
        val text = plainBody()
        if (text.isEmpty()) {
            chunkIndex = 0
            pendingJumpToLastChunk = false
            return
        }
        if (pendingJumpToLastChunk) {
            pendingJumpToLastChunk = false
            chunkIndex = maxChunkIndex(text)
        } else {
            chunkIndex = chunkIndex.coerceIn(0, maxChunkIndex(text))
        }
    }

    private fun ensureListener() {
        if (listenerInstalled) return
        EditorFactory.getInstance().eventMulticaster.addEditorMouseListener(mouseListener, parentDisposable)
        listenerInstalled = true
    }

    private fun handleClick(editor: Editor) {
        val text = plainBody()
        if (text.isBlank()) {
            showInlay(editor, "暂无正文，请先在 Legado Reader 打开一章")
            return
        }

        val line = try {
            editor.document.getLineNumber(editor.caretModel.offset)
        } catch (_: Exception) {
            -1
        }

        val showing = activeInlay != null &&
                activeInlay?.isValid == true &&
                activeEditor === editor

        if (showing && lastShowEditor === editor && line >= 0 && line == lastShowLine) {
            // 同一行再点 → 下一段
            nextChunk()
        }
        // 未显示，或换行点击：只（重新）显示当前段，不切段
        showCurrent(editor)
    }

    private fun plainBody(): String {
        return CurrentReadData.bodyContent
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .trim()
    }

    private fun maxChunkIndex(text: String): Int {
        if (text.isEmpty()) return 0
        val size = chunkSize
        return (text.length - 1) / size
    }

    private fun currentChunkText(): String {
        val text = plainBody()
        if (text.isEmpty()) return ""
        val size = chunkSize
        val max = maxChunkIndex(text)
        chunkIndex = chunkIndex.coerceIn(0, max)
        val start = chunkIndex * size
        val end = minOf(start + size, text.length)
        return text.substring(start, end).replace('\n', ' ')
    }

    private fun nextChunk() {
        val text = plainBody()
        if (text.isEmpty()) return
        val max = maxChunkIndex(text)
        if (chunkIndex < max) {
            chunkIndex++
            return
        }
        // 本章读完 → 下一章第一段
        val chapters = CurrentReadData.bookChapterList
        val idx = CurrentReadData.bookIndex
        if (idx >= 0 && idx + 1 < chapters.size) {
            CurrentReadData.indexAtomicIncrement()
            chunkIndex = 0
            ApplicationManager.getApplication().invokeLater {
                IndexUI.getInstance().switchChapter(0)
            }
        }
    }

    private fun showCurrent(editor: Editor) {
        val chunk = currentChunkText()
        if (chunk.isBlank()) {
            showInlay(editor, "（无内容）")
            return
        }
        val text = plainBody()
        val max = maxChunkIndex(text)
        val progress = "${chunkIndex + 1}/${max + 1}"
        showInlay(editor, "$chunk ·$progress")
    }

    private fun showInlay(editor: Editor, text: String) {
        if (editor.isDisposed) return
        ApplicationManager.getApplication().invokeLater {
            if (editor.isDisposed) return@invokeLater
            clearInlay()
            val offset = editor.caretModel.offset.coerceIn(0, editor.document.textLength)
            val line = try {
                editor.document.getLineNumber(offset)
            } catch (_: Exception) {
                -1
            }
            val font = resolveDisplayFont(editor, text)
            val color = JBColor(0x6A6A6A, 0x9A9A9A)
            val renderer = ChunkInlayRenderer(text, font, color)
            val inlay = editor.inlayModel.addInlineElement(offset, true, renderer)
            if (inlay != null) {
                activeInlay = inlay
                activeEditor = editor
                lastShowEditor = editor
                lastShowLine = line
            }
        }
    }

    private fun clearInlay() {
        try {
            activeInlay?.dispose()
        } catch (_: Exception) {
        }
        activeInlay = null
        activeEditor = null
        lastShowEditor = null
        lastShowLine = -1
    }

    /**
     * 选一个能显示中文的字体：优先编辑器字体，不行再兜底系统中文字体。
     */
    private fun resolveDisplayFont(editor: Editor, sample: String): Font {
        val size = editor.colorsScheme.editorFontSize.coerceAtLeast(11)
        val editorFont = editor.colorsScheme.getFont(EditorFontType.PLAIN).deriveFont(size.toFloat())
        if (canDisplayAll(editorFont, sample)) {
            return editorFont
        }
        val candidates = listOf(
            "Microsoft YaHei UI",
            "Microsoft YaHei",
            "PingFang SC",
            "Hiragino Sans GB",
            "Noto Sans CJK SC",
            "Source Han Sans SC",
            "WenQuanYi Micro Hei",
            "SimHei",
            "SimSun",
            "Dialog"
        )
        for (name in candidates) {
            val f = Font(name, Font.PLAIN, size)
            if (canDisplayAll(f, sample)) {
                return f
            }
        }
        return UIUtil.getLabelFont().deriveFont(size.toFloat())
    }

    private fun canDisplayAll(font: Font, sample: String): Boolean {
        if (sample.isEmpty()) return true
        return font.canDisplayUpTo(sample) == -1
    }

    private class ChunkInlayRenderer(
        private val text: String,
        private val font: Font,
        private val color: java.awt.Color
    ) : EditorCustomElementRenderer {

        private val paddingLeft = 6
        private val paddingRight = 4

        override fun calcWidthInPixels(inlay: Inlay<*>): Int {
            val frc = FontRenderContext(null, true, true)
            val w = font.getStringBounds(text, frc).width
            return (w + paddingLeft + paddingRight).toInt().coerceAtLeast(8)
        }

        override fun paint(
            inlay: Inlay<*>,
            g: Graphics,
            targetRegion: Rectangle,
            textAttributes: TextAttributes
        ) {
            val g2 = g.create() as Graphics2D
            try {
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                g2.font = font
                g2.color = color
                val fm = g2.fontMetrics
                val x = targetRegion.x + paddingLeft
                val y = targetRegion.y + fm.ascent +
                        ((targetRegion.height - fm.height) / 2).coerceAtLeast(0)
                g2.drawString(text, x, y)
            } finally {
                g2.dispose()
            }
        }
    }
}
