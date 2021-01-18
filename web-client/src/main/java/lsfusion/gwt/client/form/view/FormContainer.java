package lsfusion.gwt.client.form.view;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.GForm;
import lsfusion.gwt.client.base.Dimension;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.exception.ErrorHandlingCallback;
import lsfusion.gwt.client.base.result.NumberResult;
import lsfusion.gwt.client.base.view.WindowHiddenHandler;
import lsfusion.gwt.client.form.controller.FormsController;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.event.GKeyStroke;
import lsfusion.gwt.client.view.MainFrame;

import static java.lang.Math.min;

// multiple inheritance
public abstract class FormContainer<W extends Widget> {

    protected final FormsController formsController;
    protected final W contentWidget;

    protected Event initFilterEvent;

    protected GFormController form;

    public boolean async;

    public FormContainer(FormsController formsController) {
        this.formsController = formsController;

        this.contentWidget = initContentWidget();
    }

    protected abstract W initContentWidget();

    protected abstract void setContent(Widget widget);

    public abstract void show();

    public abstract void hide();

    private Element focusedElement;
    public void onFocus(boolean add) {
        if(!async) {
            form.gainedFocus();
        }

        MainFrame.setCurrentForm(this);
        assert !MainFrame.isModalPopup();

        if(!async) {
            if(add || focusedElement == null)
                form.focusFirstWidget();
            else
                focusedElement.focus();
            form.restorePopup();
        }
    }

    public void onBlur(boolean remove) {
        if(!async) {
            form.lostFocus();
            focusedElement = remove ? null : GwtClientUtils.getFocusedChild(contentWidget.getElement());
        }

        assert MainFrame.getAssertCurrentForm() == this;
        MainFrame.setCurrentForm(null);

        if(!async) {
            form.hidePopup();
        }
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

        setContent(form);

        Scheduler.get().scheduleDeferred(this::initQuickFilter);
        async = false;
    }

    public void asyncInitForm() {
        GwtClientUtils.setThemeImage("loading.gif", imageUrl -> setContent(new Image(imageUrl)), false);
    }

    protected abstract void setCaption(String caption, String tooltip);

    public GFormController getForm() {
        return form;
    }

    protected void initMaxPreferredSize() {
        Dimension size = form.getMaxPreferredSize();
        if (size.width > 0) {
            int wndWidth = Window.getClientWidth();
            size.width = min(size.width + 20, wndWidth - 20);
            form.setWidth(size.width + "px");
        }
        if (size.height > 0) {
            int wndHeight = Window.getClientHeight();
            size.height = min(size.height, wndHeight - 100);
            form.setHeight(size.height + "px");
        }
    }

    protected void initQuickFilter() {
        if (initFilterEvent != null) {
            Event event = initFilterEvent;
            if (GKeyStroke.isPossibleStartFilteringEvent(event) && !GKeyStroke.isSpaceKeyEvent(event)) {
                form.getInitialFilterProperty(new ErrorHandlingCallback<NumberResult>() {
                    @Override
                    public void success(NumberResult result) {
                        Integer initialFilterPropertyID = (Integer) result.value;

                        if (initialFilterPropertyID != null) {
                            form.quickFilter(initFilterEvent, initialFilterPropertyID);
                        }
                    }
                });
            }
        }
    }
}
