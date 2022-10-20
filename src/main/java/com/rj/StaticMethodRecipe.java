package com.rj;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.List;

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
                                                                 emptyList());



        @Override
        public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, ExecutionContext executionContext) {
            return super.visitVariable(variable, executionContext);
        }

        @Override
        public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext executionContext) {
            return super.visitVariableDeclarations(multiVariable, executionContext);
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext c) {
            if (method.hasModifier(J.Modifier.Type.Static)) {
                return method;
            }

            if (method.hasModifier(J.Modifier.Type.Private) || method.hasModifier(J.Modifier.Type.Final)) {
                List<J.Modifier> modifiers = new ArrayList<>(method.getModifiers());
                modifiers.add(staticModifier);

                method = method.withModifiers(modifiers);
            }

            return method;
        }
    }
}
