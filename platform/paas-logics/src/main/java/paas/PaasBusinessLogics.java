package paas;

import net.sf.jasperreports.engine.JRException;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import paas.api.gwt.shared.dto.ConfigurationDTO;
import paas.api.gwt.shared.dto.ModuleDTO;
import paas.api.gwt.shared.dto.ProjectDTO;
import paas.api.remote.PaasRemoteInterface;
import paas.manager.server.AppManager;
import platform.base.OrderedMap;
import platform.base.SoftHashMap;
import platform.interop.Compare;
import platform.server.Context;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.Query;
import platform.server.data.sql.DataAdapter;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.session.DataSession;

import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;

import static java.util.Arrays.asList;

public class PaasBusinessLogics extends BusinessLogics<PaasBusinessLogics> implements PaasRemoteInterface {
    public PaasLogicsModule paasLM;

    public AppManager appManager;

    public PaasBusinessLogics(DataAdapter iAdapter, int port) throws IOException, ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException, JRException {
        super(iAdapter, port);
    }

    @Override
    protected void createModules() throws IOException {
        super.createModules();

        paasLM = addModule(new PaasLogicsModule(LM, this));
        paasLM.setRequiredModules(Arrays.asList("System"));
    }

    @Override
    protected void initModules() throws ClassNotFoundException, IOException, SQLException, InstantiationException, IllegalAccessException, JRException {
        super.initModules();

        cleanDatabases();
    }

    public void setAppManager(AppManager appManager) {
        this.appManager = appManager;
        refreshConfigurationStatuses(null);
    }

    private void cleanDatabases() {
        logger.info("Очистка БД для удалённых конфигураций...");
        try {
            DataSession session = createSession();
            try {
                Map<String, KeyExpr> keys = KeyExpr.getMapKeys(asList("dbId"));
                Expr dbExpr = keys.get("dbId");

                Query<String, String> q = new Query<String, String>(keys);
                q.and(dbExpr.isClass(paasLM.database));
                q.and(
                        paasLM.databaseConfiguration.getExpr(session.getModifier(), dbExpr).getWhere().not()
                );

                q.properties.put("name", LM.name.getExpr(session.getModifier(), dbExpr));

                OrderedMap<Map<String, Object>, Map<String, Object>> values = q.execute(session.sql);
                for (Map.Entry<Map<String, Object>, Map<String, Object>> entry : values.entrySet()) {
                    String name = (String) entry.getValue().get("name");

                    if (name != null) {
                        removeDatabase(name);
                    }
                }

                session.apply(this);
            } finally {
                session.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Ошибка при считывании информации о статусе приложений", e);
        }
    }

    private void removeDatabase(String database) {
        //todo:
    }

    private int generateDatabase() throws SQLException {
        DataSession session = createSession();
        try {
            DataObject dbOjb = session.addObject(paasLM.database);

            //todo: возможно генерацию имени стоит переделать на что-нибудь более надёжное, типа зачитывания текущих значений и выбора MAX+1
            LM.name.change("paas_generated_" + System.currentTimeMillis(), session, dbOjb);

            session.apply(this);

            return (Integer) dbOjb.object;
        } finally {
            session.close();
        }
    }

    protected void initAuthentication() throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
        addUser("admin", "fusion");
    }

    @Override
    public ProjectDTO[] getProjects(String userLogin) throws RemoteException {
        assert userLogin != null;
        try {
            DataSession session = createSession();
            try {
                Map<String, KeyExpr> keys = KeyExpr.getMapKeys(asList("project"));
                Expr projExpr = keys.get("project");

                Query<String, String> q = new Query<String, String>(keys);
                q.and(paasLM.projectOwnerUserLogin.getExpr(projExpr).compare(new DataObject(userLogin), Compare.EQUALS));

                q.properties.put("name", LM.name.getExpr(projExpr));
                q.properties.put("description", paasLM.projectDescription.getExpr(projExpr));

                OrderedMap<Map<String, Object>, Map<String, Object>> values = q.execute(session.sql);
                ProjectDTO projects[] = new ProjectDTO[values.size()];
                int i = 0;
                for (Map.Entry<Map<String, Object>, Map<String, Object>> entry : values.entrySet()) {
                    Map<String, Object> propValues = entry.getValue();

                    ProjectDTO dto = new ProjectDTO();
                    dto.id = (Integer) entry.getKey().get("project");
                    dto.name = (String) propValues.get("name");
                    dto.description = (String) propValues.get("description");

                    projects[i++] = dto;
                }

                return projects;
            } finally {
                session.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RemoteException("Ошибка при считывании информации о проектах", e);
        }
    }

    @Override
    public ProjectDTO[] addNewProject(String userLogin, ProjectDTO newProject) throws RemoteException {
        try {
            DataSession session = createSession();
            try {
                int userId = getUserId(userLogin);

                DataObject projObj = session.addObject(paasLM.project);

                LM.name.change(newProject.name, session, projObj);
                paasLM.projectDescription.change(newProject.description, session, projObj);
                paasLM.projectOwner.change(userId, session, projObj);

                session.apply(this);
            } finally {
                session.close();
            }

            return getProjects(userLogin);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RemoteException("Ошибка при добавлении проекта", e);
        }
    }

    @Override
    public ProjectDTO[] updateProject(String userLogin, ProjectDTO project) throws RemoteException {
        try {
            DataSession session = createSession();

            try {
                checkProjectPermission(userLogin, project.id);

                DataObject projObj = new DataObject(project.id, paasLM.project);

                LM.name.change(project.name, session, projObj);
                paasLM.projectDescription.change(project.description, session, projObj);

                session.apply(this);

                return getProjects(userLogin);
            } finally {
                session.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RemoteException("Ошибка при сохранении проектов", e);
        }
    }

    @Override
    public ProjectDTO[] removeProject(String userLogin, int projectId) throws RemoteException {
        try {
            DataSession session = createSession();
            try {
                checkProjectPermission(userLogin, projectId);

                session.changeClass(new DataObject(projectId, paasLM.project), null);

                session.apply(this);
            } finally {
                session.close();
            }
            return getProjects(userLogin);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RemoteException("Ошибка при удалении проекта", e);
        }
    }

    @Override
    public ModuleDTO[] getProjectModules(String userLogin, int projectId) throws RemoteException {
        assert userLogin != null;
        try {
            DataSession session = createSession();
            try {
                checkProjectPermission(userLogin, projectId);

                Map<String, KeyExpr> keys = KeyExpr.getMapKeys(asList("module"));
                Expr moduleExpr = keys.get("module");
                Expr projExpr = new DataObject(projectId, paasLM.project).getExpr();

                Query<String, String> q = new Query<String, String>(keys);
                q.and(paasLM.moduleInProject.getExpr(projExpr, moduleExpr).getWhere());

                q.properties.put("order", paasLM.moduleOrder.getExpr(projExpr, moduleExpr));
                q.properties.put("name", LM.name.getExpr(moduleExpr));

                return getModuleDTOs(q.execute(session.sql, new OrderedMap(asList("order"), false)));
            } finally {
                session.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RemoteException("Ошибка при считывании информации о модулях", e);
        }
    }

    @Override
    public ModuleDTO[] getAvailalbeModules(String userLogin, int projectId) throws RemoteException {
        assert userLogin != null;
        try {
            DataSession session = createSession();
            try {
                checkProjectPermission(userLogin, projectId);

                Map<String, KeyExpr> keys = KeyExpr.getMapKeys(asList("module"));
                Expr moduleExpr = keys.get("module");
                Expr projExpr = new DataObject(projectId, paasLM.project).getExpr();

                Query<String, String> q = new Query<String, String>(keys);
                q.and(moduleExpr.isClass(paasLM.module));
                q.and(paasLM.moduleInProject.getExpr(projExpr, moduleExpr).getWhere().not());

                q.properties.put("name", LM.name.getExpr(moduleExpr));

                return getModuleDTOs(q.execute(session.sql));
            } finally {
                session.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RemoteException("Ошибка при считывании информации о модулях", e);
        }
    }

    private ModuleDTO[] getModuleDTOs(OrderedMap<Map<String, Object>, Map<String, Object>> values) {
        ModuleDTO modules[] = new ModuleDTO[values.size()];
        int i = 0;
        for (Map.Entry<Map<String, Object>, Map<String, Object>> entry : values.entrySet()) {
            Map<String, Object> propValues = entry.getValue();

            ModuleDTO dto = new ModuleDTO();
            dto.id = (Integer) entry.getKey().get("module");
            dto.name = (String) propValues.get("name");
            dto.description = "";

            modules[i++] = dto;
        }
        return modules;
    }

    @Override
    public ConfigurationDTO[] getProjectConfigurations(String userLogin, int projectId) throws RemoteException {
        try {
            DataSession session = createSession();
            try {
                checkProjectPermission(userLogin, projectId);

                Map<String, KeyExpr> keys = KeyExpr.getMapKeys(asList("conf"));
                Expr confExpr = keys.get("conf");

                Query<String, String> q = new Query<String, String>(keys);
                q.and(paasLM.configurationProject.getExpr(confExpr).compare(new DataObject(projectId, paasLM.project), Compare.EQUALS));

                q.properties.put("name", LM.name.getExpr(confExpr));
                q.properties.put("port", paasLM.configurationPort.getExpr(confExpr));
                q.properties.put("status", paasLM.configurationStatus.getExpr(confExpr));

                OrderedMap<Map<String, Object>, Map<String, Object>> values = q.execute(session.sql);
                ConfigurationDTO configurations[] = new ConfigurationDTO[values.size()];
                int i = 0;
                for (Map.Entry<Map<String, Object>, Map<String, Object>> entry : values.entrySet()) {
                    Map<String, Object> propValues = entry.getValue();

                    ConfigurationDTO dto = new ConfigurationDTO();
                    dto.id = (Integer) entry.getKey().get("conf");
                    dto.name = (String) propValues.get("name");
                    dto.port = (Integer) propValues.get("port");
                    Integer statusId = (Integer) propValues.get("status");
                    dto.status = statusId == null ? null : paasLM.status.getSID(statusId);
                    dto.description = "";

                    configurations[i++] = dto;
                }

                return configurations;
            } finally {
                session.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RemoteException("Ошибка при считывании информации о конфигурациях", e);
        }
    }

    private void checkProjectPermission(String userLogin, int projectId) throws SQLException {
        assert userLogin != null;

        int loginId = getUserId(userLogin);

        DataObject projectObj = new DataObject(projectId, paasLM.project);
        Integer ownerId = (Integer) paasLM.projectOwner.read(createSession(), projectObj);
        if (ownerId == null || ownerId != loginId) {
            throw new RuntimeException("Нет доступа к запрашиваемому проекту");
        }
    }

    private int getUserId(String userLogin) throws SQLException {
        if (userLogin == null) {
            throw new RuntimeException("Пользователь не найден");
        }

        Integer userId = (Integer) LM.loginToUser.read(createSession(), new DataObject(userLogin));
        if (userId == null) {
            throw new RuntimeException("Пользователь " + userLogin + " не найден");
        }

        return userId;
    }

    @Override
    public String getModuleText(String userLogin, int moduleId) throws RemoteException {
        assert userLogin != null;
        try {
            DataSession session = createSession();
            try {
                return (String) paasLM.moduleSource.read(session, new DataObject(moduleId, paasLM.module));
            } finally {
                session.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RemoteException("Ошибка при считывании информации о модуле", e);
        }
    }

    @Override
    public void updateModules(String userLogin, int[] moduleIds, String[] moduleTexts) throws RemoteException {
        assert moduleIds.length == moduleTexts.length;
        try {
            DataSession session = createSession();
            try {
                for (int i = 0; i < moduleIds.length; i++) {
                    paasLM.moduleSource.change(moduleTexts[i], session, new DataObject(moduleIds[i], paasLM.module));
                }

                session.apply(this);

            } finally {
                session.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RemoteException("Ошибка при сохранении модулей", e);
        }
    }

    @Override
    public ModuleDTO[] addModules(String userLogin, int projectId, int[] moduleIds) throws RemoteException {
        try {
            DataSession session = createSession();
            try {
                checkProjectPermission(userLogin, projectId);
                DataObject projectObj = new DataObject(projectId, paasLM.project);
                for (int moduleId : moduleIds) {
                    addModuleToProject(session, projectObj, new DataObject(moduleId, paasLM.module));
                }
                session.apply(this);
            } finally {
                session.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RemoteException("Ошибка при добавлении модулей", e);
        }

        return getProjectModules(userLogin, projectId);
    }

    @Override
    public ModuleDTO[] addNewModule(String userLogin, int projectId, ModuleDTO newModule) throws RemoteException {
        try {
            DataSession session = createSession();
            try {
                checkProjectPermission(userLogin, projectId);

                DataObject moduleObj = session.addObject(paasLM.module);

                LM.name.change(newModule.name, session, moduleObj);

                addModuleToProject(session, new DataObject(projectId, paasLM.project), moduleObj);

                session.apply(this);
            } finally {
                session.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RemoteException("Ошибка при добавлении модулей", e);
        }

        return getProjectModules(userLogin, projectId);
    }

    @Override
    public ModuleDTO[] removeModuleFromProject(String userLogin, int projectId, int moduleId) throws RemoteException {
        try {
            DataSession session = createSession();
            try {
                checkProjectPermission(userLogin, projectId);

                paasLM.moduleInProject.change(null, session, new DataObject(projectId, paasLM.project), new DataObject(moduleId, paasLM.module));

                session.apply(this);
            } finally {
                session.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RemoteException("Ошибка при удалении модуля", e);
        }

        return getProjectModules(userLogin, projectId);
    }

    @Override
    public ModuleDTO[] removeModule(String userLogin, int moduleId) throws RemoteException {
        //todo:
        return new ModuleDTO[0];
    }

    @Override
    public ConfigurationDTO[] addNewConfiguration(String userLogin, int projectId) throws RemoteException {
        try {
            DataSession session = createSession();
            try {
                checkProjectPermission(userLogin, projectId);

                DataObject configObj = session.addObject(paasLM.configuration);

                int databaseId = generateDatabase();

                LM.name.change("Configuration " + configObj.object, session, configObj);
                paasLM.configurationStatus.change(paasLM.status.getID("stopped"), session, configObj);
                paasLM.configurationProject.change(projectId, session, configObj);
                paasLM.configurationDatabase.change(databaseId, session, configObj);

                session.apply(this);
            } finally {
                session.close();
            }
            return getProjectConfigurations(userLogin, projectId);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RemoteException("Ошибка при добавлении конфигурации", e);
        }
    }

    @Override
    public ConfigurationDTO[] removeConfiguration(String userLogin, int projectId, int configurationId) throws RemoteException {
        try {
            DataSession session = createSession();
            try {
                checkProjectPermission(userLogin, projectId);

                session.changeClass(new DataObject(configurationId, paasLM.configuration), null);

                session.apply(this);
            } finally {
                session.close();
            }
            return getProjectConfigurations(userLogin, projectId);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RemoteException("Ошибка при удалении конфигурации", e);
        }
    }

    @Override
    public ConfigurationDTO[] updateConfiguration(String userLogin, ConfigurationDTO configuration) throws RemoteException {
        String currentStatus = getConfigurationStatus(configuration.id);
        if ("started".equals(currentStatus)) {
            //запрещаем изменение запущенных конфигураций
            throw new RuntimeException("Изменение запущенных конфигураций запрещено");
        }

        try {
            DataSession session = createSession();
            try {
                DataObject confObj = new DataObject(configuration.id, paasLM.configuration);

                Integer projId = (Integer) paasLM.configurationProject.read(session, confObj);
                checkProjectPermission(userLogin, projId);

                updateConfiguration(session, confObj, configuration);

                session.apply(this);

                return getProjectConfigurations(userLogin, projId);
            } finally {
                session.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RemoteException("Ошибка при сохранении конфигурации", e);
        }
    }

    @Override
    public ConfigurationDTO[] startConfiguration(String userLogin, ConfigurationDTO configuration) throws RemoteException {
        DataObject confObj = new DataObject(configuration.id, paasLM.configuration);
        try {
            DataSession session = createSession();

            try {

                Integer projId = (Integer) paasLM.configurationProject.read(session, confObj);
                checkProjectPermission(userLogin, projId);

                //сначала записываем новые значения
                updateConfiguration(session, confObj, configuration);

                appManager.executeScriptedBL(session, confObj);

                String errorMsg = waitForStarted(configuration.id);
                if (errorMsg != null) {
                    throw new RuntimeException("Error starting configuration: " + errorMsg);
                }

                session.apply(this);

                return getProjectConfigurations(userLogin, projId);
            } finally {
                session.close();
            }
        } catch (Exception e) {
            throw new RemoteException("Ошибка при старте конфигурации", e);
        }
    }

    @Override
    public ConfigurationDTO[] restartConfiguration(String userLogin, ConfigurationDTO configuration) throws RemoteException {
        stopConfiguration(userLogin, configuration.id);
        return startConfiguration(userLogin, configuration);
    }

    private void updateConfiguration(DataSession session, DataObject confObj, ConfigurationDTO configuration) throws SQLException {
        PaasUtils.checkPortExceptionally(configuration.port);

        paasLM.configurationPort.change(configuration.port, session, confObj);
        LM.name.change(configuration.name, session, confObj);
    }

    private String waitForStarted(int configurationId) throws RemoteException {
        return waitForStatus(configurationId, "started");
    }

    private String waitForStopped(int configurationId) throws RemoteException {
        return waitForStatus(configurationId, "stopped");
    }

    private final SoftHashMap<Integer, String> configurationLaunchErrors = new SoftHashMap<Integer, String>();

    public void pushConfigurationLaunchError(int configurationId, String error) {
        synchronized (configurationLaunchErrors) {
            configurationLaunchErrors.put(configurationId, error);
        }
    }

    public String popConfigurationLaunchError(int configurationId) {
        synchronized (configurationLaunchErrors) {
            return configurationLaunchErrors.remove(configurationId);
        }
    }

    private String waitForStatus(int configurationId, String status) throws RemoteException {
        //ждём 3 минуты
        int maxAttempts = 3 * 60;
        int attempts = 0;
        while (true) {
            String error = popConfigurationLaunchError(configurationId);
            if (error != null) {
                return error;
            }

            if (status.equals(getConfigurationStatus(configurationId))) {
                return null;
            }
            attempts++;

            if (attempts > maxAttempts) {
                return "Timeout";
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                logger.debug("Thread interrupted: ", e);
            }
        }
    }

    private String getConfigurationStatus(int configurationId) {
        try {
            DataSession session = createSession();

            try {
                DataObject confObj = new DataObject(configurationId, paasLM.configuration);

                Integer statusId = (Integer) paasLM.configurationStatus.read(session, confObj);
                if (statusId == null) {
                    return null;
                }

                return paasLM.status.getSID(statusId);
            } finally {
                session.close();
            }
        } catch (Exception e) {
            logger.warn("Ошибка при чтении статуса: ", e);
        }
        return null;
    }

    @Override
    public ConfigurationDTO[] stopConfiguration(String userLogin, int configurationId) throws RemoteException {
        try {
            DataSession session = createSession();

            try {
                DataObject confObj = new DataObject(configurationId, paasLM.configuration);

                Integer projId = (Integer) paasLM.configurationProject.read(session, confObj);
                checkProjectPermission(userLogin, projId);

                paasLM.configurationStop.execute(session, confObj);

                String errorMsg = waitForStopped(configurationId);
                if (errorMsg != null) {
                    throw new RuntimeException("Error stopping configuration: " + errorMsg);
                }

                session.apply(this);

                return getProjectConfigurations(userLogin, projId);
            } finally {
                session.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RemoteException("Ошибка при остановке конфигурации", e);
        }
    }

    @Override
    public ConfigurationDTO getConfiguration(String userLogin, int configurationId) throws RemoteException {
        assert userLogin != null;
        try {
            DataSession session = createSession();
            try {
                DataObject confObj = new DataObject(configurationId, paasLM.configuration);

                Integer projId = (Integer) paasLM.configurationProject.read(session, confObj);
                checkProjectPermission(userLogin, projId);

                Integer port = (Integer) paasLM.configurationPort.read(session, confObj);
                if (port == null) {
                    throw new IllegalStateException("Порт конфигурации не задан");
                }

                String name = (String) LM.name.read(session, confObj);

                ConfigurationDTO configuration = new ConfigurationDTO();
                configuration.port = port;
                configuration.name = name;
                return configuration;
            } finally {
                session.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RemoteException("Ошибка при считывании информации о конфигурации", e);
        }
    }

    private void addModuleToProject(DataSession session, DataObject projectId, DataObject moduleId) throws SQLException {
        paasLM.moduleInProject.change(true, session, projectId, moduleId);
    }

    public void refreshConfigurationStatuses(DataObject projId) {
        logger.info("Обновление статусов конфигураций...");
        try {
            DataSession session = createSession();
            try {
                Map<String, KeyExpr> keys = KeyExpr.getMapKeys(asList("configId"));
                Expr confExpr = keys.get("configId");

                Query<String, String> q = new Query<String, String>(keys);
                q.and(confExpr.isClass(paasLM.configuration));
                if (projId != null) {
                    q.and(
                            paasLM.configurationProject.getExpr(session.getModifier(), confExpr).compare(projId, Compare.EQUALS)
                    );
                }

                q.properties.put("port", paasLM.configurationPort.getExpr(session.getModifier(), confExpr));

                OrderedMap<Map<String, Object>, Map<String, Object>> values = q.execute(session.sql);
                for (Map.Entry<Map<String, Object>, Map<String, Object>> entry : values.entrySet()) {
                    Integer configId = (Integer) entry.getKey().get("configId");
                    Integer port = (Integer) entry.getValue().get("port");

                    if (port != null) {
                        changeConfigurationStatus(session, configId, appManager.getStatus(port));
                    }
                }

                session.apply(this);
            } finally {
                session.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Ошибка при считывании информации о статусе приложений", e);
        }
    }

    public void changeConfigurationStatus(DataObject confId, String statusStr) {
        changeConfigurationStatus(null, confId, statusStr);
    }

    public void changeConfigurationStatus(Integer confId, String statusStr) {
        changeConfigurationStatus(null, confId, statusStr);
    }

    public void changeConfigurationStatus(DataSession session, Integer confId, String statusStr) {
        changeConfigurationStatus(session, new DataObject(confId, paasLM.configuration), statusStr);
    }

    public void changeConfigurationStatus(DataSession session, DataObject confId, String statusStr) {
        try {
            boolean apply = false;
            if (session == null) {
                apply = true;
                session = createSession();
            }

            paasLM.configurationStatus.change(paasLM.status.getID(statusStr), session, confId);

            if (apply) {
                session.apply(this);
            }
        } catch (SQLException e) {
            logger.error("Ошибка при изменении статуса конфигурации: ", e);
        }
    }

    @Override
    public BusinessLogics getBL() {
        return this;
    }

    @Aspect
    private static class RemoteLogicsContextHoldingAspect {
        @Before("execution(* paas.api.remote.PaasRemoteInterface.*(..)) && target(remoteLogics)")
        public void beforeCall(BusinessLogics remoteLogics) {
            Context.context.set(remoteLogics);
        }
    }
}
