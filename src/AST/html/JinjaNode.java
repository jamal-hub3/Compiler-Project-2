package AST.html;

public abstract class JinjaNode extends HtmlNode {
    protected JinjaNode(String nodeName, int line, int column) {
        super(nodeName, line, column);
    }
}
