package lsfusion.interop.form.layout;

import java.io.Serializable;

public class FlexConstraints implements Cloneable, Serializable {

    private double flex;
    private FlexAlignment alignment;

    public FlexConstraints() {
        this(FlexAlignment.LEADING, 0);
    }

    public FlexConstraints(FlexAlignment alignment, double flex) {
        setFlex(flex);
        setAlignment(alignment);
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

    public void setFlex(double flex) {
        this.flex = flex;
    }

    public FlexAlignment getAlignment() {
        return alignment;
    }

    public void setAlignment(FlexAlignment alignment) {
        this.alignment = alignment;
    }
}
