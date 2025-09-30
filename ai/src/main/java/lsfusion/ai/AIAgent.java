package lsfusion.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

// OpenAI Java SDK
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.ResponseFormatJsonSchema;
import com.openai.models.chat.completions.*;

import java.util.*;
import java.util.function.Function;

public class AIAgent {

    public static final ChatModel baseModel = ChatModel.GPT_4_1_MINI;
    public static final ChatModel model = baseModel;

    public static final ObjectMapper M = new ObjectMapper();

    //— reads OPENAI_API_KEY from the env
    public static final OpenAIClient openai = OpenAIOkHttpClient.fromEnv();

    // there is no lang chain agent executor for java yet, so we'll do it manually
    public static String request(String prompt, List<CustomFunction> allFunctions, ResponseFormatJsonSchema jsonSchema) {
        try {
            Map<String, Function<Map<String, Object>, Object>> handlers = new HashMap<>();
            List<ChatCompletionTool> fns = new ArrayList<>();
            for (CustomFunction cf : allFunctions) {
                fns.add(cf.def);
                handlers.put(cf.def.function().name(), cf.handler);
            }

            List<ChatCompletionMessageParam> history = new ArrayList<>();
            history.add(ChatCompletionMessageParam.ofUser(ChatCompletionUserMessageParam.builder()
                    .content(ChatCompletionUserMessageParam.Content.ofText(prompt))
                    .build()));

            while (true) {
                ChatCompletionCreateParams.Builder params = ChatCompletionCreateParams.builder()
                        .model(model)
                        .messages(history)
                        .tools(fns)
                        .toolChoice(ChatCompletionToolChoiceOption.Auto.AUTO);
                if (jsonSchema != null)
                    params = params.responseFormat(jsonSchema);

                ChatCompletionMessage msg = openai.chat()
                        .completions()
                        .create(params.build()).
                        choices().
                        get(0).
                        message();

                if (msg.toolCalls().isPresent()) {
                    List<ChatCompletionMessageToolCall> toolCalls = msg.toolCalls().get();

                    for (ChatCompletionMessageToolCall tc : toolCalls) {
                        ChatCompletionMessageToolCall.Function functionCall = tc.function();
                        Map<String, Object> args = M.readValue(
                                functionCall.arguments(),
                                new TypeReference<Map<String, Object>>() {
                                }
                        );
                        Object result = handlers.get(functionCall.name()).apply(args);

                        if (result instanceof String) {
                            return (String) result;
                        }

                        String rj = M.writeValueAsString(result);
                        history.add(
                                ChatCompletionMessageParam.ofAssistant(
                                        ChatCompletionAssistantMessageParam.builder()
                                                .toolCalls(Collections.singletonList(tc))
                                                .build()
                                )
                        );
                        history.add(ChatCompletionMessageParam.ofTool(ChatCompletionToolMessageParam.builder()
                                .toolCallId(tc.id())
                                .content(rj)
                                .build()));
                    }

                    continue;
                }

                return msg.content().get();
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String requestWithRAG(String prompt,
                                        List<CustomFunction> customs, ResponseFormatJsonSchema jsonSchema) throws Exception {

        List<CustomFunction> allFunctions = new ArrayList<>();
        allFunctions.addAll(customs);
        allFunctions.add(RAGRetrieve.getRetrieveFunction());

        String docsJson = M.writeValueAsString(RAGRetrieve.retrieveDocs(prompt, true));
        return request(Prompts.RETRIEVE_DOCS_ON_DEMAND + "\n" + Prompts.FIRST_RETRIEVE_DOCS + docsJson + "\n" + Prompts.AFTER_RETRIVE_PROMPT + prompt, allFunctions, jsonSchema);
    }
}