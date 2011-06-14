package platform.gwt.view;

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.grid.ListGridField;
import platform.gwt.view.classes.GType;
import platform.gwt.view.renderer.GPropertyRenderer;

public class GPropertyDraw extends GComponent {
    public int ID;
    public GGroupObject groupObject;
    public String sID;
    public String caption;
    public GType baseType;
    public String iconPath;

    public ListGridField createGridField() {
        return baseType.createGridField(this);
    }

    public Canvas createGridCellRenderer(Object value) {
        return baseType.createCellRenderer(value, this);
    }

    private transient GPropertyRenderer panelRenderer = null;
    //по умолчанию создаём форму
    public GPropertyRenderer createPanelRenderer() {
        if (panelRenderer == null) {
            panelRenderer = baseType.createPanelRenderer(this);
        }
        return panelRenderer;
    }
}
