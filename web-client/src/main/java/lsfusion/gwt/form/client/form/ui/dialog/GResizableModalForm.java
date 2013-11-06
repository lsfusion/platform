package lsfusion.gwt.form.client.form.ui.dialog;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.client.Window;
import lsfusion.gwt.base.client.Dimension;
import lsfusion.gwt.base.client.ErrorHandlingCallback;
import lsfusion.gwt.base.shared.actions.NumberResult;
import lsfusion.gwt.form.client.form.FormsController;
import lsfusion.gwt.form.client.form.ui.GFormController;
import lsfusion.gwt.form.shared.view.GForm;
import lsfusion.gwt.form.shared.view.GKeyStroke;
import lsfusion.gwt.form.shared.view.grid.EditEvent;
import lsfusion.gwt.form.shared.view.grid.NativeEditEvent;

import static java.lang.Math.min;

public class GResizableModalForm extends GResizableModalWindow {

    private final EditEvent initFilterEvent;
    private final GFormController form;

    public GResizableModalForm(FormsController formsController, GForm gForm, boolean isDialog, EditEvent initFilterEvent, final WindowHiddenHandler hiddenHandler) {
        super(gForm.caption, hiddenHandler);
        this.initFilterEvent = initFilterEvent;

        form = new GFormController(formsController, gForm, true, isDialog) {
            @Override
            public void hideForm() {
                super.hideForm();
                GResizableModalForm.this.hide();
            }

            @Override
            protected void onInitialFormChangesReceived() {
                super.onInitialFormChangesReceived();
                initialFormChangesReceived();
            }
        };

        setContentWidget(form);

        //сразу добавляем в DOM, чтобы можно было посчитать естественную ширину элементов
        attach();
    }

    @Override
    protected void onLoad() {
        if (initialOnLoad) {
            Dimension size = form.getPreferredSize();
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

    public static GResizableModalForm showForm(FormsController formsController, GForm form, boolean isDialog, EditEvent initFilterEvent, final WindowHiddenHandler hiddenHandler) {
        GResizableModalForm modalForm = new GResizableModalForm(formsController, form, isDialog, initFilterEvent, hiddenHandler);
        modalForm.justCenter();
        return modalForm;
    }
}
