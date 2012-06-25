package platform.gwt.main.client.form.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.layout.HLayout;
import net.customware.gwt.dispatch.client.DefaultExceptionHandler;
import net.customware.gwt.dispatch.shared.Action;
import net.customware.gwt.dispatch.shared.Result;
import platform.gwt.base.shared.GClassViewType;
import platform.gwt.main.client.dispatch.FormDispatchAsync;
import platform.gwt.main.client.form.dispatch.GwtFormActionDispatcher;
import platform.gwt.main.shared.actions.GetForm;
import platform.gwt.main.shared.actions.GetFormResult;
import platform.gwt.main.shared.actions.form.*;
import platform.gwt.sgwtbase.client.ErrorAsyncCallback;
import platform.gwt.sgwtbase.client.WrapperAsyncCallbackEx;
import platform.gwt.view.*;
import platform.gwt.view.changes.GFormChanges;
import platform.gwt.view.changes.GGroupObjectValue;
import platform.gwt.view.changes.dto.GFormChangesDTO;
import platform.gwt.view.changes.dto.ObjectDTO;
import platform.gwt.view.logics.FormLogicsProvider;
import platform.gwt.view.logics.SelectObjectCallback;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GFormController extends HLayout implements FormLogicsProvider {
    @Override
    public boolean isEditingEnabled() {
        // пока отключаем редактирование в production
        return !GWT.isScript();
    }

    private final FormDispatchAsync dispatcher = new FormDispatchAsync(new DefaultExceptionHandler());

    private final GwtFormActionDispatcher actionDispatcher = new GwtFormActionDispatcher(this);

    private GForm form;
    private GFormLayout mainPane;
    private final boolean dialogMode;

    public GFormController(Map<String, String> params) {
        this(params.remove("formSID"), params);
    }

    public GFormController(String formSID) {
        this(formSID, null);
    }
    public GFormController(String formSID, Map<String, String> params) {
        this(null, new GetForm(formSID, params), false);
    }

    public GFormController(FormDispatchAsync creationDispatcher, Action<GetFormResult> getFormAction, final boolean dialogMode) {
        this.dialogMode = dialogMode;
        if (creationDispatcher == null) {
            creationDispatcher = dispatcher;
        }

        creationDispatcher.execute(getFormAction, new AsyncCallback<GetFormResult>() {
            @Override
            public void onFailure(Throwable caught) {
                GWT.log("Ошибка при инициализации формы: ", caught);
                SC.warn("Ошибка при попытке открыть форму: " + caught.getMessage());
            }

            @Override
            public void onSuccess(GetFormResult result) {
                initialize(result.form);
            }
        });
    }

    private Map<GGroupObject, GGroupObjectController> controllers = new LinkedHashMap<GGroupObject, GGroupObjectController>();
    private Map<GTreeGroup, GTreeGroupController> treeControllers = new LinkedHashMap<GTreeGroup, GTreeGroupController>();

    private void initialize(GForm form) {
        this.form = form;

        dispatcher.setForm(form);

        mainPane = new GFormLayout(this, form.mainContainer) {
            public boolean isShowTypeInItsPlace(GGroupObject groupObject) {
                GGroupObjectController goc = GFormController.this.controllers.get(groupObject);
                return goc != null && !goc.isInGrid() && goc.getShowTypeView().needToBeVisible();
            }
        };

        mainPane.setHeight100();

        initializeControllers();

        initializeRegularFilters();

        getRemoteChanges();
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
        final CheckboxItem filterCheck = new CheckboxItem();
        filterCheck.setTitle(filter.caption);
        filterCheck.setValue(false);
        filterCheck.addChangedHandler(new ChangedHandler() {
            @Override
            public void onChanged(ChangedEvent event) {
                setRegularFilter(filterGroup, filterCheck.getValueAsBoolean() ? filter : null);
            }
        });

        addFilterComponent(filterGroup, filterCheck);

        if (filterGroup.defaultFilter >= 0) {
            filterCheck.setValue(true);
        }
    }

    private void createMultipleFilterComponent(final GRegularFilterGroup filterGroup) {
        LinkedHashMap<String, String> itemsMap = new LinkedHashMap<String, String>();
        itemsMap.put("-1", "(Все)");
        int i = 0;
        for (GRegularFilter filter : filterGroup.filters) {
            itemsMap.put("" + i, filter.caption);
            ++i;
        }

        final SelectItem filterBox = new SelectItem();
        filterBox.setValueMap(itemsMap);
        filterBox.setShowTitle(false);
        filterBox.setValue("-1");
        filterBox.setPickListHeight(100);
        filterBox.setPickListWidth(200);

        filterBox.addChangedHandler(new ChangedHandler() {
            @Override
            public void onChanged(ChangedEvent event) {
                int ind = Integer.parseInt(filterBox.getValueAsString());
                setRegularFilter(filterGroup, ind == -1 ? null : filterGroup.filters.get(ind));
            }
        });

        addFilterComponent(filterGroup, filterBox);

        if (filterGroup.defaultFilter >= 0) {
            filterBox.setValue("" + filterGroup.defaultFilter);
        }
    }

    private void setRegularFilter(GRegularFilterGroup filterGroup, GRegularFilter filter) {
        setRemoteRegularFilter(filterGroup, filter);
    }

    private void addFilterComponent(GRegularFilterGroup filterGroup, FormItem item) {
        if (filterGroup.drawToToolbar) 
            controllers.get(filterGroup.groupObject).addFilterComponent(item);
        else {
            controllers.get(filterGroup.groupObject).addPanelFilterComponent(item);
        }
    }

    private void initializeControllers() {
        for (GTreeGroup treeGroup : form.treeGroups) {
            GTreeGroupController treeController = new GTreeGroupController(treeGroup, this, form, mainPane);
            treeControllers.put(treeGroup, treeController);
        }

        for (GGroupObject group : form.groupObjects) {
            if (group.parent == null) {
                GGroupObjectController controller = new GGroupObjectController(this, group, mainPane);
                controllers.put(group, controller);
            }
        }
        
        for (GPropertyDraw property : form.propertyDraws) {
            if (property.groupObject == null) {
                controllers.put(null, new GGroupObjectController(this, null, mainPane));
            }
        }

        addMember(mainPane);

        getRemoteChanges();
    }

    public GPropertyDraw getProperty(int id) {
        return form.getProperty(id);
    }

    public GGroupObject getGroupObject(int groupID) {
        return form.getGroupObject(groupID);
    }

    private void getRemoteChanges() {
        dispatcher.execute(new GetRemoteChanges(), new ServerResponseCallback());
    }

    public void applyRemoteChanges(GFormChangesDTO changesDTO) {
        GFormChanges fc = GFormChanges.remap(form, changesDTO);
        for (GGroupObjectController controller : controllers.values()) {
            controller.processFormChanges(fc);
        }
        for (GTreeGroupController treeController : treeControllers.values()) {
            treeController.processFormChanges(fc);
        }
        hideEmpty();
    }

    public void hideEmpty() {
        mainPane.hideEmpty();
    }

    @Override
    public void selectObject(GPropertyDraw property, SelectObjectCallback selectObjectCallback) {
        if (isEditingEnabled()) {
            SelectObjectDialog.showObjectDialog(property.caption, dispatcher, new CreateEditorForm(property.ID), selectObjectCallback);
        }
    }

    public void changeGroupObject(GGroupObject group, GGroupObjectValue key) {
        dispatcher.execute(new ChangeGroupObject(group.ID, key.getValueDTO()), new ServerResponseCallback());
    }

    public void executeEditAction(GPropertyDraw property, String actionSID) {
        GGroupObjectValue key = controllers.get(property.groupObject).getCurrentKey();
        if (key == null) {
            key = new GGroupObjectValue();
        }
        executeEditAction(property, key, actionSID);
    }

    public void executeEditAction(GPropertyDraw property, GGroupObjectValue key, String actionSID) {
        executeEditAction(property, key, actionSID, new ServerResponseCallback());
    }

    public void executeEditAction(GPropertyDraw property, GGroupObjectValue key, String actionSID, AsyncCallback<ServerResponseResult> callback) {
        if (isEditingEnabled()) {
            dispatcher.execute(new ExecuteEditAction(property.ID, key.getValueDTO(), actionSID), callback);
        }
    }

    public void contiueServerInvocation(Object[] actionResults, AsyncCallback<ServerResponseResult> callback) {
        syncDispatch(new ContinueInvocationAction(actionResults), callback);
    }

    public void throwInServerInvocation(Exception exception) {
        syncDispatch(new ThrowInInvocationAction(exception), new ErrorAsyncCallback<ServerResponseResult>());
    }

    public <T extends Result> void syncDispatch(Action<T> action, AsyncCallback<T> callback) {
        disable();
        dispatcher.execute(action, new WrapperAsyncCallbackEx<T>(callback) {
            @Override
            public void preProcess() {
                enable();
            }
        });
    }

    public void changeProperty(GPropertyDraw property, ObjectDTO value) {
        if (isEditingEnabled()) {
            dispatcher.execute(new ChangeProperty(property.ID, value), new ServerResponseCallback());
        }
    }

    public void changeClassView(GGroupObject groupObject, GClassViewType classView) {
        dispatcher.execute(new ChangeClassView(groupObject.ID, classView.name()), new ServerResponseCallback());
    }

    public void expandGroupObject(GGroupObject group, GGroupObjectValue value) {
        dispatcher.execute(new ExpandGroupObject(group.ID, value.getValueDTO()), new ServerResponseCallback());
    }

    public void collapseGroupObject(GGroupObject group, GGroupObjectValue value) {
        dispatcher.execute(new CollapseGroupObject(group.ID, value.getValueDTO()), new ServerResponseCallback());
    }

    public void setTabVisible(GContainer tabbedPane, GComponent visibleComponent) {
        dispatcher.execute(new SetTabVisible(tabbedPane.ID, visibleComponent.ID), new ServerResponseCallback());
    }

    private void setRemoteRegularFilter(GRegularFilterGroup filterGroup, GRegularFilter filter) {
        dispatcher.execute(new SetRegularFilter(filterGroup.ID, (filter == null) ? -1 : filter.ID), new ServerResponseCallback());
    }

    public List<GPropertyDraw> getPropertyDraws() {
        return form.propertyDraws;
    }

    private class ServerResponseCallback extends ErrorAsyncCallback<ServerResponseResult> {
        @Override
        public void success(ServerResponseResult response) {
            actionDispatcher.dispatchResponse(response);
        }
    }
}