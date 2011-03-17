package platform.client.form;

import platform.client.form.cell.PropertyController;
import platform.client.logics.ClientGroupObject;

public interface PanelLogicsSupplier {
    void updateToolbar();

    ClientGroupObject getGroupObject();

    void addPropertyToToolbar(PropertyController property);

    void removePropertyFromToolbar(PropertyController property);
}
