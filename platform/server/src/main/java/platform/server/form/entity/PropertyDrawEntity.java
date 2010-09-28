package platform.server.form.entity;

import platform.base.OrderedMap;
import platform.server.form.instance.InstanceFactory;
import platform.server.form.instance.Instantiable;
import platform.server.form.instance.PropertyDrawInstance;
import platform.server.form.instance.PropertyReadInstance;
import platform.server.form.view.DefaultFormView;
import platform.server.logics.property.PropertyInterface;

import java.util.List;
import java.util.ArrayList;

public class PropertyDrawEntity<P extends PropertyInterface> extends CellEntity implements Instantiable<PropertyDrawInstance> {

    public PropertyObjectEntity<P> propertyObject;

    public GroupObjectEntity toDraw;

    // предполагается что propertyObject ссылается на все (хотя и не обязательно)
    public List<GroupObjectEntity> columnGroupObjects = new ArrayList<GroupObjectEntity>();

    // предполагается что propertyCaption ссылается на все из propertyObject но без toDraw (хотя опять таки не обязательно)
    public PropertyObjectEntity<?> propertyCaption;

    public boolean shouldBeLast = false;
    public Byte forceViewType = null;

    @Override
    public String toString() {
        return propertyObject.toString();
    }

    public PropertyDrawEntity(int ID, PropertyObjectEntity<P> propertyObject, GroupObjectEntity toDraw) {
        super(ID);
        this.propertyObject = propertyObject;
        this.toDraw = toDraw;
    }

    public PropertyDrawInstance getInstance(InstanceFactory instanceFactory) {
        return instanceFactory.getInstance(this);
    }

    public void addColumnGroupObject(GroupObjectEntity columnGroupObject) {
        columnGroupObjects.add(columnGroupObject);
    }

    public void setPropertyCaption(PropertyObjectEntity propertyCaption) {
        this.propertyCaption = propertyCaption; 
    }

    public void proceedDefaultDesign(DefaultFormView defaultView) {
        propertyObject.property.proceedDefaultDesign(defaultView, this);
    }
}
