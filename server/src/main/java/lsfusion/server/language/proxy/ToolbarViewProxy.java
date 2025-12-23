package lsfusion.server.language.proxy;

import lsfusion.server.logics.form.interactive.design.object.ToolbarView;

public class ToolbarViewProxy extends ComponentViewProxy<ToolbarView> {
    public ToolbarViewProxy(ToolbarView target) {
        super(target);
    }

    public void setVisible(boolean visible) {
        target.setVisible(visible, getVersion());
    }

    //deprecated since 6.0, will be removed in 7.0
    @Deprecated
    @SuppressWarnings("unused")
    public void setShowGroup(boolean showGroup) {
        setShowViews(showGroup);
    }

    @SuppressWarnings("unused")
    public void setShowViews(boolean showViews) {
        target.setShowViews(showViews, getVersion());
    }

    @SuppressWarnings("unused")
    public void setShowFilters(boolean showFilters) {
        target.setShowFilters(showFilters, getVersion());
    }

    @SuppressWarnings("unused")
    public void setShowSettings(boolean showSettings) {
        target.setShowSettings(showSettings, getVersion());
    }

    @SuppressWarnings("unused")
    public void setShowCountQuantity(boolean showCountQuantity) {
        target.setShowCountQuantity(showCountQuantity, getVersion());
    }

    @SuppressWarnings("unused")
    public void setShowCalculateSum(boolean showCalculateSum) {
        target.setShowCalculateSum(showCalculateSum, getVersion());
    }

    @SuppressWarnings("unused")
    public void setShowPrintGroupXls(boolean showPrintGroupXls) {
        target.setShowPrintGroupXls(showPrintGroupXls, getVersion());
    }

    @SuppressWarnings("unused")
    public void setShowManualUpdate(boolean showManualUpdate) {
        target.setShowManualUpdate(showManualUpdate, getVersion());
    }

}
