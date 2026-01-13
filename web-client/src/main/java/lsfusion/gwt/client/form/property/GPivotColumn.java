package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.base.*;
import lsfusion.gwt.client.form.object.GGroupObject;

import java.io.Serializable;
import java.util.*;

import static lsfusion.gwt.client.form.object.table.grid.view.GPivot.COLUMN;

public class GPivotColumn implements GPropertyDrawOrPivotColumn, Serializable {
    public String groupObject;

    @SuppressWarnings("unused")
    public GPivotColumn() {
    }

    public GPivotColumn(String groupObject) {
        this.groupObject = groupObject;
    }

    @Override
    public boolean equalsGroupObject(GGroupObject group) {
        return GwtSharedUtils.nullEquals(groupObject, group.sID);
    }

    @Override
    public String getCaption(Map<GPropertyDraw, String> columnCaptionMap) {
        return COLUMN;
    }
}
