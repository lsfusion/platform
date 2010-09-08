package platform.server.form.entity;

import platform.base.Pair;
import platform.server.form.instance.InstanceFactory;
import platform.server.form.instance.Instantiable;
import platform.server.form.instance.PropertyDrawInstance;
import platform.server.logics.property.PropertyInterface;

import java.util.ArrayList;
import java.util.List;

public class PropertyDrawEntity<P extends PropertyInterface> extends CellEntity implements Instantiable<PropertyDrawInstance> {

    public PropertyObjectEntity<P> propertyObject;

    public GroupObjectEntity toDraw;
    private GroupObjectEntity[] columnGroupObjects;
    private PropertyDrawEntity[] columnDisplayProperties;
    private List<Pair<GroupObjectEntity, PropertyDrawEntity>> columGroupObjectsList = new ArrayList<Pair<GroupObjectEntity, PropertyDrawEntity>>();

    public PropertyDrawEntity<P> setToDraw(GroupObjectEntity toDraw) {
        this.toDraw = toDraw;
        return this;
    }

    public boolean shouldBeLast = false;
    public Byte forceViewType = null;

    public void setForceViewType(Byte forceViewType) {
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

    public PropertyDrawInstance getInstance(InstanceFactory instanceFactory) {
        return instanceFactory.getInstance(this);
    }

    public void addColumnGroupObject(GroupObjectEntity columnGroupObject, PropertyDrawEntity columnDisplayProperty) {
        assert (columnDisplayProperty.toDraw == columnGroupObject) : "Свойство имени колонки должно быть из того же groupObject, который идёт в колонку";
        columGroupObjectsList.add(new Pair<GroupObjectEntity, PropertyDrawEntity>(columnGroupObject, columnDisplayProperty));
    }

    private void convertColumnPairsToArrays() {
        if (columnDisplayProperties == null || columnDisplayProperties.length != columGroupObjectsList.size()) {
            int len = columGroupObjectsList.size();
            columnGroupObjects = new GroupObjectEntity[len];
            columnDisplayProperties = new PropertyDrawEntity[len];

            for (int i = 0; i < len; ++i) {
                columnGroupObjects[i] = columGroupObjectsList.get(i).first;
                columnDisplayProperties[i] = columGroupObjectsList.get(i).second;
            }
        }
    }

    public PropertyDrawEntity[] getColumnDisplayProperties() {
        convertColumnPairsToArrays();
        return columnDisplayProperties;
    }

    public GroupObjectEntity[] getColumnGroupObjects() {
        convertColumnPairsToArrays();
        return columnGroupObjects;
    }
}
