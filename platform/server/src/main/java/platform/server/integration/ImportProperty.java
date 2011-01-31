package platform.server.integration;

import platform.base.BaseUtils;
import platform.server.logics.DataObject;
import platform.server.logics.property.PropertyImplement;
import platform.server.logics.property.PropertyInterface;
import platform.server.session.DataSession;

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * User: DAle
 * Date: 27.01.11
 * Time: 18:11
 */

public class ImportProperty <P extends PropertyInterface> {
    private PropertyImplement<ImportKeyInterface, P> property;
    private PropertyImplement<ImportKeyInterface, P> converter;

    public ImportProperty(PropertyImplement<ImportKeyInterface, P> property) {
        this.property = property;
    }

    public ImportProperty(PropertyImplement<ImportKeyInterface, P> property, PropertyImplement<ImportKeyInterface, P> converter) {
        this.property = property;
        this.converter = converter;
    }

    public PropertyImplement<ImportKeyInterface, P> getProperty() {
        return property;
    }

    public PropertyImplement<ImportKeyInterface, P> getConverter() {
        return converter;
    }

    Object convertValue(DataSession session, Map<ImportKeyInterface, DataObject> keyValues) throws SQLException {
        Map<P, DataObject> mapping =
                BaseUtils.join(getConverter().mapping, createMapping(getConverter().mapping.values(), keyValues));
        return converter.property.read(session.sql, mapping, session.modifier, session.env);
    }

    void writeValue(DataSession session, Map<ImportKeyInterface, DataObject> keyValues, Object value) throws SQLException {
        Map<P, DataObject> mapping =
                BaseUtils.join(getProperty().mapping, createMapping(getProperty().mapping.values(), keyValues));
        getProperty().property.execute(mapping, session, value, session.modifier);
    }

    private Map<ImportKeyInterface, DataObject> createMapping(Collection<ImportKeyInterface> interfaces, Map<ImportKeyInterface, DataObject> keyValues) {
        Map<ImportKeyInterface, DataObject> mapping = new HashMap<ImportKeyInterface, DataObject>(keyValues);
        for (ImportKeyInterface iface : interfaces) {
            if (!mapping.containsKey(iface)) {
                mapping.put(iface, (DataObject) iface);
            }
        }
        return mapping;
    }
}
