package platform.gwt.form.client.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.layout.*;
import com.smartgwt.client.widgets.toolbar.ToolStripButton;
import net.customware.gwt.dispatch.client.DefaultExceptionHandler;
import net.customware.gwt.dispatch.shared.Action;
import platform.gwt.base.shared.GClassViewType;
import platform.gwt.sgwtbase.client.ui.ToolStripPanel;
import platform.gwt.form.client.FormDispatchAsync;
import platform.gwt.form.shared.actions.GetForm;
import platform.gwt.form.shared.actions.GetFormResult;
import platform.gwt.form.shared.actions.form.*;
import platform.gwt.view.*;
import platform.gwt.view.changes.GFormChanges;
import platform.gwt.view.changes.GGroupObjectValue;
import platform.gwt.view.changes.dto.GFormChangesDTO;
import platform.gwt.view.logics.FormLogicsProvider;
import platform.gwt.view.logics.SelectObjectCallback;

import java.util.LinkedHashMap;
import java.util.Map;

public class GFormController extends HLayout implements FormLogicsProvider {
    private final boolean dialogMode;

    @Override
    public boolean isEditingEnabled() {
        // пока отключаем редактирование в production
        return !GWT.isScript();
    }

    private final FormDispatchAsync dispatcher = new FormDispatchAsync(new DefaultExceptionHandler());

    private GForm form;
    private GFormLayout mainPane;

    public GFormController(String sid) {
        this(sid, null);
    }

    public GFormController(String sid, Map<String, String> params) {
        this(null, new GetForm(sid, params), false);
    }

    public GFormController(Map<String, String> params) {
        this(null, new GetForm(params.remove("formSID"), params), false);
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

        mainPane = new GFormLayout(form.mainContainer) {
            public boolean isShowTypeInItsPlace(GGroupObject groupObject) {
                GGroupObjectController goc = GFormController.this.controllers.get(groupObject);
                return goc != null && !goc.isInGrid() && goc.getShowTypeView().needToBeVisible();
            }
        };
        mainPane.setMinHeight(Window.getClientHeight());
        mainPane.setHeight(Window.getClientHeight());
        mainPane.setMaxHeight(Window.getClientHeight());

        if (!dialogMode) {
            mainPane.addMember(new ToolStripPanel(form.caption), 0);
        }

        initializeControllers();

        initializeRegularFilters();

        applyRemoteChanges();
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

        //todo: orders...
//        applyOrders(filter != null ? filter.orders : filterGroup.nullOrders);

//        applyRemoteChanges();
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
                GGroupObjectController controller = new GGroupObjectController(this, form, group, mainPane);
                controllers.put(group, controller);
            }
        }
        
        for (GPropertyDraw property : form.propertyDraws) {
            if (property.groupObject == null) {
                controllers.put(null, new GGroupObjectController(this, form, null, mainPane));
            }
        }

        addMember(mainPane);

        applyRemoteChanges();
    }

    private void applyRemoteChanges() {
        dispatcher.execute(new GetRemoteChanges(), new FormChangesBlockingCallback());
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

    private void refreshData() {
        dispatcher.execute(new GetRemoteChanges(), new FormChangesBlockingCallback());
    }

    public void changeGroupObject(GGroupObject group, GGroupObjectValue key) {
        dispatcher.executeChangeGroupObject(new ChangeGroupObject(group.ID, key.getValueDTO()), new FormChangesBlockingCallback());
    }

    public void executeEditAction(GPropertyDraw property, String actionSID) {
        GGroupObjectValue key = controllers.get(property.groupObject).getCurrentKey();
        key = key != null ? key : new GGroupObjectValue();
        executeEditAction(property, key, actionSID);
    }

    public void executeEditAction(GPropertyDraw property, GGroupObjectValue key, String actionSID) {
        if (isEditingEnabled()) {
            dispatcher.execute(new ExecuteEditAction(property.ID, key.getValueDTO(), actionSID), new FormChangesBlockingCallback());
        }
    }

    public void changePropertyDraw(GPropertyDraw property, Object value) {
        if (isEditingEnabled()) {
            dispatcher.execute(new ChangeProperty(property.ID, value), new FormChangesBlockingCallback());
        }
    }

    public void changePropertyDraw(GGroupObject group, GGroupObjectValue key, final GPropertyDraw property, final Object value) {
        if (isEditingEnabled()) {
            executeConsecutiveActions(new ChangeGroupObject(group.ID, key.getValueDTO()), new ChangeProperty(property.ID, value));
        }
    }
    
    public void changeClassView(GGroupObject groupObject, GClassViewType classView) {
        dispatcher.execute(new ChangeClassView(groupObject.ID, classView.name()), new FormChangesBlockingCallback());
    }

    public void expandGroupObject(GGroupObject group, GGroupObjectValue value) {
        dispatcher.execute(new ExpandGroupObject(group.ID, value.getValueDTO()), new FormChangesBlockingCallback());
    }

    public void collapseGroupObject(GGroupObject group, GGroupObjectValue value) {
        dispatcher.execute(new CollapseGroupObject(group.ID, value.getValueDTO()), new FormChangesBlockingCallback());
    }

    private void executeConsecutiveActions(Action<FormChangesResult>... actions) {
        if (actions.length > 0) {
            executeConsecutiveActions(actions, 0);
        }
    }

    private void executeConsecutiveActions(final Action<FormChangesResult> actions[], final int startFrom) {
        dispatcher.execute(actions[startFrom], new FormChangesBlockingCallback() {
            @Override
            public void afterSuccess() {
                executeConsecutiveActions(actions, startFrom + 1);
            }
        });
    }

    private void setRemoteRegularFilter(GRegularFilterGroup filterGroup, GRegularFilter filter) {
        dispatcher.execute(new SetRegularFilter(filterGroup.ID, (filter == null) ? -1 : filter.ID), new FormChangesBlockingCallback());
    }

    private void applyChanges() {
        dispatcher.execute(new ApplyChanges(), new FormChangesBlockingCallback());
    }

    private void cancelChanges() {
        dispatcher.execute(new CancelChanges(), new FormChangesBlockingCallback());
    }

    private class FormChangesBlockingCallback implements AsyncCallback<FormChangesResult> {
//        private final LoadingWindow wl;

        public FormChangesBlockingCallback() {
//            this.wl = LoadingWindow.showLoadingBlocker();
        }

        @Override
        public void onFailure(Throwable t) {
//            wl.destroy();
            GWT.log("Ошибка во время чтения данных с сервера: ", t);
            SC.warn("Ошибка во время чтения данных с сервера: <br/>" + t.getMessage());
        }

        @Override
        public void onSuccess(FormChangesResult result) {
//            wl.destroy();
            applyRemoteChanges(result.changes);
            afterSuccess();
        }

        public void afterSuccess() {
        }
    }
}