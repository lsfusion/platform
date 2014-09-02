package lsfusion.server.logics;

import lsfusion.server.classes.NumericClass;
import lsfusion.server.classes.StringClass;
import lsfusion.server.classes.sets.AndClassSet;
import lsfusion.server.classes.sets.ResolveClassSet;

import java.util.Arrays;
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

    static public final String UNKNOWNCLASS = "?";

    static public final String classDataPropPrefix = "_CLASS_";
    static public final String policyPropPrefix = "_POLICY_";
    static public final String logPropPrefix = "_LOG_";
    static public final String drillDownPrefix = "_DRILLDOWN_";

    static public String createName(String namespace, String name, List<ResolveClassSet> signature) {
        StringBuilder builder = new StringBuilder();
        builder.append(namespace);
        builder.append(".");
        builder.append(name);
        if (signature != null) {
            builder.append(signatureLBracket);
            boolean isFirst = true;
            for (ResolveClassSet cs : signature) {
                if (!isFirst) {
                    builder.append(",");
                }
                isFirst = false;
                if (cs instanceof StringClass) {
                    builder.append(commonStringClassName);
                } else if (cs instanceof NumericClass) {
                    builder.append(commonNumericClassName);
                } else if (cs != null) {
                    builder.append(cs.getCanonicalName());
                } else {
                    builder.append(UNKNOWNCLASS);
                }
            }
            builder.append(signatureRBracket);
        }
        return builder.toString();
    }

    static public String createName(String namespace, String name, ResolveClassSet... signature) {
        return createName(namespace, name, Arrays.asList(signature));
    }
}
