package platform.gwt.paas.client.widgets;

import com.gwtplatform.mvp.client.HasUiHandlers;
import com.gwtplatform.mvp.client.UiHandlers;

public class ToolbarWithUIHandlers<U extends UiHandlers> extends Toolbar implements HasUiHandlers<U> {
    protected U uiHandlers;

    public U getUiHandlers() {
        return uiHandlers;
    }

    public void setUiHandlers(U uiHandlers) {
        this.uiHandlers = uiHandlers;
    }
}
