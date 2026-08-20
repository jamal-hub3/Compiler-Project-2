package AST.jinja2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * عقدة HTML element داخل قالب Jinja2 — مثل: <div class="...">, <h2>...
 */
public class Jinja2HtmlElementNode extends Jinja2Node {
    private final String tagName;
    private final List<Jinja2Node> children = new ArrayList<>();
    private final List<Jinja2AttributeNode> attributes = new ArrayList<>();

    public Jinja2HtmlElementNode(String tagName, int line, int col) {
        super("Jinja2HtmlElement", line, col);
        this.tagName = tagName;
    }

    public String getTagName()            { return tagName; }
    public void addChild(Jinja2Node n)    { if (n != null) children.add(n); }
    public List<Jinja2Node> getChildren() { return Collections.unmodifiableList(children); }

    public void addAttribute(Jinja2AttributeNode a) { if (a != null) attributes.add(a); }
    public List<Jinja2AttributeNode> getAttributes() { return Collections.unmodifiableList(attributes); }

    @Override public void accept(Jinja2Visitor v) { v.visit(this); }
}
