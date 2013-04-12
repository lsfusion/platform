package platform.gwt.paas.client.pages.project.add;

import com.smartgwt.client.types.MultipleAppearance;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.TitleOrientation;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.CloseClickEvent;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.ButtonItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.SpacerItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import net.customware.gwt.dispatch.client.standard.StandardDispatchAsync;
import platform.gwt.paas.client.Paas;
import platform.gwt.paas.client.common.PaasCallback;
import platform.gwt.paas.client.data.ModuleRecord;
import platform.gwt.paas.client.data.ModulesDataSource;
import platform.gwt.paas.shared.FieldVerifier;
import platform.gwt.paas.shared.actions.GetAvailableModulesAction;
import platform.gwt.paas.shared.actions.GetModulesResult;

public class AddNewModuleDialog extends Window {
    private final StandardDispatchAsync dispatcher = Paas.dispatcher;
    private ButtonItem btnNew;
    private TextItem newItemName;
    private SelectItem modulesList;
    private ButtonItem btnAdd;
    private ButtonItem btnCancel;
    private final AddNewModuleUIHandlers uiHandlers;
    private DynamicForm moduleForm;
    private int currentProject;

    public AddNewModuleDialog(int currentProject, AddNewModuleUIHandlers uiHandlers) {
        this.currentProject = currentProject;
        this.uiHandlers = uiHandlers;

        setTitle("Add module");
        setShowMinimizeButton(false);
        setShowModalMask(true);
        setIsModal(true);
        setResizeFrom("R");
        setCanDragResize(true);
        setCanDragReposition(true);
        setWidth(360);
        setMinWidth(360);
        setAutoSize(true);
        setAutoCenter(true);
        setOverflow(Overflow.VISIBLE);

        newItemName = new TextItem("newModuleItemName", "Name");
        newItemName.setWidth("*");

        btnNew = new ButtonItem("newModuleBtn", "Create new module");
        btnNew.setStartRow(false);
        btnNew.setWidth("*");

        modulesList = new SelectItem("selectModulesItem", "Select module to add");
        modulesList.setMultiple(true);
        modulesList.setMultipleAppearance(MultipleAppearance.PICKLIST);
        modulesList.setTitleOrientation(TitleOrientation.TOP);
        modulesList.setDisplayField(ModuleRecord.NAME_FIELD);
        modulesList.setValueField(ModuleRecord.ID_FIELD);
        modulesList.setColSpan(3);
        modulesList.setWidth("*");

        btnAdd = new ButtonItem("addModuleBtn", "Add modules");
        btnAdd.setStartRow(false);
        btnAdd.setWidth("*");
        btnAdd.disable();

        btnCancel = new ButtonItem("cancelBtn", "Cancel");
        btnCancel.setWidth("*");
        btnCancel.setStartRow(false);

        moduleForm = new DynamicForm();
        moduleForm.setWidth100();
        moduleForm.setHeight100();
        moduleForm.setPadding(5);
        moduleForm.setColWidths("50", "*", "120");
        moduleForm.setNumCols(3);
        moduleForm.setFields(newItemName, btnNew,
                             modulesList,
                             new SpacerItem(), new SpacerItem(), btnAdd,
                             new SpacerItem(), new SpacerItem(), btnCancel
        );

        addItem(moduleForm);

        bindUIHandlers();

        fillValues();
    }

    private void fillValues() {
        dispatcher.execute(new GetAvailableModulesAction(currentProject), new PaasCallback<GetModulesResult>() {
            @Override
            public void success(GetModulesResult result) {
                if (result.modules.length == 0) {
                    btnAdd.disable();
                } else {
                    btnAdd.enable();
                }

                modulesList.setOptionDataSource(new ModulesDataSource(result.modules));
            }
        });
    }

    private void bindUIHandlers() {
        addCloseClickHandler(new CloseClickHandler() {
            public void onCloseClick(CloseClickEvent event) {
                destroy();
            }
        });

        btnNew.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                String moduleName = newItemName.getValueAsString();
                if (!FieldVerifier.isValidModuleName(moduleName)) {
                    SC.warn("Incorrect module name!");
                    return;
                }

                uiHandlers.onCreateNewModule(moduleName);
                destroy();
            }
        });
        btnAdd.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                ListGridRecord[] selected = modulesList.getSelectedRecords();
                if (selected == null || selected.length == 0) {
                    SC.warn("Select at least one module!");
                    return;
                }

                uiHandlers.onSelectModules(selected);
                destroy();
            }
        });
        btnCancel.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                destroy();
            }
        });
    }

    public static AddNewModuleDialog showDialog(int currentProject, AddNewModuleUIHandlers callback) {
        AddNewModuleDialog wl = new AddNewModuleDialog(currentProject, callback);
        wl.show();
        return wl;
    }
}
