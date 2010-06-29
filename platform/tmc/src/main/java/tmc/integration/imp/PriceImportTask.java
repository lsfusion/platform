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

    public PriceImportTask(VEDBusinessLogics BL, List<String> path, List<Integer> docID, List<Integer> actionID) {

        this.BL = BL;
        this.path = path;
        this.docID = docID;
        this.actionID = actionID;
    }

    public String getID() {
        return "priceImport (" + path + ")";
    }

    public void execute() throws Exception {

        for (int impNum = 0; impNum < path.size(); impNum++) {

            String impPath = path.get(impNum);
            Integer impDocID = docID.get(impNum);
            Integer impActionID = actionID.get(impNum);

            FlagSemaphoreTask.run(impPath + "\\tmc.new", new SinglePriceImportTask(BL, impPath + "\\datanew.dbf", impDocID, impActionID));
            FlagSemaphoreTask.run(impPath + "\\tmc.upd", new SinglePriceImportTask(BL, impPath + "\\dataupd.dbf", impDocID, impActionID));
        }
    }
}
