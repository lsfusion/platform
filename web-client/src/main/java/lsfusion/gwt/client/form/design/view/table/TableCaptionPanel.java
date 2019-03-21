package lsfusion.gwt.client.form.design.view.table;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.Dimension;
import lsfusion.gwt.client.base.EscapeUtils;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.ResizableVerticalPanel;

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
    public Dimension getMaxPreferredSize() {
        Dimension sz = GwtClientUtils.calculateMaxPreferredSize(content);
        sz.height += legend.getOffsetHeight() + 5;
        sz.width += 5;
        return sz;
    }
}
