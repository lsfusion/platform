package platform.gwt.form.client.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.VisibilityMode;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;
import com.smartgwt.client.widgets.layout.VStack;
import com.smartgwt.client.widgets.toolbar.ToolStripButton;
import net.customware.gwt.dispatch.client.DefaultExceptionHandler;
import net.customware.gwt.dispatch.shared.Action;
import platform.gwt.base.client.ui.ToolStripPanel;
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

import java.util.HashMap;
import java.util.LinkedHashMap;

public class GFormController extends HLayout implements FormLogicsProvider {
    private final boolean dialogMode;

    @Override
    public boolean isEditingEnabled() {
        // пока отключаем редактирование в production
        return !GWT.isScript();
    }

    private final FormDispatchAsync dispatcher = new FormDispatchAsync(new DefaultExceptionHandler());

    private GForm form;
//    private VLayout mainPane;
    private VStack mainPane;

    public GFormController(String sid) {
        this(null, new GetForm(sid), false);
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

    private HashMap<GGroupObject, GGroupObjectController> controllers = new HashMap<GGroupObject, GGroupObjectController>();

    private void initialize(GForm form) {
        this.form = form;

        dispatcher.setForm(form);

        setWidth100();
        setHeight100();
        setAutoHeight();

//        mainPane = new VLayout();
        mainPane = new VStack();
        mainPane.setWidth100();
        mainPane.setHeight100();
        mainPane.setAutoHeight();

        if (!dialogMode) {
            mainPane.addMember(new ToolStripPanel(form.caption) {
                @Override
                protected void addButtonsAfterLocaleChooser() {
                    addSeparator();

                    ToolStripButton refreshBtn = new ToolStripButton("Обновить", "refresh.png");
                    refreshBtn.addClickHandler(new ClickHandler() {
                        @Override
                        public void onClick(ClickEvent event) {
                            refreshData();
                        }
                    });

                    ToolStripButton applyBtn = new ToolStripButton("Применить", "apply.png");
                    applyBtn.addClickHandler(new ClickHandler() {
                        @Override
                        public void onClick(ClickEvent event) {
                            applyChanges();
                        }
                    });

                    ToolStripButton cancelBtn = new ToolStripButton("Отменить", "cancel.png");
                    cancelBtn.addClickHandler(new ClickHandler() {
                        @Override
                        public void onClick(ClickEvent event) {
                            cancelChanges();
                        }
                    });

                    addMember(refreshBtn);
                    addMember(applyBtn);
                    addMember(cancelBtn);
                }
            });
        }

        initializeControllers();

        initializeRegularFilters();
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
            setRegularFilter(filterGroup, filter);
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
            GRegularFilter defaultFilter = filterGroup.filters.get(filterGroup.defaultFilter);
            filterBox.setValue("" + filterGroup.defaultFilter);
            setRegularFilter(filterGroup, defaultFilter);
        }
    }

    private void setRegularFilter(GRegularFilterGroup filterGroup, GRegularFilter filter) {
        setRemoteRegularFilter(filterGroup, filter);

        //todo: orders...
//        applyOrders(filter != null ? filter.orders : filterGroup.nullOrders);

//        applyRemoteChanges();
    }

    private void addFilterComponent(GRegularFilterGroup filterGroup, FormItem item) {
        controllers.get(filterGroup.keyBindingGroup).addFilterComponent(item);
    }

    private void initializeControllers() {
        SectionStack groupsStack = new SectionStack();
        groupsStack.setWidth100();
        groupsStack.setAnimateSections(false);
        groupsStack.setMargin(20);
        groupsStack.setVisibilityMode(VisibilityMode.MULTIPLE);
        groupsStack.setCanResizeSections(true);
        groupsStack.setOverflow(Overflow.VISIBLE);

        for (GGroupObject group : form.groupObjects) {
            GGroupObjectController controller = new GGroupObjectController(this, form, group);
            groupsStack.addSection(controller.getSection());

            controllers.put(group, controller);
        }
        SectionStackSection emptySection = new SectionStackSection();
        emptySection.setCanCollapse(false);
        emptySection.setCanReorder(false);
        groupsStack.addSection(emptySection);

        HLayout hs = new HLayout();
        hs.setWidth100();
        hs.setHeight100();
        hs.addMember(groupsStack);

        mainPane.addMember(hs);
//        mainPane.addMember(groupsStack);

        addMember(mainPane);

        applyRemoteChanges(form.changes);
    }

    public void applyRemoteChanges(GFormChangesDTO changesDTO) {
        GFormChanges fc = GFormChanges.remap(form, changesDTO);
        for (GGroupObjectController controller : controllers.values()) {
            controller.processFormChanges(fc);
        }
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
        dispatcher.execute(new ChangeGroupObject(group.ID, key.getValues(group)), new FormChangesBlockingCallback());
    }

    public void changePropertyDraw(GPropertyDraw property, Object value) {
        if (isEditingEnabled()) {
            dispatcher.execute(new ChangeProperty(property.ID, value), new FormChangesBlockingCallback());
        }
    }

    public void changePropertyDraw(GGroupObject group, GGroupObjectValue key, final GPropertyDraw property, final Object value) {
        if (isEditingEnabled()) {
            executeConsecutiveActions(new ChangeGroupObject(group.ID, key.getValues(group)), new ChangeProperty(property.ID, value));
        }
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