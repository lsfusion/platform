package platform.server.integration;

import platform.base.BaseUtils;
import platform.server.logics.DataObject;
import platform.server.logics.property.PropertyImplement;
import platform.server.logics.property.PropertyInterface;
import platform.server.session.DataSession;

import java.sql.SQLException;
import java.util.Map;

/**
 * User: DAle
 * Date: 27.01.11
 * Time: 18:11
 */

public class ImportProperty <P extends PropertyInterface> {
    private PropertyImplement<ImportKey, P> property;
    private PropertyImplement<ImportKey, P> converter;

    public ImportProperty(PropertyImplement<ImportKey, P> property) {
        this.property = property;
    }

    public ImportProperty(PropertyImplement<ImportKey, P> property, PropertyImplement<ImportKey, P> converter) {
        this.property = property;
        this.converter = converter;
    }

    public PropertyImplement<ImportKey, P> getProperty() {
        return property;
    }

    public PropertyImplement<ImportKey, P> getConverter() {
        return converter;
    }

    Object convertValue(DataSession session, Map<ImportKey, DataObject> keyValues) throws SQLException {
        Map<P, DataObject> mapping = BaseUtils.join(getConverter().mapping, keyValues);
        return converter.property.read(session.sql, mapping, session.modifier, session.env);
    }

    void writeValue(DataSession session, Map<ImportKey, DataObject> keyValues, Object value) throws SQLException {
        Map<P, DataObject> mapping = BaseUtils.join(getProperty().mapping, keyValues);
//      todo [dale]: временная затычка
        for(Map.Entry<P, DataObject> mapEntry : mapping.entrySet())
            if(mapEntry.getValue()==null)
                mapEntry.setValue((DataObject)(Object)property.mapping.get(mapEntry.getKey()));

        getProperty().property.execute(mapping, session, value, session.modifier);
    }

}
