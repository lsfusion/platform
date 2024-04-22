package lsfusion.server.logics.form.interactive.instance.design;

import lsfusion.interop.form.property.PropertyReadType;
import lsfusion.server.logics.form.interactive.design.ComponentView;
import lsfusion.server.logics.form.interactive.instance.property.PropertyObjectInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyReaderInstance;

public class GridPropertyViewInstance extends BaseComponentViewInstance {

    public final ValueClassReaderInstance valueClassReader;

    public final PropertyObjectInstance propertyValueClass;

    public GridPropertyViewInstance(ComponentView entity, PropertyObjectInstance propertyElementClass, PropertyObjectInstance propertyValueClass) {
        super(entity, propertyElementClass);

        this.propertyValueClass = propertyValueClass;
        this.valueClassReader = new ValueClassReaderInstance();
    }

    public class ValueClassReaderInstance implements PropertyReaderInstance {

        public PropertyObjectInstance getReaderProperty() {
            return propertyValueClass;
        }

        @Override
        public byte getTypeID() {
            return PropertyReadType.GRID_VALUECLASS;
        }

        @Override
        public int getID() {
            return GridPropertyViewInstance.this.getID();
        }
        @Override
        public Object getProfiledObject() {
            return null;
        }
    }
}
