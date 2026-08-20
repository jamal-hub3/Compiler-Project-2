package AST.python;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * عقدة import — تمثّل: from flask import Flask, render_template
 */
public class ImportNode extends PythonNode {
    private final String module;          // e.g. "flask"
    private final List<String> names;     // e.g. ["Flask", "render_template"]
    private final boolean isFrom;         // from X import Y  vs  import X

    public ImportNode(String module, List<String> names, boolean isFrom, int line, int col) {
        super("Import", line, col);
        this.module = module;
        this.names  = new ArrayList<>(names);
        this.isFrom = isFrom;
    }

    public String getModule()         { return module; }
    public List<String> getNames()    { return Collections.unmodifiableList(names); }
    public boolean isFrom()           { return isFrom; }

    @Override
    public void accept(PythonVisitor visitor) { visitor.visit(this); }
}
