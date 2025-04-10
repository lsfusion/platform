package lsfusion.server.physics.dev.integration.external.to;

import lsfusion.base.Result;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.interop.session.ExternalHttpMethod;
import lsfusion.server.data.type.Type;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public class ExternalHTTPAction extends CallHTTPAction {

    public ExternalHTTPAction(boolean clientAction, ExternalHttpMethod method, ImList<Type> params, ImList<LP> targetPropList,
                              int bodyParamNamesSize, ImList<LP> bodyParamHeadersPropertyList, LP headersProperty, LP cookiesProperty,
                              LP headersToProperty, LP cookiesToProperty, boolean noEncode, boolean hasBodyUrl) {
        super(clientAction, method, params, targetPropList, bodyParamNamesSize, bodyParamHeadersPropertyList, headersProperty, cookiesProperty, headersToProperty, cookiesToProperty,
                noEncode, hasBodyUrl);
    }

    @Override
    protected Long getDefaultTimeout() {
        return 1800000L; //30 minutes
    }

    @Override
    protected UrlProcessor createUrlProcessor(String connectionString, boolean noExec) {
        Result<String> rConnectionString = new Result<>(connectionString);
        return new CallHTTPAction.UrlProcessor() {
            @Override
            public boolean proceed(int i, Object replacement, String encodedValue) {
                String prmName = getParamName(String.valueOf(i + 1));
                if(!connectionString.contains(prmName))
                    return false;

                rConnectionString.set(rConnectionString.result.replace(prmName, encodedValue));
                return true;
            }

            @Override
            public String finish(ExecutionContext<PropertyInterface> context) {
                return rConnectionString.result;
            }
        };
    }
}
