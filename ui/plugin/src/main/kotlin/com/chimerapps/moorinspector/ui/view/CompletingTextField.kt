package com.chimerapps.moorinspector.ui.view

import com.chimerapps.moorinspector.ui.util.dispatchMain
import com.chimerapps.moorinspector.ui.util.ensureMain
import java.awt.Color
import java.awt.Graphics
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.JTextField
import javax.swing.SwingWorker
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class CompletingTextField(private val completionProvider: CompletionProvider) : JTextField() {

    private var nextSuggestion: String? = null

    init {
        document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) {
                dispatchMain {
                    updateSuggestions()
                }
            }

            override fun removeUpdate(e: DocumentEvent?) {
                dispatchMain {
                    updateSuggestions()
                }
            }

            override fun changedUpdate(e: DocumentEvent?) {
                dispatchMain {
                    updateSuggestions()
                }
            }
        })
        focusTraversalKeysEnabled = false
        addKeyListener(object : KeyAdapter() {
            override fun keyTyped(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_TAB)
                    complete()
            }

            override fun keyReleased(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_RIGHT && caretPosition == text.length)
                    complete()
            }
        })
    }

    private fun updateSuggestions() {
        val current = text

        nextSuggestion = completionProvider.provideCompletion(
            if (caretPosition > 0)
                current.substring(0, caretPosition)
            else
                current
        )
        repaint()
    }

    private fun complete() {
        nextSuggestion?.let {
            text += it
        }
        updateSuggestions()
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)

        nextSuggestion?.let {
            g.font = font

            val rect = g.getFontMetrics(g.font).getStringBounds(text, g)
            val x = rect.width

            val y: Int = height / 2 + insets.top

            g.color = Color.red
            g.drawString(it, x.toInt() + insets.left, y)
        }
    }

}

interface CompletionProvider {

    fun provideCompletion(inputString: String): String?

}