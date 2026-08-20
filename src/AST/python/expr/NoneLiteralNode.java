package AST.python.expr;

import AST.python.PythonVisitor;

/** عقدة None */
public class NoneLiteralNode extends ExprNode {
    public NoneLiteralNode(int line, int col) { super("NoneLiteral", line, col); }
    @Override public void accept(PythonVisitor v) { v.visit(this); }
}
