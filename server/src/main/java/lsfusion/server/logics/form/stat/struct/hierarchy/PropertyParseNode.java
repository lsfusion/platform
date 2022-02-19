package lsfusion.server.logics.form.stat.struct.hierarchy;

import com.google.common.base.Throwables;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.logics.classes.data.ParseException;
import lsfusion.server.logics.form.stat.struct.export.hierarchy.json.FormPropertyDataInterface;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public class PropertyParseNode implements ChildParseNode {
    private final PropertyDrawEntity<?> property;
    private final boolean isExclusive;

    public PropertyParseNode(PropertyDrawEntity<?> property, boolean isExclusive) {
        this.property = property;
        this.isExclusive = isExclusive;
    }

    public String getKey() {
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
            node.addValue(node, getKey(), property.attr, value, exportData.getType(property));
            return true;
        }
        return false;
    }

    @Override
    public <X extends PropertyInterface, P extends PropertyInterface> PropertyMapImplement<?, X> getJSONProperty(FormPropertyDataInterface<P> form, ImRevMap<P, X> mapValues, ImRevMap<ObjectEntity, X> mapObjects) {
        return property.getValueProperty().getImplement(mapObjects);
    }
}
