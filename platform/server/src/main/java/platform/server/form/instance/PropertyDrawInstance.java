package platform.server.form.instance;

import platform.server.form.entity.PropertyDrawEntity;
import platform.server.logics.property.PropertyInterface;
import platform.interop.form.PropertyRead;
import platform.interop.ClassViewType;
import platform.base.BaseUtils;

import java.util.List;
import java.util.Set;
import java.util.Arrays;
import java.util.ArrayList;

// представление св-ва
public class PropertyDrawInstance<P extends PropertyInterface> extends CellInstance<PropertyDrawEntity> implements PropertyReadInstance {

    public PropertyObjectInstance<P> propertyObject;
    public PropertyObjectInstance getPropertyObject() {
        return propertyObject;
    }

    // в какой "класс" рисоваться, ессно один из Object.GroupTo должен быть ToDraw
    public GroupObjectInstance toDraw;
    public List<GroupObjectInstance> columnGroupObjects;

    public byte getTypeID() {
        return PropertyRead.DRAW;
    }

    public List<ObjectInstance> getSerializeGroupList() {
        List<ObjectInstance> result = new ArrayList<ObjectInstance>(); 
        for (GroupObjectInstance columnGroupObject : columnGroupObjects)
            if(columnGroupObject.curClassView == ClassViewType.GRID)
                result.addAll(columnGroupObject.objects);
        return result;
    }

    public List<ObjectInstance> getSerializeList(Set<PropertyDrawInstance> panelProperties) {
        List<ObjectInstance> result = getSerializeGroupList();
        if (!panelProperties.contains(this))
             result = BaseUtils.mergeList((List<ObjectInstance>)toDraw.objects, result);
        return result;
    }

    // предполагается что propertyCaption ссылается на все из propertyObject но без toDraw (хотя опять таки не обязательно)
    public final PropertyObjectInstance<?> propertyCaption;
    // извращенное множественное наследование
    public class Caption implements PropertyReadInstance {
        public PropertyObjectInstance getPropertyObject() {
            return propertyCaption;
        }

        public byte getTypeID() {
            return PropertyRead.CAPTION;
        }

        public int getID() {
            return PropertyDrawInstance.this.getID();
        }

        public List<ObjectInstance> getSerializeList(Set<PropertyDrawInstance> panelProperties) {
            return getSerializeGroupList();
        }
    }
    public Caption caption = new Caption();

    public Byte getForceViewType() {
        return entity.forceViewType;
    }

    public String toString() {
        return propertyObject.toString();
    }

    public PropertyDrawInstance(PropertyDrawEntity<P> entity, PropertyObjectInstance<P> propertyObject, GroupObjectInstance toDraw, List<GroupObjectInstance> columnGroupObjects, PropertyObjectInstance<?> propertyCaption) {
        super(entity);
        this.propertyObject = propertyObject;
        this.toDraw = toDraw;
        this.columnGroupObjects = columnGroupObjects;
        this.propertyCaption = propertyCaption;
    }
}
