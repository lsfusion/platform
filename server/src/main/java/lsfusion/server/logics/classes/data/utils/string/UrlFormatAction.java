package lsfusion.server.logics.classes.data.utils.string;

import com.google.common.base.Throwables;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.server.logics.UtilsLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.external.to.ExternalHTTPAction;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

public class UrlFormatAction extends InternalAction {

    public UrlFormatAction(UtilsLogicsModule LM) {
        super(LM);
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        try {
            ImOrderMap<String, String> params = ExternalHTTPAction.readPropertyValues(context.getSession(), findProperty("urlParsed[TEXT]")).toOrderMap();
            findProperty("urlFormatted[]").change(params.mapOrderSetValues((key, value) -> key + "=" + value).toString("&"), context);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}