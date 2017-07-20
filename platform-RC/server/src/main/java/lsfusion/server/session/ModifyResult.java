package lsfusion.server.session;

public enum ModifyResult {
    NO, // нет изменений
    DATA, // только данные в таблице (нет смысла пересчитывать выражения)
    DATA_SOURCE; // данные и метаданные

    public boolean dataChanged() {
        return this == DATA || this == DATA_SOURCE;
    }
    public boolean sourceChanged() {
        return this == DATA_SOURCE;
    }
}
