package platform.gwt.paas.shared.actions;

import net.customware.gwt.dispatch.shared.Action;

public class AddNewConfigurationAction implements Action<GetConfigurationsResult> {
    public int projectId;

    public AddNewConfigurationAction() {
    }

    public AddNewConfigurationAction(int projectId) {
        this.projectId = projectId;
    }
}
