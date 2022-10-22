package com.rj;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.tree.Space.EMPTY;

public class StaticMethodRecipe extends Recipe {
    private static final J.Modifier staticModifier = new J.Modifier(randomId(),
                                                                    EMPTY.withWhitespace(" "),
                                                                    Markers.EMPTY,
                                                                    J.Modifier.Type.Static,
                                                                    emptyList()
    );

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
        private final ArrayList<J.VariableDeclarations> instanceVariables = new ArrayList<>();

        @Override
        public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext executionContext) {
            boolean declaredVariableIsClassLevel = getCursor().firstEnclosing(J.MethodDeclaration.class) == null;
            if (!declaredVariableIsStatic(multiVariable) && declaredVariableIsClassLevel) {
                instanceVariables.add(multiVariable);
            }

            return multiVariable;
        }

        private static boolean declaredVariableIsStatic(J.VariableDeclarations variableDeclarations) {
            return variableDeclarations.getModifiers()
                                       .stream()
                                       .map(J.Modifier::getType)
                                       .anyMatch(type -> type.equals(J.Modifier.Type.Static));
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext executionContext) {
            if (methodShouldBeExcluded(method)) {
                return method;
            }
            if (methodShouldBeStatic(method) && !method.hasModifier(J.Modifier.Type.Static)) {
                return addStaticModifierTo(method);
            }

            return method;
        }

        private boolean methodShouldBeExcluded(J.MethodDeclaration method) {
            J.ClassDeclaration classDecl = getCursor().firstEnclosingOrThrow(J.ClassDeclaration.class);
            boolean enclosingClassImplementsSerializable = classImplementsSerializable(classDecl);

            if (enclosingClassImplementsSerializable) {

                ArrayList<String> excludedMethodExpressions = new ArrayList<>();
                excludedMethodExpressions.add("* writeObject(java.io.ObjectOutputStream)");
                excludedMethodExpressions.add("* readObject(java.io.ObjectInputStream)");
                excludedMethodExpressions.add("* readObjectNoData()");

                return excludedMethodExpressions.stream()
                                                .map(MethodMatcher::new)
                                                .anyMatch(methodMatcher -> methodMatcher.matches(method, classDecl));
            }

            return false;
        }

        private static boolean classImplementsSerializable(J.ClassDeclaration classDecl) {
            boolean enclosingClassImplementsSerializable = false;
            if (classDecl.getImplements() != null) {
                enclosingClassImplementsSerializable = classDecl.getImplements()
                                                                .stream()
                                                                .anyMatch(implement -> TypeUtils.isOfClassType(implement.getType(),
                                                                                                               "java.io.Serializable"
                                                                ));
            }
            return enclosingClassImplementsSerializable;
        }

        private boolean methodShouldBeStatic(J.MethodDeclaration modifiedMethod) {
            return isMethodIsNonOverridable(modifiedMethod) &&
                   !doesMethodReferenceInstanceData(modifiedMethod, this.instanceVariables);
        }

        private static boolean isMethodIsNonOverridable(J.MethodDeclaration modifiedMethod) {
            return modifiedMethod.getModifiers()
                                 .stream()
                                 .map(J.Modifier::getType)
                                 .anyMatch(type -> type.equals(J.Modifier.Type.Private) ||
                                                   type.equals(J.Modifier.Type.Final));
        }

        private static boolean doesMethodReferenceInstanceData(J.MethodDeclaration modifiedMethod, ArrayList<J.VariableDeclarations> instanceDataVariables) {
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
            }.visit(modifiedMethod.getBody(), hasInstanceDataReference);

            return hasInstanceDataReference.get();
        }

        @NotNull
        private static J.MethodDeclaration addStaticModifierTo(J.MethodDeclaration method) {
            List<J.Modifier> modifiers = new ArrayList<>(method.getModifiers());
            modifiers.add(staticModifier);

            method = method.withModifiers(modifiers);

            return method;
        }
    }
}
