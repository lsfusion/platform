package skolkovo.gwt.expertprofile.client.ui;

import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.VisibilityMode;
import com.smartgwt.client.widgets.StretchImgButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.layout.VStack;
import com.smartgwt.client.widgets.tab.TabSet;
import platform.gwt.sgwtbase.client.ui.TitledTab;
import platform.gwt.sgwtbase.client.ui.ToolStripPanel;
import platform.gwt.sgwtbase.client.ui.VLayout100;
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
        btnUpdate.setCapSize(0);
        btnUpdate.setWidth(280);
        btnUpdate.setHeight(27);
        btnUpdate.setBaseStyle("confirmButton");
        btnUpdate.setShowDisabledIcon(false);
        btnUpdate.setLabelHPad(1);
        btnUpdate.setLabelVPad(1);
        btnUpdate.setSrc(null);
        btnUpdate.setBorder("1px solid black");

        btnUpdate.setLayoutAlign(Alignment.CENTER);
        btnUpdate.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                updateButtonClicked();
            }
        });
    }

    public String validate() {
        return foresightPanel.validate();
    }

    public void showLoading() {
        btnUpdate.setIcon("loading.gif");

        btnUpdate.disable();
        foresightPanel.disable();
        infoPanel.disable();
    }

    public void hideLoading() {
        btnUpdate.setIcon(null);

        btnUpdate.enable();
        foresightPanel.enable();
        infoPanel.enable();
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

        mainSectionStack = new SectionStack();
        mainSectionStack.setOverflow(Overflow.VISIBLE);
        mainSectionStack.setAutoHeight();
        mainSectionStack.setVisibilityMode(VisibilityMode.MULTIPLE);
        mainSectionStack.setSections(detailsSection, foresightSection);
    }

    private void configureLayout() {
        VStack bottomPane = new VStack();
        bottomPane.setMargin(10);
        bottomPane.setWidth100();
        bottomPane.setAutoHeight();
        bottomPane.addMember(btnUpdate);

        VLayout centerPane = new VLayout100();
        centerPane.setShowEdges(true);
        centerPane.setOverflow(Overflow.AUTO);
        centerPane.setLayoutMargin(10);
        centerPane.setLayoutBottomMargin(0);
        centerPane.addMember(mainSectionStack);

        VLayout infoPane = new VLayout100();
        infoPane.addMember(centerPane);
        infoPane.addMember(bottomPane);

        votePanel.setLayoutMargin(10);
        votePanel.setMembersMargin(5);
        votePanel.setShowEdges(true);

        TabSet tabSet = new TabSet();
        tabSet.addTab(new TitledTab(messages.tabInfo(), infoPane));
        tabSet.addTab(new TitledTab(messages.tabVotes(), votePanel));

        VLayout main = new VLayout100();
        main.addMember(new ToolStripPanel(messages.title()));
        main.addMember(tabSet);

        addMember(main);
    }

}
