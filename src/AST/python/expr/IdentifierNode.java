package AST.python.expr;

import AST.python.PythonVisitor;

/** عقدة identifier (اسم متغير) — تمثّل: products, app, request */
public class IdentifierNode extends ExprNode {
    private final String name;
    public IdentifierNode(String name, int line, int col) {
        super("Identifier", line, col);
        this.name = name;
    }
    public String getName() { return name; }
    @Override public void accept(PythonVisitor v) { v.visit(this); }
}
