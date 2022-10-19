package com.rj;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.Markers;

import static java.util.Collections.emptyList;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.tree.Space.EMPTY;

public class NonOverrideableNoInstanceDataMethodsToStaticRecipe extends Recipe {

    @Override
    public String getDisplayName() {
        return "Private and Final Methods Without Instance Data Access Should be Static";
    }

    @Override
    public String getDescription() {
        return "Non-overridable methods (private or final) that don't access instance data can be static to prevent any misunderstanding about the contract of the method.";
    }

    @Override
    protected NonOverrideableNoInstanceDataMethodsToStaticVisitor getVisitor() {
        return new NonOverrideableNoInstanceDataMethodsToStaticVisitor();
    }

    private static class NonOverrideableNoInstanceDataMethodsToStaticVisitor extends JavaIsoVisitor<ExecutionContext> {
        private final MethodMatcher methodMatcher = new MethodMatcher("*..* *(..)");

        private final J.Modifier staticModifier = new J.Modifier(randomId(),
                                                                 EMPTY,
                                                                 Markers.EMPTY,
                                                                 J.Modifier.Type.Static,
                                                                 emptyList());

        private final JavaTemplate currentMethod = JavaTemplate
                .builder(this::getCursor, staticModifier.toString()).build();

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext c) {
            if (!methodMatcher.matches(method.getMethodType())) {
                return method;
            }

            J.Modifier.Type currentMethodIsStatic = method.getModifiers()
                                                          .stream()
                                                          .map(J.Modifier::getType)
                                                          .filter(type -> type == J.Modifier.Type.Static)
                                                          .findAny()
                                                          .orElse(null);

            if (currentMethodIsStatic == J.Modifier.Type.Static) {
                return method;
            }

            method = method.withModifiers(ListUtils.concat(staticModifier, method.getModifiers()));
//            method = method.withTemplate(currentMethod, method.getBody().getCoordinates().lastStatement());

            return method;
        }
    }
}
