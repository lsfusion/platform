package platform.gwt.paas.client.pages.projectlist.edit;

import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.TitleOrientation;
import com.smartgwt.client.widgets.Button;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.events.CloseClickEvent;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;
import paas.api.gwt.shared.dto.ProjectDTO;
import platform.gwt.paas.client.data.ProjectRecord;

public class EditProjectDialog extends Window {
    private final boolean isNew;
    private ProjectRecord project;
    private final EditProjectUIHandlers uiHandlers;

    private Button btnClose;
    private Button btnResult;
    private DynamicForm editForm;
    private TextItem nameItem;
    private TextAreaItem descriptionItem;

    public EditProjectDialog(boolean isNew, ProjectRecord project, EditProjectUIHandlers uiHandlers) {
        this.isNew = isNew;
        this.project = project;
        this.uiHandlers = uiHandlers;

        setTitle(isNew ? "New project" : "Edit project");
        setWidth(350);
        setMinWidth(350);
        setHeight(300);
        setMinHeight(300);
        setShowMinimizeButton(false);
        setShowModalMask(true);
        setIsModal(true);
        setAutoCenter(true);
        setCanDragResize(true);
        setCanDragReposition(true);
        setOverflow(Overflow.HIDDEN);

        createEditForm();

        createButtons();

        configureLayout();

        bindUIHandlers();
    }

    private void configureLayout() {
        editForm.setHeight100();
        editForm.setWidth100();

        HLayout centerPane = new HLayout();
        centerPane.setShowEdges(true);
        centerPane.setMembersMargin(5);
        centerPane.addMember(editForm);

        HLayout bottomPane = new HLayout();
        bottomPane.setAlign(Alignment.RIGHT);
        bottomPane.setMargin(5);
        bottomPane.setMembersMargin(5);
        bottomPane.setAutoHeight();
        bottomPane.addMember(btnClose);
        bottomPane.addMember(btnResult);

        VLayout mainPane = new VLayout();
        mainPane.addMember(centerPane);
        mainPane.addMember(bottomPane);

        addItem(mainPane);
    }

    private void createEditForm() {
        nameItem = new TextItem(ProjectRecord.NAME_FIELD, "Name");
        nameItem.setWidth("*");

        descriptionItem = new TextAreaItem(ProjectRecord.DESCRIPTION_FIELD, "Description");
        descriptionItem.setTitleOrientation(TitleOrientation.TOP);
        descriptionItem.setColSpan(2);
        descriptionItem.setHeight("*");
        descriptionItem.setWidth("*");

        if (!isNew) {
            nameItem.setValue(project.getName());
            descriptionItem.setValue(project.getDescription());
        }

        editForm = new DynamicForm();
        editForm.setMargin(5);
        editForm.setNumCols(2);
        editForm.setColWidths("60", "*");
        editForm.setFields(nameItem, descriptionItem);
    }

    private void createButtons() {
        btnClose = new Button("Close");

        btnResult = new Button(isNew ? "Add" : "Update");
        btnResult.setIcon("icons/apply.png");
    }

    private void bindUIHandlers() {
        addCloseClickHandler(new CloseClickHandler() {
            public void onCloseClick(CloseClickEvent event) {
                destroy();
            }
        });

        btnClose.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                destroy();
            }
        });

        btnResult.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                uiHandlers.onOk(
                        new ProjectDTO(isNew ? -1 : project.getId(), nameItem.getValueAsString(), descriptionItem.getValueAsString())
                );
                destroy();
            }
        });
    }

    public static EditProjectDialog showAddDialog(EditProjectUIHandlers uiHandlers) {
        return showDialog(true, null, uiHandlers);
    }

    public static EditProjectDialog showEditDialog(ProjectRecord project, EditProjectUIHandlers uiHandlers) {
        return showDialog(false, project, uiHandlers);
    }

    private static EditProjectDialog showDialog(boolean isNew, ProjectRecord project, EditProjectUIHandlers uiHandlers) {
        EditProjectDialog wl = new EditProjectDialog(isNew, project, uiHandlers);
        wl.show();
        return wl;
    }
}
