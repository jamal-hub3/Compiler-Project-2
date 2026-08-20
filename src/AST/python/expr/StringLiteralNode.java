package AST.python.expr;

import AST.python.PythonVisitor;

/** عقدة string literal — تمثّل: "index.html" */
public class StringLiteralNode extends ExprNode {
    private final String value;
    public StringLiteralNode(String value, int line, int col) {
        super("StringLiteral", line, col);
        this.value = value;
    }
    public String getValue() { return value; }
    @Override public void accept(PythonVisitor v) { v.visit(this); }
}
