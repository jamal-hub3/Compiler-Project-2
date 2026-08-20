package AST.visitors;

import AST.html.*;

/**
 * Visitor يطبع شجرة HTML/Jinja/CSS بشكل مقروء ومحاذٍ.
 * لكل عقدة: اسمها + رقم السطر + العمود + أبناؤها.
 */
public class AstPrintVisitor implements HtmlVisitor {

    private int indent = 0;

    private void ind() {
        System.out.print("  ".repeat(Math.max(0, indent)));
    }

    private void printNode(HtmlNode n, String extra) {
        ind();
        System.out.println(n.getNodeName()
                + " (line=" + n.getLine() + ", col=" + n.getColumn() + ") " + extra);
    }

    @Override
    public void visit(HtmlDocumentNode node) {
        printNode(node, "");
        indent++;
        for (HtmlNode ch : node.getChildren()) ch.accept(this);
        indent--;
    }

    @Override
    public void visit(ElementNode node) {
        printNode(node, "tag=" + node.getTagName());
        indent++;

        if (!node.getAttributes().isEmpty()) {
            ind();
            System.out.println("Attributes:");
            indent++;
            for (AttributeNode a : node.getAttributes()) a.accept(this);
            indent--;
        }

        for (HtmlNode ch : node.getChildren()) ch.accept(this);
        indent--;
    }

    @Override
    public void visit(AttributeNode node) {
        printNode(node, "name=" + node.getName());
        indent++;
        if (node.getValue() != null) node.getValue().accept(this);
        indent--;
    }

    @Override
    public void visit(TextNode node) {
        String text = node.getText().trim();
        if (text.isEmpty()) return; // تخطّى النصوص الفارغة
        printNode(node, "text=\"" + text.replace("\n", "\\n") + "\"");
    }

    @Override
    public void visit(JinjaExpressionNode node) {
        printNode(node, "{{ " + node.getExpression() + " }}");
    }

    @Override
    public void visit(JinjaStatementNode node) {
        printNode(node, "{% " + node.getStatement() + " %}");
    }

    @Override
    public void visit(LiteralAttributeValueNode node) {
        printNode(node, "value=\"" + node.getValue() + "\"");
    }

    @Override
    public void visit(JinjaAttributeValueNode node) {
        printNode(node, "expr=\"" + node.getExpression() + "\"");
    }

    @Override
    public void visit(StyleNode node) {
        printNode(node, "");
        indent++;

        // طباعة CSS مختصرة
        ind();
        System.out.println("rawCss=\"" + shortCss(node.getRawCss()) + "\"");

        // طباعة CSS AST محاذية تحت StyleNode
        if (node.getCssAst() != null) {
            ind();
            System.out.println("CSS-AST:");
            indent++;
            CssPrintVisitor cssPrinter = new CssPrintVisitor(indent); // ✅ نمرر الـ indent الحالي
            node.getCssAst().accept(cssPrinter);
            indent--;
        }

        indent--;
    }

    private String shortCss(String raw) {
        if (raw == null) return "";
        String s = raw.trim().replace("\r", "").replace("\n", "\\n");
        return (s.length() > 80) ? s.substring(0, 80) + "..." : s;
    }
}
