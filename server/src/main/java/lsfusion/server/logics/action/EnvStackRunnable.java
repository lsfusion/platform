package lsfusion.server.logics.action;

import lsfusion.server.logics.action.controller.stack.ExecutionStack;

public interface EnvStackRunnable {

    void run(ExecutionEnvironment env, ExecutionStack stack);

}