package com.happypuppy.toastmasterstimer.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guard against user-facing text creeping back into code: UI strings live in
 * res/values/strings.xml. Fails when a Compose `Text("...")`, `text = "..."`,
 * `contentDescription = "..."` and similar literals with a letter in them appear in app/src/main.
 *
 * Also catches `Text(if (c) "A" else "B")` and `text = if (c) "A" else "B"`. Narrow allow-list
 * ([ALLOWED_LITERALS]): animation `label` values, which are Compose tooling names and never
 * shown. Known limits: a literal that reaches the UI through a variable, a `when` expression or
 * a helper function is not seen, and neither is a literal inside a condition that has quotes of
 * its own; review those by eye.
 */
class StringLiteralGuardTest {

    @Test
    fun `no user-facing string literals in app main sources`() {
        val root = File("src/main/java")
        assertTrue("run from the app module directory, missing ${root.absolutePath}", root.isDirectory)

        val violations = root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { file -> findUserFacingLiterals(file.readText()).map { "${file.path}: $it" } }
            .toList()

        assertEquals(
            "Move these to res/values/strings.xml (or extend the allow-list with a reason):\n" +
                violations.joinToString("\n"),
            emptyList<String>(),
            violations
        )
    }

    @Test
    fun `detector flags text and content description literals`() {
        val source = """
            Text("Hello")
            Text(
                "Wrapped"
            )
            Text(text = "Named")
            Icon(Icons.Default.Add, contentDescription = "Add")
            DropdownMenuItem(text = { Text("Item") })
            Modifier.semantics { onClick(label = "Do it") { true } }
            CustomAccessibilityAction("Delete") { true }
            Surface(title = "T")
            Snackbar(actionLabel = "Undo")
            label = "Tooling name"
        """.trimIndent()

        val found = findUserFacingLiterals(source)

        assertEquals(
            listOf("Hello", "Wrapped", "Named", "Add", "Item", "Do it", "Delete", "T", "Undo", "Tooling name"),
            found
        )
    }

    @Test
    fun `detector flags literals in if else expressions used as text`() {
        val source = """
            Text(if (isEmpty) "First" else "Second")
            Text(
                text = if (filtered)
                    "Third"
                else stringResource(R.string.ok)
            )
            Text(text = if (filtered) stringResource(R.string.ok) else "Fourth")
            Text(text = if (filtered) stringResource(R.string.ok) else stringResource(R.string.cancel))
            Text(if (isVisible) stringResource(R.string.hide) else label)
        """.trimIndent()

        assertEquals(listOf("First", "Second", "Third", "Fourth"), findUserFacingLiterals(source))
    }

    @Test
    fun `detector ignores resources, symbols and the allow-list`() {
        val source = """
            Text(stringResource(R.string.app_name))
            Text(text = speakerSpeechTypeDisplayName(speaker))
            Text("▲")
            Text("")
            contentDescription = null
            label = "backgroundColor"
            label = "aspectRatio"
            label = "progressAnimation"
            Log.d("Tag", "message with words")
            val id = "table_topics"
        """.trimIndent()

        assertEquals(emptyList<String>(), findUserFacingLiterals(source))
    }

    private companion object {
        /** Compose animation labels (debug tooling names, never displayed). */
        val ALLOWED_LITERALS = setOf("backgroundColor", "aspectRatio", "progressAnimation")

        // A literal counts only if it contains a letter: "▲" or "" are symbols, not copy.
        const val LITERAL = "\"([^\"\\n]*[A-Za-z][^\"\\n]*)\""
        const val TEXT_SINK =
            """(?:\bText\(|\b(?:text|title|description|placeholder|message|actionLabel|onClickLabel|contentDescription|label|supportingText|hint)\s*=)"""
        val PATTERNS = listOf(
            Regex("""\bText\(\s*$LITERAL"""),
            Regex("""\b(?:text|title|description|placeholder|message|actionLabel|onClickLabel|contentDescription|label|supportingText|hint)\s*=\s*$LITERAL"""),
            Regex("""\bonClick\(\s*label\s*=\s*$LITERAL"""),
            Regex("""\bCustomAccessibilityAction\(\s*$LITERAL"""),
            // Text(if (c) "A" else "B") and text = if (c) "A" else "B": the literal after the condition...
            Regex("""$TEXT_SINK\s*if\s*\([^"{}]{0,200}\)\s*$LITERAL"""),
            // ...and the literal after else (the then-branch may itself be a resource call)
            Regex("""$TEXT_SINK\s*if\b[^{}]{0,300}?\belse\s+$LITERAL""")
        )

        fun findUserFacingLiterals(source: String): List<String> =
            PATTERNS.flatMap { pattern -> pattern.findAll(source).map { it.groups[1]!!.range.first to it.groupValues[1] } }
                .distinct()
                .sortedBy { it.first }
                .map { it.second }
                .filterNot { it in ALLOWED_LITERALS }
    }
}
