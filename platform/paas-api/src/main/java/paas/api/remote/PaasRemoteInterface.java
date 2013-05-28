package paas.api.remote;

import paas.api.gwt.shared.dto.ConfigurationDTO;
import paas.api.gwt.shared.dto.ModuleDTO;
import paas.api.gwt.shared.dto.ProjectDTO;
import platform.interop.RemoteLogicsInterface;

import java.rmi.RemoteException;

public interface PaasRemoteInterface extends RemoteLogicsInterface {
    public ProjectDTO[] getProjects(String userLogin) throws RemoteException;

    public ProjectDTO[] addNewProject(String userLogin, ProjectDTO project) throws RemoteException;

    public ProjectDTO[] updateProject(String userLogin, ProjectDTO project) throws RemoteException;

    public ProjectDTO[] removeProject(String userLogin, int projectId) throws RemoteException;

    public ModuleDTO[] getProjectModules(String userLogin, int projectId) throws RemoteException;

    public ModuleDTO[] getAvailalbeModules(String userLogin, int projectId) throws RemoteException;

    public ConfigurationDTO[] getProjectConfigurations(String userLogin, int projectId) throws RemoteException;

    public String getModuleText(String userLogin, int moduleId) throws RemoteException;

    public void updateModules(String userLogin, int moduleIds[], String moduleTexts[]) throws RemoteException;

    public ModuleDTO[] addModules(String userLogin, int projectId, int moduleIds[]) throws RemoteException;

    public ModuleDTO[] addNewModule(String userLogin, int projectId, ModuleDTO newModule) throws RemoteException;

    public ModuleDTO[] removeModuleFromProject(String userLogin, int projectId, int moduleId) throws RemoteException;

    public ModuleDTO[] removeModule(String userLogin, int moduleId) throws RemoteException;

    public ConfigurationDTO[] addNewConfiguration(String userLogin, int projectId) throws RemoteException;

    public ConfigurationDTO[] removeConfiguration(String userLogin, int projectId, int configurationId) throws RemoteException;

    public ConfigurationDTO[] updateConfiguration(String userLogin, ConfigurationDTO configuration) throws RemoteException;

    public ConfigurationDTO[] startConfiguration(String userLogin, ConfigurationDTO configuration) throws RemoteException;

    public ConfigurationDTO[] restartConfiguration(String userLogin, ConfigurationDTO configuration) throws RemoteException;

    public ConfigurationDTO[] stopConfiguration(String userLogin, int configurationId) throws RemoteException;

    public ConfigurationDTO getConfiguration(String userLogin, int configurationId)  throws RemoteException;
}
