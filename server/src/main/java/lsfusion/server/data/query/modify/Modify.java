package lsfusion.server.data.query.modify;

public enum Modify {
    MODIFY, // если есть заместить, иначе добавить
    ADD, // добавить с assertion'ом что нет
    LEFT, // добавить если нет
    UPDATE,  // заместить
    DELETE // удалить
}
