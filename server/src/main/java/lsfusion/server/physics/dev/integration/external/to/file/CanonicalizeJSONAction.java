package lsfusion.server.physics.dev.integration.external.to.file;

import com.fasterxml.jackson.databind.node.*;
import com.fasterxml.jackson.databind.JsonNode;
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
import java.util.Iterator;
import java.util.Map;

public class CanonicalizeJSONAction extends InternalAction {
    public CanonicalizeJSONAction(UtilsLogicsModule LM, ValueClass... valueClasses) {
        super(LM, valueClasses);
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        try {
            final RawFileData jsonFile = (RawFileData) context.getSingleDataKeyValue().getValue();
            ObjectMapper mapper = new ObjectMapper();
            JsonCanonicalizer jc = new JsonCanonicalizer(mapper.writeValueAsString(escapeAllStrings(mapper.readTree(jsonFile.getBytes()))));
            LM.findProperty("canonicalizedJSON[]").change(new DataObject(jc.getEncodedString()), context);
        } catch (IOException | ScriptingErrorLog.SemanticErrorException | SQLException | SQLHandledException e) {
            throw Throwables.propagate(e);
        }
    }

    public static JsonNode escapeAllStrings(JsonNode node) {
        if (node.isTextual()) {
            return new TextNode(escapeUnicode(node.asText()));
        } else if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            ObjectNode result = new ObjectNode(JsonNodeFactory.instance);
            Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String escapedKey = escapeUnicode(entry.getKey());
                result.set(escapedKey, escapeAllStrings(entry.getValue()));
            }
            return result;
        } else if (node.isArray()) {
            ArrayNode arrayNode = (ArrayNode) node;
            ArrayNode result = new ArrayNode(JsonNodeFactory.instance);
            for (JsonNode element : arrayNode) {
                result.add(escapeAllStrings(element));
            }
            return result;
        } else {
            return node;
        }
    }

    private static String escapeUnicode(String input) {
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (c >= 0x20 && c <= 0x7E) {
                sb.append(c);
            } else {
                sb.append(String.format("\\u%04x", (int) c));
            }
        }
        return sb.toString();
    }
}