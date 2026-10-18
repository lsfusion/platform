package lsfusion.gwt.client.form.design.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.GForm;
import lsfusion.gwt.client.GFormChanges;
import lsfusion.gwt.client.base.FocusUtils;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.jsni.JsniTestSupport;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.jsni.NativeSIDMap;
import lsfusion.gwt.client.classes.GActionType;
import lsfusion.gwt.client.classes.data.GCSVType;
import lsfusion.gwt.client.classes.data.GColorType;
import lsfusion.gwt.client.classes.data.GCustomStaticFormatFileType;
import lsfusion.gwt.client.classes.data.GDateIntervalType;
import lsfusion.gwt.client.classes.data.GIntegerType;
import lsfusion.gwt.client.classes.data.GLogicalType;
import lsfusion.gwt.client.classes.data.GStringType;
import lsfusion.gwt.client.controller.dispatch.GwtActionDispatcher;
import lsfusion.gwt.client.form.GUpdateMode;
import lsfusion.gwt.client.form.controller.GController;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.event.GInputBindingEvent;
import lsfusion.gwt.client.form.event.GKeyInputEvent;
import lsfusion.gwt.client.form.event.GKeyStroke;
import lsfusion.gwt.client.form.filter.GFilterGroupController;
import lsfusion.gwt.client.form.filter.GRegularFilter;
import lsfusion.gwt.client.form.filter.GRegularFilterGroup;
import lsfusion.gwt.client.form.filter.user.GCompare;
import lsfusion.gwt.client.form.filter.user.GFilter;
import lsfusion.gwt.client.form.filter.user.GFilterControls;
import lsfusion.gwt.client.form.filter.user.GPropertyFilter;
import lsfusion.gwt.client.form.filter.user.GPropertyFilterDTO;
import lsfusion.gwt.client.form.filter.user.controller.GFilterInputController;
import lsfusion.gwt.client.form.filter.user.controller.GFilterControlsController;
import lsfusion.gwt.client.form.filter.user.controller.GFilterControlsToggle;
import lsfusion.gwt.client.form.filter.user.view.GFilterEditors;
import lsfusion.gwt.client.form.filter.user.view.GFiltersHandler;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.GObject;
import lsfusion.gwt.client.form.object.table.GToolbar;
import lsfusion.gwt.client.form.object.table.controller.GPlatformTableController;
import lsfusion.gwt.client.form.object.table.controller.GComponentController;
import lsfusion.gwt.client.form.object.panel.controller.GPlatformPanelGroupController;
import lsfusion.gwt.client.form.object.table.controller.GGroupController;
import lsfusion.gwt.client.form.object.table.controller.GGridToolbarController;
import lsfusion.gwt.client.form.object.table.controller.GListViewController;
import lsfusion.gwt.client.form.object.table.controller.GPlatformGridToolbarController;
import lsfusion.gwt.client.form.object.table.controller.GTableController;
import lsfusion.gwt.client.form.object.table.controller.GToolbarController;
import lsfusion.gwt.client.form.object.table.controller.GUserFiltersController;
import lsfusion.gwt.client.form.object.table.controller.GUserOrdersController;
import lsfusion.gwt.client.form.object.table.controller.GPropertyController;
import lsfusion.gwt.client.form.object.table.grid.GGrid;
import lsfusion.gwt.client.form.object.table.grid.GGridProperty;
import lsfusion.gwt.client.form.object.table.grid.controller.GGridController;
import lsfusion.gwt.client.form.object.table.grid.view.GListViewType;
import lsfusion.gwt.client.form.object.table.grid.user.design.GGroupObjectUserPreferences;
import lsfusion.gwt.client.form.object.table.tree.GTreeGroup;
import lsfusion.gwt.client.form.object.table.tree.controller.GTreeController;
import lsfusion.gwt.client.form.property.GCaptionReader;
import lsfusion.gwt.client.form.property.GClassViewType;
import lsfusion.gwt.client.form.property.GBackgroundReader;
import lsfusion.gwt.client.form.property.GCommentReader;
import lsfusion.gwt.client.form.property.GComponentLabelReader;
import lsfusion.gwt.client.form.property.GComponentReader;
import lsfusion.gwt.client.form.property.GCustomOptionsReader;
import lsfusion.gwt.client.form.property.GExtraLabelReader;
import lsfusion.gwt.client.form.property.GExtraPropReader;
import lsfusion.gwt.client.form.property.GExtraPropertyReader;
import lsfusion.gwt.client.form.property.GFooterReader;
import lsfusion.gwt.client.form.property.GForegroundReader;
import lsfusion.gwt.client.form.property.GGridElementClassReader;
import lsfusion.gwt.client.form.property.GGroupObjectPropertyReader;
import lsfusion.gwt.client.form.property.GImageReader;
import lsfusion.gwt.client.form.property.GLastReader;
import lsfusion.gwt.client.form.property.GLoadingReader;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.GPropertyReader;
import lsfusion.gwt.client.form.property.GReadOnlyReader;
import lsfusion.gwt.client.form.property.GRowBackgroundReader;
import lsfusion.gwt.client.form.property.GRowForegroundReader;
import lsfusion.gwt.client.form.property.GRowSelectReader;
import lsfusion.gwt.client.form.property.GShowIfReader;
import lsfusion.gwt.client.form.property.GValueElementClassReader;
import lsfusion.gwt.client.form.property.GExtInt;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.async.GAsyncInput;
import lsfusion.gwt.client.form.property.cell.GEditBindingMap;
import lsfusion.gwt.client.form.property.cell.classes.ColorDTO;
import lsfusion.gwt.client.form.view.Column;
import lsfusion.interop.form.design.ReactProjectionNames;
import lsfusion.gwt.client.form.object.table.view.GTableView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Runs in compiled JavaScript: reference identity and JS object keys are part of the contract. */
public class GReactFormDataTest extends GWTTestCase {
    @Override public String getModuleName() { return "lsfusion.gwt.main"; }

    @Override protected void gwtSetUp() { JsniTestSupport.installMapDelete(); }

    // the user filters of each group as the form has them - what a member handed it last, or what the server reported
    // -, and what shows them told, as the form tells it (GFormController.changeFilters, applyFilterChanges)
    private static final class FormFilters {
        final GForm form;
        final LinkedHashMap<GGroupObject, ArrayList<GPropertyFilterDTO>> lists = new LinkedHashMap<>();
        Controllers controllers; // what shows each group's user filters, once made

        FormFilters(GForm form) {
            this.form = form;
        }
        // a FILTERS box's list, split into its groups' lists, each the form's now - all of them written before any is
        // told
        void change(List<GGroupObject> groups, ArrayList<GPropertyFilter> conditions) {
            NativeSIDMap<GGroupObject, ArrayList<GPropertyFilter>> split = GPropertyFilter.getGroupConditions(groups, conditions);
            for (GGroupObject group : groups)
                lists.put(group, dtos(split.get(group)));
            for (GGroupObject group : groups)
                controllers.userFilters.get(group).changeFilters(group, lists.get(group));
        }
        ArrayList<GPropertyFilter> get(GGroupObject group) {
            return GPropertyFilter.getConditions(form, group, lists.containsKey(group) ? lists.get(group) : new ArrayList<>());
        }
        // ... and the server reporting a group's: other ones than the form has are the form's now, and what shows them
        // is told
        void report(GGroupObject group, ArrayList<GPropertyFilterDTO> filters) {
            if (!filters.equals(lists.get(group))) {
                lists.put(group, filters);
                controllers.userFilters.get(group).updateFilters(group, filters);
            }
        }
    }
    // the user orders the form keeps, each group's (GFormController.currentOrders): a table's list, as a member states
    // it, split into its groups' lists, each of the group of its property and the form's now where it is another one,
    // what shows it told (GFormController.changeOrders); and the server reporting a group's (report)
    private static final class FormOrders {
        final LinkedHashMap<GGroupObject, LinkedHashMap<Column, Boolean>> lists = new LinkedHashMap<>();
        Controllers controllers; // what shows each group's user orders, once made

        void change(List<GGroupObject> groups, LinkedHashMap<Column, Boolean> orders) {
            for (GGroupObject group : groups) {
                LinkedHashMap<Column, Boolean> groupOrders = new LinkedHashMap<>();
                for (Map.Entry<Column, Boolean> order : orders.entrySet())
                    if (order.getKey().property.groupObject == group)
                        groupOrders.put(order.getKey(), order.getValue());
                if (!equalOrders(groupOrders, get(group))) {
                    lists.put(group, groupOrders);
                    controllers.userOrders.get(group).changeOrders(group, groupOrders);
                }
            }
        }
        LinkedHashMap<Column, Boolean> get(GGroupObject group) {
            return lists.containsKey(group) ? lists.get(group) : new LinkedHashMap<>();
        }
        void report(GGroupObject group, LinkedHashMap<Column, Boolean> orders) {
            if (!equalOrders(orders, lists.get(group))) {
                lists.put(group, orders);
                controllers.userOrders.get(group).updateOrders(group, orders);
            }
        }
    }
    // a call that asks the server, refused while a batch runs, as the form refuses it (GFormController.getBatchRefusal)
    private static String batchRefusal(String surface) {
        return "controller." + surface + ": a batch is running, which carries changes only: make this call before the batch or after it";
    }

    private static class Fixture {
        final GForm form = new GForm();
        final GContainer a = container(1), b = container(2), c = container(3);
        final GGroupObject group = new GGroupObject();
        final GGroupObjectValue one = new GGroupObjectValue(10, 1);
        final GGroupObjectValue two = new GGroupObjectValue(10, 2);
        final GPropertyDraw price, quantity, panel, single, nativeProperty, unnamed, plain;
        final GContainer box = container(4);
        final GReactFormData projection;
        final Controllers controllers;
        final GReactFormData.ViewState sa, sb, sc;
        final JavaScriptObject[] snapshots = new JavaScriptObject[3];
        final int[] publications = new int[3];
        Runnable onPublish;
        // the platform's controller of a group and of a property, as far as the form hands it on
        final RecordingTableController platform = new RecordingTableController();
        // ... and of the components, recording into the platform's log
        final RecordingComponentController platformComponents = new RecordingComponentController(platform);
        int placements;
        GGroupObject outer; // a group GWT draws the rows of, with one panel property in b - only in Fixture.withOuter()
        GPropertyDraw outerPanel, outerName; // ... and a list property, which GWT draws with the rows
        GPropertyDraw note; // a panel property of the group in a, beside its rows - only in Fixture.withNote()
        GPropertyDraw remark; // a second panel property of the group in b - only in withRemark()
        // properties of the group the platform draws - one named `platform`, one with no name, one grouped in columns
        // named `columns` - only in withPlatformProperties()
        GPropertyDraw platformNamed, platformUnnamed, platformInColumns;
        LinkedHashMap<Column, Boolean> sorted; // the sortings a table's orders.change() handed the form
        List<GGroupObject> sortedGroups; // ... for which groups
        int sortedCalls; // ... and how many times
        // the form's sortings of each group, as a member reads them (Verbs.getOrders): what a member handed it last, or
        // what the server reported (reportOrders) - and its user filters (reportFilters)
        final FormOrders formOrders = new FormOrders();
        final FormFilters formFilters = new FormFilters(form);
        // FILTERS(g) as a plain box in a, beside the rows
        void filtersBesideRows() {
            GContainer filters = container(5); filters.react = false; filters.custom = null;
            filters.container = a; a.children.add(filters);
            group.grid.filtersContainer = filters;
        }
        // ... or FILTERS(g) { custom = ... } in c: the conditions drawn by a view of their own, the rows in a
        void filtersApart() {
            group.grid.filtersContainer = c;
        }
        GGroupObjectValue chosen, changed; // what a member handed the form: the row its group's change(row) chose, and
                                           // the cell a property member changed
        final ArrayList<String> edits = new ArrayList<>(); // ... and every edit it was handed, in order
        PValue changedTo; // ... and the value it changed that cell to
        final ArrayList<String> filtersCalls = new ArrayList<>(); // ... and the calls a filters member made, in order
        ArrayList<GPropertyFilter> stated; // ... and the last list of conditions it stated
        final ArrayList<String> toolbarCalls = new ArrayList<>(); // ... and what a toolbar member asked, in order
        final HashMap<GGroupObject, Boolean> manualUpdates = new HashMap<>(); // ... and each group's update mode it set
        java.util.function.Consumer<Number> answered; // ... the last answer it waits for
        GReactFormData.Verbs verbs; // ... all of it, the form as far as the projection asks it
        // ... and the widget of each React view it was asked for, what the keys of what the view draws are taken with -
        // one per view, made as it is first asked for (viewWidget)
        final LinkedHashMap<GComponent, Widget> viewWidgets = new LinkedHashMap<>();
        // the React views of components that are not containers, each made after the containers', as the layout makes
        // them (GFormLayout.addReactView) - only in a test that asks (ownView) -, and what each was last given
        final ArrayList<GComponent> ownViews = new ArrayList<>();
        final LinkedHashMap<GComponent, GReactFormData.ViewState> ownStates = new LinkedHashMap<>();
        final LinkedHashMap<GComponent, JavaScriptObject> ownSnapshots = new LinkedHashMap<>();
        // the group's filter input controller where the toolbar has its button - only in a test that asks
        PanelDouble filter;
        // ... and the platform's table whose views the toolbar switches - only in a test that asks
        ListViewDouble views;

        // the scene, and what a test adds to it before the projection is built: each addition is a method of its own
        Fixture() { this(f -> { }); }
        Fixture(java.util.function.Consumer<Fixture> addition) {
            form.mainContainer = container(0);
            form.mainContainer.react = false;
            form.mainContainer.main = true;
            for (GContainer scope : Arrays.asList(a, b, c)) {
                scope.container = form.mainContainer;
                form.mainContainer.children.add(scope);
            }
            group.ID = 10; group.nativeSID = "g10"; group.sID = "items"; group.viewType = GClassViewType.LIST;
            group.grid = new GGrid(); group.grid.ID = 11; group.grid.nativeSID = "c11"; group.grid.sID = "grid"; group.grid.container = a;
            group.grid.groupObject = group;
            a.children.add(group.grid);
            group.objects.add(new GObject(group, "item", 10, "item", GIntegerType.instance));
            group.rowSelectReader = new GRowSelectReader(10);
            form.groupObjects.add(group);
            price = property(20, "price", group, true, a);
            quantity = property(21, "quantity", group, true, a);
            panel = property(22, "panel", group, false, b);
            single = property(23, "single", null, false, a);
            nativeProperty = property(24, "native", null, false, b);
            nativeProperty.platformDrawn = true;
            nativeProperty.caption = "Native";
            nativeProperty.captionReader = new GCaptionReader(24, -1);
            unnamed = property(31, "unnamed", null, false, b);
            unnamed.integrationSID = null; // React draws it, and carries it by no name
            // the platform draws it: React has nothing of it
            plain = property(30, "plain", null, false, form.mainContainer);
            plain.captionReader = new GCaptionReader(30, -1);
            box.react = false; box.custom = null; box.caption = "Box"; // a container React draws in a
            box.container = a; a.children.add(box);
            price.captionReader = new GCaptionReader(20, 10);
            price.caption = "Price";
            addition.accept(this);
            projection = new GReactFormData(form, component -> {
                placements++;
                return component.getReactPlace();
            }, verbs = new GReactFormData.Verbs() { // the form, as far as a member asks it: what it is handed is kept
                public void changeCurrentObject(GGroupObject group, GGroupObjectValue key) { chosen = key; edits.add("current " + key.toKeyString()); }
                public void changeProperty(GPropertyDraw property, GGroupObjectValue key, PValue value) {
                    changed = key;
                    changedTo = value;
                    edits.add("cell " + property.integrationSID + "@" + key.toKeyString());
                }
                public void getPropertyValues(GPropertyDraw property, GGroupObjectValue key, String value, String actionSID, JavaScriptObject successCallback, JavaScriptObject failureCallback, int increaseValuesNeededCount) { }
                public void countRecords(GGroupObject group, java.util.function.Consumer<Number> answer, java.util.function.Consumer<Throwable> failed) {
                    toolbarCalls.add("count " + group.sID); answered = answer;
                }
                public void calculateSum(GGroupObject group, GPropertyDraw property, GGroupObjectValue columnKey, java.util.function.Consumer<Number> answer, java.util.function.Consumer<Throwable> failed) {
                    toolbarCalls.add("sum " + group.sID + " " + property.integrationSID); answered = answer;
                }
                public void runTableReport(GGridProperty table) {
                    toolbarCalls.add(table.match(grid -> "export " + grid.groupObject.ID, tree -> "export tree " + tree.groups.get(0).ID));
                }
                public void changeUpdateMode(GGroupObject group, GUpdateMode updateMode) {
                    toolbarCalls.add("updateMode " + group.sID + " " + updateMode);
                    if (updateMode != GUpdateMode.FORCE)
                        manualUpdates.put(group, updateMode == GUpdateMode.MANUAL);
                }
                // as the form keeps it: as the server starts the group, until a toolbar changes it
                public boolean isManualUpdate(GGroupObject group) {
                    Boolean manual = manualUpdates.get(group);
                    return manual != null ? manual : group.enableManualUpdate;
                }
                public void expandCurrent(GGroupObject group, GGroupObjectValue key) { }
                public void published() { }
                public void expandNode(GGroupObject group, GGroupObjectValue key) { }
                public void collapseNode(GGroupObject group, GGroupObjectValue key) { }
                public void expandAll(GGroupObject group) { }
                public void collapseAll(GGroupObject group) { }
                // ... and the sortings and the filters it is handed, the form's now, told to what shows them, as the
                // form does
                public void changeOrders(List<GGroupObject> groups, LinkedHashMap<Column, Boolean> orders) {
                    sortedGroups = groups; sortedCalls++; sorted = orders;
                    formOrders.change(groups, orders);
                }
                public LinkedHashMap<Column, Boolean> getOrders(GGroupObject group) {
                    return formOrders.get(group);
                }
                public long changeFilters(List<GGroupObject> groups, ArrayList<GPropertyFilter> conditions) {
                    filtersCalls.add("changeFilters " + sids(groups) + describe(conditions)); stated = conditions;
                    formFilters.change(groups, conditions);
                    return 0;
                }
                public ArrayList<GPropertyFilter> getFilters(GGroupObject group) {
                    return formFilters.get(group);
                }
                public void changeRegularFilter(GRegularFilterGroup filterGroup, GRegularFilter filter) {
                    edits.add("regularFilter " + filterGroup.sID + " " + (filter != null ? filter.caption : "none"));
                    controllers.filterGroups.get(filterGroup).showFilter(filter);
                }
                public Widget getViewWidget(GComponent scope) {
                    return viewWidget(scope);
                }
                // ... and a call that asks the server, refused while a batch runs
                public String getRefusal(String surface) {
                    return batching ? batchRefusal(surface) : null;
                }
                // ... and a batch: what it is handed while the batch runs comes between the batch and its sending - a
                // batch begun inside it a part of it, as the form's is
                boolean batching;
                public void batch(Runnable run) {
                    if (batching) {
                        run.run();
                        return;
                    }
                    edits.add("batch");
                    batching = true;
                    try {
                        run.run();
                    } finally {
                        batching = false;
                        edits.add("sent");
                    }
                }
            });
            sa = projection.addView(a, data -> publish(0, data));
            sb = projection.addView(b, data -> publish(1, data));
            sc = projection.addView(c, data -> publish(2, data));
            for (GComponent component : ownViews)
                ownStates.put(component, projection.addView(component, data -> ownSnapshots.put(component, data)));
            controllers = new Controllers(projection, form, platform, platformComponents, filter, views);
            formFilters.controllers = controllers;
            formOrders.controllers = controllers;
            GFormChanges initial = new GFormChanges();
            initial.objects.put(group, one);
            initial.gridObjects.put(group, rows(one, two));
            put(initial, price, one, 10); put(initial, price, two, 20);
            put(initial, quantity, one, 1); put(initial, quantity, two, 2);
            put(initial, panel, GGroupObjectValue.EMPTY, 30);
            put(initial, single, GGroupObjectValue.EMPTY, 40);
            put(initial, nativeProperty, GGroupObjectValue.EMPTY, 50);
            apply(initial);
        }
        // TOOLBARSYSTEM(g) in scope, with the buttons a list's toolbar has
        void toolbarIn(GContainer scope) {
            GToolbar toolbar = new GToolbar();
            toolbar.ID = 12; toolbar.sID = "TOOLBARSYSTEM(items)"; toolbar.container = scope; scope.children.add(toolbar);
            toolbar.visible = true; toolbar.showCountQuantity = true; toolbar.showCalculateSum = true;
            toolbar.showPrintGroupXls = true; toolbar.showManualUpdate = true;
            group.grid.toolbar = toolbar;
        }
        // the widget of the view that draws what scope holds, as the form gives it (Verbs.getViewWidget)
        Widget viewWidget(GComponent scope) {
            return viewWidgets.computeIfAbsent(scope, view -> new Label());
        }
        // FILTERGROUP(stock) in scope: two filters, the first with a key, NONULL as a test says - the fixture reports
        // no filter selected
        GRegularFilterGroup filterGroupIn(GContainer scope, boolean noNull) {
            GRegularFilterGroup filterGroup = new GRegularFilterGroup();
            filterGroup.ID = 14; filterGroup.sID = "FILTERGROUP(stock)"; filterGroup.name = "stock";
            filterGroup.groupObject = group; filterGroup.noNull = noNull;
            filterGroup.container = scope; scope.children.add(filterGroup);
            GRegularFilter inStock = new GRegularFilter(); inStock.ID = 41; inStock.caption = "In stock";
            inStock.bindingEvents.add(new GInputBindingEvent(new GKeyInputEvent(new GKeyStroke(121)), null)); // F10
            GRegularFilter outOfStock = new GRegularFilter(); outOfStock.ID = 42; outOfStock.caption = "Out of stock";
            filterGroup.filters.add(inStock); filterGroup.filters.add(outOfStock);
            form.regularFilterGroups.add(filterGroup);
            return filterGroup;
        }
        // FILTERCONTROLS(g) in scope
        void filterControlsIn(GContainer scope) {
            GFilterControls controls = new GFilterControls();
            controls.ID = 13; controls.sID = "FILTERCONTROLS(items)"; controls.container = scope; scope.children.add(controls);
            group.grid.filterControls = controls;
        }
        // a component that is not a container drawn by a React component of its own - `custom = 'Own'` -, its view
        // made with the containers' (ownViews): placed by the react container it stands in, if any, as the server says
        // of a component with a view of its own there (FormView.isPlatformDrawn)
        void ownView(GComponent component) {
            component.custom = "Own"; component.react = true;
            component.platformDrawn = component.getReactPlace() != null;
            ownViews.add(component);
        }
        JavaScriptObject ownData(GComponent component) { return ownSnapshots.get(component); }
        // the additions a test asks for
        static Fixture withOuter() { return new Fixture(Fixture::addOuter); }
        static Fixture withNote() { return new Fixture(f -> f.note = f.property(25, "note", f.group, false, f.a)); }
        static Fixture withRemark() { return new Fixture(f -> f.remark = f.property(26, "remark", f.group, false, f.b)); }
        static Fixture withPlatformProperties() { return new Fixture(Fixture::addPlatformProperties); }
        // properties of the group in the main container, which the platform draws
        void addPlatformProperties() {
            platformNamed = property(37, "platform", group, false, form.mainContainer);
            platformUnnamed = property(38, "nameless", group, false, form.mainContainer);
            platformUnnamed.integrationSID = null;
            platformInColumns = property(39, "columns", group, false, form.mainContainer);
            platformInColumns.columnGroupObjects = new ArrayList<>(Arrays.asList(group));
        }
        // the group's rows drawn by the platform, its grid in the main container - its columns with it
        void rowsByThePlatform() {
            a.children.remove(group.grid);
            group.grid.container = form.mainContainer;
            form.mainContainer.children.add(group.grid);
        }
        // a group GWT draws the rows of, its grid in the main container, with one panel property in b
        void addOuter() {
            outer = new GGroupObject();
            outer.ID = 50; outer.nativeSID = "g50"; outer.sID = "outer"; outer.viewType = GClassViewType.LIST;
            outer.grid = new GGrid(); outer.grid.ID = 51; outer.grid.nativeSID = "c51"; outer.grid.sID = "outerGrid";
            outer.grid.groupObject = outer;
            outer.grid.container = form.mainContainer;
            form.mainContainer.children.add(outer.grid);
            outer.objects.add(new GObject(outer, "o", 50, "o", GIntegerType.instance));
            form.groupObjects.add(outer);
            outerPanel = property(52, "outerPanel", outer, false, b);
            outerName = property(53, "outerName", outer, true, form.mainContainer);
        }
        // as a view does: the snapshot is taken, then acted on (ReactRoot.updateData)
        void publish(int index, JavaScriptObject data) {
            snapshots[index] = data; publications[index]++;
            if (onPublish != null) onPublish.run();
        }
        // a property of the form, with its loading reader
        GPropertyDraw property(int id, String name, GGroupObject group, boolean list, GContainer scope) {
            GPropertyDraw property = GReactFormDataTest.property(form, id, name, group, list, scope);
            property.loadingReader = new GLoadingReader(id, group != null ? group.ID : -1);
            return property;
        }
        // a platform-drawn panel property React places: the design names it PROPERTY(<name>), its integration name is
        // <name>
        GPropertyDraw platformPanel(int id, String name, GGroupObject group, GContainer scope) {
            GPropertyDraw property = property(id, name, group, false, scope);
            property.platformDrawn = true;
            property.sID = "PROPERTY(" + name + ")";
            property.caption = name.toUpperCase();
            property.captionReader = new GCaptionReader(id, -1);
            return property;
        }
        JavaScriptObject data() { return snapshots[0]; }
        JavaScriptObject node() { return field(data(), "items"); }
        JavaScriptObject row(String key) { return field(field(node(), "byKey"), key); }
        // a delta, as the form hands it over: a group's rows and current object, a drop and a reader to the controller
        // of the owner - a change, published once it is applied (GFormController.applyRemoteChanges)
        void apply(GFormChanges changes) {
            projection.change(() -> {
                changes.gridObjects.foreachEntry((group, rows) -> controller(group).updateKeys(group, rows, changes, 0));
                changes.objects.foreachEntry((group, key) -> controller(group).updateCurrentKey(key));
                changes.dropProperties.forEach(controllers::drop);
                changes.properties.foreachEntry((reader, values) -> controllers.update(reader, values, changes.updateProperties.contains(reader)));
            });
        }
        // the form's controller of a group (GFormController.groupControllers): the part React draws the rows as - for
        // any other group, the platform's grid, not built here - and of a property
        GGroupController controller(GGroupObject group) {
            return controllers.groups.get(group);
        }
        // ... and what shows the group's sortings
        GUserOrdersController userOrdersController() {
            return controllers.userOrders.get(group);
        }
        // ... and what shows the group's user filters
        GUserFiltersController userFiltersController() {
            return controllers.userFilters.get(group);
        }
        // the server reporting the group's sortings, or its user filters, as the form takes them
        // (GFormController.applyOrderChanges, applyFilterChanges): other ones than the form has are the form's now,
        // and what shows them is told
        void reportOrders(LinkedHashMap<Column, Boolean> orders) {
            formOrders.report(group, orders);
        }
        void reportFilters(ArrayList<GPropertyFilterDTO> filters) {
            formFilters.report(group, filters);
        }
        GPropertyController controller(GPropertyDraw property) {
            return controllers.properties.get(property);
        }
        // the row a view names through the group's member - the row, its handle, its key - as the member reads it
        GGroupObjectValue choose(JavaScriptObject row) {
            chosen = null;
            callChange(field(sa.controller, "items"), row);
            return chosen;
        }
        // the client's own value for a cell, where the form sets it (GFormController.setLoadingValueAt): the property's
        // controller
        void setValue(GPropertyDraw property, GGroupObjectValue key, int value) {
            controller(property).setLoadingValueAt(property, key, PValue.getPValue(value));
        }
    }

    // a tree of two groups drawn in one react container: categories, and the items under them, keyed by the whole path
    // the form, as far as a member of a tree's projection asks it: what it is asked, logged in order, each request
    // numbered as the form numbers them - and what it showed before the answer, published as the form publishes it
    private static final class TreeVerbs implements GReactFormData.Verbs {
        final ArrayList<String> asked = new ArrayList<>();
        GReactFormData projection;
        Controllers controllers; // the form's controllers, a group's told of a request
        long requestIndex;

        TreeVerbs(GForm form) {
            filters = new FormFilters(form);
        }

        // as the form does: the request numbered and sent, the group's controller told of the current object at once
        // (GFormController.changeGroupObject) - published by the member's call that asked it
        public void changeCurrentObject(GGroupObject group, GGroupObjectValue key) {
            ask("current " + group.getSID() + " " + key.toKeyString());
            controllers.groups.get(group).changeCurrentKey(key);
        }
        public void changeProperty(GPropertyDraw property, GGroupObjectValue key, PValue value) { }
        public void getPropertyValues(GPropertyDraw property, GGroupObjectValue key, String value, String actionSID, JavaScriptObject successCallback, JavaScriptObject failureCallback, int increaseValuesNeededCount) { }
        public void countRecords(GGroupObject group, java.util.function.Consumer<Number> answer, java.util.function.Consumer<Throwable> failed) { }
        public void calculateSum(GGroupObject group, GPropertyDraw property, GGroupObjectValue columnKey, java.util.function.Consumer<Number> answer, java.util.function.Consumer<Throwable> failed) { }
        public void runTableReport(GGridProperty table) {
            table.match(grid -> null, tree -> asked.add("export tree " + tree.groups.get(0).ID));
        }
        public void changeUpdateMode(GGroupObject group, GUpdateMode updateMode) { }
        public boolean isManualUpdate(GGroupObject group) { return group.enableManualUpdate; }
        public String getRefusal(String surface) { return batching ? batchRefusal(surface) : null; }
        public void changeRegularFilter(GRegularFilterGroup filterGroup, GRegularFilter filter) { }
        public Widget getViewWidget(GComponent scope) { return null; }
        // as the form does: the request numbered and sent, the group's controller told of it
        public void expandNode(GGroupObject group, GGroupObjectValue key) {
            controllers.groups.get(group).changeExpanded(key, true, ask("expand " + group.getSID() + " " + key.toKeyString()));
        }
        public void collapseNode(GGroupObject group, GGroupObjectValue key) {
            controllers.groups.get(group).changeExpanded(key, false, ask("collapse " + group.getSID() + " " + key.toKeyString()));
        }
        public void expandAll(GGroupObject group) {
            controllers.groups.get(group).changeExpandedAll(true, ask("expand all " + group.getSID()));
        }
        public void collapseAll(GGroupObject group) {
            controllers.groups.get(group).changeExpandedAll(false, ask("collapse all " + group.getSID()));
        }
        public void expandCurrent(GGroupObject group, GGroupObjectValue key) {
            controllers.groups.get(group).changeExpandedUnder(key, ask("expand current " + group.getSID() + " " + key.toKeyString()));
        }
        final ArrayList<String> sorted = new ArrayList<>(); // what an orders member handed the form, in order
        // ... each group's the form's now, as the form has them
        final FormOrders orders = new FormOrders();
        public void changeOrders(List<GGroupObject> groups, LinkedHashMap<Column, Boolean> orders) {
            sorted.add(sids(groups) + describeOrders(orders));
            this.orders.change(groups, orders);
        }
        public LinkedHashMap<Column, Boolean> getOrders(GGroupObject group) {
            return orders.get(group);
        }
        final ArrayList<String> filtered = new ArrayList<>(); // what a filters member handed the form, in order
        // ... each group's the form's now, as the form has them
        final FormFilters filters;
        public long changeFilters(List<GGroupObject> groups, ArrayList<GPropertyFilter> conditions) {
            filtered.add(sids(groups) + describe(conditions));
            filters.change(groups, conditions);
            return 0;
        }
        public ArrayList<GPropertyFilter> getFilters(GGroupObject group) {
            return filters.get(group);
        }
        long ask(String request) {
            asked.add(request);
            return batching ? requestIndex + 1 : ++requestIndex;
        }
        // ... and a batch, as the form runs one: what is asked in it under the index of its one request - none when
        // nothing is asked -, a batch begun inside it a part of it. Published by the member's call that runs it
        boolean batching;
        public void batch(Runnable run) {
            if (batching) {
                run.run();
                return;
            }
            asked.add("batch");
            int begun = asked.size();
            batching = true;
            try {
                run.run();
            } finally {
                batching = false;
                if (asked.size() > begun)
                    requestIndex++;
                asked.add("sent");
            }
        }
        public void published() { }
    }
    private static final class TreeFixture {
        final GForm form = new GForm();
        final GContainer a = container(1);
        final GTreeGroup tree = new GTreeGroup();
        final GGroupObject cat = new GGroupObject(), item = new GGroupObject();
        final GGroupObjectValue c1 = new GGroupObjectValue(60, 1), c2 = new GGroupObjectValue(60, 2);
        final GGroupObjectValue i15 = itemKey(1, 5), i16 = itemKey(1, 6), i27 = itemKey(2, 7);
        final GReactFormData projection;
        final GReactFormData.ViewState sa; // the container the tree is drawn in: its controller
        final Controllers controllers;
        // the platform's controller of a group and of a property, as far as the form hands it on
        final RecordingTableController platform = new RecordingTableController();
        // ... and of the components, recording into the platform's log
        final RecordingComponentController platformComponents = new RecordingComponentController(platform);
        final TreeVerbs verbs = new TreeVerbs(form);
        // the tree's filter input controller where its toolbar has the button - only in a test that asks
        PanelDouble filter;
        JavaScriptObject data;
        int publications;
        // a second react container beside a, where a test puts a part of the tree in one (b()): its state
        GContainer b;
        GReactFormData.ViewState sb;

        TreeFixture() {
            this(f -> { });
        }
        // ... and what a test changes of the form before the projection is made of it
        TreeFixture(java.util.function.Consumer<TreeFixture> addition) {
            form.mainContainer = container(0);
            form.mainContainer.react = false;
            form.mainContainer.main = true;
            a.container = form.mainContainer;
            form.mainContainer.children.add(a);
            tree.ID = 62; tree.nativeSID = "c62"; tree.sID = "tree"; tree.name = "t";
            tree.container = a;
            a.children.add(tree);
            group(cat, 60, "cat");
            group(item, 61, "item");
            item.upTreeGroups.add(cat);
            addition.accept(this);
            projection = new GReactFormData(form, GComponent::getReactPlace, verbs);
            verbs.projection = projection;
            sa = projection.addView(a, d -> { data = d; publications++; });
            if (b != null)
                sb = projection.addView(b, d -> { });
            controllers = new Controllers(projection, form, platform, platformComponents, filter, null);
            verbs.controllers = controllers;
            verbs.filters.controllers = controllers;
            verbs.orders.controllers = controllers;
        }
        // a react container beside a, made the first time a test asks for it
        GContainer b() {
            if (b == null) {
                b = container(2);
                b.container = form.mainContainer;
                form.mainContainer.children.add(b);
            }
            return b;
        }
        // TOOLBARSYSTEM(TREE tree) in scope, shown - its flags say nothing of a tree's buttons
        void toolbarIn(GContainer scope) {
            GToolbar toolbar = new GToolbar();
            toolbar.ID = 66; toolbar.sID = "TOOLBARSYSTEM(TREE tree)"; toolbar.container = scope; scope.children.add(toolbar);
            toolbar.visible = true;
            tree.toolbar = toolbar;
        }
        // FILTERS(TREE tree) in scope, a plain box
        void filtersIn(GContainer scope) {
            GContainer filters = container(5); filters.react = false; filters.custom = null;
            filters.container = scope; scope.children.add(filters);
            tree.filtersContainer = filters;
        }
        // FILTERCONTROLS(TREE tree) in scope
        void filterControlsIn(GContainer scope) {
            GFilterControls controls = new GFilterControls();
            controls.ID = 67; controls.sID = "FILTERCONTROLS(TREE tree)"; controls.container = scope; scope.children.add(controls);
            tree.filterControls = controls;
        }
        // the tree drawn by the platform, outside the react container
        void treeByThePlatform() {
            a.children.remove(tree);
            tree.container = form.mainContainer;
            form.mainContainer.children.add(tree);
        }
        void group(GGroupObject group, int id, String sid) {
            group.ID = id; group.nativeSID = "g" + id; group.sID = sid; group.viewType = GClassViewType.LIST;
            group.parent = tree;
            tree.groups.add(group);
            group.objects.add(new GObject(group, sid, id, sid, GIntegerType.instance));
            form.groupObjects.add(group);
        }
        static GGroupObjectValue itemKey(int category, int item) {
            return new GGroupObjectValue(2, new int[]{60, 61}, new Serializable[]{category, item});
        }
        // a column of a group of the tree, drawn with its rows
        GPropertyDraw column(int id, String name, GGroupObject group) {
            return property(form, id, name, group, true, a);
        }
        // both groups with their parents, as the server sends a tree it re-read: a category is a root, and an item's
        // parent is empty - the path above it is in its own key
        GFormChanges page() {
            GFormChanges changes = new GFormChanges();
            changes.gridObjects.put(cat, rows(c1, c2));
            changes.parentObjects.put(cat, rows(GGroupObjectValue.EMPTY, GGroupObjectValue.EMPTY));
            changes.gridObjects.put(item, rows(i15, i16, i27));
            changes.parentObjects.put(item, rows(GGroupObjectValue.EMPTY, GGroupObjectValue.EMPTY, GGroupObjectValue.EMPTY));
            return changes;
        }
        JavaScriptObject row(GGroupObject group, GGroupObjectValue key) {
            return field(field(field(data, group.getSID()), "byKey"), key.toKeyString());
        }
        // a delta, as the form hands it over: a group's rows - and its hierarchy with them - and its current object to
        // the group's controller, a drop and a reader to the controller of the owner
        void apply(GFormChanges changes) {
            answer(changes, 0);
        }
        // ... the answer to a request: its keys come with its index, as the form hands them on - a change, published once
        // it is applied
        void answer(GFormChanges changes, int requestIndex) {
            projection.change(() -> {
                changes.gridObjects.foreachEntry((group, rows) -> controllers.groups.get(group).updateKeys(group, rows, changes, requestIndex));
                changes.objects.foreachEntry((group, key) -> controllers.groups.get(group).updateCurrentKey(key));
                changes.dropProperties.forEach(controllers::drop);
                changes.properties.foreachEntry((reader, values) -> controllers.update(reader, values, changes.updateProperties.contains(reader)));
            });
        }
        // the server reporting a group's user filters, as the form takes them (FormFilters.report)
        void report(GGroupObject group, ArrayList<GPropertyFilterDTO> filters) {
            verbs.filters.report(group, filters);
        }
    }

    // the platform's toolbar, as far as the projection makes one, where it makes none of its own - of a group or of a
    // tree, as the form makes them -: how many filter buttons it was given, and what the button was told to show, in
    // order. A double: the platform's buttons need the page's scripts
    private static abstract class ToolbarDouble<R extends GTableController> implements GToolbarController<R> {
        int filterToggles;
        final ArrayList<Boolean> controlsShown = new ArrayList<>();
        public void addFilterToggle(GFilterInputController filter) { filterToggles++; }
        public void showFilterControls(boolean shown) { controlsShown.add(shown); }
        public void setVisible(boolean visible) { }
    }
    // ... a group's: the rows given to it
    private static final class GridToolbarDouble extends ToolbarDouble<GGridController> implements GGridToolbarController {
        final ArrayList<GGridController> gridRows = new ArrayList<>();
        public void addRows(GGridController rows) { gridRows.add(rows); }
        public void showView(GListViewType view) { }
        public void updateState(Boolean updateState) { }
    }
    // ... and a tree's: the tree's rows given to it
    private static final class TreeToolbarDouble extends ToolbarDouble<GTreeController> {
        final ArrayList<GTreeController> treeRows = new ArrayList<>();
        public void addRows(GTreeController rows) { treeRows.add(rows); }
    }
    // the platform's panel - a filter input controller of a group or of a tree -, as far as its toolbar, its controls
    // and the projection ask it - its commands and its editors, as the panel is both -: whether its controls are shown,
    // and the toggle, which tells the toolbar, as the panel does; how many times it was told that what draws the rows
    // has changed; what it was asked, in order, the FILTERS box and the mode it answers with; and what its editors'
    // controls were told to show, in order
    private static final class PanelDouble extends GFilterInputController implements GFilterEditors {
        GToolbarController<?> toolbar;
        PanelDouble() {
            super(null, null);
        }
        int destroyed; // how many times it was let go of
        public void destroy() { destroyed++; }
        boolean shown;
        int updates;
        final ArrayList<String> calls = new ArrayList<>();
        final ArrayList<Boolean> editorsShown = new ArrayList<>();
        boolean filtersContainer = true, manual;

        public boolean isControlsVisible() { return shown; }
        public void toggleControlsVisible() { shown = !shown; toolbar.showFilterControls(shown); }
        public void setControlsVisible(boolean visible) { }
        public void setVisible(boolean visible) { }
        public void update() { updates++; }
        public void addCondition(Event keyEvent, boolean replace, boolean checkExistingFilter) { }
        public void quickEditFilter(Event keyEvent, GPropertyDraw property, GGroupObjectValue columnKey) { }
        public void replaceConditions(ArrayList<GPropertyFilter> conditions) { }
        public boolean hasConditionsToReset() { return false; }
        public boolean hasFiltersContainer() { return filtersContainer; }
        public void addCondition() { calls.add("add"); }
        public void resetConditions() { calls.add("reset"); }
        public void setConditionsControlsVisible(boolean visible) { editorsShown.add(visible); }
        public boolean isManualApplyMode() { return manual; }
        public void applyFilters() { calls.add("apply"); }
    }

    // the filter controls, of either kind - a view's, or the platform's where a test says so (shownByToolbar) -, as far
    // as the filter input controller binds them: whose commands they give, and what they were told to show, in order
    private static final class FilterControlsDouble implements GFilterControlsController {
        GFiltersHandler handler;
        GFilterEditors editors;
        boolean shownByToolbar;
        final ArrayList<Boolean> shown = new ArrayList<>();
        public void bind(GFiltersHandler handler, GFilterEditors editors) { this.handler = handler; this.editors = editors; }
        public boolean isShownByToolbar() { return shownByToolbar; }
        public void setVisible(boolean visible) { shown.add(visible); }
        public void setApplyEnabled(boolean enabled) { }
        public void setTableVisible(boolean visible) { }
    }
    // the platform's table's views, as far as its toolbar's view switch asks them: a grid and a pivot, the one shown,
    // and the change, which tells the toolbar, as the platform's does
    private static final class ListViewDouble implements GListViewController {
        GGridToolbarController toolbar;
        GListViewType shown = GListViewType.GRID;
        final ArrayList<GListViewType> changed = new ArrayList<>();

        public List<GListViewType> getListViews() { return Arrays.asList(GListViewType.GRID, GListViewType.PIVOT); }
        public GListViewType getListView() { return shown; }
        public void changeListView(GListViewType view) { shown = view; changed.add(view); toolbar.showView(view); }
    }

    private static GContainer container(int id) {
        GContainer container = new GContainer();
        container.ID = id; container.nativeSID = "c" + id; container.sID = "c" + id;
        container.react = true; container.custom = "Test";
        return container;
    }
    private static ArrayList<GGroupObjectValue> rows(GGroupObjectValue... keys) { return new ArrayList<>(Arrays.asList(keys)); }
    // a property of a form, of a group or of none, a list one or not, drawn in scope - named by its name everywhere
    private static GPropertyDraw property(GForm form, int id, String name, GGroupObject group, boolean list, GContainer scope) {
        GPropertyDraw property = new GPropertyDraw();
        property.ID = id; property.nativeSID = "p" + id; property.sID = name; property.integrationSID = name;
        property.groupObject = group; property.isList = list; property.container = scope;
        property.valueType = GIntegerType.instance;
        form.propertyDraws.add(property); scope.children.add(property);
        return property;
    }
    // the platform's controller of a group and of a property, as far as the form hands it on - a table controller,
    // which a property's view controller is too, with no form: what reaches it is recorded, and the rest of its
    // surface, which the projection never asks of it, does nothing
    private static final class RecordingTableController extends GPlatformTableController<GGridProperty, GTableView, GToolbarController<?>> implements GGridController {
        final ArrayList<GPropertyReader> updated = new ArrayList<>();
        final ArrayList<GPropertyDraw> dropped = new ArrayList<>(), focused = new ArrayList<>(), loading = new ArrayList<>();

        RecordingTableController() {
            super(null, null, null);
        }
        public void updateValue(GPropertyDraw property, NativeHashMap<GGroupObjectValue, PValue> values, boolean partial) { updated.add(property); cells.put(property, values); }
        public void updateOther(GExtraPropertyReader reader, NativeHashMap<GGroupObjectValue, PValue> values, boolean partial) { updated.add(reader); }
        public void updateLabel(GExtraLabelReader reader, NativeHashMap<GGroupObjectValue, PValue> values, boolean partial) { updated.add(reader); }
        public void updateAttribute(GGroupObjectPropertyReader reader, NativeHashMap<GGroupObjectValue, PValue> values, boolean partial) { updated.add(reader); }
        public void dropProperty(GPropertyDraw property) { dropped.add(property); }
        public boolean isPropertyShown(GPropertyDraw property) { return true; }
        public void focusProperty(GPropertyDraw property) { focused.add(property); }
        public Pair<GGroupObjectValue, PValue> setLoadingValueAt(GPropertyDraw property, GGroupObjectValue fullCurrentKey, PValue value) { loading.add(property); return null; }
        // the values of a property that reached it, a cell's - the client's own a test puts on top - answered by key
        final LinkedHashMap<GPropertyDraw, NativeHashMap<GGroupObjectValue, PValue>> cells = new LinkedHashMap<>();
        public PValue getValueAt(GPropertyDraw property, GGroupObjectValue fullCurrentKey) {
            NativeHashMap<GGroupObjectValue, PValue> values = cells.get(property);
            return values != null ? values.get(fullCurrentKey) : null;
        }
        public GGroupObjectValue getSelectedKey() { return null; }
        public int getSelectedRow() { return selectedRow; }
        public void modifyGroupObject(GGroupObjectValue key, boolean add, int position) { }
        public void updateKeys(GGroupObject group, ArrayList<GGroupObjectValue> keys, GFormChanges fc, int requestIndex) { }
        protected boolean updateView(long requestIndex, GFormChanges fc) { return false; }
        public void update(long requestIndex, GFormChanges fc) { }
        public void destroy() { }
        public void addUserPreferences(List<GGroupObjectUserPreferences> user, List<GGroupObjectUserPreferences> general) { }
        public void updateCurrentKey(GGroupObjectValue currentKey) { }
        public void updateOrders(GGroupObject group, LinkedHashMap<Column, Boolean> orders) { }

        public void updateCellGridElementClasses(GGridElementClassReader reader, NativeHashMap<GGroupObjectValue, PValue> values) { }
        public void updateCellValueElementClasses(GValueElementClassReader reader, NativeHashMap<GGroupObjectValue, PValue> values) { }
        public void updateCellCaptionElementClasses(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values) { }
        public void updateCellFooterElementClasses(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values) { }
        public void updateCellFontValues(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values) { }
        public void updateCellBackgroundValues(GBackgroundReader reader, NativeHashMap<GGroupObjectValue, PValue> values) { }
        public void updateCellForegroundValues(GForegroundReader reader, NativeHashMap<GGroupObjectValue, PValue> values) { }
        public void updateImageValues(GImageReader reader, NativeHashMap<GGroupObjectValue, PValue> values) { }
        public void updatePropertyCaptions(GCaptionReader reader, NativeHashMap<GGroupObjectValue, PValue> values) { }
        public void updateLoadings(GLoadingReader reader, NativeHashMap<GGroupObjectValue, PValue> values) { }
        public void updateShowIfValues(GShowIfReader reader, NativeHashMap<GGroupObjectValue, PValue> values) { }
        public void updateFooterValues(GFooterReader reader, NativeHashMap<GGroupObjectValue, PValue> values) { }
        public void updateReadOnlyValues(GReadOnlyReader reader, NativeHashMap<GGroupObjectValue, PValue> values) { }
        public void updatePropertyComments(GCommentReader reader, NativeHashMap<GGroupObjectValue, PValue> values) { }
        public void updateCellCommentElementClasses(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values) { }
        public void updatePlaceholderValues(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values) { }
        public void updatePatternValues(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values) { }
        public void updateRegexpValues(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values) { }
        public void updateRegexpMessageValues(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values) { }
        public void updateTooltipValues(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values) { }
        public void updateValueTooltipValues(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values) { }
        public void updatePropertyCustomOptionsValues(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values) { }
        public void updateChangeKeyValues(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values) { }
        public void updateChangeMouseValues(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values) { }
        public void updateDefaultValueValues(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values) { }
        public void updateLastValues(GLastReader reader, NativeHashMap<GGroupObjectValue, PValue> values) { }
        public void updateProperty(GPropertyDraw property, ArrayList<GGroupObjectValue> columnKeys, boolean updateKeys, NativeHashMap<GGroupObjectValue, PValue> values) { }
        public void removeProperty(GPropertyDraw property) { }
        // the lists its filter input controller states through it, in order
        final ArrayList<String> filtered = new ArrayList<>();
        public long changeFilter(ArrayList<GPropertyFilter> conditions) { filtered.add(describe(conditions)); return -1; }
        public boolean focusFirstWidget(FocusUtils.Reason reason) { return false; }
        public void updateRowBackgroundValues(NativeHashMap<GGroupObjectValue, PValue> values) { }
        public void updateRowSelectValues(NativeHashMap<GGroupObjectValue, PValue> values, boolean updateKeys) { }
        public void updateRowForegroundValues(NativeHashMap<GGroupObjectValue, PValue> values) { }
        public void updateCustomOptionsValues(NativeHashMap<GGroupObjectValue, PValue> values) { }
        public GGroupObject getSelectedGroupObject() { return null; }
        // the cell it is on, when a test puts it on one - on none, -1, else
        GPropertyDraw selectedProperty; PValue selectedValue; int selectedRow = -1;
        public GPropertyDraw getSelectedFilterProperty() { return selectedProperty; }
        public GGroupObjectValue getSelectedColumnKey() { return null; }
        public PValue getSelectedValue(GPropertyDraw property, GGroupObjectValue columnKey) { return selectedValue; }
        public List<Pair<Column, String>> getFilterColumns() { return null; }
        // ... and as the rows of a group's grid the platform draws, which give themselves to the group's toolbar
        // (GGridController): their views - a test's, where it gives some -, and how many times they were asked for
        // their buttons, which only the platform's toolbar asks for
        GListViewController views;
        int buttonsAsked;
        public void addToolbarButtons(GPlatformGridToolbarController toolbar) { buttonsAsked++; }
        public GListViewController getListViewController() { return views; }
    }
    // the platform's tree, as far as its toolbar asks it here: what it was asked to open, in order - the fixture asks
    // it nothing else -, what a view's part of the toolbar opens through members of its own, itself, and, for its
    // groups, the fixture's one recording table, as every group's controller and what shows their user orders
    private static final class PlatformTreeDouble implements GTreeController {
        final ArrayList<String> asked;
        final RecordingTableController groups;
        PlatformTreeDouble(ArrayList<String> asked, RecordingTableController groups) { this.asked = asked; this.groups = groups; }
        public GGroupController getGroupController(GGroupObject group) { return groups; }
        public GUserOrdersController getUserOrdersController() { return groups; }
        public void expandCurrent() { asked.add("expand current"); }
        public void expandTree() { asked.add("expand tree"); }
        public GTreeController getOpenedByToolbarMembers() { return this; }
        public GGroupObject getSelectedGroupObject() { return null; }
        public GPropertyDraw getSelectedFilterProperty() { return null; }
        public GGroupObjectValue getSelectedColumnKey() { return null; }
        public int getSelectedRow() { return -1; }
        public PValue getSelectedValue(GPropertyDraw property, GGroupObjectValue columnKey) { return null; }
        public List<Pair<Column, String>> getFilterColumns() { return null; }
        public long changeFilter(ArrayList<GPropertyFilter> conditions) { return -1; }
        public boolean focusFirstWidget(FocusUtils.Reason reason) { return false; }
        public void bindFilter(GFilterInputController filter) { }
        public void update(long requestIndex, GFormChanges fc) { }
        public void destroy() { }
    }
    // ... and the platform's controller of the components: what reaches it is recorded in the same log
    private static final class RecordingComponentController implements GComponentController {
        final RecordingTableController reached;

        RecordingComponentController(RecordingTableController reached) {
            this.reached = reached;
        }
        public void updateLabel(GComponentLabelReader reader, NativeHashMap<GGroupObjectValue, PValue> values, boolean partial) { reached.updated.add(reader); }
        public void updateOther(GComponentReader reader, NativeHashMap<GGroupObjectValue, PValue> values, boolean partial) { reached.updated.add(reader); }
    }
    // the form's controllers of the owners, made as the form makes them (GFormController.initializeControllers): what
    // draws each group's rows, in the order of the groups: a table's, whole (initializeTable) - a tree's at its top
    // group -, or a group's in PANEL view; then the form's controller of each owner - what React has of each owner, made from the
    // platform's controller of it -;
    // and a delivery, as the form hands it (GPropertyReader.update): to the controller of the reader's owner, told what
    // kind of reader it is. The platform's controllers are doubles, said once here: one recording table for every
    // group, with the views a test gives it; a tree that records what its toolbar asks; the toolbars' doubles, one
    // where the fixture's design has no toolbar too, as the form makes one always; no filter controls of the
    // platform's; the test's filter input controller over every toolbar, where it gives one; no panel of the platform's
    private static final class Controllers {
        final GForm form;
        final GReactFormData projection;
        final RecordingTableController platform;
        final RecordingComponentController platformComponents;
        final PanelDouble filter;
        final ListViewDouble views;
        final LinkedHashMap<GGroupObject, GGroupController> groups = new LinkedHashMap<>();
        final LinkedHashMap<GGroupObject, GUserOrdersController> userOrders = new LinkedHashMap<>();
        final LinkedHashMap<GGroupObject, GUserFiltersController> userFilters = new LinkedHashMap<>();
        // what draws each group's grid's rows and each tree's, as the projection answers it (createGridController,
        // createTreeController)
        final LinkedHashMap<GGroupObject, GGridController> gridRows = new LinkedHashMap<>();
        final LinkedHashMap<GTreeGroup, GTreeController> treeRows = new LinkedHashMap<>();
        final LinkedHashMap<GPropertyDraw, GPropertyController> properties = new LinkedHashMap<>();
        final LinkedHashMap<GComponent, GComponentController> components = new LinkedHashMap<>();

        final LinkedHashMap<GGroupObject, GGridToolbarController> toolbars = new LinkedHashMap<>();
        // each table's filter controls
        final LinkedHashMap<GGridProperty, GFilterControlsController> filterControls = new LinkedHashMap<>();
        final LinkedHashMap<GRegularFilterGroup, GFilterGroupController> filterGroups = new LinkedHashMap<>();
        final LinkedHashMap<GTreeGroup, GToolbarController<GTreeController>> treeToolbars = new LinkedHashMap<>();
        final ArrayList<String> platformTree = new ArrayList<>(); // what the platform's tree was asked by its toolbar

        Controllers(GReactFormData projection, GForm form, RecordingTableController platform, RecordingComponentController platformComponents) {
            this(projection, form, platform, platformComponents, null, null);
        }
        Controllers(GReactFormData projection, GForm form, RecordingTableController platform, RecordingComponentController platformComponents, PanelDouble filter, ListViewDouble views) {
            this.form = form;
            this.projection = projection;
            this.platform = platform;
            this.platformComponents = platformComponents;
            this.filter = filter;
            this.views = views;
            platform.views = views;
            // what draws each group's rows, as the form sets it up, in the order of the groups: a table, whole - a
            // tree at its top group, a grid (initializeTable) -, or none in PANEL view
            for (GGroupObject group : form.groupObjects)
                if (group.isInTree()) {
                    if (group.getUpTreeGroup() == null)
                        initializeTable(group.parent);
                } else if (group.viewType.isList())
                    initializeTable(group.grid);
                else
                    initializeGroupControllers(group, new GPlatformPanelGroupController(), GUserOrdersController.NONE, GUserFiltersController.NONE);
            // then the properties', the components', the filter groups'; then the projection sets out what it has
            for (GPropertyDraw property : form.propertyDraws)
                properties.put(property, projection.createPropertyController(property, platform));
            initializeComponentControllers(form.mainContainer);
            for (GRegularFilterGroup filterGroup : form.regularFilterGroups)
                filterGroups.put(filterGroup, projection.createFilterGroupController(filterGroup, () -> null));
            projection.initialize();
        }
        // A TABLE, whole, as the form makes it (GFormController.initializeTable): its toolbar and rows of its kind,
        // then its filter controls, the filter input controller over its rows, whose button the toolbar takes, what
        // shows its groups' user filters, one for all of them, what the rows give for its groups - each group's
        // controller and what shows their user orders -, and each group's own controllers
        void initializeTable(GGridProperty table) {
            Pair<GTableController, GToolbarController<?>> made = table.match(this::initializeGrid, this::initializeTree);
            GTableController rows = made.first;
            GToolbarController<?> toolbar = made.second;
            if (table.filterControls != null)
                filterControls.put(table, projection.createFilterControlsController(table, () -> null));
            if (filter != null) {
                filter.toolbar = toolbar;
                toolbar.addFilterToggle(filter);
            }
            GUserFiltersController tableFilters = projection.createFiltersControllers(table, rows, new GFilterControlsToggle(() -> { }, GFilterControlsController.NONE, toolbar), () -> null).second;
            GUserOrdersController tableOrders = rows.getUserOrdersController();
            for (GGroupObject group : table.getGroups())
                initializeGroupControllers(group, rows.getGroupController(group), tableOrders, tableFilters);
        }
        // A TABLE of its kind, as the form makes it (GFormController.initializeGrid, initializeTree): a GRID's toolbar,
        // then what draws its rows - the recording table, giving itself to the toolbar, and the view it shows, as the
        // grid tells it -; both given back for what is every table's
        Pair<GTableController, GToolbarController<?>> initializeGrid(GGrid grid) {
            GGroupObject group = grid.groupObject;
            GGridToolbarController toolbar = grid.toolbar != null ? projection.createGridToolbarController(grid, GridToolbarDouble::new) : new GridToolbarDouble();
            toolbars.put(group, toolbar);
            GGridController rows = projection.createGridController(grid, toolbar, () -> {
                toolbar.addRows(platform);
                if (views != null) {
                    views.toolbar = toolbar;
                    toolbar.showView(views.getListView());
                }
                return platform;
            });
            gridRows.put(group, rows);
            return new Pair<>(rows, toolbar);
        }
        // ... a TREE's the same way - the platform's tree in its constructor, as this double does
        Pair<GTableController, GToolbarController<?>> initializeTree(GTreeGroup tree) {
            GToolbarController<GTreeController> toolbar = tree.toolbar != null ? projection.createTreeToolbarController(tree, TreeToolbarDouble::new) : new TreeToolbarDouble();
            treeToolbars.put(tree, toolbar);
            GTreeController rows = projection.createTreeController(tree, toolbar, () -> {
                PlatformTreeDouble platformRows = new PlatformTreeDouble(platformTree, platform);
                toolbar.addRows(platformRows);
                return platformRows;
            });
            treeRows.put(tree, rows);
            return new Pair<>(rows, toolbar);
        }
        // a group's own controllers: the form's controller of it - what draws its rows gives it -, and what shows its
        // user orders and its user filters, settled by its table
        void initializeGroupControllers(GGroupObject group, GGroupController controller, GUserOrdersController groupOrders, GUserFiltersController groupFilters) {
            groups.put(group, controller);
            userOrders.put(group, groupOrders);
            userFilters.put(group, groupFilters);
        }
        void initializeComponentControllers(GComponent component) {
            components.put(component, projection.createComponentController(component, platformComponents));
            for (GComponent child : component.getChildren())
                initializeComponentControllers(child);
        }
        void update(GPropertyReader reader, NativeHashMap<GGroupObjectValue, PValue> values, boolean partial) {
            if (reader instanceof GPropertyDraw)
                properties.get(reader).updateValue((GPropertyDraw) reader, values, partial);
            // what labels a property too: all of it is its controller's
            else if (reader instanceof GExtraPropertyReader)
                ((GExtraPropertyReader) reader).update(properties.get(form.getProperty(((GExtraPropertyReader) reader).propertyID)), values, partial);
            else if (reader instanceof GGroupObjectPropertyReader)
                groups.get(form.getGroupObject(((GGroupObjectPropertyReader) reader).groupObjectID)).updateAttribute((GGroupObjectPropertyReader) reader, values, partial);
            // a component's reader - a property's too, its class, which is the platform's (createComponentController)
            else
                ((GComponentReader) reader).update(components.get(((GComponentReader) reader).getReaderComponent()), values, partial);
        }
        void drop(GPropertyDraw property) {
            properties.get(property).dropProperty(property);
        }
    }
    // whether what a delta brings for a reader reaches the platform - the form hands it what React takes nothing of
    private static boolean reachesPlatform(RecordingTableController platform, Controllers controllers, GPropertyReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        platform.updated.clear();
        controllers.update(reader, values, false);
        return platform.updated.contains(reader);
    }
    // ... and whether a property's drop does
    private static boolean dropReachesPlatform(RecordingTableController platform, Controllers controllers, GPropertyDraw property) {
        platform.dropped.clear();
        controllers.drop(property);
        return platform.dropped.contains(property);
    }
    private static void put(GFormChanges changes, GPropertyReader reader, GGroupObjectValue key, Object value) {
        NativeHashMap<GGroupObjectValue, PValue> values = changes.properties.get(reader);
        if (values == null) { values = new NativeHashMap<>(); changes.properties.put(reader, values); }
        values.put(key, value == null ? null : value instanceof PValue ? (PValue) value : value instanceof Boolean ? PValue.getPValue((Boolean) value)
                : value instanceof String ? PValue.getPValue((String) value) : PValue.getPValue((Integer) value));
    }
    private static GFormChanges drop(GPropertyDraw... properties) {
        GFormChanges changes = new GFormChanges();
        changes.dropProperties.addAll(Arrays.asList(properties));
        return changes;
    }
    private static native JavaScriptObject field(JavaScriptObject object, String name) /*-{ return object[name]; }-*/;
    private static native boolean own(JavaScriptObject object, String name) /*-{ return Object.prototype.hasOwnProperty.call(object, name); }-*/;
    private static native double value(JavaScriptObject row, String name) /*-{ return row[name].value; }-*/;
    private static native boolean hasValue(JavaScriptObject entry) /*-{ return Object.prototype.hasOwnProperty.call(entry, "value"); }-*/;
    // a shown cell says its value, null too: none is the server's "no value" for it
    private static native boolean noValue(JavaScriptObject entry) /*-{ return entry.value == null; }-*/;
    private static native String text(JavaScriptObject object, String name) /*-{ return object[name]; }-*/;
    private static native boolean flag(JavaScriptObject object, String name) /*-{ return !!object[name]; }-*/;
    private static native int length(JavaScriptObject array) /*-{ return array.length; }-*/;
    private static native boolean bare(JavaScriptObject object) /*-{ return Object.getPrototypeOf(object) === null; }-*/;
    private static native boolean plain(JavaScriptObject object) /*-{ return Object.getPrototypeOf(object) === Object.prototype; }-*/;
    private static native JavaScriptObject at(JavaScriptObject array, int index) /*-{ return array[index]; }-*/;
    private static native String join(JavaScriptObject array) /*-{ return array.join(","); }-*/;
    // a condition list as the recorder keeps it: each one's property, compare and flags, in order
    private static String describe(ArrayList<GPropertyFilter> conditions) {
        StringBuilder text = new StringBuilder();
        for (GPropertyFilter condition : conditions)
            text.append(" ").append(condition.column.property.integrationSID).append(condition.compare)
                    .append(condition.negation ? " not" : "").append(condition.junction ? "" : " or");
        return text.toString();
    }
    // ... a list of sortings, each its property's name and direction, as a test reads it
    private static String describeOrders(LinkedHashMap<Column, Boolean> orders) {
        StringBuilder text = new StringBuilder();
        for (Map.Entry<Column, Boolean> order : orders.entrySet())
            text.append(" ").append(order.getKey().property.integrationSID).append(order.getValue() ? "" : " desc");
        return text.toString();
    }
    // ... and the groups a list is stated for, by their names
    private static String sids(List<GGroupObject> groups) {
        StringBuilder text = new StringBuilder();
        for (GGroupObject group : groups)
            text.append(text.length() > 0 ? " " : "").append(group.getSID());
        return text.toString();
    }
    // a condition on price whose value is 5 the first time it is read and nothing after
    private static native JavaScriptObject valueReadOnce() /*-{
        var condition = {property: 'price'}, reads = 0;
        Object.defineProperty(condition, 'value', {enumerable: true, get: function() { return reads++ === 0 ? 5 : null; }});
        return condition;
    }-*/;
    private static native void callFilters(JavaScriptObject member) /*-{
        member.change([{property: 'price', compare: '<', value: 3, or: true}, {property: 'price'}]);
        member.change({property: 'price', value: 5, negation: true});
        member.change({property: 'price', value: ''});
    }-*/;
    // what a call through a state member is refused with, null where it is made
    private static native String refusedChange(JavaScriptObject member, Object argument) /*-{
        try { member.change(argument); return null; } catch (e) { return e.message; }
    }-*/;
    // a member's change(value, row) with a value as a view writes it, and the reason it is refused, if it is
    private static native String refusedChange(JavaScriptObject member, String value, JavaScriptObject row) /*-{
        try { member.change(value, row); return null; } catch (e) { return e.message; }
    }-*/;
    // a member of a view's controller, at a group (null: the empty group, the controller itself)
    private static JavaScriptObject member(GReactFormData.ViewState state, String group, String name) {
        JavaScriptObject owner = group == null ? state.controller : field(state.controller, group);
        return owner == null ? null : field(owner, name);
    }
    // ... a call through one, as a view makes it - its argument an object, a string, a number or a boolean - and
    // whether it is refused
    private static native void callChange(JavaScriptObject member, Object argument) /*-{ member.change(argument); }-*/;
    // ... and what a call of another verb is refused with
    private static native String refusedWith(JavaScriptObject member, String verb, Object argument) /*-{
        try { member[verb](argument); return null; } catch (e) { return e.message; }
    }-*/;
    private static void assertRefusal(String expected, String refused) {
        assertTrue(String.valueOf(refused), refused != null && refused.contains(expected));
    }
    // the names of an object's members, sorted: which there are, not the order they were made in
    private static native String keysOf(JavaScriptObject object) /*-{ return Object.keys(object).sort().join(","); }-*/;
    // what a Promise is resolved with, or rejected with
    private static native void then(JavaScriptObject promise, java.util.function.Consumer<Object> resolved, java.util.function.Consumer<Object> rejected) /*-{
        promise.then(function (value) { resolved.@java.util.function.Consumer::accept(Ljava/lang/Object;)(value); },
                     function (error) { rejected.@java.util.function.Consumer::accept(Ljava/lang/Object;)(error.message); });
    }-*/;
    private static native JavaScriptObject parse(String json) /*-{ return JSON.parse(json); }-*/;
    private static native void changeAt(JavaScriptObject member, int value, JavaScriptObject row) /*-{ member.change(value, row == null ? undefined : row); }-*/;
    // a batch, as a view begins one: run is what its function does
    private static native void batch(JavaScriptObject controller, Runnable run) /*-{
        controller.batch(function () { run.@java.lang.Runnable::run()(); });
    }-*/;
    private static boolean refuses(JavaScriptObject member, int value) {
        return refusesAt(member, value, null);
    }
    private static boolean refusesAt(JavaScriptObject member, int value, JavaScriptObject row) {
        try { changeAt(member, value, row); return false; } catch (RuntimeException e) { return true; }
    }
    private static boolean refusesBatch(JavaScriptObject controller, Runnable run) {
        try { batch(controller, run); return false; } catch (RuntimeException e) { return true; }
    }
    // whether a view's controller has a property's member under the name, at a group (null: the empty group, the
    // controller itself): an object of the verbs a property member answers to. The name says which property - the one
    // the view's node of the group carries under it
    private static boolean hasMember(GReactFormData.ViewState state, String group, String name) {
        JavaScriptObject owner = group == null ? state.controller : field(state.controller, group);
        return owner != null && own(owner, name) && "function".equals(typeOf(field(owner, name), "exec"));
    }
    private static native double number(JavaScriptObject object, String name) /*-{ return object[name]; }-*/;
    private static native boolean has(JavaScriptObject object, String name) /*-{ return name in object; }-*/;
    private static native boolean sameField(JavaScriptObject a, String aName, JavaScriptObject b, String bName) /*-{ return a[aName] === b[bName]; }-*/;
    private static native boolean isFalse(JavaScriptObject object, String name) /*-{ return object[name] === false; }-*/;

    // a group in PANEL view - or TOOLBAR - is sent no rows - only its current object, which nothing chooses among rows
    // -, so no view has a node of its rows, wherever its grid is: the platform's controller of such a group has it
    // (GPlatformPanelGroupController) - the whole key the form asks for, no row, nothing a delta brings to draw -,
    // nothing shows its orders or its user filters, as the platform shows neither for it, and a panel property of it
    // is on a plain node, with no rows and no change(row)
    public void testAGroupInPanelViewHasNoRows() {
        Fixture filtered = new Fixture(fixture -> { // ... nor user filters, wherever its FILTERS box is
            fixture.group.viewType = GClassViewType.PANEL;
            fixture.filtersApart();
        });
        assertSame(GUserFiltersController.NONE, filtered.userFiltersController());
        assertNull(field(filtered.snapshots[2], "items")); // c, where its FILTERS box is, has no node of it
        for (GClassViewType viewType : Arrays.asList(GClassViewType.PANEL, GClassViewType.TOOLBAR)) {
            Fixture f = new Fixture(fixture -> fixture.group.viewType = viewType);
            GGroupController controller = f.controller(f.group);
            assertTrue(controller instanceof GPlatformPanelGroupController);
            controller.updateKeys(f.group, rows(f.one, f.two), new GFormChanges(), 1);
            controller.updateCurrentKey(f.two);
            assertSame(GGroupObjectValue.EMPTY, controller.getSelectedKey());
            assertEquals(-1, controller.getSelectedRow());
            assertSame(GUserOrdersController.NONE, f.userOrdersController());
            assertSame(GUserFiltersController.NONE, f.userFiltersController()); // nothing shows its user filters
            assertNull(field(f.data(), "items")); // a, where its grid is, carries none of its panel properties: no node
            JavaScriptObject items = field(f.snapshots[1], "items"); // b carries one of them
            assertNotNull(field(items, "panel"));
            assertFalse(own(items, "list"));
            assertFalse(own(items, "keys"));
            assertFalse(own(items, "orders"));
            assertNull(field(field(f.sb.controller, "items"), "change"));
            assertNotNull(field(field(f.sb.controller, "items"), "panel"));
        }
    }

    public void testCellUpdateIsIsolatedAndKeepsOldSnapshots() {
        Fixture f = new Fixture();
        JavaScriptObject oldData = f.data(), oldRow = f.row("1"), otherRow = f.row("2");
        JavaScriptObject oldQuantity = field(oldRow, "quantity"), keys = field(f.node(), "keys");
        JavaScriptObject single = field(oldData, "single"), b = f.snapshots[1];
        int placements = f.placements;
        GFormChanges delta = new GFormChanges();
        put(delta, f.price, f.one, 11); put(delta, f.price, f.two, 20);
        f.setValue(f.price, f.one, 11);
        assertSame(oldData, f.data()); // local edits can be batched before publication
        f.projection.flush();
        assertNotSame(oldData, f.data());
        assertNotSame(oldRow, f.row("1"));
        assertEquals(10.0, value(oldRow, "price"), 0.0);
        assertEquals(11.0, value(f.row("1"), "price"), 0.0);
        assertSame(oldQuantity, field(f.row("1"), "quantity"));
        assertSame(otherRow, f.row("2"));
        assertSame(keys, field(f.node(), "keys"));
        assertSame(single, field(f.data(), "single"));
        assertSame(b, f.snapshots[1]);
        assertEquals(1, f.publications[1]); assertEquals(1, f.publications[2]);
        assertEquals(placements, f.placements); // no placement walk in an ordinary delta
        assertEquals(f.one, f.choose(f.row("1")));
        assertEquals(f.one, f.choose(keyOf("1"))); // a key names a row where the rows are drawn
        f.apply(delta); // server confirmation of the same values is a no-op
        assertEquals(2, f.publications[0]);
    }
    private static native JavaScriptObject keyOf(String key) /*-{ return key; }-*/;

    public void testSeveralWritesPublishOnceAndSelectionStaysWithTheRows() {
        Fixture f = new Fixture();
        JavaScriptObject oldRow = f.row("1");
        GFormChanges delta = new GFormChanges();
        put(delta, f.price, f.one, 12); put(delta, f.price, f.two, 20);
        put(delta, f.quantity, f.one, 3); put(delta, f.quantity, f.two, 2);
        f.apply(delta);
        assertEquals(2, f.publications[0]);
        assertEquals(12.0, value(f.row("1"), "price"), 0.0);
        assertEquals(3.0, value(f.row("1"), "quantity"), 0.0);
        assertEquals(1.0, value(oldRow, "quantity"), 0.0);
        JavaScriptObject panel = field(field(f.snapshots[1], "items"), "panel");
        f.controller(f.group).changeCurrentKey(null); // no current row: the rows say so...
        f.projection.flush();
        assertFalse(flag(f.row("1"), "isCurrent"));
        assertSame(panel, field(field(f.snapshots[1], "items"), "panel")); // ... and a panel property stays, value and
        assertTrue(hasMember(f.sb, "items", "panel"));                     // member: the server reads it for the
        assertEquals(1, f.publications[1]);                                // current object itself
        assertEquals(1, f.publications[2]);
        f.controller(f.group).changeCurrentKey(f.two);
        f.projection.flush();
        assertTrue(flag(f.row("2"), "isCurrent"));
        assertEquals(30.0, value(field(f.snapshots[1], "items"), "panel"), 0.0);
        assertEquals(1, f.publications[1]);
    }

    // the shape is design data: an entry is there from the start, and a drop - a SHOWIF, a container's SHOWIF above it
    // - hides it instead of taking it away. Hidden, it has no value, and its cells have none either
    public void testADroppedEntryIsHiddenNotGone() {
        Fixture f = new Fixture();
        JavaScriptObject oldRow = f.row("1");
        assertFalse(own(field(f.node(), "price"), "hidden")); // an attribute like any other: there while it has a value
        f.apply(drop(f.price));
        JavaScriptObject column = field(f.node(), "price");
        assertNotNull(column);
        assertTrue(flag(column, "hidden"));
        assertEquals("Price", text(column, "caption"));
        assertNotNull(field(f.row("1"), "price"));
        assertFalse(hasValue(field(f.row("1"), "price")));
        assertEquals(10.0, value(oldRow, "price"), 0.0);
        assertEquals("price,quantity", join(field(f.node(), "properties"))); // the index is the design's, shown or not
        assertTrue(hasMember(f.sa, "items", "price")); // ... and so is the member
        assertFalse(f.controller(f.price).isPropertyShown(f.price));
        assertTrue(refuses(member(f.sa, "items", "price"), 1)); // which refuses a call while it is hidden
        GFormChanges restore = new GFormChanges(); put(restore, f.price, f.one, 12); put(restore, f.price, f.two, 22);
        f.apply(restore);
        assertFalse(own(field(f.node(), "price"), "hidden"));
        assertEquals(12.0, value(f.row("1"), "price"), 0.0);
        assertEquals(22.0, value(f.row("2"), "price"), 0.0);
        assertTrue(f.controller(f.price).isPropertyShown(f.price));
        assertFalse(refuses(member(f.sa, "items", "price"), 13));
    }

    // a property the form hides when it opens is never sent, and never dropped: it is hidden until its first delivery
    public void testAnEntryIsHiddenUntilItsFirstDelivery() {
        Fixture f = Fixture.withNote();
        JavaScriptObject note = field(f.node(), "note");
        assertTrue(flag(note, "hidden"));
        assertFalse(hasValue(note));
        assertFalse(f.controller(f.note).isPropertyShown(f.note));
        assertTrue(hasMember(f.sa, "items", "note"));
        GFormChanges delta = new GFormChanges(); put(delta, f.note, GGroupObjectValue.EMPTY, 7); f.apply(delta);
        assertFalse(flag(field(f.node(), "note"), "hidden"));
        assertEquals(7.0, value(f.node(), "note"), 0.0);
        assertTrue(f.controller(f.note).isPropertyShown(f.note));
    }

    public void testReorderPreservesRowsAndOptimisticAddRemove() {
        Fixture f = new Fixture();
        JavaScriptObject one = f.row("1"), two = f.row("2");
        JavaScriptObject keys = field(f.node(), "keys");
        GFormChanges order = new GFormChanges(); order.gridObjects.put(f.group, rows(f.two, f.one)); f.apply(order);
        assertSame(one, f.row("1")); assertSame(two, f.row("2"));
        assertNotSame(keys, field(f.node(), "keys"));
        GGroupObjectValue third = new GGroupObjectValue(10, "__proto__");
        f.controller(f.group).modifyGroupObject(third, true, 1); f.projection.flush();
        assertNotNull(f.row("__proto__")); assertTrue(bare(field(f.node(), "byKey")));
        assertEquals(3, length(field(f.node(), "list")));
        assertEquals(third, f.choose(f.row("__proto__")));
        f.setValue(f.price, f.one, 11); f.projection.flush(); // a cell edit copies byKey
        assertTrue(bare(field(f.node(), "byKey"))); assertNotNull(f.row("__proto__")); assertEquals(11.0, value(f.row("1"), "price"), 0.0);
        f.controller(f.group).modifyGroupObject(third, false, -1); f.projection.flush();
        assertNull(f.row("__proto__")); assertEquals(2, length(field(f.node(), "list")));
    }

    public void testColumnUpdateDoesNotCopyRows() {
        Fixture f = new Fixture();
        JavaScriptObject list = field(f.node(), "list"), row = f.row("1");
        GFormChanges delta = new GFormChanges();
        put(delta, f.price.captionReader, GGroupObjectValue.EMPTY, "New price");
        f.apply(delta);
        assertEquals("New price", text(field(f.node(), "price"), "caption"));
        assertSame(list, field(f.node(), "list")); assertSame(row, f.row("1"));
        assertEquals(1, f.publications[1]);
    }

    public void testRowsAndValuesInOneDeltaUseTheFinalState() {
        Fixture f = new Fixture();
        GGroupObjectValue third = new GGroupObjectValue(10, 3);
        JavaScriptObject oldOne = f.row("1");
        // the server re-reads a grid whose keys changed: every row comes with values
        GFormChanges delta = new GFormChanges();
        delta.gridObjects.put(f.group, rows(f.one, third));
        delta.objects.put(f.group, third);
        put(delta, f.price, third, 50); put(delta, f.price, f.one, 15);
        put(delta, f.quantity, third, 5); put(delta, f.quantity, f.one, 1);
        f.apply(delta);
        assertNull(f.row("2"));
        assertEquals(50.0, value(f.row("3"), "price"), 0.0);
        assertEquals(5.0, value(f.row("3"), "quantity"), 0.0);
        assertEquals(15.0, value(f.row("1"), "price"), 0.0);
        assertEquals(10.0, value(oldOne, "price"), 0.0);
        assertTrue(flag(f.row("3"), "isCurrent"));
        assertFalse(flag(f.row("1"), "isCurrent"));
        assertEquals(2, f.publications[0]);
        assertEquals(1, f.publications[2]);
    }

    public void testDescriptorsAndRowAttributesHaveSeparateRecipients() {
        Fixture f = new Fixture();
        JavaScriptObject a = f.data(), nativeEntry = field(f.snapshots[1], "native");
        assertNull(field(field(f.snapshots[1], "items"), "list"));
        assertFalse(flag(nativeEntry, "hidden")); // delivered: the platform draws it, React places it
        GFormChanges caption = new GFormChanges();
        put(caption, f.nativeProperty.captionReader, GGroupObjectValue.EMPTY, "Updated");
        f.apply(caption);
        assertSame(a, f.data());
        assertEquals("Native", text(nativeEntry, "caption"));
        assertEquals("Updated", text(field(f.snapshots[1], "native"), "caption"));
        JavaScriptObject row = f.row("1"), price = field(row, "price");
        GFormChanges selected = new GFormChanges();
        put(selected, f.group.rowSelectReader, f.one, true);
        f.apply(selected);
        assertSame(price, field(f.row("1"), "price"));
        assertNull(field(row, "selected"));
        assertTrue(flag(f.row("1"), "selected"));
        GFormChanges unselected = new GFormChanges();
        put(unselected, f.group.rowSelectReader, f.one, null);
        f.apply(unselected);
        assertNull(field(f.row("1"), "selected"));
        JavaScriptObject node = f.node();
        GFormChanges boxCaption = new GFormChanges();
        put(boxCaption, f.box.captionReader, GGroupObjectValue.EMPTY, "Updated");
        f.apply(boxCaption);
        assertSame(node, f.node());
        assertEquals("Updated", text(field(f.data(), "c4"), "caption"));
        f.apply(drop(f.nativeProperty));
        assertTrue(flag(field(f.snapshots[1], "native"), "hidden"));
        assertEquals("Updated", text(field(f.snapshots[1], "native"), "caption"));
        GFormChanges restore = new GFormChanges(); put(restore, f.nativeProperty, GGroupObjectValue.EMPTY, null); f.apply(restore);
        assertFalse(flag(field(f.snapshots[1], "native"), "hidden"));
    }

    public void testCollidingKeysKeepTheirOwnListPositions() {
        Fixture f = new Fixture();
        GGroupObjectValue aa = new GGroupObjectValue(10, "Aa");
        GGroupObjectValue bb = new GGroupObjectValue(10, "BB");
        GGroupObjectValue c = new GGroupObjectValue(10, "C");
        assertEquals(aa.hashCode(), bb.hashCode());
        GFormChanges rows = new GFormChanges(); rows.gridObjects.put(f.group, rows(aa, bb, c)); f.apply(rows);
        JavaScriptObject oldList = field(f.node(), "list"), oldBB = f.row("BB");
        GFormChanges delta = new GFormChanges(); put(delta, f.price, c, 8); f.apply(delta);
        JavaScriptObject list = field(f.node(), "list");
        assertSame(f.row("Aa"), at(list, 0));
        assertSame(oldBB, at(list, 1));
        assertSame(f.row("C"), at(list, 2));
        assertSame(oldBB, at(oldList, 1));
        assertEquals(8.0, value(at(list, 2), "price"), 0.0);
    }

    // the form level is the EMPTY group, and its node is the container's top level: it carries the empty group's
    // entries - a platform-drawn property's among them, by its integration name, with no member - and lists no
    // `properties`, a name a property of the empty group may take there, and carries the components' descriptors too
    public void testTheTopLevelIsTheEmptyGroupsNode() {
        Fixture f = new Fixture();
        assertNotNull(field(f.data(), "single"));
        assertFalse(own(f.data(), "properties"));
        assertTrue(hasMember(f.sa, null, "single"));
        assertFalse(hasMember(f.sb, null, "single"));
        assertNotNull(field(f.snapshots[1], "native")); // a platform-drawn one is carried by name too...
        assertFalse(hasMember(f.sb, null, "native")); // ... with no member: its value is the platform's
        f.apply(drop(f.single));
        assertTrue(flag(field(f.data(), "single"), "hidden"));
        assertTrue(hasMember(f.sa, null, "single"));
        assertFalse(f.controller(f.single).isPropertyShown(f.single));
    }

    // a platform-drawn panel property's entry - what labels it - is keyed by its integration name on its group's node, where a panel
    // property React draws would be, and carried by name with them, with no member: the platform draws its value. The
    // view names it by that name everywhere - a <Platform> places it by its path, o.note - and the container finds its entry
    // by the property - what hides the host (usePlatform)
    public void testAPlatformDrawnPanelPropertyIsKeyedByItsName() {
        final GPropertyDraw[] placed = new GPropertyDraw[2];
        Fixture f = new Fixture(fixture -> {
            placed[0] = fixture.platformPanel(40, "note", fixture.group, fixture.b);
            placed[1] = fixture.platformPanel(41, "memo", null, fixture.b);
        });
        f.projection.flush();
        JavaScriptObject data = f.snapshots[1], node = field(data, "items");
        JavaScriptObject note = field(node, "note"), memo = field(data, "memo");
        assertEquals("NOTE", text(note, "caption"));
        assertFalse(hasValue(note)); // its value is the platform's
        assertEquals("MEMO", text(memo, "caption")); // the empty group's, on the top level
        assertFalse(own(data, "PROPERTY(note)"));
        assertFalse(own(data, "PROPERTY(memo)"));
        assertEquals("panel,note", join(field(node, "properties")));
        assertFalse(hasMember(f.sb, "items", "note"));
        assertFalse(hasMember(f.sb, null, "memo"));
        assertSame(note, f.sb.entryOf(data, placed[0]));
        assertSame(memo, f.sb.entryOf(data, placed[1]));
        assertEquals("items.note", GReactFormData.getName(placed[0])); // the path of its entry, not PROPERTY(note)
        assertEquals("memo", GReactFormData.getName(placed[1]));
    }

    // a per-row <Platform> names a platform-drawn list property as a panel one is named - its group, then its
    // integration name, the path of its column entry -, found among the groups whose rows this container draws: no
    // other container finds it, and a name with no group, or a property whose cells React draws, names no renderer
    public void testAPerRowPlatformDrawnIsNamedWithItsGroup() {
        final GPropertyDraw[] placed = new GPropertyDraw[1];
        Fixture f = new Fixture(fixture -> {
            placed[0] = fixture.property(44, "memo", fixture.group, true, fixture.a);
            placed[0].platformDrawn = true;
        });
        assertSame(placed[0], f.sa.getRowPlatformProperty("items.memo"));
        assertNull(f.sa.getRowPlatformProperty("memo"));       // no group
        assertNull(f.sa.getRowPlatformProperty("items.price")); // React draws its cells: no renderer
        assertNull(f.sb.getRowPlatformProperty("items.memo"));  // b does not draw the rows
    }

    // ... so two groups whose rows this container draws may share both the name and a row's key: the name says which
    // group, and a row is checked against it by its own objects - another group's row with the same key is none of it
    public void testAPerRowPlatformDrawnIsNamedByItsGroupNotByItsRow() {
        final GPropertyDraw[] memos = new GPropertyDraw[2];
        final GGroupObject[] others = new GGroupObject[1];
        Fixture f = new Fixture(fixture -> {
            GGroupObject other = others[0] = new GGroupObject();
            other.ID = 50; other.nativeSID = "g50"; other.sID = "others"; other.viewType = GClassViewType.LIST;
            other.grid = new GGrid(); other.grid.ID = 51; other.grid.nativeSID = "c51"; other.grid.sID = "otherGrid"; other.grid.container = fixture.a;
            other.grid.groupObject = other;
            fixture.a.children.add(other.grid);
            other.objects.add(new GObject(other, "other", 50, "other", GIntegerType.instance));
            other.rowSelectReader = new GRowSelectReader(50);
            fixture.form.groupObjects.add(other);
            memos[0] = fixture.property(45, "memo", fixture.group, true, fixture.a);
            memos[1] = fixture.property(46, "memo", other, true, fixture.a);
            for (GPropertyDraw memo : memos)
                memo.platformDrawn = true;
        });
        GFormChanges changes = new GFormChanges();
        changes.gridObjects.put(others[0], rows(new GGroupObjectValue(50, 1)));
        f.apply(changes);
        assertSame(memos[0], f.sa.getRowPlatformProperty("items.memo"));
        assertSame(memos[1], f.sa.getRowPlatformProperty("others.memo"));
        GGroupObjectValue itemsRow = GGroupObjectValue.resolveObject(f.row("1"));
        JavaScriptObject othersRows = field(field(f.data(), "others"), "byKey");
        GGroupObjectValue othersRow = GGroupObjectValue.resolveObject(field(othersRows, "1")); // the same key string
        assertNotNull(f.group.getRowKey(itemsRow));
        assertNull(f.group.getRowKey(othersRow)); // what getPlacement checks a row by
        assertNull(GGroupObjectValue.resolveObject(keyOf("1"))); // a key string is no row
    }

    // ... and where nothing else of its group is drawn, it brings the group's node: its entry is carried there, a
    // platform-drawn panel ACTION's too, which says only whether the form shows it, its caption being its button's face
    // - and the group's member is there, with nothing on it. What labels it, and whether the form shows it, are read
    // through the container's answer for the property, from the snapshot handed in
    public void testAPlatformDrawnPanelPropertyBringsItsGroupsNode() {
        final GPropertyDraw[] placed = new GPropertyDraw[1];
        Fixture f = new Fixture(fixture -> placed[0] = fixture.platformPanel(42, "note", fixture.group, fixture.c));
        GPropertyDraw note = placed[0];
        f.projection.flush();
        assertEquals("note", join(field(field(f.snapshots[2], "items"), "properties")));
        assertTrue(own(f.sc.controller, "items"));
        assertFalse(hasMember(f.sc, "items", "note"));
        NativeHashMap<GGroupObjectValue, PValue> caption = new NativeHashMap<>();
        caption.put(GGroupObjectValue.EMPTY, PValue.getPValue("Remark"));
        assertFalse(reachesPlatform(f.platform, f.controllers, note.captionReader, caption)); // React's
        f.projection.flush();
        assertEquals("Remark", text(f.sc.entryOf(f.snapshots[2], note), "caption"));
        GFormChanges show = new GFormChanges();
        put(show, note, f.one, 5);
        f.apply(show);
        assertFalse(flag(f.sc.entryOf(f.snapshots[2], note), "hidden"));
        f.apply(drop(note));
        assertTrue(flag(f.sc.entryOf(f.snapshots[2], note), "hidden"));

        // ... and so does a platform-drawn action, whose caption is its button's face: its entry only says whether it
        // is shown
        final GPropertyDraw[] action = new GPropertyDraw[1];
        Fixture g = new Fixture(fixture -> (action[0] = fixture.platformPanel(43, "go", fixture.group, fixture.c)).valueType = GActionType.instance);
        g.projection.flush();
        assertEquals("go", join(field(field(g.snapshots[2], "items"), "properties")));
        assertTrue(own(g.sc.controller, "items")); // the group's member, as data has its node ...
        assertFalse(hasMember(g.sc, "items", "go")); // ... with nothing on it: the platform's button runs the action
        assertTrue(reachesPlatform(g.platform, g.controllers, action[0].captionReader, caption)); // the platform's
        g.projection.flush();
        assertFalse(own(g.sc.entryOf(g.snapshots[2], action[0]), "caption"));
        assertTrue(flag(g.sc.entryOf(g.snapshots[2], action[0]), "hidden"));
    }

    // the controller is the state's, and its members mirror the entries, made once with them: a group's member wherever
    // a part of it is drawn, its change(row) only where its rows are, a member per value the node carries - none for a
    // platform-drawn property - and a member stays, as its entry does, while the form hides the property
    public void testMembersMirrorTheEntries() {
        Fixture f = new Fixture();
        assertTrue(own(field(f.sa.controller, "items"), "change"));  // the rows are drawn here...
        assertFalse(own(field(f.sb.controller, "items"), "change")); // ... and not here, where only a panel property is
        assertTrue(hasMember(f.sa, "items", "price"));
        assertTrue(hasMember(f.sb, "items", "panel"));
        assertFalse(hasMember(f.sa, "items", "panel"));
        assertTrue(hasMember(f.sa, null, "single")); // the empty group's, on the controller itself
        assertFalse(hasMember(f.sb, null, "native"));
        JavaScriptObject price = field(field(f.sa.controller, "items"), "price");
        f.apply(drop(f.price));
        assertSame(price, field(field(f.sa.controller, "items"), "price"));
    }

    // the form's controllers of what React draws are what React has of it: a group's rows, its current object, the
    // client's own add or remove go to the part the rows are drawn as; a property's optimistic value, and whether the
    // form shows it, to what React has of the property - whatever group it is of, one whose rows React draws or one
    // whose rows GWT draws - and one React carries by no name has the one that keeps nothing: nothing is sent for it.
    // Which controller the form takes is GFormController.initializeControllers', not built here
    public void testTheRoutesAreWhatReactHasOfIt() {
        Fixture f = Fixture.withOuter();
        GGroupController grid = f.controller(f.group);
        grid.updateKeys(f.group, rows(f.two, f.one), new GFormChanges(), 0);
        grid.updateCurrentKey(f.two);
        f.projection.flush();
        assertEquals("2,1", join(field(f.node(), "keys")));
        assertTrue(flag(f.row("2"), "isCurrent"));
        assertSame(f.two, grid.getSelectedKey());
        assertEquals(0, grid.getSelectedRow());
        GFormChanges delta = new GFormChanges(); put(delta, f.outerPanel, GGroupObjectValue.EMPTY, 5); f.apply(delta);
        int published = f.publications[1];
        Pair<GGroupObjectValue, PValue> panel = f.controller(f.panel).setLoadingValueAt(f.panel, GGroupObjectValue.EMPTY, PValue.getPValue(31));
        f.controller(f.outerPanel).setLoadingValueAt(f.outerPanel, GGroupObjectValue.EMPTY, PValue.getPValue(7));
        // a controller writes the draft; the form publishes when the operation is done
        assertEquals(published, f.publications[1]);
        f.projection.flush();
        assertEquals(published + 1, f.publications[1]);
        assertEquals(31.0, value(field(f.snapshots[1], "items"), "panel"), 0.0);
        assertEquals(7.0, value(field(f.snapshots[1], "outer"), "outerPanel"), 0.0);
        assertEquals(GGroupObjectValue.EMPTY, panel.first); // a panel property's one cell
        // a list property's cell is a ROW, asked of its group: the edited row's key, or the current row where there is
        // none
        assertEquals(f.one, f.controller(f.price).setLoadingValueAt(f.price, f.one, PValue.getPValue(11)).first);
        assertEquals(f.two, f.controller(f.price).setLoadingValueAt(f.price, GGroupObjectValue.EMPTY, PValue.getPValue(21)).first);
        assertTrue(f.controller(f.price).isPropertyShown(f.price));
        assertSame(GPropertyController.NONE, f.controller(f.unnamed));
        // a platform-drawn property's value is the platform's, and so is where
        f.controller(f.nativeProperty).focusProperty(f.nativeProperty);
        // it is edited: its controller is the platform's own
        assertTrue(f.platform.focused.contains(f.nativeProperty));
        assertSame(f.platform, f.controller(f.plain)); // ... and one React has nothing of is the platform's alone
        // as a component, a property is the platform's whatever draws it - a platform-drawn one, React labelling it,
        // and one React draws the content of, too (createComponentController)
        assertSame(f.platformComponents, f.controllers.components.get(f.plain));
        assertSame(f.platformComponents, f.controllers.components.get(f.nativeProperty));
        assertSame(f.platformComponents, f.controllers.components.get(f.price));
        assertSame(f.platformComponents, f.controllers.components.get(f.panel));
    }

    // what is sent for a property React draws AS A COMPONENT - its class, where the design gives it a dynamic one; a
    // design showIf on a property is the property's own SHOWIF, never the component's - is the platform's, which has
    // no view of it to apply it to (GFormLayout skips a component it built none for): React's entry is not touched
    public void testAPropertyReactDrawsIsThePlatformsAsAComponent() {
        Fixture f = new Fixture();
        JavaScriptObject before = f.data();
        NativeHashMap<GGroupObjectValue, PValue> highlighted = new NativeHashMap<>();
        highlighted.put(GGroupObjectValue.EMPTY, PValue.getPValue("highlighted"));
        assertTrue(reachesPlatform(f.platform, f.controllers, f.price.elementClassReader, highlighted));
        assertTrue(reachesPlatform(f.platform, f.controllers, f.panel.elementClassReader, highlighted));
        f.projection.flush();
        assertSame(before, f.data());
    }

    // a member holds the state it stands for and reads what a call through it names from that state - the row, the
    // value - handing the form the edit (Verbs)
    public void testAMemberHandsTheFormWhatItNames() {
        Fixture f = new Fixture();
        JavaScriptObject price = member(f.sa, "items", "price");
        changeAt(price, 5, keyOf("2"));
        assertEquals(f.two, f.changed); // a key names a row where the rows are drawn
        changeAt(price, 5, null);
        assertEquals(GGroupObjectValue.EMPTY, f.changed); // no row: the current object
        assertTrue(refusesAt(member(f.sb, "items", "panel"), 1, keyOf("1"))); // a panel entry names no row
    }

    // a platform-drawn ACTION is a part of its group as a platform-drawn property is - its group's node, its entry -,
    // but what labels it is its button's face, the platform's: its entry says only whether the form shows it - a
    // <Platform> hides its host by that -, a column's what the action is too. Like a platform-drawn property, it has no
    // member: the platform's button runs it
    public void testAPlatformDrawnActionHasAnEntryAndNoMember() {
        final GPropertyDraw[] placed = new GPropertyDraw[2];
        Fixture f = new Fixture(fixture -> {
            (placed[0] = fixture.platformPanel(46, "go", fixture.group, fixture.c)).valueType = GActionType.instance;
            GPropertyDraw approve = placed[1] = fixture.property(47, "approve", fixture.group, true, fixture.a);
            approve.platformDrawn = true;
            approve.valueType = GActionType.instance;
            approve.captionReader = new GCaptionReader(47, 10);
        });
        GPropertyDraw go = placed[0], approve = placed[1];
        assertEquals("go", join(field(field(f.snapshots[2], "items"), "properties"))); // c has the group's node for it
        NativeHashMap<GGroupObjectValue, PValue> caption = new NativeHashMap<>();
        caption.put(GGroupObjectValue.EMPTY, PValue.getPValue("Go"));
        assertTrue(reachesPlatform(f.platform, f.controllers, go.captionReader, caption)); // the face of its button
        // ... of each of its buttons
        assertTrue(reachesPlatform(f.platform, f.controllers, approve.captionReader, caption));
        f.projection.flush();
        JavaScriptObject goEntry = f.sc.entryOf(f.snapshots[2], go), approveEntry = field(f.node(), "approve");
        assertFalse(own(goEntry, "caption"));
        assertTrue(flag(goEntry, "hidden")); // the platform does not show it yet
        assertFalse(own(approveEntry, "caption"));
        assertTrue(own(approveEntry, "type")); // what the action is
        assertTrue(flag(approveEntry, "hidden"));
        assertTrue(own(f.sc.controller, "items")); // c has the group's member, as it has data.items ...
        assertFalse(hasMember(f.sc, "items", "go")); // ... and nothing on it: the platform's button runs the action
        assertFalse(hasMember(f.sa, "items", "approve")); // ... nor is there one in a, where the rows are
        GFormChanges shown = new GFormChanges();
        put(shown, go, GGroupObjectValue.EMPTY, true);
        put(shown, approve, f.one, true);
        put(shown, approve, f.two, true);
        f.apply(shown);
        assertFalse(flag(f.sc.entryOf(f.snapshots[2], go), "hidden"));
        assertFalse(flag(field(f.node(), "approve"), "hidden"));
        f.apply(drop(go));
        assertTrue(flag(f.sc.entryOf(f.snapshots[2], go), "hidden"));
    }

    // a call made in a batch reaches the form at once, as outside one - read, checked and handed over in the order it
    // was made, an edit of its own -, the form showing it at once and sending it when the batch ends
    // (GFormController.batch)
    public void testABatchHandsTheFormItsCallsAtOnce() {
        Fixture f = new Fixture();
        JavaScriptObject items = field(f.sa.controller, "items");
        JavaScriptObject price = member(f.sa, "items", "price"), quantity = member(f.sa, "items", "quantity");
        f.edits.clear();
        batch(f.sa.controller, () -> {
            changeAt(price, 5, keyOf("2"));
            assertEquals(Arrays.asList("batch", "cell price@2"), f.edits); // at once
            changeAt(quantity, 6, null);
            callChange(items, keyOf("2"));
        });
        assertEquals(Arrays.asList("batch", "cell price@2", "cell quantity@", "current 2", "sent"), f.edits);
    }

    // a call is made when it is made, so a batch that throws - a call refused, or the view's own throw - ends with what
    // was made before the throw handed to the form, which sends it as the batch ends (GFormController.batch), as calls
    // made one after the other would have gone; the throw reaches the view, and a call made after it goes at once again
    public void testABatchThatThrowsKeepsWhatItMade() {
        Fixture f = new Fixture();
        JavaScriptObject price = member(f.sa, "items", "price"), single = member(f.sa, null, "single");
        f.edits.clear();
        assertTrue(refusesBatch(f.sa.controller, () -> {
            changeAt(price, 5, keyOf("2"));
            changeAt(single, 1, keyOf("1")); // a property of no group names no row: refused
        }));
        assertTrue(refusesBatch(f.sa.controller, () -> {
            changeAt(price, 6, keyOf("1"));
            throw new IllegalStateException("the view's own");
        }));
        changeAt(price, 7, keyOf("2")); // no batch now: at once
        assertEquals(Arrays.asList("batch", "cell price@2", "sent", "batch", "cell price@1", "sent", "cell price@2"), f.edits);
    }

    // a batch begun inside another is a part of it: what it makes is sent when the outer one ends - and if it throws,
    // what it made is sent too, as what a batch makes before a throw is
    public void testABatchInsideABatchIsAPartOfIt() {
        Fixture f = new Fixture();
        JavaScriptObject price = member(f.sa, "items", "price");
        f.edits.clear();
        batch(f.sa.controller, () -> {
            changeAt(price, 1, keyOf("1"));
            batch(f.sa.controller, () -> changeAt(price, 2, keyOf("2")));
            assertTrue(refusesBatch(f.sa.controller, () -> { // the outer function catches what the inner one threw
                changeAt(price, 3, keyOf("1"));
                throw new IllegalStateException("the view's own");
            }));
            changeAt(price, 4, keyOf("2"));
        });
        assertEquals(Arrays.asList("batch", "cell price@1", "cell price@2", "cell price@1", "cell price@2", "sent"), f.edits);
    }

    // a batch is sent when its function returns, so a function returning a Promise - an async one, whose calls after
    // an await would come when the batch is over - is refused, what it made before sent; and no function at all is
    // refused before a batch is begun
    public void testABatchRefusesWhatItCannotMakeAtOnce() {
        Fixture f = new Fixture();
        f.edits.clear();
        String refused = refusedThenable(f.sa.controller, member(f.sa, "items", "price"));
        assertTrue(refused, refused.contains("controller.batch(): ") && refused.contains("Promise"));
        assertEquals(Arrays.asList("batch", "cell price@", "sent"), f.edits);
        refused = refusedBatchOf(f.sa.controller, parse("{\"property\": \"price\"}"));
        assertTrue(refused, refused.contains("takes a function"));
        assertEquals(3, f.edits.size());
    }
    private static native String refusedThenable(JavaScriptObject controller, JavaScriptObject member) /*-{
        try {
            controller.batch(function () { member.change(1); return {then: function () {}}; });
            return null;
        } catch (e) { return e.message; }
    }-*/;
    private static native String refusedBatchOf(JavaScriptObject controller, JavaScriptObject run) /*-{
        try { controller.batch(run); return null; } catch (e) { return e.message; }
    }-*/;

    // a call in a batch reads the state as the calls before it left it - the form shows each at once -: two sortings
    // stated one at a time in one batch go on from each other, as two calls one after the other do, and so do two
    // conditions
    public void testChangesInABatchGoOnFromEachOther() {
        Fixture f = new Fixture(Fixture::filtersBesideRows);
        JavaScriptObject items = field(f.sa.controller, "items");
        batch(f.sa.controller, () -> {
            callChange(field(items, "orders"), parse("{\"property\": \"price\", \"desc\": true}"));
            callChange(field(items, "orders"), parse("{\"property\": \"quantity\", \"desc\": false}"));
            callChange(field(items, "filters"), parse("{\"property\": \"price\", \"compare\": \">\", \"value\": 1}"));
            callChange(field(items, "filters"), parse("{\"property\": \"quantity\", \"value\": 2}"));
        });
        assertEquals(2, f.sortedCalls);
        assertEquals(columns(f.price, f.quantity), new ArrayList<>(f.sorted.keySet())); // the second on the first
        assertEquals(Arrays.asList("changeFilters items price>", "changeFilters items price> quantity="), f.filtersCalls);
    }

    // a react container nothing places draws no caption of its own: the platform draws it around the container, so its
    // own data has no entry for it - while a container it draws has one, and so does a platform-drawn child it places
    public void testAReactContainerHasNoDescriptorOfItsOwn() {
        Fixture f = new Fixture();
        assertFalse(own(f.data(), "c1"));
        assertTrue(own(f.data(), "c4"));
        assertEquals("Box", text(field(f.data(), "c4"), "caption"));
        assertFalse(flag(field(f.data(), "c4"), "hidden"));
    }

    // a container's SHOWIF, as the server sends it for one a react view is told about (true: hidden), is its
    // descriptor's `hidden` - the descriptor stays, as every entry does
    public void testAContainerDescriptorFollowsItsShowIf() {
        Fixture f = new Fixture();
        GFormChanges hide = new GFormChanges(); put(hide, f.box.showIfReader, GGroupObjectValue.EMPTY, true); f.apply(hide);
        assertTrue(flag(field(f.data(), "c4"), "hidden"));
        assertEquals("Box", text(field(f.data(), "c4"), "caption"));
        GFormChanges show = new GFormChanges(); put(show, f.box.showIfReader, GGroupObjectValue.EMPTY, null); f.apply(show);
        assertFalse(own(field(f.data(), "c4"), "hidden"));
    }

    // the component drawing the rows is a component like any other: its descriptor, keyed by its SID, says the form
    // hides it, and its rows stay as the server last sent them - as the platform's own grid keeps them. The columns go
    // the way any property does: the server drops what it does not show
    public void testAHiddenGridSaysSoAndKeepsItsRows() {
        Fixture f = new Fixture();
        JavaScriptObject grid = field(f.data(), "grid");
        assertNotNull(grid);
        assertFalse(own(grid, "hidden"));
        assertFalse(own(grid, "caption")); // a grid has no caption: its descriptor says only whether it is shown
        assertFalse(own(f.sa.controller, "grid")); // ... and it is data only, no member
        GFormChanges hide = new GFormChanges(); put(hide, f.group.grid.showIfReader, GGroupObjectValue.EMPTY, true); f.apply(hide);
        assertTrue(flag(field(f.data(), "grid"), "hidden"));
        assertEquals(2, length(field(f.node(), "list")));
        assertEquals(10.0, value(f.row("1"), "price"), 0.0);
        f.apply(drop(f.price, f.quantity));
        assertEquals(2, length(field(f.node(), "list")));
        assertFalse(hasValue(field(f.row("1"), "price")));
        GFormChanges show = new GFormChanges(); put(show, f.group.grid.showIfReader, GGroupObjectValue.EMPTY, null); f.apply(show);
        assertFalse(own(field(f.data(), "grid"), "hidden"));
    }

    // a grid's record has no container of its own: it is where its grid is, so a react container in the record of a
    // grid the platform draws is built, and projects what it holds - its properties, and the descriptors of its
    // components
    public void testAReactContainerInAGridRecordProjectsWhatItHolds() {
        Fixture f = Fixture.withOuter(); // `outer`: a group GWT draws, its grid in the main container
        GContainer record = container(60);
        record.react = false; record.custom = null;
        f.outer.grid.record = record; // the record is not a child of anything: the grid holds it
        record.recordContainer = f.outer.grid;
        GContainer details = container(61);
        details.container = record; record.children.add(details);
        GPropertyDraw note = f.property(62, "recordNote", f.outer, false, details);
        GContainer box = container(63);
        box.react = false; box.custom = null; box.caption = "In record";
        box.container = details; details.children.add(box);
        RecordingTableController platform = new RecordingTableController();
        GReactFormData projection = new GReactFormData(f.form, GComponent::getReactPlace, f.verbs);
        final JavaScriptObject[] published = new JavaScriptObject[1];
        projection.addView(f.a, data -> { });
        projection.addView(f.b, data -> { });
        projection.addView(f.c, data -> { });
        projection.addView(details, data -> published[0] = data); // GFormLayout builds the record's containers too
        Controllers controllers = new Controllers(projection, f.form, platform, new RecordingComponentController(platform));
        NativeHashMap<GGroupObjectValue, PValue> values = new NativeHashMap<>();
        values.put(GGroupObjectValue.EMPTY, PValue.getPValue(5));
        assertFalse(reachesPlatform(platform, controllers, note, values));
        projection.flush();
        assertEquals(5.0, value(field(published[0], "outer"), "recordNote"), 0.0);
        assertEquals("In record", text(field(published[0], "c63"), "caption"));
        assertFalse(own(published[0], "c61")); // the react container itself: nothing places it
    }

    // a platform-drawn grid property is a column React draws over the editors the platform draws in its rows: an ordinary column
    // entry, listed in `properties`, with no cells and no member - its values are the platform's
    public void testAPlatformDrawnColumnIsAnOrdinaryColumn() {
        Fixture f = new Fixture();
        GPropertyDraw editor = f.property(27, "editor", f.group, true, f.a); // added before a new projection is made
        editor.platformDrawn = true;
        editor.caption = "Editor";
        editor.captionReader = new GCaptionReader(27, 10); // every property has one: the static caption is its fallback
        RecordingTableController platform = new RecordingTableController();
        GReactFormData projection = new GReactFormData(f.form, GComponent::getReactPlace, f.verbs);
        final JavaScriptObject[] published = new JavaScriptObject[1];
        GReactFormData.ViewState sa = projection.addView(f.a, data -> published[0] = data);
        projection.addView(f.b, data -> { });
        projection.addView(f.c, data -> { });
        Controllers controllers = new Controllers(projection, f.form, platform, new RecordingComponentController(platform));
        controllers.groups.get(f.group).updateKeys(f.group, rows(f.one), null, 0);
        projection.flush();
        // hidden until the platform shows it:
        assertTrue(flag(field(field(published[0], "items"), "editor"), "hidden"));
        NativeHashMap<GGroupObjectValue, PValue> values = new NativeHashMap<>();
        values.put(f.one, PValue.getPValue(5));
        // its whole delivery, handed on to the platform's editors, which draw the value, theirs alone
        assertTrue(reachesPlatform(platform, controllers, editor, values));
        projection.flush();
        JavaScriptObject node = field(published[0], "items");
        JavaScriptObject column = field(node, "editor");
        assertEquals("Editor", text(column, "caption"));
        assertFalse(flag(column, "hidden"));
        assertFalse(own(field(field(node, "byKey"), "1"), "editor"));
        assertTrue(join(field(node, "properties")).contains("editor"));
        assertFalse(hasMember(sa, "items", "editor"));
    }
    // ... and its value in the current row is asked of the rows as any other column's - by the platform's filter
    // controller over them, the cell a new condition starts on: the platform's, which draws its cells, so an edit's
    // value is there before the server answers - none of it projected. With no row current the rows are on none
    public void testAPlatformDrawnColumnsValueIsAskedOfTheRows() {
        final GPropertyDraw[] editor = new GPropertyDraw[1];
        Fixture f = new Fixture(fixture -> {
            editor[0] = fixture.property(27, "editor", fixture.group, true, fixture.a);
            editor[0].platformDrawn = true;
            editor[0].captionReader = new GCaptionReader(27, 10);
        });
        NativeHashMap<GGroupObjectValue, PValue> values = new NativeHashMap<>();
        values.put(f.one, PValue.getPValue(5));
        values.put(f.two, PValue.getPValue(7));
        assertTrue(reachesPlatform(f.platform, f.controllers, editor[0], values));
        GTableController rows = f.controllers.gridRows.get(f.group);
        f.controller(f.group).changeCurrentKey(null);
        assertEquals(-1, rows.getSelectedRow());

        f.controller(f.group).updateCurrentKey(f.two);
        assertEquals(1, rows.getSelectedRow());
        assertEquals(PValue.getPValue(7), rows.getSelectedValue(editor[0], GGroupObjectValue.EMPTY));
        // typed in the platform's editor, not yet answered
        f.platform.cells.get(editor[0]).put(f.two, PValue.getPValue(9));
        assertEquals(PValue.getPValue(9), rows.getSelectedValue(editor[0], GGroupObjectValue.EMPTY));
        f.projection.flush();
        assertFalse(own(f.row("2"), "editor"));
    }

    public void testValueBeforeItsRowIsRetainedWithoutPublication() {
        Fixture f = new Fixture();
        GGroupObjectValue third = new GGroupObjectValue(10, 3);
        JavaScriptObject before = f.data();
        GFormChanges value = new GFormChanges(); value.updateProperties.add(f.price); put(value, f.price, third, 42); f.apply(value);
        assertSame(before, f.data());
        f.controller(f.group).modifyGroupObject(third, true, -1);
        f.projection.flush();
        assertEquals(42.0, value(f.row("3"), "price"), 0.0);
        assertSame(field(field(field(before, "items"), "byKey"), "2"), f.row("2"));
    }

    public void testMembershipAndCellEditsShareOneDraft() {
        Fixture f = new Fixture();
        JavaScriptObject before = f.data(), oldOne = f.row("1");
        GGroupObjectValue third = new GGroupObjectValue(10, 3);
        f.controller(f.group).modifyGroupObject(third, true, 0);
        f.setValue(f.price, f.one, 15);
        f.controller(f.group).modifyGroupObject(f.two, false, -1);
        f.setValue(f.quantity, f.one, 7);
        f.setValue(f.price, third, 99);
        assertSame(before, f.data());
        f.projection.flush();
        JavaScriptObject list = field(f.node(), "list");
        assertEquals(2, length(list));
        assertSame(f.row("3"), at(list, 0)); assertSame(f.row("1"), at(list, 1));
        assertNull(f.row("2"));
        assertEquals(15.0, value(f.row("1"), "price"), 0.0);
        assertEquals(7.0, value(f.row("1"), "quantity"), 0.0);
        assertEquals(99.0, value(f.row("3"), "price"), 0.0);
        assertEquals(10.0, value(oldOne, "price"), 0.0);
        assertEquals(1.0, value(oldOne, "quantity"), 0.0);
        assertEquals(2, f.publications[0]);
    }

    public void testSubscriberEditStartsANewDraft() {
        Fixture f = new Fixture();
        final int[] callbacks = {0};
        f.onPublish = () -> {
            if (callbacks[0]++ == 0)
                f.setValue(f.price, f.one, 13);
        };
        GFormChanges delta = new GFormChanges(); put(delta, f.quantity, f.one, 3); put(delta, f.quantity, f.two, 2); f.apply(delta);
        JavaScriptObject first = f.row("1");
        assertEquals(10.0, value(first, "price"), 0.0);
        assertEquals(3.0, value(first, "quantity"), 0.0);
        f.projection.flush();
        assertEquals(13.0, value(f.row("1"), "price"), 0.0);
        assertEquals(10.0, value(first, "price"), 0.0);
        assertEquals(3, f.publications[0]);
        assertEquals(1, f.publications[1]);
    }

    public void testDispatchKeepsNativeValuesAndProjectsTheirDescriptor() {
        Fixture f = new Fixture();
        NativeHashMap<GGroupObjectValue, PValue> values = new NativeHashMap<>();
        values.put(GGroupObjectValue.EMPTY, PValue.getPValue(17));
        assertTrue(reachesPlatform(f.platform, f.controllers, f.nativeProperty, values));
        assertTrue(reachesPlatform(f.platform, f.controllers, new GBackgroundReader(24, -1), values));
        values.put(GGroupObjectValue.EMPTY, PValue.getPValue("Caption"));
        assertFalse(reachesPlatform(f.platform, f.controllers, f.nativeProperty.captionReader, values));
        f.projection.flush();
        assertEquals("Caption", text(field(f.snapshots[1], "native"), "caption"));
        assertFalse(hasValue(field(f.snapshots[1], "native"))); // its value is the platform's: its descriptor has none
        assertEquals(1, f.publications[0]);
    }

    // a drop goes where the property's values go: one React draws is dropped here alone - the platform has no view of
    // it - and a platform-drawn one, like one nothing places, to the platform alone, which draws it: what React labels
    // of it hides with the drop, which comes through its entry on its way to the platform
    public void testADropGoesWhereTheValuesGo() {
        Fixture f = new Fixture();
        assertFalse(dropReachesPlatform(f.platform, f.controllers, f.panel));
        assertTrue(dropReachesPlatform(f.platform, f.controllers, f.nativeProperty));
        assertTrue(dropReachesPlatform(f.platform, f.controllers, f.plain));
        f.projection.flush();
        assertTrue(flag(field(field(f.snapshots[1], "items"), "panel"), "hidden"));
        assertTrue(flag(field(f.snapshots[1], "native"), "hidden"));
    }

    // a reader of a component React draws, with no field to project, is still React's: the platform has no view for
    // it. A react container's OWN readers are the platform's - it draws the container's frame, caption and SHOWIF
    public void testReactReadersWithoutProjectedFieldsStillSkipNativeViews() {
        Fixture f = new Fixture();
        JavaScriptObject before = f.data();
        NativeHashMap<GGroupObjectValue, PValue> values = new NativeHashMap<>();
        values.put(GGroupObjectValue.EMPTY, null);
        assertFalse(reachesPlatform(f.platform, f.controllers, f.box.elementClassReader, values));
        assertTrue(reachesPlatform(f.platform, f.controllers, f.a.showIfReader, values));
        assertTrue(reachesPlatform(f.platform, f.controllers, f.a.captionReader, values));
        assertTrue(reachesPlatform(f.platform, f.controllers, f.form.mainContainer.showIfReader, values));
        f.projection.flush();
        assertSame(before, f.data());
        assertEquals(1, f.publications[0]);
    }

    // a change a view sent of a cell: the cell says `loading` until the answer to it. The form marks it through the
    // property's controller (setLoadingValueAt: with the value it shows, or with its own for UNDEFINED) and takes it
    // off with the loading reader - null for the cells the answer is to, the others' kept. What React draws it marks;
    // what the platform draws, the platform's own controller
    public void testACellSaysItIsLoadingUntilItsChangeIsAnswered() {
        Fixture f = new Fixture();
        assertFalse(own(field(f.row("1"), "price"), "loading"));
        // nothing to show: the cell, keyed as the answer keys it, and its value before
        Pair<GGroupObjectValue, PValue> cell = f.controller(f.price).setLoadingValueAt(f.price, f.one, PValue.UNDEFINED);
        assertEquals(f.one, cell.first);
        assertEquals(PValue.getPValue(10), cell.second);
        assertEquals(f.two, f.controller(f.price).setLoadingValueAt(f.price, f.two, PValue.UNDEFINED).first);
        f.projection.flush();
        assertTrue(flag(field(f.row("1"), "price"), "loading"));
        assertEquals(10.0, value(f.row("1"), "price"), 0.0); // its own value, kept
        assertTrue(flag(field(f.row("2"), "price"), "loading"));
        assertFalse(own(field(f.row("1"), "quantity"), "loading"));
        assertFalse(own(field(f.node(), "price"), "loading")); // a cell's, not the column's
        GFormChanges answer = new GFormChanges(); // the answer to the first: as the form makes it
        put(answer, f.price.loadingReader, f.one, null);
        answer.updateProperties.add(f.price.loadingReader);
        NativeHashMap<GGroupObjectValue, PValue> answered = answer.properties.get(f.price.loadingReader);
        f.apply(answer);
        assertFalse(own(field(f.row("1"), "price"), "loading"));
        assertTrue(flag(field(f.row("2"), "price"), "loading"));
        assertFalse(reachesPlatform(f.platform, f.controllers, f.price.loadingReader, answered)); // React's alone
        // ... and one with a value to show says it with the value
        f.setValue(f.quantity, f.one, 7);
        f.projection.flush();
        assertEquals(7.0, value(f.row("1"), "quantity"), 0.0);
        assertTrue(flag(field(f.row("1"), "quantity"), "loading"));

        // a panel entry is its one cell: an action's says so while it runs
        assertEquals(GGroupObjectValue.EMPTY, f.controller(f.panel).setLoadingValueAt(f.panel, GGroupObjectValue.EMPTY, PValue.UNDEFINED).first);
        f.projection.flush();
        assertTrue(flag(field(field(f.snapshots[1], "items"), "panel"), "loading"));
        // dropped, its loading goes with its values - no answer takes it off then - so it does not come back with it
        f.apply(drop(f.panel));
        GFormChanges back = new GFormChanges();
        put(back, f.panel, GGroupObjectValue.EMPTY, 30);
        f.apply(back);
        assertFalse(own(field(field(f.snapshots[1], "items"), "panel"), "loading"));

        // the platform's: its own controller marks it - a platform-drawn property's included, React handing it over -,
        // and it is handed the answer
        f.controller(f.nativeProperty).setLoadingValueAt(f.nativeProperty, GGroupObjectValue.EMPTY, PValue.UNDEFINED);
        f.controller(f.plain).setLoadingValueAt(f.plain, GGroupObjectValue.EMPTY, PValue.UNDEFINED);
        assertEquals(Arrays.asList(f.nativeProperty, f.plain), f.platform.loading);
        assertTrue(reachesPlatform(f.platform, f.controllers, f.nativeProperty.loadingReader, answered));
    }

    public void testDroppedPropertyComesBackWithItsValues() {
        Fixture f = new Fixture();
        f.apply(drop(f.price, f.nativeProperty));
        assertTrue(flag(field(f.node(), "price"), "hidden"));
        assertFalse(hasValue(field(f.row("1"), "price")));
        assertTrue(flag(field(f.snapshots[1], "native"), "hidden"));
        GFormChanges restore = new GFormChanges();
        put(restore, f.price, f.one, 12); put(restore, f.price, f.two, 22);
        put(restore, f.nativeProperty, GGroupObjectValue.EMPTY, 16);
        f.apply(restore);
        assertEquals(12.0, value(f.row("1"), "price"), 0.0);
        assertEquals(22.0, value(f.row("2"), "price"), 0.0);
        assertEquals("Price", text(field(f.node(), "price"), "caption"));
        assertEquals("Native", text(field(f.snapshots[1], "native"), "caption"));
        assertFalse(flag(field(f.snapshots[1], "native"), "hidden"));
    }

    // each delivery is the server's WHOLE set of a reader's values, so it replaces what was kept - a row that left
    // takes its values with it without anything forgetting them - while one the client marks partial (its own
    // reconciliation) is merged into them
    public void testADeliveryReplacesAndAPartialOneMerges() {
        Fixture f = new Fixture();
        GGroupObjectValue three = new GGroupObjectValue(10, 3);
        GFormChanges page = new GFormChanges();
        page.gridObjects.put(f.group, rows(f.two, three));
        put(page, f.price, f.two, 20); put(page, f.price, three, 30);
        f.apply(page);
        GFormChanges back = new GFormChanges(); // the row that left comes back, with no value delivered for it
        back.gridObjects.put(f.group, rows(f.one, f.two, three));
        f.apply(back);
        assertTrue(noValue(field(f.row("1"), "price"))); // its value left with it: nothing kept brings it back
        assertEquals(30.0, value(f.row("3"), "price"), 0.0);
        GFormChanges partial = new GFormChanges();
        partial.updateProperties.add(f.price);
        put(partial, f.price, three, 33);
        f.apply(partial);
        assertEquals(20.0, value(f.row("2"), "price"), 0.0); // kept: a partial delivery says nothing about the others
        assertEquals(33.0, value(f.row("3"), "price"), 0.0);
        GFormChanges whole = new GFormChanges();
        put(whole, f.price, three, 34);
        f.apply(whole);
        assertTrue(noValue(field(f.row("2"), "price"))); // replaced: the server no longer has a value for it
        assertEquals(34.0, value(f.row("3"), "price"), 0.0);
    }

    public void testGroupWithoutAReactGridKeepsNeitherRowsNorSelection() {
        Fixture f = Fixture.withOuter();
        GGroupObjectValue seven = new GGroupObjectValue(50, 7), eight = new GGroupObjectValue(50, 8);
        GFormChanges delta = new GFormChanges();
        delta.gridObjects.put(f.outer, rows(seven, eight));
        delta.objects.put(f.outer, eight);
        put(delta, f.outerPanel, GGroupObjectValue.EMPTY, 5);
        f.apply(delta);
        // GWT draws these rows and holds their selection: its rows, its add and remove, go to the platform's grid
        assertSame(f.platform, f.controller(f.outer));
        assertEquals(5.0, value(field(f.snapshots[1], "outer"), "outerPanel"), 0.0); // ... and the panel in b is shown
        // no row is chosen there: the group's current object is the platform grid's
        assertNull(field(field(f.snapshots[1], "outer"), "list"));
        NativeHashMap<GGroupObjectValue, PValue> names = new NativeHashMap<>();
        names.put(seven, PValue.getPValue(1));
        // a column of those rows is GWT's too
        assertTrue(reachesPlatform(f.platform, f.controllers, f.outerName, names));
    }

    // the rows a group's per-row renderers follow (GPlatformRowsPanelController): handed out once, while the form is
    // built, they are the node's own list - the server's rows and a view's own add or remove alike -, and reading them
    // asks no placement
    public void testTheRowsRenderersFollowAreTheNodesOwn() {
        Fixture f = new Fixture(); // its form, drawn again by a projection of its own, no row delivered yet
        RecordingTableController platform = new RecordingTableController();
        int[] placements = new int[1];
        GReactFormData projection = new GReactFormData(f.form, component -> {
            placements[0]++;
            return component.getReactPlace();
        }, f.verbs);
        projection.addView(f.a, data -> { });
        projection.addView(f.b, data -> { });
        projection.addView(f.c, data -> { });
        Controllers controllers = new Controllers(projection, f.form, platform, new RecordingComponentController(platform));
        GReactFormData.Rows rows = projection.getRows(f.group); // bound once, while the form is built
        int placed = placements[0];
        ArrayList<GGroupObjectValue> none = rows.getRows();
        assertTrue(none.isEmpty());
        assertSame(none, rows.getRows()); // the node's own list, before any delivery too
        GGroupController group = controllers.groups.get(f.group);
        group.updateKeys(f.group, rows(), null, 0); // an empty first delivery changes nothing
        assertSame(none, rows.getRows());
        group.updateKeys(f.group, rows(f.one, f.two), null, 0);
        ArrayList<GGroupObjectValue> delivered = rows.getRows();
        assertEquals(rows(f.one, f.two), delivered);
        assertSame(delivered, rows.getRows());
        assertTrue(none.isEmpty()); // a new list in place: the one handed out says what it said
        group.modifyGroupObject(new GGroupObjectValue(10, 3), true, -1); // a view's own add
        assertEquals(3, rows.getRows().size());
        assertEquals(rows(f.one, f.two), delivered);
        group.updateKeys(f.group, rows(f.two), null, 0);
        assertEquals(rows(f.two), rows.getRows());
        assertEquals(placed, placements[0]); // reading them asks no placement
    }

    // what a delta brings for a reader goes to what React has of its owner, which was settled when the form was built:
    // no delta asks the design where anything is
    public void testADeltaAsksNoPlacement() {
        Fixture f = new Fixture();
        NativeHashMap<GGroupObjectValue, PValue> values = new NativeHashMap<>();
        values.put(GGroupObjectValue.EMPTY, PValue.getPValue(1));
        int placed = f.placements;
        assertTrue(reachesPlatform(f.platform, f.controllers, f.plain, values)); // React has nothing of it
        // ... nor of any reader of it
        assertTrue(reachesPlatform(f.platform, f.controllers, f.plain.captionReader, values));
        assertEquals(placed, f.placements);
    }

    // a reader goes to ONE place: what React has of its owner - the property, the group, the component it is of - told
    // what kind of reader it is, or the platform, where React takes nothing of it. A platform-drawn column's values and
    // drop go through its entry to the platform (an unnamed one is refused when the form is built); a platform-drawn
    // ACTION's caption is its button's face, the platform's, its entry saying only whether it is shown; a
    // platform-drawn container React places has its caption drawn by React - its caption's class goes on to the
    // platform, named or not, which has no caption widget to put it on - and its SHOWIF is React's alone: a view hides
    // its host (usePlatform)
    public void testAReaderGoesToWhatReactHasOfItsOwner() {
        Fixture f = new Fixture();
        GPropertyDraw column = f.property(32, "column", f.group, true, f.a);
        column.platformDrawn = true;
        GPropertyDraw button = f.property(33, "button", null, false, f.b);
        button.platformDrawn = true;
        button.valueType = GActionType.instance;
        button.captionReader = new GCaptionReader(33, -1);
        GContainer placed = container(34), unnamed = container(35);
        for (GContainer box : Arrays.asList(placed, unnamed)) {
            box.react = false; box.custom = null; box.platformDrawn = true;
            box.container = f.b; f.b.children.add(box);
        }
        unnamed.sID = null;
        RecordingTableController platform = new RecordingTableController();
        GReactFormData projection = new GReactFormData(f.form, GComponent::getReactPlace, f.verbs);
        final JavaScriptObject[] published = new JavaScriptObject[1];
        projection.addView(f.a, data -> { });
        projection.addView(f.b, data -> published[0] = data);
        projection.addView(f.c, data -> { });
        Controllers controllers = new Controllers(projection, f.form, platform, new RecordingComponentController(platform));
        NativeHashMap<GGroupObjectValue, PValue> values = new NativeHashMap<>();
        values.put(GGroupObjectValue.EMPTY, PValue.getPValue("Go"));
        // a platform-drawn column's values go through its entry...
        assertTrue(reachesPlatform(platform, controllers, column, values));
        assertTrue(dropReachesPlatform(platform, controllers, column));     // ... and so does its drop, to the platform
        // the platform's alone: its button's face
        assertTrue(reachesPlatform(platform, controllers, button.captionReader, values));
        assertFalse(reachesPlatform(platform, controllers, placed.captionReader, values));
        assertTrue(reachesPlatform(platform, controllers, placed.captionClassReader, values));
        assertTrue(reachesPlatform(platform, controllers, placed.valueClassReader, values));
        assertTrue(reachesPlatform(platform, controllers, unnamed.captionClassReader, values));
        assertTrue(reachesPlatform(platform, controllers, unnamed.captionReader, values));
        NativeHashMap<GGroupObjectValue, PValue> hidden = new NativeHashMap<>();
        hidden.put(GGroupObjectValue.EMPTY, PValue.getPValue(true));
        assertFalse(reachesPlatform(platform, controllers, placed.showIfReader, hidden)); // React's alone
        projection.flush();
        assertFalse(own(field(published[0], "button"), "caption")); // a platform-drawn ACTION's entry says only whether the form
        assertTrue(flag(field(published[0], "button"), "hidden"));   // shows it
        assertEquals("Go", text(field(published[0], "c34"), "caption"));
        assertTrue(flag(field(published[0], "c34"), "hidden"));
    }

    // a form with no react container has its projection too, an empty one: it places nothing and takes none of the
    // readers - not even one that would be React's if its container's state were made - and walks no placement, at its
    // start or with a delta
    public void testAProjectionWithNoReactContainerTakesNothing() {
        Fixture f = new Fixture(); // its form, and this time no react container's state
        int[] placements = new int[1];
        RecordingTableController platform = new RecordingTableController();
        GReactFormData projection = new GReactFormData(f.form, component -> {
            placements[0]++;
            return component.getReactPlace();
        }, f.verbs);
        Controllers controllers = new Controllers(projection, f.form, platform, new RecordingComponentController(platform));
        NativeHashMap<GGroupObjectValue, PValue> values = new NativeHashMap<>();
        values.put(f.one, PValue.getPValue(5));
        assertTrue(reachesPlatform(platform, controllers, f.price, values));
        assertTrue(reachesPlatform(platform, controllers, f.box.elementClassReader, values));
        assertTrue(reachesPlatform(platform, controllers, f.group.rowSelectReader, values));
        assertSame(platform, controllers.groups.get(f.group)); // its rows go to the platform's grid
        assertTrue(dropReachesPlatform(platform, controllers, f.price));
        projection.flush();
        assertSame(platform, controllers.properties.get(f.price)); // its values go to the platform too
        assertEquals(0, placements[0]); // nothing is walked: there is nowhere to be placed
    }

    public void testRowsAndAPanelPropertyShareTheGridNode() {
        Fixture f = Fixture.withNote();
        GFormChanges delta = new GFormChanges();
        put(delta, f.note, GGroupObjectValue.EMPTY, 7);
        f.apply(delta);
        assertEquals(7.0, value(f.node(), "note"), 0.0); // one node: the rows and the panel entry side by side
        assertEquals(2, length(field(f.node(), "list")));
        assertTrue(hasMember(f.sa, "items", "note"));
        JavaScriptObject note = field(f.node(), "note");
        f.controller(f.group).changeCurrentKey(f.two);
        f.projection.flush();
        assertTrue(flag(f.row("2"), "isCurrent"));
        assertSame(note, field(f.node(), "note")); // the selection moved in the rows, and the panel entry stayed
    }

    // a container carries an entry for what it draws or places, and a member for what it draws, and for no other
    // property of the group - one carried in another container is not on its node -, while a state names the group's
    // property wherever that is drawn (testASortingNamesThePropertyOfItsGroupWhereverItIsDrawn)
    public void testAContainerCarriesWhatItDraws() {
        Fixture f = new Fixture(); // `price`: a column where the rows are, in a; `panel`: a panel property in b
        assertTrue(hasMember(f.sa, "items", "price"));
        assertTrue(hasMember(f.sb, "items", "panel"));
        assertFalse(own(f.node(), "panel")); // a does not carry the panel property
        assertFalse(hasMember(f.sa, "items", "panel"));
        assertFalse(hasMember(f.sb, "items", "price")); // ... nor b the column
        assertFalse(own(f.sc.controller, "items")); // nor has a container with no node of it a member of the group
        assertNotNull(field(f.node(), "list")); // a draws the rows ...
        assertNull(field(field(f.snapshots[1], "items"), "list")); // ... b only a panel property of the group
    }

    // a group's node is data's OWN field: a group named like a member of Object.prototype gets a node of its own, a
    // plain object, and not a copy of what a plain `{}` answers for that name
    public void testAGroupNamedLikeAnObjectPrototypeMemberHasItsOwnNode() {
        Fixture f = new Fixture();
        f.group.sID = "constructor";
        RecordingTableController platform = new RecordingTableController();
        GReactFormData projection = new GReactFormData(f.form, GComponent::getReactPlace, f.verbs);
        final JavaScriptObject[] published = new JavaScriptObject[1];
        projection.addView(f.a, data -> published[0] = data);
        projection.addView(f.b, data -> { });
        projection.addView(f.c, data -> { });
        Controllers controllers = new Controllers(projection, f.form, platform, new RecordingComponentController(platform));
        controllers.groups.get(f.group).updateKeys(f.group, rows(f.one, f.two), null, 0);
        projection.flush();
        JavaScriptObject node = field(published[0], "constructor");
        assertTrue(plain(node));
        assertEquals("1,2", join(field(node, "keys")));
        assertEquals("price,quantity", join(field(node, "properties")));
    }

    // the client's own reconciliation marks its delivery partial: it puts back what the server did not send, and is no
    // arrival - a dropped property stays hidden until the server delivers it whole
    public void testAPartialDeliveryIsNoArrival() {
        Fixture f = new Fixture();
        f.apply(drop(f.price));
        GFormChanges partial = new GFormChanges(); put(partial, f.price, f.one, 12); partial.updateProperties.add(f.price);
        f.apply(partial);
        assertTrue(flag(field(f.node(), "price"), "hidden"));
        assertFalse(f.controller(f.price).isPropertyShown(f.price));
        assertFalse(hasValue(field(f.row("1"), "price")));
        GFormChanges whole = new GFormChanges(); put(whole, f.price, f.one, 12); put(whole, f.price, f.two, 22);
        f.apply(whole);
        assertTrue(f.controller(f.price).isPropertyShown(f.price));
        assertEquals(12.0, value(f.row("1"), "price"), 0.0);
    }

    // a subscriber may flush from inside a publication (a member call does): what it changed goes out then, with what
    // the outer flush had committed for it, and the outer flush does not give that container out again
    public void testANestedFlushPublishesWhatChangedOnce() {
        Fixture f = Fixture.withRemark(); // b's two properties: the outer batch changes one, the nested flush the other
        GFormChanges first = new GFormChanges(); put(first, f.remark, GGroupObjectValue.EMPTY, 7); f.apply(first);
        final int[] callbacks = {0};
        f.onPublish = () -> {
            if (callbacks[0]++ == 0) { // during a's publication: b, still in the outer batch, is changed and flushed
                f.setValue(f.remark, GGroupObjectValue.EMPTY, 13);
                f.projection.flush();
            }
        };
        GFormChanges delta = new GFormChanges(); put(delta, f.quantity, f.one, 3); put(delta, f.panel, GGroupObjectValue.EMPTY, 31); f.apply(delta);
        assertEquals(3.0, value(f.row("1"), "quantity"), 0.0);
        // the nested flush gave b out, with both changes: the outer batch's and its own
        assertEquals(31.0, value(field(f.snapshots[1], "items"), "panel"), 0.0);
        assertEquals(13.0, value(field(f.snapshots[1], "items"), "remark"), 0.0);
        assertEquals(2, f.publications[0]); // the fixture's own, the delta's
        // the fixture's own, remark's first value, the nested flush's: the outer flush did not give b out again
        assertEquals(3, f.publications[1]);
    }

    public void testATreeRowSaysWhereItHangs() {
        TreeFixture f = new TreeFixture();
        f.apply(f.page());
        assertTrue(has(f.row(f.cat, f.c1), "parent")); // a root states that it is one
        assertNull(field(f.row(f.cat, f.c1), "parent"));
        assertEquals(1.0, number(f.row(f.item, f.i15), "parent"), 0.0); // the way a key is written: the number
        assertEquals(2.0, number(f.row(f.item, f.i27), "parent"), 0.0);
        assertTrue(sameField(f.row(f.item, f.i27), "parent", f.row(f.cat, f.c2), "key")); // ===, not merely alike
        JavaScriptObject c1 = f.row(f.cat, f.c1), i15 = f.row(f.item, f.i15);
        int publications = f.publications;
        f.apply(f.page()); // the same rows and hierarchy again: nothing is rewritten, nothing published
        assertSame(c1, f.row(f.cat, f.c1));
        assertSame(i15, f.row(f.item, f.i15));
        assertEquals(publications, f.publications);
        GFormChanges broken = new GFormChanges(); // parents that are not parallel to the rows: no parent is stated
        broken.gridObjects.put(f.item, rows(f.i15, f.i16, f.i27));
        broken.parentObjects.put(f.item, rows(GGroupObjectValue.EMPTY));
        f.apply(broken);
        assertNull(field(f.row(f.item, f.i15), "parent"));
        assertNull(field(f.row(f.item, f.i27), "parent"));
    }
    public void testATreeRowThatMovesIsRewrittenAndNothingElse() {
        TreeFixture f = new TreeFixture();
        GGroupObjectValue c3 = new GGroupObjectValue(60, 3), c4 = new GGroupObjectValue(60, 4), c5 = new GGroupObjectValue(60, 5);
        GFormChanges page = new GFormChanges(); // categories hang in categories: 3 and 4 under 1, 5 under 2
        page.gridObjects.put(f.cat, rows(f.c1, f.c2, c3, c4, c5));
        page.parentObjects.put(f.cat, rows(GGroupObjectValue.EMPTY, GGroupObjectValue.EMPTY, f.c1, f.c1, f.c2));
        f.apply(page);
        JavaScriptObject c1 = f.row(f.cat, f.c1), c2 = f.row(f.cat, f.c2), moved = f.row(f.cat, c3);
        GFormChanges move = new GFormChanges(); // ... and then 3 under 2
        move.gridObjects.put(f.cat, rows(f.c1, f.c2, c3, c4, c5));
        move.parentObjects.put(f.cat, rows(GGroupObjectValue.EMPTY, GGroupObjectValue.EMPTY, f.c2, f.c1, f.c2));
        f.apply(move);
        assertEquals(2.0, number(f.row(f.cat, c3), "parent"), 0.0);
        assertEquals(1.0, number(moved, "parent"), 0.0); // the object handed out before still says what it said
        assertSame(c1, f.row(f.cat, f.c1));
        assertSame(c2, f.row(f.cat, f.c2));
    }
    public void testATreeRowKeepsNamingAParentThatWasTakenAway() {
        TreeFixture f = new TreeFixture();
        GGroupObjectValue c3 = new GGroupObjectValue(60, 3);
        GFormChanges page = new GFormChanges(); // 1 > 2 > 3
        page.gridObjects.put(f.cat, rows(f.c1, f.c2, c3));
        page.parentObjects.put(f.cat, rows(GGroupObjectValue.EMPTY, f.c1, f.c2));
        f.apply(page);
        GFormChanges filtered = new GFormChanges(); // a filter takes 2 away, and 3 stays: the server keeps sending it
        filtered.gridObjects.put(f.cat, rows(f.c1, c3));
        filtered.parentObjects.put(f.cat, rows(GGroupObjectValue.EMPTY, f.c2));
        f.apply(filtered);
        assertEquals(2.0, number(f.row(f.cat, c3), "parent"), 0.0); // it still names the parent this client lost
        JavaScriptObject orphan = f.row(f.cat, c3);
        GFormChanges removed = new GFormChanges(); // rows alone, with no hierarchy - a view's own remove
        removed.gridObjects.put(f.cat, rows(f.c1));
        f.apply(removed);
        f.apply(filtered); // ... and 3 back: a row made anew, which says nothing yet, is given its parent
        assertNotSame(orphan, f.row(f.cat, c3));
        assertEquals(2.0, number(f.row(f.cat, c3), "parent"), 0.0);
    }
    public void testATreeRowSaysWhetherItHasChildrenNotHowMany() {
        TreeFixture f = new TreeFixture();
        GFormChanges page = f.page();
        page.expandables.put(f.cat, counts(f.c1, 2, f.c2, 0));
        f.apply(page);
        assertTrue(flag(f.row(f.cat, f.c1), "hasChildren"));
        assertTrue(isFalse(f.row(f.cat, f.c2), "hasChildren")); // stated, not merely missing
        JavaScriptObject c1 = f.row(f.cat, f.c1), c2 = f.row(f.cat, f.c2);
        GFormChanges more = new GFormChanges(); // a count that moves without crossing zero changes nothing a row states
        more.gridObjects.put(f.cat, rows(f.c1, f.c2)); // the counts come with the rows, as the server sends them
        more.expandables.put(f.cat, counts(f.c1, 3, f.c2, 0));
        int publications = f.publications;
        f.apply(more);
        assertSame(c1, f.row(f.cat, f.c1));
        assertEquals(publications, f.publications); // nothing rewritten, nothing published
        GFormChanges first = new GFormChanges(); // ... and one that does flips exactly that row
        first.gridObjects.put(f.cat, rows(f.c1, f.c2));
        first.expandables.put(f.cat, counts(f.c1, 3, f.c2, 1));
        f.apply(first);
        assertTrue(flag(f.row(f.cat, f.c2), "hasChildren"));
        assertNotSame(c2, f.row(f.cat, f.c2));
        assertTrue(isFalse(c2, "hasChildren")); // the object handed out before still says what it said
        assertSame(c1, f.row(f.cat, f.c1));
        GFormChanges last = new GFormChanges(); // ... back to zero; and children counted, none of them loaded
        last.gridObjects.put(f.cat, rows(f.c1, f.c2));
        last.expandables.put(f.cat, counts(f.c1, 0, f.c2, 1));
        last.gridObjects.put(f.item, rows(f.i15, f.i16)); // nothing under 2
        last.parentObjects.put(f.item, rows(GGroupObjectValue.EMPTY, GGroupObjectValue.EMPTY));
        f.apply(last);
        assertTrue(isFalse(f.row(f.cat, f.c1), "hasChildren"));
        assertTrue(flag(f.row(f.cat, f.c2), "hasChildren"));
    }
    private static NativeHashMap<GGroupObjectValue, Integer> counts(GGroupObjectValue key1, int count1, GGroupObjectValue key2, int count2) {
        NativeHashMap<GGroupObjectValue, Integer> counts = new NativeHashMap<>();
        counts.put(key1, count1);
        counts.put(key2, count2);
        return counts;
    }
    public void testATreeNodeIsOpenWhileItsChildrenAreLoaded() {
        TreeFixture f = new TreeFixture();
        f.apply(f.page());
        assertTrue(flag(f.row(f.cat, f.c1), "expanded"));
        assertTrue(flag(f.row(f.cat, f.c2), "expanded"));
        assertTrue(isFalse(f.row(f.item, f.i15), "expanded")); // the bottom group: nothing under it
        JavaScriptObject c1 = f.row(f.cat, f.c1), c2 = f.row(f.cat, f.c2);
        GFormChanges collapse = new GFormChanges(); // 2 is closed: the server takes its children away
        collapse.gridObjects.put(f.item, rows(f.i15, f.i16));
        collapse.parentObjects.put(f.item, rows(GGroupObjectValue.EMPTY, GGroupObjectValue.EMPTY));
        f.apply(collapse);
        assertTrue(isFalse(f.row(f.cat, f.c2), "expanded"));
        assertTrue(flag(c2, "expanded")); // the object handed out before still says what it said
        assertTrue(flag(f.row(f.cat, f.c1), "expanded"));
        assertSame(c1, f.row(f.cat, f.c1)); // the node that stayed open kept its object
        GFormChanges again = new GFormChanges(); // 2 is opened again
        again.gridObjects.put(f.item, rows(f.i15, f.i16, f.i27));
        again.parentObjects.put(f.item, rows(GGroupObjectValue.EMPTY, GGroupObjectValue.EMPTY, GGroupObjectValue.EMPTY));
        f.apply(again);
        assertTrue(flag(f.row(f.cat, f.c2), "expanded"));
        GFormChanges gone = new GFormChanges(); // rows alone, no hierarchy - a view's own remove: 2's only child leaves
        gone.gridObjects.put(f.item, rows(f.i15, f.i16));
        f.apply(gone);
        assertTrue(isFalse(f.row(f.cat, f.c2), "expanded"));
        TreeFixture nested = new TreeFixture(); // a recursive group holds its own children too
        GFormChanges page = new GFormChanges();
        page.gridObjects.put(nested.cat, rows(nested.c1, nested.c2));
        page.parentObjects.put(nested.cat, rows(GGroupObjectValue.EMPTY, nested.c1));
        nested.apply(page);
        assertTrue(flag(nested.row(nested.cat, nested.c1), "expanded"));
        assertTrue(isFalse(nested.row(nested.cat, nested.c2), "expanded"));
    }
    public void testANodeOfATreeIsOpenedAndClosedThroughItsGroup() {
        TreeFixture f = new TreeFixture();
        f.apply(f.page());
        JavaScriptObject cat = field(f.sa.controller, "cat");
        callNode(cat, "expand", f.row(f.cat, f.c1)); // the row
        callNode(cat, "collapse", field(f.row(f.cat, f.c2), "objects")); // its objects handle
        callNode(cat, "expand", field(f.row(f.cat, f.c2), "key")); // its key
        callNode(cat, "expand", f.row(f.item, f.i27)); // a row BELOW names the node it hangs under here
        assertEquals(Arrays.asList("expand cat 1", "collapse cat 2", "expand cat 2", "expand cat 2"), f.verbs.asked);
        try { // a row ABOVE names no node of the group below
            callNode(field(f.sa.controller, "item"), "expand", f.row(f.cat, f.c1));
            fail();
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("not a row of 'item'"));
        }
        Fixture grid = new Fixture(); // a group outside a tree has no such members at all
        assertFalse(has(field(grid.sa.controller, "items"), "expand"));
        assertFalse(has(field(grid.sa.controller, "items"), "expandAll"));
    }
    private static native JavaScriptObject callNode(JavaScriptObject member, String verb, Object argument) /*-{ return member[verb](argument); }-*/;

    // a page where both categories have children and only 1's are loaded: 2 is closed
    private static GFormChanges closedPage(TreeFixture f) {
        GFormChanges page = new GFormChanges();
        page.gridObjects.put(f.cat, rows(f.c1, f.c2));
        page.parentObjects.put(f.cat, rows(GGroupObjectValue.EMPTY, GGroupObjectValue.EMPTY));
        page.expandables.put(f.cat, counts(f.c1, 2, f.c2, 1));
        page.gridObjects.put(f.item, rows(f.i15, f.i16));
        page.parentObjects.put(f.item, rows(GGroupObjectValue.EMPTY, GGroupObjectValue.EMPTY));
        return page;
    }
    // ... and 2's child loaded too: 2 is open
    private static GFormChanges openPage(TreeFixture f) {
        GFormChanges page = new GFormChanges();
        page.gridObjects.put(f.item, rows(f.i15, f.i16, f.i27));
        page.parentObjects.put(f.item, rows(GGroupObjectValue.EMPTY, GGroupObjectValue.EMPTY, GGroupObjectValue.EMPTY));
        return page;
    }
    public void testANodeIsShownAsAskedUntilTheServerAnswers() {
        TreeFixture f = new TreeFixture();
        f.apply(closedPage(f));
        JavaScriptObject cat = field(f.sa.controller, "cat"), c1 = f.row(f.cat, f.c1);
        int publications = f.publications;
        callNode(cat, "expand", f.row(f.cat, f.c2));
        assertTrue(flag(f.row(f.cat, f.c2), "expanded")); // at once, published, before its children
        assertTrue(flag(f.row(f.cat, f.c2), "loading")); // ... and says it waits for the answer
        assertEquals(publications + 1, f.publications);
        assertSame(c1, f.row(f.cat, f.c1)); // the other node kept its object
        assertFalse(own(c1, "loading"));
        f.answer(new GFormChanges(), 0); // an answer to a request before it changes nothing
        assertTrue(flag(f.row(f.cat, f.c2), "expanded"));
        assertTrue(flag(f.row(f.cat, f.c2), "loading"));
        GFormChanges gone = new GFormChanges(); // ... nor does one that takes the node away and brings it back: a row
        gone.gridObjects.put(f.cat, rows(f.c1)); // made anew is given what was asked of it
        f.answer(gone, 0);
        GFormChanges back = new GFormChanges();
        back.gridObjects.put(f.cat, rows(f.c1, f.c2));
        back.parentObjects.put(f.cat, rows(GGroupObjectValue.EMPTY, GGroupObjectValue.EMPTY));
        f.answer(back, 0);
        assertTrue(flag(f.row(f.cat, f.c2), "expanded"));
        f.answer(openPage(f), 1); // its answer: the children, and 2 open as the rows say
        assertTrue(flag(f.row(f.cat, f.c2), "expanded"));
        assertFalse(own(f.row(f.cat, f.c2), "loading"));
        assertEquals(2.0, number(f.row(f.item, f.i27), "parent"), 0.0);
        callNode(cat, "collapse", f.row(f.cat, f.c2));
        assertTrue(isFalse(f.row(f.cat, f.c2), "expanded")); // closed at once, while its child is still loaded
        assertNotNull(f.row(f.item, f.i27));
        assertFalse(own(f.row(f.cat, f.c2), "loading")); // closing a node loads nothing
        f.answer(new GFormChanges(), 2); // an answer with no keys - a failed one - leaves it as asked, as the platform's
        assertTrue(isFalse(f.row(f.cat, f.c2), "expanded")); // tree does
        f.answer(openPage(f), 2); // ... and keys that still hold its child: the server did not close it
        assertTrue(flag(f.row(f.cat, f.c2), "expanded"));
        assertFalse(own(f.row(f.cat, f.c2), "loading"));
    }
    // ... and so do two toggles of a node made in one batch: the second reads what the first asked, and the projection
    // is published when the batch ends - the view never sees the node opened in between. Both are asked under the one
    // index the batch's request is sent with, so its answer settles both
    public void testTwoTogglesInABatchOpenAndCloseANode() {
        TreeFixture f = new TreeFixture();
        f.apply(closedPage(f));
        JavaScriptObject cat = field(f.sa.controller, "cat"), c2 = f.row(f.cat, f.c2);
        int publications = f.publications;
        batch(f.sa.controller, () -> {
            callNode(cat, "toggle", c2);
            callNode(cat, "toggle", c2);
        });
        assertEquals(Arrays.asList("batch", "expand cat 2", "collapse cat 2", "sent"), f.verbs.asked);
        assertEquals(publications, f.publications); // nothing to publish: the second toggle undid the first
        assertTrue(isFalse(f.row(f.cat, f.c2), "expanded"));
        f.answer(openPage(f), (int) f.verbs.requestIndex); // the batch's answer: keys still holding its child - the
        assertTrue(flag(f.row(f.cat, f.c2), "expanded"));   // server did not close it, and nothing asked waits any more
    }
    // WHEN IT IS PUBLISHED (GReactFormData.change): what a member's call writes is published before the call returns
    // - a view calls it from its event handler, and React puts a controlled input back to what the store says once the
    // handler is done -, the form asked for no publication of its own
    public void testAMembersCallIsPublishedBeforeItReturns() {
        TreeFixture f = new TreeFixture();
        f.apply(closedPage(f));
        JavaScriptObject cat = field(f.sa.controller, "cat");
        int publications = f.publications;
        callNode(cat, "expand", f.row(f.cat, f.c2));
        assertEquals(publications + 1, f.publications);
        assertTrue(flag(f.row(f.cat, f.c2), "expanded"));
    }
    // ... what is written outside a change - by an event of the platform's -: not at once, but when the GWT entry it is
    // written in exits, the writes of the event together (Scheduler.scheduleFinally)
    public void testWhatIsWrittenOutsideAChangeIsPublishedWhenItsEntryExits() {
        TreeFixture f = new TreeFixture();
        f.apply(closedPage(f));
        int publications = f.publications;
        delayTestFinish(5000);
        inATaskOfItsOwn(() -> { // no GWT entry on the stack, as an event of the platform's comes
            enter(() -> { // the event's entry
                f.controllers.groups.get(f.cat).changeExpanded(f.c2, true, 1); // as the platform's toolbar writes it
                f.controllers.groups.get(f.cat).changeCurrentKey(f.c2);
                assertEquals(publications, f.publications);
            });
            assertEquals(publications + 1, f.publications); // its exit published them
            assertTrue(flag(f.row(f.cat, f.c2), "expanded"));
            finishTest();
        });
    }
    private static native void inATaskOfItsOwn(Runnable run) /*-{
        $wnd.setTimeout(function () {
            run.@java.lang.Runnable::run()();
        }, 0);
    }-*/;
    private static native void enter(Runnable run) /*-{
        $entry(function () {
            run.@java.lang.Runnable::run()();
        })();
    }-*/;
    // ... and what the form is set out with - its nodes, written outside a change - waits, and goes out with its
    // first changes, applied in the same task: the form's publication then finds nothing left (ReactRoot draws a form's
    // view from its first changes)
    public void testWhatTheFormIsSetOutWithGoesOutWithItsFirstChanges() {
        TreeFixture f = new TreeFixture(); // set out: its nodes written, nothing applied
        assertEquals(0, f.publications);
        f.apply(closedPage(f));
        assertEquals(1, f.publications);
        f.projection.publish(); // the projection's own, once the code running now is done
        assertEquals(1, f.publications);
    }
    public void testTwoTogglesBeforeTheAnswerOpenAndCloseANode() {
        TreeFixture f = new TreeFixture();
        f.apply(closedPage(f));
        JavaScriptObject cat = field(f.sa.controller, "cat"), c2 = f.row(f.cat, f.c2);
        callNode(cat, "toggle", c2); // closed: opened
        assertTrue(flag(f.row(f.cat, f.c2), "expanded"));
        callNode(cat, "toggle", c2); // the same row handed in again: closed again - what was asked is read, not the row
        assertTrue(isFalse(f.row(f.cat, f.c2), "expanded"));
        assertEquals(Arrays.asList("expand cat 2", "collapse cat 2"), f.verbs.asked);
        f.answer(openPage(f), 1); // the first answer: the second request is not answered yet, and it wins
        assertTrue(isFalse(f.row(f.cat, f.c2), "expanded"));
        f.answer(closedPage(f), 2);
        assertTrue(isFalse(f.row(f.cat, f.c2), "expanded"));
        callNode(cat, "toggle", f.row(f.cat, f.c1)); // its children loaded: open, so closed
        assertEquals("collapse cat 1", f.verbs.asked.get(2));
    }
    public void testANodeWithNoChildrenStaysClosed() {
        TreeFixture f = new TreeFixture();
        GFormChanges page = closedPage(f);
        page.expandables.put(f.cat, counts(f.c1, 2, f.c2, 0));
        f.apply(page);
        JavaScriptObject c2 = f.row(f.cat, f.c2);
        int publications = f.publications;
        callNode(field(f.sa.controller, "cat"), "expand", c2);
        assertTrue(isFalse(f.row(f.cat, f.c2), "expanded")); // asked, but there is nothing to open - nor to load
        assertSame(c2, f.row(f.cat, f.c2)); // so nothing is rewritten, nothing published
        assertEquals(publications, f.publications);
        assertEquals(Arrays.asList("expand cat 2"), f.verbs.asked); // the server is asked all the same
    }
    public void testAllTheNodesOfAGroupAreOpenedAndClosedThroughIt() {
        TreeFixture f = new TreeFixture(t -> t.item.isRecursive = true); // an item has children too: the group recurses
        GFormChanges page = closedPage(f);
        page.expandables.put(f.item, counts(f.i15, 1, f.i16, 0));
        f.apply(page);
        JavaScriptObject cat = field(f.sa.controller, "cat"), item = field(f.sa.controller, "item");
        callAll(cat, "expandAll"); // the top group: the whole tree
        assertTrue(flag(f.row(f.cat, f.c1), "expanded"));
        assertTrue(flag(f.row(f.cat, f.c2), "expanded")); // at once
        assertTrue(flag(f.row(f.item, f.i15), "expanded")); // ... the group below as well
        assertTrue(isFalse(f.row(f.item, f.i16), "expanded")); // ... where a node has children
        callNode(cat, "collapse", f.row(f.cat, f.c1)); // a node asked after it: the later request wins
        assertTrue(isFalse(f.row(f.cat, f.c1), "expanded"));
        assertTrue(flag(f.row(f.cat, f.c2), "expanded"));
        callAll(item, "collapseAll"); // a group below: its nodes, not those above it
        assertTrue(isFalse(f.row(f.item, f.i15), "expanded"));
        assertTrue(flag(f.row(f.cat, f.c2), "expanded"));
        assertEquals(Arrays.asList("expand all cat", "collapse cat 1", "collapse all item"), f.verbs.asked);
        f.answer(openPage(f), 3); // all answered: the rows say
        assertTrue(flag(f.row(f.cat, f.c1), "expanded"));
        assertTrue(flag(f.row(f.cat, f.c2), "expanded"));
        assertTrue(isFalse(f.row(f.item, f.i15), "expanded"));
        callNode(cat, "collapse", f.row(f.cat, f.c2)); // a node asked before it: the later request, all of them, wins
        callAll(cat, "expandAll");
        assertTrue(flag(f.row(f.cat, f.c2), "expanded"));
        GFormChanges older = new GFormChanges(); // the answer to the earlier one leaves it - and brings a row the later
        GGroupObjectValue i18 = TreeFixture.itemKey(2, 8); // one was not asked of: it says what its rows say
        older.gridObjects.put(f.item, rows(f.i15, f.i16, f.i27, i18));
        older.parentObjects.put(f.item, rows(GGroupObjectValue.EMPTY, GGroupObjectValue.EMPTY, GGroupObjectValue.EMPTY, GGroupObjectValue.EMPTY));
        older.expandables.put(f.item, counts(f.i15, 1, i18, 1));
        f.answer(older, 4);
        assertTrue(flag(f.row(f.cat, f.c2), "expanded"));
        assertTrue(isFalse(f.row(f.item, i18), "expanded"));
        f.answer(openPage(f), 5);
        callAll(cat, "collapseAll");
        assertTrue(isFalse(f.row(f.cat, f.c1), "expanded"));
        assertTrue(isFalse(f.row(f.cat, f.c2), "expanded")); // at once
        GFormChanges collapsed = new GFormChanges(); // ... and its answer takes the items away
        collapsed.gridObjects.put(f.item, rows());
        collapsed.parentObjects.put(f.item, rows());
        f.answer(collapsed, 6);
        assertTrue(isFalse(f.row(f.cat, f.c1), "expanded"));
        callNode(cat, "expand", f.row(f.cat, f.c1)); // what was open below stays open on the server: it comes back so
        GFormChanges reopened = new GFormChanges();
        reopened.gridObjects.put(f.item, rows(f.i15, f.i16));
        reopened.parentObjects.put(f.item, rows(GGroupObjectValue.EMPTY, new GGroupObjectValue(61, 5)));
        f.answer(reopened, 7);
        assertTrue(flag(f.row(f.cat, f.c1), "expanded"));
        assertTrue(flag(f.row(f.item, f.i15), "expanded"));
    }
    public void testATreeDeclaredBottomUpStillHangsTogether() {
        TreeFixture f = new TreeFixture(t -> java.util.Collections.reverse(t.form.groupObjects)); // the form's order
        f.apply(closedPage(f));
        assertTrue(flag(f.row(f.cat, f.c1), "expanded")); // the items still open the categories they hang under
        assertTrue(isFalse(f.row(f.cat, f.c2), "expanded"));
        callAll(field(f.sa.controller, "cat"), "expandAll"); // ... and asking all of them reaches below
        assertTrue(flag(f.row(f.cat, f.c2), "expanded"));
        f.answer(openPage(f), 1); // the items' keys answer it
        assertEquals(2.0, number(f.row(f.item, f.i27), "parent"), 0.0);
        assertTrue(flag(f.row(f.cat, f.c2), "expanded"));
    }
    private static native JavaScriptObject callAll(JavaScriptObject member, String verb) /*-{ return member[verb](); }-*/;

    public void testAClosedNodeIsHeldClosedByAllAgainstAnEarlierAnswer() {
        TreeFixture f = new TreeFixture();
        f.apply(closedPage(f)); // 1 open, 2 closed - both with children
        callAll(field(f.sa.controller, "cat"), "collapseAll");
        assertTrue(isFalse(f.row(f.cat, f.c1), "expanded")); // closed at once
        assertTrue(isFalse(f.row(f.cat, f.c2), "expanded"));
        f.answer(openPage(f), 0); // an answer to an earlier request opens 2: what was asked of all of them holds it
        assertTrue(isFalse(f.row(f.cat, f.c2), "expanded"));
        GFormChanges collapsed = new GFormChanges(); // ... until its own answer, which takes the items away
        collapsed.gridObjects.put(f.item, rows());
        collapsed.parentObjects.put(f.item, rows());
        f.answer(collapsed, 1);
        assertTrue(isFalse(f.row(f.cat, f.c1), "expanded"));
        assertTrue(isFalse(f.row(f.cat, f.c2), "expanded"));
    }

    // a tree of three groups: the categories, the subcategories in them, the items in those - every row keyed by the
    // whole path down to it, its objects in the order of their ids, as the platform builds a key
    private static GGroupObjectValue subKey(int category, int sub) {
        return new GGroupObjectValue(2, new int[]{60, 63}, new Serializable[]{category, sub});
    }
    private static GGroupObjectValue deepKey(int category, int sub, int item) {
        return new GGroupObjectValue(3, new int[]{60, 61, 63}, new Serializable[]{category, item, sub});
    }
    private static TreeFixture deepTree(GGroupObject sub) {
        return new TreeFixture(t -> {
            t.group(sub, 63, "sub"); // between the categories and the items, in the tree and in the form
            t.tree.groups.remove(sub);
            t.tree.groups.add(1, sub);
            t.form.groupObjects.remove(sub);
            t.form.groupObjects.add(1, sub);
            sub.upTreeGroups.add(t.cat);
            t.item.upTreeGroups.add(sub);
        });
    }
    public void testAThreeLevelTreeHangsAndOpensLevelByLevel() {
        GGroupObject sub = new GGroupObject();
        TreeFixture f = deepTree(sub);
        GGroupObjectValue s11 = subKey(1, 1), s12 = subKey(1, 2), s21 = subKey(2, 1);
        GGroupObjectValue i115 = deepKey(1, 1, 5), i126 = deepKey(1, 2, 6);
        GFormChanges page = new GFormChanges(); // category 1 open, with subcategories 1 and 2; 1.1 open, with item 5
        page.gridObjects.put(f.cat, rows(f.c1, f.c2));
        page.parentObjects.put(f.cat, rows(GGroupObjectValue.EMPTY, GGroupObjectValue.EMPTY));
        page.expandables.put(f.cat, counts(f.c1, 2, f.c2, 1));
        page.gridObjects.put(sub, rows(s11, s12));
        page.parentObjects.put(sub, rows(GGroupObjectValue.EMPTY, GGroupObjectValue.EMPTY));
        page.expandables.put(sub, counts(s11, 1, s12, 1));
        page.gridObjects.put(f.item, rows(i115));
        page.parentObjects.put(f.item, rows(GGroupObjectValue.EMPTY));
        f.apply(page);
        assertTrue(sameField(f.row(sub, s11), "parent", f.row(f.cat, f.c1), "key")); // each row hangs one level up
        assertTrue(sameField(f.row(f.item, i115), "parent", f.row(sub, s11), "key"));
        assertTrue(flag(f.row(f.cat, f.c1), "expanded")); // ... and each level is opened by the one below it
        assertTrue(flag(f.row(sub, s11), "expanded"));
        assertTrue(isFalse(f.row(sub, s12), "expanded"));
        assertTrue(isFalse(f.row(f.cat, f.c2), "expanded"));
        callAll(field(f.sa.controller, "sub"), "expandAll"); // the middle group: its nodes and below, not those above
        assertTrue(flag(f.row(sub, s12), "expanded"));
        assertTrue(isFalse(f.row(f.cat, f.c2), "expanded"));
        callNode(field(f.sa.controller, "cat"), "expand", f.row(f.cat, f.c2)); // a node above, asked after it
        assertEquals(Arrays.asList("expand all sub", "expand cat 2"), f.verbs.asked);
        GFormChanges items = new GFormChanges(); // the first answer: the items' keys - they answer the middle group
        items.gridObjects.put(f.item, rows(i115, i126));
        items.parentObjects.put(f.item, rows(GGroupObjectValue.EMPTY, GGroupObjectValue.EMPTY));
        f.answer(items, 1);
        assertTrue(flag(f.row(sub, s12), "expanded")); // what its rows say now
        assertTrue(flag(f.row(f.cat, f.c2), "expanded")); // ... and the top node is still as asked: not answered yet
        GFormChanges subs = new GFormChanges(); // the second: the subcategories' keys - they answer the top group
        subs.gridObjects.put(sub, rows(s11, s12, s21));
        subs.parentObjects.put(sub, rows(GGroupObjectValue.EMPTY, GGroupObjectValue.EMPTY, GGroupObjectValue.EMPTY));
        f.answer(subs, 2);
        assertTrue(sameField(f.row(sub, s21), "parent", f.row(f.cat, f.c2), "key"));
        assertTrue(flag(f.row(f.cat, f.c2), "expanded"));
        assertTrue(isFalse(f.row(sub, s21), "expanded")); // its own children are not loaded
    }

    // what the rows of a tree's groups state about one another is the tree's: a change of it - here the items'
    // children - places the rows of every group anew, and placing rewrites only a row whose facts changed - the
    // categories' rows keep their objects -, the tree published once; a delta that changes nothing of it places nothing
    // and publishes nothing
    public void testATreePlacesItsRowsAsOneAndRewritesNothingThatStayed() {
        TreeFixture f = new TreeFixture(t -> t.item.isRecursive = true); // an item has children too: the group recurses
        f.apply(closedPage(f));
        JavaScriptObject c1 = f.row(f.cat, f.c1), c2 = f.row(f.cat, f.c2), i16 = f.row(f.item, f.i16);
        int publications = f.publications;
        GFormChanges children = new GFormChanges(); // the same items, one of them with children now
        children.gridObjects.put(f.item, rows(f.i15, f.i16));
        children.expandables.put(f.item, counts(f.i15, 1, f.i16, 0));
        f.apply(children);
        assertEquals(publications + 1, f.publications);
        assertTrue(flag(f.row(f.item, f.i15), "hasChildren"));
        assertSame(i16, f.row(f.item, f.i16));
        assertSame(c1, f.row(f.cat, f.c1));
        assertSame(c2, f.row(f.cat, f.c2));
        f.apply(children);
        assertEquals(publications + 1, f.publications);
        assertSame(c1, f.row(f.cat, f.c1));
    }

    // a react container that draws a grid's rows beside a tree's keeps the two tables: each group's rows are its own
    // table's - a list property's column goes with its group's -, and what rows state about one another the tree
    // alone places, a grid's rows saying nothing of the kind
    public void testAContainerKeepsTheGridAndTheTreeItDraws() {
        final GGroupObject[] plain = new GGroupObject[1];
        TreeFixture f = new TreeFixture(t -> {
            GGroupObject g = plain[0] = new GGroupObject();
            g.ID = 80; g.nativeSID = "g80"; g.sID = "plain"; g.viewType = GClassViewType.LIST;
            g.grid = new GGrid(); g.grid.ID = 81; g.grid.nativeSID = "c81"; g.grid.sID = "plainGrid"; g.grid.container = t.a;
            g.grid.groupObject = g;
            t.a.children.add(g.grid);
            g.objects.add(new GObject(g, "plain", 80, "plain", GIntegerType.instance));
            t.form.groupObjects.add(g);
            property(t.form, 85, "note", g, true, t.a);
        });
        GFormChanges page = closedPage(f);
        GGroupObjectValue p1 = new GGroupObjectValue(80, 1);
        page.gridObjects.put(plain[0], rows(p1));
        f.apply(page);
        assertEquals("note", join(field(field(f.data, "plain"), "properties"))); // its column, with its rows
        assertNotNull(f.row(plain[0], p1));
        assertFalse(own(f.row(plain[0], p1), "parent")); // a grid's row says nothing of where it hangs ...
        assertTrue(own(f.row(f.cat, f.c1), "parent")); // ... a tree's does, the tree placing it
        assertTrue(own(f.row(f.item, f.i15), "parent"));
    }

    // the rows of a group that come with a request's index answer the requests up to it asked of the group's own nodes
    // and of the nodes of the group above - whose children they are -, and of no other group's, whatever their index:
    // of two requests sent together, one asked of the middle group of a three-level tree and one of the top, the top
    // group's rows answer the top's and leave the middle's as asked, and the bottom group's rows answer the middle's
    // and leave the top's
    public void testAGroupsRowsAnswerForItsOwnNodesAndThoseOfTheGroupAbove() {
        GGroupObject sub = new GGroupObject();
        TreeFixture f = deepTree(sub);
        GGroupObjectValue s11 = subKey(1, 1), s12 = subKey(1, 2), s21 = subKey(2, 1);
        GGroupObjectValue i115 = deepKey(1, 1, 5), i126 = deepKey(1, 2, 6);
        GFormChanges page = new GFormChanges(); // category 1 open, with subcategories 1 and 2; 1.1 open, with item 5
        page.gridObjects.put(f.cat, rows(f.c1, f.c2));
        page.parentObjects.put(f.cat, rows(GGroupObjectValue.EMPTY, GGroupObjectValue.EMPTY));
        page.expandables.put(f.cat, counts(f.c1, 2, f.c2, 1));
        page.gridObjects.put(sub, rows(s11, s12));
        page.parentObjects.put(sub, rows(GGroupObjectValue.EMPTY, GGroupObjectValue.EMPTY));
        page.expandables.put(sub, counts(s11, 1, s12, 1));
        page.gridObjects.put(f.item, rows(i115));
        page.parentObjects.put(f.item, rows(GGroupObjectValue.EMPTY));
        f.apply(page);
        JavaScriptObject cat = field(f.sa.controller, "cat"), middle = field(f.sa.controller, "sub");
        batch(f.sa.controller, () -> { // one request, asked of a node of the middle group and of one of the top
            callNode(middle, "expand", f.row(sub, s12));
            callNode(cat, "expand", f.row(f.cat, f.c2));
        });
        int request = (int) f.verbs.requestIndex;
        assertTrue(flag(f.row(sub, s12), "loading"));
        assertTrue(flag(f.row(f.cat, f.c2), "loading"));
        GFormChanges top = new GFormChanges(); // the top group's rows, with the request's index: its own node answered
        top.gridObjects.put(f.cat, rows(f.c1, f.c2));
        top.parentObjects.put(f.cat, rows(GGroupObjectValue.EMPTY, GGroupObjectValue.EMPTY));
        f.answer(top, request);
        assertFalse(own(f.row(f.cat, f.c2), "loading")); // ... as the rows say: nothing under it yet
        assertTrue(isFalse(f.row(f.cat, f.c2), "expanded"));
        assertTrue(flag(f.row(sub, s12), "loading")); // ... and the middle group's left as asked
        assertTrue(flag(f.row(sub, s12), "expanded"));
        GFormChanges bottom = new GFormChanges(); // the bottom group's: the middle's answer, 1.2 open as the rows say
        bottom.gridObjects.put(f.item, rows(i115, i126));
        bottom.parentObjects.put(f.item, rows(GGroupObjectValue.EMPTY, GGroupObjectValue.EMPTY));
        f.answer(bottom, request);
        assertTrue(flag(f.row(sub, s12), "expanded"));
        assertFalse(own(f.row(sub, s12), "loading"));

        batch(f.sa.controller, () -> { // the same two again, the bottom group's rows coming first this time
            callNode(middle, "collapse", f.row(sub, s12));
            callNode(cat, "expand", f.row(f.cat, f.c2));
        });
        request = (int) f.verbs.requestIndex;
        assertTrue(isFalse(f.row(sub, s12), "expanded"));
        assertTrue(flag(f.row(f.cat, f.c2), "loading"));
        f.answer(bottom, request); // the middle's answered: its child is still there, so it is open
        assertTrue(flag(f.row(sub, s12), "expanded"));
        assertTrue(flag(f.row(f.cat, f.c2), "loading")); // ... and the top's left as asked: two groups above
        GFormChanges subs = new GFormChanges(); // the middle group's rows: the top's answer, 2 open as the rows say
        subs.gridObjects.put(sub, rows(s11, s12, s21));
        subs.parentObjects.put(sub, rows(GGroupObjectValue.EMPTY, GGroupObjectValue.EMPTY, GGroupObjectValue.EMPTY));
        f.answer(subs, request);
        assertTrue(flag(f.row(f.cat, f.c2), "expanded"));
        assertFalse(own(f.row(f.cat, f.c2), "loading"));
    }

    // where the rows are drawn the node carries the group's sortings - always, empty while sorted by nothing - and a
    // container holding only a panel property of the group has none
    public void testSortingsAreOnTheNodeWhereTheRowsAre() {
        Fixture f = new Fixture();
        assertEquals(0, length(field(f.node(), "orders"))); // always there, empty while sorted by nothing
        assertNull(field(field(f.snapshots[1], "items"), "orders")); // b holds a panel property of the group, no rows
    }

    // the sortings the form hands their part - its own ORDERS in the first changes, then what the server reports - are
    // projected in priority order and in the ORDER operator's shape, replacing the ones before; the rows stay as they
    // are, and a value that changes leaves the sortings as they were
    public void testSortingsAreTheOnesTheFormHasNow() {
        Fixture f = new Fixture();
        LinkedHashMap<Column, Boolean> orders = new LinkedHashMap<>(); // as the form has them: true - ascending
        orders.put(column(f.quantity), false);
        orders.put(column(f.price), true);
        JavaScriptObject list = field(f.node(), "list");
        f.reportOrders(orders);
        f.projection.flush();
        JavaScriptObject sorted = field(f.node(), "orders");
        assertEquals(2, length(sorted));
        assertEquals("quantity", text(at(sorted, 0), "property"));
        assertTrue(flag(at(sorted, 0), "desc"));
        assertEquals("price", text(at(sorted, 1), "property"));
        assertFalse(flag(at(sorted, 1), "desc"));
        assertSame(list, field(f.node(), "list")); // the rows for the new order come from the server, separately
        assertNotSame(f.controller(f.group), f.userOrdersController()); // their part shows them, not the rows' part
        f.setValue(f.price, f.one, 11); // a value changes, the sortings do not: the same array
        f.projection.flush();
        assertSame(sorted, field(f.node(), "orders"));
        LinkedHashMap<Column, Boolean> next = new LinkedHashMap<>();
        next.put(column(f.price), false);
        f.reportOrders(next); // ... and the next list replaces them
        f.projection.flush();
        assertEquals(1, length(field(f.node(), "orders")));
        assertTrue(flag(at(field(f.node(), "orders"), 0), "desc"));
    }

    // a sorting is named by the group's name of its property, wherever that is drawn - the panel property in b, one the
    // platform draws -, as a condition is; one on a property no sorting can name - one with no name, one grouped in
    // columns - is left out (and reported) rather than named by a name that says nothing
    public void testASortingIsNamedByTheGroupsName() {
        Fixture f = Fixture.withPlatformProperties();
        LinkedHashMap<Column, Boolean> orders = new LinkedHashMap<>();
        orders.put(column(f.panel), true);
        orders.put(column(f.platformNamed), false);
        orders.put(column(f.platformUnnamed), true);
        orders.put(new Column(f.platformInColumns, f.one), true);
        orders.put(column(f.quantity), false);
        f.reportOrders(orders);
        f.projection.flush();
        JavaScriptObject sorted = field(f.node(), "orders");
        assertEquals(3, length(sorted));
        assertEquals("panel", text(at(sorted, 0), "property"));
        assertEquals("platform", text(at(sorted, 1), "property"));
        assertTrue(flag(at(sorted, 1), "desc"));
        assertEquals("quantity", text(at(sorted, 2), "property"));
    }

    // ... and a view names one the same way: the group's property of that name, wherever it is drawn - the panel
    // property in b, one the platform draws -, with what it can be sorted by checked as for any other; a name the group
    // does not carry is refused, and so is a property grouped in columns, a sorting naming no column
    public void testASortingNamesThePropertyOfItsGroupWhereverItIsDrawn() {
        Fixture f = Fixture.withPlatformProperties();
        JavaScriptObject orders = field(field(f.sa.controller, "items"), "orders");
        callChange(orders, parse("[{\"property\": \"panel\", \"desc\": true}, {\"property\": \"platform\", \"desc\": false}]"));
        assertEquals(columns(f.panel, f.platformNamed), new ArrayList<>(f.sorted.keySet()));
        assertEquals(Boolean.FALSE, f.sorted.get(column(f.panel)));
        String refused = refusedChange(orders, parse("{\"property\": \"nosuch\", \"desc\": true}"));
        assertTrue(refused, refused.contains("controller.items.orders.change(): unknown property 'nosuch' of object group 'items'"));
        refused = refusedChange(orders, parse("{\"property\": \"columns\", \"desc\": true}"));
        assertTrue(refused, refused.contains("'columns' cannot be sorted by here: it is grouped in columns, and a sorting names no column"));
        assertEquals(1, f.sortedCalls);
    }
    // a refusal says its reason alone, as a view shows it to its user: nothing of the Java exception it is on the Java
    // side - "java.lang.RuntimeException: ", which a Java exception thrown into JS puts before its message - comes first
    public void testARefusalSaysItsReasonAlone() {
        Fixture f = Fixture.withPlatformProperties();
        JavaScriptObject orders = field(field(f.sa.controller, "items"), "orders");
        String refused = refusedChange(orders, parse("{\"property\": \"nosuch\", \"desc\": true}"));
        assertTrue(refused, refused.startsWith("controller.items.orders.change(): unknown property 'nosuch' of object group 'items'"));
    }
    // a member's exec() runs the property's change event with no value given; a value the platform asks for before it
    // runs the event - an input the client knows of in advance - is asked in the cell the change is made in, and a
    // call has none: the call is refused with what the view does instead, and the form is handed nothing. It used to
    // go out, the server asked for the value, and the client, opening its editor in no cell, failed the answer - an
    // Internal Server Error. An event that asks for nothing in advance is run as it was
    public void testAnExecOfAPropertyThatAsksForAValueIsRefused() {
        Fixture f = new Fixture(r -> {
            asksForAString(r.single); // single = DATA STRING (): its change, an input of a string
            asksForAString(r.panel); // ... and an action that asks for a string first, INPUT s = STRING DO ...
            r.panel.valueType = GActionType.instance;
        });
        assertRefusal("controller.single.exec(): 'single' asks for a value, which a view gives itself: call change(value)", refusedWith(member(f.sa, null, "single"), "exec", null));
        assertRefusal("controller.items.panel.exec(): 'panel' asks for a value, which no call can give an action", refusedWith(member(f.sb, "items", "panel"), "exec", null));
        assertTrue(f.edits.toString(), f.edits.isEmpty());

        f.single.asyncExecMap = new HashMap<>(); // its change asks for nothing in advance - an ON CHANGE of its own, say
        assertNull(refusedWith(member(f.sa, null, "single"), "exec", null));
        assertEquals(Arrays.asList("cell single@"), f.edits);
    }
    private static void asksForAString(GPropertyDraw property) {
        property.asyncExecMap = new HashMap<>();
        property.asyncExecMap.put(GEditBindingMap.CHANGE, new GAsyncInput(new GStringType(GExtInt.UNLIMITED, false, false), false, null, null, null));
    }
    // a sorting names a property, never a column of it: a property React draws is never in columns
    private static Column column(GPropertyDraw property) {
        return new Column(property, GGroupObjectValue.EMPTY);
    }
    // ... and the same sortings in another priority are other ones, as the form compares them
    // (GFormController.equalOrders)
    private static boolean equalOrders(LinkedHashMap<Column, Boolean> orders, LinkedHashMap<Column, Boolean> current) {
        return current != null && new ArrayList<>(orders.entrySet()).equals(new ArrayList<>(current.entrySet()));
    }

    // whether a user may sort by a property, beside its type: asked of the property, so a panel property answers it as
    // a column does, while a property of the empty group, which no sorting can name, has none
    public void testAPropertySaysWhetherItCanBeSortedBy() {
        Fixture f = new Fixture(r -> r.quantity.noSort = true); // a design that says not to sort by it
        assertFalse(flag(field(f.node(), "price"), "noSort"));
        assertEquals("boolean", typeOf(field(f.node(), "price"), "noSort"));
        assertTrue(flag(field(f.node(), "quantity"), "noSort"));
        assertEquals("boolean", typeOf(field(field(f.snapshots[1], "items"), "panel"), "noSort"));
        assertEquals("undefined", typeOf(field(f.data(), "single"), "noSort"));
    }
    private static native String typeOf(JavaScriptObject object, String name) /*-{ return typeof object[name]; }-*/;

    // whether a change of a property asks the user first, and with what (CONFIRM, the design's askConfirm): the
    // platform's own editor and button ask, a view's call does not, so the view is told - every entry says whether,
    // the message only where it asks; a property of the empty group too
    public void testAPropertySaysWhetherItsChangeAsksFirst() {
        Fixture f = new Fixture(r -> { r.quantity.askConfirm = true; r.quantity.askConfirmMessage = "Change the quantity?"; });
        assertTrue(flag(field(f.node(), "quantity"), "askConfirm"));
        assertEquals("Change the quantity?", text(field(f.node(), "quantity"), "askConfirmMessage"));
        assertFalse(flag(field(f.node(), "price"), "askConfirm"));
        assertEquals("undefined", typeOf(field(f.node(), "price"), "askConfirmMessage"));
        assertEquals("boolean", typeOf(field(f.data(), "single"), "askConfirm"));
    }

    // the member is where the rows are drawn: a draws them, b holds only a panel property of the group
    public void testSortingsAreAMemberWhereTheRowsAre() {
        Fixture f = new Fixture();
        assertEquals("function", typeOf(field(field(f.sa.controller, "items"), "orders"), "change")); // a draws rows
        assertEquals("undefined", typeOf(field(f.sb.controller, "items"), "orders")); // b holds a panel property only
    }

    // a call through the member is read where the rows are and handed to the form as the list it makes: a property by
    // its group's name of it, `desc` turned into the client's ascending, the priority as given
    public void testSortingsAreChangedThroughTheMember() {
        Fixture f = new Fixture();
        JavaScriptObject orders = field(field(f.sa.controller, "items"), "orders");
        callChange(orders, parse("{\"property\": \"price\", \"desc\": true}")); // one, merged into none
        assertEquals(columns(f.price), new ArrayList<>(f.sorted.keySet()));
        assertEquals(Boolean.FALSE, f.sorted.get(column(f.price)));
        callChange(orders, parse("[{\"property\": \"quantity\"}, {\"property\": \"price\", \"desc\": false}]"));
        assertEquals(columns(f.price), new ArrayList<>(f.sorted.keySet())); // an order with no `desc` is none
        assertEquals(Boolean.TRUE, f.sorted.get(column(f.price)));
        callChange(orders, parse("[{\"property\": \"quantity\", \"desc\": true}, {\"property\": \"price\", \"desc\": false}]"));
        assertEquals(columns(f.quantity, f.price), new ArrayList<>(f.sorted.keySet())); // the array's order
        assertEquals(Boolean.FALSE, f.sorted.get(column(f.quantity)));
        assertEquals(Boolean.TRUE, f.sorted.get(column(f.price)));
        callChange(orders, parse("[]")); // the whole list: none
        assertEquals(0, f.sorted.size());
        // one order is merged into the sortings the form has: a property already sorted keeps its place, another is
        // appended, and the rest stay as they are
        LinkedHashMap<Column, Boolean> held = new LinkedHashMap<>();
        held.put(column(f.quantity), true);
        f.reportOrders(held);
        callChange(orders, parse("{\"property\": \"price\", \"desc\": true}"));
        assertEquals(columns(f.quantity, f.price), new ArrayList<>(f.sorted.keySet())); // appended
        assertEquals(Boolean.FALSE, f.sorted.get(column(f.price)));
        held.put(column(f.price), true);
        f.reportOrders(held);
        callChange(orders, parse("{\"property\": \"quantity\", \"desc\": true}"));
        assertEquals(columns(f.quantity, f.price), new ArrayList<>(f.sorted.keySet())); // in its place
        assertEquals(Boolean.FALSE, f.sorted.get(column(f.quantity)));
        assertEquals(Boolean.TRUE, f.sorted.get(column(f.price)));
        callChange(orders, parse("{\"property\": \"quantity\"}")); // no direction: taken off
        assertEquals(columns(f.price), new ArrayList<>(f.sorted.keySet()));
        assertEquals(Collections.singletonList(f.group), f.sortedGroups);
    }

    // what the member refuses, the form is not handed at all: a direction on a noSort property, a list naming one
    // property twice - whichever of the two has a direction - and a `desc` that is no boolean, after a good entry of
    // the same array too
    public void testRefusedSortingsAreNotHandedOn() {
        Fixture f = new Fixture(r -> r.quantity.noSort = true);
        JavaScriptObject orders = field(field(f.sa.controller, "items"), "orders");
        assertTrue(refusesChange(orders, parse("{\"property\": \"quantity\", \"desc\": false}")));
        assertTrue(refusesChange(orders, parse("[{\"property\": \"price\"}, {\"property\": \"price\", \"desc\": true}]")));
        assertTrue(refusesChange(orders, parse("[{\"property\": \"price\", \"desc\": true}, {\"property\": \"price\"}]")));
        assertTrue(refusesChange(orders, parse("{\"property\": \"price\", \"desc\": \"true\"}")));
        assertEquals(0, f.sortedCalls);
        Fixture g = new Fixture(); // both properties may be sorted by: only the second entry's `desc` is wrong
        assertTrue(refusesChange(field(field(g.sa.controller, "items"), "orders"),
                parse("[{\"property\": \"price\", \"desc\": true}, {\"property\": \"quantity\", \"desc\": 1}]")));
        assertEquals(0, g.sortedCalls);
        // ... while taking a noSort sorting off is allowed: a form's default ORDERS may name one
        LinkedHashMap<Column, Boolean> held = new LinkedHashMap<>();
        held.put(column(f.quantity), true);
        f.reportOrders(held);
        callChange(orders, parse("{\"property\": \"quantity\"}"));
        assertEquals(1, f.sortedCalls);
        assertEquals(0, f.sorted.size());
    }

    // one order never touches the sortings it does not name, those the projection leaves out included (one on a
    // property with no name); the array states the whole list
    public void testOneSortingKeepsWhatItDoesNotName() {
        Fixture f = Fixture.withPlatformProperties();
        LinkedHashMap<Column, Boolean> held = new LinkedHashMap<>();
        held.put(column(f.platformUnnamed), true);
        held.put(column(f.price), true);
        f.reportOrders(held);
        JavaScriptObject orders = field(field(f.sa.controller, "items"), "orders");
        callChange(orders, parse("{\"property\": \"price\", \"desc\": true}"));
        assertEquals(columns(f.platformUnnamed, f.price), new ArrayList<>(f.sorted.keySet()));
        callChange(orders, parse("[{\"property\": \"price\", \"desc\": true}]"));
        assertEquals(columns(f.price), new ArrayList<>(f.sorted.keySet()));
    }

    // `desc` is read once: a getter answering differently the second time does not decide the direction
    public void testSortingReadsItsDirectionOnce() {
        Fixture f = new Fixture();
        JavaScriptObject order = flippingOrder("price"); // desc: true, then false
        callChange(field(field(f.sa.controller, "items"), "orders"), order);
        assertEquals(Boolean.FALSE, f.sorted.get(column(f.price))); // descending: the first answer
        assertEquals(1, descReads(order));
    }
    private static native JavaScriptObject flippingOrder(String property) /*-{
        var order = {property: property};
        var desc = function () { desc.reads = (desc.reads || 0) + 1; return desc.reads === 1; };
        Object.defineProperty(order, 'desc', {enumerable: true, get: desc});
        return order;
    }-*/;
    private static native int descReads(JavaScriptObject order) /*-{
        return Object.getOwnPropertyDescriptor(order, 'desc').get.reads || 0;
    }-*/;
    private static List<Column> columns(GPropertyDraw... properties) {
        List<Column> columns = new ArrayList<>();
        for (GPropertyDraw property : properties)
            columns.add(column(property));
        return columns;
    }
    private static boolean refusesChange(JavaScriptObject member, JavaScriptObject argument) {
        try { callChange(member, argument); return false; } catch (RuntimeException e) { return true; }
    }

    // conditions as the form hands them on - the way the client sends them and the server reports them
    private static ArrayList<GPropertyFilterDTO> dtos(List<GPropertyFilter> conditions) {
        ArrayList<GPropertyFilterDTO> dtos = new ArrayList<>();
        for (GPropertyFilter condition : conditions)
            dtos.add(condition.getFilterDTO());
        return dtos;
    }

    public void testConditionsAreOnTheNodeWhereFiltersAre() {
        Fixture f = new Fixture(Fixture::filtersBesideRows);
        assertEquals(0, length(field(f.node(), "filters"))); // always there, empty while the group is unfiltered
        assertNull(field(f.node(), "filtersApplied")); // one list, what the server filters by: nothing held back here
        ArrayList<GPropertyFilter> conditions = new ArrayList<>();
        conditions.add(new GPropertyFilter(new GFilter(f.price), f.group, GGroupObjectValue.EMPTY, PValue.getPValue(15), true, GCompare.GREATER, false));
        JavaScriptObject list = field(f.node(), "list");
        f.reportFilters(dtos(conditions));
        f.projection.flush();
        JavaScriptObject filters = field(f.node(), "filters"); // in the FILTER operator's shape
        assertEquals(1, length(filters));
        assertEquals("price", text(at(filters, 0), "property"));
        assertEquals(">", text(at(filters, 0), "compare"));
        assertEquals(15.0, number(at(filters, 0), "value"), 0.0);
        assertTrue(flag(at(filters, 0), "negation"));
        assertTrue(flag(at(filters, 0), "or"));
        assertSame(list, field(f.node(), "list")); // the rows they leave come from the server, separately
        // b holds a panel property of the group, no filters
        assertNull(field(field(f.snapshots[1], "items"), "filters"));
        assertNotNull(field(field(f.sa.controller, "items"), "filters")); // the member, where the part is
        assertNull(field(field(f.sb.controller, "items"), "filters"));
        f.controller(f.group).updateCurrentKey(f.two); // an unrelated change: the node is new, the array is not
        f.projection.flush();
        assertTrue(flag(f.row("2"), "isCurrent"));
        assertSame(filters, field(f.node(), "filters"));

        // a condition's value is its filter type's: a property named `image` draws its own value as an image, not this
        GPropertyDraw image = f.property(71, "image", f.group, true, f.a);
        image.valueType = new GStringType(GExtInt.UNLIMITED, false, false);
        conditions.add(new GPropertyFilter(new GFilter(image), f.group, GGroupObjectValue.EMPTY, PValue.getPValue("logo.svg"), GCompare.EQUALS));
        f.reportFilters(dtos(conditions));
        f.projection.flush();
        assertEquals("logo.svg", text(at(field(f.node(), "filters"), 1), "value"));
    }

    // the filters say what a condition can be on - beside the conditions, where FILTERS(g) is, even with no property of
    // the group drawn there: the group's properties a condition can be stated on, in the form's order - not an action,
    // not one grouped in columns, not one with no name, and not a file, whose condition can only be taken off -, each
    // with its caption, the type of a condition's value, and what the platform's own editor offers, in the same
    // spelling, its list as it is, the default beside it. A property's entry says nothing of it: filtering is the
    // filters'
    public void testTheFiltersSayWhatAConditionCanBeOn() {
        Fixture f = new Fixture(fixture -> {
            fixture.filtersApart(); // FILTERS(g) is c, where no property of the group is drawn
            fixture.property(73, "approve", fixture.group, true, fixture.a).valueType = GActionType.instance;
            fixture.property(74, "perColumn", fixture.group, true, fixture.a).columnGroupObjects = new ArrayList<>(Arrays.asList(new GGroupObject()));
            fixture.property(75, "hidden", fixture.group, true, fixture.a).integrationSID = null;
            fixture.property(76, "name", fixture.group, true, fixture.a).valueType = new GStringType(GExtInt.UNLIMITED, false, false);
            fixture.property(77, "photo", fixture.group, true, fixture.a).valueType = new GCustomStaticFormatFileType();
        });
        JavaScriptObject properties = field(field(f.snapshots[2], "items"), "filterProperties");
        assertEquals(4, length(properties));
        assertEquals("price", text(at(properties, 0), "property"));
        assertEquals("quantity", text(at(properties, 1), "property"));
        assertEquals("panel", text(at(properties, 2), "property")); // a panel property of the group too
        assertEquals("name", text(at(properties, 3), "property"));
        JavaScriptObject price = at(properties, 0);
        assertEquals("Price", text(price, "caption"));
        assertEquals("number", text(price, "type"));
        assertEquals(f.price.getDefaultCompare().toString(), text(price, "defaultCompare"));
        StringBuilder compares = new StringBuilder();
        for (GCompare compare : f.price.getFilterCompares())
            compares.append(compares.length() == 0 ? "" : ",").append(compare);
        assertEquals(compares.toString(), join(field(price, "compares")));
        assertEquals("string", text(at(properties, 3), "type"));
        assertTrue(isNullField(at(properties, 1), "caption")); // none in the design
        // a property's entry says nothing of filtering; and where the filters are not, there is no catalog
        JavaScriptObject priceEntry = field(field(f.snapshots[0], "items"), "price");
        assertFalse(own(priceEntry, "compares"));
        assertFalse(own(priceEntry, "defaultCompare"));
        assertFalse(has(field(f.snapshots[0], "items"), "filterProperties"));
        // a condition on the file the server states is listed all the same - with no value here: a link to the file,
        // which the test's page cannot make (GwtClientUtils.getAppDownloadURL), for one -, so a view can take it off,
        // with no value, while one with a value is refused: the link is not the file
        ArrayList<GPropertyFilter> conditions = new ArrayList<>();
        conditions.add(new GPropertyFilter(new GFilter(byName(f.form, "photo")), f.group, GGroupObjectValue.EMPTY, null, GCompare.EQUALS));
        f.reportFilters(dtos(conditions)); // as the form has them now
        f.projection.flush();
        JavaScriptObject filters = field(field(f.snapshots[2], "items"), "filters");
        assertEquals(1, length(filters));
        assertEquals("photo", text(at(filters, 0), "property"));
        JavaScriptObject member = field(field(f.sc.controller, "items"), "filters");
        assertRefusal("'photo' is a file", refusedChange(member, parse("{\"property\": \"photo\", \"value\": \"logo.png\"}")));
        callChange(member, parse("{\"property\": \"photo\"}"));
        assertEquals(Arrays.asList("changeFilters items"), f.filtersCalls);
    }

    public void testConditionsApartNameTheColumnsWhereTheRowsAre() {
        Fixture f = new Fixture(Fixture::filtersApart); // FILTERS(g) is c, the rows are in a
        assertNull(field(field(f.snapshots[2], "items"), "list")); // c does not draw the rows
        assertNotNull(field(field(f.sc.controller, "items"), "filters")); // FILTERS(g) apart: its member is there,
        assertNull(field(field(f.sc.controller, "items"), "change"));     // and no row to choose
        assertNull(field(field(f.sa.controller, "items"), "filters"));
        callFilters(field(field(f.sc.controller, "items"), "filters")); // the member hands the form its group's edit
        // ... read where the member is and handed over as the list it makes: a condition with no value is dropped from
        // a list, and on its own takes its property's conditions off (each call merges into the list the one before it
        // handed over, the form telling the part of it)
        assertEquals(Arrays.asList("changeFilters items price< or", "changeFilters items price= not", "changeFilters items"),
                f.filtersCalls);
        JavaScriptObject member = field(field(f.sc.controller, "items"), "filters"); // every mistake is refused there,
        String refused = refusedChange(member, parse("{\"property\": \"nosuch\", \"value\": 1}")); // with the call
        assertTrue(refused, refused.contains("controller.items.filters.change(): ") && refused.contains("nosuch"));
        refused = refusedChange(member, parse("[{\"property\": \"price\", \"value\": 1, \"or\": \"yes\"}]"));
        assertTrue(refused, refused.contains("condition 0: ") && refused.contains("'or' must be true or false"));
        refused = refusedChange(member, parse("{\"property\": \"price\", \"compare\": \"IN ARRAY\", \"value\": 1}"));
        assertTrue(refused, refused.contains("'IN ARRAY' is no comparison of a filter condition"));
        assertEquals(3, f.filtersCalls.size()); // ... and the form is handed nothing
        assertNull(field(f.node(), "filters")); // not a part of the rows
        assertEquals(0, length(field(field(f.snapshots[2], "items"), "filters")));
        // c carries no column: a condition names the group's property
        callChange(member, parse("{\"property\": \"price\", \"value\": 4}"));
        assertSame(f.price, f.stated.get(0).column.property);
        ArrayList<GPropertyFilter> conditions = new ArrayList<>();
        conditions.add(new GPropertyFilter(new GFilter(f.price), f.group, GGroupObjectValue.EMPTY, PValue.getPValue(3), GCompare.LESS));
        f.reportFilters(dtos(conditions));
        f.projection.flush();
        JavaScriptObject filters = field(field(f.snapshots[2], "items"), "filters");
        assertEquals(1, length(filters));
        assertEquals("price", text(at(filters, 0), "property"));
    }

    public void testOneConditionIsMergedWhereThePartIs() {
        Fixture f = new Fixture(Fixture::filtersBesideRows);
        GPropertyFilter low = new GPropertyFilter(new GFilter(f.price), f.group, GGroupObjectValue.EMPTY, PValue.getPValue(1), GCompare.GREATER);
        GPropertyFilter high = new GPropertyFilter(new GFilter(f.price), f.group, GGroupObjectValue.EMPTY, PValue.getPValue(5), GCompare.LESS);
        GPropertyFilter amount = new GPropertyFilter(new GFilter(f.quantity), f.group, GGroupObjectValue.EMPTY, PValue.getPValue(2), GCompare.EQUALS);
        // a range on price - two conditions - and one on quantity, what the server filters by: what one condition
        // merges into
        f.reportFilters(dtos(Arrays.asList(low, high, amount)));
        JavaScriptObject member = field(field(f.sa.controller, "items"), "filters");
        callChange(member, parse("{\"property\": \"price\", \"compare\": \">=\", \"value\": 3}"));
        // the FIRST on price, in place, and the other bound kept as it was
        assertEquals("changeFilters items price>= price< quantity=", f.filtersCalls.get(0));
        // (each merged into the list the form has: the one the call before it handed over)
        callChange(member, parse("{\"property\": \"panel\", \"value\": 7}")); // a property with none: appended
        assertEquals("changeFilters items price>= price< quantity= panel=", f.filtersCalls.get(1));
        callChange(member, parse("{\"property\": \"price\"}")); // no value: every condition on price goes
        assertEquals("changeFilters items quantity= panel=", f.filtersCalls.get(2));
        callChange(member, valueReadOnce()); // a value read a second time would be none: it is read once
        assertEquals("changeFilters items quantity= panel= price=", f.filtersCalls.get(3));
        assertFalse(f.stated.get(2).nullValue());
    }

    // the form tells what shows a group's filters of every list it sends, before the answer (GFormController
    // .applyCurrentFilters): a second change merges into the list the first one sent, not into the one before it - so
    // two changes in a row both stand
    public void testAChangeMergesIntoTheListTheFormLastSent() {
        Fixture f = new Fixture(Fixture::filtersBesideRows);
        JavaScriptObject member = field(field(f.sa.controller, "items"), "filters");
        // the double keeps the list, the form's now, and tells the part, as the form does once it has sent it
        callChange(member, parse("{\"property\": \"price\", \"compare\": \">\", \"value\": 1}"));
        f.projection.flush();
        assertEquals("price", text(at(field(f.node(), "filters"), 0), "property")); // shown at once
        callChange(member, parse("{\"property\": \"quantity\", \"value\": 2}"));
        assertEquals("changeFilters items price> quantity=", f.filtersCalls.get(1));
    }

    // ===== the projection's non-obvious behaviour, each pinned (React: common's QA, 2026-10-07)

    // a condition set elsewhere that compares with no value - the platform's panel can state one - is listed with a
    // null `value`, so a view that hands the list back takes it off; on a LOGICAL property the filter type's JS value
    // of nothing is `false`, so there it is listed as false
    public void testAConditionWithNoValueIsListedAsNullAndOnALogicalPropertyAsFalse() {
        Fixture f = new Fixture(fixture -> {
            fixture.filtersBesideRows();
            fixture.property(74, "active", fixture.group, true, fixture.a).valueType = GLogicalType.instance;
        });
        ArrayList<GPropertyFilter> conditions = new ArrayList<>();
        conditions.add(new GPropertyFilter(new GFilter(f.price), f.group, GGroupObjectValue.EMPTY, null, GCompare.EQUALS));
        conditions.add(new GPropertyFilter(new GFilter(byName(f.form, "active")), f.group, GGroupObjectValue.EMPTY, null, GCompare.EQUALS));
        f.reportFilters(dtos(conditions));
        f.projection.flush();
        JavaScriptObject filters = field(f.node(), "filters");
        assertEquals(2, length(filters));
        assertEquals("price", text(at(filters, 0), "property"));
        assertTrue(isNullField(at(filters, 0), "value"));
        assertEquals("active", text(at(filters, 1), "property"));
        assertTrue(isFalseField(at(filters, 1), "value"));
    }

    // a condition set elsewhere on what no condition the projection names can be on - an action, a property grouped in
    // columns, a property with no integration SID - is left out of `filters`, the rest listed in their order, and the
    // console says which and why, once per property however many times the list comes
    public void testAConditionTheProjectionCannotNameIsLeftOutAndSaidOnce() {
        Fixture f = new Fixture(fixture -> {
            fixture.filtersBesideRows();
            fixture.property(75, "approve", fixture.group, true, fixture.a).valueType = GActionType.instance;
            fixture.property(76, "perColumn", fixture.group, true, fixture.a).columnGroupObjects = new ArrayList<>(Arrays.asList(new GGroupObject()));
            fixture.property(77, "hidden", fixture.group, true, fixture.a).integrationSID = null;
        });
        ArrayList<GPropertyFilter> conditions = new ArrayList<>();
        conditions.add(new GPropertyFilter(new GFilter(byName(f.form, "approve")), f.group, GGroupObjectValue.EMPTY, PValue.getPValue(1), GCompare.EQUALS));
        conditions.add(new GPropertyFilter(new GFilter(f.price), f.group, GGroupObjectValue.EMPTY, PValue.getPValue(1), GCompare.GREATER));
        conditions.add(new GPropertyFilter(new GFilter(byName(f.form, "perColumn")), f.group, GGroupObjectValue.EMPTY, PValue.getPValue(1), GCompare.EQUALS));
        conditions.add(new GPropertyFilter(new GFilter(byName(f.form, "hidden")), f.group, GGroupObjectValue.EMPTY, PValue.getPValue(1), GCompare.EQUALS));
        JavaScriptObject errors = spyConsoleErrors();
        try {
            f.reportFilters(dtos(conditions));
            f.projection.flush();
            JavaScriptObject filters = field(f.node(), "filters");
            assertEquals(1, length(filters));
            assertEquals("price", text(at(filters, 0), "property"));
            assertEquals(3, countMatching(errors, "data.items.filters"));
            assertEquals(1, countMatching(errors, "an action"));
            assertEquals(1, countMatching(errors, "grouped in columns"));
            assertEquals(1, countMatching(errors, "no integration SID"));
            conditions.remove(1); // another list with the same ones in it: nothing said twice
            f.reportFilters(dtos(conditions));
            f.projection.flush();
            assertEquals(3, countMatching(errors, "data.items.filters"));
        } finally {
            restoreConsole(errors);
        }
    }

    // what the member refuses, the form is not handed at all - a condition on an action or on a property grouped in
    // columns, an unknown field, an unknown compare, a `negation` that is no boolean - and the message quotes the call
    public void testTheFiltersMemberRefusesWhatNoConditionCanBe() {
        Fixture f = new Fixture(fixture -> {
            fixture.filtersBesideRows();
            fixture.property(75, "approve", fixture.group, true, fixture.a).valueType = GActionType.instance;
            fixture.property(76, "perColumn", fixture.group, true, fixture.a).columnGroupObjects = new ArrayList<>(Arrays.asList(new GGroupObject()));
        });
        JavaScriptObject member = field(field(f.sa.controller, "items"), "filters");
        String refused = refusedChange(member, parse("{\"property\": \"approve\", \"value\": 1}"));
        assertTrue(refused, refused != null && refused.contains("controller.items.filters.change(): ") && refused.contains("an action"));
        refused = refusedChange(member, parse("{\"property\": \"perColumn\", \"value\": 1}"));
        assertTrue(refused, refused != null && refused.contains("grouped in columns"));
        refused = refusedChange(member, parse("{\"property\": \"price\", \"value\": 1, \"vaule\": 2}"));
        assertTrue(refused, refused != null && refused.contains("unknown field 'vaule'"));
        refused = refusedChange(member, parse("{\"property\": \"price\", \"compare\": \"~\", \"value\": 1}"));
        assertTrue(refused, refused != null && refused.contains("unknown compare '~'"));
        refused = refusedChange(member, parse("{\"property\": \"price\", \"value\": 1, \"negation\": \"yes\"}"));
        assertTrue(refused, refused != null && refused.contains("'negation' must be true or false"));
        refused = refusedChange(member, parse("[{\"property\": \"price\", \"value\": 1}, {\"property\": \"approve\", \"value\": 1}]"));
        assertTrue(refused, refused != null && refused.contains("condition 1: ")); // one wrong entry refuses the whole list
        assertEquals(0, f.filtersCalls.size());
    }

    // a sorting set elsewhere on a property with no integration SID is left out of `orders`, and said once; a sorting a
    // view asks for by a name its group does not carry is refused, and the form handed nothing
    public void testASortingTheProjectionCannotNameIsLeftOutAndAnUnknownOneRefused() {
        Fixture f = new Fixture(fixture -> fixture.property(77, "hidden", fixture.group, true, fixture.a).integrationSID = null);
        LinkedHashMap<Column, Boolean> held = new LinkedHashMap<>();
        held.put(column(byName(f.form, "hidden")), true);
        held.put(column(f.price), false);
        JavaScriptObject errors = spyConsoleErrors();
        try {
            f.reportOrders(held);
            f.projection.flush();
            JavaScriptObject sorted = field(f.node(), "orders");
            assertEquals(1, length(sorted));
            assertEquals("price", text(at(sorted, 0), "property"));
            assertEquals(1, countMatching(errors, "data.items.orders"));
            LinkedHashMap<Column, Boolean> next = new LinkedHashMap<>(held);
            next.remove(column(f.price)); // another list with the same one in it: nothing said twice
            f.reportOrders(next);
            f.projection.flush();
            assertEquals(1, countMatching(errors, "data.items.orders"));
        } finally {
            restoreConsole(errors);
        }
        assertTrue(refusesChange(field(field(f.sa.controller, "items"), "orders"), parse("{\"property\": \"nosuch\", \"desc\": true}")));
        assertEquals(0, f.sortedCalls);
    }

    // a tree's sortings are the tree's too, as its user filters are: one part on the tree's node where its rows are
    // drawn, data.<tree>.orders - the sortings of all its groups, group by group, top down, each property named by its
    // group's name and its own -, and a list its member states is the tree's, each group's its own, a group none of it
    // names sorted by nothing. The groups' nodes carry none of it
    public void testATreesSortingsAreOnTheTreesNode() {
        TreeFixture f = new TreeFixture(fixture -> {
            fixture.column(63, "price", fixture.item);
            fixture.column(64, "rank", fixture.cat);
        });
        f.apply(f.page());
        assertEquals(0, length(field(field(f.data, "t"), "orders")));
        assertFalse(has(field(f.data, "cat"), "orders"));
        assertFalse(has(field(f.data, "item"), "orders"));
        assertFalse(has(field(f.sa.controller, "cat"), "orders"));
        assertFalse(has(field(f.sa.controller, "item"), "orders"));
        // the item group's sortings, then the cat group's: listed in the tree's order, each group's as the form has it
        LinkedHashMap<Column, Boolean> items = new LinkedHashMap<>();
        items.put(column(byName(f.form, "price")), false);
        f.verbs.orders.report(f.item, items);
        LinkedHashMap<Column, Boolean> cats = new LinkedHashMap<>();
        cats.put(column(byName(f.form, "rank")), true);
        f.verbs.orders.report(f.cat, cats);
        f.projection.flush();
        JavaScriptObject listed = field(field(f.data, "t"), "orders");
        assertEquals(2, length(listed));
        assertEquals("cat.rank", text(at(listed, 0), "property"));
        assertTrue(isFalse(at(listed, 0), "desc"));
        assertEquals("item.price", text(at(listed, 1), "property"));
        assertTrue(flag(at(listed, 1), "desc"));
        // one part for the tree
        assertSame(f.controllers.userOrders.get(f.cat), f.controllers.userOrders.get(f.item));
        // one order, merged into the tree's list - the other group's stays -, stated as the tree's
        JavaScriptObject member = field(field(f.sa.controller, "t"), "orders");
        callChange(member, parse("{\"property\": \"item.price\", \"desc\": false}"));
        assertEquals(Arrays.asList("cat item rank price"), f.verbs.sorted);
        callChange(member, parse("[{\"property\": \"item.price\", \"desc\": true}]"));
        assertEquals(Arrays.asList("cat item rank price", "cat item price desc"), f.verbs.sorted);
        assertEquals(0, f.verbs.orders.get(f.cat).size()); // the cat group, named by none of it, sorted by nothing
        // a name with no group, or a group of no such tree, names nothing
        assertTrue(refusesChange(member, parse("{\"property\": \"price\", \"desc\": true}")));
        assertTrue(refusesChange(member, parse("{\"property\": \"other.price\", \"desc\": true}")));
    }

    // a tree's groups share one FILTERS box, and its user filters are the tree's: one part on the tree's node,
    // data.<tree> - the conditions of all its groups, in the tree's order of the groups, each property named by its
    // group's name and its own -, and a list its member states is the tree's, one request for all the groups. The
    // groups' nodes carry none of it
    public void testATreesFiltersAreOnTheTreesNode() {
        TreeFixture f = new TreeFixture(fixture -> {
            fixture.filtersIn(fixture.a);
            fixture.column(63, "price", fixture.item);
            fixture.column(64, "rank", fixture.cat);
        });
        f.apply(f.page());
        assertEquals(0, length(field(field(f.data, "t"), "filters")));
        // what a condition can be on: the properties of all its groups, the upper group's first, named as `filters`
        // does
        JavaScriptObject properties = field(field(f.data, "t"), "filterProperties");
        assertEquals(2, length(properties));
        assertEquals("cat.rank", text(at(properties, 0), "property"));
        assertEquals("item.price", text(at(properties, 1), "property"));
        assertFalse(has(field(f.data, "cat"), "filters"));
        assertFalse(has(field(f.data, "item"), "filters"));
        assertFalse(has(field(f.sa.controller, "cat"), "filters"));
        assertFalse(has(field(f.sa.controller, "item"), "filters"));
        // the item group's list, then the cat group's: listed in the tree's order, each group's as the form has it
        ArrayList<GPropertyFilter> items = new ArrayList<>();
        items.add(new GPropertyFilter(new GFilter(byName(f.form, "price")), f.item, GGroupObjectValue.EMPTY, PValue.getPValue(5), GCompare.GREATER));
        f.report(f.item, dtos(items));
        ArrayList<GPropertyFilter> cats = new ArrayList<>();
        cats.add(new GPropertyFilter(new GFilter(byName(f.form, "rank")), f.cat, GGroupObjectValue.EMPTY, PValue.getPValue(2), GCompare.EQUALS));
        f.report(f.cat, dtos(cats));
        f.projection.flush();
        JavaScriptObject listed = field(field(f.data, "t"), "filters");
        assertEquals(2, length(listed));
        assertEquals("cat.rank", text(at(listed, 0), "property"));
        assertEquals("item.price", text(at(listed, 1), "property"));
        // one part for the tree
        assertSame(f.controllers.userFilters.get(f.cat), f.controllers.userFilters.get(f.item));
        // one condition, merged into the tree's list - the other group's condition stays -, sent as the tree's
        JavaScriptObject member = field(field(f.sa.controller, "t"), "filters");
        callChange(member, parse("{\"property\": \"item.price\", \"compare\": \"<\", \"value\": 9}"));
        assertEquals(Arrays.asList("cat item rank= price<"), f.verbs.filtered);
        callChange(member, parse("[]"));
        assertEquals(Arrays.asList("cat item rank= price<", "cat item"), f.verbs.filtered); // all its groups', cleared
        // a name with no group, or a group of no such tree, names nothing
        assertTrue(refusesChange(member, parse("{\"property\": \"price\", \"value\": 9}")));
        assertTrue(refusesChange(member, parse("{\"property\": \"other.price\", \"value\": 9}")));
        assertTrue(refusesChange(member, parse("{\"property\": \"cat.price\", \"value\": 9}")));
        assertEquals(2, f.verbs.filtered.size());
    }

    // ... and a change of one condition merges into the lists the form has, which the tree's list it sends last is -
    // the condition of the other group it stated included -, so two changes in a row both stand, in a batch too
    public void testATreesChangeMergesIntoTheListsTheFormLastSent() {
        TreeFixture f = new TreeFixture(fixture -> {
            fixture.filtersIn(fixture.a);
            fixture.column(63, "price", fixture.item);
            fixture.column(64, "rank", fixture.cat);
        });
        f.apply(f.page());
        JavaScriptObject member = field(field(f.sa.controller, "t"), "filters");
        callChange(member, parse("{\"property\": \"item.price\", \"compare\": \">\", \"value\": 1}"));
        callChange(member, parse("{\"property\": \"cat.rank\", \"value\": 2}"));
        batch(f.sa.controller, () -> {
            callChange(member, parse("{\"property\": \"item.price\", \"compare\": \"<\", \"value\": 9}"));
            callChange(member, parse("{\"property\": \"cat.rank\"}"));
        });
        assertEquals(Arrays.asList("cat item price>", "cat item price> rank=", "cat item rank= price<", "cat item price<"),
                f.verbs.filtered);
    }

    // a tree's condition names a property by its group's name and its own, c.name, read up to the FIRST dot: a group's
    // name has none, as its node is named, while an integration name may - a property the platform draws, which
    // nothing projects by name, keeps an EXTID with a dot -, and such a name is taken back as `filters` gives it
    public void testATreesConditionNameIsReadUpToTheGroupsDot() {
        TreeFixture f = new TreeFixture(t -> {
            t.treeByThePlatform(); // the platform draws the tree, a view what its FILTERS box holds
            t.filtersIn(t.a);
            GPropertyDraw price = t.column(63, "unit.price", t.item); // ... and the item's column is the platform's
            t.a.children.remove(price); price.container = t.form.mainContainer; t.form.mainContainer.children.add(price);
        });
        f.projection.flush();
        JavaScriptObject properties = field(field(f.data, "t"), "filterProperties");
        assertEquals("item.unit.price", text(at(properties, 0), "property"));
        JavaScriptObject member = field(field(f.sa.controller, "t"), "filters");
        callChange(member, parse("{\"property\": \"item.unit.price\", \"value\": 9}"));
        assertEquals(Arrays.asList("cat item unit.price="), f.verbs.filtered);
        assertTrue(refusesChange(member, parse("{\"property\": \"unit.price\", \"value\": 9}"))); // no group's name
        assertEquals(1, f.verbs.filtered.size());
    }

    // what the platform's filter input controller and toolbar ask of a TREE React draws they ask the tree as a whole
    // (GTreeController): the columns of all its groups, in the tree's order, whatever the form's; a new
    // condition on the first column of the current node's group - the group whose current object was set last, by the
    // server or by the view: a row even it already was -, in its current row, with a column's value in its group's
    // current row; the conditions stated as the tree's, each group's its own list
    public void testATreeReactDrawsAnswersThePlatformsFilterInputController() {
        GPropertyDraw[] columns = new GPropertyDraw[3];
        TreeFixture f = new TreeFixture(t -> { // declared bottom-up: the item's columns before the category's
            columns[1] = t.column(64, "itemName", t.item);
            columns[2] = t.column(65, "price", t.item);
            columns[0] = t.column(63, "catName", t.cat);
        });
        GFormChanges page = f.page();
        put(page, columns[0], f.c1, 1); put(page, columns[0], f.c2, 2);
        put(page, columns[1], f.i15, 15); put(page, columns[1], f.i27, 27);
        put(page, columns[2], f.i15, 150); put(page, columns[2], f.i27, 270);
        f.apply(page);
        GTreeController tree = f.controllers.treeRows.get(f.tree);

        List<Pair<Column, String>> filterColumns = tree.getFilterColumns();
        assertEquals(3, filterColumns.size()); // the top group's, then the group's below
        assertEquals(new Column(columns[0], GGroupObjectValue.EMPTY), filterColumns.get(0).first);
        assertEquals(new Column(columns[1], GGroupObjectValue.EMPTY), filterColumns.get(1).first);
        assertEquals(new Column(columns[2], GGroupObjectValue.EMPTY), filterColumns.get(2).first);
        assertSame(f.cat, tree.getSelectedGroupObject()); // no current object yet: the top group, on no row
        assertSame(columns[0], tree.getSelectedFilterProperty());
        assertEquals(GGroupObjectValue.EMPTY, tree.getSelectedColumnKey());
        assertEquals(-1, tree.getSelectedRow());

        // as the server reports them: of a tree, one group in an answer, the deepest it read - here the categories,
        // then the items
        GFormChanges category = new GFormChanges();
        category.objects.put(f.cat, f.c2);
        f.apply(category);
        GFormChanges current = new GFormChanges();
        current.objects.put(f.item, f.i27);
        f.apply(current);
        assertTrue(tree.getSelectedRow() >= 0);
        assertSame(f.item, tree.getSelectedGroupObject());
        assertSame(columns[1], tree.getSelectedFilterProperty());
        assertEquals(PValue.getPValue(270), tree.getSelectedValue(columns[2], GGroupObjectValue.EMPTY));
        // a column's value in its own group's current row
        assertEquals(PValue.getPValue(2), tree.getSelectedValue(columns[0], GGroupObjectValue.EMPTY));

        f.controllers.groups.get(f.cat).changeCurrentKey(f.c1); // the view's own choice, as the form hands it on
        assertSame(f.cat, tree.getSelectedGroupObject());
        f.controllers.groups.get(f.item).updateCurrentKey(f.i27); // the server reporting a row it already was
        assertSame(f.item, tree.getSelectedGroupObject());
        // the top group of an empty tree, reported on a path of NULLs: the top group, on no row - no condition starts
        f.controllers.groups.get(f.cat).updateCurrentKey(new GGroupObjectValue(60, (Serializable) null));
        assertSame(f.cat, tree.getSelectedGroupObject());
        assertEquals(-1, tree.getSelectedRow());

        ArrayList<GPropertyFilter> conditions = new ArrayList<>();
        conditions.add(new GPropertyFilter(new GFilter(columns[2]), f.item, GGroupObjectValue.EMPTY, PValue.getPValue(100), GCompare.GREATER));
        tree.changeFilter(conditions);
        assertEquals(Arrays.asList("cat item price>"), f.verbs.filtered);
        assertFalse(tree.focusFirstWidget(FocusUtils.Reason.APPLYFILTER)); // the focus is the view's
    }

    // a row of a group below carries the path above it: made current - by the view, or by the server, which reports
    // the deepest group's objects alone -, it makes the rows on that path current in their groups too, as the
    // platform's tree selects one node - so a column's value in its group's current row is the path's, not the row the
    // group was on before
    public void testARowOfAGroupBelowMakesItsPathCurrent() {
        GPropertyDraw[] columns = new GPropertyDraw[2];
        TreeFixture f = new TreeFixture(t -> {
            columns[0] = t.column(63, "catName", t.cat);
            columns[1] = t.column(64, "price", t.item);
        });
        GFormChanges page = f.page();
        put(page, columns[0], f.c1, 1); put(page, columns[0], f.c2, 2);
        put(page, columns[1], f.i15, 150); put(page, columns[1], f.i27, 270);
        page.objects.put(f.cat, f.c1);
        f.apply(page);
        GTreeController tree = f.controllers.treeRows.get(f.tree);
        assertEquals(PValue.getPValue(1), tree.getSelectedValue(columns[0], GGroupObjectValue.EMPTY));
        f.controllers.groups.get(f.item).changeCurrentKey(f.i27); // the view's choice under the other category
        assertEquals(PValue.getPValue(2), tree.getSelectedValue(columns[0], GGroupObjectValue.EMPTY));
        assertEquals(PValue.getPValue(270), tree.getSelectedValue(columns[1], GGroupObjectValue.EMPTY));
        f.projection.flush();
        assertTrue(flag(f.row(f.cat, f.c2), "isCurrent"));
        assertFalse(flag(f.row(f.cat, f.c1), "isCurrent"));
        assertTrue(flag(f.row(f.item, f.i27), "isCurrent"));
        GFormChanges answer = new GFormChanges(); // the server reporting an item under the first category: its alone
        answer.objects.put(f.item, f.i15);
        f.apply(answer);
        assertEquals(PValue.getPValue(1), tree.getSelectedValue(columns[0], GGroupObjectValue.EMPTY));
        assertTrue(flag(f.row(f.cat, f.c1), "isCurrent"));
        assertFalse(flag(f.row(f.cat, f.c2), "isCurrent"));
    }

    // ... and leaves the groups below it on no row, as the server has them - it drops their objects when a row above
    // is chosen, and reports nothing for them -: no row of theirs says `isCurrent`, and a column of theirs has no
    // current value to start a condition with, until a row is chosen there again - by the view, or by the server
    public void testARowMadeCurrentLeavesTheGroupsBelowOnNoRow() {
        GPropertyDraw[] columns = new GPropertyDraw[1];
        TreeFixture f = new TreeFixture(t -> columns[0] = t.column(64, "price", t.item));
        GFormChanges page = f.page();
        put(page, columns[0], f.i15, 150); put(page, columns[0], f.i27, 270);
        page.objects.put(f.item, f.i15); // the server reporting an item: the path above it current with it
        f.apply(page);
        GTreeController tree = f.controllers.treeRows.get(f.tree);
        assertEquals(PValue.getPValue(150), tree.getSelectedValue(columns[0], GGroupObjectValue.EMPTY));
        f.controllers.groups.get(f.cat).changeCurrentKey(f.c2); // the view's choice of a category
        assertSame(f.cat, tree.getSelectedGroupObject());
        assertNull(tree.getSelectedValue(columns[0], GGroupObjectValue.EMPTY)); // no item is current now
        f.projection.flush();
        assertTrue(flag(f.row(f.cat, f.c2), "isCurrent"));
        assertFalse(flag(f.row(f.item, f.i15), "isCurrent"));
        GFormChanges answer = new GFormChanges(); // the server reporting an item under it
        answer.objects.put(f.item, f.i27);
        f.apply(answer);
        assertTrue(flag(f.row(f.item, f.i27), "isCurrent"));
        answer = new GFormChanges(); // ... and then a category: the item's group on no row again
        answer.objects.put(f.cat, f.c1);
        f.apply(answer);
        assertFalse(flag(f.row(f.item, f.i27), "isCurrent"));
        assertNull(tree.getSelectedValue(columns[0], GGroupObjectValue.EMPTY));
    }

    // ... and what the platform's toolbar's two buttons open over these rows (GTreeController), as over the platform's
    // tree: the current node and every node under it, as the tree's own member does
    // (testATreesOwnMemberOpensItsCurrentNode), or the tree from the current node's group down - nothing with no
    // current node
    public void testATreesToolbarOpensItsCurrentNodeOrAllOfIt() {
        TreeFixture f = new TreeFixture(t -> t.cat.isRecursive = true); // a category hangs under a category
        GGroupObjectValue c3 = new GGroupObjectValue(60, 3), c4 = new GGroupObjectValue(60, 4);
        f.apply(nestedPage(f, c3, c4));
        GTreeController tree = f.controllers.treeRows.get(f.tree);
        tree.expandTree(); // no node current yet
        assertEquals(0, f.verbs.asked.size());
        f.controllers.groups.get(f.cat).updateCurrentKey(f.c1);
        tree.expandCurrent(); // the platform's button: published once its event is over
        assertEquals(Arrays.asList("expand current cat 1"), f.verbs.asked);
        f.projection.publish(); // as the projection's own, then
        assertTrue(flag(f.row(f.cat, c4), "expanded")); // under it all the way down
        assertTrue(isFalse(f.row(f.cat, f.c2), "expanded")); // ... and nothing else

        tree.expandTree();
        assertEquals(Arrays.asList("expand current cat 1", "expand all cat"), f.verbs.asked);
        f.projection.publish();
        assertTrue(flag(f.row(f.cat, f.c2), "expanded"));
        f.controllers.groups.get(f.item).updateCurrentKey(f.i15); // the current node an item: from its group down
        tree.expandTree();
        assertEquals("expand all item", f.verbs.asked.get(2));
    }
    // a recursive tree's page: 3 under 1, 4 under 3, 1's item loaded; 2 closed - all of them with children
    private static GFormChanges nestedPage(TreeFixture f, GGroupObjectValue c3, GGroupObjectValue c4) {
        GFormChanges page = new GFormChanges();
        page.gridObjects.put(f.cat, rows(c4, f.c1, f.c2, c3)); // a row before the one it hangs under
        page.parentObjects.put(f.cat, rows(c3, GGroupObjectValue.EMPTY, GGroupObjectValue.EMPTY, f.c1));
        NativeHashMap<GGroupObjectValue, Integer> expandables = counts(f.c1, 2, f.c2, 1);
        expandables.put(c3, 1); expandables.put(c4, 1);
        page.expandables.put(f.cat, expandables);
        page.gridObjects.put(f.item, rows(f.i15));
        page.parentObjects.put(f.item, rows(GGroupObjectValue.EMPTY));
        return page;
    }

    // where React draws a tree's rows the tree has a node of its own there, as a group has where its rows are - here
    // with nothing else of the tree drawn, no toolbar, no filters -, and on it the one verb only the tree has: its
    // current node opened, and every node under it - read when it is called: nothing before a node is current, nor for
    // a node with no children -, shown so at once all the way down, and waiting, until the answer to its request says
    // what the rows are: an answer to an earlier request leaves it as asked
    public void testATreesOwnMemberOpensItsCurrentNode() {
        TreeFixture f = new TreeFixture(t -> t.cat.isRecursive = true);
        GGroupObjectValue c3 = new GGroupObjectValue(60, 3), c4 = new GGroupObjectValue(60, 4);
        GFormChanges page = nestedPage(f, c3, c4);
        f.apply(page);
        JavaScriptObject tree = field(f.sa.controller, "t");
        assertEquals("expandCurrent,orders", keysOf(tree)); // the tree's own and its sortings: no toolbar is drawn here
        callAll(tree, "expandCurrent"); // no current node: nothing to open
        assertTrue(f.verbs.asked.isEmpty());

        f.controllers.groups.get(f.cat).updateCurrentKey(f.c1);
        f.projection.flush();
        int publications = f.publications;
        callAll(tree, "expandCurrent");
        assertEquals(Arrays.asList("expand current cat 1"), f.verbs.asked);
        assertEquals(publications + 1, f.publications); // shown at once
        assertTrue(flag(f.row(f.cat, c3), "expanded")); // ... all the way down
        assertTrue(flag(f.row(f.cat, c4), "expanded"));
        assertTrue(flag(f.row(f.cat, c4), "loading")); // ... its children asked for
        assertTrue(isFalse(f.row(f.cat, f.c2), "expanded")); // ... and nothing else
        GFormChanges earlier = new GFormChanges(); // an answer to a request before it: still as asked
        earlier.gridObjects.put(f.cat, page.gridObjects.get(f.cat));
        earlier.parentObjects.put(f.cat, page.parentObjects.get(f.cat));
        f.answer(earlier, 0);
        assertTrue(flag(f.row(f.cat, c4), "expanded"));
        assertTrue(flag(f.row(f.cat, c4), "loading"));
        GGroupObjectValue c5 = new GGroupObjectValue(60, 5);
        GFormChanges answer = new GFormChanges(); // its own: 4's child has come, and 4 is open as the rows say
        answer.gridObjects.put(f.cat, rows(c4, f.c1, f.c2, c3, c5));
        answer.parentObjects.put(f.cat, rows(c3, GGroupObjectValue.EMPTY, GGroupObjectValue.EMPTY, f.c1, c4));
        f.answer(answer, 1);
        assertTrue(flag(f.row(f.cat, c4), "expanded"));
        assertFalse(own(f.row(f.cat, c4), "loading"));

        f.controllers.groups.get(f.item).updateCurrentKey(f.i15); // a node with no children: nothing to open
        callAll(tree, "expandCurrent");
        assertEquals(1, f.verbs.asked.size());
    }

    // ... read when it is called, in a batch too: the row a view made current before it in the batch is the node it
    // opens - both going in the batch's one request, in that order, as the server makes them -, and the projection is
    // published once, when the batch ends
    public void testInABatchTheTreesMemberOpensTheRowMadeCurrentBeforeIt() {
        TreeFixture f = new TreeFixture();
        f.apply(closedPage(f)); // 1 open, 2 closed - both with children
        JavaScriptObject tree = field(f.sa.controller, "t"), cat = field(f.sa.controller, "cat");
        int publications = f.publications;
        batch(f.sa.controller, () -> {
            callNode(cat, "change", f.row(f.cat, f.c2));
            callAll(tree, "expandCurrent");
        });
        assertEquals(Arrays.asList("batch", "current cat 2", "expand current cat 2", "sent"), f.verbs.asked);
        assertEquals(publications + 1, f.publications); // once, when it ended
        assertTrue(flag(f.row(f.cat, f.c2), "isCurrent"));
        assertTrue(flag(f.row(f.cat, f.c2), "expanded")); // shown opened at once
        assertTrue(flag(f.row(f.cat, f.c2), "loading"));
        assertTrue(sameField(field(f.data, "t"), "current", f.row(f.cat, f.c2), "key")); // the tree's current row
        f.answer(openPage(f), (int) f.verbs.requestIndex); // the batch's answer: 2's child has come
        assertTrue(flag(f.row(f.cat, f.c2), "expanded"));
        assertFalse(own(f.row(f.cat, f.c2), "loading"));
    }

    // what opens a tree in bulk has one member each, whoever draws what: where React draws the rows, the tree's own
    // expandCurrent() and the top group's expandAll() - a view's part of the toolbar over those rows gives no second
    // pair, in the same container or in another, where it opens nothing at all (given up: one member each, wherever the
    // toolbar is drawn) -; where the platform draws them, a view's part of the toolbar has the platform's two buttons
    // as members, opening the platform's tree - no group's node there, nor the tree's current row -; and a toolbar the
    // design hides, or makes platform-drawn, is the platform's, given the rows React draws as its own buttons are
    public void testATreeIsOpenedInBulkByOneMemberEach() {
        TreeFixture same = new TreeFixture(t -> t.toolbarIn(t.a)); // the rows and the toolbar in one container
        same.apply(closedPage(same));
        JavaScriptObject tree = field(same.sa.controller, "t");
        assertEquals("expandCurrent,export,filterControlsVisible,orders", keysOf(tree));
        assertTrue(has(field(same.sa.controller, "cat"), "expandAll")); // the whole tree: the top group's
        same.controllers.groups.get(same.cat).updateCurrentKey(same.c2);
        callAll(tree, "expandCurrent");
        assertEquals(Arrays.asList("expand current cat 2"), same.verbs.asked); // the rows React draws opened

        TreeFixture split = new TreeFixture(t -> t.toolbarIn(t.b())); // ... in two: the rows' and the toolbar's
        assertEquals("expandCurrent,orders", keysOf(field(split.sa.controller, "t")));
        assertEquals("export,filterControlsVisible", keysOf(field(split.sb.controller, "t")));

        TreeFixture platform = new TreeFixture(t -> { t.treeByThePlatform(); t.toolbarIn(t.a); }); // its tree's
        JavaScriptObject platformTree = field(platform.sa.controller, "t");
        assertEquals("expandCurrent,expandTree,export,filterControlsVisible", keysOf(platformTree));
        assertFalse(has(platform.sa.controller, "cat")); // the rows are the platform's: no node of a group here
        platform.projection.flush();
        assertFalse(has(field(platform.data, "t"), "current")); // ... nor the tree's current row: the platform's has it
        callAll(platformTree, "expandCurrent");
        callAll(platformTree, "expandTree");
        assertEquals(Arrays.asList("expand current", "expand tree"), platform.controllers.platformTree);

        TreeFixture hidden = new TreeFixture(t -> { t.toolbarIn(t.a); t.tree.toolbar.visible = false; });
        TreeFixture platformDrawn = new TreeFixture(t -> { t.toolbarIn(t.a); t.tree.toolbar.platformDrawn = true; });
        for (TreeFixture f : Arrays.asList(hidden, platformDrawn)) {
            TreeToolbarDouble toolbar = (TreeToolbarDouble) f.controllers.treeToolbars.get(f.tree);
            assertEquals(Arrays.asList(f.controllers.treeRows.get(f.tree)), toolbar.treeRows);
            assertEquals("expandCurrent,orders", keysOf(field(f.sa.controller, "t")));
        }
    }

    // where a view draws a TREE's toolbar, the toolbar is a part of the TREE there, on the tree's node, one for all its
    // groups: what its filter button shows - the platform's controls, shown and hidden as a group's toolbar's button
    // shows and hides them - is on the tree's node, and its buttons are members on the tree's, whatever the flags say,
    // as the platform's tree shows them - over the rows React draws, as here, the filter button and the export, the
    // tree's own expandCurrent() beside them -; the groups' nodes carry none of it
    public void testATreesToolbarAViewDrawsIsAPartOfTheTree() {
        TreeFixture f = new TreeFixture(t -> {
            t.toolbarIn(t.a);
            t.tree.toolbar.showPrintGroupXls = false; t.tree.toolbar.showFilters = false; // not a tree's
            t.filter = new PanelDouble();
        });
        assertFalse(f.controllers.treeToolbars.get(f.tree) instanceof ToolbarDouble);
        f.apply(closedPage(f));
        JavaScriptObject tree = field(f.sa.controller, "t");
        for (String name : Arrays.asList("filterControlsVisible", "export")) {
            assertTrue(name, has(tree, name));
            assertFalse(name, has(field(f.sa.controller, "cat"), name)); // the tree's, not its groups'
            assertFalse(name, has(field(f.sa.controller, "item"), name));
        }
        assertFalse(has(tree, "expandTree")); // the whole tree is the top group's expandAll()
        assertTrue(isFalse(field(f.data, "t"), "filterControlsVisible"));
        assertFalse(has(field(f.data, "cat"), "filterControlsVisible"));
        callChange(field(tree, "filterControlsVisible"), true);
        assertTrue(f.filter.shown);
        f.projection.flush();
        assertTrue(flag(field(f.data, "t"), "filterControlsVisible"));

        f.controllers.groups.get(f.cat).updateCurrentKey(f.c2);
        callAll(tree, "expandCurrent"); // the tree's own, beside the toolbar's members
        callAll(tree, "export");
        assertEquals(Arrays.asList("expand current cat 2", "export tree 60"), f.verbs.asked);
        batch(f.sa.controller, () -> {
            callAll(tree, "expandCurrent"); // opens nodes: an edit, made in the batch
            callAll(tree, "export"); // asks the server, changing nothing: refused, not thrown
        });
        assertEquals(Arrays.asList("batch", "expand current cat 2", "sent"), f.verbs.asked.subList(2, f.verbs.asked.size()));
    }

    // the TREE's current row, on its node where React draws its rows - data.<tree>.current: the key of the current
    // node's row, written as `key` is, so `row.key === data.t.current` holds, as `parent` does -, null while the tree
    // has no current row loaded - before any is chosen, while the current node's group is on no row, while its row
    // has not come or has gone -, never a row above it in its stead; rewritten only when it changes - not when a cell
    // does, nor when what is asked of the nodes does
    public void testATreesNodeSaysItsCurrentRow() {
        GPropertyDraw[] columns = new GPropertyDraw[1];
        TreeFixture f = new TreeFixture(t -> columns[0] = t.column(64, "price", t.item));
        f.projection.flush();
        assertTrue(has(field(f.data, "t"), "current")); // there from the start: an empty tree has no current row
        assertTrue(isNullField(field(f.data, "t"), "current"));
        f.apply(closedPage(f));
        assertTrue(isNullField(field(f.data, "t"), "current")); // rows, none chosen
        GFormChanges chosen = new GFormChanges(); // the server reporting an item, the path above it with it
        chosen.objects.put(f.item, f.i15);
        f.apply(chosen);
        assertTrue(sameField(field(f.data, "t"), "current", f.row(f.item, f.i15), "key"));
        f.controllers.groups.get(f.cat).changeCurrentKey(f.c2); // the view's choice of a category: the category
        f.projection.flush();
        assertTrue(sameField(field(f.data, "t"), "current", f.row(f.cat, f.c2), "key"));
        chosen = new GFormChanges(); // ... and the server's of an item under the first: the path moved
        chosen.objects.put(f.item, f.i16);
        f.apply(chosen);
        assertTrue(sameField(field(f.data, "t"), "current", f.row(f.item, f.i16), "key"));

        JavaScriptObject node = field(f.data, "t"); // a cell changing, or a node asked to open, rewrites none of it
        GFormChanges cell = new GFormChanges();
        put(cell, columns[0], f.i16, 160);
        f.apply(cell);
        callNode(field(f.sa.controller, "cat"), "expand", f.row(f.cat, f.c2));
        assertSame(node, field(f.data, "t"));

        GFormChanges gone = new GFormChanges(); // the current row taken away - by a filter, say -: none, not the
        gone.gridObjects.put(f.item, rows(f.i15)); // category above it
        gone.parentObjects.put(f.item, rows(GGroupObjectValue.EMPTY));
        f.apply(gone);
        assertTrue(isNullField(field(f.data, "t"), "current"));
        GFormChanges back = new GFormChanges(); // ... and back
        back.gridObjects.put(f.item, rows(f.i15, f.i16));
        back.parentObjects.put(f.item, rows(GGroupObjectValue.EMPTY, GGroupObjectValue.EMPTY));
        f.apply(back);
        assertTrue(sameField(field(f.data, "t"), "current", f.row(f.item, f.i16), "key"));

        GFormChanges none = new GFormChanges(); // the current node's group on no row, reported on a path of nulls
        none.objects.put(f.item, new GGroupObjectValue(2, new int[]{60, 61}, new Serializable[]{null, null}));
        f.apply(none);
        assertTrue(isNullField(field(f.data, "t"), "current"));
    }

    // where a view draws a TREE's filter controls, they are a part of the tree there, on the tree's node, one for all
    // its groups, as a group's are on the group's: members on the tree's, which give the commands of the tree's filter
    // controller once it binds them, and what the apply button shows on its node
    public void testATreesFilterControlsAViewDrawsAreAPartOfTheTree() {
        TreeFixture f = new TreeFixture(t -> t.filterControlsIn(t.a));
        GFilterControlsController controls = f.controllers.filterControls.get(f.tree);
        assertNotNull(controls); // not the platform's
        f.apply(closedPage(f));
        JavaScriptObject tree = field(f.sa.controller, "t"), node = field(f.data, "t");
        for (String name : Arrays.asList("addFilter", "applyFilters", "resetFilters")) {
            assertTrue(name, has(tree, name));
            assertFalse(name, has(field(f.sa.controller, "cat"), name));
        }
        assertTrue(isFalse(node, "filtersManual"));
        assertTrue(isFalse(node, "filtersPending"));
        PanelDouble handler = new PanelDouble(); // the tree's filter input controller, binding them
        controls.bind(handler, handler);
        callAll(tree, "resetFilters");
        assertEquals(Arrays.asList("reset"), handler.calls);
    }

    // where a view draws what the group's FILTERS box holds, the controller of the box, which the platform wires up
    // where it would build its panel (createFiltersControllers), is the commands over the conditions the view draws
    // - an object of their own, not the part that shows them -, with no condition rows: each key states the group's
    // list anew from the one the form has, through the table, as a pivot's drill-down does (replaceFilterConditions) -
    // F3 and Alt+F3 on no cell, and typing, nothing; F3 on a cell its condition in the list, Alt+F3 its condition
    // alone, made at once; Shift+F3, Escape and the controls' reset an empty list
    public void testThePlatformsFilterCommandsWorkOnTheConditionsAViewDraws() {
        Fixture f = new Fixture(Fixture::filtersApart);
        FilterControlsDouble controls = new FilterControlsDouble(); // FILTERCONTROLS a view draws: no platform ones
        RecordingTableController table = new RecordingTableController();
        table.bindFilter(viewCommands(f, table, controls));
        assertNotSame(f.controllers.userFilters.get(f.group), table.getFilter()); // the commands, not the part
        assertSame(table.getFilter(), controls.handler); // ... which the view's controls give
        table.addFilter(null); table.replaceFilter(null); table.quickEditFilter(null, null, null); // on no cell, typing
        assertEquals(0, table.filtered.size()); // ... change nothing
        f.reportFilters(dtos(Arrays.asList(new GPropertyFilter(new GFilter(f.quantity), f.group, GGroupObjectValue.EMPTY, PValue.getPValue(2), GCompare.EQUALS))));
        table.selectedProperty = f.price; table.selectedValue = PValue.getPValue(5); table.selectedRow = 0;
        table.addFilter(null); // F3 on a cell: its condition, in the list the form has
        table.replaceFilter(null); // Alt+F3 on a cell: alone
        table.resetFilters(); // Shift+F3, Escape, the controls' reset
        ArrayList<GPropertyFilter> drillDown = new ArrayList<>();
        drillDown.add(new GPropertyFilter(new GFilter(f.quantity), f.group, GGroupObjectValue.EMPTY, PValue.getPValue(3), GCompare.EQUALS));
        table.replaceFilterConditions(drillDown); // a pivot cell's drill-down
        // the group's lists, stated through the table, not through a member of the part
        assertEquals(Arrays.asList(" quantity= price=", " price=", "", " quantity="), table.filtered);
        assertEquals(0, f.filtersCalls.size());
    }

    // a key typed in a column of the platform's table goes to whichever filter input controller the table has, as its
    // keys do: the panel's editor takes it, and where a view draws the conditions nothing starts - no list is stated
    public void testTypingInAColumnStartsNoConditionWhereAViewDrawsThem() {
        Fixture f = new Fixture(Fixture::filtersApart);
        RecordingTableController table = onCell(f.price, 0, PValue.getPValue(5));
        table.bindFilter(viewCommands(f, table, new FilterControlsDouble()));
        table.quickEditFilter(null, f.price, GGroupObjectValue.EMPTY);
        assertEquals(0, table.filtered.size());
        assertEquals(0, f.filtersCalls.size());
    }

    // over conditions a view draws nothing typed waits to be applied: there are no editors of the platform's, so the
    // commands bind the controls with none - the add they give all the same, the FILTERS box being there -, and the
    // controls of either kind, which show the apply button by the editors' mode and give their apply, have none to
    // give: a view's controls bound so show the mode as not manual, and their apply member refuses, saying why
    public void testAViewsConditionsHaveNoEditorsSoNothingWaitsToBeApplied() {
        Fixture f = new Fixture(fixture -> { fixture.filtersApart(); fixture.filterControlsIn(fixture.c); });
        GFilterControlsController controls = f.controllers.filterControls.get(f.group.grid);
        RecordingTableController table = onCell(f.price, 0, PValue.getPValue(5));
        table.bindFilter(viewCommands(f, table, controls));
        assertTrue(table.getFilter().hasFiltersContainer());
        f.projection.flush();
        JavaScriptObject node = field(f.snapshots[2], "items");
        assertTrue(isFalse(node, "filtersManual"));
        assertTrue(isFalse(node, "filtersPending"));
        assertRefusal("controller.items.applyFilters(): has nothing to apply: a view draws the conditions",
                refusedWith(field(f.sc.controller, "items"), "applyFilters", null));
        assertEquals(0, table.filtered.size());
        FilterControlsDouble platform = new FilterControlsDouble(); // the platform's buttons, as the commands bind them
        GFilterInputController commands = viewCommands(f, table, platform);
        assertSame(commands, platform.handler);
        assertNull(platform.editors);
    }
    // the commands over the conditions a view draws, over what draws the rows, with the controls given, as the form
    // wires them - no toolbar, no panel
    private static GFilterInputController viewCommands(Fixture f, GTableController rows, GFilterControlsController controls) {
        return f.projection.createFiltersControllers(f.group.grid, rows, new GFilterControlsToggle(null, controls, null), () -> null).first;
    }

    // ... a command that makes a condition makes it at once, as F3 and Enter make one on the platform's panel: the one
    // the table's cell starts (GReactFormData.getCellCondition), put where a condition on its property already is
    // (withCondition)
    public void testACellStartsTheConditionOfItsColumnComparedTheDefaultWayWithItsValue() {
        Fixture f = new Fixture();
        GPropertyFilter condition = GReactFormData.getCellCondition(onCell(f.price, 0, PValue.getPValue(5)));
        assertSame(f.price, condition.column.property);
        assertSame(f.group, condition.groupObject);
        assertEquals(GGroupObjectValue.EMPTY, condition.column.columnKey);
        assertEquals(GCompare.EQUALS, condition.compare);
        assertEquals(PValue.getPValue(5), condition.value.value);
    }
    // a table on a cell - of a property, in a row, -1 for none - and that cell's value
    private static RecordingTableController onCell(GPropertyDraw property, int row, PValue value) {
        RecordingTableController table = new RecordingTableController();
        table.selectedProperty = property; table.selectedRow = row; table.selectedValue = value;
        return table;
    }

    // a table on no row names a column all the same - an empty grid its first, a pivot, a map, a calendar or a custom
    // view, which have no cell, their first property -, and has no value there: a condition made at once would filter
    // by NULL and empty the rows, so none is started. An empty cell of a row is a NULL value all the same
    public void testATableOnNoRowStartsNoCondition() {
        Fixture f = new Fixture();
        assertNull(GReactFormData.getCellCondition(onCell(f.price, -1, null)));

        GPropertyFilter condition = GReactFormData.getCellCondition(onCell(f.price, 0, null));
        assertSame(f.price, condition.column.property);
        assertNull(condition.value.value);
    }

    // what a view could not name in data.<g>.filters - it would filter the rows by a condition nobody is shown -, and
    // what shows another value than it filters by
    public void testACellAViewCannotNameOrWithAnotherValueStartsNoCondition() {
        Fixture f = new Fixture();
        GPropertyDraw unnamed = f.property(75, "hidden", f.group, true, f.a);
        unnamed.integrationSID = null;
        assertNull(GReactFormData.getCellCondition(onCell(unnamed, 0, PValue.getPValue(5))));

        GPropertyDraw action = f.property(73, "approve", f.group, true, f.a);
        action.valueType = GActionType.instance;
        assertNull(GReactFormData.getCellCondition(onCell(action, 0, null)));

        GPropertyDraw inColumns = f.property(74, "perColumn", f.group, true, f.a);
        inColumns.columnGroupObjects = new ArrayList<>(Arrays.asList(new GGroupObject()));
        assertNull(GReactFormData.getCellCondition(onCell(inColumns, 0, PValue.getPValue(5))));

        GPropertyDraw different = f.property(76, "shown", f.group, true, f.a);
        different.differentValue = true;
        assertNull(GReactFormData.getCellCondition(onCell(different, 0, PValue.getPValue(5))));
    }

    // F3 on a property already filtered by takes the place of the first condition on it, so a range's other bound stays
    public void testAConditionTakesThePlaceOfTheFirstOnItsProperty() {
        Fixture f = new Fixture();
        GPropertyFilter low = condition(f.price, 1), some = condition(f.quantity, 2), high = condition(f.price, 3);
        GPropertyFilter changed = condition(f.price, 5);

        ArrayList<GPropertyFilter> result = GReactFormData.withCondition(new ArrayList<>(Arrays.asList(low, some, high)), changed);
        assertEquals(Arrays.asList(changed, some, high), result);
    }

    public void testAConditionOnAPropertyNotFilteredByIsAppended() {
        Fixture f = new Fixture();
        GPropertyFilter some = condition(f.quantity, 2), added = condition(f.price, 5);

        ArrayList<GPropertyFilter> conditions = new ArrayList<>(Arrays.asList(some));
        assertEquals(Arrays.asList(some, added), GReactFormData.withCondition(conditions, added));
        assertEquals("the list it was handed stays as it was", Arrays.asList(some), conditions);
    }
    private static GPropertyFilter condition(GPropertyDraw property, int value) {
        return new GPropertyFilter(new GFilter(property), property.groupObject, GGroupObjectValue.EMPTY, PValue.getPValue(value), null);
    }

    // a FILTERS box's list stated - a group's, or a tree's - is split into its groups' lists, each condition under its
    // property's group; a condition on a property of a group outside the box, or of no group - a quick filter on such
    // a property, sacrificed - is no condition of the box's: left out, and said on the console, which names the box's
    // groups, the property and its group
    public void testAConditionOnAPropertyOfNoGroupOfTheBoxIsLeftOutAndSaid() {
        Fixture f = new Fixture();
        GGroupObject other = new GGroupObject(); other.ID = 90; other.sID = "other";
        GPropertyDraw foreign = f.property(78, "foreign", other, true, f.a);
        GPropertyDraw nobodys = f.property(79, "nobodys", null, false, f.a); // the empty group's
        GPropertyFilter some = condition(f.price, 1), amount = condition(f.quantity, 2), theirs = condition(foreign, 3),
                nobody = condition(nobodys, 4);
        JavaScriptObject errors = spyConsoleErrors();
        try {
            NativeSIDMap<GGroupObject, ArrayList<GPropertyFilter>> lists = GPropertyFilter.getGroupConditions(
                    Collections.singletonList(f.group), new ArrayList<>(Arrays.asList(some, theirs, amount, nobody)));
            assertEquals(1, lists.size());
            assertEquals(Arrays.asList(some, amount), lists.get(f.group));
            assertEquals(2, countMatching(errors, "the filters of 'items': the condition on"));
            assertEquals(1, countMatching(errors, "'foreign' is on a property of group 'other', which is none of theirs"));
            assertEquals(1, countMatching(errors, "'nobodys' is on a property of no group, which is none of theirs"));
            lists = GPropertyFilter.getGroupConditions(Arrays.asList(other, f.group), new ArrayList<>(Arrays.asList(theirs, some)));
            assertEquals(Arrays.asList(theirs), lists.get(other)); // a box of two groups: each its own, in order
            assertEquals(Arrays.asList(some), lists.get(f.group));
            assertEquals(2, countMatching(errors, "the filters of")); // nothing left out, nothing said
        } finally {
            restoreConsole(errors);
        }
    }

    // the row a per-row <Platform> is given has to be a row of the property's group, read as a group's change(row)
    // reads it (GGroupObject.getRowKey): a tree's row of a group below passes, narrowed to the path down to the group,
    // while a row of a group above names no row of the one below
    public void testATreeRowNarrowsUpToAGroupButNotDown() {
        TreeFixture f = new TreeFixture();
        assertEquals(f.c1, f.cat.getRowKey(f.i15)); // an item under category 1: category 1's row
        assertEquals(f.i15, f.item.getRowKey(f.i15)); // the item's own
        assertNull(f.item.getRowKey(f.c1)); // a category's row is no item's
    }

    private static GPropertyDraw byName(GForm form, String sID) {
        for (GPropertyDraw property : form.propertyDraws)
            if (sID.equals(property.sID))
                return property;
        throw new IllegalArgumentException(sID);
    }
    private static native boolean isNullField(JavaScriptObject object, String name) /*-{ return object[name] === null; }-*/;
    private static native boolean isFalseField(JavaScriptObject object, String name) /*-{ return object[name] === false; }-*/;
    // the errors the console is told while the spy is on - the page's console (GwtClientUtils.logCustomViewError) and
    // the module's own (GwtClientUtils.consoleError), one object or two -, their own error put back by restoreConsole
    private static native JavaScriptObject spyConsoleErrors() /*-{
        var log = [], pageError = $wnd.console.error, moduleError = console.error;
        var spy = function () { log.push(Array.prototype.join.call(arguments, ' ')); };
        $wnd.console.error = spy;
        console.error = spy;
        log.restore = function () { $wnd.console.error = pageError; console.error = moduleError; };
        return log;
    }-*/;
    private static native void restoreConsole(JavaScriptObject log) /*-{ log.restore(); }-*/;
    private static native int countMatching(JavaScriptObject log, String part) /*-{
        return log.filter(function (message) { return message.indexOf(part) >= 0; }).length;
    }-*/;

    // a color and an interval go out as strings of their own, and come back as what they are: a condition `filters`
    // lists, handed back as it is, is the condition it was, and a member given a value in that shape changes the cell
    // to it - one in another shape is refused, not sent as a string the server takes for neither. A file's condition
    // says a link to the file, which is not the file: it can be taken off, not stated
    public void testAValueComesBackAsItWentOut() {
        Fixture f = new Fixture(fixture -> {
            fixture.filtersBesideRows();
            GPropertyDraw tint = fixture.property(74, "tint", fixture.group, true, fixture.a);
            tint.valueType = tint.externalChangeType = GColorType.instance;
            GPropertyDraw period = fixture.property(75, "period", fixture.group, true, fixture.a);
            period.valueType = period.externalChangeType = GDateIntervalType.instance;
            fixture.property(76, "scan", fixture.group, true, fixture.a).valueType = new GCSVType();
        });
        GPropertyDraw tint = f.form.getPropertyDraw(f.group, "tint"), period = f.form.getPropertyDraw(f.group, "period");
        PValue red = PValue.getPValue(new ColorDTO("ff0000")), year = PValue.getPValue(1000L, 2000L);
        ArrayList<GPropertyFilter> conditions = new ArrayList<>();
        conditions.add(new GPropertyFilter(new GFilter(tint), f.group, GGroupObjectValue.EMPTY, red, GCompare.EQUALS));
        conditions.add(new GPropertyFilter(new GFilter(period), f.group, GGroupObjectValue.EMPTY, year, GCompare.EQUALS));
        f.reportFilters(dtos(conditions));
        f.projection.flush();
        JavaScriptObject filters = field(f.node(), "filters");
        assertEquals("#ff0000", text(at(filters, 0), "value"));
        assertEquals("1000.2000", text(at(filters, 1), "value"));
        JavaScriptObject member = field(field(f.sa.controller, "items"), "filters");
        callChange(member, filters); // handed back as it is
        assertEquals(red, f.stated.get(0).value.value);
        assertEquals(year, f.stated.get(1).value.value);
        String refused = refusedChange(member, parse("{\"property\": \"tint\", \"value\": \"red\"}"));
        assertTrue(refused, refused != null && refused.contains("is not a color"));
        refused = refusedChange(member, parse("{\"property\": \"period\", \"value\": \"1000-2000\"}"));
        assertTrue(refused, refused != null && refused.contains("is not an interval"));

        // a cell: the value its member is given in the shape it is projected in
        GFormChanges shown = new GFormChanges();
        put(shown, tint, f.one, red);
        put(shown, period, f.one, year);
        f.apply(shown);
        JavaScriptObject items = field(f.sa.controller, "items");
        assertNull(refusedChange(field(items, "tint"), "#00FF00", f.row("1")));
        assertEquals(PValue.getPValue(new ColorDTO("00FF00")), f.changedTo);
        assertNull(refusedChange(field(items, "period"), "-5.3000", f.row("1"))); // a bound before 1970
        assertEquals(PValue.getPValue(-5L, 3000L), f.changedTo);
        refused = refusedChange(field(items, "tint"), "#0f0", f.row("1"));
        assertTrue(refused, refused != null && refused.contains("is not a color"));

        // a file's condition: what `filters` would say of its value is a link to the file (a link this test has no
        // page to build), which is not the file - so a condition on it is not stated, alone or in a list
        int calls = f.filtersCalls.size();
        refused = refusedChange(member, parse("{\"property\": \"scan\", \"value\": \"files/scan.csv\"}"));
        assertTrue(refused, refused != null && refused.contains("'scan' is a file"));
        refused = refusedChange(member, parse("[{\"property\": \"tint\", \"value\": \"#ff0000\"},"
                + " {\"property\": \"scan\", \"value\": \"files/scan.csv\"}]"));
        assertTrue(refused, refused != null && refused.contains("condition 1: ") && refused.contains("'scan' is a file"));
        assertEquals(calls, f.filtersCalls.size());
        callChange(member, parse("{\"property\": \"scan\"}")); // ... and taken off, as any other property's is
        assertEquals("changeFilters items tint= period=", f.filtersCalls.get(calls));
    }

    // where the platform draws a group's filters over the rows React draws, its filter input controller asks those rows
    // what it would ask a table (GTableController): the columns the view carries that the form shows and a condition
    // can be on - not a panel property beside them, not an action -, by their captions; the first of them for a new
    // condition, a view having no current column; a column's value in the current row; and the conditions it states are
    // stated as a view's are
    public void testTheRowsReactDrawsAnswerThePlatformsFilterInputController() {
        GPropertyDraw[] action = new GPropertyDraw[1];
        Fixture f = new Fixture(fixture -> {
            fixture.note = fixture.property(25, "note", fixture.group, false, fixture.a);
            action[0] = fixture.property(27, "act", fixture.group, true, fixture.a);
            action[0].valueType = GActionType.instance;
        });
        f.price.caption = "Price"; f.quantity.caption = "Quantity";
        GFormChanges delta = new GFormChanges();
        put(delta, f.price, f.one, 10); put(delta, f.price, f.two, 20);
        put(delta, f.quantity, f.one, 1); put(delta, f.quantity, f.two, 2);
        put(delta, action[0], f.one, null); put(delta, f.note, GGroupObjectValue.EMPTY, "n");
        f.apply(delta);
        GTableController rows = f.controllers.gridRows.get(f.group);

        assertSame(f.group, rows.getSelectedGroupObject());
        List<Pair<Column, String>> columns = rows.getFilterColumns();
        assertEquals(2, columns.size());
        assertEquals(new Column(f.price, GGroupObjectValue.EMPTY), columns.get(0).first);
        assertEquals("Price", columns.get(0).second);
        assertEquals(new Column(f.quantity, GGroupObjectValue.EMPTY), columns.get(1).first);
        // a caption the server sends: the column is offered by it, as a grid's
        GFormChanges caption = new GFormChanges();
        put(caption, f.price.captionReader, GGroupObjectValue.EMPTY, "Unit price");
        f.apply(caption);
        assertEquals("Unit price", rows.getFilterColumns().get(0).second);
        assertSame(f.price, rows.getSelectedFilterProperty());
        assertEquals(GGroupObjectValue.EMPTY, rows.getSelectedColumnKey());
        assertEquals(PValue.getPValue(10), rows.getSelectedValue(f.price, GGroupObjectValue.EMPTY)); // the current row
        f.controller(f.group).updateCurrentKey(f.two);
        assertEquals(PValue.getPValue(20), rows.getSelectedValue(f.price, GGroupObjectValue.EMPTY));

        f.apply(drop(f.price)); // a column the form does not show now is not offered
        assertEquals(1, rows.getFilterColumns().size());
        assertSame(f.quantity, rows.getSelectedFilterProperty());

        ArrayList<GPropertyFilter> conditions = new ArrayList<>();
        conditions.add(new GPropertyFilter(new GFilter(f.quantity), f.group, GGroupObjectValue.EMPTY, PValue.getPValue(2), GCompare.EQUALS));
        rows.changeFilter(conditions);
        assertEquals("changeFilters items quantity=", f.filtersCalls.get(0));
        assertFalse(rows.focusFirstWidget(FocusUtils.Reason.APPLYFILTER)); // the focus is the view's
    }
    // ... and the filter input controller over them, given to them once it is made, is told by them that they have
    // changed, once per delta, and let go of when the form closes - a group's grid's rows and a tree's alike -, as a
    // table of the platform's does with the one over it (GTableController.update, destroy)
    public void testTheFilterInputControllerOverTheRowsReactDrawsIsToldOfTheirChanges() {
        Fixture f = Fixture.withOuter(); // the items' rows React's
        PanelDouble filter = new PanelDouble();
        GGridController rows = f.controllers.gridRows.get(f.group);
        rows.bindFilter(filter);
        rows.update(0, new GFormChanges()); // a delta
        assertEquals(1, filter.updates);
        assertEquals(0, filter.destroyed);
        rows.destroy(); // the form closing
        assertEquals(1, filter.destroyed);

        TreeFixture tree = new TreeFixture();
        PanelDouble treeFilter = new PanelDouble();
        GTreeController treeRows = tree.controllers.treeRows.get(tree.tree);
        treeRows.bindFilter(treeFilter);
        treeRows.update(0, new GFormChanges());
        treeRows.update(1, new GFormChanges());
        assertEquals(2, treeFilter.updates);
    }

    // where a view draws a group's TOOLBARSYSTEM, the toolbar is a part of the group there, and its controller: what
    // its buttons show is on the group's node, all its buttons are members on the group's - whatever the design's
    // flags say, which are the platform's - only there
    public void testAToolbarAViewDrawsIsAPartOfTheGroup() {
        Fixture f = new Fixture(fixture -> fixture.toolbarIn(fixture.c));
        assertFalse(f.controllers.toolbars.get(f.group) instanceof ToolbarDouble);
        JavaScriptObject node = field(f.snapshots[2], "items");
        assertTrue(isFalse(node, "manual"));
        assertTrue(isFalse(node, "updatePending"));
        assertTrue(isFalse(node, "filterControlsVisible"));
        assertTrue(isNullField(node, "listView"));
        String all = "export,filterControlsVisible,getCount,getSum,listView,manual,update";
        assertEquals(all, keysOf(field(f.sc.controller, "items")));
        assertNull(member(f.sa, "items", "getCount"));

        Fixture fewer = new Fixture(fixture -> { // ... whatever the design's flags say
            fixture.toolbarIn(fixture.c);
            fixture.group.grid.toolbar.showCalculateSum = false; fixture.group.grid.toolbar.showManualUpdate = false;
        });
        assertEquals(all, keysOf(field(fewer.sc.controller, "items")));
        assertTrue(isFalse(field(fewer.snapshots[2], "items"), "manual"));
    }

    // ... and where the platform draws it, or the design does not show it, the toolbar is the platform's
    public void testAToolbarThePlatformDrawsIsThePlatforms() {
        Fixture drawn = new Fixture(fixture -> fixture.toolbarIn(fixture.form.mainContainer));
        assertTrue(drawn.controllers.toolbars.get(drawn.group) instanceof ToolbarDouble);
        Fixture hidden = new Fixture(fixture -> {
            fixture.toolbarIn(fixture.c);
            fixture.group.grid.toolbar.visible = false;
        });
        assertTrue(hidden.controllers.toolbars.get(hidden.group) instanceof ToolbarDouble);
        assertNull(member(hidden.sc, "items", "getCount"));
    }

    // its manual update is the group's update mode, as the platform's toggle sets it - on, nothing held back yet -; the
    // server holding changes back says so, in its delta; the update brings them
    public void testTheToolbarsManualUpdateIsTheGroupsUpdateMode() {
        Fixture f = new Fixture(fixture -> fixture.toolbarIn(fixture.c));
        JavaScriptObject member = field(f.sc.controller, "items");
        GGridToolbarController toolbar = f.controllers.toolbars.get(f.group);
        callChange(field(member, "manual"), true);
        assertEquals("updateMode items MANUAL", f.toolbarCalls.get(0));
        assertTrue(f.manualUpdates.get(f.group)); // the form keeps it
        assertTrue(flag(field(f.snapshots[2], "items"), "manual")); // a member's call: published before it returns

        f.projection.change(() -> toolbar.updateState(true)); // the server's delta: published once it is applied
        assertTrue(flag(field(f.snapshots[2], "items"), "updatePending"));
        int publications = f.publications[2];
        f.projection.change(() -> toolbar.updateState(null)); // ... one which says nothing new: the state stays
        assertTrue(flag(field(f.snapshots[2], "items"), "updatePending"));
        assertEquals(publications, f.publications[2]); // nothing written, nothing published
        callAll(member, "update");
        assertEquals("updateMode items FORCE", f.toolbarCalls.get(1));

        callChange(field(member, "manual"), true); // on already
        assertEquals(2, f.toolbarCalls.size());
        assertRefusal("controller.items.manual.change(): takes true or false", refusedChange(field(member, "manual"), "yes"));
    }

    // ... and it shows the group's update mode as the form keeps it (Verbs.isManualUpdate) - as the server starts the
    // group, manual with enableManualUpdate -, which is what a pivot restores (GPlatformGridController.changeGroups):
    // no toolbar keeps a mode of its own
    public void testAToolbarShowsTheUpdateModeTheFormKeeps() {
        Fixture f = new Fixture(fixture -> {
            fixture.toolbarIn(fixture.c);
            fixture.group.enableManualUpdate = true;
        });
        assertTrue(flag(field(f.snapshots[2], "items"), "manual"));
        callChange(field(field(f.sc.controller, "items"), "manual"), false);
        assertEquals("updateMode items AUTO", f.toolbarCalls.get(0));
        f.projection.flush();
        assertTrue(isFalse(field(f.snapshots[2], "items"), "manual"));

        Fixture hidden = new Fixture(fixture -> { // ... whatever the platform's toggle shows: a flag of its own alone
            fixture.toolbarIn(fixture.c);
            fixture.group.enableManualUpdate = true;
            fixture.group.grid.toolbar.showManualUpdate = false;
        });
        assertTrue(flag(field(hidden.snapshots[2], "items"), "manual"));
        callAll(field(hidden.sc.controller, "items"), "update"); // the changes held back brought: the mode stays
        assertEquals("updateMode items FORCE", hidden.toolbarCalls.get(0));
        hidden.projection.flush();
        assertTrue(flag(field(hidden.snapshots[2], "items"), "manual"));
    }

    // the count and a column's sum are asked of the server when called, each the Promise of its answer: a column named
    // as a state names one - a number, not grouped in columns
    public void testTheToolbarsCountAndSumArePromisesOfTheAnswer() {
        Fixture f = new Fixture(fixture -> {
            fixture.toolbarIn(fixture.c);
            fixture.property(27, "act", fixture.group, true, fixture.a).valueType = GActionType.instance;
        });
        JavaScriptObject member = field(f.sc.controller, "items");
        JavaScriptObject sum = callNode(member, "getSum", "price");
        assertEquals("sum items price", f.toolbarCalls.get(0));
        f.answered.accept(12.5);
        assertRefusal("controller.items.getSum(): unknown property 'nope' of object group 'items'", refusedWith(member, "getSum", "nope"));
        assertRefusal("controller.items.getSum(): takes the name of the property to sum, as a string", refusedWith(member, "getSum", 42.0));
        assertRefusal("controller.items.getSum(): 'act' is not a number", refusedWith(member, "getSum", "act"));
        callAll(member, "export");
        assertEquals("export 10", f.toolbarCalls.get(1));

        JavaScriptObject count = callAll(member, "getCount");
        assertEquals("count items", f.toolbarCalls.get(2));
        delayTestFinish(5000);
        then(sum, total -> then(count, value -> {
            assertEquals(12.5, (Double) total, 0);
            assertEquals(7.0, (Double) value, 0);
            finishTest();
        }, error -> fail("rejected: " + error)), error -> fail("rejected: " + error));
        f.answered.accept(7L);
    }

    // ... and those asking the server - the count, the sum, the export, the update mode - are refused inside a batch,
    // as a server call on the controller is, a batch carrying changes only - not thrown: the count's Promise rejected,
    // the rest made of nothing, before the toolbar's state changes
    public void testTheToolbarsRequestsAreRefusedInABatch() {
        Fixture f = new Fixture(fixture -> {
            fixture.toolbarIn(fixture.c);
            fixture.group.grid.toolbar.showViews = true;
        });
        JavaScriptObject member = field(f.sc.controller, "items");
        JavaScriptObject[] count = new JavaScriptObject[1];
        batch(f.sc.controller, () -> { // nothing thrown
            count[0] = callAll(member, "getCount");
            callNode(member, "getSum", "price");
            callAll(member, "export");
            callChange(field(member, "manual"), true);
            callAll(member, "update");
            callChange(field(member, "listView"), "grid");
        });
        assertTrue(f.toolbarCalls.isEmpty()); // nothing asked of the form
        assertTrue(isFalse(field(f.snapshots[2], "items"), "manual")); // nor the state changed
        delayTestFinish(5000);
        then(count[0], value -> fail("answered: " + value), error -> {
            assertTrue(String.valueOf(error), String.valueOf(error).contains("controller.items.getCount(): a batch is running"));
            finishTest();
        });
    }

    // the filter button, where the toolbar is a view's: a member, and what the node shows - the
    // platform's controls shown or hidden, as the platform's button shows and hides them
    public void testTheToolbarsFilterButtonIsAMember() {
        Fixture f = new Fixture(fixture -> {
            fixture.toolbarIn(fixture.c);
            fixture.group.grid.toolbar.showFilters = true;
            fixture.filter = new PanelDouble();
        });
        JavaScriptObject member = field(f.sc.controller, "items");
        assertTrue(has(member, "filterControlsVisible"));
        assertTrue(isFalse(field(f.snapshots[2], "items"), "filterControlsVisible"));
        callChange(field(member, "filterControlsVisible"), true);
        assertTrue(f.filter.shown);
        assertTrue(flag(field(f.snapshots[2], "items"), "filterControlsVisible")); // published before the call returns
        callChange(field(member, "filterControlsVisible"), true); // shown already
        assertTrue(f.filter.shown);
        assertRefusal("controller.items.filterControlsVisible.change(): takes true or false", refusedChange(field(member, "filterControlsVisible"), 1.0));
    }

    // a member's call that throws - refused - is over all the same: the next one is published before it returns again
    public void testACallThatThrowsIsOverAllTheSame() {
        Fixture f = new Fixture(fixture -> {
            fixture.toolbarIn(fixture.c);
            fixture.group.grid.toolbar.showFilters = true;
            fixture.filter = new PanelDouble();
        });
        JavaScriptObject member = field(f.sc.controller, "items");
        assertRefusal("controller.items.filterControlsVisible.change(): takes true or false", refusedChange(field(member, "filterControlsVisible"), 1.0));
        callChange(field(member, "filterControlsVisible"), true);
        assertTrue(flag(field(f.snapshots[2], "items"), "filterControlsVisible"));
    }

    // ... and its buttons there with nothing of the platform's to work refuse, saying why: the filter button has no
    // filter controls of the platform's to show, and the switch no view to switch to
    public void testTheToolbarsButtonsRefuseWhatThePlatformHasNot() {
        Fixture f = new Fixture(fixture -> fixture.toolbarIn(fixture.c));
        JavaScriptObject node = field(f.snapshots[2], "items"), member = field(f.sc.controller, "items");
        assertTrue(isFalse(node, "filterControlsVisible"));
        assertRefusal("controller.items.filterControlsVisible.change(): has no filter controls of the platform's to show",
                refusedChange(field(member, "filterControlsVisible"), true));
        assertTrue(isNullField(node, "listView"));
        assertEquals(0, length(field(node, "listViews")));
        assertRefusal("controller.items.listView.change(): has no view to switch to: no table of the platform's draws the rows",
                refusedChange(field(member, "listView"), "grid"));
    }

    // the platform's table tells its toolbar that it is shown or hidden - a grid with no columns hides itself -, and
    // its filter input controller tells the filter controls, whichever kind each is: the platform's hide with it, and a
    // view's parts, of a group or of a tree, leave what the view draws as it is - nothing written, nothing published
    public void testATableHidingItselfHidesNothingAViewDraws() {
        Fixture f = new Fixture(fixture -> {
            fixture.toolbarIn(fixture.c);
            fixture.filterControlsIn(fixture.c);
        });
        JavaScriptObject node = field(f.snapshots[2], "items");
        int publications = f.publications[2];
        f.controllers.toolbars.get(f.group).setVisible(false);
        f.controllers.filterControls.get(f.group.grid).setTableVisible(false);
        f.controllers.toolbars.get(f.group).setVisible(true);
        f.controllers.filterControls.get(f.group.grid).setTableVisible(true);
        f.projection.flush();
        assertSame(node, field(f.snapshots[2], "items"));
        assertEquals(publications, f.publications[2]);

        TreeFixture tree = new TreeFixture(t -> { t.toolbarIn(t.a); t.filterControlsIn(t.a); });
        tree.apply(closedPage(tree));
        JavaScriptObject treeNode = field(tree.data, "t");
        publications = tree.publications;
        tree.controllers.treeToolbars.get(tree.tree).setVisible(false);
        tree.controllers.filterControls.get(tree.tree).setTableVisible(false);
        tree.projection.flush();
        assertSame(treeNode, field(tree.data, "t"));
        assertEquals(publications, tree.publications);
    }

    // where a view draws a group's FILTERCONTROLS, the controls are a part of the group there: the add, the apply and
    // the reset are members on the group's, and what the apply button shows is on its node - the same whoever draws the
    // conditions -; with no filter input controller to give their commands, the members refuse, saying why
    public void testFilterControlsAViewDrawsAreAPartOfTheGroup() {
        Fixture f = new Fixture(fixture -> fixture.filterControlsIn(fixture.c));
        JavaScriptObject node = field(f.snapshots[2], "items"), member = field(f.sc.controller, "items");
        assertEquals("addFilter,applyFilters,resetFilters", keysOf(member));
        assertTrue(isFalse(node, "filtersManual"));
        assertTrue(isFalse(node, "filtersPending"));
        assertFalse(has(field(f.sa.controller, "items"), "addFilter")); // only where they are drawn
        for (String verb : Arrays.asList("addFilter", "applyFilters", "resetFilters"))
            assertRefusal("controller.items." + verb + "(): has nothing to give its commands to",
                    refusedWith(member, verb, null));
    }

    // ... and their members give the filter input controller's commands, as the platform's buttons do: the add where
    // the design has the FILTERS box, the apply only while it is lit - the manual apply mode asking for it, and
    // conditions typed in waiting -, the reset; what the node shows follows the controller, published once the event is
    // over
    public void testFilterControlsGiveTheFilterInputControllersCommands() {
        Fixture f = new Fixture(fixture -> fixture.filterControlsIn(fixture.c));
        PanelDouble handler = new PanelDouble();
        GFilterControlsController controls = f.controllers.filterControls.get(f.group.grid);
        assertFalse(controls.isShownByToolbar()); // the view shows them
        controls.bind(handler, handler); // its commands, and its editors, as the panel is both
        JavaScriptObject member = field(f.sc.controller, "items");
        callAll(member, "addFilter");
        callAll(member, "resetFilters");
        callAll(member, "applyFilters"); // nothing waits
        assertEquals(Arrays.asList("add", "reset"), handler.calls);
        handler.filtersContainer = false; // no FILTERS box in the design: no add, as the platform has no such button
        assertRefusal("controller.items.addFilter(): has no FILTERS box to add a condition to", refusedWith(member, "addFilter", null));
        assertEquals(2, handler.calls.size());

        handler.manual = true;
        controls.setVisible(true); // the toolbar shows them, and the manual apply mode asks for the apply button
        f.projection.flush();
        assertTrue(flag(field(f.snapshots[2], "items"), "filtersManual"));
        assertTrue(isFalse(field(f.snapshots[2], "items"), "filtersPending"));
        controls.setApplyEnabled(true); // a condition typed in waits
        f.projection.flush();
        assertTrue(flag(field(f.snapshots[2], "items"), "filtersPending"));
        callAll(member, "applyFilters");
        assertEquals("apply", handler.calls.get(2));

        handler.manual = false; // the setting off, or the controls hidden: the platform shows no apply button
        controls.setApplyEnabled(true);
        f.projection.flush();
        assertTrue(isFalse(field(f.snapshots[2], "items"), "filtersManual"));
        assertTrue(isFalse(field(f.snapshots[2], "items"), "filtersPending"));
        callAll(member, "applyFilters");
        assertEquals(3, handler.calls.size());
    }
    // showFilters FALSE turns off the platform's filter UI alone, a group's and a tree's alike: the form gives no panel
    // and no controls of the platform's (GFormController.initializeTable) -
    // where the platform draws the conditions, there is no filter input controller at all; where a view draws them,
    // the commands over them are there all the same, the platform's keys working, and the toolbar gets a button only
    // with the platform's controls to show
    public void testShowFiltersTurnsOffThePlatformsFilterUIOnly() {
        Fixture platform = new Fixture(); // the platform draws the conditions: with no panel, nothing
        Pair<GFilterInputController, GUserFiltersController> none = platform.projection.createFiltersControllers(platform.group.grid,
                new RecordingTableController(), new GFilterControlsToggle(() -> { }, GFilterControlsController.NONE, new GridToolbarDouble()), () -> null);
        assertNull(none.first);
        assertSame(GUserFiltersController.NONE, none.second); // ... nothing shows the filters either
        for (boolean showFilters : new boolean[]{true, false}) {
            Fixture f = new Fixture(Fixture::filtersApart); // a view draws the conditions, the platform the controls
            FilterControlsDouble platformControls = new FilterControlsDouble();
            platformControls.shownByToolbar = true;
            // the platform's controls, as the form makes them where the platform draws FILTERCONTROLS
            GFilterControlsController controls = showFilters ? platformControls : GFilterControlsController.NONE;
            GridToolbarDouble toolbar = new GridToolbarDouble();
            RecordingTableController table = onCell(f.price, 0, PValue.getPValue(5));
            GFilterInputController input = f.projection.createFiltersControllers(f.group.grid, table,
                    new GFilterControlsToggle(() -> { }, controls, toolbar), () -> {
                        throw new AssertionError("no panel where a view draws the conditions");
                    }).first;
            table.bindFilter(input);
            table.addFilter(null); // F3 on a cell
            assertEquals(Arrays.asList(" price="), table.filtered);
            assertEquals(showFilters ? 1 : 0, toolbar.filterToggles);
            assertSame(showFilters ? input : null, platformControls.handler);

            TreeFixture tree = new TreeFixture(t -> t.filtersIn(t.a)); // ... and a tree's
            FilterControlsDouble treePlatformControls = new FilterControlsDouble();
            treePlatformControls.shownByToolbar = true;
            TreeToolbarDouble treeToolbar = new TreeToolbarDouble();
            GFilterInputController treeInput = tree.projection.createFiltersControllers(tree.tree,
                    new RecordingTableController(), new GFilterControlsToggle(() -> { },
                            showFilters ? treePlatformControls : GFilterControlsController.NONE, treeToolbar), () -> {
                        throw new AssertionError("no panel where a view draws the conditions");
                    }).first;
            assertNotNull(treeInput);
            assertEquals(showFilters ? 1 : 0, treeToolbar.filterToggles);
            assertSame(showFilters ? treeInput : null, treePlatformControls.handler);
        }
    }
    // the toolbar's filter button shows and hides what of the user filters THE PLATFORM draws, by one rule for every
    // kind of the controller of FILTERS (GFilterControlsToggle): a button where the platform draws the conditions'
    // editors - its panel - or the controls, none where a view draws both - the controls then always shown -, and the
    // controls bound to the controller whoever draws them. Pressed, it shows and hides what of them the platform
    // draws - the editors, the controls - and lays the form out anew; with no button, nothing changes
    public void testTheFilterButtonTogglesWhatOfTheFiltersThePlatformDraws() {
        // the platform's panel or a view's conditions, the platform's controls or a view's: whether there is a button
        boolean[][] cases = {{true, true, true}, {true, false, true}, {false, true, true}, {false, false, false}};
        for (boolean[] c : cases) {
            String which = (c[0] ? "the panel" : "a view's conditions") + ", " + (c[1] ? "the platform's" : "a view's") + " controls";
            ToolbarDouble toolbar = new GridToolbarDouble();
            FilterControlsDouble controls = new FilterControlsDouble();
            controls.shownByToolbar = c[1];
            int[] relayouts = new int[1];
            GFilterControlsToggle toggle = new GFilterControlsToggle(() -> relayouts[0]++, controls, toolbar);
            PanelDouble filter = new PanelDouble();
            PanelDouble panel = new PanelDouble(); // the panel's editors, where there are editors
            toggle.bind(filter, c[0] ? panel : null);
            assertEquals(which, c[2] ? 1 : 0, toolbar.filterToggles);
            assertEquals(which, !c[2], toggle.isVisible()); // with no button, always shown
            assertSame(which, filter, controls.handler);
            assertEquals(which, Arrays.asList(!c[2]), controls.shown); // the controls told so once, as they are bound
            toggle.setVisible(true);
            toggle.setVisible(false);
            assertEquals(which, c[2] ? Arrays.asList(false, true, false) : Arrays.asList(true), controls.shown);
            assertEquals(which, c[0] ? Arrays.asList(true, false) : new ArrayList<Boolean>(), panel.editorsShown);
            assertEquals(which, c[2] ? Arrays.asList(true, false) : new ArrayList<Boolean>(), toolbar.controlsShown);
            assertEquals(which, c[2] ? 2 : 0, relayouts[0]);
            assertEquals(which, !c[2], toggle.isVisible()); // hidden again - or always shown
        }
    }
    // a filter group with no filters has no view of either kind, so no controller: the platform is not asked to build
    // its check box or list, and where a view would draw it, nothing of it is there - the server claims no name for it
    // either (FormView.filterGroupScope)
    public void testAFilterGroupWithNoFiltersHasNoController() {
        GRegularFilterGroup[] empty = new GRegularFilterGroup[1];
        Fixture f = new Fixture(fixture -> (empty[0] = fixture.filterGroupIn(fixture.c, false)).filters.clear());
        assertNull(f.controllers.filterGroups.get(empty[0]));
        assertNull(field(f.snapshots[2], "items")); // c draws nothing of the group
        assertNull(f.projection.createFilterGroupController(empty[0], () -> {
            throw new AssertionError("the platform is asked for the view of a filter group with no filters");
        }));
    }

    // where a view draws a filter group, it is a part of its group there, under its own name, shaped as a property's
    // entry - the filter selected, none at first; the filters' captions; whether none may be selected - with a member
    // that selects one; only there. Its filters' keys are taken with the view of the container
    public void testAFilterGroupAViewDrawsIsAPartOfTheGroup() {
        Fixture f = new Fixture(fixture -> fixture.filterGroupIn(fixture.c, false));
        JavaScriptObject stock = field(field(f.snapshots[2], "items"), "stock");
        assertTrue(isNullField(stock, "value"));
        assertEquals("In stock,Out of stock", join(field(stock, "filters")));
        assertTrue(flag(stock, "canClear"));
        assertNotNull(member(f.sc, "items", "stock"));
        assertFalse(has(field(f.snapshots[0], "items"), "stock")); // only where it is drawn
        assertSame(f.viewWidget(f.c), f.controllers.filterGroups.get(f.form.regularFilterGroups.get(0)).getBindingWidget());
    }

    // its member selects a filter by its index, as the platform's list does, in a batch too - the form sends it and
    // shows it at once (showFilter), as it shows the one the server reports -, null for none where none may be; one
    // selected already is asked again - a batch that failed leaves it shown, the server holding another -; what is no
    // filter is refused, null too where one has to be selected
    public void testAFilterGroupsMemberSelectsAFilter() {
        Fixture f = new Fixture(fixture -> fixture.filterGroupIn(fixture.c, false));
        JavaScriptObject member = member(f.sc, "items", "stock");
        callChange(member, 1.0);
        assertEquals("regularFilter FILTERGROUP(stock) Out of stock", f.edits.get(f.edits.size() - 1));
        f.projection.flush();
        assertEquals(1.0, number(field(field(f.snapshots[2], "items"), "stock"), "value"), 0);
        int edits = f.edits.size();
        callChange(member, 1.0); // selected already: set again
        assertEquals(edits + 1, f.edits.size());
        assertEquals("regularFilter FILTERGROUP(stock) Out of stock", f.edits.get(f.edits.size() - 1));
        callChange(member, null);
        assertEquals("regularFilter FILTERGROUP(stock) none", f.edits.get(f.edits.size() - 1));
        batch(f.sc.controller, () -> callChange(member, 0.0));
        assertEquals("regularFilter FILTERGROUP(stock) In stock", f.edits.get(f.edits.size() - 2));
        assertEquals("sent", f.edits.get(f.edits.size() - 1));
        batch(f.sa.controller, () -> callChange(member, 1.0)); // another view's batch: it goes with it
        assertEquals("regularFilter FILTERGROUP(stock) Out of stock", f.edits.get(f.edits.size() - 2));
        assertRefusal("controller.items.stock.change(): takes the index of one of its filters, 0 to 1, or null for none", refusedChange(member, 2.0));
        assertRefusal("controller.items.stock.change(): takes the index of one of its filters", refusedChange(member, 0.5));
        assertRefusal("controller.items.stock.change(): takes the index of one of its filters", refusedChange(member, "In stock"));

        Fixture noNull = new Fixture(fixture -> fixture.filterGroupIn(fixture.c, true));
        assertTrue(isFalse(field(field(noNull.snapshots[2], "items"), "stock"), "canClear"));
        assertRefusal("controller.items.stock.change(): takes the index of one of its filters, 0 to 1: one has to be selected",
                refusedChange(member(noNull.sc, "items", "stock"), null));
        // ... a NONULL group of one filter being the platform's check box, which is cleared all the same
        Fixture checkBox = new Fixture(fixture -> fixture.filterGroupIn(fixture.c, true).filters.remove(1));
        assertTrue(flag(field(field(checkBox.snapshots[2], "items"), "stock"), "canClear"));
        callChange(member(checkBox.sc, "items", "stock"), null);
        assertEquals("regularFilter FILTERGROUP(stock) none", checkBox.edits.get(checkBox.edits.size() - 1));
    }

    // a filter's key works as the platform's widget's does, through the filter group's part, which the form binds it
    // with: taken with the view of the container the view draws the group in, while the form shows the filter group -
    // hidden by its SHOWIF, its descriptor saying so, the part gives no widget, as the platform's widget is hidden with
    // it: not taken, so another binding of it may be -: it selects the filter - and in a group of one filter, the
    // platform's check box, pressed again, takes it off, whatever NONULL says, as the check box is cleared; a group of
    // several's list only selects
    public void testAFilterGroupsKeySelectsItsFilter() {
        Fixture f = new Fixture(fixture -> fixture.filterGroupIn(fixture.c, false));
        GRegularFilterGroup stock = f.form.regularFilterGroups.get(0);
        GFilterGroupController part = f.controllers.filterGroups.get(stock);
        assertSame(f.viewWidget(f.c), part.getBindingWidget());
        part.onBinding(stock.filters.get(0)); // F10
        assertEquals("regularFilter FILTERGROUP(stock) In stock", f.edits.get(f.edits.size() - 1));
        part.onBinding(stock.filters.get(0)); // a list: selected again
        assertEquals("regularFilter FILTERGROUP(stock) In stock", f.edits.get(f.edits.size() - 1));
        GFormChanges hide = new GFormChanges(); put(hide, stock.showIfReader, GGroupObjectValue.EMPTY, true); f.apply(hide);
        assertNull(part.getBindingWidget()); // the form hides the filter group: the key is not taken
        GFormChanges show = new GFormChanges(); put(show, stock.showIfReader, GGroupObjectValue.EMPTY, null); f.apply(show);
        assertSame(f.viewWidget(f.c), part.getBindingWidget());

        Fixture single = new Fixture(fixture -> fixture.filterGroupIn(fixture.c, false).filters.remove(1));
        GRegularFilterGroup one = single.form.regularFilterGroups.get(0);
        GFilterGroupController check = single.controllers.filterGroups.get(one);
        check.onBinding(one.filters.get(0));
        assertEquals("regularFilter FILTERGROUP(stock) In stock", single.edits.get(single.edits.size() - 1));
        check.onBinding(one.filters.get(0)); // a check box: taken off
        assertEquals("regularFilter FILTERGROUP(stock) none", single.edits.get(single.edits.size() - 1));
        Fixture noNull = new Fixture(fixture -> fixture.filterGroupIn(fixture.c, true).filters.remove(1));
        GRegularFilterGroup cleared = noNull.form.regularFilterGroups.get(0);
        GFilterGroupController key = noNull.controllers.filterGroups.get(cleared);
        key.onBinding(cleared.filters.get(0));
        key.onBinding(cleared.filters.get(0)); // NONULL: the check box is cleared all the same
        assertEquals("regularFilter FILTERGROUP(stock) none", noNull.edits.get(noNull.edits.size() - 1));
    }

    // the table's view switch, where the toolbar is a view's and the platform draws the rows: the node says the view
    // the table shows and the views the design gives it, and its member changes it, as the platform's buttons do - to
    // one of those only
    public void testTheToolbarsViewSwitchIsAMember() {
        Fixture f = new Fixture(fixture -> {
            fixture.rowsByThePlatform();
            fixture.toolbarIn(fixture.c);
            fixture.group.grid.toolbar.showViews = true;
            fixture.views = new ListViewDouble();
        });
        assertEquals(0, f.platform.buttonsAsked); // the part takes the table's views alone, none of its buttons
        JavaScriptObject node = field(f.snapshots[2], "items");
        assertEquals("grid", text(node, "listView"));
        assertEquals("grid,pivot", join(field(node, "listViews")));
        JavaScriptObject member = field(f.sc.controller, "items");
        callChange(field(member, "listView"), "pivot");
        assertEquals(Arrays.asList(GListViewType.PIVOT), f.views.changed);
        assertEquals("pivot", text(field(f.snapshots[2], "items"), "listView")); // published before the call returns
        callChange(field(member, "listView"), "pivot"); // shown already
        assertEquals(1, f.views.changed.size());
        callAll(member, "export"); // the server's report of the rows, whatever view the platform's table shows
        assertEquals(Arrays.asList("export 10"), f.toolbarCalls);
        assertRefusal("controller.items.listView.change(): takes the name of a view of the table: grid, pivot",
                refusedChange(field(member, "listView"), "map"));
    }

    // what draws a group's rows gives itself to the group's toolbar as it is made, once, whoever draws them
    // (GGridController): the rows React draws through the projection - the grid's table part, with no views, which only
    // a table of the platform's has; the group's controller is its group's rows' part, as a tree's group's is -, the
    // platform's table in its constructor, as the recording one does, the projection giving the toolbar nothing then
    public void testTheRowsGiveThemselvesToTheGroupsToolbar() {
        Fixture f = Fixture.withOuter(); // the items' rows React's, the outer group's the platform's
        List<GGridController> given = ((GridToolbarDouble) f.controllers.toolbars.get(f.group)).gridRows;
        assertEquals(1, given.size());
        assertSame(f.controllers.gridRows.get(f.group), given.get(0)); // the table's part
        assertNotSame(f.controller(f.group), given.get(0));
        assertNull(given.get(0).getListViewController());
        assertEquals(Arrays.<GGridController>asList(f.platform), ((GridToolbarDouble) f.controllers.toolbars.get(f.outer)).gridRows);
    }
    // ... and what draws a tree's rows gives itself to the tree's toolbar the same way, once, whoever draws them
    // (GTreeController): the rows React draws through the projection - the tree as a whole, its grid's part -, the
    // platform's tree in its constructor, as the recording one does, the projection giving the toolbar nothing then.
    // What a view's part of the toolbar opens through members of its own the rows say: nothing over the rows React
    // draws, whose tree has those members already
    public void testTheRowsGiveThemselvesToTheTreesToolbar() {
        TreeFixture react = new TreeFixture();
        GTreeController rows = react.controllers.treeRows.get(react.tree);
        assertEquals(Arrays.asList(rows), ((TreeToolbarDouble) react.controllers.treeToolbars.get(react.tree)).treeRows);
        assertNull(rows.getOpenedByToolbarMembers());
        TreeFixture platform = new TreeFixture(TreeFixture::treeByThePlatform);
        List<GTreeController> given = ((TreeToolbarDouble) platform.controllers.treeToolbars.get(platform.tree)).treeRows;
        assertEquals(1, given.size());
        assertTrue(given.get(0) instanceof PlatformTreeDouble);
    }

    // EVERY NAME A PART WRITES - on its group's node, in data.<g> and on controller.<g>, and on a row of the group's
    // rows - is one the server reserves for that part there (ReactProjectionNames, of which FormView builds
    // getReservedNodeNames and getReservedRowNames), so no property the server lets through takes it: each part where
    // it alone is drawn, made to write all it writes, the entries' names its node carries aside. Which lists the
    // server takes for a scope - the toolbar's all of them, whatever its flags say - the corpus pins
    // (tests/compile/*/react)
    public void testEveryNameAPartWritesIsOneTheServerReservesThere() {
        // the rows, with the sortings that go with them: every attribute of the group delivered, a row's and the node's
        Fixture rows = new Fixture(fixture -> {
            fixture.group.rowBackgroundReader = new GRowBackgroundReader(10);
            fixture.group.rowForegroundReader = new GRowForegroundReader(10);
            fixture.group.customOptionsReader = new GCustomOptionsReader(10);
        });
        GFormChanges attributes = new GFormChanges();
        put(attributes, rows.group.rowBackgroundReader, rows.one, PValue.getPValue(new ColorDTO("ff0000")));
        put(attributes, rows.group.rowForegroundReader, rows.one, PValue.getPValue(new ColorDTO("00ff00")));
        put(attributes, rows.group.rowSelectReader, rows.one, true);
        put(attributes, rows.group.customOptionsReader, GGroupObjectValue.EMPTY, "{\"columns\": 2}");
        rows.apply(attributes);
        JavaScriptObject row = rows.row("1");
        assertTrue(own(rows.node(), "options") && own(row, "background") && own(row, "foreground") && own(row, "selected"));
        String columns = join(field(rows.node(), "properties"));
        assertNodeReserved("the rows", rows.node(), field(rows.sa.controller, "items"), columns,
                ReactProjectionNames.NODE_OWN, ReactProjectionNames.GRID_PART, ReactProjectionNames.ORDERS_PART);
        assertReserved("a row", row, columns, ReactProjectionNames.GRID_ROW);
        // ... and the controller of the container they are in: its own names - its batch, and the verbs every
        // controller has, which the form gives it (GController.extendController) -, beside the group's and the
        // properties'. The editing verbs the form gives it as well (GFormController.addEditingVerbs) want a form, which
        // these tests do not build
        new GController() {
            protected long dispatchExec(String action, ArrayList<Serializable> params, GwtActionDispatcher.ServerResponseCallback callback) { return 0; }
            protected long dispatchEval(String script, boolean evalAction, ArrayList<Serializable> params, GwtActionDispatcher.ServerResponseCallback callback) { return 0; }
            protected long dispatchChange(String property, ArrayList<Serializable> keyParams, Serializable value, GwtActionDispatcher.ServerResponseCallback callback) { return 0; }
            protected boolean isClosed() { return false; }
            protected GwtActionDispatcher getControllerDispatcher() { return null; }
        }.extendController(rows.sa.controller);
        assertTrue(own(rows.sa.controller, "exec") && own(rows.sa.controller, "batch"));
        assertReserved("the controller", rows.sa.controller, "items,single", ReactProjectionNames.CONTROLLER);

        // the rows of a tree's groups, one of its nodes asked to open: loading
        TreeFixture tree = new TreeFixture();
        tree.apply(closedPage(tree));
        callNode(field(tree.sa.controller, "cat"), "expand", tree.row(tree.cat, tree.c2));
        assertTrue(flag(tree.row(tree.cat, tree.c2), "loading"));
        for (GGroupObject group : Arrays.asList(tree.cat, tree.item)) {
            JavaScriptObject node = field(tree.data, group.getSID()), list = field(node, "list");
            assertNodeReserved("the rows of " + group.getSID(), node, field(tree.sa.controller, group.getSID()), "",
                    ReactProjectionNames.NODE_OWN, ReactProjectionNames.GRID_PART, ReactProjectionNames.TREE_PART,
                    ReactProjectionNames.ORDERS_PART);
            for (int i = 0; i < length(list); i++)
                assertReserved("a row of " + group.getSID(), at(list, i), "", ReactProjectionNames.GRID_ROW,
                        ReactProjectionNames.TREE_ROW);
        }

        // what the FILTERS box holds, the filter controls, a filter group - each alone in c
        Fixture filters = new Fixture(Fixture::filtersApart);
        assertNodeReserved("the user filters", field(filters.snapshots[2], "items"), field(filters.sc.controller, "items"),
                "", ReactProjectionNames.NODE_OWN, ReactProjectionNames.FILTERS_PART);
        Fixture controls = new Fixture(fixture -> fixture.filterControlsIn(fixture.c));
        assertNodeReserved("the filter controls", field(controls.snapshots[2], "items"), field(controls.sc.controller, "items"),
                "", ReactProjectionNames.NODE_OWN, ReactProjectionNames.FILTER_CONTROLS_PART);
        Fixture filterGroup = new Fixture(fixture -> fixture.filterGroupIn(fixture.c, false));
        assertNodeReserved("a filter group", field(filterGroup.snapshots[2], "items"), field(filterGroup.sc.controller, "items"),
                "stock", ReactProjectionNames.NODE_OWN); // its own name, claimed as a property's is

        // the toolbar in c, all its buttons whatever the design's flags say, which are the platform's
        Fixture toolbar = new Fixture(fixture -> {
            fixture.toolbarIn(fixture.c);
            GToolbar design = fixture.group.grid.toolbar;
            design.showCountQuantity = design.showCalculateSum = design.showPrintGroupXls = design.showManualUpdate = false;
        });
        assertNodeReserved("the toolbar", field(toolbar.snapshots[2], "items"), field(toolbar.sc.controller, "items"), "",
                ReactProjectionNames.NODE_OWN, ReactProjectionNames.TOOLBAR_PART);
    }
    // A COMPONENT'S OWN REACT VIEW: a component that is not a container whose custom is a React component draws what
    // it shows of its group - GRID(o) { custom = 'X'; }, as NEW w { custom = 'X'; MOVE GRID(o); } draws it -: the
    // grid's rows, a part of the group on the group's node in its view, with the entries of their columns and the
    // group's change(row); none of it the platform's, nor in a react container beside it
    public void testAGridOfItsOwnDrawsTheRows() {
        Fixture f = new Fixture(fixture -> { fixture.rowsByThePlatform(); fixture.ownView(fixture.group.grid); });
        assertFalse(f.controllers.gridRows.get(f.group) instanceof RecordingTableController);
        JavaScriptObject node = field(f.ownData(f.group.grid), "items");
        assertEquals(2, length(field(node, "list")));
        assertEquals(10.0, value(field(field(node, "byKey"), "1"), "price"), 0.0);
        assertNotNull(field(node, "quantity")); // a column's entry
        assertNotNull(member(f.ownStates.get(f.group.grid), "items", "change"));
        assertNull(field(f.data(), "items")); // a, beside it, has no node of the group
        assertFalse(own(f.ownData(f.group.grid), "grid")); // its own descriptor is not in its view
    }

    // ... and inside a react container it is placed there, as a component with a view of its own: the container
    // carries its descriptor and nothing of its rows, which its own view draws
    public void testAGridOfItsOwnInAContainerIsPlacedThere() {
        Fixture f = new Fixture(fixture -> fixture.ownView(fixture.group.grid)); // the grid in a
        assertTrue(f.group.grid.platformDrawn);
        assertNotNull(field(f.data(), "grid")); // a's entry of it, which labels what a places
        assertNull(field(f.data(), "items"));
        assertEquals(2, length(field(field(f.ownData(f.group.grid), "items"), "list")));
    }

    // a view the layout has not built - what a REMOVEd component would have - draws nothing: the platform's rows
    public void testAComponentWithNoViewBuiltIsThePlatforms() {
        Fixture f = new Fixture(fixture -> {
            fixture.rowsByThePlatform();
            fixture.group.grid.custom = "Own"; fixture.group.grid.react = true; // ... and no view of it
        });
        assertTrue(f.controllers.gridRows.get(f.group) instanceof RecordingTableController);
    }

    // a TOOLBARSYSTEM of its own is the toolbar's part on the group's node in its view, all its buttons - over the
    // rows a react container draws, or over the platform's -, and the container drawing the rows has none of them
    public void testAToolbarOfItsOwnDrawsTheButtons() {
        Fixture f = new Fixture(fixture -> {
            fixture.toolbarIn(fixture.form.mainContainer);
            fixture.ownView(fixture.group.grid.toolbar);
        });
        GReactFormData.ViewState toolbar = f.ownStates.get(f.group.grid.toolbar);
        assertFalse(f.controllers.toolbars.get(f.group) instanceof ToolbarDouble);
        assertTrue(isFalse(field(f.ownData(f.group.grid.toolbar), "items"), "manual"));
        assertEquals("export,filterControlsVisible,getCount,getSum,listView,manual,update", keysOf(field(toolbar.controller, "items")));
        assertFalse(own(field(f.data(), "items"), "manual")); // a draws the rows, not the toolbar
        assertNull(member(f.sa, "items", "getCount"));

        Fixture platform = new Fixture(fixture -> {
            fixture.rowsByThePlatform();
            fixture.toolbarIn(fixture.form.mainContainer);
            fixture.ownView(fixture.group.grid.toolbar);
        });
        assertTrue(platform.controllers.gridRows.get(platform.group) instanceof RecordingTableController);
        assertNotNull(member(platform.ownStates.get(platform.group.grid.toolbar), "items", "getCount"));
    }

    // FILTERCONTROLS of their own are the controls' part on the group's node in their view
    public void testFilterControlsOfTheirOwnAreThePart() {
        Fixture f = new Fixture(fixture -> {
            fixture.filterControlsIn(fixture.form.mainContainer);
            fixture.ownView(fixture.group.grid.filterControls);
        });
        GReactFormData.ViewState controls = f.ownStates.get(f.group.grid.filterControls);
        assertEquals("addFilter,applyFilters,resetFilters", keysOf(field(controls.controller, "items")));
        assertTrue(isFalse(field(f.ownData(f.group.grid.filterControls), "items"), "filtersPending"));
        assertFalse(has(field(f.sa.controller, "items"), "addFilter"));
    }

    // a FILTERGROUP of its own is the filter group's part on its group's node in its view, its keys bound to that view
    public void testAFilterGroupOfItsOwnIsThePart() {
        GRegularFilterGroup[] stock = new GRegularFilterGroup[1];
        Fixture f = new Fixture(fixture -> {
            stock[0] = fixture.filterGroupIn(fixture.form.mainContainer, false);
            fixture.ownView(stock[0]);
        });
        JavaScriptObject entry = field(field(f.ownData(stock[0]), "items"), "stock");
        assertEquals("In stock,Out of stock", join(field(entry, "filters")));
        assertNotNull(member(f.ownStates.get(stock[0]), "items", "stock"));
        // its F10, taken with its own view - no descriptor of its own there to say it is hidden
        assertSame(f.viewWidget(stock[0]), f.controllers.filterGroups.get(stock[0]).getBindingWidget());
    }

    // ... a group's node: what the parts on it write in data.<g> and on controller.<g>
    private static void assertNodeReserved(String what, JavaScriptObject node, JavaScriptObject member, String aside, String... lists) {
        assertReserved(what + ", in data", node, aside, lists);
        assertReserved(what + ", on the controller", member, aside, lists);
    }
    // what an object of the projection carries but the names given aside - the entries its node carries, a filter
    // group -, each in one of these lists (ReactProjectionNames)
    private static void assertReserved(String what, JavaScriptObject object, String aside, String... lists) {
        List<String> reserved = new ArrayList<>();
        for (String list : lists)
            reserved.addAll(Arrays.asList(list.split(",")));
        List<String> asideNames = Arrays.asList(aside.split(","));
        for (String name : keysOf(object).split(","))
            if (!name.isEmpty() && !asideNames.contains(name))
                assertTrue(what + ": it writes '" + name + "', which the server does not reserve there", reserved.contains(name));
    }
}
