package lsfusion.server.logics.classes.utils.string;

import com.google.common.base.Throwables;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.UtilsLogicsModule;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.language.ScriptingAction;
import lsfusion.server.language.ScriptingErrorLog;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.SQLException;
import java.util.Iterator;

public class RichTextToStringActionProperty extends ScriptingAction {
    private final ClassPropertyInterface richTextInterface;

    public RichTextToStringActionProperty(UtilsLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        richTextInterface = i.next();
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        String value = (String) context.getDataKeyValue(richTextInterface).getValue();
        try {
            findProperty("resultString[]").change(value != null ? Jsoup.clean(URLDecoder.decode(value, "UTF-8"), Whitelist.none()) : null, context);
        } catch (ScriptingErrorLog.SemanticErrorException | UnsupportedEncodingException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}