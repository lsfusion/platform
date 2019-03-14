package lsfusion.server.language;

import com.google.common.base.Throwables;
import lsfusion.base.Pair;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.lambda.set.FullFunctionSet;
import lsfusion.server.language.linear.LP;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.classes.sets.ResolveClassSet;
import lsfusion.server.logics.event.Event;
import org.antlr.runtime.RecognitionException;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
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

    public static ScriptingLogicsModule evaluate(BusinessLogics BL, String script, boolean action) throws EvaluationException {
        return evaluate(BL, null, null, null, null, false, action ? EvalActionParser.parse(script) : script);
    }
    
    public static ScriptingLogicsModule evaluate(BusinessLogics BL, String namespace, String require, String priorities, ImSet<Pair<LP, List<ResolveClassSet>>> locals, boolean prevEventScope, String script) throws EvaluationException {
        String name = getUniqueName();

        String code = wrapScript(BL, namespace, require, priorities, script, name);
        ScriptingLogicsModule module = new ScriptingLogicsModule(BL.LM, BL, code);
        module.order = BL.getLogicModules().size() + 1;
        module.visible = FullFunctionSet.instance();
        module.temporary = true;
        if(prevEventScope)
            module.setPrevScope(Event.SESSION);
        String errString = "";
        try {
            module.initModuleDependencies();
            module.initMetaAndClasses();
            
            if(locals != null) {
                for(Pair<LP, List<ResolveClassSet>> local : locals) {
                    module.addWatchLocalDataProperty(local.first, local.second);
                }                
            }

            module.initMainLogic();

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

    private static String wrapScript(BusinessLogics BL, String namespace, String require, String priorities, String script, String name) {
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

    private static class EvalActionParser {
        private enum State {SCRIPT, PARAM, STRING, COMMENT}

        private static String paramPrefix = "nvbxcz";

        public static String parse(String script) {

            if(script != null) {
                if(!script.endsWith(";")) {
                    script += ";";
                }
                List<String> paramsList = new ArrayList<>();
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
                                String param = paramPrefix + currentParam;
                                result.append(param);
                                if(!paramsList.contains(param)) {
                                    paramsList.add(param);
                                }
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

                Collections.sort(paramsList);
                String params = StringUtils.join(paramsList.iterator(), ", ");
                return String.format("run(%s) {%s\n}", params, result);
            } else return null;
        }
    }
}
