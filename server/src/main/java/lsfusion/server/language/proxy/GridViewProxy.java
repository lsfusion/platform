package lsfusion.server.language.proxy;

import lsfusion.server.logics.form.interactive.design.object.GridView;

public class GridViewProxy extends ComponentViewProxy<GridView> {
    public GridViewProxy(GridView target) {
        super(target);
    }

    public void setAutoSize(boolean autoSize) {
        if(target.width == null || target.width < 0)
            target.width = autoSize ? -1 : -2;
        if(target.height == null || target.height < 0)
            target.height = autoSize ? -1 : -2;
    }

    public void setBoxed(boolean boxed) {
        target.boxed = boxed;
    }

    public void setTabVertical(boolean tabVertical) {
        target.tabVertical = tabVertical;
    }

    public void setQuickSearch(boolean quickSearch) {
        target.setQuickSearch(quickSearch);
    }

    public void setHeaderHeight(int headerHeight) {
        target.headerHeight = headerHeight;
    }

    public void setResizeOverflow(boolean resizeOverflow) {
        target.resizeOverflow = resizeOverflow;
    }

    public void setLineWidth(int lineWidth) {
        target.lineWidth = lineWidth;
    }

    public void setLineHeight(int lineHeight) {
        target.lineHeight = lineHeight;
    }
}
