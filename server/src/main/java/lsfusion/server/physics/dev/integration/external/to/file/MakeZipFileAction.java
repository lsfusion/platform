package lsfusion.server.physics.dev.integration.external.to.file;

import com.google.common.base.Throwables;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.file.FileData;
import lsfusion.base.file.RawFileData;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.query.build.QueryBuilder;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.logics.UtilsLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class MakeZipFileAction extends InternalAction {

    public MakeZipFileAction(UtilsLogicsModule LM) {
        super(LM);
    }

    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {

            KeyExpr iExpr = new KeyExpr("i");
            ImRevMap<Object, KeyExpr> keys = MapFact.singletonRev((Object) "i", iExpr);
            QueryBuilder<Object, Object> query = new QueryBuilder<>(keys);
            query.addProperty("zipping", findProperty("zipping[VARSTRING[1000]]").getExpr(context.getModifier(), iExpr));
            query.and(findProperty("zipping[VARSTRING[1000]]").getExpr(context.getModifier(), iExpr).getWhere());

            ImOrderMap<ImMap<Object, DataObject>, ImMap<Object, ObjectValue>> result = query.executeClasses(context);
            if(!result.isEmpty()) {

                File zipFile = null;
                try {
                    zipFile = File.createTempFile("zip", ".zip");
                    FileOutputStream fos = new FileOutputStream(zipFile);
                    try (ZipOutputStream zos = new ZipOutputStream(fos)) {

                        for (int i = 0; i < result.size(); i++) {
                            String fileName = (String) result.getKey(i).get("i").getValue();
                            FileData fileBytes = (FileData) result.getValue(i).get("zipping").getValue();
                            if (fileBytes != null) {
                                InputStream bis = fileBytes.getRawFile().getInputStream();
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
                    findProperty("zipped[]").change(new FileData(new RawFileData(zipFile), "zip"), context);
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