package platform.gwt.paas.client.pages.project.add;

import com.smartgwt.client.widgets.grid.ListGridRecord;

public interface AddNewModuleUIHandlers {
    void onCreateNewModule(String moduleName);

    void onSelectModules(ListGridRecord[] modules);
}
