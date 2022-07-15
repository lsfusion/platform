package lsfusion.gwt.client.base.view;

import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GContainer;

public class CollapsiblePanel extends CaptionPanel {
    public boolean collapsed = false;

    private GFormController formController;
    private GContainer container;
    
    public CollapsiblePanel(GFormController formController, GContainer container) {
        super(container.caption);
        this.formController = formController;
        this.container = container;

        headerButton.setEnabled(true);
        headerButton.addStyleName("collapsible");
        headerButton.addClickHandler(event -> toggleCollapsed());
    }

    public void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;
        
        if (collapsed) {
            headerButton.addStyleName("collapsed");
        } else {
            headerButton.removeStyleName("collapsed");
        }

        for (int i = 1; i < getChildren().size(); i++) {
            getChildren().get(i).setVisible(!collapsed);
        }
    }

    private void toggleCollapsed() {
        setCollapsed(!collapsed);

        if (formController != null)
            formController.setContainerCollapsed(container, collapsed);
    }
}
