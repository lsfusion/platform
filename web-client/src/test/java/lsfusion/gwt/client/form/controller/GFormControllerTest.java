package lsfusion.gwt.client.form.controller;

import com.google.gwt.dom.client.Document;
import lsfusion.gwt.client.form.filter.user.controller.GPlatformFiltersController;
import lsfusion.gwt.client.view.MainFrame;
import com.google.gwt.core.client.JavaScriptObject;
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
import lsfusion.gwt.client.classes.data.GStringType;
import lsfusion.gwt.client.form.controller.dispatch.GFormActionDispatcher;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.event.GBindingEnv;
import lsfusion.gwt.client.form.event.GBindingEvent;
import lsfusion.gwt.client.form.event.GInputBindingEvent;
import lsfusion.gwt.client.form.event.GKeyInputEvent;
import lsfusion.gwt.client.form.event.GKeyStroke;
import lsfusion.gwt.client.form.event.GMouseInputEvent;
import lsfusion.gwt.client.form.filter.user.GCompare;
import lsfusion.gwt.client.form.filter.user.GDataFilterValue;
import lsfusion.gwt.client.form.filter.user.GFilter;
import lsfusion.gwt.client.form.filter.user.GPropertyFilter;
import lsfusion.gwt.client.form.filter.user.GPropertyFilterDTO;
import lsfusion.gwt.client.form.filter.user.GFilterControls;
import lsfusion.gwt.client.form.filter.user.view.GFilterConditionView;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.GObject;
import lsfusion.gwt.client.form.object.table.GToolbar;
import lsfusion.gwt.client.form.object.table.grid.GGrid;
import lsfusion.gwt.client.form.property.GClassViewType;
import lsfusion.gwt.client.form.property.GExtraPropReader;
import lsfusion.gwt.client.form.property.GExtInt;
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
import java.util.Collections;

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
        JsniTestSupport.installPageSetup();
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
    // ... and one where the design gives the property none of its own - a changeMouse given by a property only: the
    // server sends its placeholder with no event, of no kind, which the form binds as a key's - gets a slot of its own
    public void testAMouseBindingOnlyAPropertyGivesItselfIsBound() {
        RowsForm rows = new RowsForm(new GInputBindingEvent(null, ANY)); // the placeholder (PropertyDrawViewProxy.setChangeMouse)
        rows.qty.changeMouseReader = new GExtraPropReader(rows.qty.ID, -1, GFormChanges.GPropertyReadType.CHANGEMOUSE);
        GFormController controller = rows.controller();
        FlowPanel host = new FlowPanel();
        RootPanel.get().add(host);
        try {
            controller.getRowsPanelController(rows.qty).place(rows.one, rows.qty, host.getElement());
            assertFalse(doubleClick(controller, host.getElement())); // none yet
            changeMouse(controller, rows.qty, new GInputBindingEvent(new GMouseInputEvent(GMouseInputEvent.DBLCLK), ANY));
            assertTrue(doubleClick(controller, host.getElement()));
            assertTrue(pressed(host.getElement()));
            changeMouse(controller, rows.qty, null);
            assertFalse(doubleClick(controller, host.getElement())); // its slot, taken off by a NULL as any
        } finally {
            RootPanel.get().remove(host);
        }
    }
    // ... and of two such - one double click, no priority given -, the later in the form takes it, whichever the server
    // sends its changeMouse first: the placeholders take their slots as the form is made
    public void testOfTwoMouseBindingsPropertiesGiveThemselvesTheLaterInTheFormTakesIt() {
        GInputBindingEvent dblclk = new GInputBindingEvent(new GMouseInputEvent(GMouseInputEvent.DBLCLK), ANY);
        for (boolean laterSentFirst : new boolean[] {false, true}) {
            RowsForm rows = new RowsForm(new GInputBindingEvent(null, ANY));
            GPropertyDraw later = rows.perRow(column(rows.form, 23, "later", rows.group), new GInputBindingEvent(null, ANY));
            rows.qty.changeMouseReader = new GExtraPropReader(rows.qty.ID, -1, GFormChanges.GPropertyReadType.CHANGEMOUSE);
            later.changeMouseReader = new GExtraPropReader(later.ID, -1, GFormChanges.GPropertyReadType.CHANGEMOUSE);
            rows.deliver(rows.qty, later);
            GFormController controller = rows.controller();
            FlowPanel earlierHost = new FlowPanel(), laterHost = new FlowPanel();
            RootPanel.get().add(earlierHost); RootPanel.get().add(laterHost);
            try {
                controller.getRowsPanelController(rows.qty).place(rows.one, rows.qty, earlierHost.getElement());
                controller.getRowsPanelController(later).place(rows.one, later, laterHost.getElement());
                changeMouse(controller, laterSentFirst ? later : rows.qty, dblclk);
                changeMouse(controller, laterSentFirst ? rows.qty : later, dblclk);
                assertTrue(doubleClick(controller, earlierHost.getElement()));
                assertTrue(pressed(laterHost.getElement()));
                assertFalse(pressed(earlierHost.getElement()));
            } finally {
                RootPanel.get().remove(earlierHost); RootPanel.get().remove(laterHost);
            }
        }
    }
    // ... and a property giving itself both, a changeKey and a changeMouse, with no static one: two placeholders, and
    // each key takes the slot of its kind - neither takes the other's, in either order, and a NULL takes off its own
    public void testAKeyAndAMouseBindingAPropertyGivesItselfTakeTheirOwnSlots() {
        RowsForm rows = new RowsForm(new GInputBindingEvent(null, ANY)); // changeKey's placeholder
        rows.qty.bindingEvents.add(new GInputBindingEvent(null, ANY)); // ... and changeMouse's
        rows.qty.changeKeyReader = new GExtraPropReader(rows.qty.ID, -1, GFormChanges.GPropertyReadType.CHANGEKEY);
        rows.qty.changeMouseReader = new GExtraPropReader(rows.qty.ID, -1, GFormChanges.GPropertyReadType.CHANGEMOUSE);
        GFormController controller = rows.controller();
        FlowPanel host = new FlowPanel();
        RootPanel.get().add(host);
        try {
            controller.getRowsPanelController(rows.qty).place(rows.one, rows.qty, host.getElement());
            changeMouse(controller, rows.qty, new GInputBindingEvent(new GMouseInputEvent(GMouseInputEvent.DBLCLK), ANY));
            changeKey(controller, rows.qty, new GInputBindingEvent(new GKeyInputEvent(new GKeyStroke(117)), ANY)); // F6
            assertTrue(doubleClick(controller, host.getElement()));
            assertTrue(press(controller, host.getElement(), 117));
            changeKey(controller, rows.qty, null);
            assertFalse(press(controller, host.getElement(), 117));
            assertTrue(doubleClick(controller, host.getElement())); // the mouse one stays
            changeMouse(controller, rows.qty, null);
            assertFalse(doubleClick(controller, host.getElement()));
            changeKey(controller, rows.qty, new GInputBindingEvent(new GKeyInputEvent(new GKeyStroke(117)), ANY)); // the other order
            changeMouse(controller, rows.qty, new GInputBindingEvent(new GMouseInputEvent(GMouseInputEvent.DBLCLK), ANY));
            assertTrue(press(controller, host.getElement(), 117));
            assertTrue(doubleClick(controller, host.getElement()));
        } finally {
            RootPanel.get().remove(host);
        }
    }
    // the changeKey the server sends for a property, as its reader's value
    private static void changeKey(GFormController controller, GPropertyDraw property, GInputBindingEvent binding) {
        GFormChanges changes = new GFormChanges();
        NativeHashMap<GGroupObjectValue, PValue> values = new NativeHashMap<>();
        values.put(GGroupObjectValue.EMPTY, binding != null ? new PValue.SerializableValue(binding) : null);
        changes.properties.put(property.changeKeyReader, values);
        controller.applyRemoteChanges(changes, 1);
    }
    // the changeMouse the server sends for a property, as its reader's value
    private static void changeMouse(GFormController controller, GPropertyDraw property, GInputBindingEvent binding) {
        GFormChanges changes = new GFormChanges();
        NativeHashMap<GGroupObjectValue, PValue> values = new NativeHashMap<>();
        values.put(GGroupObjectValue.EMPTY, binding != null ? new PValue.SerializableValue(binding) : null);
        changes.properties.put(property.changeMouseReader, values);
        controller.applyRemoteChanges(changes, 1);
    }
    // of two keys of one priority - none given -, the later in the form takes it, whatever order the server sends the
    // properties in: a key's priority is its slot's, and the form binds every property's in the form's order
    // (initializeBindings), not as their renderers come
    public void testOfTwoKeysOfOnePriorityTheLaterInTheFormTakesIt() {
        GInputBindingEvent f2 = new GInputBindingEvent(new GKeyInputEvent(new GKeyStroke(113)), ANY);
        for (boolean laterSentFirst : new boolean[] {false, true}) {
            RowsForm rows = new RowsForm(f2);
            GPropertyDraw later = rows.perRow(column(rows.form, 23, "later", rows.group), f2);
            if (laterSentFirst)
                rows.deliver(later, rows.qty);
            else
                rows.deliver(rows.qty, later);
            GFormController controller = rows.controller();
            FlowPanel earlierHost = new FlowPanel(), laterHost = new FlowPanel();
            RootPanel.get().add(earlierHost); RootPanel.get().add(laterHost);
            try {
                controller.getRowsPanelController(rows.qty).place(rows.one, rows.qty, earlierHost.getElement());
                controller.getRowsPanelController(later).place(rows.one, later, laterHost.getElement());
                assertTrue(press(controller, earlierHost.getElement(), 113));
                assertTrue(pressed(laterHost.getElement()));
                assertFalse(pressed(earlierHost.getElement()));
            } finally {
                RootPanel.get().remove(earlierHost); RootPanel.get().remove(laterHost);
            }
        }
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
            perRow(qty, binding);
            deliver(qty);
        }
        // a property the platform draws in every row, with the binding given
        GPropertyDraw perRow(GPropertyDraw property, GInputBindingEvent binding) {
            property.platformDrawn = true; // PROPERTY(qty(i)) { custom = NULL; }: the platform draws it in every row
            property.cellType = GIntegerType.instance; // ... its editor of its base type, as the server gives it
            layout(property); // ... and lays it out as the server says (ClientComponentToGwtConverter.convertPropertyDraw)
            property.valueAlignmentHorz = GFlexAlignment.END; property.valueAlignmentVert = GFlexAlignment.CENTER;
            property.panelCommentAlignment = GFlexAlignment.CENTER;
            property.valueOverflowHorz = "clip"; property.valueOverflowVert = "clip";
            property.bindingEvents.add(binding);
            return property;
        }
        // the rows, and the values of these properties in this order, as the server sends them as the form opens
        void deliver(GPropertyDraw... properties) {
            GFormChangesDTO changes = GFormChangesTest.noChanges();
            changes.objectsGroupIds = new int[] {group.ID}; changes.objects = new GGroupObjectValue[] {one};
            changes.gridObjectsGroupIds = new int[] {group.ID}; changes.gridObjects = new ArrayList[] {new ArrayList<>(Arrays.asList(one, two))};
            changes.properties = new GPropertyReaderDTO[properties.length];
            changes.propertiesValueKeys = new GGroupObjectValue[properties.length][];
            changes.propertiesValueValues = new Serializable[properties.length][];
            for (int i = 0; i < properties.length; i++) {
                changes.properties[i] = new GPropertyReaderDTO(properties[i].ID, DRAW, -1);
                changes.propertiesValueKeys[i] = new GGroupObjectValue[] {one, two};
                changes.propertiesValueValues[i] = new Serializable[] {1, 2};
            }
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

    // the platform's filter panel - FILTERS(items) the platform's, its toolbar showing the filters - makes a fixed row
    // of each FILTER the design declares: of two FILTERs moved into a react container, the one given custom = NULL in a
    // view of its own, which the container places, and the one React draws placed nowhere - React draws it, stating it
    // through the panel (GReactFormData.FilterPart), the one writer of the group's list: applied at once, and no
    // row of the panel's own shows it again
    public void testAFilterReactDrawsOverThePlatformsPanelIsItsRow() {
        FiltersForm filters = new FiltersForm(true);
        GFormController controller = filters.controller();

        assertNull(controller.getFormLayout().getBaseComponentViews().get(filters.drawn));
        assertNotNull(controller.getFormLayout().getBaseComponentViews().get(filters.placed));
        filters.change(controller, "price", 5.0);
        assertTrue(filters.filters(controller, filters.price));
        assertNull(filters.panelCondition(controller));
    }

    // ... a value it states that is NULL - a two-state LOGICAL's false, which the view's value converts to - is a
    // condition comparing with NULL, as the panel takes one from a list the server reports, not dropped as a cleared
    // row's value is
    public void testAFilterReactDrawsOverThePlatformsPanelStatesANullValue() {
        FiltersForm filters = new FiltersForm(true);
        GFormController controller = filters.controller();
        GPlatformFiltersController panel = (GPlatformFiltersController) userFiltersController(controller, filters.group);

        panel.changeFilter(filters.drawn, filters.condition(null));
        assertTrue(filters.filters(controller, filters.price));
    }

    // ... and the panel's reset takes off the NULL a fixed row has - one React states, and one of the platform's row a list
    // the server reports has -, as a row's own clearing does: no apply after it has them again
    public void testThePlatformsPanelsResetTakesOffTheNullsOfItsFixedRows() {
        FiltersForm filters = new FiltersForm(true);
        GFormController controller = filters.controller();
        GPlatformFiltersController panel = (GPlatformFiltersController) userFiltersController(controller, filters.group);

        filters.report(controller, new GPropertyFilter(new GFilter(filters.quantity), filters.group, GGroupObjectValue.EMPTY, null, false, GCompare.EQUALS, true));
        panel.changeFilter(filters.drawn, filters.condition(null));
        assertTrue(filters.filters(controller, filters.quantity));
        panel.resetConditions();
        panel.applyFilters();
        assertFalse(filters.filters(controller, filters.price));
        assertFalse(filters.filters(controller, filters.quantity));
    }

    // ... and from a list the server reports each fixed row - a FILTER's, the one React draws too - takes the first
    // condition on its column, as a FILTER shows its condition where a view draws the conditions
    // (GPropertyFilter.getFirst): a second on the column is a row of the panel's own, the rows of the rest in their order
    public void testAFixedRowTakesTheFirstConditionOnItsColumn() {
        FiltersForm filters = new FiltersForm(true);
        GFormController controller = filters.controller();
        GPlatformFiltersController panel = (GPlatformFiltersController) userFiltersController(controller, filters.group);

        filters.report(controller, new GPropertyFilter(new GFilter(filters.price), filters.group, GGroupObjectValue.EMPTY, PValue.getPValue(1), GCompare.GREATER),
                new GPropertyFilter(new GFilter(filters.price), filters.group, GGroupObjectValue.EMPTY, PValue.getPValue(5), GCompare.LESS),
                new GPropertyFilter(new GFilter(filters.quantity), filters.group, GGroupObjectValue.EMPTY, PValue.getPValue(2), GCompare.EQUALS));
        assertEquals(GCompare.GREATER, panel.getFilterCondition(filters.drawn).compare);
        assertEquals(GCompare.EQUALS, panel.getFilterCondition(filters.placed).compare);
        assertNotNull(filters.panelCondition(controller)); // price < 5, a row of its own
        filters.report(controller, new GPropertyFilter(new GFilter(filters.price), filters.group, GGroupObjectValue.EMPTY, PValue.getPValue(5), GCompare.LESS));
        assertEquals(GCompare.LESS, panel.getFilterCondition(filters.drawn).compare);
        assertNull(panel.getFilterCondition(filters.placed));
        assertNull(filters.panelCondition(controller));
    }

    // ... its FILTERS box removed from the design too, where the panel shows none of the conditions
    public void testAFilterReactDrawsOverAPanelWithNoFiltersBoxIsItsRow() {
        FiltersForm filters = new FiltersForm(false);
        GFormController controller = filters.controller();

        filters.change(controller, "price", 5.0);
        assertTrue(filters.filters(controller, filters.price));
    }

    // ... where its fixed rows take their conditions from a list the server reports all the same - the one React draws,
    // a view showing it, and the platform's placed one -, while the panel takes none of the rest, having nowhere to
    // show them: each the first on its property, or none
    public void testAPanelWithNoFiltersBoxsFixedRowsTakeTheServersList() {
        FiltersForm filters = new FiltersForm(false);
        GFormController controller = filters.controller();
        GPlatformFiltersController panel = (GPlatformFiltersController) userFiltersController(controller, filters.group);

        filters.report(controller, new GPropertyFilter(new GFilter(filters.price), filters.group, GGroupObjectValue.EMPTY, PValue.getPValue(5), GCompare.GREATER),
                new GPropertyFilter(new GFilter(filters.quantity), filters.group, GGroupObjectValue.EMPTY, PValue.getPValue(2), GCompare.EQUALS));
        assertEquals(GCompare.GREATER, panel.getFilterCondition(filters.drawn).compare);
        assertEquals(GCompare.EQUALS, panel.getFilterCondition(filters.placed).compare);
        filters.report(controller);
        assertNull(panel.getFilterCondition(filters.drawn));
        assertNull(panel.getFilterCondition(filters.placed));
    }

    // ... and a condition of the panel's list all the same: a row of the platform's before it is not the last one, the
    // junction between them a real one (the design declaring the platform's filter first)
    public void testAPlatformRowBeforeAFilterReactDrawsIsNotTheLast() {
        FiltersForm filters = new FiltersForm(true);
        Collections.reverse(filters.group.grid.filters);
        GFormController controller = filters.controller();
        GPlatformFiltersController panel = (GPlatformFiltersController) userFiltersController(controller, filters.group);

        assertFalse(isLastRow(panel, filters.placed));
        assertTrue(isLastRow(panel, filters.drawn));
    }

    // ... and in the panel's manual apply mode - its controls shown - a change waits for the panel's apply, as its own
    // rows' do: the row holds it, the form's list stays as it was, the apply sends it
    public void testAFilterReactDrawsWaitsForThePanelsApplyInManualMode() {
        boolean manual = MainFrame.userFiltersManualApplyMode;
        MainFrame.userFiltersManualApplyMode = true;
        try {
            FiltersForm filters = new FiltersForm(true);
            GFormController controller = filters.controller();
            GPlatformFiltersController panel = (GPlatformFiltersController) userFiltersController(controller, filters.group);
            panel.setControlsVisible(true);

            filters.change(controller, "price", 5.0);
            assertFalse(panel.getFilterCondition(filters.drawn).nullValue());
            assertFalse(filters.filters(controller, filters.price));
            panel.applyFilters();
            assertTrue(filters.filters(controller, filters.price));
        } finally {
            MainFrame.userFiltersManualApplyMode = manual;
        }
    }

    // ... and its junction (`or`), which its part states as its comparison and negation, is the junction of the panel's
    // row
    public void testAFilterReactDrawsOverThePlatformsPanelStatesItsJunction() {
        FiltersForm filters = new FiltersForm(true);
        GFormController controller = filters.controller();

        filters.change(controller, "price", 5.0, "{\"or\": true}");
        assertFalse(filters.first(controller, filters.price).junction);
        filters.change(controller, "price", 6.0, "{}");
        assertFalse(filters.first(controller, filters.price).junction); // kept
    }

    // F3 over the platform's panel on a column whose first row is of a FILTER React draws: that row takes the cell's
    // value, as the first row of a column does - a FILTER being a user filter given in advance -, its comparison
    // kept, applied as the panel applies a row - at once -, with no row of the panel's own added; React draws it, so
    // there is no editor to start. Where the panel applies by hand - F3 showing its controls, hidden before -, it waits
    // for the apply, the FILTER's part shown it
    public void testF3OverThePanelPutsTheCellsValueIntoTheRowOfAFilterReactDraws() {
        boolean manual = MainFrame.userFiltersManualApplyMode;
        try {
            for (boolean byHand : new boolean[]{false, true}) {
                MainFrame.userFiltersManualApplyMode = byHand;
                FiltersForm filters = new FiltersForm(true);
                filters.deliver(true);
                GFormController controller = filters.controller();
                GPlatformFiltersController panel = (GPlatformFiltersController) userFiltersController(controller, filters.group);
                filters.change(controller, "price", 3.0, "{\"compare\": \">\"}"); // the controls hidden: applied at once
                assertValue(3, filters.first(controller, filters.price).value.value);

                panel.addCondition(null, false, true); // F3, on the current row - its price, the first column -,
                panel.setControlsVisible(true);        // ... the controls shown then, as the table's F3 does
                assertNull(filters.panelCondition(controller));
                assertEquals(5.0, filterValue(filters.view(controller), "price"), 0);
                assertEquals(GCompare.GREATER, panel.getFilterCondition(filters.drawn).compare);
                assertValue(byHand ? 3 : 5, filters.first(controller, filters.price).value.value);
                panel.applyFilters();
                assertValue(5, filters.first(controller, filters.price).value.value);
                assertEquals(GCompare.GREATER, filters.first(controller, filters.price).compare);
            }
        } finally {
            MainFrame.userFiltersManualApplyMode = manual;
        }
    }

    // ... and where no row is current - an empty grid -, F3 has no value to put into it, and no editor to type one in:
    // the FILTER's condition stays
    public void testF3OverThePanelOnNoRowLeavesTheRowOfAFilterReactDraws() {
        FiltersForm filters = new FiltersForm(true);
        filters.deliver(false);
        GFormController controller = filters.controller();
        GPlatformFiltersController panel = (GPlatformFiltersController) userFiltersController(controller, filters.group);
        filters.change(controller, "price", 3.0, "{\"compare\": \">\"}");

        panel.addCondition(null, false, true); // F3
        assertValue(3, filters.first(controller, filters.price).value.value);
        assertEquals(GCompare.GREATER, filters.first(controller, filters.price).compare);
    }

    // the FILTER of a property of a group in PANEL view - which has no table and shows no user filters - has a
    // controller all the same, none (GReactFormData.createFilterController): the form tells a group's FILTERs of its
    // user filters wherever it tells what shows them, a list the server reports for the group as one the form sends
    public void testAPanelGroupsFilterIsToldOfItsUserFilters() {
        GForm form = form("panelFilter");
        GGroupObject group = new GGroupObject();
        group.ID = 10; group.nativeSID = "g10"; group.sID = "item"; group.viewType = GClassViewType.PANEL;
        group.objects.add(new GObject(group, "item", 10, "item", GIntegerType.instance));
        form.groupObjects.add(group);
        GGrid grid = new GGrid(); // its grid is in the design all the same, its FILTERS box and its FILTERs in it
        grid.ID = 11; grid.nativeSID = "c11"; grid.sID = "GRID(item)"; grid.groupObject = group;
        grid.filtersContainer = container(14, "FILTERS(item)", null);
        group.grid = grid;
        GPropertyDraw name = column(form, 20, "name", group);
        name.isList = false;
        filterIn(21, name, grid.filtersContainer, grid);
        form.initialFormChanges = GFormChangesTest.noChanges();
        GFormController controller = new GFormController(null, null, formContainer(), form, false, "panelFilter", 0, null);

        GFormChanges changes = new GFormChanges();
        ArrayList<GPropertyFilterDTO> filters = new ArrayList<>();
        filters.add(new GPropertyFilter(new GFilter(name), group, GGroupObjectValue.EMPTY, PValue.getPValue(5), GCompare.EQUALS).getFilterDTO());
        changes.userFilters.put(group, filters);
        changes.needConfirm = true; // what the form takes after the user filters: the rest of the delta
        controller.applyRemoteChanges(changes, 1);
        assertTrue(controller.needConfirm());
        controller.changeFilters(Collections.singletonList(group), new ArrayList<>()); // ... and a list the form sends
    }

    // a fixed row of the platform's panel takes a condition from a list the server reports with its comparison shown:
    // in the label it shows with its controls hidden, and as the comparison its value is edited by, as when the user
    // chooses it in the row - the label and the editor kept the comparison the row had before
    public void testAFixedRowShowsTheComparisonOfAConditionItTakes() {
        FiltersForm filters = new FiltersForm(true);
        GFormController controller = filters.controller();
        GPlatformFiltersController panel = (GPlatformFiltersController) userFiltersController(controller, filters.group);

        filters.report(controller, new GPropertyFilter(new GFilter(filters.quantity), filters.group, GGroupObjectValue.EMPTY, PValue.getPValue(2), GCompare.GREATER));
        GFilterConditionView row = panelRow(panel, filters.placed);
        assertEquals(">", compareLabel(row).getText());
        assertEquals(GCompare.GREATER, editorCompare(row));
    }

    // a fixed row of the panel takes a negated condition from a list the server reports with its negation shown in the
    // comparison's selector, which its controls show, too - the selector kept the negation it had
    public void testAFixedRowShowsTheNegationOfAConditionItTakes() {
        FiltersForm filters = new FiltersForm(true);
        GFormController controller = filters.controller();
        GPlatformFiltersController panel = (GPlatformFiltersController) userFiltersController(controller, filters.group);

        filters.report(controller, new GPropertyFilter(new GFilter(filters.quantity), filters.group, GGroupObjectValue.EMPTY, PValue.getPValue(2), true, GCompare.GREATER, true));
        panel.setControlsVisible(true);
        assertEquals("!>", compareSelector(panelRow(panel, filters.placed)).getElement().getInnerText());
    }

    // a fixed row of the panel - a FILTER's -, its controls shown, keeps its property, the design's: its caption shown
    // in place of the choice of another, which it offered
    public void testAFixedRowKeepsItsPropertyWithItsControlsShown() {
        FiltersForm filters = new FiltersForm(true);
        GFormController controller = filters.controller();
        GPlatformFiltersController panel = (GPlatformFiltersController) userFiltersController(controller, filters.group);
        panel.setControlsVisible(true);
        GFilterConditionView row = panelRow(panel, filters.placed);
        assertTrue(compareSelector(row).isVisible());
        assertFalse(propertySelector(row).isVisible());
    }

    // a row of the platform's panel takes Enter in its value cell alone, its view binding it: in another panel cell of
    // its group - a panel property's - the key is that cell's, which the panel took, applying its conditions
    public void testAPanelRowTakesEnterInItsValueCellAlone() {
        FiltersForm filters = new FiltersForm(true);
        GFormController controller = filters.controller();
        GPlatformFiltersController panel = (GPlatformFiltersController) userFiltersController(controller, filters.group);
        assertEnterInItsValueCellAlone(filters, controller, panelRow(panel, filters.placed));
    }
    // a row given a value, Enter pressed in another panel cell of its group, then in the row's value cell: the row's
    // condition stated by the second alone
    private static void assertEnterInItsValueCellAlone(FiltersForm filters, GFormController controller, GFilterConditionView view) {
        Label cell = new Label(); // another panel cell of the group
        GFormController.setBindingGroupObject(cell, filters.group);
        RootPanel.get().add(view); // on the page: a binding holds while its widget is shown
        RootPanel.get().add(cell);
        try {
            view.setValue(new GDataFilterValue(PValue.getPValue(3)));
            assertFalse(enter(controller, cell.getElement()));
            assertFalse(filters.filters(controller, filters.quantity));
            assertTrue(enter(controller, valueCell(view).getElement()));
            assertTrue(filters.filters(controller, filters.quantity));
        } finally {
            RootPanel.get().remove(cell);
            RootPanel.get().remove(view);
        }
    }

    // ... and in the value cell a property changed in the row builds anew: the cell it replaced took its Enter away,
    // and the new one was given none
    public void testAPanelRowTakesEnterInItsRebuiltValueCell() {
        FiltersForm filters = new FiltersForm(true);
        GFormController controller = filters.controller();
        GPlatformFiltersController panel = (GPlatformFiltersController) userFiltersController(controller, filters.group);
        GFilterConditionView view = panelRow(panel, filters.placed);
        rebuildValueCell(view);
        assertEnterInItsValueCellAlone(filters, controller, view);
    }

    // a fixed row of the panel in a container that aligns captions - its FILTER standing outside the FILTERS box -
    // is hidden with its caption, a widget apart from it (GFilterConditionView.initView): its SHOWIF hid the row alone
    public void testAFixedRowsAlignedCaptionIsHiddenWithIt() {
        FiltersForm filters = new FiltersForm(true);
        filters.filterAligned();
        GFormController controller = filters.controller();
        GPlatformFiltersController panel = (GPlatformFiltersController) userFiltersController(controller, filters.group);
        GFilterConditionView view = panelRow(panel, filters.outside);
        Widget caption = rowCaption(view);
        assertFalse(view.getElement().isOrHasChild(caption.getElement()));

        GFormChanges changes = new GFormChanges();
        NativeHashMap<GGroupObjectValue, PValue> hidden = new NativeHashMap<>();
        hidden.put(GGroupObjectValue.EMPTY, PValue.getPValue(true));
        changes.properties.put(filters.outside.showIfReader, hidden);
        controller.applyRemoteChanges(changes, 1);
        assertFalse(view.isVisible());
        assertFalse(caption.isVisible());
    }

    // a form of a group whose rows React draws, its filters the platform's, with the FILTERs of two of its properties
    // moved into a react container - one React draws, MOVE FILTER(price), and one the platform's panel does,
    // MOVE FILTER(quantity) { custom = NULL; } -, FILTERS(items) in the form or REMOVEd - and one of a string property
    // standing outside React (filterAligned)
    private static final class FiltersForm {
        final GForm form = new GForm();
        final GGroupObject group = new GGroupObject();
        final GPropertyDraw price, quantity;
        final GContainer board;
        final GFilter drawn, placed;
        GPropertyDraw name; // a string property, its FILTER standing outside React - only in filterAligned
        GFilter outside;

        FiltersForm(boolean filtersBox) {
            form.sID = "propertyFilters";
            form.mainContainer = new GContainer();
            form.mainContainer.ID = 1; form.mainContainer.nativeSID = "c1"; form.mainContainer.sID = "main";
            form.mainContainer.main = true;
            layout(form.mainContainer);
            form.mainContainer.lines = 1; form.mainContainer.childrenAlignment = GFlexAlignment.START;

            group.ID = 10; group.nativeSID = "g10"; group.sID = "items"; group.viewType = GClassViewType.LIST;
            group.objects.add(new GObject(group, "item", 10, "item", GIntegerType.instance));
            form.groupObjects.add(group);
            GGrid grid = new GGrid();
            grid.ID = 11; grid.nativeSID = "c11"; grid.sID = "GRID(items)"; grid.groupObject = group;
            grid.custom = "RowsGrid"; grid.react = true; // React draws the rows, the filters stay the platform's
            grid.width = -2; grid.height = -2;
            grid.lineWidth = -1; grid.lineHeight = -1;
            grid.container = form.mainContainer; form.mainContainer.children.add(grid);
            layout(grid);
            grid.toolbar = new GToolbar(); // hidden, its filters shown: the platform builds its filter panel
            grid.toolbar.ID = 12; grid.toolbar.sID = "TOOLBARSYSTEM(items)"; grid.toolbar.visible = false;
            grid.toolbar.showFilters = true;
            grid.filterControls = new GFilterControls();
            grid.filterControls.ID = 13; grid.filterControls.sID = "FILTERCONTROLS(items)";
            grid.filtersContainer = container(14, "FILTERS(items)", filtersBox ? form.mainContainer : null);
            group.grid = grid;
            price = column(form, 20, "price", group);
            quantity = column(form, 21, "quantity", group);

            board = container(15, "board", form.mainContainer);
            board.custom = "FiltersBoard"; board.react = true;
            drawn = filterIn(22, price, board, grid);
            placed = filterIn(23, quantity, board, grid);
            placed.platformDrawn = true;
            form.initialFormChanges = GFormChangesTest.noChanges();
        }
        GFormController controller() {
            return new GFormController(null, null, formContainer(), form, false, "propertyFilters", 0, null);
        }
        // as the view states a FILTER it draws: through its member, React drawing nothing here - with its options, as
        // JSON, too
        void change(GFormController controller, String property, Object value) {
            changeFilter(reactController(view(controller)), property, value);
        }
        void change(GFormController controller, String property, Object value, String options) {
            changeFilter(reactController(view(controller)), property, value, options);
        }
        // the board's view, which draws the FILTERs
        Object view(GFormController controller) {
            return controller.getFormLayout().getContainerView(board);
        }
        // the rows, the first current, and their prices, as the server sends them as the form opens - or none, the
        // price column there all the same
        void deliver(boolean rows) {
            GGroupObjectValue one = new GGroupObjectValue(group.ID, 1), two = new GGroupObjectValue(group.ID, 2);
            GFormChangesDTO changes = GFormChangesTest.noChanges();
            if (rows) {
                changes.objectsGroupIds = new int[] {group.ID}; changes.objects = new GGroupObjectValue[] {one};
            }
            changes.gridObjectsGroupIds = new int[] {group.ID};
            changes.gridObjects = new ArrayList[] {rows ? new ArrayList<>(Arrays.asList(one, two)) : new ArrayList<>()};
            changes.properties = new GPropertyReaderDTO[] {new GPropertyReaderDTO(price.ID, DRAW, -1)};
            changes.propertiesValueKeys = new GGroupObjectValue[][] {rows ? new GGroupObjectValue[] {one, two} : new GGroupObjectValue[0]};
            changes.propertiesValueValues = new Serializable[][] {rows ? new Serializable[] {5, 7} : new Serializable[0]};
            form.initialFormChanges = changes;
        }
        // the first condition on a property in the form's list, as it sent it last
        GPropertyFilter first(GFormController controller, GPropertyDraw property) {
            for (GPropertyFilter condition : controller.getFilters(group))
                if (condition.column.property == property)
                    return condition;
            return null;
        }
        // a FILTER of a string property standing outside React, in a container that aligns captions
        void filterAligned() {
            name = column(form, 24, "name", group);
            name.valueType = new GStringType(GExtInt.UNLIMITED, false, false);
            GContainer search = container(26, "search", form.mainContainer);
            search.alignCaptions = true;
            outside = filterIn(25, name, search, group.grid);
        }
        // a condition on price, compared the default way, as a FILTER's member states one
        GPropertyFilter condition(PValue value) {
            return new GPropertyFilter(new GFilter(price), group, GGroupObjectValue.EMPTY, value, false, price.getDefaultCompare(), true);
        }
        // the server reporting the group's list
        void report(GFormController controller, GPropertyFilter... conditions) {
            GFormChanges changes = new GFormChanges();
            ArrayList<GPropertyFilterDTO> filters = new ArrayList<>();
            for (GPropertyFilter condition : conditions)
                filters.add(condition.getFilterDTO());
            changes.userFilters.put(group, filters);
            controller.applyRemoteChanges(changes, 1);
        }
        // whether the form's list - as it sent it last - has a condition on the property
        boolean filters(GFormController controller, GPropertyDraw property) {
            return first(controller, property) != null;
        }
        // the condition on price the panel holds, in a view of its own - not the design's FILTER React draws -, if any
        GComponent panelCondition(GFormController controller) {
            for (GComponent component : controller.getFormLayout().getBaseComponentViews().keySet())
                if (component instanceof GFilter && component != drawn && ((GFilter) component).property == price)
                    return component;
            return null;
        }
    }

    // a value is the one stated, said as a number where it is not: a value's toString is not to be called
    private static void assertValue(int expected, PValue value) {
        assertTrue("the value is not " + expected, PValue.getPValue(expected).equals(value));
    }

    // the controller a React container's view is given, and what shows a group's user filters, read where the client
    // keeps them - neither is anything else's to ask for
    private static native JavaScriptObject reactController(Object view) /*-{
        return view.@lsfusion.gwt.client.form.design.view.ReactContainerView::state.@lsfusion.gwt.client.form.design.view.GReactFormData.ViewState::controller;
    }-*/;
    private static native Object userFiltersController(GFormController controller, GGroupObject group) /*-{
        return controller.@lsfusion.gwt.client.form.controller.GFormController::userFiltersControllers.@lsfusion.gwt.client.base.jsni.NativeSIDMap::get(*)(group);
    }-*/;
    // whether the panel's row of a FILTER is its last one, its junction hidden - read where the panel keeps its rows
    private static native boolean isLastRow(GPlatformFiltersController panel, GFilter filter) /*-{
        var view = @lsfusion.gwt.client.form.controller.GFormControllerTest::panelRow(*)(panel, filter);
        return view.@lsfusion.gwt.client.form.filter.user.view.GFilterConditionView::isLast;
    }-*/;
    private static native void changeFilter(JavaScriptObject controller, String property, Object value) /*-{
        controller.items.filter[property].change(value);
    }-*/;
    private static native void changeFilter(JavaScriptObject controller, String property, Object value, String options) /*-{
        controller.items.filter[property].change(value, JSON.parse(options));
    }-*/;
    // the value a FILTER React draws shows, as its view is given it now
    private static native double filterValue(Object view, String property) /*-{
        var state = view.@lsfusion.gwt.client.form.design.view.ReactContainerView::state;
        return state.@lsfusion.gwt.client.form.design.view.GReactFormData.ViewState::current()().items.filter[property].value;
    }-*/;
    // the panel's row of a FILTER, its caption, its comparison label, its value cell and the comparison its value is
    // edited by, read where the client keeps them
    private static native GFilterConditionView panelRow(GPlatformFiltersController panel, GFilter filter) /*-{
        var row = panel.@lsfusion.gwt.client.form.filter.user.controller.GPlatformFiltersController::getFilterRow(*)(filter);
        return panel.@lsfusion.gwt.client.form.filter.user.controller.GPlatformFiltersController::conditionViews.@java.util.Map::get(Ljava/lang/Object;)(row);
    }-*/;
    private static native Widget rowCaption(GFilterConditionView row) /*-{
        return row.@lsfusion.gwt.client.form.filter.user.view.GFilterConditionView::leftPanel;
    }-*/;
    private static native Label compareLabel(GFilterConditionView row) /*-{
        return row.@lsfusion.gwt.client.form.filter.user.view.GFilterConditionView::compareLabel;
    }-*/;
    private static native Widget valueCell(GFilterConditionView row) /*-{
        return row.@lsfusion.gwt.client.form.filter.user.view.GFilterConditionView::valueView.@lsfusion.gwt.client.form.filter.user.view.GDataFilterValueView::cell;
    }-*/;
    private static native GCompare editorCompare(GFilterConditionView row) /*-{
        var cell = @lsfusion.gwt.client.form.controller.GFormControllerTest::valueCell(*)(row);
        return cell.@lsfusion.gwt.client.form.filter.user.view.GDataFilterPropertyValue::inputList.@lsfusion.gwt.client.form.property.async.GInputList::compare;
    }-*/;
    // a filter row's choices - of its property, of its comparison -, which its controls show
    private static native Widget propertySelector(GFilterConditionView row) /*-{
        return row.@lsfusion.gwt.client.form.filter.user.view.GFilterConditionView::propertyView;
    }-*/;
    private static native Widget compareSelector(GFilterConditionView row) /*-{
        return row.@lsfusion.gwt.client.form.filter.user.view.GFilterConditionView::compareView;
    }-*/;
    // the row's value cell built anew, as a property chosen in the row builds it (GFilterConditionView.propertyChanged)
    private static native void rebuildValueCell(GFilterConditionView row) /*-{
        var condition = row.@lsfusion.gwt.client.form.filter.user.view.GFilterConditionView::condition;
        row.@lsfusion.gwt.client.form.filter.user.view.GFilterConditionView::valueView.@lsfusion.gwt.client.form.filter.user.view.GDataFilterValueView::changeProperty(*)(condition, false);
    }-*/;
    // Enter pressed in an element, as a panel cell gives a key to the form's bindings before it handles the key itself
    // (GFormController.onPropertyBrowserEvent): whether a binding took it
    private static native boolean enter(GFormController controller, Element target) /*-{
        var event = {type: 'keydown', keyCode: 13, shiftKey: false, altKey: false, ctrlKey: false, metaKey: false,
            target: target, stopPropagation: function () { }, preventDefault: function () { }};
        var handler = @lsfusion.gwt.client.base.view.EventHandler::new(Lcom/google/gwt/user/client/Event;)(event);
        controller.@lsfusion.gwt.client.form.controller.GFormController::checkKeyEvent(*)(handler, true, true, true);
        return handler.@lsfusion.gwt.client.base.view.EventHandler::consumed;
    }-*/;

    // a container of a hand-built design, laid out as the server gives it, in its parent
    private static GContainer container(int id, String sID, GContainer parent) {
        GContainer container = new GContainer();
        container.ID = id; container.nativeSID = "c" + id; container.sID = sID;
        layout(container);
        container.lines = 1; container.childrenAlignment = GFlexAlignment.START;
        container.container = parent; // none: REMOVEd from the design
        if (parent != null)
            parent.children.add(container);
        return container;
    }
    // ... and a property's FILTER of a table, FILTER(<property>), in it
    private static GFilter filterIn(int id, GPropertyDraw property, GContainer parent, GGrid grid) {
        GFilter filter = new GFilter(property);
        filter.ID = id; filter.nativeSID = "c" + id; filter.sID = "FILTER(" + property.sID + ")";
        layout(filter);
        filter.container = parent; parent.children.add(filter);
        grid.filters.add(filter);
        return filter;
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
        property.valueAlignmentHorz = GFlexAlignment.END; property.valueAlignmentVert = GFlexAlignment.CENTER;
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
