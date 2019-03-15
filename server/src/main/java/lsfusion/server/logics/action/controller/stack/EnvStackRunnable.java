package lsfusion.server.logics.action.controller.stack;

import lsfusion.server.logics.action.controller.context.ExecutionEnvironment;
import lsfusion.server.logics.action.controller.stack.ExecutionStack;

public interface EnvStackRunnable {

    void run(ExecutionEnvironment env, ExecutionStack stack);

}