package lsfusion.server.logics.form.interactive.instance.design;

import lsfusion.server.logics.form.interactive.design.ComponentView;
import lsfusion.server.logics.form.interactive.instance.CellInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyObjectInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyReaderInstance;

import static lsfusion.interop.form.property.PropertyReadType.COMPONENT_ELEMENTATTR;
import static lsfusion.interop.form.property.PropertyReadType.COMPONENT_ELEMENTCLASS;

public class ComponentViewInstance<T extends ComponentView> extends CellInstance<T> {

    public final ElementClassAttrReaderInstance elementClassReader;

    public final PropertyObjectInstance propertyElementClass;

    public final ElementClassAttrReaderInstance elementAttrReader;

    public final PropertyObjectInstance propertyElementAttr;

    public ComponentViewInstance(T entity, PropertyObjectInstance propertyElementClass, PropertyObjectInstance propertyElementAttr) {
        super(entity);

        this.propertyElementClass = propertyElementClass;
        this.elementClassReader = new ElementClassAttrReaderInstance(false);

        this.propertyElementAttr = propertyElementAttr;
        this.elementAttrReader = new ElementClassAttrReaderInstance(true);
    }

    public class ElementClassAttrReaderInstance implements PropertyReaderInstance {
        public boolean attr;
        public ElementClassAttrReaderInstance(boolean attr) {
            this.attr = attr;
        }

        public PropertyObjectInstance getReaderProperty() {
            return attr ? propertyElementAttr : propertyElementClass;
        }

        @Override
        public byte getTypeID() {
            return attr ? COMPONENT_ELEMENTATTR : COMPONENT_ELEMENTCLASS;
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
