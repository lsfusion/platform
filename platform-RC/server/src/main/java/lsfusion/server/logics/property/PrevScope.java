package lsfusion.server.logics.property;

public enum PrevScope {
    EVENT, // на начало события (локального)
    DB; // текущее значение в сессии / базе

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
