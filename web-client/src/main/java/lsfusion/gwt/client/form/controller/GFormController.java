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
import lsfusion.gwt.client.action.GFilterAction;
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
import lsfusion.gwt.client.form.filter.user.controller.GFilterController;
import lsfusion.gwt.client.form.filter.user.view.GFilterConditionView;
import lsfusion.gwt.client.form.object.*;
import lsfusion.gwt.client.form.object.panel.controller.GPanelController;
import lsfusion.gwt.client.form.object.table.controller.GAbstractTableController;
import lsfusion.gwt.client.form.object.table.controller.GFormGroupController;
import lsfusion.gwt.client.form.object.table.controller.GFormPropertyController;
import lsfusion.gwt.client.form.object.table.controller.GPropertyController;
import lsfusion.gwt.client.form.object.table.grid.controller.GGridController;
import lsfusion.gwt.client.form.object.table.grid.user.design.GColumnUserPreferences;
import lsfusion.gwt.client.form.object.table.grid.user.design.GFormUserPreferences;
import lsfusion.gwt.client.form.object.table.grid.user.design.GGridUserPreferences;
import lsfusion.gwt.client.form.object.table.grid.user.design.GGroupObjectUserPreferences;
import lsfusion.gwt.client.form.object.table.grid.view.GGridTable;
import lsfusion.gwt.client.form.object.panel.controller.GRowPanelController;
import lsfusion.gwt.client.form.object.table.grid.view.GListViewType;
import lsfusion.gwt.client.form.object.table.grid.view.GSimpleStateTableView;
import lsfusion.gwt.client.form.object.table.grid.view.GStateTableView;
import lsfusion.gwt.client.form.object.table.tree.GTreeGroup;
import lsfusion.gwt.client.form.object.table.tree.controller.GTreeGroupController;
import lsfusion.gwt.client.form.object.table.view.GridDataRecord;
import lsfusion.gwt.client.form.order.user.GOrder;
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
import java.util.Collections;
import java.text.ParseException;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

import lsfusion.gwt.client.base.jsni.NativeStringMap;
import static lsfusion.gwt.client.base.GwtClientUtils.*;
import static lsfusion.gwt.client.base.GwtSharedUtils.putToDoubleNativeMap;
import static lsfusion.gwt.client.base.GwtSharedUtils.removeFromDoubleMap;
import static lsfusion.gwt.client.form.property.cell.GEditBindingMap.isChangeEvent;

public class GFormController implements EditManager {

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

    private lsfusion.gwt.client.form.design.view.GReactFormData reactData; // CUSTOM REACT: projected @lsfusion/core data

    private final boolean isDialog;

    private Event editEvent;

    private final NativeSIDMap<GGroupObject, ArrayList<GGroupObjectValue>> currentGridObjects = new NativeSIDMap<>();

    public NativeSIDMap<GGroupObject, ArrayList<GGroupObjectValue>> getCurrentGridObjects() {
        return currentGridObjects;
    }

    private final NativeSIDMap<GGroupObject, ArrayList<GPropertyFilter>> currentFilters = new NativeSIDMap<>();

    private final LinkedHashMap<GGroupObject, GGridController> controllers = new LinkedHashMap<>();
    private final LinkedHashMap<GTreeGroup, GTreeGroupController> treeControllers = new LinkedHashMap<>();
    private final LinkedHashMap<GGroupObject, GReactController> reactControllers = new LinkedHashMap<>();
    private final LinkedHashMap<GGroupObject, GRowPanelController> rowPanelControllers = new LinkedHashMap<>(); // groups with LSF properties
    public GPanelController panelController;

    private final NativeSIDMap<GGroupObject, ArrayList<Widget>> filterViews = new NativeSIDMap<>();

    private final ArrayList<ModifyObject> pendingModifyObjectRequests = new ArrayList<>();
    private final NativeSIDMap<GGroupObject, Long> pendingChangeCurrentObjectsRequests = new NativeSIDMap<>();
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

        formLayout = new GFormLayout(this, form.mainContainer);
        if (formLayout.hasReactContainers()) // CUSTOM REACT: keep a projection only for forms that actually have a React container
            reactData = new lsfusion.gwt.client.form.design.view.GReactFormData(form, this);
        if (form.sID != null)
            formLayout.getElement().setAttribute("lsfusion-form", form.sID);

        this.editEvent = editEvent;

        initializeParams(); // has to be done before initializeControllers (since adding component uses getSize)

        initializeControllers();

        initializeRegularFilters();

        initializeDefaultOrders();

        if (form.initialFormChanges != null) {
            applyRemoteChanges(form.initialFormChanges);
            form.initialFormChanges = null;
        } else
            getRemoteChanges();

        initializeUserOrders();

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
    //   controller.exec / eval / evalAction / change                  the form-level escape hatch (GController)
    //   controller.properties.change([{property, object, value}])     the one batch — it belongs to no single member
    // A row is named the way a view has it — a data row, its `objects` handle, or the key the projection gave it (the
    // group is known here, so the key can be looked up). Mutations go through the SAME classic interactive path as a
    // normal edit (optimistic reconciliation + async exec); there is no return value, state flows back as the projection.
    // the FORM's controller: what the classic surfaces are handed - a custom object group, a custom cell editor,
    // an INTERNAL CLIENT action - and what `controller.form` is. Just the form-level verbs (exec / eval /
    // evalAction / change), which take lsf names and code. The rest of this surface - the members and the batch
    // that is a shortcut for them - belongs to a PROJECTION and exists only on one: a member says that
    // `data.<group>.<property>` is there, and an object with no data beside it has nothing to say that about.
    public final JavaScriptObject controller = initController(null);
    // ... so the members live HERE, one controller per react container, beside its own `props.data` and naming
    // exactly what that data carries. A view that cannot SEE its neighbour does not name it either; the rest of the
    // form is reached the way anything outside this surface is - change / exec / eval / evalAction, which every
    // controller carries and which are gated in their own right
    private final Map<GContainer, JavaScriptObject> scopeControllers = new LinkedHashMap<>();
    public JavaScriptObject createReactController(GContainer scope) {
        JavaScriptObject scopeController = initController(scope);
        scopeControllers.put(scope, scopeController);
        syncControllerSugar(scopeController, scope);
        return scopeController;
    }

    private JavaScriptObject getScopeController(GContainer scope) {
        return scopeControllers.get(scope); // there is one wherever a member was called: it is what handed the member out
    }

    private native JavaScriptObject initController(GContainer scope) /*-{
        var thisObj = this;
        var controller = scope == null ? {} : { // no projection, no members - and nothing for the batch to shorten
            // the one mutation no single member can own — a batch spans properties, and groups — so it hangs at the
            // level whose members those properties are: a list of {property, object, value}, or one of them alone. A `property` is "integrationSID" or
            // "groupSID.integrationSID" (the ONLY names left that the platform has to resolve, since here there is no
            // member to be the address); `object` omitted means the property's group current object, and `value`
            // omitted execs it. One request for all of them, which is the whole point — otherwise it is
            // controller.<group>.<property>.change(...)
            properties: {
                change: function (entries) {
                    return thisObj.@GFormController::controllerChangeProperties(*)(entries, scope);
                }
            }
        };
        return this.@GFormController::gController.@GController::extendController(Lcom/google/gwt/core/client/JavaScriptObject;)(controller);
    }-*/;

    // the (object, value) guess and the dispatch behind every property change. Held HERE rather than on the controller
    // object because the PLATFORM needs it — the classic group view forwards a property that is none of its own
    // columns to it (#1655) — while an author must not have it: a stringly-typed changeProperty on the controller is
    // exactly the second interface this surface does without. A Java field is reachable from JSNI and invisible to a view.
    public final JavaScriptObject classicChangeProperty = initChangeProperty(true);
    private final JavaScriptObject memberChangeProperty = initChangeProperty(false);
    private native JavaScriptObject initChangeProperty(boolean classic) /*-{
        var thisObj = this;
        var UNDEFINED = @lsfusion.gwt.client.base.GwtClientUtils::UNDEFINED;
        //   (property, surface)                - exec the property/action on the current row
        //   (property, surface, value)         - set the value on the current row
        //   (property, surface, row)           - exec it on that row
        //   (property, surface, row, value)    - set the value on that row
        return function (property, surface, object, value, callScope) {
            var scope = classic ? null : callScope; // the classic grid's closure has no projection to be called from
            if (object !== undefined) {
                if (value === undefined) // one argument: decided by it ALONE, so the same argument always means the same
                    if (thisObj.@GFormController::isChangeObject(Ljava/lang/String;Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;Llsfusion/gwt/client/form/design/GContainer;)(surface, property, object, scope))
                        value = UNDEFINED; // a row -> exec on it
                    else { value = object; object = null; } // anything else (a value, or a bare key: they are indistinguishable) -> set on the current row
            } else { value = UNDEFINED; object = null; } // no argument -> exec on the current row
            object = object === undefined ? null : object;
            // the same guess, two surfaces: the classic grid names the whole form, a member names what a member names
            return classic ? thisObj.@GFormController::classicChangeProperty(*)(surface, property, object, value)
                           : thisObj.@GFormController::controllerChangeProperty(*)(surface, property, object, value, scope);
        };
    }-*/;

    // the members themselves, from the PROJECTION: what props.data has, the controller has - which is what makes the
    // member the address instead of a string, and what makes it mean the same thing the form means. Run after every
    // projection update rather than once, because that is when what the form shows changes; it is idempotent, so it
    // adds what appeared and removes what went. Nothing here has to handle a name it cannot take: a projected name
    // that would shadow a member of this surface is a form that was refused when it was built.
    // every projection's controller, brought to what that projection shows now. The FORM's is not among them: it has
    // no members to bring anywhere
    private void syncControllers() {
        for (Map.Entry<GContainer, JavaScriptObject> scope : scopeControllers.entrySet())
            syncControllerSugar(scope.getValue(), scope.getKey());
    }

    private native void syncControllerSugar(JavaScriptObject controller, GContainer scope) /*-{
        var thisObj = this;
        var changeProperty = this.@GFormController::memberChangeProperty;
        // "already taken" means an OWN member - the controller's own methods and what this loop has put there.
        // Object.prototype's names (toString, constructor, ...) are not the controller's, and a group or property
        // called one of them shadows nothing that belongs to this surface
        // ... and `__proto__`, which is taken by the LANGUAGE: `obj["__proto__"] = v` calls the prototype setter, so
        // the accessor would not be written and what the object inherits would be replaced by it
        function taken(obj, name) { return name === "__proto__" || Object.prototype.hasOwnProperty.call(obj, name); }

        // what a member IS, so one teardown loop can serve every kind and each kind can still be told apart: a
        // property member is the projection's to remove when it stops showing the value, a group's verbs are not.
        // Non-enumerable, so `for (var k in controller)` still lists exactly the names the view may say.
        function mark(member, kind) { Object.defineProperty(member, "__member", { value: kind }); return member; }

        function makeProperty(qualified) {
            var member = {
                //   .change()          exec on the group's current row      .change(row)         exec on that row
                //   .change(value)     set it on the current row            .change(row, value)  set it on that row
                change: function (object, value) {
                    return changeProperty(qualified, qualified + ".change()", object, value, scope);
                },
                // .getValues([row,] value[, mode], ok, fail[, count]) — the suggestion list for the current row, or for
                // the row given. ok gets {data:[{displayString,rawString,objects}], more}; mode picks the server lookup
                // (default 'objects'): 'objects' (each item's `objects` is a raw handle — the object picker) | 'values'
                // (DISTINCT property values) | 'change' (the edit-time autocomplete of the property's own change action).
                // Positional guess (no options object — that's a future uniform cross-method migration): ok is the first
                // function argument, and what precedes it is read by count and type. A row here must BE a row or a handle:
                // a string is the value query, so a bare key would be read as one.
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
                    thisObj.@GFormController::controllerGetPropertyValues(*)(qualified + ".getValues()", qualified, object === undefined ? null : object, value, mode == null ? null : mode, ok, fail, count == null ? 0 : count, scope);
                }
            };
            mark(member, "property");
            return member;
        }

        // the members of one level, brought to what the projection shows there NOW: what it stopped showing goes, what
        // it started showing arrives. Only a marked property member is removed - a group's own verbs, and whatever a
        // feature put beside them, are not this loop's to take away. Nothing here has to handle a name it cannot take:
        // a projected name colliding with a controller verb, with a group's own member or with `__proto__` is a form
        // that was refused when it was built (FormView.claimProjectionName)
        function syncProperties(owner, names, prefix) {
            var wanted = Object.create(null); // keyed by a property NAME, which is data: `{}` would inherit
            for (var i = 0; i < names.length; i++)  // toString/constructor/valueOf from Object.prototype and keep
                wanted[names[i]] = true;            // a member the projection stopped showing
            var own = Object.getOwnPropertyNames(owner);
            for (var oi = 0; oi < own.length; oi++) {
                var name = own[oi], member = owner[name];
                if (member && member.__member === "property" && !wanted[name])
                    delete owner[name];
            }
            for (var i = 0; i < names.length; i++) {
                var propSID = names[i];
                if (!taken(owner, propSID)) // ... and what is already there, this loop put there
                    owner[propSID] = makeProperty(prefix + propSID);
            }
        }

        var structure = thisObj.@GFormController::getControllerStructure(Llsfusion/gwt/client/form/design/GContainer;)(scope);
        var groups = structure.groups;
        for (var gi = 0; gi < groups.length; gi++) {
            var groupSID = groups[gi][0];
            var group = controller[groupSID];
            if (!taken(controller, groupSID)) { // made once: a group is projected into this container or it is not
                // the group's own state, named the way data.<group> names it: what the projection HAS is a member here
                // too, and changing it is .change(...). A feature that states a LIST of things adds its member beside
                // this one, where an array states them all and anything else states one
                group = mark((function (g) { return {
                    change: function (row) { return thisObj.@GFormController::controllerChangeObject(*)(g + ".change()", g, row, scope); }
                }; })(groupSID), "group");
                controller[groupSID] = group;
            }
            syncProperties(group, groups[gi][1], groupSID + ".");
        }
        syncProperties(controller, structure.props, ""); // form-level (no-group) properties sit directly on the controller
    }-*/;

    // WHAT THE CONTROLLER CARRIES: what `data` carries, and nothing else. A member's change() is the form's own ON
    // CHANGE event for that property - the one a user fires by editing the cell - so a property the form is not
    // showing has nobody to call it on, and a group no react container projects has no view to fire it from. The
    // member list is therefore the projection's own: for a projected group the names of `data.<group>.properties`,
    // and at the top level the form-level draws the scope they sit in is showing. It follows the data, so it is
    // rebuilt whenever the data is (syncControllerSugar). Two things this settles that used to be checked here: a
    // name the projection carries singles ONE ordinary draw out - two draws under one integration SID, or a
    // grouped-in-columns one, is a form that is not built (FormView) - so there is nothing left to filter or report.
    // The CLASSIC stringly-typed changeProperty is another surface and goes on naming the whole form.
    private JavaScriptObject getControllerStructure(GContainer scope) {
        JavaScriptObject groups = newArray();
        JavaScriptObject formProps = newArray();
        if (reactData != null) { // it is a projection's structure, so there is one - a guard, not a case
            for (GGroupObject group : form.groupObjects) {
                if (!reactData.isProjectedGroup(group, scope))
                    continue;
                JavaScriptObject props = newArray();
                for (String integrationSID : reactData.getValueNames(group, scope)) // what this container may CHANGE,
                                                                                     // not what its node names
                    push(props, integrationSID);
                JavaScriptObject entry = newArray();
                push(entry, group.getSID());
                push(entry, props);
                push(groups, entry);
            }
            NativeStringMap<Boolean> seen = new NativeStringMap<>(); // one member per name, whichever draw shows it
            for (GPropertyDraw draw : form.propertyDraws)
                // isShownFormProperty is left alone deliberately: RESOLUTION must go on finding an lsf form-level
                // draw (or a bare name would fall through to a group that happens to draw the same integration SID
                // and silently change a cell), so the `lsf` exclusion belongs here, where the MEMBER is made
                if (reactData.isShownFormProperty(draw, scope) && !draw.isLsfView() && seen.get(draw.integrationSID) == null) {
                    seen.put(draw.integrationSID, Boolean.TRUE);
                    push(formProps, draw.integrationSID);
                }
        }
        JavaScriptObject result = GwtClientUtils.newObject();
        GwtClientUtils.setField(result, "groups", groups);
        GwtClientUtils.setField(result, "props", formProps);
        return result;
    }

    // ===== custom-controller mutation helpers (CUSTOM REACT + any form-level custom component): resolve the draw by
    // "integrationSID" or "groupSID.integrationSID"; rows/handles resolve via GGroupObjectValue.resolveObject (the
    // row-carried `objects` handle + raw-GGV accept). An explicit name prefix has priority; with none, the row's group scopes the draw.
    // Dispatch through the SAME classic path as a normal edit (executePropertyEventAction / changeGroupObject + setLoadingValueAt) =====
    // what the author typed, as the message names it: every controller entry point is given the member path it was
    // reached by ("g.change()", "g.qty.change()", "properties.change() entry 2 ('g.qty')"), so an error quotes the call
    // that is in the view's source instead of a verb name the surface no longer has
    private static String controllerPrefix(String surface) {
        return "controller." + surface + ": ";
    }

    public void controllerChangeObject(String surface, String groupSID, JavaScriptObject objectOrKey, GContainer scope) {
        String errorPrefix = controllerPrefix(surface);
        GGroupObject group = resolveGroup(errorPrefix, groupSID); // it alone used to answer a typo with silence
        GGroupObjectValue key = resolveRow(errorPrefix, group, objectOrKey, scope);
        // ... narrowed to a row of THIS group, by the group's own rule - which both checks and cuts. A key that is
        // not this group's row at all answers null, and it would otherwise set the objects it does carry and blank
        // the rest, which the server only asserts about (so: nothing at all in production). And in a TREE a row of a
        // group BELOW carries the path past this one: accepted (it holds one row of every group above), but what is
        // SET is the path down to here - the deeper half names rows of other groups, which the server asserts about
        // too, and the projection would keep as a current object no row of this group is found by
        GGroupObjectValue rowKey = key.isEmpty() ? key : group.getRowKey(key);
        if (rowKey == null)
            throw new RuntimeException(errorPrefix + "that row is not a row of '" + groupSID + "'");
        changeGroupObject(group, rowKey, null, null);
    }

    private static final String ORDER_FIELDS = "property,desc";

    // what a condition and a sorting BOTH are, before they differ: an object, carrying only fields this call knows,
    // naming a property of THIS group. Said once, or one of them would go on accepting what the other refuses - which
    // is also why this reader and the field lists beside it live in the COMMON layer with no caller of their own:
    // the user filters and the sortings each bring one, and a copy per branch would be two rules again
    private GPropertyDraw readStateProperty(String errorPrefix, GGroupObject group, GContainer scope, JavaScriptObject item, String what, String fields) {
        // a condition and a sorting are the state of ONE react view, so this is reached through a member of that
        // view's controller - and a member of it exists only for a group that view PROJECTS. Said here rather than
        // assumed, because what follows rests on it: inside a projection a name means one ordinary draw
        checkProjectedGroup(errorPrefix, group, scope);
        String fieldList = fields.replace(",", ", ");
        if (!isJSObject(item))
            throw new RuntimeException(errorPrefix + what + " must be an object {" + fieldList + "}");
        String unknown = getUnknownField(item, fields);
        if (unknown != null) // a typo'd `value` would otherwise read as "no value", i.e. silently remove the condition
            throw new RuntimeException(errorPrefix + "unknown field '" + unknown + "'; " + what + " has " + fieldList);
        String propertySID = getOwnString(item, "property");
        if (propertySID == null) // absent, or there but not a name - the same distinction the batch entry makes
            throw new RuntimeException(errorPrefix + (hasOwnField(item, "property")
                    ? "'property' must be the property's name, as a string" : what + " has no 'property'"));
        GPropertyDraw property = form.getPropertyDraw(group, propertySID);
        if (property == null)
            throw new RuntimeException(errorPrefix + "'" + propertySID + "' is not a property of this object group");
        return property; // and nothing more to check: a PROJECTED group draws no property grouped in columns and no
    }                    // two under one integration SID - such a form is refused when it is built (FormView)

    // what a react view's own state rests on: the group is one the projection carries, and there the platform has
    // already settled what the readers above do not ask - no draw grouped in COLUMNS, no integration SID answering
    // for two of them. A verb wired to a group nothing projects would read a name nothing checked, so it is refused
    private void checkProjectedGroup(String errorPrefix, GGroupObject group, GContainer scope) {
        if (reactData == null || !reactData.isProjectedGroup(group, scope))
            throw new RuntimeException(errorPrefix + "this react container does not project object group '"
                    + group.getSID() + "', and this is the state of the view that does");
    }

    // a group of a tree is named and resolved like any other: a tree holds all of its groups' state in one panel and
    // one header manager, and it is the tree's own controller that puts what goes in and out of them back among the
    // other groups', so one group's state stays its own
    private GGroupObject resolveGroup(String errorPrefix, String groupSID) {
        GGroupObject group = form.getGroupObject(groupSID);
        if (group == null)
            throw new RuntimeException(errorPrefix + "unknown object group '" + groupSID + "'");
        return group;
    }

    // a property grouped in COLUMNS is one cell per (row, column), and everything here names a ROW. The projection
    // does not carry such a draw at all, so a call naming it would land on whatever column happens to be current -
    // a different, real cell, and it would look like it worked. Refused with the reason, as `lsf` on one already is.
    private void checkAddressableDraw(String errorPrefix, GPropertyDraw draw) {
        if (draw.hasColumnGroupObjects())
            throw new RuntimeException(errorPrefix + "'" + draw.integrationSID + "' is grouped in columns - it is one cell"
                    + " per row AND column, and this names a row, so it is not addressed here (nor projected)");
    }

    private static final String FILTER_CONDITION_FIELDS = "property,compare,value,negation,or";

    // a flag is a boolean or absent - a truthy string like "false" would otherwise mean true. A condition's and a
    // sorting's alike: `negation`, `or`, `desc`
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
    // resolves a controller-call draw. `name` is either "groupSID.integrationSID" (an explicit group prefix) or a
    // bare integration SID. an explicit prefix in the name has priority: it scopes the lookup directly (the passed
    // object's own group is not consulted). with no prefix, an explicit object's group scopes it; with neither, a
    // bare SID must be form-unique. throws a loud (JS-surfaced) RuntimeException on every author mistake: unknown
    // group, ambiguous bare SID, or missing property. `objectKey` is the already-resolved row key, or null/EMPTY
    // for the current object.
    // `scope` says which surface is asking - a projection's controller, or (null) the CLASSIC stringly-typed API -
    // and it changes one thing: what a BARE name means. On a controller a bare name is a member, and a member with no
    // group is the form-level property; the classic API has no members, so there a bare name goes on meaning "the one
    // draw of the whole form that answers to it", and two of them are still an error it cannot resolve.
    private GPropertyDraw resolveControllerDraw(String surface, String name, GGroupObjectValue objectKey, GContainer scope) {
        String errorPrefix = controllerPrefix(surface);
        // a qualified name splits at its LAST dot. A property's integration SID never carries one, so everything
        // before the last dot is the group and everything after is the property - and splitting anywhere else would
        // ask for a group that does not exist. It is the CLASSIC surface this is for: there a group of several
        // objects is named by its SID, all of them joined with dots (`OBJECTS d = X, t = Y` is `d.t`). A PROJECTED
        // group cannot be one of those - a form is refused at build time until such a group is given a name.
        int dot = name.lastIndexOf('.');
        String prefixGroupSID = dot > 0 ? name.substring(0, dot) : null;
        String integrationSID = dot > 0 ? name.substring(dot + 1) : name;

        // a bare name on THIS surface is the form-level member, and it stays that whatever object came with it: a row
        // passed to a form-level property is meaningless, and letting the row's group scope the name would answer with
        // a DIFFERENT property that happens to share it. Asked before the object, so both readings say one thing
        if (scope != null && prefixGroupSID == null && reactData != null) {
            GPropertyDraw formLevel = reactData.getShownFormProperty(integrationSID, scope); // the one that HAS the member
            if (formLevel != null)
                return formLevel; // what the projection carries is checked when the form is built, so nothing to add
        }

        GGroupObject group = null;
        if (prefixGroupSID != null) { // an explicit group prefix in the name wins over the passed object's group
            group = form.getGroupObject(prefixGroupSID);
            if (group == null) // it is qualified and names no group of this form: say which half failed
                throw new RuntimeException(errorPrefix + "unknown object group '" + prefixGroupSID + "'");
        } else if (objectKey != null && !objectKey.isEmpty()) { // no prefix: the explicit row's own group scopes the lookup
            group = form.getObject(objectKey.getKey(0)).groupObject; // a resolved row/handle is a same-form object
        }

        if (group != null) {
            GPropertyDraw draw = form.getPropertyDraw(group, integrationSID);
            if (draw == null)
                throw new RuntimeException(errorPrefix + "property '" + integrationSID + "' is not drawn on group '" + group.getSID() + "'");
            if (form.countPropertyDraws(group, integrationSID) != 1) // the name singles none of them out - the same
                throw new RuntimeException(errorPrefix + "'" + integrationSID + "' is drawn more than once on group '" // rule the member list is built by
                        + group.getSID() + "', so this name does not say which of them is meant; give them explicit EXTIDs");
            checkAddressableDraw(errorPrefix, draw);
            return draw;
        }

        // ... and where the form level said nothing, a bare name means the one group that draws it - the convenience
        // `properties.change({property: 'qty'})` rests on
        GPropertyDraw draw = form.getSinglePropertyDraw(integrationSID); // throws if ambiguous
        if (draw == null)
            throw new RuntimeException(errorPrefix + "property '" + integrationSID + "' not found");
        checkAddressableDraw(errorPrefix, draw);
        return draw;
    }

    // a row of a KNOWN group, named however a view has it - the row, its handle, or the key the projection gave it.
    // Where the group is known the key can be looked up, so a view can hand back what it was handed; where it is not
    // (an unqualified changeProperty, whose group the object itself has to name) only a row or a handle can say which
    // group it belongs to, and a bare key is still refused
    private GGroupObjectValue resolveRow(String errorPrefix, GGroupObject group, JavaScriptObject objectOrKey, GContainer scope) {
        GGroupObjectValue key = reactData != null ? reactData.resolveRowKey(group, objectOrKey, scope)
                                                  : GGroupObjectValue.resolveObject(objectOrKey);
        if (key == null) {
            // a key STRING is the projection's own name for a row, looked up in the index built with the rows - so it
            // is a name only where the rows are. A view that does not draw them was handed no key to hand back
            if (reactData != null && scope != null && !reactData.drawsRows(group, scope))
                throw new RuntimeException(errorPrefix + "expects a row of '" + group.getSID() + "' or its objects"
                        + " handle: this view does not draw that group's rows, so a key string names nothing here");
            throw new RuntimeException(errorPrefix + "expects a row of '" + group.getSID() + "', its objects handle,"
                    + " or its key; that is none of them, or names no row the group has now");
        }
        return key;
    }

    private GGroupObjectValue resolveControllerObject(String surface, String property, JavaScriptObject objectOrKey, GContainer scope) {
        if (GwtClientUtils.isUndefinedOrNull(objectOrKey)) // raw JS key: a numeric 0 key reads as null under Java == null (GWT falsy-primitive collapse)
            return null;

        GGroupObject nameGroup = getNameGroup(property, scope); // whose row the NAME says this is - both halves below ask it
        GGroupObjectValue objectKey = GGroupObjectValue.resolveObject(objectOrKey);
        if (objectKey == null) { // ... a KEY resolves too, wherever the name settles which group's rows to look in
            if (nameGroup != null)
                return resolveRow(controllerPrefix(surface), nameGroup, objectOrKey, scope);
            // it does not settle it - the property is drawn on several groups, or on none - so the group is what the
            // object argument itself is being asked for, and a bare key names no group
            throw new RuntimeException(controllerPrefix(surface) + "the object argument is not a data row or an objects handle;"
                    + " pass one of those, or name the property as '<group>." + property + "' so its key can be looked up"
                    + " ('" + property + "' alone does not say which group's row is meant)");
        }
        // ... and where the name says which group's row is meant, a row of ANOTHER group is refused - exactly as
        // controllerChangeObject refuses it, and for the same reason: the key carries only that group's objects, so
        // the rest are taken from the CURRENT ones and the call lands on a cell of this group that nobody named. A
        // key was already looked up among the named group's own rows above; only a row/handle could get this far.
        // Where the name settles no group - the property is drawn on several - there is nothing to check against, and
        // the row's own group is what scopes the draw instead. In a TREE a row of a group BELOW passes, and should: its
        // key is the path down to it, so it holds one row of every group above too - the same reading
        // controllerChangeObject has always had, and the two say one thing.
        if (nameGroup != null && !objectKey.isEmpty() && objectKey.filter(Collections.singletonList(nameGroup)) == null)
            throw new RuntimeException(controllerPrefix(surface) + "that row is not a row of '" + nameGroup.getSID() + "'");
        return objectKey;
    }

    // the group a NAME settles ON ITS OWN, with no object to ask: the one its prefix names, or - for a bare name -
    // the single group it is drawn on. A key can be looked up wherever this answers, which is what lets a view hand
    // back `data.<g>.keys[i]` to `properties.change([{property: 'qty', object: <that key>}])`: the name says `qty`,
    // `qty` is drawn on one group, so the key has somewhere to be looked up. It answers null only when the name
    // genuinely cannot say - drawn on several groups, or form-level, or not drawn at all - and there the object
    // itself has to name the group, so a bare key is still refused.
    private GGroupObject getNameGroup(String name, GContainer scope) {
        int dot = name.lastIndexOf('.'); // the same one split resolveControllerDraw reads the name with
        if (dot > 0) // qualified: the prefix answers, or nothing does
            return form.getGroupObject(name.substring(0, dot));
        if (scope != null && reactData != null && reactData.getShownFormProperty(name, scope) != null) // the same reading the
            return null;                    // draw is resolved by: a bare name is the FORM-LEVEL member, which has no group
        return form.getSingleDrawnGroup(name); // drawn on more than one: only the object can say which is meant
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
    // THE single guess core, shared by both surfaces — they differ only in how the draw is found (form-level: by the
    // controller name/object resolver; classic: by its view column), the policy is one
    public boolean isChangeObject(GPropertyDraw draw, JavaScriptObject object) {
        if (draw == null || draw.groupObject == null) // no object slot at all -> the value form is the only one
            return false;
        if (GGroupObjectValue.resolveObject(object) != null)
            return true;
        return draw.isAction();
    }
    public boolean isChangeObject(String surface, String property, JavaScriptObject object, GContainer scope) {
        return isChangeObject(resolveControllerDraw(surface, property, GGroupObjectValue.resolveObject(object), scope), object);
    }
    // a member's own change(): one property, and a member is what named it
    public void controllerChangeProperty(String surface, String property, JavaScriptObject objectOrKey, JavaScriptObject value, GContainer scope) {
        changeControllerProperties(new String[]{surface}, new String[]{property}, new JavaScriptObject[]{objectOrKey}, new JavaScriptObject[]{value}, scope);
    }

    // the CLASSIC grid view's stringly-typed changeProperty, which forwards a property that is none of its own
    // columns (#1655). It has its OWN entry rather than a flag on the one above, because it is another surface with
    // another rule: it names the whole FORM, member or no member, as it always has - that API is older than this one
    // and is not a shortcut for its members. Everything else it does is the same, so it is the same three steps.
    public void classicChangeProperty(String surface, String property, JavaScriptObject objectOrKey, JavaScriptObject value) {
        GGroupObjectValue objectKey = resolveControllerObject(surface, property, objectOrKey, null); // no scope: the classic API
        GPropertyDraw draw = resolveControllerDraw(surface, property, objectKey, null); // throws on ambiguity / conflict / not-found
        changeProperties(new GPropertyDraw[]{draw}, new GGroupObjectValue[]{getControllerColumnKey(objectKey)},
                new PValue[]{GSimpleStateTableView.convertFromJSUndefValue(draw, value)});
    }
    private static final String CHANGE_ENTRY_FIELDS = "property,object,value";

    // the batch, whose names ARE its interface (no member can be one call for many properties): every entry says which
    // of them it is, so a mistake in the fifth entry is not reported as a mistake in the call
    public void controllerChangeProperties(JavaScriptObject entries, GContainer scope) {
        String surface = "properties.change()";
        String errorPrefix = controllerPrefix(surface);
        boolean one = !GSimpleStateTableView.isJSArray(entries); // a list states them all, one entry states one
        int size = one ? 1 : GSimpleStateTableView.jsArrayLength(entries);

        String[] surfaces = new String[size], properties = new String[size];
        JavaScriptObject[] objectsOrKeys = new JavaScriptObject[size], values = new JavaScriptObject[size];
        for (int i = 0; i < size; i++) {
            JavaScriptObject entry = one ? entries : GSimpleStateTableView.jsArrayGet(entries, i);
            String entryPrefix = one ? errorPrefix : errorPrefix + "entry " + i + ": ";
            if (!isJSObject(entry))
                throw new RuntimeException(entryPrefix + "a change must be an object {property, object, value}");
            String unknown = getUnknownField(entry, CHANGE_ENTRY_FIELDS);
            if (unknown != null) // a typo'd `value` would otherwise read as "no value", i.e. exec the property instead of setting it
                throw new RuntimeException(entryPrefix + "unknown field '" + unknown + "'; a change has property, object, value");
            String property = getOwnString(entry, "property");
            if (property == null) // absent, or there but not a name: a name-shaped thing that is not a name is not "none"
                throw new RuntimeException(entryPrefix + (hasOwnField(entry, "property")
                        ? "'property' must be the property's name, as a string" : "a change has no 'property'"));
            surfaces[i] = one ? surface : surface + " entry " + i + " ('" + property + "')";
            properties[i] = property;
            objectsOrKeys[i] = getOwnField(entry, "object");
            values[i] = getOwnFieldOrUndefined(entry, "value"); // absent -> UNDEFINED -> exec, as a member's change() with no argument does
        }
        changeControllerProperties(surfaces, properties, objectsOrKeys, values, scope);
    }
    // `properties.change` is a SHORTCUT for the members, not a second way in: it exists because a batch spans
    // properties, and groups, so no single member can own it - and it therefore says exactly what a member says.
    // Whatever it resolves to has to be reachable as controller.<group>.<property> (controller.<property> at the form
    // level), so a draw the controller left WITHOUT a member - a name that would have shadowed a member of the level
    // it sits on - is not addressable here either. Asked of the controller OBJECT rather than restated here: it is
    // the one that hands the members out, so whatever a feature adds to it is covered by the same question, and
    // there is one answer to "is there a way to say this" - including for a member captured before the form stopped
    // showing that property, which is the one this normally meets.
    private void checkControllerMember(String surface, GPropertyDraw draw, GContainer scope) {
        String groupSID = draw.groupObject != null ? draw.groupObject.getSID() : null;
        if (!hasControllerMember(getScopeController(scope), groupSID, draw.integrationSID))
            throw new RuntimeException(controllerPrefix(surface) + "'" + (groupSID != null ? groupSID + "." : "")
                    + draw.integrationSID + "' is not a member of this view's controller: the form is not showing it"
                    + " now, this react container does not project it, or the PLATFORM draws it - an LSF property is"
                    + " drawn and edited by its own renderer, which this view places with <Lsf name row/>; drop"
                    + " lsf = TRUE if React should draw it. properties.change() is the members' batch, not a way past"
                    + " them");
    }
    // an OWN member that IS a property member (syncControllerSugar marks them): a name off Object.prototype
    // (toString, constructor) is nothing the controller gave out, and a group node, the batch and a group's own verb
    // all answer to `change` without being a property - a form property whose SID is taken by one of those has no
    // member of its own, which is the whole question here
    private static native boolean hasControllerMember(JavaScriptObject controller, String groupSID, String name) /*-{
        var owner = groupSID == null ? controller : controller[groupSID];
        if (!owner || !Object.prototype.hasOwnProperty.call(owner, name))
            return false;
        var member = owner[name];
        return !!member && member.__member === "property";
    }-*/;

    // the controller's own surface, all of it: a member's change() and the batch that is a shortcut for the members.
    // Both say what a member says, so the member is asked for HERE, once, rather than at each entry
    private void changeControllerProperties(String[] surfaces, String[] properties, JavaScriptObject[] objectsOrKeys, JavaScriptObject[] values, GContainer scope) {
        if (properties.length == 0) // an empty batch is a no-op, not an empty request
            return;
        GPropertyDraw[] props = new GPropertyDraw[properties.length];
        GGroupObjectValue[] keys = new GGroupObjectValue[properties.length];
        PValue[] pvalues = new PValue[properties.length];
        for (int i = 0; i < properties.length; i++) {
            GGroupObjectValue objectKey = resolveControllerObject(surfaces[i], properties[i], objectsOrKeys[i], scope);
            GPropertyDraw draw = resolveControllerDraw(surfaces[i], properties[i], objectKey, scope); // throws on ambiguity / conflict / not-found
            checkControllerMember(surfaces[i], draw, scope);
            props[i] = draw;
            keys[i] = getControllerColumnKey(objectKey);
            pvalues[i] = GSimpleStateTableView.convertFromJSUndefValue(draw, values[i]);
        }
        changeProperties(props, keys, pvalues);
    }
    // the typed core: resolved draws/keys/values -> the same classic batch edit dispatch as a normal user edit
    private void changeProperties(GPropertyDraw[] pa, GGroupObjectValue[] ka, PValue[] va) {
        executePropertyEventAction(pa, ka, va, requestIndex -> {
            for (int i = 0; i < pa.length; i++)
                // WYSIWYG guard (like GSimpleStateTableView.changeProperties): only overlay the optimistic value when the
                // input writes the displayed property value; a non-WYSIWYG change (custom action / unbound input) must wait for the server
                if (va[i] != PValue.UNDEFINED && pa[i].hasExternalChangeActionForRendering(RendererType.SIMPLE))
                    setLoadingValueAt(pa[i], ka[i], va[i], requestIndex); // optimistic overlay (+ optimistic react via setLoadingValueAt -> setPropertyValue), reconciled by requestIndex
        });
    }
    // maps the public getPropertyValues `mode` to the server async actionSID (the strings coincide). null/'objects' => OBJECTS
    // (the only mode that returns object handles); 'values' => DISTINCT property values; 'change' => the property's edit-time
    // value autocomplete (respects custom INPUT/notNull/custom-change, requires editability). STRICTVALUES is intentionally NOT
    // exposed: it's only `values` + exact-match UX post-processing the platform derives from the filter operator, not an author
    // choice (add an `exactMatch` flag later if ever needed). Returns null for an unknown mode (caller rejects loudly).
    public static String getAsyncActionSID(String mode) {
        return getAsyncActionSID("controller.<property>.getValues(): ", mode); // the classic grid view, which has no member path to quote
    }
    private static String getAsyncActionSID(String errorPrefix, String mode) {
        if (mode == null || mode.equals(ServerResponse.OBJECTS))
            return ServerResponse.OBJECTS;
        if (mode.equals(ServerResponse.VALUES) || mode.equals(ServerResponse.CHANGE))
            return mode;
        GwtClientUtils.consoleError(errorPrefix + "unknown mode '" + mode + "'; expected 'objects' | 'values' | 'change'");
        return null;
    }
    // async value lookup (autocomplete / suggestion list) for a form-level / CUSTOM REACT property — the form-level twin
    // of the grid custom view's getAsyncValues (GSimpleStateTableView). Resolves the draw and optional row exactly like
    // changeProperty (GGroupObjectValue.resolveObject); the resolved row key becomes the columnKey, which
    // getFullCurrentKey overlays over the current selection -> suggestions scoped to that row (EMPTY => the property
    // group's current object). The mode picks the server lookup (OBJECTS suggestions vs distinct VALUES vs the edit-time
    // CHANGE autocomplete; CHANGE here targets THIS draw's own change action — no property:value concat, that's the
    // JSON-property cell-renderer's special case). Issues through the shared getAsyncValues.
    public void controllerGetPropertyValues(String surface, String property, JavaScriptObject objectOrKey, String value, String mode, JavaScriptObject successCallback, JavaScriptObject failureCallback, int increaseValuesNeededCount, GContainer scope) {
        String actionSID = getAsyncActionSID(controllerPrefix(surface), mode); // named by the member it was called on, like every other error here
        if (actionSID == null) { // unknown mode (already logged)
            if (failureCallback != null)
                GwtClientUtils.call(failureCallback);
            return;
        }
        GGroupObjectValue objectKey = resolveControllerObject(surface, property, objectOrKey, scope);
        GPropertyDraw draw = resolveControllerDraw(surface, property, objectKey, scope); // throws on ambiguity / conflict / not-found
        checkControllerMember(surface, draw, scope); // a member captured before the form stopped showing it asks like any other
        getAsyncValues(value, draw, getControllerColumnKey(objectKey), actionSID, getJSCallback(successCallback, failureCallback), increaseValuesNeededCount);
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
        if (reactData != null)
            initializeReactController(null);

        for (GTreeGroup treeGroup : form.treeGroups) {
            if (!isReactOwned(treeGroup)) // react-owned tree (inside a react container) -> no GWT tree controller
                initializeTreeController(treeGroup);
        }

        for (GGroupObject group : form.groupObjects) {
            // a group can have both: React draws its rows from the projection, and the platform draws its LSF
            // properties into each of those rows
            for (GPropertyDraw draw : form.propertyDraws)
                if (draw.groupObject == group && draw.isLsfViewPerRow()) {
                    rowPanelControllers.put(group, new GRowPanelController(this, group));
                    break;
                }

            // ... and a group can be MIXED the other way too: GWT draws its rows, and a react container holds one of
            // its panel properties. That property's values are routed to a react controller
            // (getReactablePropertyController), so the group needs one whether or not React draws it.
            if (isReactOwned(group)) {
                initializeReactController(group);
            } else {
                if (group.parent == null) {
                    initializeGroupController(group);
                }
                if (hasReactOwnedDraw(group))
                    initializeReactController(group);
            }
        }

        panelController = new GPanelController(this); // kept even for React: getPropertyController/update rely on it; it stays empty when only react-owned property readers are skipped, so no panel views are built
    }

    // any of the group's properties projected into a react container, its rows drawn where they are drawn
    private boolean hasReactOwnedDraw(GGroupObject group) {
        for (GPropertyDraw draw : form.propertyDraws)
            if (draw.groupObject == group && isReactOwned(draw))
                return true;
        return false;
    }

    private void initializeReactController(GGroupObject group) {
        reactControllers.put(group, new GReactController(group, reactData, this::refreshReactOptimistic));
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
        controllers.put(group, controller);
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

    private Map<GGroupObject, LinkedHashMap<GPropertyDraw, Boolean>> groupDefaultOrders() {
        Map<GGroupObject, LinkedHashMap<GPropertyDraw, Boolean>> orders = new HashMap<>();
        for(Map.Entry<GPropertyDraw, Boolean> defaultOrder : form.defaultOrders.entrySet()) {
            GGroupObject groupObject = defaultOrder.getKey().groupObject;
            LinkedHashMap<GPropertyDraw, Boolean> order = orders.computeIfAbsent(groupObject, k -> new LinkedHashMap<>());
            order.put(defaultOrder.getKey(), defaultOrder.getValue());
        }
        return orders;
    }

    public void initializeDefaultOrders() {
        Map<GGroupObject, LinkedHashMap<GPropertyDraw, Boolean>> defaultOrders = groupDefaultOrders();
        for(Map.Entry<GGroupObject, LinkedHashMap<GPropertyDraw, Boolean>> entry : defaultOrders.entrySet()) {
            GGroupObject groupObject = entry.getKey();
            GAbstractTableController controller = getGroupObjectController(groupObject);
            if (controller != null) // null for react-owned groups (no base controller)
                controller.changeOrders(groupObject, entry.getValue(), true);
        }
    }

    public void initializeUserOrders() {
        boolean changed = false;
        for (GGridController controller : controllers.values()) {
            LinkedHashMap<GPropertyDraw, Boolean> objectUserOrders = controller.getUserOrders();
            if (objectUserOrders != null)
                changed = controller.changeOrders(objectUserOrders, false)  || changed;
        }
        if (changed)
            getRemoteChanges();
    }

    public LinkedHashMap<GPropertyDraw, Boolean> getDefaultOrders(GGroupObject groupObject) {
        return form.getDefaultOrders(groupObject);
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
        GFormEventClose eventClose = new GFormEventClose(reason instanceof CommitReason);

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

        modifyFormChangesWithChangePropertyAsyncs(requestIndex, fc);

        modifyFormChangesWithLoadingPropertyAsyncs(requestIndex, fc);

        applyLoadingFilterAsyncs(requestIndex, fc);

        applyKeyChanges(fc, requestIndex);

        applyPropertyChanges(fc);

        update(fc, requestIndex);

        expandCollapseContainers(fc);

        activateElements(fc);

        applyNeedConfirm(fc);

        formLayout.update(requestIndex);

        if (reactData != null) { // CUSTOM REACT: project form state and push to the React container(s)
            reactData.update(fc);
            // the controller carries what the projection carries, so it is brought along with it - a property the
            // form stopped showing loses its member here, exactly as it loses its entry there
            syncControllers();
            formLayout.updateReactContainers(reactData);

            updateRowRenderers(); // after the projection: the per-row renderers follow the rows it now holds
        }
    }

    public void applyKeyChanges(GFormChanges fc, int requestIndex) {
        fc.gridObjects.foreachEntry((key, value) -> {
            getGroupController(key).updateKeys(key, value, fc, requestIndex);

        });

        fc.objects.foreachEntry((key, value) -> getGroupController(key).updateCurrentKey(value));
    }

    private void applyPropertyChanges(GFormChanges fc) {
        fc.dropProperties.forEach(property -> {
            GFormPropertyController controller = getReactablePropertyController(property);
            if (controller.isPropertyShown(property)) // drop properties sent without checking if it was sent for update at least once, so it's possible when drop is sent for property that has not been added
                controller.removeProperty(property);
        });

        // first proceed property with its values, then extra values (some views, for example GPivot use updated properties)
        updatePropertyChanges(fc, key -> !(key instanceof GExtraPropertyReader));
        updatePropertyChanges(fc, key -> key instanceof GExtraPropertyReader);
    }

    private void updatePropertyChanges(GFormChanges fc, Predicate<GPropertyReader> filter) {
        fc.properties.foreachEntry((key, value) -> {
            // a reader React consumes is skipped from GWT and instead fed by reactData.update(fc): a react-owned
            // component's value (into data), or an lsf child's caption/image reader (into its entry). The
            // lsf child's own VALUE draw reader is not owned, so it still reaches its GWT view here.
            if(filter.test(key) && !isReactOwned(key))
                key.update(this, value, fc.updateProperties.contains(key));
        });
    }

    public void update(GFormChanges fc, int requestIndex) {
        for (GGridController controller : controllers.values())
            controller.update(requestIndex, fc);

        for (GTreeGroupController treeController : treeControllers.values())
            treeController.update();

        for (GRowPanelController rowPanelController : rowPanelControllers.values())
            rowPanelController.update();

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

    private void expandCollapseContainers(GFormChanges formChanges) {
        for (GContainer container : formChanges.collapseContainers) {
            setContainerExtCollapsed(container, true);
        }

        for (GContainer container : formChanges.expandContainers) {
            setContainerExtCollapsed(container, false);
        }
    }

    private void setContainerExtCollapsed(GContainer container, boolean collapsed) {
        if (container.container != null) {
            GAbstractContainerView parentContainerView = formLayout.getContainerView(container.container);
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

    private void modifyFormChangesWithChangePropertyAsyncs(final int currentDispatchingRequestIndex, final GFormChanges fc) {
        pendingChangePropertyRequests.foreachEntry((property, values) -> values.foreachEntry((keys, change) -> {
            long requestIndex = change.requestIndex;
            if (requestIndex <= currentDispatchingRequestIndex) {
                removeFromDoubleMap(pendingChangePropertyRequests, property, keys);

                // react-owned: no controller, the value IS in the projection -> treat as shown (and reconcile)
                boolean propertyShown = !(property instanceof GPropertyDraw)
                        || (!fc.dropProperties.contains((GPropertyDraw) property) && getReactablePropertyController((GPropertyDraw) property).isPropertyShown((GPropertyDraw) property));
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

                if(!fc.dropProperties.contains(property) && getReactablePropertyController(property).isPropertyShown(property)) { // react-owned: no controller -> treat as shown
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

    public GAbstractTableController getGroupObjectController(GGroupObject group) {
        GGridController groupObjectController = controllers.get(group);
        if (groupObjectController != null) {
            return groupObjectController;
        }

        return treeControllers.get(group.parent);
    }

    // the LSF grid property whose rows this react container draws, by the design identifier - PROPERTY(qty(d)).
    // Asked of the FORM, not of the container's children: such a property is not a child of it (GPropertyDraw.isLsfView).
    public GPropertyDraw getRowLsfViewProperty(String sid, GContainer scope) {
        for (GPropertyDraw draw : form.propertyDraws)
            if (draw.isLsfViewPerRow() && sid.equals(draw.sID) && reactData.drawsRows(draw.groupObject, scope))
                return draw;
        return null;
    }

    public GPropertyController getPropertyController(GPropertyDraw property) {
        // an LSF property is a grid property, so without this it would be routed to its group's controller - which
        // for a group React draws is a set of deliberate no-ops, and its values would be dropped on the way
        if (property.isLsfViewPerRow()) {
            // initializeControllers makes one for every group holding such a property, by this very predicate, so a
            // miss is a broken invariant rather than a case to fall through - falling through would route the property
            // to its group's controller and drop its values there in silence
            GRowPanelController rowPanelController = rowPanelControllers.get(property.groupObject);
            assert rowPanelController != null;
            return rowPanelController;
        }

        if(property.isList) {
            return getGroupObjectController(property.groupObject);
        } else
            return panelController;
    }

    private GFormGroupController getGroupController(GGroupObject group) {
        return isReactOwned(group) ? getReactController(group) : (GFormGroupController) getGroupObjectController(group);
    }

    private GFormPropertyController getReactablePropertyController(GPropertyDraw property) {
        if (isReactOwned(property)) {
            GReactController controller = getReactController(property.groupObject);
            if (controller != null)
                return controller;
            // react-owned but no react controller for its group (e.g. a group-less form prop inside a react
            // container before any react group registered): fall through to the classic controller, never NPE
        }
        return getPropertyController(property);
    }

    private GReactController getReactController(GGroupObject group) {
        return reactControllers.get(group);
    }

    // the react container that RENDERS this component (GComponent.getDrawingReactContainer, the walk), plus the one
    // thing the walk cannot answer: a react container is itself react-owned
    public GContainer getOwningReactContainer(GComponent component) {
        if (reactData == null || component == null)
            return null;
        GContainer outer = GComponent.getDrawingReactContainer(component); // the walk itself is pure design data
        if (outer != null)
            return outer;
        // a react container is itself react-owned (its OWN layout readers — caption/image/classes/showif/custom —
        // must be skipped too, not only its descendants'; else the root react container would NPE/CCE on those readers)
        if (component instanceof GContainer && ((GContainer) component).isReact())
            return (GContainer) component;
        return null;
    }

    // the rows and the current row the projection holds for a group, for the per-row renderers that follow them. Both
    // include an optimistic change the view made before the server confirmed it
    public ArrayList<GGroupObjectValue> getReactRows(GGroupObject group) {
        return reactData != null ? reactData.getRows(group) : null;
    }

    public GGroupObjectValue getReactCurrentObject(GGroupObject group) {
        return reactData != null ? reactData.getCurrentObject(group) : null;
    }

    // the controller drawing this LSF property, for whoever is placing one of its per-row renderers
    public GRowPanelController getRowPanelController(GPropertyDraw property) {
        return property != null ? rowPanelControllers.get(property.groupObject) : null;
    }

    public boolean isReactOwned(GComponent component) {
        return getOwningReactContainer(component) != null;
    }

    public boolean isReactOwned(GGroupObject group) {
        return group != null && isReactOwned(group.getDrawComponent()); // where its ROWS are drawn
    }

    public boolean isReactOwned(GPropertyDraw property) {
        if (property == null)
            return false;

        // an LSF property is drawn by the platform - once per row for a grid property, once for a panel one - so its
        // whole reader set (value, editability, colours, font, placeholder, pattern, tooltips and the rest) feeds those
        // renderers instead of the projection. Its caption and image still reach its entry: taken earlier, by
        // isLsfViewDescriptorReader. The predicate is isLsfView, not isLsfViewPerRow, so that it states the whole rule on its
        // own: an LSF PANEL draw of a react-owned group would otherwise fall through to the group answer below and
        // come back react-owned, and its readers would then be skipped here while the projection emits only caption/image.
        // FormView.checkLsfView rejects that form at startup ("mark the group's box instead"), so this is agreement
        // with the server rather than a case that reaches us - an LSF draw is platform-drawn, and that is the answer.
        if (property.isLsfView())
            return false;

        // WHERE IT IS, for anything the platform does not draw once per row. A GRID property is drawn by the grid, so
        // its cells are projected exactly where the grid is; a PANEL property is a component of its own, standing
        // where the design put it, and it is projected there - even when its own group's rows are drawn by GWT in
        // another container. That mixed group gets a react controller beside its GWT one (initializeControllers), so
        // the value has somewhere to be routed; the projection carries the entry and nothing else of the group.
        if (property.isList)
            return isReactOwned(property.groupObject);
        return getOwningReactContainer(property) != null;
    }

    // a reader React consumes (so GWT skips it): either it belongs to a react-owned component — its value is projected
    // into `data` — OR it is an lsf child's DESCRIPTOR reader (what isLsfViewDescriptorReader admits) projected into its
    // entry. The react container thus reaches into an lsf child's descriptor readers, even though the child keeps its
    // own GWT VALUE view (that draw reader is not owned and still reaches GWT).
    private boolean isReactOwned(GPropertyReader reader) {
        if (reactData == null)
            return false;
        if (reactData.isLsfViewDescriptorReader(reader)) // an lsf child's caption/image reader, projected into its entry
            return true;
        if (reader instanceof GPropertyDraw)
            return isReactOwned((GPropertyDraw) reader);
        if (reader instanceof GExtraPropertyReader)
            return isReactOwned(getProperty(((GExtraPropertyReader) reader).propertyID));
        if (reader instanceof GGroupObjectPropertyReader)
            return isReactOwned(getGroupObject(((GGroupObjectPropertyReader) reader).groupObjectID));
        if (reader instanceof GComponentReader)
            return isReactOwned(((GComponentReader) reader).getReaderComponent());
        return false;
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
        if (reactData != null && reactData.setCurrentObject(group, key)) // optimistic: reflect the new current in the projection now (reconciled later by fc.objects)
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
            if(actionSID == null)
                return;

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
        for (int i = 0; i < length; i++) {
            eventSources[i] = GEventSource.CUSTOM;
            PValue newValue = newValues[i];
            pushAsyncResults[i] = newValue == PValue.UNDEFINED ? null : new GPushAsyncInput(GUserInputResult.singleValue(newValue));
        }
        String actionSID = GEditBindingMap.changeOrGroupChange();
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
        asyncExecutePropertyEventAction(actionSID, editContext, execContext, handler, pushAsyncResult, eventSource, requestIndex -> {
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
        }, onExec);
    }


    public void asyncAddRemove(EditContext editContext, ExecContext execContext, EventHandler handler, String actionSID, GAsyncAddRemove asyncAddRemove, GPushAsyncInput pushAsyncResult, GEventSource eventSource, Consumer<Long> onExec) {
        final GObject object = form.getObject(asyncAddRemove.object);
        final boolean add = asyncAddRemove.add;

        GFormGroupController controller = getGroupController(object.groupObject);

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
        }, onExec);
    }

    public void changePropertyOrder(GPropertyDraw property, GGroupObjectValue columnKey, GOrder modiType) {
        syncResponseDispatch(new ChangePropertyOrder(property.ID, columnKey, modiType));
    }

    public void changePropertyOrder(int goID, LinkedHashMap<Integer, Boolean> orders) {
        GGroupObject groupObject = form.getGroupObject(goID);
        if (groupObject != null) {
            LinkedHashMap<GPropertyDraw, Boolean> pOrders = new LinkedHashMap<>();
            for (Integer propertyID : orders.keySet()) {
                GPropertyDraw propertyDraw = form.getProperty(propertyID);
                if (propertyDraw != null) {
                    Boolean value = orders.get(propertyID);
                    pOrders.put(propertyDraw, value);
                }
            }

            GGridController controller = controllers.get(groupObject);
            if (controller != null) // null for react-owned groups
                controller.changeOrders(pOrders, false);
        }
    }

    public void changePropertyFilters(int goID, List<GFilterAction.FilterItem> filters) {
        GGroupObject groupObject = form.getGroupObject(goID);
        if (groupObject != null) {
            GGridController gGridController = controllers.get(groupObject);
            if (gGridController == null) // react-owned groups have no base controller (no grid filter UI)
                return;
            List<GPropertyFilter> uFilters = new ArrayList<>();
            for (GFilterAction.FilterItem filter : filters) {
                GPropertyDraw propertyDraw = form.getProperty(filter.propertyId);
                if (propertyDraw != null) {
                    PValue value = null;
                    if (filter.value instanceof String) {
                        try {
                            value = propertyDraw.getFilterBaseType().parseString((String) filter.value, propertyDraw.getPattern());
                        } catch (ParseException ignored) {
                        }
                    } else {
                        value = PValue.convertFileValue(filter.value);
                    }
                    uFilters.add(GFilterController.createNewCondition(gGridController, new GFilter(propertyDraw), GGroupObjectValue.EMPTY, value, filter.negation, GCompare.get(filter.compare), filter.junction));
                }
            }

            gGridController.changeFilters(uFilters);
        }
    }

    public void setPropertyOrders(GGroupObject groupObject, List<Integer> propertyList, List<GGroupObjectValue> columnKeyList, List<Boolean> orderList) {
        syncResponseDispatch(new SetPropertyOrders(groupObject.ID, propertyList, columnKeyList, orderList));
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

    public long changeFilter(GGroupObject groupObject, ArrayList<GPropertyFilter> conditions) {
        currentFilters.put(groupObject, conditions);
        return applyCurrentFilters(Collections.singletonList(groupObject));
    }

    public long changeFilter(GTreeGroup treeGroup, ArrayList<GPropertyFilter> conditions) {
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
            currentFilters.put(group, groupFilters);
        }

        return applyCurrentFilters(treeGroup.groups);
    }

    private long applyCurrentFilters(List<GGroupObject> groups) {
        Map<Integer, List<GPropertyFilterDTO>> filters = new LinkedHashMap<>();
        for (GGroupObject group : groups) {
            List<GPropertyFilterDTO> groupFilters = new ArrayList<>();
            List<GPropertyFilter> gFilters = currentFilters.get(group);
            for (GPropertyFilter filter : gFilters) {
                if (!filter.property.isAction()) {
                    groupFilters.add(filter.getFilterDTO());
                }
            }
            filters.put(group.ID, groupFilters);
        }
        return asyncResponseDispatch(new SetUserFilters(filters));
    }

    public void setViewFilters(ArrayList<GPropertyFilter> conditions, int pageSize) {
        asyncResponseDispatch(new SetViewFilters(conditions.stream().map(GPropertyFilter::getFilterDTO).collect(Collectors.toCollection(ArrayList::new)), pageSize));
    }

    public void quickFilter(Event event, int initialFilterPropertyID) {
        GPropertyDraw propertyDraw = getProperty(initialFilterPropertyID);
        if (propertyDraw != null && controllers.containsKey(propertyDraw.groupObject)) {
            focusProperty(propertyDraw);
            controllers.get(propertyDraw.groupObject).quickEditFilter(event, propertyDraw, GGroupObjectValue.EMPTY);
        }
    }

    public void focusProperty(GPropertyDraw propertyDraw) {
        getReactablePropertyController(propertyDraw).focusProperty(propertyDraw); // react-owned -> GReactController no-op; non-react -> the real controller
    }

    public void setLoadingValueAt(GPropertyDraw property, GGroupObjectValue fullKey, PValue value, long requestIndex) {
        GGroupObjectValue fullCurrentKey = getFullCurrentKey(property, fullKey);
        Pair<GGroupObjectValue, PValue> propertyCell = getReactablePropertyController(property).setLoadingValueAt(property, fullCurrentKey, value);

        if(propertyCell != null) {
            pendingChangeProperty(property, propertyCell.first, value, propertyCell.second, requestIndex);
            pendingLoadingProperty(property, propertyCell.first, requestIndex);
        }
    }
    // push the react projection to the containers right after an optimistic mutation (current/property); the server
    // applyRemoteChanges later reconciles through the same reactData.update(fc) + updateReactContainers path
    private void refreshReactOptimistic() {
        syncControllers(); // an optimistic change can add or drop an entry too (a current object appearing, a
        formLayout.updateReactContainers(reactData); // panel value with it), and the members go with the data

        updateRowRenderers(); // an optimistically added row gets its editors now, not when the server confirms it
    }

    private void updateRowRenderers() {
        for (GRowPanelController rowPanelController : rowPanelControllers.values())
            rowPanelController.updateRowRenderers();
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
        if(component.isTab())
            ((TabbedContainerView)formLayout.getContainerView(component.container)).activateTab(component);
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
                GGridController controller = controllers.get(groupObject);
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
                GGridController controller = controllers.get(groupObject);
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
        for (GGridController controller : controllers.values()) {
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
        for (GRowPanelController rowPanelController : rowPanelControllers.values())
            rowPanelController.destroy();

        // the same for the table views (grid / pivot / calendar / custom / map and the tree table) - each of them
        // registers in that list too, and each of them holds this form controller
        for (GGridController controller : controllers.values())
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
        Element nextFocusElement = FocusUtils.getNextFocusElement(formLayout.getElement(), forward);
        if(nextFocusElement != null) {
            FocusUtils.focus(nextFocusElement, reason);
            return true;
        }
        return false;
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
        switch (binding.bindEditing) {
            case AUTO:
                return !(isEditing() && getEditElement().isOrHasChild(Element.as(event.getEventTarget())));
            case ALL:
                return true;
            case ONLY:
                return isEditing();
            case NO:
                return !isEditing();
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
    private long editRequestIndex = -1;

    private BiConsumer<GUserInputResult, CommitReason> editBeforeCommit;
    private BiConsumer<GUserInputResult, CommitReason> editAfterCommit;
    private Consumer<CancelReason> editCancel;

    private Element focusedElement;
    private Object forceSetFocus;

    public boolean isEditing() {
        return editContext != null;
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
