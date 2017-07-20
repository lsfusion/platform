package lsfusion.interop.form.layout;

import java.io.Serializable;

public class ColumnsConstraints implements Cloneable, Serializable {

    private FlexAlignment alignment;

    public ColumnsConstraints() {
        this(FlexAlignment.STRETCH);
    }

    public ColumnsConstraints(FlexAlignment alignment) {
        setAlignment(alignment);
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError("Can't clone ColumnsConstraints");
        }
    }

    public FlexAlignment getAlignment() {
        return alignment;
    }

    public void setAlignment(FlexAlignment alignment) {
        this.alignment = alignment;
    }
}
