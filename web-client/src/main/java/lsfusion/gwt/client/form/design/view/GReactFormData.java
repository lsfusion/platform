package lsfusion.gwt.client.form.design.view;

import com.google.gwt.core.client.JavaScriptObject;

import java.util.ArrayList;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.GForm;
import lsfusion.gwt.client.GFormChanges;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.jsni.NativeSIDMap;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.GPropertyReader;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.view.RendererType;
import lsfusion.gwt.client.form.object.table.grid.view.GSimpleStateTableView;

// Maintains the @lsfusion/core-shaped `data` snapshot for CUSTOM REACT containers,
// accumulated incrementally from each GFormChanges delta, built into a JS object on demand.
// data = { <groupSID>: { list:[{key, isCurrent, <propSID>:val}], byKey, ...panelProps }, <formPropSID>: val }
public class GReactFormData {

    private final GForm form;
    private final GFormController formController;

    // accumulator (mirrors the deltas the normal views consume)
    private final NativeSIDMap<GGroupObject, JavaScriptObject> lastByKey = new NativeSIDMap<>(); // group -> { public key -> row }, cached with the list
    private final NativeSIDMap<GGroupObject, GGroupObjectValue> currentObjects = new NativeSIDMap<>();
    private final NativeSIDMap<GGroupObject, ArrayList<GGroupObjectValue>> gridRows = new NativeSIDMap<>(); // ordered row keys per group
    private final NativeSIDMap<GPropertyReader, NativeHashMap<GGroupObjectValue, PValue>> values = new NativeSIDMap<>();

    // ===== structural sharing: build() returns the SAME JS refs for unchanged subtrees, so React.memo'd components skip.
    // The cache holds the last-built objects; the dirty sets (per update, cleared after all scopes are built) decide what to rebuild.
    private final NativeSIDMap<GContainer, JavaScriptObject> lastData = new NativeSIDMap<>(); // last built top object per React scope
    private final NativeSIDMap<GGroupObject, JavaScriptObject> lastNodes = new NativeSIDMap<>();   // last built node per group
    private final NativeSIDMap<GGroupObject, JavaScriptObject> lastLists = new NativeSIDMap<>();   // last built list array per group
    private final NativeSIDMap<GGroupObject, JavaScriptObject> lastKeys = new NativeSIDMap<>();   // last built STABLE keys array per group (ref changes only on membership/order, never on a value/current change) - the <List> subscription path
    private final NativeSIDMap<GGroupObject, NativeHashMap<GGroupObjectValue, JavaScriptObject>> lastRows = new NativeSIDMap<>(); // last row obj per (group, key)
    private final NativeSIDMap<GGroupObject, Boolean> dirtyNodes = new NativeSIDMap<>();      // group node must rebuild (current/list/prop changed)
    private final NativeSIDMap<GGroupObject, Boolean> dirtyLists = new NativeSIDMap<>();      // group list (rows/order) changed
    private final NativeSIDMap<GGroupObject, Boolean> dirtyOrder = new NativeSIDMap<>();      // group membership/order changed (rebuild the stable keys array) - set ONLY by add/remove/reorder, NOT by value/current changes
    private final NativeSIDMap<GGroupObject, NativeHashMap<GGroupObjectValue, Boolean>> dirtyRowKeys = new NativeSIDMap<>(); // rows whose values changed
    private final NativeSIDMap<GContainer, Boolean> dirtyScopes = new NativeSIDMap<>();       // scopes whose top object must rebuild

    public GReactFormData(GForm form, GFormController formController) {
        this.form = form;
        this.formController = formController;
    }

    public void update(GFormChanges fc) {
        fc.objects.foreachEntry(this::setCurrentObject);

        fc.gridObjects.foreachEntry((group, rows) -> {
            ArrayList<GGroupObjectValue> list = new ArrayList<>();
            list.addAll(rows);
            gridRows.put(group, list);
            markNodeDirty(group);
            dirtyLists.put(group, Boolean.TRUE); // row set / order changed
            dirtyOrder.put(group, Boolean.TRUE); // membership/order -> the stable keys array must rebuild
        });

        fc.properties.foreachEntry((reader, keyValues) -> {
            NativeHashMap<GGroupObjectValue, PValue> fStore = getOrCreateValues(reader);
            if (reader instanceof GPropertyDraw && ((GPropertyDraw) reader).integrationSID != null) {
                GPropertyDraw draw = (GPropertyDraw) reader;
                NativeHashMap<GGroupObjectValue, PValue> changedKeys = new NativeHashMap<>();
                boolean[] changed = {false};
                keyValues.foreachEntry((key, value) -> {
                    GGroupObjectValue valueKey = getValueKey(draw, key);
                    if (!GwtClientUtils.nullEquals(fStore.get(valueKey), value)) {
                        changedKeys.put(key, value);
                        changed[0] = true;
                    }
                    fStore.put(valueKey, value);
                });
                if (changed[0])
                    markPropertyDirty(draw, changedKeys);
            } else {
                keyValues.foreachEntry(fStore::put);
            }
        });

        for (GPropertyDraw drop : fc.dropProperties) {
            values.remove(drop);
            if (drop.integrationSID != null) {
                if (drop.groupObject == null) markScopeDirty(getPropertyOwningReactContainer(drop));
                else {
                    markNodeDirty(drop.groupObject);
                    if (drop.isList) {
                        dirtyLists.put(drop.groupObject, Boolean.TRUE);
                        lastRows.put(drop.groupObject, null); // reused rows would keep the dropped property's stale value: force a full row rebuild
                    }
                }
            }
        }
    }
    // set a group's current object (from a server fc.objects delta OR an optimistic changeGroupObject); idempotent —
    // returns true if it actually changed. The old + new current rows flip their isCurrent flag, so both rebuild.
    public boolean setCurrentObject(GGroupObject group, GGroupObjectValue key) {
        if (getGroupOwningReactContainer(group) == null)
            return false;
        GGroupObjectValue old = currentObjects.get(group);
        if (GwtClientUtils.nullEquals(old, key))
            return false;
        currentObjects.put(group, key);
        markNodeDirty(group); // panel props for the new current
        dirtyLists.put(group, Boolean.TRUE); // old + new current rows flip isCurrent
        markRowDirty(group, old);
        markRowDirty(group, key);
        return true;
    }
    // apply one optimistic property value (from setLoadingValueAt) into the SAME `values` accumulator update(fc) writes,
    // keyed the same way (the property cell key), and mark it dirty like markPropertyDirty — so the react container shows
    // the edit immediately, reconciled later when the server fc.properties arrives. Returns false if the draw isn't projected.
    public boolean setPropertyValue(GPropertyDraw draw, GGroupObjectValue fullKey, PValue value) {
        if (draw.integrationSID == null || getPropertyOwningReactContainer(draw) == null)
            return false;
        NativeHashMap<GGroupObjectValue, PValue> store = getOrCreateValues(draw);
        GGroupObjectValue valueKey = getValueKey(draw, fullKey);
        boolean changed = !GwtClientUtils.nullEquals(store.get(valueKey), value);
        store.put(valueKey, value);
        if (!changed)
            return false;
        markPropertyDirty(draw, fullKey);
        return true;
    }

    private NativeHashMap<GGroupObjectValue, PValue> getOrCreateValues(GPropertyReader reader) {
        NativeHashMap<GGroupObjectValue, PValue> store = values.get(reader);
        if (store == null) {
            store = new NativeHashMap<>();
            values.put(reader, store);
        }
        return store;
    }

    private void markPropertyDirty(GPropertyDraw draw, GGroupObjectValue key) {
        GGroupObject group = draw.groupObject;
        if (group == null) { // form-level -> top-level scalar (fullKey == EMPTY, the key fillProperties reads)
            markScopeDirty(getPropertyOwningReactContainer(draw));
            return;
        }
        markNodeDirty(group);
        if (!draw.isList) // a panel property -> the node rebuilds; its list/rows are reused
            return;
        dirtyLists.put(group, Boolean.TRUE); // a list cell -> the list + the changed row rebuild
        markRowDirty(group, getValueKey(draw, key));
    }

    // apply one optimistic ADD/REMOVE to the same row accumulator update(fc) replaces from fc.gridObjects later
    public boolean modifyGroupObject(GGroupObject group, GGroupObjectValue key, boolean add, int position) {
        if (getGroupOwningReactContainer(group) == null)
            return false;
        ArrayList<GGroupObjectValue> rows = gridRows.get(group);
        if (add) {
            if (rows == null) {
                rows = new ArrayList<>();
                gridRows.put(group, rows);
            }
            boolean changed = false;
            if (!rows.contains(key)) {
                if (position >= 0 && position <= rows.size())
                    rows.add(position, key);
                else
                    rows.add(key);
                markNodeDirty(group);
                dirtyLists.put(group, Boolean.TRUE);
                dirtyOrder.put(group, Boolean.TRUE); // optimistic add -> membership changed
                markRowDirty(group, key);
                changed = true;
            }
            return setCurrentObject(group, key) || changed;
        }

        if (rows == null)
            return false;
        int index = rows.indexOf(key);
        if (index < 0)
            return false;
        rows.remove(index);

        GGroupObjectValue current = currentObjects.get(group);
        if (GwtClientUtils.nullEquals(current, key))
            setCurrentObject(group, getNearObject(rows, index));
        markNodeDirty(group);
        dirtyLists.put(group, Boolean.TRUE);
        dirtyOrder.put(group, Boolean.TRUE); // optimistic remove -> membership changed
        markRowDirty(group, key);
        return true;
    }

    private GGroupObjectValue getNearObject(ArrayList<GGroupObjectValue> rows, int removedIndex) {
        if (rows.isEmpty())
            return null;
        return rows.get(removedIndex == rows.size() ? removedIndex - 1 : removedIndex);
    }

    // read accessors over the accumulator, for the controller-less (whole-form React) optimistic paths in GFormController
    public GGroupObjectValue getCurrentObject(GGroupObject group) { return currentObjects.get(group); }
    public PValue getValue(GPropertyDraw draw, GGroupObjectValue key) {
        NativeHashMap<GGroupObjectValue, PValue> store = values.get(draw);
        return store == null || key == null ? null : store.get(key);
    }
    public int getRowIndex(GGroupObject group, GGroupObjectValue key) {
        ArrayList<GGroupObjectValue> rows = gridRows.get(group);
        return rows == null || key == null ? -1 : rows.indexOf(key);
    }

    private void markNodeDirty(GGroupObject group) {
        GContainer scope = getGroupOwningReactContainer(group);
        if (scope == null)
            return;
        dirtyNodes.put(group, Boolean.TRUE);
        markScopeDirty(scope);
    }
    private void markRowDirty(GGroupObject group, GGroupObjectValue key) { // a row whose `value`/isCurrent/props changed must rebuild
        if (key == null) return;
        NativeHashMap<GGroupObjectValue, Boolean> dr = dirtyRowKeys.get(group);
        if (dr == null) { dr = new NativeHashMap<>(); dirtyRowKeys.put(group, dr); }
        dr.put(key, Boolean.TRUE);
    }
    private void markPropertyDirty(GPropertyDraw draw, NativeHashMap<GGroupObjectValue, PValue> changedKeys) {
        GGroupObject group = draw.groupObject;
        if (group == null) { markScopeDirty(getPropertyOwningReactContainer(draw)); return; } // form-level property -> top-level scalar (re-set on the new top every build)
        markNodeDirty(group);
        if (!draw.isList) // a panel property -> the node rebuilds (panel props), but its list/rows are reused
            return;
        dirtyLists.put(group, Boolean.TRUE); // a list cell changed -> the list (and the changed rows) rebuild
        changedKeys.foreachEntry((k, v) -> markRowDirty(group, getValueKey(draw, k)));
    }

    private GGroupObjectValue getValueKey(GPropertyDraw draw, GGroupObjectValue key) {
        if (draw.groupObject == null || !draw.isList || draw.hasColumnGroupObjects())
            return key;
        GGroupObjectValue rowKey = draw.groupObject.filterRowKeys(key);
        return rowKey != null ? rowKey : key;
    }

    public JavaScriptObject build(GContainer scope) {
        JavaScriptObject cached = lastData.get(scope);
        if (dirtyScopes.get(scope) == null && cached != null) // this scope did not change -> same top ref (the whole tree memo-skips)
            return cached;
        JavaScriptObject data = newObject();
        for (GGroupObject group : form.groupObjects) {
            if (getGroupOwningReactContainer(group) != scope)
                continue;
            JavaScriptObject node = lastNodes.get(group);
            if (node == null || dirtyNodes.get(group) != null) { // rebuild only a changed (or first-seen) group node
                node = buildNode(group);
                lastNodes.put(group, node);
            }
            setValue(data, group.getSID(), node);
        }
        fillProperties(data, null, false, GGroupObjectValue.EMPTY, scope); // form-level (no group) scalars, on the new top
        lastData.put(scope, data);
        return data;
    }

    // build a group's node, reusing the unchanged list array and unchanged row objects
    private JavaScriptObject buildNode(GGroupObject group) {
        JavaScriptObject node = newObject();
        GGroupObjectValue current = currentObjects.get(group);

        ArrayList<GGroupObjectValue> rows = gridRows.get(group);
        if (rows != null) {
            JavaScriptObject list = lastLists.get(group);
            if (list == null || dirtyLists.get(group) != null) { // rebuild the list only if its rows/order/values changed
                NativeHashMap<GGroupObjectValue, JavaScriptObject> prevRows = lastRows.get(group);
                NativeHashMap<GGroupObjectValue, Boolean> dirtyKeys = dirtyRowKeys.get(group);
                boolean reuseRows = canReuseRows(rows, prevRows, dirtyKeys); // false for composite/column keys we can't map to a row
                NativeHashMap<GGroupObjectValue, JavaScriptObject> newRows = new NativeHashMap<>();
                // canonical key string -> row, rebuilt WITH the list (row refs shared with it): selectors subscribe
                // by STABLE key (s.i.byKey[row.key] — property lookup coerces a numeric key to the same string) so
                // surviving rows after a delete keep their selected identity; cached like the list, so a node-only
                // change (panel prop) keeps byKey identity too
                JavaScriptObject byKey = newObject();
                list = newArray();
                for (GGroupObjectValue rowKey : rows) {
                    JavaScriptObject prev = reuseRows && prevRows != null ? prevRows.get(rowKey) : null;
                    JavaScriptObject row;
                    if (prev != null && (dirtyKeys == null || dirtyKeys.get(rowKey) == null)) {
                        row = prev; // reuse the unchanged row object (same ref -> the row component memo-skips)
                    } else {
                        row = newObject();
                        setBoolean(row, "isCurrent", current != null && rowKey.equals(current)); // declarative current-row marker
                        fillProperties(row, group, true, rowKey, null);
                    }
                    GGroupObjectValue.registerRow(row, rowKey); // the public row.key + the non-enumerable `objects` handle
                    setValue(byKey, rowKey.toKeyString(), row);
                    newRows.put(rowKey, row);
                    push(list, row);
                }
                lastRows.put(group, newRows);
                lastLists.put(group, list);
                lastByKey.put(group, byKey);
            }
            setValue(node, "list", list);
            setValue(node, "byKey", lastByKey.get(group));
            // a referentially-STABLE keys array (rebuilt only on membership/order) + a non-enumerable group SID:
            // the <List> row-subscription path maps these keys and each row subscribes by byKey[key], so a value/current
            // change re-renders only the changed row (the keys array ref is unchanged -> the outer map is skipped).
            JavaScriptObject keys = lastKeys.get(group);
            if (keys == null || dirtyOrder.get(group) != null) {
                keys = newArray();
                for (GGroupObjectValue rowKey : rows)
                    pushString(keys, rowKey.toKeyString());
                lastKeys.put(group, keys);
            }
            setValue(node, "keys", keys);
            setGroupSID(node, group.getSID());
        }
        if (current != null) // group panel properties (shown once, for the current object)
            fillProperties(node, group, false, current, null);
        return node;
    }

    // a row can be reused by key only if every changed key maps to a current row; a dirty key that isn't a row means a
    // composite/column key (column-group property) we can't map to one row, so rebuild the whole list to stay correct.
    private boolean canReuseRows(ArrayList<GGroupObjectValue> rows, NativeHashMap<GGroupObjectValue, JavaScriptObject> prevRows, NativeHashMap<GGroupObjectValue, Boolean> dirtyKeys) {
        if (dirtyKeys == null)
            return true;
        NativeHashMap<GGroupObjectValue, Boolean> rowSet = new NativeHashMap<>();
        for (GGroupObjectValue k : rows)
            rowSet.put(k, Boolean.TRUE);
        boolean[] ok = {true};
        // a dirty key absent from the new rows is fine if it WAS a row before (a deleted row needs no mapping —
        // survivors keep identity); only a key that was never a row (composite/column key) forces a full rebuild
        dirtyKeys.foreachEntry((k, v) -> { if (rowSet.get(k) == null && (prevRows == null || prevRows.get(k) == null)) ok[0] = false; });
        return ok[0];
    }

    public void clearDirty() {
        dirtyNodes.clear();
        dirtyLists.clear();
        dirtyOrder.clear();
        dirtyRowKeys.clear();
        dirtyScopes.clear();
    }

    // group/property SID resolution lives on GForm (shared with the other integration controllers); row identity
    // registration/resolution is centralized on GGroupObjectValue (registerRow/resolveObject)

    // for the given group (null = form-level) and list/panel flag, set each matching draw's value (by integrationSID) on target
    private void fillProperties(JavaScriptObject target, GGroupObject group, boolean list, GGroupObjectValue key, GContainer scope) {
        for (GPropertyDraw draw : form.propertyDraws) {
            if (draw.groupObject != group)
                continue;
            if (group != null && draw.isList != list)
                continue;
            if (group == null && getPropertyOwningReactContainer(draw) != scope)
                continue;
            if (draw.integrationSID == null)
                continue;
            NativeHashMap<GGroupObjectValue, PValue> store = values.get(draw);
            GGroupObjectValue valueKey = list ? key : draw.filterColumnKeys(key);
            PValue pvalue = store == null || valueKey == null ? null : store.get(valueKey);
            setValue(target, draw.integrationSID, GSimpleStateTableView.convertToJSValue(draw, pvalue, RendererType.SIMPLE, true));
        }
    }


    private GContainer getGroupOwningReactContainer(GGroupObject group) {
        if (group == null)
            return null;
        return formController.getOwningReactContainer(group.grid != null ? group.grid : group.parent);
    }

    private GContainer getPropertyOwningReactContainer(GPropertyDraw draw) {
        if (draw == null)
            return null;
        GGroupObject group = draw.groupObject;
        if (group != null)
            return getGroupOwningReactContainer(group);
        return formController.getOwningReactContainer(draw);
    }

    private void markScopeDirty(GContainer scope) {
        if (scope != null)
            dirtyScopes.put(scope, Boolean.TRUE);
    }


    private static native JavaScriptObject newObject() /*-{ return {}; }-*/;
    private static native JavaScriptObject newArray() /*-{ return []; }-*/;
    private static native void push(JavaScriptObject arr, JavaScriptObject v) /*-{ arr.push(v); }-*/;
    private static native void pushString(JavaScriptObject arr, String v) /*-{ arr.push(v); }-*/;
    private static native void setGroupSID(JavaScriptObject obj, String sid) /*-{ Object.defineProperty(obj, "__groupSID", { value: sid }); }-*/; // non-enumerable: stable selector path, not user-visible data
    private static native void setValue(JavaScriptObject obj, String key, Object v) /*-{ obj[key] = v; }-*/;
    private static native void setBoolean(JavaScriptObject obj, String key, boolean v) /*-{ obj[key] = v; }-*/;
}
