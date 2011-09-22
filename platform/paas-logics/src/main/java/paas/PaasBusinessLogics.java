package paas;

import net.sf.jasperreports.engine.JRException;
import paas.api.gwt.shared.dto.ConfigurationDTO;
import paas.api.gwt.shared.dto.ModuleDTO;
import paas.api.gwt.shared.dto.ProjectDTO;
import paas.api.remote.PaasRemoteInterface;
import platform.base.OrderedMap;
import platform.interop.Compare;
import platform.interop.action.MessageClientAction;
import platform.server.auth.User;
import platform.server.classes.ValueClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.Query;
import platform.server.data.sql.DataAdapter;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.form.instance.FormInstance;
import platform.server.form.view.DefaultFormView;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.linear.LP;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.session.DataSession;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static platform.base.BaseUtils.isRedundantString;
import static platform.base.BaseUtils.nvl;

public class PaasBusinessLogics extends BusinessLogics<PaasBusinessLogics> implements PaasRemoteInterface {
    private PaasLogicsModule paasLM;

    private BLLogicsManager logicsManager;

    public PaasBusinessLogics(DataAdapter iAdapter, int port) throws IOException, ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException, JRException {
        super(iAdapter, port);
    }

    @Override
    protected void createModules() {
        super.createModules();

        paasLM = new PaasLogicsModule(LM, this);
        addLogicsModule(paasLM);
    }

    @Override
    protected void initModules() throws ClassNotFoundException, IOException, SQLException, InstantiationException, IllegalAccessException, JRException {
        super.initModules();

        logicsManager = new BLLogicsManager();
        refreshConfigurationStatuses(null);
        cleanDatabases();
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
                        paasLM.databaseConfiguration.getExpr(session.modifier, dbExpr).getWhere().not()
                );

                q.properties.put("name", LM.name.getExpr(session.modifier, dbExpr));

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
            DataObject dbOjb = session.addObject(paasLM.database, session.modifier, false, true);

            //todo: возможно генерацию имени стоит переделать на что-нибудь более надёжное, типа зачитывания текущих значений и выбора MAX+1
            LM.name.execute("paas_generated_" + System.currentTimeMillis(), session, dbOjb);

            session.apply(this);

            return (Integer)dbOjb.object;
        } finally {
            session.close();
        }
    }

    protected void initAuthentication() throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
        User admin = addUser("admin", "fusion");
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

                DataObject projObj = session.addObject(paasLM.project, session.modifier, false, true);

                LM.name.execute(newProject.name, session, projObj);
                paasLM.projectDescription.execute(newProject.description, session, projObj);
                paasLM.projectOwner.execute(userId, session, projObj);

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

                LM.name.execute(project.name, session, projObj);
                paasLM.projectDescription.execute(project.description, session, projObj);

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
                    paasLM.moduleSource.execute(moduleTexts[i], session, new DataObject(moduleIds[i], paasLM.module));
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

                DataObject moduleObj = session.addObject(paasLM.module, session.modifier, false, true);

                LM.name.execute(newModule.name, session, moduleObj);

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

                paasLM.moduleInProject.execute(null, session, new DataObject(projectId, paasLM.project), new DataObject(moduleId, paasLM.module));

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

                DataObject configObj = session.addObject(paasLM.configuration, session.modifier, false, true);

                int databaseId = generateDatabase();

                LM.name.execute("Configuration " + configObj.object, session, configObj);
                paasLM.configurationStatus.execute(paasLM.status.getID("stopped"), session, configObj);
                paasLM.configurationProject.execute(projectId, session, configObj);
                paasLM.configurationDatabase.execute(databaseId, session, configObj);

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
        try {
            DataSession session = createSession();
            try {
                DataObject confObj = new DataObject(configuration.id, paasLM.configuration);

                Integer projId = (Integer) paasLM.configurationProject.read(session, session.modifier, confObj);
                checkProjectPermission(userLogin, projId);

                paasLM.configurationPort.execute(configuration.port, session, confObj);
//                paasLM.configurationDatabase.execute(configuration.port, session, confObj);
                LM.name.execute(configuration.name, session, confObj);

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
    public ConfigurationDTO[] startConfiguration(String userLogin, int configurationId) throws RemoteException {
        try {
            DataSession session = createSession();

            try {
                DataObject confObj = new DataObject(configurationId, paasLM.configuration);

                Integer projId = (Integer) paasLM.configurationProject.read(session, session.modifier, confObj);
                checkProjectPermission(userLogin, projId);

                paasLM.configurationStart.execute(true, session, confObj);

                session.apply(this);

                return getProjectConfigurations(userLogin, projId);
            } finally {
                session.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RemoteException("Ошибка при старте конфигурации", e);
        }
    }

    @Override
    public ConfigurationDTO[] stopConfiguration(String userLogin, int configurationId) throws RemoteException {
        try {
            DataSession session = createSession();

            try {
                DataObject confObj = new DataObject(configurationId, paasLM.configuration);

                Integer projId = (Integer) paasLM.configurationProject.read(session, session.modifier, confObj);
                checkProjectPermission(userLogin, projId);

                paasLM.configurationStop.execute(true, session, confObj);

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

                Integer projId = (Integer) paasLM.configurationProject.read(session, session.modifier, confObj);
                checkProjectPermission(userLogin, projId);

                Integer port = (Integer) paasLM.configurationPort.read(session, confObj);
                if (port == null) {
                    throw new IllegalStateException("Порт конфигурации не задан");
                }
                ConfigurationDTO configuration = new ConfigurationDTO();
                configuration.port = port;
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
        paasLM.moduleInProject.execute(true, session, projectId, moduleId);
    }

    public LP addRefreshStatusProperty() {
        return paasLM.addProperty(LM.baseGroup, new LP<ClassPropertyInterface>(new RefreshStatusActionProperty(LM.genSID(), "")));
    }

    public LP addStartConfigurationProperty() {
        return paasLM.addProperty(LM.baseGroup, new LP<ClassPropertyInterface>(new StartConfigurationActionProperty(LM.genSID(), "")));
    }

    public LP addStopConfigurationProperty() {
        return paasLM.addProperty(LM.baseGroup, new LP<ClassPropertyInterface>(new StopConfigurationActionProperty(LM.genSID(), "")));
    }

    private void refreshConfigurationStatuses(DataObject projId) {
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
                            paasLM.configurationProject.getExpr(session.modifier, confExpr).compare(projId, Compare.EQUALS)
                    );
                }

                q.properties.put("port", paasLM.configurationPort.getExpr(session.modifier, confExpr));

                OrderedMap<Map<String, Object>, Map<String, Object>> values = q.execute(session.sql);
                for (Map.Entry<Map<String, Object>, Map<String, Object>> entry : values.entrySet()) {
                    Integer configId = (Integer) entry.getKey().get("configId");
                    Integer port = (Integer) entry.getValue().get("port");

                    if (port != null) {
                        changeConfigurationStatus(session, configId, logicsManager.getStatus(port));
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

    public String executeScriptedBL(DataSession session, DataObject confId) throws IOException, InterruptedException, SQLException {
        if (session == null) {
            session = createSession();
        }

        Integer port = (Integer) paasLM.configurationPort.read(session, session.modifier, confId);
        if (port == null) {
            return "Порт не задан.";
        } else if (!logicsManager.isPortAvailable(port)) {
            return "Порт " + port + " занят.";
        }

        String dbName = (String) paasLM.configurationDatabaseName.read(session, session.modifier, confId);
        if (dbName == null) {
            return "Имя базы данных не задано.";
        }

        dbName = dbName.trim();
        if (dbName.equals(adapter.dataBase.trim())) {
            return "Некорректное имя базы данных";
        }

        Integer projId = (Integer) paasLM.configurationProject.read(session, session.modifier, confId);

        Map<String, KeyExpr> keys = KeyExpr.getMapKeys(asList("moduleKey"));
        Expr moduleExpr = keys.get("moduleKey");
        Expr projExpr = new DataObject(projId, paasLM.project).getExpr();

        Query<String, String> q = new Query<String, String>(keys);
        q.and(
                paasLM.moduleInProject.getExpr(session.modifier, projExpr, moduleExpr).getWhere()
        );
        q.properties.put("moduleOrder", paasLM.moduleOrder.getExpr(session.modifier, projExpr, moduleExpr));
        q.properties.put("moduleName", LM.name.getExpr(session.modifier, moduleExpr));
        q.properties.put("moduleSource", paasLM.moduleSource.getExpr(session.modifier, moduleExpr));

        OrderedMap<String, Boolean> orders = new OrderedMap<String, Boolean>();
        orders.put("moduleOrder", false);

        OrderedMap<Map<String, Object>, Map<String, Object>> values = q.execute(session.sql, orders);

        List<String> moduleNames = new ArrayList<String>();
        List<String> moduleFilePaths = new ArrayList<String>();
        for (Map.Entry<Map<String, Object>, Map<String, Object>> entry : values.entrySet()) {
            String moduleName = (String) entry.getValue().get("moduleName");
            String moduleSource = nvl((String) entry.getValue().get("moduleSource"), "");

            if (isRedundantString(moduleName)) {
                return "Имя модуля не задано";
            }

            moduleNames.add(moduleName.trim());
            moduleFilePaths.add(createTemporaryScriptFile(moduleSource));
        }

        logicsManager.executeScriptedBL(port, dbName, moduleNames, moduleFilePaths);

        return null;
    }

    private String createTemporaryScriptFile(String moduleSource) throws IOException {
        File moduleFile = File.createTempFile("paas", ".lsf");

        PrintStream ps = new PrintStream(new FileOutputStream(moduleFile), false, "UTF-8");
        ps.print(moduleSource);
        ps.close();

        return moduleFile.getAbsolutePath();
    }

    public class RefreshStatusActionProperty extends ActionProperty {

        private RefreshStatusActionProperty(String sID, String caption) {
            super(sID, caption, new ValueClass[]{paasLM.project});
        }

        @Override
        public void execute(ExecutionContext context) throws SQLException {
            refreshConfigurationStatuses(context.getSingleKeyValue());
            FormInstance<?> form = context.getFormInstance();
            if (form != null) {
                form.refreshData();
            }
        }

        @Override
        public void proceedDefaultDesign(DefaultFormView view, PropertyDrawEntity<ClassPropertyInterface> entity) {
            super.proceedDefaultDesign(view, entity);
            view.get(entity).design.setIconPath("refresh.png");
        }
    }

    public class StartConfigurationActionProperty extends ActionProperty {

        private StartConfigurationActionProperty(String sID, String caption) {
            super(sID, caption, new ValueClass[]{paasLM.configuration});
        }

        @Override
        public void execute(ExecutionContext context) throws SQLException {
            DataObject confId = context.getSingleKeyValue();

            try {
                String errorMsg = executeScriptedBL(context.getSession(), confId);
                if (errorMsg != null) {
                    context.getActions().add(new MessageClientAction(errorMsg, "Ошибка!"));
                }
            } catch (SQLException sqle) {
                throw sqle;
            } catch (Exception e) {
                logger.warn("Ошибка при попытке запустить приложение: ", e);
            }

            changeConfigurationStatus(context.getSession(), confId, "started");
        }

        @Override
        public void proceedDefaultDesign(DefaultFormView view, PropertyDrawEntity<ClassPropertyInterface> entity) {
            super.proceedDefaultDesign(view, entity);
            view.get(entity).design.setIconPath("start.png");
        }
    }

    public class StopConfigurationActionProperty extends ActionProperty {
        private StopConfigurationActionProperty(String sID, String caption) {
            super(sID, caption, new ValueClass[]{paasLM.configuration});
        }

        @Override
        public void execute(ExecutionContext context) throws SQLException {
            DataObject confId = context.getSingleKeyValue();

            Integer port = (Integer) paasLM.configurationPort.read(context.getSession(), context.getSession().modifier, confId);
            if (port == null) {
                context.getActions().add(new MessageClientAction("Порт не задан.", "Ошибка!"));
                return;
            }

            logicsManager.stopApplication(port);

            changeConfigurationStatus(context.getSession(), confId, "stopped");
        }

        @Override
        public void proceedDefaultDesign(DefaultFormView view, PropertyDrawEntity<ClassPropertyInterface> entity) {
            super.proceedDefaultDesign(view, entity);
            view.get(entity).design.setIconPath("stop.png");
        }
    }

    private void changeConfigurationStatus(DataSession session, Integer confId, String statusStr) throws SQLException {
        changeConfigurationStatus(session, new DataObject(confId, paasLM.configuration), statusStr);
    }

    private void changeConfigurationStatus(DataSession session, DataObject confId, String statusStr) throws SQLException {
        boolean apply = false;
        if (session == null) {
            apply = true;
            session = createSession();
        }

        paasLM.configurationStatus.execute(paasLM.status.getID(statusStr), session, confId);

        if (apply) {
            session.apply(this);
        }
    }
}
