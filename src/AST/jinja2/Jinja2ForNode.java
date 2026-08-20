package AST.jinja2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * عقدة for — تمثّل: {% for product in products %}...{% endfor %}
 */
public class Jinja2ForNode extends Jinja2Node {
    private final String variable;
    private final String iterable;
    private final List<Jinja2Node> body = new ArrayList<>();

    public Jinja2ForNode(String variable, String iterable, int line, int col) {
        super("Jinja2For", line, col);
        this.variable = variable;
        this.iterable = iterable;
    }

    public String getVariable()       { return variable; }
    public String getIterable()       { return iterable; }
    public void addBody(Jinja2Node n) { if (n != null) body.add(n); }
    public List<Jinja2Node> getBody() { return Collections.unmodifiableList(body); }

    @Override public void accept(Jinja2Visitor v) { v.visit(this); }
}
