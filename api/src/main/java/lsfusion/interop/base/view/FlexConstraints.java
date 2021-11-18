package lsfusion.interop.base.view;

import java.io.Serializable;

public class FlexConstraints implements Cloneable, Serializable {

    private final FlexAlignment alignment;
    private final double flex;
    private final boolean shrink;

    public FlexConstraints(FlexAlignment alignment, double flex, boolean shrink) {
        this.alignment = alignment;
        this.flex = flex;
        this.shrink = shrink;
    }

    public double getFlex() {
        return flex;
    }

    public FlexAlignment getAlignment() {
        return alignment;
    }

    public boolean isShrink() {
        return shrink;
    }
}
