package platform.server.session;

import platform.server.data.types.Type;
import platform.server.data.classes.LogicalClass;
import platform.server.logics.ObjectValue;

import java.sql.SQLException;

public class EqualsChangeProperty implements ChangeProperty {

    DataChangeProperty data;
    ObjectValue value;

    public EqualsChangeProperty(DataChangeProperty data, ObjectValue value) {
        this.data = data;
        this.value = value;
    }

    public Type getType() {
        return LogicalClass.instance;
    }

    public void change(ChangesSession session, Object newValue, boolean externalID) throws SQLException {
        if(newValue==null)
            data.change(session,(Object)null,externalID);
        else {
            assert (Boolean)newValue;
            data.change(session,value,externalID);
        }
    }
}
