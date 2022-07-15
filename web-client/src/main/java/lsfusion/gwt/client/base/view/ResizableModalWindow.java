package lsfusion.gwt.client.base.view;

import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.form.view.ModalForm;

public class ResizableModalWindow extends ResizableWindow {

    private final DivWidget modalBackDrop;
    //This scheme is necessary when one modal window is started before the second one, but is displayed later due to delays.
    //In this case, the order in which the windows are displayed must be maintained according to the order of request indexes.
    private FormRequestData formRequestData;

    public ResizableModalWindow() {
        super();

        modalBackDrop = new DivWidget();
        modalBackDrop.setStyleName("modal-backdrop");
    }

    public void show(FormRequestData formRequestData, Integer insertIndex) {
        this.formRequestData = formRequestData;
        super.show(insertIndex, modalBackDrop);
    }

    public Pair<ModalForm, Integer> getFormInsertIndex(FormRequestData formRequestData) {
        AbsolutePanel boundaryPanel = getBoundaryPanel();
        for (int i = 0; i < boundaryPanel.getWidgetCount(); i++) {
            Widget widget = boundaryPanel.getWidget(i);
            if (widget instanceof ResizableModalWindow) {
                FormRequestData widgetData = ((ResizableModalWindow) widget).formRequestData;
                if (widgetData.isBefore(formRequestData)) {
                    return new Pair(widgetData.modalForm, i);
                }
            }
        }
        return null;
    }

    public void hide() {
        super.hide(modalBackDrop);
        formRequestData = null;
    }
}
