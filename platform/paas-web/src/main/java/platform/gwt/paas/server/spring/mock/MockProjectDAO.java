package platform.gwt.paas.server.spring.mock;

import paas.api.gwt.shared.dto.ConfigurationDTO;
import paas.api.gwt.shared.dto.ProjectDTO;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings({"ToArrayCallWithZeroLengthArrayArgument"})
public class MockProjectDAO {

    private Map<Integer, Project> projects = new HashMap<Integer, Project>();
    private Map<Integer, ProjectDTO> projectsDTOS = new HashMap<Integer, ProjectDTO>();
    private Map<Integer, ConfigurationDTO> configs = new HashMap<Integer, ConfigurationDTO>();

    private final MockModulesDAO daoMod;
    private int nextId = 1000;

    public MockProjectDAO(MockModulesDAO daoMod) {
        this.daoMod = daoMod;
    }

    public void addProject(ProjectDTO dto) {
        Project proj = new Project(dto);

        projects.put(dto.id, proj);
        projectsDTOS.put(dto.id, dto);
    }

    public void addModulesToProject(int projId, int... modIds) {
        for (int modId : modIds) {
            getProject(projId).add(daoMod.getModule(modId));
        }
    }

    public void addConfiguration(int projId, ConfigurationDTO configurationDTO) {
        configs.put(configurationDTO.id, configurationDTO);
        getProject(projId).add(configurationDTO);
    }

    public ProjectDTO[] getProjectDTOs() {
        return projectsDTOS.values().toArray(new ProjectDTO[0]);
    }

    public Project getProject(int id) {
        return projects.get(id);
    }

    public ConfigurationDTO newConfiguration() {
        ConfigurationDTO dto = new ConfigurationDTO();
        dto.id = nextId++;
        dto.status = "stopped";

        configs.put(dto.id, dto);

        return dto;
    }

    public ConfigurationDTO getConfiguration(int configurationId) {
        return configs.get(configurationId);
    }

    public void removeConfiguration(int projectId, int configurationId) {
        getProject(projectId).removeConfiguration(configs.remove(configurationId));
    }

    public int getConfigurationProject(int configurationId) {
        for (Project p : projects.values()) {
            if (p.getConfiguration(configurationId) != null) {
                return p.dto.id;
            }
        }
        return -1;
    }

    public void updateConfiguration(ConfigurationDTO newConfig) {
        ConfigurationDTO config = configs.get(newConfig.id);
//        config.id = newConfig.id;
        config.name = newConfig.name;
        config.description = newConfig.description;
//        config.database = newConfig.database;
        config.port = newConfig.port;
//        config.status = newConfig.status;
    }

    public void createProject(ProjectDTO project) {
        addProject(new ProjectDTO(nextId++, project.name, project.description));
    }

    public void removeProject(int projectId) {
        projects.remove(projectId);
        projectsDTOS.remove(projectId);
    }
}
