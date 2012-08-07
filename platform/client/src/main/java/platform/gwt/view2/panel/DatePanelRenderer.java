package platform.gwt.view2.panel;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.datepicker.client.DateBox;
import platform.gwt.view2.GPropertyDraw;

import java.sql.Time;
import java.util.Date;

public class DatePanelRenderer implements PanelRenderer {

    protected final Label label;
    protected final DateBox dateBox;
    protected final HorizontalPanel panel;

    public DatePanelRenderer(GPropertyDraw property, DateTimeFormat format) {
        label = new Label(property.getCaptionOrEmpty() + ": ");

        dateBox = new DateBox();
        dateBox.setFormat(new DateBox.DefaultFormat(format));

        panel = new HorizontalPanel();
        panel.add(label);
        panel.add(dateBox);
        panel.setCellVerticalAlignment(label, HasVerticalAlignment.ALIGN_MIDDLE);
    }

    @Override
    public Widget getComponent() {
        return panel;
    }

    @Override
    public void setValue(Object value) {
        if (value instanceof Time) {
            value = new Date(((Time) value).getTime());
        }
        dateBox.setValue((Date) value);
    }

    @Override
    public void setTitle(String title) {
        label.setText(title);
    }

    @Override
    public void updateCellBackgroundValue(Object value) {
        dateBox.getElement().getStyle().setBackgroundColor((String) value);
    }

    @Override
    public void updateCellForegroundValue(Object value) {
        dateBox.getElement().getStyle().setColor((String) value);
    }
}
