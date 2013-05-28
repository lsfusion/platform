package platform.gwt.paas.client.pages.projectlist;

import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.widgets.Button;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.layout.HStack;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.layout.VStack;
import platform.gwt.paas.client.data.ProjectRecord;
import platform.gwt.paas.client.widgets.HLayoutWithUIHandlers;
import platform.gwt.paas.client.widgets.VSpacer;

import static platform.gwt.base.client.EscapeUtils.toHtml;

public class ProjectInfoPane extends HLayoutWithUIHandlers<ProjectsListPageUIHandlers> {

    private ProjectRecord selectedProject;

    private Label lbName;
    private Label lbDescription;
    private Label lbDescriptionText;
    private Button btnOpen;
    private Button btnDelete;
    private Button btnEdit;

    public ProjectInfoPane() {
        setMargin(5);

        createComponents();

        createButtons();

        configureLayout();

        bindUIHandlers();
    }

    private void createComponents() {
        lbName = new Label();
        lbName.setHeight(45);
        lbName.setWrap(false);
        lbName.setStyleName("projectLabel");

        lbDescription = new Label("Project description: ");
        lbDescription.setStyleName("descriptionLabel");
        lbDescription.setAutoHeight();

        lbDescriptionText = new Label();
        lbDescriptionText.setStyleName("descriptionText");
        lbDescriptionText.setWidth(500);
        lbDescriptionText.setAutoHeight();
    }

    private void createButtons() {
        btnOpen = new Button("Open");
        btnOpen.setIcon("toolbar/open.png");

        btnDelete = new Button("Delete");
        btnDelete.setIcon("toolbar/delete.png");

        btnEdit = new Button("Edit");
        btnEdit.setIcon("project_edit.png");
        btnEdit.setLayoutAlign(Alignment.CENTER);
    }

    private void configureLayout() {
        HStack bottomPane = new HStack();
        bottomPane.setMembersMargin(20);
        bottomPane.setAutoHeight();
        bottomPane.addMember(btnOpen);
        bottomPane.addMember(btnDelete);

        VLayout br = new VLayout();
        br.setStyleName("projectHeaderLine");
        br.setHeight(6);

        HStack nameContainer = new HStack();
        nameContainer.setMembersMargin(15);
        nameContainer.setAutoHeight();
        nameContainer.addMember(lbName);
        nameContainer.addMember(btnEdit);

        VStack vs = new VStack();
        vs.addMember(nameContainer);
        vs.addMember(br);
        vs.addMember(new VSpacer(25));
        vs.addMember(lbDescription);
        vs.addMember(lbDescriptionText);
        vs.addMember(bottomPane);

        addMember(vs);
    }

    private void bindUIHandlers() {
        btnOpen.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                uiHandlers.openProject(selectedProject);
            }
        });

        btnDelete.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                uiHandlers.deleteProject(selectedProject);
            }
        });

        btnEdit.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                uiHandlers.editProject(selectedProject);
            }
        });
    }

    public void setProject(ProjectRecord record) {
        selectedProject = record;

        lbName.setContents(toHtml(record.getName()));
//        lbName.setContents(record.getName());
        lbDescriptionText.setContents(toHtml(record.getDescription()));

        lbName.setAutoWidth();
        lbDescriptionText.setAutoHeight();
    }
}
