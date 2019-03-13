package lsfusion.server.logics.classes.utils.string;

import com.google.common.base.Throwables;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.UtilsLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.language.ScriptingActionProperty;
import lsfusion.server.language.ScriptingErrorLog;
import org.apache.commons.codec.binary.Base64;

import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.Iterator;

public class EncodeBase64ActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface stringInterface;

    public EncodeBase64ActionProperty(UtilsLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        stringInterface = i.next();
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        String value = (String) context.getDataKeyValue(stringInterface).getValue();
        try {
            String encoded = new String(Base64.encodeBase64(value.getBytes()), Charset.forName("UTF-8"));
            findProperty("encodedBase64[]").change(encoded, context);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}