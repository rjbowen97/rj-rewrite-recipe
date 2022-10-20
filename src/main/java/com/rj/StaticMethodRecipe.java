package com.rj;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

import java.util.function.Predicate;
import java.util.stream.Stream;

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
            }

            return method;
        }

        private static boolean methodShouldBeStatic(J.MethodDeclaration modifiedMethod) {
            boolean methodIsNonOverridable = isMethodIsNonOverridable(modifiedMethod);
            boolean methodReferencesInstanceData = doesMethodReferenceInstanceData(modifiedMethod);

            return methodIsNonOverridable;
        }

        private static boolean isMethodIsNonOverridable(J.MethodDeclaration modifiedMethod) {
            return modifiedMethod.getModifiers()
                                 .stream()
                                 .map(J.Modifier::getType)
                                 .anyMatch(type -> type.equals(J.Modifier.Type.Private) || type.equals(J.Modifier.Type.Final));
        }

        private static boolean doesMethodReferenceInstanceData(J.MethodDeclaration modifiedMethod) {
            // TODO - Implement
            return true;
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {
            J.ClassDeclaration modifiedClassDecl = super.visitClassDeclaration(classDecl, executionContext);

            Stream<J.MethodDeclaration> methodDeclarations = classDecl.getBody()
                                                                      .getStatements()
                                                                      .stream()
                                                                      .filter(statement -> statement instanceof J.MethodDeclaration)
                                                                      .map(J.MethodDeclaration.class::cast);

            Predicate<J.MethodDeclaration> methodDeclarationIsNonOverrideable = methodDeclaration -> methodDeclaration.getModifiers()
                                                                                                                      .stream()
                                                                                                                      .map(J.Modifier::getType)
                                                                                                                      .anyMatch(
                                                                                                                              type -> type.equals(
                                                                                                                                      J.Modifier.Type.Private) || type.equals(
                                                                                                                                      J.Modifier.Type.Final));

            Stream<J.MethodDeclaration> nonOverridableMethods = methodDeclarations.filter(
                    methodDeclarationIsNonOverrideable);

            // TODO - Check if current nonOverridableMethod references only static class variables.

            return classDecl;
        }
    }
}
