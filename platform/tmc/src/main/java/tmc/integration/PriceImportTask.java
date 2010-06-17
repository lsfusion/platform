package tmc.integration;

import org.xBaseJ.DBF;
import org.xBaseJ.xBaseJException;

import java.io.File;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.List;
import java.sql.SQLException;

import platform.server.classes.DataClass;
import platform.server.classes.StringClass;
import platform.server.classes.DoubleClass;
import platform.server.data.KeyField;
import platform.server.data.PropertyField;
import platform.server.data.Field;
import platform.server.data.CustomSessionTable;
import platform.server.data.type.ObjectType;
import platform.server.data.query.Query;
import platform.server.data.query.Join;
import platform.server.data.expr.KeyExpr;
import platform.server.data.where.classes.ClassWhere;
import platform.server.session.DataSession;
import platform.server.session.MapDataChanges;
import platform.server.session.PropertyChange;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.scheduler.SchedulerTask;
import platform.server.logics.scheduler.FlagSemaphoreTask;
import platform.server.logics.property.PropertyInterface;
import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.interop.Compare;
import tmc.integration.SinglePriceImportTask;
import tmc.VEDBusinessLogics;

public class PriceImportTask implements SchedulerTask {

    VEDBusinessLogics BL;
    List<String> path;
    List<Integer> docID;

    public PriceImportTask(VEDBusinessLogics BL, List<String> path, List<Integer> docID) {

        this.BL = BL;
        this.path = path;
        this.docID = docID;
    }

    public String getID() {
        return "priceImport (" + path + ")";
    }

    public void execute() throws Exception {

        for (int impNum = 0; impNum < path.size(); impNum++) {

            String impPath = path.get(impNum);
            Integer impDocID = docID.get(impNum);

            FlagSemaphoreTask.run(impPath + "\\tmc.new", new SinglePriceImportTask(BL, impPath + "\\datanew.dbf", impDocID));
            FlagSemaphoreTask.run(impPath + "\\tmc.upd", new SinglePriceImportTask(BL, impPath + "\\dataupd.dbf", impDocID));
        }
    }
}
