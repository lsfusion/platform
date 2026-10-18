package lsfusion.gwt.client.form.object.panel.controller;

import lsfusion.gwt.client.GFormChanges;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GGroupController;
import lsfusion.gwt.client.form.property.GGroupObjectPropertyReader;
import lsfusion.gwt.client.form.property.PValue;

import java.util.ArrayList;

// the form's controller of a group in PANEL view - or TOOLBAR, or POPUP -, the platform's: the group shows its current
// object alone, whose properties the form's panel draws (GPlatformFormPanelController), so there are no rows here - no
// keys to show, no current row to move, no nodes to open, none of what a row's readers bring (its background,
// foreground, selection, options) to draw -, and the current key, as the server holds it, is the whole key the form
// asks for (GGroupObjectValue.EMPTY), as it was while a grid with no table stood for such a group
public final class GPlatformPanelGroupController implements GGroupController {
    public GGroupObjectValue getSelectedKey() {
        return GGroupObjectValue.EMPTY;
    }
    public int getSelectedRow() {
        return -1;
    }
    public void modifyGroupObject(GGroupObjectValue key, boolean add, int position) {
        assert false : "a group in PANEL view has no rows to add a row to or remove one from";
    }
    public void updateKeys(GGroupObject group, ArrayList<GGroupObjectValue> keys, GFormChanges fc, int requestIndex) {
    }
    public void updateCurrentKey(GGroupObjectValue currentKey) {
    }
    public void updateAttribute(GGroupObjectPropertyReader reader, NativeHashMap<GGroupObjectValue, PValue> values, boolean partial) {
    }
}
