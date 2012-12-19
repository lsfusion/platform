package platform.gwt.form.shared.view.panel;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.*;
import platform.gwt.base.shared.GwtSharedUtils;
import platform.gwt.form.client.HotkeyManager;
import platform.gwt.form.client.form.ui.GFormController;
import platform.gwt.form.client.form.ui.GSinglePropertyTable;
import platform.gwt.form.shared.view.GEditBindingMap;
import platform.gwt.form.shared.view.GKeyStroke;
import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.changes.GGroupObjectValue;
import platform.gwt.form.shared.view.changes.dto.ColorDTO;

import static platform.gwt.form.client.HotkeyManager.Binding;

public class DataPanelRenderer implements PanelRenderer {

    protected final Label label;
    protected final GSinglePropertyTable valueTable;
    protected final CellPanel panel;
    protected ResizeLayoutPanel gridPanel;

    private String caption;

    private EventTarget focusTargetAfterEdit;

    private String componentWidth = null;

    public DataPanelRenderer(GFormController form, final GPropertyDraw property, GGroupObjectValue columnKey) {
        label = new Label(caption = property.getEditCaption());
        label.addStyleName("customFontPresenter");

        int propertyPixelWidth = property.getPreferredPixelWidth();

        valueTable = new GSinglePropertyTable(form, property, columnKey) {
            @Override
            public void onEditFinished() {
                if (focusTargetAfterEdit != null) {
                    Element.as(focusTargetAfterEdit).focus();
                    focusTargetAfterEdit = null;
                } else {
                    setFocus(true);
                }
            }
        };

        gridPanel = new ResizeLayoutPanel();
        gridPanel.addStyleName("dataPanelRendererGridPanel");
        gridPanel.add(valueTable);

        panel = property.panelLabelAbove ? new VerticalPanel() : new HorizontalPanel();
        panel.addStyleName("dataPanelRendererPanel");
        panel.setWidth("100%");

        panel.add(label);
        panel.setCellVerticalAlignment(label, HasVerticalAlignment.ALIGN_MIDDLE);
        panel.setCellHorizontalAlignment(label, HasAlignment.ALIGN_CENTER);

        panel.add(gridPanel);
        panel.setCellWidth(gridPanel, "100%");

        if (property.preferredHeight != -1) {
            gridPanel.setHeight(property.preferredHeight + "px");
        } else {
            gridPanel.setHeight(property.getPreferredHeight());
        }

        if (property.preferredWidth != -1) {
            gridPanel.setWidth(property.preferredWidth + "px");
        } else if (property.fillHorizontal > 0) {
            componentWidth =  property.container.getChildPercentSize(property, true);
            // в этом случае при высте < 34px небольшой косяк в FF
        } else {
            valueTable.setTableWidth(propertyPixelWidth, Style.Unit.PX);
            gridPanel.setWidth(propertyPixelWidth + "px");
        }

        if (property.editKey != null) {
            HotkeyManager.get().addHotkeyBinding(form.getElement(), property.editKey, new Binding() {
                @Override
                public boolean onKeyPress(NativeEvent event, GKeyStroke key) {
                    focusTargetAfterEdit = event.getEventTarget();
                    valueTable.editCellAt(0, 0, GEditBindingMap.CHANGE);
                    return true;
                }
            });
        }
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
    public void setDefaultIcon() {
    }

    @Override
    public void setIcon(String iconPath) {
    }

    @Override
    public void updateCellBackgroundValue(Object value) {
        valueTable.setBackground((ColorDTO) value);
    }

    @Override
    public void updateCellForegroundValue(Object value) {
        valueTable.setForeground((ColorDTO) value);
    }

    @Override
    public void focus() {
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                valueTable.setFocus(true);
            }
        });
    }

    @Override
    public String getWidth() {
        return componentWidth;
    }
}
