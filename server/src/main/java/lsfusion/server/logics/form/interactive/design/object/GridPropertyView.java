package lsfusion.server.logics.form.interactive.design.object;

import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.server.logics.form.interactive.design.BaseComponentView;
import lsfusion.server.logics.form.interactive.design.ContainerView;
import lsfusion.server.logics.form.struct.FormEntity;

public abstract class GridPropertyView extends BaseComponentView {

    public int headerHeight = 54; // actually it is the max height

    public GridPropertyView() {
    }

    public GridPropertyView(int ID) {
        super(ID);
    }


    @Override
    public double getDefaultFlex(FormEntity formEntity) {
        return 1;
    }

    @Override
    protected boolean isDefaultShrink(FormEntity formEntity, boolean explicit) {
        ContainerView container = getLayoutParamContainer();
        if(container != null && container.isWrap())
            return true;
        return super.isDefaultShrink(formEntity, explicit);
    }

    @Override
    public FlexAlignment getDefaultAlignment(FormEntity formEntity) {
        return FlexAlignment.STRETCH;
    }

    @Override
    protected boolean isDefaultAlignShrink(FormEntity formEntity, boolean explicit) {
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

    public Boolean autoSize;

    public boolean isAutoSize(FormEntity entity) {
        if(autoSize != null)
            return autoSize;

        return isCustom();
    }

    public Boolean boxed;

    protected abstract boolean isCustom();

    @Override
    protected int getDefaultWidth(FormEntity entity) {
        if(lineWidth == null && isAutoSize(entity))
            return -1;

//        // if we have opposite direction and align shrink, setting default width to zero
//        ContainerView container = getLayoutParamContainer();
//        if(!(container != null && container.isHorizontal()) && isAlignShrink(entity))
//            return 0; // or -1 doesn't matter

        return -2;
    }

    @Override
    protected int getDefaultHeight(FormEntity entity) {
        if(lineHeight == null && isAutoSize(entity))
            return -1;

        return -2;
    }
}
