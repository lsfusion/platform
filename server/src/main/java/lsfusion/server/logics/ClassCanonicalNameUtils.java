package lsfusion.server.logics;

import lsfusion.server.classes.*;
import lsfusion.server.classes.sets.AndClassSet;
import lsfusion.server.classes.sets.OrObjectClassSet;
import lsfusion.server.classes.sets.UpClassSet;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by DAle on 02.05.14.
 * 
 */

public final class ClassCanonicalNameUtils {
    public static final String ConcatenateClassNameLBracket = "(";
    public static final String ConcatenateClassNameRBracket = ")";
    public static final String ConcatenateClassNamePrefix = "CONCAT";
    
    public static final String OrObjectClassSetNameLBracket = "{";
    public static final String OrObjectClassSetNameRBracket = "}";
    
    public static final String UpClassSetNameLBracket = "(";
    public static final String UpClassSetNameRBracket = ")";
    
    // CONCAT(CN1, ..., CNk)
    public static String createName(ConcatenateClassSet ccs) {
        AndClassSet[] classes = ccs.getClasses();
        String sid = ConcatenateClassNamePrefix + ConcatenateClassNameLBracket; 
        for (AndClassSet set : classes) {
            sid += (sid.length() > 1 ? "," : "");
            sid += set.getCanonicalSID();
        }
        return sid + ConcatenateClassNameRBracket; 
    }
    
    // {UpCN, SetCN1, ..., SetCNk}
    public static String createName(OrObjectClassSet cs) {
        if (cs.set.size() == 0) {
            return cs.up.getCanonicalSID();
        } else {
            String sid = OrObjectClassSetNameLBracket; 
            sid += cs.up.getCanonicalSID();
            for (int i = 0; i < cs.set.size(); i++) {
                sid += ",";
                sid += cs.set.get(i).getCanonicalSID();
            }
            return sid + OrObjectClassSetNameRBracket; 
        }
    }
    
    // (CN1, ..., CNk) 
    public static String createName(UpClassSet up) {
        if (up.wheres.length == 1) {
            return up.wheres[0].getCanonicalSID();
        }
        String sid = UpClassSetNameLBracket;
        for (CustomClass cls : up.wheres) {
            sid += (sid.length() > 1 ? "," : "");
            sid += cls.getCanonicalSID();
        }
        return sid + UpClassSetNameRBracket;
    }

    public static DataClass defaultStringClassObj = StringClass.text;
    public static DataClass defaultNumericClassObj = NumericClass.get(5, 2);
    
    public static DataClass getDataClass(String name) {
        return dataClassNames.get(name); 
    }
    
    private static Map<String, DataClass> dataClassNames = new HashMap<String, DataClass>() {{
        put("INTEGER", IntegerClass.instance);
        put("DOUBLE", DoubleClass.instance);
        put("LONG", LongClass.instance);
        put("BOOLEAN", LogicalClass.instance);
        put("DATE", DateClass.instance);
        put("DATETIME", DateTimeClass.instance );
        put("TIME", TimeClass.instance);
        put("YEAR", YearClass.instance);
        put("WORDFILE", WordClass.get(false, false));
        put("IMAGEFILE", ImageClass.get(false, false));
        put("PDFFILE", PDFClass.get(false, false));
        put("CUSTOMFILE", DynamicFormatFileClass.get(false, false));
        put("EXCELFILE", ExcelClass.get(false, false));
        put("COLOR", ColorClass.instance);
        put("STRING", defaultStringClassObj);
        put("NUMERIC", defaultNumericClassObj);
    }};
}
