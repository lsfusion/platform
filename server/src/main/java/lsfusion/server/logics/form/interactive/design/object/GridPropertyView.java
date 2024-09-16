package lsfusion.server.logics.form.interactive.design.object;

import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.FormInstanceContext;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerSerializationPool;
import lsfusion.server.logics.form.interactive.design.BaseComponentView;
import lsfusion.server.logics.form.interactive.design.ContainerView;
import lsfusion.server.logics.form.struct.property.PropertyObjectEntity;

import java.io.DataOutputStream;
import java.io.IOException;

public abstract class GridPropertyView extends BaseComponentView {

    public Integer captionHeight;
    public Integer captionCharHeight;

    public Boolean resizeOverflow; // actually it is the max height

    public String valueClass;
    public PropertyObjectEntity propertyValueClass;

    @Override
    protected boolean hasPropertyComponent() {
        return super.hasPropertyComponent() || propertyValueClass != null;
    }

    public GridPropertyView() {
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

    public int getCaptionHeight() {
        if(captionHeight != null)
            return captionHeight;

        if(captionCharHeight != null)
            return -2;

        return -1;
    }

    public int getCaptionCharHeight() {
        return captionCharHeight != null ? captionCharHeight : -1;
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

    public Integer lineWidth;
    public Integer lineHeight;

    public int getLineWidth() {
        if(lineWidth != null)
            return lineWidth;

        return -1;
    }

    public void setLineWidth(Integer lineWidth) {
        this.lineWidth = lineWidth;
    }

    public int getLineHeight() {
        if(lineHeight != null)
            return lineHeight;

        return -1;
    }

    public void setLineHeight(Integer lineHeight) {
        this.lineHeight = lineHeight;
    }

    @Override
    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream) throws IOException {
        super.customSerialize(pool, outStream);

        outStream.writeBoolean(boxed != null);
        if(boxed != null)
            outStream.writeBoolean(boxed);

        outStream.writeInt(getCaptionHeight());
        outStream.writeInt(getCaptionCharHeight());

        outStream.writeBoolean(resizeOverflow != null);
        if(resizeOverflow != null)
            outStream.writeBoolean(resizeOverflow);

        outStream.writeInt(getLineWidth());
        outStream.writeInt(getLineHeight());

        pool.writeString(outStream, valueClass);
    }

    public Boolean boxed;

    protected abstract boolean isCustom();

    @Override
    protected int getDefaultWidth(FormInstanceContext context) {
        if (lineWidth == null && isCustom())
            return -1;

//        // if we have opposite direction and align shrink, setting default width to zero
//        ContainerView container = getLayoutParamContainer();
//        if(!(container != null && container.isHorizontal()) && isAlignShrink(entity))
//            return 0; // or -1 doesn't matter

        return -2;
    }

    @Override
    protected int getDefaultHeight(FormInstanceContext context) {
        if (lineHeight == null && isCustom())
            return -1;

        return -2;
    }
}
