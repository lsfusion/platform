package skolkovo.gwt.expertprofile.client.ui;

import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.VisibilityMode;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.StretchImgButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.layout.VStack;
import platform.gwt.base.client.ui.ToolStripPanel;
import platform.gwt.base.client.ui.VLayout100;
import skolkovo.api.gwt.shared.ProfileInfo;
import skolkovo.gwt.expertprofile.client.ExpertProfileMessages;

public abstract class ExpertProfileMainPanel extends VLayout100 {
    private static ExpertProfileMessages messages = ExpertProfileMessages.Instance.get();

    private final ProfileInfo pi;
    private StretchImgButton btnUpdate;
    private SectionStack mainSectionStack;

    private InformationPanel infoPanel;
    private VotePanel votePanel;
    private ForesightPanel foresightPanel;

    public ExpertProfileMainPanel(ProfileInfo pi) {
        this.pi = pi;

        createInformationSection();

        createVoteSection();

        createForesightSection();

        createSections();

        createBottomPane();

        configureLayout();
    }

    private void createInformationSection() {
        infoPanel = new InformationPanel(pi);
    }

    private void createVoteSection() {
        votePanel = new VotePanel(pi);
    }

    private void createForesightSection() {
        foresightPanel = new ForesightPanel(pi);
    }

    private void createBottomPane() {
        btnUpdate = new StretchImgButton();
        btnUpdate.setTitle(messages.update());
        btnUpdate.setWidth(280);
        btnUpdate.setHeight(27);
        btnUpdate.setBaseStyle("confirmButton");
        btnUpdate.setShowDisabledIcon(false);
        btnUpdate.setLabelHPad(0);
        btnUpdate.setLabelVPad(0);
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
                infoPanel.disable();
                updateButtonClicked();
            }
        });
    }

    public abstract void updateButtonClicked();

    public ProfileInfo populateProfileInfo() {
        return pi;
    }

    private void createSections() {
        SectionStackSection detailsSection = new SectionStackSection(messages.sectionExpertDetails());
        detailsSection.setItems(infoPanel);
        detailsSection.setExpanded(true);

        SectionStackSection foresightSection = new SectionStackSection(messages.sectionForesights());
        foresightSection.setItems(foresightPanel);
        foresightSection.setExpanded(true);

//        SectionStackSection gridSection = new SectionStackSection(messages.sectionVoteList());
//        gridSection.setItems(votePanel);
//        gridSection.setExpanded(false);

        mainSectionStack = new SectionStack();
        mainSectionStack.setOverflow(Overflow.VISIBLE);
        mainSectionStack.setAutoHeight();
        mainSectionStack.setVisibilityMode(VisibilityMode.MULTIPLE);
        mainSectionStack.setSections(detailsSection, foresightSection);
//        mainSectionStack.setSections(detailsSection, foresightSection, gridSection);
    }

    private void configureLayout() {
        VStack bottomPane = new VStack();
        bottomPane.setMargin(10);
        bottomPane.setWidth100();
        bottomPane.setAutoHeight();
        bottomPane.addMember(btnUpdate);

        VLayout centerPane = new VLayout100();
        centerPane.setShowEdges(true);
        centerPane.setHeight100();
        centerPane.setOverflow(Overflow.AUTO);
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
