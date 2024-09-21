package lsfusion.server.logics.form.interactive.action.input;

import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public class InputContext<P extends PropertyInterface> {

    public final InputValueList<?> list;
    public final boolean newSession;
    public final ExecutionContext<?> context;
    public final boolean strict;

    public InputContext(InputValueList<?> list, boolean newSession, ExecutionContext<?> context, boolean strict) {
        this.list = list;
        this.newSession = newSession;
        this.context = context;
        this.strict = strict;
    }
}
