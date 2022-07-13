package lsfusion.server.physics.dev.id.name;

import lsfusion.interop.form.property.ExtInt;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.*;
import lsfusion.server.logics.classes.data.file.*;
import lsfusion.server.logics.classes.data.integral.DoubleClass;
import lsfusion.server.logics.classes.data.integral.IntegerClass;
import lsfusion.server.logics.classes.data.integral.LongClass;
import lsfusion.server.logics.classes.data.integral.NumericClass;
import lsfusion.server.logics.classes.data.link.*;
import lsfusion.server.logics.classes.data.time.*;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.classes.user.set.ResolveClassSet;
import lsfusion.server.logics.classes.user.set.ResolveConcatenateClassSet;
import lsfusion.server.logics.classes.user.set.ResolveOrObjectClassSet;
import lsfusion.server.logics.classes.user.set.ResolveUpClassSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ClassCanonicalNameUtils {
    public static final String ConcatenateClassNameLBracket = "(";
    public static final String ConcatenateClassNameRBracket = ")";
    public static final String ConcatenateClassNamePrefix = "CONCAT";
    
    public static final String OrObjectClassSetNameLBracket = "{";
    public static final String OrObjectClassSetNameRBracket = "}";
    
    public static final String UpClassSetNameLBracket = "(";
    public static final String UpClassSetNameRBracket = ")";
    
    // CONCAT(CN1, ..., CNk)
    public static String createName(ResolveConcatenateClassSet ccs) {
        ResolveClassSet[] classes = ccs.getClasses();
        String sid = ConcatenateClassNamePrefix + ConcatenateClassNameLBracket; 
        for (ResolveClassSet set : classes) {
            sid += (sid.length() > 1 ? "," : "");
            sid += set.getCanonicalName();
        }
        return sid + ConcatenateClassNameRBracket; 
    }
    
    // {UpCN, SetCN1, ..., SetCNk}
    public static String createName(ResolveOrObjectClassSet cs) {
        if (cs.set.size() == 0) {
            return cs.up.getCanonicalName();
        } else {
            String sid = OrObjectClassSetNameLBracket; 
            sid += cs.up.getCanonicalName();
            for (int i = 0; i < cs.set.size(); i++) {
                sid += ",";
                sid += cs.set.get(i).getCanonicalName();
            }
            return sid + OrObjectClassSetNameRBracket; 
        }
    }
    
    // (CN1, ..., CNk) 
    public static String createName(ResolveUpClassSet up) {
        if (up.wheres.length == 1) {
            return up.wheres[0].getCanonicalName();
        }
        String sid = UpClassSetNameLBracket;
        for (CustomClass cls : up.wheres) {
            sid += (sid.length() > 1 ? "," : "");
            sid += cls.getCanonicalName();
        }
        return sid + UpClassSetNameRBracket;
    }

    private static final DataClass defaultStringClassObj = StringClass.text;
    private static final DataClass defaultNumericClassObj = NumericClass.get(5, 2);
    private static final DataClass defaultZDateTimeClassObj = ZDateTimeClass.instance;
    private static final DataClass defaultDateTimeClassObj = DateTimeClass.instance;
    private static final DataClass defaultTimeClassObj = TimeClass.instance;

    public static DataClass getCanonicalNameDataClass(String name) {
        return canonicalDataClassNames.get(name); 
    }
    
    private static Map<String, DataClass> canonicalDataClassNames = new HashMap<String, DataClass>() {{
        put("INTEGER", IntegerClass.instance);
        put("DOUBLE", DoubleClass.instance);
        put("LONG", LongClass.instance);
        put("BOOLEAN", LogicalClass.instance);
        put("TBOOLEAN", LogicalClass.threeStateInstance);
        put("DATE", DateClass.instance);
        put("DATETIME", defaultDateTimeClassObj );
        put("ZDATETIME", defaultZDateTimeClassObj );
        put("DATEINTERVAL", DateIntervalClass.instance);
        put("DATETIMEINTERVAL", DateTimeIntervalClass.instance);
        put("TIMEINTERVAL", TimeIntervalClass.instance);
        put("TIME", defaultTimeClassObj);
        put("YEAR", YearClass.instance);
        put("WORDFILE", WordClass.get());
        put("IMAGEFILE", ImageClass.get());
        put("PDFFILE", PDFClass.get());
        put("DBFFILE", DBFClass.get());
        put("RAWFILE", CustomStaticFormatFileClass.get());
        put("FILE", DynamicFormatFileClass.get());
        put("EXCELFILE", ExcelClass.get());
        put("TEXTFILE", TXTClass.get());
        put("CSVFILE", CSVClass.get());
        put("HTMLFILE", HTMLClass.get());
        put("JSONFILE", JSONFileClass.get());
        put("XMLFILE", XMLClass.get());
        put("TABLEFILE", TableClass.get());
        put("NAMEDFILE", NamedFileClass.instance);
        put("WORDLINK", WordLinkClass.get(false));
        put("IMAGELINK", ImageLinkClass.get(false));
        put("PDFLINK", PDFLinkClass.get(false));
        put("DBFLINK", DBFLinkClass.get(false));
        put("RAWLINK", CustomStaticFormatLinkClass.get());
        put("LINK", DynamicFormatLinkClass.get(false));
        put("EXCELLINK", ExcelLinkClass.get(false));
        put("TEXTLINK", TXTLinkClass.get(false));
        put("CSVLINK", CSVLinkClass.get(false));
        put("HTMLLINK", HTMLLinkClass.get(false));
        put("JSONLINK", JSONLinkClass.get(false));
        put("XMLLINK", XMLLinkClass.get(false));
        put("TABLELINK", TableLinkClass.get(false));
        put("COLOR", ColorClass.instance);
        put("JSON", JSONClass.instance);
        put("STRING", defaultStringClassObj);
        put("NUMERIC", defaultNumericClassObj);
    }};

    public static DataClass getScriptedDataClass(String name) {
        assert !name.contains(" ");
        if (scriptedSimpleDataClassNames.containsKey(name)) {
            return scriptedSimpleDataClassNames.get(name);
        } else if (name.matches("^((BPSTRING\\[\\d+\\])|(BPISTRING\\[\\d+\\])|(STRING\\[\\d+\\])|(ISTRING\\[\\d+\\])" +
                "|(NUMERIC\\[\\d+,\\d+\\])|(INTERVAL\\[(DATE|DATETIME|TIME|ZDATETIME)\\])|(TIME\\[\\d+\\])|(DATETIME\\[\\d+\\])|(ZDATETIME\\[\\d+\\]))$")) {
            if (name.startsWith("BPSTRING[")) {
                name = name.substring("BPSTRING[".length(), name.length() - 1);
                return StringClass.get(new ExtInt(Integer.parseInt(name)));
            } else if (name.startsWith("BPISTRING[")) {
                name = name.substring("BPISTRING[".length(), name.length() - 1);
                return StringClass.geti(new ExtInt(Integer.parseInt(name)));
            } else if (name.startsWith("STRING[")) {
                name = name.substring("STRING[".length(), name.length() - 1);
                return StringClass.getv(new ExtInt(Integer.parseInt(name)));
            } else if (name.startsWith("ISTRING[")) {
                name = name.substring("ISTRING[".length(), name.length() - 1);
                return StringClass.getvi(new ExtInt(Integer.parseInt(name)));
            } else if (name.startsWith("NUMERIC[")) {
                String precision = name.substring("NUMERIC[".length(), name.indexOf(","));
                String scale = name.substring(name.indexOf(",") + 1, name.length() - 1);
                return NumericClass.get(Integer.parseInt(precision), Integer.parseInt(scale));
            } else if (name.startsWith("INTERVAL[")) {
                String intervalType = name.substring("INTERVAL[".length(), name.length() - 1);
                return IntervalClass.getInstance(intervalType);
            } else if (name.startsWith("TIME[")) {
                name = name.substring("TIME[".length(), name.length() - 1);
                return TimeClass.get(new ExtInt(Integer.parseInt(name)));
            } else if (name.startsWith("DATETIME[")) {
                name = name.substring("DATETIME[".length(), name.length() - 1);
                return DateTimeClass.get(new ExtInt(Integer.parseInt(name)));
            } else if (name.startsWith("ZDATETIME[")) {
                name = name.substring("ZDATETIME[".length(), name.length() - 1);
                return ZDateTimeClass.get(new ExtInt(Integer.parseInt(name)));
            }
        }
        return null;
    }
    
    private static Map<String, DataClass> scriptedSimpleDataClassNames = new HashMap<String, DataClass>() {{
        put("INTEGER", IntegerClass.instance);
        put("DOUBLE", DoubleClass.instance);
        put("LONG", LongClass.instance);
        put("BOOLEAN", LogicalClass.instance);
        put("TBOOLEAN", LogicalClass.threeStateInstance);
        put("DATE", DateClass.instance);
        put("DATETIME", defaultDateTimeClassObj);
        put("ZDATETIME", defaultZDateTimeClassObj);
        put("DATEINTERVAL", DateIntervalClass.instance);
        put("DATETIMEINTERVAL", DateTimeIntervalClass.instance);
        put("TIMEINTERVAL", TimeIntervalClass.instance);
        put("TIME", defaultTimeClassObj);
        put("YEAR", YearClass.instance);
        put("WORDFILE", WordClass.get());
        put("IMAGEFILE", ImageClass.get());
        put("PDFFILE", PDFClass.get());
        put("DBFFILE", DBFClass.get());
        put("RAWFILE", CustomStaticFormatFileClass.get());
        put("FILE", DynamicFormatFileClass.get());
        put("EXCELFILE", ExcelClass.get());
        put("TEXTFILE", TXTClass.get());
        put("CSVFILE", CSVClass.get());
        put("HTMLFILE", HTMLClass.get());
        put("JSONFILE", JSONFileClass.get());
        put("XMLFILE", XMLClass.get());
        put("TABLEFILE", TableClass.get());
        put("NAMEDFILE", NamedFileClass.instance);
        put("WORDLINK", WordLinkClass.get(false));
        put("IMAGELINK", ImageLinkClass.get(false));
        put("PDFLINK", PDFLinkClass.get(false));
        put("DBFLINK", DBFLinkClass.get(false));
        put("RAWLINK", CustomStaticFormatLinkClass.get());
        put("LINK", DynamicFormatLinkClass.get(false));
        put("EXCELLINK", ExcelLinkClass.get(false));
        put("TEXTLINK", TXTLinkClass.get(false));
        put("CSVLINK", CSVLinkClass.get(false));
        put("HTMLLINK", HTMLLinkClass.get(false));
        put("JSONLINK", JSONLinkClass.get(false));
        put("XMLLINK", XMLLinkClass.get(false));
        put("TABLELINK", TableLinkClass.get(false));
        put("COLOR", ColorClass.instance);
        put("JSON", JSONClass.instance);
        put("TEXT", TextClass.instance);
        put("RICHTEXT", RichTextClass.instance);
        put("HTMLTEXT", HTMLTextClass.instance);
        put("BPSTRING", StringClass.get(ExtInt.UNLIMITED));
        put("BPISTRING", StringClass.get(true, ExtInt.UNLIMITED));
        put("STRING", StringClass.getv(ExtInt.UNLIMITED));
        put("ISTRING", StringClass.getv(true, ExtInt.UNLIMITED));
        put("NUMERIC", NumericClass.defaultNumeric);
    }};

    public static List<ResolveClassSet> getResolveList(ValueClass[] classes) {
        List<ResolveClassSet> classSets;
        classSets = new ArrayList<>();
        for (ValueClass cls : classes) {
            classSets.add(cls.getResolveSet());
        }
        return classSets;
    }
}
