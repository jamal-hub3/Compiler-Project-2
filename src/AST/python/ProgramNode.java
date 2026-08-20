package AST.python;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * جذر شجرة Python AST — يمثّل الملف كاملاً.
 */
public class ProgramNode extends PythonNode {
    private final List<PythonNode> statements = new ArrayList<>();

    public ProgramNode(int line, int column) {
        super("Program", line, column);
    }

    public void addStatement(PythonNode stmt) {
        if (stmt != null) statements.add(stmt);
    }

    public List<PythonNode> getStatements() {
        return Collections.unmodifiableList(statements);
    }

    @Override
    public void accept(PythonVisitor visitor) {
        visitor.visit(this);
    }
}
