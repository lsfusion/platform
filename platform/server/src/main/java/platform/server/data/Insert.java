package platform.server.data;

public enum Insert {
    MODIFY, // если есть заместить, иначе добавить
    ADD, // добавить с assertion'ом что нет
    LEFT, // добавить если нет
    UPDATE // заместить
}
