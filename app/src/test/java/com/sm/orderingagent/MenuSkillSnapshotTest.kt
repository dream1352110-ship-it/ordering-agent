package com.sm.orderingagent

import com.sm.orderingagent.domain.DemoMenu
import com.sm.orderingagent.domain.MenuItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File
import java.security.MessageDigest

object MenuSkillSnapshot {
    const val BEGIN = "<!-- ordering-menu-snapshot:begin -->"
    const val END = "<!-- ordering-menu-snapshot:end -->"
    const val FORMAT = "ordering-menu-v1"

    fun render(menu: List<MenuItem> = DemoMenu.items): String {
        require(menu.map { it.id }.distinct().size == menu.size)
        val groups = menu.flatMap { it.options }.distinct()
        menu.forEach { item ->
            require(item.options.map { it.id }.distinct().size == item.options.size)
            item.unitPrice(item.defaults)
        }
        val body = buildString {
            appendLine("Options: groupRef | key | label | default | allowed id=label")
            groups.forEachIndexed { index, group ->
                appendLine("g${index + 1} | ${group.id} | ${group.label} | ${group.defaultId} | " +
                    group.choices.joinToString(",") { "${it.id}=${it.label}" })
            }
            appendLine()
            appendLine("Items: itemId | name (alias) | groups | complete default options")
            menu.forEach { item ->
                val alias = if (item.id == "meatballs" && item.name.endsWith("\u8089\u4e38")) {
                    " (${item.name.takeLast(2)})"
                } else ""
                val references = item.options.joinToString(",") { "g${groups.indexOf(it) + 1}" }.ifEmpty { "-" }
                val defaults = item.defaults.entries.joinToString(",", "{", "}") { "${it.key}=${it.value}" }
                appendLine("${item.id} | ${item.name}$alias | $references | $defaults")
            }
        }.trimEnd()
        val versionInput = "$FORMAT\n${menu.joinToString("\n")}\n$body"
        val digest = MessageDigest.getInstance("SHA-256").digest(versionInput.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 255) }
        return "$BEGIN\nSource: DemoMenu.items / MenuItem.defaults; format=$FORMAT; menu-sha256=$digest; items=${menu.size}\n\n```text\n$body\n```\n$END"
    }

    fun skillFiles(): List<File> {
        val app = listOf(File("app"), File("."), File("ordering-demo/app"))
            .firstOrNull { File(it, "src/main/java/com/sm/orderingagent/domain/DemoMenu.kt").isFile }
            ?: error("Run from ordering-demo or its app directory")
        return listOf("cli", "function").map {
            File(app, "src/$it/assets/osagent/skills/restaurant-ordering/SKILL.md")
        }
    }

    fun embedded(text: String): String {
        require(text.windowed(BEGIN.length).count { it == BEGIN } == 1) { "Expected one begin marker" }
        require(text.windowed(END.length).count { it == END } == 1) { "Expected one end marker" }
        val start = text.indexOf(BEGIN)
        val finish = text.indexOf(END)
        require(finish > start)
        return text.substring(start, finish + END.length)
    }

    @JvmStatic
    fun main(args: Array<String>) {
        require(args.toList() in listOf(emptyList<String>(), listOf("--check"), listOf("--write")))
        val expected = render()
        if (args.isEmpty()) {
            println(expected)
            return
        }
        skillFiles().forEach { file ->
            val text = file.readText(Charsets.UTF_8).replace("\r\n", "\n")
            val actual = embedded(text)
            if (args.single() == "--write") {
                file.writeText(text.replace(actual, expected), Charsets.UTF_8)
            } else {
                check(actual == expected) { "Regenerate menu snapshot: ${file.path}" }
            }
        }
    }
}

class MenuSkillSnapshotTest {
    @Test
    fun bothSkillsMatchRealMenu() {
        val expected = MenuSkillSnapshot.render()
        MenuSkillSnapshot.skillFiles().forEach { file ->
            val actual = file.readText(Charsets.UTF_8).replace("\r\n", "\n")
            assertEquals("Regenerate snapshot: ${file.path}", expected, MenuSkillSnapshot.embedded(actual))
            assertFalse("Public skill contains a forbidden vendor name", actual.contains("s" + "unmi", ignoreCase = true))
        }
    }

    @Test
    fun menuChangesInvalidateSnapshot() {
        val original = MenuSkillSnapshot.render()
        val item = DemoMenu.items.first()
        assertFalse(original == MenuSkillSnapshot.render(listOf(item.copy(name = "Changed")) + DemoMenu.items.drop(1)))
        assertFalse(original == MenuSkillSnapshot.render(listOf(item.copy(allergenInfoComplete = false)) + DemoMenu.items.drop(1)))
        assertFalse(original == MenuSkillSnapshot.render(DemoMenu.items.dropLast(1)))
        val drink = DemoMenu.items.first { it.options.isNotEmpty() }
        val changed = drink.copy(options = drink.options.map { group ->
            if (group.id == "size") group.copy(defaultId = "large") else group
        })
        assertFalse(original == MenuSkillSnapshot.render(DemoMenu.items.map { if (it.id == drink.id) changed else it }))
    }
}