package lsfusion.server.physics.dev.id.name;

import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.classes.data.integral.NumericClass;
import lsfusion.server.logics.classes.user.set.ResolveClassSet;

import java.util.Arrays;
import java.util.List;

public final class PropertyCanonicalNameUtils {
    static public final String signatureLBracket = "[";
    static public final String signatureRBracket = "]";
    
    static public final String commonStringClassName = "STRING";
    static public final String commonNumericClassName = "NUMERIC";

    static public final String UNKNOWNCLASS = "?";

    static public final String classDataPropPrefix = "_CLASS_";
    static public final String policyPropPrefix = "_POLICY_PROP_";
    static public final String policyActionPrefix = "_POLICY_ACTION_";
    static public final String fullPropPrefix = "_FULL_";
    static public final String logPropPrefix = "_LOG_";
    static public final String logDropPropPrefix = "_LOGDROP_";
    static public final String drillDownPrefix = "_DRILLDOWN_";
    static public final String resetPrefix = "_RESET_";

    static public final String objValuePrefix = "_OBJVALUE";
    static public final String intervalPrefix = "_INTERVAL";

    static public String createName(String namespace, String name, ResolveClassSet... signature) {
        return createName(namespace, name, Arrays.asList(signature));
    }
    
    /*  Позволяет создавать канонические имена, а также часть канонического имени, передавая
     *  null в качестве пространства имен либо сигнатуры         
     */
    static public String createName(String namespace, String name, List<ResolveClassSet> signature) {
        StringBuilder builder = new StringBuilder();
        if (namespace != null) {
            appendNamespace(builder, namespace);
        }
        builder.append(name);
        if (signature != null) {
            builder.append(createSignature(signature));
        }
        return builder.toString();
    }

    static private void appendNamespace(StringBuilder builder, String namespace) {
        builder.append(namespace);
        builder.append(CanonicalNameUtils.DELIMITER);
    }

    static public String createSignature(List<ResolveClassSet> signature) {
        StringBuilder builder = new StringBuilder();
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
        return builder.toString();
    }
    
    static public String makeSafeName(String s) {
        return s.replaceAll("[^A-Za-z0-9_]", "_");    
    }
}
