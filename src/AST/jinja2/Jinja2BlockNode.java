package AST.jinja2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * عقدة block — تمثّل: {% block content %}...{% endblock %}
 */
public class Jinja2BlockNode extends Jinja2Node {
    private final String blockName;
    private final List<Jinja2Node> body = new ArrayList<>();

    public Jinja2BlockNode(String blockName, int line, int col) {
        super("Jinja2Block", line, col);
        this.blockName = blockName;
    }

    public String getBlockName()        { return blockName; }
    public void addBody(Jinja2Node n)   { if (n != null) body.add(n); }
    public List<Jinja2Node> getBody()   { return Collections.unmodifiableList(body); }

    /** يستبدل محتوى الـ block بالكامل — يُستخدم عند دمج قالب فرعي مع base template */
    public void setBody(List<Jinja2Node> newBody) {
        body.clear();
        if (newBody != null) body.addAll(newBody);
    }

    @Override public void accept(Jinja2Visitor v) { v.visit(this); }
}
