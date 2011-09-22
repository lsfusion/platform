package platform.gwt.paas.server.spring.mock;

import paas.api.gwt.shared.dto.ConfigurationDTO;
import paas.api.gwt.shared.dto.ModuleDTO;
import paas.api.gwt.shared.dto.ProjectDTO;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Project {
    public ProjectDTO dto;
    private Map<Integer, ModuleDTO> modules = new HashMap<Integer, ModuleDTO>();
    private Map<Integer, ConfigurationDTO> configurations = new HashMap<Integer, ConfigurationDTO>();

    public Project(ProjectDTO dto) {
        this.dto = dto;
    }

    public void add(ModuleDTO module) {
        modules.put(module.id, module);
    }

    public void add(ConfigurationDTO configuration) {
        configurations.put(configuration.id, configuration);
    }

    public Set<ModuleDTO> getModules() {
        return new HashSet<ModuleDTO>(modules.values());
    }

    public Set<ConfigurationDTO> getConfigurations() {
        return new HashSet<ConfigurationDTO>(configurations.values());
    }

    public ConfigurationDTO getConfiguration(int id) {
        return configurations.get(id);
    }

    public void removeConfiguration(ConfigurationDTO dto) {
        configurations.remove(dto.id);
    }
}
