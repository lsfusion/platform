package lsfusion.server.logics.form.interactive.instance.design;

import lsfusion.base.col.MapFact;
import lsfusion.interop.form.property.PropertyReadType;
import lsfusion.server.logics.form.interactive.design.ComponentView;
import lsfusion.server.logics.form.interactive.instance.property.PropertyObjectInstance;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyReaderInstance;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.logics.property.value.NullValueProperty;

public class ComponentViewInstance<T extends ComponentView> {

    public final ComponentViewInstance.ElementClassReaderInstance elementClassReader;
    // whether the form hides the component now (true: hidden) - sent for the base components and for the containers a
    // react view is told about (FormEntity.getBaseComponents)
    public final ShowIfReaderInstance showIfReader = new ShowIfReaderInstance();

    public final PropertyObjectInstance propertyElementClass;
    public T entity;

    public ComponentViewInstance(T entity, PropertyObjectInstance propertyElementClass) {
        this.entity = entity;

        this.propertyElementClass = propertyElementClass;
        this.elementClassReader = new ComponentViewInstance.ElementClassReaderInstance();
    }

    public int getID() {
        return entity.getID();
    }

    public String getSID() {
        return entity.getSID();
    }

    public class ElementClassReaderInstance implements PropertyReaderInstance {

        public PropertyObjectInstance getReaderProperty() {
            return propertyElementClass;
        }

        @Override
        public byte getTypeID() {
            return PropertyReadType.COMPONENT_ELEMENTCLASS;
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

    public class ShowIfReaderInstance implements PropertyReaderInstance {

        public PropertyObjectInstance getReaderProperty() {
            return new PropertyObjectInstance<>(NullValueProperty.instance, MapFact.<PropertyInterface, ObjectInstance>EMPTY());
        }

        @Override
        public byte getTypeID() {
            return PropertyReadType.COMPONENT_SHOWIF;
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
