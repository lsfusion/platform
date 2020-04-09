package lsfusion.gwt.client.form.view;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.NativeEvent;
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
import lsfusion.gwt.client.form.property.cell.controller.EditEvent;
import lsfusion.gwt.client.form.property.cell.controller.NativeEditEvent;

import static java.lang.Math.min;

public class ModalForm extends ResizableModalWindow {

    private final EditEvent initFilterEvent;
    private final GFormController form;

    public ModalForm(FormsController formsController, GForm gForm, boolean isDialog, EditEvent initFilterEvent, final WindowHiddenHandler hiddenHandler) {
        super(gForm.caption, gForm.getTooltip(), hiddenHandler);
        this.initFilterEvent = initFilterEvent;

        form = new GFormController(formsController, gForm, true, isDialog) {
            @Override
            public void onFormHidden(int closeDelay) {
                super.onFormHidden(closeDelay);
                ModalForm.this.hide();
                unregisterForm();
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

        //сразу добавляем в DOM, чтобы можно было посчитать естественную ширину элементов
        attach();
    }

    public GFormController getForm() {
        return form;
    }

    @Override
    protected void onAttach() {
        super.onAttach();
        form.modalFormAttached();
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
        if (initFilterEvent != null && initFilterEvent instanceof NativeEditEvent) {
            NativeEvent event = ((NativeEditEvent) initFilterEvent).getNativeEvent();
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

    public static ModalForm showForm(FormsController formsController, GForm form, boolean isDialog, EditEvent initFilterEvent, final WindowHiddenHandler hiddenHandler) {
        ModalForm modalForm = new ModalForm(formsController, form, isDialog, initFilterEvent, hiddenHandler);
        modalForm.justCenter();
        return modalForm;
    }
}
