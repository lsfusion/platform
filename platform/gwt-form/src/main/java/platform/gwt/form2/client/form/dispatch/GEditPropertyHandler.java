package platform.gwt.form2.client.form.dispatch;

import platform.gwt.form2.shared.actions.form.ServerResponseResult;
import platform.gwt.form2.shared.view.classes.GType;

public interface GEditPropertyHandler {
    public void requestValue(GType valueType, Object oldValue);

    public void updateEditValue(Object value);

    void postDispatchResponse(ServerResponseResult response);
}
