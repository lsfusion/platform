package platform.gwt.form.client.ui;

import platform.gwt.utils.GwtSharedUtils;
import platform.gwt.view.GForm;
import platform.gwt.view.GPropertyDraw;
import platform.gwt.view.changes.GGroupObjectValue;
import platform.gwt.view.renderer.GTypeRenderer;
import platform.gwt.view.renderer.PropertyChangedHandler;

import java.util.*;

public class GPanelController {
    private final GFormController formController;
    private final GForm form;
    private GFormLayout formLayout;

    private List<GPropertyDraw> orderedProperties = new ArrayList<GPropertyDraw>();
    private Map<GPropertyDraw, GTypeRenderer> properties = new HashMap<GPropertyDraw, GTypeRenderer>();
    private HashMap<GPropertyDraw, Object> values = new HashMap<GPropertyDraw, Object>();

    private Map<GPropertyDraw, Map<GGroupObjectValue, Object>> cellBackgroundValues = new HashMap<GPropertyDraw, Map<GGroupObjectValue, Object>>();
    private Map<GPropertyDraw, Map<GGroupObjectValue, Object>> cellForegroundValues = new HashMap<GPropertyDraw, Map<GGroupObjectValue, Object>>();
    private Object rowBackground;
    private Object rowForeground;
    private Map<GPropertyDraw, Map<GGroupObjectValue, Object>> propertyCaptions = new HashMap<GPropertyDraw, Map<GGroupObjectValue, Object>>();

    public GPanelController(GFormController iformController, GForm iform, GFormLayout formLayout) {
        this.formController = iformController;
        form = iform;
        this.formLayout = formLayout;
    }

    public void addProperty(GPropertyDraw property) {
        if (!containsProperty(property)) {
            int ins = GwtSharedUtils.relativePosition(property, form.propertyDraws, orderedProperties);
            orderedProperties.add(ins, property);
        }
    }

    public GTypeRenderer createPropertyRenderer(GPropertyDraw property) {
        GTypeRenderer renderer = property.createPanelRenderer(formController);
        renderer.setChangedHandler(new PropertyChangedHandler() {
            @Override
            public void onChanged(GPropertyDraw property, Object value) {
                if (property.checkEquals && values.get(property).equals(value)) {
                    return;
                }
                formController.changePropertyDraw(property, value);
            }
        });
        return renderer;
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

    private boolean dataChanged = true;

    public void setValue(GPropertyDraw property, Map<GGroupObjectValue, Object> valueMap) {
        if (valueMap != null && !valueMap.isEmpty()) {
            dataChanged = true;
            values.put(property, valueMap.values().iterator().next());
        }
    }

    public void update() {
        if (!dataChanged) {
            return;
        }

        List<GPropertyDraw> toDraw = new ArrayList<GPropertyDraw>();
        for (GPropertyDraw property : orderedProperties) {
            GTypeRenderer renderer = null;
            if (!(propertyCaptions.get(property) != null && propertyCaptions.get(property).values().iterator().next() == null)) {
                if (properties.get(property) == null) {
                    renderer = createPropertyRenderer(property);
                    properties.put(property, renderer);
                    formLayout.add(property, renderer.getComponent(), property.container.children.indexOf(property));
                }
                toDraw.add(property);
            }

            Object propValue = values.get(property);
            if (renderer != null)
                renderer.setValue(propValue);
        }

        for (GPropertyDraw property : orderedProperties) {
            if (!toDraw.contains(property)) {
                formLayout.remove(property);
            }
        }

        for (GPropertyDraw property : orderedProperties) {
            GTypeRenderer renderer = properties.get(property);

            if (renderer != null) {
                Map<GGroupObjectValue, Object> caption = propertyCaptions.get(property);
                if (caption != null) {
                    renderer.setTitle(caption.values().iterator().next());
                }

                Object background = rowBackground != null ? rowBackground : null;
                if (background == null && cellBackgroundValues.get(property) != null)  {
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
        dataChanged = false;
    }

    public boolean isEmpty() {
        return properties.size() == 0;
    }
    
    public void hide() {
        for (GPropertyDraw property : orderedProperties) {
            formLayout.remove(property);
        }
    }
    
    public void show() {
        for (GPropertyDraw property : orderedProperties) {
            formLayout.add(property, properties.get(property).getComponent(), orderedProperties.indexOf(property));
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

//    @Override
//    protected void onInit() {
//        super.onInit();
//        onInit_GGroupPanel();
//    }

    /**
     * Исправляет баг в TileLayout.js с добавлением в позицию 0
     */
    private native void onInit_GGroupPanel() /*-{
        var self = this.@com.smartgwt.client.widgets.BaseWidget::getOrCreateJsObj()();
        self.addTile = $debox($entry(function (tile, index) {
            if (!this.tiles) return;
            if (!index && index!=0) index = this.tiles.getLength();
            this.tiles.addAt(tile, index);
            this.reLayout();
        }));
    }-*/;
}
