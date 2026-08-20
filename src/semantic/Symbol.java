package semantic;

/**
 * يمثّل رمزاً (symbol) في جدول الرموز.
 * OOP: يحفظ الاسم والنوع والسطر والعمود.
 */
public class Symbol {
    public enum Kind { VARIABLE, FUNCTION, ROUTE, TEMPLATE, BLOCK, FOR_VAR, PARAMETER }

    private final String name;
    private final Kind kind;
    private final int line;
    private final int col;
    private String type;    // نوع القيمة إن عُرف (list, dict, str, int, ...)

    public Symbol(String name, Kind kind, int line, int col) {
        this.name = name;
        this.kind = kind;
        this.line = line;
        this.col  = col;
        this.type = "unknown";
    }

    public String getName() { return name; }
    public Kind getKind()   { return kind; }
    public int getLine()    { return line; }
    public int getCol()     { return col; }
    public String getType() { return type; }
    public void setType(String t) { this.type = t; }

    @Override
    public String toString() {
        return kind + " " + name + " [" + type + "] @L" + line + ":C" + col;
    }
}
