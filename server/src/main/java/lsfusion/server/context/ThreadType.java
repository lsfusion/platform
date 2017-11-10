package lsfusion.server.context;

public enum ThreadType {
    EXECUTORFACTORY, // через ExecutorFactory - callable thread
    
    // внешние потоки
    RMI, // RMI
    HTTP, // HTTP
    START, // START
    TIMER, // TIMER
    UNREFERENCED, // UNREFERENCED
    
}
