package platform.server.integration;

import platform.server.classes.ConcreteCustomClass;
import platform.server.logics.DataObject;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyImplement;
import platform.server.logics.property.PropertyInterface;
import platform.server.session.DataSession;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * User: DAle
 * Date: 24.12.10
 * Time: 16:33
 */

public class ImportKey <P extends PropertyInterface> implements ImportKeyInterface {
    private ConcreteCustomClass keyClass;
    private PropertyImplement<ImportFieldInterface, P> property;

    public ImportKey(ConcreteCustomClass keyClass, PropertyImplement<ImportFieldInterface, P> property) {
        this.keyClass = keyClass;
        this.property = property;
    }

    public ConcreteCustomClass getCustomClass() {
        return keyClass;
    }

    public Map<P, ImportFieldInterface> getMapping() {
        return property.mapping;
    }

    public Property<P> getProperty() {
        return property.property;
    }

    public Map<P, DataObject> mapObjects(ImportTable.Row row) {
        Map<P, DataObject> map = new HashMap<P, DataObject>();
        for (Map.Entry<P, ImportFieldInterface> entry : getMapping().entrySet()) {
            DataObject obj = entry.getValue().getDataObject(row);
            map.put(entry.getKey(), obj);
        }
        return map;
    }

    Object readValue(DataSession session, ImportTable.Row row) throws SQLException {
        return getProperty().read(session.sql, mapObjects(row), session.modifier, session.env);
    }

    void writeValue(DataSession session, ImportTable.Row row, DataObject obj) throws SQLException {
        getProperty().execute(mapObjects(row), session, obj.object, session.modifier);
    }

}
