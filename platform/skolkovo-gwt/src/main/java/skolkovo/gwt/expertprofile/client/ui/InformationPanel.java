package skolkovo.gwt.expertprofile.client.ui;

import com.smartgwt.client.types.TitleOrientation;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import platform.gwt.base.client.BaseFrame;
import platform.gwt.base.client.BaseMessages;
import platform.gwt.base.client.ui.VLayout100;
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
        lbNotice.setValue("<i style=\"color: gray;\">" + baseMessages.logoffNotice() + "" +
                          "<a href=\"" + BaseFrame.getLogoffUrl() + "\">&nbsp;" + baseMessages.here() + "</a></i>");

        detailsForm = new DynamicForm();
        detailsForm.setMargin(5);
        detailsForm.setColWidths("50", "*");
        detailsForm.setTitleOrientation(TitleOrientation.LEFT);
        detailsForm.setFields(lbName, lbNotice, lbEmail);
    }

    private void createExpertiseClassForm() {
        final CheckboxItem cbTechnical = new CheckboxItem();
        cbTechnical.setTitle(messages.classTechnical());
        cbTechnical.setWrapTitle(true);
        cbTechnical.setWidth("*");
        cbTechnical.setColSpan("*");
        cbTechnical.setShowTitle(false);
        cbTechnical.setValue(pi.technical);
        cbTechnical.addChangedHandler(new ChangedHandler() {
            @Override
            public void onChanged(ChangedEvent event) {
                pi.technical = cbTechnical.getValueAsBoolean();
            }
        });

        final CheckboxItem cbBusiness = new CheckboxItem();
        cbBusiness.setTitle(messages.classBusiness());
        cbBusiness.setWrapTitle(true);
        cbBusiness.setWidth("*");
        cbBusiness.setColSpan("*");
        cbBusiness.setShowTitle(false);
        cbBusiness.setValue(pi.business);
        cbBusiness.addChangedHandler(new ChangedHandler() {
            @Override
            public void onChanged(ChangedEvent event) {
                pi.business = cbBusiness.getValueAsBoolean();
            }
        });

        expertiseForm = new DynamicForm();
        expertiseForm.setIsGroup(true);
        expertiseForm.setGroupTitle(messages.expertiseClassPrompt());
        expertiseForm.setWidth100();
        expertiseForm.setMargin(5);
        expertiseForm.setAutoHeight();
        expertiseForm.setNumCols(1);
        expertiseForm.setFields(cbTechnical, cbBusiness);
    }

    private void createApplicationTypeForm() {
        final CheckboxItem cbExpertise = new CheckboxItem();
        cbExpertise.setTitle(messages.appTypeExpertise());
        cbExpertise.setWrapTitle(true);
        cbExpertise.setWidth("*");
        cbExpertise.setColSpan("*");
        cbExpertise.setShowTitle(false);
        cbExpertise.setValue(pi.technical);
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
        cbGrant.setValue(pi.business);
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
