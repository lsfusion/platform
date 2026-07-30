package lsfusion.gwt.client.navigator.view;

import lsfusion.gwt.client.base.view.ResizableComplexPanel;
import lsfusion.gwt.client.navigator.controller.GINavigatorController;
import lsfusion.gwt.client.navigator.window.GNavigatorWindow;

// a window whose own renderer decides WHERE each standard button goes: an HTML template with <Lsf:name> places
// (CustomNavigatorView) or a React tree with <Lsf name/> hosts (ReactNavigatorView). The park a button waits in until
// the renderer places it belongs to NavigatorButtons - that is the navigator's ParkedContainerView, not this class.
public abstract class ParkedNavigatorView extends GAbstractNavigatorView {

    protected final ResizableComplexPanel panel;
    protected final NavigatorButtons buttons;

    protected ParkedNavigatorView(GNavigatorWindow window, GINavigatorController navigatorController) {
        this(window, navigatorController, new ResizableComplexPanel());
    }

    private ParkedNavigatorView(GNavigatorWindow window, GINavigatorController navigatorController, ResizableComplexPanel panel) {
        super(window, panel);

        this.panel = panel;
        this.buttons = new NavigatorButtons(window, navigatorController, panel);
    }
}
