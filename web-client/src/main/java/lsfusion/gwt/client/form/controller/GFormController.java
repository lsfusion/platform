package lsfusion.gwt.client.form.controller;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.GForm;
import lsfusion.gwt.client.GFormChanges;
import lsfusion.gwt.client.GFormChangesDTO;
import lsfusion.gwt.client.action.GAction;
import lsfusion.gwt.client.action.GLogMessageAction;
import lsfusion.gwt.client.base.*;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.jsni.NativeSIDMap;
import lsfusion.gwt.client.base.result.ListResult;
import lsfusion.gwt.client.base.result.NumberResult;
import lsfusion.gwt.client.base.result.VoidResult;
import lsfusion.gwt.client.base.view.CollapsiblePanel;
import lsfusion.gwt.client.base.view.DialogBoxHelper;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.base.view.WindowHiddenHandler;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.classes.GObjectClass;
import lsfusion.gwt.client.classes.GType;
import lsfusion.gwt.client.controller.dispatch.GwtActionDispatcher;
import lsfusion.gwt.client.controller.remote.DeferredRunner;
import lsfusion.gwt.client.controller.remote.action.*;
import lsfusion.gwt.client.controller.remote.action.form.*;
import lsfusion.gwt.client.controller.remote.action.logics.GenerateID;
import lsfusion.gwt.client.controller.remote.action.logics.GenerateIDResult;
import lsfusion.gwt.client.controller.remote.action.navigator.GainedFocus;
import lsfusion.gwt.client.form.GUpdateMode;
import lsfusion.gwt.client.form.classes.view.ClassChosenHandler;
import lsfusion.gwt.client.form.classes.view.GClassDialog;
import lsfusion.gwt.client.form.controller.dispatch.FormDispatchAsync;
import lsfusion.gwt.client.form.controller.dispatch.GFormActionDispatcher;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.design.view.CustomContainerView;
import lsfusion.gwt.client.form.design.view.GAbstractContainerView;
import lsfusion.gwt.client.form.design.view.GFormLayout;
import lsfusion.gwt.client.form.design.view.TabbedContainerView;
import lsfusion.gwt.client.form.event.*;
import lsfusion.gwt.client.form.filter.GRegularFilter;
import lsfusion.gwt.client.form.filter.GRegularFilterGroup;
import lsfusion.gwt.client.form.filter.user.GPropertyFilter;
import lsfusion.gwt.client.form.filter.user.GPropertyFilterDTO;
import lsfusion.gwt.client.form.filter.user.view.GFilterConditionView;
import lsfusion.gwt.client.form.object.*;
import lsfusion.gwt.client.form.object.panel.controller.GPanelController;
import lsfusion.gwt.client.form.object.table.controller.GAbstractTableController;
import lsfusion.gwt.client.form.object.table.controller.GPropertyController;
import lsfusion.gwt.client.form.object.table.grid.controller.GGridController;
import lsfusion.gwt.client.form.object.table.grid.user.design.GColumnUserPreferences;
import lsfusion.gwt.client.form.object.table.grid.user.design.GFormUserPreferences;
import lsfusion.gwt.client.form.object.table.grid.user.design.GGridUserPreferences;
import lsfusion.gwt.client.form.object.table.grid.user.design.GGroupObjectUserPreferences;
import lsfusion.gwt.client.form.object.table.grid.view.GListViewType;
import lsfusion.gwt.client.form.object.table.tree.GTreeGroup;
import lsfusion.gwt.client.form.object.table.tree.controller.GTreeGroupController;
import lsfusion.gwt.client.form.order.user.GOrder;
import lsfusion.gwt.client.form.property.GExtraPropertyReader;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.GPropertyGroupType;
import lsfusion.gwt.client.form.property.GPropertyReader;
import lsfusion.gwt.client.form.property.async.*;
import lsfusion.gwt.client.form.property.cell.GEditBindingMap;
import lsfusion.gwt.client.form.property.cell.classes.controller.CustomReplaceCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.controller.RequestCellEditor;
import lsfusion.gwt.client.form.property.cell.controller.*;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;
import lsfusion.gwt.client.form.property.cell.view.GUserInputResult;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;
import lsfusion.gwt.client.form.property.panel.view.ActionPanelRenderer;
import lsfusion.gwt.client.form.property.table.view.GPropertyContextMenuPopup;
import lsfusion.gwt.client.form.view.FormContainer;
import lsfusion.gwt.client.form.view.FormDockable;
import lsfusion.gwt.client.form.view.ModalForm;
import lsfusion.gwt.client.navigator.controller.GAsyncFormController;
import lsfusion.gwt.client.navigator.window.GModalityType;
import lsfusion.gwt.client.view.MainFrame;
import lsfusion.gwt.client.view.StyleDefaults;
import net.customware.gwt.dispatch.shared.Result;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static lsfusion.gwt.client.base.GwtClientUtils.*;
import static lsfusion.gwt.client.base.GwtSharedUtils.putToDoubleNativeMap;
import static lsfusion.gwt.client.base.GwtSharedUtils.removeFromDoubleMap;
import static lsfusion.gwt.client.base.view.ColorUtils.getDisplayColor;
import static lsfusion.gwt.client.form.property.cell.GEditBindingMap.CHANGE;
import static lsfusion.gwt.client.form.property.cell.GEditBindingMap.isChangeEvent;

public class GFormController implements EditManager {
    private FormDispatchAsync dispatcher;

    private final GFormActionDispatcher actionDispatcher;

    private final FormsController formsController;
    private final FormContainer formContainer;

    private final GForm form;
    public final GFormLayout formLayout;

    private final boolean isDialog;

    private Event editEvent;

    private final NativeSIDMap<GGroupObject, ArrayList<GGroupObjectValue>> currentGridObjects = new NativeSIDMap<>();

    public NativeSIDMap<GGroupObject, ArrayList<GGroupObjectValue>> getCurrentGridObjects() {
        return currentGridObjects;
    }

    private final NativeSIDMap<GGroupObject, ArrayList<GPropertyFilter>> currentFilters = new NativeSIDMap<>();

    private final LinkedHashMap<GGroupObject, GGridController> controllers = new LinkedHashMap<>();
    private final LinkedHashMap<GTreeGroup, GTreeGroupController> treeControllers = new LinkedHashMap<>();
    public GPanelController panelController;

    private final NativeSIDMap<GGroupObject, ArrayList<Widget>> filterViews = new NativeSIDMap<>();

    private final LinkedHashMap<Long, ModifyObject> pendingModifyObjectRequests = new LinkedHashMap<>();
    private final NativeSIDMap<GGroupObject, Long> pendingChangeCurrentObjectsRequests = new NativeSIDMap<>();
    private final NativeSIDMap<GPropertyDraw, NativeHashMap<GGroupObjectValue, Change>> pendingChangePropertyRequests = new NativeSIDMap<>(); // assert that should contain columnKeys + list keys if property is in list
    private final NativeSIDMap<GPropertyDraw, NativeHashMap<GGroupObjectValue, Long>> pendingLoadingPropertyRequests = new NativeSIDMap<>(); // assert that should contain columnKeys + list keys if property is in list
    private final NativeSIDMap<GFilterConditionView, Long> pendingLoadingFilterRequests = new NativeSIDMap<>();

    private boolean hasColumnGroupObjects;

    private static Timer linkEditModeTimer;

    public FormsController getFormsController() {
        return formsController;
    }

    public GFormController(FormsController formsController, FormContainer formContainer, GForm gForm, boolean isDialog, boolean autoSize, Event editEvent) {
        actionDispatcher = new GFormActionDispatcher(this);

        this.formsController = formsController;
        this.formContainer = formContainer;
        this.form = gForm;
        this.isDialog = isDialog;

        dispatcher = new FormDispatchAsync(this);

        formLayout = new GFormLayout(this, form.mainContainer, autoSize);
        if (form.sID != null)
            formLayout.getElement().setAttribute("lsfusion-form", form.sID);

        this.editEvent = editEvent;

        updateFormCaption();

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

        initializeAutoRefresh();

        initLinkEditModeTimer();
    }

    public void checkGlobalMouseEvent(Event event) {
        checkFormEvent(event, (handler, preview) -> checkMouseEvent(handler, preview, null, false, true));
    }

    public Event popEditEvent() {
        Event result = editEvent;
        editEvent = null;
        return result;
    }

    private interface CheckEvent {
        void accept(EventHandler handler, boolean preview);
    }
    private static void checkFormEvent(Event event, CheckEvent preview) {
        EventHandler handler = new EventHandler(event);

        preview.accept(handler, true); // the problem is that now we check preview twice (however it's not that big overhead, so so far will leave it this way)
        if(handler.consumed)
            return;

        preview.accept(handler, false);
    }

    public void checkMouseEvent(EventHandler handler, boolean preview, Element cellParent, boolean panel, boolean stopPreventingDblclickEvent) {
        if(GMouseStroke.isDblDownEvent(handler.event) && !stopPreventingDblclickEvent && !isEditing())
            handler.event.preventDefault(); //need to prevent selection by double mousedown event
        else if(GMouseStroke.isChangeEvent(handler.event) || GMouseStroke.isDoubleChangeEvent(handler.event))
            processBinding(handler, preview, cellParent, panel);
    }
    public void checkKeyEvent(EventHandler handler, boolean preview, Element cellParent, boolean panel) {
        if(GKeyStroke.isKeyEvent(handler.event))
            processBinding(handler, preview, cellParent, panel);
    }
    private static void checkGlobalKeyEvent(DomEvent event, Supplier<GFormController> currentForm) {
        NativeEvent nativeEvent = event.getNativeEvent();
        if(nativeEvent instanceof Event) { // just in case
            GFormController form = currentForm.get();
            if(form != null)
                checkFormEvent((Event) nativeEvent, (handler, preview) -> form.checkKeyEvent(handler, preview, null, false));
        }
    }
    public void checkMouseKeyEvent(EventHandler handler, boolean preview, Element cellParent, boolean panel, boolean customRenderer) {
        if(MainFrame.isModalPopup())
            return;

        checkMouseEvent(handler, preview, cellParent, panel, customRenderer);
        if(handler.consumed)
            return;

        checkKeyEvent(handler, preview, cellParent, panel);
    }

    public static void checkKeyEvents(DomEvent event, FormsController formsController) {
        NativeEvent nativeEvent = event.getNativeEvent();
        checkLinkEditModeEvents(formsController, nativeEvent);

        if(GKeyStroke.isSwitchFullScreenModeEvent(nativeEvent) && !MainFrame.mobile) {
            formsController.switchFullScreenMode();
        }
    }

    // we need native method and not getCtrlKey, since some events (for example focus) have ctrlKey undefined and in this case we want to ignore them
    private static native Boolean eventGetCtrlKey(NativeEvent evt) /*-{
        return evt.ctrlKey;
    }-*/;

    private static native Boolean eventGetShiftKey(NativeEvent evt) /*-{
        return evt.shiftKey;
    }-*/;
    
    private static native Boolean eventGetAltKey(NativeEvent evt) /*-{
        return evt.altKey;
    }-*/;

    private static boolean pressedCtrl = false;
    private void initLinkEditModeTimer() {
        if(linkEditModeTimer == null) {
            linkEditModeTimer = new Timer() {
                @Override
                public void run() {
                    if (pressedCtrl) {
                        pressedCtrl = false;
                    } else {
                        if (GFormController.isLinkEditModeWithCtrl()) {
                            formsController.updateLinkEditMode(false, false);
                        }
                    }
                }
            };
            linkEditModeTimer.scheduleRepeating(500); //delta between first and second events ~500ms, between next ~30ms
        }
    }

    public static void checkLinkEditModeEvents(FormsController formsController, NativeEvent event) {
        Boolean ctrlKey = eventGetCtrlKey(event);
        if(ctrlKey != null) {
            Boolean shiftKey = eventGetShiftKey(event);
            Boolean altKey = eventGetAltKey(event);
            boolean onlyCtrl = ctrlKey && (shiftKey == null || !shiftKey) && (altKey == null || !altKey);
            pressedCtrl = onlyCtrl;
            if (!onlyCtrl && GFormController.isLinkEditModeWithCtrl())
                formsController.updateLinkEditMode(false, false);
            if (onlyCtrl && !GFormController.isLinkEditMode())
                formsController.updateLinkEditMode(true, true);
        }
    }

    private static boolean linkEditMode;
    private static boolean linkEditModeWithCtrl;
    public static boolean isLinkEditMode() {
        return linkEditMode;
    }
    public static boolean isLinkEditModeWithCtrl() {
        return linkEditModeWithCtrl;
    }
    public static void setLinkEditMode(boolean enabled, boolean enabledWithCtrl) {
        linkEditMode = enabled;
        linkEditModeWithCtrl = enabledWithCtrl;
    }

    public static Timer linkEditModeStylesTimer;

    public static void scheduleLinkEditModeStylesTimer(Runnable setLinkEditModeStyles) {
        if(linkEditModeStylesTimer == null) {
            linkEditModeStylesTimer = new Timer() {
                @Override
                public void run() {
                    setLinkEditModeStyles.run();
                    linkEditModeStylesTimer = null;
                }
            };
            linkEditModeStylesTimer.schedule(250);
        }
    }

    public static void cancelLinkEditModeStylesTimer() {
        if (linkEditModeStylesTimer != null) {
            linkEditModeStylesTimer.cancel();
            linkEditModeStylesTimer = null;
        }
    }

    // will handle key events in upper container which will be better from UX point of view
    public static void initKeyEventHandler(Widget widget, FormsController formsController, Supplier<GFormController> currentForm) {
        widget.addDomHandler(event -> {
            checkGlobalKeyEvent(event, currentForm);
            checkKeyEvents(event, formsController);
        }, KeyDownEvent.getType());
        widget.addDomHandler(event -> checkGlobalKeyEvent(event, currentForm), KeyPressEvent.getType());
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
            } else {
                createMultipleFilterComponent(filterGroup);
            }
        }
    }

    private void createSingleFilterComponent(final GRegularFilterGroup filterGroup, final GRegularFilter filter) {
        final CheckBox filterCheck = new CheckBox(filter.getFullCaption());
        filterCheck.setValue(false);
        filterCheck.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> e) {
                setRegularFilter(filterGroup, e.getValue() != null && e.getValue() ? filter : null);
            }
        });
        filterCheck.addStyleName("checkBoxFilter");
        addFilterView(filterGroup, filterCheck);

        if (filterGroup.defaultFilterIndex >= 0) {
            filterCheck.setValue(true, false);
        }

        filterCheck.getElement().setPropertyObject("groupObject", filterGroup.groupObject);
        if (filter.key != null)
            addBinding(new GKeyInputEvent(filter.key), (event) -> filterCheck.setValue(!filterCheck.getValue(), true), filterCheck, filterGroup.groupObject);
    }

    private void createMultipleFilterComponent(final GRegularFilterGroup filterGroup) {
        final ListBox filterBox = new ListBox();
        filterBox.setMultipleSelect(false);
        filterBox.addItem("(" + ClientMessages.Instance.get().multipleFilterComponentAll() + ")", "-1");

        ArrayList<GRegularFilter> filters = filterGroup.filters;
        for (int i = 0; i < filters.size(); i++) {
            final GRegularFilter filter = filters.get(i);
            filterBox.addItem(filter.getFullCaption(), "" + i);

            final int filterIndex = i;
            filterBox.getElement().setPropertyObject("groupObject", filterGroup.groupObject);
            if (filter.key != null)
                addBinding(new GKeyInputEvent(filter.key), (event) -> {
                    filterBox.setSelectedIndex(filterIndex + 1);
                    setRegularFilter(filterGroup, filterIndex);
                }, filterBox, filterGroup.groupObject);
        }

        filterBox.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                setRegularFilter(filterGroup, filterBox.getSelectedIndex() - 1);
            }
        });

        filterBox.addStyleName("comboBoxFilter");
        filterBox.setHeight(StyleDefaults.COMPONENT_HEIGHT_STRING);

        addFilterView(filterGroup, filterBox);
        if (filterGroup.defaultFilterIndex >= 0) {
            filterBox.setSelectedIndex(filterGroup.defaultFilterIndex + 1);
        }
    }

    private void addFilterView(GRegularFilterGroup filterGroup, Widget filterWidget) {
        formLayout.addBaseComponent(filterGroup, filterWidget, null);

        // need this to hide / show regular filters when group object is not visible
        if (filterGroup.groupObject != null)
            filterViews.computeIfAbsent(filterGroup.groupObject, k -> new ArrayList<>()).add(filterWidget);
    }

    public void setFiltersVisible(GGroupObject groupObject, boolean visible) {
        List<Widget> groupFilters = filterViews.get(groupObject);
        if (groupFilters != null)
            for (Widget filterView : groupFilters)
                filterView.setVisible(visible);
    }

    private void setRegularFilter(GRegularFilterGroup filterGroup, int index) {
        setRemoteRegularFilter(filterGroup, index == -1 ? null : filterGroup.filters.get(index));
    }

    private void setRegularFilter(GRegularFilterGroup filterGroup, GRegularFilter filter) {
        setRemoteRegularFilter(filterGroup, filter);
    }

    private void initializeControllers() {
        for (GTreeGroup treeGroup : form.treeGroups) {
            initializeTreeController(treeGroup);
        }

        for (GGroupObject group : form.groupObjects) {
            if (group.parent == null) {
                initializeGroupController(group);
            }
        }

        panelController = new GPanelController(this);
    }

    private void initializeParams() {
        hasColumnGroupObjects = false;
        for (GPropertyDraw property : getPropertyDraws()) {
            if (property.hasColumnGroupObjects()) {
                hasColumnGroupObjects = true;
            }

            GGroupObject groupObject = property.groupObject;
            if(groupObject != null && property.isList && !property.hide && groupObject.columnCount < 10) {
                GFont font = groupObject.grid.font;
                groupObject.columnSumWidth += property.getValueWidthWithPadding(font);
                groupObject.columnCount++;
                groupObject.rowMaxHeight = Math.max(groupObject.rowMaxHeight, property.getValueHeightWithPadding(font));
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
            getGroupObjectController(groupObject).changeOrders(groupObject, entry.getValue(), true);
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

    public List<List<GPropertyDraw>> getPivotColumns(GGroupObject groupObject) {
        return form.getPivotColumns(groupObject);
    }

    public List<List<GPropertyDraw>> getPivotRows(GGroupObject groupObject) {
        return form.getPivotRows(groupObject);
    }

    public List<GPropertyDraw> getPivotMeasures(GGroupObject groupObject) {
        return form.getPivotMeasures(groupObject);
    }

    public void executeNotificationAction(final Integer idNotification) throws IOException {
        syncResponseDispatch(new ExecuteNotification(idNotification));
    }

    private void initializeAutoRefresh() {
        if (form.autoRefresh > 0) {
            scheduleRefresh();
        }
    }

    public Widget getWidget() {
        return formLayout;
    }

    private void scheduleRefresh() {
        Scheduler.get().scheduleFixedPeriod(new Scheduler.RepeatingCommand() {
            @Override
            public boolean execute() {
                if (!formHidden) {
                    if (isShowing(getWidget())) {
                        asyncDispatch(new GetRemoteChanges(true, false), new ServerResponseCallback() {
                            @Override
                            public void onSuccess(ServerResponseResult response, Runnable onDispatchFinished) {
                                super.onSuccess(response, onDispatchFinished);
                                if (!formHidden) {
                                    scheduleRefresh();
                                }
                            }
                        });
                    } else {
                        scheduleRefresh();
                    }
                }
                return false;
            }
        }, form.autoRefresh * 1000);
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

    private boolean formVisible = true;

    public void gainedFocus() {
        asyncResponseDispatch(new GainedFocus());
        formVisible = true;
    }

    public void lostFocus() {
        formVisible = false;
    }

    public void applyRemoteChanges(GFormChangesDTO changesDTO) {
        GFormChanges fc = GFormChanges.remap(form, changesDTO);

        if (hasColumnGroupObjects) // optimization
            fc.gridObjects.foreachEntry((key, value) -> currentGridObjects.put(key, value));

        int requestIndex = changesDTO.requestIndex;

        modifyFormChangesWithModifyObjectAsyncs(requestIndex, fc);

        modifyFormChangesWithChangeCurrentObjectAsyncs(requestIndex, fc);

        modifyFormChangesWithChangePropertyAsyncs(requestIndex, fc);

        modifyFormChangesWithLoadingPropertyAsyncs(requestIndex, fc);

        applyLoadingFilterAsyncs(requestIndex, fc);

        applyKeyChanges(fc);

        applyPropertyChanges(fc);

        update(fc, requestIndex);

        expandCollapseContainers(fc);

        formLayout.update(requestIndex);

        activateElements(fc);

        formLayout.onResize();
    }

    public void applyKeyChanges(GFormChanges fc) {
        fc.gridObjects.foreachEntry((key, value) ->
            getGroupObjectController(key).updateKeys(key, value, fc));

        fc.objects.foreachEntry((key, value) ->
            getGroupObjectController(key).updateCurrentKey(value));
    }

    private void applyPropertyChanges(GFormChanges fc) {
        fc.dropProperties.forEach(property -> {
            GPropertyController controller = getPropertyController(property);
            if (controller.isPropertyShown(property)) // drop properties sent without checking if it was sent for update at least once, so it's possible when drop is sent for property that has not been added
                controller.removeProperty(property);
        });

        // first proceed property with its values, then extra values (some views, for example GPivot use updated properties)
        updatePropertyChanges(fc, key -> !(key instanceof GExtraPropertyReader));
        updatePropertyChanges(fc, key -> key instanceof GExtraPropertyReader);
    }

    private void updatePropertyChanges(GFormChanges fc, Predicate<GPropertyReader> filter) {
        fc.properties.foreachEntry((key, value) -> {
            if(filter.test(key))
                key.update(this, value, key instanceof GPropertyDraw && fc.updateProperties.contains((GPropertyDraw)key));
        });
    }

    public void update(GFormChanges fc, int requestIndex) {
        for (GGridController controller : controllers.values())
            controller.update(requestIndex, fc);

        for (GTreeGroupController treeController : treeControllers.values())
            treeController.update();

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
            Widget childWidget = parentContainerView.getChildView(container);
            if (childWidget instanceof CollapsiblePanel) {
                ((CollapsiblePanel) childWidget).setCollapsed(collapsed);
            }
        }
    }

    private void modifyFormChangesWithModifyObjectAsyncs(final int currentDispatchingRequestIndex, GFormChanges fc) {
        for (Iterator<Map.Entry<Long, ModifyObject>> iterator = pendingModifyObjectRequests.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<Long, ModifyObject> cell = iterator.next();
            if (cell.getKey() <= currentDispatchingRequestIndex) {
                iterator.remove();

                ModifyObject modifyObject = cell.getValue();
                GGroupObject groupObject = modifyObject.object.groupObject;
                // делаем обратный modify, чтобы удалить/добавить ряды, асинхронно добавленные/удалённые на клиенте, если с сервера не пришло подтверждение
                // возможны скачки и путаница в строках на удалении, если до прихода ответа position утратил свою актуальность
                // по этой же причине не заморачиваемся запоминанием соседнего объекта
                if(!fc.gridObjects.containsKey(groupObject)) {
                    controllers.get(groupObject).modifyGroupObject(modifyObject.value, !modifyObject.add, modifyObject.position);
                }
            }
        }

        for (Map.Entry<Long, ModifyObject> e : pendingModifyObjectRequests.entrySet()) {
            ArrayList<GGroupObjectValue> gridObjects = fc.gridObjects.get(e.getValue().object.groupObject);
            if (gridObjects != null) {
                if (e.getValue().add) {
                    gridObjects.add(e.getValue().value);
                } else {
                    gridObjects.remove(e.getValue().value);
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

                if(getPropertyController(property).isPropertyShown(property) && !fc.dropProperties.contains(property)) {
                    NativeHashMap<GGroupObjectValue, Object> propertyValues = fc.properties.get(property);
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
            final NativeHashMap<GGroupObjectValue, Object> propertyValues = fc.properties.get(property);
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

                if(getPropertyController(property).isPropertyShown(property) && !fc.dropProperties.contains(property)) {
                    NativeHashMap<GGroupObjectValue, Object> propertyLoadings = fc.properties.get(property.loadingReader);
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

    public GPropertyController getPropertyController(GPropertyDraw property) {
        if(property.isList) {
            return getGroupObjectController(property.groupObject);
        } else
            return panelController;
    }

    public void openForm(Long requestIndex, GForm form, GModalityType modalityType, boolean forbidDuplicate, Event editEvent, EditContext editContext, final WindowHiddenHandler handler) {
        boolean isDockedModal = modalityType == GModalityType.DOCKED_MODAL;
        if (isDockedModal)
            ((FormDockable)formContainer).block();

        FormContainer blockingForm = formsController.openForm(getAsyncFormController(requestIndex), form, modalityType, forbidDuplicate, editEvent, editContext, this, () -> {
            if(isDockedModal) {
                ((FormDockable)formContainer).unblock();

                formsController.selectTab((FormDockable) formContainer);
            } else if(modalityType == GModalityType.DOCKED)
                formsController.ensureTabSelected();

            handler.onHidden();
        });

        if (isDockedModal)
            ((FormDockable)formContainer).setBlockingForm((FormDockable) blockingForm);
    }

    public void onServerInvocationResponse(ServerResponseResult response) {
        formsController.onServerInvocationResponse(response, getAsyncFormController(response.requestIndex));
    }

    public void showClassDialog(GObjectClass baseClass, GObjectClass defaultClass, boolean concreate, final ClassChosenHandler classChosenHandler) {
        GClassDialog.showDialog(baseClass, defaultClass, concreate, classChosenHandler);
    }

    public long changeGroupObject(final GGroupObject group, GGroupObjectValue key) {
        long requestIndex = asyncDispatch(new ChangeGroupObject(group.ID, key), new ServerResponseCallback() {
            @Override
            public void onSuccess(ServerResponseResult response, Runnable onDispatchFinished) {
                // not sure what for it was used
                // DeferredRunner.get().commitDelayedGroupObjectChange(group);

                super.onSuccess(response, onDispatchFinished);
            }
        });
        pendingChangeCurrentObjectsRequests.put(group, requestIndex);
        return requestIndex;
    }

    // has to be called setCurrentKey before
    public void changeGroupObjectLater(final GGroupObject group, final GGroupObjectValue key) {
        DeferredRunner.get().scheduleGroupObjectChange(group, new DeferredRunner.AbstractCommand() {
            @Override
            public void execute() {
                changeGroupObject(group, key);
            }
        });
    }

    public void pasteExternalTable(ArrayList<GPropertyDraw> propertyList, ArrayList<GGroupObjectValue> columnKeys, List<List<String>> table) {
        ArrayList<ArrayList<Object>> values = new ArrayList<>();

        int propertyColumns = propertyList.size();
        for (List<String> sRow : table) {
            ArrayList<Object> valueRow = new ArrayList<>();

            for (int i = 0; i < propertyColumns; i++) {
                GPropertyDraw property = propertyList.get(i);
                String sCell = i < sRow.size() ? sRow.get(i) : null;
                valueRow.add(property.parsePaste(sCell, property.getExternalChangeType()));
            }
            values.add(valueRow);
        }

        final ArrayList<Integer> propertyIdList = new ArrayList<>();
        for (GPropertyDraw propertyDraw : propertyList) {
            propertyIdList.add(propertyDraw.ID);
        }

        syncResponseDispatch(new PasteExternalTable(propertyIdList, columnKeys, values));
    }

    public void pasteValue(ExecuteEditContext editContext, String sValue) {
        GPropertyDraw property = editContext.getProperty();
        Serializable value = (Serializable) property.parsePaste(sValue, property.getExternalChangeType());

        changeProperty(editContext, new GUserInputResult(value));
    }

    public void changePageSizeAfterUnlock(final GGroupObject groupObject, final int pageSize) {
        Scheduler.get().scheduleFixedPeriod(new Scheduler.RepeatingCommand() {
            @Override
            public boolean execute() {
                if (dispatcher.loadingManager.isVisible()) {
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

    public void scrollToEnd(GGroupObject group, boolean toEnd) {
        syncResponseDispatch(new ScrollToEnd(group.ID, toEnd));
    }

    public void executePropertyEventAction(EventHandler handler, boolean isBinding, ExecuteEditContext editContext) {
        Event event = handler.event;
        GPropertyDraw property = editContext.getProperty();
        if(property == null)  // in tree there can be no property in groups other than last
            return;

        if(BrowserEvents.CONTEXTMENU.equals(event.getType())) {
            handler.consume();
            GPropertyContextMenuPopup.show(property, event.getClientX(), event.getClientY(), actionSID -> {
                executePropertyEventAction(editContext, actionSID, event);
            });
        } else {
            String actionSID;
            if(isBinding) {
                if(GKeyStroke.isKeyEvent(event)) // we don't want to set focus on mouse binding (it's pretty unexpected behaviour)
                    editContext.trySetFocus(); // we want element to be focused on key binding (if it's possible)
                actionSID = CHANGE;
            } else {
                actionSID = property.getEventSID(event);
                if(actionSID == null)
                    return;
            }

            // hasChangeAction check is important for quickfilter not to consume event (however with propertyReadOnly, checkCanBeChanged there will be still some problems)
            if (isChangeEvent(actionSID) && (editContext.isReadOnly() || !property.hasUserChangeAction()))
                return;
            if(GEditBindingMap.EDIT_OBJECT.equals(actionSID) && !property.hasEditObjectAction)
                return;

            handler.consume();

            executePropertyEventAction(editContext, actionSID, event);
        }
    }

    public void executePropertyEventAction(ExecuteEditContext editContext, String actionSID, Event event) {
        GPropertyDraw property = editContext.getProperty();
        if (isChangeEvent(actionSID) && property.askConfirm) {
            blockingConfirm("lsFusion", property.askConfirmMessage, false, chosenOption -> {
                if (chosenOption == DialogBoxHelper.OptionType.YES)
                    executePropertyEventActionConfirmed(editContext, actionSID, event);
            });
        }
        executePropertyEventActionConfirmed(editContext, actionSID, event);
    }

    public void executePropertyEventActionConfirmed(ExecuteEditContext editContext, String actionSID, Event event) {
        executePropertyEventAction(event, editContext, editContext, actionSID, requestIndex -> setLoading(editContext, requestIndex));
    }

    public void executePropertyEventAction(Event event, EditContext editContext, ExecContext execContext, String actionSID, Consumer<Long> onExec) {
        executePropertyEventAction(execContext.getProperty().getAsyncEventExec(actionSID), event, editContext, execContext, actionSID, null, false, onExec);
    }

    private void executePropertyEventAction(GAsyncEventExec asyncEventExec, Event event, EditContext editContext, ExecContext execContext, String actionSID, GPushAsyncInput pushAsyncResult, boolean externalChange, Consumer<Long> onExec) {
        if (asyncEventExec != null)
            asyncEventExec.exec(this, event, editContext, execContext, actionSID, pushAsyncResult, externalChange, onExec);
        else
            syncExecutePropertyEventAction(editContext, event, execContext.getProperty(), execContext.getFullKey(), pushAsyncResult, actionSID, onExec);
    }

    public void asyncExecutePropertyEventAction(String actionSID, EditContext editContext, ExecContext execContext, Event editEvent, GPushAsyncResult pushAsyncResult, boolean externaChange, Consumer<Long> onRequestExec, Consumer<Long> onExec) {
        long requestIndex = asyncExecutePropertyEventAction(actionSID, editContext, editEvent, new GPropertyDraw[]{execContext.getProperty()}, new boolean[]{externaChange}, new GGroupObjectValue[]{execContext.getFullKey()}, new GPushAsyncResult[]{pushAsyncResult});

        onExec.accept(requestIndex);

        // should be after because for example onExec can setRemoteValue, but onRequestExec also does that and should have higher priority
        onRequestExec.accept(requestIndex);
    }

    public void syncExecutePropertyEventAction(EditContext editContext, Event editEvent, GPropertyDraw property, GGroupObjectValue fullKey, GPushAsyncInput pushAsyncResult, String actionSID, Consumer<Long> onExec) {
        long requestIndex = executePropertyEventAction(actionSID, true, editContext, editEvent, new GPropertyDraw[]{property}, new boolean[]{false}, new GGroupObjectValue[]{fullKey}, new GPushAsyncResult[] {pushAsyncResult});

        onExec.accept(requestIndex);
    }

    public long asyncExecutePropertyEventAction(String actionSID, EditContext editContext, Event editEvent, GPropertyDraw[] properties, boolean[] externalChanges, GGroupObjectValue[] fullKeys, GPushAsyncResult[] pushAsyncResults) {
        return executePropertyEventAction(actionSID, false, editContext, editEvent, properties, externalChanges, fullKeys, pushAsyncResults);
    }

    private long executePropertyEventAction(String actionSID, boolean sync, EditContext editContext, Event editEvent, GPropertyDraw[] properties, boolean[] externalChanges, GGroupObjectValue[] fullKeys, GPushAsyncResult[] pushAsyncResults) {
        int length = properties.length;
        int[] IDs = new int[length];
        GGroupObjectValue[] fullCurrentKeys = new GGroupObjectValue[length];
        for (int i = 0; i < length; i++) {
            GPropertyDraw property = properties[i];
            IDs[i] = property.ID;
            fullCurrentKeys[i] = getFullCurrentKey(property, fullKeys[i]);
        }

        ExecuteEventAction executeEventAction = new ExecuteEventAction(IDs, fullCurrentKeys, actionSID, externalChanges, pushAsyncResults);
        ServerResponseCallback serverResponseCallback = new ServerResponseCallback() {
            @Override
            protected Runnable getOnRequestFinished() {
                return () -> {
                    actionDispatcher.editContext = null;
                    actionDispatcher.editEvent = null;
                };
            }

            @Override
            public void onSuccess(ServerResponseResult response, Runnable onDispatchFinished) {
                actionDispatcher.editContext = editContext;
                actionDispatcher.editEvent = editEvent;
                super.onSuccess(response, onDispatchFinished);
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

    public void asyncChange(Event event, EditContext editContext, String actionSID, GAsyncInput asyncChange, Consumer<Long> onExec) {
        GInputList inputList = asyncChange.inputList;
        edit(asyncChange.changeType, event, false, null, inputList, (value, onRequestExec) -> {
            asyncChange(event, editContext, inputList, value, actionSID, false, requestIndex -> {
                onExec.accept(requestIndex); // setLoading

                // doing that after to set the last value (onExec recursively can also set value)
                onRequestExec.accept(requestIndex); // pendingChangeProperty
            });
        }, cancelReason -> {}, editContext, actionSID, asyncChange.customEditFunction);
    }

    private final static GAsyncNoWaitExec asyncExec = new GAsyncNoWaitExec();

    private void asyncChange(Event event, EditContext editContext, GInputList inputList, GUserInputResult value, String actionSID, boolean externalChange, Consumer<Long> onExec) {
        Integer contextAction = value.getContextAction();
        executePropertyEventAction(contextAction != null ? inputList.actions[contextAction].asyncExec : asyncExec,
                event, editContext, editContext, actionSID, new GPushAsyncInput(value), externalChange, onExec);
    }

    public void asyncOpenForm(GAsyncOpenForm asyncOpenForm, EditContext editContext, ExecContext execContext, Event editEvent, String actionSID, GPushAsyncInput pushAsyncResult, boolean externalChange, Consumer<Long> onExec) {
        asyncExecutePropertyEventAction(actionSID, editContext, execContext, editEvent, pushAsyncResult, externalChange, requestIndex -> {
            formsController.asyncOpenForm(getAsyncFormController(requestIndex), asyncOpenForm, editEvent, editContext, execContext, this);
        }, onExec);
    }

    public GAsyncFormController getAsyncFormController(long requestIndex) {
        return actionDispatcher.getAsyncFormController(requestIndex);
    }

    public void continueServerInvocation(long requestIndex, Object[] actionResults, int continueIndex, RequestAsyncCallback<ServerResponseResult> callback) {
        syncDispatch(new ContinueInvocation(requestIndex, actionResults, continueIndex), callback, true);
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

        for (GGridController group : controllers.values()) {
            fullCurrentKey.putAll(group.getCurrentKey());
        }

        for (GTreeGroupController tree : treeControllers.values()) {
            GGroupObjectValue currentPath = tree.getCurrentKey();
            if (currentPath != null) {
                fullCurrentKey.putAll(currentPath);
            }
        }

        fullCurrentKey.putAll(fullKey);

        return fullCurrentKey.toGroupObjectValue();
    }

    // for custom renderer, paste, quick access
    public void changeProperty(ExecuteEditContext editContext, GUserInputResult result) {
        GPropertyDraw property = editContext.getProperty();
        lsfusion.gwt.client.base.Result<Object> oldValue = setLocalValue(editContext, property.getExternalChangeType(), result);

        asyncChange(null, editContext, property.getInputList(), result, CHANGE, true, requestIndex -> {
            setRemoteValue(editContext, oldValue, result, requestIndex);

            setLoading(editContext, requestIndex);
        });
    }

    public lsfusion.gwt.client.base.Result<Object> setLocalValue(EditContext editContext, GType type, GUserInputResult result) {
        if(editContext.canUseChangeValueForRendering(type)) {
            Object oldValue = editContext.getValue();

            editContext.setValue(result.getValue());

            return new lsfusion.gwt.client.base.Result<>(oldValue);
        }
        return null;
    }

    public void setRemoteValue(EditContext editContext, lsfusion.gwt.client.base.Result<Object> oldValue, GUserInputResult result, long requestIndex) {
        if(oldValue != null)
            pendingChangeProperty(editContext.getProperty(), editContext.getFullKey(), result.getValue(), oldValue.result, requestIndex);
    }

    public void pendingChangeProperty(GPropertyDraw property, GGroupObjectValue fullKey, Serializable value, Object oldValue, long changeRequestIndex) {
        putToDoubleNativeMap(pendingChangePropertyRequests, property, fullKey, new Change(changeRequestIndex, value, oldValue));
    }

    public void asyncNoWait(EditContext editContext, ExecContext execContext, Event editEvent, String actionSID, GAsyncNoWaitExec asyncNoWait, GPushAsyncInput pushAsyncResult, boolean externalChange, Consumer<Long> onExec) {
        asyncExecutePropertyEventAction(actionSID, editContext, execContext, editEvent, pushAsyncResult, externalChange, requestIndex -> {}, onExec);
    }

    public void asyncChange(EditContext editContext, ExecContext execContext, Event editEvent, String actionSID, GAsyncChange asyncChange, GPushAsyncInput pushAsyncResult, boolean externalChange, Consumer<Long> onExec) {
        asyncExecutePropertyEventAction(actionSID, editContext, execContext, editEvent, pushAsyncResult, externalChange, requestIndex -> {
            for (int propertyID : asyncChange.propertyIDs)
                setLoadingValueAt(propertyID, editContext.getFullKey(), asyncChange.value, requestIndex);
        }, onExec);
    }

    public void asyncAddRemove(EditContext editContext, ExecContext execContext, Event editEvent, String actionSID, GAsyncAddRemove asyncAddRemove, GPushAsyncInput pushAsyncResult, boolean externalChange, Consumer<Long> onExec) {
        final GObject object = form.getObject(asyncAddRemove.object);
        final boolean add = asyncAddRemove.add;

        GGridController controller = controllers.get(object.groupObject);

        final int position = controller.getSelectedRow();

        if (add) {
            MainFrame.logicsDispatchAsync.execute(new GenerateID(), new PriorityErrorHandlingCallback<GenerateIDResult>() {
                @Override
                public void onSuccess(GenerateIDResult result) {
                    asyncAddRemove(editContext, execContext, editEvent, actionSID, object, add, new GPushAsyncAdd(result.ID), new GGroupObjectValue(object.ID, new GCustomObjectValue(result.ID, null)), position, externalChange, onExec);
                }
            });
        } else {
            DeferredRunner.get().commitDelayedGroupObjectChange(object.groupObject);

            final GGroupObjectValue value = controllers.get(object.groupObject).getCurrentKey();
            if(value.isEmpty())
                return;
            asyncAddRemove(editContext, execContext, editEvent, actionSID, object, add, pushAsyncResult, value, position, externalChange, onExec);
        }
    }

    private void asyncAddRemove(EditContext editContext, ExecContext execContext, Event editEvent, String actionSID, GObject object, boolean add, GPushAsyncResult pushAsyncResult, GGroupObjectValue value, int position, boolean externalChange, Consumer<Long> onExec) {
        asyncExecutePropertyEventAction(actionSID, editContext, execContext, editEvent, pushAsyncResult, externalChange, requestIndex -> {
            pendingChangeCurrentObjectsRequests.put(object.groupObject, requestIndex);
            pendingModifyObjectRequests.put(requestIndex, new ModifyObject(object, add, value, position));

            controllers.get(object.groupObject).modifyGroupObject(value, add, -1);
        }, onExec);
    }

    public void changePropertyOrder(GPropertyDraw property, GGroupObjectValue columnKey, GOrder modiType) {
        syncResponseDispatch(new ChangePropertyOrder(property.ID, columnKey, modiType));
    }

    public void setPropertyOrders(GGroupObject groupObject, List<Integer> propertyList, List<GGroupObjectValue> columnKeyList, List<Boolean> orderList) {
        syncResponseDispatch(new SetPropertyOrders(groupObject.ID, propertyList, columnKeyList, orderList));
    }

    public void expandGroupObjectRecursive(GGroupObject group, boolean current) {
        DeferredRunner.get().commitDelayedGroupObjectChange(group);
        syncResponseDispatch(new ExpandGroupObjectRecursive(group.ID, current));
    }

    public void expandGroupObject(GGroupObject group, GGroupObjectValue value) {
        DeferredRunner.get().commitDelayedGroupObjectChange(group);
        syncResponseDispatch(new ExpandGroupObject(group.ID, value));
    }

    public void collapseGroupObjectRecursive(GGroupObject group, boolean current) {
        DeferredRunner.get().commitDelayedGroupObjectChange(group);
        syncResponseDispatch(new CollapseGroupObjectRecursive(group.ID, current));
    }

    public void collapseGroupObject(GGroupObject group, GGroupObjectValue value) {
        DeferredRunner.get().commitDelayedGroupObjectChange(group);
        syncResponseDispatch(new CollapseGroupObject(group.ID, value));
    }

    public void setTabActive(GContainer tabbedPane, GComponent visibleComponent) {
        asyncResponseDispatch(new SetTabActive(tabbedPane.ID, visibleComponent.ID));
        formLayout.onResize();
    }
    
    public void setContainerCollapsed(GContainer container, boolean collapsed) {
        asyncResponseDispatch(new SetContainerCollapsed(container.ID, collapsed));

        formLayout.updatePanels(); // we want to avoid blinking between setting visibility and getting response (and having updatePanels there)

        formLayout.onResize();
    }

    public void closePressed(EndReason reason) {
        syncDispatch(new ClosePressed(reason instanceof CommitReason), new ServerResponseCallback() {
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

    private void setRemoteRegularFilter(GRegularFilterGroup filterGroup, GRegularFilter filter) {
        syncResponseDispatch(new SetRegularFilter(filterGroup.ID, (filter == null) ? -1 : filter.ID));
    }

    public long changeFilter(GGroupObject groupObject, ArrayList<GPropertyFilter> conditions) {
        currentFilters.put(groupObject, conditions);
        return applyCurrentFilters();
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

        return applyCurrentFilters();
    }

    private long applyCurrentFilters() {
        ArrayList<GPropertyFilterDTO> filters = new ArrayList<>();
        currentFilters.foreachValue(groupFilters -> groupFilters.stream().map(GPropertyFilter::getFilterDTO).collect(Collectors.toCollection(() -> filters)));
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

    public void getInitialFilterProperty(PriorityErrorHandlingCallback<NumberResult> callback) {
        dispatcher.executePriority(new GetInitialFilterProperty(), callback);
    }

    public void focusProperty(GPropertyDraw propertyDraw) {
        getPropertyController(propertyDraw).focusProperty(propertyDraw);
    }

    public void setLoadingValueAt(int propertyID, GGroupObjectValue fullKey, Serializable value, long requestIndex) {
        GPropertyDraw property = getProperty(propertyID);
        GGroupObjectValue fullCurrentKey = getFullCurrentKey(property, fullKey);
        Pair<GGroupObjectValue, Object> propertyCell = getPropertyController(property).setLoadingValueAt(property, fullCurrentKey, value);

        if(propertyCell != null) {
            pendingChangeProperty(property, propertyCell.first, value, propertyCell.second, requestIndex);
            pendingLoadingProperty(property, propertyCell.first, requestIndex);
        }
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

    public abstract static class CustomErrorHandlingCallback<T> extends RequestCountingErrorHandlingCallback<T> {

        protected abstract void onSuccess(T result);

        @Override
        public void onSuccess(T result, Runnable onDispatchFinished) {
            onSuccess(result);

            if(onDispatchFinished != null)
                onDispatchFinished.run();
        }
    }

    public void countRecords(final GGroupObject groupObject, int clientX, int clientY) {
        asyncDispatch(new CountRecords(groupObject.ID), new CustomErrorHandlingCallback<NumberResult>() {
            @Override
            public void onSuccess(NumberResult result) {
                controllers.get(groupObject).showRecordQuantity((Integer) result.value, clientX, clientY);
            }
        });
    }

    public void calculateSum(final GGroupObject groupObject, final GPropertyDraw propertyDraw, GGroupObjectValue columnKey, int clientX, int clientY) {
        asyncDispatch(new CalculateSum(propertyDraw.ID, columnKey), new CustomErrorHandlingCallback<NumberResult>() {
            @Override
            public void onSuccess(NumberResult result) {
                controllers.get(groupObject).showSum(result.value, propertyDraw, clientX, clientY);
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
        syncDispatch(new GroupReport(groupObjectID, getUserPreferences()), new CustomErrorHandlingCallback<GroupReportResult>() {
            @Override
            public void onSuccess(GroupReportResult result) {
                GwtClientUtils.downloadFile(result.filename, "lsfReport", result.extension);
            }
        });
    }

    public void saveUserPreferences(GGridUserPreferences userPreferences, boolean forAllUsers, boolean completeOverride, String[] hiddenProps, final AsyncCallback<ServerResponseResult> callback) {
        syncDispatch(new SaveUserPreferencesAction(userPreferences.convertPreferences(), forAllUsers, completeOverride, hiddenProps), new CustomErrorHandlingCallback<ServerResponseResult>() {
            @Override
            public void onSuccess(ServerResponseResult response) {
                for (GAction action : response.actions) {
                    if (action instanceof GLogMessageAction) {
                        actionDispatcher.execute((GLogMessageAction) action);
                        callback.onFailure(new Throwable());
                        return;
                    }
                }
                callback.onSuccess(response);
            }

            @Override
            public void onFailure(Throwable caught) {
                callback.onFailure(caught);

                super.onFailure(caught);
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
            asyncView.setLoadingImage(set ? "loading.gif" : null);
    }

    public boolean previewEvent(Element target, Event event) {
        if(BrowserEvents.BLUR.equals(event.getType()))
            MainFrame.setLastBlurredElement(Element.as(event.getEventTarget()));
        checkLinkEditModeEvents(formsController, event);
        return previewLoadingManagerSinkEvents(event) && MainFrame.previewEvent(target, event, isEditing());
    }

    public GFormController contextEditForm;
    public void propagateFocusEvent(Event event) {
        if(BrowserEvents.BLUR.equals(event.getType()) && contextEditForm != null)
            contextEditForm.getRequestCellEditor().onBrowserEvent(contextEditForm.getEditElement(), new EventHandler(event));
    }

    private boolean previewLoadingManagerSinkEvents(Event event) {
        //focus() can trigger blur event, blur finishes editing. Editing calls syncDispatch.
        //If isEditing() and loadingManager isVisible() then flushCompletedRequests is not executed and syncDispatch is blocked.
        return !(dispatcher.loadingManager.isVisible() && (DataGrid.checkSinkEvents(event) || DataGrid.checkSinkFocusEvents(event)));
    }

    protected void onFormHidden(int closeDelay, EndReason editFormCloseReason) {
        FormDispatchAsync closeDispatcher = dispatcher;
        Scheduler.get().scheduleDeferred(() -> {
            closeDispatcher.executePriority(new Close(closeDelay), new PriorityErrorHandlingCallback<VoidResult>() {
                @Override
                public void onFailure(Throwable caught) { // supressing errors
                }
            });
            closeDispatcher.close();
        });
//        dispatcher = null; // so far there are no null checks (for example, like in desktop-client), so changePageSize can be called after (apparently close will suppress it)
    }

    public void updateFormCaption() {
        String caption = form.getCaption();
        setFormCaption(caption, form.getTooltip(caption));
    }

    public void setFormCaption(String caption, String tooltip) {
        throw new UnsupportedOperationException();
    }

    // need this because hideForm can be called twice, which will lead to several continueDispatching (and nullpointer, because currentResponse == null)
    private boolean formHidden;
    public void hideForm(int closeDelay, EndReason editFormCloseReason) {
        if(!formHidden) {
            onFormHidden(closeDelay, editFormCloseReason);
            formHidden = true;
        }
    }

    public void blockingConfirm(String caption, String message, boolean cancel, final DialogBoxHelper.CloseCallback callback) {
        DialogBoxHelper.showConfirmBox(caption, message, cancel, 0, 0, callback);
    }

    public void blockingConfirm(String caption, String message, boolean cancel, int timeout, int initialValue, final DialogBoxHelper.CloseCallback callback) {
        DialogBoxHelper.showConfirmBox(caption, message, cancel, timeout, initialValue, callback);
    }

    public void blockingMessage(String caption, String message, final DialogBoxHelper.CloseCallback callback) {
        blockingMessage(false, caption, message, callback);
    }

    public void blockingMessage(boolean isError, String caption, String message, final DialogBoxHelper.CloseCallback callback) {
        DialogBoxHelper.showMessageBox(isError, caption, message, callback);
    }

    public Dimension getPreferredSize(int maxWidth, int maxHeight) {
        return formLayout.getPreferredSize(maxWidth, maxHeight);
    }

    public boolean isWindow() {
        return formContainer instanceof ModalForm;
    }

    public boolean isDialog() {
        return isDialog;
    }

    public boolean isVisible() {
        return !formHidden && formVisible;
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

        // update captions (actually we could've set them directly to the containers, but tabbed pane physically adds / removes that views, so the check if there is a tab is required there)
        GFormLayout layout = formLayout;
        if(container.main)
            updateFormCaption();
        else
            layout.getContainerView(container.container).updateCaption(container);
    }

    public void setContainerCustomDesign(GContainer container, String customDesign) {
        GAbstractContainerView containerView = formLayout.getContainerView(container);
        ((CustomContainerView)containerView).updateCustomDesign(customDesign);
    }

    private static final class Change {
        public final long requestIndex;
        public final Object newValue;
        public final Object oldValue;

        private Change(long requestIndex, Object newValue, Object oldValue) {
            this.requestIndex = requestIndex;
            this.newValue = newValue;
            this.oldValue = oldValue;
        }
    }

    private static class ModifyObject {
        public final GObject object;
        public final boolean add;
        public final GGroupObjectValue value;
        public final int position;

        private ModifyObject(GObject object, boolean add, GGroupObjectValue value, int position) {
            this.object = object;
            this.add = add;
            this.value = value;
            this.position = position;
        }
    }

    public void focusFirstWidget() {
        if (formLayout.focusDefaultWidget()) {
            return;
        }

        for (GTreeGroupController treeController : treeControllers.values()) {
            if (treeController.focusFirstWidget()) {
                treeController.scrollToTop();
                return;
            }
        }

        for (GGridController controller : controllers.values()) { // в конце controllers лежит нулевой groupObject. его-то и следует оставить напоследок
            if (controller.focusFirstWidget()) {
                controller.scrollToTop();
                return;
            }
        }

        panelController.focusFirstWidget();
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
            result.add(addBinding(bindingEvent.inputEvent, bindingEvent.env, bindingExec, widget, propertyDraw.groupObject));
        return result;
    }
    public void removePropertyBindings(ArrayList<Integer> indices) {
        for(int index : indices)
            removeBinding(index);
    }

    public int addBinding(GInputEvent event, BindingExec pressed, Widget component, GGroupObject groupObject) {
        return addBinding(event, GBindingEnv.AUTO, pressed, component, groupObject);
    }
    public int addBinding(GInputEvent event, GBindingEnv env, BindingExec pressed, Widget component, GGroupObject groupObject) {
        return addBinding(event::isEvent, env, pressed, component, groupObject);
    }
    public int addBinding(BindingCheck event, GBindingEnv env, BindingExec pressed, Widget component, GGroupObject groupObject) {
        return addBinding(event, env, null, pressed, component, groupObject);
    }
    public int addBinding(BindingCheck event, GBindingEnv env, Supplier<Boolean> enabled, BindingExec pressed, Widget component, GGroupObject groupObject) {
        return addBinding(new GBindingEvent(event, env), new Binding(groupObject) {
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
        });
    }
    public int addBinding(GBindingEvent event, Binding action) {
        int index = bindings.size();
        bindingEvents.add(event);
        bindings.add(action);
        return index;
    }
    public void removeBinding(int index) {
        bindingEvents.remove(index);
        bindings.remove(index);
    }

    public void addEnterBindings(GBindingMode bindGroup, Consumer<Boolean> selectNextElement, GGroupObject groupObject) {
        addEnterBinding(false, bindGroup, selectNextElement, groupObject);
        addEnterBinding(true, bindGroup, selectNextElement, groupObject);
    }

    private void addEnterBinding(boolean shiftPressed, GBindingMode bindGroup, Consumer<Boolean> selectNextElement, GGroupObject groupObject) {
        addBinding(new GKeyInputEvent(new GKeyStroke(KeyCodes.KEY_ENTER, false, false, shiftPressed)),
                new GBindingEnv(-100, null, null, bindGroup, GBindingMode.NO, null, null, null),  // bindEditing - NO, because we don't want for example when editing text in grid to catch enter
                event -> selectNextElement.accept(!shiftPressed),
                null,
                groupObject);
    }

    private GGroupObject getGroupObject(Element elementTarget) {
        while (elementTarget != null) {     // пытаемся найти GroupObject, к которому относится элемент с фокусом
            GGroupObject targetGO = (GGroupObject) elementTarget.getPropertyObject("groupObject");
            if (targetGO != null)
                return targetGO;
            elementTarget = elementTarget.getParentElement();
        }
        return null;
    }

    public interface GGroupObjectSupplier {
        GGroupObject get();
    }

    public void processBinding(EventHandler handler, boolean preview, Element cellParent, boolean panel) {
        final EventTarget target = handler.event.getEventTarget();
        if (!Element.is(target)) {
            return;
        }

        Event event = handler.event;
        boolean isMouse = GMouseStroke.isEvent(event);
        TreeMap<Integer, Binding> orderedBindings = new TreeMap<>(); // descending sorting by priority

        GGroupObject groupObject = getGroupObject(Element.as(target));
        for (int i = 0, size = bindingEvents.size(); i < size; i++) {
            GBindingEvent bindingEvent = bindingEvents.get(i);
            if (bindingEvent.event.check(event)) {
                Binding binding = bindings.get(i);
                boolean equalGroup;
                GBindingEnv bindingEnv = bindingEvent.env;
                if(bindPreview(bindingEnv, isMouse, preview) &&
                    bindDialog(bindingEnv) &&
                    bindGroup(bindingEnv, groupObject, equalGroup = nullEquals(groupObject, binding.groupObject)) &&
                    bindEditing(bindingEnv, event) &&
                    bindShowing(bindingEnv, binding.showing()) &&
                    bindPanel(bindingEnv, isMouse, panel) &&
                    bindCell(bindingEnv, isMouse, cellParent != null))
                orderedBindings.put(-(GwtClientUtils.nvl(bindingEnv.priority, i) + (equalGroup ? 100 : 0)), binding); // increasing priority for group object
            }
        }

        for (Binding binding : orderedBindings.values()) {
            if (binding.enabled()) {
                checkCommitEditing();
                handler.consume();

                binding.exec(event);
                return;
            }
        }
    }

    public void checkCommitEditing() {
        RequestCellEditor requestCellEditor = getRequestCellEditor();
        if(requestCellEditor != null)
            requestCellEditor.commit(getEditElement(), CommitReason.FORCED);
    }

    private boolean bindPreview(GBindingEnv binding, boolean isMouse, boolean preview) {
        switch (binding.bindPreview) {
            case AUTO:
                return isMouse || !preview;
            case NO:
                return !preview;
            case ALL: // actually makes no since if previewed, than will be consumed so equivalent to only
                return true;
            case ONLY:
                return preview;
            default:
                throw new UnsupportedOperationException("Unsupported bindingMode " + binding.bindDialog);
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
                return !isEditing() || !targetElementIsEditing(event);
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

    private boolean targetElementIsEditing(Event event) {
        return editContext.getEditElement().isOrHasChild(Element.as(event.getEventTarget()));
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

    public Element getEditElement() {
        return editContext.getEditElement();
    }

    public Element getEditEventElement() {
        return editContext.getEditEventElement();
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
    private AsyncCallback<Pair<ArrayList<GAsync>, Boolean>> checkLast(int index, AsyncCallback<Pair<ArrayList<GAsync>, Boolean>> callback) {
        return new AsyncCallback<Pair<ArrayList<GAsync>, Boolean>>() {
            @Override
            public void onFailure(Throwable caught) {
                if(index >= editLastReceivedAsyncIndex) {
                    editLastReceivedAsyncIndex = index;
                    callback.onFailure(caught);
                }
            }

            @Override
            public void onSuccess(Pair<ArrayList<GAsync>, Boolean> result) {
                if(index >= editLastReceivedAsyncIndex) {
                    editLastReceivedAsyncIndex = index;
                    if(!result.second && index < editAsyncIndex - 1)
                        result = new Pair<>(result.first, true);
                    callback.onSuccess(result);
                }
            }
        };
    }

    // synchronous call (with request indices, etc.)
    private void getPessimisticValues(int propertyID, GGroupObjectValue columnKey, String actionSID, String value, int index, AsyncCallback<Pair<ArrayList<GAsync>, Boolean>> callback) {
        asyncDispatch(new GetAsyncValues(propertyID, columnKey, actionSID, value, index), new CustomCallback<ListResult>() {
            @Override
            public void onFailure(Throwable caught) {
                callback.onFailure(caught);
            }

            @Override
            public void onSuccess(ListResult result) {
                callback.onSuccess(new Pair<>(result.value, false));
            }
        });
    }

    public void getAsyncValues(String value, AsyncCallback<Pair<ArrayList<GAsync>, Boolean>> callback) {
        if(editContext != null) // just in case
            getAsyncValues(value, editContext.getProperty(), editContext.getColumnKey(), editAsyncValuesSID, callback);
    }

    public void getAsyncValues(String value, GPropertyDraw property, GGroupObjectValue columnKey, String actionSID, AsyncCallback<Pair<ArrayList<GAsync>, Boolean>> callback) {
        int editIndex = editAsyncIndex++;
        AsyncCallback<Pair<ArrayList<GAsync>, Boolean>> fCallback = checkLast(editIndex, callback);

        GGroupObjectValue currentKey = getFullCurrentKey(property, columnKey);

        if (!editAsyncUsePessimistic)
            dispatcher.executePriority(new GetPriorityAsyncValues(property.ID, currentKey, actionSID, value, editIndex), new PriorityAsyncCallback<ListResult>() {
                @Override
                public void onFailure(Throwable caught) {
                    fCallback.onFailure(caught);
                }

                @Override
                public void onSuccess(ListResult result) {
                    if (result.value == null) { // optimistic request failed, running pessimistic one, with request indices, etc.
                        editAsyncUsePessimistic = true;
                        getPessimisticValues(property.ID, currentKey, actionSID, value, editIndex, fCallback);
                    } else {
                        boolean moreResults = false;
                        ArrayList<GAsync> values = result.value;
                        if(values.size() > 0) {
                            GAsync lastResult = values.get(values.size() - 1);
                            if(lastResult.equals(GAsync.RECHECK)) {
                                values = removeLast(values);

                                moreResults = true;
                                getPessimisticValues(property.ID, currentKey, actionSID, value, editIndex, fCallback);
                            } else if(values.size() == 1 && lastResult.equals(GAsync.CANCELED)) // ignoring CANCELED results
                                values = null;
                        }
                        fCallback.onSuccess(new Pair<>(values, moreResults));
                    }
                }
            });
        else
            getPessimisticValues(property.ID, currentKey, actionSID, value, editIndex, fCallback);
    }

    public void edit(GType type, Event event, boolean hasOldValue, Object setOldValue, GInputList inputList, BiConsumer<GUserInputResult, Consumer<Long>> afterCommit, Consumer<CancelReason> cancel, EditContext editContext, String actionSID, String customChangeFunction) {
        assert type != null;
        lsfusion.gwt.client.base.Result<lsfusion.gwt.client.base.Result<Object>> oldValue = new lsfusion.gwt.client.base.Result<>();
        edit(type, event, hasOldValue, setOldValue, inputList, // actually it's assumed that actionAsyncs is used only here, in all subsequent calls it should not be referenced
                (inputResult, commitReason) -> oldValue.set(setLocalValue(editContext, type, inputResult)),
                (inputResult, commitReason) -> afterCommit.accept(inputResult, requestIndex -> setRemoteValue(editContext, oldValue.result, inputResult, requestIndex)),
                cancel, editContext, actionSID, customChangeFunction);
    }

    public void edit(GType type, Event event, boolean hasOldValue, Object oldValue, GInputList inputList, BiConsumer<GUserInputResult, CommitReason> beforeCommit, BiConsumer<GUserInputResult, CommitReason> afterCommit,
                     Consumer<CancelReason> cancel, EditContext editContext, String editAsyncValuesSID, String customChangeFunction) {
        GPropertyDraw property = editContext.getProperty();

        CellEditor cellEditor;
        if (customChangeFunction != null && !customChangeFunction.equals("DEFAULT")) // see LsfLogics.g propertyCustomView rule
            cellEditor = CustomReplaceCellEditor.create(this, property, customChangeFunction);
        else
            cellEditor = type.createGridCellEditor(this, property, inputList);

        if (cellEditor != null) {
            if(!hasOldValue) // property.baseType.equals(type) actually there should be something like compatible, but there is no such method for now, so we'll do this check in editors
                oldValue = editContext.getValue();

            edit(cellEditor, event, oldValue, beforeCommit, afterCommit, cancel, editContext, editAsyncValuesSID, -1);
        } else
            cancel.accept(CancelReason.OTHER);
    }

    public void edit(CellEditor cellEditor, Event event, Object oldValue, BiConsumer<GUserInputResult, CommitReason> beforeCommit, BiConsumer<GUserInputResult, CommitReason> afterCommit,
                     Consumer<CancelReason> cancel, EditContext editContext, String editAsyncValuesSID, long editRequestIndex) {
        // because for example embedded / popup (that use edit) forms are opened with a timer, there can be some pending edit calls so we need to avoid this
        checkCommitEditing();

        assert this.editContext == null;
        editBeforeCommit = beforeCommit;
        editAfterCommit = afterCommit;
        editCancel = cancel;

        this.editAsyncValuesSID = editAsyncValuesSID;

        this.editContext = editContext;
        this.editRequestIndex = editRequestIndex;  // we need to force dispatch responses till this index because otherwise we won't

        Element element = getEditElement();
        if (cellEditor instanceof ReplaceCellEditor) {
            focusedElement = GwtClientUtils.getFocusedElement();
            if(!editContext.isFocusable()) // assert that otherwise it's already has focus
                forceSetFocus = editContext.forceSetFocus();
            editContext.startEditing();

            RenderContext renderContext = editContext.getRenderContext();

            GPropertyDraw property = editContext.getProperty();
            CellRenderer cellRenderer = property.getCellRenderer();
            Pair<Integer, Integer> renderedSize = null;
            if(property.autoSize) // we need to do it before clearRender to have actual sizes + we need to remove paddings since we're setting width for wrapped component
                renderedSize = new Pair<>(element.getClientWidth(), element.getClientHeight());

            cellRenderer.clearRender(element, renderContext); // dropping previous render

            ((ReplaceCellEditor)cellEditor).render(element, renderContext, renderedSize, oldValue); // rendering new one, filling inputElement
        }

        this.cellEditor = cellEditor; // not sure if it should before or after startEditing, but definitely after removeAllChildren, since it leads to blur for example
        cellEditor.start(event, element, oldValue); //need to be after this.cellEditor = cellEditor, because there is commitEditing in start in LogicalCellEditor
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
    public void cancelEditing(CancelReason cancelReason) {
        finishEditing(false, true);

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

        CellEditor cellEditor = this.cellEditor;
        this.cellEditor = null;

        EditContext editContext = this.editContext;
//        this.editRequestIndex = -1; //it doesn't matter since it is not used when editContext / cellEditor is null
        this.editAsyncUsePessimistic = false;
        this.editAsyncValuesSID = null;

        if(cellEditor instanceof RequestCellEditor)
            ((RequestCellEditor)cellEditor).stop(renderElement, cancel);

        if(cellEditor instanceof ReplaceCellEditor)
            ((ReplaceCellEditor) cellEditor).clearRender(renderElement, editContext.getRenderContext(), cancel);

        //getAsyncValues need editContext, so it must be after clearRenderer
        this.editContext = null;

        if(cellEditor instanceof ReplaceCellEditor) {
            render(editContext);

            editContext.stopEditing();

            if(forceSetFocus != null) {
                editContext.restoreSetFocus(forceSetFocus);
                forceSetFocus = null;
            }

            if(blurred) { // when editing is commited (thus editing element is removed), set last blurred element to main widget to keep focus there
                if(editContext.isSetLastBlurred())
                    MainFrame.setLastBlurredElement(editContext.getFocusElement());
            } else {
                if (focusedElement != null) {
                    focusedElement.focus();
                    focusedElement = null;
                }
            }
        }

        update(editContext);
    }

    public void render(EditContext editContext) {
        render(editContext.getProperty(), editContext.getEditElement(), editContext.getRenderContext());
    }
    public void render(GPropertyDraw property, Element element, RenderContext renderContext) {
        if(isEdited(element)) {
            assert false;
            return;
        }

        property.getCellRenderer().render(element, renderContext);
    }

    // setting loading
    public void setLoading(ExecuteEditContext editContext, long requestIndex) {
        // actually this part we can do before sending request
        editContext.setLoading();

        // RERENDER IF NEEDED : we have the previous state
        // but in that case we need to move GridPropertyColumn.renderDom logics to GFormController.render here (or have some callbacks)
        update(editContext);

        GPropertyDraw property = editContext.getProperty();
        GGroupObjectValue rowKey = property.isList ? editContext.getRowKey() : GGroupObjectValue.EMPTY; // because for example in custom renderer editContext can be not the currentKey

        pendingLoadingProperty(property, GGroupObjectValue.getFullKey(rowKey, editContext.getColumnKey()), requestIndex);
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
    public void setValue(EditContext editContext, Object value) {
        editContext.setValue(value);

        update(editContext);
    }
    public void update(EditContext editContext) {
        update(editContext.getProperty(), editContext.getEditElement(), editContext.getUpdateContext());
    }
    public void update(GPropertyDraw property, Element element, UpdateContext updateContext) {
        if(isEdited(element))
            return;

        property.getCellRenderer().update(element, updateContext);
    }

    public boolean isEdited(Element element) {
        return editContext != null && editContext.getEditElement() == element;
    }

    public static void setBackgroundColor(Element element, String color, boolean themeConvert) {
        if (color != null) {
            element.getStyle().setBackgroundColor(themeConvert ? getDisplayColor(color) : color);
        } else {
            element.getStyle().clearBackgroundColor();
        }
    }

    public static void setForegroundColor(Element element, String color) {
        setForegroundColor(element, color, false);
    }

    public static void setForegroundColor(Element element, String color, boolean themeConvert) {
        if (color != null) {
            element.getStyle().setColor(themeConvert ? getDisplayColor(color) : color);
        } else {
            element.getStyle().clearColor();
        }
    }

    public void onPropertyBrowserEvent(EventHandler handler, Element cellParent, Element focusElement, Consumer<EventHandler> onOuterEditBefore,
                                       Consumer<EventHandler> onEdit, Consumer<EventHandler> onOuterEditAfter, Consumer<EventHandler> onCut,
                                       Consumer<EventHandler> onPaste, boolean panel, boolean customRenderer) {
        RequestCellEditor requestCellEditor = getRequestCellEditor();
        boolean isPropertyEditing = requestCellEditor != null && getEditEventElement() == cellParent;
        if(isPropertyEditing)
            requestCellEditor.onBrowserEvent(getEditElement(), handler);

        if(DataGrid.getBrowserTooltipMouseEvents().contains(handler.event.getType())) // just not to have problems in debugger
            return;

        if(handler.consumed)
            return;

        if(GMouseStroke.isChangeEvent(handler.event) && GwtClientUtils.getFocusedChild(focusElement) == null) // need to check that focus is not on the grid, otherwise when editing for example embedded form, any click will cause moving focus to grid, i.e. stopping the editing
            focusElement.focus(); // it should be done on CLICK, but also on MOUSEDOWN, since we want to focus even if mousedown is later consumed

        /*if(!previewLoadingManagerSinkEvents(handler.event)) {
            return;
        }*/

        checkMouseKeyEvent(handler, true, cellParent, panel, customRenderer);

        if(handler.consumed)
            return;

        onOuterEditBefore.accept(handler);

        if(handler.consumed)
            return;

        if (!isPropertyEditing) { // if editor did not consume event, we don't want it to be handled by "renderer" since it doesn't exist
            if (GKeyStroke.isCopyToClipboardEvent(handler.event)) {
                onCut.accept(handler);
            } else if (GKeyStroke.isPasteFromClipboardEvent(handler.event)) {
                onPaste.accept(handler);
            } else {
                onEdit.accept(handler);
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

        checkMouseKeyEvent(handler, false, cellParent, panel, customRenderer);
    }
    
    public void resetWindowsLayout() {
        formsController.resetWindowsLayout();
    }
}