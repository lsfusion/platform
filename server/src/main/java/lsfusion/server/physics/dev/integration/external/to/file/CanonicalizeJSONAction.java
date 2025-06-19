package lsfusion.server.physics.dev.integration.external.to.file;

import com.google.common.base.Throwables;
import lsfusion.base.file.FileData;
import lsfusion.base.file.RawFileData;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.logics.UtilsLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;
import org.erdtman.jcs.JsonCanonicalizer;

import java.io.IOException;
import java.sql.SQLException;

public class CanonicalizeJSONAction extends InternalAction {
    public CanonicalizeJSONAction(UtilsLogicsModule LM, ValueClass... valueClasses) {
        super(LM, valueClasses);
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        try {
            final RawFileData jsonFile = (RawFileData) context.getSingleDataKeyValue().getValue();
            JsonCanonicalizer jc = new JsonCanonicalizer(jsonFile.getBytes());
            LM.findProperty("canonicalizedJSON[]").change(new DataObject(jc.getEncodedString()), context);
        } catch (IOException | ScriptingErrorLog.SemanticErrorException | SQLException | SQLHandledException e) {
            throw Throwables.propagate(e);
        }
    }
}