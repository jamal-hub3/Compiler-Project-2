package AST.jinja2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * عقدة if — تمثّل: {% if product %}...{% endif %}
 */
public class Jinja2IfNode extends Jinja2Node {
    private final String condition;
    private final List<Jinja2Node> thenBody = new ArrayList<>();
    private final List<Jinja2Node> elseBody = new ArrayList<>();

    public Jinja2IfNode(String condition, int line, int col) {
        super("Jinja2If", line, col);
        this.condition = condition;
    }

    public String getCondition()          { return condition; }
    public void addThen(Jinja2Node n)     { if (n != null) thenBody.add(n); }
    public void addElse(Jinja2Node n)     { if (n != null) elseBody.add(n); }
    public List<Jinja2Node> getThenBody() { return Collections.unmodifiableList(thenBody); }
    public List<Jinja2Node> getElseBody() { return Collections.unmodifiableList(elseBody); }

    @Override public void accept(Jinja2Visitor v) { v.visit(this); }
}
