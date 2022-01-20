package lsfusion.client.form.design.view;

import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.design.ClientContainer;
import lsfusion.client.form.design.view.widget.Widget;

import java.awt.*;

public class CollapsiblePanel extends CaptionPanel {
    public boolean collapsed = false;
    private ClientFormController formController;
    private ClientContainer childContainer;

    public CollapsiblePanel(ClientFormController formController, ClientContainer container, boolean vertical) {
        super(container.caption, vertical);
        this.formController = formController;
        this.childContainer = container;
    }

    @Override
    protected TitledBorder createBorder(String caption) {
        return new TitledBorder(caption, true) {
            @Override
            public void onCollapsedStateChanged(boolean collapsed) {
                CollapsiblePanel.this.collapsed = collapsed;
                
                for (Widget child : getChildren()) {
                    child.setVisible(!collapsed);
                }
                
                formController.setContainerCollapsed(childContainer, collapsed);
            }
        };
    }

    @Override
    public Dimension getPreferredSize() {
        if (collapsed) {
            return titledBorder.getMinimumSize(this);
        }
        return super.getPreferredSize();
    }
}
