package platform.server.session;

import platform.base.BaseUtils;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.properties.DataProperty;
import platform.server.logics.properties.DataPropertyInterface;
import platform.server.data.types.Type;
import platform.server.data.types.TypeSerializer;
import platform.server.data.classes.DataClass;

import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

public class DataChangeProperty implements ChangeProperty {
    public final DataProperty property;
    public final Map<DataPropertyInterface,DataObject> mapping;

    public DataChangeProperty(DataProperty property, Map<DataPropertyInterface, DataObject> mapping) {
        this.property = property;
        this.mapping = mapping;
    }

    public void change(ChangesSession session, Object newValue, boolean externalID) throws SQLException {
        session.changeProperty(property, mapping, newValue, externalID);
    }

    public void change(ChangesSession session, ObjectValue newValue, boolean externalID) throws SQLException {
        session.changeProperty(property, mapping, newValue, externalID);
    }

    public Type getType() {
        return property.getType();
    }
}
