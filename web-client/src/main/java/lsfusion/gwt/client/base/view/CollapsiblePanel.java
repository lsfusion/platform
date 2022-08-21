package lsfusion.gwt.client.base.view;

import com.google.gwt.event.dom.client.ClickEvent;
import lsfusion.gwt.client.form.design.GContainer;

import java.util.function.Consumer;

public class CollapsiblePanel extends CaptionPanel {
    public boolean collapsed = false;

    private final Consumer<Boolean> onCollapseHandler;

    public CollapsiblePanel(String caption, boolean border, Consumer<Boolean> onCollapseHandler) {
        super(caption, border);

        this.onCollapseHandler = onCollapseHandler;

//        headerButton.setEnabled(true);
        header.addStyleName("collapsible");

        header.addDomHandler(event -> toggleCollapsed(), ClickEvent.getType());
    }

    public void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;
        
        if (collapsed) {
            header.addStyleName("collapsed");
        } else {
            header.removeStyleName("collapsed");
        }

        for (int i = 1; i < getChildren().size(); i++) {
            getChildren().get(i).setVisible(!collapsed);
        }
    }

    private void toggleCollapsed() {
        setCollapsed(!collapsed);

        onCollapseHandler.accept(collapsed);
    }
}
