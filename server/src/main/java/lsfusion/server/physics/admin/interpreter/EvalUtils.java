package lsfusion.server.physics.admin.interpreter;

import lsfusion.base.Pair;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.lambda.set.FullFunctionSet;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.language.*;
import lsfusion.server.language.action.LA;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.event.Event;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.property.LazyProperty;
import lsfusion.server.physics.dev.id.resolve.ExternalLAPResolver;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class EvalUtils {
    private static final AtomicLong uniqueNameCounter = new AtomicLong(0);

    private static String getUniqueName() {
        return "UNIQUE" + uniqueNameCounter.incrementAndGet() + "NSNAME";
    }

    // the module whose names are seen here besides the logics' : of the code that customized the form, or - with none -
    // of the code that customizes the application for the user (the session's, UserController.getCustomizeLM). The former
    // is itself compiled under the latter (FormEntity.getForm), so the nearest one sees what both declare
    public static EvalScriptingLogicsModule getCustomizeLM(FormEntity form, EvalScriptingLogicsModule userLM) {
        EvalScriptingLogicsModule formLM = form != null ? form.getCustomizeLM() : null;
        return formLM != null ? formLM : userLM;
    }

    // what a name that comes from outside the code is looked for in : that module, or - with none - the logics
    public static ExternalLAPResolver getExternalLAPResolver(BusinessLogics BL, EvalScriptingLogicsModule customizeLM) {
        return customizeLM != null ? customizeLM : BL;
    }

    // that module as the parents of a script evaluated with it (an EVAL inside a script adds the module of that script)
    public static Set<EvalScriptingLogicsModule> getParentLMs(EvalScriptingLogicsModule customizeLM) {
        Set<EvalScriptingLogicsModule> parentLMs = new HashSet<>();
        if (customizeLM != null)
            parentLMs.add(customizeLM);
        return parentLMs;
    }

    public static Pair<LA, EvalScriptingLogicsModule> evaluateAndFindAction(BusinessLogics BL, Set<EvalScriptingLogicsModule> parentLMs, String script, boolean action) {
        if(script != null && script.startsWith("\uFEFF"))
            script = script.substring(1);
        EvalScriptingLogicsModule module = BL.LM.evaluate(action ? EvalActionParser.parse(script) : script, parentLMs, false);
        return new Pair<>(findAction(module, "run"), module);
    }
    
    private static class WrapResult {
        public int additionalLines;
        public String code;
        
        public WrapResult(String code, int additionalLines) {
            this.code = code;
            this.additionalLines = additionalLines;
        }
    }

    public static EvalScriptingLogicsModule evaluate(BusinessLogics BL, Set<EvalScriptingLogicsModule> parentLMs, String namespace, String require, String priorities, final ImSet<Pair<LP, LogicsModule.LocalPropertyData>> locals, boolean prevEventScope, String script, boolean changesNavigator) {
        String name = getUniqueName();
        Set<String> parentModules = new HashSet<>();
        if(parentLMs != null)
            parentLMs.forEach(parentLM -> parentModules.add(parentLM.getName()));
        WrapResult wrapResult = wrapScript(BL, parentModules, namespace, require, priorities, script, name);
        
        String code = wrapResult.code;
        EvalScriptingLogicsModule module = new EvalScriptingLogicsModule(BL.LM, BL, parentLMs, code, changesNavigator);
        module.getErrLog().setLineNumberShift(wrapResult.additionalLines);
        // the module gets its name from its header, so an error before the header is parsed would have no module to point to
        module.getErrLog().setModuleId(name);
        
        module.order = BL.getLogicModules().size() + 1;
        module.visible = FullFunctionSet.instance();
        if(prevEventScope)
            module.setPrevScope(Event.SESSION);
        try { // we need separated runInit, to fail after recovered syntax errors
            module.runInit(ScriptingLogicsModule::initModuleDependencies);
            module.runInit(ScriptingLogicsModule::initMetaAndClasses);
            if(locals != null) {
                for(Pair<LP, LogicsModule.LocalPropertyData> local : locals) {
                    module.addWatchLocalDataProperty(local.first, local.second);
                }
            }
            module.runInit(ScriptingLogicsModule::initMainLogic);
            for(LazyProperty property : module.lazyProps) {
                property.finalizeInit(); // needed at least for lazy properties
                property.finalizeLazyInit();
            }
            // finalize forms task (other elements can't be created in script, but the navigator's, in a copy of the navigator)
            module.markFormsForFinalization();
            for(FormEntity form : module.getAllModuleForms())
                form.finalizeAndPreread();
            module.finalizeAndCheckNavigator();
            // an evaluated script is the only thing that could add a cyclic property dependency after checkRecursionsTask has already run, and then
            // getRecDepends would recurse until the stack ends instead of naming anything. it can't today : events are rejected here, and an abstract
            // is unusable in an eval module (its cases are never finalized, so reading them fails) - which is why there is no check around this.
            // when that abstract limitation is lifted, bring one back : catch StackOverflowError here and run checkRecursions over the module's own
            // properties (BL.getProperties() will not do, an evaluated module is not added to the business logics) - it throws naming the cycle,
            // and if it finds none the overflow was about something else and has to be rethrown as it is
        } finally {
            if(prevEventScope)
                module.dropPrevScope(Event.SESSION);
        }
        return module;
    }

    public static LA findAction(EvalScriptingLogicsModule module, String action) {
        try {
            return module.findAction(module.getNamespace() + '.' + action);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw new UnsupportedOperationException(); // should not be since there is no currentParser in module
        } catch (ScriptErrorException e) {  // we don't need stack for ScriptErrorException, since it is obvious, so will convert it to scriptParsingException
            String message = e.getMessage();
            if(message.endsWith(" is not found")) // hack but for now will do
                message = ThreadLocalContext.localize("{eval.script.form.exception.without.run.block}");
            throw new ScriptParsingException(message);
        }
    }

    private static WrapResult wrapScript(BusinessLogics BL, Set<String> parentModules, String namespace, String require, String priorities, String script, String name) {
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
            for(String parentModule : parentModules) {
                strBuilder.append(isFirst ? "" : ", ").append(parentModule);
            }
        }
        strBuilder.append(";\n");
        int additionalLines = 2;
        
        if(priorities != null) {
            strBuilder.append("PRIORITY ");
            strBuilder.append(priorities);
            strBuilder.append(";\n");
            ++additionalLines;
        }

        if(namespace != null) {
            strBuilder.append("NAMESPACE ");
            strBuilder.append(namespace);
            strBuilder.append(";\n");
            ++additionalLines;
        }

        strBuilder.append(script);
        return new WrapResult(strBuilder.toString(), additionalLines);
    }

    private static class EvalActionParser {
        private enum State {SCRIPT, PARAM, STRING, COMMENT}

        private static String paramPrefix = "nvbxcz";

        public static String parse(String script) {

            if(script != null) {
                if(!script.endsWith(";")) {
                    script += ";";
                }
                int maxParam = 0;
                StringBuilder result = new StringBuilder();
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
                                result.append(paramPrefix).append(currentParam);
                                int paramIndex = Integer.parseInt(currentParam.toString());
                                if(paramIndex > maxParam)
                                    maxParam = paramIndex;
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

                String params = ListFact.consecutiveList(maxParam).toString(index -> paramPrefix + index, ", ");
                return String.format("run(%s) {%s\n};", params, result);
            } else return null;
        }
    }
}
