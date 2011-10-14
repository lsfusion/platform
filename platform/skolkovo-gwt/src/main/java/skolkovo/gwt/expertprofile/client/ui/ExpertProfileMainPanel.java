package skolkovo.gwt.expertprofile.client.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.types.TitleOrientation;
import com.smartgwt.client.types.VisibilityMode;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;
import com.smartgwt.client.widgets.layout.VLayout;
import net.customware.gwt.dispatch.client.DefaultExceptionHandler;
import net.customware.gwt.dispatch.client.standard.StandardDispatchAsync;
import platform.gwt.base.client.BaseFrame;
import platform.gwt.base.client.BaseMessages;
import platform.gwt.base.client.ui.ToolStripPanel;
import platform.gwt.base.shared.actions.VoidResult;
import platform.gwt.ui.DateCellFormatter;
import skolkovo.api.gwt.shared.ProfileInfo;
import skolkovo.api.gwt.shared.VoteInfo;
import skolkovo.gwt.expertprofile.client.ExpertProfileMessages;
import skolkovo.gwt.expertprofile.shared.actions.SentVoteDocuments;

import java.util.ArrayList;
import java.util.List;

import static skolkovo.api.gwt.shared.Result.*;

public class ExpertProfileMainPanel extends HLayout {
    private static BaseMessages baseMessages = BaseMessages.Instance.get();
    private static ExpertProfileMessages messages = ExpertProfileMessages.Instance.get();
    private final static StandardDispatchAsync expertProfileService = new StandardDispatchAsync(new DefaultExceptionHandler());

    private boolean showUnvoted = false;
    private ListGrid grid;
    private final ProfileInfo pi;

    public ExpertProfileMainPanel(ProfileInfo pi) {
        super(20);
        this.pi = pi;

        setWidth100();
        setHeight100();

        StaticTextItem lbName = new StaticTextItem();
        lbName.setTitle(messages.name());
        lbName.setValue(pi.expertName);

        StaticTextItem lbEmail = new StaticTextItem();
        lbEmail.setTitle(messages.email());
        lbEmail.setValue(pi.expertEmail);

        StaticTextItem lbNotice = new StaticTextItem();
        lbNotice.setTitle("");
        lbNotice.setValue("<i style=\"color: gray;\">" + baseMessages.logoffNotice() + "<a href=\"" + BaseFrame.getLogoffUrl() + "\">&nbsp;" + baseMessages.here() + "</a></i>");

        final DynamicForm expertDetailsForm = new DynamicForm();
        expertDetailsForm.setMargin(5);
        expertDetailsForm.setColWidths("50", "*");
        expertDetailsForm.setTitleOrientation(TitleOrientation.LEFT);
        expertDetailsForm.setFields(lbName, lbNotice, lbEmail);

        createGrid();
        setGridData();

        final CheckboxItem cbShowUnvoted = new CheckboxItem("cbUnvoted");
        cbShowUnvoted.setTitle(messages.showUnvoted());
        cbShowUnvoted.addChangedHandler(new ChangedHandler() {
            @Override
            public void onChanged(ChangedEvent event) {
                showUnvoted = cbShowUnvoted.getValueAsBoolean();
                setGridData();
            }
        });
        DynamicForm unvotedForm = new DynamicForm();
        unvotedForm.setColWidths("10", "*");
        unvotedForm.setFields(cbShowUnvoted);

        VLayout main = new VLayout();
        main.setWidth100();
        main.setHeight100();
        //??
        main.setStyleName("tabSetContainer");

        main.addMember(new ToolStripPanel(messages.title()));

        SectionStack mainSectionStack = new SectionStack();
        mainSectionStack.setMargin(20);
        mainSectionStack.setVisibilityMode(VisibilityMode.MULTIPLE);
//        mainSectionStack.setAnimateSections(true);

        SectionStackSection detailsSection = new SectionStackSection(messages.sectionExpertDetails());
        detailsSection.setItems(expertDetailsForm);
        detailsSection.setExpanded(true);

        SectionStackSection gridSection = new SectionStackSection(messages.sectionVoteList());
        gridSection.setItems(unvotedForm, grid);
        gridSection.setExpanded(true);

        //todo: delete this when[if] implemented
        if (!GWT.isScript()) {
            SectionStackSection voteSection = new SectionStackSection(messages.sectionVoteDetails());
            voteSection.setItems(new Label("to be done..."));
            voteSection.setExpanded(true);

            mainSectionStack.setSections(detailsSection, gridSection, voteSection);
        } else {
            mainSectionStack.setSections(detailsSection, gridSection);
        }

        main.addMember(mainSectionStack);

        addMember(main);
    }

    private void setGridData() {
        List<ListVoteInfo> lvInfos = new ArrayList<ListVoteInfo>();
        for (VoteInfo voteInfo : pi.voteInfos) {
            if (!showUnvoted || !voteInfo.voteDone) {
                lvInfos.add(new ListVoteInfo(voteInfo));
            }
        }

        grid.setData(lvInfos.toArray(new ListGridRecord[lvInfos.size()]));
    }

    private void createGrid() {
        grid = new ListGrid() {
            @Override
            protected Canvas createRecordComponent(final ListGridRecord record, Integer colNum) {
                final VoteInfo vi = ((ListVoteInfo) record).vi;
                if (!vi.voteDone && "sentDocs".equals(getFieldName(colNum))) {
                    final IButton btn = new IButton(messages.send());
                    btn.setShowDisabledIcon(false);
                    btn.addClickHandler(new ClickHandler() {
                        @Override
                        public void onClick(ClickEvent event) {
                            btn.disable();
                            btn.setIcon("loading.gif");

                            expertProfileService.execute(new SentVoteDocuments(vi.voteId), new AsyncCallback<VoidResult>() {
                                @Override
                                public void onFailure(Throwable caught) {
                                    SC.warn(messages.sentFailedMessage());
                                    btn.enable();
                                    btn.setIcon(null);
                                }

                                @Override
                                public void onSuccess(VoidResult result) {
                                    SC.say(messages.sentSuccessMessage());
                                    btn.enable();
                                    btn.setIcon(null);
                                }
                            });
                        }
                    });
                    return btn;
                }

                return null;
            }
        };

        grid.setWidth("100%");
        grid.setShowAllRecords(true);
        grid.setEmptyMessage(messages.emptyVoteList());
        grid.setShowRollOver(false);
        grid.setShowRecordComponents(true);
        grid.setShowRecordComponentsByCell(true);
        grid.setEmptyCellValue("--");

        ListGridField voteResultField = new ListGridField("voteResult", messages.columnVoteResult());
        ListGridField startDateField = new ListGridField("voteStartDate", messages.columnStartDate());
        startDateField.setType(ListGridFieldType.DATE);
        startDateField.setCellFormatter(DateCellFormatter.instance);

        ListGridField endDateField = new ListGridField("voteEndDate", messages.columnEndtDate());
        endDateField.setType(ListGridFieldType.DATE);
        endDateField.setCellFormatter(DateCellFormatter.instance);

        ListGridField claimerField = new ListGridField("projectClaimer", messages.columnProjectClaimer());
        ListGridField projectField = new ListGridField("projectName", messages.columnProjectName());
        ListGridField clusterField = new ListGridField("projectCluster", messages.columnProjectCluster());
        ListGridField inClusterField = new ListGridField("inCluster", messages.columnInCluster());
        inClusterField.setType(ListGridFieldType.BOOLEAN);
        inClusterField.setWidth("60");

        ListGridField innovativeField = new ListGridField("innovative", messages.columnInnovative());
        innovativeField.setType(ListGridFieldType.BOOLEAN);
        innovativeField.setWidth("60");

        ListGridField foreignField = new ListGridField("foreign", messages.columnForeign());
        foreignField.setType(ListGridFieldType.BOOLEAN);
        foreignField.setWidth("60");

        ListGridField competentField = new ListGridField("competent", messages.columnCompetent());
        competentField.setWidth("60");
        ListGridField completeField = new ListGridField("complete", messages.columnComplete());
        completeField.setWidth("60");

        ListGridField competitiveField = new ListGridField("competitive", messages.columnCompetitive());
        competitiveField.setType(ListGridFieldType.BOOLEAN);
        competitiveField.setWidth("60");

        ListGridField commercialPotentialField = new ListGridField("commercePotential", messages.columnCommercePotential());
        commercialPotentialField.setType(ListGridFieldType.BOOLEAN);
        commercialPotentialField.setWidth("60");

        ListGridField implementField = new ListGridField("implement", messages.columnImplement());
        implementField.setType(ListGridFieldType.BOOLEAN);
        implementField.setWidth("60");

        ListGridField expertiseField = new ListGridField("expertise", messages.columnExpertise());
        expertiseField.setType(ListGridFieldType.BOOLEAN);
        expertiseField.setWidth("60");

        ListGridField internationalExperienceField = new ListGridField("internationalExperience", messages.columnInternationalExperience());
        internationalExperienceField.setType(ListGridFieldType.BOOLEAN);
        internationalExperienceField.setWidth("60");

        ListGridField enoughDocumentsField = new ListGridField("enoughDocuments", messages.columnEnoughDocuments());
        enoughDocumentsField.setType(ListGridFieldType.BOOLEAN);
        enoughDocumentsField.setWidth("60");

        ListGridField ballotLinkField = new ListGridField("ballotLink", messages.columnGoToBallot());
        ballotLinkField.setLinkText(messages.view());
        ballotLinkField.setType(ListGridFieldType.LINK);

        ListGridField sentDocsField = new ListGridField("sentDocs", messages.columnSentDocs());
        sentDocsField.setAlign(Alignment.CENTER);
        sentDocsField.setWidth("130");

        grid.setFields(voteResultField, startDateField, endDateField, claimerField, projectField, clusterField, inClusterField,
                innovativeField, foreignField, competentField, completeField, competitiveField, commercialPotentialField,
                implementField, expertiseField, internationalExperienceField, enoughDocumentsField, ballotLinkField, sentDocsField);

        grid.setCanResizeFields(true);
    }

    private static class ListVoteInfo extends ListGridRecord {
        private VoteInfo vi;

        public ListVoteInfo(VoteInfo vi) {
            this.vi = vi;

            setAttribute("projectClaimer", vi.projectClaimer);
            setAttribute("projectName", vi.projectName);
            setAttribute("projectCluster", vi.projectCluster);
            setAttribute("voteResult", getVoteResultAsString(vi.voteDone, vi.voteResult));
            if (vi.isVoted()) {
                setAttribute("inCluster", vi.inCluster);
                setAttribute("innovative", vi.innovative);
                setAttribute("foreign", vi.foreign);
                setAttribute("competent", vi.competent);
                setAttribute("complete", vi.complete);

                setAttribute("competitive", vi.competitiveAdvantages);
                setAttribute("commercePotential", vi.commercePotential);
                setAttribute("implement", vi.implement);
                setAttribute("expertise", vi.expertise);
                setAttribute("internationalExperience", vi.internationalExperience);
                setAttribute("enoughDocuments", vi.enoughDocuments);
            }
            setAttribute("voteStartDate", vi.voteStartDate);
            setAttribute("voteEndDate", vi.voteEndDate);
            setAttribute("ballotLink", BaseFrame.getPageUrlPreservingParameters("expert.html", "voteId", vi.linkHash));
        }
    }

    public static String getVoteResultAsString(boolean voteDone, String voteResult) {
        if (voteDone) {
            if (VOTED.equals(voteResult)) {
                return messages.resultVoted();
            } else if (CONNECTED.equals(voteResult)) {
                return messages.resultConnected();
            } else if (REFUSED.equals(voteResult)) {
                return messages.resultRefused();
            } else {
                return messages.resultClosed();
            }
        } else {
            return messages.resultOpened();
        }
    }
}
