package AST.python;

/**
 * عقدة append — تمثّل: products.append(new_product)
 */
public class AppendStatementNode extends PythonNode {
    private final String listName;
    private final PythonNode item;

    public AppendStatementNode(String listName, PythonNode item, int line, int col) {
        super("AppendStatement", line, col);
        this.listName = listName;
        this.item     = item;
    }

    public String getListName()  { return listName; }
    public PythonNode getItem()  { return item; }

    @Override
    public void accept(PythonVisitor visitor) { visitor.visit(this); }
}
