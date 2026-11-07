package lsfusion.tests;

import com.google.common.base.Throwables;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.event.ChangeEvent;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.data.DataProperty;
import lsfusion.server.logics.property.data.StoredDataProperty;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;

// two threads giving one property events at once, as two modules parsed at once that write one property by events do.
// The NF property keeping the event is created lazily, and its creation, the check and the write hold the data property's
// monitor, so the test holds it, as a first event would, and a second one has to wait. A started property's event is final,
// so the one written to is made here and given weData's event. Its event is read last. What the test saw goes into weFailure
public class SetWhenEventsAtOnceAction extends AtOnceAction {

    public SetWhenEventsAtOnceAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            ChangeEvent<?> event = ((DataProperty) findProperty("weData[]").property).getEvent();
            DataProperty data = new StoredDataProperty(LocalizedString.NONAME, new ValueClass[0], StringClass.text);
            String failure = event == null ? "weData has no event" :
                    runWhileHeld(context, data, "a thread wrote an event to the property while another one held it", () -> data.setEvent(event, LM.getVersion()));
            if (failure == null && data.getEvent() != event)
                failure = "the property kept the event " + data.getEvent();

            findProperty("weFailure[]").change(failure, context);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}
