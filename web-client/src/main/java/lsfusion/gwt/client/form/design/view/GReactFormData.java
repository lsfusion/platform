package lsfusion.gwt.client.form.design.view;

import com.google.gwt.core.client.JavaScriptObject;

import java.util.ArrayList;
import lsfusion.gwt.client.base.GwtClientUtils;
import static lsfusion.gwt.client.base.GwtClientUtils.*;
import lsfusion.gwt.client.GForm;
import lsfusion.gwt.client.GFormChanges;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.jsni.NativeSIDMap;
import lsfusion.gwt.client.base.jsni.NativeStringMap;
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
// data = { <groupSID>: { list:[{key, isCurrent, <propSID>:{value,...}}], byKey, keys, properties, <propSID>:{caption} }, <formPropSID>:{value,...}, <containerSID>:{caption,image} }
public class GReactFormData {

    private final GForm form;
    private final GFormController formController;

    // accumulator (mirrors the deltas the normal views consume)
    private final NativeSIDMap<GGroupObject, JavaScriptObject> lastByKey = new NativeSIDMap<>(); // group -> { public key -> row }, cached with the list
    private final NativeSIDMap<GGroupObject, GGroupObjectValue> currentObjects = new NativeSIDMap<>();
    private final NativeSIDMap<GGroupObject, ArrayList<GGroupObjectValue>> gridRows = new NativeSIDMap<>(); // ordered row keys per group
    private final NativeSIDMap<GPropertyReader, NativeHashMap<GGroupObjectValue, PValue>> values = new NativeSIDMap<>();
    private final NativeSIDMap<GPropertyDraw, Boolean> droppedProperties = new NativeSIDMap<>(); // SHOWIF/static visibility removed the whole draw
    private final NativeSIDMap<GGroupObject, ArrayList<String>> lastPropertyNames = new NativeSIDMap<>(); // what `properties` last said, to hand back the SAME array when it still says it
    private final NativeSIDMap<GGroupObject, JavaScriptObject> lastProperties = new NativeSIDMap<>();
    private final NativeSIDMap<GGroupObject, NativeHashMap<String, Boolean>> unnameable = new NativeSIDMap<>(); // already reported, so the node rebuild does not repeat itself
    private final NativeSIDMap<GGroupObject, ArrayList<GContainer>> groupScopes = new NativeSIDMap<>(); // the react scopes each group's node appears in

    // ===== structural sharing: build() returns the SAME JS refs for unchanged subtrees, so React.memo'd components skip.
    // The cache holds the last-built objects; the dirty sets (per update, cleared after all scopes are built) decide what to rebuild.
    private final NativeSIDMap<GContainer, JavaScriptObject> lastData = new NativeSIDMap<>(); // last built top object per React scope
    // a node belongs to a (scope, group): one group's parts can be drawn in different containers, and then each of
    // them has a node of its own, holding what IT draws. Keyed by a string because the key is a pair.
    private final NativeStringMap<JavaScriptObject> lastNodes = new NativeStringMap<>();   // last built node per (scope, group)
    // ... and a PART belongs to a (component, group): the component is the producer, and one component can produce a
    // part for several groups - a TREE draws them all - so a component-keyed cache would hand two groups one object
    private final NativeStringMap<JavaScriptObject> lastParts = new NativeStringMap<>();   // last built part per (component, group)
    private final NativeSIDMap<GGroupObject, JavaScriptObject> lastLists = new NativeSIDMap<>();   // last built list array per group
    private final NativeSIDMap<GGroupObject, JavaScriptObject> lastKeys = new NativeSIDMap<>();   // last built STABLE keys array per group (ref changes only on membership/order, never on a value/current change) - the <List> subscription path
    private final NativeSIDMap<GGroupObject, NativeHashMap<GGroupObjectValue, JavaScriptObject>> lastRows = new NativeSIDMap<>(); // last row obj per (group, key)
    private final NativeStringMap<Boolean> dirtyParts = new NativeStringMap<>();               // parts that must be rebuilt, by (component, group)
    private final NativeStringMap<Boolean> dirtyNodes = new NativeStringMap<>();               // nodes that must be reassembled, by (scope, group)
    // a dirty part is materialized ONCE per pass however many scopes ask for it - without this marker the second scope
    // would rebuild it and hand out a different object for a part that did not change between the two questions
    private final NativeStringMap<Boolean> builtParts = new NativeStringMap<>();               // parts already rebuilt in this pass
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
    //   a GROUP's own attributes (its options, and what a feature on top of this states about it) are direct on its
    //     node, beside list/byKey/keys.
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
            markGridDirty(group, GridDirty.ORDER); // row set / order changed -> the stable keys array must rebuild too
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
        if (!isProjectedGroup(group))
            return false;
        GGroupObjectValue old = currentObjects.get(group);
        if (GwtClientUtils.nullEquals(old, key))
            return false;
        currentObjects.put(group, key);
        markCurrentDirty(group); // panel entries are read at the new current; old + new current rows flip isCurrent
        markRowDirty(group, old);
        markRowDirty(group, key);
        return true;
    }
    // apply one optimistic property value (from setLoadingValueAt) into the SAME `values` accumulator update(fc) writes,
    // keyed the same way (the property cell key), and mark it dirty like markPropertyDirty — so the react container shows
    // the edit immediately, reconciled later when the server fc.properties arrives. Returns false if the draw isn't projected.
    public boolean setPropertyValue(GPropertyDraw draw, GGroupObjectValue fullKey, PValue value) {
        if (draw.integrationSID == null || !formController.isReactOwned(draw)) // the SAME ownership answer build() uses,
            return false;                                                      // so a value is stored only where an entry is built
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
        // the one place a columns draw still reaches: `update` walks every reader the server sent, for the whole form.
        // Nothing the projection carries is one - such a form is not built (FormView.checkProjectedDraw) - so a draw
        // that gets here is one nothing projects, and its delta has no entry to rebuild. Said for a LIST draw, which
        // is the one that would go on to rebuild rows; a form-level one falls through to a scope that is null anyway
        if (draw.isList && draw.hasColumnGroupObjects())
            return false;
        // an LSF draw: what the projection carries for it is its DESCRIPTOR, which sits at its descriptor scope - not
        // at a part scope, which it has none of (the platform draws it). Its caption, its image and its very presence
        // are read from there, so that is what a change to it must rebuild, whether it has a group or not
        if (draw.isLsfView()) {
            markScopeDirty(descriptorScope(draw));
            return false;
        }
        GGroupObject group = draw.groupObject;
        if (group == null) { // form-level -> its entry on the top object (fullKey == EMPTY, the key fillFormSingles reads)
            markScopeDirty(getTopLevelScope(draw));
            return false;
        }
        if (!draw.isList) { // a panel property -> its own entry, and the node it is assembled into; the grid untouched
            markPanelDirty(draw);
            return false;
        }
        markGridDirty(group, GridDirty.ROWS); // a list cell -> the grid part, its list and the changed rows
        return true;
    }
    // a draw APPEARED or DISAPPEARED (a SHOWIF flip, a form-structure drop or restore): its entry's EXISTENCE changed,
    // which for a list draw with CELLS changes the shape of every projected row - per-row dirty keys cannot express that.
    // An LSF list draw has no cells (only its column entry comes and goes), so its group node rebuilds and the rows stay.
    private void markPropertyPresenceDirty(GPropertyDraw draw) {
        if (draw.integrationSID == null)
            return;
        if (draw.isLsfView() && draw.isList) {
            markGridDirty(draw.groupObject, GridDirty.ENTRIES);
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
        if (!isProjectedGroup(group))
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
                markGridDirty(group, GridDirty.ORDER); // optimistic add -> membership changed
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
        markGridDirty(group, GridDirty.ORDER); // optimistic remove -> membership changed
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
    // a row of this group named by whatever a view has: the row itself, its `objects` handle, or the KEY the
    // projection gave it - `row.key`, a member of `keys`, a key out of `byKey`. The last is the one that used to be
    // a dead end: `byKey` is a JS object, so its keys are strings by the language's rule, and a key that has been
    // through it has lost the type it was written with. It is still this group's key, and this group's rows are
    // right here, so it is looked up rather than refused. Grid or tree alike: the canonical string is exactly what
    // both `byKey` and `keys` are keyed by, whether the row's key is one object or a whole tree path.
    public GGroupObjectValue resolveRowKey(GGroupObject group, JavaScriptObject keyOrRow) {
        GGroupObjectValue resolved = GGroupObjectValue.resolveObject(keyOrRow); // a row, a clone of one, or a handle
        if (resolved != null)
            return resolved;
        // ... or the canonical key string the projection handed out - which is exactly what `byKey` is keyed by, so
        // the group's own index answers it in one lookup. It is rebuilt WITH the rows, so a key whose row has gone
        // finds nothing, rather than a row that is no longer there
        JavaScriptObject byKey = lastByKey.get(group);
        return byKey == null ? null : GGroupObjectValue.resolveObject(keyedRow(byKey, keyOrRow));
    }

    // a token is the two shapes a projected key is written in: a number for a single object, the canonical string
    // otherwise - anything else is not a key this group handed out
    private static native JavaScriptObject keyedRow(JavaScriptObject byKey, JavaScriptObject token) /*-{
        return (typeof token === 'string' || typeof token === 'number') ? byKey[String(token)] : null;
    }-*/;

    // `key` is the cell's key as a caller has it - a row key, or a column key for a single-valued draw. Turned here by
    // the ONE rule the store is written by, so no caller has to know it, and none can apply it twice
    public PValue getValue(GPropertyDraw draw, GGroupObjectValue key) {
        NativeHashMap<GGroupObjectValue, PValue> store = values.get(draw);
        return store == null || key == null ? null : store.get(getValueKey(draw, key));
    }
    public int getRowIndex(GGroupObject group, GGroupObjectValue key) {
        ArrayList<GGroupObjectValue> rows = gridRows.get(group);
        return rows == null || key == null ? -1 : rows.indexOf(key);
    }

    // how much of a group's node the next build has to redo. The levels are MONOTONE - ORDER implies ROWS implies NODE -
    // so a caller states the strongest thing that changed and the implications follow, instead of re-establishing the
    // chain by hand at every site (which is what silently drifts: an order change that forgets to dirty the list).
    // what a change to the GRID's part touched: only what is written on the node (a column caption, an option), the
    // rows too, or the membership/order of the rows. Each level is a superset of the one before it.
    private enum GridDirty { ENTRIES, ROWS, ORDER }

    private void markGridDirty(GGroupObject group, GridDirty level) {
        if (!markPartDirty(group.getDrawComponent(), group)) // nobody draws this group's rows -> there is no grid part
            return;
        if (level != GridDirty.ENTRIES)
            dirtyLists.put(group, Boolean.TRUE);
        if (level == GridDirty.ORDER)
            dirtyOrder.put(group, Boolean.TRUE);
    }

    // a PANEL property is its own part: it is placed on its own, and what happens to it - a new value, a new current
    // object, an appearance or a disappearance - concerns nothing else on the node. Its scope is asked of the group
    // for now, since fillSingles still puts every panel draw of a group on that group's node.
    private boolean markPanelDirty(GPropertyDraw draw) {
        GGroupObject group = draw.groupObject;
        boolean marked = false;
        for (GContainer scope : getGroupScopes(group)) {
            dirtyParts.put(partKey(draw, group), Boolean.TRUE);
            markNodeDirty(scope, group);
            marked = true;
        }
        return marked;
    }

    // the one CROSS-part edge the base projection has: the current object is the group's, and it is read by parts that
    // do not otherwise know about each other. The grid's rows flip `isCurrent`; EVERY panel entry of the group is read
    // at the new key, so its value, and its very existence, change with it; and the index of what the node carries
    // (`properties`) follows those appearances, which is why the grid part - where that index is built - is dirtied too.
    private void markCurrentDirty(GGroupObject group) {
        markGridDirty(group, GridDirty.ROWS);
        for (GPropertyDraw draw : form.propertyDraws)
            if (draw.groupObject == group && !draw.isList)
                markPanelDirty(draw);
    }

    // a PART must be rebuilt: the component that produces it drew something else. Its node has to be reassembled too -
    // the node is where its fields are read - and the scope's top object rebuilt so the change reaches React.
    private boolean markPartDirty(GComponent producer, GGroupObject group) {
        GContainer scope = partScope(producer);
        if (scope == null) // nothing projects it, so there is no part and nothing to rebuild
            return false;
        dirtyParts.put(partKey(producer, group), Boolean.TRUE);
        markNodeDirty(scope, group);
        return true;
    }

    private void markNodeDirty(GContainer scope, GGroupObject group) {
        dirtyNodes.put(nodeKey(scope, group), Boolean.TRUE);
        markScopeDirty(scope);
    }

    // the identities the two caches are keyed by. A component's design ID is unique across the whole form, kinds
    // included - one IDGenerator (FormEntity.genID) numbers the containers, the grids and the property draws alike -
    // which is the same invariant GForm.findComponentByID already rests on
    private static String partKey(GComponent producer, GGroupObject group) {
        return producer.ID + ":" + group.getSID();
    }
    private static String nodeKey(GContainer scope, GGroupObject group) {
        return scope.ID + ":" + group.getSID();
    }

    // rebuild the list AND forbid reusing any cached row: the change altered the SHAPE of every projected row (a
    // property's entry appeared or disappeared in each of them), which per-row dirty keys cannot express. Deliberately
    // NOT a GridDirty level: ORDER still reuses surviving rows, while this preserves order but rebuilds them all.
    private void invalidateRows(GGroupObject group) {
        markGridDirty(group, GridDirty.ROWS);
        lastRows.remove(group);
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
                    markGridDirty(draw.groupObject, GridDirty.ENTRIES);
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
        // a per-row attribute -> the list + each changed row; a group-scoped one (options) -> the node only
        boolean perRow = reader.getAttributeScope() == GGroupAttributeScope.ROW;
        markGridDirty(group, perRow ? GridDirty.ROWS : GridDirty.ENTRIES);
        if (perRow)
            keyValues.foreachEntry((k, v) -> markRowDirty(group, k));
    }

    // the key a VALUE is stored and read under: the same one the ROW it sits on is keyed by, or the two never meet -
    // so it is the group's own row-key rule that is asked (GGroupObject.getRowKey), not a rule restated here. Keying a
    // tree's value by one group's objects instead would store every parent's value in one slot (last write wins for a
    // property over two of the tree's groups) and would mark a key no row is ever found by, so a changed cell would
    // keep its old object and never re-project.
    private GGroupObjectValue getValueKey(GPropertyDraw draw, GGroupObjectValue key) {
        GGroupObject group = draw.groupObject;
        if (group == null || !draw.isList) // a grouped-in-columns draw never gets here: a projected group draws none
            return key;
        GGroupObjectValue rowKey = group.getRowKey(key);
        // ... and where the key holds no row of this group at all, it is left as it is: every caller narrows by this
        // same rule before it gets here (an already-narrow key turns into itself), so the fallback is for a key that
        // is not a cell of this draw in the first place, and inventing a row key for it would be worse than storing
        // it where nothing reads it
        return rowKey != null ? rowKey : key;
    }

    public JavaScriptObject build(GContainer scope) {
        JavaScriptObject cached = lastData.get(scope);
        if (dirtyScopes.get(scope) == null && cached != null) // this scope did not change -> same top ref (the whole tree memo-skips)
            return cached;
        JavaScriptObject data = newObject();
        for (GGroupObject group : form.groupObjects) {
            if (!getGroupScopes(group).contains(scope))
                continue;
            // the node is this SCOPE's, so it is cached and rebuilt as this scope's: a pass asks for it once, and
            // nothing has to be taken out of the dirty set as it goes. What is shared between scopes is one level
            // down - the PARTS, which are materialized at most once per pass (getGridPart) - so a part that did not
            // change hands every scope the same object and the structural sharing their React.memo depends on holds.
            String key = nodeKey(scope, group);
            JavaScriptObject node = lastNodes.get(key);
            if (node == null || dirtyNodes.get(key) != null) {
                node = buildGroupEntry(group);
                lastNodes.put(key, node);
            }
            setField(data, group.getSID(), node);
        }
        fillFormSingles(data, scope); // the form-level properties, each on the top object of the scope it sits in
        fillContainers(data, scope);
        lastData.put(scope, data);
        return data;
    }

    // ===== containers ==========================================================================================

    // what has a DESCRIPTOR entry of its own: a container the author DECLARED (DESIGN's `NEW <name>` - not "has a
    // name", a group's generated BOX(g) is named too, for icons), and every `lsf` component whatever kind it is - the
    // platform draws it and React only labels the boundary it places, which is the whole of what crosses that seam.
    // The lsf half is not a convenience: GWT skips an lsf component's caption/image for React to draw (the complement
    // invariant), so a generated box marked lsf - MOVE BOX(o) { lsf = TRUE; }, the canonical case - MUST project its
    // descriptor or its caption is drawn by nobody. What stays out is what nobody wrote AND nobody placed: a group's
    // toolbar/filter machinery. And not an `lsf` LIST draw: React draws its group, so its descriptor IS its column
    // entry on the group's node, where the view reads it beside the columns it draws itself.
    private boolean isProjectedContainer(GComponent component) {
        if (component == null) // the delta path asks about a reader that names no component at all
            return false;
        if (component.isLsfView()) {
            if (!(component instanceof GPropertyDraw))
                return true;
            GPropertyDraw draw = (GPropertyDraw) component;
            // ... and a PROPERTY's descriptor comes and goes with the property, exactly as its value would: a draw the
            // form is not showing (SHOWIF, a structure drop) has no entry, or the view would go on drawing a caption
            // for a renderer that is not there - and a name the projection carries would resolve to a draw it does not
            return !draw.isList && isShownProperty(draw, GGroupObjectValue.EMPTY);
        }
        return component instanceof GContainer && ((GContainer) component).declared;
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
        if (getProjectedContainerScope(component) == scope)
            setField(data, component.sID, buildDescriptorEntry(component, GGroupObjectValue.EMPTY));
        if (component instanceof GContainer) // only a container has children; the recursion stays container-only
            for (GComponent child : ((GContainer) component).children)
                fillContainers(data, scope, child);
    }

    // the scope whose data carries this container's entry, or null when it has none - THE statement of the container
    // rule, asked by the build (fillContainers) and by the delta path (getContainerReaderScope) alike, so the two cannot
    // drift. The server asks its own copy of the same question under the same name (FormView.getProjectedContainerScope)
    // to reserve the names this emits. An LSF container is placed by the scope it sits in (getTopLevelScope).
    private GContainer getProjectedContainerScope(GComponent component) {
        return isProjectedContainer(component) ? descriptorScope(component) : null;
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
        return descriptorScope(component);
    }

    // WHERE A COMPONENT'S PART GOES - only React draws parts, so this is the one question, and its null for an
    // `lsf` child is the ANSWER, not a gap: the platform draws that component, so nothing is produced for it here.
    // Asked lazily, never at construction: getOwningReactContainer answers null for everything until GFormController
    // has assigned its reactData, which happens after this object's constructor returns.
    public GContainer partScope(GComponent component) {
        return formController.getOwningReactContainer(component);
    }

    // ... and WHERE ITS DESCRIPTOR GOES - the container that must label the boundary it places. For an `lsf` child
    // that is the container it sits in (the platform draws it, React only frames it); for anything React draws it is
    // the container that draws it. The two answers are deliberately different functions: the previous version of this
    // layer used this one for both and thereby sent an lsf component's DATA to the container that merely frames it.
    public GContainer descriptorScope(GComponent component) {
        return component.isLsfView() ? component.container : partScope(component);
    }

    public boolean isLsfViewDescriptorReader(GPropertyReader reader) {
        return getLsfViewDescriptorOwner(reader) != null;
    }

    // build a group's node, reusing the unchanged list array and unchanged row objects
    // the node a group has in `data` is ASSEMBLED out of the parts its base components produce - the grid produces the
    // rows and the columns, the panel produces the single values - rather than written by one builder that knows about
    // all of them. Each of them is drawn in one place, so what it produces is that place's, and the assembler is the
    // only thing that knows they meet on one node.
    // Copying is BY REFERENCE, so a part that did not change hands back the very objects it handed back last time.
    // `__groupSID` is stamped HERE and never copied: it is non-enumerable (a copy loop drops it in silence) and
    // non-configurable (re-stamping an object that already has it throws).
    private JavaScriptObject buildGroupEntry(GGroupObject group) {
        JavaScriptObject node = newObject();
        GGroupObjectValue current = currentObjects.get(group);

        copyFields(node, getGridPart(group, current));
        if (current != null) // the panel draws the current object, and without one it draws nothing
            copyFields(node, buildPanelPart(group, current));

        setGroupSID(node, group.getSID());
        return node;
    }

    // the grid's part, cached under its producer and rebuilt only when that producer's own dirty flag says so - and at
    // most once per pass, so every scope asking for it in one pass is handed the same object
    private JavaScriptObject getGridPart(GGroupObject group, GGroupObjectValue current) {
        String key = partKey(group.getDrawComponent(), group);
        JavaScriptObject part = lastParts.get(key);
        if (part == null || (dirtyParts.get(key) != null && builtParts.get(key) == null)) {
            part = buildGridPart(group, current);
            lastParts.put(key, part);
            builtParts.put(key, Boolean.TRUE);
        }
        return part;
    }

    // what the GRID produces: the rows, everything that is the same down a column, and the group's own attributes -
    // the group SID apart, which is the assembler's. Read: what the view sees of a group whose rows React draws.
    private JavaScriptObject buildGridPart(GGroupObject group, GGroupObjectValue current) {
        JavaScriptObject node = newObject();

        ArrayList<GGroupObjectValue> rows = gridRows.get(group);
        JavaScriptObject list = lastLists.get(group);
        JavaScriptObject byKey = lastByKey.get(group);
        if (rows != null) {
            if (list == null || dirtyLists.get(group) != null) { // rebuild the list only if its rows/order/values changed
                NativeHashMap<GGroupObjectValue, JavaScriptObject> prevRows = lastRows.get(group);
                // every dirty key is row-shaped by construction: a projected group draws no grouped-in-columns
                // property, so an unchanged row is ALWAYS safe to reuse by its key
                NativeHashMap<GGroupObjectValue, Boolean> dirtyKeys = dirtyRowKeys.get(group);
                NativeHashMap<GGroupObjectValue, JavaScriptObject> newRows = new NativeHashMap<>();
                // canonical key string -> row, rebuilt WITH the list (row refs shared with it): selectors subscribe
                // by STABLE key (s.i.byKey[row.key] — property lookup coerces a numeric key to the same string) so
                // surviving rows after a delete keep their selected identity; cached like the list, so a node-only
                // change (panel prop) keeps byKey identity too
                // keyed by DATA, not by a name this surface owns: a STRING-valued object key can be any string at
                // all, "__proto__" included, and `obj["__proto__"] = row` would replace the map's prototype instead
                // of indexing the row. No prototype, no setter - and no inherited answer either: `byKey["constructor"]`
                // used to hand back Object's own, and now says what it means, which is that there is no such row.
                // Reading an ORDINARY row is unchanged (byKey[key] / `in` / Object.keys / spreading / JSON all say
                // what they said); a row whose key is `__proto__` is now among them, which is the point. The methods
                // that came with the prototype are gone, byKey.hasOwnProperty included
                byKey = newBareObject();
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
                    GGroupObjectValue.registerRow(row, rowKey); // the public row.key + the `objects` handle beside it
                    setField(byKey, rowKey.toKeyString(), row);
                    newRows.put(rowKey, row);
                    push(list, row);
                }
                lastRows.put(group, newRows);
                lastLists.put(group, list);
                lastByKey.put(group, byKey);
            }
        } else if (list == null) { // never had rows: materialize stable empty defaults once (list/byKey are always cached together)
            list = newArray();
            byKey = newBareObject(); // the same shape as the filled one, empty
            lastLists.put(group, list);
            lastByKey.put(group, byKey);
        }
        setField(node, "list", list);
        setField(node, "byKey", byKey);
        // a referentially-STABLE keys array (rebuilt only on membership/order) + a non-enumerable group SID:
        // the <List> row-subscription path maps these keys and each row subscribes by byKey[key], so a value/current
        // change re-renders only the changed row (the keys array ref is unchanged -> the outer map is skipped).
        JavaScriptObject keys = lastKeys.get(group);
        if (keys == null || dirtyOrder.get(group) != null) {
            keys = newArray();
            if (rows != null) {
                for (GGroupObjectValue rowKey : rows)
                    push(keys, rowKey.toKeyString());
            }
            lastKeys.put(group, keys);
        }
        setField(node, "keys", keys);
        // the group's own attributes, DIRECT on the node: the names it draws, and its
        // GROUP-scoped readers (options, at EMPTY). No meta wrapper - they sit beside `list` and the column property
        // objects, rebuilt with the node.
        fillGroupAttributes(node, group); // what the GROUP itself is
        // and what is the same down each COLUMN. Reading node.<prop> for the caption and row.<prop>.value for the value is
        // the column/cell split without a merge - each attribute lives at exactly one point. Rebuilt with the node (a
        // column reader marks only the node dirty), so it doesn't churn row refs.
        fillColumns(node, group);
        return node;
    }

    // what the PANEL produces: one entry per property it shows, for the group's current object. A group with no current
    // object has no panel values at all - not empty entries, none - so the part is asked for only when there is one.
    // Each entry is a part of its own, cached under the draw that produces it: whether it EXISTS is asked here, every
    // pass, and only what it holds is cached - so an entry that comes and goes is never revived from the cache.
    private JavaScriptObject buildPanelPart(GGroupObject group, GGroupObjectValue current) {
        JavaScriptObject part = newObject();
        for (GPropertyDraw draw : form.propertyDraws) {
            if (draw.groupObject != group)
                continue;
            GGroupObjectValue valueKey = getSingleEntryKey(draw, current);
            if (valueKey != null)
                setField(part, draw.integrationSID, getPanelEntry(draw, group, valueKey));
        }
        return part;
    }

    private JavaScriptObject getPanelEntry(GPropertyDraw draw, GGroupObject group, GGroupObjectValue valueKey) {
        String key = partKey(draw, group);
        JavaScriptObject entry = lastParts.get(key);
        if (entry == null || (dirtyParts.get(key) != null && builtParts.get(key) == null)) {
            entry = buildSingleEntry(draw, valueKey);
            lastParts.put(key, entry);
            builtParts.put(key, Boolean.TRUE);
        }
        return entry;
    }

    // the group's own PER-ROW attributes (background / foreground / selected), DIRECT on the row beside `isCurrent` - each
    // reader self-declares its field + converter (COLOR / FLAG). No meta wrapper; the field names are reserved so a
    // property cannot take them (checkReactProjectionNames).
    private void fillRowAttributes(JavaScriptObject row, GGroupObject group, GGroupObjectValue rowKey, GGroupObjectValue current) {
        setField(row, "isCurrent", current != null && rowKey.equals(current)); // declarative current-row marker
        for (GGroupObjectPropertyReader reader : group.getPresentationReaders())
            if (reader != null && reader.getAttributeScope() == GGroupAttributeScope.ROW)
                emitAttribute(row, reader, rowKey, null);
    }

    // the GROUP's own attributes, direct on its node beside list/byKey/keys: the names of what it draws, and its
    // group-scoped readers (options, read once at EMPTY). The mirror of fillRowAttributes, one level up.
    private void fillGroupAttributes(JavaScriptObject node, GGroupObject group) {
        setField(node, "properties", buildProperties(group));
        for (GGroupObjectPropertyReader reader : group.getPresentationReaders())
            if (reader != null && reader.getAttributeScope() == GGroupAttributeScope.GROUP)
                emitAttribute(node, reader, GGroupObjectValue.EMPTY, null);
    }

    // the names of what the group draws, in the form's own order - the one thing a property's own entry cannot say,
    // because `data.<group>` is a flat namespace and nothing in it marks which keys are properties. Everything ABOUT a
    // property (its type, what it can be compared with) is IN its entry, beside its caption: one place, and it exists
    // exactly when the entry does. This is only the index into them.
    private JavaScriptObject buildProperties(GGroupObject group) {
        ArrayList<String> names = getEntryNames(group); // the same three questions the entries are written by
        // the node is rebuilt for anything that changes on it - one caption, one option - and this list changes for
        // almost none of that. Handing back a new array each time would re-render every component that selects it,
        // which is the one thing structural sharing is for, so it is rebuilt only when it says something else
        JavaScriptObject cached = lastProperties.get(group);
        if (cached != null && names.equals(lastPropertyNames.get(group)))
            return cached;
        JavaScriptObject array = newArray();
        for (String name : names)
            push(array, name);
        lastPropertyNames.put(group, names);
        lastProperties.put(group, array);
        return array;
    }

    // the names this group's node carries RIGHT NOW, by the SAME two predicates the entries are built by - fillColumns
    // for a list draw, fillSingles for a panel one, which is emitted for the CURRENT object and not at all without
    // one. Asking either of them at another key would list a property that has no entry, or hide one that has.
    // Also what the CONTROLLER carries a member for: a change is the form's own ON CHANGE event, so a property that
    // is not shown has nobody to call it on
    // ... and what has a VALUE here, which is a different question and the one a CONTROLLER MEMBER answers to.
    // An `lsf` draw's entry is a DESCRIPTOR - the platform draws its value in its own renderer, the projection writes
    // no cell for it (hasCellEntry) and does not even take its delta (update) - so a member for it would be a second
    // channel to that edit, going around the interface the platform already gives React (<Lsf name row/>). The
    // question is asked of PLACEMENT, never of a cache: a value is here when the part that produces it is here.
    private boolean hasValueEntry(GPropertyDraw draw, GGroupObjectValue current, GContainer scope) {
        if (draw.isLsfView()) // the platform draws it
            return false;
        if (draw.isList) // a cell exists where the grid's part is
            return hasColumnEntry(draw) && partScope(formController.getGroupDrawComponent(draw.groupObject)) == scope;
        return getSingleEntryKey(draw, current) != null;
    }

    // the names this container may CHANGE, as opposed to the names its node carries: the index keeps an lsf column,
    // because the view draws that column's header over renderers it places, and the member set does not
    public ArrayList<String> getValueNames(GGroupObject group, GContainer scope) {
        ArrayList<String> names = new ArrayList<>();
        GGroupObjectValue current = currentObjects.get(group);
        for (GPropertyDraw draw : form.propertyDraws)
            if (draw.groupObject == group && hasValueEntry(draw, current, scope))
                names.add(draw.integrationSID);
        return names;
    }

    public ArrayList<String> getEntryNames(GGroupObject group) {
        ArrayList<String> names = new ArrayList<>();
        GGroupObjectValue current = currentObjects.get(group);
        for (GPropertyDraw draw : form.propertyDraws)
            if (draw.groupObject == group && (hasColumnEntry(draw) || getSingleEntryKey(draw, current) != null))
                names.add(draw.integrationSID);
        return names;
    }

    // WHAT HAS AN ENTRY HERE, said once. The list that names them and the writes that fill them ask the same three
    // questions, or the index would name what is not there - or hide what is - and the controller, which builds its
    // members from that same list, would carry a member for a cell nobody wrote.
    // A LIST draw is a column on the node, for the group's rows as a whole
    private boolean hasColumnEntry(GPropertyDraw draw) {
        return draw.isList && isShownProperty(draw, GGroupObjectValue.EMPTY);
    }
    // ... and a cell in each row, unless the platform draws it: an LSF list property has its column and no cell
    private boolean hasCellEntry(GPropertyDraw draw, GGroupObjectValue rowKey) {
        return draw.isList && !draw.isLsfView() && isShownProperty(draw, rowKey);
    }
    // a PANEL draw is one value, for the key it is asked at - the group's current object, or EMPTY at the form level -
    // and none at all without one. The key it is written under is the answer, so asking and writing say one thing
    private GGroupObjectValue getSingleEntryKey(GPropertyDraw draw, GGroupObjectValue key) {
        if (draw.isList || key == null)
            return null;
        if (draw.isLsfView()) // its descriptor is a TOP-LEVEL entry: the platform draws such a property, and its group
            return null;      // is one the platform draws too (checkLsfView refuses it on a react-drawn group), so
                              // the group has no node here to hang it on
        GGroupObjectValue valueKey = draw.filterColumnKeys(key);
        return valueKey != null && isShownProperty(draw, key) ? valueKey : null;
    }

    // ... and the same question for a form-level draw, which has no group node: it is carried by the scope it sits
    // in, at the top object, and only while it is shown - the predicate fillSingles emits it by
    public boolean isShownFormProperty(GPropertyDraw draw, GContainer scope) {
        if (draw.groupObject != null || scope == null || getTopLevelScope(draw) != scope)
            return false;
        // an `lsf` one is SHOWN here too - its entry is the descriptor the platform's own renderer is labelled by -
        // and it must go on being found, or a bare `controller.qty.change(v)` would stop resolving to it, fall through
        // to a group that happens to draw the same integration SID, and change a cell nobody named. It has no MEMBER:
        // that exclusion belongs to the member set (getControllerStructure), not here
        if (draw.isLsfView())
            return isShownProperty(draw, GGroupObjectValue.EMPTY);
        return getSingleEntryKey(draw, GGroupObjectValue.EMPTY) != null; // the key fillFormSingles writes it under
    }
    // ... and the draw a BARE name means on the controller: the form-level one this projection is showing, which is
    // the one that has the member. Asked of the projection and not of the form, or the name would answer with a draw
    // that has no member - an unprojected form-level property, or one the form is not showing right now
    public GPropertyDraw getShownFormProperty(String integrationSID, GContainer scope) {
        for (GPropertyDraw draw : form.propertyDraws)
            if (integrationSID.equals(draw.integrationSID) && isShownFormProperty(draw, scope))
                return draw;
        return null;
    }
    // what a property IS, written into its own entry beside its caption - the entry is where everything about a
    // property already lives, so this is one more thing it says rather than a second place to look.
    // `type` names the kind the projected value was converted by - a number, a boolean, a string, a date, or JSON
    // that has been parsed into whatever it held - so a view can tell them apart without knowing the property's class.
    private void emitPropertyFacts(JavaScriptObject entry, GPropertyDraw draw) {
        setField(entry, "type", GSimpleStateTableView.getJSTypeName(draw.getRenderType(RendererType.SIMPLE)));
    }

    // a thing on a property the projection cannot name (no integration SID) leaves the list that states it describing
    // less than the group really has. Report it - once per property per group, since the node rebuilds on every change
    // - rather than let a short list read as "there is nothing here". Here with no caller for the same reason the
    // state reader in GFormController is: the branches that add `filters` and `orders` both report through it
    private void reportUnnameable(GGroupObject group, GPropertyDraw property, String node, String what) {
        String name = property != null ? property.sID : "?";
        NativeHashMap<String, Boolean> reported = unnameable.get(group);
        if (reported == null) {
            reported = new NativeHashMap<>();
            unnameable.put(group, reported);
        }
        String key = node + ":" + name;
        if (reported.get(key) == null) {
            reported.put(key, Boolean.TRUE);
            GwtClientUtils.consoleError("data." + group.getSID() + "." + node + ": the group is " + what + " by '" + name
                    + "', which the projection does not name (no integration SID), so that one is not listed");
        }
    }

    public void clearDirty() {
        dirtyParts.clear();
        dirtyNodes.clear();
        builtParts.clear();
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
            if (draw.groupObject == group && hasCellEntry(draw, rowKey))
                setField(row, draw.integrationSID, buildCellEntry(draw, rowKey));
    }

    // the FORM-LEVEL properties, on the scope's top object: no group, so one value each, read at the empty key, and
    // each belongs to the scope it sits in. The counterpart of buildPanelPart one level up - and like it, EXISTENCE is
    // asked here, every pass, so a property that stops being shown stops having an entry.
    private void fillFormSingles(JavaScriptObject data, GContainer scope) {
        for (GPropertyDraw draw : form.propertyDraws) {
            if (draw.groupObject != null || getTopLevelScope(draw) != scope)
                continue;
            GGroupObjectValue valueKey = getSingleEntryKey(draw, GGroupObjectValue.EMPTY);
            if (valueKey != null)
                setField(data, draw.integrationSID, buildSingleEntry(draw, valueKey));
        }
    }

    // the COLUMNS of a group: every list property contributes what is the same down its whole column, once, on the node
    private void fillColumns(JavaScriptObject node, GGroupObject group) {
        for (GPropertyDraw draw : form.propertyDraws)
            if (draw.groupObject == group && hasColumnEntry(draw))
                setField(node, draw.integrationSID, buildColumnEntry(draw));
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
        JavaScriptObject entry;
        if (draw.isLsfView())
            entry = buildDescriptorEntry(draw, GGroupObjectValue.EMPTY);
        else {
            entry = newObject();
            for (GPropertyReader reader : draw.getPresentationReaders())
                if (reader != null && reader.isColumnAttribute(draw))
                    emitAttribute(entry, reader, GGroupObjectValue.EMPTY, draw);
        }
        emitPropertyFacts(entry, draw); // an LSF column is filtered and sorted like any other: the platform draws
        return entry;                   // its VALUE, which says nothing about what the value is
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
        JavaScriptObject entry;
        if (draw.isLsfView())
            entry = buildDescriptorEntry(draw, key); // its own key, like any single entry - not EMPTY, which is a column's key
        else {
            entry = newObject();
            emitValue(entry, draw, key);
            for (GPropertyReader reader : draw.getPresentationReaders())
                if (reader != null)
                    emitAttribute(entry, reader, key, draw);
        }
        emitPropertyFacts(entry, draw);
        return entry;
    }

    // the value field of an entry, always present for a react-owned property (null value included, so the entry exists)
    // getValueKey, because a value is stored under the key its ROW is keyed by, and the server delivers it under a key
    // that can carry more than that (a column key, a tree path beyond this group)
    private void emitValue(JavaScriptObject entry, GPropertyDraw draw, GGroupObjectValue key) {
        setField(entry, "value", GSimpleStateTableView.convertToJSValue(draw, readerValue(draw, getValueKey(draw, key)), RendererType.SIMPLE, true));
    }

    // the ONE attribute emitter, shared by every entry (a property's, a container's, a group's, a row's): the EFFECTIVE
    // value of one attribute - the dynamic reader value if delivered (converted by the reader's own converter), else the
    // static design default - written under the field name the reader declares. Absent attributes are simply not written.
    // GWT represents java.lang.Boolean as a native JS boolean, so setField stores a String / boolean / JS object all as
    // their primitive JS form (a Boolean lands as a real true/false, not a truthy wrapper) — one path fits all.
    private void emitAttribute(JavaScriptObject entry, GPropertyReader reader, GGroupObjectValue key, GComponent owner) {
        PValue pvalue = readerValue(reader, key);
        GPropertyDraw draw = owner instanceof GPropertyDraw ? (GPropertyDraw) owner : null; // the converter wants the draw; a container / a group's own reader has none
        Object dynamic = pvalue != null ? reader.getAttributeConverter().convert(pvalue, draw) : null; // getAttributeConverter only reached when reader delivered -> reader != null
        Object value = isPresent(dynamic) ? dynamic : (owner != null ? reader.getStaticAttribute(owner) : null); // dynamic wins; else the static design default (also when a delivered image was cleared)
        String field = reader.getAttributeField(pvalue); // the no-value case is the reader's own default
        if (field != null && isPresent(value))
            setField(entry, field, value);
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


    private PValue readerValue(GPropertyReader reader, GGroupObjectValue key) {
        if (reader == null || key == null)
            return null;
        NativeHashMap<GGroupObjectValue, PValue> store = values.get(reader);
        return store == null ? null : store.get(key);
    }


    // the react scope a group's node appears in: the container that DRAWS the group, and only it. A react view
    // gets a group's data because it draws that group, never because it happens to stand next to it - a container
    // beside a platform-drawn grid would otherwise be handed rows GWT is already rendering, two engines on one
    // value. A list because the callers ask for one and because a component may serve several groups (a TREE);
    // static (the design's react containers do not come and go), so it is computed once.
    private ArrayList<GContainer> getGroupScopes(GGroupObject group) {
        ArrayList<GContainer> scopes = groupScopes.get(group);
        if (scopes == null) {
            scopes = new ArrayList<>();
            GContainer owner = partScope(formController.getGroupDrawComponent(group));
            if (owner != null)
                scopes.add(owner); // it draws the group, and any react container below it is swallowed, not a scope
            groupScopes.put(group, scopes);
        }
        return scopes;
    }

    // whether any react view sees this group at all - the one gate every accumulator mutator asks, so a group nobody
    // projects costs nothing and a group somebody projects is kept up to date whoever draws its rows
    public boolean isProjectedGroup(GGroupObject group) {
        return group != null && !getGroupScopes(group).isEmpty();
    }

    // ... and whether THIS scope is the one that sees it: `data` is per react container, and so is the
    // controller beside it - a view names what its own data carries
    public boolean isProjectedGroup(GGroupObject group, GContainer scope) {
        return group != null && getGroupScopes(group).contains(scope);
    }


    private void markScopeDirty(GContainer scope) {
        if (scope != null)
            dirtyScopes.put(scope, Boolean.TRUE);
    }

    // by REFERENCE, and own enumerable keys only: a part's objects ARE the node's objects, so an unchanged part keeps
    // every identity a React.memo downstream compares. Non-enumerable fields (__groupSID) are deliberately not copied.
    private static native void copyFields(JavaScriptObject target, JavaScriptObject source) /*-{
        for (var key in source)
            if (Object.prototype.hasOwnProperty.call(source, key))
                target[key] = source[key];
    }-*/;

    private static native void setGroupSID(JavaScriptObject obj, String sid) /*-{ Object.defineProperty(obj, "__groupSID", { value: sid }); }-*/; // non-enumerable: stable selector path, not user-visible data
}
