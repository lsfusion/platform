package lsfusion.server.data.query;

import lsfusion.server.Settings;

public class AdjustVolatileExecuteEnvironment extends DynamicExecuteEnvironment {

    // если volatileStats, то assertion что запрос без volatileStats с заданным timeout'ом не выполнился
    private boolean volatileStats;
    private int timeout = Settings.get().getTimeoutStart();

    public synchronized DynamicExecEnvSnapshot getSnapshot(int transactTimeout) {
        return new DynamicExecEnvSnapshot(volatileStats, timeout, transactTimeout);
    }

    // метод "обратный" prepareEnv'у, его задача "размешать" локальное и глобальное состояние, то есть определить когда локальное состояние мешает глобальному
    private boolean checkSnapshot(DynamicExecEnvSnapshot snapshot) {
        if(snapshot.noHandled)
            return false;

        if(!(volatileStats == snapshot.volatileStats && timeout == snapshot.timeout)) // discard'м если состояние на конец отличается от состояния на начало
            return false;

        if(timeout == 0 || (!snapshot.volatileStats && snapshot.sessionVolatileStats) || snapshot.isTransactTimeout) // уже выключен, snapshot хочет volatile, а сессия нет, включился transactTimeout
            return false;

        return true;
    }

    public synchronized void succeeded(DynamicExecEnvSnapshot snapshot) {
        if(snapshot.volatileStats && timeout > snapshot.secondsFromTransactStart) { // проверка checkSnapshot не первая для оптимизации
            if(!checkSnapshot(snapshot))
                return;

            assert volatileStats;
            // только если больше чем secondsFromTransactStart, потому как в противном случае без volatileStats с большой вероятностью выполнялось для меньшего timeout'а
            timeout = 0; // то есть без volatileStats не выполнилось, а с volatileStats выполнилось - помечаем запрос как опасный, точнее выключаем env
        }
    }

    public synchronized void failed(DynamicExecEnvSnapshot snapshot) {
        if(!checkSnapshot(snapshot))
            return;

        int degree = Settings.get().getTimeoutDegree();
        if(timeout < snapshot.setTimeout) {
            assert snapshot.setTimeout == snapshot.secondsFromTransactStart; // так как увеличить timeout может только транзакция
            timeout = snapshot.setTimeout;
        } else {
            assert !snapshot.isTransactTimeout;
            if(volatileStats)
                timeout *= degree;
        }
        volatileStats = !volatileStats;
    }
}
