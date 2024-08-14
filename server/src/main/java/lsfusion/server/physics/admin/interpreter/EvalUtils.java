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
import lsfusion.server.logics.classes.user.set.ResolveClassSet;
import lsfusion.server.logics.event.Event;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.property.LazyProperty;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class EvalUtils {
    private static final AtomicLong uniqueNameCounter = new AtomicLong(0);

    private static String getUniqueName() {
        return "UNIQUE" + uniqueNameCounter.incrementAndGet() + "NSNAME";
    }

    public static Pair<LA, EvalScriptingLogicsModule> evaluateAndFindAction(BusinessLogics BL, EvalScriptingLogicsModule parentLM, String script, boolean action) {
        return evaluateAndFindAction(BL, parentLM, null, null, null, null, false, action ? EvalActionParser.parse(script) : script, "run");
    }
    
    private static class WrapResult {
        public int additionalLines;
        public String code;
        
        public WrapResult(String code, int additionalLines) {
            this.code = code;
            this.additionalLines = additionalLines;
        }
    }

    public static Pair<LA, EvalScriptingLogicsModule> evaluateAndFindAction(BusinessLogics BL, EvalScriptingLogicsModule parentLM, String namespace, String require, String priorities, final ImSet<Pair<LP, List<ResolveClassSet>>> locals, boolean prevEventScope, String script, String action) {
        String name = getUniqueName();
        String parentModule = parentLM != null ? parentLM.getName() : null;
        WrapResult wrapResult = wrapScript(BL, parentModule, namespace, require, priorities, script, name);
        
        String code = wrapResult.code;
        EvalScriptingLogicsModule module = new EvalScriptingLogicsModule(BL.LM, BL, parentLM, code);
        module.getErrLog().setLineNumberShift(wrapResult.additionalLines);
        
        module.order = BL.getLogicModules().size() + 1;
        module.visible = FullFunctionSet.instance();
        if(prevEventScope)
            module.setPrevScope(Event.SESSION);
        try { // we need separated runInit, to fail after recovered syntax errors
            module.runInit(ScriptingLogicsModule::initModuleDependencies);
            module.runInit(ScriptingLogicsModule::initMetaAndClasses);
            if(locals != null) {
                for(Pair<LP, List<ResolveClassSet>> local : locals) {
                    module.addWatchLocalDataProperty(local.first, local.second);
                }
            }
            module.runInit(ScriptingLogicsModule::initMainLogic);
            for(LazyProperty property : module.lazyProps) {
                property.finalizeInit(); // needed at least for lazy properties
                property.finalizeLazyInit();
            }
            // finalize forms task (other elements can't be created in script)
            module.markFormsForFinalization();
            for(FormEntity form : module.getAllModuleForms())
                form.finalizeAndPreread();
        } finally {
            if(prevEventScope)
                module.dropPrevScope(Event.SESSION);
        }

        try {
            return new Pair<>(module.findAction(module.getNamespace() + '.' + action), module);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw new UnsupportedOperationException(); // should not be since there is no currentParser in module
        } catch (ScriptErrorException e) {  // we don't need stack for ScriptErrorException, since it is obvious, so will convert it to scriptParsingException
            String message = e.getMessage();
            if(message.endsWith(" is not found")) // hack but for now will do
                message = ThreadLocalContext.localize("{eval.script.form.exception.without.run.block}");
            throw new ScriptParsingException(message);
        }
    }

    private static WrapResult wrapScript(BusinessLogics BL, String parentModule, String namespace, String require, String priorities, String script, String name) {
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
            if(parentModule != null) {
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
