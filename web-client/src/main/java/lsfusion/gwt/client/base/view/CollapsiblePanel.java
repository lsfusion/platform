package lsfusion.gwt.client.base.view;

import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.event.dom.client.ClickEvent;

import java.util.function.Consumer;

public class CollapsiblePanel extends CaptionPanel {

    protected Widget header;

    public boolean collapsed = false;

    private boolean last;

    private final Consumer<Boolean> onCollapseHandler;

    public CollapsiblePanel(Widget header, boolean border, Consumer<Boolean> onCollapseHandler, boolean vertical, boolean last, GFlexAlignment alignment) {
        super(header, border, vertical, last, alignment);

        this.onCollapseHandler = onCollapseHandler;
        this.last = last;

//        headerButton.setEnabled(true);
        header.addStyleName("collapsible");

        header.addDomHandler(event -> toggleCollapsed(), ClickEvent.getType());

        this.header = header;
    }

    public void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;

        if (collapsed) {
            header.addStyleName("collapsed");
        } else {
            header.removeStyleName("collapsed");
        }

        getChildren().get(last ? 0 : 1).setVisible(!collapsed);
    }

    private void toggleCollapsed() {
        setCollapsed(!collapsed);

        onCollapseHandler.accept(collapsed);
    }
}
