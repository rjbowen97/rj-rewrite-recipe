package com.rj;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.concurrent.atomic.AtomicBoolean;

public class StaticMethodRecipe extends Recipe {

    @Override
    public String getDisplayName() {
        return "Private and Final Methods Without Instance Data Access Should be Static";
    }

    @Override
    public String getDescription() {
        return "Non-overridable methods (private or final) that don't access instance data can be static to prevent any misunderstanding about the contract of the method.";
    }

    @Override
    protected StaticMethodVisitor getVisitor() {
        return new StaticMethodVisitor();
    }

    private static class StaticMethodVisitor extends JavaIsoVisitor<ExecutionContext> {
        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext executionContext) {
            J.MethodDeclaration modifiedMethod = super.visitMethodDeclaration(method, executionContext);

            if (methodShouldBeStatic(modifiedMethod)) {
                // TODO - Modify method here
                return modifiedMethod;
            }

            return method;
        }

        @Override
        public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext executionContext) {
            return super.visitVariableDeclarations(multiVariable, executionContext);
        }

        private static boolean methodShouldBeStatic(J.MethodDeclaration modifiedMethod) {
            boolean methodIsNonOverridable = isMethodIsNonOverridable(modifiedMethod);
            boolean methodReferencesInstanceData = doesMethodReferenceInstanceData(modifiedMethod);

            return methodIsNonOverridable && methodReferencesInstanceData;
        }

        private static boolean isMethodIsNonOverridable(J.MethodDeclaration modifiedMethod) {
            return modifiedMethod.getModifiers()
                                 .stream()
                                 .map(J.Modifier::getType)
                                 .anyMatch(type -> type.equals(J.Modifier.Type.Private) || type.equals(J.Modifier.Type.Final));
        }

        private static boolean doesMethodReferenceInstanceData(J.MethodDeclaration modifiedMethod) {
            // TODO - Find static fields and create a list

            // TODO - Use said list to filter visitIdentifier

            AtomicBoolean hasInstanceDataReference = new AtomicBoolean(false);
            new JavaIsoVisitor<AtomicBoolean>() {

                @Override
                public J.Identifier visitIdentifier(J.Identifier identifier, AtomicBoolean atomicBoolean) {

                    return super.visitIdentifier(identifier, atomicBoolean);
                }

                @Override
                public Statement visitStatement(Statement statement, AtomicBoolean atomicBoolean) {
                    Statement visitStatement = super.visitStatement(statement, atomicBoolean);
                    return visitStatement;
                }
            }.visit(modifiedMethod.getBody(), hasInstanceDataReference);

            return hasInstanceDataReference.get();
        }

//        @Override
//        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {
//            J.ClassDeclaration modifiedClassDecl = super.visitClassDeclaration(classDecl, executionContext);
//
//            Stream<J.MethodDeclaration> methodDeclarations = classDecl.getBody()
//                                                                      .getStatements()
//                                                                      .stream()
//                                                                      .filter(statement -> statement instanceof J.MethodDeclaration)
//                                                                      .map(J.MethodDeclaration.class::cast);
//
//            Predicate<J.MethodDeclaration> methodDeclarationIsNonOverrideable = methodDeclaration -> methodDeclaration.getModifiers()
//                                                                                                                      .stream()
//                                                                                                                      .map(J.Modifier::getType)
//                                                                                                                      .anyMatch(
//                                                                                                                              type -> type.equals(
//                                                                                                                                      J.Modifier.Type.Private) || type.equals(
//                                                                                                                                      J.Modifier.Type.Final));
//
//            Stream<J.MethodDeclaration> nonOverridableMethods = methodDeclarations.filter(
//                    methodDeclarationIsNonOverrideable);
//
//            // TODO - Check if current nonOverridableMethod references only static class variables.
//
//            return classDecl;
//        }
    }
}
