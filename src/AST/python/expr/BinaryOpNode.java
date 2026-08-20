package AST.python.expr;

import AST.python.PythonNode;
import AST.python.PythonVisitor;

/** عقدة عملية ثنائية — تمثّل: a == b, a + b, a != b */
public class BinaryOpNode extends ExprNode {
    private final PythonNode left;
    private final String operator;
    private final PythonNode right;

    public BinaryOpNode(PythonNode left, String operator, PythonNode right, int line, int col) {
        super("BinaryOp", line, col);
        this.left = left; this.operator = operator; this.right = right;
    }
    public PythonNode getLeft()   { return left; }
    public String getOperator()   { return operator; }
    public PythonNode getRight()  { return right; }
    @Override public void accept(PythonVisitor v) { v.visit(this); }
}
