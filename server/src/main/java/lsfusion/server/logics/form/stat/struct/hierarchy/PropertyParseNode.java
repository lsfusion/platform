package lsfusion.server.logics.form.stat.struct.hierarchy;

import com.google.common.base.Throwables;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.data.expr.formula.FieldShowIf;
import lsfusion.server.data.expr.formula.JSONField;
import lsfusion.server.logics.classes.data.ParseException;
import lsfusion.server.logics.form.stat.struct.export.hierarchy.json.FormPropertyDataInterface;
import lsfusion.server.logics.form.stat.struct.imports.hierarchy.ImportHierarchicalIterator;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.form.struct.property.PropertyReaderEntity;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public class PropertyParseNode implements ChildParseNode {
    private final PropertyReaderEntity property;
    private final boolean isExclusive;

    public PropertyParseNode(PropertyReaderEntity property, boolean isExclusive) {
        this.property = property;
        this.isExclusive = isExclusive;
    }

    public PropertyReaderEntity getProperty() {
        return property;
    }

    public String getKey() {
        if(property instanceof PropertyDrawEntity) {
          return ((PropertyDrawEntity) property).getIntegrationSID();
        } else {
            return property.getReaderProperty().property.getName();
        }

    }

    @Override
    public JSONField getField() {
        FieldShowIf fieldShowIf = null;
        if(property instanceof PropertyDrawEntity) {
            PropertyReaderEntity showIfProp = ((PropertyDrawEntity) property).getShowIfProp();
            fieldShowIf = showIfProp != null ? FieldShowIf.SHOWIF : ((PropertyDrawEntity) property).extNull ? FieldShowIf.EXTNULL : null;
        }
        return new JSONField(getKey(), fieldShowIf);
    }
    public boolean isAttr() {
        if(property instanceof PropertyDrawEntity) {
            return ((PropertyDrawEntity<?>) property).attr;
        } else {
            return false;
        }
    }

    public <T extends Node<T>> void importNode(T node, ImMap<ObjectEntity, Object> upValues, ImportData importData, ImportHierarchicalIterator iterator) {
        if(property instanceof PropertyDrawEntity) {
            Object propertyValue;
            try {
                propertyValue = node.getValue(getKey(), ((PropertyDrawEntity) property).attr, ((PropertyDrawEntity) property).getImportType());
            } catch (ParseException e) {
                throw Throwables.propagate(e);
            }
            importData.addProperty((PropertyDrawEntity) property, upValues, propertyValue, isExclusive);
        }
    }
    
    public <T extends Node<T>> boolean exportNode(T node, ImMap<ObjectEntity, Object> upValues, ExportData exportData) {
        if(property instanceof PropertyDrawEntity) {
            Object value = exportData.getProperty(property, upValues);
            PropertyReaderEntity showIfProp = ((PropertyDrawEntity) property).getShowIfProp();
            boolean show = showIfProp == null || exportData.getProperty(showIfProp, upValues) != null;
            if ((show && (value != null || ((PropertyDrawEntity) property).extNull))) {
                node.addValue(node, getKey(), ((PropertyDrawEntity) property).attr, value, exportData.getType(((PropertyDrawEntity) property)));
                return true;
            }
        }
        return false;
    }

    @Override
    public <X extends PropertyInterface, P extends PropertyInterface> PropertyMapImplement<?, X> getJSONProperty(FormPropertyDataInterface<P> form, ImRevMap<P, X> mapValues, ImRevMap<ObjectEntity, X> mapObjects, boolean returnString) {
        return property.getReaderProperty().getImplement(mapObjects);
    }
}
