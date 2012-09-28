package platform.gwt.form2.client.window;

import com.google.gwt.user.client.ui.Widget;
import platform.gwt.form2.shared.view.window.GAbstractWindow;

public class SimpleWindowElement extends WindowElement {
    public GAbstractWindow window;

    public SimpleWindowElement(WindowContainer main, GAbstractWindow window) {
        super(main);
        this.window = window;
    }

    @Override
    public Widget getView() {
        return main.getWindowView(window);
    }
}
