package lsfusion.server.logics.form.interactive.instance.design;

import lsfusion.server.logics.form.interactive.design.ComponentView;
import lsfusion.server.logics.form.interactive.instance.CellInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyObjectInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyReaderInstance;

import static lsfusion.interop.form.property.PropertyReadType.COMPONENT_ELEMENTATTR;
import static lsfusion.interop.form.property.PropertyReadType.COMPONENT_ELEMENTCLASS;

public class ComponentViewInstance<T extends ComponentView> extends CellInstance<T> {

    public final ComponentViewInstance.ElementClassReaderInstance elementClassReader;

    public final PropertyObjectInstance propertyElementClass;

    public final ComponentViewInstance.ElementAttrReaderInstance elementAttrReader;

    public final PropertyObjectInstance propertyElementAttr;

    public ComponentViewInstance(T entity, PropertyObjectInstance propertyElementClass, PropertyObjectInstance propertyElementAttr) {
        super(entity);

        this.propertyElementClass = propertyElementClass;
        this.elementClassReader = new ComponentViewInstance.ElementClassReaderInstance();

        this.propertyElementAttr = propertyElementAttr;
        this.elementAttrReader = new ComponentViewInstance.ElementAttrReaderInstance();
    }

    public class ElementClassReaderInstance implements PropertyReaderInstance {

        public PropertyObjectInstance getReaderProperty() {
            return propertyElementClass;
        }

        @Override
        public byte getTypeID() {
            return COMPONENT_ELEMENTCLASS;
        }

        @Override
        public int getID() {
            return ComponentViewInstance.this.getID();
        }
        @Override
        public Object getProfiledObject() {
            return null;
        }
    }

    public class ElementAttrReaderInstance implements PropertyReaderInstance {

        public PropertyObjectInstance getReaderProperty() {
            return propertyElementAttr;
        }

        @Override
        public byte getTypeID() {
            return COMPONENT_ELEMENTATTR;
        }

        @Override
        public int getID() {
            return ComponentViewInstance.this.getID();
        }
        @Override
        public Object getProfiledObject() {
            return null;
        }
    }

}
