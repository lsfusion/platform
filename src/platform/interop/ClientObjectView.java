package platform.interop;

import platform.client.form.PropertyEditorComponent;
import platform.client.form.ClientForm;

public class ClientObjectView extends ClientCellView {

    public ClientObjectImplement object;

    public int getMaximumWidth() {
        return getPreferredWidth();
    }

    public PropertyEditorComponent getEditorComponent(ClientForm form, Object value, boolean isDataChanging, boolean externalID) {

        if (externalID) return null;

        if (!groupObject.singleViewType) {
            form.switchClassView(groupObject);
            return null;
        } else
            return super.getEditorComponent(form, value, isDataChanging, externalID);
    }

}
