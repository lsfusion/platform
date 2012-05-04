package platform.gwt.paas.shared.actions;

import com.gwtplatform.dispatch.shared.UnsecuredActionImpl;

public class AddNewConfigurationAction extends UnsecuredActionImpl<GetConfigurationsResult> {
    public int projectId;

    public AddNewConfigurationAction() {
    }

    public AddNewConfigurationAction(int projectId) {
        this.projectId = projectId;
    }
}
