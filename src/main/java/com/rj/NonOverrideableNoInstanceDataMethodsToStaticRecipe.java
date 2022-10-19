package com.rj;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;

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
    protected JavaIsoVisitor<ExecutionContext> getVisitor() {
        // getVisitor() should always return a new instance of the visitor to avoid any state leaking between cycles
        return new NonOverrideableNoInstanceDataMethodsToStaticVisitor();
    }

    public class NonOverrideableNoInstanceDataMethodsToStaticVisitor extends JavaIsoVisitor<ExecutionContext> {
        private final JavaTemplate helloTemplate =
                JavaTemplate.builder(this::getCursor, "public String hello() { return \"Hello from #{}!\"; }")
                        .build();

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {
            // In any visit() method the call to super() is what causes sub-elements of to be visited
            J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, executionContext);

            // Check if the class already has a method named "hello" so we don't incorrectly add a second "hello" method
            boolean helloMethodExists = classDecl.getBody().getStatements().stream()
                    .filter(statement -> statement instanceof J.MethodDeclaration)
                    .map(J.MethodDeclaration.class::cast)
                    .anyMatch(methodDeclaration -> methodDeclaration.getName().getSimpleName().equals("hello"));
            if (helloMethodExists) {
                return cd;
            }

            // Interpolate the fullyQualifiedClassName into the template and use the resulting AST to update the class body
            cd = cd.withBody(
                    cd.getBody().withTemplate(
                            helloTemplate,
                            cd.getBody().getCoordinates().lastStatement()
                    ));

            return cd;
        }
    }
}
