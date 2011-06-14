package platform.gwt.form.client.ui;

import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.TileLayoutPolicy;
import com.smartgwt.client.widgets.tile.TileLayout;
import platform.gwt.form.client.FormFrame;
import platform.gwt.form.client.utills.GwtUtils;
import platform.gwt.view.GForm;
import platform.gwt.view.GPropertyDraw;
import platform.gwt.view.changes.GGroupObjectValue;
import platform.gwt.view.renderer.GPropertyRenderer;

import java.util.ArrayList;
import java.util.HashMap;

public class GGroupPanel extends TileLayout {
    private final FormFrame frame;
    private final GForm form;
    private final GGroupObjectController groupController;

    public ArrayList<GPropertyDraw> properties = new ArrayList<GPropertyDraw>();
    public HashMap<GPropertyDraw, Object> values = new HashMap<GPropertyDraw, Object>();

    public GGroupPanel(FormFrame iframe, GForm iform, GGroupObjectController igroupController) {
        this.frame = iframe;
        form = iform;
        this.groupController = igroupController;

        setHeight(1);
        setLayoutPolicy(TileLayoutPolicy.FLOW);
        setOverflow(Overflow.VISIBLE);
    }

    public void addProperty(GPropertyDraw property) {
        if (!properties.contains(property)) {
            updated = true;
            int ins = GwtUtils.relativePosition(property, form.propertyDraws, properties);
            properties.add(ins, property);
        }
    }

    private boolean updated = true;
    public void removeProperty(GPropertyDraw property) {
        updated = true;
        properties.remove(property);
        values.remove(property);
    }

    public void setValue(GPropertyDraw property, HashMap<GGroupObjectValue, Object> valueMap) {
        if (valueMap != null && !valueMap.isEmpty()) {
            updated = true;
            values.put(property, valueMap.values().iterator().next());
        }
    }

    private int tileCount = 0;
    public void update() {
        if (!updated) {
            return;
        }

        while (tileCount != 0) {
            removeTile(0);
            --tileCount;
        }

        for (GPropertyDraw property : properties) {
            Object propValue = values.get(property);
            if (propValue != null) {
                GPropertyRenderer renderer = property.createPanelRenderer();
                renderer.setValue(property, propValue);

                addTile(renderer.getComponent());

                tileCount++;
            }
        }

        setVisible(!isEmpty());

        updated = true;
    }

    public boolean isEmpty() {
        return tileCount == 0;
    }
}
