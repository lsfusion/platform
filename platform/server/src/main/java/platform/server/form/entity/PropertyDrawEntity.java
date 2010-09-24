package platform.server.form.entity;

import platform.base.Pair;
import platform.base.OrderedMap;
import platform.server.form.instance.InstanceFactory;
import platform.server.form.instance.Instantiable;
import platform.server.form.instance.PropertyDrawInstance;
import platform.server.form.view.DefaultFormView;
import platform.server.logics.property.PropertyInterface;

public class PropertyDrawEntity<P extends PropertyInterface> extends CellEntity implements Instantiable<PropertyDrawInstance> {

    public PropertyObjectEntity<P> propertyObject;

    public GroupObjectEntity toDraw;
    public OrderedMap<GroupObjectEntity, PropertyDrawEntity> columnGroupObjects = new OrderedMap<GroupObjectEntity, PropertyDrawEntity>();

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

    public void addColumnGroupObject(GroupObjectEntity columnGroupObject, PropertyDrawEntity columnDisplayProperty) {
        assert (columnDisplayProperty.toDraw == columnGroupObject) : "Свойство имени колонки должно быть из того же groupObject, который идёт в колонку";
        columnGroupObjects.put(columnGroupObject, columnDisplayProperty);
    }

    public void proceedDefaultDesign(DefaultFormView defaultView) {
        propertyObject.property.proceedDefaultDesign(defaultView, this);
    }
}
