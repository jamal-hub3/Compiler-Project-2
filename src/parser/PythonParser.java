package parser;

import AST.python.*;
import AST.python.expr.*;

import java.util.ArrayList;
import java.util.List;

import static parser.PythonLexer.TokenType.*;

/**
 * Parser تنازلي متكرر (Recursive Descent Parser) لكود Python/Flask.
 * يحوّل قائمة tokens إلى Python AST.
 */
public class PythonParser {

    private final List<PythonLexer.Token> tokens;
    private int pos;

    public PythonParser(List<PythonLexer.Token> tokens) {
        // Remove newlines for simplicity in this simplified parser
        this.tokens = new ArrayList<>();
        for (PythonLexer.Token t : tokens) {
            if (t.type != NEWLINE) this.tokens.add(t);
        }
        this.pos = 0;
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private PythonLexer.Token peek() {
        return pos < tokens.size() ? tokens.get(pos) : tokens.get(tokens.size()-1);
    }

    private PythonLexer.Token consume() {
        return tokens.get(pos < tokens.size() ? pos++ : pos);
    }

    private PythonLexer.Token expect(PythonLexer.TokenType type) {
        PythonLexer.Token t = peek();
        if (t.type != type) {
            System.err.println("[PythonParser] Expected " + type + " but got " + t);
        }
        return consume();
    }

    private boolean check(PythonLexer.TokenType... types) {
        for (PythonLexer.TokenType tt : types) {
            if (peek().type == tt) return true;
        }
        return false;
    }

    private boolean match(PythonLexer.TokenType type) {
        if (check(type)) { consume(); return true; }
        return false;
    }

    // ── Top-level ──────────────────────────────────────────────────────────────

    public ProgramNode parseProgram() {
        ProgramNode prog = new ProgramNode(1, 0);
        while (!check(EOF_TOKEN)) {
            PythonNode stmt = parseTopLevelStatement();
            if (stmt != null) prog.addStatement(stmt);
        }
        return prog;
    }

    private PythonNode parseTopLevelStatement() {
        PythonLexer.Token t = peek();
        if (t.type == AT)          return parseDecoratedDef();
        if (t.type == KW_FROM)     return parseFrom();
        if (t.type == KW_IMPORT)   return parseImport();
        if (t.type == KW_DEF)      return parseFunctionDef(new ArrayList<>());
        if (t.type == KW_IF)       return parseIfStatement();
        if (t.type == KW_FOR)      return parseForStatement();
        if (t.type == KW_GLOBAL)   { skipToEndOfLine(); return null; }
        if (t.type == IDENTIFIER)  return parseAssignmentOrExprStmt();
        // skip unknown tokens
        consume();
        return null;
    }

    // ── Import ─────────────────────────────────────────────────────────────────

    private ImportNode parseFrom() {
        int l = peek().line, c = peek().col;
        consume(); // from
        String module = "";
        if (check(IDENTIFIER)) module = consume().text;
        if (check(KW_IMPORT)) consume(); // import
        List<String> names = new ArrayList<>();
        while (check(IDENTIFIER)) {
            names.add(consume().text);
            if (!match(COMMA)) break;
        }
        skipToEndOfLine();
        return new ImportNode(module, names, true, l, c);
    }

    private ImportNode parseImport() {
        int l = peek().line, c = peek().col;
        consume(); // import
        String module = "";
        if (check(IDENTIFIER)) module = consume().text;
        skipToEndOfLine();
        return new ImportNode(module, List.of(), false, l, c);
    }

    // ── Decorated Def ─────────────────────────────────────────────────────────

    private PythonNode parseDecoratedDef() {
        List<DecoratorNode> decorators = new ArrayList<>();
        while (check(AT)) {
            int l = peek().line, c = peek().col;
            consume(); // @
            StringBuilder name = new StringBuilder();
            if (check(IDENTIFIER)) name.append(consume().text);
            while (check(DOT)) {
                consume(); // .
                if (check(IDENTIFIER)) name.append(".").append(consume().text);
            }
            String arg = "";
            if (match(LPAREN)) {
                arg = readUntil(RPAREN);
                match(RPAREN);
            }
            decorators.add(new DecoratorNode(name.toString(), arg, l, c));
        }
        if (check(KW_DEF)) return parseFunctionDef(decorators);
        // fallback
        return null;
    }

    // ── Function Def ──────────────────────────────────────────────────────────

    private FunctionDefNode parseFunctionDef(List<DecoratorNode> decorators) {
        int l = peek().line, c = peek().col;
        consume(); // def
        String name = check(IDENTIFIER) ? consume().text : "unknown";
        List<String> params = parseParamList();
        match(COLON); // :
        List<PythonNode> body = parseBlock();
        FunctionDefNode fn = new FunctionDefNode(name, params, l, c);
        decorators.forEach(fn::addDecorator);
        body.forEach(fn::addBodyStatement);
        return fn;
    }

    private List<String> parseParamList() {
        List<String> params = new ArrayList<>();
        if (!match(LPAREN)) return params;
        while (!check(RPAREN, EOF_TOKEN)) {
            if (check(IDENTIFIER)) {
                String p = consume().text;
                // skip type hints and defaults
                while (!check(COMMA, RPAREN, EOF_TOKEN)) consume();
                params.add(p);
            }
            if (!match(COMMA)) break;
        }
        match(RPAREN);
        return params;
    }

    private List<PythonNode> parseBlock() {
        List<PythonNode> stmts = new ArrayList<>();
        // Simple block: read statements until dedent (simulated by indent heuristic)
        // We track brace/paren depth to handle multi-line expressions
        int initialIndent = getNextIndent();
        while (!check(EOF_TOKEN)) {
            int curIndent = getNextIndent();
            if (curIndent <= initialIndent - 4 && curIndent != initialIndent) break;
            if (check(AT, KW_DEF) && curIndent <= initialIndent - 4) break;
            PythonNode stmt = parseBlockStatement();
            if (stmt != null) stmts.add(stmt);
        }
        return stmts;
    }

    private int getNextIndent() {
        // Approximation: look at position in current token
        if (pos < tokens.size()) return tokens.get(pos).col;
        return 0;
    }

    private PythonNode parseBlockStatement() {
        PythonLexer.Token t = peek();
        if (t.type == KW_RETURN) return parseReturn();
        if (t.type == KW_IF)     return parseIfStatement();
        if (t.type == KW_FOR)    return parseForStatement();
        if (t.type == KW_GLOBAL) { skipToEndOfLine(); return null; }
        if (t.type == AT || t.type == KW_DEF) return null; // next function
        if (t.type == IDENTIFIER) return parseAssignmentOrExprStmt();
        if (t.type == KW_FROM || t.type == KW_IMPORT) { skipToEndOfLine(); return null; }
        consume(); return null;
    }

    // ── Statements ────────────────────────────────────────────────────────────

    private ReturnNode parseReturn() {
        int l = peek().line, c = peek().col;
        consume(); // return
        if (atEndOfLogicalLine()) return new ReturnNode(null, l, c);
        PythonNode val = parseExpression();
        return new ReturnNode(val, l, c);
    }

    private IfStatementNode parseIfStatement() {
        int l = peek().line, c = peek().col;
        consume(); // if
        PythonNode cond = parseExpression();
        match(COLON);
        List<PythonNode> thenBody = parseBlock();
        IfStatementNode stmt = new IfStatementNode(cond, l, c);
        thenBody.forEach(stmt::addThen);
        // else / elif
        if (check(KW_ELSE) || check(KW_ELIF)) {
            consume();
            if (check(COLON)) consume();
            List<PythonNode> elseBody = parseBlock();
            elseBody.forEach(stmt::addElse);
        }
        return stmt;
    }

    private ForStatementNode parseForStatement() {
        int l = peek().line, c = peek().col;
        consume(); // for
        String var = check(IDENTIFIER) ? consume().text : "item";
        match(KW_IN);
        PythonNode iter = parseExpression();
        match(COLON);
        List<PythonNode> body = parseBlock();
        ForStatementNode stmt = new ForStatementNode(var, iter, l, c);
        body.forEach(stmt::addBodyStatement);
        return stmt;
    }

    private PythonNode parseAssignmentOrExprStmt() {
        int l = peek().line, c = peek().col;
        // Look ahead for '='
        int saved = pos;
        String name = consume().text; // IDENTIFIER

        // attribute.access.chain
        StringBuilder fullName = new StringBuilder(name);
        while (check(DOT)) {
            consume();
            if (check(IDENTIFIER)) fullName.append(".").append(consume().text);
        }

        // subscript: products.append(...)
        if (fullName.toString().endsWith(".append") && check(LPAREN)) {
            consume(); // (
            PythonNode item = parseExpression();
            match(RPAREN);
            String listName = fullName.substring(0, fullName.lastIndexOf(".append"));
            return new AppendStatementNode(listName, item, l, c);
        }

        if (check(EQUALS)) {
            consume(); // =
            PythonNode val = parseExpression();
            return new AssignmentNode(fullName.toString(), val, l, c);
        }

        // Just an expression statement
        pos = saved;
        PythonNode expr = parseExpression();
        return new ExpressionStatementNode(expr, l, c);
    }

    // ── Expressions ───────────────────────────────────────────────────────────

    public PythonNode parseExpression() {
        return parseComparison();
    }

    private PythonNode parseComparison() {
        PythonNode left = parseAddSub();
        while (check(EQ_EQ, NOT_EQ, LT, GT, LE, GE)) {
            String op = consume().text;
            PythonNode right = parseAddSub();
            int l = left.getLine(), c = left.getColumn();
            left = new BinaryOpNode(left, op, right, l, c);
        }
        return left;
    }

    private PythonNode parseAddSub() {
        PythonNode left = parsePrimary();
        while (check(PLUS, MINUS)) {
            String op = consume().text;
            PythonNode right = parsePrimary();
            left = new BinaryOpNode(left, op, right, left.getLine(), left.getColumn());
        }
        return left;
    }

    private PythonNode parsePrimary() {
        PythonLexer.Token t = peek();

        if (t.type == STRING)   return new StringLiteralNode(consume().text, t.line, t.col);
        if (t.type == NUMBER)   return parseNumber(t);
        if (t.type == KW_TRUE)  { consume(); return new BoolLiteralNode(true,  t.line, t.col); }
        if (t.type == KW_FALSE) { consume(); return new BoolLiteralNode(false, t.line, t.col); }
        if (t.type == KW_NONE)  { consume(); return new NoneLiteralNode(t.line, t.col); }
        if (t.type == LBRACKET) return parseList(t);
        if (t.type == LBRACE)   return parseDict(t);
        if (t.type == LPAREN)   { consume(); PythonNode e = parseExpression(); match(RPAREN); return e; }

        if (t.type == IDENTIFIER) {
            consume();
            ExprNode base = new IdentifierNode(t.text, t.line, t.col);
            return parseSuffix(base);
        }

        // fallback
        consume();
        return new StringLiteralNode("?", t.line, t.col);
    }

    private PythonNode parseNumber(PythonLexer.Token t) {
        consume();
        try { return new NumberLiteralNode(Double.parseDouble(t.text), t.line, t.col); }
        catch (NumberFormatException e) { return new NumberLiteralNode(0, t.line, t.col); }
    }

    private ExprNode parseSuffix(ExprNode base) {
        while (true) {
            if (check(DOT) && pos + 1 < tokens.size() && tokens.get(pos+1).type == IDENTIFIER) {
                consume(); // .
                String attr = consume().text;
                int l = base.getLine(), c = base.getColumn();
                AttributeAccessNode acc = new AttributeAccessNode(base, attr, l, c);
                if (check(LPAREN)) {
                    base = parseCallSuffix(acc, l, c);
                } else {
                    base = acc;
                }
            } else if (check(LPAREN)) {
                base = parseCallSuffix(base, base.getLine(), base.getColumn());
            } else if (check(LBRACKET)) {
                // subscript — just skip for now
                consume();
                while (!check(RBRACKET, EOF_TOKEN)) consume();
                match(RBRACKET);
            } else {
                break;
            }
        }
        return base;
    }

    private FunctionCallNode parseCallSuffix(ExprNode callee, int l, int c) {
        consume(); // (
        FunctionCallNode call = new FunctionCallNode(callee, l, c);
        while (!check(RPAREN, EOF_TOKEN)) {
            // Check for keyword arg: name=value
            if (check(IDENTIFIER) && pos + 1 < tokens.size() && tokens.get(pos+1).type == EQUALS) {
                String kw = consume().text;
                consume(); // =
                PythonNode val = parseExpression();
                call.addKeywordArg(kw, val);
            } else {
                call.addPositionalArg(parseExpression());
            }
            if (!match(COMMA)) break;
        }
        match(RPAREN);
        return call;
    }

    private ListLiteralNode parseList(PythonLexer.Token t) {
        consume(); // [
        ListLiteralNode list = new ListLiteralNode(t.line, t.col);
        while (!check(RBRACKET, EOF_TOKEN)) {
            list.addElement(parseExpression());
            if (!match(COMMA)) break;
        }
        match(RBRACKET);
        return list;
    }

    private DictLiteralNode parseDict(PythonLexer.Token t) {
        consume(); // {
        DictLiteralNode dict = new DictLiteralNode(t.line, t.col);
        while (!check(RBRACE, EOF_TOKEN)) {
            PythonNode key = parseExpression();
            match(COLON);
            PythonNode val = parseExpression();
            dict.addEntry(key, val);
            if (!match(COMMA)) break;
        }
        match(RBRACE);
        return dict;
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private boolean atEndOfLogicalLine() {
        PythonLexer.Token t = peek();
        return t.type == EOF_TOKEN || t.type == AT || t.type == KW_DEF
                || t.type == KW_RETURN || t.type == KW_IF;
    }

    private void skipToEndOfLine() {
        while (!check(EOF_TOKEN, KW_DEF, AT)) {
            PythonLexer.Token t = peek();
            if (t.type == COLON && t.col == 0) break;
            consume();
        }
    }

    private String readUntil(PythonLexer.TokenType end) {
        StringBuilder sb = new StringBuilder();
        int depth = 0;
        while (!check(EOF_TOKEN)) {
            PythonLexer.Token t = peek();
            if (t.type == LPAREN) depth++;
            if (t.type == end && depth == 0) break;
            if (t.type == RPAREN) { if (depth > 0) depth--; else break; }
            sb.append(t.text);
            consume();
        }
        return sb.toString();
    }
}
