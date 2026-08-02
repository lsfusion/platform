package lsfusion.gwt.client.form.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.*;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.GForm;
import lsfusion.gwt.client.base.FocusUtils;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.StaticImage;
import lsfusion.gwt.client.base.view.StaticImageWidget;
import lsfusion.gwt.client.base.view.WindowHiddenHandler;
import lsfusion.gwt.client.form.controller.FormsController;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.property.cell.controller.CancelReason;
import lsfusion.gwt.client.form.property.cell.controller.EndReason;
import lsfusion.gwt.client.navigator.controller.GAsyncFormController;
import lsfusion.gwt.client.navigator.window.GWindowFormType;
import lsfusion.gwt.client.view.MainFrame;

import java.util.function.BiConsumer;

// multiple inheritance
public abstract class FormContainer {
    private static final ClientMessages messages = ClientMessages.Instance.get();

    protected final FormsController formsController;

    protected final GFormController contextForm;

    protected Event editEvent;

    protected GFormController form;

    public boolean async;
    public boolean captionInitialized;

    private boolean asyncHidden;
    private EndReason asyncHiddenReason;
    public boolean isAsyncHidden() {
        return asyncHidden;
    }

    private static int formNameCounter = 0;
    // the name a custom forms view addresses this open form by - the one it writes in <Lsf name/>, and the one the
    // projection carries. Allocated HERE, not on GFormController, because an async open shows a placeholder container
    // long before the form itself arrives, and the view must be able to name it meanwhile; GFormController.globalID is
    // the same counter one level down, and keeps its own uses
    // prefixed, and not a bare number: the log window names its messages the same way, and two names from two
    // windows would otherwise match by accident - an <Lsf> given the wrong window's name would quietly place the
    // wrong thing instead of saying it is not an open form
    public final String formName = "form" + (formNameCounter++);

    // the caption the OPEN REQUEST carried, kept only until the form itself arrives: from then on a form's caption is
    // its main container's, which GFormLayout registers against this very widget. A component drawing the forms window has to name a form
    // that is still loading, and there is nothing else to name it by
    public String requestedCaption;

    public FormContainer(FormsController formsController, GFormController contextForm, boolean async, Event editEvent) {
        this.formsController = formsController;
        this.contextForm = contextForm;
        this.async = async;
        this.captionInitialized = async;
        this.editEvent = editEvent;
    }

    protected abstract void setContent(Widget widget);

    public abstract GWindowFormType getWindowType();

    protected FormContainer getContainerForm() { // hack
        return this;
    }

    public GFormController getContextForm() {
        return contextForm;
    }

    public void onAsyncInitialized() {
        assert !async;
        // if it's an active form setting focus
        if(MainFrame.getAssertCurrentForm() == getContainerForm())
            onSyncFocus(true);
    }

    public void closePressed() {
        closePressed(CancelReason.HIDE);
    }

    public void closePressed(EndReason reason) {
        if(async) {
            // we shouldn't remove async form here, because it will be removed either in FormAction, or on response noneMatch FormAction check
//            asyncFormController.removeAsyncForm();
            hide(reason);
            asyncHidden = true;
            asyncHiddenReason = reason;
        } else {
            form.closePressed(reason);
        }
    }

    public abstract void show(GAsyncFormController asyncFormController);

    // server response reaction - hideFormAction dispatch, and incorrect modalitytype when getting form, or no form at all
    public void queryHide(EndReason editFormCloseReason) {
        if(!isAsyncHidden())
            hide(editFormCloseReason);
    }
    public abstract void hide(EndReason editFormCloseReason);

    private Element focusedElement;
    public void onFocus(boolean add) {
        MainFrame.setCurrentForm(this);
        // this assertion can be broken in tooltips (since their showing is async) - for example it's showing is scheduled, change initiated, after that tooltip is showm and then response is received and message is shown
//        assert !MainFrame.isModalPopup();

        if(!async)
            onSyncFocus(add);
    }

    public void onBlur(boolean remove) {
        if(!async)
            onSyncBlur(remove);

        //When switching tabs, it is expected that currentForm is DOCKED, but it may be FLOAT NOWAIT
        //assert MainFrame.getAssertCurrentForm() == this;
        MainFrame.setCurrentForm(null);
    }

    protected void onSyncFocus(boolean add) {
        assert !async;
        if(add || focusedElement == null) {
            if (!form.focusDefaultWidget())
                focus();
        } else
            FocusUtils.focus(focusedElement, FocusUtils.Reason.RESTOREFOCUS);
        form.gainedFocus();
    }

    protected void focus() {
        FocusUtils.focusInOut(getContentElement(), FocusUtils.Reason.SHOW);
    }

    private void onSyncBlur(boolean remove) {
        form.lostFocus();
        focusedElement = remove ? null : FocusUtils.getFocusedChild(getContentElement());
    }

    public abstract Element getContentElement();

    public void initForm(FormsController formsController, WindowHiddenHandler hiddenHandler, GForm gForm, boolean isDialog, int dispatchPriority, String formId) {
        form = new GFormController(formsController, hiddenHandler, this, gForm, isDialog, formId, dispatchPriority, editEvent);

        if(isAsyncHidden())
            form.closePressed(asyncHiddenReason);
        else
            setContent(form.getWidget());

        async = false;
    }

    public abstract Widget getCaptionWidget();

    public GFormController getForm() {
        return form;
    }

    public void setContentLoading(GAsyncFormController asyncFormController) {
        VerticalPanel loadingWidget = new VerticalPanel();
        loadingWidget.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        loadingWidget.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
        loadingWidget.setSize("100%", "100%");

        HorizontalPanel topPanel = new HorizontalPanel();
        topPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        topPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
        topPanel.setSpacing(5);

        StaticImageWidget image = new StaticImageWidget(StaticImage.LOADING_ASYNC);
        GwtClientUtils.addClassName(image, "loading-async-icon");
        image.addClickHandler(e -> asyncFormController.getDispatcher().executeVoidAction(asyncFormController.getEditRequestIndex()));

        topPanel.add(image);
        topPanel.add(new HTML(messages.loading()));

        loadingWidget.add(topPanel);

        setContent(loadingWidget);
    }
}
