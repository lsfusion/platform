package tmc.integration.scheduler;

import jxl.write.DateTime;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import platform.base.BaseUtils;
import platform.base.IOUtils;
import platform.base.OrderedMap;
import platform.interop.Compare;
import platform.interop.action.ExportFileClientAction;
import platform.server.classes.ConcreteClass;
import platform.server.classes.StaticCustomClass;
import platform.server.classes.ValueClass;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.Query;
import platform.server.logics.DataObject;
import platform.server.logics.linear.LCP;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.scripted.ScriptingActionProperty;
import platform.server.logics.scripted.ScriptingErrorLog;
import platform.server.logics.scripted.ScriptingLogicsModule;
import platform.server.session.DataSession;
import tmc.VEDBusinessLogics;
import tmc.integration.exp.DateSaleExportTask;
import tmc.integration.exp.NewSaleExportTask;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.*;

public class SaleExportTaskActionProperty extends ScriptingActionProperty {

    public SaleExportTaskActionProperty(ScriptingLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, new ValueClass[]{});
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) {
        try {
            DataSession session = createSession();
            String path = (String) LM.findLCPByCompoundName("pathSaleExportTask").read(session);
            String store = (String) LM.findLCPByCompoundName("storeSaleExportTask").read(session);
            String[] pathList = path != null ? path.split(",") : null;
            String[] storeList = store != null ? store.split(",") : null;
            if (pathList != null) {
                for (int i = 0; i < pathList.length; i++) {
                    String expPath = pathList[i];
                    Integer expStore = Integer.valueOf(storeList[i]);

                    FlagSemaphoreTask.run(expPath + "\\pos.cur", new NewSaleExportTask((VEDBusinessLogics) LM.getBL(), expPath, expStore));
                    FlagSemaphoreTask.run(expPath + "\\pos.dat", new DateSaleExportTask((VEDBusinessLogics) LM.getBL(), expPath, expStore));
                }
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
