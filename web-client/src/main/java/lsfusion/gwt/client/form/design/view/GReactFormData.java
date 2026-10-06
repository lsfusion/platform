package lsfusion.gwt.client.form.design.view;

import com.google.gwt.core.client.JavaScriptObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import lsfusion.gwt.client.base.GwtClientUtils;
import static lsfusion.gwt.client.base.GwtClientUtils.*;
import lsfusion.gwt.client.GForm;
import lsfusion.gwt.client.GFormChanges;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.jsni.NativeSIDMap;
import lsfusion.gwt.client.form.controller.GFormController;
import java.util.function.Consumer;
import java.util.function.Function;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.filter.user.GPropertyFilterDTO;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GAbstractTableController;
import lsfusion.gwt.client.form.object.table.controller.GComponentController;
import lsfusion.gwt.client.form.object.table.controller.GGroupController;
import lsfusion.gwt.client.form.object.table.controller.GLsfPropertyController;
import lsfusion.gwt.client.form.object.table.controller.GPropertyController;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.GComponentLabelReader;
import lsfusion.gwt.client.form.property.GComponentReader;
import lsfusion.gwt.client.form.property.GExtraLabelReader;
import lsfusion.gwt.client.form.property.GExtraPropertyReader;
import lsfusion.gwt.client.form.property.GPropertyReader;
import lsfusion.gwt.client.form.property.GGroupObjectPropertyReader;
import lsfusion.gwt.client.form.property.GGroupAttributeScope;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.view.RendererType;
import lsfusion.gwt.client.form.view.Column;
import lsfusion.gwt.client.form.object.table.grid.view.GSimpleStateTableView;

// The projection of a form's react containers. Each container owns its state (ContainerState): its nodes and its
// published snapshot - what its view is given - and its controller, whose members mirror that snapshot's
// entries - what the view changes them through. What React has of each group, property and component - its rows node,
// its entry, its descriptor - is the form's controller of it (GFormController.initializeOwnerControllers): made here
// once, from the platform's controller of it, and handed all the form sends for it, which it keeps itself: the values
// of the readers it draws from (ReaderValues), its rows, whether the form shows it. The projection keeps the
// containers, and one flush for all of them.
// Placement is design data, and so is the SHAPE of every snapshot: an entry a container carries is there from the
// start, and whether the form shows it now is its `hidden`, never its presence. A part of a group is projected where
// its component is - the rows where the grid (or the tree) is, a panel property where the property is. The form level
// is the empty group: its node is each container's top level. Every other component React draws or places has a
// descriptor - its entry at that top level, keyed by its SID, where a property carried by name is keyed by that name -
// an lsf one included, which the platform draws and React places; not an lsf ACTION, whose caption and image are its
// buttons' face, which the platform draws.
// What an entry says is what the server sent, as the platform keeps it for its own components: nothing here decides
// that something is hidden or empties it. A property the form does not show is dropped by the server, and its entry has
// no value; a component the form does not show has its SHOWIF, and its descriptor says so - the rows of a hidden grid
// stay as the server last sent them, as the platform's own grid keeps them. Like any attribute, `hidden` is there only
// while it has a value: while the form does not show the entry's component.
public class GReactFormData {
    private final GForm form;
    // GComponent.getReactPlace - handed in, so a test sees who asks it
    private final Function<GComponent, GContainer> place;
    private final Verbs verbs; // what a call through a member does: the form's own edit (GFormController)
    private final NativeSIDMap<GContainer, ContainerState> containers = new NativeSIDMap<>();
    private NativeSIDMap<GContainer, ContainerState> pending = new NativeSIDMap<>();

    public GReactFormData(GForm form, Function<GComponent, GContainer> place, Verbs verbs) {
        this.form = form;
        this.place = place;
        this.verbs = verbs;
    }

    // WHAT A MEMBER DOES: the form's own edit, the one a user makes. The members are made here, beside the entries they
    // mirror, and each holds the state it stands for (ReactPropertyEntry, RowsGroupNode; the batch, ContainerState): a
    // call through one reads what it names - the row, the value - from that state, and hands the form the edit
    public interface Verbs {
        // the current object of a group whose rows a view draws: one of those rows
        void changeCurrentObject(GGroupObject group, GGroupObjectValue key);
        // the properties' cells, changed in one request: the key of each (EMPTY: the current objects) and its value
        // (PValue.UNDEFINED: its change event, or the action)
        void changeProperties(GPropertyDraw[] properties, GGroupObjectValue[] keys, PValue[] values);
        // the suggestion list of a property's cell, from the server's lookup `actionSID`
        void getPropertyValues(GPropertyDraw property, GGroupObjectValue key, String value, String actionSID, JavaScriptObject successCallback, JavaScriptObject failureCallback, int increaseValuesNeededCount);
        // a node of a tree whose rows a view draws, opened: one of those rows
        void expandNode(GGroupObject group, GGroupObjectValue key);
        // ... or closed
        void collapseNode(GGroupObject group, GGroupObjectValue key);
        // ... and every node of such a group and of the groups below it: opened
        void expandAll(GGroupObject group);
        // ... or closed
        void collapseAll(GGroupObject group);
        // the sortings of a group whose rows a view draws, as its member read them (RowsGroupNode.changeOrders): the
        // whole list, in priority order, true - ascending; the form shows them at once and sends them whole
        void changeOrders(GGroupObject group, LinkedHashMap<Column, Boolean> orders);
    }

    // a react container's state, made with its view, and its controller with it
    public ContainerState addContainer(GContainer scope, Consumer<JavaScriptObject> publish) {
        ContainerState state = new ContainerState(scope, publish);
        containers.put(scope, state);
        pending.put(scope, state); // the first server delta also publishes empty / static-only containers
        return state;
    }

    // ===== THE FORM'S CONTROLLER OF EACH OWNER, made once, when every react container is there, from the platform's
    // controller of it (GFormController.initializeOwnerControllers): placement is design data. The groups first, the
    // nodes the rows are drawn on - a list property's column is on one - then the properties, in the form's order, the
    // order of the names a node carries, then the components
    // ... of a group: the node its rows are drawn on, where React draws them; else the platform's
    public GGroupController createGroupController(GGroupObject group, GAbstractTableController lsf) {
        GContainer rows = rowsScope(group);
        return rows != null ? containers.get(rows).createRowsGroupNode(group) : lsf;
    }
    // ... of a property, ALL of it - its values and what labels it alike: an entry, keyed by its integration name on
    // the node of its group, where React draws it - none where it carries it by no name, the platform's own COUNT of a
    // list group, the pivot's: React keeps nothing of it, and nothing is sent for it; an entry over the platform's,
    // keyed the same way, where the platform draws it and React labels it - a panel property React places, the column
    // of the rows React draws -, which keeps what labels the property and hands the rest on; else the platform's
    public GPropertyController createPropertyController(GPropertyDraw property, GLsfPropertyController lsf) {
        GContainer scope = nameScope(property);
        if (scope == null)
            return lsf;
        if (!hasEntry(property)) // ... and where it has no entry, it brings no node either
            return noEntry(property, lsf);
        ContainerState state = containers.get(scope);
        if (property.isList) { // its content is its column, on the node its group's rows are drawn on
            RowsGroupNode rows = state.rowsGroupNodes.get(property.groupObject);
            return property.isLsfView() ? new LsfColumnPropertyEntry(rows, property, lsf) : new ReactColumnPropertyEntry(rows, property);
        }
        // a panel property is a part of its group: on the group's node here, the empty group's on the top level
        Node node = property.groupObject == null ? state.top : state.getOrCreateGroupNode(property.groupObject);
        return property.isLsfView() ? new LsfPanelPropertyEntry(node, property, lsf) : new ReactPanelPropertyEntry(node, property);
    }
    // whether a property a view names has an entry there: not an lsf ACTION - its caption and image are its buttons'
    // face, the platform's -, nor one React draws by no name, the group's own COUNT. Any other has a name, the form being
    // refused without one (FormView.checkReactProjectionNames). A group's node is where an entry of it is (mirrors
    // FormView.hasEntry)
    private static boolean hasEntry(GPropertyDraw property) {
        return property.isLsfView() ? !property.isAction() : property.integrationSID != null;
    }
    // ... and what it has instead: an lsf action is all the platform's, and React keeps nothing of the COUNT
    private static GPropertyController noEntry(GPropertyDraw property, GLsfPropertyController lsf) {
        return property.isLsfView() ? lsf : null;
    }
    // ... of a component: none of what React keeps nothing of (reactKeepsNothing). A property's is the platform's where
    // the platform draws the property - a property is no owner as a component, all of it being its property
    // controller's (above), and the one thing sent for it as a component, its class, is the platform's to apply, to the
    // view it draws the property in. Of any other: of one React draws, all that is sent for it; of an lsf one React
    // places, its descriptor - its labels and its SHOWIF - where the design names the component, over the
    // platform's; of one it places by no name, nothing: all of it is the platform's. `lsf` is the platform's controller
    // of the component: the layout
    public GComponentController createComponentController(GComponent component, GComponentController lsf) {
        if (reactKeepsNothing(component))
            return null;
        GContainer scope = component instanceof GPropertyDraw ? null : nameScope(component);
        if (scope == null)
            return lsf;
        ContainerState state = containers.get(scope);
        return component.isLsfView() ? new LsfComponentEntry(state, component, lsf) : new ReactComponentEntry(state, component);
    }
    // ... and WHAT REACT KEEPS NOTHING OF as a component, so nothing is sent for it as one (mirrors
    // FormView.reactKeepsNothing): a property whose content React draws - all of it is its property controller's -, and
    // a component React draws that the design does not name - no descriptor, no entry to go to
    private boolean reactKeepsNothing(GComponent component) {
        if (component instanceof GPropertyDraw)
            return contentScope(component) != null;
        return component.sID == null && !component.isLsfView() && placed(component) != null;
    }
    // THE ROWS A GROUP'S PER-ROW RENDERERS FOLLOW (GGridPanelController): a group's lsf list properties are drawn per
    // row only where React draws the group's rows (FormView.checkLsfListView), so what they follow is the node the rows
    // are drawn on
    public interface Rows {
        // its rows as the view has them, its own add or remove included - the node's own list, which it replaces and
        // never changes: a list handed out keeps saying what it said
        ArrayList<GGroupObjectValue> getRows();
    }
    // ... of a group whose rows React draws, handed out once, while the form is built: placement is asked here, and
    // then never again
    public Rows getRows(GGroupObject group) {
        return containers.get(rowsScope(group)).rowsGroupNodes.get(group);
    }
    // ... and then what the nodes carry of it all, once: a group's rows are there from the start, none of them yet; the
    // names every node carries; the members that mirror its entries
    public void initialize() {
        containers.foreachValue(ContainerState::initialize);
    }

    // THE VALUES OF AN OWNER'S READERS, as the server last sent them, with the client's own on top - kept by the owner
    // that draws from them: an entry its own, the node a group's rows are drawn on the group's
    private static final class ReaderValues {
        private final NativeSIDMap<GPropertyReader, NativeHashMap<GGroupObjectValue, PValue>> values = new NativeSIDMap<>();

        // a delivery of a reader is the server's WHOLE set of its values - only the client's own reconciliation marks
        // one partial, and it adds to what is kept - so it replaces what was kept; what was is handed back
        NativeHashMap<GGroupObjectValue, PValue> put(GPropertyReader reader, NativeHashMap<GGroupObjectValue, PValue> delivered, boolean partial) {
            NativeHashMap<GGroupObjectValue, PValue> previous = values.get(reader);
            NativeHashMap<GGroupObjectValue, PValue> kept = new NativeHashMap<>();
            if (partial && previous != null)
                kept.putAll(previous);
            kept.putAll(delivered);
            values.put(reader, kept);
            return previous;
        }
        // ... and every key whose value differs from what was kept, or that is gone, rebuilt
        void update(GPropertyReader reader, NativeHashMap<GGroupObjectValue, PValue> delivered, boolean partial, Consumer<GGroupObjectValue> changed) {
            NativeHashMap<GGroupObjectValue, PValue> previous = put(reader, delivered, partial);
            NativeHashMap<GGroupObjectValue, PValue> kept = values.get(reader);
            kept.foreachEntry((key, value) -> {
                if (previous == null || !GwtClientUtils.nullEquals(previous.get(key), value))
                    changed.accept(key);
            });
            if (previous != null)
                previous.foreachKey(key -> {
                    if (!kept.containsKey(key))
                        changed.accept(key);
                });
        }
        // one value, the client's own: kept - whether it differs from what was
        boolean put(GPropertyReader reader, GGroupObjectValue key, PValue value) {
            NativeHashMap<GGroupObjectValue, PValue> kept = values.get(reader);
            if (kept == null)
                values.put(reader, kept = new NativeHashMap<>());
            if (GwtClientUtils.nullEquals(kept.get(key), value))
                return false;
            kept.put(key, value);
            return true;
        }
        // a reader's value for a key: null for one none is kept of
        PValue get(GPropertyReader reader, GGroupObjectValue key) {
            NativeHashMap<GGroupObjectValue, PValue> kept = values.get(reader);
            return kept == null ? null : kept.get(key);
        }
        void remove(GPropertyReader reader) {
            values.remove(reader);
        }
    }

    // Commit every draft before notifying subscribers. Detach the batch first so edits made by a subscriber get their
    // own draft and remain pending for the next publication.
    public void flush() {
        NativeSIDMap<GContainer, ContainerState> batch = pending;
        pending = new NativeSIDMap<>();
        batch.foreachValue(ContainerState::commit);
        batch.foreachValue(state -> {
            if (state.data != state.lastPublished) { // a nested flush may have given it out already
                state.lastPublished = state.data;
                state.publish.accept(state.data);
            }
        });
    }

    // what React writes into a snapshot for a property or a component - its entry, under its name on its node: the
    // attributes it is built from, and whether the form shows what it stands for (isShown), which the entry's `hidden`
    // says. The entry is rebuilt whenever either changes. It is the form's controller of what it stands for: of a
    // property (PropertyEntry), of a component (ComponentEntry)
    private abstract class Entry {
        final Node node;
        final String name;
        final ReaderValues values = new ReaderValues(); // what the readers it is built from say

        Entry(Node node, String name) {
            this.node = node;
            this.name = name;
        }
        // the entry, rebuilt whole: what it says, and whether the form shows what it stands for (writeEntry)
        final void updateEntry() {
            JavaScriptObject entry = newObject();
            emit(entry);
            writeEntry(entry);
        }
        // what it says: the attributes it is built from
        abstract void emit(JavaScriptObject entry);
        // what it stands for - a component, or a property, which is a component too: whose static attributes a reader
        // falls back to
        abstract GComponent owner();
        // what these readers say of it, for a key
        void emitAttributes(JavaScriptObject entry, GPropertyReader[] readers, GGroupObjectValue key) {
            for (GPropertyReader reader : readers)
                emitAttribute(entry, reader, values.get(reader, key), owner());
        }
        // what a reader of what it is built from brings: kept, and the entry rebuilt
        final void keep(GPropertyReader reader, NativeHashMap<GGroupObjectValue, PValue> delivered, boolean partial) {
            values.update(reader, delivered, partial, key -> updateEntry());
        }
        // whether the form shows what it stands for now
        abstract boolean isShown();
        // the entry, as it is built now, under its name - `hidden` while the form does not show what it stands for
        void writeEntry(JavaScriptObject entry) {
            emitHidden(entry, isShown());
            setField(node.edit(), name, entry);
        }
    }

    // what one react container is given - its nodes, its snapshot - and what its view changes them through:
    // its controller
    public final class ContainerState {
        final GContainer scope;
        final Consumer<JavaScriptObject> publish;
        // each group's node here, of whichever kind; and those its ROWS are drawn on, as their kind - what a list
        // property's column is carried on, and what answers for an lsf property drawn per row (createRowsGroupNode)
        final NativeSIDMap<GGroupObject, GroupNode> nodes = new NativeSIDMap<>();
        final NativeSIDMap<GGroupObject, RowsGroupNode> rowsGroupNodes = new NativeSIDMap<>();
        final TopNode top; // the empty group's node: the container's top level, the descriptors' as well
        JavaScriptObject data = newObject();
        JavaScriptObject draft;
        JavaScriptObject lastPublished; // what the view was last given (flush)
        // `props.controller`: made with the state and never replaced, so a view keeps the one it is given. Its members
        // mirror the entries of `data` and are made with them (initialize); the form gives it what every controller
        // carries (GFormController.addReactContainer)
        public final JavaScriptObject controller;

        ContainerState(GContainer scope, Consumer<JavaScriptObject> publish) {
            this.scope = scope;
            this.publish = publish;
            this.top = new TopNode(this);
            this.controller = makeController(this);
        }
        JavaScriptObject current() { return draft != null ? draft : data; }
        JavaScriptObject edit() {
            if (draft == null) {
                draft = copyKeepingPrototype(data);
                enlist();
            }
            return draft;
        }
        // ... or only its commit, which may write or not: in the batch, nothing copied yet (TreeRowsGroupNode.touch)
        void enlist() {
            pending.put(scope, this);
        }
        // the draft becomes the data - what its rows state about each other placed in it first, from the completed batch
        // (RowsGroupNode.place)
        void commit() {
            rowsGroupNodes.foreachValue(RowsGroupNode::place);
            if (draft != null) {
                data = draft;
                draft = null;
            }
        }

        // a property's entry in a snapshot of this container's data, where its name says it is - null where it has
        // none here: what hides the host of an lsf panel property while it says `hidden` (useLsf)
        public JavaScriptObject entryOf(JavaScriptObject snapshot, GPropertyDraw property) {
            Node node = nodeOf(property.groupObject);
            JavaScriptObject nodeData = node != null ? node.in(snapshot) : null;
            return nodeData != null ? getOwnField(nodeData, property.integrationSID) : null;
        }

        // the node where a part of the group is drawn here, made the first time it is asked for: a panel node, unless
        // the group's rows are drawn here, whose node is there already (createRowsGroupNode: the groups come first)
        GroupNode getOrCreateGroupNode(GGroupObject group) {
            GroupNode node = nodes.get(group);
            if (node == null)
                nodes.put(group, node = new PanelGroupNode(this, group));
            return node;
        }
        // ... the one the group's ROWS are drawn on, made before any other part of the group is placed
        // (createGroupController)
        RowsGroupNode createRowsGroupNode(GGroupObject group) {
            assert nodes.get(group) == null;
            RowsGroupNode node = group.isInTree() // its kind, chosen here and once: a grid's rows, or a tree's
                    ? new TreeRowsGroupNode(this, group) : new GridRowsGroupNode(this, group);
            nodes.put(group, node);
            rowsGroupNodes.put(group, node);
            return node;
        }
        Node nodeOf(GGroupObject group) {
            return group == null ? top : nodes.get(group);
        }
        // what each node sets out once - a rows node its rows, none of them yet, and every node the names it carries
        // and the members that mirror its entries: they are design data, as the entries are. The groups come in the
        // form's order, then the empty group's members, on the controller itself
        void initialize() {
            for (GGroupObject group : form.groupObjects) {
                GroupNode node = nodes.get(group);
                if (node != null)
                    node.initialize();
            }
            top.fillController();
        }

        // the property of this group (null: the empty group) this container carries under the name - which is exactly
        // how FormView claims the name: per container and group, the group's node here answering for it
        public GPropertyDraw getGroupProperty(GGroupObject group, String integrationSID) {
            Node node = nodeOf(group);
            return node != null ? node.getProperty(integrationSID) : null;
        }
        // the lsf list property whose per-row renderers an <Lsf> places, named as a panel property is - its group, then
        // its integration name, o.qty, the path of its column entry -, of a group whose rows this container draws:
        // asked of the nodes the rows are drawn on, not of the container's children, such a property being no child of
        // it (FormView.checkLsfListView). The row an <Lsf> gives says which renderer, and has to be a row of that group
        public GPropertyDraw getRowLsfProperty(String name) {
            int dot = name.lastIndexOf('.');
            if (dot < 0) // a grid property always has a group
                return null;
            String groupSID = name.substring(0, dot);
            for (GGroupObject group : form.groupObjects) {
                RowsGroupNode node = rowsGroupNodes.get(group);
                if (node != null && group.getSID().equals(groupSID))
                    return node.getRowLsfProperty(name.substring(dot + 1));
            }
            return null;
        }

        // the batch: several members' changes in ONE request (no member can be one call for many properties). Every
        // entry names its property by the MEMBER itself - {property: controller.o.qty, object, value} - so nothing is
        // read by name: what an entry changes is the state that member holds, which has to be one of THIS view's
        // controller - a member of another view, or of another form, holds another. An entry with no `value` runs the
        // change event, as exec() does. Every entry says which of them it is, so a mistake in the fifth entry is not
        // reported as a mistake in the call
        void changeProperties(JavaScriptObject entries) {
            String surface = "properties.change()";
            String errorPrefix = controllerPrefix(surface);
            boolean one = !GSimpleStateTableView.isJSArray(entries); // a list states them all, one entry states one
            int size = one ? 1 : GSimpleStateTableView.jsArrayLength(entries);
            if (size == 0) // an empty batch is a no-op, not an empty request
                return;

            GPropertyDraw[] properties = new GPropertyDraw[size];
            GGroupObjectValue[] keys = new GGroupObjectValue[size];
            PValue[] values = new PValue[size];
            for (int i = 0; i < size; i++) {
                JavaScriptObject entry = one ? entries : GSimpleStateTableView.jsArrayGet(entries, i);
                String entryPrefix = one ? errorPrefix : errorPrefix + "entry " + i + ": ";
                String memberPrefix = controllerPrefix(one ? surface : surface + " entry " + i);
                if (!isJSObject(entry))
                    throw new RuntimeException(entryPrefix + "a change must be an object {" + CHANGE_ENTRY_FIELDS.replace(",", ", ") + "}");
                String unknown = getUnknownField(entry, CHANGE_ENTRY_FIELDS);
                if (unknown != null)
                    throw new RuntimeException(entryPrefix + "unknown field '" + unknown + "'; a change has " + CHANGE_ENTRY_FIELDS.replace(",", ", "));
                ReactPropertyEntry propertyEntry = getMemberEntry(getOwnField(entry, "property"));
                if (propertyEntry == null || propertyEntry.node.state != this)
                    throw new RuntimeException(entryPrefix + "'property' must be a property member of this view's controller"
                            + " (controller.<group>.<property>, or controller.<property> for the empty group)");
                boolean exec = !hasOwnField(entry, "value");
                if (!exec && isOwnUndefined(entry, "value"))
                    throw new RuntimeException(entryPrefix + "'value' is undefined - pass null to clear it, or leave 'value' out to run the change event");
                if (!exec && propertyEntry.property.isAction())
                    throw new RuntimeException(entryPrefix + "'" + propertyEntry.property.integrationSID + "' is an action, which has no value to set: leave 'value' out to run it");
                propertyEntry.checkShown(memberPrefix);
                properties[i] = propertyEntry.property;
                keys[i] = propertyEntry.getMemberKey(memberPrefix, getOwnField(entry, "object"));
                values[i] = exec ? PValue.UNDEFINED : GSimpleStateTableView.convertFromJSUndefValue(propertyEntry.property, getOwnField(entry, "value"));
            }
            verbs.changeProperties(properties, keys, values);
        }
    }

    // the DESCRIPTOR of a component React draws or places: its entry at its container's top level, keyed by its SID -
    // its labels, its caption and image, and whether the form shows the component now: its SHOWIF, as the server sends
    // it for a component a react view is told about (true: hidden) - React's alone, as its labels are: a placed one's
    // host is hidden by the view that places it (useLsf), which the platform does not do for what React places
    private abstract class ComponentEntry extends Entry implements GComponentController {
        final GComponent component;
        final GPropertyReader[] labelReaders; // its caption and image

        ComponentEntry(ContainerState state, GComponent component) {
            super(state.top, component.sID);
            this.component = component;
            this.labelReaders = present(component.getLabelReaders());
            updateEntry(); // there from the start
        }
        // what it is built from - what labels it and its SHOWIF alike: kept, and the entry rebuilt
        public void updateLabel(GComponentLabelReader reader, NativeHashMap<GGroupObjectValue, PValue> delivered, boolean partial) {
            keep(reader, delivered, partial);
        }
        public void updateShowIf(GComponentReader reader, NativeHashMap<GGroupObjectValue, PValue> delivered, boolean partial) {
            keep(reader, delivered, partial);
        }
        // its SHOWIF, as React keeps the value (true: hidden): none, until the server sends one, is shown
        boolean isShown() {
            return !PValue.getBooleanValue(values.get(component.showIfReader, GGroupObjectValue.EMPTY));
        }
        GComponent owner() {
            return component;
        }
        // what labels it
        void emit(JavaScriptObject entry) {
            emitAttributes(entry, labelReaders, GGroupObjectValue.EMPTY);
        }
    }
    // ... of one React draws: the rest of it - its classes, its custom design - is React's as well, and draws nothing:
    // the platform has no view of it
    private final class ReactComponentEntry extends ComponentEntry {
        ReactComponentEntry(ContainerState state, GComponent component) {
            super(state, component);
        }
        public void updateOther(GComponentReader reader, NativeHashMap<GGroupObjectValue, PValue> values, boolean partial) {
        }
    }
    // ... of an lsf one React places: the platform draws it and React labels it, from this entry; the rest is the
    // platform's
    private final class LsfComponentEntry extends ComponentEntry {
        final GComponentController lsf; // the platform's controller of the component: the layout, which draws it

        LsfComponentEntry(ContainerState state, GComponent component, GComponentController lsf) {
            super(state, component);
            this.lsf = lsf;
        }
        // the rest of it is the platform's, which draws it
        public void updateOther(GComponentReader reader, NativeHashMap<GGroupObjectValue, PValue> values, boolean partial) {
            lsf.updateOther(reader, values, partial);
        }
    }

    // a node of the projection in one container - a group's, the empty group's being the container's top level, any
    // other's data.<group>: what it writes into `data` (edit), the entries of the group's properties it carries by
    // name, and its member of the controller, as its entry is in `data` (fillController), holding the members that
    // mirror those entries, one per value it carries. One JS object holds all it writes, and so does one node
    private abstract class Node {
        final ContainerState state;
        final ArrayList<PropertyEntry> properties = new ArrayList<>(); // what it carries by name, in the form's order

        Node(ContainerState state) {
            this.state = state;
        }
        abstract JavaScriptObject edit();
        abstract void fillController();
        // its object in a snapshot of the container's data, null where the snapshot has none
        abstract JavaScriptObject in(JavaScriptObject snapshot);
        void putPropertyMembers(JavaScriptObject member) {
            for (PropertyEntry propertyEntry : properties)
                propertyEntry.putMember(member);
        }
        // the property it carries under the name: shown or not, and an lsf one too - a sorting may name an lsf column.
        // Nothing outside the node answers for a name used in it
        GPropertyDraw getProperty(String integrationSID) {
            for (PropertyEntry propertyEntry : properties)
                if (integrationSID.equals(propertyEntry.property.integrationSID))
                    return propertyEntry.property;
            return null;
        }
    }
    // the empty group's node: the container's top level itself - its entries are data's own, its members the
    // controller's own
    private final class TopNode extends Node {
        TopNode(ContainerState state) {
            super(state);
        }
        JavaScriptObject edit() {
            return state.edit();
        }
        JavaScriptObject in(JavaScriptObject snapshot) {
            return snapshot;
        }
        void fillController() {
            putPropertyMembers(state.controller);
        }
    }
    // any other group's node: data.<group> - its OWN field: a plain object answers for Object.prototype's names before
    // any node is written under one - and its member controller.<group>
    private abstract class GroupNode extends Node {
        final GGroupObject group;
        final NativeHashMap<String, Boolean> unnameable = new NativeHashMap<>(); // reported already (reportUnnameable)

        GroupNode(ContainerState state, GGroupObject group) {
            super(state);
            this.group = group;
        }
        JavaScriptObject current() {
            return in(state.current());
        }
        JavaScriptObject published() {
            return in(state.data);
        }
        JavaScriptObject in(JavaScriptObject snapshot) {
            return getOwnField(snapshot, group.getSID());
        }
        JavaScriptObject edit() {
            JavaScriptObject node = current();
            if (node == published()) {
                node = node == null ? newObject() : copyKeepingPrototype(node);
                setField(state.edit(), group.getSID(), node);
            }
            return node;
        }
        // data.<group>.properties: the names of the entries it carries, in the form's order; design data, like them
        void fillDataProperties() {
            JavaScriptObject names = emptyArray();
            for (PropertyEntry propertyEntry : properties)
                push(names, propertyEntry.property.integrationSID);
            setField(edit(), "properties", names);
        }
        // controller.<group>: its member, and on it the members of the entries it carries
        void fillController() {
            JavaScriptObject member = makeMember();
            setField(state.controller, group.getSID(), member);
            putPropertyMembers(member);
        }
        // the group's own state, named the way data.<group> names it
        abstract JavaScriptObject makeMember();
        // what it sets out once: the names of its entries in `data`, the members that mirror them on the controller
        void initialize() {
            fillDataProperties();
            fillController();
        }
        // a property one of the group's lists leaves out, as the projection does not name it there - for the reason
        // given: reported once per list and property
        void reportUnnameable(GPropertyDraw property, String field, String what, String why) {
            String name = property != null ? property.sID : "?";
            String key = field + ":" + name;
            if (unnameable.get(key) == null) {
                unnameable.put(key, Boolean.TRUE);
                GwtClientUtils.logLsfViewError("data." + group.getSID() + "." + field + ": the group is " + what + " by '" + name
                        + "', which the projection does not name here (" + why + "), so that one is not listed");
            }
        }
    }
    // ... where only the group's panel properties are drawn: its member holds theirs
    private final class PanelGroupNode extends GroupNode {
        PanelGroupNode(ContainerState state, GGroupObject group) {
            super(state, group);
        }
        JavaScriptObject makeMember() {
            return newObject();
        }
    }
    // the node a group's ROWS are drawn on: the list, where each row sits in it, which of them is current; its member's
    // change(row), a current object being chosen among them; and the group's own readers, its row attributes and its
    // group attributes - React's alone. Whether the component drawing the rows is shown is that component's descriptor,
    // as anything else's is. It is the group's controller too, as a grid's controller is where the platform draws the
    // rows
    private abstract class RowsGroupNode extends GroupNode implements GGroupController, Rows {
        // as the server sends them, with a view's own add/remove on top - none before the first delivery: its own list
        // from the start, which it replaces and never changes
        ArrayList<GGroupObjectValue> rows = new ArrayList<>();
        final ReaderValues values = new ReaderValues(); // what the group's own readers say, by row and of the group
        GGroupObjectValue currentKey; // kept even before its row arrives: the row states it when it is built
        final NativeHashMap<GGroupObjectValue, Integer> positions = new NativeHashMap<>();
        // what each row carries of the group's own readers: its colors, its selection
        final GPropertyReader[] rowReaders;
        // the group's sortings, the order these rows are drawn in, as the form has them (updateOrders): in priority
        // order, true - ascending. Its own map, replaced and never changed in place
        LinkedHashMap<Column, Boolean> orders = new LinkedHashMap<>();

        RowsGroupNode(ContainerState state, GGroupObject group) {
            super(state, group);
            ArrayList<GPropertyReader> rowReaders = new ArrayList<>();
            for (GGroupObjectPropertyReader reader : group.getPresentationReaders())
                if (reader != null && reader.getAttributeScope() == GGroupAttributeScope.ROW)
                    rowReaders.add(reader);
            this.rowReaders = rowReaders.toArray(new GPropertyReader[0]);
        }
        JavaScriptObject makeMember() {
            JavaScriptObject member = makeRowsMember(this, group.getSID());
            // the sortings are the order its rows are drawn in, so they are a member where those are
            setField(member, "orders", makeOrdersMember(this, group.getSID()));
            return member;
        }
        // ... its rows first, none of them yet, and the sortings they are drawn in
        void initialize() {
            replaceRows();
            replaceOrders();
            super.initialize();
        }
        // what its rows state about each other, written once the batch is complete (ContainerState.commit)
        abstract void place();
        // the lsf list property whose per-row renderers go into the rows drawn here, by its integration name
        GPropertyDraw getRowLsfProperty(String integrationSID) {
            for (GPropertyDraw property : form.propertyDraws)
                if (property.groupObject == group && property.isLsfViewPerRow() && integrationSID.equals(property.integrationSID))
                    return property;
            return null;
        }
        // a group attribute rebuilds the rows or the node - React's alone: the platform has no view of the rows React
        // draws
        public void updateAttribute(GGroupObjectPropertyReader reader, NativeHashMap<GGroupObjectValue, PValue> delivered, boolean partial) {
            values.update(reader, delivered, partial, reader.getAttributeScope() == GGroupAttributeScope.ROW ? this::rowChanged : key -> attributeChanged(reader));
        }

        // ===== the group's controller (GFormController.groupControllers): its rows and its current object as the
        // server sends them, and a view's own add or remove - into the drafts only: the form publishes once the
        // operation is done (GFormController.refreshReactOptimistic)
        public void updateKeys(GGroupObject group, ArrayList<GGroupObjectValue> keys, GFormChanges fc, int requestIndex) {
            setRows(keys);
        }
        public void updateCurrentKey(GGroupObjectValue currentKey) {
            setCurrentObject(currentKey);
        }
        public void changeCurrentKey(GGroupObjectValue currentKey) {
            setCurrentObject(currentKey);
        }
        // the group's sortings as the form has them: its own ORDERS in the first changes, then every list the server
        // reports - an ORDER action's too -, handed over only when it is another one
        public void updateOrders(GGroupObject group, LinkedHashMap<Column, Boolean> orders) {
            this.orders = new LinkedHashMap<>(orders); // a map of its own: the caller's may change after
            replaceOrders();
        }
        // the rows React draws carry no filters yet: there is nothing to show them in
        public void updateFilters(GGroupObject group, ArrayList<GPropertyFilterDTO> filters) {
        }
        public void modifyGroupObject(GGroupObjectValue key, boolean add, int position) {
            ArrayList<GGroupObjectValue> rows = new ArrayList<>(this.rows);
            int index = rows.indexOf(key);
            if (add) {
                if (index < 0) {
                    rows.add(position >= 0 && position <= rows.size() ? position : rows.size(), key);
                    setRows(rows);
                }
                setCurrentObject(key);
            } else if (index >= 0) {
                rows.remove(index);
                setRows(rows);
                if (GwtClientUtils.nullEquals(currentKey, key))
                    setCurrentObject(getNearObject(rows, index));
            }
        }
        public GGroupObjectValue getSelectedKey() {
            return currentKey;
        }
        public int getSelectedRow() {
            Integer position = currentKey != null ? positions.get(currentKey) : null;
            return position != null ? position : -1;
        }
        // the rows as the view has them, its own add or remove included - the node's own list, which is replaced and
        // never changed (setRows)
        public ArrayList<GGroupObjectValue> getRows() {
            return rows;
        }
        // the rows as the SERVER sends them. The values of rows that have left need no forgetting: the server re-reads
        // every property of a grid whose keys changed, and each delivery replaces what was kept
        void setRows(ArrayList<GGroupObjectValue> rows) {
            if (this.rows.equals(rows))
                return;
            this.rows = new ArrayList<>(rows);
            replaceRows();
        }
        // which row is current is the GRID's: its rows say it, and nothing else here asks - so a panel property of any
        // group is not rebuilt when it moves
        void setCurrentObject(GGroupObjectValue key) {
            if (GwtClientUtils.nullEquals(currentKey, key))
                return;
            GGroupObjectValue previous = currentKey;
            currentKey = key;
            rowChanged(previous);
            rowChanged(key);
        }
        void attributeChanged(GPropertyReader reader) {
            replaceAttribute(edit(), reader, values.get(reader, GGroupObjectValue.EMPTY));
        }
        void rowChanged(GGroupObjectValue key) {
            if (key != null) {
                JavaScriptObject row = editRow(key);
                if (row != null)
                    fillRowAttributes(row, key);
            }
        }
        JavaScriptObject editRow(GGroupObjectValue key) {
            Integer position = positions.get(key);
            if (position == null)
                return null; // values can arrive before their row, or outside the current page
            JavaScriptObject current = current();
            JavaScriptObject byKey = field(current, "byKey");
            JavaScriptObject row = field(byKey, key.toKeyString());
            JavaScriptObject original = published();
            // Identity tells whether this object still belongs to the published snapshot. A copied or newly
            // inserted row is already writable, including after a reorder within the same batch.
            if (row == field(field(original, "byKey"), key.toKeyString())) {
                JavaScriptObject list = field(current, "list");
                if (list == field(original, "list")) {
                    list = copyArray(list);
                    byKey = copyKeepingPrototype(byKey);
                    setField(edit(), "list", list);
                    setField(edit(), "byKey", byKey);
                }
                row = copyKeepingPrototype(row);
                setField(byKey, key.toKeyString(), row);
                setArrayElement(list, position, row);
            }
            return row;
        }
        void replaceRows() {
            JavaScriptObject previous = field(current(), "byKey");
            JavaScriptObject list = emptyArray();
            JavaScriptObject byKey = newBareObject();
            JavaScriptObject keys = emptyArray();
            positions.clear();
            int position = 0;
            for (GGroupObjectValue key : rows) {
                JavaScriptObject row = field(previous, key.toKeyString());
                if (row == null) {
                    row = newObject();
                    GGroupObjectValue.registerRow(row, key);
                    fillRowAttributes(row, key);
                    // the node holds panel properties and lsf columns too
                    for (PropertyEntry propertyEntry : properties)
                        propertyEntry.fillRow(row, key);
                }
                positions.put(key, position++);
                setField(byKey, key.toKeyString(), row);
                push(list, row);
                push(keys, key.toKeyString());
            }
            setField(edit(), "list", list);
            setField(edit(), "byKey", byKey);
            setField(edit(), "keys", keys);
        }
        // a row's own attributes: whether it is the current one, and what the group's readers say of it
        void fillRowAttributes(JavaScriptObject row, GGroupObjectValue key) {
            setField(row, "isCurrent", key.equals(currentKey));
            for (GPropertyReader reader : rowReaders)
                replaceAttribute(row, reader, values.get(reader, key));
        }
        // `orders`, the group's sortings in the SAME shape the .lsf ORDER operator reads and writes: the property by
        // integration SID, and `desc`, the server's and the operator's spelling (the client keeps ascending). A sorting
        // is named only as THIS node names it: inside one container a name means one property, but the group may draw
        // a twin of it in another container under the same SID, and naming that one here would read as a sorting on
        // this node's own entry
        void replaceOrders() {
            JavaScriptObject array = emptyArray();
            for (Map.Entry<Column, Boolean> order : orders.entrySet()) {
                GPropertyDraw property = order.getKey().property; // a property React draws is never in columns
                if (property.integrationSID == null) {
                    reportUnnameable(property, "orders", "sorted", "no integration SID");
                    continue;
                }
                if (getProperty(property.integrationSID) != property) {
                    reportUnnameable(property, "orders", "sorted", "this node carries it by no such name");
                    continue;
                }
                JavaScriptObject entry = newObject();
                setField(entry, "property", property.integrationSID);
                setField(entry, "desc", !order.getValue());
                push(array, entry);
            }
            setField(edit(), "orders", array);
        }

        // ===== a call through its member: change(row), the current object chosen among the rows drawn here
        void change(String surface, JavaScriptObject objectOrKey) {
            verbs.changeCurrentObject(group, resolveGroupRow(controllerPrefix(surface), objectOrKey));
        }
        // the row of THIS group a call names (resolveRow), narrowed by the group's own rule - which both checks and
        // cuts. A key that is not this group's row at all answers null: it would otherwise set the objects it does
        // carry and blank the rest, which the server only asserts about (so: nothing at all in production). An empty
        // key names no row either: passed on, it would clear the group's current objects, which choosing a row among
        // those drawn does not offer. And in a TREE a row of a group BELOW carries the path past this one: accepted (it
        // holds one row of every group above), but what it names is the path down to here - the deeper half names
        // rows of other groups, which the server asserts about too, and the projection would keep as a current object
        // no row of this group is found by
        GGroupObjectValue resolveGroupRow(String errorPrefix, JavaScriptObject objectOrKey) {
            GGroupObjectValue key = resolveRow(errorPrefix, objectOrKey);
            GGroupObjectValue rowKey = key.isEmpty() ? null : group.getRowKey(key);
            if (rowKey == null)
                throw new RuntimeException(errorPrefix + "that row is not a row of '" + group.getSID() + "'");
            return rowKey;
        }
        // a row of this group, named however the view has it: the row, its `objects` handle, or the key the projection
        // gave it - a name only here, where the rows are, looked up in the index built with them
        GGroupObjectValue resolveRow(String errorPrefix, JavaScriptObject objectOrKey) {
            GGroupObjectValue key = GGroupObjectValue.resolveObject(objectOrKey);
            if (key == null)
                key = GGroupObjectValue.resolveObject(keyedRow(field(current(), "byKey"), objectOrKey));
            if (key == null)
                throw new RuntimeException(errorPrefix + "expects a row of '" + group.getSID() + "', its objects handle, or its key;"
                        + " that is none of them, or names no row the group has now");
            return key;
        }

        // ===== what a state member reads: a sorting is the state of the rows, so its member is this node's, in the
        // view that draws the rows - a view that does not has no such member at all. What it is: an object the author
        // wrote, carrying only fields this call knows, naming a property this node carries - by its integration name,
        // as `data` names it, not by a member: a state is data, written in the shape the projection publishes it, and
        // may name a property no member stands for, an lsf column. A call names what it changes by its member
        // (ContainerState.changeProperties)
        GPropertyDraw readStateProperty(String errorPrefix, JavaScriptObject item, String what, String fields) {
            String propertySID = readAuthorObject(errorPrefix, item, what, fields);
            GPropertyDraw property = getProperty(propertySID); // a name means the property this view carries
            if (property == null)
                throw new RuntimeException(errorPrefix + "'" + propertySID + "' is not a property this view carries on object group '" + group.getSID() + "'");
            // and nothing more to check: a PROJECTED property is never grouped in columns - such a form is refused when
            // it is built (FormView)
            return property;
        }

        // ===== a call through its `orders` member: the group's sortings, as the view states them, read here - where
        // the rows are, and the sortings with them - and handed to the form as the list they make (Verbs.changeOrders).
        // An order is {property, desc}, the SAME shape `orders` projects and the .lsf ORDER operator reads; one with no
        // `desc` is not a sorting, so it removes the one on that property. An array states them all, its order being
        // the priority
        void changeOrders(String surface, JavaScriptObject orders) {
            String errorPrefix = controllerPrefix(surface);

            LinkedHashMap<Column, Boolean> result = new LinkedHashMap<>();
            // every property the list NAMES, direction or not, once: a group is sorted by one once, and the server keys
            // them by it - so a list naming one twice describes no sorting a group can have, whichever of the two
            // carries a direction
            HashSet<GPropertyDraw> named = new HashSet<>();
            int size = GSimpleStateTableView.jsArrayLength(orders);
            for (int i = 0; i < size; i++) {
                JavaScriptObject order = GSimpleStateTableView.jsArrayGet(orders, i);
                String prefix = errorPrefix + "order " + i + ": ";
                GPropertyDraw property = readStateProperty(prefix, order, "an order", ORDER_FIELDS);
                if (!named.add(property))
                    throw new RuntimeException(prefix + "'" + property.integrationSID + "' is already in this list");
                applyOrder(prefix, order, property, result);
            }
            verbs.changeOrders(group, result);
        }
        // ... one order, merged into the group's sortings as this node holds them - every one of them, those it does
        // not name included
        void changeOrder(String surface, JavaScriptObject order) {
            String errorPrefix = controllerPrefix(surface);
            LinkedHashMap<Column, Boolean> result = new LinkedHashMap<>(orders);
            applyOrder(errorPrefix, order, readStateProperty(errorPrefix, order, "an order", ORDER_FIELDS), result);
            verbs.changeOrders(group, result);
        }
        // what one order the author wrote does to sortings: with no direction it is not a sorting, and takes the one on
        // its property off - which a noSort property has to be able to say, as a default ORDERS on one is listed; with
        // a direction it sorts by the property - refused for a noSort one - in place when the property is sorted
        // already (a map keeps a key where it is), appended when not. `desc` is read once, as every field of what an
        // author wrote is
        void applyOrder(String errorPrefix, JavaScriptObject order, GPropertyDraw property, LinkedHashMap<Column, Boolean> result) {
            Column column = new Column(property, GGroupObjectValue.EMPTY); // a property React draws is never in columns
            JavaScriptObject desc = getOwnField(order, "desc");
            if (isNoValue(desc)) {
                result.remove(column);
                return;
            }
            if (!isJSBoolean(desc))
                throw new RuntimeException(errorPrefix + "'desc' must be true or false");
            if (property.noSort)
                throw new RuntimeException(errorPrefix + "'" + property.integrationSID + "' cannot be sorted by");
            result.put(column, !toBoolean(desc));
        }
    }

    // ... of a GRID: its rows and nothing else
    private final class GridRowsGroupNode extends RowsGroupNode {
        GridRowsGroupNode(ContainerState state, GGroupObject group) {
            super(state, group);
        }
        // a grid's rows state nothing about each other
        void place() {
        }
        // ... and none of them is a node to open
        public void changeExpanded(GGroupObjectValue key, boolean open, long requestIndex) {
        }
        public void changeExpandedAll(boolean open, long requestIndex) {
        }
    }
    // ... of a group of a TREE: its rows say where they hang in it, from the hierarchy sent beside them. A node's
    // children are rows of its own group, when that one recurses, or of the group below; the tree draws all its groups,
    // so the node of each of them is here, and knows the nodes above and below it
    private final class TreeRowsGroupNode extends RowsGroupNode {
        TreeRowsGroupNode up; // null: the tree's top group
        TreeRowsGroupNode down; // null: its bottom group
        // the row each row hangs under, as the server last sent it
        NativeHashMap<GGroupObjectValue, GGroupObjectValue> parents = new NativeHashMap<>();
        // ... so the rows the rows hang under, as they stand: the nodes whose children are loaded (isOpen)
        NativeHashMap<GGroupObjectValue, Boolean> parentRows = new NativeHashMap<>();
        // ... and the rows it says have children
        NativeHashMap<GGroupObjectValue, Boolean> expandable = new NativeHashMap<>();
        // the nodes a view asked to open or close, each with its request, until that request is answered - as the
        // platform's tree keeps them on its own (GTreeContainerTableNode.pendingExpanding)
        final NativeHashMap<GGroupObjectValue, ExpandRequest> pendingExpanding = new NativeHashMap<>();
        boolean touched;

        TreeRowsGroupNode(ContainerState state, GGroupObject group) {
            super(state, group);
        }
        // ... and the nodes above and below it, all of them made by now: in the form's order, which need not be the
        // tree's
        void initialize() {
            super.initialize();
            GGroupObject upGroup = group.getUpTreeGroup();
            if (upGroup != null) {
                up = (TreeRowsGroupNode) state.rowsGroupNodes.get(upGroup); // a group of the same tree
                up.down = this;
            }
        }
        // a tree's rows are its nodes: the verbs that open them are here, with them
        JavaScriptObject makeMember() {
            JavaScriptObject member = super.makeMember();
            addNodeVerbs(this, member, group.getSID());
            return member;
        }
        // its rows, and the hierarchy sent beside them, as the platform's own tree reads it there too. They answer a
        // request to open or close a node they hang under, of this group or of the one above: the server sends them
        // anew with every answer to one, and what was asked up to it gives way to what they say - as in the platform's
        // tree (GTreeTableTree.synchronize)
        public void updateKeys(GGroupObject group, ArrayList<GGroupObjectValue> keys, GFormChanges fc, int requestIndex) {
            super.updateKeys(group, keys, fc, requestIndex);
            ArrayList<GGroupObjectValue> parents = fc.parentObjects.get(group); // parallel to them
            if (parents != null)
                setParents(parents);
            NativeHashMap<GGroupObjectValue, Integer> expandables = fc.expandables.get(group); // a count per key
            if (expandables != null)
                setExpandables(expandables);
            dropPendingExpanding(requestIndex);
            if (up != null)
                up.dropPendingExpanding(requestIndex);
        }
        // ... and the rows are placed anew only where they changed: a page sent again places nothing
        void setRows(ArrayList<GGroupObjectValue> rows) {
            if (this.rows.equals(rows))
                return;
            super.setRows(rows);
            hierarchyChanged();
        }
        // the hierarchy arrives beside the rows, as a list PARALLEL to them - paired here with the rows as they stand
        void setParents(ArrayList<GGroupObjectValue> parents) {
            NativeHashMap<GGroupObjectValue, GGroupObjectValue> keyed = new NativeHashMap<>();
            if (rows.size() == parents.size()) {
                for (int i = 0; i < rows.size(); i++)
                    keyed.put(rows.get(i), group.getParentRowKey(rows.get(i), parents.get(i)));
            } else {
                // parallel by construction; if they ever are not, no row states a parent
                GwtClientUtils.logLsfViewError("data." + group.getSID() + ": " + parents.size() + " parents came for "
                        + rows.size() + " rows; the hierarchy is dropped");
            }
            if (keyed.equals(this.parents)) // a hierarchy sent again unchanged touches nothing
                return;
            this.parents = keyed;
            hierarchyChanged();
        }
        // the rows, or where they hang, changed: so may have the nodes whose children are loaded - of this group, or of
        // the one above - read off the rows the group HAS: a parent is named only while its row is there
        void hierarchyChanged() {
            NativeHashMap<GGroupObjectValue, Boolean> parentRows = new NativeHashMap<>();
            for (GGroupObjectValue key : rows) {
                GGroupObjectValue parent = parents.get(key);
                if (parent != null && !parent.isEmpty())
                    parentRows.put(parent, Boolean.TRUE);
            }
            this.parentRows = parentRows;
            touch();
            if (up != null)
                up.touch();
        }
        // the children a node has, loaded or not, as a count per key - and the projection states only WHETHER it has
        // any, so the count is narrowed to that here: a count that moves without crossing zero changes nothing a row
        // states
        void setExpandables(NativeHashMap<GGroupObjectValue, Integer> counts) {
            NativeHashMap<GGroupObjectValue, Boolean> expandable = new NativeHashMap<>();
            counts.foreachEntry((key, count) -> {
                if (count != null && count > 0)
                    expandable.put(key, Boolean.TRUE);
            });
            if (expandable.equals(this.expandable))
                return;
            this.expandable = expandable;
            touch();
        }
        // ... and the container with it: its commit places the rows (place)
        void touch() {
            touched = true;
            state.enlist();
        }
        // each row of the group states its facts in the tree, once the batch is complete - the rows of a node's children
        // may come after it - each compared with what the row says now and written only where it differs: a row whose
        // facts stayed keeps its object, whatever else moved in the tree, and a row made anew says nothing yet, so it is
        // given them all
        void place() {
            if (!touched)
                return;
            touched = false;
            for (GGroupObjectValue key : rows) {
                JavaScriptObject row = currentRow(key);
                // read for a row the group HAS: a parent recorded for a row since taken away - a view's own remove
                // comes with no hierarchy - is never asked
                GGroupObjectValue parent = parents.get(key);
                if (parent == null || parent.isEmpty()) { // a root, or a row whose hierarchy was dropped
                    if (!isNullField(row, "parent"))
                        setField(editRow(key), "parent", (JavaScriptObject) null);
                } else if (!GGroupObjectValue.isKeyAt(row, "parent", parent)) // written the way a KEY is: === holds
                    GGroupObjectValue.writeKey(editRow(key), "parent", parent);
                boolean hasChildren = hasChildren(key);
                if (!isField(row, "hasChildren", hasChildren))
                    setField(editRow(key), "hasChildren", hasChildren);
                boolean expanded = isOpen(key);
                if (!isField(row, "expanded", expanded))
                    setField(editRow(key), "expanded", expanded);
            }
        }
        // whether the server counts children under a row, loaded or not
        boolean hasChildren(GGroupObjectValue key) {
            return expandable.get(key) != null;
        }
        // A node is OPEN when its children are loaded: a row of its group or of the group below names it as its parent,
        // and the keys of a tree's groups cannot collide. Until the server answers what a view asked of it, it is what
        // was asked; only a node with children opens
        boolean isOpen(GGroupObjectValue key) {
            ExpandRequest request = pendingExpanding.get(key);
            if (request != null)
                return request.open && hasChildren(key);
            return parentRows.get(key) != null || (down != null && down.parentRows.get(key) != null);
        }
        // the row as it stands now: in the draft, or as published
        JavaScriptObject currentRow(GGroupObjectValue key) {
            return field(field(current(), "byKey"), key.toKeyString());
        }
        // ===== a call through its member: expand(row), collapse(row), toggle(row) - a node of the tree, one of the
        // rows drawn here, named as change(row) names a row of its group (resolveGroupRow): a row of a group BELOW
        // carries the path past this one and names the node it hangs under here. Straight to the server: a tree whose
        // rows React draws has no platform table to go through. Expanded-ness is the SERVER's state (it decides which
        // rows a group returns at all), so these return nothing: the children arrive as rows. The node's `expanded`
        // says what was asked at once, as a changed value does, and what the rows say once the server answers
        void expand(String surface, JavaScriptObject objectOrKey) {
            verbs.expandNode(group, resolveGroupRow(controllerPrefix(surface), objectOrKey));
        }
        void collapse(String surface, JavaScriptObject objectOrKey) {
            verbs.collapseNode(group, resolveGroupRow(controllerPrefix(surface), objectOrKey));
        }
        // ... the opposite of what the node says now (`expanded`) - what was asked of it included, so two toggles
        // before the answer open it and close it again
        void toggle(String surface, JavaScriptObject objectOrKey) {
            GGroupObjectValue key = resolveGroupRow(controllerPrefix(surface), objectOrKey);
            if (isOpen(key))
                verbs.collapseNode(group, key);
            else
                verbs.expandNode(group, key);
        }
        // ... expandAll(), collapseAll(): every node of the group, and of the groups below it - for the tree's top
        // group, the whole tree
        void expandAll() {
            verbs.expandAll(group);
        }
        void collapseAll() {
            verbs.collapseAll(group);
        }
        // ===== the group's controller (GFormController.groupControllers), of a tree: a node asked to open or close,
        // once the request is sent - into the drafts only, as the form publishes then - shown as asked until the keys
        // of the answer come with the request's index (updateKeys)
        public void changeExpanded(GGroupObjectValue key, boolean open, long requestIndex) {
            setPendingExpanding(key, new ExpandRequest(open, requestIndex));
        }
        // ... and so is every node there is of the group and of the groups below it; the latest request asked of a node
        // is the one it shows
        public void changeExpandedAll(boolean open, long requestIndex) {
            ExpandRequest request = new ExpandRequest(open, requestIndex);
            for (TreeRowsGroupNode node = this; node != null; node = node.down)
                for (GGroupObjectValue key : node.rows)
                    node.setPendingExpanding(key, request);
        }
        // ... kept only where a node may have children: nothing opens elsewhere, and no keys would answer it
        void setPendingExpanding(GGroupObjectValue key, ExpandRequest request) {
            if (!group.mayHaveChildren())
                return;
            pendingExpanding.put(key, request);
            touch();
        }
        // the server answered the requests up to this one: what they asked gives way to what the rows say
        void dropPendingExpanding(int requestIndex) {
            ArrayList<GGroupObjectValue> answered = new ArrayList<>();
            pendingExpanding.foreachEntry((key, request) -> {
                if (request.requestIndex <= requestIndex)
                    answered.add(key);
            });
            for (GGroupObjectValue key : answered)
                pendingExpanding.remove(key);
            if (!answered.isEmpty())
                touch();
        }
    }
    // what a view asked of a node of a tree, and the request that asks it of the server
    private static final class ExpandRequest {
        final boolean open;
        final long requestIndex;

        ExpandRequest(boolean open, long requestIndex) {
            this.open = open;
            this.requestIndex = requestIndex;
        }
    }
    // whether a row already says so - a field it does not have yet says nothing
    private static native boolean isNullField(JavaScriptObject row, String field) /*-{ return row[field] === null; }-*/;
    private static native boolean isField(JavaScriptObject row, String field, boolean value) /*-{ return row[field] === value; }-*/;

    // what React writes for a property - its entry, on its node under its name: all of a property React draws, or the
    // labels of a property the platform draws - and whether the form shows it, which the entry says: there from the
    // start. It is the form's controller of the property, all of it: its values and what labels it alike
    private abstract class PropertyEntry extends Entry implements GPropertyController {
        final GPropertyDraw property;
        // whether the form shows the property now: delivered whole, and not dropped since - an lsf one's values and its
        // drop come through its entry on their way to the platform - kept here, as no value says it
        boolean shown;

        // carried by name - its integration name - on its node, in the form's order: a call, a condition or a sorting
        // names it by that name
        PropertyEntry(Node node, GPropertyDraw property) {
            super(node, property.integrationSID);
            this.property = property;
            node.properties.add(this);
        }
        boolean isShown() {
            return shown;
        }
        void setShown(boolean shown) {
            if (this.shown == shown)
                return;
            this.shown = shown;
            shownChanged();
        }
        void shownChanged() {
            updateEntry();
        }
        // what a new row carries of it: a column's cell; nothing of a panel entry or of an lsf property's, whose cells
        // the platform draws
        void fillRow(JavaScriptObject row, GGroupObjectValue key) {
        }
        // its member, on its node's member as its entry is on its node: none for an lsf property, whose values the
        // platform draws and edits
        void putMember(JavaScriptObject owner) {
        }
        GComponent owner() {
            return property;
        }
    }
    // a property React draws - a panel entry, a column and its cells: carried by name on its group's node, its values
    // and the attributes it is drawn with kept here, React's alone - the platform has no view of it
    private abstract class ReactPropertyEntry extends PropertyEntry {
        ReactPropertyEntry(Node node, GPropertyDraw property) {
            super(node, property);
        }
        // its values, as the server sends them: it arrives with the first whole delivery and is rebuilt then whole;
        // after that, by the keys that changed
        public void updateValue(GPropertyDraw reader, NativeHashMap<GGroupObjectValue, PValue> delivered, boolean partial) {
            if (partial || shown)
                values.update(property, delivered, partial, this::valueChanged);
            else {
                values.put(property, delivered, false);
                setShown(true);
            }
        }
        // its values go with it; its attributes stay as the server last sent them
        public void dropProperty(GPropertyDraw dropped) {
            values.remove(property);
            setShown(false);
        }
        // what React draws nothing from - its loading, its classes, its change keys - goes nowhere
        public void updateOther(GExtraPropertyReader reader, NativeHashMap<GGroupObjectValue, PValue> values, boolean partial) {
        }
        // what it is drawn with - an attribute of it, what labels it too: kept where its entry says (attribute)
        public void updateAttribute(GExtraPropertyReader reader, NativeHashMap<GGroupObjectValue, PValue> delivered, boolean partial) {
            attribute(reader, delivered, partial);
        }
        public void updateLabel(GExtraLabelReader reader, NativeHashMap<GGroupObjectValue, PValue> delivered, boolean partial) {
            attribute(reader, delivered, partial);
        }
        abstract void attribute(GPropertyReader reader, NativeHashMap<GGroupObjectValue, PValue> delivered, boolean partial);
        // a cell of it: its value while the form shows it, and what these readers say of it, for the key - the panel
        // entry is its one cell, a column has one per row
        final void emitCell(JavaScriptObject target, GPropertyReader[] readers, GGroupObjectValue key) {
            if (shown)
                emitValue(target, property, values.get(property, key));
            emitAttributes(target, readers, key);
        }

        // ===== its controller (GFormController.propertyControllers): whether the form shows it now - a dropped one is
        // not, so a reconciliation does not make an old value up for it
        // (GFormController.modifyFormChangesWithChangePropertyAsyncs) - where focus goes: nowhere, React draws it - and
        // the client's own value for a cell, an optimistic change, into the draft only: the form publishes once the
        // operation is done (GFormController.refreshReactOptimistic)
        public boolean isPropertyShown(GPropertyDraw property) {
            return shown;
        }
        public void focusProperty(GPropertyDraw property) {
        }
        public Pair<GGroupObjectValue, PValue> setLoadingValueAt(GPropertyDraw property, GGroupObjectValue fullCurrentKey, PValue value) {
            GGroupObjectValue cellKey = getCellKey(fullCurrentKey);
            if (cellKey == null)
                return null;
            PValue oldValue = values.get(property, cellKey);
            put(cellKey, value);
            return new Pair<>(cellKey, oldValue);
        }
        // the key of its cell for the edited one, which is what setLoadingValueAt RETURNS: the caller keys the pending
        // change / loading requests by it, comparing them with the keys the server's own delta carries.
        // `fullCurrentKey` is every group's current object plus the edited cell's key
        // (GFormController.getFullCurrentKey), and null - the key holds no cell of it - is "no cell"
        abstract GGroupObjectValue getCellKey(GGroupObjectValue fullCurrentKey);
        // one value, the client's own: it is kept, and its key rebuilt
        void put(GGroupObjectValue key, PValue value) {
            if (values.put(property, key, value))
                valueChanged(key);
        }
        // its value, or an attribute keyed like one
        abstract void valueChanged(GGroupObjectValue key);

        // ===== its member, on its node's member as its entry is on its node. The member IS the address - it holds this
        // state - so a call through it names nothing but the row
        void putMember(JavaScriptObject owner) {
            setField(owner, property.integrationSID, makePropertyMember(this, getName(property)));
        }
        // change(value[, row]): the value is given - null clears it - since running the property's change event without
        // one is exec(), and an action has no value to set
        void change(String surface, JavaScriptObject value, JavaScriptObject objectOrKey, boolean noValue) {
            String errorPrefix = controllerPrefix(surface);
            if (property.isAction())
                throw new RuntimeException(errorPrefix + "'" + property.integrationSID + "' is an action, which has no value to set: call exec()");
            if (noValue)
                throw new RuntimeException(errorPrefix + "the value is missing - pass null to clear it, or call exec() to run the property's change event");
            changeCell(errorPrefix, objectOrKey, GSimpleStateTableView.convertFromJSUndefValue(property, value));
        }
        // ... exec([row]): the property's change event, or the action, with no value given
        void exec(String surface, JavaScriptObject objectOrKey) {
            changeCell(controllerPrefix(surface), objectOrKey, PValue.UNDEFINED);
        }
        private void changeCell(String errorPrefix, JavaScriptObject objectOrKey, PValue value) {
            checkShown(errorPrefix);
            verbs.changeProperties(new GPropertyDraw[]{property}, new GGroupObjectValue[]{getMemberKey(errorPrefix, objectOrKey)}, new PValue[]{value});
        }
        // ... getValues([row,] value[, mode], ok, fail[, count]): the suggestion list of the cell the call names, from
        // the lookup its mode picks - an unknown one is said on the console, and the call fails
        void getValues(String surface, JavaScriptObject objectOrKey, String value, String mode, JavaScriptObject successCallback, JavaScriptObject failureCallback, int increaseValuesNeededCount) {
            String errorPrefix = controllerPrefix(surface);
            String actionSID = GFormController.getAsyncActionSID(errorPrefix, mode);
            if (actionSID == null) { // unknown mode (already logged)
                if (failureCallback != null)
                    GwtClientUtils.call(failureCallback);
                return;
            }
            checkShown(errorPrefix);
            verbs.getPropertyValues(property, getMemberKey(errorPrefix, objectOrKey), value, actionSID, successCallback, failureCallback, increaseValuesNeededCount);
        }
        // a member answers while the form shows its property: it stays on the controller when a SHOWIF hides it - the
        // names are design data, like the entries - and a call through it is refused until its entry is no longer
        // `hidden`
        void checkShown(String errorPrefix) {
            if (!shown)
                throw new RuntimeException(errorPrefix + "'" + property.integrationSID + "' is not shown by the form now"
                        + " (its entry is hidden), so it has nobody to change it for");
        }
        // the key of the cell a call names: EMPTY, the current objects, where it names no row
        abstract GGroupObjectValue getMemberKey(String errorPrefix, JavaScriptObject objectOrKey);
    }
    // a panel or empty-group property's entry: its value and every attribute of it, in one place
    private final class ReactPanelPropertyEntry extends ReactPropertyEntry {
        final GPropertyReader[] readers; // what it is drawn with

        ReactPanelPropertyEntry(Node node, GPropertyDraw property) {
            super(node, property);
            readers = present(property.getPresentationReaders());
            updateEntry(); // hidden until it is delivered
        }
        // what it is drawn with: every attribute is on its one entry
        void attribute(GPropertyReader reader, NativeHashMap<GGroupObjectValue, PValue> delivered, boolean partial) {
            keep(reader, delivered, partial);
        }
        void valueChanged(GGroupObjectValue key) {
            updateEntry();
        }
        // its one cell - its value while the form shows it, its attributes - and what the property is
        void emit(JavaScriptObject entry) {
            emitCell(entry, readers, GGroupObjectValue.EMPTY);
            emitPropertyFacts(entry, property);
        }
        // its one cell, keyed by its column objects: what is dropped is the groups' current objects
        GGroupObjectValue getCellKey(GGroupObjectValue fullCurrentKey) {
            return property.filterColumnKeys(fullCurrentKey);
        }
        // a member names no row of it: a panel entry shows its group's current object, one slot, and the empty group
        // has no rows at all - it is changed for the current objects
        GGroupObjectValue getMemberKey(String errorPrefix, JavaScriptObject objectOrKey) {
            // raw JS key: a numeric 0 key reads as null under Java == null (GWT falsy-primitive collapse)
            if (!GwtClientUtils.isUndefinedOrNull(objectOrKey))
                throw new RuntimeException(errorPrefix + (property.groupObject == null
                        ? "'" + property.integrationSID + "' belongs to no object group, so there is no row to name"
                        : "'" + property.integrationSID + "' is a panel property: it is changed for the current object of '"
                            + property.groupObject.getSID() + "', which is chosen where the rows are drawn"));
            return GGroupObjectValue.EMPTY;
        }
    }
    // a list property's column, and its cells in the rows of the node the column is on
    private final class ReactColumnPropertyEntry extends ReactPropertyEntry {
        final RowsGroupNode rowsGroupNode;
        // what it is drawn with, by the side of the column / cell split each lands on - an ACTION's image is a cell's
        final GPropertyReader[] columnReaders, cellReaders;

        ReactColumnPropertyEntry(RowsGroupNode rowsGroupNode, GPropertyDraw property) {
            super(rowsGroupNode, property);
            this.rowsGroupNode = rowsGroupNode;
            ArrayList<GPropertyReader> column = new ArrayList<>(), cell = new ArrayList<>();
            for (GPropertyReader reader : present(property.getPresentationReaders()))
                (reader.isColumnAttribute(property) ? column : cell).add(reader);
            columnReaders = column.toArray(new GPropertyReader[0]);
            cellReaders = cell.toArray(new GPropertyReader[0]);
            updateEntry(); // hidden until it is delivered
        }
        // what it is drawn with - a column's attribute rebuilds the column entry, a cell's the cell
        void attribute(GPropertyReader reader, NativeHashMap<GGroupObjectValue, PValue> delivered, boolean partial) {
            values.update(reader, delivered, partial, reader.isColumnAttribute(property) ? key -> updateEntry() : this::writeCell);
        }
        void shownChanged() { // the column says it, and so does every cell
            updateEntry();
            for (GGroupObjectValue key : rowsGroupNode.rows)
                writeCell(key);
        }
        void valueChanged(GGroupObjectValue key) {
            writeCell(key);
        }
        // the column: its attributes, and what the property is
        void emit(JavaScriptObject entry) {
            emitAttributes(entry, columnReaders, GGroupObjectValue.EMPTY);
            emitPropertyFacts(entry, property);
        }
        void writeCell(GGroupObjectValue key) {
            JavaScriptObject row = rowsGroupNode.editRow(key);
            if (row != null)
                fillRow(row, key);
        }
        // a cell, in the row
        void fillRow(JavaScriptObject row, GGroupObjectValue key) {
            JavaScriptObject cell = newObject();
            emitCell(cell, cellReaders, key);
            setField(row, property.integrationSID, cell);
        }
        // the ROW key, asked of the GROUP - a grid's own objects, a tree's whole path - and not of the grid rule
        // restated here, which on a tree gives a key no row is found by: what is dropped is the other groups' objects
        // and a column key, and an empty key is the current row's
        GGroupObjectValue getCellKey(GGroupObjectValue fullCurrentKey) {
            return fullCurrentKey.isEmpty() ? rowsGroupNode.currentKey : property.groupObject.getRowKey(fullCurrentKey);
        }
        // a row the member names: one drawn where the member is, as a row, its `objects` handle, or its key. A row of
        // ANOTHER group is refused - exactly as the group's own change(row) refuses it, and for the same reason: the
        // key carries only that group's objects, so the rest are taken from the CURRENT ones and the call lands on a
        // cell of this group that nobody named. In a TREE a row of a group BELOW passes, and should: its key is the
        // path down to it, so it holds one row of every group above too - the same reading the group's change(row) has
        // always had
        GGroupObjectValue getMemberKey(String errorPrefix, JavaScriptObject objectOrKey) {
            // raw JS key: a numeric 0 key reads as null under Java == null (GWT falsy-primitive collapse)
            if (GwtClientUtils.isUndefinedOrNull(objectOrKey))
                return GGroupObjectValue.EMPTY;
            GGroupObjectValue objectKey = GGroupObjectValue.resolveObject(objectOrKey);
            if (objectKey == null) // ... a KEY, looked up in the rows the column is drawn in
                return rowsGroupNode.resolveRow(errorPrefix, objectOrKey);
            if (!objectKey.isEmpty() && property.groupObject.filterRowKeys(objectKey) == null)
                throw new RuntimeException(errorPrefix + "that row is not a row of '" + property.groupObject.getSID() + "'");
            return objectKey;
        }
    }
    // a property the platform draws and React labels - an lsf one: the form's controller of it is this entry, over the
    // platform's (lsf), which draws it. What labels the property is React's, kept here; the rest of it - its values,
    // its drop, focus, the client's own value, any other attribute or reader - is the platform's, handed on with the
    // property. There from the start, and `hidden` whenever the platform does not show the property - it shows it from
    // its first whole delivery until its drop, both of which come through here
    private abstract class LsfPropertyEntry extends PropertyEntry {
        final GLsfPropertyController lsf; // the platform's controller of the property: the view it is drawn in
        final GPropertyReader[] labelReaders; // its caption, image and comment, a column's footer

        LsfPropertyEntry(Node node, GPropertyDraw property, GLsfPropertyController lsf, GPropertyReader... labelReaders) {
            super(node, property);
            this.lsf = lsf;
            this.labelReaders = present(labelReaders);
            updateEntry(); // there from the start, hidden until the platform shows the property
        }
        // what labels it: React's
        public void updateLabel(GExtraLabelReader reader, NativeHashMap<GGroupObjectValue, PValue> delivered, boolean partial) {
            keep(reader, delivered, partial);
        }
        void emit(JavaScriptObject entry) {
            emitAttributes(entry, labelReaders, GGroupObjectValue.EMPTY);
        }
        // ... and the rest the platform's: its values - the platform shows the property from its first whole delivery -
        // and its drop, focus, the client's own value, any other attribute or reader
        public void updateValue(GPropertyDraw property, NativeHashMap<GGroupObjectValue, PValue> values, boolean partial) {
            lsf.updateValue(property, values, partial);
            if (!partial)
                setShown(true);
        }
        public void dropProperty(GPropertyDraw property) {
            lsf.dropProperty(property);
            setShown(false);
        }
        public boolean isPropertyShown(GPropertyDraw property) {
            return lsf.isPropertyShown(property);
        }
        public void focusProperty(GPropertyDraw property) {
            lsf.focusProperty(property);
        }
        public Pair<GGroupObjectValue, PValue> setLoadingValueAt(GPropertyDraw property, GGroupObjectValue fullCurrentKey, PValue value) {
            return lsf.setLoadingValueAt(property, fullCurrentKey, value);
        }
        public void updateAttribute(GExtraPropertyReader reader, NativeHashMap<GGroupObjectValue, PValue> values, boolean partial) {
            lsf.updateAttribute(reader, values, partial);
        }
        public void updateOther(GExtraPropertyReader reader, NativeHashMap<GGroupObjectValue, PValue> values, boolean partial) {
            lsf.updateOther(reader, values, partial);
        }
    }
    // ... a panel or empty-group one's: its labels - caption, image and comment, the platform drawing none -, keyed by
    // its integration name on its group's node, where a panel property React draws is, and carried by name with them; no
    // member, its value being the platform's. An <Lsf> places it by the same name (getName), and hides its host by this
    // entry (ContainerState.entryOf)
    private final class LsfPanelPropertyEntry extends LsfPropertyEntry {
        LsfPanelPropertyEntry(Node node, GPropertyDraw property, GLsfPropertyController lsf) {
            super(node, property, lsf, property.captionReader, property.getImageReader(), property.commentReader);
        }
    }
    // ... a list one's column, whose cells the platform draws in the rows React draws: React draws the column from this
    // entry - what labels it, its caption, image, footer and comment, and what the property is - carried by name on the
    // node the rows are on
    private final class LsfColumnPropertyEntry extends LsfPropertyEntry {
        LsfColumnPropertyEntry(RowsGroupNode rowsGroupNode, GPropertyDraw property, GLsfPropertyController lsf) {
            super(rowsGroupNode, property, lsf, property.captionReader, property.getImageReader(), property.footerReader, property.commentReader);
        }
        // what labels it, and what the property is: an LSF column is filtered and sorted like any other, and the
        // platform draws its VALUE, which says nothing about what the value is
        void emit(JavaScriptObject entry) {
            super.emit(entry);
            emitPropertyFacts(entry, property);
        }
    }

    // the name a view has for a component - in `data`, on the controller and in an <Lsf> alike, null for one with none:
    // a property's is its integration name, after its group's, as its entry is data.o.note - alone for the empty
    // group's, data.note; any other component's is its design identifier, BOX(o)
    public static String getName(GComponent component) {
        if (!(component instanceof GPropertyDraw))
            return component.sID;
        GPropertyDraw property = (GPropertyDraw) component;
        if (property.integrationSID == null)
            return null;
        return property.groupObject == null ? property.integrationSID : property.groupObject.getSID() + "." + property.integrationSID;
    }

    // where a group's ROWS go: the react container that draws them, null when the platform does - asked of what DRAWS
    // them, never of the group's box (mirrors FormView.rowsScope)
    public GContainer rowsScope(GGroupObject group) {
        return contentScope(group.getDrawComponent());
    }
    // where a component's CONTENT goes, null when the platform draws it. A list property has no place of its own: its
    // content is its column, where the rows are, lsf or not (mirrors FormView.contentScope)
    public GContainer contentScope(GComponent component) {
        if (component instanceof GPropertyDraw && ((GPropertyDraw) component).isList)
            return rowsScope(((GPropertyDraw) component).groupObject);
        return component.isLsfView() ? null : placed(component);
    }
    // ... and WHERE THE VIEW NAMES IT, null where none does: the one container a component is named in (getName), and
    // where its entry is. A property is named on its group's node, where React draws its content - a list property's
    // column, lsf or not, where the rows are - or, for an lsf panel one, where React places it, its entry labelling what
    // the platform draws (an ACTION's has no entry: its caption and image are its button's face). Any other component
    // is named at the top level, where React draws or places it - one the design does not name, a user filter's,
    // nowhere (mirrors FormView.nameScope)
    public GContainer nameScope(GComponent component) {
        if (component instanceof GPropertyDraw && ((GPropertyDraw) component).isList) // its column, where the rows are
            return rowsScope(((GPropertyDraw) component).groupObject);
        return component.sID != null ? placed(component) : null;
    }
    // the react container a component is in, as long as it has a view: one REMOVEd from the design is still the
    // container of what it held, but nothing built it, so nothing of it is projected. Asked while the form is built -
    // here and by the form's controllers - and never by a delta: what a reader brings goes to what React has of its
    // owner
    private GContainer placed(GComponent component) {
        if (containers.isEmpty()) // a form with no react container: nowhere to be placed, nothing walked
            return null;
        GContainer scope = place.apply(component);
        return scope != null && containers.get(scope) != null ? scope : null;
    }

    // a group's own reader, on its node or on a row: no owner, so no static default
    private static void replaceAttribute(JavaScriptObject entry, GPropertyReader reader, PValue pvalue) {
        String field = reader.getAttributeField();
        if (field != null)
            deleteField(entry, field);
        emitAttribute(entry, reader, pvalue, null);
    }
    // the readers of these that there are - a component, a property, has not every one of them - kept by the state they
    // build
    private static GPropertyReader[] present(GPropertyReader... readers) {
        ArrayList<GPropertyReader> present = new ArrayList<>();
        for (GPropertyReader reader : readers)
            if (reader != null)
                present.add(reader);
        return present.toArray(new GPropertyReader[0]);
    }
    // a copy with the source's own prototype, so a bare byKey (newBareObject) stays bare - GwtClientUtils.copyObject
    // makes a plain object
    private static native JavaScriptObject copyKeepingPrototype(JavaScriptObject source) /*-{
        return Object.assign(Object.create(Object.getPrototypeOf(source)), source);
    }-*/;
    private static native JavaScriptObject copyArray(JavaScriptObject source) /*-{ return source.slice(); }-*/;
    private static native JavaScriptObject field(JavaScriptObject source, String name) /*-{ return source == null ? null : source[name]; }-*/;
    private static native void deleteField(JavaScriptObject target, String name) /*-{ delete target[name]; }-*/;
    private static native void setArrayElement(JavaScriptObject array, int index, JavaScriptObject value) /*-{ array[index] = value; }-*/;

    private GGroupObjectValue getNearObject(ArrayList<GGroupObjectValue> rows, int removedIndex) {
        if (rows.isEmpty())
            return null;
        return rows.get(removedIndex == rows.size() ? removedIndex - 1 : removedIndex);
    }

    private static native JavaScriptObject keyedRow(JavaScriptObject byKey, JavaScriptObject token) /*-{
        return (typeof token === 'string' || typeof token === 'number') ? byKey[String(token)] : null;
    }-*/;

    // ===== the members. A member IS the address: it holds the state it stands for - a property member its property's
    // value, a group member the node the rows are drawn on - so a call through it names nothing it changes, and its
    // arguments come in a fixed order, the value first, then the row. FormView refused a name that would take a
    // controller verb, a group's own member or `__proto__` when it built the form (claimProjectionName), so none of
    // them is taken here.
    // the state a PROPERTY member stands for, or null for anything else: a group node, the batch, a value
    private static native ReactPropertyEntry getMemberEntry(JavaScriptObject member) /*-{
        return member && member.__member === "property" ? member.__property : null;
    }-*/;
    // ... and its property
    static GPropertyDraw getMemberProperty(JavaScriptObject member) {
        ReactPropertyEntry property = getMemberEntry(member);
        return property != null ? property.property : null;
    }

    // a controller, with the one mutation no single member can own - a batch spans properties, and groups - so it hangs
    // at the level whose members those properties are: a list of {property, object, value}, or one of them alone. A
    // `property` is the MEMBER itself (controller.o.qty), never a name; `object` omitted means the property's group's
    // current object, and `value` omitted runs its change event. One request for all of them, which is the whole
    // point - otherwise it is a member's own change / exec
    private static native JavaScriptObject makeController(ContainerState state) /*-{
        return {
            properties: {
                change: function (entries) {
                    return state.@lsfusion.gwt.client.form.design.view.GReactFormData.ContainerState::changeProperties(*)(entries);
                }
            }
        };
    }-*/;

    // the member of a group whose rows are drawn here, named the way data.<group> names it: its current object is
    // chosen among its rows, so .change(row) is there, and only there. A feature that states a LIST of things adds its
    // member beside this one
    private static native JavaScriptObject makeRowsMember(RowsGroupNode node, String sid) /*-{
        return {
            change: function (row) {
                return node.@lsfusion.gwt.client.form.design.view.GReactFormData.RowsGroupNode::change(*)(sid + ".change()", row);
            }
        };
    }-*/;

    // ... and on a group of a TREE, its node verbs: a node IS a row, so the verbs that open one are the group's - and
    // those that open all of them
    private static native void addNodeVerbs(TreeRowsGroupNode node, JavaScriptObject member, String sid) /*-{
        member.expand = function (row) {
            return node.@lsfusion.gwt.client.form.design.view.GReactFormData.TreeRowsGroupNode::expand(*)(sid + ".expand()", row);
        };
        member.collapse = function (row) {
            return node.@lsfusion.gwt.client.form.design.view.GReactFormData.TreeRowsGroupNode::collapse(*)(sid + ".collapse()", row);
        };
        member.toggle = function (row) {
            return node.@lsfusion.gwt.client.form.design.view.GReactFormData.TreeRowsGroupNode::toggle(*)(sid + ".toggle()", row);
        };
        member.expandAll = function () {
            return node.@lsfusion.gwt.client.form.design.view.GReactFormData.TreeRowsGroupNode::expandAll()();
        };
        member.collapseAll = function () {
            return node.@lsfusion.gwt.client.form.design.view.GReactFormData.TreeRowsGroupNode::collapseAll()();
        };
    }-*/;

    // the group's sortings: an array states them all, its order being the priority, and one order states one
    private static native JavaScriptObject makeOrdersMember(RowsGroupNode node, String sid) /*-{
        return {
            change: function (orders) {
                return Array.isArray(orders)
                    ? node.@lsfusion.gwt.client.form.design.view.GReactFormData.RowsGroupNode::changeOrders(*)(sid + ".orders.change()", orders)
                    : node.@lsfusion.gwt.client.form.design.view.GReactFormData.RowsGroupNode::changeOrder(*)(sid + ".orders.change()", orders);
            }
        };
    }-*/;

    private static native JavaScriptObject makePropertyMember(ReactPropertyEntry propertyEntry, String qualified) /*-{
        var member = {
            //   .change(value)          set it on the current object       .change(value, row)   ... on that row
            change: function (value, object) {
                return propertyEntry.@lsfusion.gwt.client.form.design.view.GReactFormData.ReactPropertyEntry::change(*)(qualified + ".change()", value === undefined ? null : value, object === undefined ? null : object, value === undefined);
            },
            //   .exec()                 run its change event on the current object       .exec(row)   ... on that row
            exec: function (object) {
                return propertyEntry.@lsfusion.gwt.client.form.design.view.GReactFormData.ReactPropertyEntry::exec(*)(qualified + ".exec()", object === undefined ? null : object);
            },
            // .getValues([row,] value[, mode], ok, fail[, count]) — the suggestion list for the current row, or for
            // the row given. ok gets {data:[{displayString,rawString,objects}], more}; mode picks the server lookup
            // (default 'objects'): 'objects' (each item's `objects` is a raw handle — the object picker) | 'values'
            // (DISTINCT property values) | 'change' (the edit-time autocomplete of the property's own change action).
            // Positional guess (no options object — that's a future uniform cross-method migration): ok is the first
            // function argument, and what precedes it is read by count and type. A row here is a row, a handle or a
            // key - a key only with a mode after the value: with two arguments before ok a string first is the query.
            getValues: function () {
                var okIndex = -1;
                for (var i = 0; i < arguments.length; i++)
                    if (typeof arguments[i] === 'function') { okIndex = i; break; }
                var object = null, value, mode = null;
                if (okIndex === 1) { // {value}
                    value = arguments[0];
                } else if (okIndex === 2) { // {value, mode} | {row, value}
                    if (typeof arguments[0] === 'string') { value = arguments[0]; mode = arguments[1]; }
                    else { object = arguments[0]; value = arguments[1]; }
                } else { // {row, value, mode}
                    object = arguments[0]; value = arguments[1]; mode = arguments[2];
                }
                var ok = arguments[okIndex], fail = arguments[okIndex + 1], count = arguments[okIndex + 2];
                propertyEntry.@lsfusion.gwt.client.form.design.view.GReactFormData.ReactPropertyEntry::getValues(*)(qualified + ".getValues()", object === undefined ? null : object, value, mode == null ? null : mode, ok, fail, count == null ? 0 : count);
            }
        };
        // what the batch reads a member's state from (getMemberEntry): a property's is the one kind of member it
        // takes. Non-enumerable, so `for (var k in member)` still lists exactly its verbs
        Object.defineProperty(member, "__member", { value: "property" });
        Object.defineProperty(member, "__property", { value: propertyEntry });
        return member;
    }-*/;

    private void emitPropertyFacts(JavaScriptObject entry, GPropertyDraw property) {
        setField(entry, "type", GSimpleStateTableView.getJSTypeName(property.getRenderType(RendererType.SIMPLE)));
        // whether a user may sort by it - asked of the PROPERTY, so a panel property answers it too
        if (property.groupObject != null)
            setField(entry, "noSort", property.noSort);
    }

    // what the author typed, as a message names it: every controller entry point is given the member path it was
    // reached by ("g.change()", "g.qty.change()", "properties.change() entry 2"), so an error quotes the call that is
    // in the view's source instead of a verb name the surface no longer has
    public static String controllerPrefix(String surface) {
        return "controller." + surface + ": ";
    }
    private static final String CHANGE_ENTRY_FIELDS = "property,object,value";
    private static native boolean isOwnUndefined(JavaScriptObject object, String field) /*-{
        return Object.prototype.hasOwnProperty.call(object, field) && object[field] === undefined;
    }-*/;

    // ===== WHAT A STATE MEMBER READS - a condition (the user filters) and a sorting: the object the author wrote,
    // named by its `property`, read by one rule for both (readAuthorObject). Where the name is read is each one's own -
    // a sorting's on the node of the rows (RowsGroupNode.readStateProperty) - and a member reads its call as a
    // property member reads its own, and hands the form (Verbs) only what the call names
    private static final String ORDER_FIELDS = "property,desc";

    // an object the AUTHOR wrote - a condition, a sorting - named by its `property`: the three shape questions every
    // such object gets, and their four messages, once. An unknown field is refused rather than ignored because a typo'd
    // `value` would otherwise read as "no value" - remove the condition - and a `property` that is there but not a name
    // is not "no property"
    private static String readAuthorObject(String errorPrefix, JavaScriptObject item, String what, String fields) {
        String fieldList = fields.replace(",", ", ");
        if (!isJSObject(item))
            throw new RuntimeException(errorPrefix + what + " must be an object {" + fieldList + "}");
        String unknown = getUnknownField(item, fields);
        if (unknown != null)
            throw new RuntimeException(errorPrefix + "unknown field '" + unknown + "'; " + what + " has " + fieldList);
        String propertySID = getOwnString(item, "property");
        if (propertySID == null)
            throw new RuntimeException(errorPrefix + (hasOwnField(item, "property")
                    ? "'property' must be the property's name, as a string" : what + " has no 'property'"));
        return propertySID;
    }

    private static final String FILTER_CONDITION_FIELDS = "property,compare,value,negation,or";

    // a flag is a boolean or absent - a truthy string like "false" would otherwise mean true: a condition's `negation`
    // and `or`. A sorting's `desc` is not a flag: an order with none takes its sorting off (RowsGroupNode.applyOrder)
    private boolean readFlag(String errorPrefix, JavaScriptObject entry, String field) {
        if (!hasOwnField(entry, field)) // absent is false; anything PRESENT has to be the boolean it claims to be,
            return false;                 // `null` and `undefined` included - they are given, and they are not flags
        JavaScriptObject value = getOwnField(entry, field);
        if (!isJSBoolean(value))
            throw new RuntimeException(errorPrefix + "'" + field + "' must be true or false");
        return toBoolean(value);
    }

    // no value at all - absent, null, or the empty string an emptied input gives: how a view says "not filtered by
    // this" and "not sorted by this"
    private static native boolean isNoValue(JavaScriptObject v) /*-{ return v === undefined || v === null || v === ''; }-*/;

    private static void emitHidden(JavaScriptObject entry, boolean shown) {
        if (!shown) // an attribute like any other: there while it has a value
            setField(entry, "hidden", true);
    }

    private static void emitValue(JavaScriptObject entry, GPropertyDraw property, PValue value) {
        setField(entry, "value", GSimpleStateTableView.convertToJSValue(property, value, RendererType.SIMPLE, true));
    }

    private static void emitAttribute(JavaScriptObject entry, GPropertyReader reader, PValue pvalue, GComponent owner) {
        // the converter wants the property; a container / a group's own reader has none
        GPropertyDraw property = owner instanceof GPropertyDraw ? (GPropertyDraw) owner : null;
        Object dynamic = pvalue != null ? reader.getAttributeConverter().convert(pvalue, property) : null;
        Object value = isPresent(dynamic) ? dynamic : (owner != null ? reader.getStaticAttribute(owner) : null); // dynamic wins; else the static design default (also when a delivered image was cleared)
        String field = reader.getAttributeField(pvalue); // the no-value case is the reader's own default
        if (field != null && isPresent(value))
            setField(entry, field, value);
    }

}
