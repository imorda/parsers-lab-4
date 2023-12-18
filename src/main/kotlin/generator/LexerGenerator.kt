package generator

class LexerGenerator(private val walker: Walker) : CommonCodeGenerator() {
    fun generateLexer() = buildString {
        line("package ${walker.packageName}")
        line()
        line("import common.ParseException")
        line("import kotlinx.coroutines.flow.flow")
        line("import kotlinx.coroutines.flow.toList")
        line("import kotlinx.coroutines.runBlocking")
        line("import java.io.Reader")
        line("import kotlin.reflect.KClass")
        line()
        line("class ${getClassPrefix(walker.packageName)}Lexer(reader: Reader) : Iterable<Token> {")
        indentBlock {
            line("private val text = reader.readText()")
            line("private val tokens = listOf<Pair<Regex, (String) -> Token>>(")
            indentBlock {
                walker.tokens.forEach {
                    line("\"^${it.second.toString().escape()}\".toRegex() to { Token.${it.first}(it) },")
                }
            }
            line(")")
            line("private val ignored = setOf<KClass<out Token>>(")
            indentBlock {
                walker.ignoredTokens.forEach {
                    line("Token.$it::class,")
                }
            }
            line(")")
            line()
            line("fun tokenFlow() = flow {")
            indentBlock {
                line("var curTextPos = 0")
                line("while (curTextPos < text.length) {")
                indentBlock {
                    line("var found = false")
                    line("for ((regexp, factory) in tokens) {")
                    indentBlock {
                        line("val match = regexp.find(text.subSequence(curTextPos, text.length))")
                        line("if (match != null) {")
                        indentBlock {
                            line("val result = factory(match.value)")
                            line("curTextPos += match.value.length")
                            line("found = true")
                            line("if (result::class !in ignored) {")
                            indentBlock {
                                line("emit(result)")
                                line("break")
                            }
                            line("}")
                        }
                        line("}")
                    }
                    line("}")
                    line("if (!found) {")
                    indentBlock {
                        line("throw ParseException(\"No parse!\")")
                    }
                    line("}")
                }
                line("}")
                line("emit(Token.EOF)")
            }
            line("}")
            line()
            line("override fun iterator(): Iterator<Token> =")
            indentBlock {
                line("runBlocking { tokenFlow().toList().iterator() }")
            }
        }
        line("}")
        line()
    }

    fun generateToken() = buildString {
        line("package ${walker.packageName}")
        line()
        line("sealed class Token(val text: String) {")
        indentBlock {
            walker.tokens.forEach {
                line("class ${it.first}(text: String) : Token(text)")
            }
            line("data object EOF : Token(\"\$\")")
        }
        line("}")
        line()
    }
}
