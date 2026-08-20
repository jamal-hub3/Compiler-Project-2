package AST.visitors;

import AST.python.*;
import AST.python.expr.*;

/**
 * Visitor يطبع شجرة Python AST بشكل مقروء ومحاذٍ.
 * كل عقدة تُطبع مع: الاسم + رقم السطر + رقم العمود + الأبناء.
 */
public class PythonPrintVisitor implements PythonVisitor {

    private int indent = 0;

    private void ind() { System.out.print("  ".repeat(Math.max(0, indent))); }

    private void printNode(PythonNode n, String extra) {
        ind();
        System.out.println("[" + n.getNodeName() + "] (L" + n.getLine()
                + ":C" + n.getColumn() + ") " + extra);
    }

    // ── Statements ────────────────────────────────────────────────────────────

    @Override
    public void visit(ProgramNode node) {
        System.out.println("╔═══════════════════════════════════════════╗");
        System.out.println("║         PYTHON AST — Program Root         ║");
        System.out.println("╚═══════════════════════════════════════════╝");
        indent++;
        for (PythonNode s : node.getStatements()) if (s != null) s.accept(this);
        indent--;
    }

    @Override
    public void visit(ImportNode node) {
        String kind = node.isFrom() ? "from " + node.getModule() + " import" : "import " + node.getModule();
        printNode(node, kind + " " + String.join(", ", node.getNames()));
    }

    @Override
    public void visit(AssignmentNode node) {
        printNode(node, "target='" + node.getTarget() + "'");
        indent++;
        if (node.getValue() != null) node.getValue().accept(this);
        indent--;
    }

    @Override
    public void visit(FunctionDefNode node) {
        printNode(node, "name='" + node.getName() + "' params=" + node.getParams());
        indent++;
        for (DecoratorNode d : node.getDecorators()) d.accept(this);
        ind(); System.out.println("── Body ──");
        indent++;
        for (PythonNode s : node.getBody()) if (s != null) s.accept(this);
        indent -= 2;
    }

    @Override
    public void visit(DecoratorNode node) {
        printNode(node, "@" + node.getName() + "(" + node.getArgument() + ")");
    }

    @Override
    public void visit(ReturnNode node) {
        printNode(node, "");
        indent++;
        if (node.getValue() != null) node.getValue().accept(this);
        indent--;
    }

    @Override
    public void visit(ExpressionStatementNode node) {
        printNode(node, "");
        indent++;
        if (node.getExpression() != null) node.getExpression().accept(this);
        indent--;
    }

    @Override
    public void visit(IfStatementNode node) {
        printNode(node, "");
        indent++;
        ind(); System.out.println("Condition:");
        indent++;
        if (node.getCondition() != null) node.getCondition().accept(this);
        indent--;
        if (!node.getThenBody().isEmpty()) {
            ind(); System.out.println("Then:");
            indent++;
            for (PythonNode s : node.getThenBody()) if (s != null) s.accept(this);
            indent--;
        }
        if (!node.getElseBody().isEmpty()) {
            ind(); System.out.println("Else:");
            indent++;
            for (PythonNode s : node.getElseBody()) if (s != null) s.accept(this);
            indent--;
        }
        indent--;
    }

    @Override
    public void visit(ForStatementNode node) {
        printNode(node, "var='" + node.getVariable() + "' in:");
        indent++;
        if (node.getIterable() != null) node.getIterable().accept(this);
        ind(); System.out.println("Body:");
        indent++;
        for (PythonNode s : node.getBody()) if (s != null) s.accept(this);
        indent -= 2;
    }

    @Override
    public void visit(AppendStatementNode node) {
        printNode(node, "list='" + node.getListName() + "'");
        indent++;
        if (node.getItem() != null) node.getItem().accept(this);
        indent--;
    }

    // ── Expressions ───────────────────────────────────────────────────────────

    @Override
    public void visit(StringLiteralNode node) {
        printNode(node, "\"" + node.getValue() + "\"");
    }

    @Override
    public void visit(NumberLiteralNode node) {
        printNode(node, String.valueOf(node.getValue()));
    }

    @Override
    public void visit(BoolLiteralNode node) {
        printNode(node, String.valueOf(node.getValue()));
    }

    @Override
    public void visit(NoneLiteralNode node) {
        printNode(node, "None");
    }

    @Override
    public void visit(IdentifierNode node) {
        printNode(node, "'" + node.getName() + "'");
    }

    @Override
    public void visit(ListLiteralNode node) {
        printNode(node, "[" + node.getElements().size() + " elements]");
        indent++;
        for (PythonNode e : node.getElements()) if (e != null) e.accept(this);
        indent--;
    }

    @Override
    public void visit(DictLiteralNode node) {
        printNode(node, "{" + node.getEntries().size() + " entries}");
        indent++;
        for (DictLiteralNode.Entry e : node.getEntries()) {
            ind(); System.out.println("Entry:");
            indent++;
            if (e.key()   != null) e.key().accept(this);
            if (e.value() != null) e.value().accept(this);
            indent--;
        }
        indent--;
    }

    @Override
    public void visit(FunctionCallNode node) {
        printNode(node, "");
        indent++;
        ind(); System.out.println("Callee:");
        indent++;
        if (node.getCallee() != null) node.getCallee().accept(this);
        indent--;
        if (!node.getPositionalArgs().isEmpty()) {
            ind(); System.out.println("Args:");
            indent++;
            for (PythonNode a : node.getPositionalArgs()) if (a != null) a.accept(this);
            indent--;
        }
        if (!node.getKeywordArgs().isEmpty()) {
            ind(); System.out.println("KwArgs:");
            indent++;
            for (var entry : node.getKeywordArgs().entrySet()) {
                ind(); System.out.println(entry.getKey() + "=");
                indent++;
                if (entry.getValue() != null) entry.getValue().accept(this);
                indent--;
            }
            indent--;
        }
        indent--;
    }

    @Override
    public void visit(AttributeAccessNode node) {
        printNode(node, "." + node.getAttribute());
        indent++;
        if (node.getObject() != null) node.getObject().accept(this);
        indent--;
    }

    @Override
    public void visit(BinaryOpNode node) {
        printNode(node, "op='" + node.getOperator() + "'");
        indent++;
        if (node.getLeft()  != null) node.getLeft().accept(this);
        if (node.getRight() != null) node.getRight().accept(this);
        indent--;
    }
}
