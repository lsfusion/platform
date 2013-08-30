package lsfusion.gwt.form.client.form.ui.dialog;

import com.google.gwt.dom.client.NativeEvent;
import lsfusion.gwt.base.client.ErrorHandlingCallback;
import lsfusion.gwt.base.shared.actions.NumberResult;
import lsfusion.gwt.form.client.form.FormsController;
import lsfusion.gwt.form.shared.view.GForm;
import lsfusion.gwt.form.shared.view.GKeyStroke;
import lsfusion.gwt.form.shared.view.grid.EditEvent;
import lsfusion.gwt.form.shared.view.grid.NativeEditEvent;

public class GResizableModalDialog extends GResizableModalForm {
    private final EditEvent initFilterEvent;

    public GResizableModalDialog(FormsController formsController, GForm form, final WindowHiddenHandler hiddenHandler, EditEvent initFilterEvent) {
        super(formsController, form, hiddenHandler);
        this.initFilterEvent = initFilterEvent;
    }

    @Override
    public void initialFormChangesReceived() {
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

    public static GResizableModalDialog showDialog(FormsController formsController, GForm form, EditEvent initFilterEvent, WindowHiddenHandler hiddenHandler) {
        GResizableModalDialog dlg = new GResizableModalDialog(formsController, form, hiddenHandler, initFilterEvent);
        dlg.justCenter();
        return dlg;
    }
}
