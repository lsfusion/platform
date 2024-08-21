package lsfusion.server.logics.classes.data.utils.string;

import com.google.common.base.Throwables;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.interop.session.ExternalUtils;
import lsfusion.server.logics.UtilsLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.external.to.CallHTTPAction;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.net.URLEncodedUtils;

import java.nio.charset.StandardCharsets;

public class UrlFormatAction extends InternalAction {

    public UrlFormatAction(UtilsLogicsModule LM) {
        super(LM);
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        try {
            ImOrderMap<String, String> params = CallHTTPAction.readPropertyValues(context.getSession(), findProperty("urlParsed[TEXT]")).toOrderMap();
            findProperty("urlFormatted[]").change(URLEncodedUtils.format(params.mapOrderSetValues(BasicNameValuePair::new).toJavaList(), ExternalUtils.defaultUrlCharset), context);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}