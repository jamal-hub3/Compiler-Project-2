package semantic;

/**
 * يمثّل خطأً داللياً — يحفظ نوع الخطأ والرسالة والموقع.
 */
public class SemanticError {
    public enum Severity { ERROR, WARNING }

    private final String message;
    private final int line;
    private final int col;
    private final Severity severity;

    public SemanticError(String message, int line, int col) {
        this(message, line, col, Severity.ERROR);
    }

    public SemanticError(String message, int line, int col, Severity severity) {
        this.message  = message;
        this.line     = line;
        this.col      = col;
        this.severity = severity;
    }

    public String getMessage()  { return message; }
    public int getLine()        { return line; }
    public int getCol()         { return col; }
    public Severity getSeverity() { return severity; }

    @Override
    public String toString() {
        return "[" + severity + "] L" + line + ":C" + col + " — " + message;
    }
}
