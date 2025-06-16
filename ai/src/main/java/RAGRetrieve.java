import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.openai.core.JsonValue;
import com.openai.models.FunctionDefinition;
import com.openai.models.FunctionParameters;
import com.openai.models.ResponseFormatJsonSchema;
import com.openai.models.chat.completions.ChatCompletionTool;
import com.openai.models.embeddings.CreateEmbeddingResponse;
import com.openai.models.embeddings.EmbeddingCreateParams;
import io.pinecone.clients.Index;
import io.pinecone.clients.Pinecone;
import io.pinecone.unsigned_indices_model.QueryResponseWithUnsignedIndices;
import io.pinecone.unsigned_indices_model.ScoredVectorWithUnsignedIndices;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class RAGRetrieve {

    // OPENAI_API_KEY is used for the openai
    public static final String PINE_KEY   = System.getenv("PINECONE_API_KEY");
    public static final String PINE_INDEX = "lsfusion";

    public static final String EMBEDDING = "text-embedding-3-large";
    public static final String NAMESPACE = "";

    public static final String TEXT = "text";
    public static final String SOURCETYPE = "sourceType";

    public static final String SOURCETYPE_SYSTEM = "system";

    public static final String SOURCETYPE_DOCUMENTATION = "documentation";
    public static final String SOURCETYPE_DOCUMENTATION_PARADIGM = "paradigm";
    public static final String SOURCETYPE_DOCUMENTATION_HOWTO = "how-to";
    public static final String SOURCETYPE_DOCUMENTATION_LANGUAGE = "language";

    public static final String SOURCETYPE_DOCUMENTATION_TUTORIAL = "tutorial";

    public static final String SOURCETYPE_ARTICLES = "articles";

    public static final String SOURCETYPE_TALKS = "talks";

    // how many chunks to pull per sourceType
    private static final Map<String, Integer> TOP_K;
    static {
        Map<String, Integer> m = new HashMap<>();
        m.put(SOURCETYPE_SYSTEM, 10);
        m.put(SOURCETYPE_DOCUMENTATION + "-" + SOURCETYPE_DOCUMENTATION_PARADIGM, 3);
        m.put(SOURCETYPE_DOCUMENTATION + "-" + SOURCETYPE_DOCUMENTATION_HOWTO,        3);
        m.put(SOURCETYPE_DOCUMENTATION + "-" + SOURCETYPE_DOCUMENTATION_LANGUAGE,        3);
        m.put(SOURCETYPE_DOCUMENTATION + "-" + SOURCETYPE_DOCUMENTATION_TUTORIAL,        3);
        m.put(SOURCETYPE_ARTICLES,       3);
        m.put(SOURCETYPE_TALKS,       3);
        TOP_K = Collections.unmodifiableMap(m);
    }

    public static final Pinecone pinecone = new Pinecone.Builder(PINE_KEY).build();
    public static final Index index = pinecone.getIndexConnection(PINE_INDEX);

    public static Value v(String s) {
        return Value.newBuilder().setStringValue(s).build();
    }
    public static Struct eqFilter(String f, String v) {
        return Struct.newBuilder()
                .putFields(f, Value.newBuilder()
                        .setStructValue(
                                Struct.newBuilder().putFields("$eq", v(v)).build()
                        ).build()
                )
                .build();
    }

    public final static String arrayField = "values";
    /* "values" : ["dfd","dffd"] */
    public static ResponseFormatJsonSchema arrayOfStrings() {
        return arrayOfSchema(JsonValue.from(
                Collections.singletonMap("type", JsonValue.from("string"))
        ));
    }

    @NotNull
    private static ResponseFormatJsonSchema arrayOfSchema(JsonValue itemSchema) {
        // --- 1) Build the inner array‐of‐strings schema ---
        Map<String, JsonValue> arraySchemaMap = new LinkedHashMap<>();
        arraySchemaMap.put("type", JsonValue.from("array"));
        arraySchemaMap.put("items", itemSchema);
        JsonValue arraySchema = JsonValue.from(arraySchemaMap);

        // --- 2) Wrap it in a top‐level object schema with a "values" property ---
        ResponseFormatJsonSchema.JsonSchema.Schema objectSchema =
                ResponseFormatJsonSchema.JsonSchema.Schema.builder()
                        .putAdditionalProperty("type", JsonValue.from("object"))
                        .putAdditionalProperty("properties", JsonValue.from(Collections.singletonMap(arrayField, arraySchema)))
                        .putAdditionalProperty("required", JsonValue.from(Collections.singletonList(arrayField)))
                        .putAdditionalProperty("additionalProperties", JsonValue.from(false))
                        .build();

        // --- 3) Wire it all up into a ResponseFormatJsonSchema ---
        ResponseFormatJsonSchema.JsonSchema jsonSchemaConfig =
                ResponseFormatJsonSchema.JsonSchema.builder()
                        .name("array_of_strings")
                        .schema(objectSchema)
                        .strict(true)
                        .build();

        return ResponseFormatJsonSchema.builder()
                .jsonSchema(jsonSchemaConfig)
                .build();
    }

    /**
     * Builds a ResponseFormatJsonSchema for an array of { key, value } objects
     */
    public static ResponseFormatJsonSchema arrayOfKeyValue() {
        // properties: key and value
        Map<String, JsonValue> propsMap = new LinkedHashMap<>();
        propsMap.put(keyField, JsonValue.from(Collections.singletonMap("type", JsonValue.from("string"))));
        propsMap.put(valueField, JsonValue.from(Collections.singletonMap("type", JsonValue.from("string"))));

        // Build schema for a single key/value object
        Map<String, JsonValue> itemProps = new LinkedHashMap<>();
        itemProps.put("type", JsonValue.from("object"));
        itemProps.put("properties", JsonValue.from(propsMap));
        itemProps.put("required", JsonValue.from(new ArrayList<>(propsMap.keySet())));
        itemProps.put("additionalProperties", JsonValue.from(false));
        JsonValue itemSchema = JsonValue.from(itemProps);

        return arrayOfSchema(itemSchema);
    }


    public final static String keyField = "key";
    public final static String valueField = "value";
    /**
     * Parses a JSON array of { key, value } into a Map<String,String>
     */
    public static LinkedHashMap<String, String> parseKeyValueToMap(String json) throws JsonProcessingException {
        Map<String, List<Map<String, String>>> list = AIAgent.M.readValue(
                json,
                new TypeReference<Map<String, List<Map<String, String>>>>() {}
        );
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        for (Map<String, String> m : list.get(arrayField)) {
            result.put(m.get(keyField), m.get(valueField));
        }
        return result;
    }

    /**
     * 1) Embed with OpenAI → 2) Query Pinecone → 3) Sort and return.
     */
    public static List<Map<String,Object>> retrieveDocs(String query, boolean includeSystem) {
        List<Float> vec = getEmbedding(query);

        List<Map<String,Object>> out = new ArrayList<>();
        for (Map.Entry<String, Integer> e : TOP_K.entrySet()) {
            if(e.getKey().equals(SOURCETYPE_SYSTEM)) {
                if(!includeSystem)
                    continue;
            }

            QueryResponseWithUnsignedIndices resp = index.query(
                    /*topK=*/           e.getValue(),
                    /*vector=*/         vec,
                    /*sparseIndices=*/  null,
                    /*sparseValues=*/   null,
                    /*id=*/             null,
                    /*namespace=*/      NAMESPACE,
                    /*filter=*/         eqFilter(SOURCETYPE, e.getKey()),
                    /*includeValues=*/  false,
                    /*includeMetadata=*/true
            );

            for (ScoredVectorWithUnsignedIndices m : resp.getMatchesList()) {
                Map<String, Object> result = new HashMap<>();
                result.put("source", e.getKey());
                result.put("text",   m.getMetadata().getFieldsOrDefault(TEXT, v("")).getStringValue());
                result.put("score",  m.getScore());
                out.add(result);
            }
        }
        out.sort(Comparator.comparingDouble(d -> -(Float)d.get("score")));
        return out;
    }

    @NotNull
    public static List<Float> getEmbedding(String query) {
        CreateEmbeddingResponse emb = AIAgent.openai.embeddings()
                .create(EmbeddingCreateParams.builder()
                        .model(EMBEDDING) // should correspond index embedding
                        .input(query)
                        .build());
        return emb.data().get(0).embedding().stream().map(Double::floatValue).collect(Collectors.toList());
    }

    public static final String RETRIEVE_FN = "retrieve_docs";
    public static final String RETRIEVE_FN_PARAM = "query";

    @NotNull
    public static CustomFunction getRetrieveFunction() {
        // built-in retrieve_docs

        FunctionDefinition function = FunctionDefinition.builder()
                .name("retrieve_docs")
                .description("Fetch prioritized chunks from your RAG store—documentation, how-tos, tutorials and articles—based on a single search query")
                .parameters(
                        FunctionParameters.builder()
                                // строковый примитив можно создать через textNode
                                .putAdditionalProperty("type", JsonValue.from("object"))
                                // inline-схема properties
                                .putAdditionalProperty("properties", JsonValue.from(
                                        Collections.singletonMap(RETRIEVE_FN_PARAM, AIAgent.M.createObjectNode()
                                                        .put("type", "string")
                                                        .put("description", "Search query describing the topic or problem you need more information on")
                                                )
                                ))
                                // inline-массив required
                                .putAdditionalProperty("required", JsonValue.from(Collections.singletonList("query")))
                                .putAdditionalProperty("additionalProperties", JsonValue.from(false))
                                .build()
                )
                .build();
        return new CustomFunction(ChatCompletionTool.builder()
                .function(function)
                .build(), args -> retrieveDocs((String) args.get("query"), false));
    }
}
