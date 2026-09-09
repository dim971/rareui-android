/*
 * CodeTokeniser.kt
 * A small syntax highlighter, standing in for the one upstream gets from Prism.
 *
 * The scope is deliberately narrow. Prism knows nearly three hundred languages, and pulling
 * in something comparable would break the rule both this library and its iOS twin hold to,
 * which is that they have no dependencies at all. What a component gallery actually shows
 * is a handful of languages, so this covers those and falls back to plain text rather than
 * guessing at the rest.
 *
 * It is a scanner rather than a grammar: comments, strings, numbers and words, with a
 * word's kind decided by what it is and what follows it. That is enough to read well and
 * little enough to be sure of.
 */

package io.github.dim971.rareui.components.display

/** What a piece of source code is. */
public enum class CodeTokenKind {
    /** Anything with no other kind. */
    PLAIN,

    /** A comment, of either sort. */
    COMMENT,

    /** Brackets, commas, semicolons. */
    PUNCTUATION,

    /** Arithmetic, comparison and assignment. */
    OPERATOR,

    /** A reserved word, drawn in the accent itself. */
    KEYWORD,

    /** A string or a character. */
    STRING,

    /** A name being called. */
    FUNCTION,

    /** An attribute's name, in a markup language. */
    ATTRIBUTE_NAME,

    /** A number, or a word that behaves like one. */
    NUMBER,

    /** A type's name. */
    CLASS_NAME,

    /** A property or a variable. */
    PROPERTY,

    /** A regular expression. */
    REGEX,
    ;

    /** Whether the kind is drawn in italic, as upstream draws comments and attribute names. */
    internal val isItalic: Boolean
        get() = this == COMMENT || this == ATTRIBUTE_NAME
}

/** A language [CodeBlock] can colour. */
public enum class CodeLanguage(
    /** The name shown in the header when no filename is given. */
    public val displayName: String,
) {
    /** Swift. */
    SWIFT("Swift"),

    /** Kotlin. */
    KOTLIN("Kotlin"),

    /** TypeScript, and therefore JavaScript. */
    TYPESCRIPT("TypeScript"),

    /** JSON. */
    JSON("JSON"),

    /** A shell script. */
    SHELL("Shell"),

    /** Anything else, drawn without colour. */
    PLAIN("Text"),
    ;

    /** The words the language reserves. */
    internal val keywords: Set<String>
        get() =
            when (this) {
                SWIFT -> SwiftKeywords
                KOTLIN -> KotlinKeywords
                TYPESCRIPT -> TypeScriptKeywords
                JSON -> setOf("true", "false", "null")
                SHELL -> ShellKeywords
                PLAIN -> emptySet()
            }

    /** What starts a comment that runs to the end of the line. */
    internal val lineComment: String?
        get() =
            when (this) {
                SWIFT, KOTLIN, TYPESCRIPT -> "//"
                SHELL -> "#"
                JSON, PLAIN -> null
            }

    /** Whether the language has `/* */` comments. */
    internal val hasBlockComments: Boolean
        get() = this == SWIFT || this == KOTLIN || this == TYPESCRIPT
}

private val SwiftKeywords =
    setOf(
        "actor",
        "any",
        "as",
        "associatedtype",
        "async",
        "await",
        "break",
        "case",
        "catch",
        "class",
        "continue",
        "default",
        "defer",
        "deinit",
        "do",
        "else",
        "enum",
        "extension",
        "fallthrough",
        "false",
        "fileprivate",
        "final",
        "for",
        "func",
        "guard",
        "if",
        "import",
        "in",
        "indirect",
        "init",
        "inout",
        "internal",
        "is",
        "lazy",
        "let",
        "mutating",
        "nil",
        "nonisolated",
        "open",
        "operator",
        "private",
        "protocol",
        "public",
        "repeat",
        "return",
        "self",
        "some",
        "static",
        "struct",
        "subscript",
        "super",
        "switch",
        "throw",
        "throws",
        "true",
        "try",
        "typealias",
        "var",
        "where",
        "while",
    )

private val KotlinKeywords =
    setOf(
        "as",
        "break",
        "by",
        "catch",
        "class",
        "companion",
        "constructor",
        "continue",
        "data",
        "do",
        "else",
        "enum",
        "external",
        "false",
        "final",
        "finally",
        "for",
        "fun",
        "get",
        "if",
        "import",
        "in",
        "infix",
        "init",
        "inline",
        "interface",
        "internal",
        "is",
        "lateinit",
        "null",
        "object",
        "open",
        "operator",
        "override",
        "package",
        "private",
        "protected",
        "public",
        "return",
        "sealed",
        "set",
        "super",
        "suspend",
        "this",
        "throw",
        "true",
        "try",
        "typealias",
        "val",
        "var",
        "when",
        "where",
        "while",
    )

private val TypeScriptKeywords =
    setOf(
        "as",
        "async",
        "await",
        "break",
        "case",
        "catch",
        "class",
        "const",
        "continue",
        "default",
        "delete",
        "do",
        "else",
        "enum",
        "export",
        "extends",
        "false",
        "finally",
        "for",
        "from",
        "function",
        "if",
        "implements",
        "import",
        "in",
        "instanceof",
        "interface",
        "let",
        "new",
        "null",
        "of",
        "return",
        "satisfies",
        "static",
        "super",
        "switch",
        "this",
        "throw",
        "true",
        "try",
        "type",
        "typeof",
        "undefined",
        "var",
        "void",
        "while",
        "yield",
    )

private val ShellKeywords =
    setOf(
        "case",
        "cd",
        "do",
        "done",
        "echo",
        "elif",
        "else",
        "esac",
        "exit",
        "export",
        "fi",
        "for",
        "function",
        "if",
        "in",
        "local",
        "return",
        "set",
        "then",
        "while",
    )

/** One coloured run of source. */
public data class CodeToken(
    /** The text itself. */
    public val text: String,
    /** What it is. */
    public val kind: CodeTokenKind,
)

/** The characters this scanner treats as punctuation and as operators. */
private const val PUNCTUATION = "()[]{},;:"
private const val OPERATORS = "+-*/%=<>!&|^~?."

/**
 * Splits source into coloured runs, one list per line.
 *
 * Lines are kept apart rather than joined, because the gutter, the highlight wash and the
 * hover all work a line at a time.
 *
 * @param source the code.
 * @param language what it is written in.
 * @return one list of tokens per line.
 */
public fun codeTokenise(
    source: String,
    language: CodeLanguage,
): List<List<CodeToken>> = CodeScanner(source, language).run()

/** Walks source code once, emitting runs as it goes. */
private class CodeScanner(
    source: String,
    private val language: CodeLanguage,
) {
    private val source = source.toCharArray()
    private var index = 0
    private val lines = mutableListOf<List<CodeToken>>()
    private var current = mutableListOf<CodeToken>()

    @Suppress("CyclomaticComplexMethod")
    fun run(): List<List<CodeToken>> {
        while (index < source.size) {
            val character = source[index]
            val comment = language.lineComment

            when {
                character == '\n' -> {
                    lines += current
                    current = mutableListOf()
                    index++
                }

                comment != null && matches(comment) -> take(CodeTokenKind.COMMENT) { it != '\n' }
                language.hasBlockComments && matches("/*") -> takeBlockComment()
                character == '"' || character == '\'' || character == '`' -> takeString(character)
                character.isDigit() ->
                    take(CodeTokenKind.NUMBER) { it.isLetterOrDigit() || it == '.' || it == '_' }

                character.isLetter() || character == '_' || character == '@' || character == '$' ->
                    takeWord()

                character in PUNCTUATION -> {
                    emit(character.toString(), CodeTokenKind.PUNCTUATION)
                    index++
                }

                character in OPERATORS -> take(CodeTokenKind.OPERATOR) { it in OPERATORS }

                else -> {
                    take(CodeTokenKind.PLAIN) { it == ' ' || it == '\t' }
                    // Anything left is something this scanner has no opinion about.
                    if (index < source.size && source[index] != '\n' && !recognised(source[index])) {
                        emit(source[index].toString(), CodeTokenKind.PLAIN)
                        index++
                    }
                }
            }
        }

        lines += current
        return lines
    }

    private fun recognised(character: Char): Boolean =
        character.isLetterOrDigit() ||
            character == '_' ||
            character == '@' ||
            character == '$' ||
            character == '"' ||
            character == '\'' ||
            character == '`' ||
            character in PUNCTUATION ||
            character in OPERATORS ||
            character == ' ' ||
            character == '\t'

    private fun matches(text: String): Boolean {
        if (index + text.length > source.size) return false
        return text.indices.all { source[index + it] == text[it] }
    }

    private fun emit(
        text: String,
        kind: CodeTokenKind,
    ) {
        if (text.isNotEmpty()) current += CodeToken(text, kind)
    }

    private inline fun take(
        kind: CodeTokenKind,
        predicate: (Char) -> Boolean,
    ) {
        val start = index
        while (index < source.size && predicate(source[index])) index++
        emit(String(source, start, index - start), kind)
    }

    /** A block comment, which is the one construct here that spans lines. */
    private fun takeBlockComment() {
        val text = StringBuilder()
        while (index < source.size) {
            if (matches("*/")) {
                text.append("*/")
                index += 2
                break
            }
            if (source[index] == '\n') {
                emit(text.toString(), CodeTokenKind.COMMENT)
                text.clear()
                lines += current
                current = mutableListOf()
                index++
                continue
            }
            text.append(source[index])
            index++
        }
        emit(text.toString(), CodeTokenKind.COMMENT)
    }

    private fun takeString(quote: Char) {
        val text = StringBuilder().append(quote)
        index++
        while (index < source.size) {
            val character = source[index]
            // An escape takes the next character with it, so a quote inside a string does
            // not end it.
            if (character == '\\' && index + 1 < source.size) {
                text.append(character).append(source[index + 1])
                index += 2
                continue
            }
            // A string that runs to the end of the line is unterminated, not multi-line.
            if (character == '\n') break
            text.append(character)
            index++
            if (character == quote) break
        }
        emit(text.toString(), CodeTokenKind.STRING)
    }

    private fun takeWord() {
        val start = index
        if (source[index] == '@' || source[index] == '$') index++
        while (index < source.size && (source[index].isLetterOrDigit() || source[index] == '_')) index++
        val word = String(source, start, index - start)
        emit(word, kindOf(word))
    }

    /** What a word is, decided by the word itself and by what comes after it. */
    private fun kindOf(word: String): CodeTokenKind {
        if (word in language.keywords) return CodeTokenKind.KEYWORD
        // An attribute or annotation: @Observable, @Composable.
        if (word.startsWith("@")) return CodeTokenKind.ATTRIBUTE_NAME

        var probe = index
        while (probe < source.size && source[probe] == ' ') probe++

        // A name followed by a bracket is being called, whatever else it might be.
        if (probe < source.size && source[probe] == '(') return CodeTokenKind.FUNCTION
        // In JSON a word followed by a colon is a key rather than a value.
        if (language == CodeLanguage.JSON && probe < source.size && source[probe] == ':') {
            return CodeTokenKind.PROPERTY
        }

        if (word.firstOrNull()?.isUpperCase() == true) return CodeTokenKind.CLASS_NAME
        return CodeTokenKind.PLAIN
    }
}
