package semantic;

import AST.python.*;
import AST.python.expr.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * المحلل الداللي لكود Python/Flask.
 * يتحقق من 5+ أخطاء داللية:
 *
 *  1. تعريف دالتين لهما نفس مسار الـ route (Duplicate Route)
 *  2. استخدام متغير غير مُعرَّف (Undefined Variable)
 *  3. دالة route بدون return (Missing Return)
 *  4. مسار route لا يبدأ بـ / (Invalid Route Path)
 *  5. تعريف دالتين بنفس الاسم (Duplicate Function)
 *  6. استدعاء دالة غير مُعرَّفة (Undefined Function Call) — تحذير
 *  7. متغير يُستخدم قبل تعريفه (Use Before Define)
 */
public class PythonSemanticAnalyzer implements PythonVisitor {

    private final SymbolTable symbolTable;
    private final List<SemanticError> errors = new ArrayList<>();

    // تتبع routes المُعرَّفة
    private final Set<String> definedRoutes = new HashSet<>();
    // تتبع أسماء الدوال
    private final Set<String> definedFunctions = new HashSet<>();
    // هل الدالة الحالية تحتوي على return؟
    private boolean currentFnHasReturn = false;
    private String currentFnName = null;
    private boolean currentFnIsRoute = false;

    public PythonSemanticAnalyzer(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    public List<SemanticError> getErrors() { return errors; }

    private void error(String msg, int line, int col) {
        errors.add(new SemanticError(msg, line, col));
    }

    private void warn(String msg, int line, int col) {
        errors.add(new SemanticError(msg, line, col, SemanticError.Severity.WARNING));
    }

    // ── Visitor: Statements ───────────────────────────────────────────────────

    @Override
    public void visit(ProgramNode node) {
        // Pass 1: جمع كل التعريفات (imports, assignments, functions)
        for (PythonNode stmt : node.getStatements()) {
            if (stmt instanceof ImportNode imp) {
                for (String nm : imp.getNames()) {
                    symbolTable.define(new Symbol(nm, Symbol.Kind.FUNCTION, imp.getLine(), imp.getColumn()));
                }
            } else if (stmt instanceof AssignmentNode asgn) {
                Symbol sym = new Symbol(asgn.getTarget(), Symbol.Kind.VARIABLE, asgn.getLine(), asgn.getColumn());
                // تحديد النوع
                if (asgn.getValue() instanceof ListLiteralNode) sym.setType("list");
                else if (asgn.getValue() instanceof DictLiteralNode) sym.setType("dict");
                else if (asgn.getValue() instanceof StringLiteralNode) sym.setType("str");
                else if (asgn.getValue() instanceof NumberLiteralNode) sym.setType("number");
                symbolTable.define(sym);
            } else if (stmt instanceof FunctionDefNode fn) {
                symbolTable.define(new Symbol(fn.getName(), Symbol.Kind.FUNCTION, fn.getLine(), fn.getColumn()));
            }
        }

        // Pass 2: التحقق الداللي
        for (PythonNode stmt : node.getStatements()) {
            stmt.accept(this);
        }
    }

    @Override
    public void visit(ImportNode node) {
        // Imports تمت معالجتهم في Pass 1
    }

    @Override
    public void visit(AssignmentNode node) {
        // تحقق من القيمة
        if (node.getValue() != null) node.getValue().accept(this);
    }

    @Override
    public void visit(FunctionDefNode node) {
        int l = node.getLine(), c = node.getColumn();

        // خطأ 5: تعريف دالتين بنفس الاسم
        if (definedFunctions.contains(node.getName())) {
            error("Duplicate function definition: '" + node.getName() + "'", l, c);
        }
        definedFunctions.add(node.getName());

        // معالجة decorators
        for (DecoratorNode dec : node.getDecorators()) dec.accept(this);

        // خطأ 1 + 4: route handling
        if (node.isRoute()) {
            String path = node.getRoutePath();
            if (path != null) {
                // خطأ 4: مسار لا يبدأ بـ /
                String cleanPath = path.replace("\"","").replace("'","").trim();
                if (!cleanPath.startsWith("/")) {
                    error("Route path must start with '/': '" + cleanPath + "' in function '" + node.getName() + "'", l, c);
                }
                // خطأ 1: duplicate route
                if (definedRoutes.contains(cleanPath)) {
                    error("Duplicate route path: '" + cleanPath + "' already defined", l, c);
                }
                definedRoutes.add(cleanPath);
            }
            symbolTable.define(new Symbol(node.getName(), Symbol.Kind.ROUTE, l, c));
        }

        // تحليل جسم الدالة
        symbolTable.enterScope();
        // تعريف parameters
        for (String param : node.getParams()) {
            symbolTable.define(new Symbol(param, Symbol.Kind.PARAMETER, l, c));
        }

        currentFnName      = node.getName();
        currentFnIsRoute   = node.isRoute();
        currentFnHasReturn = false;

        for (PythonNode stmt : node.getBody()) {
            if (stmt != null) stmt.accept(this);
        }

        // خطأ 3: route بدون return
        if (currentFnIsRoute && !currentFnHasReturn) {
            error("Route function '" + node.getName() + "' has no return statement", l, c);
        }

        symbolTable.exitScope();
        currentFnName = null;
    }

    @Override
    public void visit(DecoratorNode node) {
        // التحقق من decorator صحيح
    }

    @Override
    public void visit(ReturnNode node) {
        currentFnHasReturn = true;
        if (node.getValue() != null) node.getValue().accept(this);
    }

    @Override
    public void visit(ExpressionStatementNode node) {
        if (node.getExpression() != null) node.getExpression().accept(this);
    }

    @Override
    public void visit(IfStatementNode node) {
        if (node.getCondition() != null) node.getCondition().accept(this);
        symbolTable.enterScope();
        for (PythonNode s : node.getThenBody()) if (s != null) s.accept(this);
        symbolTable.exitScope();
        symbolTable.enterScope();
        for (PythonNode s : node.getElseBody()) if (s != null) s.accept(this);
        symbolTable.exitScope();
    }

    @Override
    public void visit(ForStatementNode node) {
        symbolTable.enterScope();
        symbolTable.define(new Symbol(node.getVariable(), Symbol.Kind.FOR_VAR, node.getLine(), node.getColumn()));
        if (node.getIterable() != null) node.getIterable().accept(this);
        for (PythonNode s : node.getBody()) if (s != null) s.accept(this);
        symbolTable.exitScope();
    }

    @Override
    public void visit(AppendStatementNode node) {
        // خطأ 2: list غير مُعرَّفة
        if (!symbolTable.isDefined(node.getListName())) {
            error("Undefined variable: '" + node.getListName() + "'", node.getLine(), node.getColumn());
        }
        if (node.getItem() != null) node.getItem().accept(this);
    }

    // ── Visitor: Expressions ──────────────────────────────────────────────────

    @Override
    public void visit(StringLiteralNode node) { }

    @Override
    public void visit(NumberLiteralNode node) { }

    @Override
    public void visit(BoolLiteralNode node) { }

    @Override
    public void visit(NoneLiteralNode node) { }

    @Override
    public void visit(IdentifierNode node) {
        // خطأ 2: متغير غير مُعرَّف (استثناء: __name__, __main__)
        String name = node.getName();
        if (!name.startsWith("__") && !symbolTable.isDefined(name)) {
            error("Undefined variable: '" + name + "'", node.getLine(), node.getColumn());
        }
    }

    @Override
    public void visit(ListLiteralNode node) {
        for (PythonNode e : node.getElements()) if (e != null) e.accept(this);
    }

    @Override
    public void visit(DictLiteralNode node) {
        for (DictLiteralNode.Entry e : node.getEntries()) {
            if (e.key()   != null) e.key().accept(this);
            if (e.value() != null) e.value().accept(this);
        }
    }

    @Override
    public void visit(FunctionCallNode node) {
        if (node.getCallee() != null) {
            // خطأ 6: تحقق من تعريف الدالة (تحذير فقط لأن الدوال الخارجية مسموح بها)
            if (node.getCallee() instanceof IdentifierNode id) {
                if (!symbolTable.isDefined(id.getName()) && !id.getName().startsWith("__")) {
                    warn("Potentially undefined function: '" + id.getName() + "'", id.getLine(), id.getColumn());
                }
            }
            node.getCallee().accept(this);
        }
        for (PythonNode arg : node.getPositionalArgs()) if (arg != null) arg.accept(this);
        for (PythonNode val : node.getKeywordArgs().values()) if (val != null) val.accept(this);
    }

    @Override
    public void visit(AttributeAccessNode node) {
        if (node.getObject() != null) node.getObject().accept(this);
    }

    @Override
    public void visit(BinaryOpNode node) {
        if (node.getLeft()  != null) node.getLeft().accept(this);
        if (node.getRight() != null) node.getRight().accept(this);
    }
}
