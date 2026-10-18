package lsfusion.gwt.client.form.controller;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.GForm;
import lsfusion.gwt.client.GFormChanges;
import lsfusion.gwt.client.GFormChangesDTO;
import lsfusion.gwt.client.GFormChangesTest;
import lsfusion.gwt.client.action.GRequestUserInputAction;
import lsfusion.gwt.client.base.jsni.JsniTestSupport;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.base.view.GFlexAlignment;
import lsfusion.gwt.client.classes.data.GIntegerType;
import lsfusion.gwt.client.form.controller.dispatch.GFormActionDispatcher;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.event.GBindingEnv;
import lsfusion.gwt.client.form.event.GBindingEvent;
import lsfusion.gwt.client.form.event.GInputBindingEvent;
import lsfusion.gwt.client.form.event.GKeyInputEvent;
import lsfusion.gwt.client.form.event.GKeyStroke;
import lsfusion.gwt.client.form.event.GMouseInputEvent;
import lsfusion.gwt.client.form.filter.user.GFilterControls;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.GObject;
import lsfusion.gwt.client.form.object.table.GToolbar;
import lsfusion.gwt.client.form.object.table.grid.GGrid;
import lsfusion.gwt.client.form.property.GClassViewType;
import lsfusion.gwt.client.form.property.GExtraPropReader;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.GPropertyReaderDTO;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.controller.EndReason;
import lsfusion.gwt.client.form.property.cell.view.GUserInputResult;
import lsfusion.gwt.client.form.view.FormContainer;
import lsfusion.gwt.client.navigator.controller.GAsyncFormController;
import lsfusion.gwt.client.navigator.window.GModalityWindowFormType;
import lsfusion.gwt.client.navigator.window.GWindowFormType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

import static lsfusion.gwt.client.GFormChanges.GPropertyReadType.DRAW;

// A form's controller as it is made, over a form given whole with nothing behind it - no server, no window: what its
// constructor builds is what a test reads, and what it is given then is what the server would send. The React view of
// a grid of its own is built with the form's containers, before the controllers, and is sized by the grid's columns,
// which the form counts before it builds the layout. A delta goes to the controller of each owner it carries something
// of, and what React keeps nothing of has one that keeps nothing: what the delta carries of it is dropped, and the
// rest of the delta applied. An input the server asks for with no cell to enter it in is cancelled. A key is
// pressed in the page, and handed to the form's bindings as the form's listener hands it (press)
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

    // an input asks for its value in the cell the change asking for it was made in; one asked for by no such change - a
    // view's call, a key of what a React view draws, a form's event - finds no cell: it is cancelled, as a user cancels
    // one, the action asking for it going on as it then would, and the console says why. The client used to open its
    // editor in no cell and fail the server's answer - an Internal Server Error
    public void testAnInputWithNoCellToEnterItInIsCancelled() {
        GFormController controller = new GFormController(null, null, formContainer(), leafGridForm(), false, "leafGrid", 0, null);
        GFormActionDispatcher dispatcher = new GFormActionDispatcher(controller); // as the form's after such a change: no cell
        Object[] answer = new Object[1];
        String said = consoleErrorsOf(() -> answer[0] = dispatcher.execute(new GRequestUserInputAction(GIntegerType.instance, null, false, false, null, null, null)));
        assertSame(GUserInputResult.canceled, answer[0]);
        assertTrue(said, said.contains("no cell to enter it in"));
    }
    // what the client writes on the console as an error while run runs (GwtClientUtils.consoleError)
    private static native String consoleErrorsOf(Runnable run) /*-{
        var said = [], error = console.error;
        console.error = function (message) { said.push(message); };
        try { run.@java.lang.Runnable::run()(); } finally { console.error = error; }
        return said.join('\n');
    }-*/;

    // the key of a grid property the platform draws once per row of a grid its own React view draws (<Platform
    // name="items.qty" row/>) is the property's, not each row's: pressed, it goes to the editor of the CURRENT row - as
    // a grid column's goes to its selected row - while that editor is shown: placed by the view, not waiting in the
    // park. Its edit is what the platform's key of a cell starts, the renderer's binding (onBinding): here the property
    // has no change to run, and only the renderer's mark of a pressed key shows
    public void testAKeyOfAPropertyDrawnPerRowGoesToTheCurrentRow() {
        RowsForm rows = new RowsForm(new GInputBindingEvent(new GKeyInputEvent(new GKeyStroke(113)), ANY)); // F2
        GFormController controller = rows.controller();
        FlowPanel hostOne = new FlowPanel(), hostTwo = new FlowPanel(); // the hosts the view renders in its rows
        RootPanel.get().add(hostOne); RootPanel.get().add(hostTwo);
        try {
            assertFalse(press(controller, hostOne.getElement(), 113)); // the editors wait in the park: none is shown
            controller.getRowsPanelController(rows.qty).place(rows.one, rows.qty, hostOne.getElement());
            controller.getRowsPanelController(rows.qty).place(rows.two, rows.qty, hostTwo.getElement());

            assertTrue(press(controller, hostTwo.getElement(), 113));
            assertTrue(pressed(hostOne.getElement())); // the current row's, wherever the key is pressed
            assertFalse(pressed(hostTwo.getElement()));

            controller.getGroupController(rows.group).changeCurrentKey(rows.two);
            unmark(hostOne.getElement());
            assertTrue(press(controller, hostOne.getElement(), 113));
            assertTrue(pressed(hostTwo.getElement()));
            assertFalse(pressed(hostOne.getElement()));
        } finally {
            RootPanel.get().remove(hostOne); RootPanel.get().remove(hostTwo);
        }
    }
    // ... and so does its mouse binding, which the form binds once as it binds the key - taken in a cell anywhere, as
    // the modes say (bindCell) -, where the renderers drawn in every row bound none
    public void testAMouseBindingOfAPropertyDrawnPerRowGoesToTheCurrentRow() {
        RowsForm rows = new RowsForm(new GInputBindingEvent(new GMouseInputEvent(GMouseInputEvent.DBLCLK), ANY));
        GFormController controller = rows.controller();
        FlowPanel hostOne = new FlowPanel(), hostTwo = new FlowPanel();
        RootPanel.get().add(hostOne); RootPanel.get().add(hostTwo);
        try {
            controller.getRowsPanelController(rows.qty).place(rows.one, rows.qty, hostOne.getElement());
            controller.getRowsPanelController(rows.qty).place(rows.two, rows.qty, hostTwo.getElement());

            assertTrue(doubleClick(controller, hostTwo.getElement()));
            assertTrue(pressed(hostOne.getElement())); // the current row's, wherever the cell double clicked is
            assertFalse(pressed(hostTwo.getElement()));
        } finally {
            RootPanel.get().remove(hostOne); RootPanel.get().remove(hostTwo);
        }
    }

    // a key the form sends for a property later - a design's changeKey given by a property - takes the slot the form
    // bound for it, the empty one where the design gives the property no key of its own: the form takes it from its
    // reader, whoever draws the property (GExtraPropReader.update)
    public void testAKeyTheFormSendsTakesTheEmptySlotOfAProperty() {
        RowsForm rows = new RowsForm(new GInputBindingEvent(null, ANY)); // a changeKey only, no key of its own
        rows.qty.changeKeyReader = new GExtraPropReader(rows.qty.ID, -1, GFormChanges.GPropertyReadType.CHANGEKEY);
        GFormController controller = rows.controller();
        FlowPanel host = new FlowPanel();
        RootPanel.get().add(host);
        try {
            controller.getRowsPanelController(rows.qty).place(rows.one, rows.qty, host.getElement());
            assertFalse(press(controller, host.getElement(), 117)); // F6, none yet
            GFormChanges changes = new GFormChanges();
            NativeHashMap<GGroupObjectValue, PValue> keys = new NativeHashMap<>();
            keys.put(GGroupObjectValue.EMPTY, new PValue.SerializableValue(new GInputBindingEvent(new GKeyInputEvent(new GKeyStroke(117)), ANY)));
            changes.properties.put(rows.qty.changeKeyReader, keys);
            controller.applyRemoteChanges(changes, 1);
            assertTrue(press(controller, host.getElement(), 117));
            assertTrue(pressed(host.getElement()));
        } finally {
            RootPanel.get().remove(host);
        }
    }
    // ... and a mouse binding a property gives itself, a design's changeMouse given by a property, the same way: taken
    // off by a NULL, it comes back with the next one - its slot stays a mouse one's
    public void testAMouseBindingAPropertyGivesItselfComesBackAfterANull() {
        RowsForm rows = new RowsForm(new GInputBindingEvent(new GMouseInputEvent(GMouseInputEvent.DBLCLK), ANY));
        rows.qty.changeMouseReader = new GExtraPropReader(rows.qty.ID, -1, GFormChanges.GPropertyReadType.CHANGEMOUSE);
        GFormController controller = rows.controller();
        FlowPanel host = new FlowPanel();
        RootPanel.get().add(host);
        try {
            controller.getRowsPanelController(rows.qty).place(rows.one, rows.qty, host.getElement());
            assertTrue(doubleClick(controller, host.getElement())); // the design's own
            changeMouse(controller, rows.qty, null);
            assertFalse(doubleClick(controller, host.getElement()));
            changeMouse(controller, rows.qty, new GInputBindingEvent(new GMouseInputEvent(GMouseInputEvent.DBLCLK), ANY));
            assertTrue(doubleClick(controller, host.getElement()));
        } finally {
            RootPanel.get().remove(host);
        }
    }
    // the changeMouse the server sends for a property, as its reader's value
    private static void changeMouse(GFormController controller, GPropertyDraw property, GInputBindingEvent binding) {
        GFormChanges changes = new GFormChanges();
        NativeHashMap<GGroupObjectValue, PValue> values = new NativeHashMap<>();
        values.put(GGroupObjectValue.EMPTY, binding != null ? new PValue.SerializableValue(binding) : null);
        changes.properties.put(property.changeMouseReader, values);
        controller.applyRemoteChanges(changes, 1);
    }
    // the modes the server gives a binding with none of its own
    private static final GBindingEnv ANY = new GBindingEnv(null, null, null, null, null, null, null, null, null);
    // a form of a grid its own React view draws, the platform's editor of qty in each of its two rows (<Platform
    // name="items.qty" row/>), qty with the binding given, the rows and their values as the server sends them
    private static final class RowsForm {
        final GForm form = leafGridForm();
        final GGroupObject group = form.groupObjects.get(0);
        final GPropertyDraw qty = column(form, 22, "qty", group);
        final GGroupObjectValue one = new GGroupObjectValue(10, 1), two = new GGroupObjectValue(10, 2);

        RowsForm(GInputBindingEvent binding) {
            qty.platformDrawn = true; // PROPERTY(qty(i)) { custom = NULL; }: the platform draws it in every row
            qty.cellType = GIntegerType.instance; // ... its editor of its base type, as the server gives it
            layout(qty); // ... and lays it out as the server says (ClientComponentToGwtConverter.convertPropertyDraw)
            qty.valueAlignmentHorz = GFlexAlignment.END; qty.valueAlignmentVert = GFlexAlignment.CENTER;
            qty.panelCommentAlignment = GFlexAlignment.CENTER;
            qty.valueOverflowHorz = "clip"; qty.valueOverflowVert = "clip";
            qty.bindingEvents.add(binding);
            GFormChangesDTO changes = GFormChangesTest.noChanges();
            changes.objectsGroupIds = new int[] {group.ID}; changes.objects = new GGroupObjectValue[] {one};
            changes.gridObjectsGroupIds = new int[] {group.ID}; changes.gridObjects = new ArrayList[] {new ArrayList<>(Arrays.asList(one, two))};
            changes.properties = new GPropertyReaderDTO[] {new GPropertyReaderDTO(qty.ID, DRAW, -1)};
            changes.propertiesValueKeys = new GGroupObjectValue[][] {{one, two}};
            changes.propertiesValueValues = new Serializable[][] {{1, 2}};
            form.initialFormChanges = changes;
        }
        GFormController controller() {
            return new GFormController(null, null, formContainer(), form, false, "leafGrid", 0, null);
        }
    }

    // of two bindings of one key and one priority, the later one cannot be pressed now - a key whose controller gives no
    // widget to take it with, the property not drawn -: the key goes to the earlier one, as to any other one the later
    // one leaves it to, and not to nobody
    public void testAKeyALaterBindingOfItsPriorityCannotTakeGoesToTheEarlierOne() {
        GFormController controller = new GFormController(null, null, formContainer(), leafGridForm(), false, "leafGrid", 0, null);
        ArrayList<String> pressed = new ArrayList<>();
        GBindingEnv env = new GBindingEnv(5, null, null, null, null, null, null, null, null); // one priority for both
        controller.addBinding(new GBindingEvent(new GKeyStroke(118)::isEvent, env), binding(true, () -> pressed.add("earlier"))); // F7
        controller.addBinding(new GBindingEvent(new GKeyStroke(118)::isEvent, env), binding(false, () -> pressed.add("later")));
        FlowPanel target = new FlowPanel();
        RootPanel.get().add(target);
        try {
            assertTrue(press(controller, target.getElement(), 118));
            assertEquals(Arrays.asList("earlier"), pressed);
        } finally {
            RootPanel.get().remove(target);
        }
    }
    private static GFormController.Binding binding(boolean enabled, Runnable pressed) {
        return new GFormController.Binding(null) {
            @Override
            public boolean showing() {
                return true;
            }
            @Override
            public boolean enabled() {
                return enabled;
            }
            @Override
            public void exec(Event event) {
                pressed.run();
            }
        };
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

    // a double click in a cell, as the cell hands it to the form (GFormController.checkMouseKeyEvent): its bindings
    // asked in the preview, then in the plain pass - whether one of them took it
    private static boolean doubleClick(GFormController controller, Element cell) {
        NativeEvent click = Document.get().createDblClickEvent(0, 0, 0, 0, 0, false, false, false, false);
        cell.dispatchEvent(click);
        EventHandler handler = new EventHandler(Event.as(click));
        controller.checkMouseEvent(handler, true, true, false, false);
        if (!handler.consumed)
            controller.checkMouseEvent(handler, false, true, false, false);
        return handler.consumed;
    }
    // a key pressed in an element of the page, as the form's listener takes it (GFormController.checkGlobalKeyEvent):
    // its bindings asked in the preview, then in the plain pass - whether one of them took it
    private static boolean press(GFormController controller, Element target, int keyCode) {
        NativeEvent key = Document.get().createKeyDownEvent(false, false, false, false, keyCode);
        target.dispatchEvent(key);
        EventHandler handler = new EventHandler(Event.as(key));
        controller.checkKeyEvent(handler, true, false, false);
        if (!handler.consumed)
            controller.checkKeyEvent(handler, false, false, false);
        return handler.consumed;
    }
    // ... and whether the editor in a host shows the mark of a pressed key (ActionOrPropertyPanelValue.onBinding), and
    // the mark taken off
    private static native boolean pressed(Element host) /*-{
        return host.querySelector('.panel-renderer-value-binding') != null;
    }-*/;
    private static native void unmark(Element host) /*-{
        var marked = host.querySelector('.panel-renderer-value-binding');
        if (marked)
            marked.classList.remove('panel-renderer-value-binding');
    }-*/;

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
