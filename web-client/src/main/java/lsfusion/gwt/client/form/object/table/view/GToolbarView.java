package lsfusion.gwt.client.form.object.table.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.ResizableComplexPanel;

public class GToolbarView extends ResizableComplexPanel {

    public GToolbarView() {
        super();
        styleToolbar(getElement());
    }

    public static void styleToolbar(Element element) {
        GwtClientUtils.addClassName(element, "btn-toolbar");
    }
    public static void styleToolbarItem(Element element) {
        GwtClientUtils.addClassName(element, "btn-image");
        GwtClientUtils.addClassName(element, "btn-outline-secondary");
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
