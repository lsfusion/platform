package platform.gwt.paas.server.spring.mock;

import paas.api.gwt.shared.dto.ModuleDTO;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class MockModulesDAO {
    public Map<Integer, ModuleDTO> modules = new HashMap<Integer, ModuleDTO>();
    public Map<Integer, String> texts = new HashMap<Integer, String>();
    private int nextId = 100;

    public MockModulesDAO() {

    }

    public void addModule(ModuleDTO module) {
        modules.put(module.id, module);
    }

    public ModuleDTO getModule(int modId) {
        return modules.get(modId);
    }

    public Collection<ModuleDTO> getModulesDTOs() {
        return modules.values();
    }

    public ModuleDTO newModule() {
        ModuleDTO nm = new ModuleDTO();
        nm.id = nextId++;
        modules.put(nm.id, nm);
        return nm;
    }

    public String getModuleText(int moduleId) {
        return texts.get(moduleId);
    }

    public void setModuleText(int moduleId, String text) {
        texts.put(moduleId, text);
    }
}
