package lsfusion.server.logics.navigator;

import com.google.common.base.Throwables;
import lsfusion.base.file.FileData;
import lsfusion.base.file.IOUtils;
import lsfusion.base.file.RawFileData;
import lsfusion.server.Settings;
import lsfusion.server.logics.classes.StringClass;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.language.ScriptingActionProperty;
import lsfusion.server.logics.DataObject;
import lsfusion.server.physics.admin.authentication.SecurityLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.action.ExecutionContext;

import java.net.URLDecoder;
import java.sql.SQLException;
import java.util.Iterator;

import static lsfusion.base.BaseUtils.isRedundantString;

public class GenerateJNLPActionProperty extends ScriptingActionProperty {
    private static final String DEFAULT_INIT_HEAP_SIZE = "32m";
    private static final String DEFAULT_MAX_HEAP_SIZE = "800m";
    private static final String DEFAULT_MIN_HEAP_FREE_RATIO = "30";
    private static final String DEFAULT_MAX_HEAP_FREE_RATIO = "70";
    private static final String DEFAULT_CODEBASE_URL = "localhost";
    private static final String DEFAULT_JNLP_URL = "localhost";
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 7652;
    private static final String DEFAULT_EXPORT_NAME = "default";
  
    private final ClassPropertyInterface maxHeapSizeInterface;
    private final ClassPropertyInterface vmargsInterface;
    
    public GenerateJNLPActionProperty(SecurityLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        maxHeapSizeInterface = i.next();
        vmargsInterface = i.next();
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        String memoryLimitMaxHeapSize = (String) context.getKeyValue(maxHeapSizeInterface).getValue();
        String memoryLimitVMArgs = (String) context.getKeyValue(vmargsInterface).getValue();

        try {
            String initHeapSize = (String) LM.findProperty("initHeapSize[]").read(context);
            String maxHeapSize = (String) LM.findProperty("maxHeapSize[]").read(context);
            String minHeapFreeRatio = (String) LM.findProperty("minHeapFreeRatio[]").read(context);
            String maxHeapFreeRatio = (String) LM.findProperty("maxHeapFreeRatio[]").read(context);
            String vmargs = (String) LM.findProperty("vmargs[]").read(context);

            String url = (String) LM.findProperty("url[]").read(context);
            int execSlashIndex = url.lastIndexOf("/");
            String codebaseUrl = url.substring(0, execSlashIndex);
            String query = (String) LM.findProperty("query[]").read(context);
            query = query.replaceAll("&", "&amp;"); // escaping
            String jnlpUrl = url.substring(execSlashIndex + 1) + "?" + query;
            String host = (String) LM.findProperty("host[]").read(context);
            Integer port = (Integer) LM.findProperty("port[]").read(context);
            String exportName = (String) LM.findProperty("exportName[]").read(context);
            
            String jnlpString = IOUtils.readStreamToString(getClass().getResourceAsStream("/client.jnlp"));
            jnlpString = jnlpString.replace("${jnlp.codebase}", !isRedundantString(codebaseUrl) ? codebaseUrl : DEFAULT_CODEBASE_URL) 
                    .replace("${jnlp.url}", !isRedundantString(jnlpUrl) ? jnlpUrl : DEFAULT_JNLP_URL)
                    .replace("${jnlp.appName}", "lsFusion")
                    .replace("${jnlp.host}", !isRedundantString(host) ? host : DEFAULT_HOST)
                    .replace("${jnlp.port}", String.valueOf(port != null ? port : DEFAULT_PORT))
                    .replace("${jnlp.exportName}", !isRedundantString(exportName) ? exportName : DEFAULT_EXPORT_NAME)
                    .replace("${jnlp.singleInstance}", String.valueOf(Settings.get().isSingleInstance()))
                    .replace("${jnlp.initHeapSize}", !isRedundantString(initHeapSize) ? initHeapSize : DEFAULT_INIT_HEAP_SIZE)
                    .replace("${jnlp.maxHeapSize}", memoryLimitMaxHeapSize != null ? memoryLimitMaxHeapSize : (!isRedundantString(maxHeapSize) ? maxHeapSize : DEFAULT_MAX_HEAP_SIZE))
                    .replace("${jnlp.minHeapFreeRatio}", !isRedundantString(minHeapFreeRatio) ? minHeapFreeRatio : DEFAULT_MIN_HEAP_FREE_RATIO)
                    .replace("${jnlp.maxHeapFreeRatio}", !isRedundantString(maxHeapFreeRatio) ? maxHeapFreeRatio : DEFAULT_MAX_HEAP_FREE_RATIO)
                    .replace("${jnlp.vmargs}", memoryLimitVMArgs != null ? URLDecoder.decode(memoryLimitVMArgs, "utf-8") : (!isRedundantString(vmargs) ? vmargs : ""));

            findProperty("headersTo[TEXT]").change("attachment; filename=\"client.jnlp\"", context, new DataObject("Content-Disposition", StringClass.text));
            findProperty("headersTo[TEXT]").change("", context, new DataObject("Cache-Control", StringClass.text)); // with Cache-Control 'no-cache, no-store' application won't install
            findProperty("exportFile[]").change(new FileData(new RawFileData(jnlpString.getBytes()), "jnlp"), context);
        } catch (Exception e) {
            Throwables.propagate(e);
        }
    }
}
