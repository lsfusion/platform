package lsfusion.server.logics.resolving;

import lsfusion.server.logics.LogicsModule;

import java.util.List;

/**
 * Получение элементов системы в модуле по простому имени с параметром 
 */
public interface ModuleFinder<T, P> {
    List<T> resolveInModule(LogicsModule module, String simpleName, P param);
}
