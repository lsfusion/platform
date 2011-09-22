package platform.gwt.paas.client.pages.project;

import com.gwtplatform.mvp.client.UiHandlers;
import platform.gwt.paas.client.data.ModuleRecord;

public interface ProjectPageUIHandlers extends UiHandlers {
    public void moduleRecordSelected(ModuleRecord record);

    public void addNewModuleButtonClicked();

    public void saveAllButtonClicked();

    public void configurationButtonClicked();

    public void refreshButtonClicked(boolean refreshContent);

    public void removeRecordClicked(ModuleRecord record);
}
