package org.kryptonmc.krypton.util.gui

import java.awt.Color
import javax.swing.JTextArea
import javax.swing.JTextPane
import javax.swing.text.*

class ANSITextPane : JTextPane() {

    private var remaining = ""

    init {
        background = Color.BLACK
    }

    fun append(color: ANSIColor, text: String) {
        val context = StyleContext.getDefaultStyleContext()
        val attributes = context.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, color.color)
        try {
            document.insertString(document.length, text, attributes)
        } catch (ignored: BadLocationException) {}
    }

    fun appendANSI(text: String) {
        var aPosition = 0
        var aIndex: Int
        var mIndex: Int
        var tempString: String
        var stillSearching: Boolean
        val addString = remaining + text
        remaining = ""

        if (addString.isNotEmpty()) {
            aIndex = addString.indexOf('\u001B')
            if (aIndex == -1) {
                append(currentColor, addString)
                return
            }

            if (aIndex > 0) {
                tempString = addString.substring(0, aIndex)
                append(currentColor, tempString)
                aPosition = aIndex
            }

            stillSearching = true
            while (stillSearching) {
                mIndex = addString.indexOf("m", aPosition)
                if (mIndex < 0) {
                    remaining = addString.substring(aPosition, addString.length)
                    stillSearching = false
                    continue
                } else {
                    tempString = addString.substring(aPosition, mIndex + 1)
                    currentColor = ANSIColor.fromString(tempString)
                }
                aPosition = mIndex + 1

                aIndex = addString.indexOf('\u001B', aPosition)
                if (aIndex == -1) {
                    tempString = addString.substring(aPosition, addString.length)
                    append(currentColor, tempString)
                    stillSearching = false
                    continue
                }

                tempString = addString.substring(aPosition, aIndex)
                aPosition = aIndex
                append(currentColor, tempString)
            }
        }
    }

    companion object {

        private var currentColor = ANSIColor.RESET
    }
}

private const val ANSI_START = "\\u001B\\["

enum class ANSIColor(code: String, val color: Color) {

    D_BLACK("(0;)?30m", Color.BLACK),
    D_RED("(0;)?31m", Color.getHSBColor(0F, 1F, 0.502F)),
    D_GREEN("(0;)?32m", Color.getHSBColor(0.333F, 1F, 0.502F)),
    D_YELLOW("(0;)?33m", Color.getHSBColor(0.167F, 1F, 0.502F)),
    D_BLUE("(0;)?34m", Color.getHSBColor(0.667F, 1F, 0.502F)),
    D_MAGENTA("(0;)?35m", Color.getHSBColor(0.833F, 1F, 0.502F)),
    D_CYAN("(0;)?36m", Color.getHSBColor(0.5F, 1F, 0.502F)),
    D_WHITE("(0;)?37m", Color.getHSBColor(0F, 0F, 0.753F)),
    B_BLACK("1;30m", Color.getHSBColor(0F, 0F, 0.502F)),
    B_RED("1;31m", Color.getHSBColor(0F, 1F, 1F)),
    B_GREEN("1;32m", Color.getHSBColor(0.333F, 1F, 1F)),
    B_YELLOW("1;33m", Color.getHSBColor(0.167F, 1F, 1F)),
    B_BLUE("1;34m", Color.getHSBColor(0.667F, 1F, 1F)),
    B_MAGENTA("1;35m", Color.getHSBColor(0.833F, 1F, 1F)),
    B_CYAN("1;36m", Color.getHSBColor(0.5F, 1F, 1F)),
    B_WHITE("1;37m", Color.getHSBColor(0F, 0F, 1F)),
    RESET("0m", Color.getHSBColor(0F, 0F, 1F));

    val regex = (ANSI_START + code).toRegex()

    companion object {

        fun fromString(string: String): ANSIColor {
            values().forEach { if (it.regex matches string) return it }
            return ANSIColor.B_WHITE
        }
    }
}