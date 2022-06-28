package lsfusion.gwt.client.base.view;

import lsfusion.gwt.client.controller.dispatch.GwtActionDispatcher;
import lsfusion.gwt.client.form.view.ModalForm;

public class FormRequestData {
    GwtActionDispatcher dispatcher;
    ModalForm modalForm;
    Long requestIndex;

    public FormRequestData(GwtActionDispatcher dispatcher, ModalForm modalForm, Long requestIndex) {
        this.dispatcher = dispatcher;
        this.modalForm = modalForm;
        this.requestIndex = requestIndex;
    }
}