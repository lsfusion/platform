package platform.gwt.form.client.ui;

import platform.gwt.utils.GwtSharedUtils;
import platform.gwt.view.GForm;
import platform.gwt.view.GPropertyDraw;
import platform.gwt.view.changes.GGroupObjectValue;
import platform.gwt.view.renderer.GTypeRenderer;
import platform.gwt.view.renderer.PropertyChangedHandler;

import java.util.ArrayList;
import java.util.HashMap;

public class GPanelController {
    private final GFormController formController;
    private final GForm form;
    private GFormLayout formLayout;

    private ArrayList<GTypeRenderer> typeRenderers = new ArrayList<GTypeRenderer>();
    private ArrayList<GPropertyDraw> properties = new ArrayList<GPropertyDraw>();
    private HashMap<GPropertyDraw, Object> values = new HashMap<GPropertyDraw, Object>();

    public GPanelController(GFormController iformController, GForm iform, GFormLayout formLayout) {
        this.formController = iformController;
        form = iform;
        this.formLayout = formLayout;
    }

    public void addProperty(GPropertyDraw property) {
        if (!properties.contains(property)) {
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

            int ins = GwtSharedUtils.relativePosition(property, form.propertyDraws, properties);
            properties.add(ins, property);
            typeRenderers.add(ins, renderer);
            formLayout.add(property, typeRenderers.get(ins).getComponent());
        }
    }

    public void removeProperty(GPropertyDraw property) {
        int ind = properties.indexOf(property);
        if (ind != -1) {
            formLayout.remove(property);
            typeRenderers.remove(ind);

            properties.remove(ind);
            values.remove(property);
        }
    }

    private boolean dataChanged = true;

    public void setValue(GPropertyDraw property, HashMap<GGroupObjectValue, Object> valueMap) {
        if (valueMap != null && !valueMap.isEmpty()) {
            dataChanged = true;
            values.put(property, valueMap.values().iterator().next());
        }
    }

    public void update() {
        if (!dataChanged) {
            return;
        }

        for (int i = 0; i < properties.size(); i++) {
            GPropertyDraw property = properties.get(i);
            formLayout.remove(property);

            Object propValue = values.get(property);
            typeRenderers.get(i).setValue(propValue);
            formLayout.add(property, typeRenderers.get(i).getComponent());
        }

        dataChanged = false;
    }

    public boolean isEmpty() {
        return properties.size() == 0;
    }
    
    public void hide() {
        for (GTypeRenderer propRenderer : typeRenderers) {
            propRenderer.getComponent().setVisible(false);
        }
    }
    
    public void show() {
        for (GTypeRenderer propRenderer : typeRenderers) {
            propRenderer.getComponent().setVisible(true);
        }
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
