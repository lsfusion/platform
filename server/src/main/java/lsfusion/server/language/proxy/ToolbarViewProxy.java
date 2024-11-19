package lsfusion.server.language.proxy;

import lsfusion.server.logics.form.interactive.design.object.ToolbarView;

public class ToolbarViewProxy extends ComponentViewProxy<ToolbarView> {
    public ToolbarViewProxy(ToolbarView target) {
        super(target);
    }

    public void setVisible(boolean visible) {
        target.visible = visible;
    }

    //deprecated, will be removed in 7.0
    @Deprecated
    @SuppressWarnings("unused")
    public void setShowGroup(boolean showGroup) {
        setShowViews(showGroup);
    }

    @SuppressWarnings("unused")
    public void setShowViews(boolean showViews) {
        target.showViews = showViews;
    }

    @SuppressWarnings("unused")
    public void setShowFilters(boolean showFilters) {
        target.showFilters = showFilters;
    }

    @SuppressWarnings("unused")
    public void setShowSettings(boolean showSettings) {
        target.showSettings = showSettings;
    }

    @SuppressWarnings("unused")
    public void setShowCountQuantity(boolean showCountQuantity) {
        target.showCountQuantity = showCountQuantity;
    }

    @SuppressWarnings("unused")
    public void setShowCalculateSum(boolean showCalculateSum) {
        target.showCalculateSum = showCalculateSum;
    }

    @SuppressWarnings("unused")
    public void setShowPrintGroupXls(boolean showPrintGroupXls) {
        target.showPrintGroupXls = showPrintGroupXls;
    }

    @SuppressWarnings("unused")
    public void setShowManualUpdate(boolean showManualUpdate) {
        target.showManualUpdate = showManualUpdate;
    }

    @SuppressWarnings("unused")
    public void setEnableManualUpdate(boolean enableManualUpdate) {
        target.enableManualUpdate = enableManualUpdate;
    }

}
