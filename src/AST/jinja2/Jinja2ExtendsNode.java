package AST.jinja2;

/**
 * عقدة extends — تمثّل: {% extends "base.html" %}
 */
public class Jinja2ExtendsNode extends Jinja2Node {
    private final String parentTemplate;

    public Jinja2ExtendsNode(String parentTemplate, int line, int col) {
        super("Jinja2Extends", line, col);
        this.parentTemplate = parentTemplate;
    }

    public String getParentTemplate() { return parentTemplate; }

    @Override public void accept(Jinja2Visitor v) { v.visit(this); }
}
