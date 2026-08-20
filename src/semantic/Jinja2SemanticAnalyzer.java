package semantic;

import AST.jinja2.*;

import java.util.*;

/**
 * المحلل الداللي لقوالب Jinja2.
 * يتحقق من 5+ أخطاء داللية:
 *
 *  1. متغير Jinja2 غير مُعرَّف في السياق — {{ var }} (Undefined Variable)          [ERROR]
 *  2. متغير غير مُعرَّف داخل خاصية HTML — href="{{ var }}"   (Undefined In Attr)  [ERROR]
 *  3. block name مُكرَّر في نفس القالب                        (Duplicate Block)   [ERROR]
 *  4. iterable غير مُعرَّف في for loop                        (Undefined Iterable)[ERROR]
 *  5. extends داخل block أو for loop                          (Invalid Position)  [ERROR]
 *  6. for loop يُكرِّر اسم متغير خارجي                         (Shadowing)         [WARNING]
 */
public class Jinja2SemanticAnalyzer implements Jinja2Visitor {

    private final SymbolTable symbolTable;
    private final List<SemanticError> errors = new ArrayList<>();

    // تتبع حالة التحليل
    private boolean extendsFound = false;
    private boolean firstNodeProcessed = false;
    private final Set<String> definedBlocks = new HashSet<>();
    private final Deque<String> forVarStack  = new ArrayDeque<>();
    // السياق الخارجي: المتغيرات التي مرّرها Python (من Python AST)
    private final Set<String> contextVars;

    public Jinja2SemanticAnalyzer(SymbolTable symbolTable, Set<String> contextVars) {
        this.symbolTable = symbolTable;
        this.contextVars = new HashSet<>(contextVars);
        // إضافة متغيرات Jinja2 الأساسية المتاحة دائماً
        this.contextVars.add("loop");
        this.contextVars.add("self");
        this.contextVars.add("super");
        this.contextVars.add("request");
        this.contextVars.add("config");
        this.contextVars.add("url_for");
        this.contextVars.add("get_flashed_messages");
    }

    public List<SemanticError> getErrors() { return errors; }

    private void error(String msg, int line, int col) {
        errors.add(new SemanticError(msg, line, col));
    }

    private void warn(String msg, int line, int col) {
        errors.add(new SemanticError(msg, line, col, SemanticError.Severity.WARNING));
    }

    // ── Visitor ───────────────────────────────────────────────────────────────

    @Override
    public void visit(Jinja2TemplateNode node) {
        firstNodeProcessed = false;
        for (Jinja2Node child : node.getChildren()) {
            child.accept(this);
            firstNodeProcessed = true;
        }
    }

    @Override
    public void visit(Jinja2ExtendsNode node) {
        // خطأ 2: extends يجب أن يكون أولاً
        if (firstNodeProcessed && !node.getParentTemplate().isEmpty()) {
            // Only flag if there were meaningful nodes before (not just raw whitespace)
        }
        // خطأ 7: extends داخل block
        if (!forVarStack.isEmpty()) {
            error("'extends' tag cannot be used inside a block or for loop", node.getLine(), node.getColumn());
        }
        extendsFound = true;
        symbolTable.define(new Symbol("__extends__", Symbol.Kind.TEMPLATE, node.getLine(), node.getColumn()));
    }

    @Override
    public void visit(Jinja2BlockNode node) {
        // خطأ 3: block مُكرَّر
        if (definedBlocks.contains(node.getBlockName())) {
            error("Duplicate block name: '" + node.getBlockName() + "'", node.getLine(), node.getColumn());
        }
        definedBlocks.add(node.getBlockName());
        symbolTable.define(new Symbol(node.getBlockName(), Symbol.Kind.BLOCK, node.getLine(), node.getColumn()));

        symbolTable.enterScope();
        for (Jinja2Node child : node.getBody()) child.accept(this);
        symbolTable.exitScope();
    }

    @Override
    public void visit(Jinja2ForNode node) {
        int l = node.getLine(), c = node.getColumn();

        // تحقق من تعريف iterable
        String iter = node.getIterable().trim();
        // المتغير الأساسي (قبل أي dot)
        String baseIter = iter.split("[.\\[|]")[0].trim();
        if (!contextVars.contains(baseIter) && !symbolTable.isDefined(baseIter)
                && !forVarStack.contains(baseIter)) {
            error("Undefined iterable in for loop: '" + iter + "'", l, c);
        }

        // خطأ 4: إعادة تعريف متغير خارجي (shadowing)
        String loopVar = node.getVariable();
        if (contextVars.contains(loopVar)) {
            warn("For-loop variable '" + loopVar + "' shadows a context variable", l, c);
        }

        symbolTable.enterScope();
        symbolTable.define(new Symbol(loopVar, Symbol.Kind.FOR_VAR, l, c));
        forVarStack.push(loopVar);
        contextVars.add(loopVar);

        for (Jinja2Node child : node.getBody()) child.accept(this);

        forVarStack.pop();
        contextVars.remove(loopVar);
        symbolTable.exitScope();
    }

    @Override
    public void visit(Jinja2IfNode node) {
        // تحقق من متغيرات الشرط
        checkExpression(node.getCondition(), node.getLine(), node.getColumn());

        symbolTable.enterScope();
        for (Jinja2Node child : node.getThenBody()) child.accept(this);
        symbolTable.exitScope();

        symbolTable.enterScope();
        for (Jinja2Node child : node.getElseBody()) child.accept(this);
        symbolTable.exitScope();
    }

    @Override
    public void visit(Jinja2VariableNode node) {
        // خطأ 1: متغير غير مُعرَّف
        checkExpression(node.getExpression(), node.getLine(), node.getColumn());
    }

    @Override
    public void visit(Jinja2RawTextNode node) { /* نص خام — لا تحقق */ }

    @Override
    public void visit(Jinja2HtmlElementNode node) {
        // خطأ 8: متغير غير معرَّف داخل خاصية HTML مثل href="{{ product.name }}"
        for (Jinja2AttributeNode attr : node.getAttributes()) {
            if (attr.isJinjaExpr() && !attr.getValue().trim().startsWith("url_for(")) {
                checkExpression(attr.getValue(), node.getLine(), node.getColumn());
            }
        }
        for (Jinja2Node child : node.getChildren()) child.accept(this);
    }

    @Override
    public void visit(Jinja2MacroNode node) {
        symbolTable.define(new Symbol(node.getName(), Symbol.Kind.FUNCTION, node.getLine(), node.getColumn()));
        symbolTable.enterScope();
        for (String p : node.getParams()) {
            symbolTable.define(new Symbol(p.trim(), Symbol.Kind.PARAMETER, node.getLine(), node.getColumn()));
        }
        for (Jinja2Node child : node.getBody()) child.accept(this);
        symbolTable.exitScope();
    }

    // ── Expression Checker ────────────────────────────────────────────────────

    /**
     * يتحقق من المتغيرات في تعبير Jinja2 مثل: product.name, products, url_for(...)
     */
    private void checkExpression(String expr, int line, int col) {
        if (expr == null || expr.isBlank()) return;

        // تجاهل تعابير بها أرقام أو strings فقط
        String cleaned = expr.trim();
        if (cleaned.startsWith("\"") || cleaned.startsWith("'")) return;
        if (cleaned.matches("\\d+.*")) return;

        // استخراج الاسم الأساسي (قبل أي . أو [ أو | أو مسافة)
        String base = cleaned.split("[.\\[|(\\s]")[0].trim();
        if (base.isEmpty() || base.equals("not") || base.equals("and")
                || base.equals("or") || base.equals("true") || base.equals("false")
                || base.equals("none") || base.equals("True") || base.equals("False")
                || base.equals("None")) return;

        // تجاهل الأرقام
        if (base.matches("\\d+.*")) return;

        if (!contextVars.contains(base) && !symbolTable.isDefined(base)) {
            error("Undefined variable in template: '" + base + "'", line, col);
        }
    }
}
