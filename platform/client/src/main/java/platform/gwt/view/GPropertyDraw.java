package platform.gwt.view;

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.grid.ListGridField;
import platform.gwt.view.changes.GGroupObjectValue;
import platform.gwt.view.classes.GType;
import platform.gwt.view.logics.FormLogicsProvider;
import platform.gwt.view.logics.GGroupObjectLogicsSupplier;
import platform.gwt.view.reader.*;
import platform.gwt.view.renderer.GTypeRenderer;

import java.util.Map;

public class GPropertyDraw extends GComponent implements GPropertyReader {
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

    public GCaptionReader captionReader;
    public GFooterReader footerReader;
    public GBackgroundReader backgroundReader;
    public GForegroundReader foregroundReader;

    public GPropertyDraw(){}

    public ListGridField createGridField(FormLogicsProvider formLogics) {
        ListGridField gridField = baseType.createGridField(formLogics, this);
        gridField.setCanEdit(!(editType == GPropertyEditType.EDITABLE));
        if (changeType != null && baseType != changeType) {
//            gridField.setEditorType(changeType.createGridEditorItem(formLogics, this));
        }

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

    public void update(GGroupObjectLogicsSupplier controller, Map<GGroupObjectValue, Object> values) {
        controller.updatePropertyDrawValues(this, values);
    }

    @Override
    public int getGroupObjectID() {
        return groupObject != null ? groupObject.ID : -1;
    }

}
