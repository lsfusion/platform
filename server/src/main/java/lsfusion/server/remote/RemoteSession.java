package lsfusion.server.remote;

import com.google.common.base.Throwables;
import lsfusion.interop.session.ExternalRequest;
import lsfusion.interop.session.ExternalResponse;
import lsfusion.base.Result;
import lsfusion.interop.session.SessionInfo;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.interop.exception.AuthenticationException;
import lsfusion.interop.connection.AuthenticationToken;
import lsfusion.interop.session.RemoteSessionInterface;
import lsfusion.server.ServerLoggers;
import lsfusion.server.Settings;
import lsfusion.server.language.linear.LA;
import lsfusion.server.language.linear.LP;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.classes.StringClass;
import lsfusion.server.base.context.ExecutionStack;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.type.ParseException;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.navigator.ChangesController;
import lsfusion.server.logics.navigator.FormController;
import lsfusion.server.logics.LogicsInstance;
import lsfusion.server.data.ObjectValue;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.data.SessionDataProperty;
import lsfusion.server.physics.dev.integration.external.to.ExternalHTTPActionProperty;
import lsfusion.server.language.EvalUtils;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.logics.action.session.DataSession;

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
            if(action != null) {
                LA property = businessLogics.findActionByCompoundName(action);
                if (property != null) {
                    result = executeExternal(property, request);
                } else {
                    throw new RuntimeException(String.format("Action %s was not found", action));
                }
            } else {
                throw new RuntimeException("Action was not specified");
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
                LA<?> runAction = businessLogics.evaluateRun(script, action);
                if(runAction != null) {
                    result = executeExternal(runAction, request);
                } else {
                    throw new RuntimeException("Action with name 'run' was not found");

                }
            } catch (SQLException | ParseException | SQLHandledException | IOException | EvalUtils.EvaluationException | ScriptingErrorLog.SemanticErrorException e) {
                throw Throwables.propagate(e);
            }
        } else {
            throw new RuntimeException("Eval script was not found");
        }
        return result;
    }

    private ExternalResponse executeExternal(LA<?> property, ExternalRequest request) throws SQLException, ParseException, SQLHandledException, IOException {
        checkEnableApi(property);

        writeRequestInfo(dataSession, property.property, request);

        property.execute(dataSession, getStack(), ExternalHTTPActionProperty.getParams(dataSession, property, request.params, Charset.forName(request.charsetName)));

        return readResult(request.returnNames, property.property);
    }

    private void checkEnableApi(LA<?> property) {
        boolean forceAPI = false;
        String annotation = property.property.annotation;
        if(annotation != null) {
            if(annotation.equals("noauth"))
                return;
            forceAPI = annotation.equals("api");
        }
        checkEnableApi(authToken.isAnonymous(), forceAPI);
    }
    
    private static void checkEnableApi(boolean anonymous, boolean forceAPI) {
        byte enableApi = Settings.get().getEnableAPI();
        if(enableApi == 0) {
            if(forceAPI)
                enableApi = 1;
            else
                throw new RuntimeException("Api is disabled. It can be enabled by using setting enableAPI.");
        }

        if(anonymous && enableApi == 1)
            throw new AuthenticationException();        
    }
    public static void checkEnableApi(boolean anonymous) {
        checkEnableApi(anonymous, false);
    }

    public void writeRequestInfo(DataSession session, Action<?> actionProperty, ExternalRequest request) throws SQLException, SQLHandledException {
        if (actionProperty.uses(businessLogics.LM.headers.property)) {
            ExternalHTTPActionProperty.writePropertyValues(session, businessLogics.LM.headers, request.headerNames, request.headerValues);
        }
        if (actionProperty.uses(businessLogics.LM.cookies.property)) {
            ExternalHTTPActionProperty.writePropertyValues(session, businessLogics.LM.cookies, request.headerNames, request.headerValues);
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

    private ExternalResponse readResult(String[] returnNames, Action<?> property) throws SQLException, SQLHandledException, IOException {
        List<Object> returns = new ArrayList<>();

        LP[] returnProps;
        if (returnNames.length > 0) {
            returnProps = new LP[returnNames.length];
            for (int i = 0; i < returnNames.length; i++) {
                String returnName = returnNames[i];
                LP returnProperty = businessLogics.findPropertyByCompoundName(returnName);
                if (returnProperty == null)
                    throw new RuntimeException(String.format("Return property %s was not found", returnName));
                returnProps[i] = returnProperty;
            }
            for (LP<?> returnProp : returnProps)
                returns.add(formatReturnValue(returnProp.read(dataSession), returnProp.property));
        } else {
            Result<SessionDataProperty> resultProp = new Result<>(); 
            ObjectValue objectValue = businessLogics.LM.getExportValueProperty().readFirstNotNull(dataSession, resultProp, property);
            returns.add(formatReturnValue(objectValue.getValue(), resultProp.result));
        }

        ImOrderMap<String, String> headers = ExternalHTTPActionProperty.readPropertyValues(dataSession, businessLogics.LM.headersTo).toOrderMap();
        String[] headerNames = headers.keyOrderSet().toArray(new String[headers.size()]);
        String[] headerValues = headers.valuesList().toArray(new String[headers.size()]);
        ImOrderMap<String, String> cookies = ExternalHTTPActionProperty.readPropertyValues(dataSession, businessLogics.LM.cookiesTo).toOrderMap();
        String[] cookieNames = cookies.keyOrderSet().toArray(new String[cookies.size()]);
        String[] cookieValues = cookies.valuesList().toArray(new String[cookies.size()]);
        return new ExternalResponse(returns.toArray(), headerNames, headerValues, cookieNames, cookieValues);
    }

    private Object formatReturnValue(Object returnValue, Property returnProperty) {
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
            public void regChange(ImSet<Property> changes, DataSession session) {
            }

            public ImSet<Property> update(DataSession session, FormInstance form) {
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
