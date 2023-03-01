package lsfusion.server.logics.form.interactive.instance.design;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.logics.form.interactive.design.ContainerView;
import lsfusion.server.logics.form.interactive.design.ContainerViewExtraType;
import lsfusion.server.logics.form.interactive.instance.CellInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyObjectInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyReaderInstance;

public class ContainerViewInstance extends CellInstance<ContainerView> {

    public ContainerViewInstance(ContainerView entity, ImMap<ContainerViewExtraType, PropertyObjectInstance<?>> extras) {
        super(entity);

        propertyCaption = extras.get(ContainerViewExtraType.CAPTION);
        captionReader = new ExtraReaderInstance(ContainerViewExtraType.CAPTION, propertyCaption);

        propertyImage = extras.get(ContainerViewExtraType.IMAGE);
        imageReader = new ExtraReaderInstance(ContainerViewExtraType.IMAGE, propertyImage);

        propertyCustomDesign = extras.get(ContainerViewExtraType.CUSTOM);
        customDesignReader = new ExtraReaderInstance(ContainerViewExtraType.CUSTOM, propertyCustomDesign);
    }

    public final PropertyObjectInstance<?> propertyCaption;
    public final ExtraReaderInstance captionReader;

    public final PropertyObjectInstance<?> propertyImage;
    public final ExtraReaderInstance imageReader;

    public final PropertyObjectInstance<?> propertyCustomDesign;
    public final ExtraReaderInstance customDesignReader;

    public class ExtraReaderInstance implements PropertyReaderInstance {
        private final ContainerViewExtraType type;
        private final PropertyObjectInstance property;

        public ExtraReaderInstance(ContainerViewExtraType type, PropertyObjectInstance property) {
            this.type = type;
            this.property = property;
        }

        @Override
        public PropertyObjectInstance getPropertyObjectInstance() {
            return property;
        }

        @Override
        public byte getTypeID() {
            return type.getContainerReadType();
        }

        @Override
        public int getID() {
            return ContainerViewInstance.this.getID();
        }

        @Override
        public Object getProfiledObject() {
            return entity.getExtra(type);
        }

        public ContainerView getContainerView() {
            return entity;
        }
    }
}
