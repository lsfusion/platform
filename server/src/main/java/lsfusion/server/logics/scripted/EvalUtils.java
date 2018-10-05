package lsfusion.server.logics.scripted;

import com.google.common.base.Throwables;
import lsfusion.base.FullFunctionSet;
import lsfusion.base.Pair;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.classes.sets.ResolveClassSet;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.linear.LCP;
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

    public static ScriptingLogicsModule evaluate(BusinessLogics BL, String script, boolean action) throws EvaluationException {
        return evaluate(BL, null, null, null, null, false, action ? EvalActionParser.parse(script) : script);
    }
    
    public static ScriptingLogicsModule evaluate(BusinessLogics BL, String namespace, String require, String priorities, ImSet<Pair<LCP, List<ResolveClassSet>>> locals, boolean prevEventScope, String script) throws EvaluationException {
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
            module.initModule();
            module.initAliases();
            
            if(locals != null) {
                for(Pair<LCP, List<ResolveClassSet>> local : locals) {
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
                return String.format("run(%s) {%s\n};", params, result);
            } else return null;
        }
    }
}
