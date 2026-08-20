package AST.python;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * عقدة تعريف الدالة — تمثّل: def index(): ...
 * تحتوي على قائمة decorators مثل @app.route(...)
 */
public class FunctionDefNode extends PythonNode {
    private final String name;
    private final List<String> params;
    private final List<PythonNode> body;
    private final List<DecoratorNode> decorators;

    public FunctionDefNode(String name, List<String> params, int line, int col) {
        super("FunctionDef", line, col);
        this.name       = name;
        this.params     = new ArrayList<>(params);
        this.body       = new ArrayList<>();
        this.decorators = new ArrayList<>();
    }

    public String getName()                    { return name; }
    public List<String> getParams()            { return Collections.unmodifiableList(params); }
    public List<PythonNode> getBody()          { return Collections.unmodifiableList(body); }
    public List<DecoratorNode> getDecorators() { return Collections.unmodifiableList(decorators); }

    public void addBodyStatement(PythonNode stmt) { if (stmt != null) body.add(stmt); }
    public void addDecorator(DecoratorNode d)      { if (d != null) decorators.add(d); }

    /** هل هذه دالة route في Flask؟ */
    public boolean isRoute() {
        return decorators.stream().anyMatch(d -> d.getName().startsWith("app.route"));
    }

    /** مسار الـ route إن وُجد */
    public String getRoutePath() {
        return decorators.stream()
            .filter(d -> d.getName().startsWith("app.route"))
            .map(DecoratorNode::getArgument)
            .findFirst()
            .orElse(null);
    }

    @Override
    public void accept(PythonVisitor visitor) { visitor.visit(this); }
}
