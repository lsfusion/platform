package lsfusion.server.logics.action.controller.stack;

import lsfusion.server.logics.action.controller.context.ExecutionEnvironment;

public interface EnvStackRunnable {

    void run(ExecutionEnvironment env, ExecutionStack stack);

}