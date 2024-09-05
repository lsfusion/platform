package lsfusion.server.logics.classes.data.file;

import lsfusion.interop.base.view.FlexAlignment;

public abstract class RenderedClass extends StaticFormatFileClass {

    public RenderedClass(boolean multiple, boolean storeName) {
        super(multiple, storeName);
    }

    @Override
    public FlexAlignment getValueAlignmentHorz() {
        return FlexAlignment.STRETCH;
    }

    @Override
    public FlexAlignment getValueAlignmentVert() {
        return FlexAlignment.STRETCH;
    }

    @Override
    public boolean getValueShrinkHorz() {
        return true;
    }

    @Override
    public boolean getValueShrinkVert() {
        return true;
    }

//    @Override
//    public String getValueOverflowHorz() {
//        return "auto";
//    }
}
