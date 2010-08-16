package platform.server.form.entity;

import platform.interop.ClassViewType;
import platform.server.logics.property.PropertyInterface;

public class PropertyDrawEntity<P extends PropertyInterface> extends CellEntity {

    public PropertyObjectEntity<P> propertyObject;

    public GroupObjectEntity toDraw;

    public PropertyDrawEntity<P> setToDraw(GroupObjectEntity toDraw) {
        this.toDraw = toDraw;
        return this;
    }

    public boolean shouldBeLast = false;
    public byte forceViewType = ClassViewType.HIDE;

    public void setForceViewType(byte forceViewType) {
        this.forceViewType = forceViewType;
    }

    @Override
    public String toString() {
        return propertyObject.toString();
    }

    public PropertyDrawEntity(int ID, PropertyObjectEntity<P> propertyObject, GroupObjectEntity toDraw) {
        super(ID);
        this.propertyObject = propertyObject;
        this.toDraw = toDraw;
    }

}
