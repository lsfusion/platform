package platform.interop;

import platform.client.form.ClientForm;
import platform.interop.ByteArraySerializer;

public class ClientPropertyView extends ClientCellView {

    protected ClientObjectValue getEditorObjectValue(ClientForm form, Object value, boolean externalID) {

        ClientChangeValue changeValue = ByteArraySerializer.deserializeClientChangeValue(form.remoteForm.getPropertyEditorObjectValueByteArray(this.ID, externalID));
        if (changeValue == null) return null;

        return changeValue.getObjectValue(value);
    }
}
