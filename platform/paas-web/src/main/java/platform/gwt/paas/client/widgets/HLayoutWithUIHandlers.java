package platform.gwt.paas.client.widgets;

import com.gwtplatform.mvp.client.HasUiHandlers;
import com.gwtplatform.mvp.client.UiHandlers;
import com.smartgwt.client.widgets.layout.HLayout;

public class HLayoutWithUIHandlers<U extends UiHandlers> extends HLayout implements HasUiHandlers<U> {
    protected U uiHandlers;

    public U getUiHandlers() {
        return uiHandlers;
    }

    public void setUiHandlers(U uiHandlers) {
        this.uiHandlers = uiHandlers;
    }
}
