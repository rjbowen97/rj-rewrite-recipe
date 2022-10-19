package com.rj;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class NonOverrideableNoInstanceDataMethodsToStaticRecipeTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new NonOverrideableNoInstanceDataMethodsToStaticRecipe())
            .parser(JavaParser.fromJavaVersion().logCompilationWarningsAndErrors(true));
    }

    @Test
    void modifyExampleCase() {
        rewriteRun(
            //There is an overloaded version or rewriteRun that allows the RecipeSpec to be customized specifically
            //for a given test. In this case, the parser for this test is configured to not log compilation warnings.
            java("""
                package com.org;
                public class Utilities {
                    private static String magicWord = "magic";

                    private String getMagicWord() {
                        return magicWord;
                    }

                    private void setMagicWord(String value) {
                        magicWord = value;
                    }
                    
                }
                """, """
                package com.org;
                public class Utilities {
                    private static String magicWord = "magic";
                    
                    private static String getMagicWord() {
                        return magicWord;
                    }
                    
                    private static void setMagicWord(String value) {
                        magicWord = value;
                    }

                }
                """));
    }
}