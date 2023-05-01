package lsfusion.gwt.client.form.object.table.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.Dimension;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.ResizableComplexPanel;
import lsfusion.gwt.client.base.view.ResizableHorizontalPanel;
import lsfusion.gwt.client.view.StyleDefaults;

public class GToolbarView extends ResizableComplexPanel {

    public GToolbarView() {
        super();
        styleToolbar(getElement());
    }

    public static void styleToolbar(Element element) {
        element.addClassName("btn-toolbar");
    }
    public static void styleToolbarItem(Element element) {
        element.addClassName("btn-image");
        element.addClassName("btn-outline-secondary");
    }

    private boolean isEmpty = true;
    
    public void addComponent(Widget tool) {
        add(tool);
        isEmpty = false;
    }
    
    public boolean isEmpty() {
        return isEmpty;
    }
}
