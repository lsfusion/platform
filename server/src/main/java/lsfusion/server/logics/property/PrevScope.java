package lsfusion.server.logics.property;

public enum PrevScope {
    EVENT, DB;

    // учавствует в assertion'ах, которые можно убрать, но тогда придется добавить ряд оптимизаций для event'а APPLY, чтобы свести обе эти ветки воедино
    public boolean onlyDB() {
        return this == DB;
    }

    public String getSID() {
        switch (this) {
            case EVENT:
                return "E";
            case DB:
                return "D";
        }
        throw new UnsupportedOperationException();
    }
}
