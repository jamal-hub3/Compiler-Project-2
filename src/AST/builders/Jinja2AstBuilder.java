package AST.builders;

import AST.html.*;
import AST.jinja2.*;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Jinja2 AST Builder.
 * يبني شجرة Jinja2 AST (الشجرة الثانية) من HTML AST.
 * يُفصل الـ Jinja2 constructs عن بقية HTML ويبني بنية شجرة نظيفة:
 *  extends → blocks → for loops → if statements → variables → raw text
 */
public class Jinja2AstBuilder implements HtmlVisitor {

    // المكدّس الحالي للعقد المفتوحة
    private final Deque<Jinja2Node> stack = new ArrayDeque<>();
    private Jinja2TemplateNode root;
    private final String templateName;

    public Jinja2AstBuilder(String templateName) {
        this.templateName = templateName;
    }

    public Jinja2TemplateNode build(HtmlNode htmlRoot) {
        root = new Jinja2TemplateNode(templateName, htmlRoot.getLine(), htmlRoot.getColumn());
        stack.push(root);
        htmlRoot.accept(this);
        return root;
    }

    // ── Helper: الحصول على العقدة الحالية في المكدّس ─────────────────────────

    private void addToTop(Jinja2Node node) {
        if (stack.isEmpty()) { root.addChild(node); return; }
        Jinja2Node top = stack.peek();
        if      (top instanceof Jinja2TemplateNode t) t.addChild(node);
        else if (top instanceof Jinja2BlockNode b)    b.addBody(node);
        else if (top instanceof Jinja2ForNode f)      f.addBody(node);
        else if (top instanceof Jinja2IfNode i)       i.addThen(node);
        else if (top instanceof Jinja2HtmlElementNode e) e.addChild(node);
        else root.addChild(node);
    }

    // ── HtmlVisitor implementation ────────────────────────────────────────────

    @Override
    public void visit(HtmlDocumentNode node) {
        for (HtmlNode ch : node.getChildren()) ch.accept(this);
    }

    @Override
    public void visit(ElementNode node) {
        Jinja2HtmlElementNode elem = new Jinja2HtmlElementNode(
                node.getTagName(), node.getLine(), node.getColumn());

        // احفظ كل الخصائص (href, src, class, action, name...) بدل تجاهلها
        for (AttributeNode attr : node.getAttributes()) {
            AttributeValueNode val = attr.getValue();
            if (val instanceof JinjaAttributeValueNode jv) {
                elem.addAttribute(new Jinja2AttributeNode(
                        attr.getName(), jv.getExpression().trim(), true));
            } else if (val instanceof LiteralAttributeValueNode lv) {
                elem.addAttribute(new Jinja2AttributeNode(
                        attr.getName(), lv.getValue(), false));
            } else {
                elem.addAttribute(new Jinja2AttributeNode(attr.getName(), "", false));
            }
        }

        addToTop(elem);
        stack.push(elem);
        for (HtmlNode ch : node.getChildren()) ch.accept(this);
        stack.pop();
    }

    @Override
    public void visit(TextNode node) {
        String text = node.getText().trim();
        if (!text.isEmpty()) {
            addToTop(new Jinja2RawTextNode(text, node.getLine(), node.getColumn()));
        }
    }

    @Override
    public void visit(JinjaExpressionNode node) {
        // {{ expression }}
        addToTop(new Jinja2VariableNode(
                node.getExpression().trim(), node.getLine(), node.getColumn()));
    }

    @Override
    public void visit(JinjaStatementNode node) {
        // {% statement %}
        processStatement(node.getStatement().trim(), node.getLine(), node.getColumn());
    }

    @Override
    public void visit(StyleNode node) { /* ignore CSS for Jinja2 tree */ }

    @Override
    public void visit(AttributeNode node) { /* ignore */ }

    @Override
    public void visit(LiteralAttributeValueNode node) { /* ignore */ }

    @Override
    public void visit(JinjaAttributeValueNode node) { /* ignore */ }

    // ── Statement Dispatcher ──────────────────────────────────────────────────

    private void processStatement(String stmt, int line, int col) {
        if (stmt.startsWith("extends")) {
            handleExtends(stmt, line, col);
        } else if (stmt.startsWith("block ")) {
            handleBlock(stmt, line, col);
        } else if (stmt.equals("endblock") || stmt.startsWith("endblock ")) {
            handleEndBlock(line, col);
        } else if (stmt.startsWith("for ")) {
            handleFor(stmt, line, col);
        } else if (stmt.equals("endfor")) {
            handleEnd(Jinja2ForNode.class);
        } else if (stmt.startsWith("if ")) {
            handleIf(stmt, line, col);
        } else if (stmt.equals("endif")) {
            handleEnd(Jinja2IfNode.class);
        } else if (stmt.startsWith("else")) {
            handleElse();
        } else if (stmt.startsWith("macro ")) {
            handleMacro(stmt, line, col);
        } else if (stmt.equals("endmacro")) {
            handleEnd(Jinja2MacroNode.class);
        }
        // set, include, import — skip for now
    }

    private void handleExtends(String stmt, int line, int col) {
        // extends "base.html"
        String parent = extractQuoted(stmt);
        Jinja2ExtendsNode node = new Jinja2ExtendsNode(parent, line, col);
        addToTop(node);
    }

    private void handleBlock(String stmt, int line, int col) {
        // block content
        String name = stmt.substring("block ".length()).trim().split("\\s+")[0];
        Jinja2BlockNode block = new Jinja2BlockNode(name, line, col);
        addToTop(block);
        stack.push(block);
    }

    private void handleEndBlock(int line, int col) {
        while (!stack.isEmpty() && !(stack.peek() instanceof Jinja2BlockNode)) stack.pop();
        if (!stack.isEmpty()) stack.pop();
    }

    private void handleFor(String stmt, int line, int col) {
        // for product in products
        Pattern p = Pattern.compile("for\\s+(\\w+)\\s+in\\s+(.+)");
        Matcher m = p.matcher(stmt);
        if (m.matches()) {
            String var  = m.group(1).trim();
            String iter = m.group(2).trim();
            Jinja2ForNode forNode = new Jinja2ForNode(var, iter, line, col);
            addToTop(forNode);
            stack.push(forNode);
        }
    }

    private void handleIf(String stmt, int line, int col) {
        // if condition
        String cond = stmt.substring("if ".length()).trim();
        Jinja2IfNode ifNode = new Jinja2IfNode(cond, line, col);
        addToTop(ifNode);
        stack.push(ifNode);
    }

    private void handleElse() {
        // Mark else branch (simplified: just continue)
    }

    private void handleMacro(String stmt, int line, int col) {
        // macro name(params)
        Pattern p = Pattern.compile("macro\\s+(\\w+)\\s*\\(([^)]*)\\)");
        Matcher m = p.matcher(stmt);
        if (m.find()) {
            String name = m.group(1);
            List<String> params = List.of(m.group(2).split(","));
            Jinja2MacroNode macro = new Jinja2MacroNode(name, params, line, col);
            addToTop(macro);
            stack.push(macro);
        }
    }

    private void handleEnd(Class<?> clazz) {
        while (!stack.isEmpty() && !clazz.isInstance(stack.peek())) stack.pop();
        if (!stack.isEmpty()) stack.pop();
    }

    private String extractQuoted(String s) {
        int a = s.indexOf('"');
        int b = s.lastIndexOf('"');
        if (a >= 0 && b > a) return s.substring(a+1, b);
        a = s.indexOf('\''); b = s.lastIndexOf('\'');
        if (a >= 0 && b > a) return s.substring(a+1, b);
        return s;
    }
}
