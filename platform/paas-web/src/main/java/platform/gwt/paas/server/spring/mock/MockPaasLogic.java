package platform.gwt.paas.server.spring.mock;

import paas.api.gwt.shared.dto.ConfigurationDTO;
import paas.api.gwt.shared.dto.ModuleDTO;
import paas.api.gwt.shared.dto.ProjectDTO;
import paas.api.remote.PaasRemoteInterface;
import platform.interop.form.screen.ExternalScreen;
import platform.interop.form.screen.ExternalScreenParameters;
import platform.interop.navigator.RemoteNavigatorInterface;
import platform.interop.remote.MethodInvocation;
import platform.interop.remote.UserInfo;

import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.TimeZone;

@SuppressWarnings({"ToArrayCallWithZeroLengthArrayArgument"})
public class MockPaasLogic implements PaasRemoteInterface {

    @Override
    public String getName() throws RemoteException {
        return "MockPaasLogic";
    }

    @Override
    public String getDisplayName() throws RemoteException {
        return null;
    }

    @Override
    public byte[] getMainIcon() throws RemoteException {
        return new byte[0];
    }

    @Override
    public byte[] getLogo() throws RemoteException {
        return new byte[0];
    }

    @Override
    public RemoteNavigatorInterface createNavigator(boolean isFullClient, String login, String password, int computer, boolean forceCreateNew) throws RemoteException {
        return null;
    }

    @Override
    public Integer getComputer(String hostname) throws RemoteException {
        return null;
    }

    @Override
    public ExternalScreen getExternalScreen(int screenID) throws RemoteException {
        return null;
    }

    @Override
    public ExternalScreenParameters getExternalScreenParameters(int screenID, int computerId) throws RemoteException {
        return null;
    }

    @Override
    public void endSession(String clientInfo) throws RemoteException {
    }

    @Override
    public boolean checkUser(String login, String password) throws RemoteException {
        return false;
    }

    @Override
    public void ping() throws RemoteException {
    }

    @Override
    public TimeZone getTimeZone() throws RemoteException {
        return null;
    }

    @Override
    public UserInfo getUserInfo(String username) throws RemoteException {
        return new UserInfo("admin", "fusion", Arrays.asList("admin"));
    }

    @Override
    public void remindPassword(String email, String localeLanguage) throws RemoteException {
    }

    @Override
    public byte[] readFile(String sid, String... params) throws RemoteException {
        return new byte[0];
    }

    @Override
    public boolean checkDefaultViewPermission(String propertySid) throws RemoteException {
        return false;
    }

    @Override
    public boolean checkPropertyViewPermission(String userName, String propertySID) throws RemoteException {
        return false;
    }

    @Override
    public String addUser(String username, String email, String password, String firstName, String lastName, String localeLanguage) throws RemoteException {
        return null;
    }

    @Override
    public Object execute(MethodInvocation[] invocations) throws RemoteException {
        return null;
    }

    public int generateID() throws RemoteException {
        return 0;
    }

    @Override
    public Object[] createAndExecute(MethodInvocation creator, MethodInvocation[] invocations) throws RemoteException {
        return new Object[0];
    }

    @Override
    public byte[] getPropertyObjectsByteArray(byte[] classes, boolean isCompulsory, boolean isAny) throws RemoteException {
        return new byte[0];
    }

    @Override
    public byte[] getBaseClassByteArray() throws RemoteException {
        return new byte[0];
    }

    @Override
    public int generateNewID() throws RemoteException {
        return 0;
    }

    @Override
    public String getRemoteActionMessage() throws RemoteException {
        return null;
    }

    private int lastId = 0;
    private MockModulesDAO daoMod = new MockModulesDAO();
    private MockProjectDAO daoProj = new MockProjectDAO(daoMod);

    public MockPaasLogic() {
        daoProj.addProject(new ProjectDTO(1, "Project 1", "asdfa\nsdf\nas\ndf\nas\nf\naf\nsdasdf\naf\nsdas\nfafsdddddddddddasdf\nas\nsdfa"));
        daoProj.addProject(new ProjectDTO(2, "Project 2", "Some sample mock project 2..."));
        daoProj.addProject(new ProjectDTO(3, "Project 3", "Some sample mock project 3..."));

        daoProj.addConfiguration(1, new ConfigurationDTO(1, "Test", "asdfasdf", 1111, "stopped"));
        daoProj.addConfiguration(1, new ConfigurationDTO(2, "Production", "asdfasdf", 2222, "started"));

        daoProj.addConfiguration(2, new ConfigurationDTO(3, "Production 1", "asdfasdf", 2222, "started"));
        daoProj.addConfiguration(2, new ConfigurationDTO(4, "Production 2", "asdfasdf", 11232, "stopped"));
        daoProj.addConfiguration(2, new ConfigurationDTO(5, "Test", "asdfasdf", 22123, "started"));

        daoProj.addConfiguration(3, new ConfigurationDTO(6, "Prod 1", "asdfasdf", 22222, "started"));
        daoProj.addConfiguration(3, new ConfigurationDTO(7, "Prod 2", "asdfasdf", 9999, "stopped"));
        daoProj.addConfiguration(3, new ConfigurationDTO(8, "Test", "asdfasdf", 22123, "started"));

        daoMod.addModule(new ModuleDTO(1, "TestModule 1", ""));
        daoMod.setModuleText(1, "IMPORT BaseLogicsModule;\n" +
                                "\n" +
                                "CLASS employee 'Сотрудник' : named;\n" +
                                "CLASS document 'Документ' : named;\n" +
                                "\n" +
                                "documentEmployee = DATA employee (document);\n" +
                                "documentEmployeeName(document) = name(documentEmployee(document)) IN base;\n" +
                                "\n" +
                                "documentCount = DATA INTEGER (document) IN base;\n" +
                                "\n" +
                                "FORM documents 'Документы сотрудников'\n" +
                                "OBJECTS employee FIXED PANEL, document\n" +
                                "PROPERTIES name(employee), name(document), documentCount(document), documentEmployeeName(document);");
        daoMod.addModule(new ModuleDTO(2, "TestModule 2", ""));
        daoMod.setModuleText(2, "aasdfasdfsdfasdfasdfasd asdf asdf asdf asdf\n asdfasdf asdfasd f\n asdfasd asdf asdf\n asdfasdfasdfasdf");
        daoMod.addModule(new ModuleDTO(3, "SampleModule 1", ""));
        daoMod.setModuleText(3, "aasdfasdfsdfasdfasdfasd asdf asdf asdf asdf\n asdfasdf asdfasd f\n asdfasd asdf asdf\n asdfasdfasdfasdf");
        daoMod.addModule(new ModuleDTO(4, "SampleModule 2", ""));
        daoMod.setModuleText(4, "aasdfasdfsdfasdfasdfasd asdf asdf asdf asdf\n asdfasdf asdfasd f\n asdfasd asdf asdf\n asdfasdfasdfasdf");
        daoMod.addModule(new ModuleDTO(5, "MockModule 1", ""));
        daoMod.setModuleText(5, "aasdfasdfsdfasdfasdfasd asdf asdf asdf asdf\n asdfasdf asdfasd f\n asdfasd asdf asdf\n asdfasdfasdfasdf");
        daoMod.addModule(new ModuleDTO(6, "MockModule 2", ""));
        daoMod.setModuleText(6, "aasdfasdfsdfasdfasdfasd asdf asdf asdf asdf\n asdfasdf asdfasd f\n asdfasd asdf asdf\n asdfasdfasdfasdf");

        daoProj.addModulesToProject(1, 1);
        daoProj.addModulesToProject(2, 3, 4);
        daoProj.addModulesToProject(3, 5, 6);
    }


    @Override
    public ProjectDTO[] getProjects(String userLogin) throws RemoteException {
        return daoProj.getProjectDTOs();
    }

    @Override
    public ProjectDTO[] addNewProject(String userLogin, ProjectDTO dto) throws RemoteException {
        daoProj.createProject(dto);
        return getProjects(userLogin);
    }

    @Override
    public ProjectDTO[] updateProject(String userLogin, ProjectDTO dto) throws RemoteException {
        ProjectDTO project = daoProj.getProject(dto.id).dto;
        project.description = dto.description;
        project.name = dto.name;
        return getProjects(userLogin);
    }

    @Override
    public ProjectDTO[] removeProject(String userLogin, int projectId) throws RemoteException {
        daoProj.removeProject(projectId);
        return getProjects(userLogin);
    }

    @Override
    public ModuleDTO[] getProjectModules(String userLogin, int projectId) throws RemoteException {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return daoProj.getProject(projectId).getModules().toArray(new ModuleDTO[0]);
    }

    @Override
    public ModuleDTO[] getAvailalbeModules(String userLogin, int projectId) throws RemoteException {
        Set<ModuleDTO> pMods = daoProj.getProject(projectId).getModules();
        Collection<ModuleDTO> allMods = daoMod.getModulesDTOs();
        allMods.removeAll(pMods);
        return allMods.toArray(new ModuleDTO[0]);
    }

    @Override
    public ConfigurationDTO[] getProjectConfigurations(String userLogin, int projectId) throws RemoteException {
        return daoProj.getProject(projectId).getConfigurations().toArray(new ConfigurationDTO[0]);
    }

    @Override
    public String getModuleText(String userLogin, int moduleId) throws RemoteException {
        return daoMod.getModuleText(moduleId);
    }

    @Override
    public void updateModules(String userLogin, int[] moduleIds, String[] moduleTexts) throws RemoteException {
        for (int i = 0; i < moduleIds.length; ++i) {
            daoMod.setModuleText(moduleIds[i], moduleTexts[i]);
        }
    }

    @Override
    public ModuleDTO[] addModules(String userLogin, int projectId, int[] moduleIds) throws RemoteException {
        daoProj.addModulesToProject(projectId, moduleIds);
        return getProjectModules(userLogin, projectId);
    }

    @Override
    public ModuleDTO[] addNewModule(String userLogin, int projectId, ModuleDTO dto) throws RemoteException {
        ModuleDTO newModele = daoMod.newModule();
        newModele.name = dto.name;
        newModele.description = dto.description;

        return addModules(userLogin, projectId, new int[]{newModele.id});
    }

    @Override
    public ModuleDTO[] removeModuleFromProject(String userLogin, int projectId, int moduleId) throws RemoteException {
        daoProj.getProject(projectId).getModules().remove(daoMod.getModule(moduleId));
        return getProjectModules(userLogin, projectId);
    }

    @Override
    public ModuleDTO[] removeModule(String userLogin, int moduleId) throws RemoteException {
        //todo:
        return new ModuleDTO[0];
    }

    @Override
    public ConfigurationDTO[] addNewConfiguration(String userLogin, int projectId) throws RemoteException {
        daoProj.addConfiguration(projectId, daoProj.newConfiguration());
        return getProjectConfigurations(userLogin, projectId);
    }

    @Override
    public ConfigurationDTO[] removeConfiguration(String userLogin, int projectId, int configurationId) throws RemoteException {
        daoProj.removeConfiguration(projectId, configurationId);
        return getProjectConfigurations(userLogin, projectId);
    }

    @Override
    public ConfigurationDTO[] updateConfiguration(String userLogin, ConfigurationDTO configuration) throws RemoteException {
        daoProj.updateConfiguration(configuration);
        return getProjectConfigurations(userLogin, daoProj.getConfigurationProject(configuration.id));
    }

    @Override
    public ConfigurationDTO[] startConfiguration(String userLogin, ConfigurationDTO configuration) throws RemoteException {
        ConfigurationDTO conf = daoProj.getConfiguration(configuration.id);
        conf.status = "started";

        int projectId = daoProj.getConfigurationProject(configuration.id);

        return getProjectConfigurations(userLogin, projectId);
    }

    @Override
    public ConfigurationDTO[] restartConfiguration(String userLogin, ConfigurationDTO configuration) throws RemoteException {
        stopConfiguration(userLogin, configuration.id);
        return startConfiguration(userLogin, configuration);
    }

    @Override
    public ConfigurationDTO[] stopConfiguration(String userLogin, int configurationId) throws RemoteException {
        ConfigurationDTO conf = daoProj.getConfiguration(configurationId);
        conf.status = "stopped";

        int projectId = daoProj.getConfigurationProject(configurationId);

        return getProjectConfigurations(userLogin, projectId);
    }

    @Override
    public ConfigurationDTO getConfiguration(String userLogin, int configurationId) throws RemoteException {
        return daoProj.getConfiguration(configurationId);
    }
}
