package lsfusion.gwt.client.form.design.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;

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
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.interop.action.ServerResponse;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import lsfusion.gwt.client.classes.data.GIntegralType;
import lsfusion.gwt.client.form.GUpdateMode;
import lsfusion.gwt.client.form.filter.GFilterGroupController;
import lsfusion.gwt.client.form.filter.GRegularFilter;
import lsfusion.gwt.client.form.filter.GRegularFilterGroup;
import lsfusion.gwt.client.form.event.GChangeSelection;
import lsfusion.gwt.client.form.event.GInputBindingEvent;
import lsfusion.gwt.client.form.filter.user.controller.GFilterControlsController;
import lsfusion.gwt.client.form.object.table.controller.GGridToolbarController;
import lsfusion.gwt.client.form.object.table.controller.GListViewController;
import lsfusion.gwt.client.form.object.table.controller.GToolbarController;
import lsfusion.gwt.client.form.object.table.grid.controller.GGridController;
import lsfusion.gwt.client.form.object.table.grid.view.GListViewType;
import lsfusion.gwt.client.form.object.table.tree.controller.GTreeController;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.filter.user.controller.GFilterInputController;
import lsfusion.gwt.client.form.filter.user.controller.GFilterControlsToggle;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.table.grid.GGrid;
import lsfusion.gwt.client.form.object.table.grid.view.GGridSelection;
import lsfusion.gwt.client.form.object.table.grid.view.GGridTable;
import lsfusion.gwt.client.form.object.table.view.GridDataRecord;
import lsfusion.gwt.client.form.object.table.grid.GGridProperty;
import lsfusion.gwt.client.form.object.table.tree.GTreeGroup;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GComponentController;
import lsfusion.gwt.client.form.object.table.controller.GGroupController;
import lsfusion.gwt.client.form.object.table.controller.GTableController;
import lsfusion.gwt.client.form.object.table.controller.GUserOrdersController;
import lsfusion.gwt.client.form.object.table.controller.GPropertyController;
import lsfusion.gwt.client.form.filter.user.GCompare;
import lsfusion.gwt.client.form.filter.user.GFilter;
import lsfusion.gwt.client.form.filter.user.view.GFiltersHandler;
import lsfusion.gwt.client.form.filter.user.GPropertyFilter;
import lsfusion.gwt.client.form.filter.user.GUserFilter;
import lsfusion.gwt.client.form.filter.user.GUserFiltersController;
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
import lsfusion.gwt.client.form.property.JSValueConverter;

// The projection of a form's React views - each of a component whose custom is a React component: a react container,
// or a grid, a tree, a toolbar, filter controls, a filter group of its own. Each view owns its state (ViewState): its
// nodes and its published snapshot - what its view is given - and its controller, whose members mirror that
// snapshot's entries - what the view changes them through. What React has of each group, property and component - its
// rows' part, its entry, its descriptor - is the form's controller of it (GFormController.initializeControllers): made
// here once, from the platform's controller of it, and handed all the form sends for it, which it keeps itself: the
// values of the readers it draws from (ReaderValues), its rows, whether the form shows it. The projection keeps the
// views, and one flush for all of them.
// Placement is design data, and so is the SHAPE of every snapshot: an entry a view carries is there from the start, and
// whether the form shows it now is its `hidden`, never its presence. A part of a group is projected where its component
// is drawn - the rows where the grid (or the tree) is, a panel property where the property is; what a component with
// a React view of its own shows, in that view. The form level is the empty group: its node is each view's top level.
// Every other component React draws or places has a
// descriptor - its entry at that top level, keyed by its SID, where a property carried by name is keyed by that name -
// one the platform draws included, which React places: a platform-drawn ACTION's says only whether the form
// shows it, its caption and image being its buttons' face, which the platform draws. What an entry says is what the
// server sent, as the platform keeps it for its own components: nothing here decides that something is hidden or
// empties it. A property the form does not show is dropped by the server, and its entry has no value; a component the
// form does not show has its SHOWIF, and its descriptor says so - the rows of a hidden grid stay as the server last
// sent them, as the platform's own grid keeps them. Like any attribute, `hidden` is there only while it has a value:
// while the form does not show the entry's component. The model, top down: a view's state (ViewState) and its nodes
// - its top level (TopNode), and an owner's node, a group's or a tree's (GroupNode, TreeNode) -; on an owner's node,
// one part for each of the owner's components the view draws (Part), and on a node the entries of the
// properties and components it carries (Entry); the members that mirror them (THE MEMBERS); and what a view's author
// wrote, read by one rule (WHAT A STATE MEMBER READS).
public class GReactFormData {
    private final GForm form;
    // GComponent.getReactPlace - handed in, so a test sees who asks it
    private final Function<GComponent, GComponent> place;
    private final Verbs verbs; // what the parts ask of the form (GFormController)
    private final Bindings bindings;
    private final NativeSIDMap<GComponent, ViewState> views = new NativeSIDMap<>();
    private NativeSIDMap<GComponent, ViewState> pending = new NativeSIDMap<>();

    // WHEN WHAT IS WRITTEN IS PUBLISHED: a controller writes and never publishes; what is written is pending. What a
    // CHANGE writes - a call of a view's member, a server's delta applied (change) - is published once the outermost
    // one is over, at once: before the view's event handler returns, as React puts a controlled input back to what the
    // store says once the handler is done, and before a window measured when it is shown measures the form's first
    // changes (ReactRoot.mount). What is written outside a change - by an event of the platform's - is published when
    // the GWT entry it is written in exits (Scheduler.scheduleFinally), as the platform's own views defer what they
    // draw (DataGrid's DOM, the forms window)
    private int changing; // the changes under way

    public GReactFormData(GForm form, Function<GComponent, GComponent> place, Verbs verbs, Bindings bindings) {
        this.form = form;
        this.place = place;
        this.verbs = verbs;
        this.bindings = bindings;
    }

    // WHAT A MEMBER DOES: the form's own edit, the one a user makes, or a request of the form's own that changes
    // nothing - a lookup, a count, an export -, which a batch does not carry (GFormController.getBatchOrClosedRefusal); and what the parts ask of
    // the form besides: its sortings and filters as it has them, the widget of the view that draws one, a part published
    // later. The members are made here, beside the entries they mirror, and each holds the state it stands for (a
    // ReactPropertyEntry, a Part): a call through one reads what it names - the row, the value - from that state, and
    // hands the form the edit, which the form shows at once and sends at once or, inside a batch, when the batch ends
    // (batch). The user filters of each table are its model's (GUserFiltersController), sent through its Transport
    public interface Verbs {
        // ===== the current object of a group whose rows a view draws: one of those rows - and, of a grid's group, what
        // becomes of the rows selected (readSelection), null: the range stays, with what that changes of the rows the
        // view has, shown already (GridRowsPart.updateRowSelection) ...
        JavaScriptObject changeCurrentObject(GGroupObject group, GGroupObjectValue key, GChangeSelection selection, NativeHashMap<GGroupObjectValue, PValue> selectionRows);
        // ... the first or the last of a grid's rows, which the server finds ...
        JavaScriptObject scrollCurrentObject(GGroupObject group, boolean toEnd, GChangeSelection selection);
        // ... every row of it selected, and its current column, the form's active property - each with what it
        // changes of the columns the form reads as selected, the range of them being the grid's own
        // (GridRowsPart.updateColumnSelection)
        JavaScriptObject selectAllRows(GGroupObject group, NativeHashMap<GGroupObjectValue, PValue> selectionRows, ArrayList<GGridTable.ColumnSelection> selection);
        JavaScriptObject changeActiveProperty(GGroupObject group, GPropertyDraw property, ArrayList<GGridTable.ColumnSelection> selection);
        // ... and its clipboard: the cells selected - the rows the server has as selected, in these columns - copied,
        // the Promise of their text; the Promise of a text the view has itself, its current row's; and a table of
        // values pasted into these columns from the current row on, one value into every row selected (groupChange)
        // - none of them a batch's
        JavaScriptObject copySelection(String surface, ArrayList<GPropertyDraw> properties);
        JavaScriptObject copyText(String surface, String text);
        JavaScriptObject pasteSelection(String surface, ArrayList<GPropertyDraw> properties, List<List<String>> table, List<List<String>> patterns, boolean groupChange);
        // ===== a property: its cell - its key (EMPTY: the current objects), its value (PValue.UNDEFINED: its change
        // event, or the action) and what the cell shows until the answer (PValue.UNDEFINED: the value it has) -, the
        // Promise of the change's request returned, and the Promise of the cell's suggestion list, from the server's
        // lookup `actionSID`
        JavaScriptObject changeProperty(GPropertyDraw property, GGroupObjectValue key, PValue value, PValue shownValue);
        JavaScriptObject getPropertyValues(String surface, GPropertyDraw property, GGroupObjectValue key, String value, String actionSID, int increaseValuesNeededCount);
        // ===== the nodes of a tree whose rows a view draws: one of a group's rows opened ...
        JavaScriptObject expandNode(GGroupObject group, GGroupObjectValue key);
        // ... or closed
        JavaScriptObject collapseNode(GGroupObject group, GGroupObjectValue key);
        // ... every node of such a group and of the groups below it, opened ...
        JavaScriptObject expandAll(GGroupObject group);
        // ... or closed
        JavaScriptObject collapseAll(GGroupObject group);
        // ... and the tree's current node, with every node under it - what the tree's own member asks, and the
        // platform's toolbar's button over these rows (TreeTablePart): an edit, as expandAll is
        JavaScriptObject expandCurrent(GGroupObject group, GGroupObjectValue key);
        // ... and a tree's nodes opened by what draws its rows (expand, through GTreeController), the request it sends
        // the one in flight for these groups
        JavaScriptObject expandNodes(List<GGroupObject> groups, Runnable expand);
        // ===== the sortings of a table whose rows a view draws - a grid's group's, or a tree's, top down -, as its
        // member read them (OrdersTableList.changeAll): the whole list, each sorting on the property it resolved, of that
        // property's group, true - ascending; each group's its own list, in its priority order, which the form shows at
        // once and sends, each group's as a group's is
        JavaScriptObject changeOrders(List<GGroupObject> groups, LinkedHashMap<Column, Boolean> orders);
        // ... and a group's as the form has them - what one order its member states is merged into
        // (OrdersTableList.changeOne), as the platform's header merges a click into them: the form's own map, read, never
        // changed
        LinkedHashMap<Column, Boolean> getOrders(GGroupObject group);
        // ===== the user filters of a table's groups changed through their model (change - GUserFiltersController, the
        // filter input controllers -, which sends the request), the request the one in flight for these groups
        JavaScriptObject changeUserFilters(List<GGroupObject> groups, Runnable change);
        // ===== a filter group of a group: the filter selected in it - null: none -, as its member read it
        JavaScriptObject changeRegularFilter(GRegularFilterGroup filterGroup, GRegularFilter filter);
        // ===== the view filters of a list group whose rows a view draws - the conditions the view states of its own,
        // beside the table's user filters -, as its member read them (changeViewConditions): the whole list, which the
        // form sends and tells what draws the rows of (GGroupController.changeViewFilters) - with no page size, which
        // only a custom grid view states with them
        JavaScriptObject changeViewFilters(GGroupObject group, List<GPropertyFilter> conditions, Integer pageSize);
        // ===== what a toolbar asks where a view draws it - requests: a group's (GridToolbarPart), the
        // count of its rows and the sum of a column of them, answered or failed, ...
        JavaScriptObject countRecords(String surface, GGroupObject group);
        JavaScriptObject calculateSum(String surface, GGroupObject group, GPropertyDraw property, GGroupObjectValue columnKey);
        // ... the export of its rows, the server's report of them - and of a tree's, where a view draws the tree's
        // toolbar (TreeToolbarPart) ...
        JavaScriptObject runTableReport(String surface, GGridProperty table);
        // ... and its update mode: manual, automatic, or the changes the server holds back brought now
        JavaScriptObject changeUpdateMode(GGroupObject group, GUpdateMode updateMode);
        JavaScriptObject changeListView(GListViewController views, GListViewType view);
        // ... and whether its update is manual, as the form keeps it (GFormController.isManualUpdate)
        boolean isManualUpdate(GGroupObject group);
        // ... and a view's batch: run, the edits made in it shown at once and sent together, in one request, when it
        // ends - when it throws too -, a call being made when it is made, the Promise of that request returned - the
        // one each change made in it returns. A batch begun inside it is a part of it; a server call on a controller,
        // or a lookup, is refused while it runs
        JavaScriptObject batch(Runnable run);
    }

    // The views' bindings and the projection's publish event.
    public interface Bindings {
        // ===== the widget of the React view that draws what scope holds: the keys of what the view draws are taken
        // with it (the form binds them, through their controllers: FilterGroupPart, PropertyEntry), as a widget of the
        // platform's takes its own
        Widget getViewWidget(GComponent scope);
        // ===== what a binding of a property a view draws does (ReactPropertyEntry): its change event, as the platform's
        // binding of a cell runs it, for the current objects
        void pressProperty(GPropertyDraw property, Event event);
        // ... and what shows the form's asynchronous requests (GFormController.showAsync): the entry of the property
        // that shows them, drawAsync - the Refresh button -, where React draws it (ReactPanelPropertyEntry), as the
        // platform's button of it gives itself (ActionPanelRenderer)
        void setAsyncView(Consumer<Boolean> asyncView);
        void published();
    }

    // A BATCH begun from JS - a React view's controller.batch, the form's controller.form.batch, a custom grid view's
    // controller.batch (GSimpleStateTableView): fn, a function, run in the form's batch (Verbs.batch), which sends what
    // it makes when it returns - what was made before a throw too, as it would have gone outside a batch -, the Promise
    // of its request returned. The batch is the form's, whoever begins it: a member call of any view made in fn goes
    // with it (the form's server calls are refused while it runs)
    public static JavaScriptObject batch(Verbs verbs, String surface, JavaScriptObject fn) {
        String errorPrefix = controllerPrefix(surface);
        if (!isFunction(fn))
            throw refusal(errorPrefix + "takes a function, the member calls it makes being sent together");
        // fn, run at once: what follows an await in it would be made after the batch has ended, so a Promise it
        // returns is refused - what it made before is sent -, but the batch's own, which a call made in it returns,
        // and so a function written as that call (() => member.change(v))
        JavaScriptObject[] returned = new JavaScriptObject[1];
        JavaScriptObject promise = verbs.batch(() -> returned[0] = GwtClientUtils.call(fn));
        if (isThenable(returned[0]) && returned[0] != promise)
            throw refusal(errorPrefix + "the function returned a Promise: a batch is sent when it returns, so"
                    + " what follows an await in it would be made outside it. Await before the batch");
        return promise;
    }

    // a React view's state, made with the view, and its controller with it
    public ViewState addView(GComponent scope, Consumer<JavaScriptObject> publish) {
        ViewState state = new ViewState(scope, publish);
        views.put(scope, state);
        state.enlist(); // published with what the form is set out with - empty / static-only views too
        return state;
    }

    // ===== THE FORM'S CONTROLLER OF EACH OWNER, settled once, when every React view is there, from the platform's
    // controller of it (GFormController.initializeControllers): placement is design data. The groups first, as their
    // tables are set up - what draws the rows gives each group's controller and what shows the groups' user orders
    // (GTableController.getGroupController, getUserOrdersController) -, then the properties, in the form's order, the
    // order of the names a node carries, then the components, then the filter groups
    // ... of a property, ALL of it - its values and what labels it alike: an entry, keyed by its integration name on
    // the node of its group, where React draws it - NONE where it carries it by no name, the platform's own COUNT of a
    // list group, the pivot's: React keeps nothing of it (reactKeepsNothing); an entry over the platform's,
    // keyed the same way, where the platform draws it and React places it - a panel property React places, the column
    // of the rows React draws -, which keeps what labels the property and hands the rest on, an action's labels too,
    // its button's face; else the platform's
    public GPropertyController createPropertyController(GPropertyDraw property, PlatformProperty platform) {
        if (reactKeepsNothing(property))
            return GPropertyController.NONE;
        GComponent scope = nameScope(property);
        if (scope == null || !hasEntry(property)) // ... and a platform-drawn one with no entry brings no node either
            return platform;
        ViewState state = views.get(scope);
        if (property.isList) { // its content is its column, with its group's rows, on their node
            RowsPart rows = state.getRowsPart(property.groupObject);
            if (!property.isPlatformDrawn())
                return new ReactColumnPropertyEntry(rows, property);
            return property.isAction() ? new PlatformColumnActionEntry(rows, property, platform) : new PlatformColumnPropertyEntry(rows, property, platform);
        }
        // a panel property is a part of its group: on the group's node here, the empty group's on the top level
        Node node = property.groupObject == null ? state.top : state.getOrCreateGroupNode(property.groupObject);
        if (!property.isPlatformDrawn())
            return new ReactPanelPropertyEntry(node, property);
        return property.isAction() ? new PlatformPanelActionEntry(node, property, platform) : new PlatformPanelPropertyEntry(node, property, platform);
    }
    // ... the platform's controller of a property where the platform draws it, as the projection asks it
    // (GPlatformPropertyController): the form's controller of it - an entry over it hands it what the entry does not
    // keep -, and the value it has for a cell, what React's rows answer of a platform-drawn column's cell
    // (PlatformPropertyEntry)
    public interface PlatformProperty extends GPropertyController {
        PValue getValueAt(GPropertyDraw property, GGroupObjectValue fullCurrentKey);
    }
    // whether a property a view names has an entry there: one it carries by a name - a platform-drawn ACTION too, whose
    // entry says whether the form shows it -, not the one React draws by none, the group's own COUNT. Any other has a
    // name, the form being refused without one (FormView.checkReactProjectionNames). A group's node is where an entry
    // of it is (mirrors FormView.hasEntry)
    private static boolean hasEntry(GPropertyDraw property) {
        return property.integrationSID != null;
    }
    // ... and WHAT REACT KEEPS NOTHING OF as a property, so nothing is sent for it (mirrors FormView.reactKeepsNothing):
    // one a view names and carries by no name, the group's own COUNT, unless the platform draws it. Asked of a property
    // as a property: as a component it is asked the component's (createComponentController)
    private boolean reactKeepsNothing(GPropertyDraw property) {
        return !hasEntry(property) && isReactDrawn(property);
    }
    // whether a React view draws the property from its data - a column of the rows it draws, or a panel property
    // standing in it -, not the platform, whose property a view only places (mirrors FormView.isReactDrawn)
    private boolean isReactDrawn(GPropertyDraw property) {
        return nameScope(property) != null && !property.isPlatformDrawn();
    }
    // ... of a component: NONE of what React keeps nothing of (reactKeepsNothing). A property's is the platform's,
    // whatever draws the property - a property is no owner as a component, all of it being its property controller's
    // (above), and the one thing sent for it as a component, its class (a design showIf on a property is the
    // property's SHOWIF), is the platform's to apply to the view it draws the property in: none where React draws it.
    // Of any other: of one React draws, all that is sent for it; of a platform-drawn one React places, its descriptor -
    // its labels and its SHOWIF - where the design names the component, over the platform's; of one it places by no
    // name, nothing: all of it is the platform's. `platform` is the platform's controller of the component: the layout
    public GComponentController createComponentController(GComponent component, GComponentController platform) {
        if (reactKeepsNothing(component))
            return GComponentController.NONE;
        GComponent scope = component instanceof GPropertyDraw ? null : nameScope(component);
        return reactOrPlatform(scope, state -> component.isPlatformDrawn() ? new PlatformComponentEntry(state, component, platform) : new ReactComponentEntry(state, component), () -> platform);
    }
    // ... and WHAT REACT KEEPS NOTHING OF as a component, which the design gives nothing to send:
    // a component React draws that the design does not name - a grid's record -, no descriptor, no entry to
    // go to
    private boolean reactKeepsNothing(GComponent component) {
        return component.sID == null && !component.isPlatformDrawn() && placed(component) != null;
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
    // ... and the USER FILTERS of a table - its groups', the grid's group or the tree's, one FILTERS box for all of
    // them -: their model (GUserFiltersController), there with no box too, sending through the form (Transport), its manual
    // apply mode by the apply button the toggle shows; and what draws each of what the design has of them, chosen here,
    // once, as for any component - React or the platform -, each a view of the model: each property's FILTER -
    // FILTER(<property>), the user filter declared on a property of a group -, a part of the group of its property on
    // its node where the filter is drawn (filterScope, FilterPart), else the platform's row of it
    // (PlatformFilters.addFilter), none of one the design removes, itself or with a container holding it; the FILTERS
    // box, its part on the table's node where what it holds is drawn (filtersScope, FiltersPart) with the input over
    // the conditions a view draws, which takes the platform's - the keys of the rows, the controls -
    // (ReactFilterInputController), else the platform's rows of the box, its filter panel, with the panel's input -
    // (platform, PlatformFilters). The platform's are none where its toolbar turns its filters off - nor an input
    // then, over a box it would draw -, driven with the table by the toggle (GFilterControlsToggle), which is bound to
    // the input once all of them are made. The input works on what draws the rows (rows), and the toggle shows the
    // controls and the toolbar's button (GFormController.initializeTable); a group in PANEL view has none
    // (GUserFiltersController.NONE)
    public FiltersControllers createFiltersControllers(GGridProperty table, GTableController rows, GUserFiltersController.Transport transport, GFilterControlsToggle toggle, PlatformFilters platform) {
        GUserFiltersController userFilters = new GUserFiltersController(table.getGroups(), table.filters, transport, toggle);
        for (GFilter filter : table.filters) {
            GUserFiltersController.View view = reactOrPlatform(filterScope(filter),
                    state -> new FilterPart(state.getOrCreateGroupNode(filter.property.groupObject), filter, userFilters),
                    () -> filter.isInForm() ? platform.addFilter(filter, userFilters) : null);
            if (view != null)
                userFilters.addView(view);
        }
        GFilterInputController input = reactOrPlatform(filtersScope(table.filtersContainer), state -> {
            userFilters.addView(new FiltersPart(state.getOrCreateTableNode(table), table, userFilters));
            return new ReactFilterInputController(rows, toggle, userFilters);
        }, () -> platform.addBox(table.filtersContainer, userFilters));
        if (input != null)
            toggle.bind(input, userFilters, platform);
        FiltersControllers filters = new FiltersControllers(input, userFilters);
        tableFilters.put(table, filters);
        return filters;
    }
    // ... what of a table's user filters THE PLATFORM draws, as the projection asks it where it answers that the
    // platform draws one of them (GPlatformFilterRows), and as the toggle drives it (GFilterControlsToggle.Rows): the
    // row of a FILTER, a view of the model; the box's rows with the panel's input, the box's a view of the model. NONE
    // where the table's toolbar turns the platform's filters off: it draws nothing, nor gives an input over a box it
    // would draw
    public interface PlatformFilters extends GFilterControlsToggle.Rows {
        GUserFiltersController.View addFilter(GFilter filter, GUserFiltersController userFilters);
        GFilterInputController addBox(GContainer filtersContainer, GUserFiltersController userFilters);

        PlatformFilters NONE = new PlatformFilters() {
            public GUserFiltersController.View addFilter(GFilter filter, GUserFiltersController userFilters) {
                return null;
            }
            public GFilterInputController addBox(GContainer filtersContainer, GUserFiltersController userFilters) {
                return null;
            }
            public boolean drawsRows() {
                return false;
            }
            public boolean hasBoxRows() {
                return false;
            }
            public void setControlsVisible(boolean visible) {
            }
            public void update() {
            }
            public void setVisible(boolean visible) {
            }
            public void destroy() {
            }
        };
    }
    // ... which the form takes apart
    public static final class FiltersControllers {
        public final GFilterInputController input;
        public final GUserFiltersController userFilters;

        FiltersControllers(GFilterInputController input, GUserFiltersController userFilters) {
            this.input = input;
            this.userFilters = userFilters;
        }
    }
    // what the form's controller reaches a TABLE by (CONTROLLER.FORM), kept as the projection's factories make it, as
    // a member is given what it works on as it is made: what draws each table's rows, of the table's kind, whichever
    // answered (gridRows, treeRows - createGridController, createTreeController), and each table's user filters, their
    // model and the input over them (tableFilters - createFiltersControllers)
    private final NativeSIDMap<GGrid, GGridController> gridRows = new NativeSIDMap<>();
    private final NativeSIDMap<GTreeGroup, GTreeController> treeRows = new NativeSIDMap<>();
    private final NativeSIDMap<GGridProperty, FiltersControllers> tableFilters = new NativeSIDMap<>();
    // WHAT DRAWS THE ROWS of a TABLE, the form's one question for them, asked with its toolbar
    // (GFormController.initializeGrid, initializeTree): of the GRID of a list's group not in a tree, where React draws
    // them, its table's part on the group's node there, with its group's rows' part ...
    public GGridController createGridController(GGrid grid, GToolbarController<GGridController> toolbar, Supplier<GGridController> platform) {
        return createTableController(grid, gridRows, toolbar, state -> state.createGridTablePart(grid), platform);
    }
    // ... of a TREE, one for all its groups: where React draws them, its table's part on the tree's node there
    // (FormView.getTreeScopes), with the rows' part of each of its groups, which it keeps ...
    public GTreeController createTreeController(GTreeGroup tree, GToolbarController<GTreeController> toolbar, Supplier<GTreeController> platform) {
        return createTableController(tree, treeRows, toolbar, state -> state.createTreeTablePart(tree), platform);
    }
    // ... either: the table's part, of the table's kind (part), given to the toolbar as it is made, as the platform's
    // table gives itself to it, where React draws the rows - where the table is drawn -, else the platform's table
    // (platform), made only then; kept by the table, whichever answered (rows)
    private <T extends GGridProperty, R extends GTableController> R createTableController(T table, NativeSIDMap<T, R> rows, GToolbarController<R> toolbar, Function<ViewState, R> part, Supplier<R> platform) {
        R drawn = reactOrPlatform(componentScope(table), state -> {
            R made = part.apply(state);
            toolbar.addRows(made);
            return made;
        }, platform);
        rows.put(table, drawn);
        return drawn;
    }
    // ... the one choice each of the factories above makes: what React has of a component it draws in a React view
    // (scope) - a part of its owner there, or an entry, made by react -, else the platform's (platform), made only then
    private <C> C reactOrPlatform(GComponent scope, Function<ViewState, C> react, Supplier<C> platform) {
        return scope != null ? react.apply(views.get(scope)) : platform.get();
    }
    // THE ROWS A GROUP'S PER-ROW RENDERERS FOLLOW (GPlatformRowsPanelController): a group's platform-drawn list
    // properties are drawn per row only where React draws the group's rows (FormView.isPlatformDrawn), so what they
    // follow is the rows' part
    public interface Rows {
        // its rows as the view has them, its own add or remove included - the part's own list, which it replaces and
        // never changes: a list handed out keeps saying what it said
        ArrayList<GGroupObjectValue> getRows();
    }
    // ... of a group whose rows React draws, handed out once, while the form is built: placement is asked here, and
    // then never again
    public Rows getRows(GGroupObject group) {
        return views.get(rowsScope(group)).getRowsPart(group);
    }
    // ... and then what the nodes carry of it all, once: a group's rows are there from the start, none of them yet; the
    // names every node carries; the members that mirror its entries - and the form's controller, on each view's as
    // `form`, with its methods (CONTROLLER.FORM), its calls made once, as each view's own are: a call of it from a
    // view's handler is a change too
    public void initialize(JavaScriptObject formController) {
        views.foreachValue(state -> state.initialize(formController));
        addFormMethods(this, formController);
        makeCalls(this, formController);
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

    // a CHANGE of what the projection holds, published once the outermost one is over - when it throws too
    public void change(Runnable change) {
        changing++;
        try {
            change.run();
        } finally {
            changed();
        }
    }
    // ... a member's call, a change as a view makes it (makeCalls): entered, and over
    private void call() {
        changing++;
    }
    private void changed() {
        if (--changing == 0)
            publish();
    }
    // what is pending, published - nothing where nothing is
    public void publish() {
        if (!pending.isEmpty())
            flush();
    }

    // Commit every draft, then notify subscribers: what a commit writes - the rows of its tables placed
    // (TablePart.place) - is in this publication, its container pending already. The batch is detached before the
    // notifications, so that edits made by a subscriber get their own draft and remain pending for the next one.
    public void flush() {
        pending.foreachValue(ViewState::commit);
        NativeSIDMap<GComponent, ViewState> batch = pending;
        pending = new NativeSIDMap<>();
        batch.foreachValue(state -> {
            if (state.data != state.lastPublished) { // a nested flush may have given it out already
                state.lastPublished = state.data;
                state.publish.accept(state.data);
            }
        });
        bindings.published();
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
        // what these readers say of it, for a key; the owner - what it stands for, a component or a property, which is
        // a component too - is whose static attributes a reader falls back to
        void emitAttributes(JavaScriptObject entry, GPropertyReader[] readers, GGroupObjectValue key, GComponent owner) {
            for (GPropertyReader reader : readers)
                emitAttribute(entry, reader, values.get(reader, key), owner);
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

    // what one React view is given - its nodes, its snapshot - and what it changes them through: its controller
    public final class ViewState {
        final GComponent scope;
        final Consumer<JavaScriptObject> publish;
        // each group's and each tree's node here, by its name - data.<name> -, in the order they are made: the server
        // keeps the names of the groups and trees a container carries apart (FormView.checkReactProjectionNames)
        final NativeStringMap<OwnerNode> nodes = new NativeStringMap<>();
        // ... and the TABLES whose rows are drawn here, each a part on the table's node with the rows' parts of its
        // groups (createGridTablePart, createTreeTablePart): what a list property's column goes with, what answers for
        // a platform-drawn property drawn per row (getRowsPart), and what places what its rows state about one another
        // (commit)
        final NativeSIDMap<GGridProperty, TablePart<?, ?>> tableParts = new NativeSIDMap<>();
        final TopNode top; // the empty group's node: the container's top level, the descriptors' as well
        JavaScriptObject data = newObject();
        JavaScriptObject draft;
        JavaScriptObject lastPublished; // what the view was last given (flush)
        // `props.controller`: made with the state and never replaced, so a view keeps the one it is given. Its members
        // mirror the entries of `data` and are made with them (initialize), beside them the form's controller, `form`
        public final JavaScriptObject controller;

        ViewState(GComponent scope, Consumer<JavaScriptObject> publish) {
            this.scope = scope;
            this.publish = publish;
            this.top = new TopNode(this);
            this.controller = makeController(verbs);
        }
        JavaScriptObject current() { return draft != null ? draft : data; }
        JavaScriptObject edit() {
            if (draft == null) {
                draft = copyKeepingPrototype(data);
                enlist();
            }
            return draft;
        }
        // ... or only its commit, which may write or not: in the batch, nothing copied yet (TreeTablePart.touch). The
        // first of what is written outside a change has it published when the GWT entry it is written in exits - or,
        // written from plain JS, the next one -, once it is pending
        void enlist() {
            boolean first = pending.isEmpty() && changing == 0;
            pending.put(scope, this);
            if (first)
                Scheduler.get().scheduleFinally(GReactFormData.this::publish);
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
        // none here: what hides the host of a platform-drawn panel property while it says `hidden` (usePlatform)
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
        // made; then the empty group's members, on the controller itself; and once its calls are made, the form's
        // controller, whose calls are made once, for all the views (GReactFormData.initialize)
        void initialize(JavaScriptObject formController) {
            nodes.foreachValue(OwnerNode::initialize);
            top.fillController();
            makeCalls(GReactFormData.this, controller);
            setField(controller, "form", formController);
        }

        // the platform-drawn list property whose per-row renderers a <Platform> places, named as a panel property is -
        // its group, then its integration name, o.qty, the path of its column entry -, of a group whose rows this
        // container draws: asked of the rows' parts, not of the container's children, such a property being no child of
        // it (FormView.isPlatformDrawn). The row a <Platform> gives says which renderer, and has to be a row of that
        // group
        public GPropertyDraw getRowPlatformProperty(String name) {
            int dot = name.lastIndexOf('.');
            if (dot < 0) // a grid property always has a group
                return null;
            GGroupObject group = form.getGroupObject(name.substring(0, dot));
            RowsPart rows = group != null ? getRowsPart(group) : null;
            return rows != null ? rows.getRowPlatformProperty(name.substring(dot + 1)) : null;
        }

    }

    // the DESCRIPTOR of a component React draws or places: its entry at its container's top level, keyed by its SID -
    // its labels, its caption and image, and whether the form shows the component now: its SHOWIF, as the server sends
    // it for a component a react view is told about (true: hidden) - React's alone, as its labels are: a placed one's
    // host is hidden by the view that places it (usePlatform), which the platform does not do for what React places
    private abstract class ComponentEntry extends Entry implements GComponentController {
        final GComponent component;
        final GPropertyReader[] labelReaders; // its caption and image

        ComponentEntry(ViewState state, GComponent component) {
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
        // what labels it
        void emit(JavaScriptObject entry) {
            emitAttributes(entry, labelReaders, GGroupObjectValue.EMPTY, component);
        }
    }
    // ... of one React draws: the rest of it - its classes, its custom design - is React's as well, and draws nothing:
    // the platform has no view of it
    private final class ReactComponentEntry extends ComponentEntry {
        ReactComponentEntry(ViewState state, GComponent component) {
            super(state, component);
        }
        public void updateOther(GComponentReader reader, NativeHashMap<GGroupObjectValue, PValue> values, boolean partial) {
        }
    }
    // ... of a platform-drawn one React places: the platform draws it and React labels it, from this entry; the rest is
    // the platform's
    private final class PlatformComponentEntry extends ComponentEntry {
        final GComponentController platform; // the platform's controller of the component: the layout, which draws it

        PlatformComponentEntry(ViewState state, GComponent component, GComponentController platform) {
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
        final ViewState state;
        final ArrayList<PropertyEntry> properties = new ArrayList<>(); // what it carries by name, in the form's order

        Node(ViewState state) {
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
        TopNode(ViewState state) {
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
        // the groups of the owner, in the order the rows' parts of a table are asked (TablePart): the group, or the
        // tree's, top down
        final List<GGroupObject> groups;
        final ArrayList<Part<?>> parts = new ArrayList<>(); // in the order they are placed
        final JavaScriptObject member = newObject(); // controller.<name>, put on the controller with its members
        final NativeStringMap<Boolean> unnameable = new NativeStringMap<>(); // reported already (reportUnnameable)

        OwnerNode(ViewState state, String name, List<GGroupObject> groups) {
            super(state);
            this.name = name;
            this.groups = groups;
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
        // controller.<name>: its member, and on it the members of its parts, then those of the entries it carries - a
        // group's node's: a tree's carries none
        void fillController() {
            setField(state.controller, name, member);
            for (Part<?> part : parts)
                part.putMember(member);
            putPropertyMembers(member);
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
        // a property one of the owner's lists leaves out, as the projection does not name it there - for the reason
        // given: reported once per list and property
        void reportUnnameable(GPropertyDraw property, String field, String what, String why) {
            String name = property.sID;
            String key = field + ":" + name;
            if (unnameable.get(key) == null) {
                unnameable.put(key, Boolean.TRUE);
                GwtClientUtils.logCustomViewError("data." + this.name + "." + field + ": " + describe() + " is " + what + " by '"
                        + name + "', which the projection does not name here (" + why + "), so that one is not listed");
            }
        }
    }
    // a group's node: data.<group>, with the entries of the group's properties it carries by name
    private final class GroupNode extends OwnerNode {
        final GGroupObject group;

        GroupNode(ViewState state, GGroupObject group) {
            super(state, group.getSID(), Collections.singletonList(group));
            this.group = group;
        }
        String describe() {
            return "the group";
        }
        // data.<group>.properties: the names of the entries it carries, in the form's order; design data, like
        // them
        @Override
        void initialize() {
            super.initialize();
            JavaScriptObject names = emptyArray();
            for (PropertyEntry propertyEntry : properties)
                push(names, propertyEntry.property.integrationSID);
            setField(edit(), "properties", names);
        }
        // data.<group>.filter: the entries of the FILTERs of its properties drawn here, each by its property's
        // integration name (FilterPart) - one object for them all, with no prototype, as a name is application data,
        // copied before a filter writes its entry into it, so a snapshot published before stays as it was
        JavaScriptObject editFilter() {
            JavaScriptObject node = edit();
            JavaScriptObject filters = getOwnField(node, FILTER);
            JavaScriptObject published = published();
            if (filters == null || published != null && filters == getOwnField(published, FILTER)) {
                filters = filters == null ? newBareObject() : copyKeepingPrototype(filters);
                setField(node, FILTER, filters);
            }
            return filters;
        }
        // ... and controller.<group>.filter, their members by the same names, made with the first of them
        JavaScriptObject getFilterMember(JavaScriptObject groupMember) {
            JavaScriptObject filters = getOwnField(groupMember, FILTER);
            if (filters == null) {
                filters = newBareObject();
                setField(groupMember, FILTER, filters);
            }
            return filters;
        }
    }
    private static final String FILTER = "filter";
    // a TREE's node: data.<tree>, by the tree's name in the form - TREE t's t -, where what is the tree's, one for all
    // its groups, goes: where React draws its rows, its grid's part - the tree's current row and its own verb -, as a
    // group's node is where the group's rows are; and its user filters, its toolbar and its filter controls (GRID,
    // FILTERS, TOOLBARSYSTEM and FILTERCONTROLS(TREE t)). Its groups have nodes of their own, which their rows and
    // properties go on; a property is of a group, so this node carries none
    private final class TreeNode extends OwnerNode {
        final GTreeGroup tree;

        TreeNode(ViewState state, GTreeGroup tree) {
            super(state, tree.name, tree.groups);
            this.tree = tree;
        }
        String describe() {
            return "the tree";
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
        }
    }
    // ... whose state is a LIST of its table's, under the list's name on the table's node (TableList.field) - its
    // sortings (OrdersPart), its user filters (FiltersPart) -, always there, empty while there is none, and whose
    // member, beside the table's own, states it, as the list reads a call (TableList.change)
    private abstract class ListPart extends Part<OwnerNode> {
        final GGridProperty table; // whose list it is
        final TableList list; // ... and which

        ListPart(OwnerNode node, GGridProperty table, TableList list) {
            super(node);
            this.table = table;
            this.list = list;
        }
        @Override
        void initialize() {
            setField(node.edit(), list.field, emptyArray());
        }
        @Override
        void putMember(JavaScriptObject ownerMember) {
            setField(ownerMember, list.field, makeListMember(list, table, node.name + "." + list.field + ".change()"));
        }
        // an item on a property no item can be on is not listed: it would describe the rows by something nothing can
        // address - reported once per property
        boolean isListed(GPropertyDraw property) {
            if (list.canList(property))
                return true;
            node.reportUnnameable(property, list.field, list.listedBy, list.whyNot(property));
            return false;
        }
    }
    // a TABLE's USER ORDERS where React draws its rows - of a list's group not in a tree, or of a tree, one list for
    // all its groups: a part of the table of their own, as if a table's design had a box of its sortings, as FILTERS is
    // of its filters - placed on the table's node where its rows are drawn, a sorting being the order those are drawn
    // in, made with the rows' part of the table (TablePart). `orders` lists the sortings of the table's groups, group
    // by group, top down, each in its priority order - a group's sortings are its own: no group's outrank another's -,
    // each property named as the table's lists name it (getListName). It is rebuilt only when the form hands over
    // another list of a group - so it keeps its identity across renders in which nothing sorts the table anew. The
    // lists are the form's (Verbs.getOrders): the part keeps only what it publishes
    private final class OrdersPart extends ListPart implements GUserOrdersController {
        OrdersPart(OwnerNode node, GGridProperty table) {
            super(node, table, ordersList);
        }
        // a group's sortings as the form has them now: its own ORDERS in the first changes, then every list the server
        // reports - an ORDER action's too -, handed over only when it is another one; the other groups' as the form
        // has them too
        @Override
        public void updateOrders(GGroupObject group, LinkedHashMap<Column, Boolean> orders) {
            writeOrders();
        }
        // ... and as the form changes them - a member's, the form's controller's, or the platform's header's -: shown
        // at once, the same way
        @Override
        public void changeOrders(GGroupObject group, LinkedHashMap<Column, Boolean> orders) {
            writeOrders();
        }
        // `orders`, the sortings in the SAME shape the .lsf ORDER operator reads and writes: the property as the
        // table's lists name it, and `desc`, the server's and the operator's spelling (the client keeps ascending). A
        // sorting is named as a condition is, wherever its property is drawn - here, in another container, by the
        // platform (readListName) -, so what a view reads it can hand back - one a sorting can name (canList)
        void writeOrders() {
            JavaScriptObject array = emptyArray();
            for (GGroupObject group : table.getGroups())
                for (Map.Entry<Column, Boolean> order : verbs.getOrders(group).entrySet()) {
                    GPropertyDraw property = order.getKey().property;
                    if (!isListed(property))
                        continue;
                    JavaScriptObject entry = newObject();
                    setField(entry, "property", getListName(table, property));
                    setField(entry, "desc", !order.getValue());
                    push(array, entry);
                }
            setField(node.edit(), list.field, array);
        }
    }
    // the USER FILTERS where React draws them - what a FILTERS box holds: a part of their own, of the group the box is
    // of or of the tree, one box for all its groups, on the table's node where the box's content is - the node a
    // group's rows are drawn on when the two are in one container -, the conditions of the table's groups, each on a
    // property named as the table's lists name it (getListName, readListName). `filters` is the working list
    // of the table's user filters (GUserFiltersController), its active conditions - what the server is filtering by,
    // and in the manual apply mode what waits for the apply too -: the model tells the part whenever it changes, and
    // the array is rebuilt only then - so it keeps its identity across renders in which nothing states them anew. The
    // part keeps only what it publishes. It is the view's view of them, the platform's panel being the other: the
    // commands over them, where the platform wires them up, are an object of their own (ReactFilterInputController)
    private final class FiltersPart extends ListPart implements GUserFiltersController.View {
        final GUserFiltersController userFilters;

        FiltersPart(OwnerNode node, GGridProperty table, GUserFiltersController userFilters) {
            super(node, table, filtersList);
            this.userFilters = userFilters;
        }
        // ... and what a condition can be on, design data written once (filterProperties)
        @Override
        void initialize() {
            super.initialize();
            setField(node.edit(), "filterProperties", getFilterProperties(table));
        }
        // the table's user filters changed, whoever changed them - the server, a writer, the platform's input -: a view
        // shows their working list, a change waiting for the apply in the manual apply mode too
        public void changed(GUserFiltersController.Change change) {
            writeFilters();
        }

        // the conditions, in the SAME shape the .lsf FILTER operator reads them - compare by its operator
        // string, `or` for the junction - so one vocabulary describes a condition in .lsf and in the projection; only
        // the property is named as the table's lists name it (getListName), where FILTER names it by its name on
        // the form (they differ under an EXTID) - one a condition can be on (canList)
        void writeFilters() {
            JavaScriptObject array = emptyArray();
            for (GPropertyFilter condition : userFilters.getConditions()) {
                GPropertyDraw property = condition.column.property;
                if (!isListed(property))
                    continue;
                push(array, filtersList.writeCondition(table, condition));
            }
            setField(node.edit(), list.field, array);
        }
    }

    // ===== A TABLE'S LIST - its sortings (OrdersTableList), its user filters' conditions (FiltersTableList) -: what an
    // item of it is and what its property can be (canList), and what a call states - all of it, by an array, or one
    // item, merged into the rest -, whoever makes the call: a view's member, where it draws the list (ListPart), or the
    // form's controller, by the table's name (CONTROLLER.FORM: changeOrders, changeFilters). An item names its property
    // as the table's lists name it (getListName); the sortings are handed to the form as each group's own (Verbs), the
    // conditions to the table's user filters (GUserFiltersController)
    private abstract class TableList {
        final String field; // data.<name>.<field>, controller.<name>.<field>
        // an item as a message names it and the fields it carries, and what the rows are by its property: "an order",
        // ORDER_FIELDS, "sorted" - "a condition", FILTER_CONDITION_FIELDS, "filtered"
        final String item, itemFields, listedBy;

        TableList(String field, String item, String itemFields, String listedBy) {
            this.field = field;
            this.item = item;
            this.itemFields = itemFields;
            this.listedBy = listedBy;
        }
        // ... a call: an array states the items all at once, anything else one of them, merged into the rest
        JavaScriptObject change(String surface, GGridProperty table, JavaScriptObject items) {
            if (isJSArray(items))
                return changeAll(surface, table, items);
            else
                return changeOne(surface, table, items);
        }
        abstract JavaScriptObject changeAll(String surface, GGridProperty table, JavaScriptObject items);
        abstract JavaScriptObject changeOne(String surface, GGridProperty table, JavaScriptObject item);

        // whether an item can be on the property - and so be listed -, and which rule it fails, in words
        abstract boolean canList(GPropertyDraw property);
        abstract String whyNot(GPropertyDraw property);
        // ... and the property an item the AUTHOR wrote names (readAuthorObject), by the name the table's lists give it
        // - an object carrying only fields this call knows (readListName) -, refused, by the same rule, where no item
        // can be on it
        GPropertyDraw readProperty(String errorPrefix, GGridProperty table, JavaScriptObject item) {
            GPropertyDraw property = readListName(errorPrefix, table, readAuthorObject(errorPrefix, item, this.item, itemFields));
            if (!canList(property))
                throw refusal(errorPrefix + "'" + getListName(table, property) + "' cannot be " + listedBy
                        + " by here: " + whyNot(property));
            return property;
        }
    }
    // the SORTINGS of a table, as a call states them: read - each naming its property by the name `orders` gives it,
    // not by a member: a state is data, written in the shape the projection publishes it (readProperty) - and handed
    // to the form as the list they make, which the form splits into its groups' lists, each of the group of its
    // properties (Verbs.changeOrders). An order is {property, desc}, the SAME shape `orders` projects and the .lsf ORDER
    // operator reads; one with no `desc` is not a sorting, so it removes the one on that property. An array states them
    // all, its order being each group's priority - a group none of it names is sorted by nothing then
    private final class OrdersTableList extends TableList {
        OrdersTableList() {
            super("orders", "an order", ORDER_FIELDS, "sorted");
        }
        // can a sorting be on this property? Two rules: the projection addresses a property by its integration SID,
        // and a sorting names one property, not one of its COLUMNS - a property grouped in columns is sorted per
        // column, and no name here says which. The property may be the platform's to draw, so the form being built
        // says nothing about it
        boolean canList(GPropertyDraw property) {
            return property.integrationSID != null && !property.hasColumnGroupObjects();
        }
        String whyNot(GPropertyDraw property) {
            if (property.integrationSID == null)
                return "no integration SID";
            return "it is grouped in columns, and a sorting names no column";
        }
        @Override
        JavaScriptObject changeAll(String surface, GGridProperty table, JavaScriptObject orders) {
            String errorPrefix = controllerPrefix(surface);

            LinkedHashMap<Column, Boolean> result = new LinkedHashMap<>();
            // every property the list NAMES, direction or not, once: a group is sorted by one once, and the server keys
            // them by it - so a list naming one twice describes no sorting a group can have, whichever of the two
            // carries a direction
            NativeSIDMap<GPropertyDraw, Boolean> named = new NativeSIDMap<>();
            int size = jsArrayLength(orders);
            for (int i = 0; i < size; i++) {
                JavaScriptObject order = jsArrayGet(orders, i);
                String prefix = errorPrefix + "order " + i + ": ";
                GPropertyDraw property = readProperty(prefix, table, order);
                if (named.put(property, true) != null)
                    throw refusal(prefix + "'" + getListName(table, property) + "' is already in this list");
                applyOrder(prefix, table, order, property, result);
            }
            return verbs.changeOrders(table.getGroups(), result);
        }
        // ... one order, merged into the table's sortings as the form has them - every one of them, those it does not
        // name included, and those an earlier call of a batch stated
        @Override
        JavaScriptObject changeOne(String surface, GGridProperty table, JavaScriptObject order) {
            String errorPrefix = controllerPrefix(surface);
            LinkedHashMap<Column, Boolean> result = new LinkedHashMap<>();
            for (GGroupObject group : table.getGroups())
                result.putAll(verbs.getOrders(group));
            applyOrder(errorPrefix, table, order, readProperty(errorPrefix, table, order), result);
            return verbs.changeOrders(table.getGroups(), result);
        }
        // what one order the author wrote does to sortings: with no direction it is not a sorting, and takes the one on
        // its property off - which a noSort property has to be able to say, as a default ORDERS on one is listed; with
        // a direction it sorts by the property - refused for a noSort one - in place when the property is sorted
        // already (a map keeps a key where it is), appended when not. `desc` is read once, as every field of what an
        // author wrote is
        void applyOrder(String errorPrefix, GGridProperty table, JavaScriptObject order, GPropertyDraw property, LinkedHashMap<Column, Boolean> result) {
            Column column = new Column(property, GGroupObjectValue.EMPTY); // a sorting names none in columns
            JavaScriptObject desc = getOwnField(order, "desc");
            if (isNoValue(desc)) {
                result.remove(column);
                return;
            }
            if (!isJSBoolean(desc))
                throw refusal(errorPrefix + "'desc' must be true or false");
            if (property.noSort)
                throw refusal(errorPrefix + "'" + getListName(table, property) + "' cannot be sorted by");
            result.put(column, !toBoolean(desc));
        }
    }
    // The model and input controllers send requests themselves, so read their Promise from the form's state.
    // the CONDITIONS of a table's user filters, as a call states them: read (readFilterCondition) and written into the
    // table's user filters, the one model of its groups' (GUserFiltersController)
    private final class FiltersTableList extends TableList {
        FiltersTableList() {
            super("filters", "a condition", FILTER_CONDITION_FIELDS, "filtered");
        }
        // can a condition be on this property (isProjectableCondition), and which rule it fails
        boolean canList(GPropertyDraw property) {
            return isProjectableCondition(property);
        }
        String whyNot(GPropertyDraw property) {
            if (property.integrationSID == null)
                return "no integration SID";
            if (!property.canBeFiltered())
                return "an action, which the server is never asked to filter by";
            return "it is grouped in columns, and a condition names no column";
        }
        // the conditions an array states, each a new one, named in a message by its place in the array: the list the
        // table's user filters are stated as (changeAll), and the view filters of a grid's rows (changeViewConditions)
        ArrayList<GPropertyFilter> readConditions(String errorPrefix, GGridProperty table, JavaScriptObject conditions) {
            ArrayList<GPropertyFilter> result = new ArrayList<>();
            int size = jsArrayLength(conditions);
            for (int i = 0; i < size; i++) {
                String itemPrefix = errorPrefix + "condition " + i + ": ";
                JavaScriptObject condition = jsArrayGet(conditions, i);
                GPropertyDraw property = readProperty(itemPrefix, table, condition);
                JavaScriptObject value = getOwnField(condition, "value"); // read once: a condition is what was read
                result.add(readFilterCondition(itemPrefix, condition, GPropertyFilter.getBase(getConditionColumn(property), null), value));
            }
            return result;
        }
        // ... and a condition as a list gives it, its property named as the table's lists name it: `filters`'
        // (FiltersPart.writeFilters), and a grid's rows' `viewFilters` (GridRowsPart.changeViewFilters)
        JavaScriptObject writeCondition(GGridProperty table, GPropertyFilter condition) {
            GPropertyDraw property = condition.column.property;
            JavaScriptObject entry = newObject();
            setField(entry, "property", getListName(table, property));
            setField(entry, "compare", condition.compare.toString());
            // the FILTER type, the one the platform's own filter editor uses for this cell
            // - and the one a condition is read back with (readFilterCondition)
            // - and no property: one named `image` draws its own value as an image, while a condition's is plain
            setField(entry, "value", JSValueConverter.convertToJSValue(property.getFilterBaseType(), null, true, condition.value.value));
            setField(entry, "negation", condition.negation);
            // GPropertyFilter.junction is the conjunction flag; .lsf says `or`
            setField(entry, "or", !condition.junction);
            return entry;
        }
        // a call, read here (readFilterCondition) and handed to the table's user filters (GUserFiltersController) as
        // what it names: an array is the list of conditions, as FILTER reads one in .lsf - each a new condition, what
        // it does not state the default way, one with no value comparing with NULL ("is empty", negated "is not
        // empty"), as `filters` lists such a condition, so a list `filters` gives is taken back as it is; it replaces
        // the conditions a view can name, the rest keeping their places
        @Override
        JavaScriptObject changeAll(String surface, GGridProperty table, JavaScriptObject conditions) {
            ArrayList<GPropertyFilter> result = readConditions(controllerPrefix(surface), table, conditions);
            return verbs.changeUserFilters(table.getGroups(), () -> getUserFilters(table).changeConditions(result, GReactFormData::isProjectableCondition));
        }
        // ... one condition, in the working list - those an earlier call of a batch stated included -: a value given
        // its property as a property's FILTER gives one (FilterPart.change), in the place of the FIRST active one on
        // the property - what it does not state of the comparison, negation and `or` as that one has them, no slot's
        // memory -, or appended, the default way; no value takes that first one off, alone - a range's other bound
        // stays (GUserFiltersController.putCondition, removeCondition)
        @Override
        JavaScriptObject changeOne(String surface, GGridProperty table, JavaScriptObject condition) {
            String errorPrefix = controllerPrefix(surface);
            GPropertyDraw property = readProperty(errorPrefix, table, condition);
            JavaScriptObject value = getOwnField(condition, "value"); // read once, as in readConditions
            GUserFiltersController userFilters = getUserFilters(table);
            Column column = getConditionColumn(property);
            GPropertyFilter stated = readFilterCondition(errorPrefix, condition, GPropertyFilter.getBase(column, userFilters.getValueBase(column, false)), value);
            return verbs.changeUserFilters(table.getGroups(), () -> {
                if (stated.nullValue())
                    userFilters.removeCondition(column);
                else
                    userFilters.putCondition(stated);
            });
        }
    }
    private final OrdersTableList ordersList = new OrdersTableList();
    private final FiltersTableList filtersList = new FiltersTableList();
    // the PLATFORM'S INPUT over the conditions a view draws - GFilterInputController's kind where a view draws what the
    // box holds (createFiltersControllers): the keys of the table, the controls' add, apply and reset. Each writes into
    // the table's user filters (GUserFiltersController), the box's part showing them (FiltersPart). A condition is
    // written at once - there is no editor here, the view edits its value -; what waits in the manual apply mode waits
    // for the controls' apply. The platform's rows of the FILTERs it draws over the view's conditions, where it draws
    // any, are the toggle's, which the controls and the toolbar's button show the selectors of, built, shown and let go
    // of with the table's rows
    private final class ReactFilterInputController extends GFilterInputController {
        ReactFilterInputController(GTableController rows, GFilterControlsToggle toggle, GUserFiltersController userFilters) {
            super(rows, toggle, userFilters);
        }

        // F3, and the controls' add - the condition of the cell the table is on, in the list; Alt+F3 (replace): alone
        public void addCondition(Event keyEvent, boolean replace, boolean checkExistingFilter) {
            addCellCondition(replace);
        }
        public void addCondition() {
            addCellCondition(false);
        }
        // ... in the place of the first active one on its column, compared, negated and joined as that one is - or as
        // the slot on it remembers -; Alt+F3 states it the only one, the default way
        private void addCellCondition(boolean replace) {
            GPropertyFilter condition = getCellCondition(rows, column -> replace ? null : userFilters.getValueBase(column, true));
            if (condition == null)
                return;
            if (replace)
                userFilters.changeConditions(Collections.singletonList(condition), null);
            else
                userFilters.putCondition(condition);
        }
        // typing in a column, which the table gives to whichever filter input controller it has, as it gives it its
        // keys: the panel's editor takes the key typed, and here nothing starts - there is no editor to type in, the
        // view edits a condition's value its own way (decided: a key typed states no condition)
        public void quickEditFilter(Event keyEvent, GPropertyDraw property, GGroupObjectValue columnKey) {
        }
        // the controls' add, which the design has the FILTERS box for: a view draws what it holds
        public boolean hasFiltersContainer() {
            return true;
        }
    }

    // a TOOLBAR where React draws it - TOOLBARSYSTEM in a React view, its own too: a part of its table, a list's grid
    // or a tree, on the table's node there, and the toolbar's controller (GToolbarController) (GridToolbarPart,
    // TreeToolbarPart). What its buttons show is on the table's node, each under a name of its own, and its buttons are
    // members on the table's: a state's member changes it, as `filters`' does, and the rest are calls. Here is what
    // both have: the filter button - filterControlsVisible, whether the platform's filter controls it shows are shown
    // -, what it is told, and the export
    private abstract class ToolbarPart<N extends OwnerNode, R extends GTableController> extends StatePart<N> implements GToolbarController<R> {
        final GGridProperty table; // whose rows it exports
        R rows; // what draws them, given to it as it is made, whoever draws them (addRows)
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
            addToolbarMembers(GReactFormData.this, this, table, tableMember, node.name);
        }
        boolean isFilterControlsVisible() {
            return filter != null && filter.isControlsVisible();
        }

        // ===== the toolbar controller: what draws the rows, given to it - what its kind's members reach through them,
        // none of their buttons, which only the platform's toolbar asks for -; the filter button, a member - and the
        // table shown or hidden, with nothing of the platform's to hide here (GToolbarController.setVisible)
        public void addRows(R rows) {
            this.rows = rows;
        }
        public void addFilterToggle(GFilterInputController filter) {
            this.filter = filter;
        }
        public void showFilterControls(boolean shown) {
            stateChanged();
        }
        public void setVisible(boolean visible) {
        }

        // ===== a call through its member (toggleFilterControls)
        JavaScriptObject changeFilterControlsVisible(String surface, Object shown) {
            return toggleFilterControls(surface, table.getGroups(), filter, shown);
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

        GridToolbarPart(GroupNode node) {
            super(node, node.group.grid);
        }
        // what its buttons show, there from the start - the views the table may show, design data, written once: the
        // rows' views, which its member switches - none where no table of the platform's draws the rows, there is no
        // view to switch to
        @Override
        void initialize() {
            JavaScriptObject names = emptyArray();
            GListViewController views = rows.getListViewController();
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
            GListViewController views = rows.getListViewController();
            GListViewType view = views != null ? views.getListView() : null;
            setField(data, "listView", view != null ? getViewName(view) : null);
        }
        @Override
        void putMember(JavaScriptObject groupMember) {
            super.putMember(groupMember);
            addGridToolbarMembers(GReactFormData.this, node.group, groupMember, node.name);
        }

        // ===== the group's toolbar controller: the view the table shows now, the changes the server holds back, and
        // the update mode as the form changes it - nothing held back yet
        public void showView(GListViewType view) {
            stateChanged();
        }
        public void updateState(Boolean updateState) {
            if (updateState != null && updateState != updatePending) {
                updatePending = updateState;
                writeState(); // in the server's delta, published with it
            }
        }
        public void showUpdateMode(boolean manual) {
            updatePending = false;
            stateChanged();
        }
    }
    // ... of a TREE - TOOLBARSYSTEM(TREE t): a part of the tree, one toolbar for all its groups, on the tree's node,
    // whoever draws the tree's rows. Its buttons are the platform's tree toolbar's, whatever the design's flags say, as
    // the platform shows them: the filter button (filterControlsVisible), the export of the tree (export()), and the
    // two that open the tree (expandCurrent(), expandTree()) where the rows say so
    // (GTreeController.getOpenedByToolbarMembers). Given up with that: a toolbar a view draws apart from the rows React
    // draws opens nothing - each opening has one member, the rows' own, wherever the toolbar is drawn
    private final class TreeToolbarPart extends ToolbarPart<TreeNode, GTreeController> {
        TreeToolbarPart(TreeNode node) {
            super(node, node.tree);
        }
        // its members: a toolbar's, and the two that open the tree - handed to the platform's tree, which opens the
        // nodes, an edit a batch carries -, only where the rows give it what they open
        @Override
        void putMember(JavaScriptObject treeMember) {
            super.putMember(treeMember);
            GTreeController opened = rows.getOpenedByToolbarMembers();
            if (opened != null) {
                addExpandCurrentMember(GReactFormData.this, node.groups, opened, treeMember);
                addExpandTreeMember(GReactFormData.this, node.groups, opened, treeMember);
            }
        }

    }
    // the FILTER CONTROLS where React draws them - FILTERCONTROLS(g), or FILTERCONTROLS(TREE t), in a React view:
    // a part of their own, of the group or of the tree, on its node there, and the controls of its filter input
    // controller (GFilterControlsController). The same whoever draws the conditions: its buttons are members on the
    // table's - the add, the apply, the reset, each what the platform's button does over those conditions -, and what
    // the apply button shows is on the table's node: the manual apply mode (filtersManual) - the client's setting, the
    // view's apply button always being there where it gives the apply -, and whether anything waits to be sent
    // (filtersPending). Its commands are the filter input controller's, where the platform wires one up
    // (GFormController.initializeTable): with none, its members refuse, saying why, and nothing waits for its apply
    private final class FilterControlsPart extends StatePart<OwnerNode> implements GFilterControlsController, GUserFiltersController.View {
        GFiltersHandler handler; // the table's filter input controller, once bound - none where the platform makes none
        GUserFiltersController userFilters; // ... and the table's user filters

        FilterControlsPart(OwnerNode node) {
            super(node);
        }
        // what the apply button shows: the manual apply mode, and whether anything waits to be sent
        void writeState() {
            JavaScriptObject data = node.edit();
            setField(data, "filtersManual", userFilters != null && userFilters.isManual());
            setField(data, "filtersPending", userFilters != null && userFilters.isPending());
        }
        // its members, on the table's own
        @Override
        void putMember(JavaScriptObject ownerMember) {
            addFilterControlsMembers(this, ownerMember, node.name);
        }

        // ===== the filter input controller's controls: its commands, the view's to show - not the toolbar's -, the
        // apply button always there, as the view draws it (the manual apply mode the client's setting then), a view of
        // the table's user filters; and the table shown or hidden, with nothing of the platform's to hide here
        // (GFilterControlsController.setTableVisible)
        public void bind(GFiltersHandler handler, GUserFiltersController userFilters) {
            this.handler = handler;
            this.userFilters = userFilters;
            userFilters.addView(this);
            stateChanged();
        }
        public boolean isShownByToolbar() {
            return false;
        }
        public boolean isApplyShown(boolean shownByToolbar) {
            return handler != null;
        }
        public void setVisible(boolean visible) {
            stateChanged();
        }
        public void changed(GUserFiltersController.Change change) {
            stateChanged();
        }
        public void setTableVisible(boolean visible) {
        }

        // ===== a call through its member (addCondition, applyConditions, resetConditions)
        JavaScriptObject addFilter(String surface) {
            return addCondition(surface, node.groups, handler);
        }
        JavaScriptObject applyFilters(String surface) {
            return applyConditions(surface, node.groups, handler, userFilters);
        }
        JavaScriptObject resetFilters(String surface) {
            return resetConditions(surface, node.groups, handler, userFilters);
        }
    }
    // a group's FILTER GROUP where React draws it - FILTERGROUP(fg) in a React view: a part of the group, on its
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
        }
        // its filters' keys, bound by the form (GFormController.initializeBindings): taken with the view that draws it
        // while the form shows the filter group (isShown), as the platform's widget's are while the widget is shown, and
        // selecting the filter, as that widget does (onBinding)
        public Widget getBindingWidget() {
            return isShown() ? bindings.getViewWidget(node.state.scope) : null;
        }
        // whether the form shows the filter group now, which its descriptor says (ComponentEntry.isShown): its filters'
        // keys are taken while it does, as the platform's widget's are while the widget is shown - it is hidden with
        // the filter group, and its key goes with it. A filter group of its own has no descriptor in its own view: its
        // view is what the form hides then - the platform, or the host of the React container that places it, which
        // is hidden with the filter group, its view and its key with it (GwtClientUtils.isShowing)
        boolean isShown() {
            JavaScriptObject descriptor = getOwnField(node.state.current(), filterGroup.sID);
            return descriptor == null || !isField(descriptor, "hidden", true);
        }
        // its entry
        void writeState() {
            JavaScriptObject entry = newObject();
            setField(entry, "value", selected != null ? (Double) (double) filterGroup.filters.indexOf(selected) : null);
            JavaScriptObject captions = emptyArray();
            for (GRegularFilter filter : filterGroup.filters)
                push(captions, filter.caption);
            setField(entry, "filters", captions);
            setField(entry, "canClear", offersNone(filterGroup));
            setField(node.edit(), filterGroup.name, entry);
        }
        // its member, on the group's own
        @Override
        void putMember(JavaScriptObject groupMember) {
            setField(groupMember, filterGroup.name, makeFilterGroupMember(GReactFormData.this, filterGroup, node.name + "." + filterGroup.name));
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
        public void onBinding(GRegularFilter filter) {
            verbs.changeRegularFilter(filterGroup, filter == selected && filterGroup.filters.size() == 1 ? null : filter);
        }
    }
    // a property's FILTER where React draws it - FILTER(<property>) in a React view, or with a view of its own: a part
    // of the group of its property, on its node there, under `filter` by its property's integration name - as a filter
    // group is on the node under its own name -, shaped as a property's entry: the property's caption and the type of
    // a condition's value (the FILTER type, by which `value` is read and written, as `filters` has it), and the
    // filter's condition - it is a slot of its table's user filters (GUserFiltersController): the first active
    // condition on its property in the working list, wherever that stands -: whether there is one (`active`), its
    // compare, negation and junction - `or`, as `filters` says it - (with none, what the FILTER remembers of the
    // condition it showed last: the property's default compare, none and AND at first) and its value (null with none;
    // one comparing with NULL has null too - false on a LOGICAL -, `active` telling them apart). Its member's
    // change(value, options) does what the platform's row does: puts the value into that condition, or into the
    // FILTER's own, with what the options state - {compare, negation, or}, as the row's selectors do where the filter
    // controls are shown -, the condition's own, or what the FILTER remembers, where they state none; no value takes
    // that condition off, alone - a range's other bound stays, the FILTER showing it then. It is a view of the user
    // filters, told whenever they change, whoever changes them: the part keeps no condition of its own
    private final class FilterPart extends StatePart<GroupNode> implements GUserFiltersController.View {
        final GFilter filter;
        final GUserFiltersController userFilters;

        FilterPart(GroupNode node, GFilter filter, GUserFiltersController userFilters) {
            super(node);
            this.filter = filter;
            this.userFilters = userFilters;
        }

        // its entry: what its slot shows - an active entry, or its placeholder, which keeps what it remembers of its
        // condition
        void writeState() {
            GPropertyDraw property = filter.property;
            GUserFilter slot = userFilters.getSlotEntry(filter);
            GPropertyFilter condition = slot.condition;
            boolean active = slot.isActive();
            JavaScriptObject entry = newObject();
            setFieldOrNull(entry, "caption", property.caption);
            setField(entry, "type", JSValueConverter.getJSTypeName(property.getFilterBaseType()));
            setField(entry, "active", active);
            setField(entry, "compare", condition.compare.toString());
            setField(entry, "negation", condition.negation);
            setField(entry, "or", !condition.junction); // GPropertyFilter.junction is the conjunction flag
            setField(entry, "value", active ? JSValueConverter.convertToJSValue(property.getFilterBaseType(), null, true, condition.value.value) : null);
            setField(node.editFilter(), property.integrationSID, entry);
        }
        // its member, under the group's `filter`
        @Override
        void putMember(JavaScriptObject groupMember) {
            String name = filter.property.integrationSID;
            setField(node.getFilterMember(groupMember), name, makeFilterMember(this, node.name + "." + FILTER + "." + name));
        }

        // ===== the table's user filters changed, whoever changed them, and the filter shows its condition in them
        public void changed(GUserFiltersController.Change change) {
            stateChanged();
        }

        // ===== a call through its member (changeFilterCondition)
        JavaScriptObject change(String surface, JavaScriptObject value, JavaScriptObject options) {
            return changeFilterCondition(surface, filter, userFilters, value, options);
        }
    }
    // a view's name, as the group's node and its member say it: grid, pivot, custom, map, calendar
    private static String getViewName(GListViewType view) {
        return view.name().toLowerCase();
    }
    private static boolean readBoolean(String errorPrefix, Object value) {
        if (!(value instanceof Boolean))
            throw refusal(errorPrefix + "takes true or false");
        return (Boolean) value;
    }
    // an author's whole number, 0 up to `bound` - a filter's index, a count -, or -1: not a number, a fraction,
    // negative, out of its bound (a count's is infinity, which is not below itself); checked before it is narrowed
    private static int readWholeNumber(Object value, double bound) {
        double number = value instanceof Double ? (Double) value : -1;
        return number == Math.floor(number) && number >= 0 && number < bound ? (int) number : -1;
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
            addRowsVerbs(this, groupMember, node.name);
        }
        // the platform-drawn list property whose per-row renderers go into the rows drawn here, by its integration name
        GPropertyDraw getRowPlatformProperty(String integrationSID) {
            for (GPropertyDraw property : form.propertyDraws)
                if (property.groupObject == node.group && property.isPlatformDrawnPerRow() && integrationSID.equals(property.integrationSID))
                    return property;
            return null;
        }
        // a group attribute rebuilds the rows or the node - React's alone: the platform has no view of the rows React
        // draws
        public void updateAttribute(GGroupObjectPropertyReader reader, NativeHashMap<GGroupObjectValue, PValue> delivered, boolean partial) {
            values.update(reader, delivered, partial, reader.getAttributeScope() == GGroupAttributeScope.ROW ? this::rowChanged : key -> attributeChanged(reader));
        }

        // ===== the group's controller (GFormController.groupControllers): its rows and its current object as the
        // server sends them - the current object as the form changes it too (GGroupController.changeCurrentKey) -, and
        // a view's own add or remove - into the drafts only, published once the change that wrote them is over (change)
        public void updateKeys(GGroupObject group, ArrayList<GGroupObjectValue> keys, GFormChanges fc, int requestIndex) {
            setRows(keys);
        }
        public void updateCurrentKey(GGroupObjectValue currentKey) {
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
            return getRow(currentKey);
        }
        public int getRow(GGroupObjectValue key) {
            Integer position = key != null ? positions.get(key) : null;
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
        // a platform-drawn column's too: the platform's, which draws its cells - an edit's before the server answers,
        // as a grid's
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
                    columns.add(property.getFilterColumn(GGroupObjectValue.EMPTY, property.getCaption(captions, GGroupObjectValue.EMPTY)));
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
            JavaScriptObject byKey = getField(current, "byKey");
            JavaScriptObject row = getField(byKey, key.toKeyString());
            JavaScriptObject original = node.published();
            // Identity tells whether this object still belongs to the published snapshot. A copied or newly
            // inserted row is already writable, including after a reorder within the same batch.
            if (row == getField(getField(original, "byKey"), key.toKeyString())) {
                JavaScriptObject list = getField(current, "list");
                if (list == getField(original, "list")) {
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
            JavaScriptObject previous = getField(node.current(), "byKey");
            JavaScriptObject list = emptyArray();
            JavaScriptObject byKey = newBareObject();
            JavaScriptObject keys = emptyArray();
            positions.clear();
            int position = 0;
            for (GGroupObjectValue key : rows) {
                JavaScriptObject row = getField(previous, key.toKeyString());
                if (row == null) {
                    row = newObject();
                    GGroupObjectValue.registerRow(row, key);
                    fillRowAttributes(row, key);
                    // the node holds panel properties and platform-drawn columns too
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
            setField(row, "current", key.equals(currentKey));
            for (GPropertyReader reader : rowReaders)
                replaceAttribute(row, reader, values.get(reader, key));
        }

        // ===== a call through its member: change(row), the current object chosen among the rows drawn here - an edge
        // of the rows and a change of the rows selected, which a grid's member takes too (GridRowsPart), refused
        JavaScriptObject change(String surface, JavaScriptObject objectOrKey, JavaScriptObject options) {
            String errorPrefix = controllerPrefix(surface);
            if (isSelectionChange(errorPrefix, options) || isEdge(objectOrKey))
                throw refusal(errorPrefix + "takes a row of '" + node.name + "': the first and the last row and a selection are a grid's");
            return verbs.changeCurrentObject(node.group, resolveGroupRow(errorPrefix, objectOrKey), null, null);
        }
        // what a column's entry says of the column besides - a grid's alone
        void emitColumn(JavaScriptObject entry, ReactColumnPropertyEntry column) {
        }
        // the row of THIS group a call names (resolveRow), narrowed by the group's own rule (readGroupRow)
        GGroupObjectValue resolveGroupRow(String errorPrefix, JavaScriptObject objectOrKey) {
            return readGroupRow(errorPrefix, node.group, resolveRow(errorPrefix, objectOrKey));
        }
        // a row of this group, named however the view has it: the row, its `objects` handle, or the key the projection
        // gave it - a name only here, where the rows are, looked up in the index built with them
        GGroupObjectValue resolveRow(String errorPrefix, JavaScriptObject objectOrKey) {
            GGroupObjectValue key = GGroupObjectValue.resolveObject(objectOrKey);
            if (key == null)
                key = GGroupObjectValue.resolveObject(keyedRow(getField(node.current(), "byKey"), objectOrKey));
            if (key == null)
                throw refusal(errorPrefix + "expects a row of '" + node.name + "', its objects handle, or its key;"
                        + " that is none of them, or names no row the group has now");
            return key;
        }
    }

    // ... of the group of a GRID - a list's group not in a tree -: its rows, none of them a node to open
    // (GGroupController's defaults), and its view filters - the conditions the view that draws the rows has the server
    // filter them by, its own: beside the table's user filters, which a user changes and a FILTER states, and applied
    // with them; what the grid's toolbar and filter input controller ask of the grid as a whole is its table's
    // (GridTablePart)
    private final class GridRowsPart extends RowsPart {
        // its COLUMNS (getColumns), each by its place among them, and what is the grid's own of them, as the platform's
        // grid keeps its columns' (DataGrid): the current one, and the range selected, between the column it starts
        // from and the one it ends on - -1: none. The rows selected are the server's (updateRowSelection)
        int currentColumn = -1, startSelectColumn = -1, endSelectColumn = -1;

        GridRowsPart(GroupNode node) {
            super(node);
        }
        // `viewFilters`, beside its rows: there from the start, none of them yet
        @Override
        void initialize() {
            super.initialize();
            setField(node.edit(), VIEW_FILTERS, emptyArray());
        }
        // ... and their member, on the group's: change(conditions) states them all (changeViewConditions)
        @Override
        void putMember(JavaScriptObject groupMember) {
            super.putMember(groupMember);
            setField(groupMember, VIEW_FILTERS, makeViewFiltersMember(GReactFormData.this, node.group, node.name + "." + VIEW_FILTERS + ".change()"));
            addGridVerbs(this, groupMember, node.name);
        }
        // ... and the conditions, as the form sends them, whoever states them - this view's member, the form's
        // controller - (GGroupController.changeViewFilters): shown at once, each as a condition of `filters` is
        // (writeCondition), and kept as shown - the server reports no view filters, so what a request that fails
        // stated stays, as it does of the user filters
        @Override
        public void changeViewFilters(List<GPropertyFilter> conditions) {
            JavaScriptObject array = emptyArray();
            for (GPropertyFilter condition : conditions)
                push(array, filtersList.writeCondition(node.group.grid, condition));
            setField(node.edit(), VIEW_FILTERS, array);
        }

        // ===== its selection, as calls through its member state it. change(row | HOME | END[, {selection}]): the
        // current object - a row drawn here, or an edge of the rows, the first or the last of them all, which the
        // server finds - and what becomes of the rows selected (readSelection)
        @Override
        JavaScriptObject change(String surface, JavaScriptObject target, JavaScriptObject options) {
            String errorPrefix = controllerPrefix(surface);
            return changeCurrentObject(errorPrefix, target, options, row -> resolveGroupRow(errorPrefix, row));
        }
        // ... as the form's controller's changeObject names it too, a row read as it reads one (readRow)
        JavaScriptObject changeCurrentObject(String errorPrefix, JavaScriptObject target, JavaScriptObject options, RowKey readRow) {
            GChangeSelection selection = readSelection(errorPrefix, options);
            if (isEdge(target))
                return verbs.scrollCurrentObject(node.group, readToEnd(errorPrefix, target), selection);
            GGroupObjectValue key = readRow.read(target);
            return verbs.changeCurrentObject(node.group, key, selection, selection != null ? updateRowSelection(getRow(key), getRow(currentKey), selection) : null);
        }
        // the rows selected are the server's, each row saying it (GRID_ROW) - and, as the platform's table keeps its
        // own (GGridTable.updateRowSelection), changed here at once by the move that changes them, of the rows the
        // view has: what the move changes (GGridSelection.getRowChanges), which the form holds against the server's
        // until the move's request is answered (GFormController.pendingChangeGroupObject). A range run on from a row
        // the view has not, or up to one, changes none here: the server's answer says what it takes in
        NativeHashMap<GGroupObjectValue, PValue> updateRowSelection(int row, int prevRow, GChangeSelection changeSelection) {
            NativeHashMap<GGroupObjectValue, PValue> changeSelectionRows = changeSelection == GChangeSelection.MOVEEND && (row < 0 || prevRow < 0) ? new NativeHashMap<>()
                    : GGridSelection.getRowChanges(getRowSelectValues(), rows::get, row, prevRow, changeSelection);
            updateAttribute(node.group.rowSelectReader, changeSelectionRows, true);
            return changeSelectionRows;
        }
        NativeHashMap<GGroupObjectValue, PValue> getRowSelectValues() {
            NativeHashMap<GGroupObjectValue, PValue> rowSelectValues = values.get(node.group.rowSelectReader);
            return rowSelectValues != null ? rowSelectValues : new NativeHashMap<>();
        }
        // ... properties.change(column[, {selection}]): the current column, and what becomes of the columns selected,
        // as a row's change says it of the rows: 'start' leaves the column alone selected, 'extend' the range from the
        // column the range starts from up to it (GGridSelection.getStartColumn)
        JavaScriptObject changeColumn(String surface, String name, JavaScriptObject options) {
            String errorPrefix = controllerPrefix(surface);
            int column = readColumn(errorPrefix, name);
            GChangeSelection selection = readSelection(errorPrefix, options);
            updateCurrentColumn(column);
            return verbs.changeActiveProperty(node.group, getColumns().get(column).property, selection != null ? updateColumnSelection(column, selection) : new ArrayList<>());
        }
        // the column a call names, one the form shows now, by the name its entry has on the group's node
        int readColumn(String errorPrefix, String name) {
            ArrayList<ReactColumnPropertyEntry> columns = getColumns();
            for (int i = 0; i < columns.size(); i++)
                if (columns.get(i).name.equals(name)) {
                    columns.get(i).checkShown(errorPrefix);
                    return i;
                }
            throw refusal(errorPrefix + (name == null ? "takes the name of the column, as a string"
                    : "'" + name + "' is no column of '" + node.name + "': a list property the view draws in its rows, not an action"));
        }
        // ... selectAll(): every row the filter leaves - the server's, those the view has said so by its answer, the
        // form told which of them are not selected yet (GGridSelection.getAllRowChanges) - and every column; the
        // current row and the current column stay (GGridTable.selectAllCells)
        JavaScriptObject selectAll() {
            int columnCount = getColumns().size();
            return verbs.selectAllRows(node.group, GGridSelection.getAllRowChanges(getRowSelectValues(), rows::get, rows.size()),
                    updateColumnSelection(columnCount > 0 ? 0 : -1, columnCount - 1));
        }
        // its columns: the list properties React draws in these rows, with a value - an action has none -, in the
        // form's order, as data.<group>.properties lists them
        ArrayList<ReactColumnPropertyEntry> getColumns() {
            ArrayList<ReactColumnPropertyEntry> columns = new ArrayList<>();
            for (PropertyEntry propertyEntry : node.properties)
                if (propertyEntry instanceof ReactColumnPropertyEntry && !propertyEntry.property.isAction())
                    columns.add((ReactColumnPropertyEntry) propertyEntry);
            return columns;
        }
        // the current column from now on: the entry of the one that was and of the one that is rebuilt, each saying it
        // (emitColumn)
        void updateCurrentColumn(int current) {
            ArrayList<ReactColumnPropertyEntry> columns = getColumns(), changed = new ArrayList<>();
            for (int i : new int[] {currentColumn, current})
                if (current != currentColumn && i >= 0)
                    changed.add(columns.get(i));
            currentColumn = current;
            for (ReactColumnPropertyEntry column : changed)
                column.updateEntry();
        }
        // ... and the range of columns selected, as the platform's table sets its own: once a move makes a column its
        // end (GGridTable.updateColumnSelection) ...
        ArrayList<GGridTable.ColumnSelection> updateColumnSelection(int newEnd, GChangeSelection changeSelection) {
            return updateColumnSelection(GGridSelection.getStartColumn(startSelectColumn, newEnd, changeSelection), newEnd);
        }
        // ... and between two columns: the entry of every column it changes rebuilt, each saying it (emitColumn) - and
        // what it changes for the server (addSelectedColumn)
        ArrayList<GGridTable.ColumnSelection> updateColumnSelection(int newStart, int newEnd) {
            ArrayList<GGridTable.ColumnSelection> changes = new ArrayList<>();
            if (newEnd == endSelectColumn && newStart == startSelectColumn) return changes;

            ArrayList<ReactColumnPropertyEntry> columns = getColumns(), changed = new ArrayList<>();
            GGridSelection.forColumnChanges(startSelectColumn, endSelectColumn, newStart, newEnd, (i, set) -> {
                changed.add(columns.get(i));
                addSelectedColumn(columns.get(i), changes, set);
            });

            startSelectColumn = newStart;
            endSelectColumn = newEnd;

            for (ReactColumnPropertyEntry column : changed)
                column.updateEntry();

            return changes;
        }
        // ... of the columns the form reads as selected (SELECT PROPERTY) and shows now
        void addSelectedColumn(ReactColumnPropertyEntry column, ArrayList<GGridTable.ColumnSelection> changeSelectionColumns, boolean set) {
            if (column.property.hasSelectProperty && column.isShown())
                changeSelectionColumns.add(new GGridTable.ColumnSelection(column.property, GGroupObjectValue.EMPTY, set));
        }
        // what a column's entry says of it: whether it is the current one, and whether the range selected holds it - a
        // cell is selected where its row and its column both are
        @Override
        void emitColumn(JavaScriptObject entry, ReactColumnPropertyEntry column) {
            int index = getColumns().indexOf(column);
            if (index >= 0) {
                setField(entry, "current", index == currentColumn);
                setField(entry, "selected", GGridSelection.isColumnSelected(startSelectColumn, endSelectColumn, index));
            }
        }
        // ===== its clipboard. The cells are rows in columns, each side its own: the rows selected, the server's, while
        // the view shows one as selected (hasRowSelection), else the current row; the columns of the range, else the
        // current column (getSelectedColumns). copy(): their text - which the server gives of the rows it has as
        // selected, having those the view has not too, as the platform's table copies a range; of the current row the
        // view has it itself, a line of its cells
        JavaScriptObject copy(String surface) {
            ArrayList<ReactColumnPropertyEntry> columns = getSelectedColumns();
            if (hasRowSelection() && !columns.isEmpty())
                return verbs.copySelection(surface, getProperties(columns));
            List<String> cells = new ArrayList<>();
            if (getRow(currentKey) >= 0)
                for (ReactColumnPropertyEntry column : columns)
                    cells.add(column.getCopy(currentKey));
            return verbs.copyText(surface, GwtClientUtils.getClipboardTable(Collections.singletonList(cells))); // written as the server's rows are
        }
        // ... paste(text): a text pasted as the platform's table pastes one - one value into every one of those
        // cells, by a group change over the rows selected, else into the current row's; a table of them into the cells
        // from the current one on, to the right and down, as far as the table goes, the server reading the rows from
        // the current one on. Each value is read by the pattern of its cell, of a row the view has (getPattern).
        // Nothing is pasted from an empty text, nor where the view has no current row
        JavaScriptObject paste(String surface, String text) {
            if (text == null)
                throw refusal(controllerPrefix(surface) + "takes the text to paste, as a string");
            List<List<String>> table = GwtClientUtils.getClipboardTable(text);
            int width = 0;
            for (List<String> row : table)
                width = Math.max(width, row.size());
            int current = getRow(currentKey);
            boolean single = table.size() == 1 && width == 1;
            ArrayList<ReactColumnPropertyEntry> columns = text.isEmpty() || current < 0 ? new ArrayList<>()
                    : single ? getSelectedColumns() : getColumnsFromCurrent(width);
            if (single)
                table = Collections.singletonList(Collections.nCopies(columns.size(), table.get(0).get(0)));
            List<List<String>> patterns = new ArrayList<>();
            for (int i = 0; i < table.size(); i++) {
                GGroupObjectValue key = current >= 0 && current + i < rows.size() ? rows.get(current + i) : null;
                List<String> rowPatterns = new ArrayList<>();
                for (ReactColumnPropertyEntry column : columns)
                    rowPatterns.add(column.getPattern(key));
                patterns.add(rowPatterns);
            }
            return verbs.pasteSelection(surface, getProperties(columns), table, patterns, single && hasRowSelection());
        }
        // whether the view shows a row as selected, of the rows it has
        boolean hasRowSelection() {
            NativeHashMap<GGroupObjectValue, PValue> rowSelectValues = getRowSelectValues();
            for (GGroupObjectValue row : rows)
                if (GridDataRecord.isRowSelect(rowSelectValues.get(row)))
                    return true;
            return false;
        }
        // the columns of the cells: those of the range that the form shows now - with no range, the current one
        ArrayList<ReactColumnPropertyEntry> getSelectedColumns() {
            ArrayList<ReactColumnPropertyEntry> columns = getColumns(), selected = new ArrayList<>();
            for (int i = 0; i < columns.size(); i++)
                if (columns.get(i).isShown() && (startSelectColumn >= 0 ? GGridSelection.isColumnSelected(startSelectColumn, endSelectColumn, i) : i == currentColumn))
                    selected.add(columns.get(i));
            return selected;
        }
        // ... and those a table is pasted into: from the current one on, as many as the table is wide, of those the
        // form shows now - none where it does not show the current one
        ArrayList<ReactColumnPropertyEntry> getColumnsFromCurrent(int count) {
            ArrayList<ReactColumnPropertyEntry> columns = getColumns(), fromCurrent = new ArrayList<>();
            if (currentColumn >= 0 && columns.get(currentColumn).isShown())
                for (int i = currentColumn; i < columns.size() && fromCurrent.size() < count; i++)
                    if (columns.get(i).isShown())
                        fromCurrent.add(columns.get(i));
            return fromCurrent;
        }
        ArrayList<GPropertyDraw> getProperties(ArrayList<ReactColumnPropertyEntry> columns) {
            ArrayList<GPropertyDraw> properties = new ArrayList<>();
            for (ReactColumnPropertyEntry column : columns)
                properties.add(column.property);
            return properties;
        }
    }
    private static final String VIEW_FILTERS = "viewFilters";
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
            addNodeVerbs(this, groupMember, node.name);
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
        // ... and its current object, the server's or the form's: the tree's current node then, set to a row - even
        // the row it already was, as the platform's tree selects it: the server reports a group's current object anew
        // whenever it reads the group's rows again - of a tree, one group in an answer, the deepest it read
        public void updateCurrentKey(GGroupObjectValue currentKey) {
            super.updateCurrentKey(currentKey);
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
                GwtClientUtils.logCustomViewError("data." + node.name + ": " + parents.size()
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
                if (loading != isField(row, "loading", true))
                    setFlag(editRow(key), "loading", loading);
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
            return getField(getField(node.current(), "byKey"), key.toKeyString());
        }
        // ===== a call through its member: expand(row), collapse(row), toggle(row) - a node of the tree, one of the
        // rows drawn here, named as change(row) names a row of its group (resolveGroupRow): a row of a group BELOW
        // carries the path past this one and names the node it hangs under here. Straight to the server: a tree whose
        // rows React draws has no platform table to go through. Expanded-ness is the SERVER's state (it decides which
        // rows a group returns at all), so these return a Promise: the children arrive as rows. The node's `expanded`
        // says what was asked at once, as a changed value does, and what the rows say once the server answers
        JavaScriptObject expand(String surface, JavaScriptObject objectOrKey) {
            return verbs.expandNode(node.group, resolveGroupRow(controllerPrefix(surface), objectOrKey));
        }
        JavaScriptObject collapse(String surface, JavaScriptObject objectOrKey) {
            return verbs.collapseNode(node.group, resolveGroupRow(controllerPrefix(surface), objectOrKey));
        }
        // ... the opposite of what the node says now (`expanded`) - what was asked of it included, so two toggles
        // before the answer open it and close it again, made one after the other or in one batch
        JavaScriptObject toggle(String surface, JavaScriptObject objectOrKey) {
            GGroupObjectValue key = resolveGroupRow(controllerPrefix(surface), objectOrKey);
            if (table.isOpen(this, key))
                return verbs.collapseNode(node.group, key);
            else
                return verbs.expandNode(node.group, key);
        }
        // ... expandAll(), collapseAll(): every node of the group, and of the groups below it - for the tree's top
        // group, the whole tree
        JavaScriptObject expandAll() {
            return verbs.expandAll(node.group);
        }
        JavaScriptObject collapseAll() {
            return verbs.collapseAll(node.group);
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
    // group, a tree's current node's -, and the values and the columns of each group. Nothing a table's filter input
    // controller asks has keys or a column of its own, and the focus is the view's to keep. A grid's table writes
    // nothing of its own - its group's rows' part writes the rows -; a tree's writes what its rows state about one
    // another (TreeTablePart)
    private abstract class TablePart<N extends OwnerNode, R extends RowsPart> extends Part<N> implements GTableController {
        final NativeSIDMap<GGroupObject, R> rowsParts = new NativeSIDMap<>(); // each group's, as it is made
        // ... and the part of the table's user orders, beside it on the table's node, the sortings being the order the
        // rows are drawn in
        final OrdersPart orders;

        TablePart(N node, GGridProperty table) {
            super(node);
            orders = new OrdersPart(node, table);
        }
        // the rows' part of the group a condition starts on: the top group's - a grid's one group - but where the tree
        // has a current node (TreeTablePart)
        R currentPart() {
            return rowsParts.get(node.groups.get(0));
        }
        // what it writes, and its member: nothing of its own - its groups' rows' parts write the rows
        @Override
        void initialize() {
        }
        @Override
        void putMember(JavaScriptObject ownerMember) {
        }
        // ... and what its rows state about one another, placed once the batch is complete (ViewState.commit):
        // nothing - a grid's rows state nothing about each other; a tree's (TreeTablePart.place)
        void place() {
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
            for (GGroupObject group : node.groups)
                columns.addAll(rowsParts.get(group).getFilterColumns());
            return columns;
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
            super(node, node.group.grid);
        }
        public void addToolbarButtons(GGridController.Toolbar toolbar) {
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
            super(node, node.tree);
        }
        // what it writes, there from the start: no current row yet
        @Override
        void initialize() {
            placeCurrent();
        }
        // its member, on the tree's own
        @Override
        void putMember(JavaScriptObject treeMember) {
            addExpandCurrentMember(GReactFormData.this, node.groups, this, treeMember);
        }
        // the part of the current node - before any, the top group's
        @Override
        TreeRowsPart currentPart() {
            return current != null ? current : super.currentPart();
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

    // what React writes for a property - its entry, on its node under its name: all of a property React draws, or the
    // labels of a property the platform draws - and whether the form shows it, which the entry says: there from the
    // start. It is the form's controller of the property, all of it: its values and what labels it alike
    private abstract class PropertyEntry extends Entry implements GPropertyController {
        final GPropertyDraw property;
        // whether the form shows the property now: delivered whole, and not dropped since - a platform-drawn one's values and its
        // drop come through its entry on their way to the platform - kept here, as no value says it
        boolean shown;

        // carried by name - its integration name - on its node, in the form's order: a call, a condition or a sorting
        // names it by that name. Its node is its group's, the empty group's the top level, which is what carries it
        PropertyEntry(Node node, GPropertyDraw property) {
            super(node, property.integrationSID);
            this.property = property;
            node.properties.add(this);
        }
        public boolean isShown() {
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
        // what a new row carries of it: a column's cell; nothing of a panel entry or of a platform-drawn property's, whose cells
        // the platform draws
        void fillRow(JavaScriptObject row, GGroupObjectValue key) {
        }
        // its member, on its node's member as its entry is on its node: none for a platform-drawn property, whose
        // values the platform draws and edits, nor for a platform-drawn action, which the platform's button runs
        void putMember(JavaScriptObject owner) {
        }
        // its value in a cell - a row's, for a column -, as the client has it now, null for none: what the rows answer
        // of the current cell (GTableController.getSelectedValue)
        abstract PValue getValueAt(GGroupObjectValue key);
    }
    // a property React draws - a panel entry, a column and its cells: carried by name on its group's node, its values
    // and the attributes it is drawn with kept here, React's alone - the platform has no view of it
    private abstract class ReactPropertyEntry extends PropertyEntry implements PropertyMember {
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
        // a cell of it: its value while the form shows it, what these readers say of it, for the key - the panel
        // entry is its one cell, a column has one per row -, and `loading` while it is loading
        final void emitCell(JavaScriptObject target, GPropertyReader[] readers, GGroupObjectValue key) {
            if (shown)
                emitPropertyValue(target, property, getValueAt(key));
            emitAttributes(target, readers, key, property);
            if (isLoading(key))
                setField(target, "loading", true);
        }
        // whether a cell is loading: while its change waits for its answer - the client's own mark, which the answer
        // takes off (GLoadingReader) -, as the platform's cell is (ActionOrPropertyValue.isLoading)
        boolean isLoading(GGroupObjectValue key) {
            return PValue.getBooleanValue(values.get(property.loadingReader, key));
        }
        // the value of the cell a call names - of a column with no row named, the current row's; refused where there
        // is none -, a change not answered yet included: what a pick is made against
        public PValue getCellValue(String errorPrefix, GGroupObjectValue key) {
            GGroupObjectValue cellKey = getCellKey(key);
            if (cellKey == null) // a column whose group has no current row, and no row named
                throw refusal(errorPrefix + "'" + property.groupObject.getSID() + "' has no current row: name the row to pick in");
            return getValueAt(cellKey);
        }
        // its value in a cell: kept here, the client's own on top (put)
        PValue getValueAt(GGroupObjectValue key) {
            return values.get(property, key);
        }

        // ===== its controller (GFormController.propertyControllers): whether the form shows it now - a dropped one is
        // not, so a reconciliation does not make an old value up for it
        // (GFormController.modifyFormChangesWithChangePropertyAsyncs) - where focus goes: nowhere, React draws it, and
        // the server refuses ACTIVATE PROPERTY of it (FormInstance.activateProperty) - and the client's own value for a
        // cell, an optimistic change, into the draft only, published once the change that wrote it is over (change)
        public boolean isPropertyShown(GPropertyDraw property) {
            return shown;
        }
        public void focusProperty(GPropertyDraw property) {
        }
        // its bindings - keys and mouse ones - (GFormController.initializeBindings): taken with the view that draws it
        // while the form shows it, where it has a change that asks no value in the cell right away (asksValue) and is
        // not READONLY on the form, and only with a cell to press them on - its cell for the current objects, a
        // column's in the current row - the form does not show read only now (READONLYIF); with none, an event is left
        // to the other bindings. Pressed, it runs the change event as the platform's binding of a cell does
        // (pressProperty)
        public Widget getBindingWidget(GPropertyDraw property) {
            if (!shown || !property.hasUserChangeAction || property.isReadOnly() != null || asksValue())
                return null;
            GGroupObjectValue cell = getCellKey(GGroupObjectValue.EMPTY);
            if (cell == null || (property.readOnlyReader != null && values.get(property.readOnlyReader, cell) != null))
                return null;
            return bindings.getViewWidget(node.state.scope);
        }
        public void onBinding(GPropertyDraw property, Event event) {
            bindings.pressProperty(property, event);
        }
        public Pair<GGroupObjectValue, PValue> setLoadingValueAt(GPropertyDraw property, GGroupObjectValue fullCurrentKey, PValue value) {
            // no cell while the form does not show it, as the platform's view of it has none then: the answer takes
            // `loading` off only what the form shows (GFormController.modifyFormChangesWithLoadingPropertyAsyncs)
            GGroupObjectValue cellKey = shown ? getCellKey(fullCurrentKey) : null;
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
        // state - so a call through it names nothing but the row (PropertyMember)
        void putMember(JavaScriptObject owner) {
            setField(owner, property.integrationSID, makePropertyMember(this, getName(property) + "."));
        }
        public GPropertyDraw getProperty() {
            return property;
        }
        public Verbs getVerbs() {
            return verbs;
        }
    }
    // a panel or empty-group property's entry: its value and every attribute of it, in one place
    private final class ReactPanelPropertyEntry extends ReactPropertyEntry {
        final GPropertyReader[] readers; // what it is drawn with
        // whether the form shows its asynchronous requests on it now - on the property that shows them, drawAsync
        boolean forceLoading;

        ReactPanelPropertyEntry(Node node, GPropertyDraw property) {
            super(node, property);
            readers = present(property.getPresentationReaders());
            if (property.drawAsync)
                bindings.setAsyncView(this::setForceLoading);
            updateEntry(); // hidden until it is delivered
        }
        // rebuilt where it differs: the platform takes them off whenever the last answer comes, shown or not
        void setForceLoading(boolean forceLoading) {
            if (this.forceLoading != forceLoading) {
                this.forceLoading = forceLoading;
                updateEntry();
            }
        }
        // loading while its change waits for its answer, and while the form shows its requests on it, as the
        // platform's button of it is (ActionOrPropertyPanelValue.isLoading)
        @Override
        boolean isLoading(GGroupObjectValue key) {
            return super.isLoading(key) || forceLoading;
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
        public GGroupObjectValue getMemberKey(String errorPrefix, JavaScriptObject objectOrKey) {
            // raw JS key: a numeric 0 key reads as null under Java == null (GWT falsy-primitive collapse)
            if (!GwtClientUtils.isUndefinedOrNull(objectOrKey))
                throw refusal(errorPrefix + (property.groupObject == null
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
        // the column: its attributes, what the property is, and what its rows say of it (emitColumn)
        void emit(JavaScriptObject entry) {
            emitAttributes(entry, columnReaders, GGroupObjectValue.EMPTY, property);
            emitPropertyFacts(entry, property);
            rowsPart.emitColumn(entry, this);
        }
        void writeCell(GGroupObjectValue key) {
            JavaScriptObject row = rowsPart.editRow(key);
            if (row != null)
                fillRow(row, key);
        }
        // a cell's text as it is copied, formatted as the form formats what the server copies (copyExternalTable)
        String getCopy(GGroupObjectValue key) {
            return property.formatCopy(getValueAt(key), property.getPasteType(), getPattern(key));
        }
        // the pattern a value pasted into a cell is read by: the cell's, of a row the view has, else the property's
        String getPattern(GGroupObjectValue key) {
            PValue pattern = key != null && property.patternReader != null ? values.get(property.patternReader, key) : null;
            return pattern != null ? PValue.getStringValue(pattern) : property.getPattern();
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
        public GGroupObjectValue getMemberKey(String errorPrefix, JavaScriptObject objectOrKey) {
            // raw JS key: a numeric 0 key reads as null under Java == null (GWT falsy-primitive collapse)
            if (GwtClientUtils.isUndefinedOrNull(objectOrKey))
                return GGroupObjectValue.EMPTY;
            GGroupObjectValue objectKey = GGroupObjectValue.resolveObject(objectOrKey);
            if (objectKey == null) // ... a KEY, looked up in the rows the column is drawn in
                return rowsPart.resolveRow(errorPrefix, objectOrKey);
            if (!objectKey.isEmpty() && property.groupObject.filterRowKeys(objectKey) == null)
                throw refusal(errorPrefix + "that row is not a row of '" + property.groupObject.getSID() + "'");
            return objectKey;
        }
    }
    // a property the platform draws and React places: the form's controller of it is this entry, over
    // the platform's (`platform`), which draws it. Its values, its drop, focus, the client's own value, any other
    // attribute or reader are the platform's, handed on with the property; what labels it is its kind's - React's for a
    // property, the platform's for an ACTION, whose caption and image are its button's face. No member: what the
    // platform draws, the platform changes. There from the start, and `hidden` whenever the platform does not show the
    // property - it shows it from its first whole delivery until its drop, both of which come through here
    private abstract class PlatformPropertyEntry extends PropertyEntry {
        final PlatformProperty platform; // the view it is drawn in

        PlatformPropertyEntry(Node node, GPropertyDraw property, PlatformProperty platform) {
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
        // ... and so are its keys: taken on the platform's renderer - in the host the view places it in, or in the
        // current row's where it is drawn in every row
        public Widget getBindingWidget(GPropertyDraw property) {
            return platform.getBindingWidget(property);
        }
        public void onBinding(GPropertyDraw property, Event event) {
            platform.onBinding(property, event);
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
    // draws is, and carried by name with them. A <Platform> places it by the same name (getName), and hides its host by
    // this entry (ViewState.entryOf): it says whether the form shows it - all that an action's says
    private abstract class PlatformPanelEntry extends PlatformPropertyEntry {
        PlatformPanelEntry(Node node, GPropertyDraw property, PlatformProperty platform) {
            super(node, property, platform);
        }
        void emit(JavaScriptObject entry) {
        }
    }
    // ... a property's: its labels too - caption, image and comment, the platform drawing none of them
    private final class PlatformPanelPropertyEntry extends PlatformPanelEntry {
        final GPropertyReader[] labelReaders; // its caption, image and comment

        PlatformPanelPropertyEntry(Node node, GPropertyDraw property, PlatformProperty platform) {
            super(node, property, platform);
            labelReaders = present(property.captionReader, property.getImageReader(), property.commentReader);
            updateEntry(); // there from the start, hidden until the platform shows the property
        }
        // what labels it: React's
        public void updateLabel(GExtraLabelReader reader, NativeHashMap<GGroupObjectValue, PValue> delivered, boolean partial) {
            keep(reader, delivered, partial);
        }
        void emit(JavaScriptObject entry) {
            emitAttributes(entry, labelReaders, GGroupObjectValue.EMPTY, property);
        }
    }
    // ... an ACTION's: its caption and image are its button's face, which the platform draws, so what labels it is
    // handed on. The platform's button runs it
    private final class PlatformPanelActionEntry extends PlatformPanelEntry {
        PlatformPanelActionEntry(Node node, GPropertyDraw property, PlatformProperty platform) {
            super(node, property, platform);
            updateEntry(); // there from the start, hidden until the platform shows the action
        }
        // what labels it: the platform's, its button's face
        public void updateLabel(GExtraLabelReader reader, NativeHashMap<GGroupObjectValue, PValue> delivered, boolean partial) {
            platform.updateLabel(reader, delivered, partial);
        }
    }
    // ... a list one's column, whose cells or buttons the platform draws in the rows React draws, carried by name on
    // the node the rows are on: React draws the column from this entry, which says what the property is - a
    // platform-drawn column is filtered and sorted like any other, and the platform drawing its VALUE says nothing
    // about what the value is
    private abstract class PlatformColumnEntry extends PlatformPropertyEntry {
        PlatformColumnEntry(RowsPart rowsPart, GPropertyDraw property, PlatformProperty platform) {
            super(rowsPart.node, property, platform);
        }
        void emit(JavaScriptObject entry) {
            emitPropertyFacts(entry, property);
        }
    }
    // ... a property's: its labels too - caption, image, footer and comment
    private final class PlatformColumnPropertyEntry extends PlatformColumnEntry {
        final GPropertyReader[] labelReaders; // its caption, image, footer and comment

        PlatformColumnPropertyEntry(RowsPart rowsPart, GPropertyDraw property, PlatformProperty platform) {
            super(rowsPart, property, platform);
            labelReaders = present(property.captionReader, property.getImageReader(), property.footerReader, property.commentReader);
            updateEntry(); // there from the start, hidden until the platform shows the property
        }
        // what labels it: React's
        public void updateLabel(GExtraLabelReader reader, NativeHashMap<GGroupObjectValue, PValue> delivered, boolean partial) {
            keep(reader, delivered, partial);
        }
        void emit(JavaScriptObject entry) {
            emitAttributes(entry, labelReaders, GGroupObjectValue.EMPTY, property);
            super.emit(entry);
        }
    }
    // ... an ACTION's: what labels it is its buttons' face, handed on - an action's image is a button's, row by row
    private final class PlatformColumnActionEntry extends PlatformColumnEntry {
        PlatformColumnActionEntry(RowsPart rowsPart, GPropertyDraw property, PlatformProperty platform) {
            super(rowsPart, property, platform);
            updateEntry(); // there from the start, hidden until the platform shows the action
        }
        // what labels it: the platform's, its buttons' face
        public void updateLabel(GExtraLabelReader reader, NativeHashMap<GGroupObjectValue, PValue> delivered, boolean partial) {
            platform.updateLabel(reader, delivered, partial);
        }
    }

    // the name a view has for a component - in `data`, on the controller and in a <Platform> alike, null for one with
    // none: a property's is its integration name, after its group's, as its entry is data.o.note - alone for the empty
    // group's, data.note; any other component's is its design identifier, BOX(o)
    public static String getName(GComponent component) {
        if (!(component instanceof GPropertyDraw))
            return component.sID;
        GPropertyDraw property = (GPropertyDraw) component;
        if (property.integrationSID == null)
            return null;
        return property.groupObject == null ? property.integrationSID : property.groupObject.getSID() + "." + property.integrationSID;
    }

    // where a group's ROWS go: the React view that draws them, null when the platform does - asked of what DRAWS
    // them, never of the group's box. A group not in LIST view has none anywhere: the server sends it no rows, only its
    // current object, which nothing chooses among rows, so no view gets a node of rows for it, wherever its grid sits
    // (mirrors FormView.rowsScope)
    private GComponent rowsScope(GGroupObject group) {
        return !group.viewType.isList() ? null : componentScope(group.getDrawComponent());
    }
    // where what a COMPONENT draws is drawn - its content: the React view that draws it, its own where its custom is a
    // React component, null when the platform does - a platform-drawn one too, whose content is the platform's wherever
    // React places it. A list property has no place of its own: it is drawn as its column, where the rows are,
    // platform-drawn or not (mirrors FormView.componentScope)
    private GComponent componentScope(GComponent component) {
        if (component instanceof GPropertyDraw && ((GPropertyDraw) component).isList)
            return rowsScope(((GPropertyDraw) component).groupObject);
        return drawn(component);
    }
    // ... and WHERE THE VIEW NAMES IT, null where none does: the one container a component is named in (getName), and
    // where its entry is. A property is named on its group's node, where React draws its content - a list property's
    // column, platform-drawn or not, where the rows are - or, for a platform-drawn panel one, where React places it, its entry labelling what
    // the platform draws (an ACTION's saying only whether the form shows it: its caption and image are its button's
    // face). Any other component is named at the top level, where React draws or places it - one the design does not
    // name, a grid's record, nowhere (mirrors FormView.nameScope)
    private GComponent nameScope(GComponent component) {
        if (component instanceof GPropertyDraw && ((GPropertyDraw) component).isList) // its column, where the rows are
            return rowsScope(((GPropertyDraw) component).groupObject);
        return component.sID != null ? placed(component) : null;
    }
    // where what a FILTERS box holds is drawn - the box's part being its content -: the React view built here that
    // draws it, null elsewhere (mirrors FormView.filtersScope)
    private GComponent filtersScope(GContainer box) {
        return box != null ? drawn(box) : null;
    }
    // ... where a FILTER GROUP is drawn as a part of its group: one with filters - one with none has no view -, of a
    // group - none has no node of its own to go on (mirrors FormView.filterGroupScope)
    private GComponent filterGroupScope(GRegularFilterGroup filterGroup) {
        if (filterGroup.filters.isEmpty())
            return null;
        return filterGroup.groupObject != null ? componentScope(filterGroup) : null;
    }
    // ... where a property's FILTER is drawn as a part of its group - as what a FILTERS box holds is (filtersScope),
    // where the component's content is: the React view it stands in, or its own, whoever draws the group's conditions
    // -, of a group with user filters; none where the platform draws it (mirrors FormView.filterScope)
    private GComponent filterScope(GFilter filter) {
        return hasUserFilters(filter.property.groupObject) ? drawn(filter) : null;
    }
    // ... whether a group HAS user filters: a list's group's and a tree's - none in PANEL view, as the platform shows
    // none for it (mirrors FormView.hasUserFilters)
    private static boolean hasUserFilters(GGroupObject group) {
        return group.isInTree() || group.viewType.isList();
    }
    // ... where the FILTERCONTROLS of a table or a tree is drawn as a part of its table - of a list's grid, or of a
    // tree, one for all its groups: the form asks no other (GFormController.initializeTable) - (mirrors
    // FormView.filterControlsScope)
    private GComponent filterControlsScope(GGridProperty table) {
        return componentScope(table.filterControls);
    }
    // ... and where its TOOLBARSYSTEM is, shown (mirrors FormView.toolbarScope)
    private GComponent toolbarScope(GGridProperty table) {
        return table.toolbar.visible ? componentScope(table.toolbar) : null;
    }
    // the React view a component is in, as long as it has a view: one REMOVEd from the design is still the container
    // of what it held, but nothing built it, so nothing of it is projected. Asked only here, while the form is built -
    // never by a delta: what a reader brings goes to what React has of its owner
    private GComponent placed(GComponent component) {
        if (views.isEmpty()) // a form with no React view: nowhere to be placed, nothing walked
            return null;
        return built(place.apply(component));
    }
    // ... and the one what a component draws is in - its content: its own, where its custom is a React component; none
    // where it is platform-drawn, its view drawing all of it; else the one it is in (mirrors
    // ComponentView.getContentReactPlace)
    private GComponent drawn(GComponent component) {
        if (component.isReact())
            return built(component);
        return component.isPlatformDrawn() ? null : placed(component);
    }
    // ... a React view's component, as long as the view is built
    private GComponent built(GComponent scope) {
        return scope != null && views.get(scope) != null ? scope : null;
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

    private GGroupObjectValue getNearObject(ArrayList<GGroupObjectValue> rows, int removedIndex) {
        if (rows.isEmpty())
            return null;
        return rows.get(removedIndex == rows.size() ? removedIndex - 1 : removedIndex);
    }

    private static native JavaScriptObject keyedRow(JavaScriptObject byKey, JavaScriptObject token) /*-{
        return (typeof token === 'string' || typeof token === 'number') ? byKey[String(token)] : null;
    }-*/;

    // ===== WHAT A CALL DOES - of a view's member, or of the form's controller, by name (CONTROLLER.FORM, below)
    // -: once what it names is known, the same, whoever draws what it changes. A member binds what it names - the row
    // of its group, its filter group, its toolbar's group -, the form's controller reads it from the names it is given

    // the row of a GROUP a call names, narrowed by the group's own rule - which both checks and cuts. A key that is not
    // the group's row at all answers null: it would otherwise set the objects it does carry and blank the rest, which
    // the server only asserts about (so: nothing at all in production). An empty key names no row either: passed on, it
    // would clear the group's current objects, which choosing a row does not offer. And in a TREE a row of a group
    // BELOW carries the path past this one: accepted (it holds one row of every group above), but what it names is the
    // path down to here - the deeper half names rows of other groups, which the server asserts about too, and the
    // projection would keep as a current object no row of this group is found by
    private static GGroupObjectValue readGroupRow(String errorPrefix, GGroupObject group, GGroupObjectValue key) {
        GGroupObjectValue rowKey = key.isEmpty() ? null : group.getRowKey(key);
        if (rowKey == null)
            throw refusal(errorPrefix + "that row is not a row of '" + group.getSID() + "'");
        return rowKey;
    }

    // a FILTER GROUP's filter selected: the index of one of its filters, or null for none where none is offered - sent
    // even where it is shown: what the server holds may be other - a batch that failed leaves the filter it selected
    // shown -, and asked again, it is set again; what shows the filter group is told of it by the form
    // (Verbs.changeRegularFilter)
    private JavaScriptObject selectFilter(String surface, GRegularFilterGroup filterGroup, Object index) {
        String errorPrefix = controllerPrefix(surface);
        int size = filterGroup.filters.size();
        GRegularFilter filter = null;
        if (index == null) {
            if (!offersNone(filterGroup))
                throw refusal(errorPrefix + "takes the index of one of its filters, 0 to " + (size - 1)
                        + ": one has to be selected");
        } else {
            int value = readWholeNumber(index, size);
            if (value < 0)
                throw refusal(errorPrefix + "takes the index of one of its filters, 0 to " + (size - 1)
                        + (offersNone(filterGroup) ? ", or null for none" : ""));
            filter = filterGroup.filters.get(value);
        }
        return verbs.changeRegularFilter(filterGroup, filter);
    }
    // ... whether none may be selected, as the platform's widget offers it: a group of one filter is its check box,
    // cleared whatever NONULL says - the server takes a NONULL group's null too (FilterGroupAction) -, a group of
    // several its list, with "(All)" unless NONULL
    private static boolean offersNone(GRegularFilterGroup filterGroup) {
        return filterGroup.filters.size() == 1 || !filterGroup.noNull;
    }

    // a property's FILTER's condition, in its slot of the table's user filters (GUserFiltersController): a value, read
    // by the property's FILTER type as `value` gives it, or none - absent, null, '', or one the type reads as NULL, a
    // two-state LOGICAL's false -, and the options, if any, read as a condition's fields (readFilterCondition), in a
    // batch too. The options are read whatever the value - one misspelt is refused even where there is none, which
    // takes the condition off all the same. A file's value is a link to the file, not the file a condition compares
    // with, so its filter's condition can be taken off, not stated. Shown at once - sent but in the manual apply mode,
    // where it waits for the apply
    private JavaScriptObject changeFilterCondition(String surface, GFilter filter, GUserFiltersController userFilters, JavaScriptObject value, JavaScriptObject options) {
        String errorPrefix = controllerPrefix(surface);
        if (GwtClientUtils.isUndefinedOrNull(options)) // none: as stating none of them
            options = newBareObject();
        checkAuthorObject(errorPrefix, options, "the options argument", FILTER_OPTIONS);
        GPropertyFilter stated = readFilterCondition(errorPrefix, options,
                GPropertyFilter.getBase(getConditionColumn(filter.property), userFilters.getSlotEntry(filter).condition), value);
        return verbs.changeUserFilters(filter.property.groupObject.getDrawComponent().getGroups(), () -> {
            if (stated.nullValue())
                userFilters.clearFilter(filter);
            else
                userFilters.changeFilter(filter, stated);
        });
    }
    // the FILTER CONTROLS' commands, what the platform's buttons do over a table's conditions, whoever draws them: the
    // add, given to the table's filter input controller (handler), where the design has the FILTERS box, as the button
    // is there only then; the apply of what waits - nothing with nothing waiting - and the reset, the table's user
    // filters' (GUserFiltersController.apply, reset), given to that controller where there is one - the platform's
    // panel sends the rows it prepares too -, else to the user filters themselves (userFilters). Refused with nothing
    // to do it with: no FILTERS box, no filter input controller - for the apply and the reset no user filters either
    private JavaScriptObject addCondition(String surface, List<GGroupObject> groups, GFiltersHandler handler) {
        if (handler == null)
            throw noFilterInput(surface);
        if (!handler.hasFiltersContainer())
            throw refusal(controllerPrefix(surface) + "has no FILTERS box to add a condition to: the design has none");
        return verbs.changeUserFilters(groups, handler::addCondition);
    }
    private JavaScriptObject applyConditions(String surface, List<GGroupObject> groups, GFiltersHandler handler, GUserFiltersController userFilters) {
        if (handler == null && userFilters == null)
            throw noFilterInput(surface);
        return verbs.changeUserFilters(groups, handler != null ? handler::applyConditions : userFilters::apply);
    }
    private JavaScriptObject resetConditions(String surface, List<GGroupObject> groups, GFiltersHandler handler, GUserFiltersController userFilters) {
        if (handler == null && userFilters == null)
            throw noFilterInput(surface);
        return verbs.changeUserFilters(groups, handler != null ? handler::resetConditions : userFilters::reset);
    }
    // ... there being no filter input controller: the platform builds none where it draws the conditions and its
    // toolbar turns its filters off (showFilters)
    private static RuntimeException noFilterInput(String surface) {
        return refusal(controllerPrefix(surface) + "has nothing to give its commands to: the platform draws "
                + "the conditions, and its toolbar turns its filters off (showFilters)");
    }

    // the VIEW FILTERS of a grid's group whose rows a view draws, as a call states them: all of them, an array - an
    // empty one for none -, each read as a condition of the user filters' list is (FiltersTableList.readConditions):
    // on a property of the group, wherever it is drawn, what it does not state the default way, no value comparing
    // with NULL. They are applied together, as the server keeps them, so one joined to the next by `or` is refused.
    // What draws the rows is told of them by the form (Verbs.changeViewFilters)
    private JavaScriptObject changeViewConditions(String surface, GGroupObject group, JavaScriptObject conditions) {
        String errorPrefix = controllerPrefix(surface);
        if (!isJSArray(conditions))
            throw refusal(errorPrefix + "takes the conditions, as an array - an empty one for none");
        ArrayList<GPropertyFilter> result = filtersList.readConditions(errorPrefix, group.grid, conditions);
        for (int i = 0; i < result.size(); i++)
            if (!result.get(i).junction)
                throw refusal(errorPrefix + "condition " + i + ": view filters are all applied together, so 'or' cannot be true");
        return verbs.changeViewFilters(group, result, null);
    }

    // a PROPERTY'S SUGGESTION LIST - getValues(value[, {row, mode, count}]) of its member, getValues(name, value[,
    // ...]) of the form's controller -: the Promise of the list the server offers for the cell the call names, from the
    // lookup `mode` picks (objects, values, change), `count` more than the server's own number - refused while the form
    // refuses a request, as a toolbar's count is -; the cell's key read from `row` by the caller, as it names rows
    // (key: the current objects for none). The row is whatever the author gave - a data row, its handle, a key: a
    // number, a string, a Java object -, so its reader is typed as JSNI hands the value over, not through a generic,
    // whose bridge would check it as a JS object
    private static JavaScriptObject lookupValues(Verbs verbs, String surface, GPropertyDraw property, String value, JavaScriptObject options, RowKey key) {
        String errorPrefix = controllerPrefix(surface);
        if (value == null)
            throw refusal(errorPrefix + "takes the query, as a string");
        if (GwtClientUtils.isUndefinedOrNull(options))
            options = newBareObject();
        checkAuthorObject(errorPrefix, options, "the options argument", "row,mode,count");
        String mode = hasOwnField(options, "mode") ? getOwnString(options, "mode") : ServerResponse.OBJECTS;
        if (mode == null)
            throw refusal(errorPrefix + "'mode' must be the lookup's name, as a string");
        String actionSID = GFormController.getAsyncActionSID(mode);
        if (actionSID == null)
            throw refusal(errorPrefix + GFormController.getUnknownModeReason(mode));
        int count = hasOwnField(options, "count") ? readWholeNumber(JSValueConverter.toObject(getOwnField(options, "count")), Double.POSITIVE_INFINITY) : 0;
        if (count < 0)
            throw refusal(errorPrefix + "'count' must be a whole number, 0 or more");
        return verbs.getPropertyValues(surface, property, key.read(getOwnField(options, "row")), value, actionSID, count);
    }
    private interface RowKey {
        GGroupObjectValue read(JavaScriptObject row);
    }

    // what a TOOLBAR does - each a request, refused while the form refuses one -: the count of a list group's
    // rows and the sum of a column of them, each the Promise of the server's answer ...
    private JavaScriptObject countRecords(String surface, GGroupObject group) {
        return verbs.countRecords(surface, group);
    }
    // ... a column's: a number property of the group, named as a state names one - not one grouped in columns, as the
    // call names no column
    private JavaScriptObject calculateSum(String surface, GGroupObject group, String propertySID) {
        String errorPrefix = controllerPrefix(surface);
        if (propertySID == null)
            throw refusal(errorPrefix + "takes the name of the property to sum, as a string");
        GPropertyDraw property = readStateProperty(errorPrefix, group, propertySID);
        if (property.hasColumnGroupObjects())
            throw refusal(errorPrefix + "'" + propertySID + "' is grouped in columns, and a sum names no column");
        if (!(property.getValueType() instanceof GIntegralType))
            throw refusal(errorPrefix + "'" + propertySID + "' is not a number");
        return verbs.calculateSum(surface, group, property, GGroupObjectValue.EMPTY);
    }
    // ... the export of a table's rows: the server's report of them - a list group's, or a tree's
    private JavaScriptObject runTableReport(String surface, GGridProperty table) {
        return verbs.runTableReport(surface, table);
    }
    // ... a list group's manual update turned on or off, as the platform's toggle does - what shows the group's
    // toolbar told by the form, nothing held back yet (Verbs.changeUpdateMode)
    private JavaScriptObject changeManualUpdate(String surface, GGroupObject group, Object manual) {
        boolean value = readBoolean(controllerPrefix(surface), manual);
        return verbs.changeUpdateMode(group, value ? GUpdateMode.MANUAL : GUpdateMode.AUTO);
    }
    // ... and the changes it holds back brought now, as the platform's button does
    private JavaScriptObject forceUpdate(GGroupObject group) {
        return verbs.changeUpdateMode(group, GUpdateMode.FORCE);
    }
    // ... and a list group's rows shown as another view, one of those the design gives them, as the platform's button
    // does - none where no table of the platform's draws the rows (gridRows, GGridController.getListViewController): no
    // name is one of its views then
    private JavaScriptObject switchListView(String surface, GGroupObject group, Object name) {
        GListViewController views = gridRows.get(group.grid).getListViewController();
        if (views == null)
            throw refusal(controllerPrefix(surface) + "has no view to switch to: no table of the platform's draws the rows");
        GListViewType view = null;
        StringBuilder names = new StringBuilder(); // the names it takes, said where it is given another
        for (GListViewType listView : views.getListViews()) {
            String viewName = getViewName(listView);
            if (viewName.equals(name))
                view = listView;
            names.append(names.length() > 0 ? ", " : "").append(viewName);
        }
        if (view == null)
            throw refusal(controllerPrefix(surface) + "takes the name of a view of the table: " + names);
        return verbs.changeListView(views, view);
    }
    // ... and the platform's filter controls shown or hidden, as the toolbar's filter button does - on an empty list of
    // the platform's panel, with a condition to fill in -, the table's filter input controller's (filter) where its
    // toolbar has the button (hasControlsButton); refused where there is no input, or it has no button: there is
    // nothing of the platform's to show (GFilterControlsToggle)
    private JavaScriptObject toggleFilterControls(String surface, List<GGroupObject> groups, GFilterInputController filter, Object shown) {
        String errorPrefix = controllerPrefix(surface);
        boolean value = readBoolean(errorPrefix, shown);
        if (filter == null || !filter.hasControlsButton())
            throw refusal(errorPrefix + "has no filter controls of the platform's to show: the platform "
                    + "draws neither the conditions nor FILTERCONTROLS here, or its toolbar turns its filters off (showFilters)");
        return verbs.changeUserFilters(groups, () -> {
            if (value != filter.isControlsVisible())
                filter.toggleControlsVisible();
        });
    }

    // ===== CONTROLLER.FORM - the form's controller: what a React view's controller names `form`, a classic view's
    // `controller.form`, an INTERNAL CLIENT action's argument (GFormController.controller) - is built on STRINGS: it
    // projects no data, so it has no member to name anything by, and what it reaches it is given the name of, checked
    // when the call is made - a group by its SID, a tree by its name, a TABLE by its list group's SID or its tree's
    // name, a filter group by its name. Its methods, one per call a member makes, do what that member does (WHAT A CALL
    // DOES) to the form's state, whoever draws it - a view, another view, the platform, which shows it as it shows the
    // form's own edits -, the way across containers. Each is the member's call with the address read from its names,
    // given the call as a message names it (surface) by what puts it on the controller, as a member's is
    // (addFormMethods)

    // the current object of a group: a row of it - the row, or its `objects` handle -; of a grid whose rows a view
    // draws, an edge of the rows too, and what becomes of the rows selected (GridRowsPart.changeCurrentObject)
    private JavaScriptObject changeObject(String surface, String name, JavaScriptObject objectOrKey, JavaScriptObject options) {
        String errorPrefix = controllerPrefix(surface);
        GGroupObject group = readGroup(form, errorPrefix, name);
        if (hasGridRows(group) || isSelectionChange(errorPrefix, options) || isEdge(objectOrKey))
            return readGridRows(errorPrefix, group).changeCurrentObject(errorPrefix, objectOrKey, options, row -> readFormRow(errorPrefix, group, row));
        return verbs.changeCurrentObject(group, readFormRow(errorPrefix, group, objectOrKey), null, null);
    }
    // ... such a grid's current column, and every row and column of it selected
    private JavaScriptObject changeColumn(String surface, String name, String column, JavaScriptObject options) {
        return readGridRows(surface, name).changeColumn(surface, column, options);
    }
    private JavaScriptObject selectAll(String surface, String name) {
        return readGridRows(surface, name).selectAll();
    }
    // ... and its clipboard
    private JavaScriptObject copy(String surface, String name) {
        return readGridRows(surface, name).copy(surface);
    }
    private JavaScriptObject paste(String surface, String name, String text) {
        return readGridRows(surface, name).paste(surface, text);
    }
    // ... the selection and its clipboard being the grid's own where a view draws its rows: a group of a tree has
    // none, and a table of the platform's keeps its own (gridRows)
    private GridRowsPart readGridRows(String surface, String name) {
        String errorPrefix = controllerPrefix(surface);
        return readGridRows(errorPrefix, readGroup(form, errorPrefix, name));
    }
    private GridRowsPart readGridRows(String errorPrefix, GGroupObject group) {
        if (!hasGridRows(group))
            throw refusal(errorPrefix + "'" + group.getSID() + "' has no selection of a view's: a view selects among the rows it"
                    + " draws, of a list group not in a tree");
        return ((GridTablePart) gridRows.get(group.grid)).currentPart();
    }
    private boolean hasGridRows(GGroupObject group) {
        return !group.isInTree() && gridRows.get(group.grid) instanceof GridTablePart;
    }
    // ... a node of a tree's group, opened or closed, and every node of it and of the groups below it
    private JavaScriptObject expand(String surface, String name, JavaScriptObject objectOrKey) {
        String errorPrefix = controllerPrefix(surface);
        GGroupObject group = readTreeGroup(errorPrefix, name);
        return verbs.expandNode(group, readFormRow(errorPrefix, group, objectOrKey));
    }
    private JavaScriptObject collapse(String surface, String name, JavaScriptObject objectOrKey) {
        String errorPrefix = controllerPrefix(surface);
        GGroupObject group = readTreeGroup(errorPrefix, name);
        return verbs.collapseNode(group, readFormRow(errorPrefix, group, objectOrKey));
    }
    private JavaScriptObject expandAll(String surface, String name) {
        return verbs.expandAll(readTreeGroup(controllerPrefix(surface), name));
    }
    private JavaScriptObject collapseAll(String surface, String name) {
        return verbs.collapseAll(readTreeGroup(controllerPrefix(surface), name));
    }
    // ... and a tree's current node opened with every node under it, and the tree from the current node's group down -
    // by whatever draws the tree's rows, as the toolbar's buttons open it
    private JavaScriptObject expandCurrent(String surface, String name) {
        GTreeGroup tree = readTree(controllerPrefix(surface), name);
        return expandCurrentOf(tree.groups, treeRows.get(tree));
    }
    private JavaScriptObject expandTree(String surface, String name) {
        GTreeGroup tree = readTree(controllerPrefix(surface), name);
        return expandTreeOf(tree.groups, treeRows.get(tree));
    }
    // ... as a member opens it too - the tree's own, or its toolbar's (addExpandCurrentMember): by what draws its rows,
    // which sends the request itself (Verbs.expandNodes)
    private JavaScriptObject expandCurrentOf(List<GGroupObject> groups, GTreeController rows) {
        return verbs.expandNodes(groups, rows::expandCurrent);
    }
    private JavaScriptObject expandTreeOf(List<GGroupObject> groups, GTreeController rows) {
        return verbs.expandNodes(groups, rows::expandTree);
    }
    // a table's sortings
    private JavaScriptObject changeOrders(String surface, String name, JavaScriptObject orders) {
        return ordersList.change(surface, readTable(controllerPrefix(surface), name), orders);
    }
    // ... a filter group's filter selected
    private JavaScriptObject changeFilterGroup(String surface, String name, Object index) {
        return selectFilter(surface, readFilterGroup(controllerPrefix(surface), name), index);
    }
    // ... and what a table's toolbar does: of a list group, the count and a column's sum, the manual update and what
    // it holds back, the view switch; of either table, the export
    private JavaScriptObject getCount(String surface, String name) {
        return countRecords(surface, readGridGroup(controllerPrefix(surface), name));
    }
    private JavaScriptObject getSum(String surface, String name, String propertySID) {
        return calculateSum(surface, readGridGroup(controllerPrefix(surface), name), propertySID);
    }
    private JavaScriptObject changeManual(String surface, String name, Object manual) {
        return changeManualUpdate(surface, readGridGroup(controllerPrefix(surface), name), manual);
    }
    private JavaScriptObject update(String surface, String name) {
        return forceUpdate(readGridGroup(controllerPrefix(surface), name));
    }
    private JavaScriptObject changeListView(String surface, String name, Object view) {
        return switchListView(surface, readGridGroup(controllerPrefix(surface), name), view);
    }
    private JavaScriptObject exportTable(String surface, String name) {
        return runTableReport(surface, readTable(controllerPrefix(surface), name));
    }
    // ... a property's suggestion list, the property named as the classic changeProperty names one
    // (readClassicProperty) - one the form hides too, as changeProperty changes it: no view of the form's controller's
    // shows it -, the row - a data row, its `objects` handle - of the property's group, narrowed to the group's row,
    // the cell's key (readFormRow), none for the current objects, and none at all for a property of the form level,
    // which has no rows
    private JavaScriptObject getValues(String surface, String name, String value, JavaScriptObject options) {
        String errorPrefix = controllerPrefix(surface);
        GPropertyDraw property = readClassicProperty(form, errorPrefix, name);
        return lookupValues(verbs, surface, property, value, options, row -> {
            if (GwtClientUtils.isUndefinedOrNull(row))
                return GGroupObjectValue.EMPTY;
            if (property.groupObject == null)
                throw noRowsOfFormLevel(errorPrefix, name);
            return readFormRow(errorPrefix, property.groupObject, row);
        });
    }
    // ... a table's user filters: its conditions, a property's FILTER's, and what its filter controls do - given to its
    // filter input controller, whoever draws them (getFilterInput) -, and their showing, where its toolbar's button
    // shows and hides them
    private JavaScriptObject changeFilters(String surface, String name, JavaScriptObject conditions) {
        return filtersList.change(surface, readTable(controllerPrefix(surface), name), conditions);
    }
    private JavaScriptObject changeFilter(String surface, String name, JavaScriptObject value, JavaScriptObject options) {
        GFilter filter = readFilter(controllerPrefix(surface), name);
        return changeFilterCondition(surface, filter, getUserFilters(filter.property.groupObject.getDrawComponent()), value, options);
    }
    private JavaScriptObject addFilter(String surface, String name) {
        GGridProperty table = readTable(controllerPrefix(surface), name);
        return addCondition(surface, table.getGroups(), getFilterInput(table));
    }
    private JavaScriptObject applyFilters(String surface, String name) {
        GGridProperty table = readTable(controllerPrefix(surface), name);
        return applyConditions(surface, table.getGroups(), getFilterInput(table), getUserFilters(table));
    }
    private JavaScriptObject resetFilters(String surface, String name) {
        GGridProperty table = readTable(controllerPrefix(surface), name);
        return resetConditions(surface, table.getGroups(), getFilterInput(table), getUserFilters(table));
    }
    private JavaScriptObject changeFilterControlsVisible(String surface, String name, Object shown) {
        GGridProperty table = readTable(controllerPrefix(surface), name);
        return toggleFilterControls(surface, table.getGroups(), getFilterInput(table), shown);
    }
    // ... and the view filters of a grid's group whose rows a view draws: the view's own, so a group whose rows the
    // platform draws has none for a call to state - the views a table of the platform's shows the rows as state their
    // own, and a switch of them takes them off (gridRows)
    private JavaScriptObject changeViewFilters(String surface, String name, JavaScriptObject conditions) {
        String errorPrefix = controllerPrefix(surface);
        GGroupObject group = readGroup(form, errorPrefix, name);
        if (!hasGridRows(group))
            throw refusal(errorPrefix + "'" + name + "' has no view filters to state: a view states them for the rows it"
                    + " draws, of a list group not in a tree");
        return changeViewConditions(surface, group, conditions);
    }
    // the user filters of a table, its groups' one model
    private GUserFiltersController getUserFilters(GGridProperty table) {
        return tableFilters.get(table).userFilters;
    }
    // ... and its filter input controller, made with the model - none where the platform makes none
    private GFilterInputController getFilterInput(GGridProperty table) {
        return tableFilters.get(table).input;
    }

    // ... and the names, each read as the call needs it - a name is a string, and one that names nothing, or names
    // what the call has nothing to do with, is refused. A GROUP, by its SID - the group half of a classic name too
    // (readClassicProperty)
    private static GGroupObject readGroup(GForm form, String errorPrefix, String name) {
        if (name == null)
            throw refusal(errorPrefix + "takes the name of an object group, as a string");
        GGroupObject group = form.getGroupObject(name);
        if (group == null)
            throw refusal(errorPrefix + "unknown object group '" + name + "'");
        return group;
    }
    // ... a group of a tree, whose rows are the tree's nodes
    private GGroupObject readTreeGroup(String errorPrefix, String name) {
        GGroupObject group = readGroup(form, errorPrefix, name);
        if (!group.isInTree())
            throw refusal(errorPrefix + "'" + name + "' is not a group of a tree: its rows are no nodes to open");
        return group;
    }
    // ... a TREE, by its name
    private GTreeGroup readTree(String errorPrefix, String name) {
        if (name == null)
            throw refusal(errorPrefix + "takes the name of a tree, as a string");
        GTreeGroup tree = form.getTreeGroup(name);
        if (tree == null)
            throw refusal(errorPrefix + "unknown tree '" + name + "'");
        return tree;
    }
    // ... a TABLE - what sortings, user filters and a toolbar are of -: a list group's grid by the group's SID, a tree
    // by its name. A group of a tree has the tree's: the tree is named, as `data` names it
    private GGridProperty readTable(String errorPrefix, String name) {
        if (name == null)
            throw refusal(errorPrefix + "takes the name of an object group or of a tree, as a string");
        GGroupObject group = form.getGroupObject(name);
        GTreeGroup tree = form.getTreeGroup(name);
        if (group != null && tree != null)
            throw refusal(errorPrefix + "'" + name + "' names an object group and a tree, so it does not say which");
        if (tree != null)
            return tree;
        if (group == null)
            throw refusal(errorPrefix + "unknown object group or tree '" + name + "'");
        if (group.isInTree())
            throw refusal(errorPrefix + "'" + name + "' is a group of tree '" + group.parent.name + "', whose sortings,"
                    + " user filters and toolbar are the tree's: name the tree");
        if (!group.viewType.isList())
            throw refusal(errorPrefix + "'" + name + "' has no table: it is not in LIST view");
        return group.grid;
    }
    // ... a list group's, whose toolbar has the count, the sum, the manual update and the view switch: a table that is
    // no tree
    private GGroupObject readGridGroup(String errorPrefix, String name) {
        return readTable(errorPrefix, name).match(grid -> grid.groupObject, tree -> {
            throw refusal(errorPrefix + "takes the name of a list group, whose toolbar this is: '" + name + "' is a tree");
        });
    }
    // ... a FILTER GROUP, by its name: one with filters - one with none has no view, and nothing to select
    private GRegularFilterGroup readFilterGroup(String errorPrefix, String name) {
        if (name == null)
            throw refusal(errorPrefix + "takes the name of a filter group, as a string");
        GRegularFilterGroup filterGroup = form.getRegularFilterGroup(name);
        if (filterGroup == null)
            throw refusal(errorPrefix + "unknown filter group '" + name + "'");
        if (filterGroup.filters.isEmpty())
            throw refusal(errorPrefix + "filter group '" + name + "' has no filters");
        return filterGroup;
    }
    // ... a property's FILTER, by the property's name as a <Platform> names it, its group's and its own: the group's one
    // property of that name (readGroupProperty), whose FILTER its table's user filters keep - none where the property
    // has none, a condition on it being the table's (changeFilters), or where its group has no table
    private GFilter readFilter(String errorPrefix, String name) {
        if (name == null)
            throw refusal(errorPrefix + "takes the name of a property, its group's and its own, as a string");
        GPropertyDraw property = readGroupProperty(errorPrefix, form.groupObjects, name, "");
        GGroupObject group = property.groupObject;
        if (!group.viewType.isList())
            throw refusal(errorPrefix + "'" + name + "' has no FILTER to change: its group is not in LIST view, so it has"
                    + " no user filters");
        GGridProperty table = group.getDrawComponent();
        GUserFiltersController userFilters = getUserFilters(table);
        for (GFilter filter : table.filters)
            if (filter.property == property && userFilters.getSlotEntry(filter) != null) // one the design removes is none
                return filter;
        throw refusal(errorPrefix + "'" + name + "' has no FILTER: a condition on it is stated as the table's, through changeFilters");
    }
    // ... and a ROW of a group: the row, or its `objects` handle - what a view has of rows it does not draw, a lookup's
    // objects among them (getValues) -, not a key: a key names a row only in the view that draws the rows, in its index
    // of them, and the view that draws them has their member
    private static GGroupObjectValue readFormRow(String errorPrefix, GGroupObject group, JavaScriptObject objectOrKey) {
        GGroupObjectValue key = GGroupObjectValue.resolveObject(objectOrKey);
        if (key == null)
            throw refusal(errorPrefix + "expects a row of '" + group.getSID() + "' or its objects handle; a key names a"
                    + " row only in the view that draws the rows, through its member");
        return readGroupRow(errorPrefix, group, key);
    }
    // ... an EDGE of a grid's rows, which a change of its current object names in a row's place - {to: 'home'} and
    // {to: 'end'}, HOME and END on the form's controller -: the first and the last of the rows the filter leaves,
    // whichever of them the view has. An object that is no row and no handle of one is an edge by its `to` ...
    private static boolean isEdge(JavaScriptObject target) {
        return GGroupObjectValue.resolveObject(target) == null && isJSObject(target) && hasOwnField(target, "to");
    }
    // ... which says whether it is the last one
    private static boolean readToEnd(String errorPrefix, JavaScriptObject edge) {
        checkAuthorObject(errorPrefix, edge, "an edge of the rows", "to");
        String to = getOwnString(edge, "to");
        if (!"home".equals(to) && !"end".equals(to))
            throw refusal(errorPrefix + "'to' must be 'home' or 'end'");
        return "end".equals(to);
    }
    private static final JavaScriptObject HOME = makeEdge("home"), END = makeEdge("end");
    private static native JavaScriptObject makeEdge(String to) /*-{
        return Object.freeze({to: to});
    }-*/;
    // ... and what such a change, or a change of its current column, makes of the range selected - the rows', which
    // the server keeps, the columns', which the grid keeps (GridRowsPart) -, options.selection: 'start', none, a range
    // starting from there (MOVE) - what a user's move in the platform's table makes of it, so what it is when not
    // given -; 'extend', the range up to there (MOVEEND); 'keep', the range stays as it is (null)
    private static GChangeSelection readSelection(String errorPrefix, JavaScriptObject options) {
        if (GwtClientUtils.isUndefinedOrNull(options))
            return GChangeSelection.MOVE;
        checkAuthorObject(errorPrefix, options, "the options argument", "selection");
        if (!hasOwnField(options, "selection"))
            return GChangeSelection.MOVE;
        String selection = getOwnString(options, "selection");
        if ("keep".equals(selection))
            return null;
        if (!"start".equals(selection) && !"extend".equals(selection))
            throw refusal(errorPrefix + "'selection' must be 'start', 'extend' or 'keep'");
        return "start".equals(selection) ? GChangeSelection.MOVE : GChangeSelection.MOVEEND;
    }
    // ... which options given where there is no selection change, unless they keep it
    private static boolean isSelectionChange(String errorPrefix, JavaScriptObject options) {
        return !GwtClientUtils.isUndefinedOrNull(options) && readSelection(errorPrefix, options) != null;
    }

    // ===== THE MEMBERS. A member IS the address: it holds the state it stands for - a property member its property's
    // value, a part's member its part - so a call through it names nothing it changes, and its
    // arguments come in a fixed order, the value first, then the row. FormView refused a name that would take what a
    // controller carries beside its members (`form`, `batch`), a group's own member or `__proto__` when it built the
    // form (claimProjectionName), so none of them is taken here.

    // every function on a controller - a member's, the controller's own, the form's controller's - is a member's call,
    // a change of what the projection holds (change): made so once the controller is filled, the members being design
    // data. Objects are members, or the states' members on them, filled in place. What a view can
    // call is only this controller (ReactRoot's context writes nothing here): a way of writing from a view's handler
    // that is not on it - a function added later, a bridge of its own - would be published outside a change, after the
    // handler, which a controlled input bound to what it wrote does not survive
    private static native void makeCalls(GReactFormData projection, JavaScriptObject object) /*-{
        Object.keys(object).forEach(function (name) {
            var value = object[name];
            if (typeof value === 'function')
                object[name] = function () {
                    projection.@lsfusion.gwt.client.form.design.view.GReactFormData::call()();
                    try {
                        return value.apply(this, arguments);
                    } finally {
                        projection.@lsfusion.gwt.client.form.design.view.GReactFormData::changed()();
                    }
                };
            else if (value !== null && typeof value === 'object')
                @lsfusion.gwt.client.form.design.view.GReactFormData::makeCalls(*)(projection, value);
        });
    }-*/;

    // a controller, with what no single member can own: the batch its members' changes are sent together in, as many as
    // the function makes, of whatever groups and properties - no change of its own, each named by the member that makes
    // it
    private static native JavaScriptObject makeController(Verbs verbs) /*-{
        return {
            batch: function (fn) {
                return @lsfusion.gwt.client.form.design.view.GReactFormData::batch(*)(verbs, "batch()", fn);
            }
        };
    }-*/;

    // the verb of a group whose rows are drawn here, on its member, named the way data.<group> names it: its current
    // object is chosen among its rows, so .change(row) is there, and only there
    private static native void addRowsVerbs(RowsPart part, JavaScriptObject member, String sid) /*-{
        member.change = function (row, options) {
            return part.@lsfusion.gwt.client.form.design.view.GReactFormData.RowsPart::change(*)(sid + ".change()", row, options);
        };
    }-*/;

    // ... and on a GRID's group, what selects its columns and all of it, and its clipboard: properties.change(column[,
    // options]) makes a column current, selectAll() selects every row and column, copy() and paste(text) copy the
    // cells selected and paste into them
    private static native void addGridVerbs(GridRowsPart part, JavaScriptObject member, String sid) /*-{
        member.properties = {change: function (column, options) {
            return part.@lsfusion.gwt.client.form.design.view.GReactFormData.GridRowsPart::changeColumn(*)(sid + ".properties.change()", typeof column === 'string' ? column : null, options);
        }};
        member.selectAll = function () {
            return part.@lsfusion.gwt.client.form.design.view.GReactFormData.GridRowsPart::selectAll()();
        };
        member.copy = function () {
            return part.@lsfusion.gwt.client.form.design.view.GReactFormData.GridRowsPart::copy(*)(sid + ".copy()");
        };
        member.paste = function (text) {
            return part.@lsfusion.gwt.client.form.design.view.GReactFormData.GridRowsPart::paste(*)(sid + ".paste()", typeof text === 'string' ? text : null);
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
    // part opens it (GTreeController.getOpenedByToolbarMembers) -, and the tree from the current node's group down, by
    // the platform's tree alone
    private static native void addExpandCurrentMember(GReactFormData projection, List<GGroupObject> groups, GTreeController rows, JavaScriptObject member) /*-{
        member.expandCurrent = function () {
            return projection.@lsfusion.gwt.client.form.design.view.GReactFormData::expandCurrentOf(*)(groups, rows);
        };
    }-*/;
    private static native void addExpandTreeMember(GReactFormData projection, List<GGroupObject> groups, GTreeController rows, JavaScriptObject member) /*-{
        member.expandTree = function () {
            return projection.@lsfusion.gwt.client.form.design.view.GReactFormData::expandTreeOf(*)(groups, rows);
        };
    }-*/;

    // a LIST STATE's member - the table's sortings, what the group or the tree is FILTERED by, where its FILTERS is -:
    // an array states the items all at once, its order being the priority of sortings, anything else states one of
    // them and leaves the rest - the shape the state has in `data` (TableList.change). `surface` is the call as a
    // message names it
    private static native JavaScriptObject makeListMember(TableList list, GGridProperty table, String surface) /*-{
        return {
            change: function (items) {
                return list.@lsfusion.gwt.client.form.design.view.GReactFormData.TableList::change(*)(surface, table, items);
            }
        };
    }-*/;

    // a grid's group's VIEW FILTERS, on its member where its rows are drawn: change(conditions) states them all
    // (changeViewConditions)
    private static native JavaScriptObject makeViewFiltersMember(GReactFormData projection, GGroupObject group, String surface) /*-{
        return {
            change: function (conditions) {
                return projection.@lsfusion.gwt.client.form.design.view.GReactFormData::changeViewConditions(*)(surface, group, conditions);
            }
        };
    }-*/;

    // a FILTER GROUP, on its group's member: change(index) selects one of its filters - null: none (selectFilter)
    private static native JavaScriptObject makeFilterGroupMember(GReactFormData projection, GRegularFilterGroup filterGroup, String qualified) /*-{
        return {
            change: function (index) {
                return projection.@lsfusion.gwt.client.form.design.view.GReactFormData::selectFilter(*)(qualified + ".change()", filterGroup, index === undefined ? null : index);
            }
        };
    }-*/;

    // a property's FILTER, under its group's `filter`: its value changed, and its compare and negation with it
    private static native JavaScriptObject makeFilterMember(FilterPart part, String qualified) /*-{
        return {
            change: function (value, options) {
                return part.@lsfusion.gwt.client.form.design.view.GReactFormData.FilterPart::change(*)(qualified + ".change()", value, options);
            }
        };
    }-*/;

    // the FILTER CONTROLS of a group or of a tree, on the table's member: the add, the apply and the reset, as the
    // platform's buttons
    private static native void addFilterControlsMembers(FilterControlsPart part, JavaScriptObject member, String sid) /*-{
        member.addFilter = function () {
            return part.@lsfusion.gwt.client.form.design.view.GReactFormData.FilterControlsPart::addFilter(*)(sid + ".addFilter()");
        };
        member.applyFilters = function () {
            return part.@lsfusion.gwt.client.form.design.view.GReactFormData.FilterControlsPart::applyFilters(*)(sid + ".applyFilters()");
        };
        member.resetFilters = function () {
            return part.@lsfusion.gwt.client.form.design.view.GReactFormData.FilterControlsPart::resetFilters(*)(sid + ".resetFilters()");
        };
    }-*/;

    // a TOOLBAR's, of either table, on the table's member: the filter button's state's member and the export - of the
    // rows, or of the tree (runTableReport) -, whatever the design's flags say, which are the platform's. `export` is
    // a word the language reserves, so it is set by a string, and named otherwise here
    private static native void addToolbarMembers(GReactFormData projection, ToolbarPart<?, ?> part, GGridProperty table, JavaScriptObject member, String sid) /*-{
        member.filterControlsVisible = {
            change: function (shown) {
                return part.@lsfusion.gwt.client.form.design.view.GReactFormData.ToolbarPart::changeFilterControlsVisible(*)(sid + ".filterControlsVisible.change()", shown);
            }
        };
        member['export'] = function () {
            return projection.@lsfusion.gwt.client.form.design.view.GReactFormData::runTableReport(*)(sid + ".export()", table);
        };
    }-*/;
    // ... and a group's own, on the group's member: the count and a column's sum, each the Promise of the server's
    // answer, as a request is (getValues), the update the manual update holds back, and the members of the states it
    // writes on the group's node, as `filters`' is - all of them, whatever the design's flags say; each the call the
    // form's controller makes for the group (WHAT A CALL DOES)
    private static native void addGridToolbarMembers(GReactFormData projection, GGroupObject group, JavaScriptObject member, String sid) /*-{
        member.getCount = function () {
            return projection.@lsfusion.gwt.client.form.design.view.GReactFormData::countRecords(*)(sid + ".getCount()", group);
        };
        member.getSum = function (property) {
            return projection.@lsfusion.gwt.client.form.design.view.GReactFormData::calculateSum(*)(sid + ".getSum()", group, typeof property === 'string' ? property : null);
        };
        member.manual = {
            change: function (value) {
                return projection.@lsfusion.gwt.client.form.design.view.GReactFormData::changeManualUpdate(*)(sid + ".manual.change()", group, value);
            }
        };
        member.update = function () {
            return projection.@lsfusion.gwt.client.form.design.view.GReactFormData::forceUpdate(*)(group);
        };
        member.listView = {
            change: function (name) {
                return projection.@lsfusion.gwt.client.form.design.view.GReactFormData::switchListView(*)(sid + ".listView.change()", group, name);
            }
        };
    }-*/;

    // CONTROLLER.FORM's methods, on it: each takes the names of what it reaches first, as strings - anything else is
    // no name, and is refused (null) -, then what the member it stands for takes; each is given the call as a message
    // names it (surface), quoting the first name. `export` is set by a string, as a toolbar's member is
    private static native void addFormMethods(GReactFormData projection, JavaScriptObject controller) /*-{
        var name = function (value) {
            return typeof value === 'string' ? value : null;
        };
        var surface = function (method, value) {
            return "form." + method + "(" + (typeof value === 'string' ? "'" + value + "'" : "") + ")";
        };
        controller.HOME = @lsfusion.gwt.client.form.design.view.GReactFormData::HOME;
        controller.END = @lsfusion.gwt.client.form.design.view.GReactFormData::END;
        controller.changeObject = function (group, row, options) {
            return projection.@lsfusion.gwt.client.form.design.view.GReactFormData::changeObject(*)(surface('changeObject', group), name(group), row, options);
        };
        controller.changeColumn = function (group, column, options) {
            return projection.@lsfusion.gwt.client.form.design.view.GReactFormData::changeColumn(*)(surface('changeColumn', group), name(group), typeof column === 'string' ? column : null, options);
        };
        controller.selectAll = function (group) {
            return projection.@lsfusion.gwt.client.form.design.view.GReactFormData::selectAll(*)(surface('selectAll', group), name(group));
        };
        controller.copy = function (group) {
            return projection.@lsfusion.gwt.client.form.design.view.GReactFormData::copy(*)(surface('copy', group), name(group));
        };
        controller.paste = function (group, text) {
            return projection.@lsfusion.gwt.client.form.design.view.GReactFormData::paste(*)(surface('paste', group), name(group), typeof text === 'string' ? text : null);
        };
        controller.expand = function (group, row) {
            return projection.@lsfusion.gwt.client.form.design.view.GReactFormData::expand(*)(surface('expand', group), name(group), row);
        };
        controller.collapse = function (group, row) {
            return projection.@lsfusion.gwt.client.form.design.view.GReactFormData::collapse(*)(surface('collapse', group), name(group), row);
        };
        controller.expandAll = function (group) {
            return projection.@lsfusion.gwt.client.form.design.view.GReactFormData::expandAll(*)(surface('expandAll', group), name(group));
        };
        controller.collapseAll = function (group) {
            return projection.@lsfusion.gwt.client.form.design.view.GReactFormData::collapseAll(*)(surface('collapseAll', group), name(group));
        };
        controller.expandCurrent = function (tree) {
            return projection.@lsfusion.gwt.client.form.design.view.GReactFormData::expandCurrent(*)(surface('expandCurrent', tree), name(tree));
        };
        controller.expandTree = function (tree) {
            return projection.@lsfusion.gwt.client.form.design.view.GReactFormData::expandTree(*)(surface('expandTree', tree), name(tree));
        };
        controller.changeOrders = function (table, orders) {
            return projection.@lsfusion.gwt.client.form.design.view.GReactFormData::changeOrders(*)(surface('changeOrders', table), name(table), orders);
        };
        controller.changeFilterGroup = function (filterGroup, index) {
            return projection.@lsfusion.gwt.client.form.design.view.GReactFormData::changeFilterGroup(*)(surface('changeFilterGroup', filterGroup), name(filterGroup), index === undefined ? null : index);
        };
        controller.changeFilters = function (table, conditions) {
            return projection.@lsfusion.gwt.client.form.design.view.GReactFormData::changeFilters(*)(surface('changeFilters', table), name(table), conditions);
        };
        controller.changeFilter = function (property, value, options) {
            return projection.@lsfusion.gwt.client.form.design.view.GReactFormData::changeFilter(*)(surface('changeFilter', property), name(property), value, options);
        };
        controller.addFilter = function (table) {
            return projection.@lsfusion.gwt.client.form.design.view.GReactFormData::addFilter(*)(surface('addFilter', table), name(table));
        };
        controller.applyFilters = function (table) {
            return projection.@lsfusion.gwt.client.form.design.view.GReactFormData::applyFilters(*)(surface('applyFilters', table), name(table));
        };
        controller.resetFilters = function (table) {
            return projection.@lsfusion.gwt.client.form.design.view.GReactFormData::resetFilters(*)(surface('resetFilters', table), name(table));
        };
        controller.changeFilterControlsVisible = function (table, shown) {
            return projection.@lsfusion.gwt.client.form.design.view.GReactFormData::changeFilterControlsVisible(*)(surface('changeFilterControlsVisible', table), name(table), shown);
        };
        controller.changeViewFilters = function (group, conditions) {
            return projection.@lsfusion.gwt.client.form.design.view.GReactFormData::changeViewFilters(*)(surface('changeViewFilters', group), name(group), conditions);
        };
        controller.getCount = function (group) {
            return projection.@lsfusion.gwt.client.form.design.view.GReactFormData::getCount(*)(surface('getCount', group), name(group));
        };
        controller.getSum = function (group, property) {
            return projection.@lsfusion.gwt.client.form.design.view.GReactFormData::getSum(*)(surface('getSum', group), name(group), name(property));
        };
        controller.changeManual = function (group, manual) {
            return projection.@lsfusion.gwt.client.form.design.view.GReactFormData::changeManual(*)(surface('changeManual', group), name(group), manual);
        };
        controller.update = function (group) {
            return projection.@lsfusion.gwt.client.form.design.view.GReactFormData::update(*)(surface('update', group), name(group));
        };
        controller.changeListView = function (group, view) {
            return projection.@lsfusion.gwt.client.form.design.view.GReactFormData::changeListView(*)(surface('changeListView', group), name(group), view);
        };
        controller['export'] = function (table) {
            return projection.@lsfusion.gwt.client.form.design.view.GReactFormData::exportTable(*)(surface('export', table), name(table));
        };
        controller.getValues = function (property, value, options) {
            return projection.@lsfusion.gwt.client.form.design.view.GReactFormData::getValues(*)(surface('getValues', property), name(property), typeof value === 'string' ? value : null, options);
        };
    }-*/;

    // A PROPERTY'S MEMBER - change, exec, getValues -, as makePropertyMember puts it on a controller: of a property a
    // view draws, its entry (ReactPropertyEntry), whose call names a row of the view's or none; of a property the
    // platform draws with a React component of its own, the cell the component stands in (ReactCellRenderer), whose
    // call names nothing. Each says what differs - whether the form shows the property, the cell a call names, that
    // cell's value -, and what a call does with it is here, once
    interface PropertyMember {
        GPropertyDraw getProperty();
        // what makes its changes and its lookups: the form
        Verbs getVerbs();
        // a member answers while the form shows its property: it stays on the controller when a SHOWIF hides it - the
        // names are design data, like the entries - and a call through it is refused until its entry is no longer
        // `hidden`
        boolean isShown();
        // the key of the cell a call names: EMPTY, the current objects, where it names no row
        GGroupObjectValue getMemberKey(String errorPrefix, JavaScriptObject objectOrKey);
        // ... and the value of that cell, a change not answered yet included: what a pick is made against
        PValue getCellValue(String errorPrefix, GGroupObjectValue key);

        // change(value[, row]): the value is given - null clears it - since running the property's change event without
        // one is exec(), and an action has no value to set
        default JavaScriptObject change(String surface, JavaScriptObject value, JavaScriptObject objectOrKey, boolean noValue) {
            String errorPrefix = controllerPrefix(surface);
            GPropertyDraw property = getProperty();
            if (property.isAction())
                throw refusal(errorPrefix + "'" + property.integrationSID + "' is an action, which has no value to set: call exec()");
            if (noValue)
                throw refusal(errorPrefix + "the value is missing - pass null to clear it, or call exec() to run the property's change event");
            return changeValue(errorPrefix, objectOrKey, value);
        }
        // ... exec([row]): the property's change event, or the action, with no value given - but a value the platform
        // asks for before it runs the event - its change type; the group change it makes of the change asks it first
        // too - is asked in the cell the change is made in, and a call has none: the view gives a property's value
        // itself, and an action's no call can give. One the event asks for only as it runs finds no cell either, and is
        // cancelled (GFormActionDispatcher)
        default JavaScriptObject exec(String surface, JavaScriptObject objectOrKey) {
            String errorPrefix = controllerPrefix(surface);
            GPropertyDraw property = getProperty();
            if (asksValue())
                throw refusal(errorPrefix + "'" + property.integrationSID + "' asks for a value" + (property.isAction()
                        ? ", which no call can give an action" : ", which a view gives itself: call change(value)"));
            return changeValue(errorPrefix, objectOrKey, UNDEFINED);
        }
        // whether its change asks a value before it runs - its change type -, in the cell the change is made in, which
        // neither a call nor a binding of a view has: exec() is refused, and no binding is taken (getBindingWidget) -
        // the platform would open its editor there, and the editor is the view's
        default boolean asksValue() {
            return getProperty().getChangeType() != null;
        }
        // ... the cell a call names changed to the value it gives - of a selection, by the options it picks
        // (ReactSelect.change); with none given (UNDEFINED, exec()), its change event run as it is -, the cell showing
        // until the answer what the change makes of it: one change sent as it is, several in one batch - the Promise of
        // the request returned
        default JavaScriptObject changeValue(String errorPrefix, JavaScriptObject objectOrKey, JavaScriptObject value) {
            checkShown(errorPrefix);
            GPropertyDraw property = getProperty();
            Verbs verbs = getVerbs();
            GGroupObjectValue key = getMemberKey(errorPrefix, objectOrKey);
            ArrayList<PValue> changes = new ArrayList<>();
            PValue shown;
            if (property.select != null && !isUndefined(value))
                shown = ReactSelect.change(errorPrefix, property, getCellValue(errorPrefix, key), value, changes);
            else {
                PValue change = JSValueConverter.convertFromJSUndefValue(property, value);
                changes.add(change);
                shown = property.getExternalChangeRenderValue(change, RendererType.SIMPLE);
            }
            if (changes.size() == 1)
                return verbs.changeProperty(property, key, changes.get(0), shown);
            return verbs.batch(() -> {
                for (PValue change : changes)
                    verbs.changeProperty(property, key, change, shown);
            });
        }
        // ... getValues(value[, {row, mode, count}]): the Promise of the suggestion list of the cell the call names
        // (lookupValues), its row read as a member names one, by a member the form shows
        default JavaScriptObject getValues(String surface, String value, JavaScriptObject options) {
            return lookupValues(getVerbs(), surface, getProperty(), value, options, row -> {
                String errorPrefix = controllerPrefix(surface);
                checkShown(errorPrefix);
                return getMemberKey(errorPrefix, row);
            });
        }
        default void checkShown(String errorPrefix) {
            if (!isShown())
                throw refusal(errorPrefix + "'" + getProperty().integrationSID + "' is not shown by the form now"
                        + " (its entry is hidden), so it has nobody to change it for");
        }
    }

    // ... and the member itself: `prefix` is how a message names it - the path of a view's member, o.note. - none for a
    // cell's, which its component is handed whole
    static native JavaScriptObject makePropertyMember(PropertyMember propertyMember, String prefix) /*-{
        var member = {
            //   .change(value)          set it on the current object       .change(value, row)   ... on that row
            change: function (value, object) {
                return propertyMember.@lsfusion.gwt.client.form.design.view.GReactFormData.PropertyMember::change(*)(prefix + "change()", value === undefined ? null : value, object === undefined ? null : object, value === undefined);
            },
            //   .exec()                 run its change event on the current object       .exec(row)   ... on that row
            exec: function (object) {
                return propertyMember.@lsfusion.gwt.client.form.design.view.GReactFormData.PropertyMember::exec(*)(prefix + "exec()", object === undefined ? null : object);
            },
            //   .getValues(value)       the Promise of its suggestions for the current object
            //   .getValues(value, {row, mode, count})   ... for that row, from that lookup, that many more
            getValues: function (value, options) {
                return propertyMember.@lsfusion.gwt.client.form.design.view.GReactFormData.PropertyMember::getValues(*)(prefix + "getValues()", typeof value === 'string' ? value : null, options);
            }
        };
        return member;
    }-*/;

    // a property's value, as a view is given it and as a view's call changes it back (PropertyMember.changeValue): as
    // its type converts it - a SELECTION's, whose cell holds the list of its options, as ReactSelect gives and changes it
    static void emitPropertyValue(JavaScriptObject target, GPropertyDraw property, PValue value) {
        if (property.select != null)
            ReactSelect.emit(target, property, value);
        else
            setField(target, "value", JSValueConverter.convertToJSValue(property, value, RendererType.SIMPLE, true));
    }

    static void emitPropertyFacts(JavaScriptObject entry, GPropertyDraw property) {
        // the kind of its value - of a selection a text: the views of its options, which the platform's element shows as
        // text too (ReactSelect)
        setField(entry, "type", property.select != null ? "string" : JSValueConverter.getJSTypeName(property.getRenderType(RendererType.SIMPLE)));
        // whether a change of it - for an action, running it - asks the user first (CONFIRM, the design's askConfirm),
        // and with what: the platform's own editor and button ask, a view's call does not, so a view asks itself
        setField(entry, "askConfirm", property.askConfirm);
        if (property.askConfirm)
            setField(entry, "askConfirmMessage", property.askConfirmMessage);
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
    private JavaScriptObject getFilterProperties(GGridProperty table) {
        JavaScriptObject properties = emptyArray();
        for (GGroupObject group : table.getGroups())
            for (GPropertyDraw property : form.propertyDraws)
                if (property.groupObject == group && isProjectableCondition(property)
                        && !(property.getFilterBaseType().getDataType() instanceof GFileType)) {
                    JavaScriptObject entry = newObject();
                    setField(entry, "property", getListName(table, property));
                    setFieldOrNull(entry, "caption", property.caption);
                    setField(entry, "type", JSValueConverter.getJSTypeName(property.getFilterBaseType()));
                    JavaScriptObject compares = emptyArray();
                    for (GCompare compare : property.getFilterCompares()) // the editor's list as it is: the default is
                        push(compares, compare.toString());               // not added to it
                    setField(entry, "compares", compares);
                    setField(entry, "defaultCompare", property.getDefaultCompare().toString());
                    push(properties, entry);
                }
        return properties;
    }

    // the condition a cell starts where a view draws the conditions (ReactFilterInputController.addCondition): the
    // cell's value - an empty cell's NULL too - given its property as a value is given a property's FILTER, from what
    // a value on its column starts from (first: GUserFiltersController.getValueBase, GPropertyFilter.getBase), its
    // separator escaped by the comparison it gets, as the panel's row escapes it. None where the table is on no row -
    // an empty grid, or a pivot, a map, a calendar or a custom view, which have no cell, though they name a column the
    // panel would start an editor on -, on a cell a view cannot name in a condition, or on one whose value is not the
    // one it filters by (differentValue)
    static GPropertyFilter getCellCondition(GTableController rows, Function<Column, GPropertyFilter> first) {
        GPropertyDraw property = rows.getSelectedFilterProperty();
        if (property == null || rows.getSelectedRow() < 0)
            return null;
        if (!isProjectableCondition(property) || property.differentValue)
            return null;
        Column column = getConditionColumn(property); // the one the table is on: a cell a view names is no column's
        GPropertyFilter base = GPropertyFilter.getBase(column, first.apply(column));
        return base.restated(PValue.escapeSeparator(rows.getSelectedValue(property, column.columnKey), base.compare),
                base.negation, base.compare, base.junction);
    }
    // the column of a condition a view names: the property's own, the empty key - a property grouped in columns is on
    // none (isProjectableCondition)
    static Column getConditionColumn(GPropertyDraw property) {
        return new Column(property, GGroupObjectValue.EMPTY);
    }

    // can a condition be on this property - and so be listed? Three rules, each said where it belongs: the projection
    // addresses a property by its integration SID; a filter is on something that can be filtered by
    // (GPropertyDraw.canBeFiltered, which GFormController.getFilterDTOs asks too); and a condition names one property,
    // not one of its COLUMNS - a property grouped in columns is filtered per column, and no name here says which. Its
    // rows may be the platform's, so the form being built says nothing about it. A condition failing it is not listed
    // (FiltersPart.writeFilters) - and a view naming one is refused (TableList.readProperty), by the same rule;
    // FiltersTableList.whyNot says which it fails
    private static boolean isProjectableCondition(GPropertyDraw property) {
        return property.integrationSID != null && property.canBeFiltered() && !property.hasColumnGroupObjects();
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
        checkAuthorObject(errorPrefix, item, what, fields);
        String propertySID = getOwnString(item, "property");
        if (propertySID == null)
            throw refusal(errorPrefix + (hasOwnField(item, "property")
                    ? "'property' must be the property's name, as a string" : what + " has no 'property'"));
        return propertySID;
    }
    // ... the first two of them, which the options of a property filter's change get too: an object, with no field it
    // does not know
    private static void checkAuthorObject(String errorPrefix, JavaScriptObject item, String what, String fields) {
        String fieldList = fields.replace(",", ", ");
        if (!isJSObject(item))
            throw refusal(errorPrefix + what + " must be an object {" + fieldList + "}");
        String unknown = getUnknownField(item, fields);
        if (unknown != null)
            throw refusal(errorPrefix + "unknown field '" + unknown + "'; " + what + " has " + fieldList);
    }

    private static final String FILTER_CONDITION_FIELDS = "property,compare,value,negation,or";
    // ... and the options of a property filter's change, any of them, each read as a condition's field
    private static final String FILTER_OPTIONS = "compare,negation,or";

    // a property of a TABLE's groups as its lists name it - its sortings, its user filters' conditions and what they
    // can be on (TableList) -, and as a call names it back: a grid's group's by its integration name, as the group's
    // node carries it; a tree's by its group's name and its own, c.name, as a <Platform> names a property of a group
    // (getName) - each of the group of its property, in the tree's order of the groups, as the platform's panel shows
    // them and a list a view states is sent, each group's its own
    private static String getListName(GGridProperty table, GPropertyDraw property) {
        return table.match(grid -> property.integrationSID, tree -> getName(property));
    }
    // ... and the property such a name names: the group's one property of that name; a tree's group the one its name
    // starts with (readGroupProperty)
    private GPropertyDraw readListName(String errorPrefix, GGridProperty table, String name) {
        return table.match(grid -> readStateProperty(errorPrefix, grid.groupObject, name),
                tree -> readGroupProperty(errorPrefix, tree.groups, name, " of tree '" + tree.name + "'"));
    }
    // a property named by its group's name and its own, as getName gives it: of the groups given (of: whose they are,
    // as a message says it), the one whose name and a dot the name starts with, and the group's one property of the
    // rest (readStateProperty); refused where none does. A group's name has dots where no view projects the group, a
    // group of several objects being named by their names joined with dots (FormView.checkProjectedGroupSID), and an
    // integration name may too, so where two groups' names start it, as a group named as the first object of another
    // is, it does not say which
    private GPropertyDraw readGroupProperty(String errorPrefix, Iterable<GGroupObject> groups, String name, String of) {
        GGroupObject named = null;
        for (GGroupObject group : groups)
            if (name.startsWith(group.getSID() + ".")) {
                if (named != null)
                    throw refusal(errorPrefix + "'" + name + "' starts with the names of groups '" + named.getSID()
                            + "' and '" + group.getSID() + "'" + of + ", so it does not say which");
                named = group;
            }
        if (named == null)
            throw refusal(errorPrefix + "'" + name + "' names no property of a group" + of
                    + ": a property is named by its group's name and its own, <group>.<property>");
        return readStateProperty(errorPrefix, named, name.substring(named.getSID().length() + 1));
    }

    // the property a STATE names - a sorting, a condition, a FILTER, a column the toolbar sums -: the group's ONE
    // property of that name, wherever it is drawn - by this view, by another container, by the platform. A state is the
    // group's, and its part need not be where an entry of the property is - FILTERS(g) with a view of its own over the
    // platform's grid is where no node names a column, and the rows' node carries no panel property drawn elsewhere -,
    // so the name is the group's, not a node's. A form that projects a part of a group's state is not built with a name
    // the group carries on two properties (FormView.checkStateNames); the form's controller names any group's state,
    // so there a name the group carries twice is refused (readOneProperty)
    private GPropertyDraw readStateProperty(String errorPrefix, GGroupObject group, String propertySID) {
        GPropertyDraw property = readOneProperty(form, errorPrefix, group, propertySID);
        if (property == null)
            throw refusal(errorPrefix + "unknown property '" + propertySID + "' of object group '"
                    + group.getSID() + "'");
        return property;
    }
    // the ONE property of a name a group draws - the empty group's for null -, null for none: a name it draws twice
    // says not which of them is meant, and is refused - a state's name (readStateProperty), a classic name's
    // (readClassicProperty)
    private static GPropertyDraw readOneProperty(GForm form, String errorPrefix, GGroupObject group, String integrationSID) {
        GPropertyDraw property = form.getPropertyDraw(group, integrationSID);
        if (property != null && form.countPropertyDraws(group, integrationSID) != 1)
            throw refusal(errorPrefix + "'" + integrationSID + "' is drawn more than once on "
                    + (group != null ? "group '" + group.getSID() + "'" : "the form level")
                    + ", so this name does not say which of them is meant; give them explicit EXTIDs");
        return property;
    }
    // a property named as the CLASSIC changeProperty names one - the grid view's, which names the whole form (#1655),
    // the form's controller's -: "groupSID.integrationSID" for a property of an object group, a bare integration SID
    // for one of the empty group, as a view has it everywhere (getName) - so the name alone says the group, and neither
    // the row passed nor the rest of the form is asked. A qualified name splits at its LAST dot: a property's
    // integration SID never carries one, so everything before the last dot is the group - a group of several objects
    // is named by its SID, all of them joined with dots (`OBJECTS d = X, t = Y` is `d.t`). Every author mistake is
    // refused: an unknown group, a name drawn twice on its group (readOneProperty), a missing property - a bare name
    // an object group draws is told to take its group -, and a property grouped in COLUMNS: one cell per (row, column),
    // where a name here names a row, so a call would land on whatever column is current, a real cell nobody named
    // (nor is such a property projected)
    public static GPropertyDraw readClassicProperty(GForm form, String errorPrefix, String name) {
        if (name == null)
            throw refusal(errorPrefix + "takes the name of a property, as a string");
        int dot = name.lastIndexOf('.');
        GGroupObject group = dot > 0 ? readGroup(form, errorPrefix, name.substring(0, dot)) : null;
        String integrationSID = dot > 0 ? name.substring(dot + 1) : name;
        GPropertyDraw property = readOneProperty(form, errorPrefix, group, integrationSID);
        if (property == null) {
            if (group != null)
                throw refusal(errorPrefix + "property '" + integrationSID + "' is not drawn on group '" + group.getSID() + "'");
            GGroupObject drawnOn = form.getSingleDrawnGroup(name); // a bare name a group draws: say how to name it
            String groupSID = drawnOn != null ? drawnOn.getSID() : "<group>";
            throw refusal(errorPrefix + "'" + name + "' names no property of the form level; a property of an"
                    + " object group is named with its group, '" + groupSID + "." + name + "'");
        }
        if (property.hasColumnGroupObjects())
            throw refusal(errorPrefix + "'" + property.integrationSID + "' is grouped in columns - it is one cell"
                    + " per row AND column, and this names a row, so it is not addressed here (nor projected)");
        return property;
    }
    // ... and a row given with a name of the form level, which has no rows: a mistake, said with the way to name a row
    public static RuntimeException noRowsOfFormLevel(String errorPrefix, String name) {
        return refusal(errorPrefix + "'" + name + "' names a property of the form level, which has no rows: a property"
                + " of an object group is named with its group, '<group>." + name + "'");
    }

    // a condition a view states - an element of `filters.change`'s list, its one condition, a property FILTER's value
    // with its options -, from the one it starts from (GPropertyFilter.getBase): `compare`, `negation` and `or` as the
    // object states them, else as that one has them; the value, read once by the caller, by the property's FILTER type
    // as `filters` gives it - none, or one the type reads as NULL (a two-state LOGICAL's false), is NULL, which the
    // caller states (a list) or takes off (one value). The rest is read whatever the value: a mistake in a condition
    // that only takes one off is still a mistake. A file's is stated with no value alone: `filters` gives a link to the
    // file, not the file a condition compares with
    private GPropertyFilter readFilterCondition(String errorPrefix, JavaScriptObject condition, GPropertyFilter base,
                                                JavaScriptObject value) {
        GCompare compare = readFilterCompare(errorPrefix, condition, base.compare);
        boolean negation = readFlag(errorPrefix, condition, "negation", base.negation);
        boolean or = readFlag(errorPrefix, condition, "or", !base.junction);
        PValue stated = null;
        if (!isNoValue(value)) {
            GPropertyDraw property = base.column.property;
            checkFilterValue(errorPrefix, property);
            stated = JSValueConverter.convertFromJSValue(property.getFilterBaseType(), value);
        }
        return base.restated(stated, negation, compare, !or);
    }
    // ... a property a view states a condition with a value on - in `filters`, or through a property's FILTER
    // (FilterPart.change) -: not a file, whose condition can only be taken off or compare with NULL (above)
    private static void checkFilterValue(String errorPrefix, GPropertyDraw property) {
        if (property.getFilterBaseType().getDataType() instanceof GFileType)
            throw refusal(errorPrefix + "'" + property.integrationSID + "' is a file: a condition on it has"
                    + " a link to the file for its value, which is not the file, so it is stated with no value alone");
    }

    // no `compare` at all is how a view says "as the condition it starts from has it" (readFilterCondition), which is
    // what it is read as; one that is there but is not the operator string it is spelled with - a number, a boolean -
    // is a mistake, and reading it as absent would filter by something else without a word
    private GCompare readFilterCompare(String errorPrefix, JavaScriptObject condition, GCompare absent) {
        if (!hasOwnField(condition, "compare"))
            return absent;
        String compareSID = getOwnString(condition, "compare");
        if (compareSID == null)
            throw refusal(errorPrefix + "'compare' must be the operator as a string");
        GCompare compare = GCompare.get(compareSID);
        if (compare == null)
            throw refusal(errorPrefix + "unknown compare '" + compareSID + "'");
        if (compare == GCompare.INARRAY) // a condition compares with one value - FILTER refuses it the same way
            throw refusal(errorPrefix + "'" + compareSID + "' is no comparison of a filter condition");
        return compare;
    }

    // a flag is a boolean or absent - a truthy string like "false" would otherwise mean true: a condition's `negation`
    // and `or`, absent as the condition it starts from has them (readFilterCondition). A sorting's `desc` is not a
    // flag: an order with none takes its sorting off (OrdersPart.applyOrder)
    private boolean readFlag(String errorPrefix, JavaScriptObject entry, String field, boolean absent) {
        if (!hasOwnField(entry, field)) // anything PRESENT has to be the boolean it claims to be,
            return absent;                // `null` and `undefined` included - they are given, and they are not flags
        JavaScriptObject value = getOwnField(entry, field);
        if (!isJSBoolean(value))
            throw refusal(errorPrefix + "'" + field + "' must be true or false");
        return toBoolean(value);
    }

    // no value at all - absent, null, or the empty string an emptied input gives: how a view says a condition's value
    // is NULL (readFilterCondition) and "not sorted by this"
    private static native boolean isNoValue(JavaScriptObject v) /*-{ return v === undefined || v === null || v === ''; }-*/;

    private static void emitHidden(JavaScriptObject entry, boolean shown) {
        if (!shown) // an attribute like any other: there while it has a value
            setField(entry, "hidden", true);
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
