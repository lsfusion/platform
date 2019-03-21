package lsfusion.interop.base.view;

import java.io.Serializable;

public class FlexConstraints implements Cloneable, Serializable {
    public static FlexConstraints fill = new FlexConstraints(FlexAlignment.STRETCH, 1);

    private final double flex;
    private final FlexAlignment alignment;

    public FlexConstraints() {
        this(FlexAlignment.START, 0);
    }

    public FlexConstraints(FlexAlignment alignment, double flex) {
        this.alignment = alignment;
        this.flex = flex;
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError("Can't clone FlexConstraints");
        }
    }

    public double getFlex() {
        return flex;
    }

    public FlexAlignment getAlignment() {
        return alignment;
    }
}
