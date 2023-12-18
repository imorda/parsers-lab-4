package generator

const val INDENT_STEP = 4

internal fun getClassPrefix(packageName: String): String =
    packageName.split(".").last()

open class CommonCodeGenerator() {
    private var indent = 0

    protected fun String.escape() = this
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\\", "\\\\")
        .replace("'", "\\'")
        .replace("\"", "\\\"")

    protected fun StringBuilder.line(line: String = "") {
        append(" ".repeat(indent * INDENT_STEP))
        appendLine(line)
    }

    protected fun StringBuilder.indentBlock(block: StringBuilder.() -> Unit) {
        indent++
        this.block()
        indent--
    }
}
