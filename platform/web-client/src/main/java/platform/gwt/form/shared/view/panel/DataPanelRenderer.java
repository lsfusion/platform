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
    protected final HorizontalPanel panel;

    private String caption;

    private EventTarget focusTargetAfterEdit;

    public DataPanelRenderer(GFormController form, GPropertyDraw property, GGroupObjectValue columnKey) {
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

        if (property.editKey != null) {
            HotkeyManager.get().addHotkeyBinding(form.getElement(), property.editKey, new Binding() {
                @Override
                public boolean onKeyPress(NativeEvent event, GKeyStroke key) {
                    focusTargetAfterEdit = event.getEventTarget();
                    valueTable.startEdit(0, 0, GEditBindingMap.CHANGE);
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
}
