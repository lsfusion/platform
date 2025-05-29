import com.openai.models.chat.completions.ChatCompletionTool;

import java.util.Map;
import java.util.function.Function;

/**
 * Simple holder for a FunctionDefinition + its Java handler.
 */
public class CustomFunction {
    public final ChatCompletionTool def;
    public final Function<Map<String, Object>, Object> handler;

    public CustomFunction(ChatCompletionTool def,
                          Function<Map<String, Object>, Object> handler) {
        this.def = def;
        this.handler = handler;
    }
}
