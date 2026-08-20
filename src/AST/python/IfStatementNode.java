package AST.python;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * عقدة if — تمثّل: if request.method == "POST": ...
 */
public class IfStatementNode extends PythonNode {
    private final PythonNode condition;
    private final List<PythonNode> thenBody;
    private final List<PythonNode> elseBody;

    public IfStatementNode(PythonNode condition, int line, int col) {
        super("IfStatement", line, col);
        this.condition = condition;
        this.thenBody  = new ArrayList<>();
        this.elseBody  = new ArrayList<>();
    }

    public PythonNode getCondition()       { return condition; }
    public List<PythonNode> getThenBody()  { return Collections.unmodifiableList(thenBody); }
    public List<PythonNode> getElseBody()  { return Collections.unmodifiableList(elseBody); }

    public void addThen(PythonNode s) { if (s != null) thenBody.add(s); }
    public void addElse(PythonNode s) { if (s != null) elseBody.add(s); }

    @Override
    public void accept(PythonVisitor visitor) { visitor.visit(this); }
}
