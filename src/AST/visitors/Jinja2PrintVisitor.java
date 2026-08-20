package AST.visitors;

import AST.jinja2.*;

/**
 * Visitor يطبع شجرة Jinja2 AST بشكل مقروء ومحاذٍ.
 */
public class Jinja2PrintVisitor implements Jinja2Visitor {

    private int indent = 0;

    private void ind() { System.out.print("  ".repeat(Math.max(0, indent))); }

    private void printNode(Jinja2Node n, String extra) {
        ind();
        System.out.println("[" + n.getNodeName() + "] (L" + n.getLine()
                + ":C" + n.getColumn() + ") " + extra);
    }

    @Override
    public void visit(Jinja2TemplateNode node) {
        System.out.println("╔═══════════════════════════════════════════╗");
        System.out.println("║    JINJA2 AST — Template: " + padRight(node.getTemplateName(), 16) + " ║");
        System.out.println("╚═══════════════════════════════════════════╝");
        indent++;
        for (Jinja2Node ch : node.getChildren()) ch.accept(this);
        indent--;
    }

    @Override
    public void visit(Jinja2ExtendsNode node) {
        printNode(node, "parent=\"" + node.getParentTemplate() + "\"");
    }

    @Override
    public void visit(Jinja2BlockNode node) {
        printNode(node, "name=\"" + node.getBlockName() + "\"");
        indent++;
        for (Jinja2Node ch : node.getBody()) ch.accept(this);
        indent--;
        ind(); System.out.println("[/Block:" + node.getBlockName() + "]");
    }

    @Override
    public void visit(Jinja2ForNode node) {
        printNode(node, "for " + node.getVariable() + " in " + node.getIterable());
        indent++;
        for (Jinja2Node ch : node.getBody()) ch.accept(this);
        indent--;
        ind(); System.out.println("[/For]");
    }

    @Override
    public void visit(Jinja2IfNode node) {
        printNode(node, "if " + node.getCondition());
        indent++;
        if (!node.getThenBody().isEmpty()) {
            ind(); System.out.println("── then ──");
            indent++;
            for (Jinja2Node ch : node.getThenBody()) ch.accept(this);
            indent--;
        }
        if (!node.getElseBody().isEmpty()) {
            ind(); System.out.println("── else ──");
            indent++;
            for (Jinja2Node ch : node.getElseBody()) ch.accept(this);
            indent--;
        }
        indent--;
        ind(); System.out.println("[/If]");
    }

    @Override
    public void visit(Jinja2VariableNode node) {
        printNode(node, "{{ " + node.getExpression() + " }}");
    }

    @Override
    public void visit(Jinja2RawTextNode node) {
        String text = node.getText().replace("\n", "\\n");
        if (text.length() > 60) text = text.substring(0, 60) + "...";
        if (!text.isBlank()) printNode(node, "\"" + text + "\"");
    }

    @Override
    public void visit(Jinja2HtmlElementNode node) {
        String attrs = node.getAttributes().isEmpty() ? "" :
                " " + node.getAttributes().stream()
                        .map(Object::toString)
                        .reduce((a,b) -> a + " " + b).orElse("");
        printNode(node, "<" + node.getTagName() + attrs + ">");
        if (!node.getChildren().isEmpty()) {
            indent++;
            for (Jinja2Node ch : node.getChildren()) ch.accept(this);
            indent--;
        }
    }

    @Override
    public void visit(Jinja2MacroNode node) {
        printNode(node, "macro " + node.getName() + "(" + String.join(",", node.getParams()) + ")");
        indent++;
        for (Jinja2Node ch : node.getBody()) ch.accept(this);
        indent--;
        ind(); System.out.println("[/Macro]");
    }

    private String padRight(String s, int n) {
        return String.format("%-" + n + "s", s == null ? "" : s);
    }
}
