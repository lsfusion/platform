package platform.server.form.instance;

import platform.server.form.entity.PropertyDrawEntity;
import platform.server.logics.property.PropertyInterface;

// представление св-ва
public class PropertyDrawInstance<P extends PropertyInterface> extends CellInstance<PropertyDrawEntity> {

    public PropertyObjectInstance<P> propertyObject;

    // в какой "класс" рисоваться, ессно один из Object.GroupTo должен быть ToDraw
    public GroupObjectInstance toDraw;
    public GroupObjectInstance[] columnGroupObjects;

    public Byte getForceViewType() {
        return entity.forceViewType;
    }

    public String toString() {
        return propertyObject.toString();
    }

    public PropertyDrawInstance(PropertyDrawEntity<P> entity, PropertyObjectInstance<P> propertyObject, GroupObjectInstance toDraw, GroupObjectInstance[] columnGroupObjects) {
        super(entity);
        this.propertyObject = propertyObject;
        this.toDraw = toDraw;
        this.columnGroupObjects = columnGroupObjects;
    }
}
