package lsfusion.gwt.form.shared.view.panel;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.ui.*;
import lsfusion.gwt.base.client.ui.ResizableHorizontalPanel;
import lsfusion.gwt.base.client.ui.ResizableSimplePanel;
import lsfusion.gwt.base.client.ui.ResizableVerticalPanel;
import lsfusion.gwt.base.shared.GwtSharedUtils;
import lsfusion.gwt.form.client.form.ui.GFormController;
import lsfusion.gwt.form.client.form.ui.GSinglePropertyTable;
import lsfusion.gwt.form.client.form.ui.TooltipManager;
import lsfusion.gwt.form.shared.view.GEditBindingMap;
import lsfusion.gwt.form.shared.view.GKeyStroke;
import lsfusion.gwt.form.shared.view.GPropertyDraw;
import lsfusion.gwt.form.shared.view.changes.GGroupObjectValue;
import lsfusion.gwt.form.shared.view.changes.dto.ColorDTO;

import static lsfusion.gwt.form.client.HotkeyManager.Binding;

public class DataPanelRenderer implements PanelRenderer {
    protected GPropertyDraw property;

    protected final Label label;
    protected final GSinglePropertyTable valueTable;
    protected final CellPanel panel;
    protected ResizableSimplePanel gridPanel;

    private String caption;
    private String tooltip;

    private EventTarget focusTargetAfterEdit;

    private String componentWidth = null;

    public DataPanelRenderer(GFormController form, GPropertyDraw iproperty, GGroupObjectValue columnKey) {
        this.property = iproperty;
        label = new Label(caption = property.getEditCaption());
        tooltip = property.getTooltipText(property.getCaptionOrEmpty());

        label.addMouseOverHandler(new MouseOverHandler() {
            @Override
            public void onMouseOver(MouseOverEvent event) {
                TooltipManager.get().showTooltip(event.getClientX(), event.getClientY(), tooltip);
            }
        });
        label.addMouseOutHandler(new MouseOutHandler() {
            @Override
            public void onMouseOut(MouseOutEvent event) {
                TooltipManager.get().hideTooltip();
            }
        });
        label.addMouseMoveHandler(new MouseMoveHandler() {
            @Override
            public void onMouseMove(MouseMoveEvent event) {
                TooltipManager.get().updateMousePosition(event.getClientX(), event.getClientY());
            }
        });

        if (property.headerFont != null) {
            label.getElement().getStyle().setProperty("font", property.headerFont.getFullFont());
        }

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

        if (!property.focusable) {
            valueTable.setTableFocusable(false);
        }

        gridPanel = new ResizableSimplePanel();
        gridPanel.addStyleName("dataPanelRendererGridPanel");
        gridPanel.add(valueTable);
        valueTable.setSize("100%", "100%");

        panel = property.panelLabelAbove ? new ResizableVerticalPanel() : new ResizableHorizontalPanel();
        panel.addStyleName("dataPanelRendererPanel");
        panel.setWidth("100%");

        panel.add(label);
        panel.setCellVerticalAlignment(label, HasVerticalAlignment.ALIGN_MIDDLE);
        panel.setCellHorizontalAlignment(label, HasAlignment.ALIGN_CENTER);

        panel.add(gridPanel);
        panel.setCellWidth(gridPanel, "100%");
        panel.setCellVerticalAlignment(gridPanel, HasVerticalAlignment.ALIGN_MIDDLE);

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

        valueTable.getElement().setPropertyObject("groupObject", property.groupObject);
        if (property.editKey != null) {
            form.addHotkeyBinding(property.groupObject, property.editKey, new Binding() {
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
    public void setReadOnly(boolean readOnly) {
        valueTable.setReadOnly(readOnly);
    }

    @Override
    public void setCaption(String caption) {
        if (!GwtSharedUtils.nullEquals(this.caption, caption)) {
            this.caption = caption;
            label.setText(property.getEditCaption(caption));
            tooltip = property.getTooltipText(caption);
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
        valueTable.setFocus(true);
    }

    @Override
    public String getWidth() {
        return componentWidth;
    }
}
