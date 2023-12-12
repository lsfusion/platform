package lsfusion.server.logics.classes.data;

import lsfusion.interop.classes.DataType;
import lsfusion.server.data.type.Type;

public class RichTextClass extends TextClass {

    public final static RichTextClass instance = new RichTextClass();

    public RichTextClass() {
        super("rich");
    }

    @Override
    public byte getTypeID() {
        return DataType.RICHTEXT;
    }

    @Override
    public String getSID() {
        return "RICHTEXT";
    }

    @Override
    public DataClass getCompatible(DataClass compClass, boolean or) {
        return compClass instanceof RichTextClass ? this : super.getCompatible(compClass, or);
    }

    @Override
    public boolean useInputTag(boolean isPanel, boolean useBootstrap, Type changeType) {
        return false;
    }
}
