package lsfusion.server.physics.dev.integration.external.to;

import lsfusion.base.Result;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.interop.session.ExternalHttpMethod;
import lsfusion.interop.session.ExternalUtils;
import lsfusion.server.data.type.Type;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public class ExternalLSFAction extends CallHTTPAction {

    private boolean eval;
    private boolean action;

    // LSF constructor, assumes that the first param is the script
    public ExternalLSFAction(ImList<Type> paramTypes, ImList<LP> targetPropList, boolean eval, boolean action) {
        super(false, ExternalHttpMethod.POST, ListFact.add(StringClass.text, paramTypes), targetPropList, 0, ListFact.EMPTY(), null, null, null, null, false,false);

        this.eval = eval;
        this.action = action;
    }

    @Override
    protected UrlProcessor createUrlProcessor(String connectionString, boolean noExec) {
        StringBuilder query = new StringBuilder();

        Result<Object> script = new Result<>();
        Object[] params = new Object[paramInterfaces.size() - 1];
        return new UrlProcessor() {
            @Override
            public boolean proceed(int number, Object value, String encodedValue) {
                String paramName;
                if (number == 0) {
                    paramName = eval ? ExternalUtils.SCRIPT_PARAM : ExternalUtils.ACTION_CN_PARAM;
                    script.set(value);
                } else {
                    paramName = ExternalUtils.PARAMS_PARAM;
                    params[number - 1] = value;
                }
                if(query.length() != 0)
                    query.append("&");
                query.append(paramName).append("=").append(encodedValue);

                return true;
            }

            @Override
            public String finish(ExecutionContext<PropertyInterface> context) {
                return connectionString + "/" + (eval ? (action ? "eval/action" : "eval") : "exec") + "?" + query +
                        (noExec ? "&" + ExternalUtils.SIGNATURE_PARAM + "=" + context.getSecurityManager().signData(ExternalUtils.generate(script.result, eval, params)) : "");
            }
        };
    }
}
