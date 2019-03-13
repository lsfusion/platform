package lsfusion.server.logics.form.stat.integration.hierarchy;

import com.google.common.base.Throwables;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.type.ParseException;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.entity.PropertyDrawEntity;

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
    
    public <T extends Node<T>> void exportNode(T node, ImMap<ObjectEntity, Object> upValues, ExportData exportData) {
        Object value = exportData.getProperty(this.property, upValues);
        if(value != null)
            node.addValue(node, getKey(), property.attr, value, exportData.getType(property));
    }
}
