package skolkovo.gwt.expertprofile.client.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.types.TitleOrientation;
import com.smartgwt.client.types.VisibilityMode;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.ImgButton;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.events.ChangeEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangeHandler;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.toolbar.ToolStrip;
import com.smartgwt.client.widgets.toolbar.ToolStripButton;
import skolkovo.gwt.base.client.BaseFrame;
import skolkovo.gwt.base.client.BaseMessages;
import skolkovo.gwt.base.client.ui.DateCellFormatter;
import skolkovo.gwt.base.shared.GwtProfileInfo;
import skolkovo.gwt.base.shared.GwtVoteInfo;
import skolkovo.gwt.expertprofile.client.ExpertProfileMessages;
import skolkovo.gwt.expertprofile.client.ExpertProfileService;
import skolkovo.gwt.expertprofile.client.ExpertProfileServiceAsync;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static skolkovo.gwt.base.client.BaseFrame.getPageUrlPreservingParameters;
import static skolkovo.gwt.base.shared.Result.*;

public class ExpertProfileMainPanel extends HLayout {
    private static BaseMessages baseMessages = BaseMessages.Instance.get();
    private static ExpertProfileMessages messages = ExpertProfileMessages.Instance.get();
    private static ExpertProfileServiceAsync expertProfileService = ExpertProfileService.App.getInstance();

    private boolean showUnvoted = false;
    private ListGrid grid;
    private final GwtProfileInfo pi;

    public ExpertProfileMainPanel(GwtProfileInfo pi) {
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
        expertDetailsForm.setColWidths("0");
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
        unvotedForm.setColWidths("0");
        unvotedForm.setFields(cbShowUnvoted);

        VLayout main = new VLayout();
        main.setWidth100();
        main.setHeight100();
        //??
        main.setStyleName("tabSetContainer");

        main.addMember(setupToolBar());

        SectionStack mainSectionStack = new SectionStack();
        mainSectionStack.setMargin(20);
        mainSectionStack.setVisibilityMode(VisibilityMode.MULTIPLE);
        mainSectionStack.setAnimateSections(true);

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

    private ToolStrip setupToolBar() {
        ToolStrip topBar = new ToolStrip();
        topBar.setHeight(33);
        topBar.setWidth100();

        topBar.addSpacer(6);
        ImgButton homeButton = new ImgButton();
        homeButton.setSrc("tool_logo.png");
        homeButton.setWidth(24);
        homeButton.setHeight(24);
        homeButton.setShowRollOver(false);
        homeButton.setShowDownIcon(false);
        homeButton.setShowDown(false);
        homeButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
//                com.google.gwt.user.client.Window.open("http://i-gorod.../", "newwindow", null);
            }
        });
        topBar.addMember(homeButton);
        topBar.addSpacer(6);

        Label title = new Label(messages.title());
        title.setStyleName("logoTitle");
        title.setWidth(300);
        topBar.addMember(title);

        topBar.addFill();

        if (!GWT.isScript()) {
            ToolStripButton devConsoleButton = new ToolStripButton();
            devConsoleButton.setTitle("Developer Console");
            devConsoleButton.setIcon("bug.png");
            devConsoleButton.addClickHandler(new ClickHandler() {
                public void onClick(ClickEvent event) {
                    SC.showConsole();
                }
            });
            topBar.addButton(devConsoleButton);

            topBar.addSeparator();
        }

        topBar.addMember(createLocaleChooser());

        topBar.addSeparator();

        ToolStripButton logoffBtn = new ToolStripButton();
        logoffBtn.setTitle(messages.logoff());
        logoffBtn.setIcon("door.png");
        logoffBtn.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                logoff();
            }
        });

        topBar.addMember(logoffBtn);

        topBar.addSpacer(6);
        return topBar;
    }

    public void logoff() {
        com.google.gwt.user.client.Window.open(BaseFrame.getLogoffUrl(), "_self", null);
    }

    private static final String locales[] = {"ru", "en"};
    private static final String localesDescriptions[] = {
            baseMessages.localeRu(),
            baseMessages.localeEn()
    };

    private Canvas createLocaleChooser() {
        SelectItem selectItem = new SelectItem();
        selectItem.setHeight(21);
        selectItem.setWidth(130);
        selectItem.setShowTitle(false);

        LinkedHashMap<String, String> valueMap = new LinkedHashMap<String, String>();
        for (int i = 0; i < locales.length; ++i) {
            valueMap.put(locales[i], localesDescriptions[i]);
        }

        selectItem.setValueMap(valueMap);
        selectItem.setValues();
        selectItem.setDefaultValue(LocaleInfo.getCurrentLocale().getLocaleName());

        selectItem.addChangeHandler(new ChangeHandler() {
            public void onChange(ChangeEvent event) {
//                Cookies.setCookie(skinCookieName, (String) event.getValue());
//                com.google.gwt.user.client.Window.Location.reload();

                Window.open(getPageUrlPreservingParameters("locale", (String) event.getValue()), "_self", "");
            }
        });

        DynamicForm form = new DynamicForm();
        form.setNumCols(1);
        form.setFields(selectItem);

        return form;
    }

    private void setGridData() {
        List<ListVoteInfo> lvInfos = new ArrayList<ListVoteInfo>();
        for (GwtVoteInfo gwtVoteInfo : pi.voteInfos) {
            if (!showUnvoted || !gwtVoteInfo.voteDone) {
                lvInfos.add(new ListVoteInfo(gwtVoteInfo));
            }
        }

        grid.setData(lvInfos.toArray(new ListGridRecord[lvInfos.size()]));
    }

    private void createGrid() {
        grid = new ListGrid() {
            @Override
            protected Canvas createRecordComponent(final ListGridRecord record, Integer colNum) {
                final GwtVoteInfo vi = ((ListVoteInfo)record).vi;
                if (!vi.voteDone && "sentDocs".equals(getFieldName(colNum))) {
                    final IButton btn = new IButton(messages.send());
                    btn.setShowDisabledIcon(false);
                    btn.addClickHandler(new ClickHandler() {
                        @Override
                        public void onClick(ClickEvent event) {
                            btn.disable();
                            btn.setIcon("loading.gif");

                            expertProfileService.sentVoteDocuments(vi.voteId, new AsyncCallback<Void>() {
                                @Override
                                public void onFailure(Throwable caught) {
                                    SC.warn(messages.sentFailedMessae());
                                    btn.enable();
                                    btn.setIcon(null);
                                }

                                @Override
                                public void onSuccess(Void result) {
                                    SC.say(messages.sentSuccessMessae());
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

        ListGridField innovativeField = new ListGridField("innovative", messages.columnInnovative());
        innovativeField.setType(ListGridFieldType.BOOLEAN);

        ListGridField foreignField = new ListGridField("foreign", messages.columnForeign());
        foreignField.setType(ListGridFieldType.BOOLEAN);

        ListGridField competentField = new ListGridField("competent", messages.columnCompetent());
        ListGridField completeField = new ListGridField("complete", messages.columnComplete());

        ListGridField ballotLinkField = new ListGridField("ballotLink", messages.columnGoToBallot());
        ballotLinkField.setLinkText(messages.view());
        ballotLinkField.setType(ListGridFieldType.LINK);

        ListGridField sentDocsField = new ListGridField("sentDocs", messages.columnSentDocs());
        sentDocsField.setAlign(Alignment.CENTER);
        sentDocsField.setWidth("130");

        grid.setFields(voteResultField, startDateField, endDateField, claimerField, projectField, clusterField, inClusterField, innovativeField, foreignField, competentField, completeField, ballotLinkField, sentDocsField);

        grid.setCanResizeFields(true);
    }

    private static class ListVoteInfo extends ListGridRecord {
        private GwtVoteInfo vi;
        public ListVoteInfo(GwtVoteInfo vi) {
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
