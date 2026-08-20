package AST.python.expr;

import AST.python.PythonNode;
import AST.python.PythonVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * عقدة function call — تمثّل: render_template("index.html", products=products)
 */
public class FunctionCallNode extends ExprNode {
    private final PythonNode callee;                     // الدالة المستدعاة
    private final List<PythonNode> positionalArgs;       // الوسائط الموضعية
    private final Map<String, PythonNode> keywordArgs;   // الوسائط المُسمّاة

    public FunctionCallNode(PythonNode callee, int line, int col) {
        super("FunctionCall", line, col);
        this.callee        = callee;
        this.positionalArgs = new ArrayList<>();
        this.keywordArgs   = new LinkedHashMap<>();
    }

    public PythonNode getCallee()                        { return callee; }
    public List<PythonNode> getPositionalArgs()          { return Collections.unmodifiableList(positionalArgs); }
    public Map<String, PythonNode> getKeywordArgs()      { return Collections.unmodifiableMap(keywordArgs); }

    public void addPositionalArg(PythonNode arg)         { if (arg != null) positionalArgs.add(arg); }
    public void addKeywordArg(String name, PythonNode v) { keywordArgs.put(name, v); }

    @Override
    public void accept(PythonVisitor visitor) { visitor.visit(this); }
}
