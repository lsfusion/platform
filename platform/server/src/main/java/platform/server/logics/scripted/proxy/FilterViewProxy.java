package platform.server.logics.scripted.proxy;

import platform.server.form.view.FilterView;

public class FilterViewProxy extends ComponentViewProxy<FilterView> {
    public FilterViewProxy(FilterView target) {
        super(target);
    }

    public void setVisible(boolean visible) {
        target.visible = visible;
    }
}
