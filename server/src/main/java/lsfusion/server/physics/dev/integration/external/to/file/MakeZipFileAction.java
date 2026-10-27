package lsfusion.server.physics.dev.integration.external.to.file;

import com.google.common.base.Throwables;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.file.FileData;
import lsfusion.base.lambda.EConsumer;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.query.build.QueryBuilder;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.logics.UtilsLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.session.change.modifier.Modifier;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class MakeZipFileAction extends InternalAction {

    public MakeZipFileAction(UtilsLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            boolean zeroTime = getParam(0, context) != null;

            KeyExpr iExpr = new KeyExpr("i");
            ImRevMap<Object, KeyExpr> keys = MapFact.singletonRev("i", iExpr);
            QueryBuilder<Object, Object> query = new QueryBuilder<>(keys);
            Modifier modifier = context.getModifier();
            query.addProperty("zipping", findProperty("zipping[STRING[1000]]").getExpr(modifier, iExpr));
            query.addProperty("zippingPath", findProperty("zippingPath[STRING[1000]]").getExpr(modifier, iExpr));
            query.and(findProperty("zipping[STRING[1000]]").getExpr(modifier, iExpr).getWhere().or(findProperty("zippingPath[STRING[1000]]").getExpr(modifier, iExpr).getWhere()));

            ImOrderMap<ImMap<Object, DataObject>, ImMap<Object, ObjectValue>> result = query.executeClasses(context);
            if (!result.isEmpty()) {
                Map<String, EConsumer<OutputStream, IOException>> files = new HashMap<>();
                for (int i = 0; i < result.size(); i++) {
                    String fileName = (String) result.getKey(i).get("i").getValue();
                    FileData fileBytes = (FileData) result.getValue(i).get("zipping").getValue();
                    if (fileBytes != null)
                        files.put(fileName, fileBytes.getRawFile()::write);
                    else {
                        Path filePath = Paths.get((String) result.getValue(i).get("zippingPath").getValue());
                        files.put(fileName, out -> Files.copy(filePath, out));
                    }
                }
                FileData zipFile = ZipUtils.writeZipFile(files, zeroTime);
                findProperty("zipped[]").change(zipFile, context);
            }

        } catch (IOException | ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}