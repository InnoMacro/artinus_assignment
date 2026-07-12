package org.artinus.backend.architecture

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile

class PackagePathConventionTest {
    @Test
    fun `Kotlin source 경로와 package 선언은 일치한다`() {
        val mismatches =
            SOURCE_ROOTS.flatMap { sourceRoot ->
                findPackageMismatches(Path.of(sourceRoot))
            }

        assertTrue(
            mismatches.isEmpty(),
            "source 경로와 package 선언이 일치하지 않습니다:\n${mismatches.joinToString("\n")}",
        )
    }

    private fun findPackageMismatches(sourceRoot: Path): List<String> {
        if (!Files.exists(sourceRoot)) {
            return emptyList()
        }

        return Files.walk(sourceRoot).use { paths ->
            paths
                .filter { path -> path.isRegularFile() && path.extension == "kt" }
                .map { path -> packageMismatch(sourceRoot, path) }
                .filter { mismatch -> mismatch != null }
                .map { mismatch -> requireNotNull(mismatch) }
                .toList()
        }
    }

    private fun packageMismatch(
        sourceRoot: Path,
        sourceFile: Path,
    ): String? {
        val declaredPackage =
            Files.readAllLines(sourceFile)
                .firstNotNullOfOrNull(PACKAGE_DECLARATION::matchEntire)
                ?.groupValues
                ?.get(1)
                ?: return "${sourceFile.toAbsolutePath()}: package 선언 없음"
        val relativeParent = sourceRoot.relativize(requireNotNull(sourceFile.parent))
        val expectedPackage = relativeParent.joinToString(".") { segment -> segment.toString() }

        return if (declaredPackage == expectedPackage) {
            null
        } else {
            "${sourceFile.toAbsolutePath()}: expected=$expectedPackage, actual=$declaredPackage"
        }
    }

    companion object {
        private val SOURCE_ROOTS = listOf("src/main/kotlin", "src/test/kotlin")
        private val PACKAGE_DECLARATION = Regex("""\s*package\s+([A-Za-z_][A-Za-z0-9_.]*)\s*""")
    }
}
