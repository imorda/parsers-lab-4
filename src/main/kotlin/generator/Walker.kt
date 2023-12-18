package generator

import antlr.gramBaseVisitor
import antlr.gramParser
import org.antlr.v4.runtime.tree.TerminalNode

class Walker(parser: gramParser) : gramBaseVisitor<String>() {
    var packageName = "" // default empty
    lateinit var rootNode: String // where to start parsing

    val tokens = mutableListOf<Pair<String, Regex>>()
    val ignoredTokens = mutableSetOf<String>()

    val parseRules = mutableMapOf<String, NonTerminalRule>()

    init {
        visitCode(parser.code())
    }

    override fun visitCode(ctx: gramParser.CodeContext): String? {
        packageName = visitPackage(ctx.package_()) // default concatenation
        rootNode = ctx.NAME().text

        visitBody(ctx.body())
        return null
    }

    override fun visitLexRule(ctx: gramParser.LexRuleContext): String? {
        val name = ctx.LEX_NAME().text
        val detector: Regex = when {
            ctx.REGEXP() != null -> ctx.REGEXP().text.trim('/').toRegex()
            else -> Regex.escape(ctx.STRING()!!.text.trim('"')).toRegex()
        }

        tokens.add(name to detector)

        if (ctx.TO_SKIP() != null) {
            ignoredTokens.add(name)
        }

        return null
    }

    override fun visitParseRule(ctx: gramParser.ParseRuleContext): String? {
        val name = ctx.NAME().text
        val args = if (ctx.args() != null) visitArgs(ctx.args()) else null
        val returnType = if (ctx.returnType() != null) visitReturnType(ctx.returnType()) else null

        parseRules.getOrPut(name) {
            NonTerminalRule(
                name,
                returnType,
                args,
                _visitParseRuleBody(ctx.parseRuleBody()),
            )
        }

        return null
    }

    override fun visitReturnType(ctx: gramParser.ReturnTypeContext): String {
        return ctx.CODE().text.trim('`')
    }

    override fun visitArgs(ctx: gramParser.ArgsContext): String {
        return ctx.CODE().text.trim('`')
    }

    private fun _visitParseRuleBody(ctx: gramParser.ParseRuleBodyContext): Set<Rule> =
        ctx.singleRule().map { _visitSingleRule(it) }.toSet()

    private fun _visitSingleRule(ctx: gramParser.SingleRuleContext): Rule =
        Rule(
            if (ctx.returnType() != null) visitReturnType(ctx.returnType()) else null,
            ctx.singleRuleBody().map { _visitSingleRuleBody(it) },
        )

    private fun _visitSingleRuleBody(ctx: gramParser.SingleRuleBodyContext): Item =
        when {
            ctx.LEX_NAME() != null -> Item.Terminal(ctx.LEX_NAME().text)
            ctx.CODE() != null -> Item.Kotlin(ctx.CODE().text.trim('`'))
            else -> _visitNonterminal(ctx.nonterminal())
        }

    private fun _visitNonterminal(ctx: gramParser.NonterminalContext): Item.NonTerminal =
        Item.NonTerminal(ctx.NAME().text, if (ctx.args() != null) { visitArgs(ctx.args()) } else { null })

    override fun visitTerminal(node: TerminalNode): String {
        return node.text
    }

    override fun aggregateResult(aggregate: String?, nextResult: String?): String {
        return (aggregate ?: "") + (nextResult ?: "")
    }
}
