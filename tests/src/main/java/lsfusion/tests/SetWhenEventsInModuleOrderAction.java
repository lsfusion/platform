package lsfusion.tests;

import com.google.common.base.Throwables;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.event.ChangeEvent;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.data.DataProperty;
import lsfusion.server.logics.property.data.StoredDataProperty;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;

// two modules that do not require each other giving one property events, the later one in the module order writing
// first, as the parse threads' timing can have it : the property keeps the later module's event. Not to change a property
// the server started with, the one written to is made here, and given weData's and weDataLater's events.
// What the test saw goes into weOrderFailure
public class SetWhenEventsInModuleOrderAction extends AtOnceAction {

    public SetWhenEventsInModuleOrderAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            ChangeEvent<?> earlierEvent = ((DataProperty) findProperty("weData[]").property).getEvent();
            ChangeEvent<?> laterEvent = ((DataProperty) findProperty("weDataLater[]").property).getEvent();
            LogicsModule[] modules = independentModules(context.getBL(), LM);

            DataProperty data = new StoredDataProperty(LocalizedString.NONAME, new ValueClass[0], StringClass.text);
            data.setEvent(laterEvent, modules[1].getVersion());
            data.setEvent(earlierEvent, modules[0].getVersion());

            ChangeEvent<?> kept = data.getEvent();
            String failure = kept == laterEvent ? null : "the property kept the event of " + (kept == earlierEvent ?
                    "the module earlier in the module order (" + modules[0].getName() + "), written last, not of " + modules[1].getName() : kept);
            findProperty("weOrderFailure[]").change(failure, context);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}
