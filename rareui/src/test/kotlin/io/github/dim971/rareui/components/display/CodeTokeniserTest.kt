/*
 * CodeTokeniserTest.kt
 * The highlighter standing in for Prism. The same assertions run on iOS.
 */

package io.github.dim971.rareui.components.display

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CodeTokeniserTest {
    private fun text(
        source: String,
        language: CodeLanguage,
        kind: CodeTokenKind,
    ): List<String> = codeTokenise(source, language).flatten().filter { it.kind == kind }.map { it.text }

    @Test
    fun `source is split into lines, and an empty line stays a line`() {
        val lines = codeTokenise("let a = 1\n\nlet b = 2", CodeLanguage.SWIFT)
        assertEquals(3, lines.size)
        assertTrue(lines[1].isEmpty())
    }

    @Test
    fun `a language's reserved words are picked out and nothing else is`() {
        assertEquals(listOf("let"), text("let value = 1", CodeLanguage.SWIFT, CodeTokenKind.KEYWORD))
        assertEquals(listOf("val"), text("val value = 1", CodeLanguage.KOTLIN, CodeTokenKind.KEYWORD))
        assertEquals(
            listOf("const"),
            text("const value = 1", CodeLanguage.TYPESCRIPT, CodeTokenKind.KEYWORD),
        )
        // A Swift keyword is not a Kotlin one.
        assertTrue(text("let value = 1", CodeLanguage.KOTLIN, CodeTokenKind.KEYWORD).isEmpty())
    }

    @Test
    fun `a string keeps its quotes and survives an escaped one inside it`() {
        assertEquals(
            listOf("\"hello\""),
            text("""let a = "hello"""", CodeLanguage.SWIFT, CodeTokenKind.STRING),
        )
        assertEquals(
            listOf("\"say \\\"hi\\\"\""),
            text("""let a = "say \"hi\""""", CodeLanguage.SWIFT, CodeTokenKind.STRING),
        )
    }

    @Test
    fun `an unterminated string stops at the end of its line rather than eating the file`() {
        val lines = codeTokenise("let a = \"oops\nlet b = 2", CodeLanguage.SWIFT)
        assertEquals(2, lines.size)
        assertTrue(lines[1].any { it.kind == CodeTokenKind.KEYWORD && it.text == "let" })
    }

    @Test
    fun `a line comment runs to the end of the line and no further`() {
        val lines = codeTokenise("let a = 1 // why\nlet b = 2", CodeLanguage.SWIFT)
        assertTrue(lines[0].any { it.kind == CodeTokenKind.COMMENT })
        assertFalse(lines[1].any { it.kind == CodeTokenKind.COMMENT })
        // A shell comment starts with a hash instead.
        assertTrue(text("# note", CodeLanguage.SHELL, CodeTokenKind.COMMENT).isNotEmpty())
        assertTrue(text("// not a comment here", CodeLanguage.JSON, CodeTokenKind.COMMENT).isEmpty())
    }

    @Test
    fun `a block comment spans lines and every line of it is a comment`() {
        val lines = codeTokenise("/* one\n   two */\nlet a = 1", CodeLanguage.SWIFT)
        assertTrue(lines[0].all { it.kind == CodeTokenKind.COMMENT })
        assertTrue(lines[1].any { it.kind == CodeTokenKind.COMMENT })
        assertTrue(lines[2].any { it.kind == CodeTokenKind.KEYWORD })
    }

    @Test
    fun `a name followed by a bracket is being called`() {
        assertEquals(listOf("print"), text("print(value)", CodeLanguage.SWIFT, CodeTokenKind.FUNCTION))
        // Even with a space between, which is how some styles write it.
        assertEquals(listOf("print"), text("print (value)", CodeLanguage.SWIFT, CodeTokenKind.FUNCTION))
    }

    @Test
    fun `a capitalised name is a type`() {
        assertEquals(
            listOf("View"),
            text("var body: some View", CodeLanguage.SWIFT, CodeTokenKind.CLASS_NAME),
        )
    }

    @Test
    fun `an annotation is an attribute rather than a plain word`() {
        assertEquals(
            listOf("@State"),
            text("@State private var value = 1", CodeLanguage.SWIFT, CodeTokenKind.ATTRIBUTE_NAME),
        )
        assertEquals(
            listOf("@Composable"),
            text("@Composable fun Total()", CodeLanguage.KOTLIN, CodeTokenKind.ATTRIBUTE_NAME),
        )
    }

    @Test
    fun `in JSON a word before a colon is a key`() {
        // Quoted keys are strings, as they should be; a bare word before a colon is the
        // thing worth colouring differently.
        assertEquals(listOf("name"), text("{ name: 1 }", CodeLanguage.JSON, CodeTokenKind.PROPERTY))
    }

    @Test
    fun `numbers are numbers, including the ones with a decimal point in them`() {
        assertEquals(listOf("42"), text("let a = 42", CodeLanguage.SWIFT, CodeTokenKind.NUMBER))
        assertEquals(listOf("3.14"), text("let a = 3.14", CodeLanguage.SWIFT, CodeTokenKind.NUMBER))
    }

    @Test
    fun `plain text comes back as plain text and loses nothing`() {
        for (language in CodeLanguage.entries) {
            val source = "let a = 1 // note\nprint(\"hi\")"
            val rebuilt =
                codeTokenise(source, language).joinToString("\n") { line ->
                    line.joinToString("") { it.text }
                }
            assertEquals("$language lost or gained characters", source, rebuilt)
        }
    }
}
