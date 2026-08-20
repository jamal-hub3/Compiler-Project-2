package AST.jinja2;

/**
 * عقدة variable — تمثّل: {{ product.name }} أو {{ product.price }}
 */
public class Jinja2VariableNode extends Jinja2Node {
    private final String expression;

    public Jinja2VariableNode(String expression, int line, int col) {
        super("Jinja2Variable", line, col);
        this.expression = expression;
    }

    public String getExpression() { return expression; }

    @Override public void accept(Jinja2Visitor v) { v.visit(this); }
}
