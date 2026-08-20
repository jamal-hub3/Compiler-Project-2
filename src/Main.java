import Rules.HtmlCssLexer;
import Rules.HtmlCssParser;

import AST.builders.HtmlAstBuilder;
import AST.builders.Jinja2AstBuilder;
import AST.html.HtmlNode;
import AST.jinja2.Jinja2TemplateNode;
import AST.python.ProgramNode;
import AST.visitors.AstPrintVisitor;
import AST.visitors.Jinja2PrintVisitor;
import AST.visitors.PythonPrintVisitor;

import generator.Jinja2Generator;
import generator.PythonGenerator;
import generator.LayoutMerger;

import parser.PythonLexer;
import parser.PythonParser;

import semantic.*;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Main {

    private static final String[] TEMPLATES = { "index.jinja", "add_product.jinja", "edit_product.jinja" };

    private static final StringBuilder genLog     = new StringBuilder();
    private static final StringBuilder semReport  = new StringBuilder();
    private static final StringBuilder jsonPython = new StringBuilder();
    private static final StringBuilder jsonJinja  = new StringBuilder();

    public static void main(String[] args) throws Exception {

        String templatesDir = args.length > 0 ? args[0] : "templates";
        String pythonPath   = args.length > 1 ? args[1] : "python_input/app.py";

        printBanner("COMPILER PROJECT 2025/2026 — Code Generation Phase");
        log("Compiler run started. templatesDir=" + templatesDir + "  pythonPath=" + pythonPath);

        printPhase(1, "Python Lexer & Parser -> Python AST", pythonPath);
        ProgramNode pythonAst = parsePython(pythonPath);
        System.out.println("\n---- Python AST Tree ------------------------------");
        PythonPrintVisitor pyPrinter = new PythonPrintVisitor();
        String pyTreeText = capture(() -> pythonAst.accept(pyPrinter));
        System.out.print(pyTreeText);
        jsonPython.append(toJson("python_ast", pyTreeText));

        // ════ PHASE 2: Python Semantic Analysis ════
        printPhase(2, "Python Semantic Analysis", "");
        SymbolTable pythonSymbols = new SymbolTable("Python");
        PythonSemanticAnalyzer pythonAnalyzer = new PythonSemanticAnalyzer(pythonSymbols);
        pythonAst.accept(pythonAnalyzer);
        appendSemanticResults("Python", pythonAnalyzer.getErrors());
        appendSymbolTable(pythonSymbols);
        log("Python semantic analysis done: " + pythonAnalyzer.getErrors().size() + " issue(s).");

        // ════ PHASE 3: Generator يستخرج Context Data من Python AST ════
        printPhase(3, "Generator -> Context Data (from Python array)", "");
        Map<String, Object> context = Jinja2Generator.extractContext(pythonAst);
        System.out.println("Context variables extracted: " + context.keySet());
        log("Context extracted: " + context.keySet());

        // ════ PHASE 4-7: لكل قالب Jinja على حدة ════
        Map<String,String> generatedHtml = new LinkedHashMap<>();
        Map<String,Jinja2TemplateNode> jinjaTrees = new LinkedHashMap<>();

        for (String templateName : TEMPLATES) {
            String htmlPath = templatesDir + "/" + templateName;
            if (!Files.exists(Path.of(htmlPath))) {
                System.out.println("!! Skipping missing template: " + htmlPath);
                log("WARNING: template not found -> " + htmlPath);
                continue;
            }

            printPhase(4, "HTML/CSS/Jinja2 Lexer & Parser (ANTLR4)", htmlPath);
            Jinja2TemplateNode jinja2Ast = buildJinjaAst(htmlPath, templateName);
            jinjaTrees.put(templateName, jinja2Ast);

            printPhase(5, "Jinja2 AST Builder (Second Tree)", templateName);

            Jinja2PrintVisitor jinja2Printer = new Jinja2PrintVisitor();
            String jinjaTreeText = capture(() -> jinja2Ast.accept(jinja2Printer));
            System.out.print(jinjaTreeText);
            jsonJinja.append(toJson("jinja_ast_" + templateName.replaceAll("\\.(jinja|html)$",""), jinjaTreeText));

            printPhase(6, "Jinja2 Semantic Analysis", templateName);
            Set<String> contextVarNames = new HashSet<>(context.keySet());
            SymbolTable jinja2Symbols = new SymbolTable("Jinja2 - " + templateName);
            Jinja2SemanticAnalyzer jinja2Analyzer =
                    new Jinja2SemanticAnalyzer(jinja2Symbols, contextVarNames);
            jinja2Ast.accept(jinja2Analyzer);
            appendSemanticResults("Jinja2 (" + templateName + ")", jinja2Analyzer.getErrors());
            appendSymbolTable(jinja2Symbols);

            printPhase(7, "Code Generation: Context Data -> render_template() -> HTML", templateName);

            Jinja2TemplateNode treeToRender = jinja2Ast;
            String parentName = LayoutMerger.findExtendsTarget(jinja2Ast);
            if (parentName != null) {
                String basePath = templatesDir + "/" + parentName;
                if (Files.exists(Path.of(basePath))) {
                    Jinja2TemplateNode baseAst = buildJinjaAst(basePath, parentName);
                    treeToRender = LayoutMerger.merge(baseAst, jinja2Ast);
                    System.out.println("Resolved extends: " + templateName + " -> " + parentName);
                    log("Resolved extends " + templateName + " against " + parentName);
                } else {
                    System.out.println("!! Base template not found for extends: " + basePath);
                    log("WARNING: base template missing -> " + basePath);
                }
            }

            Jinja2Generator j2Gen = new Jinja2Generator(context);
            String html = j2Gen.generate(treeToRender);
            generatedHtml.put(templateName, html);
            System.out.println(html);
            log("Generated HTML for " + templateName + " (" + html.length() + " chars).");
        }

        printBanner("SAVING OUTPUTS");
        writeGeneratedOutputs(generatedHtml, templatesDir);
        copySupportFiles(pythonPath, templatesDir);
        writeCompilerOutputFiles();

        // ════ SUMMARY ════
        printBanner("COMPILATION COMPLETE");
        System.out.println("Generated pages : output/{" + String.join(", ", TEMPLATES) + "}");
        System.out.println("Support files   : output/app.py, output/style.css, output/script.js (as-is)");
        System.out.println("Analysis files  : compiler_output/ast_python.json, ast_jinja.json,");
        System.out.println("                  semantic_report.txt, generation_log.txt");
    }


    private static void writeGeneratedOutputs(Map<String,String> generatedHtml, String templatesDir) throws IOException {
        Files.createDirectories(Path.of("output"));
        for (Map.Entry<String,String> e : generatedHtml.entrySet()) {
            String outputName = e.getKey().replaceAll("\\.(jinja|html)$", "") + ".html";
            Path out = Path.of("output", outputName);
            Files.writeString(out, e.getValue());
            System.out.println("Saved: " + out);
            log("Saved generated output -> " + out);
        }
    }

    private static void copySupportFiles(String pythonPath, String templatesDir) throws IOException {
        Files.createDirectories(Path.of("output"));

        copyIfExists(Path.of(pythonPath), Path.of("output", "app.py"));

        copyFirstExisting(Path.of("output", "style.css"),
                Path.of("flask_app", "static", "style.css"),
                Path.of("test_inputs", "style.css"));

        copyFirstExisting(Path.of("output", "script.js"),
                Path.of("flask_app", "static", "script.js"),
                Path.of("test_inputs", "script.js"));
    }

    private static void copyIfExists(Path from, Path to) throws IOException {
        if (Files.exists(from)) {
            Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Copied (as-is): " + from + " -> " + to);
            log("Copied support file " + from + " -> " + to);
        } else {
            System.out.println("!! Support file not found, skipped: " + from);
            log("WARNING: support file missing -> " + from);
        }
    }

    private static void copyFirstExisting(Path to, Path... candidates) throws IOException {
        for (Path c : candidates) {
            if (Files.exists(c)) { copyIfExists(c, to); return; }
        }
        System.out.println("!! None of the candidates found for: " + to.getFileName());
        log("WARNING: no candidate found for -> " + to.getFileName());
    }



    private static void writeCompilerOutputFiles() throws IOException {
        Files.createDirectories(Path.of("compiler_output"));

        Files.writeString(Path.of("compiler_output", "ast_python.json"), "{" + jsonPython + "\n}");
        Files.writeString(Path.of("compiler_output", "ast_jinja.json"),  "{" + jsonJinja  + "\n}");
        Files.writeString(Path.of("compiler_output", "semantic_report.txt"), semReport.toString());
        Files.writeString(Path.of("compiler_output", "generation_log.txt"),  genLog.toString());

        System.out.println("Saved: compiler_output/ast_python.json");
        System.out.println("Saved: compiler_output/ast_jinja.json");
        System.out.println("Saved: compiler_output/semantic_report.txt");
        System.out.println("Saved: compiler_output/generation_log.txt");
    }

    private static String toJson(String key, String treeText) {
        String escaped = treeText
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
        return (jsonPython.length() > 0 || jsonJinja.length() > 0 ? ",\n" : "\n")
                + "  \"" + key + "\": \"" + escaped + "\"";
    }

    // ════════════════════════════════════════════════════════════════
    //  Helpers
    // ════════════════════════════════════════════════════════════════

    private static Jinja2TemplateNode buildJinjaAst(String htmlPath, String templateName) throws Exception {
        CharStream cs = CharStreams.fromFileName(htmlPath);
        HtmlCssLexer lexer = new HtmlCssLexer(cs);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        HtmlCssParser parser = new HtmlCssParser(tokens);
        ParseTree parseTree = parser.htmlDocument();

        HtmlAstBuilder htmlBuilder = new HtmlAstBuilder();
        HtmlNode htmlAst = htmlBuilder.visit(parseTree);

        Jinja2AstBuilder jinja2Builder = new Jinja2AstBuilder(templateName);
        return jinja2Builder.build(htmlAst);
    }

    private static ProgramNode parsePython(String path) {
        String src = "";
        try { src = Files.readString(Path.of(path)); }
        catch (IOException e) {
            System.err.println("Warning: Cannot read Python file: " + path);
            log("ERROR: cannot read python file -> " + path);
        }
        PythonLexer lex = new PythonLexer(src);
        PythonParser prs = new PythonParser(lex.tokenize());
        return prs.parseProgram();
    }

    private static void appendSemanticResults(String label, List<SemanticError> errors) {
        if (errors.isEmpty()) {
            semReport.append("[OK] No semantic errors in ").append(label).append("\n\n");
            System.out.println("[OK] No semantic errors in " + label);
            return;
        }
        semReport.append("[").append(label).append("] Semantic Issues (")
                .append(errors.size()).append("):\n");
        System.out.println("\n[" + label + "] Semantic Issues (" + errors.size() + "):");
        for (SemanticError e : errors) {
            String icon = e.getSeverity() == SemanticError.Severity.ERROR ? "[ERROR]" : "[WARN] ";
            String line = String.format("  %s L%d:C%d -- %s", icon, e.getLine(), e.getCol(), e.getMessage());
            semReport.append(line).append("\n");
            System.out.println(line);
        }
        semReport.append("\n");
    }

    private static void appendSymbolTable(SymbolTable table) {
        String text = capture(table::print);
        semReport.append(text).append("\n");
    }

    private static String capture(Runnable action) {
        PrintStream original = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream tee = new PrintStream(new OutputStream() {
            @Override public void write(int b) throws IOException {
                buffer.write(b);
                original.write(b);
            }
        }, true);
        System.setOut(tee);
        try { action.run(); }
        finally { System.setOut(original); }
        return buffer.toString();
    }

    private static void log(String msg) {
        genLog.append("[").append(java.time.LocalTime.now().withNano(0)).append("] ")
              .append(msg).append("\n");
    }

    private static void printBanner(String title) {
        String line = "=".repeat(56);
        System.out.println("\n+" + line + "+");
        System.out.printf("|  %-54s|%n", title);
        System.out.println("+" + line + "+\n");
    }

    private static void printPhase(int num, String name, String detail) {
        System.out.println("\n+----------------------------------------------------");
        System.out.printf("| PHASE %d: %s%n", num, name);
        if (!detail.isEmpty()) System.out.println("|  File: " + detail);
        System.out.println("+----------------------------------------------------");
    }
}
