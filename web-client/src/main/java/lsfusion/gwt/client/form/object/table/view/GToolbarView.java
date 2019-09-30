package lsfusion.gwt.client.form.object.table.view;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.Dimension;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.ResizableHorizontalPanel;

import static lsfusion.gwt.client.base.GwtClientUtils.getOffsetSize;

public class GToolbarView extends ResizableHorizontalPanel {
    private int BUTTON_HEIGHT = 20;
    
    public void addComponent(Widget tool) {
        add(tool);
        tool.addStyleName("toolbarItem");
    }
    
    public boolean isEmpty() {
        return getElement().getChildCount() == 0;
    }
    
    public void addSeparator() {
        if (!isEmpty()) {
            addComponent(GwtClientUtils.createVerticalSeparator(BUTTON_HEIGHT));
        }
    }

    @Override
    public Dimension getMaxPreferredSize() {
        return getOffsetSize(this);
    }
}
