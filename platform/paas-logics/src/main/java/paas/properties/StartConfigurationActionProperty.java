package paas.properties;

import org.apache.log4j.Logger;
import paas.PaasBusinessLogics;
import paas.PaasLogicsModule;
import platform.base.OrderedMap;
import platform.interop.action.MessageClientAction;
import platform.server.classes.ValueClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.Query;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.form.view.DefaultFormView;
import platform.server.logics.DataObject;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.session.DataSession;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.Arrays.asList;
import static platform.base.BaseUtils.isRedundantString;
import static platform.base.BaseUtils.nvl;

public class StartConfigurationActionProperty extends ActionProperty {
    private final static Logger logger = Logger.getLogger(StartConfigurationActionProperty.class);

    private PaasBusinessLogics paas;
    private PaasLogicsModule paasLM;

    public StartConfigurationActionProperty(PaasBusinessLogics paas, String sID, String caption) {
        super(sID, caption, new ValueClass[]{paas.paasLM.configuration});
        this.paasLM = paas.paasLM;
        this.paas = paas;
    }

    @Override
    public void execute(ExecutionContext context) throws SQLException {
        DataObject confObj = context.getSingleKeyValue();

        Integer configurationPort = (Integer) paas.paasLM.configurationPort.read(context.getSession(), confObj);
        try {
            String errorMsg = executeScriptedBL(context.getSession(), confObj);
            if (errorMsg != null) {
                context.getActions().add(new MessageClientAction(errorMsg, "Ошибка!"));
            }
        } catch (SQLException sqle) {
            throw sqle;
        } catch (Exception e) {
            logger.warn("Ошибка при попытке запустить приложение: ", e);
            paas.changeConfigurationStatus(confObj, paas.appManager.getStatus(configurationPort));
        }
    }

    private AtomicLong nextProcessId = new AtomicLong(0);

    private String executeScriptedBL(DataSession session, DataObject confId) throws IOException, InterruptedException, SQLException {
        paasLM = paas.paasLM;
        Integer port = (Integer) paasLM.configurationPort.read(session, confId);
        if (port == null) {
            return "Порт не задан.";
        } else if (port < 1 || port > 65535) {
            return "Значение порта должно быть между 0 и 65536";
        } else if (!paas.appManager.isPortAvailable(port)) {
            return "Порт " + port + " занят.";
        }

        String dbName = (String) paasLM.configurationDatabaseName.read(session, confId);
        if (dbName == null) {
            return "Имя базы данных не задано.";
        }

        Integer projId = (Integer) paasLM.configurationProject.read(session, confId);

        Map<String, KeyExpr> keys = KeyExpr.getMapKeys(asList("moduleKey"));
        Expr moduleExpr = keys.get("moduleKey");
        Expr projExpr = new DataObject(projId, paasLM.project).getExpr();

        Query<String, String> q = new Query<String, String>(keys);
        q.and(
                paasLM.moduleInProject.getExpr(session.modifier, projExpr, moduleExpr).getWhere()
        );
        q.properties.put("moduleOrder", paasLM.moduleOrder.getExpr(session.modifier, projExpr, moduleExpr));
        q.properties.put("moduleName", paasLM.baseLM.name.getExpr(session.modifier, moduleExpr));
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

        paas.appManager.executeScriptedBL((Integer) confId.object, port, dbName, moduleNames, moduleFilePaths, nextProcessId.incrementAndGet());

        return null;
    }

    private String createTemporaryScriptFile(String moduleSource) throws IOException {
        File moduleFile = File.createTempFile("paas", ".lsf");

        PrintStream ps = new PrintStream(new FileOutputStream(moduleFile), false, "UTF-8");
        ps.print(moduleSource);
        ps.close();

        return moduleFile.getAbsolutePath();
    }

    @Override
    public void proceedDefaultDesign(DefaultFormView view, PropertyDrawEntity<ClassPropertyInterface> entity) {
        super.proceedDefaultDesign(view, entity);
        view.get(entity).design.setIconPath("start.png");
    }
}
