package AST.python;

/**
 * جملة تعبير — تمثّل: app.run(debug=True)
 */
public class ExpressionStatementNode extends PythonNode {
    private final PythonNode expression;

    public ExpressionStatementNode(PythonNode expression, int line, int col) {
        super("ExpressionStatement", line, col);
        this.expression = expression;
    }

    public PythonNode getExpression() { return expression; }

    @Override
    public void accept(PythonVisitor visitor) { visitor.visit(this); }
}
