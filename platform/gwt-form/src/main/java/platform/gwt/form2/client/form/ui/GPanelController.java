package platform.gwt.form2.client.form.ui;

import platform.gwt.form2.shared.view.GPropertyDraw;
import platform.gwt.form2.shared.view.changes.GGroupObjectValue;
import platform.gwt.form2.shared.view.panel.PanelRenderer;
import platform.gwt.utils.GwtSharedUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GPanelController {
    private final GFormController form;
    private GFormLayout formLayout;

    private List<GPropertyDraw> orderedProperties = new ArrayList<GPropertyDraw>();
    private Map<GPropertyDraw, PanelRenderer> properties = new HashMap<GPropertyDraw, PanelRenderer>();
    private HashMap<GPropertyDraw, Object> values = new HashMap<GPropertyDraw, Object>();

    private Map<GPropertyDraw, Map<GGroupObjectValue, Object>> cellBackgroundValues = new HashMap<GPropertyDraw, Map<GGroupObjectValue, Object>>();
    private Map<GPropertyDraw, Map<GGroupObjectValue, Object>> cellForegroundValues = new HashMap<GPropertyDraw, Map<GGroupObjectValue, Object>>();
    private Object rowBackground;
    private Object rowForeground;
    private Map<GPropertyDraw, Map<GGroupObjectValue, Object>> propertyCaptions = new HashMap<GPropertyDraw, Map<GGroupObjectValue, Object>>();

    public GPanelController(GFormController iform, GFormLayout formLayout) {
        form = iform;
        this.formLayout = formLayout;
    }

    public void addProperty(GPropertyDraw property) {
        if (!containsProperty(property)) {
            int ins = GwtSharedUtils.relativePosition(property, form.getPropertyDraws(), orderedProperties);
            orderedProperties.add(ins, property);
        }
    }

    public void removeProperty(GPropertyDraw property) {
        if (containsProperty(property)) {
            formLayout.remove(property);
            orderedProperties.remove(property);
            properties.remove(property);
            values.remove(property);
            propertyCaptions.remove(property);
            cellBackgroundValues.remove(property);
            cellForegroundValues.remove(property);
        }
    }

    public void setPropertyValues(GPropertyDraw property, Map<GGroupObjectValue, Object> valueMap, boolean updateKeys) {
        //todo: use updateKeys for columnKeys logic
        if (valueMap != null && !valueMap.isEmpty()) {
            values.put(property, valueMap.values().iterator().next());
        }
    }

    public void update() {
        List<GPropertyDraw> toDraw = new ArrayList<GPropertyDraw>();
        for (GPropertyDraw property : orderedProperties) {
            if (property.container != null) {
                PanelRenderer renderer = null;
                //если propertyCaption == null, то элемент должен быть скрыт
                if (propertyCaptions.get(property) == null || propertyCaptions.get(property).values().iterator().next() != null) {
                    renderer = properties.get(property);
                    if (renderer == null) {
                        renderer = property.createPanelRenderer(form);
                        properties.put(property, renderer);
                        formLayout.add(property, renderer.getComponent(), property.container.children.indexOf(property));
                    }
                    toDraw.add(property);
                }

                Object propValue = values.get(property);
                if (renderer != null) {
                    renderer.setValue(propValue);
                }
            }
        }

        for (GPropertyDraw property : orderedProperties) {
            if (!toDraw.contains(property)) {
                formLayout.remove(property);
            }
        }

        for (GPropertyDraw property : orderedProperties) {
            PanelRenderer renderer = properties.get(property);

            if (renderer != null) {
                Map<GGroupObjectValue, Object> caption = propertyCaptions.get(property);
                if (caption != null) {
                    String dynamicCaption = property.getDynamicCaption(caption.values().iterator().next());
                    renderer.setCaption(dynamicCaption);
                }

                Object background = rowBackground != null ? rowBackground : null;
                if (background == null && cellBackgroundValues.get(property) != null) {
                    background = cellBackgroundValues.get(property).values().iterator().next();
                }
                renderer.updateCellBackgroundValue(background);

                Object foreground = rowForeground != null ? rowForeground : null;
                if (foreground == null && cellForegroundValues.get(property) != null) {
                    foreground = cellForegroundValues.get(property).values().iterator().next();
                }
                renderer.updateCellForegroundValue(foreground);
            }
        }
    }

    public boolean isEmpty() {
        return orderedProperties.size() == 0;
    }

    public void hide() {
        for (Map.Entry<GPropertyDraw, PanelRenderer> e : properties.entrySet()) {
            e.getValue().getComponent().setVisible(false);
        }
    }

    public void show() {
        for (Map.Entry<GPropertyDraw, PanelRenderer> e : properties.entrySet()) {
            e.getValue().getComponent().setVisible(true);
        }
    }

    public boolean containsProperty(GPropertyDraw property) {
        return orderedProperties.contains(property);
    }

    public void updateCellBackgroundValues(GPropertyDraw propertyDraw, Map<GGroupObjectValue, Object> values) {
        cellBackgroundValues.put(propertyDraw, values);
    }

    public void updateCellForegroundValues(GPropertyDraw propertyDraw, Map<GGroupObjectValue, Object> values) {
        cellForegroundValues.put(propertyDraw, values);
    }

    public void updatePropertyCaptions(GPropertyDraw propertyDraw, Map<GGroupObjectValue, Object> values) {
        propertyCaptions.put(propertyDraw, values);
    }

    public void updateRowBackgroundValue(Object color) {
        rowBackground = color;
    }

    public void updateRowForegroundValue(Object color) {
        rowForeground = color;
    }

    public void updateColumnKeys(GPropertyDraw property, List<GGroupObjectValue> columnKeys) {
        //todo:

    }
}
