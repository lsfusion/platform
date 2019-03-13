package lsfusion.server.logics;

import lsfusion.server.logics.classes.CustomClass;
import lsfusion.server.logics.classes.DataClass;
import lsfusion.server.logics.classes.sets.ResolveClassSet;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractPropertyNameParser {
    protected static class ParseInnerException extends RuntimeException {
        public ParseInnerException(String msg) {
            super(msg);
        }
    }

    public interface ClassFinder {
        CustomClass findCustomClass(String name);
        DataClass findDataClass(String name);
    }

    protected final ClassFinder classFinder;
    protected final String name;

    protected int pos;
    protected String parseText;
    protected int len;
    
    protected AbstractPropertyNameParser(String name, ClassFinder finder) {
        assert name != null;
        assert finder != null;
        this.name = name.replaceAll(" ", "");
        this.classFinder = finder;
    }
    
    protected boolean isNext(String str) {
        return pos + str.length() <= len && parseText.substring(pos, pos + str.length()).equals(str);
    }

    protected void checkNext(String str) {
        if (isNext(str)) {
            pos += str.length();
        } else {
            throw new ParseInnerException(String.format("'%s' was expected", str));
        }
    }

    private String parseClassName() {
        Matcher matcher = Pattern.compile("[^\\w.]").matcher(parseText);
        int nextPos = (matcher.find(pos) ? matcher.start() : len);
        if (nextPos + 1 < len && parseText.charAt(nextPos) == '[') {
            nextPos = parseText.indexOf(']', nextPos + 1) + 1;
        }
        String name = parseText.substring(pos, nextPos);
        pos = nextPos;
        return name;
    }

    private CustomClass findCustomClass(String name) {
        CustomClass cls = classFinder.findCustomClass(name);
        if (cls == null) {
            throw new ParseInnerException(String.format("Custom class '%s' was not found", name));
        }
        return cls;
    }

    protected CustomClass parseCustomClass() {
        String parsedName = parseClassName();
        return findCustomClass(parsedName);
    }

    protected ResolveClassSet parseSingleClass() {
        String parsedName = parseClassName();
        DataClass cls = classFinder.findDataClass(parsedName);

        if (cls != null) {
            return cls;
        } else {
            return findCustomClass(parsedName).getResolveSet();
        }
    }
}
