package AST.python;

/**
 * عقدة assignment — تمثّل: products = [...]  أو  app = Flask(__name__)
 */
public class AssignmentNode extends PythonNode {
    private final String target;       // اسم المتغير
    private final PythonNode value;    // القيمة

    public AssignmentNode(String target, PythonNode value, int line, int col) {
        super("Assignment", line, col);
        this.target = target;
        this.value  = value;
    }

    public String getTarget()    { return target; }
    public PythonNode getValue() { return value; }

    @Override
    public void accept(PythonVisitor visitor) { visitor.visit(this); }
}
