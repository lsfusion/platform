package platform.server.logics.scripted.proxy;

import platform.server.form.view.GridView;

public class GridViewProxy extends ComponentViewProxy<GridView> {
    public GridViewProxy(GridView target) {
        super(target);
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
