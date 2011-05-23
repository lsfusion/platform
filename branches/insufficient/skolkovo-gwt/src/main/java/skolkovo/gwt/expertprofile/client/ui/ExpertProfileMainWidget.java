package skolkovo.gwt.expertprofile.client.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.*;
import skolkovo.api.serialization.ProfileInfo;
import skolkovo.api.serialization.VoteInfo;
import skolkovo.gwt.base.client.BaseFrame;
import skolkovo.gwt.base.client.BaseMessages;
import skolkovo.gwt.base.client.ui.ActionColumn;
import skolkovo.gwt.base.client.ui.LinkColumn;
import skolkovo.gwt.base.client.ui.NumberColumn;
import skolkovo.gwt.base.client.ui.YesNoColumn;
import skolkovo.gwt.expertprofile.client.ExpertProfileMessages;

import java.util.ArrayList;
import java.util.List;

public abstract class ExpertProfileMainWidget extends Composite {
    interface ExpertProfileMainWidgetUiBinder extends UiBinder<Widget, ExpertProfileMainWidget> {}
    private static ExpertProfileMainWidgetUiBinder uiBinder = GWT.create(ExpertProfileMainWidgetUiBinder.class);

    private static BaseMessages baseMessages = BaseMessages.Instance.get();
    private static ExpertProfileMessages messages = ExpertProfileMessages.Instance.get();

    private final ProfileInfo pi;
    private boolean showUnvoted;

    @UiField
    SpanElement titleSpan;
    @UiField
    SpanElement nameLabelSpan;
    @UiField
    SpanElement nameSpan;
    @UiField
    Label logoffNotice;
    @UiField
    Anchor logoffLink;
    @UiField
    SpanElement emailSpan;
    @UiField
    SpanElement emailLabelSpan;
    @UiField(provided = true)
    CellTable table;
    @UiField
    CheckBox cbShowUnvoted;
    @UiField
    Label lbEmptyList;

    public ExpertProfileMainWidget(ProfileInfo pi) {
        this.pi = pi;

        createCellTable();

        initWidget(uiBinder.createAndBindUi(this));

        titleSpan.setInnerText(messages.title());
        nameLabelSpan.setInnerText(messages.name());
        nameSpan.setInnerText(pi.expertName);
        logoffNotice.setText(baseMessages.logoffNotice());
        logoffLink.setText(baseMessages.here());
        emailLabelSpan.setInnerText(messages.email());
        emailSpan.setInnerText(pi.expertEmail);
        cbShowUnvoted.setText(messages.showUnvoted());
        lbEmptyList.setText(messages.emptyVoteList());

        logoffLink.setHref(BaseFrame.getPageUrlPreservingParameters("logoff.html"));

        cbShowUnvoted.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> valueEvent) {
                showUnvoted = valueEvent.getValue();
                updateTableData();
            }
        });

        if (pi.voteInfos.length == 0) {
            table.setVisible(false);
            cbShowUnvoted.setVisible(false);
            lbEmptyList.setVisible(true);
        } else {
            updateTableData();
        }
    }

    private void updateTableData() {
        List<VoteInfo> filteredVotes = new ArrayList<VoteInfo>();
        for (VoteInfo VoteInfo : pi.voteInfos) {
            if (!showUnvoted || !VoteInfo.voteDone) {
                filteredVotes.add(VoteInfo);
            }
        }
        table.setRowCount(filteredVotes.size(), true);
        table.setRowData(0, filteredVotes);
    }

    private void createCellTable() {
        table = new CellTable<VoteInfo>();
        table.setWidth("100%");

        table.addColumn(new YesNoColumn<VoteInfo>() {
            @Override
            public Boolean getValue(VoteInfo vi) {
                return vi.voteDone;
            }
        }, messages.columnVoteResult());
        table.addColumn(new TextColumn<VoteInfo>() {
            @Override
            public String getValue(VoteInfo vi) {
                return vi.projectClaimer;
            }
        }, messages.columnProjectClaimer());

        table.addColumn(new TextColumn<VoteInfo>() {
            @Override
            public String getValue(VoteInfo vi) {
                return vi.projectName;
            }
        }, messages.columnProjectName());

        table.addColumn(new TextColumn<VoteInfo>() {
            @Override
            public String getValue(VoteInfo vi) {
                return vi.projectCluster;
            }
        }, messages.columnProjectCluster());

        table.addColumn(new NumberColumn<VoteInfo>() {
            @Override
            public Number getValue(VoteInfo vi) {
                return !vi.isVoted() ? null : vi.competent;
            }
        }, messages.columnCompetent());
        table.addColumn(new YesNoColumn<VoteInfo>() {
            @Override
            public Boolean getValue(VoteInfo vi) {
                return !vi.isVoted() ? null : vi.inCluster;
            }
        }, messages.columnInCluster());
        table.addColumn(new YesNoColumn<VoteInfo>() {
            @Override
            public Boolean getValue(VoteInfo vi) {
                return !vi.isVoted() ? null : vi.innovative;
            }
        }, messages.columnInnovative());
        table.addColumn(new NumberColumn<VoteInfo>() {
            @Override
            public Number getValue(VoteInfo vi) {
                return !vi.isVoted() ? null : vi.complete;
            }
        }, messages.columnComplete());
        table.addColumn(new YesNoColumn<VoteInfo>() {
            @Override
            public Boolean getValue(VoteInfo vi) {
                return !vi.isVoted() ? null : vi.foreign;
            }
        }, messages.columnForeign());
        table.addColumn(new LinkColumn<VoteInfo>() {
            @Override
            public String getLinkUrl(VoteInfo vi) {
                return BaseFrame.getPageUrlPreservingParameters("expert.html", "voteId", vi.linkHash);
            }

            @Override
            public String getLinkText(VoteInfo vi) {
                return messages.view();
            }
        }, messages.columnGoToBallot());
        table.addColumn(new ActionColumn<VoteInfo>(messages.send()) {
            @Override
            public void execute(VoteInfo vi) {
                onSend(vi);
            }

            @Override
            public boolean hidden(VoteInfo object) {
                return object.voteDone;
            }
        }, messages.columnSentDocs());
    }

    public abstract void onSend(VoteInfo vi);
}