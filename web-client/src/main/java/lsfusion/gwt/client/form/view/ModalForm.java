package lsfusion.gwt.client.form.view;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import lsfusion.gwt.client.GForm;
import lsfusion.gwt.client.base.Dimension;
import lsfusion.gwt.client.base.exception.ErrorHandlingCallback;
import lsfusion.gwt.client.base.result.NumberResult;
import lsfusion.gwt.client.base.view.ResizableModalWindow;
import lsfusion.gwt.client.base.view.WindowHiddenHandler;
import lsfusion.gwt.client.form.controller.FormsController;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.event.GKeyStroke;

import static java.lang.Math.min;

public class ModalForm extends ResizableModalWindow {

    private final Event initFilterEvent;
    private final GFormController form;

    public ModalForm(FormsController formsController, GForm gForm, boolean isDialog, Event initFilterEvent, final WindowHiddenHandler hiddenHandler) {
        super(hiddenHandler);

        this.initFilterEvent = initFilterEvent;

        form = new GFormController(formsController, gForm, true, isDialog) {
            @Override
            public void onFormHidden(int closeDelay) {
                super.onFormHidden(closeDelay);
                ModalForm.this.hide();
                unregisterForm();
            }

            @Override
            public void setFormCaption(String caption, String tooltip) {
                setCaption(caption, tooltip);
            }

            @Override
            protected void onInitialFormChangesReceived() {
                super.onInitialFormChangesReceived();
                Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                    @Override
                    public void execute() {
                        initialFormChangesReceived();
                    }
                });
            }
        };

        setContentWidget(form);

        GFormController.initKeyEventHandler(this, () -> form);
    }

    public void setCaption(String caption, String tooltip) {
        setCaption(caption);
        setTooltip(tooltip);
    }

    public GFormController getForm() {
        return form;
    }

    @Override
    protected void onLoad() {
        if (initialOnLoad) {
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
        super.onLoad();
    }

    private void initialFormChangesReceived() {
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

    public static ModalForm showForm(FormsController formsController, GForm form, boolean isDialog, Event initFilterEvent, final WindowHiddenHandler hiddenHandler) {
        ModalForm modalForm = new ModalForm(formsController, form, isDialog, initFilterEvent, hiddenHandler);
        modalForm.show();
        return modalForm;
    }
}
