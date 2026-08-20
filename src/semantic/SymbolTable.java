package semantic;

import java.util.*;

/**
 * جدول الرموز — يدعم النطاقات المتداخلة (Scopes).
 * يُستخدم في التحليل الداللي للتحقق من تعريف المتغيرات والدوال.
 */
public class SymbolTable {

    private final Deque<Map<String, Symbol>> scopes = new ArrayDeque<>();
    private final String name;

    public SymbolTable(String name) {
        this.name = name;
        enterScope(); // النطاق العام
    }

    public void enterScope()  { scopes.push(new LinkedHashMap<>()); }
    public void exitScope()   { if (scopes.size() > 1) scopes.pop(); }

    public void define(Symbol symbol) {
        if (!scopes.isEmpty()) scopes.peek().put(symbol.getName(), symbol);
    }

    public Symbol lookup(String name) {
        for (Map<String, Symbol> scope : scopes) {
            if (scope.containsKey(name)) return scope.get(name);
        }
        return null;
    }

    public boolean isDefined(String name) { return lookup(name) != null; }

    public boolean isDefinedInCurrentScope(String name) {
        return !scopes.isEmpty() && scopes.peek().containsKey(name);
    }

    /** طباعة جدول الرموز كاملاً */
    public void print() {
        System.out.println("\n╔══════════════════════════════════════════════════════════╗");
        System.out.println("║              SYMBOL TABLE — " + name);
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        int scopeIdx = scopes.size();
        for (Map<String, Symbol> scope : scopes) {
            System.out.println("║  Scope #" + scopeIdx-- + ":");
            if (scope.isEmpty()) {
                System.out.println("║    (empty)");
            } else {
                for (Symbol sym : scope.values()) {
                    System.out.printf("║    %-12s %-10s %-8s  L%-4d C%d%n",
                            sym.getName(), sym.getKind(), sym.getType(), sym.getLine(), sym.getCol());
                }
            }
        }
        System.out.println("╚══════════════════════════════════════════════════════════╝\n");
    }

    public Map<String, Symbol> getCurrentScope() {
        return scopes.isEmpty() ? Map.of() : Collections.unmodifiableMap(scopes.peek());
    }

    public List<Symbol> getAllSymbols() {
        List<Symbol> all = new ArrayList<>();
        for (Map<String, Symbol> scope : scopes) all.addAll(scope.values());
        return all;
    }
}
