package lsfusion.tests;

// a class that is no action and says when it is initialized : eval/EvalCompileRunsNothing names it in an INTERNAL
public class InitializedProbe {
    static {
        System.setProperty("lsfusion.tests.InitializedProbe", "initialized");
    }
}
