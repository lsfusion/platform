package lsfusion.server.logics.form.interactive.action.async;

import java.io.Serializable;

public class InputList implements Serializable {

    public final InputListAction[] actions;
    public final boolean strict;

    public InputList(InputListAction[] actions, boolean strict) {
        this.actions = actions;
        this.strict = strict;
    }
}
