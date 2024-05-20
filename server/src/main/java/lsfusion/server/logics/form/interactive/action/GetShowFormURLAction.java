package lsfusion.server.logics.form.interactive.action;

import com.google.common.base.Throwables;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.interop.session.ExternalUtils;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.SystemExplicitAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.reflection.ReflectionLogicsModule;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.sql.SQLException;

public class GetShowFormURLAction extends InternalAction {

    public GetShowFormURLAction(ReflectionLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            FormInstance form = context.getFormInstance(false, true);
            String script = "NEWSESSION SHOW " + form.entity.getCanonicalName();

            Charset charset = ExternalUtils.defaultUrlCharset;

            String objectsString = "";
            String paramsString = "";
            ImSet<ObjectInstance> objects = form.getObjects();
            int objectsSize = objects.size();
            Object[] httpParams = new Object[objectsSize];
            for(int i = 0; i < objectsSize; i++) {
                ObjectInstance object = objects.get(i);
                objectsString = (objectsString.isEmpty() ? "" : objectsString + ",") + object.getSID() + "=$" + (i + 1) + " NULL";
                Object httpParam = object.getType().formatHTTP(object.getObjectValue().getValue(), charset);
                httpParams[i] = httpParam;
                paramsString += "&" + ExternalUtils.PARAMS_PARAM + "=" + URLEncoder.encode((String) httpParam, charset.name());
            }

            if(!objectsString.isEmpty())
                script += " OBJECTS " + objectsString;

            script += ";";
            Object httpString = StringClass.text.formatHTTP(script, charset);

            String query = ExternalUtils.SCRIPT_PARAM + "=" + URLEncoder.encode((String) httpString, charset.name()) + paramsString +
                    "&" + ExternalUtils.SIGNATURE_PARAM + "=" + context.getSecurityManager().signData(ExternalUtils.generate(httpString, true, httpParams));
            String innerPath = "/eval/action";

            findProperty("readPath[]").change(innerPath, context);
            findProperty("readQuery[]").change(query, context);
        } catch (ScriptingErrorLog.SemanticErrorException | UnsupportedEncodingException e) {
            throw Throwables.propagate(e);
        }
    }
}
