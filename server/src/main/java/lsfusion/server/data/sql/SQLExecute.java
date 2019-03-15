package lsfusion.server.data.sql;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.*;
import lsfusion.server.data.query.exec.DynamicExecEnvSnapshot;
import lsfusion.server.data.query.exec.DynamicExecuteEnvironment;
import lsfusion.server.data.query.exec.materialize.PureTime;
import lsfusion.server.data.query.exec.materialize.PureTimeInterface;
import lsfusion.server.data.table.RegisterChange;
import lsfusion.server.data.table.TableOwner;
import lsfusion.server.data.type.ParseInterface;
import lsfusion.server.physics.admin.monitor.sql.SQLDebugInfo;

public class SQLExecute<OE, S extends DynamicExecEnvSnapshot<OE, S>> {

    public final SQLDML command;
    public final ImMap<String, ParseInterface> params;
    public final DynamicExecuteEnvironment<OE, S> queryExecEnv;
    public final OE outerEnv;
    public final int transactTimeout;
    public final OperationOwner owner;
    public final TableOwner tableOwner;
    public final PureTimeInterface pureTime;
    public final RegisterChange registerChange;    
    public final SQLDebugInfo debugInfo;

    // MATERIALIZE SUBQUERIES
    public SQLExecute(SQLDML command, ImMap<String, ParseInterface> params, DynamicExecuteEnvironment queryExecEnv, int transactTimeout, OperationOwner owner, TableOwner tableOwner, RegisterChange registerChange) {
        this(command, params, queryExecEnv, transactTimeout, owner, tableOwner, registerChange, null);
    }
    public SQLExecute(SQLDML command, ImMap<String, ParseInterface> params, DynamicExecuteEnvironment<OE, S> queryExecEnv, OE outerEnv, PureTimeInterface pureTime, int transactTimeout, OperationOwner owner, TableOwner tableOwner, RegisterChange registerChange) {
        this(command, params, queryExecEnv, outerEnv, pureTime, transactTimeout, owner, tableOwner, registerChange, null);
    }
    
    // UPDATE, DELETE, INSERTSELECT
    public SQLExecute(SQLDML command, ImMap<String, ParseInterface> params, DynamicExecuteEnvironment queryExecEnv, int transactTimeout, OperationOwner owner, TableOwner tableOwner, RegisterChange registerChange, SQLDebugInfo debugInfo) {
        this(command, params, queryExecEnv, null, PureTime.VOID, transactTimeout, owner, tableOwner, registerChange, debugInfo);
    }

    public SQLExecute(SQLDML command, ImMap<String, ParseInterface> params, DynamicExecuteEnvironment<OE, S> queryExecEnv, OE outerEnv, PureTimeInterface pureTime, int transactTimeout, OperationOwner owner, TableOwner tableOwner, RegisterChange registerChange, SQLDebugInfo debugInfo) {
        this.command = command;
        this.params = params;
        this.queryExecEnv = queryExecEnv;
        this.outerEnv = outerEnv;
        this.pureTime = pureTime;
        this.transactTimeout = transactTimeout;
        this.owner = owner;
        this.tableOwner = tableOwner;
        this.registerChange = registerChange;
        this.debugInfo = debugInfo;
    }
}
