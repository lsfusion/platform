package lsfusion.server.logics.action;

import lsfusion.server.logics.action.stack.ExecutionStack;

public interface EnvStackRunnable {

    void run(ExecutionEnvironment env, ExecutionStack stack);

}