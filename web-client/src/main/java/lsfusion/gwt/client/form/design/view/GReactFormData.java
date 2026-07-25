package lsfusion.gwt.client.form.design.view;

import com.google.gwt.core.client.JavaScriptObject;

import java.util.ArrayList;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.GForm;
import lsfusion.gwt.client.GFormChanges;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.jsni.NativeSIDMap;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.GPropertyReader;
import lsfusion.gwt.client.form.property.GGroupObjectPropertyReader;
import lsfusion.gwt.client.form.property.GGroupAttributeScope;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.view.RendererType;
import lsfusion.gwt.client.form.object.table.grid.view.GSimpleStateTableView;

// Maintains the @lsfusion/core-shaped `data` snapshot for CUSTOM REACT containers,
// accumulated incrementally from each GFormChanges delta, built into a JS object on demand.
// data = { <groupSID>: { list:[{key, isCurrent, <propSID>:{value,...}}], byKey, keys, count, <propSID>:{caption} }, <formPropSID>:{value,...}, <containerSID>:{caption,image} }
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
    private final NativeSIDMap<GGroupObject, JavaScriptObject> lastGroups = new NativeSIDMap<>();   // last built node per group
    private final NativeSIDMap<GGroupObject, JavaScriptObject> lastLists = new NativeSIDMap<>();   // last built list array per group
    private final NativeSIDMap<GGroupObject, JavaScriptObject> lastKeys = new NativeSIDMap<>();   // last built STABLE keys array per group (ref changes only on membership/order, never on a value/current change) - the <List> subscription path
    private final NativeSIDMap<GGroupObject, NativeHashMap<GGroupObjectValue, JavaScriptObject>> lastRows = new NativeSIDMap<>(); // last row obj per (group, key)
    private final NativeSIDMap<GGroupObject, Boolean> dirtyGroups = new NativeSIDMap<>();      // group node must rebuild (current/list/prop changed)
    private final NativeSIDMap<GGroupObject, Boolean> dirtyLists = new NativeSIDMap<>();      // group list (rows/order) changed
    private final NativeSIDMap<GGroupObject, Boolean> dirtyOrder = new NativeSIDMap<>();      // group membership/order changed (rebuild the stable keys array) - set ONLY by add/remove/reorder, NOT by value/current changes
    private final NativeSIDMap<GGroupObject, NativeHashMap<GGroupObjectValue, Boolean>> dirtyRowKeys = new NativeSIDMap<>(); // rows whose values changed
    private final NativeSIDMap<GContainer, Boolean> dirtyScopes = new NativeSIDMap<>();       // scopes whose top object must rebuild

    // ===== there is NO `meta` namespace: every thing the platform computed is projected DIRECTLY, keyed as the thing is
    // keyed, with its value and attributes as sibling fields.
    //   a PROPERTY is an object { value, <attrs...> }: a react-owned value under `.value`, its attributes beside it. A
    //     grouped property's COLUMN attributes (caption/image, once) live at data.<group>.<prop>; its per-CELL value +
    //     attributes at data.<group>.list[i].<prop>. A form-level property is one object at data.<prop>. Each attribute
    //     is at ONE point (effective = dynamic else static design default), so a consumer never merges.
    //   a GROUP's own attributes (count, options) are direct on its node, beside list/byKey/keys.
    //   a CONTAINER is data.<containerSID> = { caption, image }, direct in data like a group or a property.
    // Ownership excludes an LSF subtree (the chain that answers it stops at the lsf child): the platform draws such a
    // subtree whole, and React only labels its boundary. An LSF property/container projects caption/image only.

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
            markGroupDirty(group, GroupDirty.ORDER); // row set / order changed -> the stable keys array must rebuild too
        });

        fc.properties.foreachEntry((reader, keyValues) -> {
            NativeHashMap<GGroupObjectValue, PValue> fStore = getOrCreateValues(reader);
            if (reader instanceof GPropertyDraw && ((GPropertyDraw) reader).integrationSID != null) {
                GPropertyDraw draw = (GPropertyDraw) reader;
                boolean wasDropped = droppedProperties.remove(draw) != null; // a shown draw is sent again with its values
                NativeHashMap<GGroupObjectValue, PValue> changedKeys = putChanged(fStore, keyValues, draw);
                // an LSF draw's VALUE is platform-drawn: no entry carries it, so its delta rebuilds nothing - EXCEPT
                // when the draw comes back from a drop, which must re-project its descriptor entry
                if (changedKeys != null && (!draw.isLsfView() || wasDropped))
                    markPropertyDirty(draw, changedKeys);
            } else {
                // every other reader is an ATTRIBUTE of something - a property's, a container's, a group's or a row's -
                // and markAttributeDirty routes it by the OWNER the reader itself names. Without the marking, its owning
                // cell/row/node/scope would not rebuild and the projected attribute would go stale.
                NativeHashMap<GGroupObjectValue, PValue> changedKeys = putChanged(fStore, keyValues, null);
                if (changedKeys != null)
                    markAttributeDirty(reader, changedKeys);
            }
        });

        for (GPropertyDraw drop : fc.dropProperties) {
            droppedProperties.put(drop, Boolean.TRUE);
            values.remove(drop);
            markPropertyPresenceDirty(drop);
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
        markGroupDirty(group, GroupDirty.ROWS); // panel props for the new current; old + new current rows flip isCurrent
        markRowDirty(group, old);
        markRowDirty(group, key);
        return true;
    }
    // apply one optimistic property value (from setLoadingValueAt) into the SAME `values` accumulator update(fc) writes,
    // keyed the same way (the property cell key), and mark it dirty like markPropertyDirty — so the react container shows
    // the edit immediately, reconciled later when the server fc.properties arrives. Returns false if the draw isn't projected.
    public boolean setPropertyValue(GPropertyDraw draw, GGroupObjectValue fullKey, PValue value) {
        if (draw.integrationSID == null || !formController.isReactOwned(draw) // the SAME ownership answer build() uses,
                || (draw.isList && !isProjectedListDraw(draw))) // so an optimistic value is never stored for a draw whose entry the projection would not build
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
    // rows that are byte-identical and break the structural-sharing contract (an untouched row MUST keep
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
    private boolean markPropertyEntryDirty(GPropertyDraw draw) {
        if (draw.isList && !isProjectedListDraw(draw)) // not projected -> its delta changes nothing to rebuild
            return false;
        GGroupObject group = draw.groupObject;
        if (group == null) { // form-level -> its entry on the top object (fullKey == EMPTY, the key fillSingles reads)
            markScopeDirty(getTopLevelScope(draw));
            return false;
        }
        // a panel property -> the node rebuilds and its list/rows are reused; a list cell -> the list (+ rows) rebuild too
        markGroupDirty(group, draw.isList ? GroupDirty.ROWS : GroupDirty.NODE);
        return draw.isList;
    }
    // a draw APPEARED or DISAPPEARED (a SHOWIF flip, a form-structure drop or restore): its entry's EXISTENCE changed,
    // which for a list draw with CELLS changes the shape of every projected row - per-row dirty keys cannot express that.
    // An LSF list draw has no cells (only its column entry comes and goes), so its group node rebuilds and the rows stay.
    private void markPropertyPresenceDirty(GPropertyDraw draw) {
        if (draw.integrationSID == null)
            return;
        if (draw.isLsfView() && draw.isList) {
            markGroupDirty(draw.groupObject, GroupDirty.NODE);
            return;
        }
        if (markPropertyEntryDirty(draw)) // true exactly for a projected LIST draw - the one whose rows change shape
            invalidateRows(draw.groupObject);
    }
    private void markPropertyDirty(GPropertyDraw draw, GGroupObjectValue key) {
        if (markPropertyEntryDirty(draw))
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
                markGroupDirty(group, GroupDirty.ORDER); // optimistic add -> membership changed
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
        markGroupDirty(group, GroupDirty.ORDER); // optimistic remove -> membership changed
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
    // the group's rows in order - the same list an optimistic add or delete maintains, so a caller reading it here sees
    // a new row before the server has confirmed it
    public ArrayList<GGroupObjectValue> getRows(GGroupObject group) { return gridRows.get(group); }
    public PValue getValue(GPropertyDraw draw, GGroupObjectValue key) {
        NativeHashMap<GGroupObjectValue, PValue> store = values.get(draw);
        return store == null || key == null ? null : store.get(key);
    }
    public int getRowIndex(GGroupObject group, GGroupObjectValue key) {
        ArrayList<GGroupObjectValue> rows = gridRows.get(group);
        return rows == null || key == null ? -1 : rows.indexOf(key);
    }

    // how much of a group's node the next build has to redo. The levels are MONOTONE - ORDER implies ROWS implies NODE -
    // so a caller states the strongest thing that changed and the implications follow, instead of re-establishing the
    // chain by hand at every site (which is what silently drifts: an order change that forgets to dirty the list).
    private enum GroupDirty { NODE, ROWS, ORDER }

    private void markGroupDirty(GGroupObject group, GroupDirty level) {
        GContainer scope = getGroupOwningReactContainer(group);
        if (scope == null)
            return;
        dirtyGroups.put(group, Boolean.TRUE);
        markScopeDirty(scope);
        if (level != GroupDirty.NODE)
            dirtyLists.put(group, Boolean.TRUE);
        if (level == GroupDirty.ORDER)
            dirtyOrder.put(group, Boolean.TRUE);
    }

    // rebuild the list AND forbid reusing any cached row: the change altered the SHAPE of every projected row (a
    // property's entry appeared or disappeared in each of them), which per-row dirty keys cannot express. Deliberately
    // NOT a GroupDirty level: ORDER still reuses surviving rows, while this preserves order but rebuilds them all.
    private void invalidateRows(GGroupObject group) {
        markGroupDirty(group, GroupDirty.ROWS);
        lastRows.put(group, null);
    }

    private void markRowDirty(GGroupObject group, GGroupObjectValue key) { // a row whose `value`/isCurrent/props changed must rebuild
        if (key == null) return;
        NativeHashMap<GGroupObjectValue, Boolean> dr = dirtyRowKeys.get(group);
        if (dr == null) { dr = new NativeHashMap<>(); dirtyRowKeys.put(group, dr); }
        dr.put(key, Boolean.TRUE);
    }
    private void markPropertyDirty(GPropertyDraw draw, NativeHashMap<GGroupObjectValue, PValue> changedKeys) {
        if (markPropertyEntryDirty(draw))
            changedKeys.foreachEntry((k, v) -> markRowDirty(draw.groupObject, getValueKey(draw, k)));
    }

    // a react-owned presentation reader changed: mark its owning cell/row/node/scope dirty so the next build re-projects
    // its projected object (mirrors markPropertyDirty for the value draw). No-ops for non-react readers (their scopes resolve null).
    private void markAttributeDirty(GPropertyReader reader, NativeHashMap<GGroupObjectValue, PValue> keyValues) {
        if (reader.isPresenceReader()) { // it decides whether the entry EXISTS, so it is not routed as an attribute of one
            GComponent shown = reader.getAttributeComponent(form);
            if (shown instanceof GPropertyDraw)
                markPropertyPresenceDirty((GPropertyDraw) shown);
            return;
        }
        if (reader.getAttributeField() == null) // this reader is NOT projected (native CSS/font, loading, last, changeKey/changeMouse): no entry carries it,
            return;                        // so its delta changes nothing in `data` — dirtying its rows would rebuild them for nothing
        GComponent owner = reader.getAttributeComponent(form);
        if (owner instanceof GPropertyDraw) { // the attribute of a PROPERTY: where it is projected says what to rebuild
            GPropertyDraw draw = (GPropertyDraw) owner;
            if (draw.integrationSID != null) {
                if (draw.isList && reader.isColumnAttribute(draw)) // a column attribute lives on the group -> rebuild the group only, DON'T churn the list/row refs
                    markGroupDirty(draw.groupObject, GroupDirty.NODE);
                else // a cell (or single-value) attribute -> the same marking as the value it sits with
                    markPropertyDirty(draw, keyValues);
            }
            return;
        }
        GContainer containerScope = getContainerReaderScope(reader); // a projected CONTAINER's own caption/image: its
        if (containerScope != null) {                                // entry sits on the scope's top object, which
            markScopeDirty(containerScope);                          // rebuilds while nodes and rows are reused
            return;
        }
        GGroupObject group = reader.getAttributeGroup(form); // the attribute of the GROUP itself: rowBackground/rowForeground/rowSelect, or options
        if (group == null)
            return;
        // a per-row attribute -> the list + each changed row; a group-scoped one (options / count) -> the node only
        boolean perRow = reader.getAttributeScope() == GGroupAttributeScope.ROW;
        markGroupDirty(group, perRow ? GroupDirty.ROWS : GroupDirty.NODE);
        if (perRow)
            keyValues.foreachEntry((k, v) -> markRowDirty(group, k));
    }

    private GGroupObjectValue getValueKey(GPropertyDraw draw, GGroupObjectValue key) {
        if (draw.groupObject == null || !draw.isList) // grouped-in-columns draws never get here (isProjectedListDraw gates every caller)
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
            JavaScriptObject node = lastGroups.get(group);
            if (node == null || dirtyGroups.get(group) != null) { // rebuild only a changed (or first-seen) group node
                node = buildGroupEntry(group);
                lastGroups.put(group, node);
            }
            setValue(data, group.getSID(), node);
        }
        fillSingles(data, null, GGroupObjectValue.EMPTY, scope); // the form-level properties, on the new top
        fillContainers(data, scope);
        lastData.put(scope, data);
        return data;
    }

    // ===== containers ==========================================================================================

    // a container with an entry in `data`: the author DECLARED it (DESIGN's `NEW <name>` - not "has a name", a group's
    // generated BOX(g) is named too, for icons), OR it is an lsf child - whatever box that is. The lsf half is not a
    // convenience: GWT skips an lsf child's caption/image for React to draw (the complement invariant), so a generated
    // box marked lsf - MOVE BOX(o) { lsf = TRUE; }, the canonical case - MUST project its descriptor or its caption is
    // drawn by nobody. What stays out is the boxes nobody wrote AND nobody placed: a group's toolbar/filter machinery.
    private boolean isProjectedContainer(GComponent component) {
        return component instanceof GContainer && (((GContainer) component).declared || component.isLsfView());
    }

    // a projected container goes DIRECTLY in data, keyed by its design sid, with what the platform computed for it
    // (caption / image) - a container is a thing in `data` like a group or a property, no meta wrapper. ALWAYS, `{}`
    // when it has neither, so nothing has to predict what the entry will hold. WHICH containers is isProjectedContainer
    // (declared or lsf); the scope walk is what excludes an LSF SUBTREE: ownership climbs only through non-lsf hops, so
    // nothing below an lsf child - declared or not - resolves to any scope. Only the boundary itself is projected;
    // everything under it the platform draws whole, its captions consumed by its own native renderers.
    private void fillContainers(JavaScriptObject data, GContainer scope) {
        fillContainers(data, scope, form.mainContainer);
    }
    private void fillContainers(JavaScriptObject data, GContainer scope, GComponent component) {
        if (component instanceof GContainer) {
            if (getProjectedContainerScope(component) == scope)
                setValue(data, component.sID, buildDescriptorEntry(component, GGroupObjectValue.EMPTY));
            for (GComponent child : ((GContainer) component).children)
                fillContainers(data, scope, child);
        }
    }

    // the scope whose data carries this container's entry, or null when it has none - THE statement of the container
    // rule, asked by the build (fillContainers) and by the delta path (getContainerReaderScope) alike, so the two cannot
    // drift. The server asks its own copy of the same question under the same name (FormView.getProjectedContainerScope)
    // to reserve the names this emits. An LSF container is placed by the scope it sits in (getTopLevelScope).
    private GContainer getProjectedContainerScope(GComponent component) {
        return isProjectedContainer(component) ? getTopLevelScope(component) : null;
    }

    // what the platform computed about a COMPONENT itself (a container, or an lsf property whose value it draws):
    // caption and image, dynamic value first and the static design value as the fallback. `{}` when it has neither - an
    // entry is never withheld for being empty, so nothing has to predict emptiness (see fillContainers).
    // For an LSF property this set is not a narrowing of its column attributes, it is the exact complement of what its
    // platform renderer draws: GFormController.isReactOwned hands GWT every reader of an lsf draw and takes back only
    // what isDescriptorAttribute + isProjectedDescriptorAttribute admit (isLsfViewDescriptorReader) - the rest stay with
    // the renderer that renders them; projecting them would draw them twice. The two sides ask the same predicates.
    private JavaScriptObject buildDescriptorEntry(GComponent component, GGroupObjectValue key) {
        JavaScriptObject entry = newObject();
        for (GPropertyReader reader : component.getDescriptorReaders())
            if (isProjectedDescriptorAttribute(reader, component))
                emitAttribute(entry, reader, key, component);
        return entry;
    }

    // whether this component attribute reaches the projection at all. An LSF LIST property has one entry for the
    // whole column, so a row-keyed attribute (an action's image) has no place in it - it stays with the per-row renderer,
    // which does key it by row, instead of being taken away from GWT and then dropped. Only a list has that problem: a
    // single-valued draw's entry is read at its own key, so every component attribute of it fits. The same answer decides
    // both sides, which is what keeps them from disagreeing: what GWT skips (getLsfViewDescriptorOwner) is exactly what is emitted.
    private boolean isProjectedDescriptorAttribute(GPropertyReader reader, GComponent component) {
        if (reader == null)
            return false;
        return !(component instanceof GPropertyDraw) || !((GPropertyDraw) component).isList
                || reader.isColumnAttribute((GPropertyDraw) component);
    }

    // the LSF child (a container or a property) whose caption / image this reader carries, or null - isLsfView()
    // already means a direct child of a React container, so its caption is React's to draw and GWT skips this reader
    private GComponent getLsfViewDescriptorOwner(GPropertyReader reader) {
        GComponent child = getDescriptorOwner(reader);
        return child != null && child.isLsfView() && isProjectedDescriptorAttribute(reader, child) ? child : null;
    }

    // the component whose OWN caption / image this reader carries, or null for anything else - both halves are the
    // reader's own answers (getAttributeComponent / isDescriptorAttribute), so nothing here dispatches on its class
    private GComponent getDescriptorOwner(GPropertyReader reader) {
        return reader.isDescriptorAttribute() ? reader.getAttributeComponent(form) : null;
    }

    // this reader's owning CONTAINER's entry scope, or null for anything else - including a generated box, which has no
    // entry to dirty (a property's caption/image reader is presentation like any other and reaches its object through
    // the ordinary presentation path)
    private GContainer getContainerReaderScope(GPropertyReader reader) {
        return getProjectedContainerScope(getDescriptorOwner(reader));
    }

    // the scope whose data carries this component - a container, or a form-level property (both live directly on the
    // top object, keyed as the thing is keyed). An LSF component is placed by the scope it is declared in: the
    // platform draws it, but hands its caption to React. Anything else belongs to the scope that OWNS it, which by
    // construction is nothing inside an lsf subtree. (A GROUPED property's object lives in its group node, not here.)
    private GContainer getTopLevelScope(GComponent component) {
        return component.isLsfView() ? component.container : formController.getOwningReactContainer(component);
    }

    public boolean isLsfViewDescriptorReader(GPropertyReader reader) {
        return getLsfViewDescriptorOwner(reader) != null;
    }

    // build a group's node, reusing the unchanged list array and unchanged row objects
    private JavaScriptObject buildGroupEntry(GGroupObject group) {
        JavaScriptObject node = newObject();
        GGroupObjectValue current = currentObjects.get(group);

        ArrayList<GGroupObjectValue> rows = gridRows.get(group);
        JavaScriptObject list = lastLists.get(group);
        JavaScriptObject byKey = lastByKey.get(group);
        if (rows != null) {
            if (list == null || dirtyLists.get(group) != null) { // rebuild the list only if its rows/order/values changed
                NativeHashMap<GGroupObjectValue, JavaScriptObject> prevRows = lastRows.get(group);
                // every dirty key is row-shaped by construction: composite (grouped-in-columns) draws never reach the
                // dirty paths (isProjectedListDraw), so an unchanged row is ALWAYS safe to reuse by its key
                NativeHashMap<GGroupObjectValue, Boolean> dirtyKeys = dirtyRowKeys.get(group);
                NativeHashMap<GGroupObjectValue, JavaScriptObject> newRows = new NativeHashMap<>();
                // canonical key string -> row, rebuilt WITH the list (row refs shared with it): selectors subscribe
                // by STABLE key (s.i.byKey[row.key] — property lookup coerces a numeric key to the same string) so
                // surviving rows after a delete keep their selected identity; cached like the list, so a node-only
                // change (panel prop) keeps byKey identity too
                byKey = newObject();
                list = newArray();
                for (GGroupObjectValue rowKey : rows) {
                    JavaScriptObject prev = prevRows != null ? prevRows.get(rowKey) : null;
                    JavaScriptObject row;
                    if (prev != null && (dirtyKeys == null || dirtyKeys.get(rowKey) == null)) {
                        row = prev; // reuse the unchanged row object (same ref -> the row component memo-skips)
                    } else {
                        row = newObject();
                        fillRowAttributes(row, group, rowKey, current); // what the ROW itself is: isCurrent, background, foreground, selected
                        fillCells(row, group, rowKey);                  // what is ON the row: one entry per list property
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
        if (current != null) // the group's panel properties (shown once, for the current object)
            fillSingles(node, group, current, null);
        // the group's own attributes, DIRECT on the node: the loaded row count + its GROUP-scoped readers (options, at
        // EMPTY). No meta wrapper - `count`/`options` sit beside `list`/the column property objects, rebuilt with the node.
        fillGroupAttributes(node, group, rows); // what the GROUP itself is: count, options
        // and what is the same down each COLUMN. Reading node.<prop> for the caption and row.<prop>.value for the value is
        // the column/cell split without a merge - each attribute lives at exactly one point. Rebuilt with the node (a
        // column reader marks only the node dirty), so it doesn't churn row refs.
        fillColumns(node, group);
        return node;
    }

    // the group's own PER-ROW attributes (background / foreground / selected), DIRECT on the row beside `isCurrent` - each
    // reader self-declares its field + converter (COLOR / FLAG). No meta wrapper; the field names are reserved so a
    // property cannot take them (checkReactProjectionNames).
    private void fillRowAttributes(JavaScriptObject row, GGroupObject group, GGroupObjectValue rowKey, GGroupObjectValue current) {
        setBoolean(row, "isCurrent", current != null && rowKey.equals(current)); // declarative current-row marker
        for (GGroupObjectPropertyReader reader : group.getPresentationReaders())
            if (reader != null && reader.getAttributeScope() == GGroupAttributeScope.ROW)
                emitAttribute(row, reader, rowKey, null);
    }

    // the GROUP's own attributes, direct on its node beside list/byKey/keys: how many rows are loaded, and its
    // group-scoped readers (options, read once at EMPTY). The mirror of fillRowAttributes, one level up.
    private void fillGroupAttributes(JavaScriptObject node, GGroupObject group, ArrayList<GGroupObjectValue> rows) {
        setInt(node, "count", rows != null ? rows.size() : 0);
        for (GGroupObjectPropertyReader reader : group.getPresentationReaders())
            if (reader != null && reader.getAttributeScope() == GGroupAttributeScope.GROUP)
                emitAttribute(node, reader, GGroupObjectValue.EMPTY, null);
    }

    public void clearDirty() {
        dirtyGroups.clear();
        dirtyLists.clear();
        dirtyOrder.clear();
        dirtyRowKeys.clear();
        dirtyScopes.clear();
    }

    // group/property SID resolution lives on GForm (shared with the other integration controllers); row identity
    // registration/resolution is centralized on GGroupObjectValue (registerRow/resolveObject)

    // the CELLS of one row: every projected list property of the group contributes its {value, ...cell attributes}
    // under its sid. EXISTENCE is decided here, where the row is enumerated - not by a builder returning null: an LSF
    // draw has no cell (the platform draws it in the row), and a grouped-in-columns draw is not projected at all.
    private void fillCells(JavaScriptObject row, GGroupObject group, GGroupObjectValue rowKey) {
        for (GPropertyDraw draw : form.propertyDraws)
            if (draw.groupObject == group && draw.isList && isProjectedListDraw(draw) && !draw.isLsfView()
                    && isShownProperty(draw, rowKey))
                setValue(row, draw.integrationSID, buildCellEntry(draw, rowKey));
    }

    // a list draw the projection carries at all. Grouped-in-columns draws are a follow-up: their attributes are keyed by
    // a COLUMN tuple that one node entry cannot hold (and one row key cannot address), so they are skipped EVERYWHERE -
    // the build (no entry), the dirty paths and the optimistic write (nothing to rebuild) - by this one predicate.
    private boolean isProjectedListDraw(GPropertyDraw draw) {
        return !draw.hasColumnGroupObjects();
    }

    // the SINGLE-valued properties on a target: a group's panel properties on its node, or the form-level properties on
    // the scope's top object - each one entry with its value and all its attributes
    private void fillSingles(JavaScriptObject target, GGroupObject group, GGroupObjectValue key, GContainer scope) {
        for (GPropertyDraw draw : form.propertyDraws) {
            if (draw.groupObject != group || draw.isList)
                continue;
            if (group == null && getTopLevelScope(draw) != scope) // a form-level property belongs to the scope it sits in
                continue;
            GGroupObjectValue valueKey = draw.filterColumnKeys(key);
            if (valueKey != null && isShownProperty(draw, key))
                setValue(target, draw.integrationSID, buildSingleEntry(draw, valueKey));
        }
    }

    // the COLUMNS of a group: every list property contributes what is the same down its whole column, once, on the node
    private void fillColumns(JavaScriptObject node, GGroupObject group) {
        for (GPropertyDraw draw : form.propertyDraws)
            if (draw.groupObject == group && draw.isList && isProjectedListDraw(draw)
                    && isShownProperty(draw, GGroupObjectValue.EMPTY))
                setValue(node, draw.integrationSID, buildColumnEntry(draw));
    }

    // a draw reaches the projection at all: it has a name there, and it is not hidden right now
    private boolean isShownProperty(GPropertyDraw draw, GGroupObjectValue key) {
        return draw.integrationSID != null && isPropertyShown(draw, key);
    }

    // ===== a property's projected ENTRY. Projected AT ALL is decided before this (isShownProperty / delegation); once it
    // is, the entry exists whatever it holds, so `data.<group>.<prop>` is there even for a column with no caption at all.
    // A property drawn per row has TWO of them, because it genuinely has two: one
    // caption for the whole column, one value and one background per row. A property with a single value has ONE, with
    // everything in it. Each attribute is an EFFECTIVE value - the dynamic value at the key, else the static design
    // default - so it is delivered at one point and a consumer never merges a column base with a row override.
    // An LSF property hands React only caption/image (the platform draws its value WITH the rest of its
    // presentation), so its column / single entry is a component descriptor and it has no cell at all.

    // the COLUMN entry of a list property: what is the same down the whole column (caption / image / footer / comment /
    // tooltip / default). No value - the values are in the cells.
    private JavaScriptObject buildColumnEntry(GPropertyDraw draw) {
        if (draw.isLsfView())
            return buildDescriptorEntry(draw, GGroupObjectValue.EMPTY);
        JavaScriptObject entry = newObject();
        for (GPropertyReader reader : draw.getPresentationReaders())
            if (reader != null && reader.isColumnAttribute(draw))
                emitAttribute(entry, reader, GGroupObjectValue.EMPTY, draw);
        return entry;
    }

    // one ROW's cell of a list property: its value, and the attributes that can differ from row to row (background /
    // foreground / readOnly / placeholder / pattern / ...)
    private JavaScriptObject buildCellEntry(GPropertyDraw draw, GGroupObjectValue rowKey) {
        JavaScriptObject entry = newObject();
        emitValue(entry, draw, rowKey);
        for (GPropertyReader reader : draw.getPresentationReaders())
            if (reader != null && !reader.isColumnAttribute(draw))
                emitAttribute(entry, reader, rowKey, draw);
        return entry;
    }

    // the single entry of a property with ONE value (form-level, or a group's panel property): the value and ALL its
    // attributes together - with one value there is nothing to split between a column and a cell
    private JavaScriptObject buildSingleEntry(GPropertyDraw draw, GGroupObjectValue key) {
        if (draw.isLsfView())
            return buildDescriptorEntry(draw, key); // its own key, like any single entry - not EMPTY, which is a column's key
        JavaScriptObject entry = newObject();
        emitValue(entry, draw, key);
        for (GPropertyReader reader : draw.getPresentationReaders())
            if (reader != null)
                emitAttribute(entry, reader, key, draw);
        return entry;
    }

    // the value field of an entry, always present for a react-owned property (null value included, so the entry exists)
    private void emitValue(JavaScriptObject entry, GPropertyDraw draw, GGroupObjectValue key) {
        setValue(entry, "value", GSimpleStateTableView.convertToJSValue(draw, readerValue(draw, key), RendererType.SIMPLE, true));
    }

    // the ONE attribute emitter, shared by every entry (a property's, a container's, a group's, a row's): the EFFECTIVE
    // value of one attribute - the dynamic reader value if delivered (converted by the reader's own converter), else the
    // static design default - written under the field name the reader declares. Absent attributes are simply not written.
    // GWT represents java.lang.Boolean as a native JS boolean, so setValue stores a String / boolean / JS object all as
    // their primitive JS form (a Boolean lands as a real true/false, not a truthy wrapper) — one path fits all.
    private void emitAttribute(JavaScriptObject entry, GPropertyReader reader, GGroupObjectValue key, GComponent owner) {
        PValue pvalue = readerValue(reader, key);
        GPropertyDraw draw = owner instanceof GPropertyDraw ? (GPropertyDraw) owner : null; // the converter wants the draw; a container / a group's own reader has none
        Object dynamic = pvalue != null ? reader.getAttributeConverter().convert(pvalue, draw) : null; // getAttributeConverter only reached when reader delivered -> reader != null
        Object value = isPresent(dynamic) ? dynamic : (owner != null ? reader.getStaticAttribute(owner) : null); // dynamic wins; else the static design default (also when a delivered image was cleared)
        String field = reader.getAttributeField(pvalue); // the no-value case is the reader's own default
        if (field != null && isPresent(value))
            setValue(entry, field, value);
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
    // false / 0 / "" — e.g. a JSON options or a readOnly false), this only treats a real null/undefined as absent.
    private static native boolean isPresent(Object v) /*-{ return v !== undefined && v !== null; }-*/;

    private PValue readerValue(GPropertyReader reader, GGroupObjectValue key) {
        if (reader == null || key == null)
            return null;
        NativeHashMap<GGroupObjectValue, PValue> store = values.get(reader);
        return store == null ? null : store.get(key);
    }

    private GContainer getGroupOwningReactContainer(GGroupObject group) {
        if (group == null)
            return null;
        return formController.getOwningReactContainer(group.grid != null ? group.grid : group.parent);
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
}
