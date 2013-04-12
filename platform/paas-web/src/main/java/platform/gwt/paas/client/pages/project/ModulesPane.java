package platform.gwt.paas.client.pages.project;

import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.tab.Tab;
import com.smartgwt.client.widgets.tab.TabSet;
import com.smartgwt.client.widgets.tab.events.CloseClickHandler;
import com.smartgwt.client.widgets.tab.events.TabCloseClickEvent;
import paas.api.gwt.shared.dto.ModuleDTO;
import platform.gwt.paas.client.data.ModuleRecord;
import platform.gwt.paas.shared.actions.UpdateModulesAction;

import java.util.HashMap;
import java.util.HashSet;

public class ModulesPane extends HLayout {
    private HashMap<Integer, ModuleTab> idToTab = new HashMap<Integer, ModuleTab>();
    private TabSet tabSet;

    public ModulesPane() {
        super();

        tabSet = new TabSet();
        tabSet.addCloseClickHandler(new CloseClickHandler() {
            @Override
            public void onCloseClick(TabCloseClickEvent event) {
                ModuleTab closedTab = (ModuleTab) event.getTab();
                idToTab.remove(closedTab.getModuleId());
            }
        });

        addMember(tabSet);
    }

    public void openModuleTab(ModuleRecord record) {
        Tab tab = idToTab.get(record.getId());
        if (tab == null) {
            tab = createNewTab(record);
        }
        tabSet.selectTab(tab);
    }

    private Tab createNewTab(ModuleRecord record) {
        int moduleId = record.getId();
        String moduleName = record.getName();

        ModuleTab tab = new ModuleTab(moduleId);
        tab.setTitle(moduleName);

        tab.setCanClose(true);

        tab.editModule(record.getId());

        tabSet.addTab(tab);

        idToTab.put(moduleId, tab);

        return tab;
    }

    public UpdateModulesAction prepareSaveModuleAction() {
        Tab[] tabs = tabSet.getTabs();

        int moduleIds[] = new int[tabs.length];
        String moduleTexts[] = new String[tabs.length];

        for (int i = 0; i < tabs.length; ++i) {
            ModuleTab tab = (ModuleTab) tabs[i];
            moduleIds[i] = tab.getModuleId();
            moduleTexts[i] = tab.getCurrentText();
        }
        return new UpdateModulesAction(moduleIds, moduleTexts);
    }

    public void refreshModules() {
        Tab[] tabs = tabSet.getTabs();

        for (Tab t : tabs) {
            ModuleTab tab = (ModuleTab) t;
            tab.refresh();
        }
    }

    public void removeTabsForMissingModules(ModuleDTO[] modules) {
        Tab[] tabs = tabSet.getTabs();

        HashSet<Integer> newIds = new HashSet<Integer>();
        for (ModuleDTO dto : modules) {
            newIds.add(dto.id);
        }

        for (Tab t : tabs) {
            ModuleTab tab = (ModuleTab) t;
            if (!newIds.contains(tab.getModuleId())) {
                tabSet.updateTab(tab, null);
                tabSet.removeTab(tab);
                idToTab.remove(tab.getModuleId());
            }
        }
    }
}
