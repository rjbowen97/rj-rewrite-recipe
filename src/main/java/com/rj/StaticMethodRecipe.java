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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

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

    private static final Collection<String> METHODS_TO_EXCLUDE_FROM_RECIPE = Arrays.asList(
            "* writeObject(java.io.ObjectOutputStream)",
            "* readObject(java.io.ObjectInputStream)",
            "* readObjectNoData()"
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
        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext executionContext) {
            if (methodIsStatic(method) || methodIsInExclusionList(method)) {
                return method;
            }

            if (methodShouldBeStatic(method)) {
                return addStaticModifierTo(method);
            }

            return method;
        }

        private static boolean methodIsStatic(J.MethodDeclaration method) {
            return method.hasModifier(J.Modifier.Type.Static);
        }

        private boolean methodIsInExclusionList(J.MethodDeclaration method) {
            J.ClassDeclaration classDecl = getCursor().firstEnclosingOrThrow(J.ClassDeclaration.class);
            boolean enclosingClassImplementsSerializable = classImplementsSerializable(classDecl);

            if (enclosingClassImplementsSerializable) {
                return METHODS_TO_EXCLUDE_FROM_RECIPE.stream()
                                                     .map(MethodMatcher::new)
                                                     .anyMatch(methodMatcher -> methodMatcher.matches(method, classDecl));
            }

            return false;
        }

        private static boolean classImplementsSerializable(J.ClassDeclaration classDecl) {
            if (classDecl.getImplements() != null) {
                return classDecl.getImplements()
                                .stream()
                                .anyMatch(implement -> TypeUtils.isOfClassType(implement.getType(),
                                                                               "java.io.Serializable"
                                ));
            }
            return false;
        }

        private boolean methodShouldBeStatic(J.MethodDeclaration modifiedMethod) {
            return hasPrivateOrFinal(modifiedMethod.getModifiers()) && !doesMethodReferenceInstanceData(modifiedMethod);
        }

        private static boolean hasPrivateOrFinal(List<J.Modifier> modifiers) {
            return modifiers.stream()
                            .map(J.Modifier::getType)
                            .anyMatch(type -> type.equals(J.Modifier.Type.Private) ||
                                              type.equals(J.Modifier.Type.Final));
        }

        private boolean doesMethodReferenceInstanceData(J.MethodDeclaration modifiedMethod) {
            AtomicBoolean hasInstanceDataReference = new AtomicBoolean(false);
            new JavaIsoVisitor<AtomicBoolean>() {
                @Override
                public J.Identifier visitIdentifier(J.Identifier identifier, AtomicBoolean atomicBoolean) {
                    J.Identifier visitIdentifier = super.visitIdentifier(identifier, atomicBoolean);

                    if (identifierIsInstanceVariable(identifier)) {
                        atomicBoolean.set(true);
                    }

                    return visitIdentifier;
                }
            }.visit(modifiedMethod.getBody(), hasInstanceDataReference);

            return hasInstanceDataReference.get();
        }

        private boolean identifierIsInstanceVariable(J.Identifier identifier) {
            J.ClassDeclaration classDecl = getCursor().firstEnclosingOrThrow(J.ClassDeclaration.class);

            return classDecl.getBody()
                            .getStatements()
                            .stream()
                            .filter(J.VariableDeclarations.class::isInstance)
                            .map(J.VariableDeclarations.class::cast)
                            .filter(variableDeclarations -> variableDeclarations.getModifiers()
                                                                                .stream()
                                                                                .map(J.Modifier::getType)
                                                                                .noneMatch(type -> type.equals(J.Modifier.Type.Static)))
                            .flatMap(variableDeclarations -> variableDeclarations.getVariables().stream())
                            .anyMatch(namedVariable -> namedVariable.getName()
                                                                    .getSimpleName()
                                                                    .equals(identifier.getSimpleName()));
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
