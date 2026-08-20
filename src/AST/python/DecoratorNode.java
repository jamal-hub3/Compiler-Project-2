package AST.python;

/**
 * عقدة decorator — تمثّل: @app.route("/")
 */
public class DecoratorNode extends PythonNode {
    private final String name;      // e.g. "app.route"
    private final String argument;  // e.g. "/"

    public DecoratorNode(String name, String argument, int line, int col) {
        super("Decorator", line, col);
        this.name     = name;
        this.argument = argument;
    }

    public String getName()     { return name; }
    public String getArgument() { return argument; }

    @Override
    public void accept(PythonVisitor visitor) { visitor.visit(this); }
}
