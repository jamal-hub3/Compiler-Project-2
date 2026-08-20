package AST.python.expr;

import AST.python.PythonNode;
import AST.python.PythonVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * عقدة list literal — تمثّل: [item1, item2, ...]
 */
public class ListLiteralNode extends ExprNode {
    private final List<PythonNode> elements = new ArrayList<>();

    public ListLiteralNode(int line, int col) {
        super("ListLiteral", line, col);
    }

    public void addElement(PythonNode e) { if (e != null) elements.add(e); }
    public List<PythonNode> getElements() { return Collections.unmodifiableList(elements); }

    @Override
    public void accept(PythonVisitor visitor) { visitor.visit(this); }
}
