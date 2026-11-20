package com.example.sarisari.manager

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Static checks over the main source tree. Failure messages name the file and the requirement.
 * Validates: Requirements 2.11, 10.8
 */
class SourceRulesTest {

    private val appDir: File = listOf(File("."), File("app")).first { File(it, "src/main").exists() }
    private val mainDir = File(appDir, "src/main")
    private val srcRoot = File(mainDir, "java/com/example/sarisari/manager")

    private fun sources(under: File = srcRoot): List<File> =
        under.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    private fun File.relative(): String = relativeTo(appDir).path.replace('\\', '/')

    private fun violations(files: List<File>, requirement: String, matches: (String) -> Boolean): List<String> =
        files.filter { matches(it.readText()) }.map { "${it.relative()} violates requirement $requirement" }

    private fun assertNone(violations: List<String>) {
        assertTrue(violations.joinToString("\n"), violations.isEmpty())
    }

    @Test
    fun noEmptyPackages() {
        val empty = srcRoot.walkTopDown().filter { it.isDirectory }
            .filter { dir -> dir.walkTopDown().none { it.isFile } }
            .map { "${it.relative()} is an empty package (requirement 2.3)" }.toList()
        assertNone(empty)
    }

    @Test
    fun legacyThemePackageIsAbsent() {
        assertTrue("ui/theme exists (requirement 6.2)", !File(srcRoot, "ui/theme").exists())
    }

    @Test
    fun domainLayerHasNoFrameworkImports() {
        val domain = sources(File(srcRoot, "domain"))
        assertNone(
            violations(domain, "2.4") { text ->
                text.lines().any { line ->
                    line.startsWith("import android.") || line.startsWith("import androidx.") ||
                        line.startsWith("import dagger.")
                }
            }
        )
    }

    @Test
    fun uiLayersDoNotImportRoomPackages() {
        val ui = sources(File(srcRoot, "feature")) + sources(File(srcRoot, "core"))
        assertNone(violations(ui, "2.9, 10.4") { it.contains("import com.example.sarisari.manager.data.local") })
    }

    @Test
    fun exactlyOneNavHost() {
        val count = sources().sumOf { Regex("""\bNavHost\(""").findAll(it.readText()).count() }
        assertEquals("expected exactly one NavHost( (requirement 7.1)", 1, count)
    }

    @Test
    fun dispatchersAreOnlyReferencedInDispatcherModule() {
        val others = sources().filter { it.name != "DispatcherModule.kt" }
        assertNone(violations(others, "4.8") { Regex("""\bDispatchers\.""").containsMatchIn(it) })
    }

    @Test
    fun noDestructiveMigrationOrMainThreadQueries() {
        assertNone(
            violations(sources(), "5.10, 5.15") {
                it.contains("fallbackToDestructiveMigration") || it.contains("allowMainThreadQueries")
            }
        )
    }

    @Test
    fun gradleFilesDoNotUseKapt() {
        val gradleFiles = listOf(
            File(appDir, "build.gradle.kts"),
            File(appDir.absoluteFile.parentFile, "build.gradle.kts"),
            File(appDir.absoluteFile.parentFile, "gradle/libs.versions.toml")
        ).filter { it.exists() }
        assertNone(violations(gradleFiles, "1.3") { it.contains("kapt") })
    }

    @Test
    fun manifestHasNoInternetAndNoBackup() {
        val manifest = File(mainDir, "AndroidManifest.xml").readText()
        assertTrue("manifest requests INTERNET (requirements 1.10, 11.4)", !manifest.contains("INTERNET"))
        assertTrue("manifest must set allowBackup=\"false\" (requirement 3.4)", manifest.contains("android:allowBackup=\"false\""))
    }

    @Test
    fun noTemplatePurpleColors() {
        val purples = listOf("6650A4", "625B71", "7D5260", "D0BCFF", "CCC2DC", "EFB8C8")
        assertNone(
            violations(sources(), "6.3") { text -> purples.any { text.contains(it, ignoreCase = true) } }
        )
    }

    @Test
    fun noHardcodedTextLiterals() {
        assertNone(violations(sources(), "6.10") { Regex("""\bText\(\s*"""").containsMatchIn(it) })
    }

    @Test
    fun noGreetingTemplateLeftover() {
        assertNone(violations(sources(), "8.8") { Regex("""\bGreeting\b""").containsMatchIn(it) })
    }

    @Test
    fun eachFeatureHasAScreenViewModelAndUiState() {
        val features = File(srcRoot, "feature").listFiles { f -> f.isDirectory }.orEmpty()
        val problems = features.flatMap { dir ->
            val files = sources(dir).filter { !it.relative().contains("/components/") }
            listOf("Screen", "ViewModel", "UiState").mapNotNull { suffix ->
                val count = files.count { it.nameWithoutExtension.endsWith(suffix) }
                if (count < 1) "${dir.relative()} has no *$suffix file (requirement 2.8)" else null
            }
        }
        assertNone(problems)
    }
}
