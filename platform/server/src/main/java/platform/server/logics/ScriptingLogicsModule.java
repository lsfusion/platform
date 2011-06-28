package platform.server.logics;

import net.sf.jasperreports.engine.JRException;
import org.antlr.runtime.ANTLRFileStream;
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonTokenStream;
import platform.server.LsfLogicsLexer;
import platform.server.LsfLogicsParser;
import platform.server.classes.CustomClass;
import platform.server.classes.ValueClass;
import platform.server.logics.property.group.AbstractGroup;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User: DAle
 * Date: 03.06.11
 * Time: 14:54
 */

public class ScriptingLogicsModule extends LogicsModule {
    private String scriptName;
    private String code = null;
    private String filename = null;
    private final BusinessLogics<?> BL;

    private final Map<String, List<String>> classes = new HashMap<String, List<String>>();

    private ScriptingLogicsModule(String scriptName, BaseLogicsModule<?> baseModule, BusinessLogics<?> BL) {
        super(scriptName);
        setBaseLogicsModule(baseModule);
        this.scriptName = scriptName;
        this.BL = BL;
    }

    public static ScriptingLogicsModule createFromString(String scriptName, String code, BaseLogicsModule<?> baseModule, BusinessLogics<?> BL) {
        ScriptingLogicsModule module = new ScriptingLogicsModule(scriptName, baseModule, BL);
        module.code = code;
        return module;
    }

    public static ScriptingLogicsModule createFromFile(String scriptName, String filename, BaseLogicsModule<?> baseModule, BusinessLogics<?> BL) {
        ScriptingLogicsModule module = new ScriptingLogicsModule(scriptName, baseModule, BL);
        module.filename = filename;
        return module;
    }

    private CharStream createStream() throws IOException {
        if (code != null) {
            return new ANTLRStringStream(code);
        } else {
            return new ANTLRFileStream(filename);
        }
    }

    protected LogicsModule getModule(String sid) {
        List<LogicsModule> modules = BL.getLogicModules();
        for (LogicsModule module : modules) {
            if (module.getSID().equals(sid)) {
                return module;
            }
        }
        return null;
    }

    private String transformCaptionStr(String captionStr) {
        String caption = captionStr.replace("\'", "'");
        return caption.substring(1, captionStr.length()-1);
    }

    public void addScriptedClass(String className, String captionStr, boolean isAbstract, List<String> parentNames, Set<String> importedModules) {
        String caption = (captionStr == null ? className : transformCaptionStr(captionStr));
        CustomClass[] parents = new CustomClass[parentNames.size()];
        for (int i = 0; i < parentNames.size(); i++) {
            String parentName = parentNames.get(i);
            ValueClass valueClass;
            int dotPosition = parentName.indexOf('.');
            if (dotPosition > 0) {
                LogicsModule module = getModule(parentName.substring(0, dotPosition));
                valueClass = module.getClass(module.transformNameToSID(parentName.substring(dotPosition + 1)));
            } else {
                valueClass = getClass(transformNameToSID(parentName));
                if (valueClass == null) {
                    for (String importModuleName : importedModules) {
                        LogicsModule module = getModule(importModuleName);
                        if ((valueClass = module.getClass(module.transformNameToSID(parentName))) != null) {
                            break;
                        }
                    }
                }
            }
            assert valueClass instanceof CustomClass;
            parents[i] = (CustomClass) valueClass;
        }
        if (isAbstract) {
            addAbstractClass(className, caption, parents);
        } else {
            addConcreteClass(className, caption, parents);
        }
    }

    public void addScriptedGroup(String groupName, String captionStr, String parentName, Set<String> importedModules) {
        String caption = (captionStr == null ? groupName : transformCaptionStr(captionStr));

        if (parentName != null) {
            AbstractGroup parentGroup;
            int dotPosition = parentName.indexOf('.');
            if (dotPosition > 0) {
                LogicsModule module = getModule(parentName.substring(0, dotPosition));
                parentGroup = module.getGroup(module.transformNameToSID(parentName.substring(dotPosition + 1)));
            } else {
                parentGroup = getGroup(transformNameToSID(parentName));
                if (parentGroup == null) {
                    for (String importModuleName : importedModules) {
                        LogicsModule module = getModule(importModuleName);
                        if ((parentGroup = module.getGroup(module.transformNameToSID(parentName))) != null) {
                            break;
                        }
                    }
                }
            }
            addAbstractGroup(groupName, caption, parentGroup);
        } else {
            addAbstractGroup(groupName, caption, null);
        }
    }

    @Override
    public void initClasses() {
        try {
            LsfLogicsLexer lexer = new LsfLogicsLexer(createStream());
            LsfLogicsParser parser = new LsfLogicsParser(new CommonTokenStream(lexer));
            parser.self = this;
            parser.script();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void initTables() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void initGroups() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void initProperties()  {
    }

    @Override
    public void initIndexes() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void initNavigators() throws JRException, FileNotFoundException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getNamePrefix() {
        return scriptName;
    }
}
