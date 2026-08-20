package AST.css;

/**
 * العقدة الأساسية لكل عقد الـ CSS AST.
 * كل عقدة تحفظ رقم السطر والعمود (line, col) كما يطلب المشروع.
 */
public abstract class CssNode {

    private final int line;
    private final int col;

    protected CssNode(int line, int col) {
        this.line = line;
        this.col  = col;
    }

    public int getLine() { return line; }
    public int getCol()  { return col;  }

    public abstract void accept(CssVisitor visitor);
}
