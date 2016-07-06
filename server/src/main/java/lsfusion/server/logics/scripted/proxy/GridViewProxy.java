package lsfusion.server.logics.scripted.proxy;

import lsfusion.server.form.view.GridView;

public class GridViewProxy extends ComponentViewProxy<GridView> {
    public GridViewProxy(GridView target) {
        super(target);
    }
    
    public void setTabVertical(boolean tabVertical) {
        target.tabVertical = tabVertical;
    }

    public void setAutoHide(boolean autoHide) {
        target.autoHide = autoHide;
    }

    public void setQuickSearch(boolean quickSearch) {
        target.setQuickSearch(quickSearch);
    }
}
