package lsfusion.gwt.base.client.ui;

public class FlexPanelImplWebkit extends FlexPanelImpl {

    @Override
    protected String getDisplayFlexValue() {
        return "-webkit-flex";
    }

    @Override
    protected String getDirectionAttrName() {
        return "WebkitFlexDirection";
    }

    @Override
    protected String getJustifyContentAttrName() {
        return "WebkitJustifyContent";
    }

    @Override
    protected String getStartAlignmentValue() {
        return "flex-start";
    }

    @Override
    protected String getEndAlignmentValue() {
        return "flex-end";
    }

    @Override
    protected String getAlignAttrName() {
        return "WebkitAlignSelf";
    }

    @Override
    protected String getFlexAttrName() {
        return "WebkitFlex";
    }
}
