package lsfusion.server.logics.property.actions;

import lsfusion.base.Pair;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.form.view.DefaultFormView;
import lsfusion.server.form.view.PropertyDrawView;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.SystemEventsLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;

import java.sql.SQLException;

public class DisconnectActionProperty extends ScriptingActionProperty {

    public DisconnectActionProperty(SystemEventsLogicsModule lm) {
        super(lm, new ValueClass[] {lm.getClassByName("Connection")});
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        DataObject connection = context.getDataKeyValue(getOrderInterfaces().get(0));

        String login = ((String) context.getBL().systemEventsLM.userLoginConnection.read(context, connection)).trim();
        Integer computer = (Integer) context.getBL().systemEventsLM.computerConnection.read(context, connection);
        Pair<String, Integer> key = new Pair<String, Integer>(login, computer);

        context.getNavigatorsManager().forceDisconnect(key);
    }

    @Override
    public void proceedDefaultDesign(PropertyDrawView propertyView, DefaultFormView view) {
        super.proceedDefaultDesign(propertyView, view);
        propertyView.design.setIconPath("disconnect.png");
    }
}
