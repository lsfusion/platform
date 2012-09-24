package platform.server.logics.scripted.proxy;

import platform.server.form.view.ToolbarView;

public class ToolbarViewProxy extends ComponentViewProxy<ToolbarView> {
    public ToolbarViewProxy(ToolbarView target) {
        super(target);
    }

    public void setVisible(boolean visible) {
        target.visible = visible;
    }

    public void setShowGroupChange(boolean showGroupChange) {
        target.showGroupChange = showGroupChange;
    }

    public void setShowCountQuantity(boolean showCountQuantity) {
        target.showCountQuantity = showCountQuantity;
    }

    public void setShowCalculateSum(boolean showCalculateSum) {
        target.showCalculateSum = showCalculateSum;
    }

    public void setShowGroup(boolean showGroup) {
        target.showGroup = showGroup;
    }

    public void setShowPrintGroupButton(boolean showPrintGroupButton) {
        target.showPrintGroupButton = showPrintGroupButton;
    }

    public void setShowPrintGroupXlsButton(boolean showPrintGroupXlsButton) {
        target.showPrintGroupXlsButton = showPrintGroupXlsButton;
    }

    public void setShowHideSettings(boolean showHideSettings) {
        target.showHideSettings = showHideSettings;
    }
}
