package platform.server.session;

import platform.server.data.type.Type;
import platform.server.classes.LogicalClass;
import platform.server.logics.ObjectValue;

import java.sql.SQLException;

public class EqualsChange implements DataChange {

    PropertyChange property;
    ObjectValue value;

    public EqualsChange(PropertyChange property, ObjectValue value) {
        this.property = property;
        this.value = value;
    }

    public Type getType() {
        return LogicalClass.instance;
    }

    public void change(DataSession session, TableModifier<? extends TableChanges> modifier, Object newValue, boolean externalID) throws SQLException {
        if(newValue==null)
            property.change(session, modifier, null,externalID);
        else {
            assert (Boolean)newValue;
            property.change(session,value,externalID, modifier);
        }
    }
}
