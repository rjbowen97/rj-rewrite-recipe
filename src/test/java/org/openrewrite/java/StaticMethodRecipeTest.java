package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class StaticMethodRecipeTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new StaticMethodRecipe())
            .parser(JavaParser.fromJavaVersion().logCompilationWarningsAndErrors(true));
    }

    @Test
    void modifyExampleCase() {
        rewriteRun(
            //There is an overloaded version or rewriteRun that allows the RecipeSpec to be customized specifically
            //for a given test. In this case, the parser for this test is configured to not log compilation warnings.
            java("""
                    import java.io.IOException;
                    import java.io.ObjectStreamException;
                    import java.io.Serializable;
                    
                    public class Utilities implements Serializable {
                        private static String magicWord = "magic";
                        private boolean magicWordIsLocked = true;
                        private static final String helloWorldString = "Hello world!";
                    
                        public boolean a, b = true;
                        public static boolean c, d = true;
                    
                        // STATIC
                        private String getMagicWord() {
                            return magicWord;
                        }
                    
                        // NOT STATIC
                        private String getExtraMagicalWord() {
                            if (a && b) {
                                return "Extra " + magicWord;
                            }
                            return magicWord;
                        }
                    
                        // NOT STATIC
                        private void setMagicWord(String newMagicWord) {
                            if (!magicWordIsLocked) {
                                magicWord = newMagicWord;
                            }
                        }
                    
                        // NOT STATIC
                        public boolean getMagicWordIsLocked() {
                            return magicWordIsLocked;
                        }
                    
                        // NOT STATIC
                        public void setMagicWordIsLocked(boolean magicWordIsLocked) {
                            this.magicWordIsLocked = magicWordIsLocked;
                        }
                    
                        // NOT STATIC
                        public String helloWorld() {
                            return helloWorldString;
                        }
                    
                        // NOT STATIC
                        private void writeObject(java.io.ObjectOutputStream out) throws IOException {
                            out.write(magicWord.getBytes());
                        }
                    
                        // NOT STATIC
                        private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
                            in.defaultReadObject();
                            magicWord = (String) in.readObject();
                        }
                    
                        // NOT STATIC
                        private void readObjectNoData() throws ObjectStreamException {
                            magicWord = "Just make something up!";
                        }
                    
                        private static class Toolbox {
                            private String wrench = "wrench";
                            private static String drill = "drill";
                    
                            // NOT STATIC
                            public String getWrench() {
                                return wrench;
                            }
                    
                            // NOT STATIC
                            public void setWrench(String wrench) {
                                this.wrench = wrench;
                            }
                    
                            // STATIC
                            private String getDrill() {
                                Utilities utilities = new Utilities() {
                    
                                    // NOT STATIC (static anonymous class method declarations are not supported at language level 8)
                                    private void helloWorldFromAnonymousClass() {
                                        System.out.println("hello world from anonymous class!");
                                    }
                                };
                    
                    
                                return Toolbox.drill;
                            }
                    
                            // NOT STATIC
                            public void setDrill(String drill) {
                                Toolbox.drill = drill;
                            }
                    
                            // STATIC
                            private void changeDrill(String drill) {
                                if (c || d) {
                                    Toolbox.drill = drill;
                                }
                            }
                        }
                    }
                """, """
                
                    import java.io.IOException;
                    import java.io.ObjectStreamException;
                    import java.io.Serializable;
                    
                    public class Utilities implements Serializable {
                        private static String magicWord = "magic";
                        private boolean magicWordIsLocked = true;
                        private static final String helloWorldString = "Hello world!";
                    
                        public boolean a, b = true;
                        public static boolean c, d = true;
                    
                        // STATIC
                        private static String getMagicWord() {
                            return magicWord;
                        }
                    
                        // NOT STATIC
                        private String getExtraMagicalWord() {
                            if (a && b) {
                                return "Extra " + magicWord;
                            }
                            return magicWord;
                        }
                    
                        // NOT STATIC
                        private void setMagicWord(String newMagicWord) {
                            if (!magicWordIsLocked) {
                                magicWord = newMagicWord;
                            }
                        }
                    
                        // NOT STATIC
                        public boolean getMagicWordIsLocked() {
                            return magicWordIsLocked;
                        }
                    
                        // NOT STATIC
                        public void setMagicWordIsLocked(boolean magicWordIsLocked) {
                            this.magicWordIsLocked = magicWordIsLocked;
                        }
                    
                        // NOT STATIC
                        public String helloWorld() {
                            return helloWorldString;
                        }
                    
                        // NOT STATIC
                        private void writeObject(java.io.ObjectOutputStream out) throws IOException {
                            out.write(magicWord.getBytes());
                        }
                    
                        // NOT STATIC
                        private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
                            in.defaultReadObject();
                            magicWord = (String) in.readObject();
                        }
                    
                        // NOT STATIC
                        private void readObjectNoData() throws ObjectStreamException {
                            magicWord = "Just make something up!";
                        }
                    
                        private static class Toolbox {
                            private String wrench = "wrench";
                            private static String drill = "drill";
                    
                            // NOT STATIC
                            public String getWrench() {
                                return wrench;
                            }
                    
                            // NOT STATIC
                            public void setWrench(String wrench) {
                                this.wrench = wrench;
                            }
                    
                            // STATIC
                            private static String getDrill() {
                                Utilities utilities = new Utilities() {
                    
                                    // NOT STATIC (static anonymous class method declarations are not supported at language level 8)
                                    private void helloWorldFromAnonymousClass() {
                                        System.out.println("hello world from anonymous class!");
                                    }
                                };
                    
                    
                                return Toolbox.drill;
                            }
                    
                            // NOT STATIC
                            public void setDrill(String drill) {
                                Toolbox.drill = drill;
                            }
                    
                            // STATIC
                            private static void changeDrill(String drill) {
                                if (c || d) {
                                    Toolbox.drill = drill;
                                }
                            }
                        }
                    }
                """));
    }
}