package platform.gwt.view2;

import com.google.gwt.user.cellview.client.Column;
import platform.gwt.view2.changes.GGroupObjectValue;
import platform.gwt.view2.classes.GType;
import platform.gwt.view2.logics.FormLogicsProvider;
import platform.gwt.view2.logics.GGroupObjectLogicsSupplier;
import platform.gwt.view2.reader.*;
import platform.gwt.view2.panel.PanelRenderer;
import platform.gwt.view2.grid.EditManager;

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

    public void update(GGroupObjectLogicsSupplier controller, Map<GGroupObjectValue, Object> values) {
        controller.updatePropertyDrawValues(this, values);
    }

    public Column<GridDataRecord, Object> createGridColumn(EditManager editManager, FormLogicsProvider form) {
        return baseType.createGridColumn(editManager, form, this);
    }

    public PanelRenderer createPanelRenderer(FormLogicsProvider form) {
        return baseType.createPanelRenderer(form, this);
    }

    @Override
    public int getGroupObjectID() {
        return groupObject != null ? groupObject.ID : -1;
    }

    public String getCaptionOrEmpty() {
        return caption == null ? "" : caption;
    }

    @Override
    public String toString() {
        return "GPropertyDraw{" +
                ", sID='" + sID + '\'' +
                ", caption='" + caption + '\'' +
                ", baseType=" + baseType +
                ", changeType=" + changeType +
                ", iconPath='" + iconPath + '\'' +
                ", focusable=" + focusable +
                ", checkEquals=" + checkEquals +
                ", editType=" + editType +
                '}';
    }
}
