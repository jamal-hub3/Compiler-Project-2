package AST.python;

import AST.python.expr.*;

/**
 * Visitor interface لعقد Python AST.
 * كل نوع عقدة لها method زيارة مخصصة (Polymorphism).
 */
public interface PythonVisitor {
    // Statements
    void visit(ProgramNode node);
    void visit(ImportNode node);
    void visit(AssignmentNode node);
    void visit(FunctionDefNode node);
    void visit(DecoratorNode node);
    void visit(ReturnNode node);
    void visit(ExpressionStatementNode node);
    void visit(IfStatementNode node);
    void visit(ForStatementNode node);
    void visit(AppendStatementNode node);

    // Expressions
    void visit(StringLiteralNode node);
    void visit(NumberLiteralNode node);
    void visit(BoolLiteralNode node);
    void visit(NoneLiteralNode node);
    void visit(IdentifierNode node);
    void visit(ListLiteralNode node);
    void visit(DictLiteralNode node);
    void visit(FunctionCallNode node);
    void visit(AttributeAccessNode node);
    void visit(BinaryOpNode node);
}
