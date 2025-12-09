package lsfusion.server.language.proxy;

import lsfusion.server.logics.form.interactive.design.object.GridView;

public class GridViewProxy extends GridPropertyViewProxy<GridView> {
    public GridViewProxy(GridView target) {
        super(target);
    }

    @SuppressWarnings("unused")
    public void setAutoSize(boolean autoSize) {
        Integer width = target.getWidth();
        Integer height = target.getHeight();
        if(width == null || width < 0)
            target.setWidth(autoSize ? -1 : -2, getVersion());
        if(height == null || height < 0)
            target.setHeight(autoSize ? -1 : -2, getVersion());
    }

    @SuppressWarnings("unused")
    public void setBoxed(boolean boxed) {
        target.boxed = boxed;
    }

    @SuppressWarnings("unused")
    public void setTabVertical(boolean tabVertical) {
        target.tabVertical = tabVertical;
    }

    @SuppressWarnings("unused")
    public void setQuickSearch(boolean quickSearch) {
        target.setQuickSearch(quickSearch);
    }

    @Deprecated
    @SuppressWarnings("unused")
    public void setHeaderHeight(int headerHeight) {
        target.captionHeight = headerHeight;
    }

    @SuppressWarnings("unused")
    public void setCaptionHeight(int height) {
        target.captionHeight = height;
    }

    @SuppressWarnings("unused")
    public void setCaptionCharHeight(int height) {
        target.captionCharHeight = height;
    }

    @SuppressWarnings("unused")
    public void setResizeOverflow(boolean resizeOverflow) {
        target.resizeOverflow = resizeOverflow;
    }

    @SuppressWarnings("unused")
    public void setLineWidth(int lineWidth) {
        target.lineWidth = lineWidth;
    }

    @SuppressWarnings("unused")
    public void setLineHeight(int lineHeight) {
        target.lineHeight = lineHeight;
    }

    @SuppressWarnings("unused")
    public void setEnableManualUpdate(boolean enableManualUpdate) {
        target.groupObject.entity.enableManualUpdate = enableManualUpdate;
    }
}
