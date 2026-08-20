package AST.python.expr;

import AST.python.PythonVisitor;

/** عقدة boolean — تمثّل: True / False */
public class BoolLiteralNode extends ExprNode {
    private final boolean value;
    public BoolLiteralNode(boolean value, int line, int col) {
        super("BoolLiteral", line, col);
        this.value = value;
    }
    public boolean getValue() { return value; }
    @Override public void accept(PythonVisitor v) { v.visit(this); }
}
