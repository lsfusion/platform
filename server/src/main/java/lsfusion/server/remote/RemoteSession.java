package lsfusion.server.remote;

import lsfusion.base.ExternalRequest;
import lsfusion.base.ExternalResponse;
import lsfusion.base.SessionInfo;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.interop.exceptions.AuthenticationException;
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
import lsfusion.server.logics.property.CalcProperty;
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
    
    public RemoteSession(int port, LogicsInstance logicsInstance, String login, SessionInfo sessionInfo, ExecutionStack stack) throws RemoteException, ClassNotFoundException, SQLException, SQLHandledException, InstantiationException, IllegalAccessException {
        super(port, "session", stack);

        initLocalContext(logicsInstance);
        if(login != null || !isLocal()) { // we won't need this context, because we'll call non-remote method (without aspect) abd will work with remoteLogicsContext
            setContext(new RemoteSessionContext(this));
            initContext(logicsInstance, login, sessionInfo, stack);
        }

        dataSession = createSession();
    }

    @Override
    public ExternalResponse exec(String action, ExternalRequest request) {
        return exec(false, action, request, getStack());
    }

    public ExternalResponse exec(boolean anonymous, String action, ExternalRequest request, ExecutionStack stack) {
        ExternalResponse result;
        try {
            LAP property = businessLogics.findActionByCompoundName(action);
            if (property != null) {
                result = executeExternal(anonymous, property, request, stack);
            } else {
                throw new RuntimeException(String.format("Action %s was not found", action));
            }
        } catch (ParseException | SQLHandledException | SQLException | IOException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    @Override
    public ExternalResponse eval(boolean action, Object paramScript, ExternalRequest returnNames) {
        return eval(false, action, paramScript, returnNames, getStack());
    }

    public ExternalResponse eval(boolean anonymous, boolean action, Object paramScript, ExternalRequest request, ExecutionStack stack) {
        ExternalResponse result;
        if (paramScript != null) {
            try {
                Charset charset = Charset.forName(request.charsetName);
                String script = StringClass.text.parseHTTP(paramScript, charset);
                if (action) {
                    //оборачиваем в run без параметров
                    script = "run() {" + script + "\n}";
                }
                LAP<?> runAction = businessLogics.evaluateRun(script);
                if(runAction != null) {
                    result = executeExternal(anonymous, runAction, request, stack);
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

    private ExternalResponse executeExternal(boolean anonymous, LAP<?> property, ExternalRequest request, ExecutionStack stack) throws SQLException, ParseException, SQLHandledException, IOException {
        String annotation = property.property.annotation;
        if(annotation == null || !annotation.equals("noauth"))
            checkEnableApi(anonymous);

        if(property.property.uses(businessLogics.LM.headers.property)) // optimization
            ExternalHTTPActionProperty.writeHeaders(dataSession, businessLogics.LM.headers, request.headerNames, request.headerValues);

        property.execute(dataSession, stack, ExternalHTTPActionProperty.getParams(dataSession, property, request.params, Charset.forName(request.charsetName)));

        return readResult(request.returnNames);
    }

    public static void checkEnableApi(boolean anonymous) {
        byte enableApi = Settings.get().getEnableApi();
        if(enableApi == 0)
            throw new RuntimeException("REST Api is disabled. It can be enabled by using setting enableApi.");

        if(anonymous && enableApi == 1)
            throw new AuthenticationException();
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
            for (LCP returnProp : returnProps)
                returns.addAll(readReturnProperty(returnProp));
        } else
            returns.add(businessLogics.LM.getExportValueProperty().readFirstNotNull(dataSession).getValue());

        ImOrderMap<String, String> headers = ExternalHTTPActionProperty.readHeaders(dataSession, businessLogics.LM.headersTo).toOrderMap();
        return new ExternalResponse(returns.toArray(), headers.keyOrderSet().toArray(new String[headers.size()]), headers.valuesList().toArray(new String[headers.size()]));
    }

    private List<Object> readReturnProperty(LCP<?> returnProperty, ObjectValue... params) throws SQLException, SQLHandledException, IOException {
        Object returnValue = returnProperty.read(dataSession, params);
        Type returnType = returnProperty.property.getType();
        return readReturnProperty(returnValue, returnType);
    }

    private List<Object> readReturnProperty(Object returnValue, Type returnType) throws IOException {
        List<Object> returnList = new ArrayList<>();
//        boolean jdbcSingleRow = false;
//        if (returnType instanceof DynamicFormatFileClass && returnValue != null) {
//            if (((FileData) returnValue).getExtension().equals("jdbc")) {
//                JDBCTable jdbcTable = JDBCTable.deserializeJDBC(((FileData) returnValue).getRawFile());
//                if (jdbcTable.singleRow) {
//                    ImMap<String, Object> row = jdbcTable.set.isEmpty() ? null : jdbcTable.set.get(0);
//                    for (String field : jdbcTable.fields) {
//                        Type fieldType = jdbcTable.fieldTypes.get(field);
//                        if(row == null)
//                            returnList.add(null);
//                        else
//                            returnList.addAll(readReturnProperty(row.get(field), fieldType));
//                    }
//                    jdbcSingleRow = true;
//                }
//            }
//        }
//        if (!jdbcSingleRow)
        returnList.add(returnType.formatHTTP(returnValue, null));
        return returnList;
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
