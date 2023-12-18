package generator

data class NonTerminalRule(
    val name: String,
    val returnType: String?,
    val inputAttr: String?,
    val alternatives: Set<Rule>,
)

data class Rule(
    val returnAttr: String?,
    val body: List<Item>,
)

sealed interface Item {
    data class NonTerminal(val name: String, val args: String?) : Item
    data class Terminal(val tokenType: String) : Item
    data class Kotlin(val body: String) : Item
}
