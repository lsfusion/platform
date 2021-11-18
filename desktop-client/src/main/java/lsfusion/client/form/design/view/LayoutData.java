package lsfusion.client.form.design.view;

import lsfusion.interop.base.view.FlexAlignment;

public final class LayoutData {
    //public Element child;
    public FlexAlignment alignment;

    // current, both are changed in resizeWidget
    public double flex;
    public Integer flexBasis; // changed on autosize and on tab change (in this case baseFlexBasis should be change to)

    public double baseFlex;
    public Integer baseFlexBasis; // if null, than we have to get it similar to fixFlexBases by setting flexes to 0

    public boolean shrink;
    public boolean alignShrink;

    public void setFlexBasis(Integer flexBasis) {
        this.flexBasis = flexBasis;
        baseFlexBasis = flexBasis;
    }

    public LayoutData(/*Element child, */FlexAlignment alignment, double flex, Integer flexBasis, boolean shrink, boolean alignShrink) {
        //this.child = child;
        this.alignment = alignment;
        this.flex = flex;
        this.flexBasis = flexBasis;

        this.baseFlex = flex;
        this.baseFlexBasis = flexBasis;

        this.shrink = shrink;
        this.alignShrink = alignShrink;
    }
}
