package generator

class ParserGenerator(private val walker: Walker) : CommonCodeGenerator() {

    private val first = mutableMapOf<NonTerminalRule, MutableSet<String?>>() // Rule -> Set(tokens)
    private val follow = mutableMapOf<NonTerminalRule, MutableSet<String>>() // NonTerminal -> Set(tokens)

    init {
        evalFirst()
        evalFollow()
    }

    private fun evalFirst() {
        var change = true
        while (change) {
            change = false
            walker.parseRules.forEach { (_, nonTerm) ->
                val row = first.getOrPut(nonTerm) { mutableSetOf() }
                nonTerm.alternatives.forEach { rule ->
                    change = row.addAll(firstFun(rule.body)) || change
                }
            }
        }
    }

    private fun firstFun(
        rule: List<Item>,
    ): Set<String?> {
        val row = mutableSetOf<String?>()
        if (rule.isEmpty()) {
            row.add(null)
            return row
        } else {
            for (item in rule) {
                when (item) {
                    is Item.Kotlin -> continue
                    is Item.Terminal -> {
                        row.add(item.tokenType)
                        return row
                    }

                    is Item.NonTerminal -> {
                        val firstA =
                            first.getOrPut(walker.parseRules.getValue(item.name)) { mutableSetOf() }
                        if (null in firstA) {
                            row.addAll(firstA - null)
                            continue
                        } else {
                            row.addAll(firstA)
                            return row
                        }
                    }
                }
            }
            row.add(null)
            return row
        }
    }

    private fun evalFollow() {
        follow.getOrPut(walker.parseRules.getValue(walker.rootNode)) { mutableSetOf("EOF") }

        var change = true
        while (change) {
            change = false
            walker.parseRules.forEach { (_, nonTerm) ->
                val row = follow.getOrPut(nonTerm) { mutableSetOf() }
                nonTerm.alternatives.forEach { rule ->
                    for (i in (0..<rule.body.size)) {
                        val item = rule.body[i]
                        if (item is Item.NonTerminal) {
                            val followB = follow.getOrPut(walker.parseRules.getValue(item.name)) { mutableSetOf() }
                            val firstGamma = firstFun(rule.body.subList(i + 1, rule.body.size))

                            change = followB.addAll(firstGamma.mapNotNull { it }) || change
                            if (null in firstGamma) {
                                change = followB.addAll(row) || change
                            }
                        }
                    }
                }
            }
        }
    }

    fun generateParser() = buildString {
        line("package ${walker.packageName}")
        line()
        line("import common.ParseException")
        line("import kotlin.reflect.KClass")
        line()
        line("class ${getClassPrefix(walker.packageName)}Parser(lexer: Iterable<Token>) {")
        indentBlock {
            line("private val tokenIterator = lexer.iterator()")
            line("private var curToken = tokenIterator.next()")
            line()
            line("private fun expect(template: KClass<out Token>? = null): Token {")
            indentBlock {
                line("val lastToken = curToken")
                line("if (template == null || template == lastToken::class) {")
                indentBlock {
                    line("curToken = tokenIterator.next()")
                    line("return lastToken")
                }
                line("}")
                line("throw ParseException(\"Expected \${template.simpleName} got \${lastToken::class.simpleName}\")")
            }
            line("}")
            walker.parseRules.forEach { _, nonTerminal ->
                line()
                line("fun ${nonTerminal.name}(${nonTerminal.inputAttr ?: ""}): ${nonTerminal.returnType ?: "AST"} = when (curToken) {")
                indentBlock {
                    val allTerms = mutableSetOf<String>()
                    nonTerminal.alternatives.forEach { rule ->
                        val fst = firstFun(rule.body)
                        val expectList: Set<String> = if (null in fst) {
                            follow.getValue(nonTerminal)
                        } else {
                            fst.mapNotNull { it }.toSet()
                        }

                        allTerms.addAll(expectList)
                        line(expectList.map { "is Token.$it" }.joinToString(", ") + " -> {")
                        indentBlock {
                            var termCnt = 1
                            var nonTermCnt = 1
                            rule.body.forEach {
                                when (it) {
                                    is Item.Kotlin -> line("val " + it.body)
                                    is Item.Terminal -> line("val term${termCnt++} = expect(Token.${it.tokenType}::class).text")
                                    is Item.NonTerminal -> line("val nonTerm${nonTermCnt++} = ${it.name}(${it.args ?: ""})")
                                }
                            }
                            if (rule.returnAttr != null) {
                                line(rule.returnAttr)
                            } else {
                                termCnt = 1
                                nonTermCnt = 1
                                val returnType = mutableListOf<String>()
                                rule.body.forEach {
                                    when (it) {
                                        is Item.Terminal -> returnType.add("AST(term${termCnt++})")
                                        is Item.NonTerminal -> returnType.add("nonTerm${nonTermCnt++}")
                                        else -> {}
                                    }
                                }
                                line("AST(\"${nonTerminal.name}\", ${returnType.joinToString(", ")})")
                            }
                        }
                        line("}")
                    }

                    line("else -> throw ParseException(\"Expected ${allTerms.joinToString(" or ")}\")")
                }
                line("}")
            }
        }
        line("}")
        line()
    }

    fun generateAST() = buildString {
        line("package ${walker.packageName}")
        line()
        line("class AST(val node: String, vararg val children: AST) {")
        indentBlock {
            line("override fun toString(): String = \"\${node}(\${children.joinToString(\", \")})\"")
        }
        line("}")
        line()
    }
}
