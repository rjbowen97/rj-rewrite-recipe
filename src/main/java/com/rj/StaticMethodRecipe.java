package com.rj;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.tree.Space.EMPTY;

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
        private final J.Modifier staticModifier = new J.Modifier(randomId(),
                                                                 EMPTY.withWhitespace(" "),
                                                                 Markers.EMPTY,
                                                                 J.Modifier.Type.Static,
                                                                 emptyList()
        );

        private ArrayList<J.VariableDeclarations> instanceDataVariables = new ArrayList<>();

        @Override
        public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext executionContext) {

            J.VariableDeclarations variableDeclarations = super.visitVariableDeclarations(multiVariable,
                                                                                          executionContext
            );

            boolean declaredVariableIsStatic = variableDeclarations.getModifiers()
                                                                   .stream()
                                                                   .map(J.Modifier::getType)
                                                                   .anyMatch(type -> type.equals(J.Modifier.Type.Static));

            if (!declaredVariableIsStatic && (getCursor().firstEnclosing(J.MethodDeclaration.class) == null)) {
                instanceDataVariables.add(variableDeclarations);
            }

            return variableDeclarations;
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext executionContext) {
            J.MethodDeclaration modifiedMethod = super.visitMethodDeclaration(method, executionContext);

            if (methodShouldBeStatic(modifiedMethod) && !method.hasModifier(J.Modifier.Type.Static)) {
                List<J.Modifier> modifiers = new ArrayList<>(modifiedMethod.getModifiers());
                modifiers.add(staticModifier);

                modifiedMethod = modifiedMethod.withModifiers(modifiers);

                return modifiedMethod;
            }

            return method;
        }

        private boolean methodShouldBeStatic(J.MethodDeclaration modifiedMethod) {
            boolean methodIsNonOverridable = isMethodIsNonOverridable(modifiedMethod);
            boolean methodReferencesInstanceData = doesMethodReferenceInstanceData(modifiedMethod,
                                                                                   this.instanceDataVariables
            );

            return methodIsNonOverridable && !methodReferencesInstanceData;
        }

        private static boolean isMethodIsNonOverridable(J.MethodDeclaration modifiedMethod) {
            return modifiedMethod.getModifiers()
                                 .stream()
                                 .map(J.Modifier::getType)
                                 .anyMatch(type -> type.equals(J.Modifier.Type.Private) || type.equals(J.Modifier.Type.Final));
        }

        private static boolean doesMethodReferenceInstanceData(J.MethodDeclaration modifiedMethod, ArrayList<J.VariableDeclarations> instanceDataVariables) {
            // TODO - Find static fields and create a list

            // TODO - Use said list to filter visitIdentifier

            List<String> instanceDataVariablesSimpleNames = instanceDataVariables.stream()
                                                                                 .flatMap(variableDeclarations -> variableDeclarations.getVariables()
                                                                                                                                      .stream()
                                                                                                                                      .map(J.VariableDeclarations.NamedVariable::getSimpleName))
                                                                                 .collect(Collectors.toList());

            AtomicBoolean hasInstanceDataReference = new AtomicBoolean(false);
            new JavaIsoVisitor<AtomicBoolean>() {

                @Override
                public J.Identifier visitIdentifier(J.Identifier identifier, AtomicBoolean atomicBoolean) {
                    J.Identifier visitIdentifier = super.visitIdentifier(identifier, atomicBoolean);

                    if (instanceDataVariablesSimpleNames.contains(visitIdentifier.getSimpleName())) {
                        atomicBoolean.set(true);
                    }

                    return visitIdentifier;
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
