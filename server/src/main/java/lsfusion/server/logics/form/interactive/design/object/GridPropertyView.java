package lsfusion.server.logics.form.interactive.design.object;

import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.server.base.version.NFFact;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.interfaces.NFProperty;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.form.ObjectMapping;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.FormInstanceContext;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerSerializationPool;
import lsfusion.server.logics.form.interactive.design.BaseComponentView;
import lsfusion.server.logics.form.interactive.design.ContainerView;
import lsfusion.server.logics.form.struct.property.PropertyObjectEntity;

import java.io.DataOutputStream;
import java.io.IOException;

import static lsfusion.base.BaseUtils.nvl;

public abstract class GridPropertyView extends BaseComponentView {

    private NFProperty<Integer> captionHeight = NFFact.property();
    private NFProperty<Integer> captionCharHeight = NFFact.property();

    private NFProperty<Boolean> resizeOverflow = NFFact.property(); // actually it is the max height

    private NFProperty<String> valueClass = NFFact.property();
    private NFProperty<PropertyObjectEntity> propertyValueClass = NFFact.property();

    private NFProperty<Integer> lineWidth = NFFact.property();
    private NFProperty<Integer> lineHeight = NFFact.property();

    private NFProperty<Boolean> boxed = NFFact.property();

    @Override
    protected boolean hasPropertyComponent() {
        return super.hasPropertyComponent() || getPropertyValueClass() != null;
    }

    public GridPropertyView(int ID) {
        super(ID);
    }


    @Override
    public double getDefaultFlex(FormInstanceContext context) {
        return 1;
    }

    @Override
    protected boolean isDefaultShrink(FormInstanceContext context, boolean explicit) {
        ContainerView container = getLayoutParamContainer();
        if(container != null && container.isWrap())
            return true;
        return super.isDefaultShrink(context, explicit);
    }

    @Override
    public FlexAlignment getDefaultAlignment(FormInstanceContext context) {
        return FlexAlignment.STRETCH;
    }

    @Override
    protected boolean isDefaultAlignShrink(FormInstanceContext context, boolean explicit) {
        // actually not needed mostly since for STRETCH align shrink is set, but just in case
        return true;
    }

    @Override
    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream) throws IOException {
        super.customSerialize(pool, outStream);

        Boolean boxed = getBoxed();
        outStream.writeBoolean(boxed != null);
        if(boxed != null)
            outStream.writeBoolean(boxed);

        outStream.writeInt(getCaptionHeight());
        outStream.writeInt(getCaptionCharHeightValue());

        Boolean resizeOverflow = getResizeOverflow();
        outStream.writeBoolean(resizeOverflow != null);
        if(resizeOverflow != null)
            outStream.writeBoolean(resizeOverflow);

        outStream.writeInt(getLineWidthValue());
        outStream.writeInt(getLineHeightValue());

        pool.writeString(outStream, getValueClass());
    }

    public Integer getCaptionHeight() {
        Integer captionHeightValue = captionHeight.get();
        if(captionHeightValue != null)
            return captionHeightValue;

        Integer captionCharHeightValue = getCaptionCharHeight();
        if(captionCharHeightValue != null)
            return -2;

        return -1;
    }
    public void setCaptionHeight(Integer value, Version version) {
        captionHeight.set(value, version);
    }

    public Integer getCaptionCharHeightValue() {
        return nvl(getCaptionCharHeight(), -1);
    }
    public Integer getCaptionCharHeight() {
        return captionCharHeight.get();
    }
    public void setCaptionCharHeight(Integer value, Version version) {
        captionCharHeight.set(value, version);
    }

    public Boolean getResizeOverflow() {
        return resizeOverflow.get();
    }
    public void setResizeOverflow(Boolean value, Version version) {
        resizeOverflow.set(value, version);
    }

    public String getValueClass() {
        return valueClass.get();
    }
    public void setValueClass(String value, Version version) {
        valueClass.set(value, version);
    }

    public PropertyObjectEntity getPropertyValueClass() {
        return propertyValueClass.get();
    }
    public void setPropertyValueClass(PropertyObjectEntity value, Version version) {
        propertyValueClass.set(value, version);
    }

    public int getLineWidthValue() {
        return nvl(getLineWidth(), -1);
    }
    public Integer getLineWidth() {
        return lineWidth.get();
    }
    public void setLineWidth(Integer value, Version version) {
        lineWidth.set(value, version);
    }

    public int getLineHeightValue() {
        return nvl(getLineHeight(), -1);
    }
    public Integer getLineHeight() {
        return lineHeight.get();
    }
    public void setLineHeight(Integer value, Version version) {
        lineHeight.set(value, version);
    }

    public Boolean getBoxed() {
        return boxed.get();
    }
    public void setBoxed(Boolean value, Version version) {
        boxed.set(value, version);
    }

    protected abstract boolean isCustom();

    @Override
    protected int getDefaultWidth(FormInstanceContext context) {
        if (getLineWidth() == null && isCustom())
            return -1;

//        // if we have opposite direction and align shrink, setting default width to zero
//        ContainerView container = getLayoutParamContainer();
//        if(!(container != null && container.isHorizontal()) && isAlignShrink(entity))
//            return 0; // or -1 doesn't matter

        return -2;
    }

    @Override
    protected int getDefaultHeight(FormInstanceContext context) {
        if (getLineHeight() == null && isCustom())
            return -1;

        return -2;
    }

    // copy-constructor
    public GridPropertyView(GridPropertyView src, ObjectMapping mapping) {
        super(src, mapping);
        this.ID = BaseLogicsModule.generateStaticNewID();

        captionHeight.set(src.captionHeight, p -> p, mapping.version);
        captionCharHeight.set(src.captionCharHeight, p -> p, mapping.version);

        resizeOverflow.set(src.resizeOverflow, p -> p, mapping.version);

        valueClass.set(src.valueClass, p -> p, mapping.version);
        propertyValueClass.set(src.propertyValueClass, mapping::get, mapping.version);

        lineWidth.set(src.lineWidth, p -> p, mapping.version);
        lineHeight.set(src.lineHeight, p -> p, mapping.version);

        boxed.set(src.boxed, p -> p, mapping.version);
    }
}
