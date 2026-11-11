package lsfusion.gwt.client.form.controller;

import com.google.gwt.dom.client.Document;
import lsfusion.gwt.client.form.filter.user.controller.GFilterInputController;
import lsfusion.gwt.client.base.jsni.NativeSIDMap;
import lsfusion.gwt.client.form.filter.user.controller.GPlatformFilterController;
import lsfusion.gwt.client.form.filter.user.controller.GPlatformFilterInputController;
import lsfusion.gwt.client.view.MainFrame;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.UnsafeNativeLong;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Node;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.GForm;
import lsfusion.gwt.client.GFormChanges;
import lsfusion.gwt.client.GFormChangesDTO;
import lsfusion.gwt.client.GFormChangesTest;
import lsfusion.gwt.client.RemoteDispatchAsync;
import lsfusion.gwt.client.action.GRequestUserInputAction;
import lsfusion.gwt.client.action.GProcessFormChangesAction;
import lsfusion.gwt.client.base.jsni.JsniTestSupport;
import lsfusion.gwt.client.base.GAsync;
import lsfusion.gwt.client.form.property.panel.view.ActionOrPropertyValueController;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.result.ListResult;
import lsfusion.gwt.client.base.result.NumberResult;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.base.FocusUtils;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.base.view.GFlexAlignment;
import lsfusion.gwt.client.base.view.SizedWidget;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.classes.GActionType;
import lsfusion.gwt.client.classes.GInputType;
import lsfusion.gwt.client.classes.data.GIntegerType;
import lsfusion.gwt.client.classes.data.GStringType;
import lsfusion.gwt.client.action.GAction;
import lsfusion.gwt.client.controller.remote.action.RequestCountingAsyncCallback;
import lsfusion.gwt.client.controller.remote.action.form.ContinueInvocation;
import lsfusion.gwt.client.controller.remote.action.form.ControllerExecAction;
import lsfusion.gwt.client.controller.remote.action.form.CountRecords;
import lsfusion.gwt.client.controller.remote.action.form.CalculateSum;
import lsfusion.gwt.client.controller.remote.action.form.GroupReport;
import lsfusion.gwt.client.controller.remote.action.form.GroupReportResult;
import lsfusion.gwt.client.controller.remote.action.form.ChangeMode;
import lsfusion.gwt.client.controller.remote.action.form.ChangeGroupObject;
import lsfusion.gwt.client.controller.remote.action.form.ChangePropertyActive;
import lsfusion.gwt.client.controller.remote.action.form.ExpandGroupObject;
import lsfusion.gwt.client.controller.remote.action.form.ExpandGroupObjectRecursive;
import lsfusion.gwt.client.controller.remote.action.form.CollapseGroupObject;
import lsfusion.gwt.client.controller.remote.action.form.CollapseGroupObjectRecursive;
import lsfusion.gwt.client.controller.remote.action.form.ExecuteBatch;
import lsfusion.gwt.client.controller.remote.action.form.ScrollToEnd;
import lsfusion.gwt.client.controller.remote.action.form.SelectAll;
import lsfusion.gwt.client.controller.remote.action.form.SetViewFilters;
import lsfusion.gwt.client.controller.remote.action.form.FormRequestCountingAction;
import lsfusion.gwt.client.controller.remote.action.form.ExecuteEventAction;
import lsfusion.gwt.client.controller.remote.action.form.SetUserFilters;
import lsfusion.gwt.client.controller.remote.action.form.SetPropertyOrders;
import lsfusion.gwt.client.controller.remote.action.form.FormAction;
import lsfusion.gwt.client.controller.remote.action.form.FormRequestAction;
import lsfusion.gwt.client.controller.remote.action.form.GetAsyncValues;
import lsfusion.gwt.client.controller.remote.action.form.GetPriorityAsyncValues;
import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.client.controller.remote.action.navigator.ExecuteNavigatorAction;
import lsfusion.gwt.client.controller.remote.action.navigator.NavigatorRequestAction;
import lsfusion.gwt.client.controller.remote.action.logics.GenerateID;
import lsfusion.gwt.client.controller.remote.action.logics.GenerateIDResult;
import lsfusion.gwt.client.controller.dispatch.LogicsDispatchAsync;
import lsfusion.gwt.client.form.controller.dispatch.FormDispatchAsync;
import lsfusion.gwt.client.form.controller.dispatch.GFormActionDispatcher;
import lsfusion.gwt.client.controller.remote.DeferredRunner;
import lsfusion.gwt.client.form.GUpdateMode;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.design.view.GReactFormData;
import lsfusion.gwt.client.form.design.view.CaptionWidget;
import lsfusion.gwt.client.form.design.view.ComponentViewWidget;
import lsfusion.gwt.client.form.event.GBindingEnv;
import lsfusion.gwt.client.form.event.GBindingEvent;
import lsfusion.gwt.client.form.event.GChangeSelection;
import lsfusion.gwt.client.form.event.GInputBindingEvent;
import lsfusion.gwt.client.form.event.GKeyInputEvent;
import lsfusion.gwt.client.form.event.GKeyStroke;
import lsfusion.gwt.client.form.event.GMouseInputEvent;
import lsfusion.gwt.client.form.filter.user.GCompare;
import lsfusion.gwt.client.form.filter.user.GFilter;
import lsfusion.gwt.client.form.filter.user.GPropertyFilter;
import lsfusion.gwt.client.form.filter.user.GPropertyFilterDTO;
import lsfusion.gwt.client.form.filter.user.GUserFilter;
import lsfusion.gwt.client.form.filter.user.GUserFiltersController;
import lsfusion.gwt.client.form.filter.user.GFilterControls;
import lsfusion.gwt.client.form.filter.user.view.GFilterConditionView;
import lsfusion.gwt.client.form.filter.user.view.GDataFilterPropertyValue;
import lsfusion.gwt.client.form.filter.user.view.GFilterCompareSelector;
import lsfusion.gwt.client.form.filter.user.view.GFilterOptionSelector;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.GObject;
import lsfusion.gwt.client.form.object.panel.controller.GPlatformRowsPanelController;
import lsfusion.gwt.client.form.object.table.GToolbar;
import lsfusion.gwt.client.form.object.table.grid.GGrid;
import lsfusion.gwt.client.form.object.table.grid.user.toolbar.view.GToolbarButton;
import lsfusion.gwt.client.form.property.GClassViewType;
import lsfusion.gwt.client.form.property.GExtraPropReader;
import lsfusion.gwt.client.form.property.GExtInt;
import lsfusion.gwt.client.form.property.GLoadingReader;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.panel.view.PropertyPanelRenderer;
import lsfusion.gwt.client.form.property.GPropertyReaderDTO;
import lsfusion.gwt.client.form.property.GRowSelectReader;
import lsfusion.gwt.client.form.property.GShowIfReader;
import lsfusion.gwt.client.form.property.GEventSource;
import lsfusion.gwt.client.form.property.async.GAsyncAddRemove;
import lsfusion.gwt.client.form.property.async.GAsyncCloseForm;
import lsfusion.gwt.client.form.property.async.GAsyncEventExec;
import lsfusion.gwt.client.form.property.async.GAsyncInput;
import lsfusion.gwt.client.form.property.async.GPushAsyncInput;
import lsfusion.gwt.client.form.property.panel.view.ActionOrPropertyPanelValue;
import lsfusion.gwt.client.form.property.panel.view.PanelRenderer;
import lsfusion.gwt.client.form.property.cell.GEditBindingMap;
import lsfusion.gwt.client.form.property.cell.controller.ExecContext;
import lsfusion.gwt.client.form.property.cell.controller.EditContext;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.controller.CancelReason;
import lsfusion.gwt.client.form.property.cell.controller.EndReason;
import lsfusion.gwt.client.form.property.cell.view.GUserInputResult;
import lsfusion.gwt.client.form.object.table.controller.GGroupController;
import lsfusion.gwt.client.form.object.table.controller.GListViewController;
import lsfusion.gwt.client.form.object.table.controller.GPlatformTableController;
import lsfusion.gwt.client.form.object.table.controller.GTableController;
import lsfusion.gwt.client.form.object.table.grid.GGridProperty;
import lsfusion.gwt.client.form.object.table.grid.view.GGridTable;
import lsfusion.gwt.client.form.object.table.grid.view.GListViewType;
import lsfusion.gwt.client.form.object.table.grid.view.GSimpleStateTableView;
import lsfusion.gwt.client.form.property.cell.classes.view.HTMLTextCellRenderer;
import lsfusion.gwt.client.form.object.table.tree.GTreeGroup;
import lsfusion.gwt.client.form.object.table.tree.controller.GTreeController;
import lsfusion.gwt.client.form.object.table.grid.user.toolbar.view.GToolbarButton;
import lsfusion.gwt.client.form.view.Column;
import lsfusion.gwt.client.form.view.FormContainer;
import lsfusion.gwt.client.navigator.controller.GAsyncFormController;
import lsfusion.gwt.client.navigator.controller.dispatch.GNavigatorActionDispatcher;
import lsfusion.gwt.client.navigator.window.GAbstractWindow;
import lsfusion.gwt.client.navigator.window.GModalityWindowFormType;
import lsfusion.gwt.client.navigator.window.GWindowFormType;
import lsfusion.gwt.client.navigator.window.view.WindowsController;
import net.customware.gwt.dispatch.client.standard.StandardDispatchServiceAsync;
import net.customware.gwt.dispatch.shared.Action;
import net.customware.gwt.dispatch.shared.Result;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Consumer;

import static lsfusion.gwt.client.GFormChanges.GPropertyReadType.DRAW;
import static lsfusion.gwt.client.base.GwtClientUtils.jsonStringify;
import static lsfusion.gwt.client.base.jsni.JsniTestSupport.batch;
import static lsfusion.gwt.client.base.jsni.JsniTestSupport.call;
import static lsfusion.gwt.client.base.jsni.JsniTestSupport.field;
import static lsfusion.gwt.client.base.jsni.JsniTestSupport.objects;
import static lsfusion.gwt.client.base.jsni.JsniTestSupport.own;
import static lsfusion.gwt.client.base.jsni.JsniTestSupport.parse;
import static lsfusion.gwt.client.base.jsni.JsniTestSupport.then;

// A form's controller as it is made, over a form given whole with nothing behind it - no server, no window: what its
// constructor builds is what a test reads, and what it is given then is what the server would send. The React view of
// a grid of its own is built with the form's containers, before the controllers, and is sized by the grid's columns,
// which the form counts before it builds the layout. A delta goes to the controller of each owner it carries something
// of, and what React keeps nothing of has one that keeps nothing: what the delta carries of it is dropped, and the
// rest of the delta applied. An input the server asks for with no cell to enter it in is cancelled. A key is
// pressed in the page, and handed to the form's bindings as the form's listener hands it (press). The form's
// dispatcher is told of requests sent and answered, with nothing sent, and its timer runs as it does (inTurn). A
// property's lookup is the Promise of its own answers, given as the server gives them (LookupServer), and a view's change
// and batch the Promise of their request (ChangeServer)
public class GFormControllerTest extends GWTTestCase {
    @Override public String getModuleName() { return "lsfusion.gwt.main"; }

    @Override protected void gwtSetUp() {
        JsniTestSupport.installMapDelete();
        JsniTestSupport.installResizeObserver();
        JsniTestSupport.installPageSetup();
        installPageHelpers();
    }
    @Override protected void gwtTearDown() {
        if (server != null)
            replaceService(server.pageService);
    }

    // Clearing a diff or an HTML cell deletes even 0, "", false, and never assigns a global value in either window.
    public void testClearDiffDeletesFalsyPrevList() {
        Element element = Document.get().createDivElement();
        for (Object value : new Object[] {0.0, "", false}) {
            GwtClientUtils.setField(element, "prevList", value);
            assertTrue(own(element, "prevList"));
            GSimpleStateTableView.clearDiff(element);
            assertFalse("clearDiff kept falsy prevList: " + value, own(element, "prevList"));
        }
    }

    public void testHtmlTextClearDeletesFalsyController() {
        Element element = Document.get().createDivElement();
        HTMLTextCellRenderer renderer = new HTMLTextCellRenderer(new GPropertyDraw(), true);
        for (Object value : new Object[] {0.0, "", false}) {
            GwtClientUtils.setField(element, "controller", value);
            assertTrue(own(element, "controller"));
            renderer.clearRenderContent(element, null);
            assertFalse("clearRenderContent kept falsy controller: " + value, own(element, "controller"));
        }
    }

    public void testClearDiffDoesNotLeakGlobalValue() {
        Element element = Document.get().createDivElement();
        GwtClientUtils.setField(element, "prevList", GwtClientUtils.newObject());
        assertNoGlobalValue("before clearDiff");
        try {
            GSimpleStateTableView.clearDiff(element);
            assertNoGlobalValue("clearDiff leaked");
        } finally {
            GwtClientUtils.deleteField(JsniTestSupport.moduleWindow(), "value");
            GwtClientUtils.deleteField(JsniTestSupport.pageWindow(), "value");
        }
    }

    public void testHtmlTextClearDoesNotLeakGlobalValue() {
        Element element = Document.get().createDivElement();
        HTMLTextCellRenderer renderer = new HTMLTextCellRenderer(new GPropertyDraw(), true);
        GwtClientUtils.setField(element, "controller", GwtClientUtils.newObject());
        assertNoGlobalValue("before clearRenderContent");
        try {
            renderer.clearRenderContent(element, null);
            assertNoGlobalValue("clearRenderContent leaked");
        } finally {
            GwtClientUtils.deleteField(JsniTestSupport.moduleWindow(), "value");
            GwtClientUtils.deleteField(JsniTestSupport.pageWindow(), "value");
        }
    }

    private static void assertNoGlobalValue(String message) {
        assertFalse(message + " window.value", own(JsniTestSupport.moduleWindow(), "value"));
        assertFalse(message + " $wnd.value", own(JsniTestSupport.pageWindow(), "value"));
    }

    // what the client's page gives it and a test's page does not (webapp/static/js/utils.js): its text and element
    // helpers the layout calls as it builds the views - here doing nothing, or answering as for plain text: nothing
    // here reads a caption -, and jQuery UI, which a modal window is made draggable and resizable with (ModalWindow)
    public static native void installPageHelpers() /*-{
        var noop = function () { };
        // HtmlUnit lacks Node.isConnected, which a refused host's takeover checks before placing into it.
        if (!("isConnected" in $doc.documentElement))
            Object.defineProperty($wnd.Node.prototype, "isConnected", {
                get: function () { return $doc.documentElement.contains(this); }
            });
        var helpers = {
            initCaptionHtmlOrText: noop, setCaptionHtmlOrText: noop, setCaptionNodeText: noop, initDataHtmlOrText: noop,
            setDataHtmlOrText: noop, clearDataHtmlOrText: noop, initHtmlOrText: noop, clearHtmlOrText: noop,
            setHtmlOrText: noop, setNodeText: noop, fixHyphens: noop, setAttributeOrStyle: noop,
            removeAttributeOrStyle: noop, setGlobalClassName: noop, setReadonlyNative: noop, setReadonlyClass: noop,
            setReadonlyHeur: noop, setReadonlyType: noop, setDisabledNative: noop, setDisabledClass: noop,
            setDisabledType: noop, addShowCollapsedContainerEvent: noop,
            containsHtmlTag: function () { return false; },
            hasLongStringWithoutSpacesAndHTMLTags: function () { return false; },
            createPlainObject: function () { return {}; },
            createFocusElement: function (tag) { return $doc.createElement(tag); },
            createFocusElementType: function (tag) { return $doc.createElement(tag); },
            $: function () { return {draggable: noop, resizable: noop}; }
        };
        for (var name in helpers)
            if (!$wnd[name])
                $wnd[name] = helpers[name];
    }-*/;

    // a real form's requests asked of the server, answered by the test as the server answers them: the page's RPC
    // service replaced for the requests a kind takes and the rest passed on to it - the other tests' requests among
    // them, which a page with no server behind it retries -, and the form's requests sent apart (dispatchApart)
    private Server server;
    private static abstract class Server implements StandardDispatchServiceAsync {
        final StandardDispatchServiceAsync pageService = getPageService();
        final ArrayList<Action<?>> actions = new ArrayList<>();
        final ArrayList<AsyncCallback<Result>> callbacks = new ArrayList<>();
        final GFormController controller;

        Server(GFormController controller) {
            this.controller = controller;
            replaceService(this);
            dispatchApart(controller);
        }
        @Override
        public void execute(Action<?> action, AsyncCallback<Result> callback) {
            if (takes(action)) {
                actions.add(action);
                callbacks.add(callback);
            } else
                pageService.execute(action, callback);
        }
        abstract boolean takes(Action<?> action);
        // the server's answer to the request asked index-th: the request over, or the client to go on with it
        void answer(int index, boolean resumeInvocation) {
            callbacks.get(index).onSuccess(new ServerResponseResult(new GAction[0], ((FormRequestAction<?>) actions.get(index)).requestIndex, resumeInvocation));
        }
        void answerChanges(int index) {
            GFormChangesDTO changes = GFormChangesTest.noChanges();
            changes.requestIndex = (int) ((FormRequestAction<?>) actions.get(index)).requestIndex;
            callbacks.get(index).onSuccess(new ServerResponseResult(new GAction[] {new GProcessFormChangesAction(changes)}, changes.requestIndex, false));
        }
    }
    // ... a property's lookups
    private LookupServer lookups() {
        LookupServer lookups = new LookupServer(new FiltersForm(false));
        server = lookups;
        return lookups;
    }
    private static final class LookupServer extends Server {
        final FiltersForm form;

        LookupServer(FiltersForm form) {
            super(form.controller());
            this.form = form;
        }
        @Override
        boolean takes(Action<?> action) {
            return action instanceof GetPriorityAsyncValues || action instanceof GetAsyncValues;
        }
        // the price's suggestions for the query, as its member asks for them
        JavaScriptObject getValues(String query) {
            return controller.getPropertyValues("items.price.getValues()", form.price, GGroupObjectValue.EMPTY, query, "objects", 0);
        }
        // ... and as the form's controller asks for them, by the property's name
        JavaScriptObject getValuesByName(String query) {
            return (JavaScriptObject) call(controller.controller, "getValues", "items.price", query);
        }
        // the server's answer to the request asked index-th: its suggestions, and what it marks them with (RECHECK,
        // CANCELED, NEEDMORE)
        void answer(int index, GAsync... values) {
            callbacks.get(index).onSuccess(new ListResult(new ArrayList<>(Arrays.asList(values))));
        }
    }
    // ... and the toolbar's asks, answered without changing the form's state.
    private AskServer asks() {
        FiltersForm form = new FiltersForm(false);
        form.form.sessionID = "asks" + (++asksForms);
        AskServer asks = new AskServer(form);
        server = asks;
        return asks;
    }
    private static int asksForms;
    private static final class AskServer extends Server {
        final FiltersForm form;

        AskServer(FiltersForm form) {
            super(form.controller());
            this.form = form;
        }
        @Override
        boolean takes(Action<?> action) {
            return (action instanceof GetPriorityAsyncValues || action instanceof GetAsyncValues
                    || action instanceof CountRecords || action instanceof CalculateSum || action instanceof GroupReport || action instanceof ChangeMode
                    || action instanceof ExecuteBatch || action instanceof SetViewFilters)
                    && controller.getForm().sessionID.equals(((FormAction<?>) action).formSessionID);
        }
    }

    // ... and the requests of the changes a view's calls make - a property's change, a batch, an exec on the form's
    // controller, a continuation of any -, of a form that sends nothing as it opens, so an answer is taken in as it
    // comes, nothing before it in the form's queue, and named apart, the other tests' forms asking the same requests
    private ChangeServer changes() {
        return changes(form -> { });
    }
    private ChangeServer changes(Consumer<GForm> design) {
        GForm form = form("changes");
        form.sessionID = "changes" + (++changesForms);
        GContainer board = container(2, "board", form.mainContainer); // NEW board { custom = 'Board'; MOVE PROPERTY(note()); }
        board.custom = "Board"; board.react = true;
        GPropertyDraw note = column(form, 20, "note", null);
        note.isList = false;
        note.container = board; board.children.add(note);
        design.accept(form);
        form.initialFormChanges = GFormChangesTest.noChanges();
        ChangeServer changes = new ChangeServer(new GFormController(formsController(), null, formContainer(), form, false, "changes", 0, null), note);
        server = changes;
        return changes;
    }
    private static int changesForms;
    private static final class ChangeServer extends Server {
        final GPropertyDraw note;

        ChangeServer(GFormController controller, GPropertyDraw note) {
            super(controller);
            this.note = note;
        }
        @Override
        boolean takes(Action<?> action) {
            return action instanceof GenerateID || ((action instanceof ExecuteEventAction || action instanceof ExecuteBatch || action instanceof ControllerExecAction
                    || action instanceof ContinueInvocation) && controller.getForm().sessionID.equals(((FormAction<?>) action).formSessionID));
        }
        // a change of the note, as its member makes it: the Promise of its request
        JavaScriptObject change(int value) {
            return controller.changeProperty(note, GGroupObjectValue.EMPTY, PValue.getPValue(value), PValue.getPValue(value));
        }
    }
    // ... and the requests of a table's filter commands - the user filters set, a batch of them -, of a form whose
    // filter panel the platform draws
    private FilterServer filters() {
        FiltersForm form = new FiltersForm(true);
        form.form.sessionID = "filters" + (++filtersForms);
        FilterServer filters = new FilterServer(form);
        server = filters;
        return filters;
    }
    private static int filtersForms;
    private static final class FilterServer extends Server {
        final FiltersForm form;

        FilterServer(FiltersForm form) {
            super(form.controller());
            this.form = form;
        }
        @Override
        boolean takes(Action<?> action) {
            return (action instanceof SetUserFilters || action instanceof ExecuteBatch)
                    && controller.getForm().sessionID.equals(((FormAction<?>) action).formSessionID);
        }
    }
    // ... and the requests of a group's current object and sortings, answered apart from other tests' forms
    private GroupServer groups() {
        GForm form = form("groups");
        form.sessionID = "groups" + (++groupsForms);
        GTreeGroup tree = platformTree(form);
        for (int id : new int[] {90, 100}) {
            GGroupObject group = new GGroupObject();
            group.ID = id; group.nativeSID = "g" + id; group.sID = "g" + id; group.viewType = GClassViewType.LIST;
            group.objects.add(new GObject(group, group.sID, id, group.sID, GIntegerType.instance));
            group.parent = tree; group.upTreeGroups.addAll(tree.groups);
            tree.groups.add(group); form.groupObjects.add(group);
            GPropertyDraw property = column(form, id + 5, "name" + id, group);
            property.cellType = property.valueType;
        }
        form.initialFormChanges = GFormChangesTest.noChanges();
        GroupServer groups = new GroupServer(new GFormController(formsController(), null, formContainer(), form, false, "groups", 0, null));
        server = groups;
        return groups;
    }
    private static int groupsForms;
    private static final class GroupServer extends Server {
        GroupServer(GFormController controller) {
            super(controller);
        }
        @Override
        boolean takes(Action<?> action) {
            return (action instanceof SetPropertyOrders || action instanceof ChangeGroupObject || action instanceof ExecuteBatch
                    || action instanceof SetUserFilters || action instanceof ExpandGroupObject || action instanceof ExpandGroupObjectRecursive
                    || action instanceof CollapseGroupObject || action instanceof CollapseGroupObjectRecursive)
                    && controller.getForm().sessionID.equals(((FormAction<?>) action).formSessionID);
        }
    }
    // the forms' controller, as far as a form's answers ask it: told of each, which opens and hides no form here
    private static FormsController formsController() {
        return new FormsController(new WindowsController() {
            public Widget getWindowView(GAbstractWindow window) { return null; }
        }) {
            public <T extends Result> long syncDispatch(NavigatorRequestAction<T> action, RequestCountingAsyncCallback<T> callback) { return -1; }
            public long asyncDispatch(ExecuteNavigatorAction action, RequestCountingAsyncCallback<ServerResponseResult> callback) { return -1; }
            public GNavigatorActionDispatcher getDispatcher() { return null; }
        };
    }
    private static native StandardDispatchServiceAsync getPageService() /*-{
        return @lsfusion.gwt.client.controller.dispatch.GWTDispatch::getRealServiceInstance()();
    }-*/;
    private static native void replaceService(StandardDispatchServiceAsync service) /*-{
        @lsfusion.gwt.client.controller.dispatch.GWTDispatch::realService = service;
    }-*/;
    private static GAsync suggestion(String raw) {
        return new GAsync(raw.toUpperCase(), raw, "{\"id\":17}");
    }
    // ... and an answer of it, as JSON: the Promise of the final answer, `more`, an object there
    private static String answer(String raw, boolean preliminary) {
        return "{\"data\":[{\"displayString\":\"" + raw.toUpperCase() + "\",\"rawString\":\"" + raw + "\",\"objects\":{\"id\":17}}]"
                + (preliminary ? ",\"more\":{}" : "") + "}";
    }

    // a member's lookup is the Promise of the server's answer: the suggestions
    public void testALookupIsThePromiseOfItsAnswer() {
        LookupServer lookups = lookups();
        JavaScriptObject promise = lookups.getValues("ab");
        GetPriorityAsyncValues request = (GetPriorityAsyncValues) lookups.actions.get(0);
        assertEquals("ab objects 0", request.value + " " + request.actionSID + " " + request.increaseValuesNeededCount);
        delayTestFinish(5000);
        then(promise, (JavaScriptObject answer) -> {
            assertEquals(answer("ab", false), jsonStringify(answer));
            finishTest();
        }, error -> fail(error));
        lookups.answer(0, suggestion("ab"));
    }

    // ... the form's controller's lookup by the property's name is the same request, and the same Promise of its answer
    public void testTheFormsControllersLookupByNameIsThePromiseOfItsAnswer() {
        LookupServer lookups = lookups();
        JavaScriptObject promise = lookups.getValuesByName("ab");
        GetPriorityAsyncValues request = (GetPriorityAsyncValues) lookups.actions.get(0);
        assertEquals("ab objects 0", request.value + " " + request.actionSID + " " + request.increaseValuesNeededCount);
        delayTestFinish(5000);
        then(promise, (JavaScriptObject answer) -> {
            assertEquals(answer("ab", false), jsonStringify(answer));
            finishTest();
        }, error -> fail(error));
        lookups.answer(0, suggestion("ab"));
    }

    // a query too short for the server to search by is rejected with what the platform's editor says then, as exec and
    // eval are with the server's message
    public void testAQueryTooShortToSearchByIsRejectedWithThePlatformsMessage() {
        LookupServer lookups = lookups();
        JavaScriptObject promise = lookups.getValues("a");
        delayTestFinish(5000);
        then(promise, (JavaScriptObject answer) -> fail("answered " + jsonStringify(answer)), error -> {
            assertEquals(ClientMessages.Instance.get().needMoreSymbols(), error);
            finishTest();
        });
        lookups.answer(0, GAsync.NEEDMORE);
    }

    // an answer the server gave before it was sure of the form's changes (RECHECK) is asked again through the form's
    // queue: it carries the Promise of that answer, so a view can show both
    public void testAPreliminaryAnswerCarriesThePromiseOfTheFinalOne() {
        LookupServer lookups = lookups();
        JavaScriptObject promise = lookups.getValues("ab");
        delayTestFinish(5000);
        then(promise, (JavaScriptObject answer) -> {
            assertEquals(answer("fast", true), jsonStringify(answer));
            assertTrue(lookups.actions.get(1) instanceof GetAsyncValues);
            then(field(answer, "more"), (JavaScriptObject last) -> {
                assertEquals(answer("checked", false), jsonStringify(last));
                finishTest();
            }, error -> fail(error));
            lookups.answer(1, suggestion("checked"));
        }, error -> fail(error));
        lookups.answer(0, suggestion("fast"), GAsync.RECHECK);
    }

    // ... and once the form is closed, the Promise of that answer is rejected as a call on a closed form is: the form
    // asks nothing more
    public void testAPreliminaryAnswerAfterTheFormClosesRejectsTheFinalOne() {
        LookupServer lookups = lookups();
        JavaScriptObject promise = lookups.getValues("ab");
        closeForm(lookups.controller);
        delayTestFinish(5000);
        then(promise, (JavaScriptObject answer) -> {
            assertEquals(answer("fast", true), jsonStringify(answer));
            then(field(answer, "more"), (JavaScriptObject last) -> fail("answered " + jsonStringify(last)), error -> {
                assertEquals("Form is closed", error);
                assertEquals(1, lookups.actions.size());
                finishTest();
            });
        }, error -> fail(error));
        lookups.answer(0, suggestion("fast"), GAsync.RECHECK);
    }

    // a lookup with no list in its mode - a change of a property that cannot be changed - is rejected, not cancelled
    public void testALookupWithNoListIsRejected() {
        LookupServer lookups = lookups();
        JavaScriptObject promise = lookups.getValues("ab");
        delayTestFinish(5000);
        then(promise, (JavaScriptObject answer) -> fail("answered " + jsonStringify(answer)), error -> {
            assertEquals("controller.items.price.getValues(): 'price' has no values to offer in mode 'objects'", error);
            finishTest();
        });
        lookups.answer(0, GAsync.CANCELED);
    }

    public void testAFailedLookupIsRejected() {
        LookupServer lookups = lookups();
        JavaScriptObject promise = lookups.getValues("ab");
        delayTestFinish(5000);
        then(promise, (JavaScriptObject answer) -> fail("answered " + jsonStringify(answer)), error -> {
            assertEquals("the server failed", error);
            finishTest();
        });
        lookups.callbacks.get(0).onFailure(new RuntimeException("the server failed"));
    }

    // the server looks values up one lookup of a form at a time, a later one cancelling an earlier one: its Promise is
    // rejected `cancelled` whenever the answer comes - after the later lookup's too, which checkLast would drop
    public void testALookupALaterOneCancelledIsRejectedCancelled() {
        LookupServer lookups = lookups();
        JavaScriptObject first = lookups.getValues("a");
        lookups.getValues("ab");
        delayTestFinish(5000);
        then(first, (JavaScriptObject answer) -> fail("answered " + jsonStringify(answer)), error -> {
            assertEquals("controller.items.price.getValues(): a later lookup of the form replaced it: the form looks values up one at a time (cancelled)", error);
            finishTest();
        });
        lookups.answer(1, suggestion("ab"));
        lookups.answer(0, GAsync.CANCELED);
    }

    // ... and so is the final answer a preliminary one carries, asked through the queue as a lookup of the same index -
    // after the later lookup's answer too
    public void testAFinalAnswerALaterLookupCancelledIsRejectedCancelled() {
        LookupServer lookups = lookups();
        JavaScriptObject promise = lookups.getValues("a");
        delayTestFinish(5000);
        then(promise, (JavaScriptObject answer) -> {
            lookups.getValues("ab");
            then(field(answer, "more"), (JavaScriptObject last) -> fail("answered " + jsonStringify(last)), error -> {
                assertTrue(error, error.endsWith("(cancelled)"));
                finishTest();
            });
            lookups.answer(2, suggestion("ab"));
            lookups.answer(1, GAsync.CANCELED);
        }, error -> fail(error));
        lookups.answer(0, suggestion("a"), GAsync.RECHECK);
    }

    // ... but a lookup the server answered before the later one came keeps its answer, whichever comes first
    public void testAnEarlierLookupKeepsItsOwnAnswer() {
        LookupServer lookups = lookups();
        JavaScriptObject first = lookups.getValues("a");
        lookups.getValues("ab");
        delayTestFinish(5000);
        then(first, (JavaScriptObject answer) -> {
            assertEquals(answer("a", false), jsonStringify(answer));
            finishTest();
        }, error -> fail(error));
        lookups.answer(1, suggestion("ab"));
        lookups.answer(0, suggestion("a"));
    }

    // the editor's lookups and the classic views', which drop an earlier lookup's answer once a later one's has come
    // (checkLast), drop it after a member's answer too - else it would come marked as one more answers follow
    public void testAMembersAnswerDropsAnEarlierLookupOfTheEditor() {
        LookupServer lookups = lookups();
        ArrayList<String> answered = new ArrayList<>();
        lookups.controller.getAsyncValues("a", lookups.form.price, GGroupObjectValue.EMPTY, "objects", new AsyncCallback<GFormController.GAsyncResult>() {
            public void onFailure(Throwable caught) {
                answered.add("failed");
            }
            public void onSuccess(GFormController.GAsyncResult result) {
                answered.add("answered, more: " + result.moreRequests);
            }
        }, 0);
        lookups.getValues("ab");
        lookups.answer(1, suggestion("ab"));
        lookups.answer(0, suggestion("a"));
        assertTrue(String.valueOf(answered), answered.isEmpty());
    }

    // Count and sum return the Promise of their own answer, without a wrapper in the projection.
    public void testTheToolbarsCountAndSumWaitForTheirAnswers() {
        AskServer asks = asks();
        GFormController controller = asks.controller;
        JavaScriptObject count = controller.countRecords("items.getCount()", asks.form.group);
        JavaScriptObject sum = controller.calculateSum("items.getSum()", asks.form.group, asks.form.price, GGroupObjectValue.EMPTY);
        assertEquals(2, asks.actions.size());
        assertTrue(asks.actions.get(0) instanceof CountRecords);
        assertTrue(asks.actions.get(1) instanceof CalculateSum);
        ArrayList<String> settled = new ArrayList<>();
        then(count, (Double value) -> {
            assertEquals(7.0, value, 0);
            settled.add("count");
        }, error -> fail(error));
        then(sum, (Double value) -> {
            assertEquals(12.5, value, 0);
            settled.add("sum");
        }, error -> fail(error));
        delayTestFinish(5000);
        Scheduler.get().scheduleDeferred(() -> {
            assertTrue(settled.isEmpty());
            asks.callbacks.get(0).onSuccess(new NumberResult(7L));
            Scheduler.get().scheduleDeferred(() -> {
                assertEquals(Collections.singletonList("count"), settled);
                asks.callbacks.get(1).onSuccess(new NumberResult(12.5));
                Scheduler.get().scheduleDeferred(() -> {
                    assertEquals(Arrays.asList("count", "sum"), settled);
                    finishTest();
                });
            });
        });
    }

    // Export has its own answer callback, rather than a form-changes response.
    public void testAnExportWaitsForItsAnswerAndRejectsItsFailure() {
        AskServer asks = asks();
        JavaScriptObject first = asks.controller.runTableReport("items.export()", asks.form.group.grid);
        JavaScriptObject second = asks.controller.runTableReport("items.export()", asks.form.group.grid);
        assertEquals(2, asks.actions.size());
        ArrayList<String> settled = new ArrayList<>();
        then(first, answer -> {
            assertNull(answer);
            settled.add("first");
        }, error -> fail(error));
        then(second, answer -> fail("resolved"), error -> {
            assertEquals("the export failed", error);
            assertEquals(Collections.singletonList("first"), settled);
            assertTrue(closeDialog("the export failed"));
            finishTest();
        });
        delayTestFinish(5000);
        Scheduler.get().scheduleDeferred(() -> {
            assertTrue(settled.isEmpty());
            asks.callbacks.get(0).onSuccess(new GroupReportResult());
            Scheduler.get().scheduleDeferred(() -> {
                assertEquals(Collections.singletonList("first"), settled);
                asks.callbacks.get(1).onFailure(new RuntimeException("the export failed"));
            });
        });
    }

    // The platform's export button calls this overload directly: a closed form must keep neither a queued report
    // nor its sync count, and must reject the report's own Promise.
    public void testThePlatformsExportOfAClosedFormIsRejectedWithoutQueuingARequest() {
        AskServer asks = asks();
        FormDispatchAsync dispatcher = closeForm(asks.controller);
        int queued = queuedRequests(dispatcher), sync = dispatcher.syncCount, async = dispatcher.asyncCount;
        long next = nextRequestIndex(asks.controller);
        JavaScriptObject promise = asks.controller.runTableReport(asks.form.group.grid);
        assertTrue(asks.actions.isEmpty());
        assertEquals(queued, queuedRequests(dispatcher));
        assertEquals(sync, dispatcher.syncCount);
        assertEquals(async, dispatcher.asyncCount);
        assertEquals(next, nextRequestIndex(asks.controller));
        delayTestFinish(5000);
        then(promise, answer -> fail("resolved"), error -> {
            assertEquals("Form is closed", error);
            finishTest();
        });
    }

    // Update and view changes wait for their own responses; keeping their state sends nothing and resolves at once.
    public void testUpdateAndListViewPromisesWaitForTheirOwnAnswers() {
        AskServer asks = asks();
        GFormController controller = asks.controller;
        GGroupObject group = asks.form.group;
        GListViewController views = new GListViewController() {
            GListViewType shown = GListViewType.GRID;
            public List<GListViewType> getListViews() { return Arrays.asList(GListViewType.GRID, GListViewType.PIVOT); }
            public GListViewType getListView() { return shown; }
            public void changeListView(GListViewType view, ExecCallback onExec) { // as the platform's: the view shown is no switch
                if (view == shown) {
                    onExec.onCancel();
                    return;
                }
                shown = view;
                onExec.onSuccess(controller.changeListViewType(group, 20, view, null));
            }
        };
        JavaScriptObject sameMode = controller.changeUpdateMode(group, GUpdateMode.AUTO);
        JavaScriptObject sameView = controller.changeListView(views, GListViewType.GRID);
        assertTrue(asks.actions.isEmpty());
        JavaScriptObject manual = controller.changeUpdateMode(group, GUpdateMode.MANUAL);
        JavaScriptObject view = controller.changeListView(views, GListViewType.PIVOT);
        JavaScriptObject update = controller.changeUpdateMode(group, GUpdateMode.FORCE);
        assertEquals(3, asks.actions.size());
        ArrayList<String> settled = new ArrayList<>();
        then(sameMode, answer -> settled.add("same mode"), error -> fail(error));
        then(sameView, answer -> settled.add("same view"), error -> fail(error));
        then(manual, answer -> settled.add("manual"), error -> fail(error));
        then(view, answer -> settled.add("view"), error -> fail(error));
        then(update, answer -> settled.add("update"), error -> fail(error));
        delayTestFinish(5000);
        inTurn(() -> {
            assertEquals(Arrays.asList("same mode", "same view"), settled);
            asks.answer(0, false);
        }, () -> {
            assertEquals(Arrays.asList("same mode", "same view", "manual"), settled);
            asks.answer(1, false);
        }, () -> {
            assertEquals(Arrays.asList("same mode", "same view", "manual", "view"), settled);
            asks.answer(2, false);
        }, () -> {
            assertEquals(Arrays.asList("same mode", "same view", "manual", "view", "update"), settled);
            finishTest();
        });
    }

    // The update mode and the view go with a batch: made at once, their requests sent in its one request, its Promise
    // theirs; on a closed form refused before they run.
    public void testTheUpdateModeAndTheViewGoWithABatch() {
        AskServer asks = asks();
        GFormController controller = asks.controller;
        GGroupObject group = asks.form.group;
        GListViewController views = new GListViewController() {
            GListViewType shown = GListViewType.GRID;
            public List<GListViewType> getListViews() { return Arrays.asList(GListViewType.GRID, GListViewType.PIVOT); }
            public GListViewType getListView() { return shown; }
            public void changeListView(GListViewType view, ExecCallback onExec) { // as the platform's: the view shown is no switch
                if (view == shown) {
                    onExec.onCancel();
                    return;
                }
                shown = view;
                onExec.onSuccess(controller.changeListViewType(group, 20, view, null));
            }
        };
        ArrayList<JavaScriptObject> promises = new ArrayList<>();
        JavaScriptObject batch = controller.batch(() -> {
            promises.add(controller.changeUpdateMode(group, GUpdateMode.MANUAL));
            promises.add(controller.changeListView(views, GListViewType.PIVOT));
            promises.add(controller.changeUpdateMode(group, GUpdateMode.FORCE));
            assertTrue(controller.isManualUpdate(group));
            assertEquals(GListViewType.PIVOT, views.getListView());
            assertTrue(asks.actions.isEmpty());
        });
        assertEquals(1, asks.actions.size());
        ArrayList<FormRequestCountingAction<?>> requests = ((ExecuteBatch) asks.actions.get(0)).requests;
        assertEquals(3, requests.size());
        for (FormRequestCountingAction<?> request : requests)
            assertTrue(request instanceof ChangeMode);
        for (JavaScriptObject promise : promises)
            assertSame(batch, promise);
        ArrayList<String> settled = new ArrayList<>();
        then(batch, answer -> settled.add("batch"), error -> fail(error));
        delayTestFinish(5000);
        inTurn(() -> {
            assertTrue(settled.isEmpty());
            asks.answer(0, false);
        }, () -> {
            assertEquals(Collections.singletonList("batch"), settled);
            closeForm(controller);
            then(controller.changeUpdateMode(group, GUpdateMode.AUTO), answer -> fail("resolved"), error -> {
                assertEquals("Form is closed", error);
                assertTrue(controller.isManualUpdate(group));
                assertEquals(1, asks.actions.size());
                finishTest();
            });
        });
    }

    // A classic view's view filters are the Promise of their request, sent with their group and the view's page size:
    // pending till its answer; with no column of the view's to filter by - no request to make - resolved at once; in a
    // batch the batch's; on a closed form refused before they run.
    public void testViewFiltersAreThePromiseOfTheirRequest() {
        AskServer asks = asks();
        GFormController controller = asks.controller;
        GGroupObject group = asks.form.group;
        ArrayList<GPropertyFilter> filters = new ArrayList<>(Collections.singletonList(asks.form.condition(asks.form.quantity, PValue.getPValue(2), GCompare.EQUALS)));
        ArrayList<String> settled = new ArrayList<>();
        then(controller.changeViewFilters(group, filters, 1000), answer -> settled.add("filtered"), error -> fail(error));
        then(controller.noRequestPromise(), answer -> settled.add("none"), error -> fail(error));
        assertEquals(1, asks.actions.size());
        assertTrue(asks.actions.get(0) instanceof SetViewFilters);
        assertEquals(group.ID, ((SetViewFilters) asks.actions.get(0)).groupObjectID);
        assertEquals(Integer.valueOf(1000), ((SetViewFilters) asks.actions.get(0)).pageSize);
        JavaScriptObject[] inBatch = new JavaScriptObject[1];
        JavaScriptObject batch = controller.batch(() -> {
            inBatch[0] = controller.changeViewFilters(group, filters, 1000);
            assertSame(inBatch[0], controller.noRequestPromise());
        });
        assertSame(batch, inBatch[0]);
        assertEquals(2, asks.actions.size());
        assertTrue(asks.actions.get(1) instanceof ExecuteBatch);
        delayTestFinish(5000);
        inTurn(() -> {
            assertEquals(Collections.singletonList("none"), settled);
            asks.answer(0, false);
        }, () -> {
            assertEquals(Arrays.asList("none", "filtered"), settled);
            closeForm(controller);
            then(controller.changeViewFilters(group, filters, 1000), answer -> fail("resolved"), error -> {
                assertEquals("Form is closed", error);
                assertEquals(2, asks.actions.size());
                finishTest();
            });
        });
    }

    // A React view's view filters are sent with their group and no page size - an empty list too, which takes them
    // off -, and the rows' view, GRID(items)'s own, shows them at once, told by the form; on a closed form they are
    // neither shown nor sent.
    public void testAViewsViewFiltersAreShownByItsRowsAndSentWithTheirGroup() {
        AskServer asks = asks();
        GFormController controller = asks.controller;
        GGroupObject group = asks.form.group;
        List<GPropertyFilter> filters = Collections.singletonList(asks.form.condition(asks.form.quantity, PValue.getPValue(2), GCompare.EQUALS));
        assertEquals("", rowsViewFilters(controller, group));
        controller.changeViewFilters(group, filters, null);
        assertEquals("quantity=2", rowsViewFilters(controller, group));
        controller.changeViewFilters(group, new ArrayList<>(), null);
        assertEquals("", rowsViewFilters(controller, group));
        assertEquals(2, asks.actions.size());
        assertEquals(1, ((SetViewFilters) asks.actions.get(0)).filters.size());
        SetViewFilters none = (SetViewFilters) asks.actions.get(1);
        assertEquals(group.ID, none.groupObjectID);
        assertTrue(none.filters.isEmpty());
        assertNull(none.pageSize);
        closeForm(controller);
        delayTestFinish(5000);
        then(controller.changeViewFilters(group, filters, null), answer -> fail("resolved"), error -> {
            assertEquals("Form is closed", error);
            assertEquals("", rowsViewFilters(controller, group));
            assertEquals(2, asks.actions.size());
            finishTest();
        });
    }
    // the view filters of a group's rows, as the view of its grid that draws them is given them now: each one's
    // property, comparison and value
    private static native String rowsViewFilters(GFormController controller, GGroupObject group) /*-{
        var projection = controller.@lsfusion.gwt.client.form.controller.GFormController::reactData;
        var state = projection.@lsfusion.gwt.client.form.design.view.GReactFormData::views.@lsfusion.gwt.client.base.jsni.NativeSIDMap::get(*)(group.@lsfusion.gwt.client.form.object.GGroupObject::grid);
        return state.@lsfusion.gwt.client.form.design.view.GReactFormData.ViewState::current()().items.viewFilters.map(function (condition) {
            return condition.property + condition.compare + condition.value;
        }).join(' ');
    }-*/;

    // A grid's SELECTION, where a view draws its rows, is the platform's own requests, as its grid makes them, each the
    // Promise of its request: a row and what becomes of the rows selected (ChangeGroupObject), an edge of the rows
    // (ScrollToEnd), every row (SelectAll), the current column (ChangePropertyActive) - that one made only where the
    // form follows what changes of the columns, the active property or a column it reads as selected (quantity):
    // following none of it, the call is the Promise of no request, resolved at once. In a batch they go in its one
    // request, in the order they are made, each the batch's Promise; on a closed form they are refused
    public void testAGridsSelectionIsTheFormsOwnRequests() {
        FiltersForm filters = new FiltersForm(false);
        filters.deliver(true);
        filters.form.sessionID = "selection" + (++asksForms);
        filters.quantity.hasSelectProperty = true;
        filters.group.rowSelectReader = new GRowSelectReader(filters.group.ID); // the rows selected, as every group of a form reads them
        GFormController controller = filters.controller();
        Server requests = new Server(controller) {
            @Override
            boolean takes(Action<?> action) {
                return action instanceof FormRequestCountingAction && controller.getForm().sessionID.equals(((FormAction<?>) action).formSessionID);
            }
        };
        server = requests;
        JavaScriptObject form = controller.controller;
        GGroupObjectValue one = new GGroupObjectValue(filters.group.ID, 1), two = new GGroupObjectValue(filters.group.ID, 2);
        ArrayList<String> settled = new ArrayList<>();
        then((JavaScriptObject) call(form, "changeColumn", "items", "price", parse("{\"selection\": \"start\"}")), answer -> settled.add("price"), error -> fail(error));
        assertTrue(requests.actions.isEmpty()); // the form follows nothing of price
        call(form, "changeObject", "items", objects(two), parse("{\"selection\": \"extend\"}"));
        ChangeGroupObject row = (ChangeGroupObject) requests.actions.get(0);
        assertEquals(two, row.keyValues);
        assertEquals(GChangeSelection.MOVEEND, row.changeSelection);
        call(form, "changeObject", "items", field(form, "END"), parse("{\"selection\": \"keep\"}"));
        ScrollToEnd end = (ScrollToEnd) requests.actions.get(1);
        assertTrue(end.toEnd && end.changeSelection == null);
        then((JavaScriptObject) call(form, "changeColumn", "items", "quantity", parse("{\"selection\": \"extend\"}")), answer -> settled.add("quantity"), error -> fail(error));
        ChangePropertyActive column = (ChangePropertyActive) requests.actions.get(2);
        assertEquals(filters.quantity.ID, column.propertyId);
        assertTrue(Arrays.equals(new int[] {filters.quantity.ID}, column.changeSelectionProps) && column.changeSelectionValues[0]);

        JavaScriptObject[] inBatch = new JavaScriptObject[3];
        JavaScriptObject batch = controller.batch(() -> {
            inBatch[0] = (JavaScriptObject) call(form, "changeObject", "items", objects(one), parse("{\"selection\": \"start\"}"));
            inBatch[1] = (JavaScriptObject) call(form, "changeColumn", "items", "price", parse("{\"selection\": \"start\"}"));
            inBatch[2] = (JavaScriptObject) call(form, "selectAll", "items");
        });
        for (JavaScriptObject promise : inBatch)
            assertSame(batch, promise);
        assertEquals(4, requests.actions.size());
        ArrayList<FormRequestCountingAction<?>> sent = ((ExecuteBatch) requests.actions.get(3)).requests;
        assertEquals(3, sent.size());
        assertEquals(GChangeSelection.MOVE, ((ChangeGroupObject) sent.get(0)).changeSelection);
        assertEquals(filters.price.ID, ((ChangePropertyActive) sent.get(1)).propertyId);
        assertFalse(((ChangePropertyActive) sent.get(1)).changeSelectionValues[0]); // quantity is selected no longer
        assertTrue(((SelectAll) sent.get(2)).changeSelectionValues[0]); // ... and is again
        delayTestFinish(5000);
        inTurn(() -> {
            assertEquals(Collections.singletonList("price"), settled);
            for (int i = 0; i < 4; i++)
                requests.answer(i, false);
        }, () -> {
            assertEquals(Arrays.asList("price", "quantity"), settled);
            closeForm(controller);
            then((JavaScriptObject) call(form, "selectAll", "items"), answer -> fail("resolved"), error -> {
                assertEquals("Form is closed", error);
                assertEquals(4, requests.actions.size());
                finishTest();
            });
        });
    }

    // ... and the rows a move selects are HELD as the platform's table's are, by the form's own way with a row's
    // selection - the move's request waited for (changeGroupObjectLater), the deliveries made meanwhile and its answer
    // set right by it (modifyFormChangesWithChangePropertyAsyncs): shown at once; kept through a delivery that comes
    // before the request is answered and says them not selected; kept by the answer that says them; and, the rows a
    // move leaves out, shown again where its answer says nothing of them - the server left them selected
    public void testTheRowsAGridsMoveSelectsAreHeldUntilItsRequestIsAnswered() {
        FiltersForm filters = new FiltersForm(false);
        filters.deliver(true);
        filters.form.sessionID = "held" + (++asksForms);
        filters.group.rowSelectReader = new GRowSelectReader(filters.group.ID); // the rows selected, as every group of a form reads them
        GFormController controller = filters.controller();
        Server requests = new Server(controller) {
            @Override
            boolean takes(Action<?> action) {
                return action instanceof ChangeGroupObject && controller.getForm().sessionID.equals(((FormAction<?>) action).formSessionID);
            }
        };
        server = requests;
        JavaScriptObject form = controller.controller;
        GGroupObject group = filters.group;
        GGroupObjectValue one = new GGroupObjectValue(group.ID, 1), two = new GGroupObjectValue(group.ID, 2);
        call(form, "changeObject", "items", objects(two), parse("{\"selection\": \"extend\"}"));
        assertEquals("1,2", rowsSelected(controller, group)); // at once
        int extended = (int) ((FormRequestAction<?>) requests.actions.get(0)).requestIndex;
        GFormChanges before = new GFormChanges(); // the rows as the server had them before the move: none selected
        before.properties.put(group.rowSelectReader, new NativeHashMap<>());
        controller.applyRemoteChanges(before, extended - 1);
        assertEquals("1,2", rowsSelected(controller, group));
        GFormChanges answer = new GFormChanges();
        NativeHashMap<GGroupObjectValue, PValue> selected = new NativeHashMap<>();
        selected.put(one, PValue.getPValue(true));
        selected.put(two, PValue.getPValue(true));
        answer.properties.put(group.rowSelectReader, selected);
        controller.applyRemoteChanges(answer, extended);
        assertEquals("1,2", rowsSelected(controller, group));

        call(form, "changeObject", "items", objects(one)); // a range anew: none selected
        assertEquals("", rowsSelected(controller, group));
        controller.applyRemoteChanges(new GFormChanges(), (int) ((FormRequestAction<?>) requests.actions.get(1)).requestIndex);
        assertEquals("1,2", rowsSelected(controller, group)); // the answer says nothing of them: they are as they were
        requests.answer(0, false);
        requests.answer(1, false);
    }
    // the rows of a group that say they are selected, by their keys, as the view of its grid that draws them is given
    // them now
    private static native String rowsSelected(GFormController controller, GGroupObject group) /*-{
        var projection = controller.@lsfusion.gwt.client.form.controller.GFormController::reactData;
        var state = projection.@lsfusion.gwt.client.form.design.view.GReactFormData::views.@lsfusion.gwt.client.base.jsni.NativeSIDMap::get(*)(group.@lsfusion.gwt.client.form.object.GGroupObject::grid);
        return state.@lsfusion.gwt.client.form.design.view.GReactFormData.ViewState::current()().items.list.filter(function (row) {
            return row.selected;
        }).map(function (row) { return row.key; }).join(',');
    }-*/;

    // Asks refuse inside the form: no request or state change, their Promise rejected with the reason.
    public void testAsksAreRefusedInABatchAndOnAClosedForm() {
        AskServer asks = asks();
        GFormController controller = asks.controller;
        GGroupObject group = asks.form.group;
        ArrayList<JavaScriptObject> promises = new ArrayList<>();
        Runnable ask = () -> {
            promises.add(controller.countRecords("items.getCount()", group));
            promises.add(controller.calculateSum("items.getSum()", group, asks.form.price, GGroupObjectValue.EMPTY));
            promises.add(controller.getPropertyValues("items.price.getValues()", asks.form.price, GGroupObjectValue.EMPTY, "ab", "objects", 0));
            promises.add((JavaScriptObject) call(controller.controller, "getCount", "items"));
            promises.add((JavaScriptObject) call(controller.controller, "getSum", "items", "price"));
            promises.add((JavaScriptObject) call(controller.controller, "getValues", "items.price", "ab", null));
            promises.add(controller.runTableReport("items.export()", group.grid));
            promises.add((JavaScriptObject) call(controller.controller, "export", "items"));
            assertTrue(refusalOf(() -> call(controller.controller, "getValues", "items.price", null, parse("{\"typo\":true}"))).contains("takes the query"));
            assertTrue(refusalOf(() -> call(controller.controller, "getValues", "items.price", "ab", parse("{\"typo\":true}"))).contains("unknown field 'typo'"));
            assertTrue(refusalOf(() -> call(controller.controller, "getSum", "items", "missing")).contains("unknown property 'missing'"));
            assertTrue(refusalOf(() -> call(controller.controller, "changeListView", "items", "unknown")).contains("has no view to switch to"));
        };
        assertEquals("", consoleErrorsOf(() -> controller.batch(ask)));
        assertTrue(asks.actions.isEmpty());
        closeForm(controller);
        assertEquals("", consoleErrorsOf(ask));
        assertTrue(asks.actions.isEmpty());
        List<String> reasons = new ArrayList<>();
        for (String surface : Arrays.asList("items.getCount()", "items.getSum()", "items.price.getValues()", "form.getCount('items')",
                "form.getSum('items')", "form.getValues('items.price')", "items.export()", "form.export('items')"))
            reasons.add("controller." + surface + ": a batch is running, which carries changes only: make this call before the batch or after it");
        reasons.addAll(Collections.nCopies(reasons.size(), "Form is closed"));
        int[] rejected = {0};
        delayTestFinish(5000);
        for (int i = 0; i < promises.size(); i++) {
            String reason = reasons.get(i);
            then(promises.get(i), answer -> fail("resolved"), error -> {
                assertEquals(reason, error);
                if (++rejected[0] == reasons.size())
                    finishTest();
            });
        }
    }

    private static native String refusalOf(Runnable run) /*-{
        try { run.@java.lang.Runnable::run()(); return null; } catch (e) { return e.message; }
    }-*/;

    // a view's change of a property is the Promise of its request: resolved when the request is over - the server's
    // answer taken in, and the answer to the continuation it asked the client for -, not before
    public void testAChangeIsThePromiseOfItsRequest() {
        ChangeServer changes = changes();
        ArrayList<String> settled = new ArrayList<>();
        then(changes.change(5), answer -> settled.add("resolved"), error -> settled.add("rejected: " + error));
        assertTrue(changes.actions.get(0) instanceof ExecuteEventAction);
        delayTestFinish(5000);
        changes.answer(0, true); // the server asks the client to go on with the request
        assertTrue(changes.actions.get(1) instanceof ContinueInvocation);
        Scheduler.get().scheduleDeferred(() -> {
            assertEquals(Collections.emptyList(), settled); // ... so it is not over
            changes.answer(1, false);
            Scheduler.get().scheduleDeferred(() -> {
                assertEquals(Collections.singletonList("resolved"), settled);
                finishTest();
            });
        });
    }

    // ... and over once the form is closed: a request made before the close and answered after it, the answer asking
    // the client to go on with it - with a question's answer, with the failure of what the client ran -, fails, its
    // continuation not sent, and its Promise is rejected as a call on a closed form is - a member's change, and an exec
    // on the form's controller -, with nothing left queued or counted (the busy dialog stayed up) and nothing shown
    public void testAContinuationDueAfterTheFormClosesRejectsItsRequest() {
        ChangeServer changes = changes();
        JavaScriptObject change = changes.change(5);
        JavaScriptObject exec = (JavaScriptObject) call(changes.controller.controller, "exec", "act");
        assertEquals(2, changes.actions.size());
        FormDispatchAsync dispatcher = closeForm(changes.controller);
        ArrayList<String> settled = new ArrayList<>();
        then(change, answer -> fail("resolved"), error -> settled.add("change: " + error));
        then(exec, answer -> fail("resolved"), error -> settled.add("exec: " + error));
        delayTestFinish(5000);
        changes.answer(0, true); // the server asks the client to go on with the change
        GAction failing = d -> { throw new RuntimeException("the client failed"); };
        changes.callbacks.get(1).onSuccess(new ServerResponseResult(new GAction[] {failing},
                ((FormRequestAction<?>) changes.actions.get(1)).requestIndex, true)); // ... and with the exec, what it asked the client to run failing
        inTurn(() -> {
            assertEquals(Arrays.asList("change: Form is closed", "exec: Form is closed"), settled);
            assertEquals(2, changes.actions.size()); // nothing sent,
            assertEquals(0, queuedRequests(dispatcher)); // ... left queued
            assertEquals(0, dispatcher.syncCount); // ... or counted
            assertFalse(closeDialog("Form is closed")); // ... or shown
            assertNoPendingPropertyRequests(changes.controller);
            finishTest();
        });
    }

    // ... but a form only off the page, its dispatcher not closed yet, still goes on with it - the server hides a form
    // and may ask before it closes it -, the Promise resolved once the continuation is answered
    public void testAContinuationDueAfterTheFormLeftThePageIsSent() {
        ChangeServer changes = changes();
        ArrayList<String> settled = new ArrayList<>();
        then(changes.change(5), answer -> settled.add("resolved"), error -> fail(error));
        delayTestFinish(5000);
        hide(changes.controller);
        changes.answer(0, true); // the server asks the client to go on with the change
        assertEquals(2, changes.actions.size());
        assertTrue(changes.actions.get(1) instanceof ContinueInvocation);
        changes.answer(1, false);
        Scheduler.get().scheduleDeferred(() -> {
            assertEquals(Collections.singletonList("resolved"), settled);
            finishTest();
        });
    }

    // ... and any request made once the form's dispatcher is closed fails, as it is not sent - the platform's own, which
    // nothing refuses before it is made: a dialog of the form's answered after it closed; a classic view's lookup -,
    // told to whoever waits for it, with nothing left queued or counted (a sync one kept the busy dialog up) and nothing
    // shown
    public void testARequestMadeOnAClosedFormFails() {
        LookupServer lookups = lookups();
        FormDispatchAsync dispatcher = closeForm(lookups.controller);
        ArrayList<String> told = new ArrayList<>();
        lookups.controller.executeVoidAction(); // a sync request
        lookups.controller.getRemoteChanges(false); // ... an async one
        lookups.controller.getAsyncValues("ab", lookups.form.price, GGroupObjectValue.EMPTY, "objects", new AsyncCallback<GFormController.GAsyncResult>() {
            @Override public void onFailure(Throwable caught) { told.add(caught.getMessage()); }
            @Override public void onSuccess(GFormController.GAsyncResult result) { told.add("answered"); }
        }, 0); // ... and a lookup
        delayTestFinish(5000);
        inTurn(() -> {
            assertEquals(Collections.singletonList("Form is closed"), told);
            assertTrue(lookups.actions.isEmpty());
            assertEquals(0, queuedRequests(dispatcher));
            assertEquals(0, dispatcher.syncCount);
            assertEquals(0, dispatcher.asyncCount);
            assertFalse(dispatcher.getBusyDialogDisplayer().isVisible());
            assertFalse(closeDialog("Form is closed"));
            finishTest();
        });
    }

    // an exec shares the change's Promise path, including the synchronous event's answer
    public void testAnExecIsThePromiseOfItsRequest() {
        ChangeServer changes = changes();
        changes.note.asyncExecMap = new HashMap<>(); // no asynchronous exec of its own: the event goes as the request it is
        JavaScriptObject promise = changes.controller.executePropertyEventAction(changes.note, GGroupObjectValue.EMPTY, PValue.UNDEFINED, index -> { });
        assertEquals(1, changes.actions.size());
        assertTrue(changes.actions.get(0) instanceof ExecuteEventAction);
        ArrayList<String> settled = new ArrayList<>();
        then(promise, answer -> settled.add("resolved"), error -> fail(error));
        delayTestFinish(5000);
        Scheduler.get().scheduleDeferred(() -> {
            assertTrue(settled.isEmpty());
            changes.answer(0, false);
            Scheduler.get().scheduleDeferred(() -> {
                assertEquals(Collections.singletonList("resolved"), settled);
                finishTest();
            });
        });
    }

    // A declined close makes no request: its Promise resolves after No, without marking the property as loading.
    public void testADeclinedCloseResolvesItsPromise() {
        ChangeServer changes = changes();
        changes.note.asyncExecMap = Collections.singletonMap(GEditBindingMap.CHANGE, new GAsyncCloseForm());
        GFormChanges confirm = new GFormChanges();
        confirm.needConfirm = true;
        changes.controller.applyRemoteChanges(confirm, -1);
        JavaScriptObject promise = changes.controller.changeProperty(changes.note, GGroupObjectValue.EMPTY, PValue.UNDEFINED, PValue.UNDEFINED);
        ArrayList<String> settled = new ArrayList<>();
        then(promise, answer -> settled.add("resolved"), error -> fail(error));
        delayTestFinish(5000);
        inTurn(() -> {
            assertTrue(settled.isEmpty());
            assertTrue(changes.actions.isEmpty());
            assertTrue(answerDialog(ClientMessages.Instance.get().doYouReallyWantToCloseForm(), ClientMessages.Instance.get().no()));
        }, () -> {
            assertEquals(Collections.singletonList("resolved"), settled);
            assertTrue(changes.actions.isEmpty());
            assertNoPendingPropertyRequests(changes.controller);
            finishTest();
        });
    }

    // A DELETE with no row has no request to await, including through the form's classic changeProperty.
    public void testADeleteWithNoRowResolvesItsPromise() {
        ChangeServer changes = changes(form -> platformGrid(form, 90, "items"));
        changes.note.asyncExecMap = Collections.singletonMap(GEditBindingMap.CHANGE, new GAsyncAddRemove(90, false));
        JavaScriptObject promise = (JavaScriptObject) call(changes.controller.controller, "changeProperty", "note");
        assertTrue(changes.actions.isEmpty());
        delayTestFinish(5000);
        then(promise, answer -> {
            assertNoPendingPropertyRequests(changes.controller);
            finishTest();
        }, error -> fail(error));
    }

    // A failed ID lookup sends no NEW event: the platform shows its error, and the exec's Promise is rejected with it.
    public void testANewWithoutAnIdRejectsItsPromise() {
        ChangeServer changes = changes(form -> platformGrid(form, 90, "items"));
        changes.note.asyncExecMap = Collections.singletonMap(GEditBindingMap.CHANGE, new GAsyncAddRemove(90, true));
        LogicsDispatchAsync pageLogics = MainFrame.logicsDispatchAsync;
        MainFrame.logicsDispatchAsync = new LogicsDispatchAsync(null, null, null);
        dispatchApart(MainFrame.logicsDispatchAsync);
        JavaScriptObject promise;
        try {
            promise = changes.controller.executePropertyEventAction(changes.note, GGroupObjectValue.EMPTY, PValue.UNDEFINED,
                    index -> fail("no event request was made"));
        } finally {
            MainFrame.logicsDispatchAsync = pageLogics;
        }
        assertEquals(1, changes.actions.size());
        assertTrue(changes.actions.get(0) instanceof GenerateID);
        ArrayList<String> settled = new ArrayList<>();
        then(promise, answer -> fail("resolved"), error -> settled.add(error));
        delayTestFinish(5000);
        inTurn(() -> {
            assertTrue(settled.isEmpty());
            changes.callbacks.get(0).onFailure(new RuntimeException("no new object id"));
            assertTrue(closeDialog("no new object id"));
        }, () -> {
            assertEquals(Collections.singletonList("no new object id"), settled);
            assertEquals(1, changes.actions.size());
            assertNoPendingPropertyRequests(changes.controller);
            finishTest();
        });
    }

    // Closing during ID generation must reject NEW without queuing its request or predicting a row it cannot clear.
    public void testANewWhoseIdArrivesAfterTheFormClosesIsRejected() {
        ChangeServer changes = changes(form -> platformGrid(form, 90, "items"));
        changes.note.asyncExecMap = Collections.singletonMap(GEditBindingMap.CHANGE, new GAsyncAddRemove(90, true));
        LogicsDispatchAsync pageLogics = MainFrame.logicsDispatchAsync;
        MainFrame.logicsDispatchAsync = new LogicsDispatchAsync(null, null, null);
        dispatchApart(MainFrame.logicsDispatchAsync);
        JavaScriptObject promise;
        try {
            promise = changes.controller.changeProperty(changes.note, GGroupObjectValue.EMPTY, PValue.UNDEFINED, PValue.UNDEFINED);
        } finally {
            MainFrame.logicsDispatchAsync = pageLogics;
        }
        assertEquals(1, changes.actions.size());
        assertTrue(changes.actions.get(0) instanceof GenerateID);
        FormDispatchAsync dispatcher = (FormDispatchAsync) dispatcher(changes.controller);
        int queued = queuedRequests(dispatcher), async = dispatcher.asyncCount;
        GGroupObject group = changes.controller.getForm().groupObjects.get(0);
        ArrayList<String> settled = new ArrayList<>();
        then(promise, answer -> fail("resolved"), error -> settled.add(error));
        delayTestFinish(5000);
        inTurn(() -> {
            assertTrue(settled.isEmpty());
            closeForm(changes.controller);
            changes.callbacks.get(0).onSuccess(new GenerateIDResult(123));
        }, () -> {
            assertEquals(queued, queuedRequests(dispatcher));
            assertEquals(async, dispatcher.asyncCount);
            assertTrue(rowKeys(platformGridView(changes.controller, group)).isEmpty());
            assertNull(sentAt(changes.controller, group));
            assertEquals(Collections.singletonList("Form is closed"), settled);
            assertEquals(1, changes.actions.size());
            assertNoPendingPropertyRequests(changes.controller);
            finishTest();
        });
    }

    // ... but a form only off the page, its dispatcher not closed yet, still makes it - an editor's form commits its
    // input as it closes -, the Promise its request's
    public void testANewWhoseIdArrivesAfterTheFormLeftThePageIsSent() {
        ChangeServer changes = changes(form -> platformGrid(form, 90, "items"));
        changes.note.asyncExecMap = Collections.singletonMap(GEditBindingMap.CHANGE, new GAsyncAddRemove(90, true));
        LogicsDispatchAsync pageLogics = MainFrame.logicsDispatchAsync;
        MainFrame.logicsDispatchAsync = new LogicsDispatchAsync(null, null, null);
        dispatchApart(MainFrame.logicsDispatchAsync);
        JavaScriptObject promise;
        try {
            promise = changes.controller.changeProperty(changes.note, GGroupObjectValue.EMPTY, PValue.UNDEFINED, PValue.UNDEFINED);
        } finally {
            MainFrame.logicsDispatchAsync = pageLogics;
        }
        assertEquals(1, changes.actions.size());
        assertTrue(changes.actions.get(0) instanceof GenerateID);
        ArrayList<String> settled = new ArrayList<>();
        then(promise, answer -> settled.add("resolved"), error -> fail(error));
        delayTestFinish(5000);
        inTurn(() -> {
            hide(changes.controller);
            changes.callbacks.get(0).onSuccess(new GenerateIDResult(123));
        }, () -> {
            assertTrue(settled.isEmpty());
            assertEquals(2, changes.actions.size());
            assertTrue(changes.actions.get(1) instanceof ExecuteEventAction);
            changes.answer(1, false);
        }, () -> {
            assertEquals(Collections.singletonList("resolved"), settled);
            finishTest();
        });
    }

    // In a batch the same calls are plain event requests and share its Promise until the batch's answer.
    public void testNoRequestExecsInABatchWaitForItsAnswer() {
        ChangeServer changes = changes(form -> platformGrid(form, 90, "items"));
        GFormChanges confirm = new GFormChanges();
        confirm.needConfirm = true;
        changes.controller.applyRemoteChanges(confirm, -1);
        ArrayList<JavaScriptObject> made = new ArrayList<>();
        JavaScriptObject promise = changes.controller.batch(() -> {
            for (GAsyncEventExec exec : new GAsyncEventExec[] {new GAsyncCloseForm(), new GAsyncAddRemove(90, false), new GAsyncAddRemove(90, true)}) {
                changes.note.asyncExecMap = Collections.singletonMap(GEditBindingMap.CHANGE, exec);
                made.add(changes.controller.changeProperty(changes.note, GGroupObjectValue.EMPTY, PValue.UNDEFINED, PValue.UNDEFINED));
            }
        });
        for (JavaScriptObject member : made)
            assertSame(promise, member);
        assertEquals(1, changes.actions.size());
        assertTrue(changes.actions.get(0) instanceof ExecuteBatch);
        assertEquals(3, ((ExecuteBatch) changes.actions.get(0)).requests.size());
        ArrayList<String> settled = new ArrayList<>();
        then(promise, answer -> settled.add("resolved"), error -> fail(error));
        delayTestFinish(5000);
        inTurn(() -> {
            assertTrue(settled.isEmpty());
            assertFalse(answerDialog(ClientMessages.Instance.get().doYouReallyWantToCloseForm(), ClientMessages.Instance.get().no()));
            changes.answerChanges(0);
        }, () -> {
            assertEquals(Collections.singletonList("resolved"), settled);
            assertNoPendingPropertyRequests(changes.controller);
            finishTest();
        });
    }

    private static native void assertNoPendingPropertyRequests(GFormController controller) /*-{
        @junit.framework.Assert::assertTrue(Z)(controller.@lsfusion.gwt.client.form.controller.GFormController::pendingLoadingPropertyRequests.@lsfusion.gwt.client.base.jsni.NativeSIDMap::isEmpty()());
        @junit.framework.Assert::assertTrue(Z)(controller.@lsfusion.gwt.client.form.controller.GFormController::pendingChangePropertyRequests.@lsfusion.gwt.client.base.jsni.NativeSIDMap::isEmpty()());
        @junit.framework.Assert::assertTrue(Z)(controller.@lsfusion.gwt.client.form.controller.GFormController::pendingModifyObjectRequests.@java.util.ArrayList::isEmpty()());
        @junit.framework.Assert::assertTrue(Z)(controller.@lsfusion.gwt.client.form.controller.GFormController::requestPromises.@lsfusion.gwt.client.base.jsni.NativeHashMap::isEmpty()());
    }-*/;

    // A deferred change gets its own request index; an earlier independent answer must not settle it.
    public void testADeferredChangeWaitsForItsLateRequestsAnswer() {
        ChangeServer changes = changes();
        JavaScriptObject earlier = changes.change(1);
        changes.note.asyncExecMap = new HashMap<>();
        changes.note.asyncExecMap.put(GEditBindingMap.CHANGE, new GAsyncEventExec() {
            @Override
            public void exec(GFormController formController, EventHandler handler, EditContext editContext, ExecContext execContext,
                             String actionSID, GPushAsyncInput pushAsyncResult, GEventSource eventSource, ExecCallback onExec) {
                Scheduler.get().scheduleDeferred(() -> formController.syncExecutePropertyEventAction(editContext, handler,
                        execContext.getProperty(), execContext.getFullKey(), pushAsyncResult, actionSID, eventSource, onExec));
            }
        });
        JavaScriptObject late = changes.controller.changeProperty(changes.note, GGroupObjectValue.EMPTY, PValue.UNDEFINED, PValue.UNDEFINED);
        assertEquals(1, changes.actions.size()); // the change's request has not been made yet
        ArrayList<String> settled = new ArrayList<>();
        then(earlier, answer -> settled.add("earlier"), error -> fail(error));
        then(late, answer -> settled.add("late"), error -> fail(error));
        delayTestFinish(5000);
        changes.answer(0, false);
        Scheduler.get().scheduleDeferred(() -> {
            assertEquals(2, changes.actions.size());
            assertTrue(((FormRequestAction<?>) changes.actions.get(0)).requestIndex < ((FormRequestAction<?>) changes.actions.get(1)).requestIndex);
            assertEquals(Collections.singletonList("earlier"), settled);
            changes.answer(1, false);
            Scheduler.get().scheduleDeferred(() -> {
                assertEquals(Arrays.asList("earlier", "late"), settled);
                finishTest();
            });
        });
    }

    // a request that fails rejects its Promise with the reason, which the platform shows the user, as it does a failed
    // edit's
    public void testAFailedChangeIsRejectedWithTheReason() {
        ChangeServer changes = changes();
        JavaScriptObject promise = changes.change(5);
        delayTestFinish(5000);
        then(promise, (JavaScriptObject answer) -> fail("resolved"), error -> {
            assertEquals("the server failed", error);
            assertTrue(closeDialog("the server failed"));
            finishTest();
        });
        changes.callbacks.get(0).onFailure(new RuntimeException("the server failed"));
    }

    // ... an exec on the form's controller too, when it is a continuation of the request that fails
    public void testAFailedContinuationRejectsAnExecOnTheFormsController() {
        ChangeServer changes = changes();
        JavaScriptObject exec = (JavaScriptObject) call(changes.controller.controller, "exec", "act");
        delayTestFinish(5000);
        then(exec, answer -> fail("resolved"), error -> {
            assertEquals("the continuation failed", error);
            assertTrue(closeDialog("the continuation failed"));
            finishTest();
        });
        changes.answer(0, true); // the server asks the client to go on with the exec
        assertTrue(changes.actions.get(1) instanceof ContinueInvocation);
        changes.callbacks.get(1).onFailure(new RuntimeException("the continuation failed"));
    }
    // a dialog the client shows, saying the text, closed as a user closes it - whether there was one
    private static boolean closeDialog(String text) {
        return answerDialog(text, ClientMessages.Instance.get().close());
    }
    private static native boolean answerDialog(String text, String answer) /*-{
        var dialogs = $doc.querySelectorAll('.modal-content');
        for (var i = 0; i < dialogs.length; i++)
            if (dialogs[i].textContent.indexOf(text) >= 0) {
                var buttons = dialogs[i].querySelectorAll('button');
                for (var j = 0; j < buttons.length; j++)
                    if (buttons[j].textContent === answer) {
                        buttons[j].click();
                        return true;
                    }
            }
        return false;
    }-*/;

    // the calls of a batch make one request, so each returns the batch's Promise - a batch begun inside it too -,
    // resolved when that request is over
    public void testTheCallsOfABatchReturnItsPromise() {
        ChangeServer changes = changes();
        JavaScriptObject[] made = new JavaScriptObject[3];
        JavaScriptObject promise = changes.controller.batch(() -> {
            made[0] = changes.change(5);
            made[1] = changes.controller.batch(() -> made[2] = changes.change(6));
        });
        assertSame(promise, made[0]);
        assertSame(promise, made[1]);
        assertSame(promise, made[2]);
        assertEquals(1, changes.actions.size());
        assertTrue(changes.actions.get(0) instanceof ExecuteBatch);
        delayTestFinish(5000);
        then(promise, (JavaScriptObject answer) -> finishTest(), error -> fail(error));
        changes.answer(0, false);
    }

    // a batch that makes no request has nothing to wait for: resolved at once, and nothing sent
    public void testABatchThatSendsNothingIsResolved() {
        ChangeServer changes = changes();
        JavaScriptObject promise = changes.controller.batch(() -> { });
        delayTestFinish(5000);
        then(promise, (JavaScriptObject answer) -> {
            assertTrue(changes.actions.isEmpty());
            finishTest();
        }, error -> fail(error));
    }

    // a throw still sends the preceding changes and clears the batch before the next independent request
    public void testAThrowInABatchLeavesTheNextRequestsPromiseIndependent() {
        FilterServer filters = filters();
        GUserFiltersController model = filters.form.model(filters.controller);
        JavaScriptObject[] made = new JavaScriptObject[1];
        RuntimeException failure = new RuntimeException("the view failed");
        try {
            filters.controller.batch(() -> {
                made[0] = filters.controller.batch(() -> model.changeFilter(filters.form.drawn, filters.form.condition(null)));
                throw failure;
            });
            fail("not thrown");
        } catch (RuntimeException caught) {
            assertSame(failure, caught);
        }
        JavaScriptObject next = filters.controller.batch(model::reset);
        assertNotSame(made[0], next);
        assertEquals(2, filters.actions.size());
        assertTrue(filters.actions.get(0) instanceof ExecuteBatch);
        assertTrue(filters.actions.get(1) instanceof ExecuteBatch);
        ArrayList<String> settled = new ArrayList<>();
        then(made[0], answer -> settled.add("batch"), error -> fail(error));
        then(next, answer -> settled.add("next"), error -> fail(error));
        delayTestFinish(5000);
        filters.answer(0, false);
        Scheduler.get().scheduleDeferred(() -> {
            assertEquals(Collections.singletonList("batch"), settled);
            filters.answer(1, false);
            Scheduler.get().scheduleDeferred(() -> {
                assertEquals(Arrays.asList("batch", "next"), settled);
                finishTest();
            });
        });
    }

    // a closed form sends no request, so a change and a batch are refused - their Promise rejected, as a server call on
    // the form's controller is -, and the batch's function is not run
    public void testAChangeAndABatchOfAClosedFormAreRejected() {
        ChangeServer changes = changes();
        closeForm(changes.controller);
        boolean[] ran = {false};
        JavaScriptObject batch = changes.controller.batch(() -> ran[0] = true);
        delayTestFinish(5000);
        then(changes.change(5), answer -> fail("resolved"), error -> {
            assertEquals("Form is closed", error);
            then(batch, answer -> fail("resolved"), batchError -> {
                assertEquals("Form is closed", batchError);
                assertFalse(ran[0]);
                assertTrue(changes.actions.isEmpty());
                finishTest();
            });
        });
    }

    // a filter command - the apply of what waits in the manual apply mode, the platform's panel's - is the Promise of
    // the request it makes, resolved when that request is over
    public void testAFilterCommandIsThePromiseOfItsRequest() {
        boolean manual = MainFrame.userFiltersManualApplyMode;
        MainFrame.userFiltersManualApplyMode = true;
        try {
            FilterServer filters = filters();
            GPlatformFilterInputController panel = (GPlatformFilterInputController) userFiltersController(filters.controller, filters.form.group);
            panel.setControlsVisible(true); // the panel's apply button shown: its manual apply mode
            GUserFiltersController model = filters.form.model(filters.controller);
            model.changeFilter(filters.form.drawn, filters.form.condition(null)); // waits for the apply
            assertTrue(filters.actions.isEmpty());
            JavaScriptObject first = (JavaScriptObject) call(filters.controller.controller, "applyFilters", "items");
            assertSame(first, filters.controller.changeUserFilters(Collections.singletonList(filters.form.group), () -> { }));
            assertSame(first, call(filters.controller.controller, "applyFilters", "items")); // nothing waiting to send
            model.changeFilter(filters.form.drawn, filters.form.condition(PValue.getPValue(5))); // waits again
            JavaScriptObject second = (JavaScriptObject) call(filters.controller.controller, "applyFilters", "items");
            assertSame(second, filters.controller.changeUserFilters(Collections.singletonList(filters.form.group), () -> { }));
            assertSame(second, call(filters.controller.controller, "applyFilters", "items"));
            assertEquals(2, filters.actions.size());
            assertTrue(filters.actions.get(1) instanceof SetUserFilters);
            ArrayList<String> settled = new ArrayList<>();
            then(first, answer -> settled.add("first"), error -> settled.add("rejected: " + error));
            then(second, answer -> settled.add("second"), error -> settled.add("rejected: " + error));
            delayTestFinish(5000);
            Scheduler.get().scheduleDeferred(() -> {
                assertEquals(Collections.emptyList(), settled); // not before the answer
                filters.answerChanges(0);
                Scheduler.get().scheduleDeferred(() -> {
                    assertEquals(Collections.singletonList("first"), settled); // each by its own request's answer
                    assertSame(second, filters.controller.changeUserFilters(Collections.singletonList(filters.form.group), () -> { }));
                    filters.answerChanges(1);
                    Scheduler.get().scheduleDeferred(() -> {
                        assertEquals(Arrays.asList("first", "second"), settled);
                        then((JavaScriptObject) call(filters.controller.controller, "applyFilters", "items"), answer -> finishTest(), error -> fail(error));
                    });
                });
            });
        } finally {
            MainFrame.userFiltersManualApplyMode = manual;
        }
    }

    // With no filters in flight, an apply that sends nothing resolves at once, and one made in a batch is the
    // batch's Promise, whether the call makes a request of its own or not
    public void testAFilterCommandMakingNoRequestIsResolvedAndInABatchItIsTheBatchs() {
        FilterServer filters = filters();
        GUserFiltersController model = filters.form.model(filters.controller);
        JavaScriptObject none = (JavaScriptObject) call(filters.controller.controller, "applyFilters", "items");
        assertTrue(filters.actions.isEmpty());
        JavaScriptObject[] made = new JavaScriptObject[2];
        JavaScriptObject batch = filters.controller.batch(() -> {
            made[0] = (JavaScriptObject) call(filters.controller.controller, "applyFilters", "items"); // nothing waits: no request of its own
            model.changeFilter(filters.form.drawn, filters.form.condition(null)); // sent at once: a request the batch carries
            made[1] = (JavaScriptObject) call(filters.controller.controller, "resetFilters", "items");
        });
        assertSame(batch, made[0]);
        assertSame(batch, made[1]);
        assertEquals(1, filters.actions.size());
        assertTrue(filters.actions.get(0) instanceof ExecuteBatch);
        ArrayList<String> settled = new ArrayList<>();
        then(none, answer -> settled.add("none"), error -> fail(error));
        then(batch, answer -> settled.add("batch"), error -> fail(error));
        delayTestFinish(5000);
        Scheduler.get().scheduleDeferred(() -> {
            assertEquals(Collections.singletonList("none"), settled); // the batch's waits for its request
            filters.answer(0, false);
            Scheduler.get().scheduleDeferred(() -> {
                assertEquals(Arrays.asList("none", "batch"), settled);
                finishTest();
            });
        });
    }

    // ... one whose request fails is rejected with the reason, which the platform shows, as a failed change's is
    public void testAFailedFilterCommandIsRejectedWithTheReason() {
        FilterServer filters = filters();
        GUserFiltersController model = filters.form.model(filters.controller);
        model.changeFilter(filters.form.drawn, filters.form.condition(null)); // sent at once
        JavaScriptObject promise = (JavaScriptObject) call(filters.controller.controller, "resetFilters", "items");
        assertEquals(2, filters.actions.size());
        delayTestFinish(5000);
        then(promise, answer -> fail("resolved"), error -> {
            assertEquals("the server failed", error);
            assertTrue(closeDialog("the server failed"));
            finishTest();
        });
        filters.answer(0, false);
        filters.callbacks.get(1).onFailure(new RuntimeException("the server failed"));
    }

    // A successful request may carry no delta; a later read must not wait for that request again.
    public void testFiltersAnsweredWithoutChangesCanBeReadAgain() {
        GroupServer groups = groups();
        GFormController controller = groups.controller;
        List<GGroupObject> tableGroups = controller.getForm().groupObjects;
        controller.changeFilters(tableGroups, new ArrayList<>());
        JavaScriptObject first = controller.changeUserFilters(tableGroups, () -> { });
        controller.changeFilters(tableGroups, new ArrayList<>());
        JavaScriptObject second = controller.changeUserFilters(tableGroups, () -> { });
        assertNotSame(first, second);
        delayTestFinish(5000);
        then(first, answer -> {
            assertSame(second, controller.changeUserFilters(tableGroups, () -> { }));
            groups.answer(1, false);
        }, error -> fail(error));
        then(second, answer -> {
            JavaScriptObject read = controller.changeUserFilters(tableGroups, () -> { });
            assertNotSame(second, read);
            then(read, readAnswer -> then(controller.changeUserFilters(tableGroups, () -> { }), repeated -> {
                assertEquals(2, groups.actions.size());
                finishTest();
            }, error -> fail(error)), error -> fail(error));
        }, error -> fail(error));
        groups.answer(0, false);
    }

    // A publication subscriber must read the original Promise until the request finishes, even after its delta
    // has been applied: clearing the pending maps there made the subscriber read a new, resolved Promise.
    public void testStateReadsDuringPublicationKeepTheRequestsPromise() {
        GroupServer groups = groups();
        GFormController controller = groups.controller;
        List<GGroupObject> tableGroups = controller.getForm().groupObjects;
        JavaScriptObject promise = controller.batch(() -> {
            controller.changeFilters(tableGroups, new ArrayList<>());
            controller.expandAll(tableGroups.get(0));
        });
        boolean[] published = {false};
        controller.addReactView(controller.getForm().mainContainer, data -> {
            published[0] = true;
            assertSame(promise, controller.changeUserFilters(tableGroups, () -> { }));
            assertSame(promise, controller.expandNodes(tableGroups, () -> { }));
        });
        delayTestFinish(5000);
        then(promise, answer -> {
            assertTrue(published[0]);
            assertNotSame(promise, controller.changeUserFilters(tableGroups, () -> { }));
            assertNotSame(promise, controller.expandNodes(tableGroups, () -> { }));
            finishTest();
        }, error -> fail(error));
        groups.answerChanges(0);
    }

    // Failure reconciliation publishes before rejecting the request: a subscriber must receive that same rejection.
    public void testStateReadsDuringFailurePublicationKeepTheRequestsPromise() {
        GroupServer groups = groups();
        GFormController controller = groups.controller;
        List<GGroupObject> tableGroups = controller.getForm().groupObjects;
        JavaScriptObject promise = controller.batch(() -> {
            controller.changeFilters(tableGroups, new ArrayList<>());
            controller.expandAll(tableGroups.get(0));
        });
        boolean[] published = {false};
        controller.addReactView(controller.getForm().mainContainer, data -> {
            published[0] = true;
            assertSame(promise, controller.changeUserFilters(tableGroups, () -> { }));
            assertSame(promise, controller.expandNodes(tableGroups, () -> { }));
        });
        delayTestFinish(5000);
        then(promise, answer -> fail("resolved"), error -> {
            assertEquals("the server failed", error);
            assertTrue(published[0]);
            assertNotSame(promise, controller.changeUserFilters(tableGroups, () -> { }));
            assertNotSame(promise, controller.expandNodes(tableGroups, () -> { }));
            assertTrue(closeDialog("the server failed"));
            finishTest();
        });
        groups.callbacks.get(0).onFailure(new RuntimeException("the server failed"));
    }

    // A closed form refuses a filter command before it runs: what was to reset is still there, nothing more asked.
    public void testAFilterCommandOfAClosedFormIsRejected() {
        FilterServer filters = filters();
        GUserFiltersController model = filters.form.model(filters.controller);
        model.changeFilter(filters.form.drawn, filters.form.condition(null)); // sent at once
        closeForm(filters.controller);
        JavaScriptObject promise = (JavaScriptObject) call(filters.controller.controller, "resetFilters", "items");
        assertTrue(filters.form.filters(filters.controller, filters.form.price));
        assertEquals(1, filters.actions.size());
        delayTestFinish(5000);
        then(promise, answer -> fail("resolved"), error -> {
            assertEquals("Form is closed", error);
            finishTest();
        });
    }

    // A table waits for the latest filter request among its groups, while even an empty batch owns its Promise.
    public void testFiltersOfSeveralGroupsWaitForTheLatestRequest() {
        GroupServer groups = groups();
        GFormController controller = groups.controller;
        List<GGroupObject> tableGroups = controller.getForm().groupObjects;
        List<GGroupObject> top = Collections.singletonList(tableGroups.get(0)), bottom = Collections.singletonList(tableGroups.get(1));
        controller.changeFilters(top, new ArrayList<>());
        JavaScriptObject first = controller.changeUserFilters(top, () -> { });
        controller.changeFilters(bottom, new ArrayList<>());
        JavaScriptObject second = controller.changeUserFilters(bottom, () -> { });
        assertNotSame(first, second);
        assertSame(second, controller.changeUserFilters(tableGroups, () -> { }));
        assertSame(second, controller.changeUserFilters(Arrays.asList(tableGroups.get(1), tableGroups.get(0)), () -> { }));
        JavaScriptObject[] made = new JavaScriptObject[1];
        JavaScriptObject batch = controller.batch(() -> made[0] = controller.changeUserFilters(tableGroups, () -> { }));
        assertSame(batch, made[0]);
        assertNotSame(second, batch);
        assertEquals(2, groups.actions.size());
        ArrayList<String> settled = new ArrayList<>();
        then(first, answer -> settled.add("first"), error -> fail(error));
        then(second, answer -> settled.add("second"), error -> fail(error));
        then(batch, answer -> settled.add("batch"), error -> fail(error));
        delayTestFinish(5000);
        Scheduler.get().scheduleDeferred(() -> {
            assertEquals(Collections.singletonList("batch"), settled);
            groups.answerChanges(0);
            Scheduler.get().scheduleDeferred(() -> {
                assertEquals(Arrays.asList("batch", "first"), settled);
                assertSame(second, controller.changeUserFilters(tableGroups, () -> { }));
                groups.answerChanges(1);
                Scheduler.get().scheduleDeferred(() -> {
                    assertEquals(Arrays.asList("batch", "first", "second"), settled);
                    then(controller.changeUserFilters(tableGroups, () -> { }), answer -> finishTest(), error -> fail(error));
                });
            });
        });
    }

    // A platform tree's command waits for its expansion request without making a batch for it.
    public void testATreeOpeningWaitsForItsExpansionRequest() {
        GroupServer groups = groups();
        GFormController controller = groups.controller;
        GGroupObject group = controller.getForm().groupObjects.get(0);
        GTreeGroup tree = group.parent;
        GGroupObjectValue key = new GGroupObjectValue(group.ID, 1);
        GFormChanges shown = new GFormChanges();
        shown.gridObjects.put(group, new ArrayList<>(Collections.singletonList(key)));
        shown.parentObjects.put(group, new ArrayList<>(Collections.singletonList(GGroupObjectValue.EMPTY)));
        shown.objects.put(group, key);
        NativeHashMap<GGroupObjectValue, Integer> expandables = new NativeHashMap<>();
        expandables.put(key, 1);
        shown.expandables.put(group, expandables);
        controller.applyRemoteChanges(shown, -1);
        Widget page = controller.getFormLayout();
        RootPanel.get().add(page);
        delayTestFinish(5000);
        Scheduler.get().scheduleDeferred(() -> {
            JavaScriptObject first, second;
            try {
                first = controller.expandNode(group, key);
                assertSame(first, controller.expandNodes(tree.groups, () -> { }));
                second = (JavaScriptObject) call(controller.controller, "expandTree", "t");
                assertEquals(2, groups.actions.size());
                assertTrue(groups.actions.get(0) instanceof ExpandGroupObject);
                assertTrue(groups.actions.get(1) instanceof ExpandGroupObjectRecursive);
                assertNotSame(first, second);
                assertSame(second, controller.expandNodes(tree.groups, () -> { }));
            } finally {
                RootPanel.get().remove(page);
            }
            ArrayList<String> settled = new ArrayList<>();
            then(first, answer -> settled.add("first"), error -> fail(error));
            then(second, answer -> settled.add("second"), error -> fail(error));
            Scheduler.get().scheduleDeferred(() -> {
                assertTrue(settled.isEmpty());
                groups.answerChanges(0);
                Scheduler.get().scheduleDeferred(() -> {
                    assertEquals(Collections.singletonList("first"), settled);
                    assertSame(second, controller.expandNodes(tree.groups, () -> { }));
                    groups.answerChanges(1);
                    Scheduler.get().scheduleDeferred(() -> {
                        assertEquals(Arrays.asList("first", "second"), settled);
                        then(controller.expandNodes(tree.groups, () -> { }), answer -> finishTest(), error -> fail(error));
                    });
                });
            });
        });
    }

    // An opening with no node resolves, shares an enclosing batch's Promise, and rejects on a closed form.
    public void testATreeOpeningWithNoRequestAndOnAClosedForm() {
        GroupServer groups = groups();
        GFormController controller = groups.controller;
        JavaScriptObject none = (JavaScriptObject) call(controller.controller, "expandCurrent", "t");
        JavaScriptObject[] made = new JavaScriptObject[1];
        JavaScriptObject batch = controller.batch(() -> made[0] = (JavaScriptObject) call(controller.controller, "expandTree", "t"));
        assertSame(batch, made[0]);
        assertTrue(groups.actions.isEmpty());
        closeForm(controller);
        JavaScriptObject closed = (JavaScriptObject) call(controller.controller, "expandCurrent", "t");
        delayTestFinish(5000);
        then(none, answer -> then(batch, batchAnswer -> then(closed, closedAnswer -> fail("resolved"), error -> {
            assertEquals("Form is closed", error);
            finishTest();
        }), error -> fail(error)), error -> fail(error));
    }

    // An expansion can finish without reporting rows; keep only requests that are still in flight.
    public void testExpansionsAnsweredWithoutChangesCanBeReadAgain() {
        GroupServer groups = groups();
        GFormController controller = groups.controller;
        GGroupObject group = controller.getForm().groupObjects.get(0);
        List<GGroupObject> tableGroups = group.parent.groups;
        JavaScriptObject first = controller.expandAll(group);
        JavaScriptObject second = controller.collapseAll(group);
        assertNotSame(first, second);
        assertSame(second, controller.expandNodes(tableGroups, () -> { }));
        delayTestFinish(5000);
        then(first, answer -> {
            assertSame(second, controller.expandNodes(tableGroups, () -> { }));
            groups.answer(1, false);
        }, error -> fail(error));
        then(second, answer -> {
            JavaScriptObject read = controller.expandNodes(tableGroups, () -> { });
            assertNotSame(second, read);
            then(read, readAnswer -> then(controller.expandNodes(tableGroups, () -> { }), repeated -> {
                assertEquals(2, groups.actions.size());
                finishTest();
            }, error -> fail(error)), error -> fail(error));
        }, error -> fail(error));
        groups.answer(0, false);
    }

    // members' ordinary requests are answered without an event action callback, each Promise by its own index
    public void testAGroupMembersPromiseWaitsForItsOwnAnswer() {
        GroupServer groups = groups();
        GFormController controller = groups.controller;
        GGroupObject group = controller.getForm().groupObjects.get(0);
        LinkedHashMap<Column, Boolean> orders = new LinkedHashMap<>();
        orders.put(new Column(controller.getForm().propertyDraws.get(0), GGroupObjectValue.EMPTY), true);
        JavaScriptObject sorted = controller.changeOrders(Collections.singletonList(group), orders);
        JavaScriptObject chosen = controller.changeCurrentObject(group, new GGroupObjectValue(group.ID, 1), null, null);
        assertEquals(2, groups.actions.size());
        assertTrue(groups.actions.get(0) instanceof SetPropertyOrders); // its own request, no batch
        assertTrue(groups.actions.get(1) instanceof ChangeGroupObject);
        ArrayList<String> settled = new ArrayList<>();
        then(sorted, answer -> settled.add("sorted"), error -> fail(error));
        then(chosen, answer -> settled.add("chosen"), error -> fail(error));
        delayTestFinish(5000);
        Scheduler.get().scheduleDeferred(() -> {
            assertTrue(settled.isEmpty());
            groups.answer(0, false);
            Scheduler.get().scheduleDeferred(() -> {
                assertEquals(Collections.singletonList("sorted"), settled);
                groups.answer(1, false);
                Scheduler.get().scheduleDeferred(() -> {
                    assertEquals(Arrays.asList("sorted", "chosen"), settled);
                    finishTest();
                });
            });
        });
    }

    // A tree's groups are sorted a request each, the Promise settled by the last one's answer; in a batch the batch's.
    public void testOrdersOfSeveralGroupsAreSettledByTheLastRequestAndJoinABatch() {
        GroupServer groups = groups();
        GFormController controller = groups.controller;
        GForm form = controller.getForm();
        LinkedHashMap<Column, Boolean> orders = new LinkedHashMap<>();
        for (GPropertyDraw property : form.propertyDraws)
            orders.put(new Column(property, GGroupObjectValue.EMPTY), true);
        JavaScriptObject sorted = controller.changeOrders(form.groupObjects, orders);
        JavaScriptObject[] made = new JavaScriptObject[1];
        JavaScriptObject batch = controller.batch(() -> made[0] = controller.changeOrders(form.groupObjects, orders));
        assertSame(batch, made[0]);
        assertNotSame(sorted, batch);
        assertEquals(3, groups.actions.size());
        for (int i = 0; i < 2; i++) {
            SetPropertyOrders order = (SetPropertyOrders) groups.actions.get(i);
            assertEquals(form.groupObjects.get(i).ID, order.groupObjectID);
            assertEquals(Collections.singletonList(form.propertyDraws.get(i).ID), order.propertyList);
        }
        assertEquals(2, ((ExecuteBatch) groups.actions.get(2)).requests.size());
        ArrayList<String> settled = new ArrayList<>();
        then(sorted, answer -> settled.add("sorted"), error -> fail(error));
        then(batch, answer -> settled.add("batch"), error -> fail(error));
        delayTestFinish(5000);
        Scheduler.get().scheduleDeferred(() -> {
            groups.answer(0, false);
            Scheduler.get().scheduleDeferred(() -> {
                assertTrue(settled.isEmpty()); // the first group's answer: the last one still to come
                groups.answer(1, false);
                Scheduler.get().scheduleDeferred(() -> {
                    assertEquals(Collections.singletonList("sorted"), settled);
                    groups.answer(2, false);
                    Scheduler.get().scheduleDeferred(() -> {
                        assertEquals(Arrays.asList("sorted", "batch"), settled);
                        finishTest();
                    });
                });
            });
        });
    }

    // A failed sorting request rejects the operation's Promise with the platform's error.
    public void testAFailedSortingRequestRejectsItsPromise() {
        GroupServer groups = groups();
        GFormController controller = groups.controller;
        JavaScriptObject sorted = controller.changeOrders(controller.getForm().groupObjects, new LinkedHashMap<>());
        assertEquals(2, groups.actions.size()); // a request per group: the last one's Promise
        delayTestFinish(5000);
        then(sorted, answer -> fail("resolved"), error -> {
            assertEquals("the sorting failed", error);
            assertTrue(closeDialog("the sorting failed"));
            finishTest();
        });
        groups.answer(0, false);
        groups.callbacks.get(1).onFailure(new RuntimeException("the sorting failed"));
    }

    // An operation that sends nothing resolves at once: a selection still held back is not waited for.
    public void testAnOperationMakingNoRequestDoesNotWaitForAnEarlierDelayedSelection() {
        GroupServer groups = groups();
        GFormController controller = groups.controller;
        GGroupObject group = controller.getForm().groupObjects.get(0);
        controller.changeGroupObjectLater(group, new GGroupObjectValue(group.ID, 1), null, null);
        assertTrue(groups.actions.isEmpty());
        JavaScriptObject none = controller.changeOrders(Collections.emptyList(), new LinkedHashMap<>());
        assertTrue(groups.actions.isEmpty()); // no request of its own: the selection still held back, not waited for
        delayTestFinish(5000);
        then(none, answer -> finishTest(), error -> fail(error));
    }

    // A closed form refuses an operation before it changes the form's state.
    public void testAnOperationOfAClosedFormIsRejectedBeforeItRuns() {
        GroupServer groups = groups();
        GFormController controller = groups.controller;
        GGroupObject group = controller.getForm().groupObjects.get(0);
        GGroupObjectValue before = controller.getGroupController(group).getSelectedKey();
        closeForm(controller);
        JavaScriptObject chosen = controller.changeCurrentObject(group, new GGroupObjectValue(group.ID, 1), null, null);
        assertEquals(before, controller.getGroupController(group).getSelectedKey());
        assertTrue(groups.actions.isEmpty());
        delayTestFinish(5000);
        then(chosen, answer -> fail("resolved"), error -> {
            assertEquals("Form is closed", error);
            finishTest();
        });
    }

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

    // the form waiting for the server, on its Refresh button React draws - the property that shows it, drawAsync -, as
    // on the platform's button of it: the button's entry says `loading` from the moment an asynchronous request of the
    // form's has waited the dispatcher's timeout - one answered sooner shows nothing -, until its answer comes; a view
    // that does not draw the button is not given anything anew (#1669)
    public void testTheRefreshButtonReactDrawsSaysWhileTheFormWaitsForTheServer() {
        GForm form = form("busy");
        GContainer board = container(2, "board", form.mainContainer), other = container(3, "other", form.mainContainer);
        board.custom = "Board"; board.react = true; // NEW board { custom = 'Board'; MOVE PROPERTY(formRefresh()); }
        other.custom = "Other"; other.react = true; // NEW other { custom = 'Other'; }
        GPropertyDraw refresh = column(form, 20, "formRefresh", null);
        refresh.isList = false; refresh.valueType = GActionType.instance; refresh.drawAsync = true;
        refresh.container = board; board.children.add(refresh);
        form.initialFormChanges = GFormChangesTest.noChanges();
        GFormController controller = new GFormController(null, null, formContainer(), form, false, "busy", 0, null);
        Object view = controller.getFormLayout().getContainerView(board);
        Object otherView = controller.getFormLayout().getContainerView(other);
        JavaScriptObject otherData = published(otherView);
        RemoteDispatchAsync dispatcher = dispatcher(controller);
        assertEquals(0, dispatcher.asyncCount); // nothing sent yet

        dispatcher.onAsyncStarted(); // a request answered before the timeout
        dispatcher.onAsyncFinished();
        delayTestFinish(5000);
        inTurn(() -> {
            assertNull(loading(published(view), "formRefresh")); // ... shows nothing
            dispatcher.onAsyncStarted(); // one that waits
        }, () -> {
            assertEquals(Boolean.TRUE, loading(published(view), "formRefresh"));
            dispatcher.onAsyncFinished(); // ... and its answer
        }, () -> {
            assertNull(loading(published(view), "formRefresh"));
            assertSame(otherData, published(otherView));
            finishTest();
        });
    }
    // the steps, each well after the one before - past the time the form's dispatcher lets a request wait before it
    // shows it (RemoteDispatchAsync.ASYNC_TIME_OUT) -, in a GWT entry of its own, as a timer runs: what a step writes
    // outside a change is published when it exits
    private static void inTurn(Runnable... steps) {
        new Timer() {
            int next;
            @Override
            public void run() {
                steps[next++].run();
                if (next < steps.length)
                    schedule(200);
            }
        }.schedule(200);
    }
    // the form's dispatcher, which counts its requests, what a React container's view was last given, and the
    // `loading` of an entry in it, null where there is none - read where the client keeps them
    // a form closed as the server closes it: hidden (hideForm), then its dispatcher closed (destroyForm)
    public static FormDispatchAsync closeForm(GFormController controller) {
        hide(controller);
        FormDispatchAsync dispatcher = (FormDispatchAsync) dispatcher(controller);
        dispatcher.close();
        return dispatcher;
    }
    private static native void hide(GFormController controller) /*-{
        controller.@lsfusion.gwt.client.form.controller.GFormController::formHidden = true;
    }-*/;
    public static native RemoteDispatchAsync dispatcher(GFormController controller) /*-{
        return controller.@lsfusion.gwt.client.form.controller.GFormController::dispatcher;
    }-*/;
    private static native int queuedRequests(RemoteDispatchAsync dispatcher) /*-{
        return dispatcher.@lsfusion.gwt.client.RemoteDispatchAsync::q.@java.util.LinkedList::size()();
    }-*/;
    private static native JavaScriptObject published(Object view) /*-{
        return view.@lsfusion.gwt.client.form.design.view.ReactContainerView::state.@lsfusion.gwt.client.form.design.view.GReactFormData.ViewState::lastPublished;
    }-*/;
    private static native Boolean loading(JavaScriptObject data, String name) /*-{ return data[name].loading; }-*/;

    // a change of a property React draws, made while the form does not show it - the change of another property
    // predicted for it, the classic changeProperty -, marks no `loading`, as the platform's panel, which has no view of
    // it then, marks none: the answer takes `loading` off only what the form shows, so a mark made while it was hidden
    // stayed once it was shown
    public void testAPropertyReactDrawsMarksNoLoadingWhileHidden() {
        GForm form = form("hiddenLoading");
        GContainer board = container(2, "board", form.mainContainer); // NEW board { custom = 'Board'; MOVE PROPERTY(note()); }
        board.custom = "Board"; board.react = true;
        GPropertyDraw note = column(form, 20, "note", null);
        note.isList = false;
        note.container = board; board.children.add(note);
        form.initialFormChanges = GFormChangesTest.noChanges(); // the form does not show it yet
        GFormController controller = new GFormController(null, null, formContainer(), form, false, "hiddenLoading", 0, null);
        Object view = controller.getFormLayout().getContainerView(board);

        controller.setLoadingValueAt(note, GGroupObjectValue.EMPTY, PValue.UNDEFINED, 1); // a change of it, request 1
        controller.applyRemoteChanges(new GFormChanges(), 1); // ... answered
        GFormChanges shown = new GFormChanges();
        NativeHashMap<GGroupObjectValue, PValue> values = new NativeHashMap<>();
        values.put(GGroupObjectValue.EMPTY, PValue.getPValue(5));
        shown.properties.put(note, values);
        controller.applyRemoteChanges(shown, 2); // ... and then shown
        assertNull(loading(published(view), "note"));
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

    // the selected column of a grid the platform draws gone - hidden by its SHOWIF - leaves the selection on the nearest
    // column that takes the focus, those on its left first: a, b being FOCUSABLE FALSE. It was moved only to the column
    // just on the left, and stayed where it was where that one takes no focus - past the grid's end, the gone column the
    // last one: what asked for the current property next - a filter, the sum, a pivot, a key in the grid - read the
    // selected cell out of bounds (IndexOutOfBoundsException)
    public void testASelectedColumnGoneLeavesTheSelectionOnTheNearestColumnThatTakesTheFocus() {
        ColumnsForm columns = new ColumnsForm("a", "-b", "c");
        columns.choose("c");
        columns.hide("c");
        assertEquals("a", columns.selected());
    }
    // ... and so do two columns gone in one answer, the selected one the last - whichever of them goes first
    public void testTwoColumnsGoneAtOnceLeaveTheSelectionOnTheNearestColumnThatTakesTheFocus() {
        ColumnsForm columns = new ColumnsForm("a", "-b", "c", "d");
        columns.choose("d");
        columns.hide("c", "d");
        assertEquals("a", columns.selected());
    }
    // ... and with no column left that takes the focus, the selection stays in the grid: on the last column
    public void testASelectedColumnGoneLeavesTheSelectionInTheGridWhereNoColumnTakesTheFocus() {
        ColumnsForm columns = new ColumnsForm("-a", "-b", "c");
        columns.choose("c");
        columns.hide("c");
        assertEquals("b", columns.selected());
    }
    // a column gone on the left of the selected one leaves the selection on it. The grid moves the columns that stay to
    // their new places before it removes the gone ones, and a move shifted the selection as if the moved column were
    // removed only: here it ended on d, which typing then edited
    public void testAColumnGoneOnTheLeftLeavesTheSelectionOnItsColumn() {
        ColumnsForm columns = new ColumnsForm("a", "b", "c", "d", "e");
        columns.choose("e");
        columns.hide("b");
        assertEquals("e", columns.selected());
    }
    // the selected column gone with columns on its right leaves the selection on its neighbour as the columns stood -
    // not on the column that took its place (c, here FOCUSABLE FALSE, where the selection ended), nor on the last
    // column that stays (where the removal would give it: the moves leave the gone columns at the grid's end) -, and
    // the form is told
    public void testASelectedColumnGoneLeavesTheSelectionOnItsNeighbourAsTheColumnsStood() {
        ColumnsForm columns = new ColumnsForm("a", "b", "-c", "d");
        columns.choose("b");
        columns.hide("b");
        assertEquals("a", columns.selected());
        assertEquals("a", columns.active());
    }
    // ... on its right where none on its left takes the focus
    public void testASelectedColumnGoneLeavesTheSelectionOnItsRightNeighbourWhereNoneOnItsLeftTakesTheFocus() {
        ColumnsForm columns = new ColumnsForm("-a", "b", "c", "d");
        columns.choose("b");
        columns.hide("b");
        assertEquals("c", columns.selected());
        assertEquals("c", columns.active());
    }
    // a column gone on the left of a selected range of columns leaves the range on its columns - what a copy and a
    // paste take. A move shifted an end on the moved column as if that column were removed: here b..d ended as d..d
    public void testAColumnGoneOnTheLeftLeavesASelectedRangeOnItsColumns() {
        ColumnsForm columns = new ColumnsForm("a", "b", "c", "d");
        columns.choose("b");
        columns.chooseUpTo("d");
        columns.hide("a");
        assertEquals("b..d", columns.range());
    }
    // a form of one grid the platform draws, of two rows, the first current, with a column of each property named -
    // FOCUSABLE FALSE where the name starts with "-" -, each one with a SHOWIF of its own; the grid not on the page, the
    // form's requests sent through a dispatch of its own
    private static final class ColumnsForm {
        final GForm form = form("columnsGone");
        final GGroupObject group = platformGrid(form, 90, "r");
        final GFormController controller;

        ColumnsForm(String... names) {
            int id = 95;
            for (String name : names) {
                GPropertyDraw property = column(form, id++, name.replace("-", ""), group);
                property.cellType = property.valueType; // as the server gives both: its cells are drawn
                if (name.startsWith("-"))
                    property.focusable = false;
                property.showIfReader = new GShowIfReader(property.ID, group.ID);
            }
            form.initialFormChanges = GFormChangesTest.noChanges();
            controller = controllerApart(form);
            GFormChanges shown = new GFormChanges();
            GGroupObjectValue first = new GGroupObjectValue(group.ID, 1);
            shown.gridObjects.put(group, new ArrayList<>(Arrays.asList(first, new GGroupObjectValue(group.ID, 2))));
            shown.objects.put(group, first);
            for (GPropertyDraw property : form.propertyDraws)
                shown.properties.put(property, new NativeHashMap<>());
            controller.applyRemoteChanges(shown, 1);
        }
        GPropertyDraw property(String name) {
            for (GPropertyDraw property : form.propertyDraws)
                if (property.sID.equals(name))
                    return property;
            throw new IllegalArgumentException(name);
        }
        GGridTable grid() {
            return platformGridView(controller, group);
        }
        // a column chosen, as a click on its cell chooses it
        void choose(String name) {
            chooseColumn(grid(), grid().getGridColumnIndex(property(name), GGroupObjectValue.EMPTY), FocusUtils.Reason.MOUSENAVIGATE, null);
        }
        // ... with the shift key held: the columns from the one chosen before to it, a range, selected
        void chooseUpTo(String name) {
            chooseColumn(grid(), grid().getGridColumnIndex(property(name), GGroupObjectValue.EMPTY), FocusUtils.Reason.MOUSENAVIGATE,
                    Document.get().createClickEvent(0, 0, 0, 0, 0, false, false, true, false));
        }
        // columns hidden by their SHOWIFs, in one answer of the server's
        void hide(String... names) {
            GFormChanges hidden = new GFormChanges();
            for (String name : names) {
                NativeHashMap<GGroupObjectValue, PValue> showIf = new NativeHashMap<>();
                showIf.put(GGroupObjectValue.EMPTY, null);
                hidden.properties.put(property(name).showIfReader, showIf);
            }
            controller.applyRemoteChanges(hidden, 2);
        }
        // the property of the selected column - what a filter, the sum, a key in the grid read it by
        String selected() {
            return grid().getSelectedProperty().sID;
        }
        // ... and the one the form was told last, by the grid's column handler, as the server's active property
        String active() {
            return activeProperty(controller).sID;
        }
        // the selected range of columns, from the column it was started at to the one it ends at
        String range() {
            GGridTable grid = grid();
            return grid.getProperty(rangeEnd(grid, true)).sID + ".." + grid.getProperty(rangeEnd(grid, false)).sID;
        }
    }
    private static native GPropertyDraw activeProperty(GFormController controller) /*-{
        return controller.@lsfusion.gwt.client.form.controller.GFormController::prevPropertyActive;
    }-*/;
    private static native int rangeEnd(GGridTable grid, boolean start) /*-{
        return start ? grid.@lsfusion.gwt.client.base.view.grid.DataGrid::startSelectColumn : grid.@lsfusion.gwt.client.base.view.grid.DataGrid::endSelectColumn;
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
            controller.getRowsPanelController(rows.qty).place("i.qty", rows.one, rows.qty, hostOne.getElement());
            controller.getRowsPanelController(rows.qty).place("i.qty", rows.two, rows.qty, hostTwo.getElement());

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
            controller.getRowsPanelController(rows.qty).place("i.qty", rows.one, rows.qty, hostOne.getElement());
            controller.getRowsPanelController(rows.qty).place("i.qty", rows.two, rows.qty, hostTwo.getElement());

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
            controller.getRowsPanelController(rows.qty).place("i.qty", rows.one, rows.qty, host.getElement());
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
            controller.getRowsPanelController(rows.qty).place("i.qty", rows.one, rows.qty, host.getElement());
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
            controller.getRowsPanelController(rows.qty).place("i.qty", rows.one, rows.qty, host.getElement());
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
                controller.getRowsPanelController(rows.qty).place("i.qty", rows.one, rows.qty, earlierHost.getElement());
                controller.getRowsPanelController(later).place("i." + later.integrationSID, rows.one, later, laterHost.getElement());
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
            controller.getRowsPanelController(rows.qty).place("i.qty", rows.one, rows.qty, host.getElement());
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
                controller.getRowsPanelController(rows.qty).place("i.qty", rows.one, rows.qty, earlierHost.getElement());
                controller.getRowsPanelController(later).place("i." + later.integrationSID, rows.one, later, laterHost.getElement());
                assertTrue(press(controller, earlierHost.getElement(), 113));
                assertTrue(pressed(laterHost.getElement()));
                assertFalse(pressed(earlierHost.getElement()));
            } finally {
                RootPanel.get().remove(earlierHost); RootPanel.get().remove(laterHost);
            }
        }
    }
    // a row host can change size without the form resizing: only the renderer in that host must measure again
    public void testResizingARowHostResizesOnlyItsRenderer() {
        JsniTestSupport.installResizeObserver();
        RowsForm rows = new RowsForm();
        NativeHashMap<GGroupObjectValue, RowResizePanel> counters = new NativeHashMap<>();
        GPropertyDraw property = rows.perRow(column(rows.form, 23, "resized", rows.group, new GPropertyDraw() {
            @Override
            public PanelRenderer createPanelRenderer(GFormController form, ActionOrPropertyValueController controller,
                                                     GGroupObjectValue columnKey, GGroupObjectValue rowKey,
                                                     lsfusion.gwt.client.base.Result<CaptionWidget> captionContainer) {
                return new PropertyPanelRenderer(form, controller, this, columnKey, rowKey, captionContainer) {
                    private final RowResizePanel panel = new RowResizePanel();
                    private final ComponentViewWidget view = new SizedWidget(panel).view;
                    {
                        panel.add(super.getComponentViewWidget().getSingleWidget().widget);
                        counters.put(rowKey, panel);
                    }
                    @Override
                    public ComponentViewWidget getComponentViewWidget() {
                        return view;
                    }
                };
            }
        }), new GInputBindingEvent(null, ANY));
        rows.deliver(property);
        GFormController controller = rows.controller();
        GPlatformRowsPanelController panel = controller.getRowsPanelController(property);
        FlowPanel hostOne = new FlowPanel(), hostTwo = new FlowPanel();
        RootPanel.get().add(hostOne); RootPanel.get().add(hostTwo);
        panel.place("i." + property.integrationSID, rows.one, property, hostOne.getElement());
        panel.place("i." + property.integrationSID, rows.two, property, hostTwo.getElement());
        assertTrue(hostOne.getElement().isOrHasChild(counters.get(rows.one).getElement()));
        assertTrue(hostTwo.getElement().isOrHasChild(counters.get(rows.two).getElement()));

        delayTestFinish(5000);
        Scheduler.get().scheduleDeferred(() -> {
            int beforeOne = counters.get(rows.one).resizes, beforeTwo = counters.get(rows.two).resizes;
            JsniTestSupport.resize(hostOne.getElement());
            Scheduler.get().scheduleDeferred(() -> {
                try {
                    assertTrue("resizing the first row's host reaches its renderer", counters.get(rows.one).resizes > beforeOne);
                    assertEquals("resizing one row leaves the other renderer alone", beforeTwo, counters.get(rows.two).resizes);
                    finishTest();
                } finally {
                    panel.unplace(rows.one, property, hostOne.getElement());
                    panel.unplace(rows.two, property, hostTwo.getElement());
                    RootPanel.get().remove(hostOne); RootPanel.get().remove(hostTwo);
                }
            });
        });
    }
    private static final class RowResizePanel extends FlowPanel implements RequiresResize {
        int resizes;
        @Override
        public void onResize() {
            resizes++;
        }
    }

    // a row gone makes a host invalid even while its first host waits; the invalid one must never take over
    public void testASecondHostOfARowGoneIsNotQueued() {
        RowsForm rows = new RowsForm();
        GFormController controller = rows.controller();
        GPlatformRowsPanelController panel = controller.getRowsPanelController(rows.qty);
        FlowPanel first = new FlowPanel(), second = new FlowPanel();
        RootPanel.get().add(first); RootPanel.get().add(second);
        try {
            assertTrue(panel.place("i.qty", rows.one, rows.qty, first.getElement()));
            rows.keys(controller, rows.two);
            assertFalse(panel.place("i.qty", rows.one, rows.qty, second.getElement()));
            assertEquals("lsFusion: the row given to 'qty' is not in the group any more", second.getElement().getInnerText());

            rows.keys(controller, rows.one, rows.two);
            rows.values(controller, rows.one, rows.two);
            Element element = assertRowRenderer(controller, rows, rows.one, first, 1);
            panel.unplace(rows.one, rows.qty, first.getElement());
            assertFalse(second.getElement().isOrHasChild(element));
            assertTrue(controller.getFormLayout().attachContainer.getElement().isOrHasChild(element));
            panel.unplace(rows.one, rows.qty, second.getElement());
            assertEquals("", second.getElement().getInnerText());
        } finally {
            RootPanel.get().remove(first); RootPanel.get().remove(second);
        }
    }

    // a host registered before the property arrives waits for its row's renderer, and receives that row's value
    public void testARowHostPlacedBeforeItsRendererReceivesItsOwnValue() {
        RowsForm rows = new RowsForm();
        rows.deliver(); // the rows arrive as the form opens, but the property has not arrived yet
        GFormController controller = rows.controller();
        GPlatformRowsPanelController panel = controller.getRowsPanelController(rows.qty);
        FlowPanel hostOne = new FlowPanel(), hostTwo = new FlowPanel();
        RootPanel.get().add(hostOne); RootPanel.get().add(hostTwo);
        try {
            assertTrue(panel.place("i.qty", rows.one, rows.qty, hostOne.getElement()));
            assertTrue(panel.place("i.qty", rows.two, rows.qty, hostTwo.getElement()));
            assertEquals(0, hostOne.getElement().getChildCount());
            assertEquals(0, hostTwo.getElement().getChildCount());
            assertNull(rowRenderer(panel, rows.qty, rows.one));
            assertNull(rowRenderer(panel, rows.qty, rows.two));

            rows.values(controller, rows.one, rows.two);
            Element one = assertRowRenderer(controller, rows, rows.one, hostOne, 1);
            Element two = assertRowRenderer(controller, rows, rows.two, hostTwo, 2);
            assertNotSame(one, two);
        } finally {
            RootPanel.get().remove(hostOne); RootPanel.get().remove(hostTwo);
        }
    }
    // a renderer made before React renders its host waits in the form's park, then moves with its value intact
    public void testARowRendererPlacedIntoALaterHostKeepsItsOwnValue() {
        RowsForm rows = new RowsForm();
        GFormController controller = rows.controller();
        GPlatformRowsPanelController panel = controller.getRowsPanelController(rows.qty);
        PanelRenderer one = rowRenderer(panel, rows.qty, rows.one), two = rowRenderer(panel, rows.qty, rows.two);
        Element oneElement = rowValue(one).getElement(), twoElement = rowValue(two).getElement();
        Element park = controller.getFormLayout().attachContainer.getElement();
        assertTrue(park.isOrHasChild(oneElement));
        assertTrue(park.isOrHasChild(twoElement));
        FlowPanel hostOne = new FlowPanel(), hostTwo = new FlowPanel();
        RootPanel.get().add(hostOne); RootPanel.get().add(hostTwo);
        try {
            assertTrue(panel.place("i.qty", rows.two, rows.qty, hostTwo.getElement()));
            assertTrue(panel.place("i.qty", rows.one, rows.qty, hostOne.getElement()));
            assertSame(oneElement, assertRowRenderer(controller, rows, rows.one, hostOne, 1));
            assertSame(twoElement, assertRowRenderer(controller, rows, rows.two, hostTwo, 2));
            assertFalse(park.isOrHasChild(oneElement));
            assertFalse(park.isOrHasChild(twoElement));
        } finally {
            RootPanel.get().remove(hostOne); RootPanel.get().remove(hostTwo);
        }
    }
    // a second host of a row's renderer is refused while the first holds it, and once the first is taken away the
    // eldest host still waiting takes the renderer, the mistake it showed gone - not one the view took away meanwhile,
    // nor one no longer in the page (removed with the first, in one render) -, and the next one in line once that one
    // goes
    public void testAHostRefusedARowsRendererTakesItOnceTheFirstGoes() {
        RowsForm rows = new RowsForm();
        GFormController controller = rows.controller();
        GPlatformRowsPanelController panel = controller.getRowsPanelController(rows.qty);
        FlowPanel first = new FlowPanel(), withdrawn = new FlowPanel(), removed = new FlowPanel(), next = new FlowPanel(),
                last = new FlowPanel();
        FlowPanel[] hosts = {first, withdrawn, removed, next, last};
        for (FlowPanel host : hosts)
            RootPanel.get().add(host);
        try {
            assertTrue(panel.place("i.qty", rows.one, rows.qty, first.getElement()));
            for (FlowPanel host : new FlowPanel[] {withdrawn, removed, next, last}) {
                assertFalse(panel.place("i.uiCustomViewQuantity", rows.one, rows.qty, host.getElement()));
                assertEquals("lsFusion: 'i.uiCustomViewQuantity' is already placed by another <Platform>", host.getElement().getInnerText());
            }
            Element one = assertRowRenderer(controller, rows, rows.one, first, 1);

            panel.unplace(rows.one, rows.qty, withdrawn.getElement());
            assertEquals("", withdrawn.getElement().getInnerText());
            RootPanel.get().remove(removed);
            panel.unplace(rows.one, rows.qty, first.getElement());
            assertSame(one, assertRowRenderer(controller, rows, rows.one, next, 1));
            assertFalse(next.getElement().getInnerText(), next.getElement().getInnerText().contains("already placed"));
            assertFalse(withdrawn.getElement().isOrHasChild(one));
            assertFalse(removed.getElement().isOrHasChild(one));
            assertFalse(last.getElement().isOrHasChild(one));

            panel.unplace(rows.one, rows.qty, next.getElement());
            assertSame(one, assertRowRenderer(controller, rows, rows.one, last, 1));
        } finally {
            for (FlowPanel host : hosts)
                RootPanel.get().remove(host);
        }
    }
    // changing the second row through its renderer sends that row's key even while the first row is current;
    // both the optimistic value and the server's answer leave the first row alone
    public void testAChangeThroughARowRendererChangesOnlyThatRow() {
        RowsForm rows = new RowsForm();
        rows.form.sessionID = "rowChanges" + (++changesForms);
        rows.qty.hasChangeAction = true;
        rows.qty.asyncExecMap = Collections.emptyMap();
        rows.qty.externalChangeType = GIntegerType.instance;
        ChangeServer edits = new ChangeServer(new GFormController(formsController(), null, formContainer(), rows.form, false, "rowChanges", 0, null), rows.qty);
        server = edits;
        GFormController controller = edits.controller;
        GPlatformRowsPanelController panel = controller.getRowsPanelController(rows.qty);
        FlowPanel hostOne = new FlowPanel(), hostTwo = new FlowPanel();
        RootPanel.get().add(hostOne); RootPanel.get().add(hostTwo);
        try {
            panel.place("i.qty", rows.one, rows.qty, hostOne.getElement());
            panel.place("i.qty", rows.two, rows.qty, hostTwo.getElement());
            Element one = assertRowRenderer(controller, rows, rows.one, hostOne, 1);
            Element two = assertRowRenderer(controller, rows, rows.two, hostTwo, 2);
            rowValue(rowRenderer(panel, rows.qty, rows.two)).changeProperty(PValue.getPValue(7), null);

            assertEquals(1, edits.actions.size());
            ExecuteEventAction request = (ExecuteEventAction) edits.actions.get(0);
            assertEquals(GEditBindingMap.CHANGE, request.actionSID);
            assertEquals(1, request.propertyIds.length);
            assertEquals(rows.qty.ID, request.propertyIds[0]);
            assertEquals(rows.two, request.fullKeys[0]);
            assertValue(7, ((GPushAsyncInput) request.pushAsyncResults[0]).result.getPValue());
            assertSame(one, assertRowRenderer(controller, rows, rows.one, hostOne, 1));
            assertSame(two, assertRowRenderer(controller, rows, rows.two, hostTwo, 7));

            GFormChangesDTO answer = GFormChangesTest.noChanges();
            answer.properties = new GPropertyReaderDTO[] {new GPropertyReaderDTO(rows.qty.ID, DRAW, -1)};
            answer.requestIndex = (int) request.requestIndex;
            answer.propertiesValueKeys = new GGroupObjectValue[][] {{rows.one, rows.two}};
            answer.propertiesValueValues = new Serializable[][] {{1, 9}};
            edits.callbacks.get(0).onSuccess(new ServerResponseResult(new GAction[] {new GProcessFormChangesAction(answer)}, request.requestIndex, false));
            assertSame(one, assertRowRenderer(controller, rows, rows.one, hostOne, 1));
            assertSame(two, assertRowRenderer(controller, rows, rows.two, hostTwo, 9));
            assertEquals(rows.one, controller.getGroupController(rows.group).getSelectedKey());
        } finally {
            RootPanel.get().remove(hostOne); RootPanel.get().remove(hostTwo);
        }
    }
    // Cancelling an async input reports no request, so the platform must leave no loading mark behind.
    public void testACancelledAsyncInputReportsNoRequestWithoutLoading() {
        RowsForm rows = new RowsForm();
        rows.qty.inputType = new GInputType("number");
        rows.qty.asyncExecMap = Collections.singletonMap(GEditBindingMap.CHANGE,
                new GAsyncInput(GIntegerType.instance, false, null, null, null));
        GFormController controller = rows.controller();
        GPlatformRowsPanelController panel = controller.getRowsPanelController(rows.qty);
        FlowPanel host = new FlowPanel();
        RootPanel.get().add(host);
        try {
            panel.place("i.qty", rows.one, rows.qty, host.getElement());
            ActionOrPropertyPanelValue value = rowValue(rowRenderer(panel, rows.qty, rows.one));
            ArrayList<Long> requests = new ArrayList<>();
            int[] cancelled = {0};
            EventHandler handler = new EventHandler(Event.as(Document.get().createDblClickEvent(0, 0, 0, 0, 0, false, false, false, false)));
            controller.executePropertyEventAction(handler, value, value, GEditBindingMap.CHANGE, GEventSource.EDIT, new ExecCallback() {
                @Override
                public void onSuccess(long index) {
                    requests.add(index);
                    controller.setLoading(value, index);
                }

                @Override
                public void onCancel() {
                    cancelled[0]++;
                }
            });
            assertTrue(controller.isEditing());
            controller.cancelEditing(CancelReason.ESCAPE_PRESSED);
            assertFalse(controller.isEditing());
            assertTrue(requests.isEmpty());
            assertEquals(1, cancelled[0]);
            assertNoPendingPropertyRequests(controller);
            assertValue(1, value.getValue());
        } finally {
            RootPanel.get().remove(host);
        }
    }

    // server rows gain and lose only their own renderers; a new host can wait before reconciliation or arrive after it,
    // and the renderers of the rows that stay keep their identity, including when their positions change
    public void testAServerRowAddedOrDeletedMovesOnlyItsRenderers() {
        for (boolean hostFirst : new boolean[] {true, false}) {
            RowsForm rows = new RowsForm();
            GFormController controller = rows.controller();
            GPlatformRowsPanelController panel = controller.getRowsPanelController(rows.qty);
            FlowPanel hostOne = new FlowPanel(), hostTwo = new FlowPanel(), hostThree = new FlowPanel();
            RootPanel.get().add(hostOne); RootPanel.get().add(hostTwo); RootPanel.get().add(hostThree);
            try {
                panel.place("i.qty", rows.one, rows.qty, hostOne.getElement());
                panel.place("i.qty", rows.two, rows.qty, hostTwo.getElement());
                Element one = assertRowRenderer(controller, rows, rows.one, hostOne, 1);
                Element two = assertRowRenderer(controller, rows, rows.two, hostTwo, 2);
                if (hostFirst) {
                    // keep the projection's publication pending, as while React is placing the newly drawn row
                    reactData(controller).change(() -> {
                        rows.keys(controller, rows.one, rows.two, rows.three);
                        assertNull(rowRenderer(panel, rows.qty, rows.three));
                        assertTrue(panel.place("i.qty", rows.three, rows.qty, hostThree.getElement()));
                    });
                } else {
                    rows.keys(controller, rows.one, rows.two, rows.three);
                    assertTrue(controller.getFormLayout().attachContainer.getElement().isOrHasChild(rowValue(rowRenderer(panel, rows.qty, rows.three)).getElement()));
                    panel.place("i.qty", rows.three, rows.qty, hostThree.getElement());
                }
                rows.values(controller, rows.one, rows.two, rows.three);
                Element three = assertRowRenderer(controller, rows, rows.three, hostThree, 3);
                assertSame(one, assertRowRenderer(controller, rows, rows.one, hostOne, 1));
                assertSame(two, assertRowRenderer(controller, rows, rows.two, hostTwo, 2));

                rows.keys(controller, rows.three, rows.one); // two is deleted; both staying rows move
                assertEquals(0, hostTwo.getElement().getChildCount());
                assertNull(two.getParentElement());
                assertNull(rowRenderer(panel, rows.qty, rows.two));
                assertSame(one, assertRowRenderer(controller, rows, rows.one, hostOne, 1));
                assertSame(three, assertRowRenderer(controller, rows, rows.three, hostThree, 3));
                assertFalse(panel.place("i.qty", rows.two, rows.qty, hostTwo.getElement()));
                assertTrue(hostTwo.getElement().getInnerText().contains("is not in the group any more"));
                assertFalse(hostTwo.getElement().isOrHasChild(two));
            } finally {
                RootPanel.get().remove(hostOne); RootPanel.get().remove(hostTwo); RootPanel.get().remove(hostThree);
            }
        }
    }
    // the next page replaces every old row: old hosts empty, new hosts get new renderers and the new rows' values
    public void testANextPageReplacesTheRowRenderers() {
        RowsForm rows = new RowsForm();
        GFormController controller = rows.controller();
        GPlatformRowsPanelController panel = controller.getRowsPanelController(rows.qty);
        FlowPanel hostOne = new FlowPanel(), hostTwo = new FlowPanel(), hostThree = new FlowPanel(), hostFour = new FlowPanel();
        RootPanel.get().add(hostOne); RootPanel.get().add(hostTwo); RootPanel.get().add(hostThree); RootPanel.get().add(hostFour);
        try {
            panel.place("i.qty", rows.one, rows.qty, hostOne.getElement());
            panel.place("i.qty", rows.two, rows.qty, hostTwo.getElement());
            Element one = assertRowRenderer(controller, rows, rows.one, hostOne, 1);
            Element two = assertRowRenderer(controller, rows, rows.two, hostTwo, 2);
            rows.keys(controller, rows.three, rows.four);
            rows.values(controller, rows.three, rows.four);
            assertEquals(0, hostOne.getElement().getChildCount());
            assertEquals(0, hostTwo.getElement().getChildCount());
            assertNull(one.getParentElement());
            assertNull(two.getParentElement());
            assertNull(rowRenderer(panel, rows.qty, rows.one));
            assertNull(rowRenderer(panel, rows.qty, rows.two));
            panel.place("i.qty", rows.three, rows.qty, hostThree.getElement());
            panel.place("i.qty", rows.four, rows.qty, hostFour.getElement());
            Element three = assertRowRenderer(controller, rows, rows.three, hostThree, 3);
            Element four = assertRowRenderer(controller, rows, rows.four, hostFour, 4);
            assertNotSame(one, three); assertNotSame(two, three);
            assertNotSame(one, four); assertNotSame(two, four);
            assertNotSame(three, four);
        } finally {
            RootPanel.get().remove(hostOne); RootPanel.get().remove(hostTwo); RootPanel.get().remove(hostThree); RootPanel.get().remove(hostFour);
        }
    }
    // unmounting a host parks its renderer; mounting the row elsewhere moves the same element with its value intact
    public void testARowUnplacedAndPlacedAgainKeepsItsRenderer() {
        RowsForm rows = new RowsForm();
        GFormController controller = rows.controller();
        GPlatformRowsPanelController panel = controller.getRowsPanelController(rows.qty);
        FlowPanel first = new FlowPanel(), next = new FlowPanel();
        RootPanel.get().add(first); RootPanel.get().add(next);
        try {
            panel.place("i.qty", rows.one, rows.qty, first.getElement());
            Element element = assertRowRenderer(controller, rows, rows.one, first, 1);
            PanelRenderer renderer = rowRenderer(panel, rows.qty, rows.one);
            panel.unplace(rows.one, rows.qty, first.getElement());
            assertEquals(0, first.getElement().getChildCount());
            assertTrue(controller.getFormLayout().attachContainer.getElement().isOrHasChild(element));
            assertSame(renderer, rowRenderer(panel, rows.qty, rows.one));
            panel.place("i.qty", rows.one, rows.qty, next.getElement());
            assertSame(element, assertRowRenderer(controller, rows, rows.one, next, 1));
            assertSame(renderer, rowRenderer(panel, rows.qty, rows.one));
        } finally {
            RootPanel.get().remove(first); RootPanel.get().remove(next);
        }
    }
    // a second host for the same row and property is refused, then takes over without another place call
    public void testASecondRowHostTakesOverWhenTheFirstIsUnplaced() {
        RowsForm rows = new RowsForm();
        GFormController controller = rows.controller();
        GPlatformRowsPanelController panel = controller.getRowsPanelController(rows.qty);
        FlowPanel first = new FlowPanel(), second = new FlowPanel();
        RootPanel.get().add(first); RootPanel.get().add(second);
        try {
            panel.place("i.qty", rows.one, rows.qty, first.getElement());
            Element element = assertRowRenderer(controller, rows, rows.one, first, 1);
            assertFalse(panel.place("i.qty", rows.one, rows.qty, second.getElement()));
            assertSame(element, assertRowRenderer(controller, rows, rows.one, first, 1));
            assertEquals("lsFusion: 'i.qty' is already placed by another <Platform>", second.getElement().getInnerText());
            assertTrue(Document.get().getBody().isOrHasChild(second.getElement()));
            panel.unplace(rows.one, rows.qty, first.getElement());
            assertEquals(0, first.getElement().getChildCount());
            assertSame(element, assertRowRenderer(controller, rows, rows.one, second, 1));
        } finally {
            RootPanel.get().remove(first); RootPanel.get().remove(second);
        }
    }
    // SHOWIF removes the property's renderers from every host; showing it restores each row without placing again
    public void testAShowIfHiddenRowPropertyReturnsToItsHosts() {
        RowsForm rows = new RowsForm();
        GFormController controller = rows.controller();
        GPlatformRowsPanelController panel = controller.getRowsPanelController(rows.qty);
        FlowPanel hostOne = new FlowPanel(), hostTwo = new FlowPanel();
        RootPanel.get().add(hostOne); RootPanel.get().add(hostTwo);
        try {
            panel.place("i.qty", rows.one, rows.qty, hostOne.getElement());
            panel.place("i.qty", rows.two, rows.qty, hostTwo.getElement());
            assertRowRenderer(controller, rows, rows.one, hostOne, 1);
            assertRowRenderer(controller, rows, rows.two, hostTwo, 2);
            rows.showIf(controller, false);
            assertEquals(0, hostOne.getElement().getChildCount());
            assertEquals(0, hostTwo.getElement().getChildCount());
            assertNull(rowRenderer(panel, rows.qty, rows.one));
            assertNull(rowRenderer(panel, rows.qty, rows.two));
            rows.showIf(controller, true);
            assertRowRenderer(controller, rows, rows.one, hostOne, 1);
            assertRowRenderer(controller, rows, rows.two, hostTwo, 2);
        } finally {
            RootPanel.get().remove(hostOne); RootPanel.get().remove(hostTwo);
        }
    }
    // a property dropped outright also takes out its renderers, but keeps the hosts for its next whole delivery
    public void testADroppedRowPropertyReturnsToItsHosts() {
        RowsForm rows = new RowsForm();
        GFormController controller = rows.controller();
        GPlatformRowsPanelController panel = controller.getRowsPanelController(rows.qty);
        FlowPanel hostOne = new FlowPanel(), hostTwo = new FlowPanel();
        RootPanel.get().add(hostOne); RootPanel.get().add(hostTwo);
        try {
            panel.place("i.qty", rows.one, rows.qty, hostOne.getElement());
            panel.place("i.qty", rows.two, rows.qty, hostTwo.getElement());
            Element one = assertRowRenderer(controller, rows, rows.one, hostOne, 1);
            Element two = assertRowRenderer(controller, rows, rows.two, hostTwo, 2);
            GFormChanges dropped = new GFormChanges();
            dropped.dropProperties.add(rows.qty);
            controller.applyRemoteChanges(dropped, 1);
            assertEquals(0, hostOne.getElement().getChildCount());
            assertEquals(0, hostTwo.getElement().getChildCount());
            assertNull(one.getParentElement());
            assertNull(two.getParentElement());
            assertNull(rowRenderer(panel, rows.qty, rows.one));
            assertNull(rowRenderer(panel, rows.qty, rows.two));
            rows.values(controller, rows.one, rows.two);
            assertRowRenderer(controller, rows, rows.one, hostOne, 1);
            assertRowRenderer(controller, rows, rows.two, hostTwo, 2);
        } finally {
            RootPanel.get().remove(hostOne); RootPanel.get().remove(hostTwo);
        }
    }
    // focus follows the group's current row, whether the server or the React side chooses it, regardless of key order
    public void testARowPanelFocusFollowsTheCurrentRow() {
        RowsForm rows = new RowsForm();
        GFormController controller = rows.controller();
        GPlatformRowsPanelController panel = controller.getRowsPanelController(rows.qty);
        ArrayList<GGroupObjectValue> reversed = new ArrayList<>(Arrays.asList(rows.two, rows.one));
        assertEquals(rows.one, panel.getFocusKey(reversed));
        GFormChanges current = new GFormChanges();
        current.objects.put(rows.group, rows.two);
        controller.applyRemoteChanges(current, 1);
        assertEquals(rows.two, panel.getFocusKey(new ArrayList<>(Arrays.asList(rows.one, rows.two))));
        controller.getGroupController(rows.group).changeCurrentKey(rows.one);
        assertEquals(rows.one, panel.getFocusKey(reversed));
    }
    // optimistic rows live in GReactFormData before any server answer; publishing their add and delete also reconciles
    // the platform's renderers, without replacing those of the rows that stay
    public void testAReactOptimisticRowAddAndDeleteMovesTheRenderers() {
        RowsForm rows = new RowsForm();
        GFormController controller = rows.controller();
        GPlatformRowsPanelController panel = controller.getRowsPanelController(rows.qty);
        FlowPanel hostOne = new FlowPanel(), hostTwo = new FlowPanel(), hostThree = new FlowPanel();
        RootPanel.get().add(hostOne); RootPanel.get().add(hostTwo); RootPanel.get().add(hostThree);
        try {
            panel.place("i.qty", rows.one, rows.qty, hostOne.getElement());
            panel.place("i.qty", rows.two, rows.qty, hostTwo.getElement());
            Element one = assertRowRenderer(controller, rows, rows.one, hostOne, 1);
            Element two = assertRowRenderer(controller, rows, rows.two, hostTwo, 2);
            reactData(controller).change(() -> controller.getGroupController(rows.group).modifyGroupObject(rows.three, true, 0));
            PanelRenderer added = rowRenderer(panel, rows.qty, rows.three);
            assertNotNull(added);
            assertTrue(controller.getFormLayout().attachContainer.getElement().isOrHasChild(rowValue(added).getElement()));
            panel.place("i.qty", rows.three, rows.qty, hostThree.getElement());
            assertTrue(hostThree.getElement().isOrHasChild(rowValue(added).getElement()));
            assertNull(rowValue(added).getValue()); // the server has sent no value for the optimistic row
            assertEquals(rows.three, panel.getFocusKey(new ArrayList<>(Arrays.asList(rows.one, rows.two, rows.three))));
            assertSame(one, assertRowRenderer(controller, rows, rows.one, hostOne, 1));
            assertSame(two, assertRowRenderer(controller, rows, rows.two, hostTwo, 2));

            reactData(controller).change(() -> controller.getGroupController(rows.group).modifyGroupObject(rows.three, false, -1));
            assertEquals(0, hostThree.getElement().getChildCount());
            assertNull(rowValue(added).getElement().getParentElement());
            assertNull(rowRenderer(panel, rows.qty, rows.three));
            assertSame(one, assertRowRenderer(controller, rows, rows.one, hostOne, 1));
            assertSame(two, assertRowRenderer(controller, rows, rows.two, hostTwo, 2));
        } finally {
            RootPanel.get().remove(hostOne); RootPanel.get().remove(hostTwo); RootPanel.get().remove(hostThree);
        }
    }
    // the renderer filed under this row, read from the property's controller, the only record of it
    private static native PanelRenderer rowRenderer(GPlatformRowsPanelController panel, GPropertyDraw property, GGroupObjectValue key) /*-{
        var controller = panel.@lsfusion.gwt.client.form.object.panel.controller.GPlatformPanelController::propertyControllers.@lsfusion.gwt.client.base.jsni.NativeSIDMap::get(*)(property);
        return controller ? controller.@lsfusion.gwt.client.form.object.panel.controller.GPropertyPanelController::getRenderer(*)(key) : null;
    }-*/;
    private static native ActionOrPropertyPanelValue rowValue(PanelRenderer renderer) /*-{
        return renderer.@lsfusion.gwt.client.form.property.panel.view.PanelRenderer::value;
    }-*/;
    private static Element assertRowRenderer(GFormController controller, RowsForm rows, GGroupObjectValue key, FlowPanel host, int expected) {
        PanelRenderer renderer = rowRenderer(controller.getRowsPanelController(rows.qty), rows.qty, key);
        assertNotNull("the row has a renderer", renderer);
        ActionOrPropertyPanelValue value = rowValue(renderer);
        assertTrue("the row's renderer is in its host", host.getElement().isOrHasChild(value.getElement()));
        assertEquals(key, value.getFullKey());
        assertValue(expected, value.getValue());
        return value.getElement();
    }
    private static native GReactFormData reactData(GFormController controller) /*-{
        return controller.@lsfusion.gwt.client.form.controller.GFormController::reactData;
    }-*/;

    // the modes the server gives a binding with none of its own
    private static final GBindingEnv ANY = new GBindingEnv(null, null, null, null, null, null, null, null, null);
    // a form of a grid its own React view draws, the platform's editor of qty in each of its two rows (<Platform
    // name="items.qty" row/>), qty with the binding given, the rows and their values as the server sends them
    private static final class RowsForm {
        final GForm form = leafGridForm();
        final GGroupObject group = form.groupObjects.get(0);
        final GPropertyDraw qty = column(form, 22, "qty", group);
        final GGroupObjectValue one = new GGroupObjectValue(10, 1), two = new GGroupObjectValue(10, 2);
        final GGroupObjectValue three = new GGroupObjectValue(10, 3), four = new GGroupObjectValue(10, 4);

        RowsForm() {
            this(new GInputBindingEvent(null, ANY));
            qty.showIfReader = new GShowIfReader(qty.ID, group.ID);
        }
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
        // another set of rows, as a server answer carries it, with no property delivery mixed in
        void keys(GFormController controller, GGroupObjectValue... keys) {
            GFormChanges changes = new GFormChanges();
            changes.gridObjects.put(group, new ArrayList<>(Arrays.asList(keys)));
            controller.applyRemoteChanges(changes, 1);
        }
        // the qty of each named row, its id here, as a whole delivery of the property
        void values(GFormController controller, GGroupObjectValue... keys) {
            GFormChanges changes = new GFormChanges();
            NativeHashMap<GGroupObjectValue, PValue> values = new NativeHashMap<>();
            for (GGroupObjectValue key : keys)
                values.put(key, PValue.getPValue((Integer) key.getValue(0)));
            changes.properties.put(qty, values);
            controller.applyRemoteChanges(changes, 1);
        }
        void showIf(GFormController controller, boolean shown) {
            GFormChanges changes = new GFormChanges();
            NativeHashMap<GGroupObjectValue, PValue> values = new NativeHashMap<>();
            values.put(GGroupObjectValue.EMPTY, shown ? PValue.getPValue(true) : null);
            changes.properties.put(qty.showIfReader, values);
            controller.applyRemoteChanges(changes, 1);
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

    // a group's update mode, whoever changes it, is shown by what shows the group's toolbar, as the form tells it - the
    // platform's toggle too, a mode it did not set itself (GFormController.changeUpdateMode)
    public void testTheFormTellsTheToolbarItsUpdateMode() {
        GFormController controller = manualUpdateGrid();
        GGroupObject group = controller.getForm().groupObjects.get(0);
        assertFalse(forceUpdateButton(controller, group).isVisible());
        controller.changeUpdateMode(group, GUpdateMode.MANUAL);
        assertTrue(forceUpdateButton(controller, group).isVisible());
        controller.changeUpdateMode(group, GUpdateMode.AUTO);
        assertFalse(forceUpdateButton(controller, group).isVisible());
    }
    // a form of one grid the platform draws, GRID(items), its toolbar with the manual update toggle - its requests sent
    // through a dispatch of its own
    private static GFormController manualUpdateGrid() {
        GForm form = leafGridForm();
        GGroupObject group = form.groupObjects.get(0);
        group.grid.custom = null; group.grid.react = false; // GRID(items) the platform's, a grid
        group.listViewType = GListViewType.GRID;
        group.grid.toolbar.showManualUpdate = true;
        return controllerApart(form);
    }
    // ... the button of the platform's toolbar of a group that brings its rows up to date: shown while the update is
    // manual, enabled while the server holds changes back
    private static native GToolbarButton forceUpdateButton(GFormController controller, GGroupObject group) /*-{
        var toolbar = controller.@lsfusion.gwt.client.form.controller.GFormController::gridToolbarControllers.@lsfusion.gwt.client.base.jsni.NativeSIDMap::get(*)(group);
        return toolbar.@lsfusion.gwt.client.form.object.table.controller.GPlatformGridToolbarController::forceUpdateTableButton;
    }-*/;

    // the update mode a group has, set again - a toggle's, a view's call - changes nothing: the button that brings its rows
    // up to date stays enabled while the server holds changes back (GFormController.changeUpdateMode)
    public void testTheModeAGroupHasSetAgainKeepsWhatTheServerHoldsBack() {
        GFormController controller = manualUpdateGrid();
        GGroupObject group = controller.getForm().groupObjects.get(0);
        controller.changeUpdateMode(group, GUpdateMode.MANUAL);
        GFormChanges heldBack = new GFormChanges(); // ... the server holding changes back
        heldBack.updateStateObjects.put(group, true);
        controller.applyRemoteChanges(heldBack, 1);
        assertTrue(forceUpdateButton(controller, group).isEnabled());
        long next = nextRequestIndex(controller);
        controller.changeUpdateMode(group, GUpdateMode.MANUAL); // ... the mode it has, again: no request
        assertTrue(forceUpdateButton(controller, group).isEnabled());
        assertEquals(next, nextRequestIndex(controller));
    }
    // the view a group's rows are shown as, set again, is no switch - for the platform's button, which switches through
    // the grid's switch, as for a view's member -: no request, the view kept, not drawn anew
    public void testTheListViewShownSetAgainIsNoSwitch() {
        GFormController controller = manualUpdateGrid();
        GGroupObject group = controller.getForm().groupObjects.get(0);
        GListViewController views = (GListViewController) tableController(controller, group.grid);
        DataGrid<?> shown = platformView(controller, group.grid);
        long next = nextRequestIndex(controller);
        ArrayList<String> told = new ArrayList<>();
        views.changeListView(GListViewType.GRID, new ExecCallback() {
            public void onSuccess(long requestIndex) { told.add("request"); }
            public void onCancel() { told.add("none"); }
        });
        assertEquals(Collections.singletonList("none"), told);
        assertEquals(next, nextRequestIndex(controller));
        assertSame(shown, platformView(controller, group.grid));
    }

    // the index the form's next request takes, read where its dispatcher keeps it
    @UnsafeNativeLong
    private static native long nextRequestIndex(GFormController controller) /*-{
        return controller.@lsfusion.gwt.client.form.controller.GFormController::dispatcher.@lsfusion.gwt.client.RemoteDispatchAsync::nextRequestIndex;
    }-*/;
    // a form's requests sent through a dispatch of its own: in the suite they go to no server and are retried
    // for minutes, which holds the page's dispatch busy for the tests after
    private static native void dispatchApart(GFormController controller) /*-{
        var dispatcher = controller.@lsfusion.gwt.client.form.controller.GFormController::dispatcher;
        dispatcher.@lsfusion.gwt.client.RemoteDispatchAsync::gwtDispatch = @lsfusion.gwt.client.controller.dispatch.GWTDispatch::new()();
    }-*/;
    private static native void dispatchApart(LogicsDispatchAsync logics) /*-{
        logics.@lsfusion.gwt.client.controller.dispatch.LogicsDispatchAsync::gwtDispatch = @lsfusion.gwt.client.controller.dispatch.GWTDispatch::new()();
    }-*/;
    // ... and the controller of a form as it opens, its requests sent so
    public static GFormController controllerApart(GForm form) {
        GFormController controller = new GFormController(null, null, formContainer(), form, false, form.sID, 0, null);
        dispatchApart(controller);
        return controller;
    }

    // a current object the form chooses where the platform draws the rows is the form's at once - the grid names it,
    // so what the form asks next is of its row, before the server answers and the grid shows it (GTableView
    // .getCurrentKey)
    public void testACurrentObjectTheFormChoosesIsThePlatformGridsAtOnce() {
        GFormController controller = platformGrids(3, 90);
        GGroupObject group = controller.getForm().groupObjects.get(0);
        GPropertyDraw name = controller.getForm().propertyDraws.get(0);
        GGroupObjectValue two = new GGroupObjectValue(90, 2);
        controller.changeCurrentObject(group, two, null, null);
        assertEquals(two, group.getRowKey(controller.getFullCurrentKey(name, GGroupObjectValue.EMPTY)));
    }
    // ... and over a tree the platform draws: a node the form chose is the node the tree's toolbar opens next, before
    // the server answers and the tree shows it - only a node with children opens, so the one the tree was on, with
    // none, would open nothing
    public void testATreeOpensTheNodeTheFormChoseBeforeItShowsIt() {
        GForm form = form("platformTree");
        GTreeGroup tree = platformTree(form); // TREE t (cat)
        GGroupObject cat = new GGroupObject(); // a recursive group: its nodes may have children of its own
        cat.ID = 10; cat.nativeSID = "g10"; cat.sID = "cat"; cat.viewType = GClassViewType.LIST; cat.isRecursive = true;
        cat.objects.add(new GObject(cat, "cat", 10, "cat", GIntegerType.instance));
        cat.parent = tree; tree.groups.add(cat); form.groupObjects.add(cat);
        column(form, 20, "name", cat);
        GGroupObjectValue c1 = new GGroupObjectValue(10, 1), c2 = new GGroupObjectValue(10, 2);
        GFormChangesDTO changes = GFormChangesTest.noChanges(); // two roots, the first current - only the second has children
        changes.objectsGroupIds = new int[] {cat.ID}; changes.objects = new GGroupObjectValue[] {c1};
        changes.gridObjectsGroupIds = new int[] {cat.ID}; changes.gridObjects = new ArrayList[] {new ArrayList<>(Arrays.asList(c1, c2))};
        changes.parentObjectsGroupIds = new int[] {cat.ID}; changes.parentObjects = new ArrayList[] {new ArrayList<>(Arrays.asList(GGroupObjectValue.EMPTY, GGroupObjectValue.EMPTY))};
        changes.expandablesGroupIds = new int[] {cat.ID}; changes.expandableKeys = new GGroupObjectValue[][] {{c1, c2}}; changes.expandableValues = new Integer[][] {{0, 3}};
        form.initialFormChanges = changes;
        GFormController controller = controllerApart(form);
        Widget page = controller.getFormLayout(); // on the page, the tree draws its rows - what opening a node redraws
        RootPanel.get().add(page);

        delayTestFinish(5000);
        Scheduler.get().scheduleDeferred(() -> { // ... once it has drawn them
            try {
                controller.changeCurrentObject(cat, c2, null, null);
                ((GTreeController) tableController(controller, tree)).expandCurrent(); // the tree's toolbar's button
                assertEquals(Boolean.TRUE, pendingExpanding(platformView(controller, tree), cat, c2));
                finishTest();
            } finally {
                RootPanel.get().remove(page);
            }
        });
    }
    // whether the platform's tree shows a node as being opened - or closed - till the answer, null for neither: read
    // where its table keeps its nodes
    private static native Boolean pendingExpanding(DataGrid<?> view, GGroupObject group, GGroupObjectValue key) /*-{
        var nodes = view.@lsfusion.gwt.client.form.object.table.tree.view.GTreeTable::tree.@lsfusion.gwt.client.form.object.table.tree.view.GTreeTableTree::getGroupNodes(*)(group);
        var node = nodes.@lsfusion.gwt.client.base.jsni.NativeHashMap::get(*)(key);
        return node.@lsfusion.gwt.client.form.object.table.tree.view.GTreeContainerTableNode::pendingExpanding;
    }-*/;

    // a column chosen of a property in COLUMNS of two groups, PROPERTIES(i, d, s) qty COLUMNS (d, s), all three groups
    // in grids the platform draws: each column group's current object is its object of the column - the column's key
    // carries both groups' objects -, so the grid of a group whose object the column does not change stays on its row
    // once the server answers with nothing of it
    public void testAColumnChosenKeepsTheGridOfAColumnGroupOnItsRow() {
        GForm form = form("columns");
        GGroupObject d = platformGrid(form, 60, "d"), s = platformGrid(form, 70, "s"), i = platformGrid(form, 80, "i");
        column(form, 65, "nameD", d);
        column(form, 75, "nameS", s);
        GPropertyDraw qty = column(form, 85, "qty", i);
        qty.columnGroupObjects = new ArrayList<>(Arrays.asList(d, s));
        qty.cellType = qty.valueType; // as the server gives both: its cells are drawn, the grid not on the page too
        form.initialFormChanges = GFormChangesTest.noChanges();
        GFormController controller = controllerApart(form);
        GGroupObjectValue d1 = new GGroupObjectValue(60, 1), d2 = new GGroupObjectValue(60, 2);
        GGroupObjectValue s1 = new GGroupObjectValue(70, 1), s2 = new GGroupObjectValue(70, 2);
        GGroupObjectValue i1 = new GGroupObjectValue(80, 1);
        GFormChanges shown = new GFormChanges(); // d1, s1 and i1 current, qty in a column of each d and s
        shown.gridObjects.put(d, new ArrayList<>(Arrays.asList(d1, d2))); shown.objects.put(d, d1);
        shown.gridObjects.put(s, new ArrayList<>(Arrays.asList(s1, s2))); shown.objects.put(s, s1);
        shown.gridObjects.put(i, new ArrayList<>(Arrays.asList(i1))); shown.objects.put(i, i1);
        shown.properties.put(qty, new NativeHashMap<>());
        controller.applyRemoteChanges(shown, 1);

        GGridTable items = platformGridView(controller, i); // the column (d1, s2) chosen
        int column = items.getGridColumnIndex(qty, GGroupObjectValue.getFullKey(d1, s2));
        assertTrue(column >= 0);
        chooseColumn(items, column, FocusUtils.Reason.ACTIVATE, null);
        DeferredRunner.get().commitDelayedGroupObjectChange(d); // ... sent
        DeferredRunner.get().commitDelayedGroupObjectChange(s);
        controller.applyRemoteChanges(new GFormChanges(), 2); // ... answered, nothing of d: d stays on d1, s goes to s2
        assertEquals(d1, controller.getGroupController(d).getSelectedKey());
        assertEquals(s2, controller.getGroupController(s).getSelectedKey());
    }
    // ... a column of a grid chosen, as a click on its cell, a key or activating its property ends: the grid's column
    // changed, its handler told (DataGrid.changeSelectedColumn) - the grid not on the page, nothing focused
    private static native void chooseColumn(GGridTable grid, int column, FocusUtils.Reason reason, NativeEvent event) /*-{
        grid.@lsfusion.gwt.client.base.view.grid.DataGrid::changeSelectedColumn(*)(column, reason, event);
    }-*/;

    // a move in a platform grid in another selection mode within the 50 ms its previous move waits to be sent - a
    // shift held, a drag from where the mouse was pressed - sends that one first; the grid stays on the row it moved to
    // once the answer to it comes, before the move's own request is sent
    public void testAMoveSentEarlierLeavesTheGridOnTheRowItMovedTo() {
        GFormController controller = platformGrids(3, 90);
        GGroupObject group = controller.getForm().groupObjects.get(0);
        GGridTable rows = platformGridView(controller, group);
        try {
            moveTo(rows, 1, FocusUtils.Reason.KEYMOVENAVIGATE, null); // down
            moveTo(rows, 2, FocusUtils.Reason.KEYMOVENAVIGATE, Document.get().createKeyDownEvent(false, false, true, false, 40)); // shift+down
            controller.applyRemoteChanges(new GFormChanges(), 2); // the answer to the first move
            assertEquals(new GGroupObjectValue(90, 3), rows.getSelectedKey());
        } finally { // the move still waiting sent now, not in a test after
            DeferredRunner.get().commitDelayedGroupObjectChange(group);
        }
    }
    // ... and a row the user chooses in it after one the form chose, before the answer to the form's: the user's
    public void testARowTheUserChoosesAfterTheFormsChoiceStays() {
        GFormController controller = platformGrids(3, 90);
        GGroupObject group = controller.getForm().groupObjects.get(0);
        GGridTable rows = platformGridView(controller, group);
        try {
            controller.changeCurrentObject(group, new GGroupObjectValue(90, 3), null, null);
            moveTo(rows, 1, FocusUtils.Reason.MOUSENAVIGATE, null); // a click on the second row
            controller.applyRemoteChanges(new GFormChanges(), 2); // the answer to the form's
            assertEquals(new GGroupObjectValue(90, 2), rows.getSelectedKey());
        } finally {
            DeferredRunner.get().commitDelayedGroupObjectChange(group);
        }
    }
    // a form of platform grids, one of each group ID given with its one column of that ID + 5, of as many rows each as
    // given, on the first, the first grid's column selected - not on the page, its requests sent through a dispatch of
    // its own
    private static GFormController platformGrids(int rows, int... ids) {
        GForm form = form("platformGrids");
        for (int id : ids) {
            GPropertyDraw name = column(form, id + 5, "name" + id, platformGrid(form, id, "g" + id));
            name.cellType = name.valueType; // its cells are drawn
        }
        form.initialFormChanges = GFormChangesTest.noChanges();
        GFormController controller = controllerApart(form);
        GFormChanges shown = new GFormChanges(); // the rows of each, the first current
        for (int i = 0; i < ids.length; i++) {
            GGroupObject group = form.groupObjects.get(i);
            ArrayList<GGroupObjectValue> keys = new ArrayList<>();
            for (int row = 1; row <= rows; row++)
                keys.add(new GGroupObjectValue(ids[i], row));
            shown.gridObjects.put(group, keys);
            shown.objects.put(group, keys.get(0));
            shown.properties.put(form.propertyDraws.get(i), new NativeHashMap<>());
        }
        controller.applyRemoteChanges(shown, 1);
        platformGridView(controller, form.groupObjects.get(0)).setSelectedColumn(0);
        return controller;
    }
    // ... the grid moved to a row as the user moves it - a click, a key -, its handler told (DataGrid.changeSelectedRow)
    private static native void moveTo(DataGrid<?> grid, int row, FocusUtils.Reason reason, NativeEvent event) /*-{
        grid.@lsfusion.gwt.client.base.view.grid.DataGrid::changeSelectedRow(*)(row, reason, event);
    }-*/;
    // a list group whose rows the platform draws in a grid, GRID(<sID>), with nothing of its toolbar and filters to build
    private static GGroupObject platformGrid(GForm form, int id, String sID) {
        GGroupObject group = new GGroupObject();
        group.ID = id; group.nativeSID = "g" + id; group.sID = sID; group.viewType = GClassViewType.LIST;
        group.listViewType = GListViewType.GRID;
        group.objects.add(new GObject(group, sID, id, sID, GIntegerType.instance));
        group.rowSelectReader = new GRowSelectReader(id); // what a move's selection of rows is sent by
        form.groupObjects.add(group);
        GGrid grid = new GGrid();
        grid.ID = id + 1; grid.nativeSID = "c" + (id + 1); grid.sID = "GRID(" + sID + ")"; grid.groupObject = group;
        grid.width = -2; grid.height = -2; grid.lineWidth = -1; grid.lineHeight = -1;
        grid.container = form.mainContainer; form.mainContainer.children.add(grid);
        layout(grid);
        grid.toolbar = new GToolbar(); grid.toolbar.ID = id + 2; grid.toolbar.sID = "TOOLBARSYSTEM(" + sID + ")"; grid.toolbar.visible = false;
        grid.filterControls = new GFilterControls(); grid.filterControls.ID = id + 3; grid.filterControls.sID = "FILTERCONTROLS(" + sID + ")";
        grid.filtersContainer = container(id + 4, "FILTERS(" + sID + ")", null);
        group.grid = grid;
        return group;
    }
    // ... and that grid, where its table's rows are drawn
    private static GGridTable platformGridView(GFormController controller, GGroupObject group) {
        return (GGridTable) platformView(controller, group.grid);
    }
    // a tree the platform draws, TREE t, with nothing of its toolbar and filters to build - its groups for the caller
    // to add
    private static GTreeGroup platformTree(GForm form) {
        GTreeGroup tree = new GTreeGroup();
        tree.ID = 30; tree.nativeSID = "c30"; tree.sID = "TREE t"; tree.name = "t";
        tree.width = -2; tree.height = -2; tree.lineWidth = -1; tree.lineHeight = -1;
        layout(tree);
        tree.container = form.mainContainer; form.mainContainer.children.add(tree);
        tree.toolbar = new GToolbar(); tree.toolbar.ID = 31; tree.toolbar.sID = "TOOLBARSYSTEM(TREE t)"; tree.toolbar.visible = false;
        tree.filterControls = new GFilterControls(); tree.filterControls.ID = 32; tree.filterControls.sID = "FILTERCONTROLS(TREE t)";
        tree.filtersContainer = container(33, "FILTERS(TREE t)", null);
        return tree;
    }
    // what draws a table's rows, as the form keeps them - the platform's table here
    private static native GTableController tableController(GFormController controller, GGridProperty table) /*-{
        return controller.@lsfusion.gwt.client.form.controller.GFormController::tableControllers.@lsfusion.gwt.client.base.jsni.NativeSIDMap::get(*)(table);
    }-*/;
    // ... and the view the platform draws a table's rows in - a grid, a tree
    private static native DataGrid<?> platformView(GFormController controller, GGridProperty table) /*-{
        var rows = @lsfusion.gwt.client.form.controller.GFormControllerTest::tableController(*)(controller, table);
        return rows.@lsfusion.gwt.client.form.object.table.controller.GPlatformTableController::view;
    }-*/;

    // a row the form chose is the one a new filter condition starts on, before the grid shows it - its row and its cell's
    // value, as the table gives them to its filters (GTableController.getSelectedRow, getSelectedValue)
    public void testAFilterConditionStartsOnTheRowTheFormChose() {
        GFormController controller = platformGrids(3, 90);
        GGroupObject group = controller.getForm().groupObjects.get(0);
        GPropertyDraw name = controller.getForm().propertyDraws.get(0);
        GFormChanges values = new GFormChanges(); // 10, 20, 30
        NativeHashMap<GGroupObjectValue, PValue> names = new NativeHashMap<>();
        for (int i = 1; i <= 3; i++)
            names.put(new GGroupObjectValue(90, i), PValue.getPValue(i * 10));
        values.properties.put(name, names);
        controller.applyRemoteChanges(values, 2);

        controller.changeCurrentObject(group, new GGroupObjectValue(90, 2), null, null);
        GTableController rows = tableController(controller, group.grid);
        assertEquals(1, rows.getSelectedRow());
        assertValue(20, rows.getSelectedValue(name, GGroupObjectValue.EMPTY));
    }
    // ... and over a tree the platform draws, TREE t (c = Category, i = Item): a node of another group - an item under
    // the second category, the tree on the first - is the one whose property and value a condition starts on
    public void testATreesFilterConditionStartsOnTheNodeTheFormChose() {
        GFormController controller = catalogTree();
        GGroupObject item = controller.getForm().groupObjects.get(1);
        GPropertyDraw itemName = controller.getForm().propertyDraws.get(1);
        GGroupObjectValue i1 = GGroupObjectValue.getFullKey(new GGroupObjectValue(10, 2), new GGroupObjectValue(40, 1));
        GTreeGroup tree = item.parent;
        controller.changeCurrentObject(item, i1, null, null);
        GTableController rows = tableController(controller, tree);
        assertSame(itemName, rows.getSelectedFilterProperty());
        assertValue(7, rows.getSelectedValue(itemName, GGroupObjectValue.EMPTY));
    }
    // ... and the platform's rows of the FILTERs of a tree, FILTER(catName) and FILTER(itemName), show their properties'
    // captions whatever node the tree is on - a row reads its caption again with every change of the tree: on the first
    // category as the form opens, then on the item, once the server has answered the move. The columns a tree gives its
    // filters are of every group of it (GTableController.getFilterColumns), a condition being of the group of its
    // property
    public void testATreesFiltersShowTheirCaptionsWhateverNodeItIsOn() {
        GFilter[] filters = new GFilter[2];
        GFormController controller = catalogTree(form -> {
            GPropertyDraw catName = form.propertyDraws.get(0), itemName = form.propertyDraws.get(1);
            catName.caption = "Category"; itemName.caption = "Item";
            GTreeGroup tree = catName.groupObject.parent;
            tree.toolbar.showFilters = true; // the platform's filters: its rows of the FILTERs
            filters[0] = filterIn(34, catName, form.mainContainer, tree);
            filters[1] = filterIn(35, itemName, form.mainContainer, tree);
        });
        GGroupObject item = controller.getForm().groupObjects.get(1);
        GTableController rows = tableController(controller, item.parent);
        GFilterInputController input = ((GPlatformTableController<?, ?, ?>) rows).getFilter();
        assertSame(controller.getForm().propertyDraws.get(0), rows.getSelectedFilterProperty()); // on a category
        List<String> captions = captions(input, filters);

        GGroupObjectValue i1 = GGroupObjectValue.getFullKey(new GGroupObjectValue(10, 2), new GGroupObjectValue(40, 1));
        controller.applyRemoteChanges(new GFormChanges(), (int) controller.changeGroupObject(item, i1, null, null));
        assertSame(controller.getForm().propertyDraws.get(1), rows.getSelectedFilterProperty()); // on the item
        captions.addAll(captions(input, filters));
        assertEquals(Arrays.asList("Category", "Item", "Category", "Item"), captions);
    }
    // ... the captions the platform's rows of FILTERs show, in the order the FILTERs are given
    private static List<String> captions(GFilterInputController input, GFilter... filters) {
        List<String> captions = new ArrayList<>();
        for (GFilter filter : filters)
            captions.add(propertyLabel(panelRow(input, filter)).getText());
        return captions;
    }
    // a form of a tree the platform draws, TREE t (c = Category, i = Item): two categories, the first current, and an item
    // under the second - its rows c1, c2, i1 -, the name of each group a column; not on the page, its requests sent
    // through a dispatch of its own
    private static GFormController catalogTree() {
        return catalogTree(form -> { });
    }
    // ... its design given what a test adds to it first - the form's properties are the categories' name, then the
    // items'
    private static GFormController catalogTree(Consumer<GForm> design) {
        GForm form = form("catalog");
        GTreeGroup tree = platformTree(form);
        GGroupObject cat = new GGroupObject(), item = new GGroupObject();
        cat.ID = 10; cat.nativeSID = "g10"; cat.sID = "c"; cat.viewType = GClassViewType.LIST;
        cat.objects.add(new GObject(cat, "c", 10, "c", GIntegerType.instance));
        item.ID = 40; item.nativeSID = "g40"; item.sID = "i"; item.viewType = GClassViewType.LIST;
        item.objects.add(new GObject(item, "i", 40, "i", GIntegerType.instance));
        cat.parent = tree; item.parent = tree; tree.groups.add(cat); tree.groups.add(item);
        item.upTreeGroups.add(cat); // as the server gives the groups above
        form.groupObjects.add(cat); form.groupObjects.add(item);
        GPropertyDraw catName = column(form, 20, "catName", cat), itemName = column(form, 45, "itemName", item);
        catName.cellType = catName.valueType; itemName.cellType = itemName.valueType; // its cells are drawn
        form.initialFormChanges = GFormChangesTest.noChanges();
        design.accept(form);
        GFormController controller = controllerApart(form);
        GGroupObjectValue c1 = new GGroupObjectValue(10, 1), c2 = new GGroupObjectValue(10, 2);
        GGroupObjectValue i1 = GGroupObjectValue.getFullKey(c2, new GGroupObjectValue(40, 1)); // its path
        GFormChanges shown = new GFormChanges(); // two categories, the item under the second, the first current
        shown.gridObjects.put(cat, new ArrayList<>(Arrays.asList(c1, c2)));
        shown.parentObjects.put(cat, new ArrayList<>(Arrays.asList(GGroupObjectValue.EMPTY, GGroupObjectValue.EMPTY)));
        NativeHashMap<GGroupObjectValue, Integer> expandables = new NativeHashMap<>();
        expandables.put(c1, 0); expandables.put(c2, 1);
        shown.expandables.put(cat, expandables);
        shown.gridObjects.put(item, new ArrayList<>(Arrays.asList(i1)));
        shown.parentObjects.put(item, new ArrayList<>(Arrays.asList(GGroupObjectValue.EMPTY)));
        NativeHashMap<GGroupObjectValue, Integer> itemExpandables = new NativeHashMap<>();
        itemExpandables.put(i1, 0);
        shown.expandables.put(item, itemExpandables);
        shown.objects.put(cat, c1);
        NativeHashMap<GGroupObjectValue, PValue> catNames = new NativeHashMap<>(), itemNames = new NativeHashMap<>();
        catNames.put(c1, PValue.getPValue(1)); catNames.put(c2, PValue.getPValue(2)); itemNames.put(i1, PValue.getPValue(7));
        shown.properties.put(catName, catNames); shown.properties.put(itemName, itemNames);
        controller.applyRemoteChanges(shown, 1);
        return controller;
    }

    // a row deleted that is not the one the grid is on - a view's delete runs on any row - and kept by the server, the
    // delete's answer bringing nothing of the rows: it goes back where it was, not where the current row is
    // (GGroupController.getRow)
    public void testARowTheServerKeepsGoesBackWhereItWas() {
        GFormController controller = platformGrids(3, 90);
        GGroupObject group = controller.getForm().groupObjects.get(0);
        GPropertyDraw name = controller.getForm().propertyDraws.get(0);
        GGroupObjectValue one = new GGroupObjectValue(90, 1), two = new GGroupObjectValue(90, 2), three = new GGroupObjectValue(90, 3);
        controller.asyncAddRemove(null, new ExecContext() { // the second row's delete, the grid on the first
            public GPropertyDraw getProperty() { return name; }
            public GGroupObjectValue getFullKey() { return two; }
        }, null, GEditBindingMap.CHANGE, new GAsyncAddRemove(90, false), null, GEventSource.CUSTOM, requestIndex -> { });
        assertEquals(Arrays.asList(one, three), rowKeys(platformGridView(controller, group)));
        controller.applyRemoteChanges(new GFormChanges(), 100); // ... answered with nothing of the rows: kept
        assertEquals(Arrays.asList(one, two, three), rowKeys(platformGridView(controller, group)));
    }
    // ... the keys of a platform grid's rows, in their order
    private static native ArrayList<GGroupObjectValue> rowKeys(GGridTable grid) /*-{
        return grid.@lsfusion.gwt.client.form.object.table.grid.view.GGridTable::rowKeys;
    }-*/;

    // a current object set and not shown yet - none, as the server may say - and then the row the grid is on set again,
    // before the grid shows either: the grid stays on that row (GGridPropertyTable.setCurrentKey)
    public void testTheRowTheGridIsOnSetAgainAfterNoneStays() {
        GFormController controller = platformGrids(3, 90);
        GGroupObject group = controller.getForm().groupObjects.get(0);
        GGroupController rows = controller.getGroupController(group);
        rows.updateCurrentKey(null);
        rows.updateCurrentKey(new GGroupObjectValue(90, 1));
        controller.applyRemoteChanges(new GFormChanges(), 2);
        assertEquals(new GGroupObjectValue(90, 1), platformGridView(controller, group).getSelectedKey());
    }

    // a move waiting in a form while another open form - its groups of the same IDs - makes one: each form's move waits
    // and is sent as its own (DeferredRunner.groupCommandID)
    public void testAMoveWaitingInAFormIsNotAnotherFormsOneOfTheSameGroupID() {
        GFormController one = platformGrids(3, 90), other = platformGrids(3, 90);
        GGroupObject group = one.getForm().groupObjects.get(0), otherGroup = other.getForm().groupObjects.get(0);
        try {
            moveTo(platformGridView(one, group), 1, FocusUtils.Reason.KEYMOVENAVIGATE, null); // down in the one
            moveTo(platformGridView(other, otherGroup), 2, FocusUtils.Reason.KEYMOVENAVIGATE, null); // ... in the other
            DeferredRunner.get().commitDelayedGroupObjectChange(group); // the one's sent
            assertTrue(sentAt(one, group) < Long.MAX_VALUE);
            assertEquals(Long.MAX_VALUE, (long) sentAt(other, otherGroup)); // ... the other's still waiting
        } finally {
            DeferredRunner.get().commitDelayedGroupObjectChange(group);
            DeferredRunner.get().commitDelayedGroupObjectChange(otherGroup);
        }
    }
    // ... and a page size asked in a form while another open form - its groups of the same IDs - asks one: each form's is
    // sent (DeferredRunner.groupCommandID)
    public void testAPageSizeAskedInAFormIsNotAnotherFormsOneOfTheSameGroupID() {
        GFormController one = platformGrids(3, 90), other = platformGrids(3, 90);
        long oneNext = nextRequestIndex(one), otherNext = nextRequestIndex(other);
        one.changePageSizeAfterUnlock(one.getForm().groupObjects.get(0), 10); // sent once nothing blocks the form, then
        other.changePageSizeAfterUnlock(other.getForm().groupObjects.get(0), 20); // 100 ms later
        delayTestFinish(15000);
        new Timer() {
            int checks;
            @Override
            public void run() {
                if (!(nextRequestIndex(one) > oneNext && nextRequestIndex(other) > otherNext) && ++checks < 50)
                    return; // ... both, or as long as they may take
                cancel();
                assertTrue(nextRequestIndex(one) > oneNext); // the one's sent
                assertTrue(nextRequestIndex(other) > otherNext); // ... and the other's
                finishTest();
            }
        }.scheduleRepeating(200);
    }
    // the index of the request that sent a group's current object - Long.MAX_VALUE while its move waits -, read where the
    // form keeps it (pendingChangeCurrentObjectsRequests)
    private static native Long sentAt(GFormController controller, GGroupObject group) /*-{
        return controller.@lsfusion.gwt.client.form.controller.GFormController::pendingChangeCurrentObjectsRequests.@lsfusion.gwt.client.base.jsni.NativeSIDMap::get(*)(group);
    }-*/;

    // a current object chosen and sent at once for a group whose move still waits - in the same selection mode - takes
    // that move's place: the move is not sent after it, to take the choice's place on the server
    // (GFormController.changeGroupObject)
    public void testAChoiceSentAtOnceTakesTheWaitingMovesPlace() {
        GFormController controller = platformGrids(3, 90);
        GGroupObject group = controller.getForm().groupObjects.get(0);
        try {
            moveTo(platformGridView(controller, group), 1, FocusUtils.Reason.SCROLLNAVIGATE, null); // to the second row, by a scroll: no selection mode, as the choice's
            long sent = controller.changeGroupObject(group, new GGroupObjectValue(90, 3), null, null); // the third, at once
            DeferredRunner.get().commitDelayedGroupObjectChange(group); // ... and what still waits sent
            assertEquals(sent, (long) sentAt(controller, group));
        } finally {
            DeferredRunner.get().commitDelayedGroupObjectChange(group);
        }
    }
    // ... and a move still waiting in another group goes before the choice, the last one made
    // (GFormController.changeGroupObject)
    public void testAChoiceSentAtOnceSendsTheMovesWaitingFirst() {
        GFormController controller = platformGrids(2, 60, 70);
        GGroupObject a = controller.getForm().groupObjects.get(0), b = controller.getForm().groupObjects.get(1);
        try {
            moveTo(platformGridView(controller, a), 1, FocusUtils.Reason.KEYMOVENAVIGATE, null); // down in a
            long sent = controller.changeGroupObject(b, new GGroupObjectValue(70, 2), null, null); // b's second, at once
            assertTrue(sentAt(controller, a) < sent);
        } finally {
            DeferredRunner.get().commitDelayedGroupObjectChange(a);
        }
    }

    // a tree's moves within the wait - to the second category, to the item under it, back to the category: one waits
    // per group - sent first in the order they were made, the tree's last one last, where the form sends all that waits
    // (GFormController.batch): the item's, then the category's
    public void testATreesMovesWaitingAreSentInTheOrderMade() {
        GFormController controller = catalogTree();
        GGroupObject cat = controller.getForm().groupObjects.get(0), item = controller.getForm().groupObjects.get(1);
        DataGrid<?> rows = platformView(controller, cat.parent);
        try {
            moveTo(rows, 1, FocusUtils.Reason.KEYMOVENAVIGATE, null); // to the second category
            moveTo(rows, 2, FocusUtils.Reason.KEYMOVENAVIGATE, null); // ... to the item under it
            moveTo(rows, 1, FocusUtils.Reason.KEYMOVENAVIGATE, null); // ... back to the category
            controller.batch(() -> { });
            assertTrue(sentAt(controller, item) < sentAt(controller, cat));
        } finally {
            DeferredRunner.get().commitDelayedGroupObjectChange(cat);
            DeferredRunner.get().commitDelayedGroupObjectChange(item);
        }
    }

    // an action of a property of one grid while a move in another grid still waits: the move goes first - the action's
    // key takes that grid's current object, and so does what the server does on it (GFormController.getFullCurrentKey)
    public void testAnActionSendsTheMovesOfOtherGroupsFirst() {
        GFormController controller = platformGrids(2, 60, 70);
        GGroupObject a = controller.getForm().groupObjects.get(0);
        GPropertyDraw nameB = controller.getForm().propertyDraws.get(1);
        try {
            moveTo(platformGridView(controller, a), 1, FocusUtils.Reason.KEYMOVENAVIGATE, null); // down in a
            long action = controller.asyncExecutePropertyEventAction(GEditBindingMap.CHANGE, null, null, nameB, GEventSource.CUSTOM, GGroupObjectValue.EMPTY, null); // ... b's action
            assertTrue(sentAt(controller, a) < action);
        } finally {
            DeferredRunner.get().commitDelayedGroupObjectChange(a);
        }
    }

    // a scroll to the last row (Ctrl+End) while a move still waits: the move goes first, so the scroll goes from its row
    // (GFormController.scrollToEnd)
    public void testAScrollToTheEndSendsTheMoveWaitingFirst() {
        GFormController controller = platformGrids(3, 90);
        GGroupObject group = controller.getForm().groupObjects.get(0);
        try {
            moveTo(platformGridView(controller, group), 1, FocusUtils.Reason.KEYMOVENAVIGATE, null); // down
            long next = nextRequestIndex(controller);
            controller.scrollToEnd(group, true, null); // ... Ctrl+End
            assertEquals(next, (long) sentAt(controller, group));
        } finally {
            dispatcher(controller).getBusyDialogDisplayer().stop(true); // the scroll's blocking panel, no answer coming
            DeferredRunner.get().commitDelayedGroupObjectChange(group);
        }
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
    static GForm form(String sID) {
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
        return press(controller, target, keyCode, false);
    }
    // ... with Alt held
    private static boolean press(GFormController controller, Element target, int keyCode, boolean alt) {
        NativeEvent key = Document.get().createKeyDownEvent(false, alt, false, false, keyCode);
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
        GPlatformFilterInputController panel = (GPlatformFilterInputController) userFiltersController(controller, filters.group);

        filters.model(controller).changeFilter(filters.drawn, filters.condition(null));
        assertTrue(filters.filters(controller, filters.price));
    }

    // ... and the panel's reset takes off the NULL a fixed row has - one React states, and one of the platform's row a list
    // the server reports has -, as a row's own clearing does: no apply after it has them again
    public void testThePlatformsPanelsResetTakesOffTheNullsOfItsFixedRows() {
        FiltersForm filters = new FiltersForm(true);
        GFormController controller = filters.controller();
        GPlatformFilterInputController panel = (GPlatformFilterInputController) userFiltersController(controller, filters.group);

        filters.report(controller, new GPropertyFilter(new GFilter(filters.quantity), filters.group, GGroupObjectValue.EMPTY, null, false, GCompare.EQUALS, true));
        filters.model(controller).changeFilter(filters.drawn, filters.condition(null));
        assertTrue(filters.filters(controller, filters.quantity));
        panel.resetConditions();
        panel.applyConditions();
        assertFalse(filters.filters(controller, filters.price));
        assertFalse(filters.filters(controller, filters.quantity));
    }

    // ... and from a list the server reports each fixed row - a FILTER's, the one React draws too - takes the first
    // condition on its column, as a FILTER shows its condition where a view draws the conditions
    // (GUserFiltersController.getSlotEntry): a second on the column is a row of the panel's own, the rows of the rest
    // in their order
    public void testAFixedRowTakesTheFirstConditionOnItsColumn() {
        FiltersForm filters = new FiltersForm(true);
        GFormController controller = filters.controller();
        GPlatformFilterInputController panel = (GPlatformFilterInputController) userFiltersController(controller, filters.group);

        filters.report(controller, new GPropertyFilter(new GFilter(filters.price), filters.group, GGroupObjectValue.EMPTY, PValue.getPValue(1), GCompare.GREATER),
                new GPropertyFilter(new GFilter(filters.price), filters.group, GGroupObjectValue.EMPTY, PValue.getPValue(5), GCompare.LESS),
                new GPropertyFilter(new GFilter(filters.quantity), filters.group, GGroupObjectValue.EMPTY, PValue.getPValue(2), GCompare.EQUALS));
        assertEquals(GCompare.GREATER, filters.slot(controller, filters.drawn).compare);
        assertEquals(GCompare.EQUALS, filters.slot(controller, filters.placed).compare);
        assertNotNull(filters.panelCondition(controller)); // price < 5, a row of its own
        filters.report(controller, new GPropertyFilter(new GFilter(filters.price), filters.group, GGroupObjectValue.EMPTY, PValue.getPValue(5), GCompare.LESS));
        assertEquals(GCompare.LESS, filters.slot(controller, filters.drawn).compare);
        assertNull(filters.slot(controller, filters.placed));
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
        GPlatformFilterInputController panel = (GPlatformFilterInputController) userFiltersController(controller, filters.group);

        filters.report(controller, new GPropertyFilter(new GFilter(filters.price), filters.group, GGroupObjectValue.EMPTY, PValue.getPValue(5), GCompare.GREATER),
                new GPropertyFilter(new GFilter(filters.quantity), filters.group, GGroupObjectValue.EMPTY, PValue.getPValue(2), GCompare.EQUALS));
        assertEquals(GCompare.GREATER, filters.slot(controller, filters.drawn).compare);
        assertEquals(GCompare.EQUALS, filters.slot(controller, filters.placed).compare);
        filters.report(controller);
        assertNull(filters.slot(controller, filters.drawn));
        assertNull(filters.slot(controller, filters.placed));
    }

    // ... and a condition of the list all the same: a row of the platform's before it, a condition following, is not
    // the last one, the junction between them a real one (the design declaring the platform's filter first); with no
    // condition after it, it is: a junction joins a condition to the next active one of its group
    public void testAPlatformRowBeforeAFilterReactDrawsIsNotTheLast() {
        FiltersForm filters = new FiltersForm(true);
        Collections.reverse(filters.group.grid.filters);
        GFormController controller = filters.controller();
        GPlatformFilterInputController panel = (GPlatformFilterInputController) userFiltersController(controller, filters.group);

        assertTrue(isLast(panelRow(panel, filters.placed)));
        filters.change(controller, "price", 5.0);
        assertFalse(isLast(panelRow(panel, filters.placed)));
        assertTrue(filters.model(controller).isLast(filters.model(controller).getSlotEntry(filters.drawn)));
    }

    // a FILTER's condition taken off - its row emptied -, the next condition on its column takes its row at once: a row
    // of the box's no more, its condition and its place in the list kept, the list sent
    public void testTheNextConditionOnAFiltersColumnTakesItsRowOnceItsOwnIsOff() {
        FiltersForm filters = new FiltersForm(true);
        GFormController controller = filters.controller();
        GPlatformFilterInputController panel = (GPlatformFilterInputController) userFiltersController(controller, filters.group);
        filters.report(controller, filters.condition(filters.quantity, PValue.getPValue(2), GCompare.EQUALS),
                filters.condition(filters.quantity, PValue.getPValue(1), GCompare.GREATER));
        assertEquals(1, freeRows(panel).size()); // the second on quantity, a row of the box's

        changeRowValue(panelRow(panel, filters.placed), null);
        assertTrue(freeRows(panel).isEmpty());
        GPropertyFilter shown = rowCondition(panelRow(panel, filters.placed));
        assertEquals(GCompare.GREATER, shown.compare);
        assertValue(1, shown.value.value);
        assertEquals(Collections.singletonList(filters.quantity), sentProperties(controller, filters));
        assertEquals(GCompare.GREATER, filters.first(controller, filters.quantity).compare);
    }

    // the list is sent in its own order - a FILTER's condition where it stands -, not the FILTERs' first: a condition
    // joined by OR to the one after it stays joined to it
    public void testTheListIsSentInItsOwnOrder() {
        FiltersForm filters = new FiltersForm(true);
        filters.plainFirst();
        GFormController controller = filters.controller();
        GPlatformFilterInputController panel = (GPlatformFilterInputController) userFiltersController(controller, filters.group);
        GPropertyFilter amount = new GPropertyFilter(new GFilter(filters.amount), filters.group, GGroupObjectValue.EMPTY, PValue.getPValue(50), false, GCompare.EQUALS, false);
        filters.report(controller, amount, filters.condition(filters.quantity, PValue.getPValue(2), GCompare.EQUALS));

        changeRowValue(panelRow(panel, filters.placed), PValue.getPValue(4));
        assertEquals(Arrays.asList(filters.amount, filters.quantity), sentProperties(controller, filters));
        assertFalse(filters.first(controller, filters.amount).junction);
    }

    // with no FILTERS box the conditions no FILTER shows stay in the list all the same: a FILTER's value is sent with
    // them, none lost
    public void testAPanelWithNoFiltersBoxKeepsTheConditionsItDoesNotShow() {
        FiltersForm filters = new FiltersForm(false);
        filters.plainFirst();
        GFormController controller = filters.controller();
        GPlatformFilterInputController panel = (GPlatformFilterInputController) userFiltersController(controller, filters.group);
        filters.report(controller, filters.condition(filters.amount, PValue.getPValue(50), GCompare.EQUALS),
                filters.condition(filters.quantity, PValue.getPValue(2), GCompare.EQUALS));

        changeRowValue(panelRow(panel, filters.placed), PValue.getPValue(4));
        assertEquals(Arrays.asList(filters.amount, filters.quantity), sentProperties(controller, filters));
    }

    // ... and in the panel's manual apply mode - its controls shown - a change waits for the panel's apply, as its own
    // rows' do: the row holds it, the form's list stays as it was, the apply sends it
    public void testAFilterReactDrawsWaitsForThePanelsApplyInManualMode() {
        boolean manual = MainFrame.userFiltersManualApplyMode;
        MainFrame.userFiltersManualApplyMode = true;
        try {
            FiltersForm filters = new FiltersForm(true);
            GFormController controller = filters.controller();
            GPlatformFilterInputController panel = (GPlatformFilterInputController) userFiltersController(controller, filters.group);
            panel.setControlsVisible(true);

            filters.change(controller, "price", 5.0);
            assertFalse(filters.slot(controller, filters.drawn).nullValue());
            assertFalse(filters.filters(controller, filters.price));
            panel.applyConditions();
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
                GPlatformFilterInputController panel = (GPlatformFilterInputController) userFiltersController(controller, filters.group);
                filters.change(controller, "price", 3.0, "{\"compare\": \">\"}"); // the controls hidden: applied at once
                assertValue(3, filters.first(controller, filters.price).value.value);

                panel.setControlsVisible(true);        // the controls shown first, as the table's F3 does,
                panel.addCondition(null, false, true); // ... then F3, on the current row - its price, the first column
                assertNull(filters.panelCondition(controller));
                assertEquals(5.0, filterValue(filters.view(controller), "price"), 0);
                assertEquals(GCompare.GREATER, filters.slot(controller, filters.drawn).compare);
                assertValue(byHand ? 3 : 5, filters.first(controller, filters.price).value.value);
                panel.applyConditions();
                assertValue(5, filters.first(controller, filters.price).value.value);
                assertEquals(GCompare.GREATER, filters.first(controller, filters.price).compare);
            }
        } finally {
            MainFrame.userFiltersManualApplyMode = manual;
        }
    }

    // ... and a new row on the column of a FILTER React draws showing no condition would be its - drawn by React, with
    // no editor here -: Alt+F3 and the controls' add put the cell's value into it, written as F3 writes it, and a key
    // typed in the column starts nothing
    public void testANewRowOnTheColumnOfAnEmptyFilterReactDrawsIsItsValue() {
        FiltersForm filters = new FiltersForm(true);
        filters.deliver(true);
        GFormController controller = filters.controller();
        GPlatformFilterInputController panel = (GPlatformFilterInputController) userFiltersController(controller, filters.group);
        panel.quickEditFilter(null, filters.price, GGroupObjectValue.EMPTY); // typing
        assertNull(filters.slot(controller, filters.drawn));
        assertTrue(freeRows(panel).isEmpty());
        panel.addCondition(null, true, false); // Alt+F3: the reset, then the current row's price
        assertValue(5, filters.first(controller, filters.price).value.value);
        panel.resetConditions();
        panel.addCondition(); // the controls' add
        assertValue(5, filters.first(controller, filters.price).value.value);
        assertTrue(freeRows(panel).isEmpty());
    }

    // ... and F3 on an empty cell into a FILTER React draws: no value, so its condition is taken off - as the
    // platform's row of a FILTER, which allows no NULL, takes none -, not one comparing with NULL stated; what it
    // remembers stays
    public void testF3OnAnEmptyCellTakesTheConditionOfAFilterReactDrawsOff() {
        FiltersForm filters = new FiltersForm(true);
        filters.deliver(true);
        GFormController controller = filters.controller();
        GPlatformFilterInputController panel = (GPlatformFilterInputController) userFiltersController(controller, filters.group);
        filters.change(controller, "price", 3.0, "{\"compare\": \">\"}");
        assertTrue(filters.filters(controller, filters.price));
        GFormChanges empty = new GFormChanges(); // the current row's price NULL
        NativeHashMap<GGroupObjectValue, PValue> prices = new NativeHashMap<>();
        prices.put(new GGroupObjectValue(filters.group.ID, 1), null);
        empty.properties.put(filters.price, prices);
        controller.applyRemoteChanges(empty, 2);

        panel.addCondition(null, false, true); // F3
        assertFalse(filters.filters(controller, filters.price));
        assertEquals(GCompare.GREATER, filters.model(controller).getSlotEntry(filters.drawn).condition.compare);
    }

    // ... and where no row is current - an empty grid -, F3 has no value to put into it, and no editor to type one in:
    // the FILTER's condition stays
    public void testF3OverThePanelOnNoRowLeavesTheRowOfAFilterReactDraws() {
        FiltersForm filters = new FiltersForm(true);
        filters.deliver(false);
        GFormController controller = filters.controller();
        GPlatformFilterInputController panel = (GPlatformFilterInputController) userFiltersController(controller, filters.group);
        filters.change(controller, "price", 3.0, "{\"compare\": \">\"}");

        panel.addCondition(null, false, true); // F3
        assertValue(3, filters.first(controller, filters.price).value.value);
        assertEquals(GCompare.GREATER, filters.first(controller, filters.price).compare);
    }

    // A REACT VIEW'S CONTROLLER NAMES THE FORM'S as `form` - the object the classic surfaces are handed: the server
    // calls, the editing a view declares, the batch - and carries none of the form's verbs itself; the form's batch is
    // the form's, as a view's is
    public void testAViewsControllerNamesTheFormsController() {
        FiltersForm filters = new FiltersForm(true);
        GFormController controller = filters.controller();
        JavaScriptObject board = reactController(controller.getFormLayout().getContainerView(filters.board));

        assertTrue(field(board, "form") == controller.controller);
        for (String verb : new String[]{"exec", "eval", "evalAction", "change", "startEditing", "stopEditing", "isEditing"}) {
            assertTrue(verb, own(controller.controller, verb));
            assertFalse(verb, own(board, verb));
        }
        boolean[] batching = {false};
        batch(controller.controller, () -> batching[0] = controller.isBatching());
        assertTrue(batching[0]);
    }

    // CONTROLLER.FORM over the rows the platform draws: the current object it chooses is the form's current one at
    // once - the grid names it, so what the form asks next is of its row, before the server answers and the grid shows
    // it -, and the sortings and the update mode it changes are the form's, which the platform's grid and toolbar are
    // told of
    public void testTheFormsControllerChoosesARowThePlatformDraws() {
        GFormController controller = manualUpdateGrid();
        GGroupObject group = controller.getForm().groupObjects.get(0);
        GPropertyDraw price = controller.getForm().propertyDraws.get(0);
        GGroupObjectValue two = new GGroupObjectValue(10, 2);

        call(controller.controller, "changeObject", "items", objects(two)); // what a view has of a row it does not draw
        assertEquals(two, group.getRowKey(controller.getFullCurrentKey(price, GGroupObjectValue.EMPTY)));
        call(controller.controller, "changeOrders", "items", parse("[{\"property\": \"price\", \"desc\": true}]"));
        assertEquals(Boolean.FALSE, controller.getOrders(group).get(new Column(price, GGroupObjectValue.EMPTY)));
        call(controller.controller, "changeManual", "items", true);
        assertTrue(controller.isManualUpdate(group));
    }

    // ... and over a table's user filters the platform draws - its panel and its input, a view drawing a FILTER over
    // them -: the controls shown as the toolbar's button shows them, the apply with them - the manual apply mode -; the
    // conditions stated by the table's name and a FILTER's by its property's, each waiting for the apply, which sends
    // them; the reset an empty list, sent at once; the add a new row of the panel's own with the value of the cell the
    // rows are on, prepared, not sent; the controls hidden again
    public void testTheFormsControllerWorksTheUserFiltersThePlatformDraws() {
        boolean manual = MainFrame.userFiltersManualApplyMode;
        MainFrame.userFiltersManualApplyMode = true;
        try {
            FiltersForm filters = new FiltersForm(true);
            filters.plainFirst();
            filters.deliver(true);
            GFormController controller = filters.controller();
            JavaScriptObject form = controller.controller;
            GPlatformFilterInputController panel = (GPlatformFilterInputController) userFiltersController(controller, filters.group);
            call(form, "changeFilterControlsVisible", "items", true);
            assertTrue(panel.isControlsVisible());

            call(form, "changeFilters", "items", parse("{\"property\": \"price\", \"compare\": \">\", \"value\": 6}"));
            call(form, "changeFilter", "items.quantity", 4.0);
            assertValue(4, filters.slot(controller, filters.placed).value.value);
            assertFalse(filters.filters(controller, filters.price)); // waiting for the apply
            call(form, "applyFilters", "items");
            assertEquals(GCompare.GREATER, filters.first(controller, filters.price).compare);
            assertValue(4, filters.first(controller, filters.quantity).value.value);

            call(form, "resetFilters", "items");
            assertTrue(filters.sent(controller).isEmpty());
            call(form, "addFilter", "items");
            List<GFilterConditionView> rows = freeRows(panel);
            assertEquals(1, rows.size());
            assertSame(filters.amount, rowCondition(rows.get(0)).column.property);
            assertValue(50, rowCondition(rows.get(0)).value.value);
            assertTrue(filters.sent(controller).isEmpty());
            call(form, "changeFilterControlsVisible", "items", false);
            assertFalse(panel.isControlsVisible());
        } finally {
            MainFrame.userFiltersManualApplyMode = manual;
        }
    }

    // the FILTER of a property of a group in PANEL view - which has no table and shows no user filters -: the group's
    // user filters are none (GUserFiltersController.NONE), a list the server reports for the group taken all the same,
    // as one the form sends
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
        GPlatformFilterInputController panel = (GPlatformFilterInputController) userFiltersController(controller, filters.group);

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
        GPlatformFilterInputController panel = (GPlatformFilterInputController) userFiltersController(controller, filters.group);

        filters.report(controller, new GPropertyFilter(new GFilter(filters.quantity), filters.group, GGroupObjectValue.EMPTY, PValue.getPValue(2), true, GCompare.GREATER, true));
        panel.setControlsVisible(true);
        assertEquals("!>", compareSelector(panelRow(panel, filters.placed)).getElement().getInnerText());
    }

    // a fixed row of the panel - a FILTER's -, its controls shown, keeps its property, the design's: its caption shown
    // in place of the choice of another, which it offered
    public void testAFixedRowKeepsItsPropertyWithItsControlsShown() {
        FiltersForm filters = new FiltersForm(true);
        GFormController controller = filters.controller();
        GPlatformFilterInputController panel = (GPlatformFilterInputController) userFiltersController(controller, filters.group);
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
        GPlatformFilterInputController panel = (GPlatformFilterInputController) userFiltersController(controller, filters.group);
        assertEnterInItsValueCellAlone(filters, controller, panelRow(panel, filters.placed));
    }
    // a value prepared in quantity's row, Enter pressed in another panel cell of its group, then in the row's value
    // cell: the row's condition stated by the second alone
    private static void assertEnterInItsValueCellAlone(FiltersForm filters, GFormController controller, GFilterConditionView view) {
        Label cell = new Label(); // another panel cell of the group
        GFormController.setBindingGroupObject(cell, filters.group);
        RootPanel.get().add(view); // on the page: a binding holds while its widget is shown
        RootPanel.get().add(cell);
        try {
            filters.prepare(controller, filters.placed, PValue.getPValue(3));
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
        GPlatformFilterInputController panel = (GPlatformFilterInputController) userFiltersController(controller, filters.group);
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
        GPlatformFilterInputController panel = (GPlatformFilterInputController) userFiltersController(controller, filters.group);
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

    // ===== THE PLATFORM'S FILTER PANEL AS IT IS: what its rows and its input do, pinned before the panel becomes a
    // view of the table's user filters (the one-owner model: agg handoff/react-common/filters-one-owner)

    // F3 on a column no row is on: a new row of the panel's own takes the cell's value - prepared, not sent -, its
    // apply button lit: a commit of the value, or the next apply of any row, sends it
    public void testF3PreparesTheCellsValueInANewRow() {
        FiltersForm filters = new FiltersForm(true);
        filters.plainFirst();
        filters.deliver(true);
        GFormController controller = filters.controller();
        GPlatformFilterInputController panel = (GPlatformFilterInputController) userFiltersController(controller, filters.group);

        panel.addCondition(GKeyStroke.createAddUserFilterKeyEvent(), false, true); // F3
        List<GFilterConditionView> rows = freeRows(panel);
        assertEquals(1, rows.size());
        assertSame(filters.amount, rowCondition(rows.get(0)).column.property);
        assertValue(50, rowCondition(rows.get(0)).value.value);
        assertTrue(filters.sent(controller).isEmpty());
        assertFalse(applied(rows.get(0)));
        panel.applyConditions();
        assertValue(50, filters.first(controller, filters.amount).value.value);
    }

    // ... on a column a row of the panel's own is on: that row - the first on the column - takes it, none added
    public void testF3PutsTheCellsValueIntoTheFirstRowOnItsColumn() {
        FiltersForm filters = new FiltersForm(true);
        filters.plainFirst();
        filters.deliver(true);
        GFormController controller = filters.controller();
        GPlatformFilterInputController panel = (GPlatformFilterInputController) userFiltersController(controller, filters.group);
        filters.model(controller).add(filters.condition(filters.amount, PValue.getPValue(1), GCompare.GREATER));
        panel.applyConditions();

        panel.addCondition(GKeyStroke.createAddUserFilterKeyEvent(), false, true); // F3
        List<GFilterConditionView> rows = freeRows(panel);
        assertEquals(1, rows.size());
        assertValue(50, rowCondition(rows.get(0)).value.value);
        assertEquals(GCompare.GREATER, rowCondition(rows.get(0)).compare); // its comparison kept
        assertValue(1, filters.first(controller, filters.amount).value.value); // not sent
    }

    // ... on the column of a FILTER the platform draws: its fixed row takes it, prepared as a row of the panel's own is
    public void testF3PutsTheCellsValueIntoAFixedRow() {
        FiltersForm filters = new FiltersForm(true);
        filters.quantityFirst();
        filters.deliver(true);
        GFormController controller = filters.controller();
        GPlatformFilterInputController panel = (GPlatformFilterInputController) userFiltersController(controller, filters.group);

        panel.addCondition(GKeyStroke.createAddUserFilterKeyEvent(), false, true); // F3
        assertValue(2, rowCondition(panelRow(panel, filters.placed)).value.value);
        assertTrue(freeRows(panel).isEmpty());
        assertTrue(filters.sent(controller).isEmpty());
    }

    // Alt+F3: every condition off, sent at once, and a new row prepared with the cell's value - the only one
    public void testAltF3SendsTheResetAndPreparesANewRow() {
        FiltersForm filters = new FiltersForm(true);
        filters.plainFirst();
        filters.deliver(true);
        GFormController controller = filters.controller();
        GPlatformFilterInputController panel = (GPlatformFilterInputController) userFiltersController(controller, filters.group);
        filters.model(controller).add(filters.condition(filters.quantity, PValue.getPValue(1), GCompare.EQUALS));
        panel.applyConditions();
        assertTrue(filters.filters(controller, filters.quantity));

        panel.addCondition(GKeyStroke.createAddUserFilterKeyEvent(), true, false); // Alt+F3
        assertTrue(filters.sent(controller).isEmpty());
        List<GFilterConditionView> rows = freeRows(panel);
        assertEquals(1, rows.size());
        assertValue(50, rowCondition(rows.get(0)).value.value);
    }

    // typing in a column: its first row, or a new one, takes the cell's value as F3's does - the key starting the
    // editor -, prepared
    public void testTypingInAColumnPreparesARow() {
        FiltersForm filters = new FiltersForm(true);
        filters.plainFirst();
        filters.deliver(true);
        GFormController controller = filters.controller();
        GPlatformFilterInputController panel = (GPlatformFilterInputController) userFiltersController(controller, filters.group);

        panel.quickEditFilter(GKeyStroke.createAddUserFilterKeyEvent(), filters.amount, GGroupObjectValue.EMPTY);
        List<GFilterConditionView> rows = freeRows(panel);
        assertEquals(1, rows.size());
        assertValue(50, rowCondition(rows.get(0)).value.value);
        assertTrue(filters.sent(controller).isEmpty());
        panel.quickEditFilter(GKeyStroke.createAddUserFilterKeyEvent(), filters.amount, GGroupObjectValue.EMPTY);
        assertEquals(1, freeRows(panel).size());
    }

    // the controls' add: a new row on the current column every time, with the cell's value, prepared
    public void testTheControlsAddPreparesANewRowEveryTime() {
        FiltersForm filters = new FiltersForm(true);
        filters.plainFirst();
        filters.deliver(true);
        GFormController controller = filters.controller();
        GPlatformFilterInputController panel = (GPlatformFilterInputController) userFiltersController(controller, filters.group);

        panel.addCondition();
        panel.addCondition();
        List<GFilterConditionView> rows = freeRows(panel);
        assertEquals(2, rows.size());
        assertValue(50, rowCondition(rows.get(1)).value.value);
        assertTrue(filters.sent(controller).isEmpty());
    }

    // a value committed in a row - its cell's editor reporting it - is sent at once, in the list of every row applied
    // (a value, or NULL allowed), in the list's order: the FILTERs' places first, as the design declares them, then the
    // rows added
    public void testARowsValueIsSentAtOnceWithTheOtherRows() {
        FiltersForm filters = new FiltersForm(true);
        filters.plainFirst();
        GFormController controller = filters.controller();
        GPlatformFilterInputController panel = (GPlatformFilterInputController) userFiltersController(controller, filters.group);
        filters.model(controller).add(filters.condition(filters.amount, null, GCompare.EQUALS));
        changeRowValue(panelRow(panel, filters.placed), PValue.getPValue(3));
        assertEquals(Arrays.asList(filters.quantity, filters.amount), sentProperties(controller, filters)); // the amount row allows NULL

        changeRowValue(freeRows(panel).get(0), PValue.getPValue(7));
        assertValue(7, filters.first(controller, filters.amount).value.value);
        assertValue(3, filters.first(controller, filters.quantity).value.value);
    }

    // ... a comparison chosen in it, its negation, its junction, NULL allowed - each sent at once
    public void testARowsSelectorsSendAtOnce() {
        FiltersForm filters = new FiltersForm(true);
        filters.plainFirst();
        GFormController controller = filters.controller();
        GPlatformFilterInputController panel = (GPlatformFilterInputController) userFiltersController(controller, filters.group);
        filters.model(controller).add(filters.condition(filters.amount, PValue.getPValue(5), GCompare.EQUALS));
        GFilterConditionView row = freeRows(panel).get(0);

        ((GFilterCompareSelector) compareSelector(row)).valueChanged(GCompare.GREATER);
        assertEquals(GCompare.GREATER, filters.first(controller, filters.amount).compare);
        ((GFilterCompareSelector) compareSelector(row)).negationChanged(true);
        assertTrue(filters.first(controller, filters.amount).negation);
        junctionButton(row).getClickHandler().onClick(null);
        assertFalse(filters.first(controller, filters.amount).junction);
        changeRowValue(row, null); // emptied: NULL allowed, a free row's own default - "is empty"
        assertTrue(filters.first(controller, filters.amount).nullValue());
        ((GFilterCompareSelector) compareSelector(row)).allowNullChanged(false);
        assertFalse(filters.filters(controller, filters.amount)); // ... not allowed: sent no more, the row stays
        assertEquals(1, freeRows(panel).size());
    }

    // another property chosen in a row the panel adds by hand only lights its apply button, in either mode: the row's
    // value is to be entered anew, and the form's list keeps the condition it sent before
    public void testAnotherPropertyChosenInARowIsNotSent() {
        boolean manual = MainFrame.userFiltersManualApplyMode;
        try {
            for (boolean byHand : new boolean[]{false, true}) {
                MainFrame.userFiltersManualApplyMode = byHand;
                FiltersForm filters = new FiltersForm(true);
                filters.noFilters(); // no FILTER on either property: a slot would show the condition on its own
                filters.deliver(false);
                GFormController controller = filters.controller();
                GPlatformFilterInputController panel = (GPlatformFilterInputController) userFiltersController(controller, filters.group);
                filters.model(controller).add(filters.condition(filters.quantity, PValue.getPValue(5), GCompare.EQUALS));
                panel.setControlsVisible(true);
                panel.applyConditions();
                GFilterConditionView row = freeRows(panel).get(0);
                GToolbarButton apply = applyButton(filterControls(controller, filters));
                assertFalse(apply.getElement().hasClassName("active"));

                ((GFilterOptionSelector<Column>) propertySelector(row)).valueChanged(new Column(filters.price, GGroupObjectValue.EMPTY));
                assertSame(filters.price, rowCondition(row).column.property);
                assertValue(5, filters.first(controller, filters.quantity).value.value);
                assertFalse(filters.filters(controller, filters.price));
                assertTrue(apply.isEnabled());
                assertTrue(apply.getElement().hasClassName("active"));
            }
        } finally {
            MainFrame.userFiltersManualApplyMode = manual;
        }
    }

    // the server's list in another order: the box's rows follow it, laid out in its order
    public void testTheBoxsRowsFollowTheOrderOfTheList() {
        FiltersForm filters = new FiltersForm(true);
        filters.noFilters();
        filters.deliver(true);
        GFormController controller = filters.controller();
        GPlatformFilterInputController panel = (GPlatformFilterInputController) userFiltersController(controller, filters.group);
        GPropertyFilter price = filters.condition(filters.price, PValue.getPValue(5), GCompare.EQUALS);
        GPropertyFilter quantity = filters.condition(filters.quantity, PValue.getPValue(2), GCompare.EQUALS);
        filters.report(controller, price, quantity);
        filters.report(controller, quantity, price);
        List<GPropertyDraw> rows = new ArrayList<>(), laidOut = new ArrayList<>();
        for (GFilterConditionView row : freeRows(panel))
            rows.add(rowCondition(row).column.property);
        for (GComponent child : laidOut(controller.getFormLayout().getContainerView(filters.group.grid.filtersContainer)))
            laidOut.add(((GFilter) child).property);
        assertEquals(Arrays.asList(filters.quantity, filters.price), rows);
        assertEquals(rows, laidOut);
    }

    // F3 on an empty cell into a FILTER's row showing a condition: no value, so no condition - the row empties, a
    // FILTER's row still, not one of the box's; prepared, nothing sent
    public void testAnEmptyCellsValuePreparedInAFiltersRowEmptiesIt() {
        FiltersForm filters = new FiltersForm(true);
        GFormController controller = filters.controller();
        GPlatformFilterInputController panel = (GPlatformFilterInputController) userFiltersController(controller, filters.group);
        filters.report(controller, filters.condition(filters.quantity, PValue.getPValue(2), GCompare.EQUALS));
        filters.prepare(controller, filters.placed, null);
        assertTrue(freeRows(panel).isEmpty());
        assertTrue(rowCondition(panelRow(panel, filters.placed)).nullValue());
        assertTrue(filters.filters(controller, filters.quantity));
    }

    // CASE 7: a FILTER the platform draws over the conditions a React view draws - placed by the view (custom = NULL),
    // or standing outside React - is a row of the table's user filters as any FILTER's, drawn by the platform's rows of
    // the FILTERs beside the view's input: a value committed in it sent at once, a condition the view states shown in
    // it, the view's taking them off emptying it
    public void testAFilterThePlatformDrawsOverAViewsConditionsIsItsRow() {
        FiltersForm filters = new FiltersForm(true);
        filters.boxInBoard();
        filters.filterAligned();
        GFormController controller = filters.controller();
        GFilterInputController input = (GFilterInputController) userFiltersController(controller, filters.group);
        GFilterConditionView placed = panelRow(input, filters.placed), outside = panelRow(input, filters.outside);
        assertNotNull(placed);
        assertNotNull(outside);
        assertNotNull(controller.getFormLayout().getBaseComponentViews().get(filters.outside));

        changeRowValue(placed, PValue.getPValue(3));
        assertValue(3, filters.first(controller, filters.quantity).value.value);
        changeFilters(reactController(filters.view(controller)), "{\"property\": \"quantity\", \"value\": 4}");
        assertValue(4, rowCondition(placed).value.value);
        changeFilters(reactController(filters.view(controller)), "[]");
        assertTrue(rowCondition(placed).nullValue());
        assertTrue(filters.sent(controller).isEmpty());
    }

    // ... hidden by its SHOWIF from the start - the value with the form's first changes, which come before its row is
    // built with them -, its row standing outside React is hidden, over the platform's panel too (one a view places,
    // the view hides by its entry)
    public void testAFiltersRowHiddenFromTheStartIsHidden() {
        for (boolean overAView : new boolean[]{true, false}) {
            FiltersForm filters = new FiltersForm(true);
            if (overAView)
                filters.boxInBoard();
            filters.filterAligned();
            filters.deliver(true);
            filters.hiddenFromTheStart(filters.outside);
            GFormController controller = filters.controller();
            assertFalse(controller.getFormLayout().getBaseComponentViews().get(filters.outside).isVisible());
        }
    }

    // ... in the manual apply mode, the platform's controls shown by the toolbar's button - which shows the row's
    // selectors too -, a value committed in it waits for the apply, as any row's
    public void testAFilterThePlatformDrawsOverAViewWaitsForTheApplyInManualMode() {
        boolean manual = MainFrame.userFiltersManualApplyMode;
        MainFrame.userFiltersManualApplyMode = true;
        try {
            FiltersForm filters = new FiltersForm(true);
            filters.boxInBoard();
            GFormController controller = filters.controller();
            GFilterInputController input = (GFilterInputController) userFiltersController(controller, filters.group);
            GFilterConditionView placed = panelRow(input, filters.placed);
            assertFalse(compareSelector(placed).isVisible());
            input.setControlsVisible(true); // the toolbar's button
            assertTrue(compareSelector(placed).isVisible());
            changeRowValue(placed, PValue.getPValue(3));
            assertFalse(filters.filters(controller, filters.quantity));
            input.applyConditions();
            assertValue(3, filters.first(controller, filters.quantity).value.value);
        } finally {
            MainFrame.userFiltersManualApplyMode = manual;
        }
    }

    // ... and the view's input takes the table's keys: F3 on the FILTER's column puts the cell's value into its row,
    // written at once - a FILTER's value -; Escape in the row's selectors takes every condition off
    public void testF3OverAViewsConditionsPutsTheCellsValueIntoTheRowOfAFilterThePlatformDraws() {
        FiltersForm filters = new FiltersForm(true);
        filters.boxInBoard();
        filters.quantityFirst(); // the current column: quantity, the placed FILTER's
        filters.deliver(true);
        GFormController controller = filters.controller();
        GFilterInputController input = (GFilterInputController) userFiltersController(controller, filters.group);
        GFilterConditionView placed = panelRow(input, filters.placed);
        input.addCondition(null, false, true); // F3
        assertValue(2, filters.first(controller, filters.quantity).value.value);
        assertValue(2, rowCondition(placed).value.value);
        filterRows(input, filters.placed).resetConditions(); // Escape in the row's selectors (GFilterConditionView.UIHandler)
        assertTrue(filters.sent(controller).isEmpty());
        assertTrue(rowCondition(placed).nullValue());
    }

    // F3 and Alt+F3 PRESSED in a grid the platform draws over the conditions a view draws, in the manual apply mode:
    // the condition each key writes waits for the apply, as a view's own change does - F3's in the list, Alt+F3's
    // alone -, the view shown it, the form's list as the server reported it, no request sent; the apply sends it. The
    // controls the platform's - their apply button, which the mode comes with, hidden with them till each key shows
    // them, before it writes (the toolbar's button hides them between the two) - or a view's, their apply always there
    public void testF3AndAltF3OverAViewsConditionsWaitForTheApplyInManualMode() {
        boolean manual = MainFrame.userFiltersManualApplyMode;
        MainFrame.userFiltersManualApplyMode = true;
        try {
            for (boolean viewControls : new boolean[]{false, true}) {
                FiltersForm filters = new FiltersForm(true);
                filters.noFilters();
                filters.platformRows();
                filters.boxInBoard();
                if (viewControls)
                    filters.controlsInBoard();
                filters.deliver(true); // the grid on the first row, its first column price: 5
                GFormController controller = filters.controller();
                GFilterInputController input = ((GPlatformTableController<?, ?, ?>) tableController(controller, filters.group.grid)).getFilter();
                filters.report(controller, filters.condition(filters.quantity, PValue.getPValue(2), GCompare.EQUALS));
                assertEquals(viewControls, filters.model(controller).isManual()); // the platform's apply hidden with its controls

                // the element the grid takes its keys in, put on the page - a binding holds while its widget is
                // shown -, its widget not attached: a key pressed there reaches the form's bindings alone
                Element keys = platformGridView(controller, filters.group).getTableDataFocusElement();
                Element parent = keys.getParentElement();
                Node next = keys.getNextSibling();
                RootPanel.getBodyElement().appendChild(keys);
                long requests = nextRequestIndex(controller);
                try {
                    assertTrue(press(controller, keys, 114)); // F3
                    assertTrue(filters.model(controller).isManual());
                    assertEquals("quantity=2 price=5", viewConditions(filters.view(controller)));
                    input.setControlsVisible(false); // the toolbar's button: what waits stays
                    assertTrue(press(controller, keys, 114, true)); // Alt+F3
                    assertTrue(filters.model(controller).isManual());
                    assertEquals("price=5", viewConditions(filters.view(controller)));
                } finally {
                    parent.insertBefore(keys, next);
                }
                assertEquals(requests, nextRequestIndex(controller)); // nothing sent
                assertEquals(Collections.singletonList(filters.quantity), sentProperties(controller, filters));
                assertTrue(filters.model(controller).isPending());

                input.applyConditions(); // the controls' apply
                assertEquals(requests + 1, nextRequestIndex(controller));
                assertEquals(Collections.singletonList(filters.price), sentProperties(controller, filters));
                assertValue(5, filters.first(controller, filters.price).value.value);
                assertFalse(filters.model(controller).isPending());
            }
        } finally {
            MainFrame.userFiltersManualApplyMode = manual;
        }
    }

    // a FILTER's row bound to another entry - its own taken off, the next on its column shown - shows no loading of the
    // one it showed
    public void testAFiltersRowBoundToAnotherEntryIsNotLoading() {
        FiltersForm filters = new FiltersForm(true);
        GFormController controller = filters.controller();
        GPlatformFilterInputController panel = (GPlatformFilterInputController) userFiltersController(controller, filters.group);
        filters.report(controller, filters.condition(filters.quantity, PValue.getPValue(2), GCompare.EQUALS),
                filters.condition(filters.quantity, PValue.getPValue(1), GCompare.GREATER));
        GFilterConditionView row = panelRow(panel, filters.placed);
        changeRowValue(row, PValue.getPValue(4)); // sent: loading till the answer
        assertTrue(isLoading(controller, row));
        filters.model(controller).clearFilter(filters.placed);
        assertEquals(GCompare.GREATER, rowCondition(row).compare);
        assertFalse(isLoading(controller, row));
    }

    // a row deleted: the rest sent at once
    public void testARowDeletedSendsTheRest() {
        FiltersForm filters = new FiltersForm(true);
        filters.plainFirst();
        filters.noFilters(); // no FILTER on quantity: its slot would show its condition
        GFormController controller = filters.controller();
        GPlatformFilterInputController panel = (GPlatformFilterInputController) userFiltersController(controller, filters.group);
        filters.model(controller).add(filters.condition(filters.amount, PValue.getPValue(5), GCompare.EQUALS));
        filters.model(controller).add(filters.condition(filters.quantity, PValue.getPValue(2), GCompare.EQUALS));
        panel.applyConditions();

        deleteButton(freeRows(panel).get(0)).getClickHandler().onClick(null);
        assertEquals(Collections.singletonList(filters.quantity), sentProperties(controller, filters));
        assertEquals(1, freeRows(panel).size());
    }

    // Escape in the editor of a row no value was committed in takes the row away - the rest sent -; a row a value was
    // committed in stays
    public void testEscapeTakesAwayARowNoValueWasCommittedIn() {
        FiltersForm filters = new FiltersForm(true);
        filters.plainFirst();
        filters.deliver(true);
        GFormController controller = filters.controller();
        GPlatformFilterInputController panel = (GPlatformFilterInputController) userFiltersController(controller, filters.group);
        panel.addCondition(); // a new row, its value prepared, not committed
        GFilterConditionView row = freeRows(panel).get(0);

        cancelRowEditing(row, CancelReason.ESCAPE_PRESSED);
        assertTrue(freeRows(panel).isEmpty());
        panel.addCondition();
        row = freeRows(panel).get(0);
        changeRowValue(row, PValue.getPValue(9));
        cancelRowEditing(row, CancelReason.ESCAPE_PRESSED);
        assertEquals(1, freeRows(panel).size());
        assertValue(9, filters.first(controller, filters.amount).value.value);
    }

    // ... and a row of a condition the server has, or one sent, is the user's already: Escape in its editor cancels the
    // edit, the row stays
    public void testEscapeKeepsARowOfAConditionTheServerHasOrOneSent() {
        FiltersForm filters = new FiltersForm(true);
        filters.plainFirst();
        filters.deliver(true);
        GFormController controller = filters.controller();
        GPlatformFilterInputController panel = (GPlatformFilterInputController) userFiltersController(controller, filters.group);
        filters.report(controller, filters.condition(filters.amount, PValue.getPValue(50), GCompare.EQUALS));
        cancelRowEditing(freeRows(panel).get(0), CancelReason.ESCAPE_PRESSED);
        assertEquals(1, freeRows(panel).size());
        assertTrue(filters.filters(controller, filters.amount));

        panel.addCondition(); // a new row, its value prepared - not the user's yet -, sent by the apply: the user's then
        panel.applyConditions();
        cancelRowEditing(freeRows(panel).get(1), CancelReason.ESCAPE_PRESSED);
        assertEquals(2, freeRows(panel).size());
    }

    // a FILTER is never removed: its row has no delete button, the controls shown or not, its deletion asked all the
    // same takes nothing off; the reset empties it, and it stays
    public void testAFiltersRowIsNeverRemoved() {
        FiltersForm filters = new FiltersForm(true);
        GFormController controller = filters.controller();
        GPlatformFilterInputController panel = (GPlatformFilterInputController) userFiltersController(controller, filters.group);
        panel.setControlsVisible(true);
        GFilterConditionView row = panelRow(panel, filters.placed);
        assertFalse(deleteButton(row).isVisible());
        changeRowValue(row, PValue.getPValue(3));
        filterRows(panel, filters.placed).removeCondition(row);
        assertSame(row, panelRow(panel, filters.placed));
        assertTrue(filters.filters(controller, filters.quantity));
        panel.resetConditions();
        assertSame(row, panelRow(panel, filters.placed));
        assertNotNull(controller.getFormLayout().getBaseComponentViews().get(filters.placed));
    }

    // the reset - the controls' button, Shift+F3 -: every condition off, sent at once, the panel's own rows taken away,
    // its fixed rows emptied, kept
    public void testTheResetSendsAnEmptyListAndKeepsTheFixedRows() {
        FiltersForm filters = new FiltersForm(true);
        filters.plainFirst();
        GFormController controller = filters.controller();
        GPlatformFilterInputController panel = (GPlatformFilterInputController) userFiltersController(controller, filters.group);
        filters.model(controller).add(filters.condition(filters.amount, PValue.getPValue(5), GCompare.EQUALS));
        changeRowValue(panelRow(panel, filters.placed), PValue.getPValue(3));
        assertFalse(filters.sent(controller).isEmpty());

        panel.resetConditions();
        assertTrue(filters.sent(controller).isEmpty());
        assertTrue(freeRows(panel).isEmpty());
        assertNotNull(panelRow(panel, filters.placed));
        assertTrue(rowCondition(panelRow(panel, filters.placed)).nullValue());
    }

    // what a reset has to take off - Escape in the grid asks it: a row of the box's, a condition, or a change waiting
    public void testThePanelSaysWhetherItHasAnythingToReset() {
        FiltersForm filters = new FiltersForm(true);
        filters.plainFirst();
        GFormController controller = filters.controller();
        GPlatformFilterInputController panel = (GPlatformFilterInputController) userFiltersController(controller, filters.group);
        assertFalse(panel.hasConditionsToReset());
        changeRowValue(panelRow(panel, filters.placed), PValue.getPValue(3));
        assertTrue(panel.hasConditionsToReset());
        panel.resetConditions();
        assertFalse(panel.hasConditionsToReset());
        filters.model(controller).add(filters.condition(filters.amount, null, GCompare.EQUALS));
        assertTrue(panel.hasConditionsToReset());
    }

    // a row made while the table is hidden - one of the box's the model gets, the table hidden - is hidden with it, as
    // the rows there are, and shown with it again
    public void testARowMadeWhileTheTableIsHiddenIsHidden() {
        FiltersForm filters = new FiltersForm(true);
        filters.plainFirst();
        GFormController controller = filters.controller();
        GPlatformFilterInputController panel = (GPlatformFilterInputController) userFiltersController(controller, filters.group);
        panel.setVisible(false); // the table hidden
        filters.model(controller).add(filters.condition(filters.amount, null, GCompare.EQUALS));
        assertEquals(1, freeRows(panel).size());
        assertFalse(freeRows(panel).get(0).isVisible());
        panel.setVisible(true);
        assertTrue(freeRows(panel).get(0).isVisible());
    }

    // in the manual apply mode - its controls shown - a value committed in a row waits: the row holds it, the form's
    // list stays as it was, the apply button lit; pressed, it sends the rows and is no longer lit
    public void testARowsValueWaitsForTheApplyInManualMode() {
        boolean manual = MainFrame.userFiltersManualApplyMode;
        MainFrame.userFiltersManualApplyMode = true;
        try {
            FiltersForm filters = new FiltersForm(true);
            GFormController controller = filters.controller();
            GPlatformFilterInputController panel = (GPlatformFilterInputController) userFiltersController(controller, filters.group);
            panel.setControlsVisible(true);
            GFilterConditionView row = panelRow(panel, filters.placed);
            GToolbarButton apply = applyButton(filterControls(controller, filters));

            changeRowValue(row, PValue.getPValue(3));
            assertValue(3, rowCondition(row).value.value);
            assertTrue(filters.sent(controller).isEmpty());
            assertTrue(apply.isEnabled());
            assertTrue(apply.getElement().hasClassName("active"));
            apply.getClickHandler().onClick(null);
            assertValue(3, filters.first(controller, filters.quantity).value.value);
            assertFalse(apply.isEnabled());
            assertFalse(apply.getElement().hasClassName("active"));
        } finally {
            MainFrame.userFiltersManualApplyMode = manual;
        }
    }

    // ... a comparison chosen waits too: nothing sent, so nothing marked loading
    public void testARowsComparisonWaitsInManualMode() {
        boolean manual = MainFrame.userFiltersManualApplyMode;
        MainFrame.userFiltersManualApplyMode = true;
        try {
            FiltersForm filters = new FiltersForm(true);
            GFormController controller = filters.controller();
            GPlatformFilterInputController panel = (GPlatformFilterInputController) userFiltersController(controller, filters.group);
            panel.setControlsVisible(true);
            GFilterConditionView row = panelRow(panel, filters.placed);
            filters.prepare(controller, filters.placed, PValue.getPValue(3)); // a value in the row, not sent

            ((GFilterCompareSelector) compareSelector(row)).valueChanged(GCompare.GREATER);
            assertEquals(GCompare.GREATER, rowCondition(row).compare);
            assertTrue(filters.sent(controller).isEmpty());
            assertFalse(isLoading(controller, row));
        } finally {
            MainFrame.userFiltersManualApplyMode = manual;
        }
    }

    // ... and a FILTER's condition emptied waits, the server still filtering by it: there is that to reset - Escape in
    // the grid asks -, the reset sending the empty list at once
    public void testAnEmptiedFilterWaitingInManualModeIsThereToReset() {
        boolean manual = MainFrame.userFiltersManualApplyMode;
        MainFrame.userFiltersManualApplyMode = true;
        try {
            FiltersForm filters = new FiltersForm(true);
            GFormController controller = filters.controller();
            GPlatformFilterInputController panel = (GPlatformFilterInputController) userFiltersController(controller, filters.group);
            panel.setControlsVisible(true);
            GFilterConditionView row = panelRow(panel, filters.placed);
            changeRowValue(row, PValue.getPValue(3));
            panel.applyConditions();
            assertTrue(filters.filters(controller, filters.quantity));
            changeRowValue(row, null);
            assertTrue(filters.filters(controller, filters.quantity));
            assertTrue(panel.hasConditionsToReset());
            panel.resetConditions();
            assertFalse(filters.filters(controller, filters.quantity));
        } finally {
            MainFrame.userFiltersManualApplyMode = manual;
        }
    }

    // ... and one given back the value it was emptied of: nothing waits, and the server still filters by it - there is
    // that to reset all the same
    public void testAFilterGivenBackItsValueInManualModeIsThereToReset() {
        boolean manual = MainFrame.userFiltersManualApplyMode;
        MainFrame.userFiltersManualApplyMode = true;
        try {
            FiltersForm filters = new FiltersForm(true);
            GFormController controller = filters.controller();
            GPlatformFilterInputController panel = (GPlatformFilterInputController) userFiltersController(controller, filters.group);
            panel.setControlsVisible(true);
            GFilterConditionView row = panelRow(panel, filters.placed);
            changeRowValue(row, PValue.getPValue(3));
            panel.applyConditions();
            changeRowValue(row, null);
            changeRowValue(row, PValue.getPValue(3));
            assertFalse(filters.model(controller).isPending());
            assertTrue(filters.filters(controller, filters.quantity));
            assertTrue(panel.hasConditionsToReset());
            panel.resetConditions();
            assertFalse(filters.filters(controller, filters.quantity));
        } finally {
            MainFrame.userFiltersManualApplyMode = manual;
        }
    }

    // ... while a row deleted, the reset - Escape in a row's selector, with no popup shown -, and the drill-down are
    // sent at once, what waits sent with them
    public void testWhatIsSentAtOnceInManualMode() {
        boolean manual = MainFrame.userFiltersManualApplyMode;
        MainFrame.userFiltersManualApplyMode = true;
        try {
            FiltersForm filters = new FiltersForm(true);
            filters.plainFirst();
            GFormController controller = filters.controller();
            GPlatformFilterInputController panel = (GPlatformFilterInputController) userFiltersController(controller, filters.group);
            panel.setControlsVisible(true);
            filters.model(controller).add(filters.condition(filters.amount, PValue.getPValue(5), GCompare.GREATER));
            filters.model(controller).add(filters.condition(filters.amount, PValue.getPValue(9), GCompare.LESS)); // a range
            changeRowValue(panelRow(panel, filters.placed), PValue.getPValue(3)); // waits
            assertTrue(filters.sent(controller).isEmpty());

            deleteButton(freeRows(panel).get(1)).getClickHandler().onClick(null); // a row deleted
            assertEquals(Arrays.asList(filters.quantity, filters.amount), sentProperties(controller, filters));

            Widget box = controller.getFormLayout().getContainerView(filters.group.grid.filtersContainer).getView();
            RootPanel.get().add(box);
            try {
                compareSelector(freeRows(panel).get(0)).getElement().dispatchEvent(
                        Document.get().createKeyDownEvent(false, false, false, false, 27)); // Escape
                assertTrue(filters.sent(controller).isEmpty());
            } finally {
                RootPanel.get().remove(box);
            }

            filters.model(controller).replaceConditions(new ArrayList<>(Collections.singletonList(filters.condition(filters.amount, PValue.getPValue(4), GCompare.EQUALS)))); // a drill-down
            assertValue(4, filters.first(controller, filters.amount).value.value);
        } finally {
            MainFrame.userFiltersManualApplyMode = manual;
        }
    }

    // ... as Enter in a value cell with no editor open applies every row; and hiding the controls sends nothing - what
    // waits stays, the apply lit again once they are shown
    public void testEnterAppliesAndHidingTheControlsSendsNothingInManualMode() {
        boolean manual = MainFrame.userFiltersManualApplyMode;
        MainFrame.userFiltersManualApplyMode = true;
        try {
            FiltersForm filters = new FiltersForm(true);
            GFormController controller = filters.controller();
            GPlatformFilterInputController panel = (GPlatformFilterInputController) userFiltersController(controller, filters.group);
            panel.setControlsVisible(true);
            GFilterConditionView row = panelRow(panel, filters.placed);
            GToolbarButton apply = applyButton(filterControls(controller, filters));
            changeRowValue(row, PValue.getPValue(3));

            panel.setControlsVisible(false);
            assertTrue(filters.sent(controller).isEmpty());
            panel.setControlsVisible(true);
            assertTrue(apply.getElement().hasClassName("active"));

            RootPanel.get().add(row); // on the page: a binding holds while its widget is shown
            try {
                assertTrue(enter(controller, valueCell(row).getElement()));
            } finally {
                RootPanel.get().remove(row);
            }
            assertValue(3, filters.first(controller, filters.quantity).value.value);
        } finally {
            MainFrame.userFiltersManualApplyMode = manual;
        }
    }

    // a value committed in a row marks it loading till the answer to the list it is sent in; Enter, an apply of
    // every row, marks none; the rows sent are applied, a row prepared is not
    public void testARowIsLoadingTillTheAnswerAndAppliedOnceSent() {
        FiltersForm filters = new FiltersForm(true);
        filters.plainFirst();
        filters.deliver(true);
        GFormController controller = filters.controller();
        GPlatformFilterInputController panel = (GPlatformFilterInputController) userFiltersController(controller, filters.group);
        GFilterConditionView row = panelRow(panel, filters.placed);

        filters.prepare(controller, filters.placed, PValue.getPValue(3));
        assertFalse(applied(row));
        RootPanel.get().add(row);
        try {
            assertTrue(enter(controller, valueCell(row).getElement())); // Enter
        } finally {
            RootPanel.get().remove(row);
        }
        assertFalse(isLoading(controller, row));
        assertTrue(applied(row));
        changeRowValue(row, PValue.getPValue(4)); // a value committed
        assertTrue(isLoading(controller, row));
        controller.applyRemoteChanges(new GFormChanges(), Integer.MAX_VALUE); // the answer
        assertFalse(isLoading(controller, row));

        panel.addCondition(GKeyStroke.createAddUserFilterKeyEvent(), false, true); // F3: prepared
        assertFalse(applied(freeRows(panel).get(0)));
    }

    // a list the server reports: the panel's rows from it, in its order - a FILTER's showing the first condition on its
    // column (testAFixedRowTakesTheFirstConditionOnItsColumn) -, nothing sent, an entry kept where a condition of the
    // list is matched to it (an unchanged one, else one on its column), and its row with it; one equal to what the
    // panel sent last changes nothing
    public void testAServersListMakesThePanelsRowsAnewAndItsEchoChangesNothing() {
        FiltersForm filters = new FiltersForm(true);
        filters.plainFirst();
        GFormController controller = filters.controller();
        GPlatformFilterInputController panel = (GPlatformFilterInputController) userFiltersController(controller, filters.group);
        filters.model(controller).add(filters.condition(filters.amount, PValue.getPValue(5), GCompare.EQUALS));
        panel.applyConditions();
        GFilterConditionView row = freeRows(panel).get(0);

        filters.report(controller, filters.condition(filters.amount, PValue.getPValue(5), GCompare.EQUALS)); // the echo
        assertSame(row, freeRows(panel).get(0));
        filters.report(controller, filters.condition(filters.price, PValue.getPValue(1), GCompare.GREATER),
                filters.condition(filters.amount, PValue.getPValue(6), GCompare.EQUALS));
        List<GFilterConditionView> rows = freeRows(panel);
        assertEquals(1, rows.size()); // price's goes to the FILTER React draws
        assertSame(row, rows.get(0)); // amount's matched by its column: its entry, its row kept
        assertValue(6, rowCondition(rows.get(0)).value.value);
    }

    // the toolbar's filter button showing the controls with no row of the panel's own adds one, on the current column
    // - prepared -; the controls hidden once the panel has no row at all
    public void testShowingTheControlsAddsARowAndNoRowHidesThem() {
        FiltersForm filters = new FiltersForm(true);
        filters.plainFirst();
        filters.noFilters();
        filters.deliver(true);
        GFormController controller = filters.controller();
        GPlatformFilterInputController panel = (GPlatformFilterInputController) userFiltersController(controller, filters.group);

        panel.toggleControlsVisible();
        assertTrue(panel.isControlsVisible());
        assertEquals(1, freeRows(panel).size());
        assertTrue(filters.sent(controller).isEmpty());
        deleteButton(freeRows(panel).get(0)).getClickHandler().onClick(null);
        assertFalse(panel.isControlsVisible());
    }

    // the toolbar's filter button is there for what of the platform's it shows and hides that is in the form
    // (GFilterControlsToggle): its FILTERCONTROLS - none where the design REMOVEs them, or a container that holds them -,
    // or the rows of its FILTERS box laid out, whose selectors it shows with no FILTERCONTROLS too
    public void testTheFilterButtonIsThereForWhatOfTheFiltersIsInTheForm() {
        GForm controls = platformGridFilters(); // FILTERCONTROLS(items) in the form
        GGroupObject group = controls.groupObjects.get(0);
        into(controls.mainContainer, group.grid.filterControls);
        GFormController shown = controllerApart(controls);
        GToolbarButton button = filterButton(shown, group);
        assertNotNull(button);
        assertTrue(toolbarView(shown, group).getElement().isOrHasChild(button.getElement()));

        GForm removed = platformGridFilters(); // REMOVE FILTERCONTROLS(items)
        assertNull(filterButton(controllerApart(removed), removed.groupObjects.get(0)));

        GForm boxRemoved = platformGridFilters(); // REMOVE FILTERBOX(items), which holds them
        GGrid grid = boxRemoved.groupObjects.get(0).grid;
        GContainer filterBox = container(96, "FILTERBOX(items)", null);
        into(filterBox, grid.filtersContainer);
        into(filterBox, grid.filterControls);
        assertNull(filterButton(controllerApart(boxRemoved), grid.groupObject));

        GForm rows = platformGridFilters(); // REMOVE FILTERCONTROLS(items), FILTERS(items) in the form
        into(rows.mainContainer, rows.groupObjects.get(0).grid.filtersContainer);
        assertNotNull(filterButton(controllerApart(rows), rows.groupObjects.get(0)));
    }

    // a FILTER removed with FILTERS, FILTERBOX or another container that holds it is no slot of the form's user
    // filters, nor a row of the platform's. With FILTERBOX removed there is nothing for the toolbar's filter button
    // to show, nor an apply to wait for; the surviving controls or FILTERS box still have their button and mode
    public void testAFilterInARemovedContainerHasNoSlotOrPlatformRow() {
        boolean manual = MainFrame.userFiltersManualApplyMode;
        MainFrame.userFiltersManualApplyMode = true;
        try {
            for (String removed : new String[]{"FILTERS(items)", "FILTERBOX(items)", "holder"}) {
                FiltersForm filters = new FiltersForm(true);
                filters.noFilters();
                GGrid grid = filters.group.grid;
                grid.toolbar.visible = true;
                grid.toolbar.nativeSID = "c12";
                layout(grid.toolbar);
                filters.form.mainContainer.add(grid.toolbar);
                GFilter filter = filterIn(28, filters.price, grid.filtersContainer, grid);
                GContainer holder = grid.filtersContainer;
                if (removed.equals("FILTERBOX(items)")) {
                    holder = container(29, removed, filters.form.mainContainer);
                    holder.add(grid.filtersContainer);
                    holder.add(grid.filterControls);
                } else if (removed.equals("holder")) {
                    holder = container(29, removed, filters.form.mainContainer);
                    holder.add(filter);
                }
                holder.container.removeFromChildren(holder);
                assertNotNull(filter.container);
                assertFalse(filter.isInForm());
                GFormController controller = filters.controller();
                GPlatformFilterInputController panel = (GPlatformFilterInputController) userFiltersController(controller, filters.group);

                assertNull(removed + ": no FILTER slot", filters.model(controller).getSlotEntry(filter));
                assertNull(removed + ": no platform row", panelRow(panel, filter));
                assertNull(removed + ": no component view", controller.getFormLayout().getBaseComponentViews().get(filter));
                panel.setControlsVisible(true);
                if (removed.equals("FILTERBOX(items)")) {
                    assertNull(filterButton(controller, filters.group));
                    assertFalse(filters.model(controller).isManual());
                } else {
                    GToolbarButton button = filterButton(controller, filters.group);
                    assertNotNull(button);
                    assertTrue(toolbarView(controller, filters.group).getElement().isOrHasChild(button.getElement()));
                    assertTrue(filters.model(controller).isManual());
                }
            }
        } finally {
            MainFrame.userFiltersManualApplyMode = manual;
        }
    }

    // with FILTERS still laid out, F3 on the column of a FILTER removed with its holder prepares a row of that box,
    // not the detached FILTER's row. What the form sent stays till Apply, a value committed in manual mode too
    public void testF3OnAFilterInARemovedContainerUsesTheVisibleFiltersBox() {
        boolean manual = MainFrame.userFiltersManualApplyMode;
        MainFrame.userFiltersManualApplyMode = true;
        try {
            FiltersForm filters = new FiltersForm(true);
            filters.noFilters();
            GContainer holder = container(28, "holder", filters.form.mainContainer);
            GFilter filter = filterIn(29, filters.price, holder, filters.group.grid);
            filters.form.mainContainer.removeFromChildren(holder);
            assertNotNull(filter.container);
            filters.deliver(true);
            GFormController controller = filters.controller();
            GPlatformFilterInputController panel = (GPlatformFilterInputController) userFiltersController(controller, filters.group);
            filters.report(controller, filters.condition(filters.quantity, PValue.getPValue(2), GCompare.EQUALS));

            panel.setControlsVisible(true); // as the table's F3 does, before giving the key to its input
            panel.addCondition(GKeyStroke.createAddUserFilterKeyEvent(), false, true);
            List<GFilterConditionView> rows = freeRows(panel);
            assertEquals("F3 prepares a row in the surviving FILTERS box", 2, rows.size());
            GFilterConditionView row = rows.get(1);
            GPropertyFilter condition = rowCondition(row);
            assertSame(filters.price, condition.column.property);
            assertSame(filters.group.grid.filtersContainer, condition.filter.container);
            assertTrue(controller.getFormLayout().getContainerView(filters.group.grid.filtersContainer).getView()
                    .getElement().isOrHasChild(row.getElement()));
            assertValue(5, condition.value.value);
            assertEquals(Collections.singletonList(filters.quantity), sentProperties(controller, filters));
            assertValue(2, filters.first(controller, filters.quantity).value.value);
            assertTrue(filters.model(controller).isManual());
            changeRowValue(row, PValue.getPValue(6));
            assertEquals(Collections.singletonList(filters.quantity), sentProperties(controller, filters));
            panel.applyConditions();
            assertEquals(Arrays.asList(filters.quantity, filters.price), sentProperties(controller, filters));
            assertValue(6, filters.first(controller, filters.price).value.value);
        } finally {
            MainFrame.userFiltersManualApplyMode = manual;
        }
    }

    // a form of one grid the platform draws, GRID(items), its toolbar shown with the platform's filters, no FILTER, its
    // FILTERCONTROLS and FILTERS(items) REMOVEd till a test puts them into a container (into)
    private static GForm platformGridFilters() {
        GForm form = form("platformGridFilters");
        GGroupObject group = platformGrid(form, 90, "items");
        column(form, 95, "name", group);
        GGrid grid = group.grid;
        grid.toolbar.visible = true; grid.toolbar.showFilters = true;
        grid.filterControls.nativeSID = "c" + grid.filterControls.ID;
        layout(grid.filterControls);
        form.initialFormChanges = GFormChangesTest.noChanges();
        return form;
    }
    // ... a component put into a container
    private static void into(GContainer container, GComponent component) {
        component.container = container; container.children.add(component);
    }
    // ... and the platform's toolbar of the form's grid: its view, and the filter button on it - none where the toolbar
    // was given none (GPlatformToolbarController.addFilterToggle)
    private static native Widget toolbarView(GFormController controller, GGroupObject group) /*-{
        var toolbar = controller.@lsfusion.gwt.client.form.controller.GFormController::gridToolbarControllers.@lsfusion.gwt.client.base.jsni.NativeSIDMap::get(*)(group);
        return toolbar.@lsfusion.gwt.client.form.object.table.controller.GPlatformToolbarController::view;
    }-*/;
    private static native GToolbarButton filterButton(GFormController controller, GGroupObject group) /*-{
        var toolbar = controller.@lsfusion.gwt.client.form.controller.GFormController::gridToolbarControllers.@lsfusion.gwt.client.base.jsni.NativeSIDMap::get(*)(group);
        return toolbar.@lsfusion.gwt.client.form.object.table.controller.GPlatformToolbarController::filterButton;
    }-*/;

    // with FILTERCONTROLS REMOVEd the toolbar's button still shows the rows' selectors, and the manual apply mode comes
    // with them, Enter in a row's value cell its apply: a value committed in a row waits for it
    public void testARowsValueWaitsForEnterWithNoFilterControlsInManualMode() {
        boolean manual = MainFrame.userFiltersManualApplyMode;
        MainFrame.userFiltersManualApplyMode = true;
        try {
            FiltersForm filters = new FiltersForm(true);
            filters.noControls();
            GFormController controller = filters.controller();
            GPlatformFilterInputController panel = (GPlatformFilterInputController) userFiltersController(controller, filters.group);
            panel.setControlsVisible(true);
            GFilterConditionView row = panelRow(panel, filters.placed);
            changeRowValue(row, PValue.getPValue(3));
            assertTrue(filters.sent(controller).isEmpty());

            RootPanel.get().add(row); // on the page: a binding holds while its widget is shown
            try {
                assertTrue(enter(controller, valueCell(row).getElement()));
            } finally {
                RootPanel.get().remove(row);
            }
            assertValue(3, filters.first(controller, filters.quantity).value.value);
        } finally {
            MainFrame.userFiltersManualApplyMode = manual;
        }
    }

    // ... while with no row of the platform's either there is nothing for a button to show, nor a manual apply mode,
    // in which nothing would apply: what a view states for a FILTER it draws is sent at once, the controls asked to
    // show - as the button did - or not
    public void testNoFilterControlsAndNoRowOfThePlatformsBringNoManualMode() {
        boolean manual = MainFrame.userFiltersManualApplyMode;
        MainFrame.userFiltersManualApplyMode = true;
        try {
            FiltersForm filters = new FiltersForm(false); // FILTERS(items) REMOVEd
            filters.noControls();
            filters.board.children.remove(filters.placed); // no FILTER the platform draws: FILTER(price), React's, alone
            filters.group.grid.filters.remove(filters.placed);
            GFormController controller = filters.controller();
            GFilterInputController input = (GFilterInputController) userFiltersController(controller, filters.group);
            input.toggleControlsVisible();
            assertFalse(filters.model(controller).isManual());

            filters.change(controller, "price", 5.0);
            assertTrue(filters.filters(controller, filters.price));
        } finally {
            MainFrame.userFiltersManualApplyMode = manual;
        }
    }

    // a form of a group whose rows React draws, its filters the platform's, with the FILTERs of two of its properties
    // moved into a react container - one React draws, MOVE FILTER(price), and one the platform's panel does,
    // MOVE FILTER(quantity) { custom = NULL; } -, FILTERS(items) in the form or REMOVEd, FILTERCONTROLS(items) in the
    // form (or REMOVEd: noControls) - and one of a string property standing outside React (filterAligned)
    private static final class FiltersForm {
        final GForm form = new GForm();
        final GGroupObject group = new GGroupObject();
        final GPropertyDraw price, quantity;
        final GContainer board;
        final GFilter drawn, placed;
        GPropertyDraw name; // a string property, its FILTER standing outside React - only in filterAligned
        GFilter outside;
        GPropertyDraw amount; // a column with no FILTER, the first one - only in plainFirst

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
            grid.filterControls.ID = 13; grid.filterControls.nativeSID = "c13"; grid.filterControls.sID = "FILTERCONTROLS(items)";
            layout(grid.filterControls);
            grid.filterControls.container = form.mainContainer; form.mainContainer.children.add(grid.filterControls);
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
            return new GFormController(formsController(), null, formContainer(), form, false, "propertyFilters", 0, null);
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
            // price 5 and 7, quantity 2 and 4, amount 50 and 52
            List<GPropertyDraw> columns = new ArrayList<>(Arrays.asList(price, quantity));
            if (amount != null)
                columns.add(amount);
            changes.properties = new GPropertyReaderDTO[columns.size()];
            changes.propertiesValueKeys = new GGroupObjectValue[columns.size()][];
            changes.propertiesValueValues = new Serializable[columns.size()][];
            for (int i = 0; i < columns.size(); i++) {
                GPropertyDraw column = columns.get(i);
                int first = column == price ? 5 : column == quantity ? 2 : 50;
                changes.properties[i] = new GPropertyReaderDTO(column.ID, DRAW, -1);
                changes.propertiesValueKeys[i] = rows ? new GGroupObjectValue[] {one, two} : new GGroupObjectValue[0];
                changes.propertiesValueValues[i] = rows ? new Serializable[] {first, first + 2} : new Serializable[0];
            }
            form.initialFormChanges = changes;
        }
        // a column with no FILTER, first in the form: the one a new condition starts on, a view having no current
        // column
        void plainFirst() {
            amount = column(form, 27, "amount", group);
            form.propertyDraws.remove(amount);
            form.propertyDraws.add(0, amount);
        }
        // ... or quantity first: the column of the FILTER the platform draws
        void quantityFirst() {
            form.propertyDraws.remove(quantity);
            form.propertyDraws.add(0, quantity);
        }
        // ... and a component hidden by its SHOWIF from the start, with the form's first changes - after deliver
        void hiddenFromTheStart(GComponent component) {
            GFormChangesDTO changes = form.initialFormChanges;
            int count = changes.properties.length;
            changes.properties = Arrays.copyOf(changes.properties, count + 1);
            changes.propertiesValueKeys = Arrays.copyOf(changes.propertiesValueKeys, count + 1);
            changes.propertiesValueValues = Arrays.copyOf(changes.propertiesValueValues, count + 1);
            changes.properties[count] = new GPropertyReaderDTO(component.ID, GFormChanges.GPropertyReadType.COMPONENT_SHOWIF, -1);
            changes.propertiesValueKeys[count] = new GGroupObjectValue[] {GGroupObjectValue.EMPTY};
            changes.propertiesValueValues[count] = new Serializable[] {true};
        }
        // ... or FILTERS(items) drawn by the board - the conditions a view draws -, the platform drawing the FILTER it
        // places over them (placed), and one standing outside React (filterAligned)
        void boxInBoard() {
            GContainer box = group.grid.filtersContainer;
            if (box.container != null)
                box.container.children.remove(box);
            box.container = board; board.children.add(box);
        }
        // ... and FILTERCONTROLS(items) drawn by the board too: the controls a view draws, their apply always there
        void controlsInBoard() {
            GFilterControls controls = group.grid.filterControls;
            form.mainContainer.children.remove(controls);
            controls.container = board; board.children.add(controls);
        }
        // ... and the rows the platform's - GRID(items) with no view of its own, shown as a grid, its cells drawn -: a
        // grid that takes the table's keys
        void platformRows() {
            group.grid.custom = null; group.grid.react = false;
            group.listViewType = GListViewType.GRID;
            price.cellType = price.valueType; quantity.cellType = quantity.valueType;
        }
        // ... and no FILTER at all: the panel's rows its own alone
        void noFilters() {
            group.grid.filters.clear();
            board.children.clear();
        }
        // ... or no FILTERCONTROLS: REMOVE FILTERCONTROLS(items)
        void noControls() {
            GFilterControls controls = group.grid.filterControls;
            form.mainContainer.children.remove(controls);
            controls.container = null;
        }
        // the form's list, as it sent it last - or the server reported it -, and the first condition on a property in
        // it
        List<GPropertyFilter> sent(GFormController controller) {
            return GPropertyFilter.getConditions(form, group, controller.getFilterDTOs(group));
        }
        GPropertyFilter first(GFormController controller, GPropertyDraw property) {
            for (GPropertyFilter condition : sent(controller))
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
            return condition(price, value, price.getDefaultCompare());
        }
        // ... and one on a property, compared so
        GPropertyFilter condition(GPropertyDraw property, PValue value, GCompare compare) {
            return new GPropertyFilter(new GFilter(property), group, GGroupObjectValue.EMPTY, value, false, compare, true);
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
        // the model of the group's user filters, and the condition a FILTER shows in it - none while it has none
        GUserFiltersController model(GFormController controller) {
            return controller.getUserFiltersController(group);
        }
        GPropertyFilter slot(GFormController controller, GFilter filter) {
            GUserFilter entry = model(controller).getSlotEntry(filter);
            return entry != null && entry.isActive() ? entry.condition : null;
        }
        // ... a value put into a FILTER's entry, prepared - as F3 puts the cell's -, not sent
        void prepare(GFormController controller, GFilter filter, PValue value) {
            model(controller).prepareValue(model(controller).getSlotEntry(filter), value);
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
        var rows = controller.@lsfusion.gwt.client.form.controller.GFormController::tableControllers.@lsfusion.gwt.client.base.jsni.NativeSIDMap::get(*)(group.@lsfusion.gwt.client.form.object.GGroupObject::grid);
        return rows.@lsfusion.gwt.client.form.design.view.GReactFormData.TablePart::filter;
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
    // ... and the conditions of the group's list it draws, as its view is given them now: each one's property,
    // comparison and value
    private static native String viewConditions(Object view) /*-{
        var state = view.@lsfusion.gwt.client.form.design.view.ReactContainerView::state;
        return state.@lsfusion.gwt.client.form.design.view.GReactFormData.ViewState::current()().items.filters.map(function (condition) {
            return condition.property + condition.compare + condition.value;
        }).join(' ');
    }-*/;
    // the platform's rows of a FILTER of a table - what reports its row's events -, none where React draws it, read
    // where the platform keeps its rows of the table's user filters, which its input's toggle drives; and its row: its
    // caption - the widget, the label of its property in it -, its comparison label, its value cell and the comparison
    // its value is edited by, read where the client keeps them
    private static GPlatformFilterController filterRows(GFilterInputController input, GFilter filter) {
        return filterRowsOf(input).get(filter);
    }
    private static native NativeSIDMap<GFilter, GPlatformFilterController> filterRowsOf(GFilterInputController input) /*-{
        var toggle = input.@lsfusion.gwt.client.form.filter.user.controller.GFilterInputController::toggle;
        var rows = toggle.@lsfusion.gwt.client.form.filter.user.controller.GFilterControlsToggle::rows;
        return rows.@lsfusion.gwt.client.form.filter.user.controller.GPlatformFilterRows::filters;
    }-*/;
    private static GFilterConditionView panelRow(GFilterInputController input, GFilter filter) {
        GPlatformFilterController rows = filterRows(input, filter);
        return rows != null ? rowOf(rows) : null;
    }
    private static native GFilterConditionView rowOf(GPlatformFilterController rows) /*-{
        return rows.@lsfusion.gwt.client.form.filter.user.controller.GPlatformFilterController::row;
    }-*/;
    // a list a view states through the group's `filters` member, as JSON
    private static native void changeFilters(JavaScriptObject controller, String conditions) /*-{
        controller.items.filters.change(JSON.parse(conditions));
    }-*/;
    private static native Widget rowCaption(GFilterConditionView row) /*-{
        return row.@lsfusion.gwt.client.form.filter.user.view.GFilterConditionView::leftPanel;
    }-*/;
    private static native Label propertyLabel(GFilterConditionView row) /*-{
        return row.@lsfusion.gwt.client.form.filter.user.view.GFilterConditionView::propertyLabel;
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
    // whether a row is the last one of its group with a condition after it, its junction hidden
    private static native boolean isLast(GFilterConditionView row) /*-{
        return row.@lsfusion.gwt.client.form.filter.user.view.GFilterConditionView::isLast;
    }-*/;
    // the rows of the panel's own - the FILTERS box's, none of a FILTER's -, in its order, read where it keeps them
    private static native NativeHashMap<GUserFilter, GFilterConditionView> boxRows(GPlatformFilterInputController panel) /*-{
        var rows = panel.@lsfusion.gwt.client.form.filter.user.controller.GPlatformFilterInputController::platformRows;
        var box = rows.@lsfusion.gwt.client.form.filter.user.controller.GPlatformFilterRows::box;
        return box.@lsfusion.gwt.client.form.filter.user.controller.GPlatformFiltersController::boxRows;
    }-*/;
    private static List<GFilterConditionView> freeRows(GPlatformFilterInputController panel) {
        List<GFilterConditionView> rows = new ArrayList<>();
        boxRows(panel).foreachValue(rows::add);
        return rows;
    }
    // the components a container's view lays out, in its order - its own list, not the design's
    private static native List<GComponent> laidOut(Object view) /*-{
        return view.@lsfusion.gwt.client.form.design.view.GAbstractContainerView::children;
    }-*/;
    // ... a row's condition, as the row holds it
    private static native GPropertyFilter rowCondition(GFilterConditionView row) /*-{
        return row.@lsfusion.gwt.client.form.filter.user.view.GFilterConditionView::condition;
    }-*/;
    // ... a value committed in its cell, as its editor commits one - the cell shows it, then reports it -, and the
    // editor cancelled
    private static void changeRowValue(GFilterConditionView row, PValue value) {
        ((GDataFilterPropertyValue) valueCell(row)).changeProperty(value, null);
    }
    private static native void cancelRowEditing(GFilterConditionView row, CancelReason reason) /*-{
        var view = row.@lsfusion.gwt.client.form.filter.user.view.GFilterConditionView::valueView;
        view.@lsfusion.gwt.client.form.filter.user.view.GDataFilterValueView::editingCancelled(*)(reason);
    }-*/;
    // ... whether it shows its condition applied - in the list sent last, or the server's -, as its value cell marks it
    private static boolean applied(GFilterConditionView row) {
        return valueCell(row).getElement().hasClassName("filter-applied");
    }
    // ... its delete and junction buttons
    private static native GToolbarButton deleteButton(GFilterConditionView row) /*-{
        return row.@lsfusion.gwt.client.form.filter.user.view.GFilterConditionView::deleteButton;
    }-*/;
    private static native GToolbarButton junctionButton(GFilterConditionView row) /*-{
        return row.@lsfusion.gwt.client.form.filter.user.view.GFilterConditionView::junctionView;
    }-*/;
    // ... whether it is marked loading, waiting for the answer to the list it was sent in
    private static native boolean isLoading(GFormController controller, GFilterConditionView row) /*-{
        return controller.@lsfusion.gwt.client.form.controller.GFormController::pendingLoadingFilterRequests.@lsfusion.gwt.client.base.jsni.NativeSIDMap::get(*)(row) != null;
    }-*/;
    // the platform's filter controls of the group, and their apply button, read where they keep it
    private static Widget filterControls(GFormController controller, FiltersForm filters) {
        return controller.getFormLayout().getBaseComponentViews().get(filters.group.grid.filterControls).getShowingWidget();
    }
    private static native GToolbarButton applyButton(Widget controls) /*-{
        return controls.@lsfusion.gwt.client.form.filter.user.view.GFilterControlsView::applyButton;
    }-*/;
    // the properties of the conditions the form sent last, in their order
    private static List<GPropertyDraw> sentProperties(GFormController controller, FiltersForm filters) {
        List<GPropertyDraw> properties = new ArrayList<>();
        for (GPropertyFilter condition : filters.sent(controller))
            properties.add(condition.column.property);
        return properties;
    }
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
    // ... and a property's FILTER of a table - a grid, a tree -, FILTER(<property>), in it
    private static GFilter filterIn(int id, GPropertyDraw property, GContainer parent, GGridProperty table) {
        GFilter filter = new GFilter(property);
        filter.ID = id; filter.nativeSID = "c" + id; filter.sID = "FILTER(" + property.sID + ")";
        layout(filter);
        filter.container = parent; parent.children.add(filter);
        table.filters.add(filter);
        return filter;
    }

    // what the server gives every component of a design to be laid out by
    private static void layout(GComponent component) {
        component.span = 1;
        component.setAlignment(GFlexAlignment.STRETCH);
        component.captionAlignmentHorz = GFlexAlignment.START; component.captionAlignmentVert = GFlexAlignment.START;
    }

    private static GPropertyDraw column(GForm form, int id, String name, GGroupObject group) {
        return column(form, id, name, group, new GPropertyDraw());
    }
    private static GPropertyDraw column(GForm form, int id, String name, GGroupObject group, GPropertyDraw property) {
        property.ID = id; property.nativeSID = "p" + id; property.sID = name; property.integrationSID = name;
        property.propertyFormName = name; property.groupObject = group; property.isList = true;
        property.valueType = GIntegerType.instance; property.valueWidth = 80; property.valueHeight = 20;
        property.valueAlignmentHorz = GFlexAlignment.END; property.valueAlignmentVert = GFlexAlignment.CENTER;
        property.loadingReader = new GLoadingReader(id, group != null ? group.ID : -1); // as the server gives every one
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
