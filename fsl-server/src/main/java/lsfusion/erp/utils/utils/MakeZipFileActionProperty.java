package lsfusion.erp.utils.utils;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.IOUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class MakeZipFileActionProperty extends ScriptingActionProperty {

    public MakeZipFileActionProperty(ScriptingLogicsModule LM, ValueClass... classes) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, classes);
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {

            KeyExpr iExpr = new KeyExpr("i");
            ImRevMap<Object, KeyExpr> keys = MapFact.singletonRev((Object) "i", iExpr);
            QueryBuilder<Object, Object> query = new QueryBuilder<>(keys);
            query.addProperty("zipping", findProperty("zipping[VARSTRING[100]]").getExpr(context.getModifier(), iExpr));
            query.and(findProperty("zipping[VARSTRING[100]]").getExpr(context.getModifier(), iExpr).getWhere());

            ImOrderMap<ImMap<Object, DataObject>, ImMap<Object, ObjectValue>> result = query.executeClasses(context);
            if(!result.isEmpty()) {

                File zipFile = null;
                try {
                    zipFile = File.createTempFile("zip", ".zip");
                    FileOutputStream fos = new FileOutputStream(zipFile);
                    try (ZipOutputStream zos = new ZipOutputStream(fos)) {

                        for (int i = 0; i < result.size(); i++) {
                            String fileName = (String) result.getKey(i).get("i").getValue();
                            byte[] fileBytes = (byte[]) result.getValue(i).get("zipping").getValue();
                            if (fileBytes != null) {
                                ByteArrayInputStream bis = new ByteArrayInputStream(fileBytes);
                                zos.putNextEntry(new ZipEntry(fileName));
                                byte[] buf = new byte[1024];
                                int len;
                                while ((len = bis.read(buf)) > 0) {
                                    zos.write(buf, 0, len);
                                }
                                bis.close();
                            }
                        }
                    }
                    findProperty("zipped[]").change(BaseUtils.mergeFileAndExtension(IOUtils.getFileBytes(zipFile), "zip".getBytes()), context);
                } finally {
                    if(zipFile != null && !zipFile.delete())
                        zipFile.deleteOnExit();
                }
            }

        } catch (IOException | ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}