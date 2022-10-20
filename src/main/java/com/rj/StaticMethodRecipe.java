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

            boolean classHasNonOverridableMethods = methodDeclarations.anyMatch(methodDeclarationIsNonOverrideable);

            if (classHasNonOverridableMethods) {
                // TODO - Insert method checks here (checking for methods with references only to static class variables)
                return modifiedClassDecl;
            }

            return classDecl;
        }
    }
}
