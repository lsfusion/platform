package lsfusion.server.remote;

import lsfusion.base.ExternalRequest;
import lsfusion.base.ExternalResponse;
import lsfusion.base.Result;
import lsfusion.base.SessionInfo;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.interop.exceptions.AuthenticationException;
import lsfusion.interop.remote.AuthenticationToken;
import lsfusion.interop.session.RemoteSessionInterface;
import lsfusion.server.ServerLoggers;
import lsfusion.server.Settings;
import lsfusion.server.classes.StringClass;
import lsfusion.server.context.ExecutionStack;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.type.ParseException;
import lsfusion.server.data.type.Type;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.form.navigator.ChangesController;
import lsfusion.server.form.navigator.FormController;
import lsfusion.server.logics.LogicsInstance;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.linear.LAP;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.ActionProperty;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.SessionDataProperty;
import lsfusion.server.logics.property.actions.external.ExternalHTTPActionProperty;
import lsfusion.server.session.DataSession;

import java.io.IOException;
import java.nio.charset.Charset;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RemoteSession extends RemoteConnection implements RemoteSessionInterface {
    
    private DataSession dataSession;
    
    public RemoteSession(int port, LogicsInstance logicsInstance, AuthenticationToken token, SessionInfo sessionInfo, ExecutionStack stack) throws RemoteException, ClassNotFoundException, SQLException, SQLHandledException, InstantiationException, IllegalAccessException {
        super(port, "session", stack);

        setContext(new RemoteSessionContext(this));
        initContext(logicsInstance, token, sessionInfo, stack);

        dataSession = createSession();
    }

    @Override
    public ExternalResponse exec(String action, ExternalRequest request) {
        ExternalResponse result;
        try {
            LAP property = businessLogics.findActionByCompoundName(action);
            if (property != null) {
                result = executeExternal(property, request);
            } else {
                throw new RuntimeException(String.format("Action %s was not found", action));
            }
        } catch (ParseException | SQLHandledException | SQLException | IOException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    @Override
    public ExternalResponse eval(boolean action, Object paramScript, ExternalRequest request) {
        ExternalResponse result;
        if (paramScript != null) {
            try {
                Charset charset = Charset.forName(request.charsetName);
                String script = StringClass.text.parseHTTP(paramScript, charset);
                LAP<?> runAction = businessLogics.evaluateRun(script, action);
                if(runAction != null) {
                    result = executeExternal(runAction, request);
                } else {
                    throw new RuntimeException("Action run[] was not found");

                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException("Eval script was not found");
        }
        return result;
    }

    private ExternalResponse executeExternal(LAP<?> property, ExternalRequest request) throws SQLException, ParseException, SQLHandledException, IOException {
        String annotation = property.property.annotation;
        if(annotation == null || !annotation.equals("noauth"))
            checkEnableApi(authToken.isAnonymous());

        writeRequestInfo(dataSession, property.property, request);

        property.execute(dataSession, getStack(), ExternalHTTPActionProperty.getParams(dataSession, property, request.params, Charset.forName(request.charsetName)));

        return readResult(request.returnNames);
    }

    public static void checkEnableApi(boolean anonymous) {
        byte enableApi = Settings.get().getEnableAPI();
        if(enableApi == 0)
            throw new RuntimeException("Api is disabled. It can be enabled by using setting enableAPI.");

        if(anonymous && enableApi == 1)
            throw new AuthenticationException();
    }

    public void writeRequestInfo(DataSession session, ActionProperty<?> actionProperty, ExternalRequest request) throws SQLException, SQLHandledException {
        if (actionProperty.uses(businessLogics.LM.headers.property)) {
            ExternalHTTPActionProperty.writeHeaders(session, businessLogics.LM.headers, request.headerNames, request.headerValues);
        }
        if (request.url != null) {
            businessLogics.LM.url.change(request.url, session);
        }
        if (request.query != null) {
            businessLogics.LM.query.change(request.query, session);
        }
        if (request.host != null) {
            businessLogics.LM.host.change(request.host, session);
        }
        if (request.port != null) {
            businessLogics.LM.port.change(request.port, session);
        }
        if (request.exportName != null) {
            businessLogics.LM.exportName.change(request.exportName, session);
        }
    }

    private ExternalResponse readResult(String[] returnNames) throws SQLException, SQLHandledException, IOException {
        List<Object> returns = new ArrayList<>();

        LCP[] returnProps;
        if (returnNames.length > 0) {
            returnProps = new LCP[returnNames.length];
            for (int i = 0; i < returnNames.length; i++) {
                String returnName = returnNames[i];
                LCP returnProperty = businessLogics.findPropertyByCompoundName(returnName);
                if (returnProperty == null)
                    throw new RuntimeException(String.format("Return property %s was not found", returnName));
                returnProps[i] = returnProperty;
            }
            for (LCP<?> returnProp : returnProps)
                returns.add(formatReturnValue(returnProp.read(dataSession), returnProp.property));
        } else {
            Result<SessionDataProperty> resultProp = new Result<>(); 
            ObjectValue objectValue = businessLogics.LM.getExportValueProperty().readFirstNotNull(dataSession, resultProp);
            returns.add(formatReturnValue(objectValue.getValue(), resultProp.result));
        }

        ImOrderMap<String, String> headers = ExternalHTTPActionProperty.readHeaders(dataSession, businessLogics.LM.headersTo).toOrderMap();
        return new ExternalResponse(returns.toArray(), headers.keyOrderSet().toArray(new String[headers.size()]), headers.valuesList().toArray(new String[headers.size()]));
    }

    private Object formatReturnValue(Object returnValue, CalcProperty returnProperty) {
        Type returnType = returnProperty.getType();
        return returnType.formatHTTP(returnValue, null);
    }

    @Override
    protected FormController createFormController() {
        return new FormController() {
            @Override
            public void changeCurrentForm(String form) {
                throw new RuntimeException("not supported");
            }

            @Override
            public String getCurrentForm() {
                return null;
            }
        };
    }

    @Override
    protected ChangesController createChangesController() {
        return new ChangesController() {
            public void regChange(ImSet<CalcProperty> changes, DataSession session) {
            }

            public ImSet<CalcProperty> update(DataSession session, FormInstance form) {
                return SetFact.EMPTY();
            }

            public void registerForm(FormInstance form) {
            }

            public void unregisterForm(FormInstance form) {
            }
        };
    }

    @Override
    protected Long getConnectionId() {
        return null;
    }

    @Override
    public Object getProfiledObject() {
        return "rs";
    }

    @Override
    protected void onClose() {
        try {
            dataSession.close();
        } catch (Throwable t) {
            ServerLoggers.sqlSuppLog(t);
        }
        
        super.onClose();
    }
    @Override
    public synchronized void close() throws RemoteException { // without this RemoteContextAspect will not be weaved
        super.close();
    }
}
