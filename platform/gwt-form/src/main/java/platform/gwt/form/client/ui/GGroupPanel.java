package platform.gwt.form.client.ui;

import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.layout.FlowLayout;
import platform.gwt.utils.GwtUtils;
import platform.gwt.view.GForm;
import platform.gwt.view.GPropertyDraw;
import platform.gwt.view.changes.GGroupObjectValue;
import platform.gwt.view.renderer.GTypeRenderer;
import platform.gwt.view.renderer.PropertyChangedHandler;

import java.util.ArrayList;
import java.util.HashMap;

public class GGroupPanel extends FlowLayout {
    private final GFormController formController;
    private final GForm form;
    private final GGroupObjectController groupController;

    private ArrayList<GTypeRenderer> typeRenderers = new ArrayList<GTypeRenderer>();
    private ArrayList<GPropertyDraw> properties = new ArrayList<GPropertyDraw>();
    private HashMap<GPropertyDraw, Object> values = new HashMap<GPropertyDraw, Object>();

    public GGroupPanel(GFormController iformController, GForm iform, GGroupObjectController igroupController) {
        this.formController = iformController;
        form = iform;
        this.groupController = igroupController;

        setHeight(1);
        setOverflow(Overflow.VISIBLE);
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

            int ins = GwtUtils.relativePosition(property, form.propertyDraws, properties);
            properties.add(ins, property);
            typeRenderers.add(ins, renderer);

            addTile(renderer.getComponent(), ins);
        }
    }

    public void removeProperty(GPropertyDraw property) {
        int ind = properties.indexOf(property);
        if (ind != -1) {
            GTypeRenderer removedRenederer = typeRenderers.remove(ind);
            removeTile(removedRenederer.getComponent());

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

            Object propValue = values.get(property);
            typeRenderers.get(i).setValue(propValue);
        }

        setVisible(!isEmpty());

        dataChanged = false;
    }

    public boolean isEmpty() {
        return properties.size() == 0;
    }

    @Override
    protected void onInit() {
        super.onInit();
        onInit_GGroupPanel();
    }

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
