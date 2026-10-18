plugins {
    id("com.android.application") version "9.4.0" apply false
    // AGP 9 ma wbudowaną obsługę Kotlina; plugin kompilatora Compose jest osobny.
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21" apply false
}
