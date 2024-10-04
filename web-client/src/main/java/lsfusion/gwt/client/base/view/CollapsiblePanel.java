package lsfusion.gwt.client.base.view;

import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.event.dom.client.ClickEvent;
import lsfusion.gwt.client.base.GwtClientUtils;

import java.util.function.Consumer;

public class CollapsiblePanel extends CaptionPanel {

    protected Widget header;

    public boolean collapsed = false;

    private boolean last;

    private final Consumer<Boolean> onCollapseHandler;

    public CollapsiblePanel(Widget header, boolean border, Consumer<Boolean> onCollapseHandler, boolean vertical, boolean last, GFlexAlignment alignmentHorz, GFlexAlignment alignmentVert) {
        super(header, border, vertical, last, alignmentHorz, alignmentVert);

        this.onCollapseHandler = onCollapseHandler;
        this.last = last;

//        headerButton.setEnabled(true);
        GwtClientUtils.addClassName(header, "collapsible");

        header.addDomHandler(event -> toggleCollapsed(), ClickEvent.getType());

        this.header = header;
    }

    public void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;

        if (collapsed) {
            GwtClientUtils.addClassName(header, "collapsed");
        } else {
            GwtClientUtils.removeClassName(header, "collapsed");
        }

        getChildren().get(last ? 0 : 1).setVisible(!collapsed);
    }

    private void toggleCollapsed() {
        setCollapsed(!collapsed);

        onCollapseHandler.accept(collapsed);
    }
}
