package platform.gwt.form2.client.form.ui;

import com.google.gwt.user.client.ui.Button;
import platform.gwt.utils.GwtSharedUtils;
import platform.gwt.view2.GPropertyDraw;
import platform.gwt.view2.changes.GGroupObjectValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GPanelController {
    private final GFormController form;
    private GFormLayout formLayout;

    private List<GPropertyDraw> orderedProperties = new ArrayList<GPropertyDraw>();
//    private Map<GPropertyDraw, GTypeRenderer> properties = new HashMap<GPropertyDraw, GTypeRenderer>();
    private Map<GPropertyDraw, Button> properties = new HashMap<GPropertyDraw, Button>();
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

    public void setValue(GPropertyDraw property, Map<GGroupObjectValue, Object> valueMap) {
        if (valueMap != null && !valueMap.isEmpty()) {
            values.put(property, valueMap.values().iterator().next());
        }
    }

    public void update() {
        List<GPropertyDraw> toDraw = new ArrayList<GPropertyDraw>();
        for (GPropertyDraw property : orderedProperties) {
//            GTypeRenderer renderer = null;
            Button renderer = null;
            if (!(propertyCaptions.get(property) != null && propertyCaptions.get(property).values().iterator().next() == null)) {
                renderer = properties.get(property);
                if (renderer == null) {
                    renderer = new Button(property.caption == null || property.caption.isEmpty() ? "[" + property.sID + "]" : property.caption);
                    properties.put(property, renderer);
                    formLayout.add(property, renderer, property.container.children.indexOf(property));
                }
                toDraw.add(property);
            }

            Object propValue = values.get(property);
//            if (renderer != null)
//                renderer.setValue(propValue);
        }

        for (GPropertyDraw property : orderedProperties) {
            if (!toDraw.contains(property)) {
                formLayout.remove(property);
            }
        }

        for (GPropertyDraw property : orderedProperties) {
            Button renderer = properties.get(property);

            if (renderer != null) {
                Map<GGroupObjectValue, Object> caption = propertyCaptions.get(property);
                if (caption != null) {
                    renderer.setTitle(caption.values().iterator().next().toString());
                }

                Object background = rowBackground != null ? rowBackground : null;
                if (background == null && cellBackgroundValues.get(property) != null)  {
                    background = cellBackgroundValues.get(property).values().iterator().next();
                }
//                renderer.updateCellBackgroundValue(background);

                Object foreground = rowForeground != null ? rowForeground : null;
                if (foreground == null && cellForegroundValues.get(property) != null) {
                    foreground = cellForegroundValues.get(property).values().iterator().next();
                }
//                renderer.updateCellForegroundValue(foreground);
            }
        }
    }

//    private GTypeRenderer createPropertyRenderer(GPropertyDraw property) {
//        GTypeRenderer renderer = property.createPanelRenderer(form);
//        renderer.setChangedHandler(new PropertyChangedHandler() {
//            @Override
//            public void onChanged(GPropertyDraw property, Object value) {
//                if (property.checkEquals && values.get(property).equals(value)) {
//                    return;
//                }
//
//                //todo:
////                form.changePropertyDraw(property, value);
//                form.changeProperty(property, null);
//            }
//        });
//        return renderer;
//    }
//
    public boolean isEmpty() {
        return orderedProperties.size() == 0;
    }

    public void hide() {
        for (Map.Entry<GPropertyDraw, Button> e : properties.entrySet()) {
            e.getValue().setVisible(false);
        }
    }

    public void show() {
        for (Map.Entry<GPropertyDraw, Button> e : properties.entrySet()) {
            e.getValue().setVisible(true);
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
}
