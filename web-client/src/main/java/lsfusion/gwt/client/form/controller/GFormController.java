package lsfusion.gwt.client.form.controller;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.*;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.*;
import lsfusion.gwt.client.action.GAction;
import lsfusion.gwt.client.action.GActionDispatcherLookAhead;
import lsfusion.gwt.client.action.GMessageAction;
import lsfusion.gwt.client.base.*;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.jsni.NativeSIDMap;
import lsfusion.gwt.client.base.result.ListResult;
import lsfusion.gwt.client.base.result.NumberResult;
import lsfusion.gwt.client.base.result.VoidResult;
import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.base.view.*;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.classes.GType;
import lsfusion.gwt.client.controller.SmartScheduler;
import lsfusion.gwt.client.controller.dispatch.GwtActionDispatcher;
import lsfusion.gwt.client.controller.remote.DeferredRunner;
import lsfusion.gwt.client.controller.remote.action.*;
import lsfusion.gwt.client.controller.remote.action.form.*;
import lsfusion.gwt.client.controller.remote.action.form.SelectAll;
import lsfusion.gwt.client.controller.remote.action.logics.GenerateID;
import lsfusion.gwt.client.controller.remote.action.logics.GenerateIDResult;
import lsfusion.gwt.client.controller.remote.action.navigator.GainedFocus;
import lsfusion.gwt.client.controller.remote.action.navigator.VoidFormAction;
import lsfusion.gwt.client.form.ContainerForm;
import lsfusion.gwt.client.form.GUpdateMode;
import lsfusion.gwt.client.form.controller.dispatch.ExceptionResult;
import lsfusion.gwt.client.form.controller.dispatch.FormDispatchAsync;
import lsfusion.gwt.client.form.controller.dispatch.GFormActionDispatcher;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.design.view.*;
import lsfusion.gwt.client.form.event.*;
import lsfusion.gwt.client.form.filter.GRegularFilter;
import lsfusion.gwt.client.form.filter.GRegularFilterGroup;
import lsfusion.gwt.client.form.filter.user.*;
import lsfusion.gwt.client.form.filter.user.view.GFilterConditionView;
import lsfusion.gwt.client.form.object.*;
import lsfusion.gwt.client.form.object.panel.controller.GPanelController;
import lsfusion.gwt.client.form.object.table.controller.GAbstractTableController;
import lsfusion.gwt.client.form.object.table.controller.GComponentController;
import lsfusion.gwt.client.form.object.table.controller.GGroupController;
import lsfusion.gwt.client.form.object.table.controller.GPropertyController;
import lsfusion.gwt.client.form.object.table.controller.GLayoutController;
import lsfusion.gwt.client.form.object.table.controller.GLsfPropertyController;
import lsfusion.gwt.client.form.object.table.grid.controller.GGridController;
import lsfusion.gwt.client.form.object.table.grid.user.design.GColumnUserPreferences;
import lsfusion.gwt.client.form.object.table.grid.user.design.GFormUserPreferences;
import lsfusion.gwt.client.form.object.table.grid.user.design.GGridUserPreferences;
import lsfusion.gwt.client.form.object.table.grid.user.design.GGroupObjectUserPreferences;
import lsfusion.gwt.client.form.object.table.grid.view.GGridTable;
import lsfusion.gwt.client.form.object.panel.controller.GGridPanelController;
import lsfusion.gwt.client.form.object.table.grid.view.GListViewType;
import lsfusion.gwt.client.form.object.table.grid.view.GSimpleStateTableView;
import lsfusion.gwt.client.form.object.table.grid.view.GStateTableView;
import lsfusion.gwt.client.form.object.table.tree.GTreeGroup;
import lsfusion.gwt.client.form.object.table.tree.controller.GTreeGroupController;
import lsfusion.gwt.client.form.object.table.view.GridDataRecord;
import lsfusion.gwt.client.form.property.*;
import lsfusion.gwt.client.form.property.async.*;
import lsfusion.gwt.client.form.property.cell.GEditBindingMap;
import lsfusion.gwt.client.form.property.cell.view.RendererType;
import lsfusion.gwt.client.form.property.cell.classes.controller.CustomReplaceCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.controller.RequestCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.controller.RequestValueCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.view.InputBasedCellRenderer;
import lsfusion.gwt.client.form.property.cell.classes.view.LogicalCellRenderer;
import lsfusion.gwt.client.form.property.cell.controller.*;
import lsfusion.gwt.client.form.property.cell.view.*;
import lsfusion.gwt.client.form.property.panel.view.ActionPanelRenderer;
import lsfusion.gwt.client.form.property.table.view.GPropertyContextMenuPopup;
import lsfusion.gwt.client.form.view.Column;
import lsfusion.gwt.client.form.view.FormContainer;
import lsfusion.gwt.client.form.view.FormDockable;
import lsfusion.gwt.client.form.view.ModalForm;
import lsfusion.gwt.client.navigator.controller.GAsyncFormController;
import lsfusion.gwt.client.view.MainFrame;
import lsfusion.interop.action.ServerResponse;
import net.customware.gwt.dispatch.shared.Result;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.text.ParseException;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

import static lsfusion.gwt.client.base.GwtClientUtils.*;
import static lsfusion.gwt.client.base.GwtSharedUtils.putToDoubleNativeMap;
import static lsfusion.gwt.client.form.design.view.GReactFormData.controllerPrefix;
import static lsfusion.gwt.client.base.GwtSharedUtils.removeFromDoubleMap;
import static lsfusion.gwt.client.form.property.cell.GEditBindingMap.isChangeEvent;

public class GFormController implements EditManager, GReactFormData.Verbs {

    private static final ClientMessages messages = ClientMessages.Instance.get();

    private final FormDispatchAsync dispatcher;

    public int getDispatchPriority() {
        return dispatcher.dispatchPriority;
    }

    private final GFormActionDispatcher actionDispatcher;

    private final FormsController formsController;
    private final FormContainer formContainer;

    public final GForm form;
    public GFormLayout formLayout;

    private final GReactFormData reactData; // CUSTOM REACT: projected @lsfusion/core data

    private final boolean isDialog;

    private Event editEvent;

    private final NativeSIDMap<GGroupObject, ArrayList<GGroupObjectValue>> currentGridObjects = new NativeSIDMap<>();

    public NativeSIDMap<GGroupObject, ArrayList<GGroupObjectValue>> getCurrentGridObjects() {
        return currentGridObjects;
    }

    // the user orders of each group as the form has them - as a client set them last, or as the server reported them -:
    // the group's controller gets other orders only, and a pivot starts sorted by them; true - ascending
    private final NativeSIDMap<GGroupObject, LinkedHashMap<Column, Boolean>> currentOrders = new NativeSIDMap<>();
    // the user filters of each group as the server has them, as far as the client knows: as it sent them last, or as
    // the server reported them
    private final NativeSIDMap<GGroupObject, ArrayList<GPropertyFilterDTO>> currentFilters = new NativeSIDMap<>();

    private final LinkedHashMap<GGroupObject, GGridController> gridControllers = new LinkedHashMap<>();
    private final LinkedHashMap<GTreeGroup, GTreeGroupController> treeControllers = new LinkedHashMap<>();
    // THE FORM'S CONTROLLER OF EACH GROUP, PROPERTY AND COMPONENT - where all it sends for one goes: a group's rows and
    // current object, a property's values and drop, what every reader brings. Settled once by
    // initializeOwnerControllers: who draws what is design data, so one asked for per delta re-derived what the
    // controllers themselves were built from.
    private final LinkedHashMap<GGroupObject, GGroupController> groupControllers = new LinkedHashMap<>();
    private final LinkedHashMap<GPropertyDraw, GPropertyController> propertyControllers = new LinkedHashMap<>();
    private final LinkedHashMap<GComponent, GComponentController> componentControllers = new LinkedHashMap<>();
    // groups with LSF properties
    private final LinkedHashMap<GGroupObject, GGridPanelController> gridPanelControllers = new LinkedHashMap<>();
    public GPanelController panelController;
    // the platform's controller of the components it draws, one for the form: a component reader names its component
    private final GLayoutController layoutController = new GLayoutController(this);

    private final NativeSIDMap<GGroupObject, ArrayList<Widget>> filterViews = new NativeSIDMap<>();

    private final ArrayList<ModifyObject> pendingModifyObjectRequests = new ArrayList<>();
    private final NativeSIDMap<GGroupObject, Long> pendingChangeCurrentObjectsRequests = new NativeSIDMap<>();
    private final NativeSIDMap<GGroupObject, Long> pendingChangeOrdersRequests = new NativeSIDMap<>();
    private final NativeSIDMap<GGroupObject, Long> pendingChangeFiltersRequests = new NativeSIDMap<>();
    private final NativeSIDMap<GPropertyReader, NativeHashMap<GGroupObjectValue, Change>> pendingChangePropertyRequests = new NativeSIDMap<>(); // assert that should contain columnKeys + list keys if property is in list
    private final NativeSIDMap<GPropertyDraw, NativeHashMap<GGroupObjectValue, Long>> pendingLoadingPropertyRequests = new NativeSIDMap<>(); // assert that should contain columnKeys + list keys if property is in list
    private final NativeSIDMap<GFilterConditionView, Long> pendingLoadingFilterRequests = new NativeSIDMap<>();

    private boolean hasColumnGroupObjects;

    private boolean needConfirm;

    public FormsController getFormsController() {
        return formsController;
    }

    private final Set<ContainerForm> containerForms = new HashSet<>();

    public void addContainerForm(ContainerForm containerForm) {
        containerForms.add(containerForm);
    }

    public void removeContainerForm(ContainerForm containerForm) {
        containerForms.remove(containerForm);
    }

    private static int idCounter = 0;
    // we need the global id to make ids globally unique in some cases
    public String globalID;

    public String formId;

    public WindowHiddenHandler hiddenHandler;

    public GFormController(FormsController formsController, WindowHiddenHandler hiddenHandler, FormContainer formContainer, GForm gForm, boolean isDialog, String formId, int dispatchPriority, Event editEvent) {
        actionDispatcher = new GFormActionDispatcher(this);

        this.hiddenHandler = hiddenHandler;

        this.formsController = formsController;
        this.formContainer = formContainer;
        this.form = gForm;
        this.isDialog = isDialog;

        this.formId = formId;
        this.globalID = "" + (idCounter++);

        dispatcher = new FormDispatchAsync(this, dispatchPriority);

        // the form's projection: empty on a form with no react container, since a react container's view makes its
        // state in it (addReactContainer). A call through a member is the form's own edit: the form is its Verbs
        reactData = new GReactFormData(form, GComponent::getReactPlace, this);
        formLayout = new GFormLayout(this, form.mainContainer);
        if (form.sID != null)
            formLayout.getElement().setAttribute("lsfusion-form", form.sID);

        this.editEvent = editEvent;

        initializeParams(); // has to be done before initializeControllers (since adding component uses getSize)

        initializeControllers();

        initializeRegularFilters();

        if (form.initialFormChanges != null) {
            applyRemoteChanges(form.initialFormChanges);
            form.initialFormChanges = null;
        } else
            getRemoteChanges();

        initializeFormSchedulers();
    }

    public void checkGlobalMouseEvent(Event event) {
        checkFormEvent(new EventHandler(event), (handler, preview) -> checkMouseEvent(handler, preview, false, false, false));
    }

    public Event popEditEvent() {
        Event result = editEvent;
        editEvent = null;
        return result;
    }

    public void checkFocusElement(boolean isFocused, Element renderElement) {
        focusedCustom = isFocused && renderElement != null && CellRenderer.isCustomElement(renderElement) ? renderElement : null;
    }

    private interface CheckEvent {
        void accept(EventHandler handler, boolean preview);
    }
    private static void checkFormEvent(EventHandler handler, CheckEvent preview) {
        preview.accept(handler, true); // the problem is that now we check preview twice (however it's not that big overhead, so so far will leave it this way)
        if(handler.consumed)
            return;

        preview.accept(handler, false);
    }

    public void checkMouseEvent(EventHandler handler, boolean preview, boolean isCell, boolean panel, boolean stopPreventingDblclickEvent) {
        if(GMouseStroke.isDblDownEvent(handler.event) && !stopPreventingDblclickEvent && !isEditing())
            handler.event.preventDefault(); //need to prevent selection by double mousedown event
        else if(GMouseStroke.isChangeEvent(handler.event) || GMouseStroke.isDoubleChangeEvent(handler.event))
            processBinding(handler, preview, isCell, panel);
    }
    public void checkKeyEvent(EventHandler handler, boolean preview, boolean isCell, boolean panel) {
        if(GKeyStroke.isKeyEvent(handler.event))
            processBinding(handler, preview, isCell, panel);
    }
    private static void checkGlobalKeyEvent(DomEvent event, FormsController formsController, Supplier<GFormController> currentForm) {
        NativeEvent nativeEvent = event.getNativeEvent();
        if (nativeEvent instanceof Event) { // just in case
            EventHandler eventHandler = new EventHandler((Event) nativeEvent);
            GFormController form = currentForm.get();
            if (form != null)
                checkFormEvent(eventHandler, (handler, preview) -> form.checkKeyEvent(handler, preview, false, false));
            if (!eventHandler.consumed && !MainFrame.isModalPopup()) //ignore if modal window
                formsController.processBinding(eventHandler);
        }
    }
    public void checkMouseKeyEvent(EventHandler handler, boolean preview, boolean isCell, boolean panel, boolean customRenderer) {
        if(MainFrame.isModalPopup())
            return;

        checkMouseEvent(handler, preview, isCell, panel, customRenderer);
        if(handler.consumed)
            return;

        checkKeyEvent(handler, preview, isCell, panel);
    }

    public static void checkKeyEvents(DomEvent event, FormsController formsController) {
        NativeEvent nativeEvent = event.getNativeEvent();
        formsController.checkEditModeEvents(nativeEvent);

        if(GKeyStroke.isSwitchFullScreenModeEvent(nativeEvent) && !MainFrame.mobile) {
            formsController.switchFullScreenMode();
        }
    }

    // will handle key events in upper container which will be better from UX point of view
    public static void initKeyEventHandler(Widget widget, FormsController formsController, Supplier<GFormController> currentForm) {
        widget.addDomHandler(event -> {
            checkGlobalKeyEvent(event, formsController, currentForm);
            checkKeyEvents(event, formsController);
        }, KeyDownEvent.getType());
        widget.addDomHandler(event -> checkGlobalKeyEvent(event, formsController, currentForm), KeyPressEvent.getType());
        widget.addDomHandler(event -> checkKeyEvents(event, formsController), KeyUpEvent.getType());
    }

    public GFormLayout getFormLayout() {
        return formLayout;
    }

    public boolean hasCanonicalName() {
        return form.canonicalName != null;
    }

    private void initializeRegularFilters() {
        for (final GRegularFilterGroup filterGroup : form.regularFilterGroups) {
            if (filterGroup.filters.size() == 1) {
                createSingleFilterComponent(filterGroup, filterGroup.filters.iterator().next());
            } else if (filterGroup.filters.size() > 1) {
                createMultipleFilterComponent(filterGroup);
            }
        }
    }

    private void createSingleFilterComponent(final GRegularFilterGroup filterGroup, final GRegularFilter filter) {
        final CheckBox filterCheck = new FormCheckBox(filter.getFullCaption());
        filterCheck.setValue(false);
        filterCheck.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> e) {
                setRemoteRegularFilter(filterGroup, e.getValue() != null && e.getValue() ? filter : null);
            }
        });
        GwtClientUtils.addClassName(filterCheck, "filter-group-check");
        addFilterView(filterGroup, filterCheck);

        if (filterGroup.defaultFilterIndex >= 0) {
            filterCheck.setValue(true, false);
        }

        setBindingGroupObject(filterCheck, filterGroup.groupObject);
        for (GInputBindingEvent bindingEvent : filter.bindingEvents) {
            addRegularFilterBinding(bindingEvent, (event) -> filterCheck.setValue(!filterCheck.getValue(), true), filterCheck, filterGroup.groupObject);
        }
    }

    private void createMultipleFilterComponent(final GRegularFilterGroup filterGroup) {
        final ListBox filterBox = new ListBox();
        filterBox.setMultipleSelect(false);
        if (!filterGroup.noNull)
            filterBox.addItem("(" + messages.multipleFilterComponentAll() + ")", "-1");

        ArrayList<GRegularFilter> filters = filterGroup.filters;
        for (int i = 0; i < filters.size(); i++) {
            final GRegularFilter filter = filters.get(i);
            filterBox.addItem(filter.getFullCaption(), "" + i);

            final int filterIndex = i;
            GFormController.setBindingGroupObject(filterBox, filterGroup.groupObject);
            for(GInputBindingEvent bindingEvent : filter.bindingEvents) {
                addRegularFilterBinding(bindingEvent, (event) -> {
                    setRegularFilter(filterGroup, filterBox, filterIndex);
                }, filterBox, filterGroup.groupObject);
            }
        }

        filterBox.addChangeHandler(event -> setRemoteRegularFilter(filterGroup, filterBox.getSelectedIndex() - (filterGroup.noNull ? 0 : 1)));

        GwtClientUtils.addClassName(filterBox, "filter-group-select");
        GwtClientUtils.addClassName(filterBox, "form-select");

        addFilterView(filterGroup, filterBox);
        if (filterGroup.defaultFilterIndex >= 0) {
            filterBox.setSelectedIndex(filterGroup.defaultFilterIndex + (filterGroup.noNull ? 0 : 1));
        }
    }

    private void setRegularFilter(GRegularFilterGroup filterGroup, ListBox filterBox, int filterIndex) {
        filterBox.setSelectedIndex(filterIndex + 1);
        setRemoteRegularFilter(filterGroup, filterIndex);
    }

    public void setRegularFilterIndex(Integer filterGroup, Integer index) {
        for(Map.Entry<GComponent, ComponentViewWidget> entry : formLayout.getBaseComponentViews().entrySet()) {
            GComponent component = entry.getKey();
            if (component instanceof GRegularFilterGroup && (filterGroup == null || filterGroup == component.ID)) {
                Widget widget = entry.getValue().getSingleWidget().widget;
                if (widget instanceof CheckBox) { //single filter
                    ((CheckBox) widget).setValue(index > 0 ? true : null, true);
                } else if (widget instanceof ListBox) { //multiple filter
                    setRegularFilter((GRegularFilterGroup) component, ((ListBox) widget), index - 1);
                }
            }
        }
    }

    private void addFilterView(GRegularFilterGroup filterGroup, Widget filterWidget) {
        formLayout.addBaseComponent(filterGroup, filterWidget, null);

        // need this to hide / show regular filters when group object is not visible
        if (filterGroup.groupObject != null)
            filterViews.computeIfAbsent(filterGroup.groupObject, k -> new ArrayList<>()).add(filterWidget);
    }

    // shared controller (exec/eval/change) machinery, dispatched in THIS form's session/pipeline
    private final GController gController = new GController() {
        @Override
        protected long dispatchExec(String action, ArrayList<Serializable> params, GwtActionDispatcher.ServerResponseCallback callback) {
            return asyncDispatch(new ControllerExecAction(action, params), callback);
        }
        @Override
        protected long dispatchEval(String script, boolean evalAction, ArrayList<Serializable> params, GwtActionDispatcher.ServerResponseCallback callback) {
            return asyncDispatch(new ControllerEvalAction(script, evalAction, params), callback);
        }
        @Override
        protected long dispatchChange(String property, ArrayList<Serializable> keyParams, Serializable value, GwtActionDispatcher.ServerResponseCallback callback) {
            return asyncDispatch(new ControllerChangeAction(property, keyParams, value), callback);
        }
        @Override
        protected boolean isClosed() {
            return dispatcher.isFormClosed();
        }
        @Override
        protected GwtActionDispatcher getControllerDispatcher() {
            return actionDispatcher;
        }
    };

    // the controller: ONE way to address what the form SHOWS, and it mirrors props.data — what a view READS as
    // data.<group>.<property> it CHANGES as controller.<group>.<property>.change(...). One PER PROJECTION, so the
    // two say the same thing: a react view's controller carries exactly its own container's data, and what it
    // cannot see it does not name. The FORM's controller - the classic surfaces' - has none of this: just the
    // form-level verbs. The member IS the address, so
    // there is no second, stringly-typed set of verbs saying the same thing by SID, and a name typed wrong is a member
    // that does not exist rather than a string the platform has to validate.
    //   controller.<group>.<property>.change(...) / .getValues(...)   a property drawn on an object group
    //   controller.<property>.change(...) / .getValues(...)           a form-level (no-group) property
    //   controller.<group>.change(row)                                the group's current object
    //   controller.<group>.expand(row) / .collapse(row) / .toggle(row)   one node of a tree
    //   controller.<group>.expandAll() / .collapseAll()                  ... every node of its group and below it
    //   controller.exec / eval / evalAction / change                  the form-level escape hatch (GController)
    //   controller.properties.change([{property, object, value}])     the one batch — it belongs to no single member
    // A row is named the way a view has it — a data row, its `objects` handle, or the key the projection gave it (the
    // group is known here, so the key can be looked up). Mutations go through the SAME classic interactive path as a
    // normal edit (optimistic reconciliation + async exec); there is no return value, state flows back as the
    // projection.
    // the FORM's controller: what the classic surfaces are handed - a custom object group, a custom cell editor,
    // an INTERNAL CLIENT action - and what `controller.form` is. Just what every controller of the form carries
    // (extendController): the form-level verbs, which take lsf names and code, and the editing a view declares. The
    // rest of this surface - the members and the batch that is a shortcut for them - belongs to a PROJECTION and
    // exists only on one: a member says that `data.<group>.<property>` is there, and an object with no data beside it
    // has nothing to say that about.
    public final JavaScriptObject controller = extendController(GwtClientUtils.newObject());
    // ... so the members live on a PROJECTION: each react container's state carries its own controller beside its own
    // `props.data`, naming exactly what that data carries (GReactFormData.ContainerState.controller). A view that
    // cannot SEE its neighbour does not name it either; the rest of the form is reached the way anything outside this
    // surface is - change / exec / eval / evalAction, which every controller carries and which are gated in their own
    // right. A react container's state is made with its view, and its controller with it, to which the form adds what
    // every controller carries
    public GReactFormData.ContainerState addReactContainer(GContainer container, Consumer<JavaScriptObject> publish) {
        GReactFormData.ContainerState state = reactData.addContainer(container, publish);
        extendController(state.controller);
        return state;
    }
    // what every controller of the form carries: the verbs every controller has (GController.extendController), and
    // the editing a view declares in what it drew (FormView.CONTROLLER_NAMES claims all their names)
    private JavaScriptObject extendController(JavaScriptObject controller) {
        addEditingVerbs(controller);
        return gController.extendController(controller);
    }
    // say that the user has started, and finished, editing in `element`. This is the CUSTOM branch of the form's
    // editing state (customEditElement): while it stands, the bindings that step aside for the form's own open editor
    // - ENTER to the next component, ESCAPE to close a modal - step aside for this element too. Declare on a wrapper
    // and everything inside it is covered.
    // It says the state, it does not open an edit, and it cannot: an edit is a property's, with a value to commit or
    // cancel and only one live at a time. To edit a property from a React view, place the property - <Lsf name/> - and
    // the form brings its own editor, with nothing to declare.
    // The element is the caller's to name, from a ref: the form cannot tell which of a view's elements was meant, and
    // guessing would be wrong exactly where this is used
    private native void addEditingVerbs(JavaScriptObject controller)/*-{
        var thisObj = this;
        controller.startEditing = function (element) {
            thisObj.@GFormController::startCustomEditing(Lcom/google/gwt/dom/client/Element;)(element);
        };
        controller.stopEditing = function (element) {
            thisObj.@GFormController::stopCustomEditing(Lcom/google/gwt/dom/client/Element;)(element);
        };
        controller.isEditing = function (element) {
            return thisObj.@GFormController::isCustomEditing(Lcom/google/gwt/dom/client/Element;)(element);
        };
    }-*/;

    // the (object, value) guess and the dispatch behind the CLASSIC grid view's changeProperty. Held HERE rather than
    // on the controller object because the PLATFORM needs it — the classic group view forwards a property that is none
    // of its own columns to it (#1655) — while an author must not have it: a stringly-typed changeProperty on the
    // controller is exactly the second interface this surface does without. A Java field is reachable from JSNI and
    // invisible to a view. A react view's members guess nothing: their arguments come in a fixed order
    // (GReactFormData.ReactPropertyEntry)
    public final JavaScriptObject classicChangeProperty = initChangeProperty();
    private native JavaScriptObject initChangeProperty() /*-{
        var thisObj = this;
        var UNDEFINED = @lsfusion.gwt.client.base.GwtClientUtils::UNDEFINED;
        //   (target, surface)                - exec the property/action on the current row
        //   (target, surface, value)         - set the value on the current row
        //   (target, surface, row)           - exec it on that row
        //   (target, surface, row, value)    - set the value on that row
        return function (target, surface, object, value) {
            if (object !== undefined) {
                // one argument: decided by it ALONE, so the same argument always means the same
                if (value === undefined)
                    if (thisObj.@GFormController::isClassicChangeObject(*)(surface, target, object))
                        value = UNDEFINED; // a row -> exec on it
                    // anything else (a value, or a bare key: they are indistinguishable) -> set on the current row
                    else { value = object; object = null; }
            } else { value = UNDEFINED; object = null; } // no argument -> exec on the current row
            object = object === undefined ? null : object;
            return thisObj.@GFormController::classicChangeProperty(*)(surface, target, object, value);
        };
    }-*/;

    // ===== custom-controller mutation helpers (CUSTOM REACT + any form-level custom component): a member holds the
    // state of what it changes and reads what a call names from it (GReactFormData), the batch is handed the members
    // themselves, and only the classic grid's changeProperty still resolves "integrationSID" /
    // "groupSID.integrationSID" by itself; rows/handles resolve via GGroupObjectValue.resolveObject (the row-carried
    // `objects` handle + raw-GGV accept). Dispatch through the SAME classic path as a normal edit
    // (executePropertyEventAction / changeGroupObject + setLoadingValueAt) ===== a group member's change(row), once it
    // has read the row (GReactFormData.RowsGroupNode.change): the group's current object
    @Override
    public void changeCurrentObject(GGroupObject group, GGroupObjectValue key) {
        changeGroupObject(group, key, null, null);
    }
    // ... and a tree group member's expand / collapse / toggle, once it has read the node and decided
    // (TreeRowsGroupNode): the platform's own request, which takes the state asked for - and the group's controller
    // told of it, to show it at once until the keys of the answer come with the request's index, as changeGroupObject
    // tells it of a current object
    @Override
    public void expandNode(GGroupObject group, GGroupObjectValue key) {
        long requestIndex = expandGroupObject(group, key, true);
        getGroupController(group).changeExpanded(key, true, requestIndex);
        refreshReactOptimistic();
    }
    @Override
    public void collapseNode(GGroupObject group, GGroupObjectValue key) {
        long requestIndex = expandGroupObject(group, key, false);
        getGroupController(group).changeExpanded(key, false, requestIndex);
        refreshReactOptimistic();
    }
    // ... and its expandAll / collapseAll: the server starts at the group it is given - its nodes under the open nodes
    // of the group above, all of them for the top group - and goes down
    @Override
    public void expandAll(GGroupObject group) {
        long requestIndex = expandGroupObjectRecursive(group, false, true); // false: the whole group, not the current object
        getGroupController(group).changeExpandedAll(true, requestIndex);
        refreshReactOptimistic();
    }
    @Override
    public void collapseAll(GGroupObject group) {
        long requestIndex = expandGroupObjectRecursive(group, false, false);
        getGroupController(group).changeExpandedAll(false, requestIndex);
        refreshReactOptimistic();
    }

    // a property of a group, by a CLASSIC name: the group's one property of that name - a name the group draws twice
    // says nothing and is refused. Null when the group draws no such property: each caller says so its own way
    private GPropertyDraw resolveClassicGroupProperty(String errorPrefix, GGroupObject group, String integrationSID) {
        GPropertyDraw property = form.getPropertyDraw(group, integrationSID);
        if (property != null && form.countPropertyDraws(group, integrationSID) != 1)
            throw new RuntimeException(errorPrefix + "'" + integrationSID + "' is drawn more than once on "
                    + (group != null ? "group '" + group.getSID() + "'" : "the form level")
                    + ", so this name does not say which of them is meant; give them explicit EXTIDs");
        return property;
    }

    // a property grouped in COLUMNS is one cell per (row, column), and everything here names a ROW. The projection
    // does not carry such a property at all, so a call naming it would land on whatever column happens to be current -
    // a different, real cell, and it would look like it worked. Refused with the reason, as `lsf` on one already is.
    private void checkAddressableProperty(String errorPrefix, GPropertyDraw property) {
        if (property.hasColumnGroupObjects())
            throw new RuntimeException(errorPrefix + "'" + property.integrationSID + "' is grouped in columns - it is one cell"
                    + " per row AND column, and this names a row, so it is not addressed here (nor projected)");
    }

    // the CLASSIC stringly-typed surface's reading of a name - the grid view's changeProperty, which names the whole
    // form (#1655). A name is what a view has for a property everywhere (GReactFormData.getName): "groupSID.
    // integrationSID" for a property of an object group, a bare integration SID for one of the empty group - so the
    // name alone says the group, and neither the row passed nor the rest of the form is asked. Every author mistake
    // throws: an unknown group, a name drawn twice on its group, a missing property - a bare name an object group
    // draws is told to take its group. A react view's controller reads no name this way: its members hold their
    // states, and its batch is handed the members themselves (GReactFormData.ContainerState.changeProperties).
    private GPropertyDraw resolveClassicProperty(String surface, String name) {
        String errorPrefix = controllerPrefix(surface);
        // a qualified name splits at its LAST dot. A property's integration SID never carries one, so everything
        // before the last dot is the group and everything after is the property - and splitting anywhere else would
        // ask for a group that does not exist: a group of several objects is named by its SID, all of them joined with
        // dots (`OBJECTS d = X, t = Y` is `d.t`)
        int dot = name.lastIndexOf('.');
        GGroupObject group = dot > 0 ? form.getGroupObject(name.substring(0, dot)) : null;
        if (dot > 0 && group == null) // it is qualified and names no group of this form: say which half failed
            throw new RuntimeException(errorPrefix + "unknown object group '" + name.substring(0, dot) + "'");
        String integrationSID = dot > 0 ? name.substring(dot + 1) : name;

        GPropertyDraw property = resolveClassicGroupProperty(errorPrefix, group, integrationSID);
        if (property == null) {
            if (group != null)
                throw new RuntimeException(errorPrefix + "property '" + integrationSID + "' is not drawn on group '" + group.getSID() + "'");
            GGroupObject drawnOn = form.getSingleDrawnGroup(name); // a bare name a group draws: say how to name it
            String groupSID = drawnOn != null ? drawnOn.getSID() : "<group>";
            throw new RuntimeException(errorPrefix + "'" + name + "' names no property of the form level; a property of an"
                    + " object group is named with its group, '" + groupSID + "." + name + "'");
        }
        checkAddressableProperty(errorPrefix, property);
        return property;
    }

    // the row a classic call names, once it is known WHOSE row it has to be - null: nobody's (a form-level property, or
    // a name drawn on several groups). A data row or a raw handle carries its objects, and then has to be a row of that
    // group - in a TREE a row of a group BELOW passes, its key being the path down to it, as a member's row does
    // (GReactFormData.ReactColumnPropertyEntry.getMemberKey). A bare KEY names a row only in the react view that draws
    // the rows, where the index built with them is, and the classic changeProperty draws none
    private GGroupObjectValue resolveGroupObject(String surface, GGroupObject group, JavaScriptObject objectOrKey, String noGroup) {
        if (GwtClientUtils.isUndefinedOrNull(objectOrKey)) // raw JS key: a numeric 0 key reads as null under Java == null (GWT falsy-primitive collapse)
            return null;
        GGroupObjectValue objectKey = GGroupObjectValue.resolveObject(objectOrKey);
        if (objectKey == null)
            throw new RuntimeException(controllerPrefix(surface) + (group != null
                    ? "expects a row of '" + group.getSID() + "', its objects handle; a key names a row only in the react view that draws the rows"
                    : "the object argument is not a data row or an objects handle; pass one of those" + noGroup));
        if (group != null && !objectKey.isEmpty() && group.filterRowKeys(objectKey) == null)
            throw new RuntimeException(controllerPrefix(surface) + "that row is not a row of '" + group.getSID() + "'");
        return objectKey;
    }
    // ... and the row a classic name is given: one of the group its prefix names - a bare name names a property of the
    // form level, which has no rows, so a row given with it is a mistake
    private GGroupObjectValue resolveClassicObject(String surface, String property, JavaScriptObject objectOrKey) {
        if (GwtClientUtils.isUndefinedOrNull(objectOrKey)) // no row given: nothing for the name to settle
            return null;
        int dot = property.lastIndexOf('.'); // the same one split resolveClassicProperty reads the name with
        if (dot <= 0)
            throw noRowsOfFormLevel(surface, property);
        return resolveGroupObject(surface, form.getGroupObject(property.substring(0, dot)), objectOrKey, "");
    }
    private RuntimeException noRowsOfFormLevel(String surface, String property) {
        return new RuntimeException(controllerPrefix(surface) + "'" + property + "' names a property of the form level,"
                + " which has no rows: a property of an object group is named with its group, '<group>." + property
                + "'");
    }

    private static GGroupObjectValue getControllerColumnKey(GGroupObjectValue objectKey) {
        return objectKey != null ? objectKey : GGroupObjectValue.EMPTY; // null object => the current object
    }

    // the 2-arg changeProperty(property, X) guess — WHICH form is the call, (property, value) or (property, object)?
    // Decided by X ALONE, so the same argument always means the same thing: X is the row when it is an unambiguous row
    // carrier (resolveObject — a data row or a raw objects handle, never a bare key/clone/primitive: ids collide with
    // values generically), and the value otherwise. The property's change type must NOT enter here: it is null for
    // everything whose change is not a plain async input (an FK picked in a dialog, a custom CHANGE action), and keying
    // on it read the documented changeProperty(property, value) of such a property as an object and rejected the value.
    // An action is the one draw with no value slot, so the row is the only reading left for it — a non-row X goes to the
    // object slot as is and the EXPLICIT dispatch path rejects it loudly, naming what was expected; null alone is not
    // rejected there, it names the current object, as it does wherever a row is passed.
    // THE single guess core, shared by both surfaces — they differ only in how the property is found (the classic
    // changeProperty: by its name; the grid custom view: by its view column), the policy is one
    public boolean isChangeObject(GPropertyDraw draw, JavaScriptObject object) {
        if (draw == null || draw.groupObject == null) // no object slot at all -> the value form is the only one
            return false;
        if (GGroupObjectValue.resolveObject(object) != null)
            return true;
        return draw.isAction();
    }
    public boolean isClassicChangeObject(String surface, String name, JavaScriptObject object) {
        GPropertyDraw property = resolveClassicProperty(surface, name);
        // a row for a property of the form level, which has none: refused here, before the guess reads it as the value
        if (property.groupObject == null && GGroupObjectValue.resolveObject(object) != null)
            throw noRowsOfFormLevel(surface, name);
        return isChangeObject(property, object);
    }
    // the CLASSIC grid view's stringly-typed changeProperty, which forwards a property that is none of its own
    // columns (#1655). It has its OWN entry rather than a flag on the one above, because it is another surface with
    // another rule: it names the whole FORM, member or no member, as it always has - that API is older than this one
    // and is not a shortcut for its members. So it is the one call left that finds what it changes by a name.
    public void classicChangeProperty(String surface, String name, JavaScriptObject objectOrKey, JavaScriptObject value) {
        GPropertyDraw property = resolveClassicProperty(surface, name); // throws on an unknown or ambiguous name
        GGroupObjectValue objectKey = resolveClassicObject(surface, name, objectOrKey);
        changeProperties(new GPropertyDraw[]{property}, new GGroupObjectValue[]{getControllerColumnKey(objectKey)},
                new PValue[]{GSimpleStateTableView.convertFromJSUndefValue(property, value)});
    }
    // the typed core: resolved properties/keys/values -> the same classic batch edit dispatch as a normal user edit - a
    // member's, once it has read what it names (GReactFormData), and the classic changeProperty's
    @Override
    public void changeProperties(GPropertyDraw[] pa, GGroupObjectValue[] ka, PValue[] va) {
        executePropertyEventAction(pa, ka, va, requestIndex -> {
            for (int i = 0; i < pa.length; i++)
                // WYSIWYG guard (like GSimpleStateTableView.changeProperties): only overlay the optimistic value when the
                // input writes the displayed property value; a non-WYSIWYG change (custom action / unbound input) must wait for the server
                if (va[i] != PValue.UNDEFINED && pa[i].hasExternalChangeActionForRendering(RendererType.SIMPLE))
                    // optimistic overlay (+ optimistic react through the property's controller,
                    // ReactPropertyEntry.setLoadingValueAt), reconciled by requestIndex
                    setLoadingValueAt(pa[i], ka[i], va[i], requestIndex);
            refreshReactOptimistic(); // once, for the whole batch
        });
    }
    // maps the public getPropertyValues `mode` to the server async actionSID (the strings coincide). null/'objects' => OBJECTS
    // (the only mode that returns object handles); 'values' => DISTINCT property values; 'change' => the property's edit-time
    // value autocomplete (respects custom INPUT/notNull/custom-change, requires editability). STRICTVALUES is intentionally NOT
    // exposed: it's only `values` + exact-match UX post-processing the platform derives from the filter operator, not an author
    // choice (add an `exactMatch` flag later if ever needed). Returns null for an unknown mode (caller rejects loudly).
    public static String getAsyncActionSID(String mode) {
        // the classic grid view, which has no member path to quote
        return getAsyncActionSID("controller.<property>.getValues(): ", mode);
    }
    // ... and a member's getValues, named by it
    public static String getAsyncActionSID(String errorPrefix, String mode) {
        if (mode == null || mode.equals(ServerResponse.OBJECTS))
            return ServerResponse.OBJECTS;
        if (mode.equals(ServerResponse.VALUES) || mode.equals(ServerResponse.CHANGE))
            return mode;
        GwtClientUtils.consoleError(errorPrefix + "unknown mode '" + mode + "'; expected 'objects' | 'values' | 'change'");
        return null;
    }
    // async value lookup (autocomplete / suggestion list) for a form-level / CUSTOM REACT property — the form-level twin
    // of the grid custom view's getAsyncValues (GSimpleStateTableView). The property is the member's own, and the
    // optional row is read as the member's change() reads it (GReactFormData.ReactPropertyEntry.getValues); its key is
    // the columnKey, which getFullCurrentKey overlays over the current selection -> suggestions scoped to that row
    // (EMPTY => the property group's current object). The mode picks the server lookup (OBJECTS suggestions vs distinct
    // VALUES vs the edit-time CHANGE autocomplete; CHANGE here targets THIS property's own change action — no
    // property:value concat, that's the JSON-property cell-renderer's special case). Issues through the shared
    // getAsyncValues.
    @Override
    public void getPropertyValues(GPropertyDraw property, GGroupObjectValue key, String value, String actionSID, JavaScriptObject successCallback, JavaScriptObject failureCallback, int increaseValuesNeededCount) {
        getAsyncValues(value, property, key, actionSID, getJSCallback(successCallback, failureCallback), increaseValuesNeededCount);
    }

    // terminal GControllerResult/ExceptionAction (delivered through GFormActionDispatcher) -> resolve/reject the promise
    public void controllerCallbackResult(long requestIndex, JavaScriptObject result) {
        gController.controllerCallbackResult(requestIndex, result);
    }
    public void controllerCallbackException(long requestIndex, String message, boolean cancelled) {
        gController.controllerCallbackException(requestIndex, message, cancelled);
    }

    @Override
    public JavaScriptObject getFormController() { // EditManager: the form's JS controller, exposed as the CUSTOM editor's `form` field
        return controller;
    }

    public void setFiltersVisible(GGroupObject groupObject, boolean visible) {
        List<Widget> groupFilters = filterViews.get(groupObject);
        if (groupFilters != null)
            for (Widget filterView : groupFilters)
                GwtClientUtils.setGridVisible(filterView, visible);
    }

    private void setRemoteRegularFilter(GRegularFilterGroup filterGroup, int index) {
        setRemoteRegularFilter(filterGroup, index == -1 ? null : filterGroup.filters.get(index));
    }

    private void initializeControllers() {
        for (GTreeGroup treeGroup : form.treeGroups) {
            if (reactData.contentScope(treeGroup) == null) // a tree React draws has no GWT tree controller
                initializeTreeController(treeGroup);
        }

        for (GGroupObject group : form.groupObjects) {
            if (reactData.rowsScope(group) == null && group.parent == null) // React draws the rows from its node
                initializeGroupController(group);
        }

        // kept even for React: getLsfPropertyController/update rely on it; it stays empty when only react-owned
        // property readers are skipped, so no panel views are built
        panelController = new GPanelController(this);

        // ... and now that every controller exists, where each thing's changes go is settled
        initializeOwnerControllers();
    }

    // the form's controller of each group, property and component, taken once, from the questions the platform's
    // controllers were made by: the platform's controller of it, or what React has of it, made here by the projection
    // from the platform's (GReactFormData.createGroupController / createPropertyController / createComponentController)
    // - a group's rows node where React draws the rows, a property's entry where React draws or labels it, a
    // component's descriptor. The groups first, whose rows nodes a list property's column is on, then the grid panel
    // controllers, which draw over those rows, then the properties, in the form's order, which is the order of the
    // names a node carries, then the components; then the projection
    // sets out what it has. A property is ONE owner: all that is sent for it goes to its controller, what labels it
    // included; as a component only its class is sent, which is the platform's to apply (createComponentController).
    // These controllers are design data, like the platform's: made once, they live as long as the form.
    private void initializeOwnerControllers() {
        for (GGroupObject group : form.groupObjects)
            groupControllers.put(group, reactData.createGroupController(group, getTableController(group)));
        // a group can have both: React draws its rows from the projection, and the platform draws its LSF properties
        // into each of those rows - the only rows such a property is drawn in (FormView.checkLsfListView), so its
        // controller is made over them, before the properties, whose controller it is (getLsfPropertyController)
        for (GGroupObject group : form.groupObjects)
            for (GPropertyDraw draw : form.propertyDraws)
                if (draw.groupObject == group && draw.isLsfViewPerRow()) {
                    gridPanelControllers.put(group, new GGridPanelController(this, group, reactData.getRows(group)));
                    break;
                }
        for (GPropertyDraw property : form.propertyDraws) {
            GPropertyController controller = reactData.createPropertyController(property, getLsfPropertyController(property));
            if (controller != null) // none: React keeps nothing of it (getPropertyController)
                propertyControllers.put(property, controller);
        }
        initializeComponentControllers(form.mainContainer);
        reactData.initialize();
    }
    // ... every component, down the design - a grid's record is inside its grid, and so is what it holds -, a property
    // too, as a component (createComponentController)
    private void initializeComponentControllers(GComponent component) {
        GComponentController controller = reactData.createComponentController(component, layoutController);
        if (controller != null) // none: React keeps nothing of it (getComponentController)
            componentControllers.put(component, controller);
        for (GComponent child : component.getChildren())
            initializeComponentControllers(child);
    }

    public Pair<Widget, Boolean> getCaptionWidget() {
        boolean captionInitialized = formContainer.captionInitialized;
        formContainer.captionInitialized = true; // need this for the recreate form
        return new Pair<>(formContainer.getCaptionWidget(), captionInitialized);
    }
    private void initializeParams() {
        hasColumnGroupObjects = false;
        for (GPropertyDraw property : getPropertyDraws()) {
            if (property.hasColumnGroupObjects()) {
                hasColumnGroupObjects = true;
            }

            GGroupObject groupObject = property.groupObject;
            if (groupObject != null && property.isList && !property.hideOrRemove()) {
                groupObject.highlightDuplicateValue |= property.highlightDuplicateValue();

                if (groupObject.columnCount < 10) {
                    GFont font = groupObject.grid != null ? groupObject.grid.font : null;
                    // in theory property renderers padding should be included, but it's hard to do that (there will be problems with the memoization)
                    // plus usually there are no paddings for the property renderers in the table (td paddings are used, and they are included see the usages)
                    groupObject.setColumnSumWidth(groupObject.getColumnSumWidth().add(property.getValueWidth(font, true, true)));
                    groupObject.columnCount++;
                    groupObject.setRowMaxHeight(groupObject.getRowMaxHeight().max(property.getValueHeight(font, true, true)));
                }
            }
        }
    }

    private void initializeGroupController(GGroupObject group) {
        GGridController controller = new GGridController(this, group, form.userPreferences != null ? extractUserPreferences(form.userPreferences, group) : null);
        gridControllers.put(group, controller);
    }

    private void initializeTreeController(GTreeGroup treeGroup) {
        GTreeGroupController treeController = new GTreeGroupController(treeGroup, this, form);
        treeControllers.put(treeGroup, treeController);
    }

    private GGridUserPreferences[] extractUserPreferences(GFormUserPreferences formPreferences, GGroupObject groupObject) {
        if (formPreferences != null) {
            GGridUserPreferences[] gridPreferences = new GGridUserPreferences[2];
            gridPreferences[0] = findGridUserPreferences(formPreferences.getGroupObjectGeneralPreferencesList(), groupObject);
            gridPreferences[1] = findGridUserPreferences(formPreferences.getGroupObjectUserPreferencesList(), groupObject);
            return gridPreferences;
        }
        return null;
    }

    private GGridUserPreferences findGridUserPreferences(List<GGroupObjectUserPreferences> groupObjectUserPreferences, GGroupObject groupObject) {
        for (GGroupObjectUserPreferences groupPreferences : groupObjectUserPreferences) {
            if (groupObject.getSID().equals(groupPreferences.getGroupObjectSID())) {
                Map<GPropertyDraw, GColumnUserPreferences> columnPreferences = new HashMap<>();
                for (Map.Entry<String, GColumnUserPreferences> entry : groupPreferences.getColumnUserPreferences().entrySet()) {
                    GPropertyDraw property = form.getProperty(entry.getKey());
                    if (property != null) {
                        columnPreferences.put(property, entry.getValue());
                    }
                }
                return new GGridUserPreferences(groupObject, columnPreferences, groupPreferences.getFont(), groupPreferences.getPageSize(), groupPreferences.getHeaderHeight(), groupPreferences.hasUserPreferences());
            }
        }
        return null;
    }

    public ArrayList<ArrayList<GPropertyDrawOrPivotColumn>> getPivotColumns(GGroupObject groupObject) {
        return form.getPivotColumns(groupObject);
    }

    public ArrayList<ArrayList<GPropertyDrawOrPivotColumn>> getPivotRows(GGroupObject groupObject) {
        return form.getPivotRows(groupObject);
    }

    public ArrayList<GPropertyDraw> getPivotMeasures(GGroupObject groupObject) {
        return form.getPivotMeasures(groupObject);
    }

    public void executeNotificationAction(final String notification) throws IOException {
        syncResponseDispatch(new ExecuteNotification(notification));
    }

    private void initializeFormSchedulers() {
        for(GFormEvent formEvent : form.asyncExecMap.keySet()) {
            if(formEvent instanceof GFormScheduler)
                scheduleFormScheduler((GFormScheduler) formEvent);
        }
    }

    public Widget getWidget() {
        return formLayout;
    }

    private void scheduleFormScheduler(GFormScheduler formScheduler) {

        Scheduler.get().scheduleFixedPeriod(new Scheduler.RepeatingCommand() {
            @Override
            public boolean execute() {
                if (isVisible()) {
                    if (isShowing(getWidget()) && !MainFrame.isModalPopup()) {
                        executeFormEventAction(formScheduler, new ServerResponseCallback() {
                            public void onSuccess(ServerResponseResult response, Runnable onDispatchFinished) {
                                super.onSuccess(response, onDispatchFinished);
                                if (isVisible() && !formScheduler.fixed) {
                                    scheduleFormScheduler(formScheduler);
                                }
                            }
                        });

//                        if(formScheduler.fixed) {
//                            scheduleFormScheduler(formScheduler);
//                        }
                        return formScheduler.fixed;
                    } else {
                        return true;
                    }
                }
                return false;
            }
        }, formScheduler.period * 1000);
    }

    public void closePressed(EndReason reason) {
        closePressed(reason, false);
    }

    // unshown: the client is closing a form it is not going to show - it arrived for an open answered with another
    // form, or its container was closed while it was on its way. Nobody asked for this close, so nobody is asked
    // about it either: the close carries its own confirmation, and none of what an ordinary one does around it -
    // no close reason to hand the response, no local question, and no optimistic hiding of a form that is in no
    // window to hide
    public void closePressed(EndReason reason, boolean unshown) {
        GFormEventClose eventClose = new GFormEventClose(!unshown && reason instanceof CommitReason);

        if (unshown) {
            ExecuteFormEventAction closeAction = new ExecuteFormEventAction(eventClose);
            closeAction.pushAsyncResult = new GPushAsyncClose();
            syncResponseDispatch(closeAction);
            return;
        }

        executeFormEventAction(eventClose, new ServerResponseCallback() {
            @Override
            protected Runnable getOnRequestFinished() {
                return () -> {
                    actionDispatcher.editFormCloseReason = null;
                };
            }

            @Override
            public void onSuccess(ServerResponseResult response, Runnable onDispatchFinished) {
                actionDispatcher.editFormCloseReason = reason;
                super.onSuccess(response, onDispatchFinished);
            }
        });
    }

    private void executeFormEventAction(GFormEvent formEvent, ServerResponseCallback serverResponseCallback) {
        ExecuteFormEventAction executeFormEventAction = new ExecuteFormEventAction(formEvent);

        GAsyncExec asyncExec = getAsyncExec(form.asyncExecMap.get(formEvent));
        if (asyncExec != null) {
            asyncExec.exec(formsController, this, formContainer, editEvent, new GAsyncExecutor(actionDispatcher, pushAsyncResult -> {
                executeFormEventAction.pushAsyncResult = pushAsyncResult;
                return asyncDispatch(executeFormEventAction, serverResponseCallback);
            }));
        } else {
            syncDispatch(executeFormEventAction, serverResponseCallback);
        }
    }

    private GAsyncExec getAsyncExec(GAsyncEventExec asyncEventExec) {
        if(asyncEventExec instanceof GAsyncExec)
            return (GAsyncExec) asyncEventExec;
        return null;
    }

    public GPropertyDraw getProperty(String propertyFormName) {
        return form.getProperty(propertyFormName);
    }

    public GPropertyDraw getProperty(int id) {
        return form.getProperty(id);
    }

    public GGroupObject getGroupObject(int groupID) {
        return form.getGroupObject(groupID);
    }

    public void getRemoteChanges() {
        getRemoteChanges(false);
    }

    public void getRemoteChanges(boolean forceLocalEvents) {
        asyncResponseDispatch(new GetRemoteChanges(forceLocalEvents));
    }

    private boolean formActive = true;

    public void gainedFocus() {
        asyncResponseDispatch(new GainedFocus());
        formActive = true;
    }

    public void lostFocus() {
        formActive = false;
    }

    public void applyRemoteChanges(GFormChangesDTO changesDTO) {
        applyRemoteChanges(GFormChanges.remap(form, changesDTO), changesDTO.requestIndex);
    }

    public void applyRemoteChanges(GFormChanges fc, int requestIndex) {
        if (hasColumnGroupObjects) // optimization
            fc.gridObjects.foreachEntry((key, value) -> currentGridObjects.put(key, value));

        modifyFormChangesWithModifyObjectAsyncs(requestIndex, fc);

        modifyFormChangesWithChangeCurrentObjectAsyncs(requestIndex, fc);

        modifyFormChangesWithChangeOrdersAsyncs(requestIndex, fc);

        modifyFormChangesWithChangeFiltersAsyncs(requestIndex, fc);

        modifyFormChangesWithChangePropertyAsyncs(requestIndex, fc);

        modifyFormChangesWithLoadingPropertyAsyncs(requestIndex, fc);

        applyLoadingFilterAsyncs(requestIndex, fc);

        applyKeyChanges(fc, requestIndex);

        applyPropertyChanges(fc);

        applyOrderChanges(fc);

        update(fc, requestIndex);

        // after the update, which gives a filter panel its fixed conditions the first time
        applyFilterChanges(fc);

        expandCollapseContainers(fc);

        activateElements(fc);

        applyNeedConfirm(fc);

        formLayout.update(requestIndex);

        reactData.flush();
        updateRowRenderers(); // after the projection: the per-row renderers follow the rows it now holds
    }

    public void applyKeyChanges(GFormChanges fc, int requestIndex) {
        fc.gridObjects.foreachEntry((key, value) -> {
            getGroupController(key).updateKeys(key, value, fc, requestIndex);

        });

        fc.objects.foreachEntry((key, value) -> getGroupController(key).updateCurrentKey(value));
    }

    private void applyOrderChanges(GFormChanges fc) {
        fc.userOrders.foreachEntry((group, orders) -> showOrders(group, orders));
    }

    // the orders the form has for a group, given to the group's controller only when they are other orders
    private void showOrders(GGroupObject group, LinkedHashMap<Column, Boolean> orders) {
        if (!equalOrders(orders, currentOrders.get(group))) {
            currentOrders.put(group, orders);
            getGroupController(group).updateOrders(group, orders);
        }
    }

    // the same orders in another priority are other orders
    private static boolean equalOrders(LinkedHashMap<Column, Boolean> orders, LinkedHashMap<Column, Boolean> current) {
        return current != null && new ArrayList<>(orders.entrySet()).equals(new ArrayList<>(current.entrySet()));
    }

    // filters equal to the ones the client has are shown already: a condition the user is editing stays as it is
    private void applyFilterChanges(GFormChanges fc) {
        fc.userFilters.foreachEntry((group, filters) -> {
            if (!filters.equals(currentFilters.get(group))) {
                currentFilters.put(group, filters);
                getGroupController(group).updateFilters(group, filters);
            }
        });
    }

    private void applyPropertyChanges(GFormChanges fc) {
        // a drop is its property's values gone, so it goes where they go (updatePropertyChanges): to the property's
        // controller. It need not wait for the values: the server sends a property EITHER with its values OR in the
        // drop set (FormInstance: shown, else dropped if it was shown), and the optimistic reconciliation above never
        // puts a value back for a dropped one
        fc.dropProperties.forEach(property -> getPropertyController(property).dropProperty(property));

        // Native value readers create the property's view/state before extra readers address it.
        updatePropertyChanges(fc, false);
        updatePropertyChanges(fc, true);
    }

    // a reader's values go to the controller of its owner - the property, the group, the component it is of - told what
    // kind of reader it is (GPropertyReader.update)
    private void updatePropertyChanges(GFormChanges fc, boolean extras) {
        fc.properties.foreachEntry((reader, values) -> {
            if ((reader instanceof GExtraPropertyReader) != extras)
                return;
            boolean partial = fc.updateProperties.contains(reader);
            reader.update(this, values, partial);
        });
    }
    public void update(GFormChanges fc, int requestIndex) {
        for (GGridController controller : gridControllers.values())
            controller.update(requestIndex, fc);

        for (GTreeGroupController treeController : treeControllers.values())
            treeController.update();

        for (GGridPanelController gridPanelController : gridPanelControllers.values())
            gridPanelController.update();

        panelController.update();
    }

    private void activateElements(GFormChanges fc) {
        Scheduler.get().scheduleDeferred(() -> {
            for(GComponent component : fc.activateTabs)
                activateTab(component);

            for(GPropertyDraw propertyDraw : fc.activateProps)
                focusProperty(propertyDraw);
        });
    }

    private void applyNeedConfirm(GFormChanges fc) {
        needConfirm = fc.needConfirm;
    }

    // whether closing this form would ask the user first, as the server last said: what the async close goes by,
    // and what a FORMS window's close-delay goes by when it decides not to ask at all
    public boolean needConfirm() {
        return needConfirm;
    }

    private void expandCollapseContainers(GFormChanges formChanges) {
        for (GContainer container : formChanges.collapseContainers) {
            setContainerExtCollapsed(container, true);
        }

        for (GContainer container : formChanges.expandContainers) {
            setContainerExtCollapsed(container, false);
        }
    }

    private void setContainerExtCollapsed(GContainer container, boolean collapsed) {
        GAbstractContainerView parentContainerView = container.container != null ? formLayout.getContainerView(container.container) : null;
        if (parentContainerView != null) { // a container React draws has no view: nothing to collapse
            Widget childWidget = parentContainerView.getChildWidget(container);
            if (childWidget instanceof CollapsiblePanel) {
                ((CollapsiblePanel) childWidget).setCollapsed(collapsed);
            }
        }
    }

    private void modifyFormChangesWithModifyObjectAsyncs(final int currentDispatchingRequestIndex, GFormChanges fc) {
        for (Iterator<ModifyObject> iterator = pendingModifyObjectRequests.iterator(); iterator.hasNext(); ) {
            ModifyObject modifyObject = iterator.next();
            if (modifyObject.requestIndex <= currentDispatchingRequestIndex) {
                iterator.remove();

                GGroupObject groupObject = modifyObject.object.groupObject;
                // делаем обратный modify, чтобы удалить/добавить ряды, асинхронно добавленные/удалённые на клиенте, если с сервера не пришло подтверждение
                // возможны скачки и путаница в строках на удалении, если до прихода ответа position утратил свою актуальность
                // по этой же причине не заморачиваемся запоминанием соседнего объекта
                if(!fc.gridObjects.containsKey(groupObject)) {
                    getGroupController(groupObject).modifyGroupObject(modifyObject.value, !modifyObject.add, modifyObject.position);
                }
            }
        }

        for (Iterator<ModifyObject> iterator = pendingModifyObjectRequests.iterator(); iterator.hasNext(); ) {
            ModifyObject modifyObject = iterator.next();
            ArrayList<GGroupObjectValue> gridObjects = fc.gridObjects.get(modifyObject.object.groupObject);
            if (gridObjects != null) {
                if (modifyObject.add) {
                    gridObjects.add(modifyObject.value);
                } else {
                    if(!gridObjects.remove(modifyObject.value)) { //could be removed in previous formChange (for example, two async groupChanges)
                        iterator.remove();
                    }
                }
            }
        }
    }

    private void modifyFormChangesWithChangeCurrentObjectAsyncs(final long currentDispatchingRequestIndex, final GFormChanges fc) {
        pendingChangeCurrentObjectsRequests.foreachEntry((group, requestIndex) -> {
            if (requestIndex <= currentDispatchingRequestIndex)
                pendingChangeCurrentObjectsRequests.remove(group);
            else
                fc.objects.remove(group);
        });
    }

    private void modifyFormChangesWithChangeOrdersAsyncs(final long currentDispatchingRequestIndex, final GFormChanges fc) {
        pendingChangeOrdersRequests.foreachEntry((group, requestIndex) -> {
            if (requestIndex <= currentDispatchingRequestIndex)
                pendingChangeOrdersRequests.remove(group);
            else
                fc.userOrders.remove(group);
        });
    }

    private void modifyFormChangesWithChangeFiltersAsyncs(final long currentDispatchingRequestIndex, final GFormChanges fc) {
        pendingChangeFiltersRequests.foreachEntry((group, requestIndex) -> {
            if (requestIndex <= currentDispatchingRequestIndex)
                pendingChangeFiltersRequests.remove(group);
            else
                fc.userFilters.remove(group);
        });
    }

    private void modifyFormChangesWithChangePropertyAsyncs(final int currentDispatchingRequestIndex, final GFormChanges fc) {
        pendingChangePropertyRequests.foreachEntry((property, values) -> values.foreachEntry((keys, change) -> {
            long requestIndex = change.requestIndex;
            if (requestIndex <= currentDispatchingRequestIndex) {
                removeFromDoubleMap(pendingChangePropertyRequests, property, keys);

                // whether the form shows it now is its controller's answer: the platform's, or what React has of it
                boolean propertyShown = !(property instanceof GPropertyDraw)
                        || (!fc.dropProperties.contains((GPropertyDraw) property) && getPropertyController((GPropertyDraw) property).isPropertyShown((GPropertyDraw) property));
                if(propertyShown) {
                    NativeHashMap<GGroupObjectValue, PValue> propertyValues = fc.properties.get(property);
                    if (propertyValues == null) {
                        // включаем изменение на старое значение, если ответ с сервера пришел, а новое значение нет
                        propertyValues = new NativeHashMap<>();
                        fc.properties.put(property, propertyValues);
                        fc.updateProperties.add(property);
                    }

                    if (fc.updateProperties.contains(property) && !propertyValues.containsKey(keys)) {
                        propertyValues.put(keys, change.oldValue);
                    }
                }
            }
        }));

        pendingChangePropertyRequests.foreachEntry((property, values) -> {
            final NativeHashMap<GGroupObjectValue, PValue> propertyValues = fc.properties.get(property);
            if (propertyValues != null) {
                values.foreachEntry((key, change) -> {
                    propertyValues.put(key, change.newValue);
                });
            }
        });
    }

    private void modifyFormChangesWithLoadingPropertyAsyncs(final int currentDispatchingRequestIndex, final GFormChanges fc) {
        pendingLoadingPropertyRequests.foreachEntry((property, values) -> values.foreachEntry((keys, requestIndex) -> {
            if (requestIndex <= currentDispatchingRequestIndex) {

                removeFromDoubleMap(pendingLoadingPropertyRequests, property, keys);

                // as its controller has it
                if(!fc.dropProperties.contains(property) && getPropertyController(property).isPropertyShown(property)) {
                    NativeHashMap<GGroupObjectValue, PValue> propertyLoadings = fc.properties.get(property.loadingReader);
                    if (propertyLoadings == null) {
                        propertyLoadings = new NativeHashMap<>();
                        fc.properties.put(property.loadingReader, propertyLoadings);
                    }
                    propertyLoadings.put(keys, null);
                }
            }
        }));
    }

    private void applyLoadingFilterAsyncs(final int currentDispatchingRequestIndex, final GFormChanges fc) {
        pendingLoadingFilterRequests.foreachEntry((filter, requestIndex) -> {
            if (requestIndex <= currentDispatchingRequestIndex) {

                pendingLoadingFilterRequests.remove(filter);

                if(!filter.isRemoved)
                    filter.updateLoading(false);
            }
        });
    }

    public GAbstractTableController getTableController(GGroupObject group) {
        GGridController groupObjectController = gridControllers.get(group);
        if (groupObjectController != null) {
            return groupObjectController;
        }

        return treeControllers.get(group.parent);
    }

    public GLsfPropertyController getLsfPropertyController(GPropertyDraw property) {
        // an LSF property is a grid property, so without this it would be routed to its group's controller - which
        // for a group React draws is a set of deliberate no-ops, and its values would be dropped on the way
        if (property.isLsfViewPerRow()) {
            // initializeControllers makes one for every group holding such a property, by this very predicate, so a
            // miss is a broken invariant rather than a case to fall through - falling through would route the property
            // to its group's controller and drop its values there in silence
            GGridPanelController gridPanelController = gridPanelControllers.get(property.groupObject);
            assert gridPanelController != null;
            return gridPanelController;
        }

        if(property.isList) {
            return getTableController(property.groupObject);
        } else
            return panelController;
    }

    // the form's controller of an owner: where all it sends for the owner goes (initializeOwnerControllers). One React
    // draws and keeps nothing of has none, and nothing is sent for it: a property React carries by no name - the
    // platform's own COUNT of a list group, the pivot's, whose SHOWIF a group React draws never meets -, and, as a
    // component, a property whose content React draws and a component the design does not name
    // (GReactFormData.reactKeepsNothing)
    public GGroupController getGroupController(GGroupObject group) {
        return groupControllers.get(group);
    }
    public GPropertyController getPropertyController(GPropertyDraw property) {
        GPropertyController controller = propertyControllers.get(property);
        assert controller != null : property.sID;
        return controller;
    }
    public GComponentController getComponentController(GComponent component) {
        GComponentController controller = componentControllers.get(component);
        assert controller != null : component.sID;
        return controller;
    }

    // the controller drawing this LSF property, for whoever is placing one of its per-row renderers
    public GGridPanelController getGridPanelController(GPropertyDraw property) {
        return gridPanelControllers.get(property.groupObject);
    }

    public FormDockable getFormDockableContainer(boolean isDockedModal) {
        if (isDockedModal) {
            FormContainer contextContainer = getContextContainer();
            return contextContainer instanceof FormDockable ? (FormDockable) contextContainer : null;
        } else {
            return null;
        }
    }

    private FormContainer getContextContainer() {
        GFormController contextForm = getContextForm();
        return contextForm != null ? contextForm.formContainer : null;
    }

    private GFormController getContextForm() {
        if (formHidden) {
            GFormController contextForm = formContainer.getContextForm();
            return contextForm != null ? contextForm.getContextForm() : null;
        } else {
            return this;
        }
    }

    public void onServerInvocationResponse(ServerResponseResult response) {
        formsController.onServerInvocationResponse(response, getAsyncFormController(response.requestIndex));
    }

    public void onServerInvocationFailed(ExceptionResult exceptionResult) {
        formsController.onServerInvocationFailed(getAsyncFormController(exceptionResult.requestIndex));
        applyRemoteChanges(new GFormChanges(), (int) exceptionResult.requestIndex);
    }

    public long changeGroupObject(final GGroupObject group, GGroupObjectValue key, GChangeSelection changeSelection, NativeHashMap<GGroupObjectValue, PValue> changeSelectionRows) {
        long requestIndex = asyncResponseDispatch(new ChangeGroupObject(group.ID, key, changeSelection));
        pendingChangeGroupObject(group, changeSelectionRows, requestIndex);
        // optimistic: the new current where React draws the rows, now (reconciled later by fc.objects)
        getGroupController(group).changeCurrentKey(key);
        refreshReactOptimistic();
        return requestIndex;
    }

    private void pendingChangeGroupObject(GGroupObject group, NativeHashMap<GGroupObjectValue, PValue> changeSelectionRows, long requestIndex) {
        pendingChangeCurrentObjectsRequests.put(group, requestIndex);
        if(changeSelectionRows != null)
            changeSelectionRows.foreachEntry((k, v) -> putToDoubleNativeMap(pendingChangePropertyRequests, group.rowSelectReader, k, new Change(requestIndex, v, GridDataRecord.invertSelect(v))));
    }

    private final NativeSIDMap<GGroupObject, NativeHashMap<GGroupObjectValue, PValue>> delayedChangeSelectionRows = new NativeSIDMap<>();
    private final NativeSIDMap<GGroupObject, GChangeSelection> delayedChangeSelections = new NativeSIDMap<>();

    // has to be called setCurrentKey before
    public void changeGroupObjectLater(final GGroupObject group, final GGroupObjectValue key, GChangeSelection changeSelection, NativeHashMap<GGroupObjectValue, PValue> changeSelectionRows) {
        GChangeSelection delayedChangeSelection = delayedChangeSelections.get(group);
        if (delayedChangeSelections.containsKey(group) && delayedChangeSelection != changeSelection) {
            DeferredRunner.get().commitDelayedGroupObjectChange(group);
        }

        // we need to pend at once until we'll get the real request index
        pendingChangeGroupObject(group, changeSelectionRows, Long.MAX_VALUE);
        if(changeSelectionRows != null)
            delayedChangeSelectionRows.computeIfAbsent(group, g -> new NativeHashMap<>()).putAll(changeSelectionRows);
        delayedChangeSelections.put(group, changeSelection);

        DeferredRunner.get().scheduleGroupObjectChange(group, new DeferredRunner.AbstractCommand() {
            @Override
            public void execute() {
                delayedChangeSelections.remove(group);
                changeGroupObject(group, key, changeSelection, delayedChangeSelectionRows.remove(group));
            }
        });
    }

    public void pasteExternalTable(ArrayList<GPropertyDraw> propertyList, ArrayList<GGroupObjectValue> columnKeys, List<List<String>> table, List<List<String>> patterns) {
        pasteExternalTable(propertyList, columnKeys, table, patterns, false);
    }

    public void pasteExternalTable(ArrayList<GPropertyDraw> propertyList, ArrayList<GGroupObjectValue> columnKeys, List<List<String>> table, List<List<String>> patterns, boolean forceGroupChange) {
        ArrayList<ArrayList<Object>> values = new ArrayList<>();
        ArrayList<ArrayList<String>> rawValues = new ArrayList<>();

        for (int j = 0; j < table.size(); j++) {
            List<String> sRow = table.get(j);
            List<String> pRow = patterns.get(j);
            ArrayList<Object> valueRow = new ArrayList<>();
            ArrayList<String> rawValueRow = new ArrayList<>();

            for (int i = 0, propertyColumns = propertyList.size(); i < propertyColumns; i++) {
                GPropertyDraw property = propertyList.get(i);
                String sCell = i < sRow.size() ? sRow.get(i) : null;
                String pCell = i < pRow.size() ? pRow.get(i) : null;

                GType externalType = property.getExternalChangeType();
                if (externalType == null)
                    externalType = property.getPasteType();
                valueRow.add(PValue.convertFileValueBack(property.parsePaste(sCell, externalType, pCell)));
                rawValueRow.add(sCell);
            }
            values.add(valueRow);
            rawValues.add(rawValueRow);
        }

        final ArrayList<Integer> propertyIdList = new ArrayList<>();
        for (GPropertyDraw propertyDraw : propertyList) {
            propertyIdList.add(propertyDraw.ID);
        }

        syncResponseDispatch(new PasteExternalTable(propertyIdList, columnKeys, values, rawValues, forceGroupChange));
    }

    public void copyExternalTable(ArrayList<GPropertyDraw> propertyList, ArrayList<GGroupObjectValue> columnKeys, Consumer<List<List<String>>> callback) {
        final ArrayList<Integer> propertyIdList = new ArrayList<>();
        for (GPropertyDraw propertyDraw : propertyList) {
            propertyIdList.add(propertyDraw.ID);
        }

        asyncDispatch(new CopyExternalTable(propertyIdList, columnKeys), new SimpleRequestCallback<CopyExternalTableResult>() {
            @Override
            protected void onSuccess(CopyExternalTableResult result) {
                // Process values with convertFileValue and formatCopy (symmetrical to paste)
                ArrayList<ArrayList<Object>> values = result.getValues();
                ArrayList<ArrayList<String>> rawValues = result.getRawValues();
                List<List<String>> table = new ArrayList<>();

                for (int j = 0; j < values.size(); j++) {
                    ArrayList<Object> valueRow = values.get(j);
                    ArrayList<String> rawValueRow = rawValues.get(j);
                    ArrayList<String> stringRow = new ArrayList<>();

                    for (int i = 0; i < propertyList.size() && i < valueRow.size(); i++) {
                        GPropertyDraw property = propertyList.get(i);
                        Object value = valueRow.get(i);
                        String rawValue = rawValueRow.get(i);

                        // Convert file values (symmetrical to convertFileValueBack in paste)
                        PValue pValue = PValue.convertFileValue((Serializable) value);

                        // Format for clipboard (symmetrical to parsePaste in paste)
                        GType copyType = property.getPasteType();

                        stringRow.add(copyType != null ? property.formatCopy(pValue, copyType, property.getPattern()) : rawValue);
                    }
                    table.add(stringRow);
                }

                callback.accept(table);
            }
        });
    }

    public void pasteValue(ExecuteEditContext editContext, String sValue, boolean forceGroupChange) {
        GPropertyDraw property = editContext.getProperty();
        GType externalType = property.getExternalChangeType();
        String pattern = editContext.getUpdateContext().getPattern();
        if(externalType != null) {
            changeProperty(editContext, property.parsePaste(sValue, externalType, pattern), forceGroupChange, GEventSource.PASTE, null);
        } else {
            ArrayList<GPropertyDraw> propertyList = new ArrayList<>();
            propertyList.add(property);
            ArrayList<GGroupObjectValue> columnKeys = new ArrayList<>();
            columnKeys.add(editContext.getColumnKey());
            ArrayList<String> row = new ArrayList<>();
            row.add(sValue);
            List<List<String>> table = new ArrayList<>();
            table.add(row);
            ArrayList<String> rowPattern = new ArrayList<>();
            rowPattern.add(pattern);
            List<List<String>> tablePattern = new ArrayList<>();
            tablePattern.add(rowPattern);

            pasteExternalTable(propertyList, columnKeys, table, tablePattern, forceGroupChange);
        }
    }

    public void changePageSizeAfterUnlock(final GGroupObject groupObject, final int pageSize) {
        Scheduler.get().scheduleFixedPeriod(new Scheduler.RepeatingCommand() {
            @Override
            public boolean execute() {
                if (dispatcher.getBusyDialogDisplayer().isVisible()) {
                    return true;
                } else {
                    changePageSizeLater(groupObject, pageSize);
                    return false;
                }
            }
        }, 1000);
    }

    private void changePageSizeLater(final GGroupObject groupObject, final int pageSize) {
        DeferredRunner.get().scheduleChangePageSize(groupObject, new DeferredRunner.AbstractCommand() {
            @Override
            public void execute() {
                changePageSize(groupObject, pageSize);
            }
        });
    }

    private void changePageSize(GGroupObject groupObject, int pageSize) {
        asyncResponseDispatch(new ChangePageSize(groupObject.ID, pageSize));
    }

    public void scrollToEnd(GGroupObject group, boolean toEnd, GChangeSelection changeSelection) {
        syncResponseDispatch(new ScrollToEnd(group.ID, toEnd, changeSelection));
    }

    public void selectAll(GGroupObject group, NativeHashMap<GGroupObjectValue, PValue> changeSelectionRows, ArrayList<GGridTable.ColumnSelection> changeSelectionColumns) {
        int size = changeSelectionColumns.size();
        int[] changeSelectionProps = new int[size];
        GGroupObjectValue[] changeSelectionColumnKeys = new GGroupObjectValue[size];
        boolean[] changeSelectionValues = new boolean[size];
        for (int i = 0; i < size; i++) {
            GGridTable.ColumnSelection selection = changeSelectionColumns.get(i);
            changeSelectionProps[i] = selection.property.ID;
            changeSelectionColumnKeys[i] = selection.columnKey;
            changeSelectionValues[i] = selection.set;
        }

        DeferredRunner.get().commitDelayedGroupObjectChange(group);

        long requestIndex = syncResponseDispatch(new SelectAll(group.ID, changeSelectionProps, changeSelectionColumnKeys, changeSelectionValues));
        pendingChangeGroupObject(group, changeSelectionRows, requestIndex);
    }

    public void onPropertyBinding(Event bindingEvent, ExecuteEditContext editContext) {
        if(GKeyStroke.isKeyEvent(bindingEvent) && editContext.isFocusable()) // we don't want to set focus on mouse binding (it's pretty unexpected behaviour)
            editContext.focus(FocusUtils.Reason.BINDING); // we want element to be focused on key binding (if it's possible)

        executePropertyEventAction(new EventHandler(bindingEvent), true, editContext);
    }

    public void executePropertyEventAction(EventHandler handler, ExecuteEditContext editContext) {
        executePropertyEventAction(handler, false, editContext);
    }

    public void executePropertyEventAction(EventHandler handler, boolean isBinding, ExecuteEditContext editContext) {
        Event event = handler.event;
        GPropertyDraw property = editContext.getProperty();

        GEventSource eventSource = isBinding ? GEventSource.BINDING : GEventSource.EDIT;
        if(BrowserEvents.CONTEXTMENU.equals(event.getType())) {
            handler.consume();
            GPropertyContextMenuPopup.show(new PopupOwner(editContext.getPopupOwnerWidget(), Element.as(event.getEventTarget())), property, actionSID -> {
                executePropertyEventAction(editContext, actionSID, eventSource, handler);
            });
        } else {
            lsfusion.gwt.client.base.Result<Integer> contextAction = new lsfusion.gwt.client.base.Result<>();
            String actionSID = property.getEventSID(event, isBinding, editContext, contextAction);
            if(actionSID == null) {
                if(this.editContext != editContext && property.isSuppressedPanelSingleClick(event, editContext)) {
                    Element editElement = editContext.getEditElement();
                    Element nativeEventElement = editElement != null ? InputBasedCellRenderer.getFocusEventTarget(editElement, event) : null;
                    if(nativeEventElement != null)
                        MainFrame.preventClickAfterDown(nativeEventElement, event);
                    handler.consume(false, true);
                }
                return;
            }

            // hasChangeAction check is important for quickfilter not to consume event (however with propertyReadOnly, checkCanBeChanged there will be still some problems)
            if (isChangeEvent(actionSID) && (editContext.isReadOnly() != null || (contextAction.result == null && !property.hasUserChangeAction)))
                return;
            if(GEditBindingMap.EDIT_OBJECT.equals(actionSID) && !property.hasEditObjectAction)
                return;

            if(contextAction.result != null)
                executeContextAction(handler, editContext, actionSID, eventSource, contextAction.result);
            else
                executePropertyEventAction(editContext, actionSID, eventSource, handler);

            if(!handler.consumed)
                handler.consume();
        }
    }

    public void executePropertyEventAction(ExecuteEditContext editContext, String actionSID, GEventSource eventSource, EventHandler handler) {
        GPropertyDraw property = editContext.getProperty();
        if (isChangeEvent(actionSID) && property.askConfirm) {
            DialogBoxHelper.showConfirmBox("lsFusion", EscapeUtils.toHTML(property.askConfirmMessage, StaticImage.MESSAGE_WARN), editContext.getPopupOwner(), chosenOption -> {
                        if (chosenOption == DialogBoxHelper.OptionType.YES)
                            executePropertyEventActionConfirmed(editContext, actionSID, eventSource, handler);
                    });
        } else
            executePropertyEventActionConfirmed(editContext, actionSID, eventSource, handler);
    }

    public void executePropertyEventActionConfirmed(ExecuteEditContext editContext, String actionSID, GEventSource eventSource, EventHandler handler) {
        executePropertyEventAction(handler, editContext, editContext, actionSID, eventSource, requestIndex -> setLoading(editContext, requestIndex));
    }

    public void executePropertyEventAction(GPropertyDraw[] properties, GGroupObjectValue[] fullKeys, PValue[] newValues, Consumer<Long> onExec) {
        int length = properties.length;
        GEventSource[] eventSources = new GEventSource[length];
        GPushAsyncResult[] pushAsyncResults = new GPushAsyncResult[length];
        // the guard GPropertyDraw states for the same mistake, in the terms this path has: a property that is not a
        // list has no group to change, and a call that CARRIES a value carries the value its caller wrote, which is
        // not the cell gesture a group change is for. Without it a change made while the user happens to hold Shift -
        // typing a capital letter into a view's own input does exactly that - became a GROUP change and the value was
        // dropped. A call with no value is left alone: that is a view forwarding a click, where a held modifier still
        // means what it means anywhere else. GPropertyDraw says it as changeOrGroupChange(isCharAddKeyEvent(editEvent))
        boolean noGroupChange = false;
        for (int i = 0; i < length; i++) {
            eventSources[i] = GEventSource.CUSTOM;
            PValue newValue = newValues[i];
            pushAsyncResults[i] = newValue == PValue.UNDEFINED ? null : new GPushAsyncInput(GUserInputResult.singleValue(newValue));

            noGroupChange = noGroupChange || !properties[i].isList || pushAsyncResults[i] != null;
        }
        String actionSID = GEditBindingMap.changeOrGroupChange(noGroupChange);
        if(length == 1 && pushAsyncResults[0] == null) // execute action / event
            executePropertyEventAction(properties[0], fullKeys[0], actionSID, eventSources[0], onExec);
        else // change properties with value
            onExec.accept(asyncExecutePropertyEventAction(actionSID, null, null, properties, eventSources, fullKeys, pushAsyncResults));
    }

    public void executePropertyEventAction(GPropertyDraw property, GGroupObjectValue fullKey, String actionSID, GEventSource eventSource, Consumer<Long> onExec) {
        executePropertyEventAction(null, null, new ExecContext() {
            @Override
            public GPropertyDraw getProperty() {
                return property;
            }

            @Override
            public GGroupObjectValue getFullKey() {
                return fullKey;
            }
        }, actionSID, eventSource, onExec);
    }

    public void executePropertyEventAction(EventHandler handler, EditContext editContext, ExecContext execContext, String actionSID, GEventSource eventSource, Consumer<Long> onExec) {
        executePropertyEventAction(execContext.getProperty().getAsyncEventExec(actionSID), handler, editContext, execContext, actionSID, null, eventSource, onExec);
    }

    private void executePropertyEventAction(GAsyncEventExec asyncEventExec, EventHandler handler, EditContext editContext, ExecContext execContext, String actionSID, GUserInputResult value, GEventSource eventSource, Consumer<Long> onExec) {
        GPushAsyncInput pushAsyncInput = value != null ? new GPushAsyncInput(value) : null;
        if (asyncEventExec != null)
            asyncEventExec.exec(this, handler, editContext, execContext, actionSID, pushAsyncInput, eventSource, onExec);
        else
            syncExecutePropertyEventAction(editContext, handler, execContext.getProperty(), execContext.getFullKey(), pushAsyncInput, actionSID, eventSource, onExec);
    }

    public void asyncExecutePropertyEventAction(String actionSID, EditContext editContext, ExecContext execContext, EventHandler handler, GPushAsyncResult pushAsyncResult, GEventSource eventSource, Consumer<Long> onRequestExec, Consumer<Long> onExec) {
        long requestIndex = asyncExecutePropertyEventAction(actionSID, editContext, handler, new GPropertyDraw[]{execContext.getProperty()}, new GEventSource[]{eventSource}, new GGroupObjectValue[]{execContext.getFullKey()}, new GPushAsyncResult[]{pushAsyncResult});

        onExec.accept(requestIndex);

        // should be after because for example onExec can setRemoteValue, but onRequestExec also does that and should have higher priority
        onRequestExec.accept(requestIndex);
    }

    public void syncExecutePropertyEventAction(EditContext editContext, EventHandler handler, GPropertyDraw property, GGroupObjectValue fullKey, GPushAsyncInput pushAsyncResult, String actionSID, GEventSource eventSource, Consumer<Long> onExec) {
        long requestIndex = executePropertyEventAction(actionSID, true, editContext, handler, new GPropertyDraw[]{property}, new GEventSource[]{eventSource}, new GGroupObjectValue[]{fullKey}, new GPushAsyncResult[] {pushAsyncResult});

        onExec.accept(requestIndex);
    }

    public long asyncExecutePropertyEventAction(String actionSID, EditContext editContext, EventHandler handler, GPropertyDraw[] properties, GEventSource[] eventSources, GGroupObjectValue[] fullKeys, GPushAsyncResult[] pushAsyncResults) {
        return executePropertyEventAction(actionSID, false, editContext, handler, properties, eventSources, fullKeys, pushAsyncResults);
    }

    private long executePropertyEventAction(String actionSID, boolean sync, EditContext editContext, EventHandler handler, GPropertyDraw[] properties, GEventSource[] eventSources, GGroupObjectValue[] fullKeys, GPushAsyncResult[] pushAsyncResults) {
        int length = properties.length;
        int[] IDs = new int[length];
        GGroupObjectValue[] fullCurrentKeys = new GGroupObjectValue[length];
        for (int i = 0; i < length; i++) {
            GPropertyDraw property = properties[i];
            IDs[i] = property.ID;
            fullCurrentKeys[i] = getFullCurrentKey(property, fullKeys[i]);
        }

        ExecuteEventAction executeEventAction = new ExecuteEventAction(IDs, fullCurrentKeys, actionSID, eventSources, pushAsyncResults);
        ServerResponseCallback serverResponseCallback = new ServerResponseCallback() {
            @Override
            protected Runnable getOnRequestFinished() {
                return () -> {
                    actionDispatcher.editContext = null;
                    actionDispatcher.editEventHandler = null;
                };
            }

            @Override
            public void onSuccess(ServerResponseResult response, Runnable onDispatchFinished) {
                actionDispatcher.editContext = editContext;
                actionDispatcher.editEventHandler = handler;
                super.onSuccess(response, onDispatchFinished);
            }

            @Override
            public void onFailure(ExceptionResult exceptionResult) {
                actionDispatcher.editContext = editContext;
                super.onFailure(exceptionResult);
            }
        };

        if(sync)
            return syncDispatch(executeEventAction, serverResponseCallback);
        else
            return asyncDispatch(executeEventAction, serverResponseCallback);
    }

    public long asyncResponseDispatch(final FormRequestCountingAction<ServerResponseResult> action) {
        return asyncDispatch(action, new ServerResponseCallback());
    }
    public long syncResponseDispatch(final FormRequestCountingAction<ServerResponseResult> action) {
        return syncDispatch(action, new ServerResponseCallback());
    }

    public void asyncInput(EventHandler handler, EditContext editContext, ExecContext execContext, String actionSID, GAsyncInput asyncChange, GEventSource eventSource, Consumer<Long> onExec) {
        if (editContext == null) { // no cell to host the editor on the controller / global change path - run on the server
            syncExecutePropertyEventAction(null, handler, execContext.getProperty(), execContext.getFullKey(), null, actionSID, eventSource, onExec);
            return;
        }
        GInputList inputList = asyncChange.inputList;
        GInputListAction[] inputListActions = asyncChange.inputListActions;

        // when the value list is disabled the change type is not something to edit (for an object input without an explicit LIST it's the object's id), so the picker dialog is executed instead -
        // a mouse change is resolved to that action already (see GPropertyDraw.getEditEventSID), any other trigger would edit the raw id
        Integer dialogActionIndex = execContext.getProperty().getDialogInputActionIndex(inputListActions);
        if(dialogActionIndex != null) {
            executePropertyEventAction(handler, editContext, inputListActions, GUserInputResult.singleValue(null, dialogActionIndex), actionSID, eventSource, onExec);
            return;
        }

        edit(asyncChange.changeType, handler, false, null, asyncChange.multipleInput, inputList, inputListActions, (value, onRequestExec) ->
            executePropertyEventAction(handler, editContext, inputListActions, value, actionSID, eventSource, requestIndex -> {
                onExec.accept(requestIndex); // setLoading

                // doing that after to set the last value (onExec recursively can also set value)
                onRequestExec.accept(requestIndex); // pendingChangeProperty
        }), cancelReason -> {}, editContext, actionSID, asyncChange.customEditFunction);
    }

    private final static GAsyncNoWaitExec asyncExec = new GAsyncNoWaitExec();

    private void executePropertyEventAction(EventHandler handler, EditContext editContext, GInputListAction[] inputListActions, GUserInputResult value, String actionSID, GEventSource eventSource, Consumer<Long> onExec) {
        GInputListAction contextAction = getContextAction(inputListActions, value);
        executePropertyEventAction(contextAction != null ? contextAction.asyncExec : asyncExec, handler, editContext, editContext, actionSID, value, eventSource, onExec);
    }

    private GInputListAction getContextAction(GInputListAction[] inputListActions, GUserInputResult value) {
        Integer contextActionIndex = value != null ? value.getContextAction() : null;
        if (contextActionIndex != null) {
            for (GInputListAction action : inputListActions) {
                if (action.index == contextActionIndex)
                    return action;
            }
        }
        return null;
    }

    public void asyncOpenForm(GAsyncOpenForm asyncOpenForm, EditContext editContext, ExecContext execContext, EventHandler handler, String actionSID, GPushAsyncInput pushAsyncResult, GEventSource eventSource, Consumer<Long> onExec) {
        // External changes can use a different prediction on the server, including another open form.
        GPushAsyncResult activated = pushAsyncResult == null && (eventSource == GEventSource.EDIT || eventSource == GEventSource.BINDING)
                ? formsController.reuseOpenForm(asyncOpenForm, handler != null ? handler.event : null) : null;
        asyncExecutePropertyEventAction(actionSID, editContext, execContext, handler,
                activated != null ? activated : pushAsyncResult, eventSource, requestIndex -> {
            if (activated == null)
                formsController.asyncOpenForm(getAsyncFormController(requestIndex), asyncOpenForm, editEvent, editContext, execContext, this);
        }, onExec);
    }

    public GAsyncFormController getAsyncFormController(long requestIndex) {
        return actionDispatcher.getAsyncFormController(requestIndex);
    }

    public PopupOwner getPopupOwner() {
        return new PopupOwner(getWidget());
    }
    public void asyncCloseFormConfirmed(Runnable runnable) {
        if(needConfirm) {
            DialogBoxHelper.showConfirmBox("lsFusion", messages.doYouReallyWantToCloseForm(), getPopupOwner(), chosenOption -> {
                if(chosenOption == DialogBoxHelper.OptionType.YES) {
                    runnable.run();
                }
            });
        } else {
            runnable.run();
        }
    }

    public void asyncCloseForm(GAsyncExecutor asyncExecutor) {
        asyncCloseFormConfirmed(() -> formsController.asyncCloseForm(asyncExecutor, formContainer));
    }

    public void asyncCloseForm(EditContext editContext, ExecContext execContext, EventHandler handler, String actionSID, GPushAsyncInput pushAsyncResult, GEventSource eventSource, Consumer<Long> onExec) {
        asyncCloseFormConfirmed(() -> asyncCloseForm(editContext, execContext, handler, actionSID, eventSource, onExec));
    }

    private void asyncCloseForm(EditContext editContext, ExecContext execContext, EventHandler handler, String actionSID, GEventSource eventSource, Consumer<Long> onExec) {
        asyncExecutePropertyEventAction(actionSID, editContext, execContext, handler, new GPushAsyncClose(), eventSource, requestIndex ->
                formsController.asyncCloseForm(getAsyncFormController(requestIndex), formContainer), onExec);
    }

    public void continueServerInvocation(long requestIndex, Object actionResult, int continueIndex, RequestAsyncCallback<ServerResponseResult> callback) {
        syncDispatch(new ContinueInvocation(requestIndex, actionResult, continueIndex), callback, true);
    }

    public void throwInServerInvocation(long requestIndex, Throwable throwable, int continueIndex, RequestAsyncCallback<ServerResponseResult> callback) {
        syncDispatch(new ThrowInInvocation(requestIndex, throwable, continueIndex), callback, true);
    }

    public <T extends Result> long asyncDispatch(final FormRequestCountingAction<T> action, RequestCountingAsyncCallback<T> callback) {
        return dispatcher.asyncExecute(action, callback);
    }

    public <T extends Result> long syncDispatch(final FormRequestCountingAction<T> action, RequestCountingAsyncCallback<T> callback) {
        return syncDispatch(action, callback, false);
    }

    public <T extends Result> long syncDispatch(final FormRequestAction<T> action, RequestAsyncCallback<T> callback, boolean continueInvocation) {
        return dispatcher.syncExecute(action, callback, continueInvocation);
    }

    public GGroupObjectValue getFullCurrentKey(GPropertyDraw property, GGroupObjectValue fullKey) {
        DeferredRunner.get().commitDelayedGroupObjectChange(property.groupObject);

        GGroupObjectValueBuilder fullCurrentKey = new GGroupObjectValueBuilder();

        // there was a check that if groupObject is list, then currentKey should not be empty, but I'm not sure what for this check was needed
//        property.groupObject isList isEmpty() ??

        for (GGroupObject group : form.groupObjects) {
            GGroupObjectValue current = getGroupController(group).getSelectedKey();
            if (current != null)
                fullCurrentKey.putAll(current);
        }

        fullCurrentKey.putAll(fullKey);

        return fullCurrentKey.toGroupObjectValue();
    }

    public void changeProperty(ExecuteEditContext editContext, PValue changeValue, ChangedRenderValueSupplier renderValueSupplier) {
        changeProperty(editContext, changeValue, false, GEventSource.CUSTOM, renderValueSupplier);
    }

    // for custom renderer, paste
    public void changeProperty(ExecuteEditContext editContext, PValue changeValue, boolean forceGroupChange, GEventSource eventSource, ChangedRenderValueSupplier renderValueSupplier) {
        ChangedRenderValue changedRenderValue = changeValue != PValue.UNDEFINED ? setLocalValue(editContext, changeValue, renderValueSupplier) : null;
        // noGroupChange is needed for custom renderers that use onBlur to change values:
        // a) when ALT+TAB pressed there is no keydown previewed to disable group change mode, which is not what we want
        // b) when binding with ALT calls check commit editing, we don't want it to be treated as the group change
        String actionSID = forceGroupChange ? GEditBindingMap.GROUP_CHANGE : GEditBindingMap.changeOrGroupChange(!editContext.getProperty().isList || MainFrame.switchedToAnotherWindow || forcedBlurCustom);
        executePropertyEventAction(null, editContext, actionSID, eventSource, changeValue == PValue.UNDEFINED ? null : GUserInputResult.singleValue(changeValue), requestIndex -> {
            setRemoteValue(editContext, changedRenderValue, requestIndex);
        });
    }

    // for quick access actions (in toolbar and keypress)
    public void executeContextAction(EventHandler handler, ExecuteEditContext editContext, String actionSID, GEventSource eventSource, int contextAction) {
        executePropertyEventAction(handler, editContext, actionSID, eventSource, GUserInputResult.singleValue(null, contextAction), requestIndex -> {});
    }

    // for custom renderer, paste, quick access actions (in toolbar and keypress)
    private void executePropertyEventAction(EventHandler handler, ExecuteEditContext editContext, String actionSID, GEventSource eventSource, GUserInputResult value, Consumer<Long> onExec) {
        executePropertyEventAction(handler, editContext, editContext.getProperty().getInputListActions(), value, actionSID, eventSource, requestIndex -> {
            onExec.accept(requestIndex);

            setLoading(editContext, requestIndex);
        });
    }

    public interface ChangedRenderValueSupplier {
        PValue getValue(PValue oldValue, PValue changeValue);
    }
    private static class ChangedRenderValue {
        public final PValue oldValue;
        public final PValue newValue;

        public ChangedRenderValue(PValue oldValue, PValue newValue) {
            this.oldValue = oldValue;
            this.newValue = newValue;
        }
    }

    public ChangedRenderValue setLocalValue(EditContext editContext, PValue changeValue, ChangedRenderValueSupplier renderValueSupplier) {
        return setLocalValue(editContext, editContext.getProperty().getExternalChangeType(), changeValue, renderValueSupplier);
    }
    public ChangedRenderValue setLocalValue(EditContext editContext, GType changeType, PValue changeValue, ChangedRenderValueSupplier renderValueSupplier) {
        if(renderValueSupplier != null || editContext.canUseChangeValueForRendering(changeType)) {
            PValue oldValue = editContext.getValue();

            PValue newValue = renderValueSupplier != null ? renderValueSupplier.getValue(oldValue, changeValue) : changeValue;
            editContext.setValue(newValue);

            return new ChangedRenderValue(oldValue, newValue);
        }
        return null;
    }

    public void setRemoteValue(EditContext editContext, ChangedRenderValue changedRenderValue, long requestIndex) {
        if(changedRenderValue != null)
            pendingChangeProperty(editContext.getProperty(), editContext.getFullKey(), changedRenderValue.newValue, changedRenderValue.oldValue, requestIndex);
    }

    public void pendingChangeProperty(GPropertyDraw property, GGroupObjectValue fullKey, PValue value, PValue oldValue, long changeRequestIndex) {
        putToDoubleNativeMap(pendingChangePropertyRequests, property, fullKey, new Change(changeRequestIndex, value, oldValue));
    }

    public void asyncNoWait(EditContext editContext, ExecContext execContext, EventHandler handler, String actionSID, GAsyncNoWaitExec asyncNoWait, GPushAsyncInput pushAsyncResult, GEventSource eventSource, Consumer<Long> onExec) {
        asyncExecutePropertyEventAction(actionSID, editContext, execContext, handler, pushAsyncResult, eventSource, requestIndex -> {}, onExec);
    }

    public void asyncChange(EditContext editContext, ExecContext execContext, EventHandler handler, String actionSID, GAsyncChange asyncChange, GPushAsyncInput pushAsyncResult, GEventSource eventSource, Consumer<Long> onExec) {
        asyncExecutePropertyEventAction(actionSID, editContext, execContext, handler, pushAsyncResult, eventSource, requestIndex -> {
            for (int propertyID : asyncChange.propertyIDs)
                setLoadingValueAt(getProperty(propertyID), execContext.getFullKey(), PValue.convertFileValue(asyncChange.value), requestIndex);
            refreshReactOptimistic();
        }, onExec);
    }


    public void asyncAddRemove(EditContext editContext, ExecContext execContext, EventHandler handler, String actionSID, GAsyncAddRemove asyncAddRemove, GPushAsyncInput pushAsyncResult, GEventSource eventSource, Consumer<Long> onExec) {
        final GObject object = form.getObject(asyncAddRemove.object);
        final boolean add = asyncAddRemove.add;

        GGroupController controller = getGroupController(object.groupObject);

        final int position = controller.getSelectedRow();

        if (add) {
            MainFrame.logicsDispatchAsync.executePriority(new GenerateID(), new PriorityErrorHandlingCallback<GenerateIDResult>(editContext != null ? editContext.getPopupOwner() : getPopupOwner()) { // no cell on the controller path - form popup owner
                @Override
                public void onSuccess(GenerateIDResult result) {
                    asyncAddRemove(editContext, execContext, handler, actionSID, object, add, new GPushAsyncAdd(result.ID), new GGroupObjectValue(object.ID, new GCustomObjectValue(result.ID, null)), position, eventSource, onExec);
                }
            });
        } else {
            DeferredRunner.get().commitDelayedGroupObjectChange(object.groupObject);

            // the optimistic removal must drop the row the action RUNS ON, not whichever row happens to be selected: a
            // custom view can exec the delete on any row (controller.changeProperty('DELETE', row)), and the server does
            // delete that row — predicting the selected one instead made the wrong row vanish until the server corrected
            // it. On the classic toolbar path the action runs on the current row, so the two agree.
            GGroupObjectValue value = object.groupObject.filterRowKeys(execContext.getFullKey());
            if(value == null || value.isEmpty())
                value = controller.getSelectedKey();
            if(value == null || value.isEmpty())
                return;
            asyncAddRemove(editContext, execContext, handler, actionSID, object, add, pushAsyncResult, value, position, eventSource, onExec);
        }
    }

    private void asyncAddRemove(EditContext editContext, ExecContext execContext, EventHandler handler, String actionSID, GObject object, boolean add, GPushAsyncResult pushAsyncResult, GGroupObjectValue value, int position, GEventSource eventSource, Consumer<Long> onExec) {
        asyncExecutePropertyEventAction(actionSID, editContext, execContext, handler, pushAsyncResult, eventSource, requestIndex -> {
            pendingChangeCurrentObjectsRequests.put(object.groupObject, requestIndex);
            pendingModifyObjectRequests.add(new ModifyObject(requestIndex, object, add, value, position));

            getGroupController(object.groupObject).modifyGroupObject(value, add, -1);
            refreshReactOptimistic();
        }, onExec);
    }

    // a group's user orders as a client sets them: shown at once, and sent whole; true - ascending. A header only asks
    // for them, as the form keeps them: they are shown through the hook the reports use, unlike the filters, which the
    // filter panel shows itself
    public void changeOrders(GGroupObject group, LinkedHashMap<Column, Boolean> orders) {
        showOrders(group, orders);

        List<Integer> propertyList = new ArrayList<>();
        List<GGroupObjectValue> columnKeyList = new ArrayList<>();
        List<Boolean> orderList = new ArrayList<>();
        for (Map.Entry<Column, Boolean> order : orders.entrySet()) {
            propertyList.add(order.getKey().property.ID);
            columnKeyList.add(order.getKey().columnKey);
            orderList.add(order.getValue());
        }
        long requestIndex = asyncResponseDispatch(new SetPropertyOrders(group.ID, propertyList, columnKeyList, orderList));
        pendingChangeOrdersRequests.put(group, requestIndex);
    }

    public LinkedHashMap<Column, Boolean> getOrders(GGroupObject group) {
        LinkedHashMap<Column, Boolean> orders = currentOrders.get(group);
        return orders != null ? orders : new LinkedHashMap<>();
    }

    public long expandGroupObjectRecursive(GGroupObject group, boolean current, boolean open) {
        DeferredRunner.get().commitDelayedGroupObjectChange(group);
        return asyncResponseDispatch(open ? new ExpandGroupObjectRecursive(group.ID, current) : new CollapseGroupObjectRecursive(group.ID, current));
    }

    public long expandGroupObject(GGroupObject group, GGroupObjectValue value, boolean open) {
        DeferredRunner.get().commitDelayedGroupObjectChange(group);
        return asyncResponseDispatch(open ? new ExpandGroupObject(group.ID, value) : new CollapseGroupObject(group.ID, value));
    }

    public void setTabActive(GContainer tabbedPane, GComponent visibleComponent) {
        asyncResponseDispatch(new SetTabActive(tabbedPane.ID, visibleComponent.ID));

        formLayout.updatePanels(); // maybe it's not needed, but we want to make it symmetrical to the container collapsed call
    }

    private GPropertyDraw prevPropertyActive;

    public void updatePropertyActive(GPropertyDraw property, GGroupObjectValue columnKey, boolean focused,
                                     ArrayList<GGridTable.ColumnSelection> changeSelectionColumns) {
        if (prevPropertyActive != null && prevPropertyActive.hasActiveProperty || (property != null && property.hasActiveProperty) || changeSelectionColumns != null) {
            int[] changeSelectionProps = null;
            GGroupObjectValue[] changeSelectionColumnKeys = null;
            boolean[] changeSelectionValues = null;
            if(changeSelectionColumns != null) {
                int size = changeSelectionColumns.size();
                changeSelectionProps = new int[size];
                changeSelectionColumnKeys = new GGroupObjectValue[size];
                changeSelectionValues = new boolean[size];
                for (int i = 0; i < size; i++) {
                    GGridTable.ColumnSelection selection = changeSelectionColumns.get(i);
                    changeSelectionProps[i] = selection.property.ID;
                    changeSelectionColumnKeys[i] = selection.columnKey;
                    changeSelectionValues[i] = selection.set;
                }
            }

            asyncResponseDispatch(new ChangePropertyActive(property != null ? property.ID : -1, columnKey, focused,
                    changeSelectionProps, changeSelectionColumnKeys, changeSelectionValues));
        }
        prevPropertyActive = property;
    }
    
    public void setContainerCollapsed(GContainer container, boolean collapsed) {
        setUserHidden(container, collapsed);

        formLayout.updatePanels(); // we want to avoid blinking between setting visibility and getting response (and having updatePanels there)
    }

    // one RPC for every user-driven hide: a collapsed container (above), or a CUSTOM REACT component showing / hiding a
    // lsf child so its data is not read while React does not show it
    public void setUserHidden(GComponent component, boolean hidden) {
        asyncResponseDispatch(new SetUserHidden(component.ID, hidden));
    }

    private void setRemoteRegularFilter(GRegularFilterGroup filterGroup, GRegularFilter filter) {
        syncResponseDispatch(new SetRegularFilter(filterGroup.ID, (filter == null) ? -1 : filter.ID));
    }

    public long changeFilters(GGroupObject groupObject, ArrayList<GPropertyFilter> conditions) {
        currentFilters.put(groupObject, getFilterDTOs(conditions));
        return applyCurrentFilters(Collections.singletonList(groupObject));
    }

    public long changeFilters(GTreeGroup treeGroup, ArrayList<GPropertyFilter> conditions) {
        Map<GGroupObject, ArrayList<GPropertyFilter>> filters = GwtSharedUtils.groupList(new GwtSharedUtils.Group<GGroupObject, GPropertyFilter>() {
            public GGroupObject group(GPropertyFilter key) {
                return key.groupObject;
            }
        }, conditions);

        for (GGroupObject group : treeGroup.groups) {
            ArrayList<GPropertyFilter> groupFilters = filters.get(group);
            if (groupFilters == null) {
                groupFilters = new ArrayList<>();
            }
            currentFilters.put(group, getFilterDTOs(groupFilters));
        }

        return applyCurrentFilters(treeGroup.groups);
    }

    // the filters as a client sends them: an action is no filter
    private static ArrayList<GPropertyFilterDTO> getFilterDTOs(List<GPropertyFilter> filters) {
        ArrayList<GPropertyFilterDTO> filterDTOs = new ArrayList<>();
        for (GPropertyFilter filter : filters) {
            if (!filter.property.isAction()) {
                filterDTOs.add(filter.getFilterDTO());
            }
        }
        return filterDTOs;
    }

    private long applyCurrentFilters(List<GGroupObject> groups) {
        Map<Integer, List<GPropertyFilterDTO>> filters = new LinkedHashMap<>();
        for (GGroupObject group : groups) {
            filters.put(group.ID, currentFilters.get(group));
        }
        long requestIndex = asyncResponseDispatch(new SetUserFilters(filters));
        for (GGroupObject group : groups) {
            pendingChangeFiltersRequests.put(group, requestIndex);
        }
        return requestIndex;
    }

    public void setViewFilters(ArrayList<GPropertyFilter> conditions, int pageSize) {
        asyncResponseDispatch(new SetViewFilters(conditions.stream().map(GPropertyFilter::getFilterDTO).collect(Collectors.toCollection(ArrayList::new)), pageSize));
    }

    public void quickFilter(Event event, int initialFilterPropertyID) {
        GPropertyDraw propertyDraw = getProperty(initialFilterPropertyID);
        if (propertyDraw != null && gridControllers.containsKey(propertyDraw.groupObject)) {
            focusProperty(propertyDraw);
            gridControllers.get(propertyDraw.groupObject).quickEditFilter(event, propertyDraw, GGroupObjectValue.EMPTY);
        }
    }

    public void focusProperty(GPropertyDraw propertyDraw) {
        // react-owned -> nowhere, React draws it; non-react -> the real controller
        getPropertyController(propertyDraw).focusProperty(propertyDraw);
    }

    public void setLoadingValueAt(GPropertyDraw property, GGroupObjectValue fullKey, PValue value, long requestIndex) {
        GGroupObjectValue fullCurrentKey = getFullCurrentKey(property, fullKey);
        Pair<GGroupObjectValue, PValue> propertyCell = getPropertyController(property).setLoadingValueAt(property, fullCurrentKey, value);

        if(propertyCell != null) {
            pendingChangeProperty(property, propertyCell.first, value, propertyCell.second, requestIndex);
            pendingLoadingProperty(property, propertyCell.first, requestIndex);
        }
    }
    // publish the projection once an optimistic operation is done - a change of the current object, a batch of property
    // changes, a row added or removed - whatever of it went into the drafts; a controller writes and never publishes.
    // The server's response is reconciled through the same controllers and published by applyRemoteChanges' own flush
    private void refreshReactOptimistic() {
        reactData.flush();

        updateRowRenderers(); // an optimistically added row gets its editors now, not when the server confirms it
    }

    private void updateRowRenderers() {
        for (GGridPanelController gridPanelController : gridPanelControllers.values())
            gridPanelController.updateRowRenderers();
    }

    private Map<Integer, Integer> getTabMap(TabbedContainerView containerView, GContainer component) {
        Map<Integer, Integer> tabMap = new HashMap<>();
        ArrayList<GComponent> tabs = component.children;
        if (tabs != null) {
            int c = 0;
            for (int i = 0; i < tabs.size(); i++) {
                GComponent tab = tabs.get(i);
                if (containerView.isTabVisible(tab)) {
                    tabMap.put(tab.ID, c++);
                }
            }
        }
        return tabMap;
    }

    public void activateTab(GComponent component) {
        GAbstractContainerView parentView = component.container != null ? formLayout.getContainerView(component.container) : null;
        // a container React draws has no view: nothing to switch
        if(component.isTab() && parentView instanceof TabbedContainerView)
            ((TabbedContainerView) parentView).activateTab(component);
    }

    public abstract static class CustomCallback<T> implements RequestCountingAsyncCallback<T> {

        protected abstract void onSuccess(T result);

        @Override
        public void onSuccess(T result, Runnable onDispatchFinished) {
            onSuccess(result);

            if(onDispatchFinished != null)
                onDispatchFinished.run();
        }
    }

    private abstract class SimpleRequestCallback<R> extends lsfusion.gwt.client.form.controller.SimpleRequestCallback<R> {

        public SimpleRequestCallback() {
        }

        @Override
        public PopupOwner getPopupOwner() {
            return GFormController.this.getPopupOwner();
        }
    }

    public void countRecords(final GGroupObject groupObject) {
        DeferredRunner.get().commitDelayedGroupObjectChange(groupObject); // flush the pending selection so the server aggregates the marked rows
        asyncDispatch(new CountRecords(groupObject.ID), new SimpleRequestCallback<NumberResult>() {
            @Override
            public void onSuccess(NumberResult result) {
                GGridController controller = gridControllers.get(groupObject);
                if (controller != null)
                    controller.showRecordQuantity((Long) result.value);
            }
        });
    }

    public void calculateSum(final GGroupObject groupObject, final GPropertyDraw propertyDraw, GGroupObjectValue columnKey) {
        DeferredRunner.get().commitDelayedGroupObjectChange(groupObject); // flush the pending selection so the server aggregates the marked rows
        asyncDispatch(new CalculateSum(propertyDraw.ID, columnKey), new SimpleRequestCallback<NumberResult>() {
            @Override
            public void onSuccess(NumberResult result) {
                GGridController controller = gridControllers.get(groupObject);
                if (controller != null)
                    controller.showSum(result.value, propertyDraw);
            }
        });
    }

    // change group mode with force refresh
    public long changeListViewType(final GGroupObject groupObject, int pageSize, GListViewType viewType, GUpdateMode updateMode) {
        boolean enableGroup = viewType == GListViewType.PIVOT;
        return changeMode(groupObject, true, enableGroup ? new ArrayList<>() : null, enableGroup ? new ArrayList<>() : null, 0, null, pageSize, true, updateMode, viewType);
    }
    public long changeMode(final GGroupObject groupObject, boolean setGroup, List<GPropertyDraw> properties, List<GGroupObjectValue> columnKeysList, int aggrProps, GPropertyGroupType aggrType, Integer pageSize, boolean forceRefresh, GUpdateMode updateMode, GListViewType viewType) {
        int[] propertyIDs = null;
        GGroupObjectValue[] columnKeys = null;
        if(properties != null) {
            propertyIDs = new int[properties.size()];
            columnKeys = new GGroupObjectValue[properties.size()];
            for(int i=0;i<propertyIDs.length;i++) {
                propertyIDs[i] = properties.get(i).ID;
                columnKeys[i] = columnKeysList.get(i);
            }
        }
        return asyncResponseDispatch(new ChangeMode(groupObject.ID, setGroup, propertyIDs, columnKeys, aggrProps, aggrType, pageSize, forceRefresh, updateMode, viewType));
    }

    public GFormUserPreferences getUserPreferences() {
        List<GGroupObjectUserPreferences> groupObjectUserPreferencesList = new ArrayList<>();
        List<GGroupObjectUserPreferences> groupObjectGeneralPreferencesList = new ArrayList<>();
        for (GGridController controller : gridControllers.values()) {
            if (controller.isList()) {
                groupObjectUserPreferencesList.add(controller.getUserGridPreferences());
                groupObjectGeneralPreferencesList.add(controller.getGeneralGridPreferences());
            }
        }
        return new GFormUserPreferences(groupObjectGeneralPreferencesList, groupObjectUserPreferencesList);
    }

    public void runGroupReport(Integer groupObjectID) {
        syncDispatch(new GroupReport(groupObjectID, getUserPreferences(), MainFrame.jasperReportsIgnorePageMargins), new SimpleRequestCallback<GroupReportResult>() {
            @Override
            public void onSuccess(GroupReportResult result) {
                GwtClientUtils.openFile(result.filename, false, null);
            }
        });
    }

    public void runTreeGroupReport(int groupObjectID) {
        syncDispatch(new TreeGroupReport(groupObjectID, getUserPreferences()), new SimpleRequestCallback<GroupReportResult>() {
            @Override
            public void onSuccess(GroupReportResult result) {
                GwtClientUtils.openFile(result.filename, false, null);
            }
        });
    }

    public void executeVoidAction() {
        syncResponseDispatch(new VoidFormAction());
    }

    public void saveUserPreferences(GGridUserPreferences userPreferences, boolean forAllUsers, boolean completeOverride, String[] hiddenProps, final AsyncCallback<ServerResponseResult> callback) {
        syncDispatch(new SaveUserPreferencesAction(userPreferences.convertPreferences(), forAllUsers, completeOverride, hiddenProps), new SimpleRequestCallback<ServerResponseResult>() {
            @Override
            public void onSuccess(ServerResponseResult response) {
                for (GAction action : response.actions) {
                    if (action instanceof GMessageAction) {
                        actionDispatcher.execute((GMessageAction) action);
                        callback.onFailure(new Throwable());
                        return;
                    }
                }
                callback.onSuccess(response);
            }

            @Override
            public void onFailure(ExceptionResult exceptionResult) {
                callback.onFailure(exceptionResult.throwable);

                super.onFailure(exceptionResult);
            }
        });
    }

    public void refreshUPHiddenProps(String groupObjectSID, String[] propSids) {
        syncResponseDispatch(new RefreshUPHiddenPropsAction(groupObjectSID, propSids));
    }

    public List<GPropertyDraw> getPropertyDraws() {
        return form.propertyDraws;
    }

    private ActionPanelRenderer asyncView;
    public void setAsyncView(ActionPanelRenderer asyncView) {
        this.asyncView = asyncView;
    }
    public void showAsync(boolean set) {
        if(asyncView != null)
            asyncView.setForceLoading(set);
    }

    public Element getTargetAndPreview(Element element, Event event) {
        // there is a problem with non-bubbling events (there are not many such events, see CellBasedWidgetImplStandard.nonBubblingEvents, so basically focus events):
        // handleNonBubblingEvent just looks for the first event listener
        // but if there are 2 widgets listening to the focus events (for example when in ActionOrPropertyValue there is an embedded form and there is a TableContainer inside)
        // then the lower widget gets both blur events (there 2 of them with different current targets, i.e supposed to be handled by different elements) and the upper widget gets none of them (which leads to the very undesirable behaviour with the "double" finishEditing, etc.)
        // WE DON'T NEED IT NOW SINCE WE RELY ON FOCUSOUT
//        if(DataGrid.checkNonBubblingEvents(event)) { // there is a bubble field, but it does
//            EventTarget currentEventTarget = event.getCurrentEventTarget();
//            if(Element.is(currentEventTarget)) {
//                Element currentTarget = currentEventTarget.cast();
//                 maybe sinked focus events should be checked for the currentTarget
//                if(!currentTarget.equals(element) && DOM.dispatchEvent(event, currentTarget))
//                    return null;
//            }
//        }

        Element target = Element.as(event.getEventTarget());
        if(target == null)
            return null;

        if(DataGrid.FOCUSPREVIEWOUT.equals(event.getType()))
            FocusUtils.setLastBlurredElement(Element.as(event.getEventTarget()));

        formsController.checkEditModeEvents(event);

        //focus() can trigger blur event, blur finishes editing. Editing calls syncDispatch.
        //If isEditing() and busyDialogDisplayer isVisible() then flushCompletedRequests is not executed and syncDispatch is blocked.
        if(dispatcher.getBusyDialogDisplayer().isVisible() && (DataGrid.checkSinkEvents(event) || DataGrid.checkSinkFocusEvents(event)))
            return null;

        if(!DataGrid.checkSinkFocusEvents(event))
            SmartScheduler.getInstance().flush();

        if(!MainFrame.previewClickEvent(target, event))
            return null;

        // see GEditBindingMap.changeOrGroupChange
        boolean isEditing = isEditing();
        if (isEditing || focusedCustom != null) { // we need switchedToAnotherWindow to be filled - see it's usages
            boolean switched = MainFrame.previewSwitchToAnotherWindow(event);
            if(switched && isEditing) // we don't want to stop additing when switching to another window
                return null;
        }

        return target;
    }

    public boolean previewCustomEvent(Event event, Element element) {
        return getTargetAndPreview(element, event) != null;
    }

    // need this because hideForm can be called twice, which will lead to several continueDispatching (and nullpointer, because currentResponse == null)
    private boolean formHidden;
    public void hideForm(GAsyncFormController asyncFormController, GActionDispatcherLookAhead lookAhead, EndReason editFormCloseReason) {
        if(formHidden)
            return;

        for(ContainerForm containerForm : containerForms) {
            containerForm.getForm().closePressed(editFormCloseReason);
        }

        // because when recreating, there should not be tooltip in the caption (it will break the assertion)
        TooltipManager.removeTooltip(getCaptionWidget().first);

        formHidden = true;

        hiddenHandler.onHidden(lookAhead, asyncFormController, editFormCloseReason);
    }

    private boolean formDestroyed;
    public void destroyForm(int closeDelay) {
        if(formDestroyed)
            return;

        // every panel renderer registers in MainFrame's static color-theme listener list, and nothing on a closing
        // form unregisters it - each one would stay reachable for the life of the page: the form panel's, and the
        // per-row ones multiplied by the size of the key window
        panelController.destroy();
        for (GGridPanelController gridPanelController : gridPanelControllers.values())
            gridPanelController.destroy();

        // the same for the table views (grid / pivot / calendar / custom / map and the tree table) - each of them
        // registers in that list too, and each of them holds this form controller
        for (GGridController controller : gridControllers.values())
            controller.destroy();
        for (GTreeGroupController treeController : treeControllers.values())
            treeController.destroy();

        FormDispatchAsync closeDispatcher = dispatcher;
        Scheduler.get().scheduleDeferred(() -> {
            closeDispatcher.executePriority(new Close(closeDelay), new PriorityErrorHandlingCallback<VoidResult>(getPopupOwner()) {
                @Override
                public void onFailure(Throwable caught) { // supressing errors
                }
            });
            closeDispatcher.close();
        });

//        dispatcher = null; // so far there are no null checks (for example, like in desktop-client), so changePageSize can be called after (apparently close will suppress it)
        formDestroyed = true;
    }

    public void initPreferredSize(Widget maxWindow, GSize maxWidth, GSize maxHeight) {
        formLayout.initPreferredSize(maxWindow, maxWidth, maxHeight);
    }

    // this form is shown in a window whose size is measured once when it is shown and then fixed (a -3 main container,
    // see GFormLayout.initPreferredSize) - the only case where a component has to have drawn before that measurement.
    // A docked form is never measured, and a -1 main container stays dynamic and follows its content afterwards
    public boolean isSizeFixedOnShow() {
        return formContainer instanceof ModalForm && formLayout.isFixedSizeOnInit();
    }

    public boolean isWindow() {
        return nvl(getContextContainer(), formContainer) instanceof ModalForm;
    }

    public boolean isDialog() {
        return isDialog;
    }

    public boolean isVisible() {
        return !formHidden;
    }

    public boolean isActive() {
        return isVisible() && formActive;
    }

    public GForm getForm() {
        return form;
    }

    public List<GObject> getObjects() {
        ArrayList<GObject> objects = new ArrayList<>();
        for (GGroupObject groupObject : form.groupObjects) {
            for (GObject object : groupObject.objects) {
                objects.add(object);
            }
        }
        return objects;
    }

    public void setContainerCaption(GContainer container, String caption) {
        container.caption = caption;
        updateCaption(container); // an lsf container's caption is drawn by React: its reader is react-owned (rerouted into its entry), so this runs only for GWT containers
    }

    public void setContainerImage(GContainer container, AppBaseImage image) {
        container.image = image;
        updateImage(container); // an lsf container's image goes to React (its entry); its reader never reaches here
    }

    private Widget getCaptionWidget(GContainer container) {
        return formLayout.getContainerCaption(container);
    }

    public void updateCaption(GContainer container) {
        Widget captionWidget = getCaptionWidget(container);
        if(captionWidget != null)
            BaseImage.updateText(captionWidget, container.caption);
        updateFormsView(container);
    }
    public void updateImage(GContainer container) {
        Widget captionWidget = getCaptionWidget(container);
        if(captionWidget != null)
            BaseImage.updateImage(container.image, captionWidget);
        updateFormsView(container);
    }

    // a form's caption and image ARE its main container's, so this is also where they change for a custom forms view,
    // which reads them from its projection rather than from the tab widget the lines above write into
    private void updateFormsView(GContainer container) {
        if(container.main)
            formsController.requestViewUpdate();
    }

    public void setContainerCustom(GContainer container, String custom) {
        GAbstractContainerView containerView = formLayout.getContainerView(container);
        ((TemplateContainerView)containerView).updateCustom(custom);
    }

    private static final class Change {
        public final long requestIndex;
        public final PValue newValue;
        public final PValue oldValue;

        private Change(long requestIndex, PValue newValue, PValue oldValue) {
            this.requestIndex = requestIndex;
            this.newValue = newValue;
            this.oldValue = oldValue;
        }
    }

    private static class ModifyObject {
        public final long requestIndex;

        public final GObject object;
        public final boolean add;
        public final GGroupObjectValue value;
        public final int position;

        private ModifyObject(long requestIndex, GObject object, boolean add, GGroupObjectValue value, int position) {
            this.requestIndex = requestIndex;
            this.object = object;
            this.add = add;
            this.value = value;
            this.position = position;
        }
    }

    public boolean focusDefaultWidget() {
        FocusUtils.Reason reason = FocusUtils.Reason.SHOW;
        if (formLayout.focusDefaultWidget(reason)) {
            return true;
        }

        return focusNextElement(reason, true);
    }

    public boolean focusNextElement(FocusUtils.Reason reason, boolean forward) {
        return FocusUtils.focusNextElement(formLayout.getElement(), reason, forward);
    }

    private class ServerResponseCallback extends GwtActionDispatcher.ServerResponseCallback {

        public ServerResponseCallback() {
            super(false);
        }

        @Override
        protected GwtActionDispatcher getDispatcher() {
            return actionDispatcher;
        }
    }

    public abstract static class Binding implements BindingExec {

        public GGroupObject groupObject;

        public Binding(GGroupObject groupObject) {
            this.groupObject = groupObject;
        }

        public abstract boolean showing();

        public boolean enabled() {
            return true;
        }
    }

    private final ArrayList<GBindingEvent> bindingEvents = new ArrayList<>();
    private final ArrayList<Binding> bindings = new ArrayList<>();

    public interface BindingCheck {
        boolean check(Event event);
    }
    public interface BindingExec {
        void exec(Event event);
    }
    public ArrayList<Integer> addPropertyBindings(GPropertyDraw propertyDraw, BindingExec bindingExec, Widget widget) {
        ArrayList<Integer> result = new ArrayList<>();
        for(GInputBindingEvent bindingEvent : propertyDraw.bindingEvents) // supplier for optimization
            result.add(addBinding(bindingEvent.inputEvent, bindingEvent.env, propertyDraw, bindingExec, widget, propertyDraw.groupObject));
        return result;
    }

    public void addDynamicBinding(GInputBindingEvent inputBindingEvent, GPropertyDraw property, boolean mouse) {
        for (int i = 0, size = bindingEvents.size(); i < size; i++) {
            GBindingEvent bindingEvent = bindingEvents.get(i);
            if (bindingEvent == null) // a cleared slot
                continue;
            if (property.equals(bindingEvent.property) && mouse == bindingEvent.mouse) {
                if(inputBindingEvent == null)
                    inputBindingEvent = GInputBindingEvent.dumb;
                // the same binding under another event, put back where it was: this runs on ordinary state updates,
                // so spending a slot each time would grow the list - and everything dispatched through it - endlessly
                replaceBinding(i, createBindingEvent(inputBindingEvent.inputEvent, inputBindingEvent.env, property,
                        bindingEvent.widget), bindings.get(i));
            }
        }
    }

    public void removePropertyBindings(ArrayList<Integer> indices) {
        for(int index : indices)
            removeBinding(index);
    }

    public int addRegularFilterBinding(GInputBindingEvent event, BindingExec pressed, Widget component, GGroupObject groupObject) {
        return addBinding(event.inputEvent, event.env, pressed, component, groupObject);
    }
    public int addBinding(GInputEvent event, GBindingEnv env, BindingExec pressed, Widget component, GGroupObject groupObject) {
        return addBinding(event::isEvent, env, null, pressed, component, groupObject);
    }
    public int addBinding(GInputEvent event, GBindingEnv env, GPropertyDraw property, BindingExec pressed, Widget component, GGroupObject groupObject) {
        return addBinding(createBindingEvent(event, env, property, component), createBinding(null, pressed, component, groupObject));
    }
    // the event half of a binding, built the same way whether the binding is being added or replaced
    private GBindingEvent createBindingEvent(GInputEvent event, GBindingEnv env, GPropertyDraw property, Widget component) {
        //event != null - dumb check (see InputBindingEvent.dumb)
        return new GBindingEvent(e -> event != null && event.isEvent(e), env, property, component, event instanceof GMouseInputEvent);
    }
    public int addBinding(BindingCheck event, GBindingEnv env, Supplier<Boolean> enabled, BindingExec pressed, Widget component, GGroupObject groupObject) {
        return addBinding(event, env, null, false, enabled, pressed, component, groupObject);
    }
    public int addBinding(BindingCheck event, GBindingEnv env, GPropertyDraw property, boolean mouse, Supplier<Boolean> enabled, BindingExec pressed, Widget component, GGroupObject groupObject) {
        return addBinding(new GBindingEvent(event, env, property, component, mouse), createBinding(enabled, pressed, component, groupObject));
    }
    private Binding createBinding(Supplier<Boolean> enabled, BindingExec pressed, Widget component, GGroupObject groupObject) {
        return new Binding(groupObject) {
            @Override
            public boolean showing() {
                return component == null || isShowing(component);
            }

            @Override
            public boolean enabled() {
                return enabled != null ? enabled.get() : super.enabled();
            }

            @Override
            public void exec(Event event) {
                pressed.exec(event);
            }
        };
    }
    public int addBinding(GBindingEvent event, Binding action) {
        int index = bindings.size();
        bindingEvents.add(event);
        bindings.add(action);
        return index;
    }
    // a binding put back where it already is, keeping the index that names it. Adding instead would spend a slot on
    // every replacement, and taking the entry out would shift every later one - and the index is the only handle a
    // caller has for taking its own binding back later. Kept to this class: a slot belongs to whoever added it
    private void replaceBinding(int index, GBindingEvent event, Binding action) {
        bindingEvents.set(index, event);
        bindings.set(index, action);
    }
    // cleared in place, for the same reason: an index kept elsewhere must go on naming its own binding. A cleared
    // slot is not reused, so the lists keep the length of everything a form ever bound - two null references per
    // binding that came and went, which is the price of the index staying a handle
    public void removeBinding(int index) {
        replaceBinding(index, null, null);
    }
    // every binding a widget was given, taken back with the widget - a filter condition removed, a value cell rebuilt
    // for another property. The list lives as long as the form, so a binding left behind keeps the widget it names,
    // and through it everything that widget draws
    public void removeBindings(Widget component) {
        for(int i = bindings.size() - 1; i >= 0; i--)
            if(bindingEvents.get(i) != null && bindingEvents.get(i).widget == component)
                removeBinding(i);
    }

    public void addEnterBindings(GBindingMode bindGroup, BiConsumer<Boolean, NativeEvent> selectNextElement, GGroupObject groupObject) {
        addEnterBinding(false, bindGroup, selectNextElement, groupObject);
        addEnterBinding(true, bindGroup, selectNextElement, groupObject);
    }

    private void addEnterBinding(boolean shiftPressed, GBindingMode bindGroup, BiConsumer<Boolean, NativeEvent> selectNextElement, GGroupObject groupObject) {
        addBinding(new GKeyInputEvent(new GKeyStroke(KeyCodes.KEY_ENTER, false, false, shiftPressed)),
                new GBindingEnv(-100, GBindingMode.NO, null, null, bindGroup, GBindingMode.NO, null, null, null),  // bindEditing - NO, because we don't want for example when editing text in grid to catch enter
                event -> selectNextElement.accept(!shiftPressed, event),
                null,
                groupObject);
    }

    private static final String bindingGroupObject = "groupObject";
    public static void setBindingGroupObject(Widget widget, GGroupObject groupObject) {
        widget.getElement().setPropertyObject(bindingGroupObject, groupObject);
    }

    private static GGroupObject getBindingGroupObject(Element elementTarget) {
        while (elementTarget != null) {     // пытаемся найти GroupObject, к которому относится элемент с фокусом
            GGroupObject targetGO = (GGroupObject) elementTarget.getPropertyObject(bindingGroupObject);
            if (targetGO != null)
                return targetGO;
            elementTarget = elementTarget.getParentElement();
        }
        return null;
    }

    public void processBinding(EventHandler handler, boolean preview, boolean isCell, boolean panel) {
        ProcessBinding.processBinding(handler, preview, isCell, panel, bindingEvents, bindings,
                target -> getBindingGroupObject(Element.as(target)),
                this::bindPreview, this::bindDialog, this::bindWindow, this::bindGroup, this::bindEditing, this::bindShowing,
                this::bindPanel, this::bindCell, this::checkCommitEditing);
    }

    public void checkCommitEditing() {
        RequestCellEditor requestCellEditor = getRequestCellEditor();
        if(requestCellEditor != null)
            requestCellEditor.commit(getEditElement(), CommitReason.FORCED_BLURRED);
        else
            if(focusedCustom != null) {
                Element focusedElement = FocusUtils.getFocusedChild(focusedCustom);
                if(focusedElement != null) { // we do the fake blur to call onBlur, because custom render
                    forcedBlurCustom = true;
                    try {
                        // move focus "outside" the element (to the first with tabIndex ???, that what blur actually does) and then return back to the element ???
                        FocusUtils.triggerFocus(reason -> FocusUtils.focusOut(focusedCustom, reason), focusedElement);
                    } finally {
                        forcedBlurCustom = false;
                    }
                }
            }

    }

    private boolean bindPreview(GBindingEnv binding, boolean preview) {
        switch (binding.bindPreview) {
            case AUTO:
            case ONLY:
                return preview;
            case NO:
                return !preview;
            case ALL: // actually makes no since if previewed, than will be consumed so equivalent to only
                return true;
            default:
                throw new UnsupportedOperationException("Unsupported bindingMode " + binding.bindPreview);
        }
    }

    private boolean bindDialog(GBindingEnv binding) {
        switch (binding.bindDialog) {
            case AUTO:
            case ALL:
                return true;
            case ONLY:
                return isDialog();
            case NO:
                return !isDialog();
            case INPUT:
            default:
                throw new UnsupportedOperationException("Unsupported bindingMode " + binding.bindDialog);
        }
    }

    private boolean bindWindow(GBindingEnv binding) {
        switch (binding.bindWindow) {
            case AUTO:
            case ALL:
                return true;
            case ONLY:
                return isWindow();
            case NO:
                return !isWindow();
            case INPUT:
            default:
                throw new UnsupportedOperationException("Unsupported bindingMode " + binding.bindWindow);
        }
    }

    private boolean bindGroup(GBindingEnv bindingEvent, GGroupObject groupObject, boolean equalGroup) {
        switch (bindingEvent.bindGroup) {
            case AUTO:
            case ALL:
                return true;
            case ONLY:
                return equalGroup;
            case NO:
                return !equalGroup;
            case INPUT:
                return groupObject != null && form.inputGroupObjects.contains(groupObject);
            default:
                throw new UnsupportedOperationException("Unsupported bindingMode");
        }
    }

    private boolean bindEditing(GBindingEnv binding, Event event) {
        Element editing = getEditingElement();
        switch (binding.bindEditing) {
            case AUTO:
                return !(editing != null && editing.isOrHasChild(Element.as(event.getEventTarget())));
            case ALL:
                return true;
            case ONLY:
                return editing != null;
            case NO:
                return editing == null;
            case INPUT:
            default:
                throw new UnsupportedOperationException("Unsupported bindingMode " + binding.bindEditing);
        }
    }

    private boolean bindShowing(GBindingEnv binding, boolean showing) {
        switch (binding.bindShowing) {
            case ALL:
                return true;
            case AUTO:
            case ONLY:
                return showing;
            case NO:
                return !showing;
            case INPUT:
            default:
                throw new UnsupportedOperationException("Unsupported bindingMode " + binding.bindShowing);
        }
    }

    private boolean bindPanel(GBindingEnv binding, boolean isMouse, boolean panel) {
        switch (binding.bindPanel) {
            case ALL:
                return true;
            case AUTO:
                return !isMouse || !panel;
            case ONLY:
                return panel;
            case NO:
                return !panel;
            case INPUT:
            default:
                throw new UnsupportedOperationException("Unsupported bindingMode " + binding.bindPanel);
        }
    }

    private boolean bindCell(GBindingEnv binding, boolean isMouse, boolean isCell) {
        switch (binding.bindCell) {
            case ALL:
                return true;
            case AUTO:
                return !isMouse || isCell;
            case ONLY:
                return isCell;
            case NO:
                return !isCell;
            case INPUT:
            default:
                throw new UnsupportedOperationException("Unsupported bindingMode " + binding.bindCell);
        }
    }

    private CellEditor cellEditor;
    private Element focusedCustom;
    private boolean forcedBlurCustom;

    public Element getEditElement() {
        return editContext.getEditElement();
    }

    private EditContext editContext;
    // the CUSTOM branch of the same state: what a view that draws its own content said the user is editing in.
    // The form cannot open an edit for such content - an edit is a property's, with a value to commit or to cancel,
    // and only one is live at a time - but it can be TOLD, and the editing scope below reads the two branches the
    // same way. Set and cleared by controller.startEditing / stopEditing
    private Element customEditElement;
    private long editRequestIndex = -1;

    private BiConsumer<GUserInputResult, CommitReason> editBeforeCommit;
    private BiConsumer<GUserInputResult, CommitReason> editAfterCommit;
    private Consumer<CancelReason> editCancel;

    private Element focusedElement;
    private Object forceSetFocus;

    public boolean isEditing() {
        return editContext != null;
    }

    public void startCustomEditing(Element element) {
        customEditElement = element;
    }
    public void stopCustomEditing(Element element) {
        if (customEditElement == element) // a view stops its own editing, never somebody else's
            customEditElement = null;
    }
    public boolean isCustomEditing(Element element) {
        return customEditElement == element;
    }

    // the element the user is editing in, whichever branch it is. The form's own editor wins while it is open, which
    // is all "one live input" needs here: a view's word is simply not consulted meanwhile, and is still there when
    // the form's edit ends - which matters, since a view can declare on a wrapper that holds an `lsf = TRUE` child
    // and the form opens its editor inside that. Stopping is the view's to say; a view that never says it holds its
    // form's ENTER and ESCAPE, and that is the view's bug to see
    private Element getEditingElement() {
        assert customEditElement == null || Document.get().getBody().isOrHasChild(customEditElement);
        return editContext != null ? editContext.getEditElement() : customEditElement;
    }


    public long getEditingRequestIndex() {
        return editRequestIndex;
    }

    private String editAsyncValuesSID;
    private boolean editAsyncUsePessimistic; // optimimization
    // shouldn't be zeroed when editing ends, since we assume that there is only one live input on the form
    private int editAsyncIndex;
    private int editLastReceivedAsyncIndex;
    // we don't want to proceed results if "later" request results where proceeded
    private AsyncCallback<GAsyncResult> checkLast(int index, AsyncCallback<GAsyncResult> callback) {
        return new AsyncCallback<GAsyncResult>() {
            @Override
            public void onFailure(Throwable caught) {
                if(index >= editLastReceivedAsyncIndex) {
                    editLastReceivedAsyncIndex = index;
                    callback.onFailure(caught);
                }
            }

            @Override
            public void onSuccess(GAsyncResult result) {
                if(index >= editLastReceivedAsyncIndex) {
                    editLastReceivedAsyncIndex = index;
                    if(!result.moreRequests && index < editAsyncIndex - 1)
                        result = new GAsyncResult(result.asyncs, result.needMoreSymbols, true);
                    callback.onSuccess(result);
                }
            }
        };
    }

    // synchronous call (with request indices, etc.)
    private void getPessimisticValues(int propertyID, GGroupObjectValue columnKey, String actionSID, String value, int index, AsyncCallback<GAsyncResult> callback) {
        asyncDispatch(new GetAsyncValues(propertyID, columnKey, actionSID, value, index), new CustomCallback<ListResult>() {
            @Override
            public void onFailure(ExceptionResult exceptionResult) {
                callback.onFailure(exceptionResult.throwable);
            }

            @Override
            public void onSuccess(ListResult result) {
                callback.onSuccess(convertAsyncResult(result));
            }
        });
    }

    public void getAsyncValues(String value, AsyncCallback<GAsyncResult> callback) {
        if(editContext != null) // just in case
            getAsyncValues(value, editContext, editAsyncValuesSID, callback, 0);
    }

    public void getAsyncValues(String value, EditContext editContext, String actionSID, AsyncCallback<GAsyncResult> callback, int increaseValuesNeededCount) {
        getAsyncValues(value, editContext.getProperty(), editContext.getColumnKey(), actionSID, callback, increaseValuesNeededCount);
    }

    public static class GAsyncResult {
        public final ArrayList<GAsync> asyncs;
        public final boolean needMoreSymbols;
        public final boolean moreRequests;

        public GAsyncResult(ArrayList<GAsync> asyncs, boolean needMoreSymbols, boolean moreRequests) {
            this.asyncs = asyncs;
            this.needMoreSymbols = needMoreSymbols;
            this.moreRequests = moreRequests;
        }
    }
    public void getAsyncValues(String value, GPropertyDraw property, GGroupObjectValue columnKey, String actionSID, AsyncCallback<GAsyncResult> callback, int increaseValuesNeededCount) {
        int editIndex = editAsyncIndex++;
        AsyncCallback<GAsyncResult> fCallback = checkLast(editIndex, callback);

        GGroupObjectValue currentKey = getFullCurrentKey(property, columnKey);

        Runnable runPessimistic = () -> getPessimisticValues(property.ID, currentKey, actionSID, value, editIndex, fCallback);
        if (!editAsyncUsePessimistic)
            dispatcher.executePriority(new GetPriorityAsyncValues(property.ID, currentKey, actionSID, value, editIndex, increaseValuesNeededCount), new PriorityAsyncCallback<ListResult>() {
                @Override
                public void onFailure(Throwable caught) {
                    fCallback.onFailure(caught);
                }

                @Override
                public void onSuccess(ListResult result) {
                    if (result.value == null) { // optimistic request failed, running pessimistic one, with request indices, etc.
                        editAsyncUsePessimistic = true;
                        runPessimistic.run();
                    } else {
                        GAsyncResult asyncResult = convertAsyncResult(result);
                        if(asyncResult.moreRequests)
                            runPessimistic.run();
                        fCallback.onSuccess(asyncResult);
                    }
                }
            });
        else
            runPessimistic.run();
    }

    // single source of truth for the async-value callback: converts a GAsyncResult to the JS shape custom views consume
    // ({data:[{displayString,rawString,objects}], more}). Shared by the form/REACT controller, the grid custom view
    // (GSimpleStateTableView), and the custom cell renderer (CustomCellRenderer) — all route their getValues through here.
    public static AsyncCallback<GAsyncResult> getJSCallback(JavaScriptObject successCallBack, JavaScriptObject failureCallBack) {
        return new AsyncCallback<GAsyncResult>() {
            @Override
            public void onFailure(Throwable caught) {
                if (failureCallBack != null)
                    GwtClientUtils.call(failureCallBack);
            }

            @Override
            public void onSuccess(GAsyncResult result) {
                assert !result.needMoreSymbols;
                ArrayList<GAsync> asyncs = result.asyncs;
                if (asyncs == null) {
                    if (!result.moreRequests && failureCallBack != null)
                        GwtClientUtils.call(failureCallBack);
                    return;
                }

                GwtClientUtils.call(successCallBack, convertToJSObject(result));
            }
        };
    }

    private static JavaScriptObject convertToJSObject(GAsyncResult result) {
        JavaScriptObject[] results = new JavaScriptObject[result.asyncs.size()];
        for (int i = 0; i < result.asyncs.size(); i++) {
            JavaScriptObject object = GwtClientUtils.newObject();
            GAsync suggestion = result.asyncs.get(i);
            GwtClientUtils.setField(object, "displayString", GStateTableView.fromString(PValue.getStringValue(suggestion.getDisplayValue())));
            GwtClientUtils.setField(object, "rawString", GStateTableView.fromString(PValue.getStringValue(suggestion.getRawValue())));
            GwtClientUtils.setField(object, "objects", convertToJSKey(suggestion.key));
            results[i] = object;
        }
        JavaScriptObject data = GwtClientUtils.newObject();
        GwtClientUtils.setField(data, "data", GStateTableView.fromObject(results));
        GwtClientUtils.setField(data, "more", GStateTableView.fromBoolean(result.moreRequests));
        return data;
    }

    private static JavaScriptObject convertToJSKey(Serializable key) {
        if (key instanceof GGroupObjectValue)
            return GStateTableView.fromObject(key);

        return GwtClientUtils.jsonParse((String) key);
    }

    private static GAsyncResult convertAsyncResult(ListResult result) {
        boolean needMoreSymbols = false;
        boolean moreResults = false;
        ArrayList<GAsync> values = result.value;
        if(values.size() > 0) {
            GAsync lastResult = values.get(values.size() - 1);
            if(lastResult.equals(GAsync.RECHECK)) {
                values = removeLast(values);

                moreResults = true;
            } else if(values.size() == 1 && (lastResult.equals(GAsync.CANCELED) || lastResult.equals(GAsync.NEEDMORE))) { // ignoring CANCELED results
                needMoreSymbols = lastResult.equals(GAsync.NEEDMORE);
                values = needMoreSymbols ? new ArrayList<>() : null;
            }
        }
        return new GAsyncResult(values, needMoreSymbols, moreResults);
    }

    public void edit(GType type, EventHandler handler, boolean hasOldValue, PValue setOldValue, GInputList inputList, GInputListAction[] inputListActions, BiConsumer<GUserInputResult, Consumer<Long>> afterCommit, Consumer<CancelReason> cancel, EditContext editContext, String actionSID, String customChangeFunction) {
        edit(type, handler, hasOldValue, setOldValue, false, inputList, inputListActions, afterCommit, cancel, editContext, actionSID, customChangeFunction);
    }

    public void edit(GType type, EventHandler handler, boolean hasOldValue, PValue setOldValue, boolean multipleInput, GInputList inputList, GInputListAction[] inputListActions, BiConsumer<GUserInputResult, Consumer<Long>> afterCommit, Consumer<CancelReason> cancel, EditContext editContext, String actionSID, String customChangeFunction) {
        assert type != null;
        lsfusion.gwt.client.base.Result<ChangedRenderValue> changedRenderValue = new lsfusion.gwt.client.base.Result<>();
        edit(type, handler, hasOldValue, setOldValue, multipleInput, inputList, inputListActions, // actually it's assumed that actionAsyncs is used only here, in all subsequent calls it should not be referenced
                (inputResult, commitReason) -> changedRenderValue.set(setLocalValue(editContext, type, inputResult.getPValue(), null)),
                (inputResult, commitReason) -> afterCommit.accept(inputResult, requestIndex -> setRemoteValue(editContext, changedRenderValue.result, requestIndex)),
                cancel, editContext, actionSID, customChangeFunction);
    }

    public void edit(GType type, EventHandler handler, boolean hasOldValue, PValue oldValue, GInputList inputList, GInputListAction[] inputListActions, BiConsumer<GUserInputResult, CommitReason> beforeCommit, BiConsumer<GUserInputResult, CommitReason> afterCommit,
                     Consumer<CancelReason> cancel, EditContext editContext, String editAsyncValuesSID, String customChangeFunction) {
        edit(type, handler, hasOldValue, oldValue, false, inputList, inputListActions, beforeCommit, afterCommit, cancel, editContext, editAsyncValuesSID, customChangeFunction);
    }

    public void edit(GType type, EventHandler handler, boolean hasOldValue, PValue oldValue, boolean multipleInput, GInputList inputList, GInputListAction[] inputListActions, BiConsumer<GUserInputResult, CommitReason> beforeCommit, BiConsumer<GUserInputResult, CommitReason> afterCommit,
                     Consumer<CancelReason> cancel, EditContext editContext, String editAsyncValuesSID, String customChangeFunction) {
        GPropertyDraw property = editContext.getProperty();

        RequestValueCellEditor cellEditor;
        boolean hasCustomEditor = customChangeFunction != null && !customChangeFunction.equals("DEFAULT");
        if (hasCustomEditor) // see LsfLogics.g propertyCustomView rule
            cellEditor = CustomReplaceCellEditor.create(this, property, type, customChangeFunction);
        else {
            if(property.echoSymbols) // disabling dropdown if echo
                inputList = null;

            cellEditor = type.createCellEditor(this, property, inputList, inputListActions, editContext, multipleInput);
        }

        if (cellEditor != null) {
            boolean canUseChangeValueForRendering = editContext.canUseChangeValueForRendering(type);
            if(canUseChangeValueForRendering)
                cellEditor.setCancelTheSameValueOnBlur(editContext.getValue());

            if(!hasOldValue) { // property.baseType.equals(type) actually there should be something like compatible, but there is no such method for now, so we'll do this check in editors
                assert oldValue == null;
                oldValue = editContext.getValue();
                if(oldValue != null && !canUseChangeValueForRendering && !hasCustomEditor) {
                    try {
                        oldValue = type.parseString(PValue.getStringValue(oldValue), editContext.getRenderContext().getPattern());
                    } catch (ParseException e) {
                        oldValue = null;
                    }
                }
            }

            edit(cellEditor, handler, oldValue, beforeCommit, afterCommit, cancel, editContext, editAsyncValuesSID, -1);
        } else
            cancel.accept(CancelReason.FORCED);
    }

    public void edit(CellEditor cellEditor, EventHandler handler, PValue oldValue, BiConsumer<GUserInputResult, CommitReason> beforeCommit, BiConsumer<GUserInputResult, CommitReason> afterCommit,
                     Consumer<CancelReason> cancel, EditContext editContext, String editAsyncValuesSID, long editRequestIndex) {
        assert this.editContext == null;
        editBeforeCommit = beforeCommit;
        editAfterCommit = afterCommit;
        editCancel = cancel;

        this.editAsyncValuesSID = editAsyncValuesSID;

        this.editContext = editContext;
        this.editRequestIndex = editRequestIndex;  // we need to force dispatch responses till this index because otherwise we won't

        focusedElement = FocusUtils.getFocusedElement();

        Element element = getEditElement();
        FocusUtils.startFocusTransaction(element);

        editContext.startEditing();

        boolean notFocusable = !editContext.isFocusable();
        if(notFocusable) // assert that otherwise it's already has focus
            forceSetFocus = editContext.forceSetFocus();

        RenderContext renderContext = editContext.getRenderContext();
        if (cellEditor instanceof ReplaceCellEditor && ((ReplaceCellEditor) cellEditor).needReplace(element, renderContext)) {
            GPropertyDraw property = editContext.getProperty();
            CellRenderer cellRenderer = property.getCellRenderer(renderContext.getRendererType());

            // we need to do it before clearRender to have actual sizes + we need to remove paddings since we're setting width for wrapped component
            Integer renderedWidth = null;
            if(property.hasAutoWidth())
                renderedWidth = GwtClientUtils.getWidth(element);
            Integer renderedHeight = null;
//            if(property.hasAutoHeight()) // now we don't need to set autosize height, since input works without it, and for textarea special auto size library is used
//                renderedHeight = GwtClientUtils.getHeight(element);

            cellRenderer.clearRender(element, renderContext); // dropping previous render

            ((ReplaceCellEditor)cellEditor).render(element, renderContext, oldValue, renderedWidth, renderedHeight); // rendering new one, filling inputElement
        }

        this.cellEditor = cellEditor; // not sure if it should before or after startEditing, but definitely after removeAllChildren, since it leads to blur for example
        cellEditor.start(handler, element, renderContext, notFocusable, oldValue); //need to be after this.cellEditor = cellEditor, because there is commitEditing in start in LogicalCellEditor

        FocusUtils.endFocusTransaction();
    }

    // only request cell editor can be long-living
    protected RequestCellEditor getRequestCellEditor() {
        return (RequestCellEditor)cellEditor;
    }

    @Override
    public void commitEditing(GUserInputResult result, CommitReason commitReason) {
        editBeforeCommit.accept(result, commitReason);
        editBeforeCommit = null;

        finishEditing(commitReason.isBlurred(), false);

        BiConsumer<GUserInputResult, CommitReason> editAfterCommit = this.editAfterCommit;
        this.editAfterCommit = null; // it seems this is needed because after commit another editing can be started
        editAfterCommit.accept(result, commitReason);

        onEditingFinished();
    }

    @Override
    public boolean isThisCellEditing(CellEditor cellEditor) {
        return cellEditor == this.cellEditor;
    }

    // a renderer about to be dropped must not stay the edit context: the editor would later be committed or cancelled
    // against a detached element, and the value would be written for a row that is no longer there
    public void cancelEditing(EditContext context, CancelReason cancelReason) {
        if (editContext == context)
            cancelEditing(cancelReason);
    }

    @Override
    public void cancelEditing(CancelReason cancelReason) {
        finishEditing(cancelReason.isBlurred(), true);

        editCancel.accept(cancelReason);
        editCancel = null;

        onEditingFinished();
    }

    // we have to do it after "after commit" is called because there can be for example applyRemoteChanges, and pendingChangeProperty not yet called (so there will be previous value set)
    private void onEditingFinished() {
        dispatcher.onEditingFinished();
    }

    private void finishEditing(boolean blurred, boolean cancel) {
        Element renderElement = getEditElement();
        FocusUtils.startFocusTransaction(renderElement);

        CellEditor cellEditor = this.cellEditor;
        this.cellEditor = null;

        EditContext editContext = this.editContext;
//        this.editRequestIndex = -1; //it doesn't matter since it is not used when editContext / cellEditor is null
        this.editAsyncUsePessimistic = false;
        this.editAsyncValuesSID = null;

        if(cellEditor instanceof RequestCellEditor)
            ((RequestCellEditor)cellEditor).stop(renderElement, cancel, blurred);

        boolean isReplace = cellEditor instanceof ReplaceCellEditor && ((ReplaceCellEditor) cellEditor).needReplace(renderElement, editContext.getRenderContext());
        if(isReplace)
            ((ReplaceCellEditor) cellEditor).clearRender(renderElement, editContext.getRenderContext(), cancel);

        //getAsyncValues need editContext, so it must be after clearRenderer
        this.editContext = null;

        if(isReplace)
            render(editContext);

        update(editContext);

        editContext.stopEditing();

        if(forceSetFocus != null) {
            editContext.restoreSetFocus(forceSetFocus);
            forceSetFocus = null;
        }

        if(blurred) { // when editing is commited (thus editing element is removed), set last blurred element to main widget to keep focus there
            if(editContext.isSetLastBlurred()) {
                Element focusElement = editContext.getFocusElement();
                if(focusElement != null)
                    FocusUtils.setLastBlurredElement(focusElement);
            }
        } else {
            if (focusedElement != null) {
                FocusUtils.focus(focusedElement, FocusUtils.Reason.RESTOREFOCUS);
                focusedElement = null;
            }
        }

        FocusUtils.endFocusTransaction();
    }

    public void render(EditContext editContext) {
        render(editContext.getProperty(), editContext.getEditElement(), editContext.getRenderContext());
    }
    public void render(GPropertyDraw property, Element element, RenderContext renderContext) {
        if(isEdited(element)) {
            assert false;
            return;
        }

        property.getCellRenderer(renderContext.getRendererType()).render(element, renderContext);
    }

    // setting loading
    public void setLoading(ExecuteEditContext editContext, long requestIndex) {
        // actually this part we can do before sending request
        editContext.setLoading();

        // RERENDER IF NEEDED : we have the previous state
        // but in that case we need to move GridPropertyColumn.renderDom logics to GFormController.render here (or have some callbacks)
        update(editContext);

        // the loading mark must be keyed exactly as the change it waits for, so it's the same key the request was sent with
        pendingLoadingProperty(editContext.getProperty(), editContext.getFullKey(), requestIndex);
    }

    private void pendingLoadingProperty(GPropertyDraw property, GGroupObjectValue fullKey, long requestIndex) {
        putToDoubleNativeMap(pendingLoadingPropertyRequests, property, fullKey, requestIndex);
    }

    public void setLoading(GFilterConditionView filterView, long requestIndex) {

        // RERENDER IF NEEDED : we have the previous state

        filterView.updateLoading(true);

        pendingLoadingFilterRequests.put(filterView, requestIndex);
    }
    // "external" update - paste + server update edit value
    public void setValue(EditContext editContext, PValue value) {
        if(setLocalValue(editContext, value, null) != null)
            update(editContext);
    }
    public void update(EditContext editContext) {
        update(editContext.getProperty(), editContext.getEditElement(), editContext.getUpdateContext());
    }
    public void update(GPropertyDraw property, Element element, UpdateContext updateContext) {
        if(isEdited(element))
            return;

        property.getCellRenderer(updateContext.getRendererType()).update(element, updateContext);
    }

    public boolean isEdited(Element element) {
        return editContext != null && getEditElement() == element;
    }


    // we want to set the colors for the td (not the sized element), since we usually want the background color to include td paddings
    // it's not pretty, but other solutions also are not (for example pulling td all over the stack, or including it in the update contexts, however later this might change)
    private static Element getColorElement(Element element) {
        Element parentElement = element.getParentElement();
        if(isTDorTH(parentElement))
            return parentElement;
        return element;
    }

    public static void updateFontColors(Element element, GFont font, String backgroundColor, String foregroundColor) {
        setFont(element, font);
        setBackgroundColor(element, backgroundColor);
        setForegroundColor(element, foregroundColor);
    }

    public static void clearColors(Element element) {
        setBackgroundColor(element, null);
        setForegroundColor(element, null);
    }

    private static void setBackgroundColor(Element element, String color) {
        element = getColorElement(element);

        if(color != null) {
            GwtClientUtils.addClassName(element, "cell-with-background");
        } else {
            GwtClientUtils.removeClassName(element, "cell-with-background");
        }
        setCellBackgroundColor(element, color);
    }

    private static native void setCellBackgroundColor(Element element, String background) /*-{
        // if pass null as a value, it will be undefined in browser.
        // in this case, the sticky column will become transparent in TREE view
        if (background != null) {
            element.style.setProperty("--bs-table-bg", background);
            element.style.setProperty("--bs-body-bg", background);
        } else {
            element.style.removeProperty("--bs-table-bg");
            element.style.removeProperty("--bs-body-bg");
        }
    }-*/;

    private static void setForegroundColor(Element element, String color) {
        if(color != null)
            GwtClientUtils.addClassName(element, "cell-with-foreground");
        else
            GwtClientUtils.removeClassName(element, "cell-with-foreground");

        setCellForegroundColor(element, color);
    }

    private static native void setCellForegroundColor(Element element, String color) /*-{
        // if pass null as a value, it will be undefined in browser
        if (color != null)
            element.style.setProperty("--foreground-color", color);
        else
            element.style.removeProperty("--foreground-color");
    }-*/;

    public static void setFont(Element element, GFont font) {
        if(font != null) {
            GwtClientUtils.addClassName(element, "cell-with-custom-font");
            setCellFont(element, font.family, font.size, font.italic, font.bold);
        } else {
            GwtClientUtils.removeClassName(element, "cell-with-custom-font");
            clearCellFont(element);
        }
    }

    public static void clearFont(Element element) {
        setFont(element, null);
    }

    private static native void setCellFont(Element element, String family, int size, boolean italic, boolean bold)/*-{
        if (family != null)
            element.style.setProperty("--custom-font-family", family);
        if (size > 0)
            element.style.setProperty("--custom-font-size", size + "px");
        element.style.setProperty("--custom-font-style", italic ? "italic" : "normal");
        element.style.setProperty("--custom-font-weight", bold ? "bold" : "normal");
    }-*/;

    private static native void clearCellFont(Element element)/*-{
        element.style.removeProperty("--custom-font-family");
        element.style.removeProperty("--custom-font-size");
        element.style.removeProperty("--custom-font-style");
        element.style.removeProperty("--custom-font-weight");
    }-*/;

    public static GFont getFont(GPropertyDraw property, RenderContext renderContext) {
        return property.font != null ? property.font : renderContext.getFont();
    }

    // in the inputs the native select all is scoped to the input itself, so there it shouldn't be overridden
    private static boolean isTextTarget(EventHandler handler) {
        EventTarget target = handler.event.getEventTarget();
        return Element.is(target) && GwtClientUtils.isInput(Element.as(target));
    }

    public void onPropertyBrowserEvent(EventHandler handler, Element renderElement, boolean isCell, Element focusElement, Consumer<EventHandler> onOuterEditBefore,
                                       Consumer<EventHandler> onEdit, Consumer<EventHandler> onOuterEditAfter, Consumer<EventHandler> onCut,
                                       Consumer<EventHandler> onPaste, boolean panel, boolean customRenderer, boolean focusable) {
        RequestCellEditor requestCellEditor = getRequestCellEditor();
        boolean isPropertyEditing = requestCellEditor != null && getEditElement() == renderElement;
        if(isPropertyEditing)
            requestCellEditor.onBrowserEvent(renderElement, handler);

//        used in dragging selection
//        if(DataGrid.getBrowserTooltipMouseEvents().contains(handler.event.getType())) // just not to have problems in debugger
//            return;

        if(handler.consumed)
            return;

        if(GMouseStroke.isChangeEvent(handler.event) && focusElement != null &&
                FocusUtils.getFocusedChild(focusElement) == null) { // need to check that focus is not on the grid, otherwise when editing for example embedded form, any click will cause moving focus to grid, i.e. stopping the editing
            if(focusable)
                FocusUtils.focus(focusElement, FocusUtils.Reason.MOUSECHANGE, handler.event); // it should be done on CLICK, but also on MOUSEDOWN, since we want to focus even if mousedown is later consumed
            else
                checkCommitEditing();
        }

        /*if(!previewBusyDialogDisplayerSinkEvents(handler.event)) {
            return;
        }*/
        if (renderElement != null)
            checkChangeEvent(handler, renderElement);

        if(handler.consumed)
            return;

        checkMouseKeyEvent(handler, true, isCell, panel, customRenderer);

        if(handler.consumed)
            return;

        onOuterEditBefore.accept(handler);

        if(handler.consumed)
            return;

        if (!isPropertyEditing) { // if editor did not consume event, we don't want it to be handled by "renderer" since it doesn't exist
            onEdit.accept(handler);
            if(handler.consumed)
                return;

            if (GKeyStroke.isCopyToClipboardEvent(handler.event)) {
                onCut.accept(handler);
            } else if (GKeyStroke.isPasteFromClipboardEvent(handler.event)) {
                onPaste.accept(handler);
            } else if (panel && renderElement != null && GKeyStroke.isSelectAllKeyEvent(handler.event) && !isTextTarget(handler)) {
                handler.consume();
                CopyPasteUtils.selectElement(renderElement);
            }
        }

        if(handler.consumed)
            return;

        onOuterEditAfter.accept(handler);

        if(handler.consumed)
            return;

        // if we consume mouse down we disable "text selection" feature
//        if(GMouseStroke.isDownEvent(handler.event)) // we want to cancel focusing (to avoid blinking if change event IS CLICK) + native selection odd behaviour (when some events are consumed, and some - not)
//            handler.consume(false, true); // but we want to propagate event upper (to GFormController to proceed bindings)

        checkMouseKeyEvent(handler, false, isCell, panel, customRenderer);
    }

    public void checkChangeEvent(EventHandler handler, Element renderElement) {
        InputElement inputElement;
        // there can be the CHANGE event that wasn't started with regular edit mechanism (for example if !isChangeOnSingleClick and user clicks on the input)
        // in this case we have to "cancel" the change
        // probably the same should be done for SimpleTextBasedRenderer but there are no such scenarios for now
        if(GKeyStroke.isChangeEvent(handler.event) &&
                (inputElement = InputBasedCellRenderer.getInputEventTarget(renderElement, handler.event)) != null &&
                InputBasedCellRenderer.getInputElementType(inputElement).isLogical()) {
            LogicalCellRenderer.cancelChecked(inputElement);
            handler.consume();
        }
    }

    public void resetWindowsLayout() {
        formsController.resetWindowsLayout();
    }
}
