package AST.jinja2;

/**
 * يمثّل خاصية HTML واحدة داخل Jinja2HtmlElementNode — مثل:
 *   href="{{ url_for('index') }}"   (isJinjaExpr = true,  value = "url_for('index')")
 *   class="product-card"             (isJinjaExpr = false, value = "product-card")
 *
 * ليست عقدة Jinja2Node مستقلة (لا تحتاج accept خاص بها) — بيانات وصفية
 * تحملها Jinja2HtmlElementNode.
 */
public class Jinja2AttributeNode {
    private final String  name;
    private final String  value;       // نص القيمة الخام (بدون {{ }})
    private final boolean isJinjaExpr; // هل القيمة تعبير Jinja يحتاج تبديل؟

    public Jinja2AttributeNode(String name, String value, boolean isJinjaExpr) {
        this.name = name;
        this.value = value;
        this.isJinjaExpr = isJinjaExpr;
    }

    public String  getName()        { return name; }
    public String  getValue()       { return value; }
    public boolean isJinjaExpr()    { return isJinjaExpr; }

    @Override
    public String toString() {
        return isJinjaExpr ? name + "=\"{{ " + value + " }}\"" : name + "=\"" + value + "\"";
    }
}
