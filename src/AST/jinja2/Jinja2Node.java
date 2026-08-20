package AST.jinja2;

/**
 * العقدة الأساسية (abstract) لشجرة Jinja2 AST.
 * هذه الشجرة مستقلة عن HTML AST وتمثّل بنية القالب (Template Structure).
 * OOP + Inheritance + Polymorphism.
 */
public abstract class Jinja2Node {
    private final String nodeName;
    private final int line;
    private final int column;

    protected Jinja2Node(String nodeName, int line, int column) {
        this.nodeName = nodeName;
        this.line = line;
        this.column = column;
    }

    public String getNodeName() { return nodeName; }
    public int getLine()        { return line; }
    public int getColumn()      { return column; }

    public abstract void accept(Jinja2Visitor visitor);

    @Override
    public String toString() {
        return nodeName + "(L" + line + ":" + column + ")";
    }
}
