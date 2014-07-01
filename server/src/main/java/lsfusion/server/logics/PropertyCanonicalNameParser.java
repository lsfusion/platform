package lsfusion.server.logics;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.implementations.ArIndexedSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.classes.*;
import lsfusion.server.classes.sets.AndClassSet;
import lsfusion.server.classes.sets.OrObjectClassSet;
import lsfusion.server.classes.sets.UpClassSet;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by DAle on 02.05.14.
 * 
 */

public class PropertyCanonicalNameParser {
    private static class CNParseInnerException extends RuntimeException {
        public CNParseInnerException(String msg) {
            super(msg);
        }
    }

    public static class CNParseException extends Exception {
        public CNParseException(String msg) {
            super(msg);
        }
    }

    private final BusinessLogics BL;
    private final String canonicalName;

    private int pos;
    private String parseText;
    private int len;
    private final String CPREFIX = ClassCanonicalNameUtils.ConcatenateClassNamePrefix + ClassCanonicalNameUtils.ConcatenateClassNameLBracket;
    
    public PropertyCanonicalNameParser(BusinessLogics BL, String canonicalName) {
        assert canonicalName != null;
        this.canonicalName = canonicalName.replaceAll(" ", "");
        this.BL = BL;
    }

    public String getNamespace() throws CNParseException {
        int pointIndex = canonicalName.indexOf('.');
        if (pointIndex < 0) {
            throw new CNParseException("Отсутствует имя пространства имен");
        }
        String namespaceName = canonicalName.substring(0, pointIndex);
        return checkID(namespaceName);
    }

    public String getName() throws CNParseException {
        getNamespace(); // проверим валидность пространства имен
        int pointIndex = canonicalName.indexOf('.');
        int bracketIndex = canonicalName.indexOf(PropertyCanonicalNameUtils.signatureLBracket);

        String name;
        if (bracketIndex < 0) {
            name = canonicalName.substring(pointIndex + 1);
        } else {
            name = canonicalName.substring(pointIndex + 1, bracketIndex);
        }
        return checkID(name);
    }

    public List<AndClassSet> getSignature() throws CNParseException {
        int bracketPos = canonicalName.indexOf(PropertyCanonicalNameUtils.signatureLBracket);
        if (bracketPos >= 0) {
            if (canonicalName.lastIndexOf(PropertyCanonicalNameUtils.signatureRBracket) != canonicalName.length() - 1) {
                throw new CNParseException("Сигнатура должна завершаться скобкой");
            }

            parseText = canonicalName.substring(bracketPos + 1, canonicalName.length() - 1);
            pos = 0;
            len = parseText.length();

            try {
                List<AndClassSet> result = parseAndClassSetList(true);
                if (pos < len) {
                    throw new CNParseException("Ошибка парсинга");
                }
                return result;
            } catch (CNParseInnerException e) {
                throw new CNParseException(e.getMessage());
            }
        }
        return null;
    }

    private boolean isNext(String str) {
        return pos + str.length() <= len && parseText.substring(pos, pos + str.length()).equals(str);
    }

    private void checkNext(String str) {
        if (isNext(str)) {
            pos += str.length();
        } else {
            throw new CNParseInnerException("Ожидалась подстрока '" + str + "'");
        }
    }

    private String checkID(final String str) throws CNParseException {
        if (!str.matches("[a-zA-Z0-9_]+")) {
            throw new CNParseException("Идентификатор содержит запрещенные символы");
        }
        return str;
    }

    private List<AndClassSet> parseAndClassSetList(boolean isSignature) {
        List<AndClassSet> result = new ArrayList<AndClassSet>();
        while (pos < len) {
            if (isSignature && isNext(PropertyCanonicalNameUtils.UNKNOWNCLASS)) {
                checkNext(PropertyCanonicalNameUtils.UNKNOWNCLASS);
                result.add(null);
            } else {
                result.add(parseAndClassSet());
            }

            if (isNext(",")) {
                checkNext(",");
            } else {
                break;
            }
        }
        return result;
    }

    private AndClassSet parseAndClassSet() {
        AndClassSet result;
        if (isNext(CPREFIX)) {
            result = parseConcatenateClassSet();
        } else if (isNext(ClassCanonicalNameUtils.OrObjectClassSetNameLBracket)) {
            result = parseOrObjectClassSet();
        } else if (isNext(ClassCanonicalNameUtils.UpClassSetNameLBracket)) {
            result = parseUpClassSet();
        } else {
            result = parseSingleClass();
        }
        return result;
    }

    private ConcatenateClassSet parseConcatenateClassSet() {
        checkNext(CPREFIX);
        List<AndClassSet> classes = parseAndClassSetList(false);
        checkNext(ClassCanonicalNameUtils.ConcatenateClassNameRBracket);
        return new ConcatenateClassSet(classes.toArray(new AndClassSet[classes.size()]));
    }

    private OrObjectClassSet parseOrObjectClassSet() {
        checkNext(ClassCanonicalNameUtils.OrObjectClassSetNameLBracket);
        UpClassSet up = parseUpClassSet();
        checkNext(",");
        ImSet<ConcreteCustomClass> customClasses = SetFact.EMPTY();
        if (!isNext(ClassCanonicalNameUtils.OrObjectClassSetNameRBracket)) {
            customClasses = parseCustomClassList();
        }
        OrObjectClassSet orSet = new OrObjectClassSet(up, customClasses);
        checkNext(ClassCanonicalNameUtils.OrObjectClassSetNameRBracket);
        return orSet;
    }

    private ImSet<ConcreteCustomClass> parseCustomClassList() {
        List<ConcreteCustomClass> classes = new ArrayList<ConcreteCustomClass>();
        while (pos < len) {
            ConcreteCustomClass cls = (ConcreteCustomClass) parseCustomClass();
            classes.add(cls);
            if (!isNext(",")) {
                break;
            }
            checkNext(",");
        }
        return new ArIndexedSet<ConcreteCustomClass>(classes.size(), classes.toArray(new ConcreteCustomClass[classes.size()]));
    }

    private UpClassSet parseUpClassSet() {
        if (isNext(ClassCanonicalNameUtils.UpClassSetNameLBracket)) {
            checkNext(ClassCanonicalNameUtils.UpClassSetNameLBracket);
            List<CustomClass> classes = new ArrayList<CustomClass>();
            while (!isNext(ClassCanonicalNameUtils.UpClassSetNameRBracket)) {
                classes.add(parseCustomClass());
                if (!isNext(ClassCanonicalNameUtils.UpClassSetNameRBracket)) {
                    checkNext(",");
                }
            }
            checkNext(ClassCanonicalNameUtils.UpClassSetNameRBracket);
            return new UpClassSet(classes.toArray(new CustomClass[classes.size()]));
        } else {
            CustomClass cls = parseCustomClass();
            return new UpClassSet(cls);
        }
    }

    private String parseClassName() {
        Matcher matcher = Pattern.compile("[^\\w\\.]").matcher(parseText);
        int nextPos = (matcher.find(pos) ? matcher.start() : len);
        if (nextPos + 1 < len && parseText.charAt(nextPos) == '[') {
            nextPos = parseText.indexOf(']', nextPos + 1) + 1;
        }
        String name = parseText.substring(pos, nextPos);
        pos = nextPos;
        return name;
    }

    private CustomClass findCustomClass(String name) {
        CustomClass cls = BL.findClass(name);
        if (cls == null) {
            throw new CNParseInnerException("Пользовательский класс " + name + " не найден");
        }
        return cls;
    }

    private CustomClass parseCustomClass() {
        String parsedName = parseClassName();
        return findCustomClass(parsedName);
    }

    private AndClassSet parseSingleClass() {
        String parsedName = parseClassName();
        DataClass cls = ClassCanonicalNameUtils.getDataClass(parsedName);

        if (cls != null) {
            return cls;
        } else {
            return findCustomClass(parsedName).getUpSet();
        }
    }
}
