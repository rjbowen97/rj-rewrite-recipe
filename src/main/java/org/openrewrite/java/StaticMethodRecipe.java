package org.openrewrite.java;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
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
        return "Add `static` to non-overridable methods without instance variable usage";
    }

    @Override
    public String getDescription() {
        return "Makes `private` or `final` methods `static` if without references to instance variables. " +
               "When `java.io.Serializable` is implemented by a class, the following methods are excluded from this " +
               "recipe: `private void writeObject(java.io.ObjectOutputStream out) throws IOException;`" +
               ",`private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException;`" +
               ",`private void readObjectNoData() throws ObjectStreamException;`";
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
                                                     .anyMatch(methodMatcher -> methodMatcher.matches(method,
                                                                                                      classDecl
                                                     ));
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

                    if (isInstanceVariableOfEnclosingClass(identifier)) {
                        atomicBoolean.set(true);
                    }

                    return visitIdentifier;
                }
            }.visit(modifiedMethod.getBody(), hasInstanceDataReference);

            return hasInstanceDataReference.get();
        }

        private boolean isInstanceVariableOfEnclosingClass(J.Identifier identifier) {
            J.ClassDeclaration enclosingClass = getCursor().firstEnclosingOrThrow(J.ClassDeclaration.class);

            return enclosingClass.getBody()
                                 .getStatements()
                                 .stream()
                                 .filter(J.VariableDeclarations.class::isInstance)
                                 .map(J.VariableDeclarations.class::cast)
                                 .filter(variableDeclarations -> variableDeclarations.getVariables()
                                                                                     .stream()
                                                                                     .anyMatch(namedVariable -> namedVariable.getName()
                                                                                                                             .getSimpleName()
                                                                                                                             .equals(identifier.getSimpleName())))
                                 .anyMatch(variableDeclarations -> variableDeclarations.getModifiers()
                                                                                       .stream()
                                                                                       .map(J.Modifier::getType)
                                                                                       .noneMatch(type -> type.equals(J.Modifier.Type.Static)));
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
