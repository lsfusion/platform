package skolkovo.gwt.expertprofile.client.ui;

import com.google.gwt.i18n.client.LocaleInfo;
import com.smartgwt.client.types.*;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import net.customware.gwt.dispatch.client.DefaultExceptionHandler;
import net.customware.gwt.dispatch.client.standard.StandardDispatchAsync;
import platform.gwt.base.client.AsyncCallbackEx;
import platform.gwt.base.client.GwtClientUtils;
import platform.gwt.base.shared.actions.VoidResult;
import platform.gwt.sgwtbase.client.ui.VLayout100;
import platform.gwt.sgwtbase.client.ui.DateCellFormatter;
import skolkovo.api.gwt.shared.ProfileInfo;
import skolkovo.api.gwt.shared.VoteInfo;
import skolkovo.gwt.expertprofile.client.ExpertProfileMessages;
import skolkovo.gwt.expertprofile.shared.actions.SentVoteDocuments;

import java.util.ArrayList;
import java.util.List;

import static skolkovo.api.gwt.shared.Result.*;

public class VotePanel extends VLayout100 {
    private static ExpertProfileMessages messages = ExpertProfileMessages.Instance.get();
    private final static StandardDispatchAsync expertProfileService = new StandardDispatchAsync(new DefaultExceptionHandler());

    private boolean showUnvoted = true;
    private ListGrid grid;
    private DynamicForm unvotedForm;
    private CheckboxItem cbShowUnvoted;
    private ProfileInfo pi;

    public VotePanel(ProfileInfo PI) {
        this.pi = PI;

        createForm();

        createGrid();

        setGridData();

        addMember(unvotedForm);
        addMember(grid);
    }

    private void createForm() {
        cbShowUnvoted = new CheckboxItem("cbUnvoted");
        cbShowUnvoted.setValue(true);
        cbShowUnvoted.setTitle(messages.showUnvoted());
        cbShowUnvoted.addChangedHandler(new ChangedHandler() {
            @Override
            public void onChanged(ChangedEvent event) {
                showUnvoted = cbShowUnvoted.getValueAsBoolean();
                setGridData();
            }
        });
        unvotedForm = new DynamicForm();
        unvotedForm.setColWidths("10", "*");
        unvotedForm.setFields(cbShowUnvoted);
    }

    private void setGridData() {
        List<ListVoteInfo> lvInfos = new ArrayList<ListVoteInfo>();
        for (VoteInfo voteInfo : pi.voteInfos) {
            if (!showUnvoted || !voteInfo.voteDone) {
                lvInfos.add(new ListVoteInfo(voteInfo));
            }
        }

        grid.setData(lvInfos.toArray(new ListVoteInfo[lvInfos.size()]));
    }

    private void createGrid() {
        grid = new ListGrid() {
            @Override
            protected Canvas createRecordComponent(final ListGridRecord record, Integer colNum) {
                final VoteInfo vi = ((ListVoteInfo) record).vi;
                if (!vi.voteDone && "sentDocs".equals(getFieldName(colNum))) {
                    return new SendDocumentsButton(vi);
                }

                return null;
            }

            @Override
            public Canvas updateRecordComponent(ListGridRecord record, Integer colNum, Canvas component, boolean recordChanged) {
                final VoteInfo vi = ((ListVoteInfo) record).vi;
                if (!vi.voteDone && "sentDocs".equals(getFieldName(colNum))) {
                    final SendDocumentsButton btn = (SendDocumentsButton) component;
                    btn.setVoteInfo(vi);
                    return btn;
                }
                return null;
            }
        };

        grid.setWidth100();
        grid.setShowAllRecords(true);
        grid.setEmptyMessage(messages.emptyVoteList());
        grid.setShowRollOver(false);
        grid.setShowRecordComponents(true);
        grid.setShowRecordComponentsByCell(true);
        grid.setRecordComponentPoolingMode(RecordComponentPoolingMode.RECYCLE);
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
        competentField.setType(ListGridFieldType.INTEGER);
        competentField.setWidth("60");
        ListGridField completeField = new ListGridField("complete", messages.columnComplete());
        completeField.setType(ListGridFieldType.INTEGER);
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
            setAttribute("ballotLink",
                         GwtClientUtils.getPageUrlPreservingParameters("expert.html",
                                                                       "voteId", vi.linkHash,
                                                                       "locale", LocaleInfo.getCurrentLocale().getLocaleName()));
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

    private static class SendDocumentsButton extends IButton {
        public VoteInfo voteInfo;

        public SendDocumentsButton(VoteInfo vi) {
            super(messages.send());

            setVoteInfo(vi);
            addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    if (voteInfo == null) {
                        return;
                    }

                    disable();
                    setIcon("loading.gif");

                    expertProfileService.execute(new SentVoteDocuments(voteInfo.voteId), new AsyncCallbackEx<VoidResult>() {
                        @Override
                        public void failure(Throwable caught) {
                            SC.warn(messages.sentFailedMessage());
                        }

                        @Override
                        public void success(VoidResult result) {
                            SC.say(messages.sentSuccessMessage());
                        }

                        @Override
                        public void postProcess() {
                            enable();
                            setIcon(null);
                        }
                    });
                }
            });
        }

        public void setVoteInfo(VoteInfo vi) {
            this.voteInfo = vi;
        }
    }
}
