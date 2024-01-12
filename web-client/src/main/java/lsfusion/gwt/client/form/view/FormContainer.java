package lsfusion.gwt.client.form.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.*;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.GForm;
import lsfusion.gwt.client.base.*;
import lsfusion.gwt.client.base.busy.GBusyDialog;
import lsfusion.gwt.client.base.view.StaticImageWidget;
import lsfusion.gwt.client.form.controller.FormsController;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.property.cell.controller.EndReason;
import lsfusion.gwt.client.navigator.controller.GAsyncFormController;
import lsfusion.gwt.client.navigator.window.GWindowFormType;
import lsfusion.gwt.client.view.MainFrame;

import java.util.function.BiConsumer;

// multiple inheritance
public abstract class FormContainer {
    private static final ClientMessages messages = ClientMessages.Instance.get();

    protected final FormsController formsController;

    protected Event editEvent;

    protected GFormController form;

    public boolean async;

    private boolean asyncHidden;
    private EndReason asyncHiddenReason;
    public boolean isAsyncHidden() {
        return asyncHidden;
    }

    public String formId;

    public FormContainer(FormsController formsController, boolean async, Event editEvent) {
        this.formsController = formsController;
        this.async = async;
        this.editEvent = editEvent;
    }

    protected abstract void setContent(Widget widget);

    public abstract GWindowFormType getWindowType();

    protected FormContainer getContainerForm() { // hack
        return this;
    }

    public void onAsyncInitialized() {
        assert !async;
        // if it's an active form setting focus
        if(MainFrame.getAssertCurrentForm() == getContainerForm())
            onSyncFocus(true);
    }

    public void closePressed() {
        closePressed(null);
    }

    public void closePressed(EndReason reason) {
        if(async) {
            // we shouldn't remove async form here, because it will be removed either in FormAction, or on response noneMatch FormAction check
//            asyncFormController.removeAsyncForm();
            GBusyDialog.confirmInterruptAction(() -> {
                hide(reason);
                asyncHidden = true;
                asyncHiddenReason = reason;
                MainFrame.navigatorDispatchAsync.interrupt(false);
            });
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

        assert MainFrame.getAssertCurrentForm() == this;
        MainFrame.setCurrentForm(null);
    }

    protected void onSyncFocus(boolean add) {
        if(add || focusedElement == null)
            form.focusFirstWidget(FocusUtils.Reason.SHOW);
        else
            FocusUtils.focus(focusedElement, FocusUtils.Reason.RESTOREFOCUS);
        form.gainedFocus();
    }

    private void onSyncBlur(boolean remove) {
        form.lostFocus();
        focusedElement = remove ? null : GwtClientUtils.getFocusedChild(getFocusedElement());
    }

    public abstract Element getFocusedElement();

    public void initForm(FormsController formsController, GForm gForm, BiConsumer<GAsyncFormController, EndReason> hiddenHandler, boolean isDialog, int dispatchPriority, String formId) {
        form = new GFormController(formsController, this, gForm, isDialog, dispatchPriority, editEvent) {
            @Override
            public void onFormHidden(GAsyncFormController asyncFormController, int closeDelay, EndReason editFormCloseReason) {
                super.onFormHidden(asyncFormController, closeDelay, editFormCloseReason);

                hiddenHandler.accept(asyncFormController, editFormCloseReason);
            }
        };

        if(isAsyncHidden())
            form.closePressed(asyncHiddenReason);
        else
            setContent(form.getWidget());

        async = false;

        this.formId = formId;
    }

    public abstract Widget getCaptionWidget();

    public GFormController getForm() {
        return form;
    }

    public void setContentLoading() {
        VerticalPanel loadingWidget = new VerticalPanel();
        loadingWidget.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        loadingWidget.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
        loadingWidget.setSize("100%", "100%");

        HorizontalPanel topPanel = new HorizontalPanel();
        topPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        topPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
        topPanel.setSpacing(5);

        StaticImageWidget image = new StaticImageWidget(StaticImage.LOADING_ASYNC);
        image.addStyleName("loading-async-icon");

        topPanel.add(image);
        topPanel.add(new HTML(messages.loading()));

        loadingWidget.add(topPanel);

        setContent(loadingWidget);
    }
}
