package lsfusion.gwt.base.client.ui;

public class FlexPanelImplIE extends FlexPanelImpl {

    @Override
    protected String getDisplayFlexValue() {
        return "-ms-flexbox";
    }

    @Override
    protected String getDirectionAttrName() {
        return "MsFlexDirection";
    }

    @Override
    protected String getJustifyContentAttrName() {
        return "MsFlexPack";
    }

    @Override
    protected String getLeadingAlignmentValue() {
        return "start";
    }

    @Override
    protected String getTrailingAlignmentValue() {
        return "end";
    }

    @Override
    protected String getAlignAttrName() {
        return "MsFlexItemAlign";
    }

    @Override
    protected String getFlexAttrName() {
        return "MsFlex";
    }
}
