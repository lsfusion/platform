package lsfusion.server.logics.navigator.controller.env;

import org.postgresql.replication.LogSequenceNumber;

public interface SQLSessionLSNProvider {
    void updateLSN(LogSequenceNumber lsn);
    LogSequenceNumber getLSN();
}
