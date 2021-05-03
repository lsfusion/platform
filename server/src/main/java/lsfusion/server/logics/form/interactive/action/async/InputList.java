package lsfusion.server.logics.form.interactive.action.async;

import java.io.Serializable;

public class InputList implements Serializable {

    public final String[] actions;
    public final AsyncExec[] actionAsyncs; // it's an asyncexec and not asynceventexec, since in continueDispatching there is no push infrastructure so far (and it's not clear if it's needed at all)
    public final boolean strict;

    public InputList(String[] actions, AsyncExec[] actionAsyncs, boolean strict) {
        this.actions = actions;
        this.actionAsyncs = actionAsyncs;
        this.strict = strict;

        assert actions.length == actionAsyncs.length;
    }
}
