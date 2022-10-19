package com.rj;

import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.junit.jupiter.api.Assertions.*;

class NonOverrideableNoInstanceDataMethodsToStaticRecipeTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new NonOverrideableNoInstanceDataMethodsToStaticRecipe());
    }
}