package AST.jinja2;

/**
 * Visitor interface — كل عقدة Jinja2 لها method زيارة (Polymorphism).
 */
public interface Jinja2Visitor {
    void visit(Jinja2TemplateNode node);
    void visit(Jinja2ExtendsNode node);
    void visit(Jinja2BlockNode node);
    void visit(Jinja2ForNode node);
    void visit(Jinja2IfNode node);
    void visit(Jinja2VariableNode node);
    void visit(Jinja2RawTextNode node);
    void visit(Jinja2HtmlElementNode node);
    void visit(Jinja2MacroNode node);
}
