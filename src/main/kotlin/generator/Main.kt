package generator

import antlr.gramLexer
import antlr.gramParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.createDirectories

fun main(args: Array<String>) {
    if (args.size < 2 || args.size > 3) {
        System.err.println("Incorrect args!")
        System.err.println("Usage: lab-4 <input grammar path> <output folder>")
        return
    }
    val generateMain = if (args.size == 3 && args[2] == "1") {
        true
    } else {
        false
    }

    val inputFile = File(args[0])
    val outputFolder = Path(args[1])

    val lexer = gramLexer(CharStreams.fromReader(inputFile.bufferedReader()))
    val tokens = CommonTokenStream(lexer)
    val parser = gramParser(tokens)
    val walker = Walker(parser)

    val lexerGen = LexerGenerator(walker)
    val parserGen = ParserGenerator(walker)
    val mainGen = MainGenerator(walker)

    val packageFolder = outputFolder.resolve(walker.packageName.replace(".", "/"))

    packageFolder.createDirectories()

    // Generate lexer
    packageFolder.resolve("${getClassPrefix(walker.packageName)}Lexer.kt").toFile().writeText(lexerGen.generateLexer())
    packageFolder.resolve("Token.kt").toFile().writeText(lexerGen.generateToken())
    packageFolder.resolve("${getClassPrefix(walker.packageName)}Parser.kt").toFile().writeText(parserGen.generateParser())
    packageFolder.resolve("AST.kt").toFile().writeText(parserGen.generateAST())
    if (generateMain) {
        packageFolder.resolve("Main.kt").toFile().writeText(mainGen.generateMain())
    }
}
