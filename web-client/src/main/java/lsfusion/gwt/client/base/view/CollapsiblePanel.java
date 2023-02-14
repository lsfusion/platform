package lsfusion.gwt.client.base.view;

import lsfusion.gwt.client.base.TooltipManager;
import com.google.gwt.event.dom.client.ClickEvent;
import lsfusion.gwt.client.form.design.GContainer;

import java.util.function.Consumer;

public class CollapsiblePanel extends CaptionPanel {
    public boolean collapsed = false;

    private final Consumer<Boolean> onCollapseHandler;

    public CollapsiblePanel(GContainer container, boolean border, Consumer<Boolean> onCollapseHandler) {
        super(container.caption, border);

        this.onCollapseHandler = onCollapseHandler;

//        headerButton.setEnabled(true);
        header.addStyleName("collapsible");

        header.addDomHandler(event -> toggleCollapsed(), ClickEvent.getType());

        TooltipManager.registerWidget(header, new TooltipManager.TooltipHelper() {
            @Override
            public String getTooltip() {
                return container.getTooltipText(container.caption);
            }

            @Override
            public String getPath() {
                return container.getPath();
            }

            @Override
            public String getCreationPath() {
                return container.getCreationPath();
            }

            @Override
            public boolean stillShowTooltip() {
                return true;
            }
        });
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
