package lsfusion.server.logics.property.actions;

import lsfusion.interop.remote.CallbackMessage;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.view.DefaultFormView;
import lsfusion.server.form.view.PropertyDrawView;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.SystemEventsLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;

import java.sql.SQLException;

public class DisconnectActionProperty extends ScriptingActionProperty {

    public DisconnectActionProperty(SystemEventsLogicsModule lm, ValueClass... classes) throws ScriptingErrorLog.SemanticErrorException {
        super(lm, classes);
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        DataObject connection = context.getDataKeyValue(getOrderInterfaces().get(0));

        Integer user = ((Integer) context.getBL().systemEventsLM.userConnection.read(context, connection));
        Integer computer = (Integer) context.getBL().systemEventsLM.computerConnection.read(context, connection);

        context.getNavigatorsManager().forceDisconnect(context.stack, user, computer, CallbackMessage.DISCONNECTED);
    }

    @Override
    public void proceedDefaultDesign(PropertyDrawView propertyView, DefaultFormView view) {
        super.proceedDefaultDesign(propertyView, view);
        propertyView.design.setImagePath("disconnect.png");
    }
}
