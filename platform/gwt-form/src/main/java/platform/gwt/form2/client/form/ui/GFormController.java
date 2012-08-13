package platform.gwt.form2.client.form.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import net.customware.gwt.dispatch.client.DefaultExceptionHandler;
import net.customware.gwt.dispatch.shared.Action;
import net.customware.gwt.dispatch.shared.Result;
import platform.gwt.base.client.ErrorAsyncCallback;
import platform.gwt.base.client.WrapperAsyncCallbackEx;
import platform.gwt.base.shared.GClassViewType;
import platform.gwt.form2.client.dispatch.FormDispatchAsync;
import platform.gwt.form2.client.form.classes.ClassChosenHandler;
import platform.gwt.form2.client.form.dispatch.GwtFormActionDispatcher;
import platform.gwt.form2.client.form.ui.dialog.MessageBox;
import platform.gwt.form2.shared.actions.GetForm;
import platform.gwt.form2.shared.actions.GetFormResult;
import platform.gwt.form2.shared.actions.form.*;
import platform.gwt.view2.*;
import platform.gwt.view2.changes.GFormChanges;
import platform.gwt.view2.changes.GGroupObjectValue;
import platform.gwt.view2.changes.dto.GFormChangesDTO;
import platform.gwt.view2.changes.dto.GGroupObjectValueDTO;
import platform.gwt.view2.classes.GObjectClass;
import platform.gwt.view2.logics.FormLogicsProvider;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GFormController extends SimplePanel implements FormLogicsProvider {
    @Override
    public boolean isEditingEnabled() {
        // пока отключаем редактирование в production
//        return !GWT.isScript();
        return true;
    }

    private final FormDispatchAsync dispatcher = new FormDispatchAsync(new DefaultExceptionHandler());

    private final GwtFormActionDispatcher actionDispatcher = new GwtFormActionDispatcher(this);

    private GForm form;
    private GFormLayout formLayout;
    private final boolean dialogMode;

    private Map<GGroupObject, GGroupObjectController> controllers = new LinkedHashMap<GGroupObject, GGroupObjectController>();
    private Map<GTreeGroup, GTreeGroupController> treeControllers = new LinkedHashMap<GTreeGroup, GTreeGroupController>();

    public GFormController(Map<String, String> params) {
        this(params.remove("formSID"), params);
    }

    public GFormController(String formSID) {
        this(formSID, null);
    }

    public GFormController(String formSID, Map<String, String> params) {
        this(new GetForm(formSID, params), false);
    }

    public GFormController(Action<GetFormResult> getFormAction, final boolean dialogMode) {
        this.dialogMode = dialogMode;

        dispatcher.execute(getFormAction, new ErrorAsyncCallback<GetFormResult>() {
            @Override
            public void success(GetFormResult result) {
                initialize(result.form);
            }
        });
    }

    public GFormController(GForm gForm, final boolean dialogMode) {
        this.dialogMode = dialogMode;
        initialize(gForm);
    }

    private void initialize(GForm form) {
        this.form = form;

        dispatcher.setForm(form);

        formLayout = new GFormLayout(this, form.mainContainer) {
            public boolean isShowTypeViewInPanel(GGroupObject groupObject) {
                GGroupObjectController controller = controllers.get(groupObject);
                return controller != null && !controller.isInGridClassView() && controller.isShowTypeVisible();
            }
        };

        addStyleName("formController");

        add(formLayout);

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
        final CheckBox filterCheck = new CheckBox(filter.caption);
        filterCheck.setValue(false);
        filterCheck.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> e) {
                setRegularFilter(filterGroup, e.getValue() != null && e.getValue() ? filter : null);
            }
        });
        addFilterComponent(filterGroup, filterCheck);

        if (filterGroup.defaultFilter >= 0) {
            filterCheck.setValue(true);
        }
    }

    private void createMultipleFilterComponent(final GRegularFilterGroup filterGroup) {
        final ListBox filterBox = new ListBox(false);
        filterBox.addItem("(Все)", "-1");

        ArrayList<GRegularFilter> filters = filterGroup.filters;
        for (int i = 0; i < filters.size(); i++) {
            GRegularFilter filter = filters.get(i);
            filterBox.addItem(filter.caption, "" + i);
        }

        filterBox.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                int ind = Integer.parseInt(filterBox.getValue(filterBox.getSelectedIndex()));
                setRegularFilter(filterGroup, ind == -1 ? null : filterGroup.filters.get(ind));
            }
        });

        addFilterComponent(filterGroup, filterBox);
        if (filterGroup.defaultFilter >= 0) {
            //todo: check if this is correct
            filterBox.setSelectedIndex(filterGroup.defaultFilter + 1);
        }
    }

    private void setRegularFilter(GRegularFilterGroup filterGroup, GRegularFilter filter) {
        setRemoteRegularFilter(filterGroup, filter);
    }

    private void addFilterComponent(GRegularFilterGroup filterGroup, Widget filterWidget) {
        if (filterGroup.drawToToolbar)
            controllers.get(filterGroup.groupObject).addFilterComponent(filterWidget);
        else {
            controllers.get(filterGroup.groupObject).addPanelFilterComponent(filterWidget);
        }
    }

    private void initializeControllers() {
        for (GTreeGroup treeGroup : form.treeGroups) {
            GTreeGroupController treeController = new GTreeGroupController(treeGroup, this, form, formLayout);
            treeControllers.put(treeGroup, treeController);
        }

        for (GGroupObject group : form.groupObjects) {
            if (group.parent == null) {
                GGroupObjectController controller = new GGroupObjectController(this, group, formLayout);
                controllers.put(group, controller);
            }
        }

        for (GPropertyDraw property : form.propertyDraws) {
            if (property.groupObject == null) {
                controllers.put(null, new GGroupObjectController(this, null, formLayout));
            }
        }

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
        formLayout.hideEmptyContainerViews();
    }

    public void showModalDialog(GForm form, final WindowHiddenHandler handler) {
        //todo: http://stackoverflow.com/questions/2061699/disable-user-interaction-in-a-gwt-container
////        disable();
//        GModalDialog.showDialog(form, new WindowHiddenHandler() {
//            @Override
//            public void onHidden() {
////                enable();
//                handler.onHidden();
//            }
//        });
    }

    public void showModalForm(GForm form, final WindowHiddenHandler handler) {
////        disable();
//        GModalForm.showForm(form, new WindowHiddenHandler() {
//            @Override
//            public void onHidden() {
////                enable();
//                handler.onHidden();
//            }
//        });
    }

    public void showClassDialog(GObjectClass baseClass, GObjectClass defaultClass, boolean concreate, final ClassChosenHandler classChosenHandler) {
////        disable();
//        GClassDialog.showDialog(baseClass, defaultClass, concreate, new ClassChosenHandler() {
//            @Override
//            public void onClassChosen(GObjectClass chosenClass) {
////                enable();
//                classChosenHandler.onClassChosen(chosenClass);
//            }
//        });
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
            //todo: columnKeys
            syncDispatch(new ExecuteEditAction(property.ID, new GGroupObjectValueDTO(), actionSID), callback);
//            syncDispatch(new ExecuteEditAction(property.ID, key.getValueDTO(), actionSID), callback);
        }
    }

    public void contiueServerInvocation(Object[] actionResults, AsyncCallback<ServerResponseResult> callback) {
        syncDispatch(new ContinueInvocationAction(actionResults), callback);
    }

    public void throwInServerInvocation(Exception exception) {
        syncDispatch(new ThrowInInvocationAction(exception), new ErrorAsyncCallback<ServerResponseResult>());
    }

    public <T extends Result> void syncDispatch(Action<T> action, AsyncCallback<T> callback) {
//        disable();
        dispatcher.execute(action, new WrapperAsyncCallbackEx<T>(callback) {
            @Override
            public void preProcess() {
//                enable();
            }
        });
    }

    public void changeProperty(GPropertyDraw property, Serializable value) {
        dispatcher.execute(new ChangeProperty(property.ID, value), new ServerResponseCallback());
    }

    public void changeClassView(GGroupObject groupObject, GClassViewType newClassView) {
        dispatcher.execute(new ChangeClassView(groupObject.ID, newClassView), new ServerResponseCallback());
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

    public void hideForm() {
        //do nothing by default
    }

    public void runPrintReport(String reportSID) {
        openReportWindow(reportSID, "pdf");
    }

    public void runOpenInExcel(String reportSID) {
        openReportWindow(reportSID, "xls");
    }

    private void openReportWindow(String reportSID, String type) {
        String url = GWT.getHostPageBaseURL() + "report?file=" + reportSID + "&type=" + type;
        Window.open(url, "Report", "");
    }

    public void blockingConfirm(String caption, String message, final MessageBox.CloseCallback callback) {
//        disable();
        MessageBox.showConfirmBox(caption, message, new MessageBox.CloseCallback() {
            @Override
            public void closed(boolean okPressed) {
//                enable();
                callback.closed(okPressed);
            }
        });
    }

    public void blockingMessage(String caption, String message, final MessageBox.CloseCallback callback) {
        blockingMessage(false, caption, message, callback);
    }

    public void blockingMessage(boolean isError, String caption, String message, final MessageBox.CloseCallback callback) {
//        disable();

        MessageBox.showMessageBox(isError, caption, message, new MessageBox.CloseCallback() {
            @Override
            public void closed(boolean okPressed) {
//                enable();
                callback.closed(okPressed);
            }
        });
    }

    private class ServerResponseCallback extends ErrorAsyncCallback<ServerResponseResult> {
        @Override
        public void success(ServerResponseResult response) {
            actionDispatcher.dispatchResponse(response);
        }
    }
}