package platform.gwt.view2;

import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import platform.gwt.view2.changes.GGroupObjectValue;
import platform.gwt.view2.classes.GType;
import platform.gwt.view2.logics.FormLogicsProvider;
import platform.gwt.view2.logics.GGroupObjectLogicsSupplier;
import platform.gwt.view2.reader.*;

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

    public Column<GridDataRecord, ?> createGridColumn(FormLogicsProvider form) {
        return new TextColumn<GridDataRecord>() {
            @Override
            public String getValue(GridDataRecord object) {
                Object value = object.getAttribute(sID);
                return value == null ? "<null>" : value.toString();
            }
        };
    }

    @Override
    public int getGroupObjectID() {
        return groupObject != null ? groupObject.ID : -1;
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
