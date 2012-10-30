package tmc.integration.scheduler;

import platform.server.classes.ValueClass;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingActionProperty;
import platform.server.logics.scripted.ScriptingErrorLog;
import platform.server.logics.scripted.ScriptingLogicsModule;
import platform.server.session.DataSession;
import tmc.VEDBusinessLogics;
import tmc.integration.exp.DateSaleExportTask;
import tmc.integration.exp.NewSaleExportTask;
import tmc.integration.imp.SinglePriceImportTask;

import java.sql.SQLException;

public class PriceImportTaskActionProperty extends ScriptingActionProperty {

    public PriceImportTaskActionProperty(ScriptingLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, new ValueClass[]{});
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) {
        try {
            DataSession session = createSession();
            String path = (String) LM.findLCPByCompoundName("pathPriceImportTask").read(session);
            String docID = (String) LM.findLCPByCompoundName("docIDPriceImportTask").read(session);
            String actionID = (String) LM.findLCPByCompoundName("actionIDPriceImportTask").read(session);
            String returnDocID = (String) LM.findLCPByCompoundName("returnDocIDPriceImportTask").read(session);

            String[] pathList = path != null ? path.split(",") : null;
            String[] docIDList = path != null ? docID.split(",") : null;
            String[] actionIDList = path != null ? actionID.split(",") : null;
            String[] returnDocIDList = path != null ? returnDocID.split(",") : null;
            
            if(pathList!=null)
            for (int impNum = 0; impNum < pathList.length; impNum++) {

                String impPath = pathList[impNum];
                Integer impDocID = Integer.valueOf(docIDList[impNum]);
                Integer impActionID = Integer.valueOf(actionIDList[impNum]);
                Integer impReturnDocID = Integer.valueOf(returnDocIDList[impNum]);

                FlagSemaphoreTask.run(impPath + "\\tmc.new", new SinglePriceImportTask((VEDBusinessLogics) LM.getBL(), impPath, "datanew", impDocID, impActionID, impReturnDocID));
                FlagSemaphoreTask.run(impPath + "\\tmc.upd", new SinglePriceImportTask((VEDBusinessLogics) LM.getBL(), impPath, "dataupd", impDocID, impActionID, impReturnDocID));
            }
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

    }


}
