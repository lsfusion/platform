package tmc.integration.exp;

import platform.server.logics.scheduler.FlagSemaphoreTask;
import platform.server.logics.scheduler.SchedulerTask;
import tmc.VEDBusinessLogics;

import java.util.List;

public class SaleExportTask implements SchedulerTask {

    private VEDBusinessLogics BL;
    private List<String> path;
    private List<Integer> store;

    public String getPath(Integer storePath ) {
        for(int i=0;i<store.size();i++)
            if(store.get(i).equals(storePath))
                return path.get(i);
        return null;
    }

    public SaleExportTask(VEDBusinessLogics BL, List<String> path, List<Integer> store) {

        this.BL = BL;
        this.path = path;
        this.store = store;
    }

    public String getID() {
        return "saleExport";
    }

    public void execute() throws Exception {

        for(int i=0;i<path.size();i++) {
            String expPath = path.get(i);
            Integer expStore = store.get(i);

            FlagSemaphoreTask.run(expPath + "\\pos.cur", new NewSaleExportTask(BL, expPath, expStore));
            FlagSemaphoreTask.run(expPath + "\\pos.dat", new DateSaleExportTask(BL, expPath, expStore));
        }
    }

}
