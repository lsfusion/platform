package lsfusion.interop.base.view;

import java.io.Serializable;

public class FlexConstraints implements Cloneable, Serializable {

    private final double flex;
    private final FlexAlignment alignment;

    public FlexConstraints(FlexAlignment alignment, double flex) {
        this.alignment = alignment;
        this.flex = flex;
    }

    public double getFlex() {
        return flex;
    }

    public FlexAlignment getAlignment() {
        return alignment;
    }
}
