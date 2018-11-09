package lsfusion.utils.utils;

import com.google.common.base.Throwables;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.SQLException;
import java.util.Iterator;

public class RichTextToStringActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface richTextInterface;

    public RichTextToStringActionProperty(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        richTextInterface = i.next();
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        String value = (String) context.getDataKeyValue(richTextInterface).getValue();
        try {
            findProperty("resultString[]").change(Jsoup.clean(URLDecoder.decode(value, "UTF-8"), Whitelist.none()), context);
        } catch (ScriptingErrorLog.SemanticErrorException | UnsupportedEncodingException e) {
            throw Throwables.propagate(e);
        }
    }
}