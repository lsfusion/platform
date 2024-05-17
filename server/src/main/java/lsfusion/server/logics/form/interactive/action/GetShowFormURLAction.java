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
            for(int i = 0, size = objects.size(); i < size; i++) {
                ObjectInstance object = objects.get(i);
                objectsString = (objectsString.isEmpty() ? "" : objectsString + ",") + object.getSID() + "=$" + (i + 1) + " NULL";
                paramsString += "&" + ExternalUtils.PARAMS_PARAM + "=" + object.getType().formatHTTP(object.getObjectValue().getValue(), charset);
            }

            if(!objectsString.isEmpty())
                script += " OBJECTS " + objectsString;

            script += ";";

            String query = ExternalUtils.SCRIPT_PARAM + "=" + URLEncoder.encode(script, charset.name()) + paramsString;
            String innerPath = "/eval/action/";

            findProperty("readPath[]").change(innerPath, context);
            findProperty("readQuery[]").change(query, context);
        } catch (UnsupportedEncodingException | ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}
