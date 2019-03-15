package lsfusion.server.base.thread;

public enum SyncType {
    SYNC, // sync с submit thread'ом - один вызов
    NOSYNC, // не sync с submit thread'ом
    MULTISYNC // sync c submit thread'ом, но много потоков
}
