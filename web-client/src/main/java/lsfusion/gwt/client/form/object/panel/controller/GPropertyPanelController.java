package lsfusion.gwt.client.form.object.panel.controller;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.panel.view.ActionPanelRenderer;
import lsfusion.gwt.client.form.property.panel.view.PanelRenderer;

import java.util.ArrayList;

import static lsfusion.gwt.client.base.GwtClientUtils.isShowing;

public class GPropertyPanelController {
    private boolean columnsUpdated = true;

    public GPropertyDraw property;

    private final GFormController form;

    public NativeHashMap<GGroupObjectValue, PanelRenderer> renderers;

    public Panel renderersPanel;

    private ArrayList<GGroupObjectValue> columnKeys;
    // it doesn't make sense to make this maps Native since they come from server and are built anyway
    private NativeHashMap<GGroupObjectValue, Object> values;
    private NativeHashMap<GGroupObjectValue, Object> captions;
    private NativeHashMap<GGroupObjectValue, Object> showIfs;
    private NativeHashMap<GGroupObjectValue, Object> readOnly;
    private NativeHashMap<GGroupObjectValue, Object> cellBackgroundValues;
    private NativeHashMap<GGroupObjectValue, Object> cellForegroundValues;

    private NativeHashMap<GGroupObjectValue, Object> images;

    public GPropertyPanelController(GPropertyDraw property, GFormController form) {
        this.property = property;
        this.form = form;
        renderers = new NativeHashMap<>();

        renderersPanel = new Panel(property.panelColumnVertical); // needed for groups-to-columns
    }

    public interface CaptionContainer {
        void put(Widget widget, Pair<Integer, Integer> valueSizes);
    }

    public static class Panel extends FlexPanel {

        public Panel(boolean vertical) {
            super(vertical);
        }

        public CaptionContainer captionContainer;
    }

    public Widget getView() {
        return renderersPanel.asWidget();
    }

    public void update() {
        if (columnsUpdated) {

            //adding new renderers
            NativeHashMap<GGroupObjectValue, PanelRenderer> newRenderers = new NativeHashMap<>();
            for (GGroupObjectValue columnKey : columnKeys) {
                if (showIfs == null || showIfs.get(columnKey) != null) {
                    PanelRenderer renderer = renderers.remove(columnKey);
                    if (renderer == null && (!property.hide || property.hasKeyBinding())) {
                        if (renderersPanel.getWidgetCount() > 0) {
                            renderersPanel.add(GwtClientUtils.createHorizontalStrut(4));
                        }
                        
                        PanelRenderer newRenderer = property.createPanelRenderer(form, columnKey, renderersPanel.captionContainer);
                        newRenderer.setReadOnly(property.isReadOnly());
                        Widget component = newRenderer.getComponent();
                        if(!property.hide) {
                            renderersPanel.addFill(component);
                        }
                        newRenderer.bindingEventIndices = form.addPropertyBindings(property, newRenderer::onBinding, component);

                        renderer = newRenderer;
                    }
                    if(renderer != null) {
                        newRenderers.put(columnKey, renderer);
                    }
                }
            }

            // removing old renderers
            renderers.foreachValue(renderer -> {
                form.removePropertyBindings(renderer.bindingEventIndices);
                if (!property.hide) {
                    renderersPanel.remove(renderer.getComponent());
                }
            });
            renderers = newRenderers;

            columnsUpdated = false;

            if (property.drawAsync && !renderers.isEmpty()) {
                form.setAsyncView((ActionPanelRenderer)renderers.get(columnKeys.get(0)));
            }
        }

        renderers.foreachEntry(this::updateRenderer);
    }

    private void updateRenderer(GGroupObjectValue columnKey, PanelRenderer renderer) {
        renderer.updateValue(values.get(columnKey));

        if (readOnly != null) {
            renderer.setReadOnly(readOnly.get(columnKey) != null);
        }

        Object background = null;
        if (cellBackgroundValues != null) {
            background = cellBackgroundValues.get(columnKey);
        }
        renderer.updateCellBackgroundValue(background == null ? property.background : background);

        Object foreground = null;
        if (cellForegroundValues != null) {
            foreground = cellForegroundValues.get(columnKey);
        }
        renderer.updateCellForegroundValue(foreground == null ? property.foreground : foreground);

        if (captions != null) {
            renderer.setCaption(property.getDynamicCaption(captions.get(columnKey)));
        }

        if (images != null && renderer instanceof ActionPanelRenderer) {
            Object image = images.get(columnKey);
            ((ActionPanelRenderer)renderer).setDynamicImage(image != null ? image : "null");
        }
    }

    public boolean focusFirstWidget() {
        if (renderers == null || renderers.isEmpty()) {
            return false;
        }

        PanelRenderer toFocus = columnKeys == null ? renderers.firstValue() : renderers.get(columnKeys.get(0));
        if (isShowing(toFocus.getComponent())) {
            toFocus.focus();
            return true;
        }
        return false;
    }

    public void setPropertyValues(NativeHashMap<GGroupObjectValue, Object> valueMap, boolean updateKeys) {
        if (updateKeys) {
            values.putAll(valueMap);
        } else {
            values = valueMap;
        }
    }

    public void setPropertyCaptions(NativeHashMap<GGroupObjectValue, Object> captions) {
        this.captions = captions;
    }

    public void setReadOnlyValues(NativeHashMap<GGroupObjectValue, Object> readOnly) {
        this.readOnly = readOnly;
    }

    public void setShowIfs(NativeHashMap<GGroupObjectValue, Object> showIfs) {
        if (!GwtSharedUtils.nullEquals(this.showIfs, showIfs)) {
            this.showIfs = showIfs;
            columnsUpdated = true;
        }
    }

    public void setColumnKeys(ArrayList<GGroupObjectValue> columnKeys) {
        if (!GwtSharedUtils.nullEquals(this.columnKeys, columnKeys)) {
            this.columnKeys = columnKeys;
            columnsUpdated = true;
        }
    }

    public void setCellBackgroundValues(NativeHashMap<GGroupObjectValue, Object> cellBackgroundValues) {
        this.cellBackgroundValues = cellBackgroundValues;
    }

    public void setCellForegroundValues(NativeHashMap<GGroupObjectValue, Object> cellForegroundValues) {
        this.cellForegroundValues = cellForegroundValues;
    }

    public void setImages(NativeHashMap<GGroupObjectValue, Object> images) {
        this.images = images;
    }
}
