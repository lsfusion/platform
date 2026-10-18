package lsfusion.gwt.client.form.controller;

import com.google.gwt.dom.client.Element;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.GForm;
import lsfusion.gwt.client.GFormChanges;
import lsfusion.gwt.client.GFormChangesTest;
import lsfusion.gwt.client.base.jsni.JsniTestSupport;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.base.view.GFlexAlignment;
import lsfusion.gwt.client.classes.data.GIntegerType;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.filter.user.GFilterControls;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.GObject;
import lsfusion.gwt.client.form.object.table.GToolbar;
import lsfusion.gwt.client.form.object.table.grid.GGrid;
import lsfusion.gwt.client.form.property.GClassViewType;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.controller.EndReason;
import lsfusion.gwt.client.form.view.FormContainer;
import lsfusion.gwt.client.navigator.controller.GAsyncFormController;
import lsfusion.gwt.client.navigator.window.GModalityWindowFormType;
import lsfusion.gwt.client.navigator.window.GWindowFormType;

// A form's controller as it is made, over a form given whole with nothing behind it - no server, no window: what its
// constructor builds is what a test reads, and what it is given then is what the server would send. The React view of
// a grid of its own is built with the form's containers, before the controllers, and is sized by the grid's columns,
// which the form counts before it builds the layout. A delta goes to the controller of each owner it carries something
// of, and what React keeps nothing of has one that keeps nothing: what the delta carries of it is dropped, and the
// rest of the delta applied
public class GFormControllerTest extends GWTTestCase {
    @Override public String getModuleName() { return "lsfusion.gwt.main"; }

    @Override protected void gwtSetUp() {
        JsniTestSupport.installMapDelete();
        installPageHelpers();
    }
    // what the client's page gives it and a test's page does not (webapp/static/js/utils.js): its text and element
    // helpers the layout calls as it builds the views - here doing nothing, or answering as for plain text: nothing
    // here reads a caption
    private static native void installPageHelpers() /*-{
        var noop = function () { };
        var helpers = {
            initCaptionHtmlOrText: noop, setCaptionHtmlOrText: noop, setCaptionNodeText: noop, initDataHtmlOrText: noop,
            setDataHtmlOrText: noop, clearDataHtmlOrText: noop, initHtmlOrText: noop, clearHtmlOrText: noop,
            setHtmlOrText: noop, setNodeText: noop, fixHyphens: noop, setAttributeOrStyle: noop,
            removeAttributeOrStyle: noop, setGlobalClassName: noop, setReadonlyNative: noop, setReadonlyClass: noop,
            setReadonlyHeur: noop, setReadonlyType: noop, setDisabledNative: noop, setDisabledClass: noop,
            setDisabledType: noop,
            containsHtmlTag: function () { return false; },
            hasLongStringWithoutSpacesAndHTMLTags: function () { return false; },
            createPlainObject: function () { return {}; },
            createFocusElement: function (tag) { return $doc.createElement(tag); },
            createFocusElementType: function (tag) { return $doc.createElement(tag); }
        };
        for (var name in helpers)
            if (!$wnd[name])
                $wnd[name] = helpers[name];
    }-*/;

    public void testAGridOfItsOwnIsSizedByItsColumns() {
        GForm form = leafGridForm();
        GGrid grid = form.groupObjects.get(0).grid;

        // no window to open it in: no forms controller
        GFormController controller = new GFormController(null, null, formContainer(), form, false, "leafGrid", 0, null);

        // the grid's view as the main container lays it out: its base size along the container, a vertical one - the
        // grid's height, its rows' by its columns' (a style here keeps no calc() size to read the width from)
        Widget view = controller.getFormLayout().getBaseComponentViews().get(grid).getShowingWidget();
        while (!(view.getLayoutData() instanceof FlexPanel.WidgetLayoutData))
            view = view.getParent();
        GSize laidOut = ((FlexPanel.WidgetLayoutData) view.getLayoutData()).flex.baseFlexBasis;
        assertEquals(grid.getHeight().getString(), laidOut.getString());
    }

    // the count a list group gets for its pivot, COUNT(<group>), shown only in the pivot, of a group whose rows a React
    // view draws, which is never in it: React keeps nothing of it, and the server sends nothing of it (#1892). A delta
    // that carries its values all the same - as the server's answer to a CANCEL did, for such a group on a page a tab
    // strip does not show - is applied without them; it used to fail the whole answer, the client actions after it too
    public void testAValueOfACountReactKeepsNothingOfIsDropped() {
        GForm form = leafGridForm();
        GPropertyDraw count = count(form, 22, form.groupObjects.get(0));
        GFormController controller = new GFormController(null, null, formContainer(), form, false, "leafGrid", 0, null);

        GFormChanges changes = new GFormChanges();
        NativeHashMap<GGroupObjectValue, PValue> values = new NativeHashMap<>();
        values.put(new GGroupObjectValue(10, 1), null); // the values of a property not read yet: none
        changes.properties.put(count, values);
        changes.needConfirm = true; // what the form takes after the values: the rest of the delta
        controller.applyRemoteChanges(changes, 1);
        assertTrue(controller.needConfirm());
    }

    // ... and its drop, as the answer that showed that page then sent, the count having been taken as shown there
    public void testADropOfACountReactKeepsNothingOfIsDropped() {
        GForm form = leafGridForm();
        GPropertyDraw count = count(form, 22, form.groupObjects.get(0));
        GFormController controller = new GFormController(null, null, formContainer(), form, false, "leafGrid", 0, null);

        GFormChanges changes = new GFormChanges();
        changes.dropProperties.add(count);
        changes.needConfirm = true;
        controller.applyRemoteChanges(changes, 1);
        assertTrue(controller.needConfirm());
    }

    // ... and a component React draws that the design does not name: React keeps nothing of it either, nothing of it is
    // sent, and a delta that carries its class all the same is applied without it
    public void testAClassOfAComponentReactKeepsNothingOfIsDropped() {
        GForm form = form("board");
        GContainer board = new GContainer(); // NEW board { custom = 'Board'; ... }
        board.ID = 2; board.nativeSID = "c2"; board.sID = "board"; board.custom = "Board"; board.react = true;
        board.container = form.mainContainer; form.mainContainer.children.add(board);
        layout(board);
        GContainer unnamed = new GContainer(); // ... with a container in it the design does not name
        unnamed.ID = 3; unnamed.nativeSID = "c3";
        unnamed.container = board; board.children.add(unnamed);
        layout(unnamed);
        form.initialFormChanges = GFormChangesTest.noChanges();
        GFormController controller = new GFormController(null, null, formContainer(), form, false, "board", 0, null);

        GFormChanges changes = new GFormChanges();
        NativeHashMap<GGroupObjectValue, PValue> values = new NativeHashMap<>();
        values.put(GGroupObjectValue.EMPTY, null);
        changes.properties.put(unnamed.elementClassReader, values);
        changes.needConfirm = true;
        controller.applyRemoteChanges(changes, 1);
        assertTrue(controller.needConfirm());
    }

    // a form of one list group whose rows the React view of its grid draws, GRID(items) { custom = 'LeafGrid'; }, with
    // two columns, as the server gives it - with nothing of its toolbar and filters to build
    private static GForm leafGridForm() {
        GForm form = form("leafGrid");
        GGroupObject group = new GGroupObject();
        group.ID = 10; group.nativeSID = "g10"; group.sID = "items"; group.viewType = GClassViewType.LIST;
        group.objects.add(new GObject(group, "item", 10, "item", GIntegerType.instance));
        form.groupObjects.add(group);
        GGrid grid = new GGrid();
        grid.ID = 11; grid.nativeSID = "c11"; grid.sID = "GRID(items)"; grid.groupObject = group;
        grid.custom = "LeafGrid"; grid.react = true; // GRID(items) { custom = 'LeafGrid'; }
        grid.width = -2; grid.height = -2; // its default size, as the server says, of its lines' default number
        grid.lineWidth = -1; grid.lineHeight = -1;
        grid.container = form.mainContainer; form.mainContainer.children.add(grid);
        layout(grid);
        grid.toolbar = new GToolbar(); // the design hides it, and the filters with it: nothing of them to build
        grid.toolbar.ID = 12; grid.toolbar.sID = "TOOLBARSYSTEM(items)"; grid.toolbar.visible = false;
        grid.filterControls = new GFilterControls();
        grid.filterControls.ID = 13; grid.filterControls.sID = "FILTERCONTROLS(items)";
        grid.filtersContainer = new GContainer();
        grid.filtersContainer.ID = 14; grid.filtersContainer.sID = "FILTERS(items)";
        group.grid = grid;
        column(form, 20, "price", group);
        column(form, 21, "quantity", group);
        form.initialFormChanges = GFormChangesTest.noChanges(); // it arrives with no changes
        return form;
    }

    // a form with nothing in its main container yet, which lays its children out in one line from the start
    private static GForm form(String sID) {
        GForm form = new GForm();
        form.sID = sID;
        form.mainContainer = new GContainer();
        form.mainContainer.ID = 1; form.mainContainer.nativeSID = "c1"; form.mainContainer.sID = "main";
        form.mainContainer.main = true;
        layout(form.mainContainer);
        form.mainContainer.lines = 1; form.mainContainer.childrenAlignment = GFlexAlignment.START;
        return form;
    }

    // what the server gives every component of a design to be laid out by
    private static void layout(GComponent component) {
        component.span = 1;
        component.setAlignment(GFlexAlignment.STRETCH);
        component.captionAlignmentHorz = GFlexAlignment.START; component.captionAlignmentVert = GFlexAlignment.START;
    }

    private static GPropertyDraw column(GForm form, int id, String name, GGroupObject group) {
        GPropertyDraw property = new GPropertyDraw();
        property.ID = id; property.nativeSID = "p" + id; property.sID = name; property.integrationSID = name;
        property.propertyFormName = name; property.groupObject = group; property.isList = true;
        property.valueType = GIntegerType.instance; property.valueWidth = 80; property.valueHeight = 20;
        form.propertyDraws.add(property);
        return property;
    }

    // ... and the count the server gives a list group for its pivot (FormEntity.initDefaultGroupElements): a column of
    // the group with no name, NOEXTID
    private static GPropertyDraw count(GForm form, int id, GGroupObject group) {
        GPropertyDraw count = column(form, id, "COUNT(" + group.sID + ")", group);
        count.integrationSID = null;
        return count;
    }

    private static FormContainer formContainer() {
        return new FormContainer(null, null, false, null) {
            protected void setContent(Widget widget) { }
            public GWindowFormType getWindowType() { return GModalityWindowFormType.FLOAT; }
            public void show(GAsyncFormController asyncFormController) { }
            public void hide(EndReason editFormCloseReason) { }
            public Element getContentElement() { return null; }
            public Widget getCaptionWidget() { return new Label(); }
        };
    }
}
