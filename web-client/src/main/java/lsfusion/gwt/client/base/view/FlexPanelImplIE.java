package lsfusion.gwt.client.base.view;

public class FlexPanelImplIE extends FlexPanelImpl {

    @Override
    protected String getDisplayFlexValue() {
        return "-ms-flexbox";
    }

    @Override
    protected String getDirectionAttrName() {
        return "msFlexDirection";
    }

    @Override
    protected String getJustifyContentAttrName() {
        return "msFlexPack";
    }

    @Override
    protected String getStartAlignmentValue() {
        return "start";
    }

    @Override
    protected String getEndAlignmentValue() {
        return "end";
    }

    @Override
    protected String getAlignAttrName() {
        return "msFlexItemAlign";
    }

    @Override
    protected String getFlexAttrName() {
        return "msFlex";
    }
}
