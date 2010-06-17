package tmc.integration;

import platform.server.logics.scheduler.SchedulerTask;
import platform.server.logics.scheduler.FlagSemaphoreTask;

import java.io.File;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.util.List;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.sql.SQLException;

import org.xBaseJ.DBF;
import org.xBaseJ.xBaseJException;
import tmc.VEDBusinessLogics;

public class SaleExportTask extends FlagSemaphoreTask implements SchedulerTask {

    private VEDBusinessLogics BL;
    private List<String> path;

    public SaleExportTask(VEDBusinessLogics BL, List<String> path) {

        this.BL = BL;
        this.path = path;
    }

    public String getID() {
        return "saleExport";
    }

    public void execute() throws Exception {
        FlagSemaphoreTask.run(path + "\\pos.cur", this);
    }

    protected void run() throws Exception {
    }
}
