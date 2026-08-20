package AST.css;

public class RulesetNode extends CssNode {
    public SelectorGroupNode selectors;
    public DeclarationBlockNode block;

    public RulesetNode(int line, int col) { super(line, col); }

    @Override public void accept(CssVisitor visitor) { visitor.visit(this); }
}

