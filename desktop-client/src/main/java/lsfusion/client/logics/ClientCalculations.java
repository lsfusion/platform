package lsfusion.client.logics;

import lsfusion.client.ClientResourceBundle;

public class ClientCalculations extends ClientComponent {
    public ClientCalculations() {}

    @Override
    public String getCaption() {
        return ClientResourceBundle.getString("logics.calculations");
    }
}
