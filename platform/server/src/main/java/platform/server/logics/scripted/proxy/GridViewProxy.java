package platform.server.logics.scripted.proxy;

import platform.server.form.view.GridView;

public class GridViewProxy extends ComponentViewProxy<GridView> {
    public GridViewProxy(GridView target) {
        super(target);
    }
    
    public void setShowFind(boolean showFind) {
        target.showFind = showFind;
    }

    public void setShowFilter(boolean showFilter) {
        target.showFilter = showFilter;
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

    public void setMinRowCount(byte minRowCount) {
        target.minRowCount = minRowCount;
    }

    public void setTabVertical(boolean tabVertical) {
        target.tabVertical = tabVertical;
    }

    public void setAutoHide(boolean autoHide) {
        target.autoHide = autoHide;
    }
}
