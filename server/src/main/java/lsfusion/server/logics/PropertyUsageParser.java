package lsfusion.server.logics;

import com.google.common.base.Throwables;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.classes.DataClass;
import lsfusion.server.classes.sets.ResolveClassSet;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by DAle on 09.07.2014.
 */

public class PropertyUsageParser extends AbstractPropertyNameParser {
    public static class PropertyUsageClassFinder implements ClassFinder {
        private ScriptingLogicsModule module;
        public PropertyUsageClassFinder(ScriptingLogicsModule module) {
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

    public PropertyUsageParser(ScriptingLogicsModule module, String name) {
        this(name, new PropertyUsageClassFinder(module));
    }

    public PropertyUsageParser(String name, ClassFinder finder) {
        super(name, finder);
    }

    public String getCompoundName() throws ParseException {
        return getCompoundName(name);
    }

    public static String getCompoundName(String name) throws ParseException {
        name = name.replaceAll(" ", "");
        int signatureIndex = name.indexOf(PropertyCanonicalNameUtils.signatureLBracket);
        if (signatureIndex > 0) {
            name = name.substring(0, signatureIndex);
        }
        if (name.indexOf(".") != name.lastIndexOf(".")) {
            throw new ParseException(String.format("Identifier '%s' must be in '[namespace.]name' format", name));            
        }
        int pointIndex;
        if ((pointIndex = name.indexOf(".")) >= 0) {
            checkID(name.substring(0, pointIndex));
            checkID(name.substring(pointIndex+1));
        }
        return name;
    }

    public List<ResolveClassSet> getSignature() throws ParseException {
        int bracketPos = name.indexOf(PropertyCanonicalNameUtils.signatureLBracket);
        if (bracketPos >= 0) {
            if (name.lastIndexOf(PropertyCanonicalNameUtils.signatureRBracket) != name.length() - 1) {
                throw new ParseException(String.format("'%s' should be at the end", PropertyCanonicalNameUtils.signatureRBracket));
            }

            parseText = name.substring(bracketPos + 1, name.length() - 1);
            pos = 0;
            len = parseText.length();

            try {
                List<ResolveClassSet> result = parseClassList();
                if (pos < len) {
                    throw new ParseException("Parse error");
                }
                return result;
            } catch (ParseInnerException e) {
                throw new ParseException(e.getMessage());
            } catch (RuntimeException re) {
                throw new ParseException(re.getMessage());
            }
        }
        return null;
    }

    private List<ResolveClassSet> parseClassList() {
        List<ResolveClassSet> result = new ArrayList<ResolveClassSet>();
        while (pos < len) {
            if (isNext(PropertyCanonicalNameUtils.UNKNOWNCLASS)) {
                checkNext(PropertyCanonicalNameUtils.UNKNOWNCLASS);
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
