package lsfusion.server.language.proxy;

import lsfusion.server.base.version.Version;
import lsfusion.server.logics.form.interactive.design.object.GridView;

public class GridViewProxy extends GridPropertyViewProxy<GridView> {
    public GridViewProxy(GridView target) {
        super(target);
    }

    @SuppressWarnings("unused")
    public void setAutoSize(boolean autoSize) {
        Version version = getVersion();
        Integer width = target.getNFWidth(version);
        Integer height = target.getNFHeight(version);
        if(width == null || width < 0)
            target.setWidth(autoSize ? -1 : -2, version);
        if(height == null || height < 0)
            target.setHeight(autoSize ? -1 : -2, version);
    }

    @SuppressWarnings("unused")
    public void setBoxed(boolean boxed) {
        target.setBoxed(boxed, getVersion());
    }

    @SuppressWarnings("unused")
    public void setTabVertical(boolean tabVertical) {
        target.setTabVertical(tabVertical, getVersion());
    }

    @SuppressWarnings("unused")
    public void setQuickSearch(boolean quickSearch) {
        target.setQuickSearch(quickSearch, getVersion());
    }

    @Deprecated
    @SuppressWarnings("unused")
    public void setHeaderHeight(int headerHeight) {
        target.setCaptionHeight(headerHeight, getVersion());
    }

    @SuppressWarnings("unused")
    public void setCaptionHeight(int height) {
        target.setCaptionHeight(height, getVersion());
    }

    @SuppressWarnings("unused")
    public void setCaptionCharHeight(int height) {
        target.setCaptionCharHeight(height, getVersion());
    }

    @SuppressWarnings("unused")
    public void setResizeOverflow(boolean resizeOverflow) {
        target.setResizeOverflow(resizeOverflow, getVersion());
    }

    @SuppressWarnings("unused")
    public void setLineWidth(int lineWidth) {
        target.setLineWidth(lineWidth, getVersion());
    }

    @SuppressWarnings("unused")
    public void setLineHeight(int lineHeight) {
        target.setLineHeight(lineHeight, getVersion());
    }

    @SuppressWarnings("unused")
    public void setEnableManualUpdate(boolean enableManualUpdate) {
        target.groupObject.entity.setEnableManualUpdate(enableManualUpdate, getVersion());
    }
}
