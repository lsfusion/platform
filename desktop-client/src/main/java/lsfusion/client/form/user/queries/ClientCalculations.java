package lsfusion.client.form.user.queries;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.layout.ClientComponent;

public class ClientCalculations extends ClientComponent {
    public ClientCalculations() {}

    @Override
    public String getCaption() {
        return ClientResourceBundle.getString("logics.calculations");
    }
}
