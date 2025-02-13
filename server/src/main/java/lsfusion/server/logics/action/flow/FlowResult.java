package lsfusion.server.logics.action.flow;

public enum FlowResult {
    RETURN, BREAK, CONTINUE, THROWS, FINISH;

    public boolean isFinish() {
        return this == FINISH;
    }
    public boolean isContinue() {
        return this == CONTINUE;
    }
    public boolean isBreak() {
        return this == BREAK;
    }
    public boolean isThrows() {
        return this == THROWS;
    }
}
