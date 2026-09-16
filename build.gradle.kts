// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}

val checkDesignSystemUsage by tasks.registering {
    group = "verification"
    description = "Checks that feature UI uses semantic design-system APIs."

    doLast {
        val forbiddenUsages = linkedMapOf(
            "raw ARGB color" to Regex("""Color\s*\(\s*0x"""),
            "legacy color palette" to Regex("""designsystem\.theme\.(GameColors|MaterialColors)"""),
            "direct MaterialTheme access" to Regex("""import\s+androidx\.compose\.material3\.MaterialTheme"""),
            "local rounded shape" to Regex("""import\s+androidx\.compose\.foundation\.shape\.RoundedCornerShape"""),
            "local font weight" to Regex("""import\s+androidx\.compose\.ui\.text\.font\.FontWeight"""),
        )
        val violations = mutableListOf<String>()

        val uiSources = listOf("app", "feature")
            .flatMap { directory ->
                fileTree(rootDir.resolve(directory)) {
                    include("**/src/main/**/*.kt")
                }.files
            }

        uiSources.sorted().forEach { source ->
            source.readLines().forEachIndexed { index, line ->
                forbiddenUsages.forEach { (label, pattern) ->
                    if (pattern.containsMatchIn(line)) {
                        val path = source.relativeTo(rootDir).invariantSeparatorsPath
                        violations += "$path:${index + 1}: $label"
                    }
                }
            }
        }

        check(violations.isEmpty()) {
            "Feature UI bypasses the design system:\n${violations.joinToString("\n")}"
        }
    }
}

tasks.register("check") {
    group = "verification"
    dependsOn(checkDesignSystemUsage)
}

