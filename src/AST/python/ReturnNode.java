package AST.python;

/**
 * عقدة return — تمثّل: return render_template("index.html", products=products)
 */
public class ReturnNode extends PythonNode {
    private final PythonNode value; // القيمة المُرجعة (ممكن null)

    public ReturnNode(PythonNode value, int line, int col) {
        super("Return", line, col);
        this.value = value;
    }

    public PythonNode getValue() { return value; }

    @Override
    public void accept(PythonVisitor visitor) { visitor.visit(this); }
}
