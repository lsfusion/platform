package tmc.integration.imp;

import java.util.List;

import platform.server.logics.scheduler.SchedulerTask;
import platform.server.logics.scheduler.FlagSemaphoreTask;
import tmc.integration.imp.SinglePriceImportTask;
import tmc.VEDBusinessLogics;

public class PriceImportTask implements SchedulerTask {

    VEDBusinessLogics BL;
    List<String> path;
    List<Integer> docID;
    List<Integer> actionID;
    List<Integer> returnDocID;

    public PriceImportTask(VEDBusinessLogics BL, List<String> path, List<Integer> docID, List<Integer> actionID, List<Integer> returnDocID) {

        this.BL = BL;
        this.path = path;
        this.docID = docID;
        this.actionID = actionID;
        this.returnDocID = returnDocID;
    }

    public String getID() {
        return "priceImport (" + path + ")";
    }

    public void execute() throws Exception {

        for (int impNum = 0; impNum < path.size(); impNum++) {

            String impPath = path.get(impNum);
            Integer impDocID = docID.get(impNum);
            Integer impActionID = actionID.get(impNum);
            Integer impReturnDocID = returnDocID.get(impNum);

            FlagSemaphoreTask.run(impPath + "\\tmc.new", new SinglePriceImportTask(BL, impPath + "\\datanew.dbf", impDocID, impActionID, impReturnDocID));
            FlagSemaphoreTask.run(impPath + "\\tmc.upd", new SinglePriceImportTask(BL, impPath + "\\dataupd.dbf", impDocID, impActionID, impReturnDocID));
        }
    }
}
