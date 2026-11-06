package lsfusion.tests;

import com.google.common.base.Throwables;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.event.ChangeEvent;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.data.DataProperty;

import java.sql.SQLException;

// two threads giving one property events at once, as two modules parsed at once that write one property by events do.
// An event is written - and checked - on the data property's own monitor, so the test holds it, as a first event would,
// and a second one has to wait. The event written is the one the property has. What the test saw goes into weFailure
public class SetWhenEventsAtOnceAction extends AtOnceAction {

    public SetWhenEventsAtOnceAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            DataProperty data = (DataProperty) findProperty("weData[]").property;
            ChangeEvent<?> event = data.event;
            String failure = event == null ? "the property has no event" :
                    runWhileHeld(context, data, "a thread wrote an event to the property while another one held it", () -> data.setEvent(event));
            if (failure == null && data.event != event)
                failure = "the property kept the event " + data.event;

            findProperty("weFailure[]").change(failure, context);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}
