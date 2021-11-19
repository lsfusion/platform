package lsfusion.interop.base.view;

import java.io.Serializable;

public class FlexConstraints implements Cloneable, Serializable {

    private final FlexAlignment alignment;
    private final double flex;
    private final boolean shrink;
    private final boolean alignShrink;

    public FlexConstraints(FlexAlignment alignment, double flex, boolean shrink, boolean alignShrink) {
        this.alignment = alignment;
        this.flex = flex;
        this.shrink = shrink;
        this.alignShrink = alignShrink;
    }

    public FlexAlignment getAlignment() {
        return alignment;
    }

    public double getFlex() {
        return flex;
    }

    public boolean isShrink() {
        return shrink;
    }

    public boolean isAlignShrink() {
        return alignShrink;
    }
}
