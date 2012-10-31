package platform.gwt.form2.shared.view.panel;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.*;
import platform.gwt.form2.client.form.ui.GFormController;
import platform.gwt.form2.client.form.ui.GSinglePropertyTable;
import platform.gwt.form2.shared.view.GPropertyDraw;
import platform.gwt.form2.shared.view.changes.GGroupObjectValue;
import platform.gwt.form2.shared.view.changes.dto.ColorDTO;
import platform.gwt.base.shared.GwtSharedUtils;

public class DataPanelRenderer implements PanelRenderer {

    protected final Label label;
    protected final GSinglePropertyTable valueTable;
    protected final HorizontalPanel panel;

    private String caption;

    public DataPanelRenderer(GFormController form, GPropertyDraw property, GGroupObjectValue columnKey) {
        label = new Label(caption = property.getCaptionOrEmpty());
        label.addStyleName("customFontPresenter");

        int propertyPixelWidth = property.getPreferredPixelWidth();

        valueTable = new GSinglePropertyTable(form, property, columnKey);

        valueTable.setTableWidth(propertyPixelWidth, Style.Unit.PX);
        valueTable.setWidth("100%");
        valueTable.setHeight("100%");

        ResizeLayoutPanel gridPanel = new ResizeLayoutPanel();
        gridPanel.setPixelSize(propertyPixelWidth, 16);
        gridPanel.addStyleName("dataPanelRendererGridPanel");
        gridPanel.add(valueTable);

        panel = new HorizontalPanel();
        panel.add(label);
        panel.add(gridPanel);
        panel.setCellVerticalAlignment(label, HasVerticalAlignment.ALIGN_MIDDLE);
        panel.addStyleName("dataPanelRendererPanel");
    }

    @Override
    public Widget getComponent() {
        return panel;
    }

    @Override
    public void setValue(Object value) {
        valueTable.setValue(value);
    }

    @Override
    public void setCaption(String caption) {
        if (!GwtSharedUtils.nullEquals(this.caption, caption)) {
            this.caption = caption;
            label.setText(caption);
        }
    }

    @Override
    public void updateCellBackgroundValue(Object value) {
        valueTable.setBackground((ColorDTO) value);
    }

    @Override
    public void updateCellForegroundValue(Object value) {
        valueTable.setForeground((ColorDTO) value);
    }
}
