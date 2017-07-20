package lsfusion.server;

import lsfusion.server.context.ExecutionStack;
import lsfusion.server.session.ExecutionEnvironment;

public interface EnvStackRunnable {

    void run(ExecutionEnvironment env, ExecutionStack stack);

}