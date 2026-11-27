package lsfusion.server.logics.action.controller.stack;

import lsfusion.server.language.EvalScriptingLogicsModule;
import lsfusion.server.logics.action.session.DataSession;

// the code above runs with this module (ExecutionStack.getEvalLM) : a script's (EvalUtils.findAction), a form's
// (ExecutionContext.createAndRequestFormInstance). Sacrificed for one way to run a script: a script compiled without the
// module of an EVAL it runs under (a debugger's expression) hides that module from an EVAL inside it
public class EvalExecutionStack extends SameThreadExecutionStack {

    private final EvalScriptingLogicsModule evalLM;

    public EvalExecutionStack(EvalScriptingLogicsModule evalLM, ExecutionStack upStack) {
        super(upStack);

        this.evalLM = evalLM;
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
