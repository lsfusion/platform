package lsfusion.server.logics.classes.data.utils.string;

import com.google.common.base.Throwables;
import lsfusion.server.logics.UtilsLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.external.to.ExternalHTTPAction;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class UrlParseAction extends InternalAction {

    public UrlParseAction(UtilsLogicsModule LM) {
        super(LM);
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        try {
            String url = (String) LM.findProperty("urlFormatted[]").read(context);
            List<NameValuePair> params = URLEncodedUtils.parse(url, StandardCharsets.UTF_8);
            String[] names = new String[params.size()];
            String[] values = new String[params.size()];
            for (int i = 0; i < params.size(); i++) {
                NameValuePair param = params.get(i);
                names[i] = param.getName();
                values[i] = param.getValue();
            }
            ExternalHTTPAction.writePropertyValues(context.getSession(), findProperty("urlParsed[TEXT]"), names, values);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}