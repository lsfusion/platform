package lsfusion.server.language.proxy;

import lsfusion.server.logics.form.interactive.design.object.GridView;

public class GridViewProxy extends ComponentViewProxy<GridView> {
    public GridViewProxy(GridView target) {
        super(target);
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

    public void setLineWidth(int lineWidth) {
        target.lineWidth = lineWidth;
    }

    public void setLineHeight(int lineHeight) {
        target.lineHeight = lineHeight;
    }
}
