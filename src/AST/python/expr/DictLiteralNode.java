package AST.python.expr;

import AST.python.PythonNode;
import AST.python.PythonVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * عقدة dict literal — تمثّل: {"id": 1, "name": "Laptop", ...}
 */
public class DictLiteralNode extends ExprNode {

    public record Entry(PythonNode key, PythonNode value) {}

    private final List<Entry> entries = new ArrayList<>();

    public DictLiteralNode(int line, int col) {
        super("DictLiteral", line, col);
    }

    public void addEntry(PythonNode key, PythonNode value) {
        entries.add(new Entry(key, value));
    }

    public List<Entry> getEntries() { return Collections.unmodifiableList(entries); }

    @Override
    public void accept(PythonVisitor visitor) { visitor.visit(this); }
}
