package lsfusion.server.logics.scripted.proxy;

import lsfusion.server.form.view.ToolbarView;

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
        target.showCountRows = showCountQuantity;
    }

    public void setShowCalculateSum(boolean showCalculateSum) {
        target.showCalculateSum = showCalculateSum;
    }

    public void setShowGroup(boolean showGroup) {
        target.showGroupReport = showGroup;
    }

    public void setShowPrintGroupButton(boolean showPrintGroupButton) {
        target.showPrint = showPrintGroupButton;
    }

    public void setShowPrintGroupXlsButton(boolean showPrintGroupXlsButton) {
        target.showXls = showPrintGroupXlsButton;
    }

    public void setShowHideSettings(boolean showHideSettings) {
        target.showSettings = showHideSettings;
    }
}
