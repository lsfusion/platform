package platform.gwt.view;

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.grid.ListGridField;
import platform.gwt.view.classes.GType;
import platform.gwt.view.logics.FormLogicsProvider;
import platform.gwt.view.renderer.GTypeRenderer;

public class GPropertyDraw extends GComponent {
    public int ID;
    public GGroupObject groupObject;
    public String sID;
    public String caption;
    public GType baseType;
    public GType changeType;
    public String iconPath;
    public Boolean focusable;
    public boolean checkEquals;
    public GPropertyEditType editType = GPropertyEditType.EDITABLE;

    public ListGridField createGridField(FormLogicsProvider formLogics) {
        ListGridField gridField = baseType.createGridField(formLogics, this);
        gridField.setCanEdit(!(editType == GPropertyEditType.EDITABLE));
        if (baseType != changeType) {
            gridField.setEditorType(changeType.createEditorType(formLogics, this));
        }
        gridField.setShowHover(true);

        return gridField;
    }

    public Canvas createGridCellRenderer(FormLogicsProvider formLogics, GGroupObject group, GridDataRecord record) {
        return baseType.createGridCellRenderer(formLogics, group, record, this);
    }

    public Canvas updateGridCellRenderer(Canvas component, GridDataRecord record) {
        return baseType.updateGridCellRenderer(component, record);
    }

    private transient GTypeRenderer panelRenderer = null;
    public GTypeRenderer createPanelRenderer(FormLogicsProvider formLogics) {
//        if (panelRenderer == null) {
//            panelRenderer = baseType.createPanelRenderer(formLogics, this);
//        }
//        return panelRenderer;

        return baseType.createPanelRenderer(formLogics, this);
    }
}
