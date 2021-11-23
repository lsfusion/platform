package lsfusion.gwt.client.form.object.table.view;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.Dimension;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.ResizableHorizontalPanel;
import lsfusion.gwt.client.view.StyleDefaults;

import static lsfusion.gwt.client.base.GwtClientUtils.getOffsetSize;

public class GToolbarView extends ResizableHorizontalPanel {
    private boolean isEmpty = true;
    
    public void addComponent(Widget tool) {
        add(tool);
        tool.addStyleName("toolbarItem");
        isEmpty = false;
    }
    
    public boolean isEmpty() {
        return isEmpty;
    }
    
    public void addSeparator() {
        if (!isEmpty()) {
            addComponent(GwtClientUtils.createVerticalSeparator(StyleDefaults.COMPONENT_HEIGHT));
        }
    }
}
