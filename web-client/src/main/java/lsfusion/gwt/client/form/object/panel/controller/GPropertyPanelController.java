package lsfusion.gwt.client.form.object.panel.controller;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.panel.view.PanelRenderer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static lsfusion.gwt.client.base.GwtClientUtils.isShowing;

public class GPropertyPanelController {
    private boolean columnsUpdated = true;

    public GPropertyDraw property;

    private final GFormController form;

    public Map<GGroupObjectValue, PanelRenderer> renderers;

    public FlexPanel renderersPanel;

    private final Supplier<Object> rowBackground;
    private final Supplier<Object> rowForeground;

    private List<GGroupObjectValue> columnKeys;
    private Map<GGroupObjectValue, Object> values;
    private Map<GGroupObjectValue, Object> captions;
    private Map<GGroupObjectValue, Object> showIfs;
    private Map<GGroupObjectValue, Object> readOnly;
    private Map<GGroupObjectValue, Object> cellBackgroundValues;
    private Map<GGroupObjectValue, Object> cellForegroundValues;

    public GPropertyPanelController(GPropertyDraw property, GFormController form, Supplier<Object> rowBackground, Supplier<Object> rowForeground) {
        this.property = property;
        this.form = form;
        renderers = new HashMap<>();

        this.rowBackground = rowBackground;
        this.rowForeground = rowForeground;

        renderersPanel = new FlexPanel(property.columnKeysVertical);
    }

    public Widget getView() {
        return renderersPanel.asWidget();
    }

    public void update() {
        if (columnsUpdated) {

            //adding new renderers
            Map<GGroupObjectValue, PanelRenderer> newRenderers = new HashMap<>();
            for (GGroupObjectValue columnKey : columnKeys) {
                if (showIfs == null || showIfs.get(columnKey) != null) {
                    PanelRenderer renderer = renderers.remove(columnKey);
                    if (renderer == null && !property.hide) {
                        if (renderersPanel.getWidgetCount() > 0) {
                            renderersPanel.add(GwtClientUtils.createHorizontalStrut(4));
                        }
                        
                        renderer = property.createPanelRenderer(form, columnKey);
                        renderer.setReadOnly(property.isReadOnly());
                        renderersPanel.addFill(renderer.getComponent());
                    }
                    if(renderer != null) {
                        newRenderers.put(columnKey, renderer);
                    }
                }
            }

            // removing old renderers
            for (PanelRenderer renderer : renderers.values())
                renderersPanel.remove(renderer.getComponent());
            renderers = newRenderers;

            columnsUpdated = false;

            if (property.drawAsync && !renderers.isEmpty()) {
                form.setAsyncView(renderers.get(columnKeys.get(0)));
            }
        }

        for (Map.Entry<GGroupObjectValue, PanelRenderer> e : renderers.entrySet()) {
            updateRenderer(e.getKey(), e.getValue());
        }
    }

    private void updateRenderer(GGroupObjectValue columnKey, PanelRenderer renderer) {
        renderer.setValue(values.get(columnKey));

        if (readOnly != null) {
            renderer.setReadOnly(readOnly.get(columnKey) != null);
        }

        Object background = rowBackground.get();
        if (background == null && cellBackgroundValues != null) {
            background = cellBackgroundValues.get(columnKey);
        }
        renderer.updateCellBackgroundValue(background == null ? property.background : background);

        Object foreground = rowForeground.get();
        if (foreground == null && cellForegroundValues != null) {
            foreground = cellForegroundValues.get(columnKey);
        }
        renderer.updateCellForegroundValue(foreground == null ? property.foreground : foreground);

        if (captions != null) {
            renderer.setCaption(property.getDynamicCaption(captions.get(columnKey)));
        }
    }

    public boolean focusFirstWidget() {
        if (renderers == null || renderers.isEmpty()) {
            return false;
        }

        PanelRenderer toFocus = columnKeys == null ? renderers.values().iterator().next() : renderers.get(columnKeys.get(0));
        if (isShowing(toFocus.getComponent())) {
            toFocus.focus();
            return true;
        }
        return false;
    }

    public void setPropertyValues(Map<GGroupObjectValue, Object> valueMap, boolean updateKeys) {
        if (updateKeys) {
            values.putAll(valueMap);
        } else {
            values = valueMap;
        }
    }

    public void setPropertyCaptions(Map<GGroupObjectValue,Object> captions) {
        this.captions = captions;
    }

    public void setReadOnlyValues(Map<GGroupObjectValue,Object> readOnly) {
        this.readOnly = readOnly;
    }

    public void setShowIfs(Map<GGroupObjectValue,Object> showIfs) {
        if (!GwtSharedUtils.nullEquals(this.showIfs, showIfs)) {
            this.showIfs = showIfs;
            columnsUpdated = true;
        }
    }

    public void setColumnKeys(List<GGroupObjectValue> columnKeys) {
        if (!GwtSharedUtils.nullEquals(this.columnKeys, columnKeys)) {
            this.columnKeys = columnKeys;
            columnsUpdated = true;
        }
    }

    public void setCellBackgroundValues(Map<GGroupObjectValue, Object> cellBackgroundValues) {
        this.cellBackgroundValues = cellBackgroundValues;
    }

    public void setCellForegroundValues(Map<GGroupObjectValue, Object> cellForegroundValues) {
        this.cellForegroundValues = cellForegroundValues;
    }
}
