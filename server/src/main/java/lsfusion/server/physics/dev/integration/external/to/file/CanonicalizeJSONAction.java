package lsfusion.server.physics.dev.integration.external.to.file;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
import lsfusion.base.file.RawFileData;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.logics.UtilsLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;
import org.erdtman.jcs.JsonCanonicalizer;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CanonicalizeJSONAction extends InternalAction {
    public CanonicalizeJSONAction(UtilsLogicsModule LM, ValueClass... valueClasses) {
        super(LM, valueClasses);
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        try {
            final RawFileData jsonFile = (RawFileData) context.getSingleDataKeyValue().getValue();
            JsonCanonicalizer jc = new JsonCanonicalizer(escapeAllStrings(new ObjectMapper().readValue(new String(jsonFile.getBytes()), Object.class)));
            LM.findProperty("canonicalizedJSON[]").change(new DataObject(jc.getEncodedString()), context);
        } catch (IOException | ScriptingErrorLog.SemanticErrorException | SQLException | SQLHandledException e) {
            throw Throwables.propagate(e);
        }
    }

    public static String escapeAllStrings(Object jsonNode) {
        if (jsonNode instanceof Map) {
            Map<String, Object> escaped = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) jsonNode).entrySet()) {
                String key = escapeUnicode(entry.getKey().toString());
                escaped.put(key, escapeAllStrings(entry.getValue()));
            }
            return new ObjectMapper().valueToTree(escaped).toString();
        } else if (jsonNode instanceof List) {
            List<Object> escapedList = new ArrayList<>();
            for (Object item : (List<?>) jsonNode) {
                escapedList.add(escapeAllStrings(item));
            }
            return new ObjectMapper().valueToTree(escapedList).toString();
        } else if (jsonNode instanceof String) {
            return escapeUnicode((String) jsonNode);
        } else {
            return jsonNode.toString();
        }
    }

    public static String escapeUnicode(String input) {
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (c < 0x20 || c > 0x7E) {
                sb.append(String.format("\\u%04x", (int) c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}