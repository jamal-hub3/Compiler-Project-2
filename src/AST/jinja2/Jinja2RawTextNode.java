package AST.jinja2;

/**
 * عقدة نص خام — النص بين تعابير Jinja2 (HTML عادي أو نص)
 */
public class Jinja2RawTextNode extends Jinja2Node {
    private final String text;

    public Jinja2RawTextNode(String text, int line, int col) {
        super("Jinja2RawText", line, col);
        this.text = text;
    }

    public String getText() { return text; }

    @Override public void accept(Jinja2Visitor v) { v.visit(this); }
}
