package paas;

import paas.manager.server.AppManager;
import platform.base.SoftHashMap;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderMap;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.interop.Compare;
import platform.server.context.ThreadLocalContext;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.QueryBuilder;
import platform.server.lifecycle.LifecycleEvent;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.session.DataSession;

import java.io.IOException;
import java.sql.SQLException;

public class PaasBusinessLogics extends BusinessLogics<PaasBusinessLogics> {

    private final SoftHashMap<Integer, String> configurationLaunchErrors = new SoftHashMap<Integer, String>();

    public PaasLogicsModule paasLM;

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
    }

    @Override
    protected void onStarted(LifecycleEvent event) {
        super.onStarted(event);

        logger.info("Removing databases of deleted configurations.");
        cleanDatabases();

        logger.info("Updating configurations statuses.");
        refreshConfigurationStatuses(null);
    }

    @Override
    protected void createModules() throws IOException {
        super.createModules();

        paasLM = addModule(new PaasLogicsModule(this));
    }

    private void cleanDatabases() {
        try {
            DataSession session = getDbManager().createSession();
            try {
                ImRevMap<String, KeyExpr> keys = KeyExpr.getMapKeys(SetFact.singleton("dbId"));
                Expr dbExpr = keys.get("dbId");

                QueryBuilder<String, String> q = new QueryBuilder<String, String>(keys);
                q.and(dbExpr.isClass(paasLM.database));
                q.and(
                        paasLM.databaseConfiguration.getExpr(session.getModifier(), dbExpr).getWhere().not()
                );

                q.addProperty("name", LM.name.getExpr(session.getModifier(), dbExpr));

                ImOrderMap<ImMap<String, Object>, ImMap<String, Object>> values = q.execute(session.sql);
                for (ImMap<String, Object> entry : values.valueIt()) {
                    String name = (String) entry.get("name");

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

    public void refreshConfigurationStatuses(DataObject projId) {
        AppManager appManager = ThreadLocalContext.getLogicsInstance().getCustomObject(AppManager.class);
        try {
            DataSession session = getDbManager().createSession();
            try {
                ImRevMap<String, KeyExpr> keys = KeyExpr.getMapKeys(SetFact.singleton("configId"));
                Expr confExpr = keys.get("configId");

                QueryBuilder<String, String> q = new QueryBuilder<String, String>(keys);
                q.and(confExpr.isClass(paasLM.configuration));
                if (projId != null) {
                    q.and(
                            paasLM.configurationProject.getExpr(session.getModifier(), confExpr).compare(projId, Compare.EQUALS)
                    );
                }

                q.addProperty("port", paasLM.configurationPort.getExpr(session.getModifier(), confExpr));

                ImOrderMap<ImMap<String, Object>, ImMap<String, Object>> values = q.execute(session.sql);
                for (int i=0,size=values.size();i<size;i++) {
                    Integer configId = (Integer) values.getKey(i).get("configId");
                    Integer port = (Integer) values.getValue(i).get("port");

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

    public String popConfigurationLaunchError(int configurationId) {
        synchronized (configurationLaunchErrors) {
            return configurationLaunchErrors.remove(configurationId);
        }
    }

    public void pushConfigurationLaunchError(int configurationId, String error) {
        synchronized (configurationLaunchErrors) {
            configurationLaunchErrors.put(configurationId, error);
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
                session = getDbManager().createSession();
            }

            paasLM.configurationStatus.change(paasLM.status.getObjectID(statusStr), session, confId);

            if (apply) {
                session.apply(this);
            }
        } catch (SQLException e) {
            logger.error("Ошибка при изменении статуса конфигурации: ", e);
        }
    }
}
