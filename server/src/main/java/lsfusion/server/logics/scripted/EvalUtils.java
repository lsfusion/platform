package lsfusion.server.logics.scripted;

import com.google.common.base.Throwables;
import lsfusion.base.FullFunctionSet;
import lsfusion.base.Pair;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.classes.sets.ResolveClassSet;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.linear.LP;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.property.Event;
import org.antlr.runtime.RecognitionException;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class EvalUtils {
    public static class EvaluationException extends Exception {
        public EvaluationException(String errString) {
            super(errString);
        }
    }

    private static final AtomicLong uniqueNameCounter = new AtomicLong(0);

    private static String getUniqueName() {
        return "UNIQUE" + uniqueNameCounter.incrementAndGet() + "NSNAME";
    }

    public static ScriptingLogicsModule evaluate(BusinessLogics BL, String script) throws EvaluationException {
        return evaluate(BL, null, null, null, null, false, script);
    }
    
    public static ScriptingLogicsModule evaluate(BusinessLogics BL, String namespace, String require, String priorities, ImSet<Pair<LP, List<ResolveClassSet>>> locals, boolean prevEventScope, String script) throws EvaluationException {
        String name = getUniqueName();

        ScriptingLogicsModule module = new ScriptingLogicsModule(BL.LM, BL, wrapScript(BL, namespace, require, priorities, script, name));
        module.order = BL.getOrderedModules().size() + 1;
        module.visible = FullFunctionSet.<Version>instance();
        if(prevEventScope)
            module.setPrevScope(Event.SESSION);
        String errString = "";
        try {
            module.initModuleDependencies();
            module.initModule();
            module.initAliases();
            
            if(locals != null) {
                for(Pair<LP, List<ResolveClassSet>> local : locals) {
                    module.addWatchLocalDataProperty(local.first, local.second);
                }                
            }
            
            module.initProperties();

            errString = module.getErrorsDescription();
        } catch (RecognitionException e) {
            errString = module.getErrorsDescription() + e.getMessage();
        } catch (Exception e) {
            if (!module.getErrorsDescription().isEmpty()) {
                errString = module.getErrorsDescription() + e.getMessage();
            } else {
                throw Throwables.propagate(e);
            }
        } finally {
            if(prevEventScope)
                module.dropPrevScope(Event.SESSION);
        }

        if (!errString.isEmpty()) {
            throw new EvaluationException(errString);
        }
        
        return module;
    }

    private static String wrapScript(BusinessLogics<?> BL, String namespace, String require, String priorities, String script, String name) {
        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append("MODULE ");
        strBuilder.append(name);
        strBuilder.append(";\n");
        strBuilder.append("REQUIRE ");
        
        if (require != null) {
            strBuilder.append(require);
        } else {
            boolean isFirst = true;
            for (LogicsModule module : BL.getLogicModules()) {
                if (!isFirst) {
                    strBuilder.append(", ");
                }
                isFirst = false;
                strBuilder.append(module.getName());
            }
        }
        strBuilder.append(";\n");

        if(priorities != null) {
            strBuilder.append("PRIORITY ");
            strBuilder.append(priorities);
            strBuilder.append(";\n");
        }

        if(namespace != null) {
            strBuilder.append("NAMESPACE ");
            strBuilder.append(namespace);
            strBuilder.append(";\n");
        }

        strBuilder.append(script);
        return strBuilder.toString();
    }
}
