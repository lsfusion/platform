package lsfusion.utils.utils;

import com.google.common.base.Throwables;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.Iterator;

public class UrlEncodeActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface stringInterface;
    private final ClassPropertyInterface encodingInterface;
    
    public UrlEncodeActionProperty(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        stringInterface = i.next();
        encodingInterface = i.next();
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            String string = (String) context.getKeyValue(stringInterface).getValue();
            String encoding = (String) context.getKeyValue(encodingInterface).getValue();
            String encoded = URLEncoder.encode(string, encoding);
            findProperty("urlEncoded[]").change(encoded, context);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}
