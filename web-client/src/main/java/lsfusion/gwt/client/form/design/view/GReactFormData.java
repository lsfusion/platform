package lsfusion.gwt.client.form.design.view;

import com.google.gwt.core.client.JavaScriptObject;

import java.util.ArrayList;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.GForm;
import lsfusion.gwt.client.GFormChanges;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.jsni.NativeSIDMap;
import lsfusion.gwt.client.classes.data.GJSONType;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GComponentReader;
import lsfusion.gwt.client.form.property.GExtraPropertyReader;
import lsfusion.gwt.client.form.property.GMetaConverter;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.GPropertyReader;
import lsfusion.gwt.client.form.property.GShowIfReader;
import lsfusion.gwt.client.form.property.GGroupObjectPropertyReader;
import lsfusion.gwt.client.form.property.GMetaScope;
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
    private final NativeSIDMap<GPropertyDraw, Boolean> droppedProperties = new NativeSIDMap<>(); // SHOWIF/static visibility removed the whole draw

    // ===== structural sharing: build() returns the SAME JS refs for unchanged subtrees, so React.memo'd components skip.
    // The cache holds the last-built objects; the dirty sets (per update, cleared after all scopes are built) decide what to rebuild.
    private final NativeSIDMap<GContainer, JavaScriptObject> lastData = new NativeSIDMap<>(); // last built top object per React scope
    private final NativeSIDMap<GContainer, JavaScriptObject> lastComponents = new NativeSIDMap<>(); // last built data.components per scope (reused unless dirtyComponents) - same lastX+dirtyX pattern as lastNodes+dirtyNodes
    private final NativeSIDMap<GGroupObject, JavaScriptObject> lastNodes = new NativeSIDMap<>();   // last built node per group
    private final NativeSIDMap<GGroupObject, JavaScriptObject> lastLists = new NativeSIDMap<>();   // last built list array per group
    private final NativeSIDMap<GGroupObject, JavaScriptObject> lastKeys = new NativeSIDMap<>();   // last built STABLE keys array per group (ref changes only on membership/order, never on a value/current change) - the <List> subscription path
    private final NativeSIDMap<GGroupObject, NativeHashMap<GGroupObjectValue, JavaScriptObject>> lastRows = new NativeSIDMap<>(); // last row obj per (group, key)
    private final NativeSIDMap<GGroupObject, Boolean> dirtyNodes = new NativeSIDMap<>();      // group node must rebuild (current/list/prop changed)
    private final NativeSIDMap<GGroupObject, Boolean> dirtyLists = new NativeSIDMap<>();      // group list (rows/order) changed
    private final NativeSIDMap<GGroupObject, Boolean> dirtyOrder = new NativeSIDMap<>();      // group membership/order changed (rebuild the stable keys array) - set ONLY by add/remove/reorder, NOT by value/current changes
    private final NativeSIDMap<GGroupObject, NativeHashMap<GGroupObjectValue, Boolean>> dirtyRowKeys = new NativeSIDMap<>(); // rows whose values changed
    private final NativeSIDMap<GContainer, Boolean> dirtyScopes = new NativeSIDMap<>();       // scopes whose top object must rebuild
    private final NativeSIDMap<GContainer, Boolean> dirtyComponents = new NativeSIDMap<>();   // scopes whose data.components changed (a descriptor reader fired) - set ONLY by descriptor changes, so a value-only rebuild reuses the components ref off the cached top

    // ===== data.components: semantic descriptors (caption / image) of the DELEGATED children of each React scope,
    // keyed by child sid in DESIGN order. Delivered by the delegated caption/image READERS (they never touch GWT);
    // stored in `values` like any reader and read back in the top build (buildComponents), the static design value as the
    // fallback — so there is NO dedicated cache, data.components is assembled exactly like the panel scalars and shares
    // the top object's structural sharing (rebuilt only when its scope is dirty).

    public GReactFormData(GForm form, GFormController formController) {
        this.form = form;
        this.formController = formController;
    }

    public void update(GFormChanges fc) {
        fc.objects.foreachEntry(this::setCurrentObject);

        fc.gridObjects.foreachEntry((group, rows) -> {
            ArrayList<GGroupObjectValue> list = new ArrayList<>();
            list.addAll(rows);
            ArrayList<GGroupObjectValue> prev = gridRows.get(group);
            gridRows.put(group, list);
            if (prev != null && prev.equals(list)) // the server re-read the group and sent back the SAME rows in the same
                return;                            // order: nothing changed, so don't churn the node / list / stable keys
            markNodeDirty(group);
            dirtyLists.put(group, Boolean.TRUE); // row set / order changed
            dirtyOrder.put(group, Boolean.TRUE); // membership/order -> the stable keys array must rebuild
        });

        fc.properties.foreachEntry((reader, keyValues) -> {
            NativeHashMap<GGroupObjectValue, PValue> fStore = getOrCreateValues(reader);
            GComponent componentChild = getComponentChild(reader);
            if (componentChild != null) { // a delegated child's caption/class/image: stored like any reader, read back into data.components in the top build
                if (putChanged(fStore, keyValues, null) != null)
                    markComponentDirty(componentChild.container); // data.components rebuilds (and the top with it); other data reused
            } else if (reader instanceof GPropertyDraw && ((GPropertyDraw) reader).integrationSID != null) {
                GPropertyDraw draw = (GPropertyDraw) reader;
                droppedProperties.remove(draw); // a shown draw is sent again with its values
                NativeHashMap<GGroupObjectValue, PValue> changedKeys = putChanged(fStore, keyValues, draw);
                if (changedKeys != null)
                    markPropertyDirty(draw, changedKeys);
            } else {
                // a react-owned property/row PRESENTATION reader (readOnly/background/caption/image/... or row
                // background/foreground/select/customOptions): it lands here with NO value-draw dirty flag, so its owning
                // cell/row/node/scope would not rebuild and data.meta would go stale -> mark it dirty to re-project meta.
                NativeHashMap<GGroupObjectValue, PValue> changedKeys = putChanged(fStore, keyValues, null);
                if (changedKeys != null)
                    markPresentationDirty(reader, changedKeys);
            }
        });

        for (GPropertyDraw drop : fc.dropProperties) {
            droppedProperties.put(drop, Boolean.TRUE);
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

    // store the delta and return ONLY the keys whose value actually changed (null if none changed). The server re-delivers
    // values that did not change (a reader recomputed for the whole group, a refresh); marking those dirty would rebuild
    // rows / data.components that are byte-identical and break the structural-sharing contract (an untouched row MUST keep
    // its ref so React.memo skips it). draw != null -> the store is keyed by the draw's value key, like the value itself.
    private NativeHashMap<GGroupObjectValue, PValue> putChanged(NativeHashMap<GGroupObjectValue, PValue> store,
                                                                NativeHashMap<GGroupObjectValue, PValue> keyValues, GPropertyDraw draw) {
        NativeHashMap<GGroupObjectValue, PValue> changedKeys = new NativeHashMap<>();
        boolean[] changed = {false};
        keyValues.foreachEntry((key, value) -> {
            GGroupObjectValue storeKey = draw != null ? getValueKey(draw, key) : key;
            if (!GwtClientUtils.nullEquals(store.get(storeKey), value)) {
                changedKeys.put(key, value);
                changed[0] = true;
            }
            store.put(storeKey, value);
        });
        return changed[0] ? changedKeys : null;
    }

    // mark the draw's node dirty (form-level -> scope; panel -> node only). Returns true if it's a LIST draw whose changed
    // rows still need markRowDirty (the only difference between the two markPropertyDirty overloads below).
    private boolean markDrawNodeDirty(GPropertyDraw draw) {
        GGroupObject group = draw.groupObject;
        if (group == null) { // form-level -> top-level scalar (fullKey == EMPTY, the key fillProperties reads)
            markScopeDirty(getPropertyOwningReactContainer(draw));
            return false;
        }
        markNodeDirty(group);
        if (!draw.isList) // a panel property -> the node rebuilds; its list/rows are reused
            return false;
        dirtyLists.put(group, Boolean.TRUE); // a list cell -> the list (+ the changed rows) rebuild
        return true;
    }
    private void markPropertyDirty(GPropertyDraw draw, GGroupObjectValue key) {
        if (markDrawNodeDirty(draw))
            markRowDirty(draw.groupObject, getValueKey(draw, key));
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

    private void markComponentDirty(GContainer scope) { // a delegated child's descriptor changed -> data.components rebuilds; scope is the child's container, always non-null here (getComponentChild required it)
        dirtyComponents.put(scope, Boolean.TRUE);
        markScopeDirty(scope);
    }
    private void markRowDirty(GGroupObject group, GGroupObjectValue key) { // a row whose `value`/isCurrent/props changed must rebuild
        if (key == null) return;
        NativeHashMap<GGroupObjectValue, Boolean> dr = dirtyRowKeys.get(group);
        if (dr == null) { dr = new NativeHashMap<>(); dirtyRowKeys.put(group, dr); }
        dr.put(key, Boolean.TRUE);
    }
    private void markPropertyDirty(GPropertyDraw draw, NativeHashMap<GGroupObjectValue, PValue> changedKeys) {
        if (markDrawNodeDirty(draw))
            changedKeys.foreachEntry((k, v) -> markRowDirty(draw.groupObject, getValueKey(draw, k)));
    }

    // a react-owned presentation reader changed: mark its owning cell/row/node/scope dirty so the next build re-projects
    // data.meta (mirrors markPropertyDirty for the value draw). No-ops for non-react readers (their scopes resolve null).
    private void markPresentationDirty(GPropertyReader reader, NativeHashMap<GGroupObjectValue, PValue> keyValues) {
        if (reader instanceof GShowIfReader) {
            GPropertyDraw draw = form.getProperty(((GShowIfReader) reader).propertyID);
            if (draw != null && draw.integrationSID != null) {
                markDrawNodeDirty(draw);
                if (draw.isList)
                    lastRows.put(draw.groupObject, null); // SHOWIF changes the shape of every projected row in the column
            }
            return;
        }
        if (reader.getMetaField() == null) // this reader is NOT projected (native CSS/font, loading, last, changeKey/changeMouse): buildPropMeta skips it,
            return;                        // so its delta changes nothing in `data` — dirtying its rows would rebuild them for nothing
        if (reader instanceof GExtraPropertyReader) { // a property's semantic presentation reader (caption/editability/colors/image/comment/tooltip/...)
            GPropertyDraw draw = form.getProperty(((GExtraPropertyReader) reader).propertyID);
            if (draw != null && draw.integrationSID != null) {
                if (draw.isList && reader.isColumnLevel(draw)) // a column-level reader lives in node.meta[prop] -> rebuild the node only, DON'T churn the list/row refs
                    markNodeDirty(draw.groupObject);
                else // a cell-level (or panel) reader -> same node/list/rows/scope marking as the value draw
                    markPropertyDirty(draw, keyValues);
            }
        } else if (reader instanceof GGroupObjectPropertyReader) { // a group-object reader: rowBackground/rowForeground/rowSelect (per row) or customOptions (group-scoped)
            GGroupObject group = form.getGroupObject(((GGroupObjectPropertyReader) reader).groupObjectID);
            if (group == null)
                return;
            markNodeDirty(group); // the group node (its meta.customOptions/count) rebuilds
            if (((GGroupObjectPropertyReader) reader).getMetaScope() == GMetaScope.ROW) { // a per-row reader -> the list + each changed row rebuild (row.meta.row); a group-scoped one (customOptions) -> node only
                dirtyLists.put(group, Boolean.TRUE);
                keyValues.foreachEntry((k, v) -> markRowDirty(group, k));
            }
        }
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
        JavaScriptObject components = lastComponents.get(scope);
        if (components == null || dirtyComponents.get(scope) != null) { // rebuild data.components only when a descriptor reader fired (else reuse the ref) - exactly like a group node above
            components = buildComponents(scope);
            lastComponents.put(scope, components);
        }
        setValue(data, "components", components);
        lastData.put(scope, data);
        return data;
    }

    // ===== data.components ====================================================================================

    // data.components is a first-class cached slot exactly like a group node: lastComponents + dirtyComponents (set only in
    // update's descriptor branch). build() reuses the cached components ref unless a descriptor actually changed, so an
    // unrelated value change in the scope does not churn its ref. The descriptor VALUES themselves live in `values`.

    // the DELEGATED, React-scoped child whose caption / image this reader carries, or null
    private GComponent getComponentChild(GPropertyReader reader) {
        GComponent child = getPresentationComponent(reader);
        if (child == null || !child.isDelegated())
            return null;
        GContainer scope = child.container; // the React container that places this child (its LsfComponents lists it)
        return scope != null && scope.isReact() ? child : null;
    }

    // the component a presentation reader belongs to (only caption / image readers qualify), or null.
    // reader-TYPE dispatch resolves the component (a container's reader carries the component; a property's reader carries
    // its ID); then the component's own presentation aspects decide whether the reader is one of them.
    private GComponent getPresentationComponent(GPropertyReader reader) {
        GComponent comp = null;
        if (reader instanceof GComponentReader) // a container's caption / image reader
            comp = ((GComponentReader) reader).getReaderComponent();
        else if (reader instanceof GExtraPropertyReader) // a property's caption / captionElementClass reader
            comp = form.getProperty(((GExtraPropertyReader) reader).propertyID);
        if (comp != null)
            for (GPropertyReader r : comp.getComponentReaders())
                if (reader == r)
                    return comp;
        return null;
    }

    // data.components, DESIGN order, assembled in the top build (see fillProperties caller) from the SAME `values` store
    // as the panel scalars — rebuilt with the top on any scope change, no dedicated cache
    private JavaScriptObject buildComponents(GContainer scope) {
        JavaScriptObject components = newObject();
        for (GComponent child : scope.children) // DESIGN order
            if (child.isDelegated())
                setValue(components, child.sID, buildComponent(child));
        return components;
    }

    // each aspect is ALWAYS projected (dynamic value else the static design value) — no dynamic-only omission and no separate
    // static channel. This is cheap because data.components rides structural sharing (see build/dirtyComponents): the static
    // parts are re-derived only when the object is actually rebuilt (a presentation reader fired), not on unrelated value
    // changes. A reader that delivered null falls back to the static design value. Same loop shape as buildRowMeta/buildNode:
    // each component reader declares its own field, converter and static fallback.
    private JavaScriptObject buildComponent(GComponent child) {
        JavaScriptObject[] holder = {newObject()}; // eager: keep a (possibly empty) descriptor object per delegated child
        for (GPropertyReader reader : child.getComponentReaders())
            if (reader != null)
                emitPresentation(holder, reader, GGroupObjectValue.EMPTY, null, reader.getColumnStatic(child));
        return holder[0];
    }

    public boolean isComponentReader(GPropertyReader reader) {
        return getComponentChild(reader) != null;
    }

    // build a group's node, reusing the unchanged list array and unchanged row objects
    private JavaScriptObject buildNode(GGroupObject group) {
        JavaScriptObject node = newObject();
        GGroupObjectValue current = currentObjects.get(group);

        ArrayList<GGroupObjectValue> rows = gridRows.get(group);
        JavaScriptObject list = lastLists.get(group);
        JavaScriptObject byKey = lastByKey.get(group);
        if (rows != null) {
            if (list == null || dirtyLists.get(group) != null) { // rebuild the list only if its rows/order/values changed
                NativeHashMap<GGroupObjectValue, JavaScriptObject> prevRows = lastRows.get(group);
                NativeHashMap<GGroupObjectValue, Boolean> dirtyKeys = dirtyRowKeys.get(group);
                boolean reuseRows = canReuseRows(group, dirtyKeys); // false for composite/column keys we can't map to a row
                NativeHashMap<GGroupObjectValue, JavaScriptObject> newRows = new NativeHashMap<>();
                // canonical key string -> row, rebuilt WITH the list (row refs shared with it): selectors subscribe
                // by STABLE key (s.i.byKey[row.key] — property lookup coerces a numeric key to the same string) so
                // surviving rows after a delete keep their selected identity; cached like the list, so a node-only
                // change (panel prop) keeps byKey identity too
                byKey = newObject();
                list = newArray();
                for (GGroupObjectValue rowKey : rows) {
                    JavaScriptObject prev = reuseRows && prevRows != null ? prevRows.get(rowKey) : null;
                    JavaScriptObject row;
                    if (prev != null && (dirtyKeys == null || dirtyKeys.get(rowKey) == null)) {
                        row = prev; // reuse the unchanged row object (same ref -> the row component memo-skips)
                    } else {
                        row = newObject();
                        setBoolean(row, "isCurrent", current != null && rowKey.equals(current)); // declarative current-row marker
                        fillProperties(row, group, true, rowKey, null); // per-cell values + row.meta[propSID]
                        JavaScriptObject rowMeta = buildRowMeta(group, rowKey); // row.meta.row = { background, foreground, selected }
                        if (rowMeta != null)
                            setValue(getMeta(row), "row", rowMeta);
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
        } else if (list == null) { // never had rows: materialize stable empty defaults once (list/byKey are always cached together)
            list = newArray();
            byKey = newObject();
            lastLists.put(group, list);
            lastByKey.put(group, byKey);
        }
        setValue(node, "list", list);
        setValue(node, "byKey", byKey);
        // a referentially-STABLE keys array (rebuilt only on membership/order) + a non-enumerable group SID:
        // the <List> row-subscription path maps these keys and each row subscribes by byKey[key], so a value/current
        // change re-renders only the changed row (the keys array ref is unchanged -> the outer map is skipped).
        JavaScriptObject keys = lastKeys.get(group);
        if (keys == null || dirtyOrder.get(group) != null) {
            keys = newArray();
            if (rows != null) {
                for (GGroupObjectValue rowKey : rows)
                    pushString(keys, rowKey.toKeyString());
            }
            lastKeys.put(group, keys);
        }
        setValue(node, "keys", keys);
        setGroupSID(node, group.getSID());
        if (current != null) // group panel properties (shown once, for the current object) + their node.meta[propSID]
            fillProperties(node, group, false, current, null);
        // group-level meta: loaded row count + the group's GROUP-LEVEL readers (customOptions, EMPTY key). Shares node.meta
        // with the panel props above (getMeta reuses the object); count rebuilds with the node (list/reader change marks it).
        JavaScriptObject groupMeta = getMeta(node);
        setInt(groupMeta, "count", rows != null ? rows.size() : 0);
        JavaScriptObject[] gm = {groupMeta}; // eager holder -> emitPresentation writes into the existing groupMeta
        for (GGroupObjectPropertyReader reader : group.getPresentationReaders()) // the NODE-scoped readers (customOptions) -> node.meta, once at EMPTY
            if (reader != null && reader.getMetaScope() == GMetaScope.NODE)
                emitPresentation(gm, reader, GGroupObjectValue.EMPTY, null, null);
        // COLUMN-level presentation for LIST properties -> node.meta[propSID], ONCE per column (not per row): buildPropMeta
        // with columnStatic emits the column-level readers (caption/footer/property image, read at the column key)
        // + semantic static design values; cell-level readers miss at EMPTY, so they stay in row.meta[propSID].
        // A consumer merges node base <- row override. Single (EMPTY) column key; grouped-in-columns is a follow-up (skip,
        // don't mis-key). Rebuilt with the node (a column reader marks only the node dirty), so it doesn't churn row refs.
        for (GPropertyDraw draw : form.propertyDraws) {
            if (draw.groupObject != group || !draw.isList || draw.integrationSID == null || draw.hasColumnGroupObjects())
                continue;
            if (!isPropertyShown(draw, GGroupObjectValue.EMPTY))
                continue;
            JavaScriptObject colMeta = buildPropMeta(draw, GGroupObjectValue.EMPTY, true);
            if (colMeta != null)
                setValue(getMeta(node), draw.integrationSID, colMeta);
        }
        return node;
    }

    // row.meta.row = { background, foreground, selected } from the group's PER-ROW readers (keyed by the row key); each
    // reader self-declares its field + converter (COLOR / FLAG), so this is the same emitPresentation loop as everywhere else.
    private JavaScriptObject buildRowMeta(GGroupObject group, GGroupObjectValue rowKey) {
        JavaScriptObject[] rm = {null};
        for (GGroupObjectPropertyReader reader : group.getPresentationReaders()) // the group's PER-ROW (ROW-scoped) readers self-declare field+converter (background/foreground -> COLOR, selected -> FLAG)
            if (reader != null && reader.getMetaScope() == GMetaScope.ROW)
                emitPresentation(rm, reader, rowKey, null, null);
        return rm[0];
    }

    // Rows may be reused only if every dirty key can be matched against a row key. A grouped-in-columns draw
    // (hasColumnGroupObjects) is dirtied under its COMPOSITE key (getValueKey keeps it whole), which no row key equals —
    // the per-row dirty mapping is then untrustworthy (a row could have changed without its key being marked), so every
    // row must rebuild. The test is the key's SHAPE, not its history: a row-shaped key that is not a current row (a just
    // DELETED row) is simply never consulted, so the survivors must keep their identity.
    private boolean canReuseRows(GGroupObject group, NativeHashMap<GGroupObjectValue, Boolean> dirtyKeys) {
        if (dirtyKeys == null)
            return true;
        int rowKeySize = group.objects.size();
        boolean[] ok = {true};
        dirtyKeys.foreachEntry((k, v) -> { if (k.size() != rowKeySize) ok[0] = false; });
        return ok[0];
    }

    public void clearDirty() {
        dirtyNodes.clear();
        dirtyLists.clear();
        dirtyOrder.clear();
        dirtyRowKeys.clear();
        dirtyScopes.clear();
        dirtyComponents.clear();
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
            if (!isPropertyShown(draw, key))
                continue;
            NativeHashMap<GGroupObjectValue, PValue> store = values.get(draw);
            GGroupObjectValue valueKey = list ? key : draw.filterColumnKeys(key);
            PValue pvalue = store == null || valueKey == null ? null : store.get(valueKey);
            setValue(target, draw.integrationSID, GSimpleStateTableView.convertToJSValue(draw, pvalue, RendererType.SIMPLE, true));
            JavaScriptObject propMeta = buildPropMeta(draw, valueKey, !list); // target.meta[integrationSID]: a row cell is dynamic-only; a panel/form-level prop (node) gets column-level static (caption/image)
            if (propMeta != null)
                setValue(getMeta(target), draw.integrationSID, propMeta);
        }
    }

    // ===== data.meta: the per-property PRESENTATION the platform computes for native rendering, projected for react-owned
    // properties from the SAME `values` store (each presentation reader), converted via the reader's own GMetaConverter
    // (the same helpers the GWT views use). Built inline with the value (shares the row/node/top structural sharing); a presentation
    // reader change marks the owning cell/row/scope dirty (markPresentationDirty) so meta rebuilds with its target.

    // the per-property presentation object for one cell/panel key, or null if no reader delivered a value (omit an empty one).
    // Each of the draw's presentation readers declares its own meta field name + converter, so this is a generic loop.
    // one property's presentation, read at `key`. columnStatic = the COLUMN/base projection (node.meta[prop] for a panel or
    // a list column): a column-level reader (caption/footer/property image) falls back to its static design
    // value. columnStatic=false = the per-CELL projection (row.meta[prop]): dynamic-only, no static.
    private JavaScriptObject buildPropMeta(GPropertyDraw draw, GGroupObjectValue key, boolean columnStatic) {
        JavaScriptObject[] m = {null};
        for (GPropertyReader reader : draw.getPresentationReaders()) {
            if (reader == null)
                continue;
            // the static design value belongs in the COLUMN entry, whatever the reader's dynamic value is keyed by:
            // a cell-level reader (pattern / comment / placeholder / tooltip / colors ...) misses at the column key, so its
            // static lands there, and its per-row dynamic value overrides it in the row entry — exactly the column-then-row
            // merge the consumer does. Without this a static design PATTERN (and the rest) reached no React view at all.
            emitPresentation(m, reader, key, draw, columnStatic ? reader.getColumnStatic(draw) : null);
        }
        return m[0];
    }

    // read pv from the reader's store, convert per the reader's declared kind, set on the (lazily created) meta object; skip
    // if absent/null. GWT represents java.lang.Boolean as a native JS boolean, so setValue stores a String / boolean / JS
    // object all as their primitive JS form (a Boolean lands as a real true/false, not a truthy wrapper) — one path fits all.
    // the ONE presentation-field emitter, shared by data.meta (react-owned props) and data.components (delegated children):
    // the dynamic reader value if delivered (converted via the reader's own converter), else the static design value
    // (null on the dynamic-only meta path), else skip. The only difference between the two projections is staticValue.
    private void emitPresentation(JavaScriptObject[] holder, GPropertyReader reader, GGroupObjectValue key, GPropertyDraw draw, String staticValue) {
        PValue pvalue = readerValue(reader, key);
        Object dynamic = pvalue != null ? reader.getMetaConverter().convert(pvalue, draw) : null; // getMetaConverter only reached when reader delivered -> reader != null
        Object value = isPresent(dynamic) ? dynamic : staticValue; // dynamic wins; else the static design fallback (also when a delivered image was cleared)
        String field = pvalue != null ? reader.getMetaField(pvalue) : reader.getMetaField();
        if (field != null && isPresent(value))
            setValue(lazyMeta(holder), field, value);
    }

    private boolean isPropertyShown(GPropertyDraw draw, GGroupObjectValue key) {
        if (droppedProperties.get(draw) != null)
            return false;
        if (draw.showIfReader == null)
            return true;
        NativeHashMap<GGroupObjectValue, PValue> showIfs = values.get(draw.showIfReader);
        if (showIfs == null)
            return true;
        GGroupObjectValue columnKey = draw.filterColumnKeys(key);
        return columnKey != null && PValue.getBooleanValue(showIfs.get(columnKey));
    }

    // JS-level "has a value": unlike a GWT-generated Java `!= null` (which the falsy-primitive trap misfires on, dropping a delivered
    // false / 0 / "" — e.g. a JSON customOptions or a readOnly false), this only treats a real null/undefined as absent.
    private static native boolean isPresent(Object v) /*-{ return v !== undefined && v !== null; }-*/;

    private PValue readerValue(GPropertyReader reader, GGroupObjectValue key) {
        if (reader == null || key == null)
            return null;
        NativeHashMap<GGroupObjectValue, PValue> store = values.get(reader);
        return store == null ? null : store.get(key);
    }

    private static JavaScriptObject lazyMeta(JavaScriptObject[] m) { // lazily create the per-target meta object on the first delivered field (stays null -> omitted if none); cf. getMeta which always creates obj.meta
        if (m[0] == null)
            m[0] = newObject();
        return m[0];
    }



    private GContainer getGroupOwningReactContainer(GGroupObject group) {
        if (group == null)
            return null;
        return formController.getOwningReactContainer(group.grid != null ? group.grid : group.parent);
    }

    private GContainer getPropertyOwningReactContainer(GPropertyDraw draw) {
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
    private static native void setInt(JavaScriptObject obj, String key, int v) /*-{ obj[key] = v; }-*/;
    private static native JavaScriptObject getMeta(JavaScriptObject obj) /*-{ return obj.meta || (obj.meta = {}); }-*/; // the shared per-target meta object (per-prop presentation + row / group count)
}
