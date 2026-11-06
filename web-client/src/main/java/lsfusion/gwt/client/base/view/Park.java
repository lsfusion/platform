package lsfusion.gwt.client.base.view;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

// where a view of the platform's waits while nothing places it - a navigator window's buttons, the forms window's forms,
// the log's messages, the navigator's windows. It stays in the document and stays a logical child of the panel that is
// drawn in, or GWT would detach the widget (and a button's tooltip with it); its element is OUTSIDE that panel, because
// what draws the panel overwrites its content - React clears its root, a template its innerHTML. display:none, as a
// background tab is
public class Park {

    private final ResizableComplexPanel panel;
    private final Element element = Document.get().createDivElement();

    public Park(ResizableComplexPanel panel) {
        this.panel = panel;

        element.getStyle().setDisplay(Style.Display.NONE);
        RootPanel.getBodyElement().appendChild(element);
    }

    // a new view: a logical child of the panel, waiting here until something places it
    public void add(Widget view) {
        panel.addToElement(view, element);
    }

    // a placed view taken back: still the panel's child, attached and alive, ready to be placed again
    public void put(Widget view) {
        element.appendChild(view.getElement());
    }
}
