package lsfusion.server.language.proxy;

import lsfusion.server.form.view.FilterView;

public class FilterViewProxy extends ComponentViewProxy<FilterView> {
    public FilterViewProxy(FilterView target) {
        super(target);
    }

    public void setVisible(boolean visible) {
        target.visible = visible;
    }
}
