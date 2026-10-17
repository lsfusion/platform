package lsfusion.gwt.client.form.design.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.Event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lsfusion.gwt.client.base.FocusUtils;
import lsfusion.gwt.client.base.GwtClientUtils;
import static lsfusion.gwt.client.base.GwtClientUtils.*;
import lsfusion.gwt.client.GForm;
import lsfusion.gwt.client.GFormChanges;
import lsfusion.gwt.client.classes.data.GFileType;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.jsni.NativeSIDMap;
import lsfusion.gwt.client.base.jsni.NativeStringMap;
import lsfusion.gwt.client.form.controller.GController;
import lsfusion.gwt.client.form.controller.GFormController;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import lsfusion.gwt.client.classes.data.GIntegralType;
import lsfusion.gwt.client.form.GUpdateMode;
import lsfusion.gwt.client.form.filter.GFilterGroupController;
import lsfusion.gwt.client.form.filter.GRegularFilter;
import lsfusion.gwt.client.form.filter.GRegularFilterGroup;
import lsfusion.gwt.client.form.event.GInputBindingEvent;
import lsfusion.gwt.client.form.filter.user.controller.GFilterControlsController;
import lsfusion.gwt.client.form.object.table.controller.GGridToolbarController;
import lsfusion.gwt.client.form.object.table.controller.GListViewController;
import lsfusion.gwt.client.form.object.table.controller.GPlatformGridToolbarController;
import lsfusion.gwt.client.form.object.table.controller.GToolbarController;
import lsfusion.gwt.client.form.object.table.grid.controller.GGridController;
import lsfusion.gwt.client.form.object.table.grid.view.GListViewType;
import lsfusion.gwt.client.form.object.table.tree.controller.GTreeController;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.filter.user.GPropertyFilterDTO;
import lsfusion.gwt.client.form.filter.user.controller.GFilterInputController;
import lsfusion.gwt.client.form.filter.user.controller.GFilterControlsToggle;
import lsfusion.gwt.client.form.filter.user.controller.GPlatformFiltersController;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.table.grid.GGrid;
import lsfusion.gwt.client.form.object.table.grid.GGridProperty;
import lsfusion.gwt.client.form.object.table.tree.GTreeGroup;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GComponentController;
import lsfusion.gwt.client.form.object.table.controller.GGroupController;
import lsfusion.gwt.client.form.object.table.controller.GUserFiltersController;
import lsfusion.gwt.client.form.object.table.controller.GTableController;
import lsfusion.gwt.client.form.object.table.controller.GUserOrdersController;
import lsfusion.gwt.client.form.object.table.view.GGridPropertyTable;
import lsfusion.gwt.client.form.object.table.controller.GPlatformPropertyController;
import lsfusion.gwt.client.form.object.table.controller.GPropertyController;
import lsfusion.gwt.client.form.filter.user.GCompare;
import lsfusion.gwt.client.form.filter.user.GFilter;
import lsfusion.gwt.client.form.filter.user.view.GFiltersHandler;
import lsfusion.gwt.client.form.filter.user.GPropertyFilter;
import lsfusion.gwt.client.form.filter.user.view.GFilterEditors;
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
// entries - what the view changes them through. What React has of each group, property and component - its rows' part,
// its entry, its descriptor - is the form's controller of it (GFormController.initializeControllers): made here
// once, from the platform's controller of it, and handed all the form sends for it, which it keeps itself: the values
// of the readers it draws from (ReaderValues), its rows, whether the form shows it. The projection keeps the
// containers, and one flush for all of them.
// Placement is design data, and so is the SHAPE of every snapshot: an entry a container carries is there from the
// start, and whether the form shows it now is its `hidden`, never its presence. A part of a group is projected where
// its component is - the rows where the grid (or the tree) is, a panel property where the property is. The form level
// is the empty group: its node is each container's top level. Every other component React draws or places has a
// descriptor - its entry at that top level, keyed by its SID, where a property carried by name is keyed by that name -
// an lsf one included, which the platform draws and React places: an lsf ACTION's says only whether the form shows it,
// its caption and image being its buttons' face, which the platform draws.
// What an entry says is what the server sent, as the platform keeps it for its own components: nothing here decides
// that something is hidden or empties it. A property the form does not show is dropped by the server, and its entry has
// no value; a component the form does not show has its SHOWIF, and its descriptor says so - the rows of a hidden grid
// stay as the server last sent them, as the platform's own grid keeps them. Like any attribute, `hidden` is there only
// while it has a value: while the form does not show the entry's component.
// The model, top down: a container's state (ContainerState) and its nodes - its top level (TopNode), and an owner's
// node, a group's or a tree's (GroupNode, TreeNode) -; on an owner's node, one part for each of the owner's components
// the container draws (Part), and on a node the entries of the properties and components it carries (Entry); the
// members that mirror them (THE MEMBERS); and what a view's author wrote, read by one rule (WHAT A STATE MEMBER READS).
public class GReactFormData {
    private final GForm form;
    // GComponent.getReactPlace - handed in, so a test sees who asks it
    private final Function<GComponent, GContainer> place;
    private final Verbs verbs; // what the parts ask of the form (GFormController)
    private final NativeSIDMap<GContainer, ContainerState> containers = new NativeSIDMap<>();
    private NativeSIDMap<GContainer, ContainerState> pending = new NativeSIDMap<>();

    public GReactFormData(GForm form, Function<GComponent, GContainer> place, Verbs verbs) {
        this.form = form;
        this.place = place;
        this.verbs = verbs;
    }

    // WHAT A MEMBER DOES: the form's own edit, the one a user makes, or a request of the form's own that changes
    // nothing - a lookup, a count, an export -, which a batch does not carry (getRefusal); and what the parts ask of
    // the form besides: its sortings and filters as it has them, a filter's key bound, a part published later. The
    // members are made here, beside the entries they mirror, and each holds the state it stands for (a
    // ReactPropertyEntry, a Part): a call through one reads what it names - the row, the value - from that state, and
    // hands the form the edit, which the form shows at once and sends at once or, inside a batch, when the batch ends
    // (batch)
    public interface Verbs {
        // ===== the current object of a group whose rows a view draws: one of those rows
        void changeCurrentObject(GGroupObject group, GGroupObjectValue key);
        // ===== a property: its cell - its key (EMPTY: the current objects) and its value (PValue.UNDEFINED: its change
        // event, or the action) -, and the suggestion list of the cell, from the server's lookup `actionSID`
        void changeProperty(GPropertyDraw property, GGroupObjectValue key, PValue value);
        void getPropertyValues(GPropertyDraw property, GGroupObjectValue key, String value, String actionSID, JavaScriptObject successCallback, JavaScriptObject failureCallback, int increaseValuesNeededCount);
        // ===== the nodes of a tree whose rows a view draws: one of a group's rows opened ...
        void expandNode(GGroupObject group, GGroupObjectValue key);
        // ... or closed
        void collapseNode(GGroupObject group, GGroupObjectValue key);
        // ... every node of such a group and of the groups below it, opened ...
        void expandAll(GGroupObject group);
        // ... or closed
        void collapseAll(GGroupObject group);
        // ... and the tree's current node, with every node under it - what the tree's own member asks, and the
        // platform's toolbar's button over these rows (TreeTablePart): an edit, as expandAll is
        void expandCurrent(GGroupObject group, GGroupObjectValue key);
        // ===== the sortings of a table whose rows a view draws - a grid's group's, or a tree's, top down -, as its
        // member read them (OrdersPart.changeAll): the whole list, each sorting on the property it resolved, of that
        // property's group, true - ascending; each group's its own list, in its priority order, which the form shows at
        // once and sends, each group's as a group's is
        void changeOrders(List<GGroupObject> groups, LinkedHashMap<Column, Boolean> orders);
        // ... and a group's as the form has them - what one order its member states is merged into
        // (OrdersPart.changeOne), as the platform's header merges a click into them: the form's own map, read, never
        // changed
        LinkedHashMap<Column, Boolean> getOrders(GGroupObject group);
        // ===== the user filters of a FILTERS box's groups - a group's, or a tree's, top down -, as its filters part
        // states them: the whole list they are to be filtered by, each condition on the property it resolved, of that
        // property's group - each group's its own list, sent in one request, as the platform's panel sends them, the
        // request's index returned, which the panel's loading marks need and a view does not
        long changeFilters(List<GGroupObject> groups, ArrayList<GPropertyFilter> conditions);
        // ... and a group's as the form has them - what a view's part shows, and what one condition stated is merged
        // into: a list of the caller's own
        ArrayList<GPropertyFilter> getFilters(GGroupObject group);
        // ===== a filter group of a group: the filter selected in it - null: none -, as its member read it ...
        void changeRegularFilter(GRegularFilterGroup filterGroup, GRegularFilter filter);
        // ... and a filter's key, which selects it where a view draws the filter group (FilterGroupPart) - the
        // platform's binding, taken while the react container the view draws it in is shown and the form shows the
        // filter group (shown), as the platform's widget's is while the widget is shown
        int addRegularFilterBinding(GInputBindingEvent event, GFormController.BindingExec pressed, GContainer scope, Supplier<Boolean> shown, GGroupObject groupObject);
        // ===== what a toolbar asks where a view draws it - requests (getRefusal): a group's (GridToolbarPart), the
        // count of its rows and the sum of a column of them, answered or failed, ...
        void countRecords(GGroupObject group, Consumer<Number> answer, Consumer<Throwable> failed);
        void calculateSum(GGroupObject group, GPropertyDraw property, GGroupObjectValue columnKey, Consumer<Number> answer, Consumer<Throwable> failed);
        // ... the export of its rows, the server's report of them - and of a tree's, where a view draws the tree's
        // toolbar (TreeToolbarPart) ...
        void runTableReport(GGridProperty table);
        // ... and its update mode: manual, automatic, or the changes the server holds back brought now
        void changeUpdateMode(GGroupObject group, GUpdateMode updateMode);
        // ... and whether its update is manual, as the form keeps it (GFormController.isManualUpdate)
        boolean isManualUpdate(GGroupObject group);
        // ===== the form's own: why it refuses a request now, null where it does not - a batch runs, which carries
        // changes only, or the form is closed -: the reason the call fails with, as a server call on a controller does
        String getRefusal(String surface);
        // ... a part's state the client alone changed - no server's delta, which is published once applied, and no
        // optimistic operation, published once done -: published once the event that changed it is over
        void refreshReactLater();
        // ... and a view's batch: run, the edits made in it shown at once and sent together, in one request, when it
        // ends - when it throws too -, a call being made when it is made. A batch begun inside it is a part of it; a
        // server call on a controller, or a lookup, is refused while it runs
        void batch(Runnable run);
    }

    // a batch's fn (ContainerState.batch): a function, the member calls it makes being sent together
    private static void checkFunction(String errorPrefix, JavaScriptObject fn) {
        if (!isFunction(fn))
            throw new RuntimeException(errorPrefix + "takes a function, the member calls it makes being sent together");
    }
    // fn, run at once: what follows an await in it would be made after the batch has ended, so a Promise it returns is
    // refused - what it made before is sent. A custom grid view's batch runs its fn so too (GSimpleStateTableView)
    public static void runBatchFunction(String errorPrefix, JavaScriptObject fn) {
        if (isThenable(GwtClientUtils.call(fn)))
            throw new RuntimeException(errorPrefix + "the function returned a Promise: a batch is sent when it returns, so"
                    + " what follows an await in it would be made outside it. Await before the batch");
    }
    private static native boolean isFunction(JavaScriptObject value) /*-{ return typeof value === 'function'; }-*/;
    private static native boolean isThenable(JavaScriptObject value) /*-{
        return value != null && typeof value.then === 'function';
    }-*/;

    // a react container's state, made with its view, and its controller with it
    public ContainerState addContainer(GContainer scope, Consumer<JavaScriptObject> publish) {
        ContainerState state = new ContainerState(scope, publish);
        containers.put(scope, state);
        pending.put(scope, state); // the first server delta also publishes empty / static-only containers
        return state;
    }

    // ===== THE FORM'S CONTROLLER OF EACH OWNER, settled once, when every react container is there, from the platform's
    // controller of it (GFormController.initializeControllers): placement is design data. The groups first, as their
    // tables are set up - what draws the rows gives each group's controller and what shows the groups' user orders
    // (GTableController.getGroupController, getUserOrdersController) -, then the properties, in the form's order, the
    // order of the names a node carries, then the components, then the filter groups
    // ... of a property, ALL of it - its values and what labels it alike: an entry, keyed by its integration name on
    // the node of its group, where React draws it - none where it carries it by no name, the platform's own COUNT of a
    // list group, the pivot's: React keeps nothing of it, and nothing is sent for it; an entry over the platform's,
    // keyed the same way, where the platform draws it and React places it - a panel property React places, the column
    // of the rows React draws -, which keeps what labels the property and hands the rest on, an action's labels too,
    // its button's face; else the platform's
    public GPropertyController createPropertyController(GPropertyDraw property, GPlatformPropertyController platform) {
        GContainer scope = nameScope(property);
        if (scope == null)
            return platform;
        if (!hasEntry(property)) // ... and where it has no entry, it brings no node either
            return noEntry(property, platform);
        ContainerState state = containers.get(scope);
        if (property.isList) { // its content is its column, with its group's rows, on their node
            RowsPart rows = state.getRowsPart(property.groupObject);
            if (!property.isLsfView())
                return new ReactColumnPropertyEntry(rows, property);
            return property.isAction() ? new LsfColumnActionEntry(rows, property, platform) : new LsfColumnPropertyEntry(rows, property, platform);
        }
        // a panel property is a part of its group: on the group's node here, the empty group's on the top level
        Node node = property.groupObject == null ? state.top : state.getOrCreateGroupNode(property.groupObject);
        if (!property.isLsfView())
            return new ReactPanelPropertyEntry(node, property);
        return property.isAction() ? new LsfPanelActionEntry(node, property, platform) : new LsfPanelPropertyEntry(node, property, platform);
    }
    // whether a property a view names has an entry there: one it carries by a name - an lsf ACTION too, whose entry
    // says whether the form shows it -, not the one React draws by none, the group's own COUNT. Any other has a name,
    // the form being refused without one (FormView.checkReactProjectionNames). A group's node is where an entry of it
    // is (mirrors FormView.hasEntry)
    private static boolean hasEntry(GPropertyDraw property) {
        return property.integrationSID != null;
    }
    // ... and what the COUNT has instead: nothing, React keeping nothing of it - the platform's controller, where the
    // platform draws it
    private static GPropertyController noEntry(GPropertyDraw property, GPlatformPropertyController platform) {
        return property.isLsfView() ? platform : null;
    }
    // ... of a component: none of what React keeps nothing of (reactKeepsNothing). A property's is the platform's,
    // whatever draws the property - a property is no owner as a component, all of it being its property controller's
    // (above), and the one thing sent for it as a component, its class (a design showIf on a property is the
    // property's SHOWIF), is the platform's to apply to the view it draws the property in: none where React draws it.
    // Of any other: of one React draws, all that is sent for it; of an lsf one React places, its descriptor - its labels
    // and its SHOWIF - where the design names the component, over the platform's; of one it places by no name,
    // nothing: all of it is the platform's. `platform` is the platform's controller of the component: the layout
    public GComponentController createComponentController(GComponent component, GComponentController platform) {
        if (reactKeepsNothing(component))
            return null;
        GContainer scope = component instanceof GPropertyDraw ? null : nameScope(component);
        return reactOrPlatform(scope, state -> component.isLsfView() ? new LsfComponentEntry(state, component, platform) : new ReactComponentEntry(state, component), () -> platform);
    }
    // ... and WHAT REACT KEEPS NOTHING OF as a component, so nothing is sent for it as one (mirrors
    // FormView.reactKeepsNothing): a component React draws that the design does not name - no descriptor, no entry to
    // go to
    private boolean reactKeepsNothing(GComponent component) {
        return component.sID == null && !component.isLsfView() && placed(component) != null;
    }
    // ... and the controller of a FILTER GROUP of a group: none of one with no filters, which has no view of either
    // kind; its part, a part of the group, on its node where FILTERGROUP(fg) is drawn (filterGroupScope), else the
    // platform's check box or list
    public GFilterGroupController createFilterGroupController(GRegularFilterGroup filterGroup, Supplier<GFilterGroupController> platform) {
        if (filterGroup.filters.isEmpty())
            return null;
        return reactOrPlatform(filterGroupScope(filterGroup), state -> new FilterGroupPart(state.getOrCreateGroupNode(filterGroup.groupObject), filterGroup), platform);
    }
    // ===== WHAT GOES OVER THE ROWS of a list's group not in a tree, or of a tree, one for all its groups, each of the
    // kind of whoever draws it, made where the form sets the table up (GFormController.initializeGrid,
    // initializeTree), with what draws the rows (createGridController, createTreeController)
    // ... a group's TOOLBAR's controller: its part, a part of the group of its own, on its node where TOOLBARSYSTEM(g)
    // is drawn, shown (toolbarScope), else the platform's, made only then
    public GGridToolbarController createGridToolbarController(GGrid grid, Supplier<GGridToolbarController> platform) {
        return reactOrPlatform(toolbarScope(grid), state -> new GridToolbarPart(state.getOrCreateGroupNode(grid.groupObject)), platform);
    }
    // ... and a TREE's toolbar's controller, one for all its groups: its part, a part of the tree, on the tree's node
    // where TOOLBARSYSTEM(TREE t) is drawn, shown (toolbarScope), else the platform's, made only then
    public GToolbarController<GTreeController> createTreeToolbarController(GTreeGroup tree, Supplier<GToolbarController<GTreeController>> platform) {
        return reactOrPlatform(toolbarScope(tree), state -> new TreeToolbarPart(state.getOrCreateTreeNode(tree)), platform);
    }
    // ... and the controller of a TABLE's FILTER CONTROLS: their part, a part of the table, on the table's node where
    // FILTERCONTROLS is drawn (filterControlsScope), there whether the platform builds a filter input controller to
    // give their commands or not, else the platform's buttons
    public GFilterControlsController createFilterControlsController(GGridProperty table, Supplier<GFilterControlsController> platform) {
        return reactOrPlatform(filterControlsScope(table), state -> new FilterControlsPart(state.getOrCreateTableNode(table)), platform);
    }
    // ... and the controllers of what the FILTERS box of a table holds - the USER FILTERS of its groups, the grid's
    // group or the tree's, one box for all of them -, both of the one owner of the box's content (filtersScope): where
    // a view draws it, the commands over the conditions it draws, which take the platform's input - the keys of the
    // rows, the controls, a pivot's drill-down - (ReactFilterInputController), and their part, which shows the
    // conditions, on the table's node there (FiltersPart); else the platform's filter panel, both in one, where it
    // builds one (platform), made only then - else neither: no input, and nothing shows them
    // (GUserFiltersController.NONE). The input works on what draws the rows (rows), and the toggle shows the controls
    // and the toolbar's button (GFormController.initializeTable); a group in PANEL view has neither, as the platform
    // shows it none
    public Pair<GFilterInputController, GUserFiltersController> createFiltersControllers(GGridProperty table, GTableController rows, GFilterControlsToggle toggle, Supplier<GPlatformFiltersController> platform) {
        return reactOrPlatform(filtersScope(table.filtersContainer), state -> new Pair<GFilterInputController, GUserFiltersController>(
                new ReactFilterInputController(table.getGroups(), rows, toggle), new FiltersPart(state.getOrCreateTableNode(table))), () -> {
            GPlatformFiltersController panel = platform.get();
            return new Pair<GFilterInputController, GUserFiltersController>(panel, panel != null ? panel : GUserFiltersController.NONE);
        });
    }
    // WHAT DRAWS THE ROWS of a TABLE, the form's one question for them, asked with its toolbar
    // (GFormController.initializeGrid, initializeTree): of the GRID of a list's group not in a tree, where React draws
    // them, its table's part on the group's node there, with its group's rows' part ...
    public GGridController createGridController(GGrid grid, GToolbarController<GGridController> toolbar, Supplier<GGridController> platform) {
        return createTableController(grid, toolbar, state -> state.createGridTablePart(grid), platform);
    }
    // ... of a TREE, one for all its groups: where React draws them, its table's part on the tree's node there
    // (FormView.getTreeScopes), with the rows' part of each of its groups, which it keeps ...
    public GTreeController createTreeController(GTreeGroup tree, GToolbarController<GTreeController> toolbar, Supplier<GTreeController> platform) {
        return createTableController(tree, toolbar, state -> state.createTreeTablePart(tree), platform);
    }
    // ... either: the table's part, of the table's kind (part), given to the toolbar as it is made, as the platform's
    // table gives itself to it, where React draws the rows - where the table is drawn -, else the platform's table
    // (platform), made only then
    private <R extends GTableController> R createTableController(GGridProperty table, GToolbarController<R> toolbar, Function<ContainerState, R> part, Supplier<R> platform) {
        return reactOrPlatform(componentScope(table), state -> {
            R rows = part.apply(state);
            toolbar.addRows(rows);
            return rows;
        }, platform);
    }
    // ... the one choice each of the factories above makes: what React has of a component it draws in a react container
    // (scope) - a part of its owner there, or an entry, made by react -, else the platform's (platform), made only then
    private <C> C reactOrPlatform(GContainer scope, Function<ContainerState, C> react, Supplier<C> platform) {
        return scope != null ? react.apply(containers.get(scope)) : platform.get();
    }
    // THE ROWS A GROUP'S PER-ROW RENDERERS FOLLOW (GPlatformRowsPanelController): a group's lsf list properties are
    // drawn per row only where React draws the group's rows (FormView.checkLsfListView), so what they follow is the
    // rows' part
    public interface Rows {
        // its rows as the view has them, its own add or remove included - the part's own list, which it replaces and
        // never changes: a list handed out keeps saying what it said
        ArrayList<GGroupObjectValue> getRows();
    }
    // ... of a group whose rows React draws, handed out once, while the form is built: placement is asked here, and
    // then never again
    public Rows getRows(GGroupObject group) {
        return containers.get(rowsScope(group)).getRowsPart(group);
    }
    // ... and then what the nodes carry of it all, once: a group's rows are there from the start, none of them yet; the
    // names every node carries; the members that mirror its entries
    public void initialize() {
        containers.foreachValue(ContainerState::initialize);
    }

    // THE VALUES OF AN OWNER'S READERS, as the server last sent them, with the client's own on top - kept by the owner
    // that draws from them: an entry its own, a group's rows' part the group's
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
        // ... and all its values, by key: null for a reader none are kept of
        NativeHashMap<GGroupObjectValue, PValue> get(GPropertyReader reader) {
            return values.get(reader);
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
        // each group's and each tree's node here, by its name - data.<name> -, in the order they are made: the server
        // keeps the names of the groups and trees a container carries apart (FormView.checkReactProjectionNames)
        final NativeStringMap<OwnerNode> nodes = new NativeStringMap<>();
        // ... and the TABLES whose rows are drawn here, each a part on the table's node with the rows' parts of its
        // groups (createGridTablePart, createTreeTablePart): what a list property's column goes with, what answers for
        // an lsf property drawn per row (getRowsPart), and what places what its rows state about one another (commit)
        final NativeSIDMap<GGridProperty, TablePart<?, ?>> tableParts = new NativeSIDMap<>();
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
        // ... or only its commit, which may write or not: in the batch, nothing copied yet (TreeTablePart.touch)
        void enlist() {
            pending.put(scope, this);
        }
        // the draft becomes the data - what the rows of each table drawn here state about one another placed in it
        // first, from the completed batch (TablePart.place)
        void commit() {
            tableParts.foreachValue(TablePart::place);
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

        // the group's node here, where its parts and entries drawn here go, made the first time it is asked for - by
        // its name, a group's node under it
        GroupNode getOrCreateGroupNode(GGroupObject group) {
            return (GroupNode) getOrCreateNode(group.getSID(), () -> new GroupNode(this, group));
        }
        // ... a tree's, made the first time it is asked for, the same way
        TreeNode getOrCreateTreeNode(GTreeGroup tree) {
            return (TreeNode) getOrCreateNode(tree.name, () -> new TreeNode(this, tree));
        }
        private OwnerNode getOrCreateNode(String name, Supplier<OwnerNode> made) {
            OwnerNode node = nodes.get(name);
            if (node == null) {
                node = made.get();
                nodes.put(name, node);
            }
            return node;
        }
        // ... and a TABLE's: a grid's is its group's, a tree's the tree's
        OwnerNode getOrCreateTableNode(GGridProperty table) {
            return table.match(grid -> getOrCreateGroupNode(grid.groupObject), this::getOrCreateTreeNode);
        }
        // ... and a TABLE whose rows are drawn here, a part on the table's node, with the ROWS of its groups, each a
        // part of its group on its node, of the kind the rows' maker asks for, once: a grid's, with its group's rows
        // (createGridController) ...
        GridTablePart createGridTablePart(GGrid grid) {
            GridTablePart table = new GridTablePart(getOrCreateGroupNode(grid.groupObject));
            table.rowsParts.put(grid.groupObject, new GridRowsPart(table.node));
            tableParts.put(grid, table);
            return table;
        }
        // ... or a tree's, with the rows of each of its groups, top down, each linked with the group above
        // (TreeRowsPart) (createTreeController)
        TreeTablePart createTreeTablePart(GTreeGroup tree) {
            TreeTablePart table = new TreeTablePart(getOrCreateTreeNode(tree));
            for (GGroupObject group : tree.groups)
                table.rowsParts.put(group, new TreeRowsPart(getOrCreateGroupNode(group), table));
            tableParts.put(tree, table);
            return table;
        }
        // ... the rows' part of a group whose rows are drawn here, its table's - none of a group whose rows are not
        RowsPart getRowsPart(GGroupObject group) {
            TablePart<?, ?> table = tableParts.get(group.getDrawComponent());
            return table != null ? table.rowsParts.get(group) : null;
        }
        Node nodeOf(GGroupObject group) {
            return group == null ? top : nodes.get(group.getSID());
        }
        // what each node sets out once - what each part of it writes, its rows none of them yet, the names it carries
        // and the members of its parts and entries: they are design data, as the entries are -, in the order they are
        // made; then the empty group's members, on the controller itself
        void initialize() {
            nodes.foreachValue(OwnerNode::initialize);
            top.fillController();
        }

        // the lsf list property whose per-row renderers an <Lsf> places, named as a panel property is - its group, then
        // its integration name, o.qty, the path of its column entry -, of a group whose rows this container draws:
        // asked of the rows' parts, not of the container's children, such a property being no child of it
        // (FormView.checkLsfListView). The row an <Lsf> gives says which renderer, and has to be a row of that group
        public GPropertyDraw getRowLsfProperty(String name) {
            int dot = name.lastIndexOf('.');
            if (dot < 0) // a grid property always has a group
                return null;
            GGroupObject group = form.getGroupObject(name.substring(0, dot));
            RowsPart rows = group != null ? getRowsPart(group) : null;
            return rows != null ? rows.getRowLsfProperty(name.substring(dot + 1)) : null;
        }

        // controller.batch(fn): fn run in the form's batch (Verbs.batch), which sends what it makes when it returns -
        // what was made before a throw too, as it would have gone outside a batch. fn changes the form through this
        // view's members: a member of another view called in it goes with this batch
        void batch(JavaScriptObject fn) {
            String errorPrefix = controllerPrefix("batch()");
            checkFunction(errorPrefix, fn);
            verbs.batch(() -> runBatchFunction(errorPrefix, fn));
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
        final GComponentController platform; // the platform's controller of the component: the layout, which draws it

        LsfComponentEntry(ContainerState state, GComponent component, GComponentController platform) {
            super(state, component);
            this.platform = platform;
        }
        // the rest of it is the platform's, which draws it
        public void updateOther(GComponentReader reader, NativeHashMap<GGroupObjectValue, PValue> values, boolean partial) {
            platform.updateOther(reader, values, partial);
        }
    }

    // a node of the projection in one container - a group's or a tree's, the empty group's being the container's top
    // level, any other's data.<name>: what it writes into `data` (edit), and its member of the controller, as its
    // entry is in `data` (fillController). The empty group's node and a group's carry the entries of the group's
    // properties by name, and on their member the members that mirror those entries, one per value they carry - a
    // property is of a group, so a tree's node carries none. One JS object holds all it writes, and so does one node
    private abstract class Node {
        final ContainerState state;
        final ArrayList<PropertyEntry> properties = new ArrayList<>(); // what it carries by name, in the form's order

        Node(ContainerState state) {
            this.state = state;
        }
        // ... and on a member, the members that mirror them
        final void putPropertyMembers(JavaScriptObject member) {
            for (PropertyEntry propertyEntry : properties)
                propertyEntry.putMember(member);
        }
        abstract JavaScriptObject edit();
        abstract void fillController();
        // its object in a snapshot of the container's data, null where the snapshot has none
        abstract JavaScriptObject in(JavaScriptObject snapshot);
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
    // the node of an OWNER of parts - a group, a tree -: data.<name> - its OWN field: a plain object answers for
    // Object.prototype's names before any node is written under one - and its member controller.<name>: the parts of
    // the owner placed on it (Part), each writing its own there
    private abstract class OwnerNode extends Node {
        final String name; // the owner's name in the form: data.<name>, controller.<name>
        final ArrayList<Part<?>> parts = new ArrayList<>(); // in the order they are placed
        final JavaScriptObject member = newObject(); // controller.<name>, put on the controller with its members
        final NativeStringMap<Boolean> unnameable = new NativeStringMap<>(); // reported already (reportUnnameable)

        OwnerNode(ContainerState state, String name) {
            super(state);
            this.name = name;
        }
        JavaScriptObject current() {
            return in(state.current());
        }
        JavaScriptObject published() {
            return in(state.data);
        }
        JavaScriptObject in(JavaScriptObject snapshot) {
            return getOwnField(snapshot, name);
        }
        JavaScriptObject edit() {
            JavaScriptObject node = current();
            if (node == published()) {
                node = node == null ? newObject() : copyKeepingPrototype(node);
                setField(state.edit(), name, node);
            }
            return node;
        }
        // controller.<name>: its member, and on it the members of its parts
        void fillController() {
            setField(state.controller, name, member);
            for (Part<?> part : parts)
                part.putMember(member);
        }
        // what it sets out once: what its parts write - its rows, none of them yet -, the members on the controller
        void initialize() {
            for (Part<?> part : parts) {
                part.initialized = true;
                part.initialize();
            }
            fillController();
        }
        // what the owner is, as a message names it
        abstract String describe();
        // the groups of the owner, in the order their conditions and properties are listed (FiltersPart): the group,
        // or the tree's, top down
        abstract List<GGroupObject> getGroups();
        // ... a property of them as the owner's lists name it - its user filters' conditions and what they can be on -,
        // a call naming it back so
        abstract String getPropertyName(GPropertyDraw property);
        // ... and the property such a name names - refused where there is none
        abstract GPropertyDraw readPropertyName(String errorPrefix, String name);
        // a property one of the owner's lists leaves out, as the projection does not name it there - for the reason
        // given: reported once per list and property
        void reportUnnameable(GPropertyDraw property, String field, String what, String why) {
            String name = property.sID;
            String key = field + ":" + name;
            if (unnameable.get(key) == null) {
                unnameable.put(key, Boolean.TRUE);
                GwtClientUtils.logLsfViewError("data." + this.name + "." + field + ": " + describe() + " is " + what + " by '"
                        + name + "', which the projection does not name here (" + why + "), so that one is not listed");
            }
        }
    }
    // a group's node: data.<group>, with the entries of the group's properties it carries by name
    private final class GroupNode extends OwnerNode {
        final GGroupObject group;

        GroupNode(ContainerState state, GGroupObject group) {
            super(state, group.getSID());
            this.group = group;
        }
        String describe() {
            return "the group";
        }
        List<GGroupObject> getGroups() {
            return Collections.singletonList(group);
        }
        // a property of the group, by its integration name; the group's one property of that name, given back
        // (readStateProperty)
        String getPropertyName(GPropertyDraw property) {
            return property.integrationSID;
        }
        GPropertyDraw readPropertyName(String errorPrefix, String name) {
            return readStateProperty(errorPrefix, group, name);
        }
        // ... and on its member, after its parts', the members of the entries it carries
        @Override
        void fillController() {
            super.fillController();
            putPropertyMembers(member);
        }
        // ... and data.<group>.properties: the names of the entries it carries, in the form's order; design data, like
        // them
        @Override
        void initialize() {
            super.initialize();
            JavaScriptObject names = emptyArray();
            for (PropertyEntry propertyEntry : properties)
                push(names, propertyEntry.property.integrationSID);
            setField(edit(), "properties", names);
        }
    }
    // a TREE's node: data.<tree>, by the tree's name in the form - TREE t's t -, where what is the tree's, one for all
    // its groups, goes: where React draws its rows, its grid's part - the tree's current row and its own verb -, as a
    // group's node is where the group's rows are; and its user filters, its toolbar and its filter controls (GRID,
    // FILTERS, TOOLBARSYSTEM and FILTERCONTROLS(TREE t)). Its groups have nodes of their own, which their rows and
    // properties go on; a property is of a group, so this node carries none
    private final class TreeNode extends OwnerNode {
        final GTreeGroup tree;

        TreeNode(ContainerState state, GTreeGroup tree) {
            super(state, tree.name);
            this.tree = tree;
        }
        String describe() {
            return "the tree";
        }
        List<GGroupObject> getGroups() {
            return tree.groups;
        }
        // a property of a group of the tree, by its group's name and its integration name, c.name, as an <Lsf> names a
        // property of a group - each of the group of its property, in the tree's order of the groups, as the
        // platform's panel shows them and a list a view states is sent, each group's its own
        String getPropertyName(GPropertyDraw property) {
            return getName(property);
        }
        // ... and a name a view gives back: split at its FIRST dot - a group's name has none
        // (FormView.checkProjectedGroupSID), an integration name may: a property the platform draws is asked to have
        // none by nothing (FormView.checkProjectedName) -, a group of the tree, and its one property of that name
        GPropertyDraw readPropertyName(String errorPrefix, String name) {
            int dot = name.indexOf('.');
            String groupSID = dot >= 0 ? name.substring(0, dot) : null;
            for (GGroupObject group : tree.groups)
                if (group.getSID().equals(groupSID))
                    return readStateProperty(errorPrefix, group, name.substring(dot + 1));
            throw new RuntimeException(errorPrefix + "'" + name + "' names no property of a group of tree '" + this.name
                    + "': a condition names one by its group's name and its own, " + tree.groups.get(0).getSID() + ".<name>");
        }
    }
    // a PART on a node - of a GROUP: its rows, its filter groups; of a TABLE, on the table's node - a list group's, or
    // the tree's -: its rows as a whole, its sortings, user filters, filter controls and toolbar -, placed in the
    // container where its component is: its state, what it writes on the node, and its member, which goes on the node's
    // member there (OwnerNode.fillController). It is the form's controller of what it shows
    private abstract class Part<N extends OwnerNode> {
        final N node;
        // whether it is set out: what it is told before - while the platform builds what it is a part of - it neither
        // writes nor publishes, initialize writing it all
        boolean initialized;

        Part(N node) {
            this.node = node;
            node.parts.add(this);
        }
        // what it writes, set out once - there from the start -, when every part and entry of the form is made
        abstract void initialize();
        // its member, on the node's member
        abstract void putMember(JavaScriptObject ownerMember);
    }
    // ... whose STATE is written whole (writeState): there from the start, and anew as it changes - in a delta,
    // published with it, or by the client alone, a member or as the platform's buttons would, once it is set out,
    // published once the event is over (stateChanged): a table's toolbar (ToolbarPart) and filter controls
    // (FilterControlsPart), a group's filter group (FilterGroupPart)
    private abstract class StatePart<N extends OwnerNode> extends Part<N> {
        StatePart(N node) {
            super(node);
        }
        @Override
        void initialize() {
            writeState();
        }
        abstract void writeState();
        void stateChanged() {
            if (!initialized)
                return;
            writeState();
            verbs.refreshReactLater();
        }
    }
    // ... whose state is a LIST, under its name on the node (field) - a table's sortings (OrdersPart), its user
    // filters (FiltersPart) -, always there, empty while there is none, and whose member, beside the table's own,
    // states it (makeListMember): all of it, by an array, or one item, merged into the rest
    private abstract class ListPart extends Part<OwnerNode> {
        final String field; // data.<name>.<field>, controller.<name>.<field>

        ListPart(OwnerNode node, String field) {
            super(node);
            this.field = field;
        }
        @Override
        void initialize() {
            setField(node.edit(), field, emptyArray());
        }
        @Override
        void putMember(JavaScriptObject ownerMember) {
            setField(ownerMember, field, makeListMember(this, node.name + "." + field + ".change()"));
        }
        // ... a call through it: an array states the items all at once, anything else one of them, merged into the rest
        void change(String surface, JavaScriptObject items) {
            if (GSimpleStateTableView.isJSArray(items))
                changeAll(surface, items);
            else
                changeOne(surface, items);
        }
        abstract void changeAll(String surface, JavaScriptObject items);
        abstract void changeOne(String surface, JavaScriptObject item);
    }
    // a TABLE's USER ORDERS where React draws its rows - of a list's group not in a tree, or of a tree, one list for
    // all its groups: a part of the table of their own, as if a table's design had a box of its sortings, as FILTERS is
    // of its filters - placed on the table's node where its rows are drawn, a sorting being the order those are drawn
    // in, made with the rows' part of the table (TablePart). `orders` lists the sortings of the table's groups, group
    // by group, top down, each in its priority order - a group's sortings are its own: no group's outrank another's -,
    // each property named as the node names it (OwnerNode.getPropertyName). It is rebuilt only when the form hands over
    // another list of a group - so it keeps its identity across renders in which nothing sorts the table anew. The
    // lists are the form's (Verbs.getOrders): the part keeps only what it publishes
    private final class OrdersPart extends ListPart implements GUserOrdersController {
        OrdersPart(OwnerNode node) {
            super(node, "orders");
        }
        // a group's sortings as the form has them now: its own ORDERS in the first changes, then every list the server
        // reports - an ORDER action's too -, handed over only when it is another one; the other groups' as the form
        // has them too
        @Override
        public void updateOrders(GGroupObject group, LinkedHashMap<Column, Boolean> orders) {
            writeOrders();
        }
        // ... and as the form changes them - its member's, or the platform's header's -: shown at once, the same way
        @Override
        public void changeOrders(GGroupObject group, LinkedHashMap<Column, Boolean> orders) {
            writeOrders();
        }
        // `orders`, the sortings in the SAME shape the .lsf ORDER operator reads and writes: the property as the node
        // names it, and `desc`, the server's and the operator's spelling (the client keeps ascending). A sorting is
        // named as a condition is, wherever its property is drawn - here, in another container, by the platform
        // (OwnerNode.readPropertyName) -, so what a view reads it can hand back. A sorting on what no sorting can name
        // is not listed (isProjectableSorting): it would describe the rows by something nothing can address
        void writeOrders() {
            JavaScriptObject array = emptyArray();
            for (GGroupObject group : node.getGroups())
                for (Map.Entry<Column, Boolean> order : verbs.getOrders(group).entrySet()) {
                    GPropertyDraw property = order.getKey().property;
                    if (!isProjectableSorting(property)) {
                        node.reportUnnameable(property, "orders", "sorted", whyNotSorting(property));
                        continue;
                    }
                    JavaScriptObject entry = newObject();
                    setField(entry, "property", node.getPropertyName(property));
                    setField(entry, "desc", !order.getValue());
                    push(array, entry);
                }
            setField(node.edit(), "orders", array);
        }
        // ... and the sortings of all its groups as the form has them, one map, group by group
        LinkedHashMap<Column, Boolean> getOrders() {
            LinkedHashMap<Column, Boolean> orders = new LinkedHashMap<>();
            for (GGroupObject group : node.getGroups())
                orders.putAll(verbs.getOrders(group));
            return orders;
        }

        // ===== what its member reads: a sorting is the state of the rows, so its member is where they are drawn - a
        // view that does not draw them has none. What it is: an object the author wrote, carrying only fields this call
        // knows, naming a property of the table's groups - by the name `orders` gives it, not by a member: a state is
        // data, written in the shape the projection publishes it. A call names what it changes by its member
        // (ReactPropertyEntry.change). The name is the node's (OwnerNode.readPropertyName), and one a sorting can name
        // (isProjectableSorting)
        GPropertyDraw readSortingProperty(String errorPrefix, JavaScriptObject order) {
            GPropertyDraw property = node.readPropertyName(errorPrefix, readAuthorObject(errorPrefix, order, "an order", ORDER_FIELDS));
            if (!isProjectableSorting(property))
                throw new RuntimeException(errorPrefix + "'" + node.getPropertyName(property) + "' cannot be sorted by here: "
                        + whyNotSorting(property));
            return property;
        }

        // ===== a call through its member: the table's sortings, as the view states them, read here - with the rows -
        // and handed to the form as the list they make, which the form splits into its groups' lists, each of the group
        // of its properties (Verbs.changeOrders). An order is {property, desc}, the SAME shape `orders` projects and
        // the .lsf ORDER operator reads; one with no `desc` is not a sorting, so it removes the one on that property.
        // An array states them all, its order being each group's priority - a group none of it names is sorted by
        // nothing then
        @Override
        void changeAll(String surface, JavaScriptObject orders) {
            String errorPrefix = controllerPrefix(surface);

            LinkedHashMap<Column, Boolean> result = new LinkedHashMap<>();
            // every property the list NAMES, direction or not, once: a group is sorted by one once, and the server keys
            // them by it - so a list naming one twice describes no sorting a group can have, whichever of the two
            // carries a direction
            NativeSIDMap<GPropertyDraw, Boolean> named = new NativeSIDMap<>();
            int size = GSimpleStateTableView.jsArrayLength(orders);
            for (int i = 0; i < size; i++) {
                JavaScriptObject order = GSimpleStateTableView.jsArrayGet(orders, i);
                String prefix = errorPrefix + "order " + i + ": ";
                GPropertyDraw property = readSortingProperty(prefix, order);
                if (named.put(property, true) != null)
                    throw new RuntimeException(prefix + "'" + node.getPropertyName(property) + "' is already in this list");
                applyOrder(prefix, order, property, result);
            }
            verbs.changeOrders(node.getGroups(), result);
        }
        // ... one order, merged into the table's sortings as the form has them - every one of them, those it does not
        // name included, and those an earlier call of a batch stated
        @Override
        void changeOne(String surface, JavaScriptObject order) {
            String errorPrefix = controllerPrefix(surface);
            LinkedHashMap<Column, Boolean> result = getOrders();
            applyOrder(errorPrefix, order, readSortingProperty(errorPrefix, order), result);
            verbs.changeOrders(node.getGroups(), result);
        }
        // what one order the author wrote does to sortings: with no direction it is not a sorting, and takes the one on
        // its property off - which a noSort property has to be able to say, as a default ORDERS on one is listed; with
        // a direction it sorts by the property - refused for a noSort one - in place when the property is sorted
        // already (a map keeps a key where it is), appended when not. `desc` is read once, as every field of what an
        // author wrote is
        void applyOrder(String errorPrefix, JavaScriptObject order, GPropertyDraw property, LinkedHashMap<Column, Boolean> result) {
            Column column = new Column(property, GGroupObjectValue.EMPTY); // a sorting names none in columns
            JavaScriptObject desc = getOwnField(order, "desc");
            if (isNoValue(desc)) {
                result.remove(column);
                return;
            }
            if (!isJSBoolean(desc))
                throw new RuntimeException(errorPrefix + "'desc' must be true or false");
            if (property.noSort)
                throw new RuntimeException(errorPrefix + "'" + node.getPropertyName(property) + "' cannot be sorted by");
            result.put(column, !toBoolean(desc));
        }
    }
    // the USER FILTERS where React draws them - what a FILTERS box holds: a part of their own, of the group the box is
    // of or of the tree, one box for all its groups, on the table's node where the box's content is - the node a
    // group's rows are drawn on when the two are in one container -, the conditions of the table's groups, each on a
    // property named as the node names it (OwnerNode.getPropertyName, readPropertyName). `filters` is what the
    // server is filtering by: the form tells the part whenever a group's list changes - it sends one, or the server
    // reports one -, and the array is rebuilt only then, from the lists the form has (Verbs.getFilters) - so it keeps
    // its identity across renders in which nothing states them anew. The part keeps only what it publishes.
    // It is what shows the user filters of the box (GUserFiltersController), the view's kind of it, the platform's
    // panel being the other, there whether the platform wires a filter input controller up or not: the commands over
    // them, where it does, are an object of their own (ReactFilterInputController), and a list they state reaches the
    // part as any list the form sends does
    private final class FiltersPart extends ListPart implements GUserFiltersController {
        FiltersPart(OwnerNode node) {
            super(node, "filters");
        }
        // ... and what a condition can be on, design data written once (filterProperties)
        @Override
        void initialize() {
            super.initialize();
            setField(node.edit(), "filterProperties", getFilterProperties(node));
        }
        // a group's list changed, whoever changed it - the server, or the form sending it: the form has it now, and a
        // view shows it either way, the other groups' as the form has them too
        @Override
        public void updateFilters(GGroupObject group, ArrayList<GPropertyFilterDTO> filters) {
            writeFilters();
        }
        @Override
        public void changeFilters(GGroupObject group, ArrayList<GPropertyFilterDTO> filters) {
            writeFilters();
        }

        // the conditions, in the SAME shape the .lsf FILTER operator reads them - compare by its operator
        // string, `or` for the junction - so one vocabulary describes a condition in .lsf and in the projection; only
        // the property is named as the node names it (getPropertyName), where FILTER names it by its name on
        // the form (they differ under an EXTID). A condition on what no condition can be on is not listed
        // (isProjectableCondition): it would describe what is filtered by something nothing can address
        void writeFilters() {
            JavaScriptObject array = emptyArray();
            for (GPropertyFilter condition : getConditions(node.getGroups())) {
                GPropertyDraw property = condition.column.property;
                if (!isProjectableCondition(property)) {
                    node.reportUnnameable(property, "filters", "filtered", whyNotCondition(property));
                    continue;
                }
                JavaScriptObject entry = newObject();
                setField(entry, "property", node.getPropertyName(property));
                setField(entry, "compare", condition.compare.toString());
                // the FILTER type, the one the platform's own filter editor uses for this cell
                // - and the one a condition is read back with (readFilterCondition)
                // - and no property: one named `image` draws its own value as an image, while a condition's is plain
                setField(entry, "value", GSimpleStateTableView.convertToJSValue(property.getFilterBaseType(), null, true, condition.value.value));
                setField(entry, "negation", condition.negation);
                // GPropertyFilter.junction is the conjunction flag; .lsf says `or`
                setField(entry, "or", !condition.junction);
                push(array, entry);
            }
            setField(node.edit(), "filters", array);
        }

        // the property a condition the AUTHOR wrote names (readAuthorObject), by the name `filters` gives it - one a
        // condition can be on (isProjectableCondition)
        GPropertyDraw readFilterProperty(String errorPrefix, JavaScriptObject condition) {
            GPropertyDraw property = node.readPropertyName(errorPrefix, readAuthorObject(errorPrefix, condition, "a condition", FILTER_CONDITION_FIELDS));
            if (!isProjectableCondition(property))
                throw new RuntimeException(errorPrefix + "'" + node.getPropertyName(property) + "' cannot be filtered by here: "
                        + whyNotCondition(property));
            return property;
        }
        // a call through its member, read here, on the part that holds the conditions (readFilterCondition), and handed
        // to the form as what it names
        @Override
        void changeAll(String surface, JavaScriptObject conditions) {
            String errorPrefix = controllerPrefix(surface);
            ArrayList<GPropertyFilter> result = new ArrayList<>();
            int size = GSimpleStateTableView.jsArrayLength(conditions);
            for (int i = 0; i < size; i++) {
                String itemPrefix = errorPrefix + "condition " + i + ": ";
                JavaScriptObject condition = GSimpleStateTableView.jsArrayGet(conditions, i);
                GPropertyDraw property = readFilterProperty(itemPrefix, condition);
                JavaScriptObject value = getOwnField(condition, "value"); // read once: a condition is what was read
                if (isNoValue(value)) // no value: "not filtered by this", not listed
                    checkFilterFields(itemPrefix, condition, property);
                else
                    result.add(readFilterCondition(itemPrefix, condition, property, value));
            }
            verbs.changeFilters(node.getGroups(), result);
        }
        // ... one condition, merged into the conditions the server is filtering by - those an earlier call of a batch
        // stated included -, and the list it makes handed over: with no value, every condition on its property goes -
        // "this column is not filtered by"; with one, it takes the place of the FIRST on its property, its own `or`
        // along, or is appended when there is none (withCondition)
        @Override
        void changeOne(String surface, JavaScriptObject condition) {
            String errorPrefix = controllerPrefix(surface);
            GPropertyDraw property = readFilterProperty(errorPrefix, condition);
            JavaScriptObject value = getOwnField(condition, "value"); // read once, as in changeAll
            ArrayList<GPropertyFilter> result = getConditions(node.getGroups());
            if (isNoValue(value)) {
                checkFilterFields(errorPrefix, condition, property);
                result.removeIf(existing -> existing.column.property.equals(property));
            } else
                result = withCondition(result, readFilterCondition(errorPrefix, condition, property, value));
            verbs.changeFilters(node.getGroups(), result);
        }
    }
    // the PLATFORM'S INPUT over the conditions a view draws - GFilterInputController's kind where a view draws what the
    // box holds (createFiltersControllers): the keys of the table, a pivot's drill-down, the controls' add and
    // reset. Each states the box's list anew from the form's (Verbs.getFilters) through what draws the rows
    // (GTableController.changeFilter), the box's part showing it (FiltersPart). A new condition is made at once - there
    // is no editor here, the view edits its value -, so nothing waits to be applied, and the controls and the toolbar's
    // button get no editors
    private final class ReactFilterInputController implements GFilterInputController {
        // the groups whose conditions they are, in their order: the group, or the tree's, top down
        final List<GGroupObject> groups;
        // what draws the rows - the cell a new condition starts on, and what a list is stated through
        final GTableController rows;
        // what shows the controls and the toolbar's button, the platform's, which show and hide only those: there is no
        // editor of the platform's here
        final GFilterControlsToggle toggle;

        ReactFilterInputController(List<GGroupObject> groups, GTableController rows, GFilterControlsToggle toggle) {
            this.groups = groups;
            this.rows = rows;
            this.toggle = toggle;
            toggle.bind(this, null); // as the panel binds them: its controls give its commands - with no editors
        }

        // F3, and the controls' add - the condition of the cell the table is on, in the list; Alt+F3 (replace): alone
        public void addCondition(Event keyEvent, boolean replace, boolean checkExistingFilter) {
            addCellCondition(replace);
        }
        public void addCondition() {
            addCellCondition(false);
        }
        private void addCellCondition(boolean replace) {
            GPropertyFilter condition = getCellCondition(rows);
            if (condition != null)
                rows.changeFilter(withCondition(replace ? new ArrayList<>() : getConditions(groups), condition));
        }
        // typing in a column, which the table gives to whichever filter input controller it has, as it gives it its
        // keys: the panel's editor takes the key typed, and here nothing starts - there is no editor to type in, the
        // view edits a condition's value its own way (decided: a key typed states no condition)
        public void quickEditFilter(Event keyEvent, GPropertyDraw property, GGroupObjectValue columnKey) {
        }
        // a pivot's drill-down: the conditions it states, the box's list
        public void replaceConditions(ArrayList<GPropertyFilter> conditions) {
            rows.changeFilter(conditions);
        }
        // the controls' reset, Shift+F3 and Escape
        public void resetConditions() {
            rows.changeFilter(new ArrayList<>());
        }
        public boolean hasConditionsToReset() {
            return !getConditions(groups).isEmpty();
        }
        // the controls' add, which the design has the FILTERS box for: a view draws what it holds
        public boolean hasFiltersContainer() {
            return true;
        }
        // the controls, and the table shown or hidden: the platform's controls with it
        public boolean isControlsVisible() {
            return toggle.isVisible();
        }
        public void toggleControlsVisible() {
            setControlsVisible(!toggle.isVisible());
        }
        public void setControlsVisible(boolean visible) {
            toggle.setVisible(visible);
        }
        public void setVisible(boolean visible) {
            toggle.setTableVisible(visible);
        }
        // ... and what draws the rows changed: nothing to read anew - the view's part reads the form's list
        public void update() {
        }
    }

    // a TOOLBAR where React draws it - TOOLBARSYSTEM in a react container: a part of its table, a list's grid or a
    // tree, on the table's node there, and the toolbar's controller (GToolbarController) (GridToolbarPart,
    // TreeToolbarPart). What its buttons show is on the table's node, each under a name of its own, and its buttons are
    // members on the table's: a state's member changes it, as `filters`' does, and the rest are calls. Here is what
    // both have: the filter button - filterControlsVisible, whether the platform's filter controls it shows are shown
    // -, what it is told, and the export
    private abstract class ToolbarPart<N extends OwnerNode, R extends GTableController> extends StatePart<N> implements GToolbarController<R> {
        final GGridProperty table; // whose rows it exports
        // whose controls its member shows and hides: none where the platform has none of its own to show - it draws
        // neither the conditions nor FILTERCONTROLS, or builds no filter input controller -, and the member refuses
        GFilterInputController filter;

        ToolbarPart(N node, GGridProperty table) {
            super(node);
            this.table = table;
        }
        // what its buttons show: the filter button's - a toolbar's -, and its kind's own
        void writeState() {
            setField(node.edit(), "filterControlsVisible", isFilterControlsVisible());
        }
        // its members, on the table's own: a toolbar's - and its kind's own
        @Override
        void putMember(JavaScriptObject tableMember) {
            addToolbarMembers(this, tableMember, node.name);
        }
        boolean isFilterControlsVisible() {
            return filter != null && filter.isControlsVisible();
        }

        // ===== the toolbar controller: the filter button, a member - and the table shown or hidden, with nothing of
        // the platform's to hide here (GToolbarController.setVisible)
        public void addFilterToggle(GFilterInputController filter) {
            this.filter = filter;
        }
        public void showFilterControls(boolean shown) {
            stateChanged();
        }
        public void setVisible(boolean visible) {
        }

        // ===== a call through its member: the filter controls shown or hidden, as the platform's button does - on an
        // empty list of the platform's panel, with a condition to fill in; refused where the platform has none of its
        // own to show
        void changeFilterControlsVisible(String surface, Object shown) {
            String errorPrefix = controllerPrefix(surface);
            boolean value = readBoolean(errorPrefix, shown);
            if (filter == null)
                throw new RuntimeException(errorPrefix + "has no filter controls of the platform's to show: the platform "
                        + "draws neither the conditions nor FILTERCONTROLS here, or its toolbar turns its filters off (showFilters)");
            if (value != filter.isControlsVisible())
                filter.toggleControlsVisible();
        }
        // ... and the export, a request (refused): the server's report of the rows - the group's, or the tree's
        void exportRows(String surface) {
            if (!refused(surface))
                verbs.runTableReport(table);
        }
    }
    // ... of a LIST's group - TOOLBARSYSTEM(g): a part of the group of its own. Its buttons are all a group's toolbar
    // has, whatever the design's flags say - its show... flags are the platform's alone: a view draws what it wants -:
    // manual, filterControlsVisible, listView (listViews the ones it may be), each with its member, and the calls - the
    // count and a column's sum, each the Promise of the server's answer, the export of the rows, the update the manual
    // update holds back (updatePending). The table's settings are not here: a dialog of the platform's, set on the
    // table it draws
    private final class GridToolbarPart extends ToolbarPart<GroupNode, GGridController> implements GGridToolbarController {
        boolean updatePending; // the server holds changes back - while the update is manual
        // whose views its member switches: none where no table of the platform's draws the rows - there is no view to
        // switch to
        GListViewController views;

        GridToolbarPart(GroupNode node) {
            super(node, node.group.grid);
        }
        // what its buttons show, there from the start - the views the table may show, design data, written once: none
        // where no table of the platform's draws the rows
        @Override
        void initialize() {
            JavaScriptObject names = emptyArray();
            if (views != null)
                for (GListViewType listView : views.getListViews())
                    push(names, getViewName(listView));
            setField(node.edit(), "listViews", names);
            super.initialize();
        }
        @Override
        void writeState() {
            super.writeState();
            JavaScriptObject data = node.edit();
            setField(data, "manual", verbs.isManualUpdate(node.group));
            setField(data, "updatePending", updatePending);
            GListViewType view = views != null ? views.getListView() : null;
            setField(data, "listView", view != null ? getViewName(view) : null);
        }
        @Override
        void putMember(JavaScriptObject groupMember) {
            super.putMember(groupMember);
            addGridToolbarMembers(this, groupMember, node.name);
        }

        // ===== the group's toolbar controller: the rows give it their views, a member - a table of the platform's its
        // own, the rows React draws none -, none of their buttons, which only the platform's toolbar asks for
        public void addRows(GGridController rows) {
            this.views = rows.getListViewController();
        }
        public void showView(GListViewType view) {
            stateChanged();
        }
        public void updateState(Boolean updateState) {
            if (updateState != null && updateState != updatePending) {
                updatePending = updateState;
                writeState(); // in the server's delta, published with it
            }
        }
        // ===== a call through its member, checked here, and handed to the form - each one but the filter button's a
        // request (refused)
        JavaScriptObject getCount(String surface) {
            String refusal = verbs.getRefusal(surface);
            if (refusal != null)
                return GController.rejectedControllerPromise(refusal);
            return answerPromise((answer, failed) -> verbs.countRecords(node.group, answer, failed));
        }
        // ... a column's: a number property of the group, named as a state names one - not one grouped in columns, as
        // the call names no column
        JavaScriptObject getSum(String surface, String propertySID) {
            String refusal = verbs.getRefusal(surface);
            if (refusal != null)
                return GController.rejectedControllerPromise(refusal);
            String errorPrefix = controllerPrefix(surface);
            if (propertySID == null)
                throw new RuntimeException(errorPrefix + "takes the name of the property to sum, as a string");
            GPropertyDraw property = readStateProperty(errorPrefix, node.group, propertySID);
            if (property.hasColumnGroupObjects())
                throw new RuntimeException(errorPrefix + "'" + propertySID + "' is grouped in columns, and a sum names no column");
            if (!(property.getValueType() instanceof GIntegralType))
                throw new RuntimeException(errorPrefix + "'" + propertySID + "' is not a number");
            return answerPromise((answer, failed) -> verbs.calculateSum(node.group, property, GGroupObjectValue.EMPTY, answer, failed));
        }
        // the manual update turned on or off, as the platform's toggle does - on, nothing held back yet
        void changeManual(String surface, Object manual) {
            boolean value = readBoolean(controllerPrefix(surface), manual);
            if (refused(surface)) // before the mode changes
                return;
            if (verbs.isManualUpdate(node.group) == value)
                return;
            updatePending = false;
            verbs.changeUpdateMode(node.group, value ? GUpdateMode.MANUAL : GUpdateMode.AUTO);
            stateChanged();
        }
        // ... and the changes it holds back brought now, as the platform's button does
        void forceUpdate(String surface) {
            if (!refused(surface))
                verbs.changeUpdateMode(node.group, GUpdateMode.FORCE);
        }
        // the table's rows shown as another view, one of those the design gives them, as the platform's button does -
        // none where no table of the platform's draws the rows: no name is one of its views then
        void changeListView(String surface, Object name) {
            if (refused(surface))
                return;
            List<GListViewType> listViews = views != null ? views.getListViews() : new ArrayList<>();
            GListViewType view = null;
            StringBuilder names = new StringBuilder(); // the names it takes, said where it is given another
            for (GListViewType listView : listViews) {
                String viewName = getViewName(listView);
                if (viewName.equals(name))
                    view = listView;
                names.append(names.length() > 0 ? ", " : "").append(viewName);
            }
            if (view == null) {
                if (listViews.isEmpty())
                    throw new RuntimeException(controllerPrefix(surface) + "has no view to switch to: no table of the platform's draws the rows");
                throw new RuntimeException(controllerPrefix(surface) + "takes the name of a view of the table: " + names);
            }
            if (view != views.getListView())
                views.changeListView(view);
        }
    }
    // ... of a TREE - TOOLBARSYSTEM(TREE t): a part of the tree, one toolbar for all its groups, on the tree's node,
    // whoever draws the tree's rows. Its buttons are the platform's tree toolbar's, whatever the design's flags say, as
    // the platform shows them: the filter button (filterControlsVisible), the export of the tree (export()), and the
    // two that open the tree (expandCurrent(), expandTree()) where the rows say so
    // (GTreeController.getOpenedByToolbarMembers). Given up with that: a toolbar a view draws apart from the rows React
    // draws opens nothing - each opening has one member, the rows' own, wherever the toolbar is drawn
    private final class TreeToolbarPart extends ToolbarPart<TreeNode, GTreeController> {
        GTreeController opened; // what its two members open, if anything, given with the rows (addRows)

        TreeToolbarPart(TreeNode node) {
            super(node, node.tree);
        }
        // its members: a toolbar's, and the two that open the tree - handed to the platform's tree, which opens the
        // nodes, an edit a batch carries -, only where the rows give it what they open
        @Override
        void putMember(JavaScriptObject treeMember) {
            super.putMember(treeMember);
            if (opened != null) {
                addExpandCurrentMember(opened, treeMember);
                addExpandTreeMember(opened, treeMember);
            }
        }

        // ===== the tree's toolbar controller: what draws its rows, given to it - what its members open, if anything
        public void addRows(GTreeController rows) {
            opened = rows.getOpenedByToolbarMembers();
        }
    }
    // the FILTER CONTROLS where React draws them - FILTERCONTROLS(g), or FILTERCONTROLS(TREE t), in a react container:
    // a part of their own, of the group or of the tree, on its node there, and the controls of its filter input
    // controller (GFilterControlsController). The same whoever draws the conditions: its buttons are members on the
    // table's - the add, the apply, the reset, each what the platform's button does over those conditions -, and what
    // the apply button shows is on the table's node: whether the platform would show it at all, the manual apply mode
    // asking for it (filtersManual), and whether it is lit, conditions typed in waiting for it (filtersPending). Over
    // conditions a view draws nothing ever waits, so both are false. Its commands are the filter input controller's,
    // where the platform wires one up (GFormController.initializeTable), and its apply the platform's
    // editors' of its conditions, the panel's: with none, its members refuse, saying why
    private final class FilterControlsPart extends StatePart<OwnerNode> implements GFilterControlsController {
        GFiltersHandler handler; // the table's filter input controller, once bound - none where the platform makes none
        GFilterEditors editors; // ... and the platform's editors of its conditions - none where a view draws them
        boolean applyEnabled;

        FilterControlsPart(OwnerNode node) {
            super(node);
        }
        // what the apply button shows
        void writeState() {
            boolean manual = editors != null && editors.isManualApplyMode();
            JavaScriptObject data = node.edit();
            setField(data, "filtersManual", manual);
            setField(data, "filtersPending", manual && applyEnabled);
        }
        // its members, on the table's own
        @Override
        void putMember(JavaScriptObject ownerMember) {
            addFilterControlsMembers(this, ownerMember, node.name);
        }

        // ===== the filter input controller's controls: its commands, the view's to show - not the toolbar's -, the
        // manual apply mode shown with them, and lit while conditions typed in wait; and the table shown or hidden,
        // with nothing of the platform's to hide here (GFilterControlsController.setTableVisible)
        public void bind(GFiltersHandler handler, GFilterEditors editors) {
            this.handler = handler;
            this.editors = editors;
        }
        public boolean isShownByToolbar() {
            return false;
        }
        public void setVisible(boolean visible) {
            stateChanged();
        }
        public void setApplyEnabled(boolean enabled) {
            applyEnabled = enabled;
            stateChanged();
        }
        public void setTableVisible(boolean visible) {
        }

        // ===== a call through its member: what the platform's button does - the add where the design has the FILTERS
        // box, as the button is there only then, the apply while it is lit, as it is clickable only then - refused
        // where there is nothing to do it with: no filter input controller, no FILTERS box, no editors of the
        // platform's
        void addFilter(String surface) {
            GFiltersHandler handler = getHandler(surface);
            if (!handler.hasFiltersContainer())
                throw new RuntimeException(controllerPrefix(surface) + "has no FILTERS box to add a condition to: the design has none");
            handler.addCondition();
        }
        void applyFilters(String surface) {
            getHandler(surface);
            if (editors == null)
                throw new RuntimeException(controllerPrefix(surface) + "has nothing to apply: a view draws the conditions, "
                        + "each change of which is sent at once");
            if (editors.isManualApplyMode() && applyEnabled)
                editors.applyFilters();
        }
        void resetFilters(String surface) {
            getHandler(surface).resetConditions();
        }
        // the commands whose controls these are - refused where the platform builds no filter input controller: it
        // draws the conditions, and its toolbar turns its filters off (showFilters)
        private GFiltersHandler getHandler(String surface) {
            if (handler == null)
                throw new RuntimeException(controllerPrefix(surface) + "has nothing to give its commands to: the platform draws "
                        + "the conditions, and its toolbar turns its filters off (showFilters)");
            return handler;
        }
    }
    // a group's FILTER GROUP where React draws it - FILTERGROUP(fg) in a react container: a part of the group, on its
    // node there, under the filter group's own name - the form's name of it, sent with it as a tree's is
    // (GRegularFilterGroup.name) -, shaped as a property's entry - `value`, the index of the filter selected, null for
    // none; `filters`, their captions; `canClear`, whether none may be selected, as the platform's widget offers it
    // (offersNone) - and the controller of the filter group (GFilterGroupController). Its member's change(index)
    // selects one as the platform's widget does, in a batch too; the server applies it and reports it (showFilter); a
    // filter's key works as the platform's widget's does - while the form shows the filter group and the container it
    // is in, selecting the filter, and taking it off in a group of one filter, a check box
    private final class FilterGroupPart extends StatePart<GroupNode> implements GFilterGroupController {
        final GRegularFilterGroup filterGroup;
        GRegularFilter selected; // null: none

        FilterGroupPart(GroupNode node, GRegularFilterGroup filterGroup) {
            super(node);
            this.filterGroup = filterGroup;
            // its filters' keys, bound as it is made, as the platform's check box or list binds its own: a key's
            // priority is the order it is bound in (ProcessBinding), so the filter groups' keys keep the form's order
            // whoever draws them - bound when the part is set out, they would come after every platform's group's
            for (GRegularFilter filter : filterGroup.filters)
                for (GInputBindingEvent bindingEvent : filter.bindingEvents)
                    verbs.addRegularFilterBinding(bindingEvent, event -> toggle(filter), node.state.scope, this::isShown, filterGroup.groupObject);
        }
        // whether the form shows the filter group now, which its descriptor says (ComponentEntry.isShown): its filters'
        // keys are taken while it does, as the platform's widget's are while the widget is shown - it is hidden with
        // the filter group, and its key goes with it
        boolean isShown() {
            return !isField(getOwnField(node.state.current(), filterGroup.sID), "hidden", true);
        }
        // its entry
        void writeState() {
            JavaScriptObject entry = newObject();
            setField(entry, "value", selected != null ? (Double) (double) filterGroup.filters.indexOf(selected) : null);
            JavaScriptObject captions = emptyArray();
            for (GRegularFilter filter : filterGroup.filters)
                push(captions, filter.caption);
            setField(entry, "filters", captions);
            setField(entry, "canClear", offersNone());
            setField(node.edit(), filterGroup.name, entry);
        }
        // its member, on the group's own
        @Override
        void putMember(JavaScriptObject groupMember) {
            setField(groupMember, filterGroup.name, makeFilterGroupMember(this, node.group.getSID() + "." + filterGroup.name));
        }

        // ===== the filter group's controller: the filter the server reports selected - in its delta, published with
        // it -, or the one just asked for
        public void showFilter(GRegularFilter filter) {
            if (filter != selected) {
                selected = filter;
                writeState();
            }
        }

        // ===== a filter's key, as the platform's widget takes it: selected - in a group of one filter, the platform's
        // check box, taken off where it is selected already; a group of several's list only selects
        void toggle(GRegularFilter filter) {
            select(filter == selected && filterGroup.filters.size() == 1 ? null : filter);
        }
        // whether none may be selected, as the platform's widget offers it: a group of one filter is its check box,
        // cleared whatever NONULL says - the server takes a NONULL group's null too (FilterGroupAction) -, a group of
        // several its list, with "(All)" unless NONULL
        private boolean offersNone() {
            return filterGroup.filters.size() == 1 || !filterGroup.noNull;
        }
        // ... and a call through its member: the index of one of its filters, or null for none where none is offered
        void change(String surface, Object index) {
            String errorPrefix = controllerPrefix(surface);
            int size = filterGroup.filters.size();
            if (index == null) {
                if (!offersNone())
                    throw new RuntimeException(errorPrefix + "takes the index of one of its filters, 0 to " + (size - 1)
                            + ": one has to be selected");
                select(null);
                return;
            }
            double value = index instanceof Double ? (Double) index : -1;
            if (value != Math.floor(value) || value < 0 || value >= size)
                throw new RuntimeException(errorPrefix + "takes the index of one of its filters, 0 to " + (size - 1)
                        + (offersNone() ? ", or null for none" : ""));
            select(filterGroup.filters.get((int) value));
        }
        // ... the one asked for, sent even where it is shown: what the server holds may be other - a batch that failed
        // leaves the filter it selected shown -, and asked again, it is set again
        private void select(GRegularFilter filter) {
            verbs.changeRegularFilter(filterGroup, filter);
        }
    }
    // a view's name, as the group's node and its member say it: grid, pivot, custom, map, calendar
    private static String getViewName(GListViewType view) {
        return view.name().toLowerCase();
    }
    private static boolean readBoolean(String errorPrefix, Object value) {
        if (!(value instanceof Boolean))
            throw new RuntimeException(errorPrefix + "takes true or false");
        return (Boolean) value;
    }

    // a group's ROWS where React draws them: a part of the group, on its node where the component drawing them is -
    // the list, where each row sits in it, which of them is current; its member's change(row), a current object being
    // chosen among them; and the group's own readers, its row attributes and its group attributes - React's alone.
    // Whether the component drawing the rows is shown is that component's descriptor, as anything else's is. It is the
    // group's controller too, as a grid's controller is where the platform draws the rows. Of the parts, it alone reads
    // the entries its node carries: a list property's column is drawn in its rows, so the columns there are the rows'
    // - a new row's cells (replaceRows), and what the filter input controller asks of the rows (getFilterColumns)
    private abstract class RowsPart extends Part<GroupNode> implements GGroupController, Rows {
        // as the server sends them, with a view's own add/remove on top - none before the first delivery: its own list
        // from the start, which it replaces and never changes
        ArrayList<GGroupObjectValue> rows = new ArrayList<>();
        final ReaderValues values = new ReaderValues(); // what the group's own readers say, by row and of the group
        GGroupObjectValue currentKey; // kept even before its row arrives: the row states it when it is built
        final NativeHashMap<GGroupObjectValue, Integer> positions = new NativeHashMap<>();
        // what each row carries of the group's own readers: its colors, its selection
        final GPropertyReader[] rowReaders;

        RowsPart(GroupNode node) {
            super(node);
            ArrayList<GPropertyReader> rowReaders = new ArrayList<>();
            for (GGroupObjectPropertyReader reader : node.group.getPresentationReaders())
                if (reader != null && reader.getAttributeScope() == GGroupAttributeScope.ROW)
                    rowReaders.add(reader);
            this.rowReaders = rowReaders.toArray(new GPropertyReader[0]);
        }
        // its rows, none of them yet
        @Override
        void initialize() {
            replaceRows();
        }
        // its member, the group's own: change(row)
        @Override
        void putMember(JavaScriptObject groupMember) {
            addRowsVerbs(this, groupMember, node.group.getSID());
        }
        // the lsf list property whose per-row renderers go into the rows drawn here, by its integration name
        GPropertyDraw getRowLsfProperty(String integrationSID) {
            for (GPropertyDraw property : form.propertyDraws)
                if (property.groupObject == node.group && property.isLsfViewPerRow() && integrationSID.equals(property.integrationSID))
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
        // the rows as the view has them, its own add or remove included - the part's own list, which is replaced and
        // never changed (setRows)
        public ArrayList<GGroupObjectValue> getRows() {
            return rows;
        }

        // ===== what a filter input controller asks of these rows, through their table (TablePart): the columns
        // carried here that the form shows and a condition can be on, in the form's order, by their captions - a view
        // has no current column, so a new condition starts on the first of them -, and a column's value in the current
        // row, to start one with
        GPropertyDraw getSelectedFilterProperty() {
            for (PropertyEntry propertyEntry : node.properties)
                if (isFilterColumn(propertyEntry))
                    return propertyEntry.property;
            return null;
        }
        // an lsf column's too: the platform's, which draws its cells - an edit's before the server answers, as a grid's
        PValue getSelectedValue(GPropertyDraw property, GGroupObjectValue columnKey) {
            for (PropertyEntry propertyEntry : node.properties)
                if (propertyEntry.property == property)
                    return currentKey != null ? propertyEntry.getValueAt(currentKey) : null;
            return null;
        }
        List<Pair<Column, String>> getFilterColumns() {
            List<Pair<Column, String>> columns = new ArrayList<>();
            for (PropertyEntry propertyEntry : node.properties)
                if (isFilterColumn(propertyEntry)) { // by the caption it is shown with, as a grid's column is
                    GPropertyDraw property = propertyEntry.property;
                    NativeHashMap<GGroupObjectValue, PValue> captions = property.captionReader != null ? propertyEntry.values.get(property.captionReader) : null;
                    columns.add(GGridPropertyTable.getFilterColumn(property, GGroupObjectValue.EMPTY, GGridPropertyTable.getPropertyCaption(captions, property, GGroupObjectValue.EMPTY)));
                }
            return columns;
        }
        private boolean isFilterColumn(PropertyEntry propertyEntry) {
            return propertyEntry.property.isList && propertyEntry.isShown() && propertyEntry.property.canBeFiltered();
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
            replaceAttribute(node.edit(), reader, values.get(reader, GGroupObjectValue.EMPTY));
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
            JavaScriptObject current = node.current();
            JavaScriptObject byKey = field(current, "byKey");
            JavaScriptObject row = field(byKey, key.toKeyString());
            JavaScriptObject original = node.published();
            // Identity tells whether this object still belongs to the published snapshot. A copied or newly
            // inserted row is already writable, including after a reorder within the same batch.
            if (row == field(field(original, "byKey"), key.toKeyString())) {
                JavaScriptObject list = field(current, "list");
                if (list == field(original, "list")) {
                    list = copyArray(list);
                    byKey = copyKeepingPrototype(byKey);
                    setField(node.edit(), "list", list);
                    setField(node.edit(), "byKey", byKey);
                }
                row = copyKeepingPrototype(row);
                setField(byKey, key.toKeyString(), row);
                setArrayElement(list, position, row);
            }
            return row;
        }
        void replaceRows() {
            JavaScriptObject previous = field(node.current(), "byKey");
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
                    for (PropertyEntry propertyEntry : node.properties)
                        propertyEntry.fillRow(row, key);
                }
                positions.put(key, position++);
                setField(byKey, key.toKeyString(), row);
                push(list, row);
                push(keys, key.toKeyString());
            }
            setField(node.edit(), "list", list);
            setField(node.edit(), "byKey", byKey);
            setField(node.edit(), "keys", keys);
        }
        // a row's own attributes: whether it is the current one, and what the group's readers say of it
        void fillRowAttributes(JavaScriptObject row, GGroupObjectValue key) {
            setField(row, "isCurrent", key.equals(currentKey));
            for (GPropertyReader reader : rowReaders)
                replaceAttribute(row, reader, values.get(reader, key));
        }

        // ===== a call through its member: change(row), the current object chosen among the rows drawn here
        void change(String surface, JavaScriptObject objectOrKey) {
            verbs.changeCurrentObject(node.group, resolveGroupRow(controllerPrefix(surface), objectOrKey));
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
            GGroupObjectValue rowKey = key.isEmpty() ? null : node.group.getRowKey(key);
            if (rowKey == null)
                throw new RuntimeException(errorPrefix + "that row is not a row of '" + node.group.getSID() + "'");
            return rowKey;
        }
        // a row of this group, named however the view has it: the row, its `objects` handle, or the key the projection
        // gave it - a name only here, where the rows are, looked up in the index built with them
        GGroupObjectValue resolveRow(String errorPrefix, JavaScriptObject objectOrKey) {
            GGroupObjectValue key = GGroupObjectValue.resolveObject(objectOrKey);
            if (key == null)
                key = GGroupObjectValue.resolveObject(keyedRow(field(node.current(), "byKey"), objectOrKey));
            if (key == null)
                throw new RuntimeException(errorPrefix + "expects a row of '" + node.group.getSID() + "', its objects handle, or its key;"
                        + " that is none of them, or names no row the group has now");
            return key;
        }
    }

    // ... of the group of a GRID - a list's group not in a tree -: its rows alone, none of them a node to open
    // (GGroupController's defaults); what the grid's toolbar and filter input controller ask of the grid as a whole is
    // its table's (GridTablePart)
    private final class GridRowsPart extends RowsPart {
        GridRowsPart(GroupNode node) {
            super(node);
        }
    }
    // ... of a group of a TREE: its rows say where they hang in it, from the hierarchy sent beside them. A node's
    // children are rows of its own group, when that one recurses, or of the group below; the tree draws all its groups,
    // so the rows of each of them are here, each group's its part - and what the rows of one group state about those
    // of another, which nodes are open and what was asked of them, is the tree's, which the part asks (TreeTablePart)
    private final class TreeRowsPart extends RowsPart {
        // the tree as a whole, its grid's part, which keeps its groups' parts and what crosses them
        final TreeTablePart table;
        // the tree's links, the group above and the group below - none at its top and at its bottom -, set as the parts
        // are made, top down, and walked by the tree alone
        TreeRowsPart up;
        TreeRowsPart down;
        // the row each row hangs under, as the server last sent it
        NativeHashMap<GGroupObjectValue, GGroupObjectValue> parents = new NativeHashMap<>();
        // ... so the rows its rows hang under, as they stand - nodes of this group, where it recurses, or of the group
        // above: the nodes whose children are loaded (hasRowsUnder)
        NativeHashMap<GGroupObjectValue, Boolean> parentRows = new NativeHashMap<>();
        // ... and the rows it says have children
        NativeHashMap<GGroupObjectValue, Boolean> expandable = new NativeHashMap<>();

        // ... linked with the part of the group above, made before it: the tree's groups' parts are made top down
        // (createTreeController)
        TreeRowsPart(GroupNode node, TreeTablePart table) {
            super(node);
            this.table = table;
            GGroupObject upGroup = node.group.getUpTreeGroup();
            if (upGroup != null) {
                up = table.rowsParts.get(upGroup);
                up.down = this;
            }
        }
        // a tree's rows are its nodes: the verbs that open them are here, with them, on the group's member as well
        @Override
        void putMember(JavaScriptObject groupMember) {
            super.putMember(groupMember);
            addNodeVerbs(this, groupMember, node.group.getSID());
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
            table.answered(this, requestIndex);
        }
        // ... and its current object, the server's or the view's: the tree's current node then, set to a row - even
        // the row it already was, as the platform's tree selects it: the server reports a group's current object anew
        // whenever it reads the group's rows again - of a tree, one group in an answer, the deepest it read
        public void updateCurrentKey(GGroupObjectValue currentKey) {
            super.updateCurrentKey(currentKey);
            table.currentSet(this, currentKey);
        }
        public void changeCurrentKey(GGroupObjectValue currentKey) {
            super.changeCurrentKey(currentKey);
            table.currentSet(this, currentKey);
        }
        // ... however it moves - the server's, the view's, a view's own add or remove, the path a row below makes -:
        // the tree's current row may move with it, so the tree places it anew (TreeTablePart.place)
        void setCurrentObject(GGroupObjectValue key) {
            if (GwtClientUtils.nullEquals(currentKey, key))
                return;
            super.setCurrentObject(key);
            table.touch();
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
                    keyed.put(rows.get(i), node.group.getParentRowKey(rows.get(i), parents.get(i)));
            } else {
                // parallel by construction; if they ever are not, no row states a parent
                GwtClientUtils.logLsfViewError("data." + node.group.getSID() + ": " + parents.size()
                        + " parents came for " + rows.size() + " rows; the hierarchy is dropped");
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
            table.touch();
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
            table.touch();
        }
        // each row of the group states its facts in the tree, once the batch is complete (TreeTablePart.place) - each
        // compared with what the row says now and written only where it differs: a row whose facts stayed keeps its
        // object, whatever else moved in the tree, and a row made anew says nothing yet, so it is given them all. Where
        // it hangs and whether it has children are the group's own; whether it is open, and whether its children are
        // being loaded, cross the groups: the tree's
        void place() {
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
                boolean expanded = table.isOpen(this, key);
                if (!isField(row, "expanded", expanded))
                    setField(editRow(key), "expanded", expanded);
                // ... and that its children are being loaded: a view asked to open it, and the rows of the answer have not
                // come - as a changed cell says it waits for its answer. Closing a node loads nothing, and nor does
                // opening one with no children, which stays closed (isOpen). There only while it is so
                boolean loading = table.isPendingExpanding(key) && expanded;
                if (loading != isField(row, "loading", true)) {
                    JavaScriptObject edited = editRow(key);
                    if (loading)
                        setField(edited, "loading", true);
                    else
                        deleteField(edited, "loading");
                }
            }
        }
        // whether the server counts children under a row, loaded or not
        boolean hasChildren(GGroupObjectValue key) {
            return expandable.get(key) != null;
        }
        // ... and whether a row of the group hangs under a node - of this group, where it recurses, or of the group
        // above -: the node's children are loaded (TreeTablePart.isOpen)
        boolean hasRowsUnder(GGroupObjectValue key) {
            return parentRows.get(key) != null;
        }
        // the row as it stands now: in the draft, or as published
        JavaScriptObject currentRow(GGroupObjectValue key) {
            return field(field(node.current(), "byKey"), key.toKeyString());
        }
        // ===== a call through its member: expand(row), collapse(row), toggle(row) - a node of the tree, one of the
        // rows drawn here, named as change(row) names a row of its group (resolveGroupRow): a row of a group BELOW
        // carries the path past this one and names the node it hangs under here. Straight to the server: a tree whose
        // rows React draws has no platform table to go through. Expanded-ness is the SERVER's state (it decides which
        // rows a group returns at all), so these return nothing: the children arrive as rows. The node's `expanded`
        // says what was asked at once, as a changed value does, and what the rows say once the server answers
        void expand(String surface, JavaScriptObject objectOrKey) {
            verbs.expandNode(node.group, resolveGroupRow(controllerPrefix(surface), objectOrKey));
        }
        void collapse(String surface, JavaScriptObject objectOrKey) {
            verbs.collapseNode(node.group, resolveGroupRow(controllerPrefix(surface), objectOrKey));
        }
        // ... the opposite of what the node says now (`expanded`) - what was asked of it included, so two toggles
        // before the answer open it and close it again, made one after the other or in one batch
        void toggle(String surface, JavaScriptObject objectOrKey) {
            GGroupObjectValue key = resolveGroupRow(controllerPrefix(surface), objectOrKey);
            if (table.isOpen(this, key))
                verbs.collapseNode(node.group, key);
            else
                verbs.expandNode(node.group, key);
        }
        // ... expandAll(), collapseAll(): every node of the group, and of the groups below it - for the tree's top
        // group, the whole tree
        void expandAll() {
            verbs.expandAll(node.group);
        }
        void collapseAll() {
            verbs.collapseAll(node.group);
        }
        // ===== the group's controller (GGroupController), of a tree: a node asked to open or close, once the request
        // is sent - into the drafts only, as the form publishes then -: shown as asked until the keys of the answer
        // come with the request's index (updateKeys) - the tree keeps what was asked, of a node of this group, of every
        // node of it and of the groups below, or of a node and every node under it
        public void changeExpanded(GGroupObjectValue key, boolean open, long requestIndex) {
            table.changeExpanded(this, key, open, requestIndex);
        }
        public void changeExpandedAll(boolean open, long requestIndex) {
            table.changeExpandedAll(this, open, requestIndex);
        }
        public void changeExpandedUnder(GGroupObjectValue key, long requestIndex) {
            table.changeExpandedUnder(this, key, requestIndex);
        }
    }
    // a TABLE where React draws its rows - GRID(g) of a list's group not in a tree, or GRID(TREE t), in a react
    // container -: a part on the table's node there - the group's, or the tree's -, what its toolbar and filter input
    // controller ask of its rows (GTableController), over the rows' parts of its groups, each kept as it is made
    // (rowsParts): the condition a key or the controls start, on the CURRENT group's rows (currentPart) - a grid's one
    // group, a tree's current node's -, the values and the columns of each group, and the list a filter input
    // controller states, each group's its own (Verbs.changeFilters). Nothing a table's filter input controller asks has
    // keys or a column of its own, and the focus is the view's to keep. A grid's table writes nothing of its own - its
    // group's rows' part writes the rows -; a tree's writes what its rows state about one another (TreeTablePart)
    private abstract class TablePart<N extends OwnerNode, R extends RowsPart> extends Part<N> implements GTableController {
        final NativeSIDMap<GGroupObject, R> rowsParts = new NativeSIDMap<>(); // each group's, as it is made
        // ... and the part of the table's user orders, beside it on the table's node, the sortings being the order the
        // rows are drawn in
        final OrdersPart orders;

        TablePart(N node) {
            super(node);
            orders = new OrdersPart(node);
        }
        // the rows' part of the group a condition starts on
        abstract R currentPart();
        // what it writes, and its member: nothing of its own - its groups' rows' parts write the rows
        @Override
        void initialize() {
        }
        @Override
        void putMember(JavaScriptObject ownerMember) {
        }
        // ... and what its rows state about one another, placed once the batch is complete (ContainerState.commit):
        // nothing - a grid's rows state nothing about each other; a tree's (TreeTablePart.place)
        void place() {
        }

        public GGroupObject getSelectedGroupObject() {
            return currentPart().node.group;
        }
        public GPropertyDraw getSelectedFilterProperty() {
            return currentPart().getSelectedFilterProperty();
        }
        public GGroupObjectValue getSelectedColumnKey() {
            return GGroupObjectValue.EMPTY;
        }
        public int getSelectedRow() {
            return currentPart().getSelectedRow();
        }
        public PValue getSelectedValue(GPropertyDraw property, GGroupObjectValue columnKey) {
            return rowsParts.get(property.groupObject).getSelectedValue(property, columnKey);
        }
        public List<Pair<Column, String>> getFilterColumns() {
            List<Pair<Column, String>> columns = new ArrayList<>();
            for (GGroupObject group : node.getGroups())
                columns.addAll(rowsParts.get(group).getFilterColumns());
            return columns;
        }
        public long changeFilter(ArrayList<GPropertyFilter> conditions) {
            return verbs.changeFilters(node.getGroups(), conditions);
        }
        public boolean focusFirstWidget(FocusUtils.Reason reason) {
            return false;
        }
        // ... and the filter input controller itself, which the rows have no use of - no keys -: told that they have
        // changed, once per delta, and let go of when the form closes, the rows' only part in either - the projection
        // publishes what React draws (GTableController.update, destroy)
        GFilterInputController filter;
        public void bindFilter(GFilterInputController filter) {
            this.filter = filter;
        }
        public void update(long requestIndex, GFormChanges fc) {
            if (filter != null)
                filter.update();
        }
        public void destroy() {
            if (filter != null)
                filter.destroy();
        }
        // ... and what the form takes of them for the table's groups: each group's rows' part, and the orders' part
        public GGroupController getGroupController(GGroupObject group) {
            return rowsParts.get(group);
        }
        public GUserOrdersController getUserOrdersController() {
            return orders;
        }
    }
    // ... of a GRID: its one group's rows' part, made with it (createGridController), and what the toolbar asks of a
    // grid's rows (GGridController) - no views, which only a table of the platform's has, and on the platform's toolbar
    // its buttons that need no table, which the toolbar makes: React's rows build nothing of the platform's
    private final class GridTablePart extends TablePart<GroupNode, GridRowsPart> implements GGridController {
        GridTablePart(GroupNode node) {
            super(node);
        }
        GridRowsPart currentPart() {
            return rowsParts.get(node.group);
        }
        public void addToolbarButtons(GPlatformGridToolbarController toolbar) {
            toolbar.addButtonsWithoutTable();
        }
        public GListViewController getListViewController() {
            return null;
        }
    }
    // ... of a TREE, one for all its groups: the rows' parts of its groups, each kept as it is made (TreeRowsPart), and
    // what their rows state about one another, which no group's rows state alone - which nodes are open (isOpen), what
    // a view asked of them (pendingExpanding), the current node (currentSet). On the tree's node it writes its current
    // row (`current`), and its member is the one verb only the tree has (expandCurrent()). What the platform's toolbar
    // asks of the tree is its opening (GTreeController)
    private final class TreeTablePart extends TablePart<TreeNode, TreeRowsPart> implements GTreeController {
        TreeRowsPart current; // none before a group's current object is set to a row
        // the nodes a view asked to open or close, of any of its groups - the keys of a tree's groups never collide -,
        // each with its request, until that request is answered - as the platform's tree keeps them on its own
        // (GTreeContainerTableNode.pendingExpanding)
        final NativeHashMap<GGroupObjectValue, ExpandRequest> pendingExpanding = new NativeHashMap<>();
        // whether what its rows state about one another changed since they were placed: the rows, where they hang,
        // what was asked of them, which of them is current
        boolean touched;

        TreeTablePart(TreeNode node) {
            super(node);
        }
        // what it writes, there from the start: no current row yet
        @Override
        void initialize() {
            placeCurrent();
        }
        // its member, on the tree's own
        @Override
        void putMember(JavaScriptObject treeMember) {
            addExpandCurrentMember(this, treeMember);
        }
        // the part of the current node - before any, the top group's
        TreeRowsPart currentPart() {
            return current != null ? current : rowsParts.get(node.tree.groups.get(0));
        }
        // a group's current object set: a row - the group's is the current node now, the rows on the path above it,
        // which its key carries, are current in their groups, and the groups below it are on no row, as the server has
        // them: a current object set sets the objects of the groups on the path and drops those of the groups below
        // (GroupObjectInstance.change), and the server reports the deepest group's objects alone, the path in them, as
        // the platform's tree selects the one node -, or none. The current row moves with the group (place), as it
        // does with the group's current object (TreeRowsPart.setCurrentObject)
        void currentSet(TreeRowsPart part, GGroupObjectValue key) {
            if (key != null && !key.isEmpty()) {
                if (current != part) {
                    current = part;
                    touch();
                }
                for (TreeRowsPart up = part.up; up != null; up = up.up)
                    up.setCurrentObject(up.node.group.getRowKey(key));
                for (TreeRowsPart down = part.down; down != null; down = down.down)
                    down.setCurrentObject(null);
            }
        }

        // ===== what crosses its groups, which its parts ask it: the rows, or where they hang, or what was asked of
        // them, or which of them is current, changed - of any group -, so they are placed anew, all of them, once the
        // batch is complete (place), and the container with them
        void touch() {
            touched = true;
            node.state.enlist();
        }
        // each row of each group states its facts in the tree, from the completed batch - the rows of a node's children
        // may come after it -, where anything they state about one another changed since: the rows of every group, a
        // row whose facts stayed keeping its object (TreeRowsPart.place), and the tree its current row
        @Override
        void place() {
            if (!touched)
                return;
            touched = false;
            rowsParts.foreachValue(TreeRowsPart::place);
            placeCurrent();
        }
        // the tree's CURRENT ROW, data.<tree>.current: the key of the current node's row, written the way a key is - so
        // `row.key === data.t.current` holds, as a row's `parent` does -, null while the tree has no current row
        // LOADED: before any is set, while the current node's group is on no row, while its row has not come or has
        // gone - never a row above it in its stead, nor the top group's (currentPart), which is no row chosen. Compared
        // with what the node says now and written only where it differs, as a row's facts are (TreeRowsPart.place)
        void placeCurrent() {
            GGroupObjectValue key = getCurrentRowKey();
            JavaScriptObject data = node.current();
            if (key == null) {
                if (data == null || !isNullField(data, "current"))
                    setField(node.edit(), "current", (JavaScriptObject) null);
            } else if (data == null || !GGroupObjectValue.isKeyAt(data, "current", key))
                GGroupObjectValue.writeKey(node.edit(), "current", key);
        }
        // ... the key of that row, none where there is none
        GGroupObjectValue getCurrentRowKey() {
            GGroupObjectValue key = current != null ? current.currentKey : null;
            return key != null && current.positions.get(key) != null ? key : null;
        }
        // whether a node of a group is OPEN: its children are loaded - a row of its own group or of the group below
        // hangs under it, and the keys of a tree's groups cannot collide. Until the server answers what a view asked of
        // it, it is what was asked; only a node with children opens
        boolean isOpen(TreeRowsPart part, GGroupObjectValue key) {
            ExpandRequest request = pendingExpanding.get(key);
            if (request != null)
                return request.open && part.hasChildren(key);
            return part.hasRowsUnder(key) || (part.down != null && part.down.hasRowsUnder(key));
        }
        // ... and whether a view asked to open or close it and the server has not answered: its children are being
        // loaded while it is open so (TreeRowsPart.place)
        boolean isPendingExpanding(GGroupObjectValue key) {
            return pendingExpanding.get(key) != null;
        }
        // a node of a group asked to open or close, with the index of its request (GGroupController.changeExpanded):
        // shown as asked until the keys of the answer come with that index - kept only where a node of the group may
        // have children: nothing opens elsewhere, and no keys would answer it
        void changeExpanded(TreeRowsPart part, GGroupObjectValue key, boolean open, long requestIndex) {
            if (!part.node.group.mayHaveChildren())
                return;
            pendingExpanding.put(key, new ExpandRequest(part, open, requestIndex));
            touch();
        }
        // ... and so is every node there is of a group and of the groups below it; the latest request asked of a node
        // is the one it shows
        void changeExpandedAll(TreeRowsPart part, boolean open, long requestIndex) {
            for (TreeRowsPart below = part; below != null; below = below.down)
                for (GGroupObjectValue key : below.rows)
                    changeExpanded(below, key, open, requestIndex);
        }
        // ... and so is a node and every node under it there is, opened - only opened, as expandCurrent() opens -: the
        // rows of its group hanging under it - all the way down, where the group recurses - and of the groups below,
        // under those
        void changeExpandedUnder(TreeRowsPart part, GGroupObjectValue key, long requestIndex) {
            NativeHashMap<GGroupObjectValue, Boolean> under = new NativeHashMap<>(); // the keys of a tree never collide
            under.put(key, Boolean.TRUE);
            for (TreeRowsPart below = part; below != null; below = below.down) {
                boolean found;
                do { // a row may come before the row it hangs under
                    found = false;
                    for (GGroupObjectValue row : below.rows) {
                        GGroupObjectValue parent = below.parents.get(row);
                        if (under.get(row) == null && parent != null && under.get(parent) != null) {
                            under.put(row, Boolean.TRUE);
                            found = true;
                        }
                    }
                } while (found);
                for (GGroupObjectValue row : below.rows)
                    if (under.get(row) != null)
                        changeExpanded(below, row, true, requestIndex);
            }
        }
        // the rows of a group came with a request's index: the server answered the requests up to that one asked of the
        // nodes they hang under - of their own group, and of the group above -, and what those asked gives way to what
        // the rows say
        void answered(TreeRowsPart part, int requestIndex) {
            ArrayList<GGroupObjectValue> answered = new ArrayList<>();
            pendingExpanding.foreachEntry((key, request) -> {
                if ((request.part == part || request.part == part.up) && request.requestIndex <= requestIndex)
                    answered.add(key);
            });
            for (GGroupObjectValue key : answered)
                pendingExpanding.remove(key);
            if (!answered.isEmpty())
                touch();
        }

        // ===== what opens the tree (GTreeController) - its member, and the two buttons of the platform's toolbar over
        // these rows, as the platform's tree's buttons open it: the current node opened, and every node under it - read
        // when it is asked, and only a node with children, as the platform's button opens only such a one: nothing
        // before a node is current -, an edit a batch carries, which its rows show at once all the way down
        // (changeExpandedUnder); or the tree from the current node's group down - every node of that group under the
        // nodes open above it, and of the groups below (GTreeTable.fireExpandNodeRecursive) -: nothing with no current
        // row loaded. The whole tree is the top group's expandAll()
        public void expandCurrent() {
            TreeRowsPart part = currentPart();
            GGroupObjectValue key = part.currentKey;
            if (key != null && part.hasChildren(key))
                verbs.expandCurrent(part.node.group, key);
        }
        public void expandTree() {
            if (getCurrentRowKey() != null)
                verbs.expandAll(current.node.group);
        }
        // ... and nothing a view's part of the toolbar opens through members of its own
        // (GTreeController.getOpenedByToolbarMembers)
        public GTreeController getOpenedByToolbarMembers() {
            return null;
        }
    }
    // what a view asked of a node of a tree - of the group whose rows' part it is -, and the request that asks it of
    // the server
    private static final class ExpandRequest {
        final TreeRowsPart part;
        final boolean open;
        final long requestIndex;

        ExpandRequest(TreeRowsPart part, boolean open, long requestIndex) {
            this.part = part;
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
        // names it by that name. Its node is its group's, the empty group's the top level, which is what carries it
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
        // platform draws and edits, nor for an lsf action, which the platform's button runs
        void putMember(JavaScriptObject owner) {
        }
        GComponent owner() {
            return property;
        }
        // its value in a cell - a row's, for a column -, as the client has it now, null for none: what the rows answer
        // of the current cell (GTableController.getSelectedValue)
        abstract PValue getValueAt(GGroupObjectValue key);
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
        // its values go with it, and so does what loads them - an answer to a change of a property the form no longer
        // shows takes no loading off; its attributes stay as the server last sent them
        public void dropProperty(GPropertyDraw dropped) {
            values.remove(property);
            values.remove(property.loadingReader);
            setShown(false);
        }
        // what React draws nothing from - its classes, its change keys - goes nowhere
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
        // its value in a cell: kept here, the client's own on top (put)
        PValue getValueAt(GGroupObjectValue key) {
            return values.get(property, key);
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
            // the cell says `loading` until the answer to its change takes it off (GLoadingReader), as the platform's does
            put(property.loadingReader, cellKey, PValue.getPValue(true));
            if (value != PValue.UNDEFINED) // none to show - an action's run, a value the server decides -: its own
                put(property, cellKey, value);
            return new Pair<>(cellKey, oldValue);
        }
        // the key of its cell for the edited one, which is what setLoadingValueAt RETURNS: the caller keys the pending
        // change / loading requests by it, comparing them with the keys the server's own delta carries.
        // `fullCurrentKey` is every group's current object plus the edited cell's key
        // (GFormController.getFullCurrentKey), and null - the key holds no cell of it - is "no cell"
        abstract GGroupObjectValue getCellKey(GGroupObjectValue fullCurrentKey);
        // one value of a cell, the client's own - its value, or its loading -: it is kept, and the cell rebuilt
        void put(GPropertyReader reader, GGroupObjectValue key, PValue value) {
            if (values.put(reader, key, value))
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
            verbs.changeProperty(property, getMemberKey(errorPrefix, objectOrKey), value);
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
    // a list property's column, on the node its group's rows are on, and its cells in those rows
    private final class ReactColumnPropertyEntry extends ReactPropertyEntry {
        final RowsPart rowsPart;
        // what it is drawn with, by the side of the column / cell split each lands on - an ACTION's image is a cell's
        final GPropertyReader[] columnReaders, cellReaders;

        ReactColumnPropertyEntry(RowsPart rowsPart, GPropertyDraw property) {
            super(rowsPart.node, property);
            this.rowsPart = rowsPart;
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
            for (GGroupObjectValue key : rowsPart.rows)
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
            JavaScriptObject row = rowsPart.editRow(key);
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
            return fullCurrentKey.isEmpty() ? rowsPart.currentKey : property.groupObject.getRowKey(fullCurrentKey);
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
                return rowsPart.resolveRow(errorPrefix, objectOrKey);
            if (!objectKey.isEmpty() && property.groupObject.filterRowKeys(objectKey) == null)
                throw new RuntimeException(errorPrefix + "that row is not a row of '" + property.groupObject.getSID() + "'");
            return objectKey;
        }
    }
    // a property the platform draws and React places - an lsf one: the form's controller of it is this entry, over the
    // platform's (`platform`), which draws it. Its values, its drop, focus, the client's own value, any other attribute
    // or reader are the platform's, handed on with the property; what labels it is its kind's - React's for a property,
    // the platform's for an ACTION, whose caption and image are its button's face. No member: what the platform draws,
    // the platform changes. There from the start, and `hidden` whenever the platform does not show the property - it
    // shows it from its first whole delivery until its drop, both of which come through here
    private abstract class LsfPropertyEntry extends PropertyEntry {
        final GPlatformPropertyController platform; // the view it is drawn in

        LsfPropertyEntry(Node node, GPropertyDraw property, GPlatformPropertyController platform) {
            super(node, property);
            this.platform = platform;
        }
        // its values - the platform shows the property from its first whole delivery - and its drop, focus, the
        // client's own value, any other attribute or reader: the platform's
        public void updateValue(GPropertyDraw property, NativeHashMap<GGroupObjectValue, PValue> values, boolean partial) {
            platform.updateValue(property, values, partial);
            if (!partial)
                setShown(true);
        }
        public void dropProperty(GPropertyDraw property) {
            platform.dropProperty(property);
            setShown(false);
        }
        // ... and so is its value in a cell: an edit's, put on top by the platform's editor, before the server answers
        PValue getValueAt(GGroupObjectValue key) {
            return platform.getValueAt(property, key);
        }
        public boolean isPropertyShown(GPropertyDraw property) {
            return platform.isPropertyShown(property);
        }
        public void focusProperty(GPropertyDraw property) {
            platform.focusProperty(property);
        }
        public Pair<GGroupObjectValue, PValue> setLoadingValueAt(GPropertyDraw property, GGroupObjectValue fullCurrentKey, PValue value) {
            return platform.setLoadingValueAt(property, fullCurrentKey, value);
        }
        public void updateAttribute(GExtraPropertyReader reader, NativeHashMap<GGroupObjectValue, PValue> values, boolean partial) {
            platform.updateAttribute(reader, values, partial);
        }
        public void updateOther(GExtraPropertyReader reader, NativeHashMap<GGroupObjectValue, PValue> values, boolean partial) {
            platform.updateOther(reader, values, partial);
        }
    }
    // ... a panel or empty-group one's: keyed by its integration name on its group's node, where a panel property React
    // draws is, and carried by name with them. An <Lsf> places it by the same name (getName), and hides its host by
    // this entry (ContainerState.entryOf): it says whether the form shows it - all that an action's says
    private abstract class LsfPanelEntry extends LsfPropertyEntry {
        LsfPanelEntry(Node node, GPropertyDraw property, GPlatformPropertyController platform) {
            super(node, property, platform);
        }
        void emit(JavaScriptObject entry) {
        }
    }
    // ... a property's: its labels too - caption, image and comment, the platform drawing none of them
    private final class LsfPanelPropertyEntry extends LsfPanelEntry {
        final GPropertyReader[] labelReaders; // its caption, image and comment

        LsfPanelPropertyEntry(Node node, GPropertyDraw property, GPlatformPropertyController platform) {
            super(node, property, platform);
            labelReaders = present(property.captionReader, property.getImageReader(), property.commentReader);
            updateEntry(); // there from the start, hidden until the platform shows the property
        }
        // what labels it: React's
        public void updateLabel(GExtraLabelReader reader, NativeHashMap<GGroupObjectValue, PValue> delivered, boolean partial) {
            keep(reader, delivered, partial);
        }
        void emit(JavaScriptObject entry) {
            emitAttributes(entry, labelReaders, GGroupObjectValue.EMPTY);
        }
    }
    // ... an ACTION's: its caption and image are its button's face, which the platform draws, so what labels it is
    // handed on. The platform's button runs it
    private final class LsfPanelActionEntry extends LsfPanelEntry {
        LsfPanelActionEntry(Node node, GPropertyDraw property, GPlatformPropertyController platform) {
            super(node, property, platform);
            updateEntry(); // there from the start, hidden until the platform shows the action
        }
        // what labels it: the platform's, its button's face
        public void updateLabel(GExtraLabelReader reader, NativeHashMap<GGroupObjectValue, PValue> delivered, boolean partial) {
            platform.updateLabel(reader, delivered, partial);
        }
    }
    // ... a list one's column, whose cells or buttons the platform draws in the rows React draws, carried by name on
    // the node the rows are on: React draws the column from this entry, which says what the property is - an LSF column
    // is filtered and sorted like any other, and the platform drawing its VALUE says nothing about what the value is
    private abstract class LsfColumnEntry extends LsfPropertyEntry {
        LsfColumnEntry(RowsPart rowsPart, GPropertyDraw property, GPlatformPropertyController platform) {
            super(rowsPart.node, property, platform);
        }
        void emit(JavaScriptObject entry) {
            emitPropertyFacts(entry, property);
        }
    }
    // ... a property's: its labels too - caption, image, footer and comment
    private final class LsfColumnPropertyEntry extends LsfColumnEntry {
        final GPropertyReader[] labelReaders; // its caption, image, footer and comment

        LsfColumnPropertyEntry(RowsPart rowsPart, GPropertyDraw property, GPlatformPropertyController platform) {
            super(rowsPart, property, platform);
            labelReaders = present(property.captionReader, property.getImageReader(), property.footerReader, property.commentReader);
            updateEntry(); // there from the start, hidden until the platform shows the property
        }
        // what labels it: React's
        public void updateLabel(GExtraLabelReader reader, NativeHashMap<GGroupObjectValue, PValue> delivered, boolean partial) {
            keep(reader, delivered, partial);
        }
        void emit(JavaScriptObject entry) {
            emitAttributes(entry, labelReaders, GGroupObjectValue.EMPTY);
            super.emit(entry);
        }
    }
    // ... an ACTION's: what labels it is its buttons' face, handed on - an action's image is a button's, row by row
    private final class LsfColumnActionEntry extends LsfColumnEntry {
        LsfColumnActionEntry(RowsPart rowsPart, GPropertyDraw property, GPlatformPropertyController platform) {
            super(rowsPart, property, platform);
            updateEntry(); // there from the start, hidden until the platform shows the action
        }
        // what labels it: the platform's, its buttons' face
        public void updateLabel(GExtraLabelReader reader, NativeHashMap<GGroupObjectValue, PValue> delivered, boolean partial) {
            platform.updateLabel(reader, delivered, partial);
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
    // them, never of the group's box. A group in PANEL view has none anywhere: the server sends it no rows, only its
    // current object, which nothing chooses among rows, so no view gets a node of rows for it, wherever its grid sits
    // (mirrors FormView.rowsScope)
    private GContainer rowsScope(GGroupObject group) {
        return group.viewType.isPanel() ? null : componentScope(group.getDrawComponent());
    }
    // where a COMPONENT is drawn: the react container that draws it, null when the platform does - an lsf one too,
    // whose content is the platform's wherever React places it. A list property has no place of its own: it is drawn
    // as its column, where the rows are, lsf or not (mirrors FormView.componentScope)
    private GContainer componentScope(GComponent component) {
        if (component instanceof GPropertyDraw && ((GPropertyDraw) component).isList)
            return rowsScope(((GPropertyDraw) component).groupObject);
        return component.isLsfView() ? null : placed(component);
    }
    // ... and WHERE THE VIEW NAMES IT, null where none does: the one container a component is named in (getName), and
    // where its entry is. A property is named on its group's node, where React draws its content - a list property's
    // column, lsf or not, where the rows are - or, for an lsf panel one, where React places it, its entry labelling what
    // the platform draws (an ACTION's saying only whether the form shows it: its caption and image are its button's
    // face). Any other component is named at the top level, where React draws or places it - one the design does not
    // name, a user filter's, nowhere (mirrors FormView.nameScope)
    private GContainer nameScope(GComponent component) {
        if (component instanceof GPropertyDraw && ((GPropertyDraw) component).isList) // its column, where the rows are
            return rowsScope(((GPropertyDraw) component).groupObject);
        return component.sID != null ? placed(component) : null;
    }
    // where what a FILTERS box holds is drawn - the box's part being its content -: the react container a view built
    // here draws it in, null elsewhere (mirrors FormView.filtersScope)
    private GContainer filtersScope(GContainer box) {
        GContainer filters = box != null ? box.getChildrenReactPlace() : null;
        return filters != null && containers.get(filters) != null ? filters : null;
    }
    // ... where a FILTER GROUP is drawn as a part of its group: one with filters - one with none has no view -, of a
    // group - none has no node of its own to go on (mirrors FormView.filterGroupScope)
    private GContainer filterGroupScope(GRegularFilterGroup filterGroup) {
        if (filterGroup.filters.isEmpty())
            return null;
        return filterGroup.groupObject != null ? componentScope(filterGroup) : null;
    }
    // ... where the FILTERCONTROLS of a table or a tree is drawn as a part of its table - of a list's grid, or of a
    // tree, one for all its groups: the form asks no other (GFormController.initializeTable) - (mirrors
    // FormView.filterControlsScope)
    private GContainer filterControlsScope(GGridProperty table) {
        return componentScope(table.filterControls);
    }
    // ... and where its TOOLBARSYSTEM is, shown (mirrors FormView.toolbarScope)
    private GContainer toolbarScope(GGridProperty table) {
        return table.toolbar.visible ? componentScope(table.toolbar) : null;
    }
    // the react container a component is in, as long as it has a view: one REMOVEd from the design is still the
    // container of what it held, but nothing built it, so nothing of it is projected. Asked only here, while the form
    // is built - never by a delta: what a reader brings goes to what React has of its owner
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
    // a value the design need not give, null where it gives none: a Java field never set is undefined in compiled JS,
    // and the projection says "none" by null
    private static native void setFieldOrNull(JavaScriptObject object, String field, Object value) /*-{ object[field] = value == null ? null : value; }-*/;
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

    // ===== THE MEMBERS. A member IS the address: it holds the state it stands for - a property member its property's
    // value, a part's member its part - so a call through it names nothing it changes, and its
    // arguments come in a fixed order, the value first, then the row. FormView refused a name that would take a
    // controller verb, a group's own member or `__proto__` when it built the form (claimProjectionName), so none of
    // them is taken here.

    // a controller, with what no single member can own: the batch its members' changes are sent together in, as many as
    // the function makes, of whatever groups and properties - no change of its own, each named by the member that makes
    // it
    private static native JavaScriptObject makeController(ContainerState state) /*-{
        return {
            batch: function (fn) {
                state.@lsfusion.gwt.client.form.design.view.GReactFormData.ContainerState::batch(*)(fn);
            }
        };
    }-*/;

    // the verb of a group whose rows are drawn here, on its member, named the way data.<group> names it: its current
    // object is chosen among its rows, so .change(row) is there, and only there
    private static native void addRowsVerbs(RowsPart part, JavaScriptObject member, String sid) /*-{
        member.change = function (row) {
            return part.@lsfusion.gwt.client.form.design.view.GReactFormData.RowsPart::change(*)(sid + ".change()", row);
        };
    }-*/;

    // ... and on a group of a TREE, its node verbs: a node IS a row, so the verbs that open one are the group's - and
    // those that open all of them, a level, and on the top group the whole tree
    private static native void addNodeVerbs(TreeRowsPart part, JavaScriptObject member, String sid) /*-{
        member.expand = function (row) {
            return part.@lsfusion.gwt.client.form.design.view.GReactFormData.TreeRowsPart::expand(*)(sid + ".expand()", row);
        };
        member.collapse = function (row) {
            return part.@lsfusion.gwt.client.form.design.view.GReactFormData.TreeRowsPart::collapse(*)(sid + ".collapse()", row);
        };
        member.toggle = function (row) {
            return part.@lsfusion.gwt.client.form.design.view.GReactFormData.TreeRowsPart::toggle(*)(sid + ".toggle()", row);
        };
        member.expandAll = function () {
            return part.@lsfusion.gwt.client.form.design.view.GReactFormData.TreeRowsPart::expandAll()();
        };
        member.collapseAll = function () {
            return part.@lsfusion.gwt.client.form.design.view.GReactFormData.TreeRowsPart::collapseAll()();
        };
    }-*/;
    // ... and on the TREE, the verb only the tree has: its current node, which no group's rows say alone, opened with
    // every node under it - by the rows React draws (TreeTablePart), or by the platform's tree, where the toolbar's
    // part opens it (TreeToolbarPart.opened) -, and the tree from the current node's group down, by the platform's
    private static native void addExpandCurrentMember(GTreeController rows, JavaScriptObject member) /*-{
        member.expandCurrent = function () {
            rows.@lsfusion.gwt.client.form.object.table.tree.controller.GTreeController::expandCurrent()();
        };
    }-*/;
    private static native void addExpandTreeMember(GTreeController rows, JavaScriptObject member) /*-{
        member.expandTree = function () {
            rows.@lsfusion.gwt.client.form.object.table.tree.controller.GTreeController::expandTree()();
        };
    }-*/;

    // a LIST STATE's member - the group's sortings, what the group or the tree is FILTERED by, where its FILTERS is -:
    // an array states the items all at once, its order being the priority of sortings, anything else states one of
    // them and leaves the rest - the shape the state has in `data` (ListPart.change). `surface` is the call as a
    // message names it
    private static native JavaScriptObject makeListMember(ListPart part, String surface) /*-{
        return {
            change: function (items) {
                part.@lsfusion.gwt.client.form.design.view.GReactFormData.ListPart::change(*)(surface, items);
            }
        };
    }-*/;

    // a FILTER GROUP, on its group's member: change(index) selects one of its filters - null: none
    private static native JavaScriptObject makeFilterGroupMember(FilterGroupPart part, String qualified) /*-{
        return {
            change: function (index) {
                part.@lsfusion.gwt.client.form.design.view.GReactFormData.FilterGroupPart::change(*)(qualified + ".change()", index === undefined ? null : index);
            }
        };
    }-*/;

    // the FILTER CONTROLS of a group or of a tree, on the table's member: the add, the apply and the reset, as the
    // platform's buttons
    private static native void addFilterControlsMembers(FilterControlsPart part, JavaScriptObject member, String sid) /*-{
        member.addFilter = function () {
            part.@lsfusion.gwt.client.form.design.view.GReactFormData.FilterControlsPart::addFilter(*)(sid + ".addFilter()");
        };
        member.applyFilters = function () {
            part.@lsfusion.gwt.client.form.design.view.GReactFormData.FilterControlsPart::applyFilters(*)(sid + ".applyFilters()");
        };
        member.resetFilters = function () {
            part.@lsfusion.gwt.client.form.design.view.GReactFormData.FilterControlsPart::resetFilters(*)(sid + ".resetFilters()");
        };
    }-*/;

    // a TOOLBAR's, of either table, on the table's member: the filter button's state's member and the export - of the
    // rows, or of the tree -, whatever the design's flags say, which are the platform's. `export` is a word the
    // language reserves, so it is set by a string, and named otherwise here
    private static native void addToolbarMembers(ToolbarPart<?, ?> part, JavaScriptObject member, String sid) /*-{
        member.filterControlsVisible = {
            change: function (shown) {
                part.@lsfusion.gwt.client.form.design.view.GReactFormData.ToolbarPart::changeFilterControlsVisible(*)(sid + ".filterControlsVisible.change()", shown);
            }
        };
        member['export'] = function () {
            part.@lsfusion.gwt.client.form.design.view.GReactFormData.ToolbarPart::exportRows(*)(sid + ".export()");
        };
    }-*/;
    // ... and a group's own, on the group's member: the count and a column's sum, each the Promise of the server's
    // answer, as a request is (getValues), the update the manual update holds back, and the members of the states it
    // writes on the group's node, as `filters`' is - all of them, whatever the design's flags say
    private static native void addGridToolbarMembers(GridToolbarPart part, JavaScriptObject member, String sid) /*-{
        member.getCount = function () {
            return part.@lsfusion.gwt.client.form.design.view.GReactFormData.GridToolbarPart::getCount(*)(sid + ".getCount()");
        };
        member.getSum = function (property) {
            return part.@lsfusion.gwt.client.form.design.view.GReactFormData.GridToolbarPart::getSum(*)(sid + ".getSum()", typeof property === 'string' ? property : null);
        };
        member.manual = {
            change: function (value) {
                part.@lsfusion.gwt.client.form.design.view.GReactFormData.GridToolbarPart::changeManual(*)(sid + ".manual.change()", value);
            }
        };
        member.update = function () {
            part.@lsfusion.gwt.client.form.design.view.GReactFormData.GridToolbarPart::forceUpdate(*)(sid + ".update()");
        };
        member.listView = {
            change: function (name) {
                part.@lsfusion.gwt.client.form.design.view.GReactFormData.GridToolbarPart::changeListView(*)(sid + ".listView.change()", name);
            }
        };
    }-*/;
    // what the server answers a call, as the Promise the call returns: the number - null where there is none -, or
    // the error, which the form shows too
    private static JavaScriptObject answerPromise(BiConsumer<Consumer<Number>, Consumer<Throwable>> ask) {
        return newPromise((resolve, reject) -> ask.accept(
                number -> resolveNumber(resolve, number != null ? number.doubleValue() : null),
                error -> rejectError(reject, error.getMessage())));
    }
    // a call that asks the server and makes no change a batch carries, refused by the form now (Verbs.getRefusal): the
    // reason said on the console - not thrown, as a lookup's refusal is not -, and the call made of nothing; one that
    // answers with a Promise has it rejected with the reason, as a server call's is
    // (GController.rejectedControllerPromise)
    private boolean refused(String surface) {
        String refusal = verbs.getRefusal(surface);
        if (refusal == null)
            return false;
        GwtClientUtils.consoleError(refusal);
        return true;
    }
    private static native JavaScriptObject newPromise(BiConsumer<JavaScriptObject, JavaScriptObject> body) /*-{
        return new $wnd.Promise(function (resolve, reject) {
            body.@java.util.function.BiConsumer::accept(*)(resolve, reject);
        });
    }-*/;
    private static native void resolveNumber(JavaScriptObject resolve, Double value) /*-{
        resolve(value);
    }-*/;
    private static native void rejectError(JavaScriptObject reject, String message) /*-{
        reject(new $wnd.Error(message));
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
        return member;
    }-*/;

    private void emitPropertyFacts(JavaScriptObject entry, GPropertyDraw property) {
        setField(entry, "type", GSimpleStateTableView.getJSTypeName(property.getRenderType(RendererType.SIMPLE)));
        // whether a user may sort by it - asked of the PROPERTY, so a panel property answers it too
        if (property.groupObject != null)
            setField(entry, "noSort", property.noSort);
    }

    // what a condition of user filters can be on - FILTERS(g)'s or FILTERS(TREE t)'s, beside its conditions
    // (FiltersPart): the properties of its groups a condition can be stated on - one a condition can name
    // (isProjectableCondition), but a file, whose condition a view can take off and not state (readFilterCondition) -,
    // wherever they are drawn, group by group, in the form's order, named as `filters` names them, each with what a
    // condition editor is built of - its caption in the design, the type of a condition's value (the FILTER type, by
    // which `value` is read and written), the comparisons the platform's own filter editor offers, in the spelling
    // `filters` uses, and the one it starts with (the design's, which need not be one of them)
    private JavaScriptObject getFilterProperties(OwnerNode node) {
        JavaScriptObject properties = emptyArray();
        for (GGroupObject group : node.getGroups())
            for (GPropertyDraw property : form.propertyDraws)
                if (property.groupObject == group && isProjectableCondition(property)
                        && !(property.getFilterBaseType().getDataType() instanceof GFileType)) {
                    JavaScriptObject entry = newObject();
                    setField(entry, "property", node.getPropertyName(property));
                    setFieldOrNull(entry, "caption", property.caption);
                    setField(entry, "type", GSimpleStateTableView.getJSTypeName(property.getFilterBaseType()));
                    JavaScriptObject compares = emptyArray();
                    for (GCompare compare : property.getFilterCompares()) // the editor's list as it is: the default is
                        push(compares, compare.toString());               // not added to it
                    setField(entry, "compares", compares);
                    setField(entry, "defaultCompare", property.getDefaultCompare().toString());
                    push(properties, entry);
                }
        return properties;
    }

    // the user filters of these groups as the form has them (Verbs.getFilters), in their order - a list of the
    // caller's own, which a call stating ONE condition is merged into, so the form is handed the whole list it makes
    private ArrayList<GPropertyFilter> getConditions(List<GGroupObject> groups) {
        ArrayList<GPropertyFilter> conditions = new ArrayList<>();
        for (GGroupObject group : groups)
            conditions.addAll(verbs.getFilters(group));
        return conditions;
    }
    // the condition a cell starts where a view draws the conditions (ReactFilterInputController.addCondition): on its
    // property and column, compared the property's default way with the cell's value - an empty cell's NULL too. None
    // where the table is on no row - an empty grid, or a pivot, a map, a calendar or a custom view, which have no cell,
    // though they name a column the panel would start an editor on -, on a cell a view cannot name in a condition, or
    // on one whose value is not the one it filters by (differentValue)
    static GPropertyFilter getCellCondition(GTableController rows) {
        GPropertyDraw property = rows.getSelectedFilterProperty();
        if (property == null || rows.getSelectedRow() < 0)
            return null;
        if (!isProjectableCondition(property) || property.differentValue)
            return null;
        GGroupObjectValue columnKey = rows.getSelectedColumnKey();
        if (columnKey == null)
            columnKey = GGroupObjectValue.EMPTY;
        GCompare compare = property.getDefaultCompare();
        return new GPropertyFilter(new GFilter(property), property.groupObject, columnKey,
                PValue.escapeSeparator(rows.getSelectedValue(property, columnKey), compare), compare);
    }
    // ... which takes the place of the first condition on its property - so the other conditions keep their places and
    // a range's other bound stays -, or is appended
    static ArrayList<GPropertyFilter> withCondition(ArrayList<GPropertyFilter> conditions, GPropertyFilter condition) {
        ArrayList<GPropertyFilter> result = new ArrayList<>(conditions);
        for (int i = 0; i < result.size(); i++)
            if (result.get(i).column.property.equals(condition.column.property)) {
                result.set(i, condition);
                return result;
            }
        result.add(condition);
        return result;
    }

    // can a sorting be on this property - and so be listed? Two rules: the projection addresses a property by its
    // integration SID, and a sorting names one property, not one of its COLUMNS - a property grouped in columns is
    // sorted per column, and no name here says which. The property may be the platform's to draw, so the form being
    // built says nothing about it. A sorting failing it is not listed (OrdersPart.writeOrders) - and a view naming
    // one is refused (OrdersPart.readSortingProperty), by the same rule
    private static boolean isProjectableSorting(GPropertyDraw property) {
        return property.integrationSID != null && !property.hasColumnGroupObjects();
    }
    // ... and which of them it fails, in words
    private static String whyNotSorting(GPropertyDraw property) {
        if (property.integrationSID == null)
            return "no integration SID";
        return "it is grouped in columns, and a sorting names no column";
    }
    // can a condition be on this property - and so be listed? Three rules, each said where it belongs: the projection
    // addresses a property by its integration SID; a filter is on something that can be filtered by
    // (GPropertyDraw.canBeFiltered, which GFormController.getFilterDTOs asks too); and a condition names
    // one property, not one of its COLUMNS - a property grouped in columns is filtered per column, and no name here
    // says which. Its rows may be the platform's, so the form being built says nothing about it. A condition failing it
    // is not listed (FiltersPart.writeFilters)
    // - and a view naming one is refused (FiltersPart.readFilterProperty), by the same rule
    private static boolean isProjectableCondition(GPropertyDraw property) {
        return property.integrationSID != null && property.canBeFiltered() && !property.hasColumnGroupObjects();
    }
    // ... and which of them it fails, in words
    private static String whyNotCondition(GPropertyDraw property) {
        if (property.integrationSID == null)
            return "no integration SID";
        if (!property.canBeFiltered())
            return "an action, which the server is never asked to filter by";
        return "it is grouped in columns, and a condition names no column";
    }

    // what the author typed, as a message names it: every controller entry point is given the member path it was
    // reached by ("g.change()", "g.qty.change()", "batch()"), so an error quotes the call that is in the view's source
    // instead of a verb name the surface no longer has
    public static String controllerPrefix(String surface) {
        return "controller." + surface + ": ";
    }

    // ===== WHAT A STATE MEMBER READS - a condition (the user filters) and a sorting: the object the author wrote,
    // named by its `property`, read by one rule for both (readAuthorObject), the name by one lookup for both, the
    // group's (readStateProperty) - and a member reads its call as a property member reads its own, and hands the form
    // (Verbs) only what the call names
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

    // the property a STATE names - a sorting, a condition, a column the toolbar sums -: the group's ONE property of
    // that name, wherever it is drawn - by this view, by another container, by the platform. A state is the group's,
    // and its part need not be where an entry of the property is - FILTERS(g) with a view of its own over the
    // platform's grid is where no node names a column, and the rows' node carries no panel property drawn elsewhere -,
    // so the name is the group's, not a node's. One is all there can be: a form that projects a part of a group's state
    // is not built with a name the group carries on two properties (FormView.checkStateNames)
    private GPropertyDraw readStateProperty(String errorPrefix, GGroupObject group, String propertySID) {
        assert form.countPropertyDraws(group, propertySID) <= 1;
        GPropertyDraw property = form.getPropertyDraw(group, propertySID);
        if (property == null)
            throw new RuntimeException(errorPrefix + "unknown property '" + propertySID + "' of object group '"
                    + group.getSID() + "'");
        return property;
    }

    // a condition with no value - "not filtered by this" (isNoValue) - states none, and the rest of it is read all the
    // same: a mistake in a condition that only takes others off is still a mistake, and dropping it would hide it
    private void checkFilterFields(String errorPrefix, JavaScriptObject condition, GPropertyDraw property) {
        readFilterCompare(errorPrefix, condition, property);
        readFlag(errorPrefix, condition, "negation");
        readFlag(errorPrefix, condition, "or");
    }

    // ... and one with a value, which the caller read - each field is read once, so the condition is what was read -,
    // on the property already resolved: the singular call needs the property first, as it merges by property. The
    // value is the ONE field whose kind is not checked here: it is converted by the property's own FILTER type, the
    // way `filters` gives it - so a condition `filters` lists is taken back as it is. But for a file's: `filters` says
    // a link to the file, which is not the file a condition compares with, so a view can take one off, not state one
    private GPropertyFilter readFilterCondition(String errorPrefix, JavaScriptObject condition, GPropertyDraw property,
                                                JavaScriptObject value) {
        if (property.getFilterBaseType().getDataType() instanceof GFileType)
            throw new RuntimeException(errorPrefix + "'" + property.integrationSID + "' is a file: a condition on it has"
                    + " a link to the file for its value, which is not the file, so it can be taken off, not stated");
        GCompare compare = readFilterCompare(errorPrefix, condition, property);
        boolean negation = readFlag(errorPrefix, condition, "negation");
        boolean or = readFlag(errorPrefix, condition, "or");
        return new GPropertyFilter(new GFilter(property), property.groupObject, GGroupObjectValue.EMPTY,
                GSimpleStateTableView.convertFromJSValue(property.getFilterBaseType(), value), negation, compare, !or);
    }

    // no `compare` at all is how a view says "the property's own default", which is what it is read as; one that is
    // there but is not the operator string it is spelled with - a number, a boolean - is a mistake, and reading it as
    // absent would filter by something else without a word
    private GCompare readFilterCompare(String errorPrefix, JavaScriptObject condition, GPropertyDraw property) {
        if (!hasOwnField(condition, "compare"))
            return property.getDefaultCompare();
        String compareSID = getOwnString(condition, "compare");
        if (compareSID == null)
            throw new RuntimeException(errorPrefix + "'compare' must be the operator as a string");
        GCompare compare = GCompare.get(compareSID);
        if (compare == null)
            throw new RuntimeException(errorPrefix + "unknown compare '" + compareSID + "'");
        if (compare == GCompare.INARRAY) // a condition compares with one value - FILTER refuses it the same way
            throw new RuntimeException(errorPrefix + "'" + compareSID + "' is no comparison of a filter condition");
        return compare;
    }

    // a flag is a boolean or absent - a truthy string like "false" would otherwise mean true: a condition's `negation`
    // and `or`. A sorting's `desc` is not a flag: an order with none takes its sorting off (OrdersPart.applyOrder)
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
