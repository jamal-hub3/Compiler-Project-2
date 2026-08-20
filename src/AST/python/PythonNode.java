package AST.python;

/**
 * العقدة الأساسية (abstract) لكل عقد شجرة Python AST.
 * كل عقدة تحفظ: اسم العقدة، رقم السطر، رقم العمود.
 * تطبق مبدأ OOP + Inheritance + Polymorphism.
 */
public abstract class PythonNode {
    private final String nodeName;
    private final int line;
    private final int column;

    protected PythonNode(String nodeName, int line, int column) {
        this.nodeName = nodeName;
        this.line = line;
        this.column = column;
    }

    public String getNodeName() { return nodeName; }
    public int getLine()        { return line; }
    public int getColumn()      { return column; }

    /** Visitor pattern - polymorphism */
    public abstract void accept(PythonVisitor visitor);

    @Override
    public String toString() {
        return nodeName + "(line=" + line + ", col=" + column + ")";
    }
}
