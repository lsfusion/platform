package lsfusion.gwt.client.form.view;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.GForm;
import lsfusion.gwt.client.base.Dimension;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.result.NumberResult;
import lsfusion.gwt.client.controller.remote.action.PriorityErrorHandlingCallback;
import lsfusion.gwt.client.form.controller.FormsController;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.event.GKeyStroke;
import lsfusion.gwt.client.form.property.cell.controller.EndReason;
import lsfusion.gwt.client.navigator.controller.GAsyncFormController;
import lsfusion.gwt.client.navigator.window.GWindowFormType;
import lsfusion.gwt.client.view.MainFrame;

import java.util.function.BiConsumer;

import static java.lang.Math.min;

// multiple inheritance
public abstract class FormContainer {
    private static final ClientMessages messages = ClientMessages.Instance.get();

    protected final FormsController formsController;

    protected Event editEvent;

    protected GFormController form;

    public GAsyncFormController asyncFormController;
    public boolean async;

    private boolean asyncHidden;
    private EndReason asyncHiddenReason;
    public boolean isAsyncHidden() {
        return asyncHidden;
    }

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
            hide(reason);
            asyncHidden = true;
            asyncHiddenReason = reason;
        } else {
            form.closePressed(reason);
        }
    }

    public void show() {
        show(null);
    }

    public abstract void show(Integer index);

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
            form.focusFirstWidget();
        else
            focusedElement.focus();
        form.gainedFocus();
    }

    private void onSyncBlur(boolean remove) {
        form.lostFocus();
        focusedElement = remove ? null : GwtClientUtils.getFocusedChild(getFocusedElement());
    }

    protected abstract Element getFocusedElement();

    public void initForm(FormsController formsController, GForm gForm, BiConsumer<GAsyncFormController, EndReason> hiddenHandler, boolean isDialog, boolean autoSize) {
        form = new GFormController(formsController, this, gForm, isDialog, autoSize, editEvent) {
            @Override
            public void onFormHidden(GAsyncFormController asyncFormController, int closeDelay, EndReason editFormCloseReason) {
                super.onFormHidden(asyncFormController, closeDelay, editFormCloseReason);

                hiddenHandler.accept(asyncFormController, editFormCloseReason);
            }

            @Override
            public void setFormCaption(String caption, String tooltip) {
                setCaption(caption, tooltip);
            }
        };

        if(isAsyncHidden())
            form.closePressed(asyncHiddenReason);
        else {
            setContent(form.getWidget());
            Scheduler.get().scheduleDeferred(this::initQuickFilter);
        }

        async = false;
    }

    protected abstract void setCaption(String caption, String tooltip);

    public GFormController getForm() {
        return form;
    }

    protected void initQuickFilter() {
        if (editEvent != null) {
            Event event = editEvent;
            if (GKeyStroke.isPossibleStartFilteringEvent(event) && !GKeyStroke.isSpaceKeyEvent(event)) {
                form.getInitialFilterProperty(new PriorityErrorHandlingCallback<NumberResult>() {
                    @Override
                    public void onSuccess(NumberResult result) {
                        Integer initialFilterPropertyID = (Integer) result.value;

                        if (initialFilterPropertyID != null) {
                            form.quickFilter(editEvent, initialFilterPropertyID);
                        }
                    }
                });
            }
        }
    }

    public void setContentLoading() {
        GwtClientUtils.setThemeImage(loadingAsyncImage, imageUrl -> setContent(createLoadingWidget(imageUrl)), false);
    }
    protected static String loadingAsyncImage = "loading_async.gif";
    protected static Widget createLoadingWidget(String imageUrl) {
        VerticalPanel loadingWidget = new VerticalPanel();
        loadingWidget.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        loadingWidget.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
        loadingWidget.setSize("100%", "100%");

        HorizontalPanel topPanel = new HorizontalPanel();
        topPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        topPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
        topPanel.setSpacing(5);
        Image image = new Image(imageUrl);
        image.setSize("32px", "32px");
        topPanel.add(image);
        topPanel.add(new HTML(messages.loading()));

        loadingWidget.add(topPanel);

        return loadingWidget;
    }
}
