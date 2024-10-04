package lsfusion.gwt.client.navigator.view;

import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.ResizableComplexPanel;

public class NavigatorPanel extends ResizableComplexPanel {

    public final ResizableComplexPanel panel;

    public NavigatorPanel(boolean vertical) {
        GwtClientUtils.addClassNames(this, "navbar", "p-0", "navbar-" + (vertical ? "vert" : "horz"));

        panel = new ResizableComplexPanel();
        GwtClientUtils.addClassNames(panel, "navbar-nav", vertical ? "navbar-nav-vert" : "navbar-nav-horz");

        add(panel);
    }
}
