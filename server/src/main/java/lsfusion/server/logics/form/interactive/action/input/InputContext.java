package lsfusion.server.logics.form.interactive.action.input;

import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.action.session.change.modifier.Modifier;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public class InputContext<P extends PropertyInterface> {

    public final InputValueList<P> list;
    public final boolean newSession;
    public final DataSession session;
    public final Modifier modifier;

    public InputContext(InputValueList<P> list, boolean newSession, DataSession session, Modifier modifier) {
        this.list = list;
        this.newSession = newSession;
        this.session = session;
        this.modifier = modifier;
    }
}
