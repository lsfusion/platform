package lsfusion.server.logics.debug;

import com.google.common.base.Throwables;
import lsfusion.base.*;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MOrderExclSet;
import lsfusion.base.col.interfaces.mutable.SymmAddValue;
import lsfusion.base.col.interfaces.mutable.add.MAddMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetKeyValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.ServerLoggers;
import lsfusion.server.SystemProperties;
import lsfusion.server.caches.IdentityLazy;
import lsfusion.server.classes.LogicalClass;
import lsfusion.server.classes.sets.ResolveClassSet;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.linear.LAP;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.linear.LP;
import lsfusion.server.logics.property.ActionProperty;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.property.actions.flow.FlowResult;
import lsfusion.server.logics.scripted.EvalUtils;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import org.apache.log4j.Logger;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.SQLException;
import java.util.*;

import static java.util.Arrays.asList;
import static lsfusion.server.logics.debug.ActionDelegationType.*;

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

    private final MAddMap<Pair<String, Integer>, ActionDebugInfo> firstInLineDelegates = MapFact.mAddMap(new SymmAddValue<Pair<String, Integer>, ActionDebugInfo>() {
        public ActionDebugInfo addValue(Pair<String, Integer> key, ActionDebugInfo prevValue, ActionDebugInfo newValue) {
            return newValue.offset > prevValue.offset ? prevValue : newValue;
        }
    });
    public boolean isDebugFirstInLine(ActionDebugInfo debugInfo) {
        return BaseUtils.hashEquals(firstInLineDelegates.get(debugInfo.getModuleLine()), debugInfo);
    }

    //в Java есть ограничение на количество имён в файле (~65000), поэтому нельзя всё впихнуть в один файл
    //приходится разбивать - пока просто для каждого модуля - свой класс
    private Map<String, Class> delegatesHolderClasses = new HashMap<String, Class>();

    private ActionPropertyDebugger() {
    } //singleton

    private Set<ActionDebugInfo> delegates = new HashSet<ActionDebugInfo>();
    
    public ImMap<String, ImSet<ActionDebugInfo>> getGroupDelegates() {
        return SetFact.fromJavaSet(delegates).group(new BaseUtils.Group<String, ActionDebugInfo>() {
                @Override
                public String group(ActionDebugInfo key) {
                    return key.moduleName;
                }
            });
    }

    public synchronized <P extends PropertyInterface> void addDelegate(ActionProperty<P> property, ActionDebugInfo debugInfo) {
        property.setDebugInfo(debugInfo);

        delegates.add(debugInfo);

        firstInLineDelegates.add(debugInfo.getModuleLine(), debugInfo);
    }

    public synchronized <P extends PropertyInterface> void addParamInfo(ActionProperty<P> property, Map<String, P> paramsToInterfaces, Map<String, String> paramsToClassFQN) {
        ParamDebugInfo<P> paramInfo = new ParamDebugInfo<P>(MapFact.fromJavaRevMap(paramsToInterfaces), MapFact.fromJavaMap(paramsToClassFQN));

        property.setParamInfo(paramInfo);
    }

    public void compileDelegatesHolders(File sourceDir, ImMap<String, ImSet<ActionDebugInfo>> modules) throws IOException, ClassNotFoundException {
        List<InMemoryJavaFileObject> filesToCompile = new ArrayList<InMemoryJavaFileObject>();

        generateDelegateClasses(modules, filesToCompile);

        compileDelegateClasses(sourceDir.getAbsolutePath(), filesToCompile);

        loadDelegateClasses(modules.keys(), sourceDir);
    }

    private void generateDelegateClasses(ImMap<String, ImSet<ActionDebugInfo>> groupedActions, List<InMemoryJavaFileObject> filesToCompile) {
        for (int i = 0,size = groupedActions.size(); i < size; i++) {
            filesToCompile.add(createJavaFileObject(groupedActions.getKey(i), groupedActions.getValue(i)));
        }
    }

    private InMemoryJavaFileObject createJavaFileObject(String moduleName, ImSet<ActionDebugInfo> infos) {
        String holderClassName = DELEGATES_HOLDER_CLASS_NAME_PREFIX + moduleName;

        String holderFQN = DELEGATES_HOLDER_CLASS_FQN_PREFIX + moduleName;

        String sourceString =
            "package " + DELEGATES_HOLDER_CLASS_PACKAGE + ";\n" +
            "\n" +
            "import lsfusion.server.data.SQLHandledException;\n" +
            "import lsfusion.server.logics.property.ActionProperty;\n" +
            "import lsfusion.server.logics.property.ExecutionContext;\n" +
            "import lsfusion.server.logics.property.actions.flow.FlowResult;\n" +
            "\n" +
            "import java.sql.SQLException;\n" +
            "\n" +
            "public class " + holderClassName + " {\n";

        for (ActionDebugInfo info : infos) {
            String methodName = getMethodName(info);
            String body = (info.delegationType == IN_DELEGATE ? "return action.executeImpl(context);" : "return null;");
            sourceString +=
                "    public static FlowResult " + methodName + "(ActionProperty action, ExecutionContext context) throws SQLException, SQLHandledException {\n" +
                "        " + body + "\n" +
                "    }\n";
        }
        sourceString += "}";

        try {
            return new InMemoryJavaFileObject(holderFQN, sourceString);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    private void compileDelegateClasses(String outputFolder, List<InMemoryJavaFileObject> filesToCompile) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        DiagnosticListener diagnostics = new IgnoreDiagnosticListener();

        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, Locale.ENGLISH, null);

        JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, asList("-g", "-d", outputFolder), null, filesToCompile);
        if (!task.call()) {
            throw new IllegalStateException("Compilation of debugger delegate files failed. ");
        }
    }

    public void loadDelegateClasses(ImSet<String> moduleNames, File sourceDir) throws MalformedURLException, ClassNotFoundException {
        // Load and instantiate compiled class.
        URLClassLoader classLoader = URLClassLoader.newInstance(new URL[]{sourceDir.toURI().toURL()});

        for (String moduleName : moduleNames) {
            Class holderClass = Class.forName(DELEGATES_HOLDER_CLASS_FQN_PREFIX + moduleName, true, classLoader);
            delegatesHolderClasses.put(moduleName, holderClass);
        }
    }

    private String getMethodName(ActionDebugInfo info) {
        return info.getMethodName(isDebugFirstInLine(info));
    }

    public <P extends PropertyInterface> FlowResult delegate(ActionProperty<P> action, ExecutionContext<P> context) throws SQLException, SQLHandledException {
        ActionDebugInfo debugInfo = action.getDebugInfo();

        if (debugInfo == null || !isEnabled()) {
            throw new IllegalStateException("Shouldn't happen: debug isn't enabled");
        }

        Class<?> delegatesHolderClass = delegatesHolderClasses.get(debugInfo.moduleName);
        if (delegatesHolderClass == null)
            return action.executeImpl(context);

        FlowResult result = null;
        if (debugInfo.delegationType == BEFORE_DELEGATE) {
            result = action.executeImpl(context);
        }

        try {
            Method method = delegatesHolderClass.getMethod(getMethodName(debugInfo), ActionProperty.class, ExecutionContext.class);
            FlowResult delegateResult = (FlowResult) commonExecuteDelegate(delegatesHolderClass, method, action, context);
            if (debugInfo.delegationType == IN_DELEGATE) {
                return delegateResult;
            }
        } catch (InvocationTargetException e) {
            throw ExceptionUtils.propagate(e.getCause(), SQLException.class, SQLHandledException.class);
        } catch (Exception e) {
            logger.warn("Error while delegating to ActionPropertyDebugger: ", e);
            //если упало исключение в reflection, то просто вызываем оригинальный execute
            if(debugInfo.delegationType == IN_DELEGATE)
                return action.executeImpl(context);
        }

        if (debugInfo.delegationType == AFTER_DELEGATE) {
            result = action.executeImpl(context);
        }

        return result;


    }
    
    private Object commonExecuteDelegate(Class<?> clazz, Method method, ActionProperty action, ExecutionContext context) throws InvocationTargetException, IllegalAccessException {
        return method.invoke(clazz, action, context);
    }
    
    public static ThreadLocal<Boolean> watchHack = new ThreadLocal<Boolean>();

    @SuppressWarnings("UnusedDeclaration") //this method is used by IDEA plugin
    private Object evalAction(ActionProperty action, ExecutionContext context, String require, String statements)
            throws EvalUtils.EvaluationException, ScriptingErrorLog.SemanticErrorException, SQLException, SQLHandledException {
        return evalAction(context, require, "{" + statements + "}", null);
    }

    @SuppressWarnings("UnusedDeclaration") //this method is used by IDEA plugin
    private Object eval(ActionProperty action, ExecutionContext<?> context, String require, String expression)
        throws EvalUtils.EvaluationException, ScriptingErrorLog.SemanticErrorException, SQLException, SQLHandledException {

//        context.showStack();

        if (!isEnabled()) {
            throw new IllegalStateException("Action debugger isn't enabled!");
        }

        final String valueName = "sfdjdfkljgfk";

        if (!expression.contains("ORDER")) // жестковато конечно пока, но будет работать
            expression = "(" + expression + ")";
        String actionText = "FOR " + valueName + " == " + expression + " DO watch();";
        return evalAction(context, require, actionText, valueName);
    }

    private Object evalAction(ExecutionContext<?> context, String require, String expression, final String valueName) throws EvalUtils.EvaluationException, ScriptingErrorLog.SemanticErrorException, SQLException, SQLHandledException {
        //используем все доступные в контексте параметры
        ImOrderMap<String, String> paramsWithClasses = context.getAllParamsWithClassesInStack().toOrderMap();
        ImMap<String, ObjectValue> paramsWithValues = context.getAllParamsWithValuesInStack();

        ImSet<Pair<LP, List<ResolveClassSet>>> locals = context.getAllLocalsInStack();

        Pair<LAP<PropertyInterface>, Boolean> evalResult = evalAction(require, expression, paramsWithClasses, locals, context.getBL());
        LAP<PropertyInterface> evalAction = evalResult.first;
        boolean forExHack = evalResult.second;

        ExecutionContext<PropertyInterface> watchContext = new ExecutionContext<PropertyInterface>(MapFact.<PropertyInterface, ObjectValue>EMPTY(), context.getEnv());
        final MOrderExclSet<ImMap<String, ObjectValue>> mResult = SetFact.mOrderExclSet();
        final ImSet<String> externalParamNames = paramsWithClasses.keys();
        watchContext.setWatcher(new Processor<ImMap<String, ObjectValue>>() {
            public void proceed(ImMap<String, ObjectValue> value) {
                mResult.exclAdd(value.remove(externalParamNames));
            }
        });

        ObjectValue[] orderedValues = paramsWithClasses.keyOrderSet().mapOrderMap(paramsWithValues).valuesList().toArray(new ObjectValue[paramsWithClasses.size()]);
        evalAction.execute(watchContext, orderedValues);

        ImOrderSet<ImMap<String, ObjectValue>> result = mResult.immutableOrder();
        if(result.size() == 0) {
            if(forExHack)
                return new ArrayList();
            else
                return null;
        }
        if(result.size() == 1) {
            ImMap<String, ObjectValue> value = result.single();
            if(value.size() == 1)
                return value.singleValue();
        }
        return result.mapOrderSetValues(new GetValue<ActionWatchEntry, ImMap<String,ObjectValue>>() {
            public ActionWatchEntry getMapValue(ImMap<String, ObjectValue> value) {
                return getWatchEntry(value, valueName);
            }
        }).toJavaList();
    }

    @IdentityLazy
    private Pair<LAP<PropertyInterface>, Boolean> evalAction(String require, String action, ImOrderMap<String, String> paramWithClasses, ImSet<Pair<LP, List<ResolveClassSet>>> locals, BusinessLogics bl) throws EvalUtils.EvaluationException, ScriptingErrorLog.SemanticErrorException {
        
        String paramString = "";
        for (int i = 0, size = paramWithClasses.size(); i < size; i++) {
            String param = paramWithClasses.getKey(i);
            String clazz = paramWithClasses.getValue(i);
            
            if (!paramString.isEmpty()) {
                paramString += ", "; 
            }
            
            if (clazz != null) {
                paramString += clazz + " ";
            }

            paramString += param;
        }

        String script = "evalStub(" + paramString + ") = ACTION " + action;

        watchHack.set(false);

        ScriptingLogicsModule module = EvalUtils.evaluate(bl, require, locals, script);

        boolean forExHack = watchHack.get();
        watchHack.set(null);

        String evalPropName = module.getName() + "." + "evalStub";

        return new Pair<LAP<PropertyInterface>, Boolean>((LAP<PropertyInterface>) module.findAction(evalPropName), forExHack);
    }

    private static ActionWatchEntry getWatchEntry(ImMap<String, ObjectValue> row, String valueName) {
        ObjectValue value = null;
        if(valueName !=  null) {
            value = row.get(valueName);
            if (value instanceof DataObject && ((DataObject) value).objectClass instanceof LogicalClass) {
                value = null;
            }
            row = row.remove(valueName);
        }
        return new ActionWatchEntry(row.toOrderMap().mapOrderSetValues(new GetKeyValue<ActionWatchEntry.Param, String, ObjectValue>() {
            public ActionWatchEntry.Param getMapValue(String key, ObjectValue value) {
                return new ActionWatchEntry.Param(key, value);
            }
        }).toJavaList(), value);
    }

    public static class InMemoryJavaFileObject extends SimpleJavaFileObject {
        private String contents = null;

        public InMemoryJavaFileObject(String className, String contents) throws Exception {
            super(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.contents = contents;
        }

        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return contents;
        }
    }

    private static class IgnoreDiagnosticListener implements DiagnosticListener {
        @Override
        public void report(Diagnostic diagnostic) {
            // ignore
        }
    }
}
