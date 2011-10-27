package skolkovo.gwt.expertprofile.client.ui;

import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.TitleOrientation;
import com.smartgwt.client.types.VisibilityMode;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Button;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.layout.VStack;
import platform.gwt.base.client.BaseFrame;
import platform.gwt.base.client.BaseMessages;
import platform.gwt.base.client.ui.ToolStripPanel;
import platform.gwt.base.client.ui.VLayout100;
import skolkovo.api.gwt.shared.ProfileInfo;
import skolkovo.gwt.expertprofile.client.ExpertProfileMessages;

public abstract class ExpertProfileMainPanel extends VLayout100 {
    private static BaseMessages baseMessages = BaseMessages.Instance.get();
    private static ExpertProfileMessages messages = ExpertProfileMessages.Instance.get();

    private final ProfileInfo pi;
    private DynamicForm expertDetailsForm;
    private StaticTextItem lbName;
    private StaticTextItem lbEmail;
    private StaticTextItem lbNotice;
    private Button btnUpdate;
    private SectionStack mainSectionStack;
//    private VotePanel votePanel;
    private ForesightPanel foresightPanel;

    public ExpertProfileMainPanel(ProfileInfo pi) {
        this.pi = pi;

        createDetailsSection();

        createVoteSection();

        createForesightSection();

        createSections();

        createBottomPane();

        configureLayout();
    }

    private void createDetailsSection() {
        lbName = new StaticTextItem();
        lbName.setTitle(messages.name());
        lbName.setValue(pi.expertName);

        lbEmail = new StaticTextItem();
        lbEmail.setTitle(messages.email());
        lbEmail.setValue(pi.expertEmail);

        lbNotice = new StaticTextItem();
        lbNotice.setTitle("");
        lbNotice.setValue("<i style=\"color: gray;\">" + baseMessages.logoffNotice() + "<a href=\"" + BaseFrame.getLogoffUrl() + "\">&nbsp;" + baseMessages.here() + "</a></i>");

        expertDetailsForm = new DynamicForm();
        expertDetailsForm.setMargin(5);
        expertDetailsForm.setColWidths("50", "*");
        expertDetailsForm.setTitleOrientation(TitleOrientation.LEFT);
        expertDetailsForm.setFields(lbName, lbNotice, lbEmail);
    }

    private void createVoteSection() {
//        votePanel = new VotePanel(pi);
    }

    private void createForesightSection() {
        foresightPanel = new ForesightPanel(pi);
    }

    private void createBottomPane() {
        btnUpdate = new Button(messages.update());
        btnUpdate.setShowDisabledIcon(false);
        btnUpdate.setLayoutAlign(Alignment.CENTER);
        btnUpdate.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                String error = foresightPanel.validate();
                if (error != null) {
                    SC.warn(error);
                    return;
                }

                btnUpdate.disable();
                btnUpdate.setIcon("loading.gif");
                foresightPanel.disable();
                updateButtonClicked();
            }
        });
    }

    public abstract void updateButtonClicked();

    public ProfileInfo populateProfileInfo() {
        return foresightPanel.populateProfileInfo();
    }

    private void createSections() {
        SectionStackSection detailsSection = new SectionStackSection(messages.sectionExpertDetails());
        detailsSection.setItems(expertDetailsForm);
        detailsSection.setExpanded(true);

        SectionStackSection foresightSection = new SectionStackSection(messages.sectionForesights());
        foresightSection.setItems(foresightPanel);
        foresightSection.setExpanded(true);

//        SectionStackSection gridSection = new SectionStackSection(messages.sectionVoteList());
//        gridSection.setItems(votePanel);
//        gridSection.setExpanded(false);

        mainSectionStack = new SectionStack();
        mainSectionStack.setVisibilityMode(VisibilityMode.MULTIPLE);
//        mainSectionStack.setSections(detailsSection, foresightSection, gridSection);
        mainSectionStack.setSections(detailsSection, foresightSection);
    }

    private void configureLayout() {
        VStack bottomPane = new VStack();
        bottomPane.setMargin(10);
        bottomPane.setWidth100();
        bottomPane.setAutoHeight();
        bottomPane.addMember(btnUpdate);

        VLayout centerPane = new VLayout100();
        centerPane.setLayoutMargin(10);
        centerPane.setLayoutBottomMargin(0);
        centerPane.addMember(mainSectionStack);

        VLayout main = new VLayout100();
        main.addMember(new ToolStripPanel(messages.title()));
        main.addMember(centerPane);
        main.addMember(bottomPane);

        addMember(main);
    }

}
