package tmc.integration.exp;

import platform.server.logics.scheduler.SchedulerTask;
import platform.server.logics.scheduler.FlagSemaphoreTask;
import tmc.VEDBusinessLogics;

public class SaleExportTask implements SchedulerTask {

    private VEDBusinessLogics BL;
    private String path;

    public SaleExportTask(VEDBusinessLogics BL, String path) {

        this.BL = BL;
        this.path = path;
    }

    public String getID() {
        return "saleExport";
    }

    public void execute() throws Exception {

        FlagSemaphoreTask.run(path + "\\pos.cur", new NewSaleExportTask(BL, path));
        FlagSemaphoreTask.run(path + "\\pos.dat", new DateSaleExportTask(BL, path));
    }

}
