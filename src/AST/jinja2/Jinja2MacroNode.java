package AST.jinja2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * عقدة macro — تمثّل: {% macro render_product(product) %}...{% endmacro %}
 */
public class Jinja2MacroNode extends Jinja2Node {
    private final String name;
    private final List<String> params;
    private final List<Jinja2Node> body = new ArrayList<>();

    public Jinja2MacroNode(String name, List<String> params, int line, int col) {
        super("Jinja2Macro", line, col);
        this.name = name;
        this.params = new ArrayList<>(params);
    }

    public String getName()               { return name; }
    public List<String> getParams()       { return Collections.unmodifiableList(params); }
    public void addBody(Jinja2Node n)     { if (n != null) body.add(n); }
    public List<Jinja2Node> getBody()     { return Collections.unmodifiableList(body); }

    @Override public void accept(Jinja2Visitor v) { v.visit(this); }
}
