package lsfusion.gwt.client.navigator.view;

import lsfusion.gwt.client.base.view.ResizableComplexPanel;

public class NavigatorPanel extends ResizableComplexPanel {

    public final ResizableComplexPanel panel;

    public NavigatorPanel(boolean vertical) {
        addStyleName("navbar p-0");

        addStyleName("navbar-" + (vertical ? "vert" : "horz"));

        panel = new ResizableComplexPanel();
        panel.addStyleName("navbar-nav");
        panel.addStyleName(vertical ? "navbar-nav-vert" : "navbar-nav-horz");

        add(panel);
    }
}
