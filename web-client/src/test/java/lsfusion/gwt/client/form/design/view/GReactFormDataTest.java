package lsfusion.gwt.client.form.design.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.junit.client.GWTTestCase;
import lsfusion.gwt.client.GForm;
import lsfusion.gwt.client.GFormChanges;
import lsfusion.gwt.client.base.FocusUtils;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.jsni.JsniTestSupport;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.classes.GActionType;
import lsfusion.gwt.client.classes.data.GIntegerType;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.filter.user.GFilter;
import lsfusion.gwt.client.form.filter.user.GFilterControls;
import lsfusion.gwt.client.form.filter.user.GPropertyFilter;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.GObject;
import lsfusion.gwt.client.form.object.table.controller.GAbstractTableController;
import lsfusion.gwt.client.form.object.table.controller.GComponentController;
import lsfusion.gwt.client.form.object.table.controller.GGroupController;
import lsfusion.gwt.client.form.object.table.controller.GPropertyController;
import lsfusion.gwt.client.form.object.table.grid.GGrid;
import lsfusion.gwt.client.form.object.table.grid.GGridProperty;
import lsfusion.gwt.client.form.object.table.tree.GTreeGroup;
import lsfusion.gwt.client.form.property.GCaptionReader;
import lsfusion.gwt.client.form.property.GBackgroundReader;
import lsfusion.gwt.client.form.property.GCommentReader;
import lsfusion.gwt.client.form.property.GComponentLabelReader;
import lsfusion.gwt.client.form.property.GComponentReader;
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
import lsfusion.gwt.client.form.property.GRowSelectReader;
import lsfusion.gwt.client.form.property.GShowIfReader;
import lsfusion.gwt.client.form.property.GValueElementClassReader;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.view.Column;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

/** Runs in compiled JavaScript: reference identity and JS object keys are part of the contract. */
public class GReactFormDataTest extends GWTTestCase {
    @Override public String getModuleName() { return "lsfusion.gwt.main"; }

    @Override protected void gwtSetUp() { JsniTestSupport.installMapDelete(); }

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
        final GReactFormData.ContainerState sa, sb, sc;
        final JavaScriptObject[] snapshots = new JavaScriptObject[3];
        final int[] publications = new int[3];
        Runnable onPublish;
        // the platform's controller of a group and of a property, as far as the form hands it on
        final RecordingTableController lsf = new RecordingTableController();
        // ... and of the components, recording into lsf's log
        final RecordingLayoutController layout = new RecordingLayoutController(lsf);
        int placements;
        GGroupObject outer; // a group GWT draws the rows of, with one panel property in b - only in Fixture.withOuter()
        GPropertyDraw outerPanel, outerName; // ... and a list property, which GWT draws with the rows
        GPropertyDraw note; // a panel property of the group in a, beside its rows - only in Fixture.withNote()
        GPropertyDraw twin; // a panel property of the group in b named like the column `price` - only in withTwin()
        GGroupObjectValue chosen, changed; // what a member handed the form: the row its group's change(row) chose, and
                                           // the cell a property member changed

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
            group.ID = 10; group.nativeSID = "g10"; group.sID = "items";
            group.grid = new GGrid(); group.grid.ID = 11; group.grid.sID = "grid"; group.grid.container = a;
            a.children.add(group.grid);
            group.objects.add(new GObject(group, "item", 10, "item", GIntegerType.instance));
            group.rowSelectReader = new GRowSelectReader(10);
            form.groupObjects.add(group);
            price = property(20, "price", group, true, a);
            quantity = property(21, "quantity", group, true, a);
            panel = property(22, "panel", group, false, b);
            single = property(23, "single", null, false, a);
            nativeProperty = property(24, "native", null, false, b);
            nativeProperty.lsf = true;
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
            }, new GReactFormData.Verbs() { // the form, as far as a member asks it: what it is handed is kept
                public void changeCurrentObject(GGroupObject group, GGroupObjectValue key) { chosen = key; }
                public void changeProperties(GPropertyDraw[] properties, GGroupObjectValue[] keys, PValue[] values) { changed = keys[keys.length - 1]; }
                public void getPropertyValues(GPropertyDraw property, GGroupObjectValue key, String value, String actionSID, JavaScriptObject successCallback, JavaScriptObject failureCallback, int increaseValuesNeededCount) { }
                public long expandNode(GGroupObject group, GGroupObjectValue key) { return 0; }
                public long collapseNode(GGroupObject group, GGroupObjectValue key) { return 0; }
                public long expandAll(GGroupObject group) { return 0; }
                public long collapseAll(GGroupObject group) { return 0; }
                public void refreshOptimistic() { }
            });
            sa = projection.addContainer(a, data -> publish(0, data));
            sb = projection.addContainer(b, data -> publish(1, data));
            sc = projection.addContainer(c, data -> publish(2, data));
            controllers = new Controllers(projection, form, lsf, layout);
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
        // the additions a test asks for
        static Fixture withOuter() { return new Fixture(Fixture::addOuter); }
        static Fixture withNote() { return new Fixture(f -> f.note = f.property(25, "note", f.group, false, f.a)); }
        static Fixture withTwin() { return new Fixture(f -> f.twin = f.property(26, "price", f.group, false, f.b)); }
        // a group GWT draws the rows of, its grid in the main container, with one panel property in b
        void addOuter() {
            outer = new GGroupObject();
            outer.ID = 50; outer.nativeSID = "g50"; outer.sID = "outer";
            outer.grid = new GGrid(); outer.grid.ID = 51; outer.grid.sID = "outerGrid";
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
        GPropertyDraw property(int id, String name, GGroupObject group, boolean list, GContainer scope) {
            GPropertyDraw property = new GPropertyDraw();
            property.ID = id; property.nativeSID = "p" + id; property.sID = name; property.integrationSID = name;
            ((GComponent) property).ID = id; ((GComponent) property).sID = name;
            property.groupObject = group; property.isList = list; property.container = scope;
            property.valueType = GIntegerType.instance;
            form.propertyDraws.add(property); scope.children.add(property);
            return property;
        }
        // an lsf panel property React places: the design names it PROPERTY(<name>), its integration name is <name>
        GPropertyDraw lsfPanel(int id, String name, GGroupObject group, GContainer scope) {
            GPropertyDraw property = property(id, name, group, false, scope);
            property.lsf = true;
            property.sID = "PROPERTY(" + name + ")";
            ((GComponent) property).sID = property.sID;
            property.caption = name.toUpperCase();
            property.captionReader = new GCaptionReader(id, -1);
            return property;
        }
        JavaScriptObject data() { return snapshots[0]; }
        JavaScriptObject node() { return field(data(), "items"); }
        JavaScriptObject row(String key) { return field(field(node(), "byKey"), key); }
        // a delta, as the form hands it over: a group's rows and current object, a drop and a reader to the controller
        // of the owner
        void apply(GFormChanges changes) {
            changes.gridObjects.foreachEntry((group, rows) -> controller(group).updateKeys(group, rows, changes, 0));
            changes.objects.foreachEntry((group, key) -> controller(group).updateCurrentKey(key));
            changes.dropProperties.forEach(controllers::drop);
            changes.properties.foreachEntry((reader, values) -> controllers.update(reader, values, changes.updateProperties.contains(reader)));
            projection.flush();
        }
        // the form's controller of a group (GFormController.groupControllers): the node React draws the rows on - for
        // any other group, the platform's grid, not built here - and of a property
        GGroupController controller(GGroupObject group) {
            return controllers.groups.get(group);
        }
        GPropertyController controller(GPropertyDraw property) {
            return controllers.properties.get(property);
        }
        // the row a view names through the group's member - the row, its handle, its key - as the member reads it
        GGroupObjectValue choose(JavaScriptObject row) {
            chosen = null;
            changeRow(field(sa.controller, "items"), row);
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
        long requestIndex;

        public void changeCurrentObject(GGroupObject group, GGroupObjectValue key) { asked.add("current " + group.getSID() + " " + key.toKeyString()); }
        public void changeProperties(GPropertyDraw[] properties, GGroupObjectValue[] keys, PValue[] values) { }
        public void getPropertyValues(GPropertyDraw property, GGroupObjectValue key, String value, String actionSID, JavaScriptObject successCallback, JavaScriptObject failureCallback, int increaseValuesNeededCount) { }
        public long expandNode(GGroupObject group, GGroupObjectValue key) { return ask("expand " + group.getSID() + " " + key.toKeyString()); }
        public long collapseNode(GGroupObject group, GGroupObjectValue key) { return ask("collapse " + group.getSID() + " " + key.toKeyString()); }
        public long expandAll(GGroupObject group) { return ask("expand all " + group.getSID()); }
        public long collapseAll(GGroupObject group) { return ask("collapse all " + group.getSID()); }
        public void refreshOptimistic() { projection.flush(); }
        long ask(String request) {
            asked.add(request);
            return ++requestIndex;
        }
    }
    private static final class TreeFixture {
        final GForm form = new GForm();
        final GContainer a = container(1);
        final GTreeGroup tree = new GTreeGroup();
        final GGroupObject cat = new GGroupObject(), item = new GGroupObject();
        final GGroupObjectValue c1 = new GGroupObjectValue(60, 1), c2 = new GGroupObjectValue(60, 2);
        final GGroupObjectValue i15 = itemKey(1, 5), i16 = itemKey(1, 6), i27 = itemKey(2, 7);
        final GReactFormData projection;
        final GReactFormData.ContainerState sa; // the container the tree is drawn in: its controller
        final Controllers controllers;
        // the platform's controller of a group and of a property, as far as the form hands it on
        final RecordingTableController lsf = new RecordingTableController();
        // ... and of the components, recording into lsf's log
        final RecordingLayoutController layout = new RecordingLayoutController(lsf);
        final TreeVerbs verbs = new TreeVerbs();
        JavaScriptObject data;
        int publications;

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
            tree.ID = 62; tree.sID = "tree";
            tree.container = a;
            a.children.add(tree);
            form.treeGroups.add(tree);
            group(cat, 60, "cat");
            group(item, 61, "item");
            item.upTreeGroups.add(cat);
            addition.accept(this);
            projection = new GReactFormData(form, GComponent::getReactPlace, verbs);
            verbs.projection = projection;
            sa = projection.addContainer(a, d -> { data = d; publications++; });
            controllers = new Controllers(projection, form, lsf, layout);
        }
        void group(GGroupObject group, int id, String sid) {
            group.ID = id; group.nativeSID = "g" + id; group.sID = sid;
            group.parent = tree;
            tree.groups.add(group);
            group.objects.add(new GObject(group, sid, id, sid, GIntegerType.instance));
            form.groupObjects.add(group);
        }
        static GGroupObjectValue itemKey(int category, int item) {
            return new GGroupObjectValue(2, new int[]{60, 61}, new Serializable[]{category, item});
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
        // ... the answer to a request: its keys come with its index, as the form hands them on
        void answer(GFormChanges changes, int requestIndex) {
            changes.gridObjects.foreachEntry((group, rows) -> controllers.groups.get(group).updateKeys(group, rows, changes, requestIndex));
            changes.objects.foreachEntry((group, key) -> controllers.groups.get(group).updateCurrentKey(key));
            changes.dropProperties.forEach(controllers::drop);
            changes.properties.foreachEntry((reader, values) -> controllers.update(reader, values, changes.updateProperties.contains(reader)));
            projection.flush();
        }
    }

    private static GContainer container(int id) {
        GContainer container = new GContainer();
        container.ID = id; container.nativeSID = "c" + id; container.sID = "c" + id;
        container.react = true; container.custom = "Test";
        return container;
    }
    private static ArrayList<GGroupObjectValue> rows(GGroupObjectValue... keys) { return new ArrayList<>(Arrays.asList(keys)); }
    // the platform's controller of a group and of a property, as far as the form hands it on - a table controller,
    // which a property's view controller is too, with no form: what reaches it is recorded, and the rest of its
    // surface, which the projection never asks of it, does nothing
    private static final class RecordingTableController extends GAbstractTableController {
        final ArrayList<GPropertyReader> updated = new ArrayList<>();
        final ArrayList<GPropertyDraw> dropped = new ArrayList<>(), focused = new ArrayList<>();

        RecordingTableController() {
            super(null, null, false);
        }
        public void updateValue(GPropertyDraw property, NativeHashMap<GGroupObjectValue, PValue> values, boolean partial) { updated.add(property); }
        public void updateOther(GExtraPropertyReader reader, NativeHashMap<GGroupObjectValue, PValue> values, boolean partial) { updated.add(reader); }
        public void updateLabel(GExtraLabelReader reader, NativeHashMap<GGroupObjectValue, PValue> values, boolean partial) { updated.add(reader); }
        public void updateAttribute(GGroupObjectPropertyReader reader, NativeHashMap<GGroupObjectValue, PValue> values, boolean partial) { updated.add(reader); }
        public void dropProperty(GPropertyDraw property) { dropped.add(property); }
        public boolean isPropertyShown(GPropertyDraw property) { return true; }
        public void focusProperty(GPropertyDraw property) { focused.add(property); }
        public Pair<GGroupObjectValue, PValue> setLoadingValueAt(GPropertyDraw property, GGroupObjectValue fullCurrentKey, PValue value) { return null; }
        public GGroupObjectValue getSelectedKey() { return null; }
        public int getSelectedRow() { return -1; }
        public void modifyGroupObject(GGroupObjectValue key, boolean add, int position) { }
        public void updateKeys(GGroupObject group, ArrayList<GGroupObjectValue> keys, GFormChanges fc, int requestIndex) { }
        public void updateCurrentKey(GGroupObjectValue currentKey) { }
        public void changeCurrentKey(GGroupObjectValue currentKey) { }

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
        protected void configureToolbar() { }
        public List<GFilter> getFilters() { return null; }
        protected long changeFilter(ArrayList<GPropertyFilter> conditions) { return -1; }
        public boolean focusFirstWidget(FocusUtils.Reason reason) { return false; }
        public GGridProperty getGridComponent() { return null; }
        public void updateRowBackgroundValues(NativeHashMap<GGroupObjectValue, PValue> values) { }
        public void updateRowSelectValues(NativeHashMap<GGroupObjectValue, PValue> values, boolean updateKeys) { }
        public void updateRowForegroundValues(NativeHashMap<GGroupObjectValue, PValue> values) { }
        public void updateCustomOptionsValues(NativeHashMap<GGroupObjectValue, PValue> values) { }
        public GGroupObject getSelectedGroupObject() { return null; }
        public List<GPropertyDraw> getGroupObjectProperties() { return null; }
        public GPropertyDraw getSelectedFilterProperty() { return null; }
        public GGroupObjectValue getSelectedColumnKey() { return null; }
        public PValue getSelectedValue(GPropertyDraw property, GGroupObjectValue columnKey) { return null; }
        public List<Pair<Column, String>> getFilterColumns() { return null; }
        public GContainer getFiltersContainer() { return null; }
        public GFilterControls getFilterControls() { return null; }
        public boolean changeOrders(GGroupObject groupObject, LinkedHashMap<GPropertyDraw, Boolean> value, boolean alreadySet) { return false; }
    }
    // ... and the platform's controller of the components, the layout: what reaches it is recorded in the same log
    private static final class RecordingLayoutController implements GComponentController {
        final RecordingTableController reached;

        RecordingLayoutController(RecordingTableController reached) {
            this.reached = reached;
        }
        public void updateLabel(GComponentLabelReader reader, NativeHashMap<GGroupObjectValue, PValue> values, boolean partial) { reached.updated.add(reader); }
        public void updateOther(GComponentReader reader, NativeHashMap<GGroupObjectValue, PValue> values, boolean partial) { reached.updated.add(reader); }
    }
    // the form's controllers of the owners, as GFormController.initializeOwnerControllers settles them - what React has
    // of each owner, made from the platform's controller of it - and a delivery, as the form hands it
    // (GPropertyReader.update): to the controller of the reader's owner, told what kind of reader it is
    private static final class Controllers {
        final GForm form;
        final LinkedHashMap<GGroupObject, GGroupController> groups = new LinkedHashMap<>();
        final LinkedHashMap<GPropertyDraw, GPropertyController> properties = new LinkedHashMap<>();
        final LinkedHashMap<GComponent, GComponentController> components = new LinkedHashMap<>();

        Controllers(GReactFormData projection, GForm form, RecordingTableController lsf, RecordingLayoutController layout) {
            this.form = form;
            for (GGroupObject group : form.groupObjects)
                groups.put(group, projection.createGroupController(group, lsf));
            for (GPropertyDraw property : form.propertyDraws) {
                GPropertyController controller = projection.createPropertyController(property, lsf);
                if (controller != null) // none, as the form keeps it: React keeps nothing of it
                    properties.put(property, controller);
            }
            addComponent(projection, form.mainContainer, layout);
            projection.initialize();
        }
        void addComponent(GReactFormData projection, GComponent component, RecordingLayoutController layout) {
            GComponentController controller = projection.createComponentController(component, layout);
            if (controller != null)
                components.put(component, controller);
            for (GComponent child : component.getChildren())
                addComponent(projection, child, layout);
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
    private static boolean reachesLsf(RecordingTableController lsf, Controllers controllers, GPropertyReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        lsf.updated.clear();
        controllers.update(reader, values, false);
        return lsf.updated.contains(reader);
    }
    // ... and whether a property's drop does
    private static boolean dropReachesLsf(RecordingTableController lsf, Controllers controllers, GPropertyDraw property) {
        lsf.dropped.clear();
        controllers.drop(property);
        return lsf.dropped.contains(property);
    }
    private static void put(GFormChanges changes, GPropertyReader reader, GGroupObjectValue key, Object value) {
        NativeHashMap<GGroupObjectValue, PValue> values = changes.properties.get(reader);
        if (values == null) { values = new NativeHashMap<>(); changes.properties.put(reader, values); }
        values.put(key, value == null ? null : value instanceof Boolean ? PValue.getPValue((Boolean) value)
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
    // a member of a view's controller, at a group (null: the empty group, the controller itself)
    private static JavaScriptObject member(GReactFormData.ContainerState state, String group, String name) {
        JavaScriptObject owner = group == null ? state.controller : field(state.controller, group);
        return owner == null ? null : field(owner, name);
    }
    // ... a call through one, as a view makes it - and whether it is refused
    private static native void changeRow(JavaScriptObject member, JavaScriptObject row) /*-{ member.change(row); }-*/;
    private static native void changeAt(JavaScriptObject member, int value, JavaScriptObject row) /*-{ member.change(value, row == null ? undefined : row); }-*/;
    private static native void changeBatch(JavaScriptObject controller, JavaScriptObject member, int value) /*-{ controller.properties.change({property: member, value: value}); }-*/;
    private static boolean refuses(JavaScriptObject member, int value) {
        return refusesAt(member, value, null);
    }
    private static boolean refusesAt(JavaScriptObject member, int value, JavaScriptObject row) {
        try { changeAt(member, value, row); return false; } catch (RuntimeException e) { return true; }
    }
    private static boolean refusesBatch(JavaScriptObject controller, JavaScriptObject member, int value) {
        try { changeBatch(controller, member, value); return false; } catch (RuntimeException e) { return true; }
    }
    // the property a member of a view's controller stands for, at a group (null: the empty group, the controller
    // itself)
    private static GPropertyDraw memberProperty(GReactFormData.ContainerState state, String group, String name) {
        JavaScriptObject owner = group == null ? state.controller : field(state.controller, group);
        return owner == null ? null : GReactFormData.getMemberProperty(field(owner, name));
    }
    private static native double number(JavaScriptObject object, String name) /*-{ return object[name]; }-*/;
    private static native boolean has(JavaScriptObject object, String name) /*-{ return name in object; }-*/;
    private static native boolean sameField(JavaScriptObject a, String aName, JavaScriptObject b, String bName) /*-{ return a[aName] === b[bName]; }-*/;
    private static native boolean isFalse(JavaScriptObject object, String name) /*-{ return object[name] === false; }-*/;

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
        assertSame(f.panel, memberProperty(f.sb, "items", "panel"));          // member: the server reads it for the
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
        assertSame(f.price, memberProperty(f.sa, "items", "price")); // ... and so is the member
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
        assertSame(f.note, memberProperty(f.sa, "items", "note"));
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
    // entries - an lsf property's among them, by its integration name, with no member - and lists no `properties` (the
    // controller's batch at that level), and carries the components' descriptors too
    public void testTheTopLevelIsTheEmptyGroupsNode() {
        Fixture f = new Fixture();
        assertNotNull(field(f.data(), "single"));
        assertFalse(own(f.data(), "properties"));
        assertSame(f.single, memberProperty(f.sa, null, "single"));
        assertSame(f.single, f.sa.getGroupProperty(null, "single"));
        assertNull(f.sb.getGroupProperty(null, "single"));
        assertSame(f.nativeProperty, f.sb.getGroupProperty(null, "native")); // an lsf one is carried by name too...
        assertNull(memberProperty(f.sb, null, "native")); // ... with no member: its value is the platform's
        f.apply(drop(f.single));
        assertTrue(flag(field(f.data(), "single"), "hidden"));
        assertSame(f.single, f.sa.getGroupProperty(null, "single"));
        assertSame(f.single, memberProperty(f.sa, null, "single"));
        assertFalse(f.controller(f.single).isPropertyShown(f.single));
    }

    // an lsf panel property's entry - what labels it - is keyed by its integration name on its group's node, where a panel
    // property React draws would be, and carried by name with them, with no member: the platform draws its value. The
    // view names it by that name everywhere - an <Lsf> places it by its path, o.note - and the container finds its entry
    // by the property - what hides the host (useLsf)
    public void testAnLsfPanelPropertyIsKeyedByItsName() {
        final GPropertyDraw[] placed = new GPropertyDraw[2];
        Fixture f = new Fixture(fixture -> {
            placed[0] = fixture.lsfPanel(40, "note", fixture.group, fixture.b);
            placed[1] = fixture.lsfPanel(41, "memo", null, fixture.b);
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
        assertSame(placed[0], f.sb.getGroupProperty(f.group, "note"));
        assertSame(placed[1], f.sb.getGroupProperty(null, "memo"));
        assertNull(memberProperty(f.sb, "items", "note"));
        assertNull(memberProperty(f.sb, null, "memo"));
        assertSame(note, f.sb.entryOf(data, placed[0]));
        assertSame(memo, f.sb.entryOf(data, placed[1]));
        assertEquals("items.note", GReactFormData.getName(placed[0])); // the path of its entry, not PROPERTY(note)
        assertEquals("memo", GReactFormData.getName(placed[1]));
    }

    // a per-row <Lsf> names an LSF list property as a panel one is named - its group, then its integration name, the
    // path of its column entry -, found among the groups whose rows this container draws: no other container finds it,
    // and a name with no group, or a property whose cells React draws, names no renderer
    public void testAPerRowLsfIsNamedWithItsGroup() {
        final GPropertyDraw[] placed = new GPropertyDraw[1];
        Fixture f = new Fixture(fixture -> {
            placed[0] = fixture.property(44, "memo", fixture.group, true, fixture.a);
            placed[0].lsf = true;
        });
        assertSame(placed[0], f.sa.getRowLsfProperty("items.memo"));
        assertNull(f.sa.getRowLsfProperty("memo"));       // no group
        assertNull(f.sa.getRowLsfProperty("items.price")); // React draws its cells: no renderer
        assertNull(f.sb.getRowLsfProperty("items.memo"));  // b does not draw the rows
    }

    // ... so two groups whose rows this container draws may share both the name and a row's key: the name says which
    // group, and a row is checked against it by its own objects - another group's row with the same key is none of it
    public void testAPerRowLsfIsNamedByItsGroupNotByItsRow() {
        final GPropertyDraw[] memos = new GPropertyDraw[2];
        final GGroupObject[] others = new GGroupObject[1];
        Fixture f = new Fixture(fixture -> {
            GGroupObject other = others[0] = new GGroupObject();
            other.ID = 50; other.nativeSID = "g50"; other.sID = "others";
            other.grid = new GGrid(); other.grid.ID = 51; other.grid.sID = "otherGrid"; other.grid.container = fixture.a;
            fixture.a.children.add(other.grid);
            other.objects.add(new GObject(other, "other", 50, "other", GIntegerType.instance));
            other.rowSelectReader = new GRowSelectReader(50);
            fixture.form.groupObjects.add(other);
            memos[0] = fixture.property(45, "memo", fixture.group, true, fixture.a);
            memos[1] = fixture.property(46, "memo", other, true, fixture.a);
            for (GPropertyDraw memo : memos)
                memo.lsf = true;
        });
        GFormChanges changes = new GFormChanges();
        changes.gridObjects.put(others[0], rows(new GGroupObjectValue(50, 1)));
        f.apply(changes);
        assertSame(memos[0], f.sa.getRowLsfProperty("items.memo"));
        assertSame(memos[1], f.sa.getRowLsfProperty("others.memo"));
        GGroupObjectValue itemsRow = GGroupObjectValue.resolveObject(f.row("1"));
        JavaScriptObject othersRows = field(field(f.data(), "others"), "byKey");
        GGroupObjectValue othersRow = GGroupObjectValue.resolveObject(field(othersRows, "1")); // the same key string
        assertNotNull(f.group.getRowKey(itemsRow));
        assertNull(f.group.getRowKey(othersRow)); // what getPlacement checks a row by
        assertNull(GGroupObjectValue.resolveObject(keyOf("1"))); // a key string is no row
    }

    // ... and where nothing else of its group is drawn, it brings the group's node: its entry is carried there - while
    // an lsf panel ACTION, which has no entry, brings nothing. What labels it, and whether the form shows it, are read
    // through the container's answer for the property, from the snapshot handed in
    public void testAnLsfPanelPropertyBringsItsGroupsNode() {
        final GPropertyDraw[] placed = new GPropertyDraw[1];
        Fixture f = new Fixture(fixture -> placed[0] = fixture.lsfPanel(42, "note", fixture.group, fixture.c));
        GPropertyDraw note = placed[0];
        f.projection.flush();
        assertEquals("note", join(field(field(f.snapshots[2], "items"), "properties")));
        assertTrue(own(f.sc.controller, "items"));
        assertNull(memberProperty(f.sc, "items", "note"));
        NativeHashMap<GGroupObjectValue, PValue> caption = new NativeHashMap<>();
        caption.put(GGroupObjectValue.EMPTY, PValue.getPValue("Remark"));
        assertFalse(reachesLsf(f.lsf, f.controllers, note.captionReader, caption)); // React's
        f.projection.flush();
        assertEquals("Remark", text(f.sc.entryOf(f.snapshots[2], note), "caption"));
        GFormChanges show = new GFormChanges();
        put(show, note, f.one, 5);
        f.apply(show);
        assertFalse(flag(f.sc.entryOf(f.snapshots[2], note), "hidden"));
        f.apply(drop(note));
        assertTrue(flag(f.sc.entryOf(f.snapshots[2], note), "hidden"));

        final GPropertyDraw[] action = new GPropertyDraw[1];
        Fixture g = new Fixture(fixture -> (action[0] = fixture.lsfPanel(43, "go", fixture.group, fixture.c)).valueType = GActionType.instance);
        g.projection.flush();
        assertFalse(own(g.snapshots[2], "items"));
        assertFalse(own(g.sc.controller, "items"));
        assertNull(g.sc.entryOf(g.snapshots[2], action[0]));
    }

    // the controller is the state's, and its members mirror the entries, made once with them: a group's member wherever
    // a part of it is drawn, its change(row) only where its rows are, a member per value the node carries - none for an
    // lsf property - and a member stays, as its entry does, while the form hides the property
    public void testMembersMirrorTheEntries() {
        Fixture f = new Fixture();
        assertTrue(own(field(f.sa.controller, "items"), "change"));  // the rows are drawn here...
        assertFalse(own(field(f.sb.controller, "items"), "change")); // ... and not here, where only a panel property is
        assertSame(f.price, memberProperty(f.sa, "items", "price"));
        assertSame(f.panel, memberProperty(f.sb, "items", "panel"));
        assertNull(memberProperty(f.sa, "items", "panel"));
        assertSame(f.single, memberProperty(f.sa, null, "single")); // the empty group's, on the controller itself
        assertNull(memberProperty(f.sb, null, "native"));
        JavaScriptObject price = field(field(f.sa.controller, "items"), "price");
        f.apply(drop(f.price));
        assertSame(price, field(field(f.sa.controller, "items"), "price"));
    }

    // the form's controllers of what React draws are what React has of it: a group's rows, its current object, the
    // client's own add or remove go to the node the rows are drawn on; a property's optimistic value, and whether the
    // form shows it, to what React has of the property - whatever group it is of, one whose rows React draws or one
    // whose rows GWT draws - and one React carries by no name has none: nothing is sent for it. Which controller the
    // form takes is GFormController.initializeOwnerControllers', not built here
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
        assertNull(f.controller(f.unnamed));
        // an lsf property's value is the platform's, and so is where
        f.controller(f.nativeProperty).focusProperty(f.nativeProperty);
        assertTrue(f.lsf.focused.contains(f.nativeProperty));  // it is edited: its controller is the platform's own
        assertSame(f.lsf, f.controller(f.plain)); // ... and one React has nothing of is the platform's alone
        // as a component, a property is the platform's where the platform draws it - an lsf one too, React labelling it
        // - and has no controller where React draws its content: nothing is sent for it (createComponentController)
        assertSame(f.layout, f.controllers.components.get(f.plain));
        assertSame(f.layout, f.controllers.components.get(f.nativeProperty));
        assertNull(f.controllers.components.get(f.price));
    }

    // a member holds the state it stands for and reads what a call through it names from that state - the row, the
    // value - handing the form the edit (Verbs); the batch takes only a member of its own view's controller
    public void testAMemberHandsTheFormWhatItNames() {
        Fixture f = new Fixture();
        JavaScriptObject price = member(f.sa, "items", "price");
        changeAt(price, 5, keyOf("2"));
        assertEquals(f.two, f.changed); // a key names a row where the rows are drawn
        changeAt(price, 5, null);
        assertEquals(GGroupObjectValue.EMPTY, f.changed); // no row: the current object
        assertTrue(refusesAt(member(f.sb, "items", "panel"), 1, keyOf("1"))); // a panel entry names no row
        f.changed = null;
        assertTrue(refusesBatch(f.sb.controller, price, 1)); // a member of another view's controller
        assertNull(f.changed);
        assertFalse(refusesBatch(f.sa.controller, price, 1));
        assertEquals(GGroupObjectValue.EMPTY, f.changed);
    }

    // a react container nothing places draws no caption of its own: the platform draws it around the container, so its
    // own data has no entry for it - while a container it draws has one, and so does an lsf child it places
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
        assertNull(f.sa.getGroupProperty(null, "grid")); // ... and it is data only, no member
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
        RecordingTableController lsf = new RecordingTableController();
        GReactFormData projection = new GReactFormData(f.form, GComponent::getReactPlace, null);
        final JavaScriptObject[] published = new JavaScriptObject[1];
        projection.addContainer(f.a, data -> { });
        projection.addContainer(f.b, data -> { });
        projection.addContainer(f.c, data -> { });
        projection.addContainer(details, data -> published[0] = data); // GFormLayout builds the record's containers too
        Controllers controllers = new Controllers(projection, f.form, lsf, new RecordingLayoutController(lsf));
        NativeHashMap<GGroupObjectValue, PValue> values = new NativeHashMap<>();
        values.put(GGroupObjectValue.EMPTY, PValue.getPValue(5));
        assertFalse(reachesLsf(lsf, controllers, note, values));
        projection.flush();
        assertEquals(5.0, value(field(published[0], "outer"), "recordNote"), 0.0);
        assertEquals("In record", text(field(published[0], "c63"), "caption"));
        assertFalse(own(published[0], "c61")); // the react container itself: nothing places it
    }

    // an LSF grid property is a column React draws over the editors the platform draws in its rows: an ordinary column
    // entry, listed in `properties`, with no cells and no member - its values are the platform's
    public void testAnLsfColumnIsAnOrdinaryColumn() {
        Fixture f = new Fixture();
        GPropertyDraw editor = f.property(27, "editor", f.group, true, f.a); // added before a new projection is made
        editor.lsf = true;
        editor.caption = "Editor";
        editor.captionReader = new GCaptionReader(27, 10); // every property has one: the static caption is its fallback
        RecordingTableController lsf = new RecordingTableController();
        GReactFormData projection = new GReactFormData(f.form, GComponent::getReactPlace, null);
        final JavaScriptObject[] published = new JavaScriptObject[1];
        GReactFormData.ContainerState sa = projection.addContainer(f.a, data -> published[0] = data);
        projection.addContainer(f.b, data -> { });
        projection.addContainer(f.c, data -> { });
        Controllers controllers = new Controllers(projection, f.form, lsf, new RecordingLayoutController(lsf));
        controllers.groups.get(f.group).updateKeys(f.group, rows(f.one), null, 0);
        projection.flush();
        // hidden until the platform shows it:
        assertTrue(flag(field(field(published[0], "items"), "editor"), "hidden"));
        NativeHashMap<GGroupObjectValue, PValue> values = new NativeHashMap<>();
        values.put(f.one, PValue.getPValue(5));
        // its whole delivery, handed on to the platform's editors, which draw the value, theirs alone
        assertTrue(reachesLsf(lsf, controllers, editor, values));
        projection.flush();
        JavaScriptObject node = field(published[0], "items");
        JavaScriptObject column = field(node, "editor");
        assertEquals("Editor", text(column, "caption"));
        assertFalse(flag(column, "hidden"));
        assertFalse(own(field(field(node, "byKey"), "1"), "editor"));
        assertTrue(join(field(node, "properties")).contains("editor"));
        assertNull(memberProperty(sa, "items", "editor"));
        assertSame(editor, sa.getGroupProperty(f.group, "editor")); // a condition or a sorting may name it
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
        assertTrue(reachesLsf(f.lsf, f.controllers, f.nativeProperty, values));
        assertTrue(reachesLsf(f.lsf, f.controllers, new GBackgroundReader(24, -1), values));
        values.put(GGroupObjectValue.EMPTY, PValue.getPValue("Caption"));
        assertFalse(reachesLsf(f.lsf, f.controllers, f.nativeProperty.captionReader, values));
        f.projection.flush();
        assertEquals("Caption", text(field(f.snapshots[1], "native"), "caption"));
        assertFalse(hasValue(field(f.snapshots[1], "native"))); // its value is the platform's: its descriptor has none
        assertEquals(1, f.publications[0]);
    }

    // a drop goes where the property's values go: one React draws is dropped here alone - the platform has no view of
    // it - and an lsf one, like one nothing places, to the platform alone, which draws it: what React labels of it
    // hides with the drop, which comes through its entry on its way to the platform
    public void testADropGoesWhereTheValuesGo() {
        Fixture f = new Fixture();
        assertFalse(dropReachesLsf(f.lsf, f.controllers, f.panel));
        assertTrue(dropReachesLsf(f.lsf, f.controllers, f.nativeProperty));
        assertTrue(dropReachesLsf(f.lsf, f.controllers, f.plain));
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
        assertFalse(reachesLsf(f.lsf, f.controllers, f.box.elementClassReader, values));
        assertFalse(reachesLsf(f.lsf, f.controllers, new GLoadingReader(20, 10), values));
        assertTrue(reachesLsf(f.lsf, f.controllers, f.a.showIfReader, values));
        assertTrue(reachesLsf(f.lsf, f.controllers, f.a.captionReader, values));
        assertTrue(reachesLsf(f.lsf, f.controllers, f.form.mainContainer.showIfReader, values));
        f.projection.flush();
        assertSame(before, f.data());
        assertEquals(1, f.publications[0]);
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
        assertSame(f.lsf, f.controller(f.outer));
        assertEquals(5.0, value(field(f.snapshots[1], "outer"), "outerPanel"), 0.0); // ... and the panel in b is shown
        // no row is chosen there: the group's current object is the platform grid's
        assertNull(field(field(f.snapshots[1], "outer"), "list"));
        NativeHashMap<GGroupObjectValue, PValue> names = new NativeHashMap<>();
        names.put(seven, PValue.getPValue(1));
        assertTrue(reachesLsf(f.lsf, f.controllers, f.outerName, names)); // a column of those rows is GWT's too
    }

    // the rows a group's per-row renderers follow (GGridPanelController): handed out once, while the form is built,
    // they are the node's own list - the server's rows and a view's own add or remove alike -, and reading them asks no
    // placement
    public void testTheRowsRenderersFollowAreTheNodesOwn() {
        Fixture f = new Fixture(); // its form, drawn again by a projection of its own, no row delivered yet
        RecordingTableController lsf = new RecordingTableController();
        int[] placements = new int[1];
        GReactFormData projection = new GReactFormData(f.form, component -> {
            placements[0]++;
            return component.getReactPlace();
        }, null);
        projection.addContainer(f.a, data -> { });
        projection.addContainer(f.b, data -> { });
        projection.addContainer(f.c, data -> { });
        Controllers controllers = new Controllers(projection, f.form, lsf, new RecordingLayoutController(lsf));
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
        assertTrue(reachesLsf(f.lsf, f.controllers, f.plain, values)); // React has nothing of it
        assertTrue(reachesLsf(f.lsf, f.controllers, f.plain.captionReader, values)); // ... nor of any reader of it
        assertEquals(placed, f.placements);
    }

    // a reader goes to ONE place: what React has of its owner - the property, the group, the component it is of - told
    // what kind of reader it is, or the platform, where React takes nothing of it. An lsf column's values and drop go
    // through its entry to the platform (an unnamed one is refused when the form is built); an lsf ACTION has no entry,
    // its caption being its button's face, the platform's; an lsf container React places has its caption drawn by
    // React - its caption's class goes on to the platform, named or not, which has no caption widget to put it on - and
    // its SHOWIF is React's alone: a view hides its host (useLsf)
    public void testAReaderGoesToWhatReactHasOfItsOwner() {
        Fixture f = new Fixture();
        GPropertyDraw column = f.property(32, "column", f.group, true, f.a);
        column.lsf = true;
        GPropertyDraw button = f.property(33, "button", null, false, f.b);
        button.lsf = true;
        button.valueType = GActionType.instance;
        button.captionReader = new GCaptionReader(33, -1);
        GContainer placed = container(34), unnamed = container(35);
        for (GContainer lsf : Arrays.asList(placed, unnamed)) {
            lsf.react = false; lsf.custom = null; lsf.lsf = true;
            lsf.container = f.b; f.b.children.add(lsf);
        }
        unnamed.sID = null;
        RecordingTableController lsf = new RecordingTableController();
        GReactFormData projection = new GReactFormData(f.form, GComponent::getReactPlace, null);
        final JavaScriptObject[] published = new JavaScriptObject[1];
        projection.addContainer(f.a, data -> { });
        projection.addContainer(f.b, data -> published[0] = data);
        projection.addContainer(f.c, data -> { });
        Controllers controllers = new Controllers(projection, f.form, lsf, new RecordingLayoutController(lsf));
        NativeHashMap<GGroupObjectValue, PValue> values = new NativeHashMap<>();
        values.put(GGroupObjectValue.EMPTY, PValue.getPValue("Go"));
        assertTrue(reachesLsf(lsf, controllers, column, values)); // an lsf column's values go through its entry...
        assertTrue(dropReachesLsf(lsf, controllers, column));     // ... and so does its drop, to the platform
        // the platform's alone: its button's face
        assertTrue(reachesLsf(lsf, controllers, button.captionReader, values));
        assertFalse(reachesLsf(lsf, controllers, placed.captionReader, values));
        assertTrue(reachesLsf(lsf, controllers, placed.captionClassReader, values));
        assertTrue(reachesLsf(lsf, controllers, placed.valueClassReader, values));
        assertTrue(reachesLsf(lsf, controllers, unnamed.captionClassReader, values));
        assertTrue(reachesLsf(lsf, controllers, unnamed.captionReader, values));
        NativeHashMap<GGroupObjectValue, PValue> hidden = new NativeHashMap<>();
        hidden.put(GGroupObjectValue.EMPTY, PValue.getPValue(true));
        assertFalse(reachesLsf(lsf, controllers, placed.showIfReader, hidden)); // React's alone
        projection.flush();
        assertFalse(own(published[0], "button")); // an lsf ACTION has no entry
        assertEquals("Go", text(field(published[0], "c34"), "caption"));
        assertTrue(flag(field(published[0], "c34"), "hidden"));
    }

    // a form with no react container has its projection too, an empty one: it places nothing and takes none of the
    // readers - not even one that would be React's if its container's state were made - and walks no placement, at its
    // start or with a delta
    public void testAProjectionWithNoReactContainerTakesNothing() {
        Fixture f = new Fixture(); // its form, and this time no react container's state
        int[] placements = new int[1];
        RecordingTableController lsf = new RecordingTableController();
        GReactFormData projection = new GReactFormData(f.form, component -> {
            placements[0]++;
            return component.getReactPlace();
        }, null);
        Controllers controllers = new Controllers(projection, f.form, lsf, new RecordingLayoutController(lsf));
        NativeHashMap<GGroupObjectValue, PValue> values = new NativeHashMap<>();
        values.put(f.one, PValue.getPValue(5));
        assertTrue(reachesLsf(lsf, controllers, f.price, values));
        assertTrue(reachesLsf(lsf, controllers, f.box.elementClassReader, values));
        assertTrue(reachesLsf(lsf, controllers, f.group.rowSelectReader, values));
        assertSame(lsf, controllers.groups.get(f.group)); // its rows go to the platform's grid
        assertTrue(dropReachesLsf(lsf, controllers, f.price));
        projection.flush();
        assertSame(lsf, controllers.properties.get(f.price)); // its values go to the platform too
        assertEquals(0, placements[0]); // nothing is walked: there is nowhere to be placed
    }

    public void testRowsAndAPanelPropertyShareTheGridNode() {
        Fixture f = Fixture.withNote();
        GFormChanges delta = new GFormChanges();
        put(delta, f.note, GGroupObjectValue.EMPTY, 7);
        f.apply(delta);
        assertEquals(7.0, value(f.node(), "note"), 0.0); // one node: the rows and the panel entry side by side
        assertEquals(2, length(field(f.node(), "list")));
        assertSame(f.note, memberProperty(f.sa, "items", "note"));
        JavaScriptObject note = field(f.node(), "note");
        f.controller(f.group).changeCurrentKey(f.two);
        f.projection.flush();
        assertTrue(flag(f.row("2"), "isCurrent"));
        assertSame(note, field(f.node(), "note")); // the selection moved in the rows, and the panel entry stayed
    }

    public void testANameMeansThePropertyThisContainerCarries() {
        Fixture f = Fixture.withTwin(); // `price`: a column where the rows are, and a panel property in b
        assertSame(f.price, f.sa.getGroupProperty(f.group, "price"));
        assertSame(f.twin, f.sb.getGroupProperty(f.group, "price"));
        assertNull(f.sb.getGroupProperty(f.group, "quantity")); // b does not carry the column
        assertNull(f.sc.getGroupProperty(f.group, "price"));    // nor does a container with no node of it
        assertNotNull(field(f.node(), "list")); // a draws the rows ...
        assertNull(field(field(f.snapshots[1], "items"), "list")); // ... b only a panel property of the group
    }

    // a group's node is data's OWN field: a group named like a member of Object.prototype gets a node of its own, a
    // plain object, and not a copy of what a plain `{}` answers for that name
    public void testAGroupNamedLikeAnObjectPrototypeMemberHasItsOwnNode() {
        Fixture f = new Fixture();
        f.group.sID = "constructor";
        RecordingTableController lsf = new RecordingTableController();
        GReactFormData projection = new GReactFormData(f.form, GComponent::getReactPlace, null);
        final JavaScriptObject[] published = new JavaScriptObject[1];
        projection.addContainer(f.a, data -> published[0] = data);
        projection.addContainer(f.b, data -> { });
        projection.addContainer(f.c, data -> { });
        Controllers controllers = new Controllers(projection, f.form, lsf, new RecordingLayoutController(lsf));
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
        Fixture f = Fixture.withTwin(); // two properties in b: the outer batch changes one, the nested flush the other
        GFormChanges first = new GFormChanges(); put(first, f.twin, GGroupObjectValue.EMPTY, 7); f.apply(first);
        final int[] callbacks = {0};
        f.onPublish = () -> {
            if (callbacks[0]++ == 0) { // during a's publication: b, still in the outer batch, is changed and flushed
                f.setValue(f.twin, GGroupObjectValue.EMPTY, 13);
                f.projection.flush();
            }
        };
        GFormChanges delta = new GFormChanges(); put(delta, f.quantity, f.one, 3); put(delta, f.panel, GGroupObjectValue.EMPTY, 31); f.apply(delta);
        assertEquals(3.0, value(f.row("1"), "quantity"), 0.0);
        // the nested flush gave b out, with both changes: the outer batch's and its own
        assertEquals(31.0, value(field(f.snapshots[1], "items"), "panel"), 0.0);
        assertEquals(13.0, value(field(f.snapshots[1], "items"), "price"), 0.0);
        assertEquals(2, f.publications[0]); // the fixture's own, the delta's
        // the fixture's own, the twin's first value, the nested flush's: the outer flush did not give b out again
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
    private static native void callNode(JavaScriptObject member, String verb, JavaScriptObject row) /*-{ member[verb](row); }-*/;

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
        assertEquals(publications + 1, f.publications);
        assertSame(c1, f.row(f.cat, f.c1)); // the other node kept its object
        f.answer(new GFormChanges(), 0); // an answer to a request before it changes nothing
        assertTrue(flag(f.row(f.cat, f.c2), "expanded"));
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
        assertEquals(2.0, number(f.row(f.item, f.i27), "parent"), 0.0);
        callNode(cat, "collapse", f.row(f.cat, f.c2));
        assertTrue(isFalse(f.row(f.cat, f.c2), "expanded")); // closed at once, while its child is still loaded
        assertNotNull(f.row(f.item, f.i27));
        f.answer(new GFormChanges(), 2); // an answer with no keys - a failed one - leaves it as asked, as the platform's
        assertTrue(isFalse(f.row(f.cat, f.c2), "expanded")); // tree does
        f.answer(openPage(f), 2); // ... and keys that still hold its child: the server did not close it
        assertTrue(flag(f.row(f.cat, f.c2), "expanded"));
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
        assertTrue(isFalse(f.row(f.cat, f.c2), "expanded")); // asked, but there is nothing to open
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
    private static native void callAll(JavaScriptObject member, String verb) /*-{ member[verb](); }-*/;

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
}
