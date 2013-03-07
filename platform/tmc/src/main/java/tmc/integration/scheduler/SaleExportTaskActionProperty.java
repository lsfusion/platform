package tmc.integration.scheduler;

import com.google.common.base.Throwables;
import platform.server.classes.ValueClass;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingActionProperty;
import platform.server.logics.scripted.ScriptingErrorLog;
import platform.server.logics.scripted.ScriptingLogicsModule;
import platform.server.session.DataSession;
import tmc.integration.exp.DateSaleExportTask;
import tmc.integration.exp.NewSaleExportTask;

public class SaleExportTaskActionProperty extends ScriptingActionProperty {

    public SaleExportTaskActionProperty(ScriptingLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, new ValueClass[]{});
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) {
        try {
            DataSession session = context.createSession();
            String path = (String) LM.findLCPByCompoundName("pathSaleExportTask").read(session);
            String store = (String) LM.findLCPByCompoundName("storeSaleExportTask").read(session);
            String[] pathList = path != null ? path.split(",") : null;
            String[] storeList = store != null ? store.split(",") : null;
            if (pathList != null) {
                for (int i = 0; i < pathList.length; i++) {
                    String expPath = pathList[i];
                    Integer expStore = Integer.valueOf(storeList[i]);

                    FlagSemaphoreTask.run(expPath + "\\pos.cur", new NewSaleExportTask(context, expPath, expStore));
                    FlagSemaphoreTask.run(expPath + "\\pos.dat", new DateSaleExportTask(context, expPath, expStore));
                }
            }
        } catch (Exception e) {
            Throwables.propagate(e);
        }
    }
}
