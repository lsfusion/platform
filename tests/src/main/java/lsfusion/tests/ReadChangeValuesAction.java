package lsfusion.tests;

import com.google.common.base.Throwables;
import lsfusion.base.col.MapFact;
import lsfusion.interop.action.ServerResponse;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.property.Async;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;
import java.util.Iterator;

// the values a client is offered for a change of a property of the form the action runs in, asked for as the client
// asks while the user types into the property (RemoteForm.getAsyncValues) - what a custom view's getValues 'change'
// reads. They are written into cvValues one per line, or CANCELED, which is what the client is answered with when
// there is no list to offer
public class ReadChangeValuesAction extends InternalAction {
    private final ClassPropertyInterface propertyInterface;
    private final ClassPropertyInterface valueInterface;

    public ReadChangeValuesAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
        Iterator<ClassPropertyInterface> interfaces = getOrderInterfaces().iterator();
        propertyInterface = interfaces.next();
        valueInterface = interfaces.next();
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        FormInstance form = context.getFormInstance(false, true);
        Async[] asyncs = form.getAsyncValues(form.getPropertyDraw((String) context.getKeyValue(propertyInterface).getValue()),
                MapFact.EMPTY(), ServerResponse.CHANGE, (String) context.getKeyValue(valueInterface).getValue(), 20, false,
                () -> true, form.context, context.stack);

        StringBuilder values = new StringBuilder();
        for (Async async : asyncs)
            values.append(values.length() > 0 ? "\n" : "").append(async == Async.CANCELED ? "CANCELED" : async.displayString);
        try {
            findProperty("cvValues[]").change(values.toString(), context);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}
