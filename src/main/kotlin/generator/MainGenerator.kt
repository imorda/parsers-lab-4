package generator

class MainGenerator(private val walker: Walker) : CommonCodeGenerator() {
    fun generateMain() = buildString {
        line("package ${walker.packageName}")
        line()
        line("fun main() {")
        indentBlock {
            line("println(${getClassPrefix(walker.packageName)}Parser(${getClassPrefix(walker.packageName)}Lexer(System.`in`.reader())).${walker.rootNode}())")
        }
        line("}")
    }
}
