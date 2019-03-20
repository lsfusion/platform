package lsfusion.client.form.object.table.grid.user.toolbar;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.design.ClientComponent;

public class ClientCalculations extends ClientComponent {
    public ClientCalculations() {}

    @Override
    public String getCaption() {
        return ClientResourceBundle.getString("logics.calculations");
    }
}
