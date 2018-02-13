package lsfusion.interop.form.layout;

import java.io.Serializable;

public class FlexConstraints implements Cloneable, Serializable {
    public static FlexConstraints fill = new FlexConstraints(FlexAlignment.STRETCH, 1);
    public static FlexConstraints stretch_self = new FlexConstraints(FlexAlignment.STRETCH, 0);
    public static FlexConstraints leading_self = new FlexConstraints(FlexAlignment.LEADING, 0);
    public static FlexConstraints center_self = new FlexConstraints(FlexAlignment.CENTER, 0);
    public static FlexConstraints trailing_self = new FlexConstraints(FlexAlignment.TRAILING, 0);

    private final double flex;
    private final FlexAlignment alignment;

    public FlexConstraints() {
        this(FlexAlignment.LEADING, 0);
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
