package lsfusion.server.logics.scripted;

import com.google.common.base.Throwables;
import lsfusion.base.ExceptionUtils;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.debug.ActionDelegationType;
import lsfusion.server.logics.debug.WatchActionProperty;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.linear.LAP;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ClassType;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.property.actions.SystemExplicitActionProperty;
import lsfusion.server.logics.property.actions.flow.ChangeFlowType;
import lsfusion.server.stack.ExecutionStackAspect;
import org.antlr.runtime.RecognitionException;

import java.sql.SQLException;
import java.util.List;

public class EvalActionProperty<P extends PropertyInterface> extends SystemExplicitActionProperty {
    private final LCP<P> source;
    private final List<LCP<P>> params;
    private final ImMap<P, ClassPropertyInterface> mapSource;
    private boolean action;

    public EvalActionProperty(LocalizedString caption, LCP<P> source, List<LCP<P>> params, boolean action) {
        super(caption, source.getInterfaceClasses(ClassType.aroundPolicy));
        mapSource = source.listInterfaces.mapSet(getOrderInterfaces());
        this.source = source;
        this.params = params;
        this.action = action;
    }

    private String getScript(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        ImMap<P, ? extends ObjectValue> sourceToData = mapSource.join(context.getKeys());
        String script = (String) source.read(context, source.listInterfaces.mapList(sourceToData).toArray(new ObjectValue[interfaces.size()]));
        return action ? EvalActionParser.parse(script) : script;
    }

    private ObjectValue[] getParams(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        ObjectValue[] result = new ObjectValue[params.size()];
        for(int i = 0; i < params.size(); i++) {
            LCP<P> param = params.get(i);
            ImMap<P, ? extends ObjectValue> paramToData = param.listInterfaces.mapSet(getOrderInterfaces()).join(context.getKeys());
            result[i] = param.readClasses(context, (DataObject[]) param.listInterfaces.mapList(paramToData).toArray(new DataObject[param.listInterfaces.size()]));
        }
        return result;
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        String script = getScript(context);

        try {
            LAP<?> runAction = context.getBL().evaluateRun(script);
            if (runAction != null)
                runAction.execute(context, getParams(context));
        } catch (EvalUtils.EvaluationException | RecognitionException e) {
            throw Throwables.propagate(e);
        }
    }
    @Override
    public boolean hasFlow(ChangeFlowType type) {
        return true;
    }

    @Override
    public ActionDelegationType getDelegationType(boolean modifyContext) {
        return ActionDelegationType.IN_DELEGATE; // execute just like EXEC operator
    }

    private String getMessage(Throwable e) {
        return e.getMessage() == null ? String.valueOf(e) : e.getMessage();
    }

    private static class EvalActionParser {
        private enum State {SCRIPT, PARAM, STRING, COMMENT}

        private static String paramPrefix = "nvbxcz";

        public static String parse(String script) {

            if(script != null) {
                if(!script.endsWith(";")) {
                    script += ";";
                }
                StringBuilder result = new StringBuilder();
                StringBuilder params = new StringBuilder();
                StringBuilder currentParam = new StringBuilder();
                State currentState = State.SCRIPT;
                boolean prevSlash = false;
                boolean prevBackSlash = false;

                int i = 0;
                while (i < script.length()) {
                    char c = script.charAt(i);
                    switch (currentState) {
                        case SCRIPT:
                            if (c == '/' && prevSlash) {
                                //comment starts
                                currentState = State.COMMENT;
                                result.append(c);
                            } else if (c == '\'') {
                                //string literal starts
                                currentState = State.STRING;
                                result.append(c);
                            } else if (c == '$') {
                                //param starts
                                currentState = State.PARAM;
                                currentParam = new StringBuilder();
                                break;
                            } else {
                                result.append(c);
                            }
                            break;
                        case PARAM:
                            if (Character.isDigit(c)) {
                                //param continues
                                currentParam.append(c);
                            } else {
                                //param ends
                                String param = paramPrefix + currentParam;
                                result.append(param);
                                params.append(params.length() > 0 ? ", " : "").append(param);

                                if (c == '/' && prevSlash) {
                                    //comment starts
                                    currentState = State.COMMENT;
                                } else if (c == '\'') {
                                    //string literal starts
                                    currentState = State.STRING;
                                } else {
                                    currentState = State.SCRIPT;
                                }
                                result.append(c);
                            }
                            break;
                        case STRING:
                            if (c == '\'' && !prevBackSlash) {
                                //string literal ends
                                currentState = State.SCRIPT;
                            }
                            result.append(c);
                            break;
                        case COMMENT:
                            if (c == '\n') {
                                //comment ends
                                currentState = State.SCRIPT;
                            }
                            result.append(c);
                            break;
                    }
                    prevSlash = c == '/';
                    prevBackSlash = c == '\\';
                    i++;
                }
                return String.format("run(%s) = {%s\n};", params, result);
            } else return null;
        }
    }

}
