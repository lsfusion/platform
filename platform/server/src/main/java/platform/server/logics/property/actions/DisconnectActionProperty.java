package platform.server.logics.property.actions;

import platform.base.Pair;
import platform.server.classes.ValueClass;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.PropertyDrawView;
import platform.server.logics.DataObject;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;

import java.sql.SQLException;

import static platform.server.logics.ServerResourceBundle.getString;

public class DisconnectActionProperty extends AdminActionProperty {

    public DisconnectActionProperty(ValueClass dictionary) {
        super("disconnectConnection", getString("logics.connection.disconnect"), new ValueClass[] {dictionary});
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        DataObject connection = context.getDataKeyValue(getOrderInterfaces().get(0));

        String login = ((String) context.getBL().systemEventsLM.userLoginConnection.read(context, connection)).trim();
        Integer computer = (Integer) context.getBL().systemEventsLM.computerConnection.read(context, connection);
        Pair<String, Integer> key = new Pair<String, Integer>(login, computer);

        context.getNavigatorsManager().cutOffConnection(key);
    }

    @Override
    public void proceedDefaultDesign(PropertyDrawView propertyView, DefaultFormView view) {
        super.proceedDefaultDesign(propertyView, view);
        propertyView.design.setIconPath("disconnect.png");
    }
}
