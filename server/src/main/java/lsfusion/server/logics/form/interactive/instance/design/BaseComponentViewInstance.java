package lsfusion.server.logics.form.interactive.instance.design;

import lsfusion.base.col.MapFact;
import lsfusion.server.logics.form.interactive.design.ComponentView;
import lsfusion.server.logics.form.interactive.instance.CellInstance;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyObjectInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyReaderInstance;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.logics.property.value.NullValueProperty;

import static lsfusion.interop.form.property.PropertyReadType.COMPONENT_SHOWIF;

public class BaseComponentViewInstance extends CellInstance<ComponentView> {

    public final ShowIfReaderInstance showIfReader;

    public BaseComponentViewInstance(ComponentView entity) {
        super(entity);

        this.showIfReader = new ShowIfReaderInstance();
    }

    public class ShowIfReaderInstance implements PropertyReaderInstance {

        public PropertyObjectInstance getPropertyObjectInstance() {
            return new PropertyObjectInstance<>(NullValueProperty.instance, MapFact.<PropertyInterface, ObjectInstance>EMPTY());
        }

        @Override
        public byte getTypeID() {
            return COMPONENT_SHOWIF;
        }

        @Override
        public int getID() {
            return BaseComponentViewInstance.this.getID();
        }
        @Override
        public Object getProfiledObject() {
            return null;
        }
    }
}
