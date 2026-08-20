package AST.jinja2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * جذر شجرة Jinja2 — يمثّل ملف قالب كامل.
 */
public class Jinja2TemplateNode extends Jinja2Node {
    private final String templateName;
    private final List<Jinja2Node> children = new ArrayList<>();

    public Jinja2TemplateNode(String templateName, int line, int col) {
        super("Jinja2Template", line, col);
        this.templateName = templateName;
    }

    public String getTemplateName()       { return templateName; }
    public void addChild(Jinja2Node n)    { if (n != null) children.add(n); }
    public List<Jinja2Node> getChildren() { return Collections.unmodifiableList(children); }

    @Override public void accept(Jinja2Visitor v) { v.visit(this); }
}
