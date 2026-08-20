package parser;

import java.util.ArrayList;
import java.util.List;

/**
 * Lexer يدوي (Manual Lexer) لكود Python/Flask المبسّط.
 * يحوّل النص الخام إلى قائمة tokens.
 */
public class PythonLexer {

    public enum TokenType {
        // Keywords
        KW_FROM, KW_IMPORT, KW_DEF, KW_RETURN, KW_IF, KW_ELSE,
        KW_ELIF, KW_FOR, KW_IN, KW_WHILE, KW_CLASS, KW_GLOBAL,
        KW_AND, KW_OR, KW_NOT, KW_NONE, KW_TRUE, KW_FALSE,

        // Literals
        STRING, NUMBER, IDENTIFIER,

        // Operators & Punctuation
        AT,         // @
        EQUALS,     // =
        EQ_EQ,      // ==
        NOT_EQ,     // !=
        LT, GT, LE, GE,
        PLUS, MINUS, STAR, SLASH,
        LPAREN, RPAREN,
        LBRACKET, RBRACKET,
        LBRACE, RBRACE,
        COLON, COMMA, DOT, SEMI,

        // Special
        NEWLINE, INDENT, DEDENT, EOF_TOKEN, UNKNOWN
    }

    public static class Token {
        public final TokenType type;
        public final String text;
        public final int line;
        public final int col;

        public Token(TokenType type, String text, int line, int col) {
            this.type = type; this.text = text; this.line = line; this.col = col;
        }

        @Override
        public String toString() {
            return type + "(\"" + text.replace("\n","\\n") + "\" L" + line + ":" + col + ")";
        }
    }

    private final String src;
    private int pos;
    private int line;
    private int col;
    private final List<Token> tokens = new ArrayList<>();

    public PythonLexer(String src) {
        this.src  = src;
        this.pos  = 0;
        this.line = 1;
        this.col  = 0;
    }

    public List<Token> tokenize() {
        while (pos < src.length()) {
            skipLineComments();
            if (pos >= src.length()) break;
            char c = src.charAt(pos);

            // Newline
            if (c == '\n') {
                tokens.add(new Token(TokenType.NEWLINE, "\\n", line, col));
                pos++; line++; col = 0;
                continue;
            }
            if (c == '\r') { pos++; continue; }

            // Whitespace (spaces/tabs — skip but track col)
            if (c == ' ' || c == '\t') { pos++; col++; continue; }

            // String literals
            if (c == '"' || c == '\'') { readString(c); continue; }

            // Numbers
            if (Character.isDigit(c)) { readNumber(); continue; }

            // Identifiers / keywords
            if (Character.isLetter(c) || c == '_') { readIdentifier(); continue; }

            // Operators / punctuation
            Token t = readPunctuation();
            if (t != null) tokens.add(t);
        }
        tokens.add(new Token(TokenType.EOF_TOKEN, "", line, col));
        return tokens;
    }

    private void skipLineComments() {
        while (pos < src.length()) {
            if (src.charAt(pos) == '#') {
                while (pos < src.length() && src.charAt(pos) != '\n') { pos++; col++; }
            } else break;
        }
    }

    private void readString(char quote) {
        int startLine = line, startCol = col;
        StringBuilder sb = new StringBuilder();
        pos++; col++; // skip opening quote

        // Triple-quote detection
        boolean triple = false;
        if (pos + 1 < src.length() && src.charAt(pos) == quote && src.charAt(pos+1) == quote) {
            triple = true; pos += 2; col += 2;
        }

        while (pos < src.length()) {
            char c = src.charAt(pos);
            if (triple) {
                if (c == quote && pos + 2 < src.length()
                        && src.charAt(pos+1) == quote && src.charAt(pos+2) == quote) {
                    pos += 3; col += 3; break;
                }
            } else {
                if (c == quote) { pos++; col++; break; }
                if (c == '\n')  { break; } // unterminated
            }
            if (c == '\\' && pos + 1 < src.length()) {
                sb.append(src.charAt(pos+1)); pos += 2; col += 2;
            } else {
                sb.append(c); pos++; col++;
            }
        }
        tokens.add(new Token(TokenType.STRING, sb.toString(), startLine, startCol));
    }

    private void readNumber() {
        int start = pos, startCol = col, startLine = line;
        while (pos < src.length() && (Character.isDigit(src.charAt(pos)) || src.charAt(pos) == '.')) {
            pos++; col++;
        }
        tokens.add(new Token(TokenType.NUMBER, src.substring(start, pos), startLine, startCol));
    }

    private void readIdentifier() {
        int start = pos, startCol = col, startLine = line;
        while (pos < src.length() && (Character.isLetterOrDigit(src.charAt(pos)) || src.charAt(pos) == '_')) {
            pos++; col++;
        }
        String word = src.substring(start, pos);
        tokens.add(new Token(keyword(word), word, startLine, startCol));
    }

    private TokenType keyword(String w) {
        return switch (w) {
            case "from"   -> TokenType.KW_FROM;
            case "import" -> TokenType.KW_IMPORT;
            case "def"    -> TokenType.KW_DEF;
            case "return" -> TokenType.KW_RETURN;
            case "if"     -> TokenType.KW_IF;
            case "else"   -> TokenType.KW_ELSE;
            case "elif"   -> TokenType.KW_ELIF;
            case "for"    -> TokenType.KW_FOR;
            case "in"     -> TokenType.KW_IN;
            case "while"  -> TokenType.KW_WHILE;
            case "class"  -> TokenType.KW_CLASS;
            case "global" -> TokenType.KW_GLOBAL;
            case "and"    -> TokenType.KW_AND;
            case "or"     -> TokenType.KW_OR;
            case "not"    -> TokenType.KW_NOT;
            case "None"   -> TokenType.KW_NONE;
            case "True"   -> TokenType.KW_TRUE;
            case "False"  -> TokenType.KW_FALSE;
            default       -> TokenType.IDENTIFIER;
        };
    }

    private Token readPunctuation() {
        char c = src.charAt(pos);
        int l = line, cl = col;
        String s = String.valueOf(c);

        // Two-char operators
        if (pos + 1 < src.length()) {
            char n = src.charAt(pos + 1);
            String two = "" + c + n;
            TokenType tt = switch (two) {
                case "==" -> TokenType.EQ_EQ;
                case "!=" -> TokenType.NOT_EQ;
                case "<=" -> TokenType.LE;
                case ">=" -> TokenType.GE;
                default   -> null;
            };
            if (tt != null) { pos += 2; col += 2; return new Token(tt, two, l, cl); }
        }

        TokenType tt = switch (c) {
            case '@' -> TokenType.AT;
            case '=' -> TokenType.EQUALS;
            case '<' -> TokenType.LT;
            case '>' -> TokenType.GT;
            case '+' -> TokenType.PLUS;
            case '-' -> TokenType.MINUS;
            case '*' -> TokenType.STAR;
            case '/' -> TokenType.SLASH;
            case '(' -> TokenType.LPAREN;
            case ')' -> TokenType.RPAREN;
            case '[' -> TokenType.LBRACKET;
            case ']' -> TokenType.RBRACKET;
            case '{' -> TokenType.LBRACE;
            case '}' -> TokenType.RBRACE;
            case ':' -> TokenType.COLON;
            case ',' -> TokenType.COMMA;
            case '.' -> TokenType.DOT;
            case ';' -> TokenType.SEMI;
            default  -> TokenType.UNKNOWN;
        };
        pos++; col++;
        return new Token(tt, s, l, cl);
    }
}
