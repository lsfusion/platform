package lsfusion.gwt.client.base.view;

import lsfusion.gwt.client.controller.dispatch.GwtActionDispatcher;
import lsfusion.gwt.client.form.view.ModalForm;

public class FormRequestData {
    public final GwtActionDispatcher dispatcher;
    public final ModalForm modalForm;
    public final long requestIndex;

    public FormRequestData(GwtActionDispatcher dispatcher, ModalForm modalForm, long requestIndex) {
        this.dispatcher = dispatcher;
        this.modalForm = modalForm;
        this.requestIndex = requestIndex;
    }

    public boolean isBefore(FormRequestData formRequestData) {
        return dispatcher.equals(formRequestData.dispatcher) && requestIndex < formRequestData.requestIndex;
    }
}