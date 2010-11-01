package platform.server.form.instance;

import platform.base.OrderedMap;
import platform.base.Pair;
import platform.server.data.Field;
import platform.server.data.KeyField;
import platform.server.data.PropertyField;
import platform.server.data.SessionTable;
import platform.server.data.where.classes.ClassWhere;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: DAle
 * Date: 27.08.2010
 * Time: 12:44:04
 */

public class ReportTable extends SessionTable<ReportTable> {
    public enum PropertyType {PLAIN, ORDER, CAPTION}
    public final Map<KeyField, ObjectInstance> mapKeys;
    public final OrderedMap<PropertyField, Object> orders;
    public final Map<Pair<Object, PropertyType>, PropertyField> objectsToFields;

    public ReportTable(String name, List<GroupObjectInstance> groups, List<PropertyDrawInstance> props) {
        super("reportTable_" + name);

        mapKeys = new HashMap<KeyField, ObjectInstance>();
        orders = new OrderedMap<PropertyField, Object>();
        objectsToFields = new HashMap<Pair<Object, PropertyType>, PropertyField>();

        int orderID = 0;
        for (GroupObjectInstance group : groups)
        {
            for(Map.Entry<OrderInstance, Boolean> order : group.orders.entrySet()) {
                PropertyField orderProp = new PropertyField("order" + orderID, order.getKey().getType());
                orderID++;
                objectsToFields.put(new Pair<Object, PropertyType>(order.getKey(), PropertyType.ORDER), orderProp);
                orders.put(orderProp, order.getKey());
                properties.add(orderProp);
            }

            for(ObjectInstance object : group.objects) {
                PropertyField objectProp = new PropertyField("obj_order" + object.getsID(), object.getType());
                objectsToFields.put(new Pair<Object, PropertyType>(object, PropertyType.PLAIN), objectProp);
                orders.put(objectProp, object);
                properties.add(objectProp);

                KeyField key = new KeyField("object" + object.getsID(), object.getType());
                mapKeys.put(key, object);
                keys.add(key);
            }
        }

        for(PropertyDrawInstance property : props) {
            if (groups.contains(property.propertyObject.getApplyObject())) {
                PropertyField propField = new PropertyField("property" + property.getsID(), property.propertyObject.getType());
                objectsToFields.put(new Pair<Object, PropertyType>(property, PropertyType.PLAIN), propField);
                properties.add(propField);

                if (property.propertyCaption != null) {
                    PropertyField captionField = new PropertyField("caption" + property.getsID(), property.propertyCaption.getType());
                    objectsToFields.put(new Pair<Object, PropertyType>(property, PropertyType.CAPTION), captionField);
                    properties.add(captionField);
                }
            }
        }
    }

    private ReportTable(ReportTable from, String name, Map<KeyField, ObjectInstance> mapKeys, ClassWhere<KeyField> classes, Map<PropertyField, ClassWhere<Field>> propertyClasses, Map<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>> rows) {
        super(name, classes, propertyClasses, rows);

        this.mapKeys = mapKeys;
        keys.addAll(mapKeys.keySet());
        orders = from.orders;
        objectsToFields = from.objectsToFields;
        properties.addAll(from.properties);
    }

    public ReportTable createThis(ClassWhere<KeyField> classes, Map<PropertyField, ClassWhere<Field>> propertyClasses, Map<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>> rows) {
        return new ReportTable(this, name, mapKeys, classes, propertyClasses, rows);
    }
}
