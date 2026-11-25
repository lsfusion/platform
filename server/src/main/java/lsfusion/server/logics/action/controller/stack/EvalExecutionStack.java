package lsfusion.server.logics.action.controller.stack;

import lsfusion.server.language.EvalScriptingLogicsModule;
import lsfusion.server.logics.action.session.DataSession;

// the code of a script runs in a stack of its module, wherever the script is run. Sacrificed for one way to run a
// script: a script compiled without the module of an EVAL it runs under (one run on a form that EVAL showed, a
// debugger's expression) hides that module from an EVAL inside it
public class EvalExecutionStack extends SameThreadExecutionStack {

    private final EvalScriptingLogicsModule evalLM;

    public EvalExecutionStack(EvalScriptingLogicsModule evalLM, ExecutionStack upStack) {
        super(upStack);

        this.evalLM = evalLM;

        evalLM.addWrapped(); // the module's code starts running
    }

    @Override
    protected DataSession getSession() { // nothing of a session is kept here : the context right below checks the session the script runs in
        return null;
    }

    @Override
    public EvalScriptingLogicsModule getEvalLM() {
        return evalLM;
    }
}
