package lsfusion.server.logics.form.interactive.instance.design;

import lsfusion.interop.form.property.PropertyReadType;
import lsfusion.server.logics.form.interactive.design.ComponentView;
import lsfusion.server.logics.form.interactive.instance.property.PropertyObjectInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyReaderInstance;

public class TreeGroupViewInstance extends GridPropertyViewInstance {

    public final HierarchicalCaptionReaderInstance hierarchicalCaptionReader;

    public final PropertyObjectInstance propertyHierarchicalCaption;

    public TreeGroupViewInstance(ComponentView entity, PropertyObjectInstance propertyElementClass, PropertyObjectInstance propertyValueClass,
                                 PropertyObjectInstance propertyHierarchicalCaption) {
        super(entity, propertyElementClass, propertyValueClass);

        this.propertyHierarchicalCaption = propertyHierarchicalCaption;
        this.hierarchicalCaptionReader = new HierarchicalCaptionReaderInstance();
    }

    public class HierarchicalCaptionReaderInstance implements PropertyReaderInstance {

        public PropertyObjectInstance getReaderProperty() {
            return propertyHierarchicalCaption;
        }

        @Override
        public byte getTypeID() {
            return PropertyReadType.TREE_HIERARCHICALCAPTION;
        }

        @Override
        public int getID() {
            return TreeGroupViewInstance.this.getID();
        }
        @Override
        public Object getProfiledObject() {
            return null;
        }
    }
}
