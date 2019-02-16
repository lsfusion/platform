package lsfusion.server.remote;

import lsfusion.base.BaseUtils;
import lsfusion.base.ExecResult;
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
        if(login != null) { // we won't need this context, because we'll call not-remomte method (without aspect) abd will work with remoteLogicsContext
            setContext(new RemoteSessionContext(this));
            initContext(logicsInstance, login, sessionInfo, stack);
        } else
            assert isLocal();

        dataSession = createSession();
    }

    @Override
    public ExecResult exec(String action, String[] returnNames, Object[] params, String charsetName, String[] headerNames, String[] headerValues) {
        return exec(false, action, returnNames, params, charsetName, headerNames, headerValues, getStack());
    }

    public ExecResult exec(boolean anonymous, String action, String[] returnNames, Object[] params, String charsetName, String[] headerNames, String[] headerValues, ExecutionStack stack) {
        ExecResult result;
        try {
            LAP property = businessLogics.findActionByCompoundName(action);
            if (property != null) {
                result = executeExternal(anonymous, property, returnNames, params, Charset.forName(charsetName), headerNames, headerValues, stack);
            } else {
                throw new RuntimeException(String.format("Action %s was not found", action));
            }
        } catch (ParseException | SQLHandledException | SQLException | IOException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    @Override
    public ExecResult eval(boolean action, Object paramScript, String[] returnNames, Object[] params, String charsetName, String[] headerNames, String[] headerValues) {
        return eval(false, action, paramScript, returnNames, params, charsetName, headerNames, headerValues, getStack());
    }

    public ExecResult eval(boolean anonymous, boolean action, Object paramScript, String[] returnNames, Object[] params, String charsetName, String[] headerNames, String[] headerValues, ExecutionStack stack) {
        ExecResult result;
        if (paramScript != null) {
            try {
                Charset charset = Charset.forName(charsetName);
                String script = StringClass.text.parseHTTP(paramScript, charset);
                if (action) {
                    //оборачиваем в run без параметров
                    script = "run() {" + script + "\n}";
                }
                LAP<?> runAction = businessLogics.evaluateRun(script);
                if(runAction != null) {
                    result = executeExternal(anonymous, runAction, returnNames, params, charset, headerNames, headerValues, stack);
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

    private ExecResult executeExternal(boolean anonymous, LAP<?> property, String[] returnNames, Object[] params, Charset charset, String[] headerNames, String[] headerValues, ExecutionStack stack) throws SQLException, ParseException, SQLHandledException, IOException {
        String annotation = property.property.annotation;
        if(annotation == null || !annotation.equals("noauth")) {
            byte enableApi = Settings.get().getEnableApi();
            if(enableApi == 0)
                throw new RuntimeException("REST Api is disabled. It can be enabled using setting enableRESTApi.");
            
            if(anonymous && enableApi == 1)
                throw new AuthenticationException();
        }

        if(property.property.uses(businessLogics.LM.headers.property)) // optimization
            ExternalHTTPActionProperty.writeHeaders(dataSession, businessLogics.LM.headers, headerNames, headerValues);

        property.execute(dataSession, stack, ExternalHTTPActionProperty.getParams(dataSession, property, params, charset));

        return readResult(returnNames);
    }

    private ExecResult readResult(String[] returnNames) throws SQLException, SQLHandledException, IOException {
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
        } else {
            returnProps = new LCP[] {businessLogics.LM.exportFile};
        }

        List<Object> returns = new ArrayList<>();
        for (LCP returnProp : returnProps)
            returns.addAll(readReturnProperty(returnProp));

        ImOrderMap<String, String> headers = ExternalHTTPActionProperty.readHeaders(dataSession, businessLogics.LM.headersTo).toOrderMap();
        return new ExecResult(returns.toArray(), headers.keyOrderSet().toArray(new String[headers.size()]), headers.valuesList().toArray(new String[headers.size()]));
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
