package lsfusion.gwt.client.base.view;

import lsfusion.gwt.client.action.GActionDispatcherLookAhead;
import lsfusion.gwt.client.form.property.cell.controller.EndReason;
import lsfusion.gwt.client.navigator.controller.GAsyncFormController;

public interface WindowHiddenHandler {
    void onHidden(GActionDispatcherLookAhead lookAhead, GAsyncFormController formController, EndReason reason);
}
