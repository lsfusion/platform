package lsfusion.server.logics;

import lsfusion.server.classes.NumericClass;
import lsfusion.server.classes.StringClass;
import lsfusion.server.classes.sets.AndClassSet;

import java.util.List;

/**
 * Created by DAle on 03.05.14.
 * 
 */

public final class PropertyCanonicalNameUtils {
    static public final String signatureLBracket = "[";
    static public final String signatureRBracket = "]";
    
    static public final String commonStringClassName = "STRING";
    static public final String commonNumericClassName = "NUMERIC";
    
    static public String createName(String namespace, String name, List<AndClassSet> signature) {
        StringBuilder builder = new StringBuilder();
        builder.append(namespace);
        builder.append(".");
        builder.append(name);
        if (signature != null) {
            builder.append(signatureLBracket);
            boolean isFirst = true;
            for (AndClassSet cs : signature) {
                if (!isFirst) {
                    builder.append(",");
                }
                isFirst = false;
                if (cs instanceof StringClass) {
                    builder.append(commonStringClassName);
                } else if (cs instanceof NumericClass) {
                    builder.append(commonNumericClassName);
                } else {
                    builder.append(cs.getCanonicalSID());
                }
            }
            builder.append(signatureRBracket);
        }
        return builder.toString();
    } 
}
