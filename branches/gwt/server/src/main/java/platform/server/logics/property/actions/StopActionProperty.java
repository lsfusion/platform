package platform.server.logics.property.actions;

import platform.interop.action.ClientAction;
import platform.interop.action.MessageClientAction;
import platform.interop.action.StopAutoActionsClientAction;
import platform.server.classes.ValueClass;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.session.Changes;
import platform.server.session.DataSession;
import platform.server.session.Modifier;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class StopActionProperty extends ActionProperty {

    private String header;

    public StopActionProperty(String sID, String caption, String header) {
        super(sID, caption, new ValueClass[] {});

        this.header = header;
    }

    @Override
    public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, DataSession session, Modifier<? extends Changes> modifier, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects, boolean groupLast) throws SQLException {
        actions.add(new MessageClientAction(caption, header));
        actions.add(new StopAutoActionsClientAction());
    }
}
