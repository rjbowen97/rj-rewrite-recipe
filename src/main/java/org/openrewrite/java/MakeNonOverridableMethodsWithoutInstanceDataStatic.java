package org.openrewrite.java;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.Markers;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.tree.Space.EMPTY;

public class MakeNonOverridableMethodsWithoutInstanceDataStatic extends Recipe {
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
            if (method.hasModifier(J.Modifier.Type.Static) ||
                methodIsOverridable(method) ||
                methodIsInExclusionList(method)) {
                return method;
            }

            if (!methodReferencesInstanceDataOfEnclosingClass(method)) {
                return addStaticModifierTo(method);
            }

            return method;
        }

        private static boolean methodIsOverridable(J.MethodDeclaration method) {
            return !method.hasModifier(J.Modifier.Type.Private) && !method.hasModifier(J.Modifier.Type.Final);
        }

        private boolean methodIsInExclusionList(J.MethodDeclaration method) {
            J.ClassDeclaration enclosingClass = getCursor().firstEnclosingOrThrow(J.ClassDeclaration.class);
            boolean enclosingClassImplementsSerializable = classImplementsSerializable(enclosingClass);

            if (enclosingClassImplementsSerializable) {
                return METHODS_TO_EXCLUDE_FROM_RECIPE.stream()
                                                     .map(MethodMatcher::new)
                                                     .anyMatch(methodMatcher -> methodMatcher.matches(method,
                                                                                                      enclosingClass
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

        private boolean methodReferencesInstanceDataOfEnclosingClass(J.MethodDeclaration methodDeclaration) {
            AtomicBoolean methodReferencesInstanceDataOfEnclosingClass = new AtomicBoolean(false);
            new JavaIsoVisitor<AtomicBoolean>() {
                @Override
                public J.Identifier visitIdentifier(J.Identifier identifier, AtomicBoolean atomicBoolean) {
                    if (identifierIsInstanceVariableOfEnclosingClass(identifier)) {
                        atomicBoolean.set(true);
                    }

                    return identifier;
                }
            }.visit(methodDeclaration.getBody(), methodReferencesInstanceDataOfEnclosingClass);

            return methodReferencesInstanceDataOfEnclosingClass.get();
        }

        private boolean identifierIsInstanceVariableOfEnclosingClass(J.Identifier identifier) {
            J.ClassDeclaration enclosingClass = getCursor().firstEnclosingOrThrow(J.ClassDeclaration.class);

            return getVariableDeclarations(enclosingClass).filter(variablesMatchingSimpleNameOf(identifier))
                                                          .anyMatch(noStaticModifier());
        }

        @NotNull
        private static Predicate<J.VariableDeclarations> variablesMatchingSimpleNameOf(J.Identifier identifier) {
            return variableDeclarations -> variableDeclarations.getVariables()
                                                               .stream()
                                                               .anyMatch(namedVariable -> namedVariable.getName()
                                                                                                       .getSimpleName()
                                                                                                       .equals(identifier.getSimpleName()));
        }

        @NotNull
        private static Predicate<J.VariableDeclarations> noStaticModifier() {
            return variableDeclarations -> !variableDeclarations.hasModifier(J.Modifier.Type.Static);
        }

        private Stream<J.VariableDeclarations> getVariableDeclarations(J.ClassDeclaration classDeclaration) {
            return classDeclaration.getBody()
                                   .getStatements()
                                   .stream()
                                   .filter(J.VariableDeclarations.class::isInstance)
                                   .map(J.VariableDeclarations.class::cast);
        }

        @NotNull
        private static J.MethodDeclaration addStaticModifierTo(J.MethodDeclaration method) {

            J.Modifier staticModifier = new J.Modifier(randomId(),
                                                       EMPTY.withWhitespace(" "),
                                                       Markers.EMPTY,
                                                       J.Modifier.Type.Static,
                                                       emptyList()
            );

            method = method.withModifiers(ListUtils.concat(method.getModifiers(), staticModifier));

            return method;
        }
    }
}
