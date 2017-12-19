package lsfusion.gwt.base.server.spring;

import org.springframework.beans.factory.annotation.Autowired;

import java.rmi.RemoteException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EvalRequestHandler extends ExternalRequestHandler {
    private static final String SCRIPT_PARAM = "script";

    @Autowired
    private BusinessLogicsProvider blProvider;

    @Override
    public List<Object> processRequest(String script, String[] returns, List<Object> params) throws RemoteException {
        if(script == null && !params.isEmpty()) {
            //Первый параметр считаем скриптом
            script = formatParam(params.get(0));
            params = params.subList(1, params.size());
        }
        if(script != null) {
            //оборачиваем в run без параметров
            Pattern p = Pattern.compile("run\\((.*)\\)\\s*?=\\s*?\\{.*\\}");
            Matcher m = p.matcher(script);
            if (!m.matches())
                script = "run() = {" + script + ";\n};";
        }
        return blProvider.getLogics().eval(script, returns, params.toArray());
    }

    @Override
    public String getPropertyParam() {
        return SCRIPT_PARAM;
    }

    private String formatParam(Object param) {
        if (param instanceof byte[])
            param = new String((byte[]) param);
        if (param instanceof String)
            return ((String) param).isEmpty() ? null : (String) param;
        else return null;
    }
}
