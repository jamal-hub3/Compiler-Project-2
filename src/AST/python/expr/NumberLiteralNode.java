package AST.python.expr;

import AST.python.PythonVisitor;

/** عقدة رقم — تمثّل: 1, 3.14, 999.99 */
public class NumberLiteralNode extends ExprNode {
    private final double value;
    public NumberLiteralNode(double value, int line, int col) {
        super("NumberLiteral", line, col);
        this.value = value;
    }
    public double getValue() { return value; }
    @Override public void accept(PythonVisitor v) { v.visit(this); }
}
