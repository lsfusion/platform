package lsfusion.server.language;

// not during parsing, but during find*
public class ScriptErrorException extends RuntimeException {
    public ScriptErrorException(String message) {
        super(message);
    }
}
