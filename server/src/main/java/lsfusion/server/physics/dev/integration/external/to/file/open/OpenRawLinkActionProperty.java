package lsfusion.server.physics.dev.integration.external.to.file.open;

import com.google.common.base.Throwables;
import lsfusion.interop.action.OpenUriClientAction;
import lsfusion.server.data.DataObject;
import lsfusion.server.data.ObjectValue;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.physics.dev.integration.internal.to.ScriptingAction;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.link.LinkClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import org.apache.commons.httpclient.util.URIUtil;

import java.net.URI;
import java.sql.SQLException;
import java.util.Iterator;

public class OpenRawLinkActionProperty extends ScriptingAction {
    private final ClassPropertyInterface sourceInterface;

    public OpenRawLinkActionProperty(BaseLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        sourceInterface = i.next();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            ObjectValue sourceObject = context.getDataKeyValue(sourceInterface);
            for (URI file : ((LinkClass) ((DataObject) sourceObject).getType()).getFiles(sourceObject.getValue())) {
                context.delayUserInteraction(new OpenUriClientAction(new URI(URIUtil.decode(file.toString()))));
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}