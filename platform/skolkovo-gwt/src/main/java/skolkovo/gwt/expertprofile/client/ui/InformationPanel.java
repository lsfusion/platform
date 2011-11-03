package skolkovo.gwt.expertprofile.client.ui;

import com.smartgwt.client.types.TitleOrientation;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.FormItemIfFunction;
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import platform.gwt.base.client.GwtClientUtils;
import platform.gwt.base.client.BaseMessages;
import platform.gwt.sgwtbase.client.ui.VLayout100;
import skolkovo.api.gwt.shared.ProfileInfo;
import skolkovo.gwt.expertprofile.client.ExpertProfileMessages;

public class InformationPanel extends VLayout100 {
    private static BaseMessages baseMessages = BaseMessages.Instance.get();
    private static ExpertProfileMessages messages = ExpertProfileMessages.Instance.get();

    private ProfileInfo pi;

    private DynamicForm detailsForm;
    private DynamicForm expertiseForm;
    private DynamicForm applicationForm;

    public InformationPanel(ProfileInfo PI) {
        this.pi = PI;

        setAutoHeight();
        setMembersMargin(5);

        createDetailsForm();
        createExpertiseClassForm();
        createApplicationTypeForm();

        addMember(detailsForm);
        addMember(expertiseForm);
        addMember(applicationForm);
    }

    public void createDetailsForm() {
        StaticTextItem lbName = new StaticTextItem();
        lbName.setTitle(messages.name());
        lbName.setValue(pi.expertName);

        StaticTextItem lbEmail = new StaticTextItem();
        lbEmail.setTitle(messages.email());
        lbEmail.setValue(pi.expertEmail);

        StaticTextItem lbNotice = new StaticTextItem();
        lbNotice.setTitle("");
        lbNotice.setValue("<i style=\"color: gray;\">" + baseMessages.logoutNotice() + "" +
                          "<a href=\"" + GwtClientUtils.getLogoutUrl() + "\">&nbsp;" + baseMessages.here() + "</a></i>");

        detailsForm = new DynamicForm();
        detailsForm.setMargin(5);
        detailsForm.setColWidths("50", "*");
        detailsForm.setTitleOrientation(TitleOrientation.LEFT);
        detailsForm.setFields(lbName, lbNotice, lbEmail);
    }

    private void createExpertiseClassForm() {
        final TextAreaItem taScientific = new TextAreaItem();
        taScientific.setTitleOrientation(TitleOrientation.TOP);
        taScientific.setTitle(messages.classHint());
        taScientific.setEndRow(true);
        taScientific.setStartRow(true);
        taScientific.setWidth("*");
        taScientific.setHeight(50);
        taScientific.setValue(pi.commentScientific);
        taScientific.addChangedHandler(new ChangedHandler() {
            @Override
            public void onChanged(ChangedEvent event) {
                pi.commentScientific = taScientific.getValueAsString();
            }
        });

        final CheckboxItem cbScientific = new CheckboxItem();
        cbScientific.setTitle(messages.classScientific());
        cbScientific.setWrapTitle(true);
        cbScientific.setWidth("*");
        cbScientific.setColSpan("*");
        cbScientific.setShowTitle(false);
        cbScientific.setValue(pi.scientific);
        cbScientific.setRedrawOnChange(true);
        cbScientific.addChangedHandler(new ChangedHandler() {
            @Override
            public void onChanged(ChangedEvent event) {
                pi.scientific = cbScientific.getValueAsBoolean();
            }
        });

        taScientific.setShowIfCondition(new FormItemIfFunction() {
            @Override
            public boolean execute(FormItem item, Object value, DynamicForm form) {
                return cbScientific.getValueAsBoolean();
            }
        });

        final TextAreaItem taTechnical = new TextAreaItem();
        taTechnical.setTitleOrientation(TitleOrientation.TOP);
        taTechnical.setTitle(messages.classHint());
        taTechnical.setEndRow(true);
        taTechnical.setStartRow(true);
        taTechnical.setWidth("*");
        taTechnical.setHeight(50);
        taTechnical.setValue(pi.commentTechnical);
        taTechnical.addChangedHandler(new ChangedHandler() {
            @Override
            public void onChanged(ChangedEvent event) {
                pi.commentTechnical = taTechnical.getValueAsString();
            }
        });

        final CheckboxItem cbTechnical = new CheckboxItem();
        cbTechnical.setTitle(messages.classTechnical());
        cbTechnical.setWrapTitle(true);
        cbTechnical.setWidth("*");
        cbTechnical.setColSpan("*");
        cbTechnical.setShowTitle(false);
        cbTechnical.setValue(pi.technical);
        cbTechnical.setRedrawOnChange(true);
        cbTechnical.addChangedHandler(new ChangedHandler() {
            @Override
            public void onChanged(ChangedEvent event) {
                pi.technical = cbTechnical.getValueAsBoolean();
            }
        });

        taTechnical.setShowIfCondition(new FormItemIfFunction() {
            @Override
            public boolean execute(FormItem item, Object value, DynamicForm form) {
                return cbTechnical.getValueAsBoolean();
            }
        });

        final TextAreaItem taBusiness = new TextAreaItem();
        taBusiness.setTitleOrientation(TitleOrientation.TOP);
        taBusiness.setTitle(messages.classHint());
        taBusiness.setEndRow(true);
        taBusiness.setStartRow(true);
        taBusiness.setWidth("*");
        taBusiness.setHeight(50);
        taBusiness.setValue(pi.commentBusiness);
        taBusiness.addChangedHandler(new ChangedHandler() {
            @Override
            public void onChanged(ChangedEvent event) {
                pi.commentBusiness = taBusiness.getValueAsString();
            }
        });

        final CheckboxItem cbBusiness = new CheckboxItem();
        cbBusiness.setTitle(messages.classBusiness());
        cbBusiness.setWrapTitle(true);
        cbBusiness.setWidth("*");
        cbBusiness.setColSpan("*");
        cbBusiness.setShowTitle(false);
        cbBusiness.setValue(pi.business);
        cbBusiness.setRedrawOnChange(true);
        cbBusiness.addChangedHandler(new ChangedHandler() {
            @Override
            public void onChanged(ChangedEvent event) {
                pi.business = cbBusiness.getValueAsBoolean();
            }
        });

        taBusiness.setShowIfCondition(new FormItemIfFunction() {
            @Override
            public boolean execute(FormItem item, Object value, DynamicForm form) {
                return cbBusiness.getValueAsBoolean();
            }
        });

        expertiseForm = new DynamicForm();
        expertiseForm.setIsGroup(true);
        expertiseForm.setGroupTitle(messages.expertiseClassPrompt());
        expertiseForm.setWidth100();
        expertiseForm.setMargin(5);
        expertiseForm.setAutoHeight();
        expertiseForm.setNumCols(1);
        expertiseForm.setFields(cbScientific, taScientific, cbTechnical, taTechnical, cbBusiness, taBusiness);
    }

    private void createApplicationTypeForm() {
        final CheckboxItem cbExpertise = new CheckboxItem();
        cbExpertise.setTitle(messages.appTypeExpertise());
        cbExpertise.setWrapTitle(true);
        cbExpertise.setWidth("*");
        cbExpertise.setColSpan("*");
        cbExpertise.setShowTitle(false);
        cbExpertise.setValue(pi.expertise);
        cbExpertise.addChangedHandler(new ChangedHandler() {
            @Override
            public void onChanged(ChangedEvent event) {
                pi.expertise = cbExpertise.getValueAsBoolean();
            }
        });

        final CheckboxItem cbGrant = new CheckboxItem();
        cbGrant.setTitle(messages.appTypeGrant());
        cbGrant.setWrapTitle(true);
        cbGrant.setWidth("*");
        cbGrant.setColSpan("*");
        cbGrant.setShowTitle(false);
        cbGrant.setValue(pi.grant);
        cbGrant.addChangedHandler(new ChangedHandler() {
            @Override
            public void onChanged(ChangedEvent event) {
                pi.grant = cbGrant.getValueAsBoolean();
            }
        });

        applicationForm = new DynamicForm();
        applicationForm.setIsGroup(true);
        applicationForm.setGroupTitle(messages.appTypePropmpt());
        applicationForm.setWidth100();
        applicationForm.setMargin(5);
        applicationForm.setAutoHeight();
        applicationForm.setNumCols(1);
        applicationForm.setFields(cbExpertise, cbGrant);
    }
}
