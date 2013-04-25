package paas;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import paas.api.gwt.shared.dto.ConfigurationDTO;
import paas.api.gwt.shared.dto.ModuleDTO;
import paas.api.gwt.shared.dto.ProjectDTO;
import paas.api.remote.PaasRemoteInterface;
import paas.manager.server.AppManager;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderMap;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.interop.Compare;
import platform.interop.exceptions.RemoteMessageException;
import platform.server.context.ThreadLocalContext;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.QueryBuilder;
import platform.server.lifecycle.LifecycleEvent;
import platform.server.logics.DataObject;
import platform.server.remote.RemoteLogics;
import platform.server.session.DataSession;

import java.rmi.RemoteException;
import java.sql.SQLException;

import static platform.base.BaseUtils.nullTrim;

public class PaasRemoteLogics extends RemoteLogics<PaasBusinessLogics> implements PaasRemoteInterface {
    public PaasLogicsModule paasLM;

    @Override
    public void lifecycleEvent(LifecycleEvent event) {
        super.lifecycleEvent(event);
        if (LifecycleEvent.INIT.equals(event.getType())) {
            this.paasLM = businessLogics.paasLM;
        }
    }

    @Override
    public ProjectDTO[] getProjects(String userLogin) throws RemoteException {
        assert userLogin != null;
        try {
            DataSession session = createSession();
            try {
                ImRevMap<String, KeyExpr> keys = KeyExpr.getMapKeys(SetFact.singleton("project"));
                Expr projExpr = keys.get("project");

                QueryBuilder<String, String> q = new QueryBuilder<String, String>(keys);
                q.and(paasLM.projectOwnerLogin.getExpr(projExpr).compare(new DataObject(userLogin), Compare.EQUALS));

                q.addProperty("name", baseLM.name.getExpr(projExpr));
                q.addProperty("description", paasLM.projectDescription.getExpr(projExpr));

                ImOrderMap<ImMap<String, Object>, ImMap<String, Object>> values = q.execute(session.sql);
                ProjectDTO projects[] = new ProjectDTO[values.size()];
                for (int i = 0, size = values.size(); i < size;i++) {
                    ImMap<String, Object> propValues = values.getValue(i);

                    ProjectDTO dto = new ProjectDTO();
                    dto.id = (Integer) values.getKey(i).get("project");
                    dto.name = (String) propValues.get("name");
                    dto.description = (String) propValues.get("description");

                    projects[i] = dto;
                }

                return projects;
            } finally {
                session.close();
            }
        } catch (Exception e) {
            logger.error("", e);
            throw new RemoteMessageException("Ошибка при считывании информации о проектах", e);
        }
    }

    @Override
    public ProjectDTO[] addNewProject(String userLogin, ProjectDTO newProject) throws RemoteException {
        try {
            DataSession session = createSession();
            try {
                int userId = getUserId(userLogin);

                DataObject projObj = session.addObject(paasLM.project);

                baseLM.name.change(newProject.name, session, projObj);
                paasLM.projectDescription.change(newProject.description, session, projObj);
                paasLM.projectOwner.change(userId, session, projObj);

                session.apply(businessLogics);
            } finally {
                session.close();
            }

            return getProjects(userLogin);
        } catch (Exception e) {
            logger.error("", e);
            throw new RemoteMessageException("Ошибка при добавлении проекта", e);
        }
    }

    @Override
    public ProjectDTO[] updateProject(String userLogin, ProjectDTO project) throws RemoteException {
        try {
            DataSession session = createSession();

            try {
                checkProjectPermission(userLogin, project.id);

                DataObject projObj = new DataObject(project.id, paasLM.project);

                baseLM.name.change(project.name, session, projObj);
                paasLM.projectDescription.change(project.description, session, projObj);

                session.apply(businessLogics);

                return getProjects(userLogin);
            } finally {
                session.close();
            }
        } catch (Exception e) {
            logger.error("", e);
            throw new RemoteMessageException("Ошибка при сохранении проектов", e);
        }
    }

    @Override
    public ProjectDTO[] removeProject(String userLogin, int projectId) throws RemoteException {
        try {
            DataSession session = createSession();
            try {
                checkProjectPermission(userLogin, projectId);

                session.changeClass(new DataObject(projectId, paasLM.project), null);

                session.apply(businessLogics);
            } finally {
                session.close();
            }
            return getProjects(userLogin);
        } catch (Exception e) {
            logger.error("", e);
            throw new RemoteMessageException("Ошибка при удалении проекта", e);
        }
    }

    @Override
    public ModuleDTO[] getProjectModules(String userLogin, int projectId) throws RemoteException {
        assert userLogin != null;
        try {
            DataSession session = createSession();
            try {
                checkProjectPermission(userLogin, projectId);

                ImRevMap<String, KeyExpr> keys = KeyExpr.getMapKeys(SetFact.singleton("module"));
                Expr moduleExpr = keys.get("module");
                Expr projExpr = new DataObject(projectId, paasLM.project).getExpr();

                QueryBuilder<String, String> q = new QueryBuilder<String, String>(keys);
                q.and(paasLM.moduleInProject.getExpr(projExpr, moduleExpr).getWhere());

                q.addProperty("name", baseLM.name.getExpr(moduleExpr));

                return getModuleDTOs(q.execute(session.sql));
            } finally {
                session.close();
            }
        } catch (Exception e) {
            logger.error("", e);
            throw new RemoteMessageException("Ошибка при считывании информации о модулях", e);
        }
    }

    @Override
    public ModuleDTO[] getAvailalbeModules(String userLogin, int projectId) throws RemoteException {
        assert userLogin != null;
        try {
            DataSession session = createSession();
            try {
                checkProjectPermission(userLogin, projectId);

                ImRevMap<String, KeyExpr> keys = KeyExpr.getMapKeys(SetFact.singleton("module"));
                Expr moduleExpr = keys.get("module");
                Expr projExpr = new DataObject(projectId, paasLM.project).getExpr();

                QueryBuilder<String, String> q = new QueryBuilder<String, String>(keys);
                q.and(moduleExpr.isClass(paasLM.module));
                q.and(paasLM.moduleInProject.getExpr(projExpr, moduleExpr).getWhere().not());

                q.addProperty("name", baseLM.name.getExpr(moduleExpr));

                return getModuleDTOs(q.execute(session.sql));
            } finally {
                session.close();
            }
        } catch (Exception e) {
            logger.error("", e);
            throw new RemoteMessageException("Ошибка при считывании информации о модулях", e);
        }
    }

    private ModuleDTO[] getModuleDTOs(ImOrderMap<ImMap<String, Object>, ImMap<String, Object>> values) {
        ModuleDTO modules[] = new ModuleDTO[values.size()];
        for (int i=0,size=values.size();i<size;i++) {
            ImMap<String, Object> propValues = values.getValue(i);

            ModuleDTO dto = new ModuleDTO();
            dto.id = (Integer) values.getKey(i).get("module");
            dto.name = (String) propValues.get("name");
            dto.description = "";

            modules[i] = dto;
        }
        return modules;
    }

    @Override
    public ConfigurationDTO[] getProjectConfigurations(String userLogin, int projectId) throws RemoteException {
        try {
            DataSession session = createSession();
            try {
                checkProjectPermission(userLogin, projectId);

                ImRevMap<String, KeyExpr> keys = KeyExpr.getMapKeys(SetFact.singleton("conf"));
                Expr confExpr = keys.get("conf");

                QueryBuilder<String, String> q = new QueryBuilder<String, String>(keys);
                q.and(paasLM.configurationProject.getExpr(confExpr).compare(new DataObject(projectId, paasLM.project), Compare.EQUALS));

                q.addProperty("name", baseLM.name.getExpr(confExpr));
                q.addProperty("port", paasLM.configurationPort.getExpr(confExpr));
                q.addProperty("exportName", paasLM.configurationExportName.getExpr(confExpr));
                q.addProperty("status", paasLM.configurationStatus.getExpr(confExpr));

                ImOrderMap<ImMap<String, Object>, ImMap<String, Object>> values = q.execute(session.sql);
                ConfigurationDTO configurations[] = new ConfigurationDTO[values.size()];
                for (int i = 0, size=values.size(); i<size;i++) {
                    ImMap<String, Object> propValues = values.getValue(i);

                    ConfigurationDTO dto = new ConfigurationDTO();
                    dto.id = (Integer) values.getKey(i).get("conf");
                    dto.name = (String) propValues.get("name");
                    dto.port = (Integer) propValues.get("port");
                    dto.exportName = nullTrim((String) propValues.get("exportName"));
                    Integer statusId = (Integer) propValues.get("status");
                    dto.status = statusId == null ? null : paasLM.status.getObjectName(statusId);
                    dto.description = "";

                    configurations[i] = dto;
                }

                return configurations;
            } finally {
                session.close();
            }
        } catch (Exception e) {
            logger.error("", e);
            throw new RemoteMessageException("Ошибка при считывании информации о конфигурациях", e);
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

        Integer userId = (Integer) businessLogics.authenticationLM.customUserLogin.read(createSession(), new DataObject(userLogin));
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
            logger.error("", e);
            throw new RemoteMessageException("Ошибка при считывании информации о модуле", e);
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

                session.apply(businessLogics);

            } finally {
                session.close();
            }
        } catch (Exception e) {
            logger.error("", e);
            throw new RemoteMessageException("Ошибка при сохранении модулей", e);
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
                session.apply(businessLogics);
            } finally {
                session.close();
            }
        } catch (Exception e) {
            logger.error("", e);
            throw new RemoteMessageException("Ошибка при добавлении модулей", e);
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

                baseLM.name.change(newModule.name, session, moduleObj);

                addModuleToProject(session, new DataObject(projectId, paasLM.project), moduleObj);

                session.apply(businessLogics);
            } finally {
                session.close();
            }
        } catch (Exception e) {
            logger.error("", e);
            throw new RemoteMessageException("Ошибка при добавлении модулей", e);
        }

        return getProjectModules(userLogin, projectId);
    }

    @Override
    public ModuleDTO[] removeModuleFromProject(String userLogin, int projectId, int moduleId) throws RemoteException {
        try {
            DataSession session = createSession();
            try {
                checkProjectPermission(userLogin, projectId);

                paasLM.moduleInProject.change((Object)null, session, new DataObject(projectId, paasLM.project), new DataObject(moduleId, paasLM.module));

                session.apply(businessLogics);
            } finally {
                session.close();
            }
        } catch (Exception e) {
            logger.error("", e);
            throw new RemoteMessageException("Ошибка при удалении модуля", e);
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

                DataObject dbObj = session.addObject(paasLM.database);
                String dbName = "generated_config_" + configObj.object;
                baseLM.name.change(dbName, session, dbObj);

                baseLM.name.change("Configuration " + configObj.object, session, configObj);
                paasLM.configurationStatus.change(paasLM.status.getObjectID("stopped"), session, configObj);
                paasLM.configurationProject.change(projectId, session, configObj);
                paasLM.configurationDatabase.change(dbObj.object, session, configObj);
                paasLM.configurationExportName.change(dbName, session, configObj);
                paasLM.configurationPort.change(0, session, configObj);

                session.apply(businessLogics);
            } finally {
                session.close();
            }
            return getProjectConfigurations(userLogin, projectId);
        } catch (Exception e) {
            logger.error("", e);
            throw new RemoteMessageException("Ошибка при добавлении конфигурации", e);
        }
    }

    @Override
    public ConfigurationDTO[] removeConfiguration(String userLogin, int projectId, int configurationId) throws RemoteException {
        try {
            DataSession session = createSession();
            try {
                checkProjectPermission(userLogin, projectId);

                session.changeClass(new DataObject(configurationId, paasLM.configuration), null);

                session.apply(businessLogics);
            } finally {
                session.close();
            }
            return getProjectConfigurations(userLogin, projectId);
        } catch (Exception e) {
            logger.error("", e);
            throw new RemoteMessageException("Ошибка при удалении конфигурации", e);
        }
    }

    @Override
    public ConfigurationDTO[] updateConfiguration(String userLogin, ConfigurationDTO configuration) throws RemoteException {
        String currentStatus = businessLogics.getConfigurationStatus(configuration.id);
        if ("started".equals(currentStatus)) {
            //запрещаем изменение запущенных конфигураций
            throw new RemoteMessageException("Изменение запущенных конфигураций запрещено");
        }

        try {
            DataSession session = createSession();
            try {
                DataObject confObj = new DataObject(configuration.id, paasLM.configuration);

                Integer projId = (Integer) paasLM.configurationProject.read(session, confObj);
                checkProjectPermission(userLogin, projId);

                updateConfiguration(session, confObj, configuration);

                session.apply(businessLogics);

                return getProjectConfigurations(userLogin, projId);
            } finally {
                session.close();
            }
        } catch (Exception e) {
            logger.error("", e);
            throw new RemoteMessageException("Ошибка при сохранении конфигурации", e);
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

                AppManager appManager = context.getLogicsInstance().getCustomObject(AppManager.class);
                appManager.executeConfiguration(session, confObj);

                String errorMsg = waitForStarted(configuration.id);
                if (errorMsg != null) {
                    throw new RuntimeException(errorMsg);
                }

                session.apply(businessLogics);

                return getProjectConfigurations(userLogin, projId);
            } finally {
                session.close();
            }
        } catch (Exception e) {
            throw new RemoteMessageException("Ошибка при старте конфигурации", e);
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
        // пока не даём менять укникальное сгенерированное имя для экспорта
//        paasLM.configurationExportName.change(configuration.exportName, session, confObj);
        baseLM.name.change(configuration.name, session, confObj);
    }

    private String waitForStarted(int configurationId) throws RemoteException {
        return waitForStatus(configurationId, "started");
    }

    private String waitForStopped(int configurationId) throws RemoteException {
        return waitForStatus(configurationId, "stopped");
    }

    private String waitForStatus(int configurationId, String status) throws RemoteException {
        //ждём 3 минуты
        int maxAttempts = 3 * 60;
        int attempts = 0;
        while (true) {
            String error = businessLogics.popConfigurationLaunchError(configurationId);
            if (error != null) {
                return error;
            }

            if (status.equals(businessLogics.getConfigurationStatus(configurationId))) {
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

                session.apply(businessLogics);

                return getProjectConfigurations(userLogin, projId);
            } finally {
                session.close();
            }
        } catch (Exception e) {
            logger.error("", e);
            throw new RemoteMessageException("Ошибка при остановке конфигурации", e);
        }
    }

    @Override
    public ConfigurationDTO getConfiguration(String userLogin, int configurationId) throws RemoteException {
        try {
            DataSession session = createSession();
            try {
                DataObject confObj = session.getDataObject(paasLM.configuration, configurationId);
                if (confObj.objectClass != paasLM.configuration) {
                    throw new RuntimeException("Конфигурация не найдена");
                }

                Integer projId = (Integer) paasLM.configurationProject.read(session, confObj);
                if (userLogin != null) {
                    //если userLogin == null, то мы запрашиваем информацию о конфигурации для внутренних целей (для соединения с запущенной конфигурацией)
                    checkProjectPermission(userLogin, projId);
                }

                Integer port = (Integer) paasLM.configurationPort.read(session, confObj);

                String exportName = (String) paasLM.configurationExportName.read(session, confObj);

                String name = (String) baseLM.name.read(session, confObj);

                ConfigurationDTO configuration = new ConfigurationDTO();
                configuration.name = name;
                configuration.port = port;
                configuration.exportName = nullTrim(exportName);
                return configuration;
            } finally {
                session.close();
            }
        } catch (Exception e) {
            logger.error("", e);
            throw new RemoteMessageException("Ошибка при считывании информации о конфигурации", e);
        }
    }

    private void addModuleToProject(DataSession session, DataObject projectId, DataObject moduleId) throws SQLException {
        paasLM.moduleInProject.change(true, session, projectId, moduleId);
    }

    @Aspect
    private static class RemoteLogicsContextHoldingAspect {
        @Before("execution(* paas.api.remote.PaasRemoteInterface.*(..)) && target(remoteLogics)")
        public void beforeCall(PaasRemoteLogics remoteLogics) {
            ThreadLocalContext.set(remoteLogics.getContext());
        }
    }
}
