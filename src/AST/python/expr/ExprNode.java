package AST.python.expr;

import AST.python.PythonNode;
import AST.python.PythonVisitor;

/**
 * العقدة الأساسية لكل تعابير Python (expressions).
 * ترث من PythonNode → OOP + Inheritance.
 */
public abstract class ExprNode extends PythonNode {
    protected ExprNode(String nodeName, int line, int col) {
        super(nodeName, line, col);
    }
}
