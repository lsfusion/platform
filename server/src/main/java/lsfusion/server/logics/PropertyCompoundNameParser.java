package lsfusion.server.logics;

import com.google.common.base.Throwables;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.classes.DataClass;
import lsfusion.server.classes.sets.ResolveClassSet;
import lsfusion.server.logics.CompoundNameUtils.ParseException;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.util.ArrayList;
import java.util.List;

import static lsfusion.server.logics.CompoundNameUtils.DELIMITER;
import static lsfusion.server.logics.CompoundNameUtils.checkForCorrectness;
import static lsfusion.server.logics.PropertyCanonicalNameUtils.*;

// Под compoundName мы в данном случае понимам любую форму обращения к свойству из скрипта
// В compoundName может быть не указана сигнатура и/или пространство имен
public class PropertyCompoundNameParser extends AbstractPropertyNameParser {
    public static class ModulePropertyUsageClassFinder implements ClassFinder {
        private ScriptingLogicsModule module;
        public ModulePropertyUsageClassFinder(ScriptingLogicsModule module) {
            this.module = module;
        }

        @Override
        public CustomClass findCustomClass(String name) {
            try {
                return (CustomClass) module.findClass(name);
            } catch (ScriptingErrorLog.SemanticErrorException e) {
                Throwables.propagate(e);
            }
            return null;
        }

        @Override
        public DataClass findDataClass(String name) {
            return ClassCanonicalNameUtils.getScriptedDataClass(name);
        }
    }

    public static class BLCompoundNameClassFinder implements ClassFinder {
        private BusinessLogics BL;
        public BLCompoundNameClassFinder(BusinessLogics BL) {
            this.BL = BL;
        }

        @Override
        public CustomClass findCustomClass(String name) {
            return BL.findClassByCompoundName(name);
        }

        @Override
        public DataClass findDataClass(String name) {
            return ClassCanonicalNameUtils.getScriptedDataClass(name);
        }
    }

    public PropertyCompoundNameParser(ScriptingLogicsModule module, String name) {
        this(name, new ModulePropertyUsageClassFinder(module));
    }

    public PropertyCompoundNameParser(BusinessLogics BL, String name) {
        this(name, new BLCompoundNameClassFinder(BL));
    }

    public PropertyCompoundNameParser(String name, ClassFinder finder) {
        super(name, finder);
    }

    public String getName() {
        return getName(name);
    }

    public static String getName(String propertyCompoundName) {
        return CompoundNameUtils.getName(propertyCompoundNameWithoutSignature(propertyCompoundName));
    }
    
    public String getNamespace() {
        return getNamespace(name);
    }

    public static String getNamespace(String propertyCompoundName) {
        return CompoundNameUtils.getNamespace(propertyCompoundNameWithoutSignature(propertyCompoundName));
    }
    
    public String propertyCompoundNameWithoutSignature() {
        return propertyCompoundNameWithoutSignature(name);
    }

    private static String propertyCompoundNameWithoutSignature(String name) {
        name = name.replaceAll(" ", "");
        int signatureIndex = leftBracketPosition(name);
        if (signatureIndex > 0) {
            name = name.substring(0, signatureIndex);
        }
        if (name.indexOf(DELIMITER) != name.lastIndexOf(DELIMITER)) {
            throw new ParseException(String.format("compound name '%s' must be in '[namespace.]name' format", name));            
        }
        checkForCorrectness(name);
        return name;
    }

    public List<ResolveClassSet> getSignature() {
        int bracketPos = leftBracketPosition(name);
        if (bracketPos >= 0) {
            if (name.lastIndexOf(signatureRBracket) != name.length() - 1) {
                throw new ParseException(String.format("'%s' should be at the end", signatureRBracket));
            }

            parseText = name.substring(bracketPos + 1, name.length() - 1);
            pos = 0;
            len = parseText.length();

            try {
                List<ResolveClassSet> result = parseClassList();
                if (pos < len) {
                    throw new ParseException("parse error");
                }
                return result;
            } catch (RuntimeException re) {
                throw new ParseException(re.getMessage());
            }
        }
        return null;
    }

    private static int leftBracketPosition(String name) {
        return name.indexOf(signatureLBracket);
    }
    
    private List<ResolveClassSet> parseClassList() {
        List<ResolveClassSet> result = new ArrayList<>();
        while (pos < len) {
            if (isNext(UNKNOWNCLASS)) {
                checkNext(UNKNOWNCLASS);
                result.add(null);
            } else {
                result.add(parseSingleClass());
            }

            if (isNext(",")) {
                checkNext(",");
            } else {
                break;
            }
        }
        return result;
    }
}
