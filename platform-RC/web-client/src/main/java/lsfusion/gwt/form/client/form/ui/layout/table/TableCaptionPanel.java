package lsfusion.gwt.form.client.form.ui.layout.table;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.base.client.Dimension;
import lsfusion.gwt.base.client.EscapeUtils;
import lsfusion.gwt.base.client.GwtClientUtils;
import lsfusion.gwt.base.client.ui.ResizableVerticalPanel;

public class TableCaptionPanel extends ResizableVerticalPanel {

    private final Label legend;
    private final Widget content;

    public TableCaptionPanel(String title, Widget content) {

        this.content = content;

        this.legend = new Label(EscapeUtils.unicodeEscape(title));

        setStyleName("tableCaptionPanel");

        ResizableVerticalPanel container = new ResizableVerticalPanel();
        container.setSize("100%", "100%");
        container.setStyleName("tableCaptionPanelContainer");

        legend.setStyleName("tableCaptionPanelLegend");

        container.add(legend);
        container.add(this.content);

        container.setCellHeight(content, "100%");
        container.setCellWidth(content, "100%");

        content.setSize("100%", "100%");

        add(container);
        setCellHeight(container, "100%");
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension sz = GwtClientUtils.calculatePreferredSize(content);
        sz.height += legend.getOffsetHeight() + 5;
        sz.width += 5;
        return sz;
    }
}
