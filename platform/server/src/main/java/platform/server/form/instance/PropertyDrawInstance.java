package platform.server.form.instance;

import platform.server.logics.property.PropertyInterface;

// представление св-ва
public class PropertyDrawInstance<P extends PropertyInterface> extends CellInstance {

    public PropertyObjectInstance<P> propertyObject;

    // в какой "класс" рисоваться, ессно одмн из Object.GroupTo должен быть ToDraw
    public GroupObjectInstance toDraw;

    public byte forceViewType;

    public String toString() {
        return propertyObject.toString();
    }

    public PropertyDrawInstance(int ID, String sID, PropertyObjectInstance<P> propertyObject, GroupObjectInstance toDraw, byte forceViewType) {
        super(ID,sID);
        this.propertyObject = propertyObject;
        this.toDraw = toDraw;
        this.forceViewType = forceViewType;
    }
}
