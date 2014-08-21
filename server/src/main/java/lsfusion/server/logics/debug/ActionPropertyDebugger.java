package lsfusion.server.logics.debug;

import lsfusion.base.BaseUtils;
import lsfusion.base.ExceptionUtils;
import lsfusion.base.IOUtils;
import lsfusion.base.NullOutputStream;
import lsfusion.server.ServerLoggers;
import lsfusion.server.SystemProperties;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.ActionProperty;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.property.actions.flow.FlowResult;
import lsfusion.server.logics.scripted.EvalUtils;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import org.apache.log4j.Logger;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ActionPropertyDebugger {
    public static final String DELEGATES_HOLDER_CLASS_PACKAGE = "lsfusion.server.logics.debug";
    public static final String DELEGATES_HOLDER_CLASS_NAME_PREFIX = "DebugDelegatesHolder_";
    public static final String DELEGATES_HOLDER_CLASS_FQN_PREFIX = DELEGATES_HOLDER_CLASS_PACKAGE + "." + DELEGATES_HOLDER_CLASS_NAME_PREFIX;

    private static final Logger logger = ServerLoggers.systemLogger;

    private static final ActionPropertyDebugger instance = new ActionPropertyDebugger();

    public static ActionPropertyDebugger getInstance() {
        return instance;
    }

    public boolean isEnabled() {
//        return true;
        return SystemProperties.isActionDebugEnabled;
    }

    private final Map<ActionDebugInfo, ActionProperty> delegates = new HashMap<ActionDebugInfo, ActionProperty>();

    //в Java есть ограничение на количество имён в файле (~65000), поэтому нельзя всё впихнуть в один файл
    //приходится разбивать - пока просто для каждого модуля - свой класс
    private Map<String, Class> delegatesHolderClasses = new HashMap<String, Class>();

    private ActionPropertyDebugger() {
    } //singleton

    public synchronized void addDelegate(ActionProperty property, Map<String, PropertyInterface> paramsToInterfaces, Map<String, String> paramsToClassFQN,
                                         String moduleName, int line, int offset, boolean delegateExecute) {
        ActionDebugInfo debugInfo = new ActionDebugInfo(paramsToInterfaces, paramsToClassFQN, moduleName, line, offset, delegateExecute);

        property.setDebugInfo(debugInfo);

        delegates.put(debugInfo, property);
    }

    public void compileDelegatesHolders() throws IOException, ClassNotFoundException {
        Map<String, Collection<ActionDebugInfo>> groupedActions = BaseUtils.group(
            new BaseUtils.Group<String, ActionDebugInfo>() {
                @Override
                public String group(ActionDebugInfo key) {
                    return key.moduleName;
                }
            }, delegates.keySet());

        File sourceDir = IOUtils.createTempDirectory("lsfusiondebug");

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        for (Map.Entry<String, Collection<ActionDebugInfo>> e : groupedActions.entrySet()) {
            String moduleName = e.getKey();

            //compiling
            ByteArrayOutputStream errOut = new ByteArrayOutputStream();
            int status = compiler.run(null, new NullOutputStream(), errOut, "-g", createDelegatesHolderFile(sourceDir, moduleName, e.getValue()));
            if (status != 0) {
                throw new IllegalStateException("Compilation of debugger delegate files failed: " + new String(errOut.toByteArray()));
            }
        }

        // Load and instantiate compiled class.
        URLClassLoader classLoader = URLClassLoader.newInstance(new URL[]{sourceDir.toURI().toURL()});
        
        for (String moduleName : groupedActions.keySet()) {
            Class holderClass = Class.forName(DELEGATES_HOLDER_CLASS_FQN_PREFIX + moduleName, true, classLoader);
            delegatesHolderClasses.put(moduleName, holderClass);
        }

        //убираем ненужные ссылки 
        delegates.clear();
    }

    private String createDelegatesHolderFile(File sourceDir, String moduleName, Collection<ActionDebugInfo> infos) throws IOException, ClassNotFoundException {
        String holderClassName = DELEGATES_HOLDER_CLASS_NAME_PREFIX + moduleName;
        
        String holderFQN = DELEGATES_HOLDER_CLASS_FQN_PREFIX + moduleName;
        
        String surceFileName = holderFQN.replace('.', '/') + ".java";
        File sourceFile = new File(sourceDir, surceFileName);
        
        sourceFile.getParentFile().mkdirs();

        PrintStream out = new PrintStream(sourceFile, "UTF-8");

        out.println("package " + DELEGATES_HOLDER_CLASS_PACKAGE + ";\n" +
                    "\n" +
                    "import lsfusion.server.data.SQLHandledException;\n" +
                    "import lsfusion.server.logics.property.ActionProperty;\n" +
                    "import lsfusion.server.logics.property.ExecutionContext;\n" +
                    "import lsfusion.server.logics.property.actions.flow.FlowResult;\n" +
                    "\n" +
                    "import java.sql.SQLException;\n" +
                    "\n" +
                    "public class " + holderClassName + " {\n" +
                    "");

        for (ActionDebugInfo info : infos) {
            String methodName = info.getMethodName();
            String body = (info.delegateExecute ? "return action.executeImpl(context);" : "return null;");
            out.println(
                "    public static FlowResult " + methodName + "(ActionProperty action, ExecutionContext context) throws SQLException, SQLHandledException {\n" +
                "        " + body + "\n" +
                "    }\n"
            );
        }

        out.println("}");
        out.close();
        
        return sourceFile.getAbsolutePath();
    }

    public <P extends PropertyInterface> FlowResult delegate(ActionProperty<P> action, ExecutionContext<P> context) throws SQLException, SQLHandledException {
        ActionDebugInfo debugInfo = action.getDebugInfo();

        if (debugInfo == null || !isEnabled()) {
            throw new IllegalStateException("Shouldn't happen: debug isn't enabled");
        }

        Class<?> delegatesHolderClass = delegatesHolderClasses.get(debugInfo.moduleName);

        try {
            Method method = delegatesHolderClass.getMethod(debugInfo.getMethodName(), ActionProperty.class, ExecutionContext.class);
//            FlowResult result = (FlowResult) method.invoke(delegatesHolderClass, action, context);

            FlowResult result = (FlowResult) resumeBreakpointDelegate(delegatesHolderClass, method, action, context);
            
            return debugInfo.delegateExecute
                    ? result
                    : action.executeImpl(context);

        } catch (InvocationTargetException e) {
            throw ExceptionUtils.propagate(e.getCause(), SQLException.class, SQLHandledException.class);
        } catch (Exception e) {
            logger.warn("Error while delegating to ActionPropertyDebugger: ", e);
            //если упало исключение в reflection, то просто вызываем оригинальный execute
            return action.executeImpl(context);
        }
    }
    
    private Object resumeBreakpointDelegate(Class<?> clazz, Method method, ActionProperty action, ExecutionContext context) throws InvocationTargetException, IllegalAccessException {
        return method.invoke(clazz, action, context);
    }

    @SuppressWarnings("UnusedDeclaration") //this method is used by IDEA plugin
    private Object eval(ActionProperty action, ExecutionContext context, String require, String expression)
        throws EvalUtils.EvaluationException, ScriptingErrorLog.SemanticErrorException, SQLException, SQLHandledException {


        if (!isEnabled()) {
            throw new IllegalStateException("Action debugger isn't enabled!");
        }

        //используем все доступные в контексте параметры
        String[][] paramWithClasses = context.getAllParamsWithClassesInStack();
        String[] params = paramWithClasses[0];
        String[] classes = paramWithClasses[1];
        
        String paramString = "";
        expression = "(" + expression + ")";
        for (int i = 0; i < params.length; i++) {
            String param = params[i];
            String clazz = classes[i];
            
            if (!paramString.isEmpty()) {
                paramString += ", "; 
            }
            
            if (clazz != null) {
                paramString += clazz + " ";
            }

            paramString += param;
            
            //todo: ? непонятно, что делать, если clazz==null, но он должен быть примитивным классом
            expression += " IF " + param + " IS " + (clazz != null ? clazz : "Object");
        }

        String script = "evalStub(" + paramString + ") = " + expression + ";" ;

        ScriptingLogicsModule module = EvalUtils.evaluate(context.getBL(), require, script);

        String evalPropName = module.getName() + "." + "evalStub";

        LCP<?> evalProp = module.findProperty(evalPropName);

        ObjectValue values[] = getParamValuesFromContextStack(context, params);

        return evalProp.read(context.getSession(), values);
    }

    private ObjectValue[] getParamValuesFromContextStack(ExecutionContext context, String[] params) {
        ObjectValue[] values = new ObjectValue[params.length];

        for (int i = 0; i < params.length; i++) {
            String param = params[i];
            ObjectValue value = context.getParamValue(param);
            if (value == null) {
                throw new IllegalStateException("param isn't found in context");
            }
                
            values[i] = value;
        }
        
        return values;
    }
}
