package lsfusion.ai;

public class Prompts {

    public static final String RETRIEVE_DOCS_ON_DEMAND = "[SYSTEM INSTRUCTIONS]\n" +
            "You are an expert assistant.  \n" +
            "- If you determine that your internal knowledge is insufficient to fully answer the request, call `" + RAGRetrieve.RETRIEVE_FN + " `, passing `" + RAGRetrieve.RETRIEVE_FN_PARAM + "` as a brief description of the missing topic or issue.\n" +
            "– After retrieval, integrate RAG fragments into your answer. ";
    public static final String FIRST_RETRIEVE_DOCS = "[RETRIEVED DOCUMENTS JSON]";
    public static final String AFTER_RETRIVE_PROMPT = "[USER TASK]";

}
