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
import lsfusion.gwt.client.base.view.WindowHiddenHandler;
import lsfusion.gwt.client.controller.remote.action.PriorityErrorHandlingCallback;
import lsfusion.gwt.client.form.controller.FormsController;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.event.GKeyStroke;
import lsfusion.gwt.client.navigator.controller.GAsyncFormController;
import lsfusion.gwt.client.view.MainFrame;

import static java.lang.Math.min;

// multiple inheritance
public abstract class FormContainer<W extends Widget> {
    private static final ClientMessages messages = ClientMessages.Instance.get();

    protected final FormsController formsController;
    protected final W contentWidget;

    protected Event initFilterEvent;

    protected GFormController form;

    public GAsyncFormController asyncFormController;
    public boolean async;

    public FormContainer(FormsController formsController, GAsyncFormController asyncFormController, boolean async) {
        this.formsController = formsController;
        this.asyncFormController = asyncFormController;
        this.async = async;

        this.contentWidget = initContentWidget();
    }

    protected abstract W initContentWidget();

    protected abstract void setContent(Widget widget);

    public void onAsyncInitialized() {
        assert !async;
        // if it's an active form setting focus
        if(MainFrame.getAssertCurrentForm() == this)
            onSyncFocus(true);
    }

    public abstract void show();

    public abstract void hide();

    private Element focusedElement;
    public void onFocus(boolean add) {
        MainFrame.setCurrentForm(this);
        assert !MainFrame.isModalPopup();

        if(!async)
            onSyncFocus(add);
    }

    public void onBlur(boolean remove) {
        if(!async)
            onSyncBlur(remove);

        assert MainFrame.getAssertCurrentForm() == this;
        MainFrame.setCurrentForm(null);
    }

    private void onSyncFocus(boolean add) {
        if(add || focusedElement == null)
            form.focusFirstWidget();
        else
            focusedElement.focus();
        form.gainedFocus();
    }

    private void onSyncBlur(boolean remove) {
        form.lostFocus();
        focusedElement = remove ? null : GwtClientUtils.getFocusedChild(contentWidget.getElement());
    }

    public void initForm(FormsController formsController, GForm gForm, WindowHiddenHandler hiddenHandler, boolean isDialog, Event initFilterEvent) {
        this.initFilterEvent = initFilterEvent;

        form = new GFormController(formsController, this, gForm, isDialog) {
            @Override
            public void onFormHidden(int closeDelay) {
                super.onFormHidden(closeDelay);

                hiddenHandler.onHidden();
            }

            @Override
            public void setFormCaption(String caption, String tooltip) {
                setCaption(caption, tooltip);
            }
        };

        setContent(form.getWidget());

        Scheduler.get().scheduleDeferred(this::initQuickFilter);
        async = false;
        asyncFormController = null;
    }

    protected abstract void setCaption(String caption, String tooltip);

    public GFormController getForm() {
        return form;
    }

    protected void initPreferredSize() {
        if(!async) {
            int horzClientOffset = 20;
            int vertClientOffset = 100;

            int wndWidth = Window.getClientWidth();
            int wndHeight = Window.getClientHeight();
            Dimension size = form.getPreferredSize(wndWidth - horzClientOffset, wndHeight - vertClientOffset);
            form.formLayout.setWidth(size.width + "px");
            form.formLayout.setHeight(size.height + "px");
        }
    }

    protected void initQuickFilter() {
        if (initFilterEvent != null) {
            Event event = initFilterEvent;
            if (GKeyStroke.isPossibleStartFilteringEvent(event) && !GKeyStroke.isSpaceKeyEvent(event)) {
                form.getInitialFilterProperty(new PriorityErrorHandlingCallback<NumberResult>() {
                    @Override
                    public void onSuccess(NumberResult result) {
                        Integer initialFilterPropertyID = (Integer) result.value;

                        if (initialFilterPropertyID != null) {
                            form.quickFilter(initFilterEvent, initialFilterPropertyID);
                        }
                    }
                });
            }
        }
    }

    protected String loadingAsyncImage = "loading_async.gif";
    protected Widget createLoadingWidget(String imageUrl) {
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
