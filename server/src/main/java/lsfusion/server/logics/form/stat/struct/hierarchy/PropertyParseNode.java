package lsfusion.server.logics.form.stat.struct.hierarchy;

import com.google.common.base.Throwables;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.logics.classes.data.ParseException;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;

public class PropertyParseNode extends ParseNode {
    private final PropertyDrawEntity<?> property;
    private final boolean isExclusive;

    public PropertyParseNode(PropertyDrawEntity<?> property, boolean isExclusive) {
        this.property = property;
        this.isExclusive = isExclusive;
    }

    protected String getKey() {
        return property.getIntegrationSID();
    }

    public <T extends Node<T>> void importNode(T node, ImMap<ObjectEntity, Object> upValues, ImportData importData) {
        Object propertyValue;
        try {
            propertyValue = node.getValue(getKey(), property.attr, property.getType());
        } catch (ParseException e) {
            throw Throwables.propagate(e);
        }
        importData.addProperty(property, upValues, propertyValue, isExclusive);
    }
    
    public <T extends Node<T>> boolean exportNode(T node, ImMap<ObjectEntity, Object> upValues, ExportData exportData) {
        Object value = exportData.getProperty(this.property, upValues);
        if(value != null || property.extNull) {
            node.addValue(node, getKey(), property.attr, property.extNull, value, exportData.getType(property));
            return true;
        }
        return false;
    }
}
