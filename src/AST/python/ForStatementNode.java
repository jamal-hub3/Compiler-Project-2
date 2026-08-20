package AST.python;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * عقدة for — تمثّل: for product in products: ...
 */
public class ForStatementNode extends PythonNode {
    private final String variable;
    private final PythonNode iterable;
    private final List<PythonNode> body;

    public ForStatementNode(String variable, PythonNode iterable, int line, int col) {
        super("ForStatement", line, col);
        this.variable = variable;
        this.iterable = iterable;
        this.body     = new ArrayList<>();
    }

    public String getVariable()         { return variable; }
    public PythonNode getIterable()     { return iterable; }
    public List<PythonNode> getBody()   { return Collections.unmodifiableList(body); }

    public void addBodyStatement(PythonNode s) { if (s != null) body.add(s); }

    @Override
    public void accept(PythonVisitor visitor) { visitor.visit(this); }
}
