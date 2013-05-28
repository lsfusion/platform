package platform.gwt.paas.client.pages.project;

import com.smartgwt.client.widgets.tab.Tab;

public class ModuleTab extends Tab {

    private static final String MODULE_ID = "moduleId";
    private ModuleEditor editor;

    public ModuleTab(int moduleId) {
        setModuleId(moduleId);
    }

    public void setModuleId(int moduleId) {
        setAttribute(MODULE_ID, moduleId);
    }

    public int getModuleId() {
        return getAttributeAsInt(MODULE_ID);
    }

    public String getCurrentText() {
        return editor.getCurrentText();
    }

    public void editModule(int id) {
        editor = new ModuleEditor(id);
        setPane(editor);
    }

    public void refresh() {
        editor.updateModuleText();
    }
}
