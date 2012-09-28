package platform.gwt.form2.client.navigator;

import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import platform.gwt.form2.shared.view.GNavigatorElement;
import platform.gwt.form2.shared.view.window.GNavigatorWindow;

import java.util.Set;

public abstract class GNavigatorView {
    protected GNavigatorWindow window;
    protected Widget component;
    protected GINavigatorController navigatorController;
    protected GNavigatorElement selected;

    public GNavigatorView(GNavigatorWindow window, GINavigatorController navigatorController) {
        this(window, null, navigatorController);
    }

    public GNavigatorView(GNavigatorWindow window, Widget component, GINavigatorController navigatorController) {
        this.window = window;
        this.navigatorController = navigatorController;
        setComponent(component);
    }

    public void setComponent(Widget component) {
        this.component = window.drawScrollBars ? new ScrollPanel(component) : component;
    }

    public Widget getView() {
        return component;
    }

    public Widget getComponent() {
        return window.drawScrollBars ? ((ScrollPanel) component).getWidget() : component;
    }

    public abstract void refresh(Set<GNavigatorElement> newElements);

    public abstract GNavigatorElement getSelectedElement();

    public abstract int getHeight();

    public abstract int getWidth();
}
