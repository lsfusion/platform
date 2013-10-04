package lsfusion.gwt.form.client.form.ui.layout.table;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.base.client.EscapeUtils;
import lsfusion.gwt.base.client.ui.ResizableVerticalPanel;

public class TableCaptionPanel extends ResizableVerticalPanel {
    public TableCaptionPanel(String title, Widget content) {
        setStyleName("tableCaptionPanel");

        ResizableVerticalPanel container = new ResizableVerticalPanel();
        container.setSize("100%", "100%");
        container.setStyleName("tableCaptionPanelContainer");

        Label legend = new Label(EscapeUtils.unicodeEscape(title));
        legend.setStyleName("tableCaptionPanelLegend");

        container.add(legend);
        container.add(content);

        container.setCellHeight(content, "100%");
        container.setCellWidth(content, "100%");

        content.setSize("100%", "100%");

        add(container);
        setCellHeight(container, "100%");
    }
}
