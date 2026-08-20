package AST.python.expr;

import AST.python.PythonNode;
import AST.python.PythonVisitor;

/**
 * عقدة attribute access — تمثّل: request.method  أو  request.form["name"]
 */
public class AttributeAccessNode extends ExprNode {
    private final PythonNode object;
    private final String attribute;

    public AttributeAccessNode(PythonNode object, String attribute, int line, int col) {
        super("AttributeAccess", line, col);
        this.object    = object;
        this.attribute = attribute;
    }

    public PythonNode getObject()   { return object; }
    public String getAttribute()    { return attribute; }

    @Override
    public void accept(PythonVisitor visitor) { visitor.visit(this); }
}
