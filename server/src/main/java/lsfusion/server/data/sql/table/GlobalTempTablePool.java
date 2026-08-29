package lsfusion.server.data.sql.table;

import lsfusion.base.Result;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.ExceptionUtils;
import lsfusion.server.base.caches.CacheStats;
import lsfusion.server.data.table.KeyField;
import lsfusion.server.data.table.PropertyField;
import lsfusion.server.data.table.TableOwner;
import java.lang.ref.WeakReference;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.adapter.DataAdapter;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.admin.log.ServerLoggers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

// A pool of tables the whole node shares, handed out instead of creating a temporary one : creating a table takes a lock that suppresses the fast path of the lock manager for the whole
// cluster until the transaction ends, and a shared table is created once for the node rather than once for every connection that ever needs that shape. What the node keeps is therefore an
// ordinary UNLOGGED table rather than a temporary one, named so that it belongs to this node.
//
// One rule covers everything that can go wrong with a slot : DISCARD it - out of the registry, never handed out again, dropped later outside a transaction. Nothing is repaired or quarantined.
public class GlobalTempTablePool {

    // the pool's own schema, and the reason it has one : a name in it cannot be anything else. Told apart by a prefix the tables were indistinguishable from an application table that happened
    // to start with the same letters - and the sweep drops what MATCHES, so that table would have gone. A schema the platform creates and owns says whose these are structurally, keeps them
    // out of the application's namespace in a client and lets a backup drop them by naming one thing. The relation name inside it still carries the node number, which is what lets a start
    // sweep away exactly what THIS node left behind the last time it ran
    private static final String SCHEMA = "lsfpool";

    // one pool per adapter, the way every other thing hanging off a database is : what it hands out are relations of ONE database, and its node number is claimed on that database
    private final DataAdapter adapter;
    public GlobalTempTablePool(DataAdapter adapter) {
        this.adapter = adapter;
    }

    // read without the lock on the hot path : it only ever goes true once, and false for good
    private volatile boolean ready;
    private String namePrefix;
    // set before anything that can fail, so it says "the start was tried" - a server that could not take a node number works without the pool rather than paying for the attempt on every request
    private volatile boolean started;
    private int node;

    private long counter;
    // ordered against the start of a transaction, so a snapshot-based one can tell whether a free slot was already empty when it took its snapshot
    private final AtomicLong stamp = new AtomicLong();

    // everything the pool knows about one of its tables, from the moment its name is minted until it is killed
    private static class Slot {
        private final TemporaryTableStruct struct;
        private DataAdapter.Server server; // the one its table is on : an unlogged relation is a relation of ONE server, and a session that moves takes the slot with it (see moved)
        private long freedStamp; // the moment its emptiness dates from - ordered against the start of a transaction, so a snapshot based one can tell whether it was already empty when it started
        private long deadRows; // what the emptyings left behind since its storage was last reset - the node's count, not any one connection's, since the slot moves between them

        private Slot(TemporaryTableStruct struct, DataAdapter.Server server, long freedStamp) {
            this.struct = struct;
            this.server = server;
            this.freedStamp = freedStamp; // never zero, so that a slot can not reach the free set dated before every transaction there is
        }
    }
    private final Map<String, Slot> slots = new HashMap<>();
    private final Map<TemporaryTableStruct, Set<String>> free = new HashMap<>(); // by shape : the names anybody may take
    private final Map<String, DataAdapter.Server> pendingDrops = new HashMap<>(); // where each name's table is, since a drop has to go to the server that holds it. A map for the idempotence of kill, not for any order

    public synchronized void addDeadRows(String table, long rows) {
        Slot slot = slots.get(table);
        if(slot != null)
            slot.deadRows += rows;
    }
    public synchronized void resetDeadRows(String table) {
        Slot slot = slots.get(table);
        if(slot != null)
            slot.deadRows = 0;
    }

    public long nextStamp() {
        return stamp.incrementAndGet();
    }

    // whether a slot can be asked for, starting the pool if nobody has yet. Read without the lock on the way in : the start happens once, and after it this is the hot path
    public boolean ensureReady() {
        if(started)
            return ready;

        synchronized (this) {
            if(started)
                return ready;
            started = true;

            try {
                node = adapter.getNodeId();
                if(node < 0) {
                    ServerLoggers.serviceLogger.error("GLOBAL TEMP TABLE POOL : this server has no node number, working without the pool");
                    return false;
                }

                namePrefix = SCHEMA + ".n" + node + "_t_";

                // all or nothing : anything left behind still holds the rows of the last run, so the pool stays out unless the sweep finished. Every server, not just the master - a slot lives
                // on whichever server its session sits on, so the last run left its tables wherever it went
                adapter.runOnAllServers(server -> adapter.runMaintenance(server, connection -> dropOwnTables(connection, server)));

                ready = true;
                ServerLoggers.serviceLogger.info("GLOBAL TEMP TABLE POOL READY : node " + node + ", names " + namePrefix + "*");
            } catch (Throwable t) { // the pool is optional, so nothing about it takes the server down - and the failure is logged with its stack rather than swallowed
                ServerLoggers.serviceLogger.error("GLOBAL TEMP TABLE POOL : could not start, working without it", t);
            }
            return ready;
        }
    }

    // a server registered after the pool started missed the sweep above, and the pool starts on the first request for a table - which on a starting server comes long before the replicas are
    // registered. So a joining one is swept too, before it is registered and therefore before anything of this node can be on it. A failure here does not take the pool out : the pool is
    // already serving every other server, and what is left on this one is an orphan of the kind the next start sweeps.
    // It does leave that server without the schema, which the sweep is also what creates, and every slot minted or moved there then fails on the create. That is loud and it loses nothing -
    // the create is the first statement, so a mint burns a name and a move leaves every table where it was - and it is the same answer the pool gives to any server it could not prepare
    public void sweepJoinedServer(DataAdapter.Server server) {
        if(!ready)
            return;
        try {
            adapter.runMaintenance(server, connection -> dropOwnTables(connection, server));
        } catch (Throwable t) {
            ServerLoggers.serviceLogger.error("GLOBAL TEMP TABLE POOL : could not sweep " + server.host + " as it joined", t);
        }
    }

    // whatever this node left behind on that server the last time it ran : the pool creates its tables lazily, so there is nothing to keep and nothing to check the shape of
    private void dropOwnTables(Connection connection, DataAdapter.Server server) throws SQLException {
        setLockTimeout(connection);

        // the schema is made here rather than at the first create : this runs on every server before anything of this node can be on it, which is exactly when a server needs one. Every start of
        // every node does it, so it is IF NOT EXISTS - and if it cannot be made the sweep fails, which takes the pool out, which is the right answer to having nowhere to put its tables
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA IF NOT EXISTS " + SCHEMA);
        }

        List<String> tables = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT tablename FROM pg_tables WHERE schemaname = '" + SCHEMA + "' AND tablename LIKE E'n" + node + "\\\\_t\\\\_%' ESCAPE E'\\\\'")) {
            while (result.next())
                tables.add(SCHEMA + "." + result.getString(1)); // the catalog answers with the relation alone, and everything here goes by the name a statement would use
        }
        // a name this pool is accounting for right now is not a leftover, whoever else it may look like one to - and WHICHEVER server it is accounted on. At start there are none, so a start
        // still drops everything it finds; only a server joining later can carry a name that is live, and then the question is not which of the two it is recorded under but whether the pool
        // has it at all. Reading the record here instead would put the whole weight of it on this one drop : a server registered a second time is a new object (addSlave checks nothing), the
        // slots of the first registration name the other one, and their live tables would be dropped - silently, if the owner's connection is restarted before it next touches one, since the
        // migration would then recreate the name empty and record the move. What this costs is a copy that an unfinished move left on this server: it is accounted for elsewhere, so it stays
        // until the next start of the node, and a later move of that slot here fails on the name - loudly, and with every table still where it was
        synchronized (this) {
            for(String slot : slots.keySet())
                tables.remove(slot);
        }
        for(String table : tables)
            SQLSession.dropTemporaryTableFromDB(connection, adapter.syntax, table, OperationOwner.unknown);
        if(!tables.isEmpty())
            ServerLoggers.serviceLogger.info("GLOBAL TEMP TABLE POOL : dropped " + tables.size() + " tables left on " + server.host + " by an earlier run of node " + node);
    }


    // a slot for that shape, registered with its owner and created if it had to be minted, or null to go on with an ordinary temporary table - the job SQLTemporaryPool.getTable does for the
    // tables of one connection. takenBack is a name the asking transaction already has a claim on, so it needs no stamp and nobody else could have taken it
    public String getTable(SQLSession session, Connection connection, TemporaryTableStruct struct, ImOrderSet<KeyField> keys, ImSet<PropertyField> properties,
                           Map<String, WeakReference<TableOwner>> used, Map<String, String> debugInfo, String takenBack, Long startStamp, Result<Boolean> isNew, TableOwner owner, OperationOwner opOwner) throws SQLException {
        String table = takenBack;
        boolean create = false;
        if(table == null) {
            // a slot is a relation of ONE server, so what may be handed out is what is free on the one this session sits on. Every server can hold one : a replica here is an ordinary writable
            // server subscribed to the master, and an unlogged relation is simply never replicated, which is exactly what a scratch table of one node wants
            DataAdapter.Server server = DataAdapter.getServer(connection);

            table = acquire(struct, startStamp, server);
            create = table == null;
            if(create) {
                table = reserveNew(struct, server);
                if(table == null) // the node is at its limit : an ordinary temporary table it is
                    return null;
            }
        }
        isNew.set(create);

        used.put(table, new WeakReference<>(owner)); // before any sql, as in SQLTemporaryPool.getTable
        debugInfo.put(table, owner.getDebugInfo());

        if(!create) // the counterpart of the check in SQLTemporaryPool.getTable, and after the registration for the same reason : it runs sql
            ServerLoggers.assertLog(!Settings.get().isCheckSessionCount() || session.getSessionCount(table, opOwner) == 0, "GLOBAL POOL TABLE SHOULD BE EMPTY AT CACHE : " + table);

        if(create) {
            try {
                session.createGlobalSessionTable(table, keys, properties, opOwner);
            } catch (Throwable t) {
                used.remove(table);
                debugInfo.remove(table);
                kill(table); // the name is burned whether or not the table is there : a slot this process failed to create is never asked about again
                throw ExceptionUtils.propagate(t, SQLException.class);
            }
        }

        if(create)
            CacheStats.incrementMissed(CacheStats.CacheType.TEMP_TABLE);
        else
            CacheStats.incrementHit(CacheStats.CacheType.TEMP_TABLE);
        session.logTempTable(create ? SQLSession.TempTableOperation.MISS : SQLSession.TempTableOperation.HIT, table,
                create ? "reason=no free slot of that shape in the node pool" : (takenBack != null ? "reason=taken back from this transaction" : null));

        return table;
    }

    // a free slot of that shape, or null. A snapshot-based transaction may only take one that was already free when it started : its snapshot is older than a later emptying, so it would still
    // see the previous owner's rows - silently, since nothing about the table says they are there
    private synchronized String acquire(TemporaryTableStruct struct, Long startStamp, DataAdapter.Server server) {
        if(!ready)
            return null;

        Set<String> structFree = free.get(struct);
        if(structFree == null)
            return null;

        for(Iterator<String> iterator = structFree.iterator(); iterator.hasNext(); ) {
            String table = iterator.next();
            Slot slot = slots.get(table);
            if(slot.server.equals(server) && (startStamp == null || slot.freedStamp < startStamp)) {
                iterator.remove();
                if(structFree.isEmpty())
                    free.remove(struct); // an empty set of a shape would otherwise sit there for the life of the node
                return table;
            }
        }
        return null;
    }

    // the name for a slot that is about to be created. It is burned whether or not the creation succeeds : a name this process has ever used physically is never handed out again, which is what
    // makes discarding a slot a complete answer on its own
    private synchronized String reserveNew(TemporaryTableStruct struct, DataAdapter.Server server) {
        if(!ready || slots.size() >= Settings.get().getGlobalTempTablePoolMaxTables())
            return null;

        String table = namePrefix + counter++;
        slots.put(table, new Slot(struct, server, stamp.incrementAndGet()));
        return table;
    }

    // the slot went with its session to another server, its table copied there the way a temporary table's is - and dropped on the one it came from, so a slot's table is on exactly one server
    public synchronized void moved(String table, DataAdapter.Server server) {
        Slot slot = slots.get(table);
        if(slot != null)
            slot.server = server;
    }

    // whether the slot's table is on that server. Servers are compared as the rest of the platform compares them, by identity : they come from the adapter's own list, a connection keeps the
    // object it was opened with, and Server has no equals. It is not watertight - a replica registered a second time is a NEW object, and slots created under the first one go on naming that
    // one, so one database is read as two. Nothing checks for a second registration, which is what makes it reachable. What it costs is a move that is refused and a slot that stays where it
    // is : the sweep deliberately does not read this record, so being wrong about a server no longer takes a live table with it
    public synchronized boolean isOn(String table, DataAdapter.Server server) {
        Slot slot = slots.get(table);
        return slot != null && slot.server.equals(server);
    }

    // the server its table is on, for a session that has to ask for a connection to the same one
    public synchronized DataAdapter.Server getServer(String table) {
        Slot slot = slots.get(table);
        return slot != null ? slot.server : null;
    }

    public synchronized TemporaryTableStruct getStruct(String table) {
        Slot slot = slots.get(table);
        return slot != null ? slot.struct : null;
    }

    // take a free slot back for the owner that gave it up. Outside a transaction a give-back publishes at once, so a return that is being undone (see rollReturnTemporaryTable) has to ask
    // rather than assume : false means the slot is not this owner's to take back - somebody else has it, or it was killed while it was out
    public synchronized boolean reclaim(String table) {
        Slot slot = slots.get(table);
        if(slot == null)
            return false;

        Set<String> structFree = free.get(slot.struct);
        if(structFree == null || !structFree.remove(table))
            return false;
        if(structFree.isEmpty())
            free.remove(slot.struct);
        return true; // the stamp is left as it is : the slot is simply out again, exactly as acquire leaves it
    }

    // a slot goes back to the pool only when its emptying is committed, so its emptiness is dated from here in every case
    public synchronized void release(String table) {
        Slot slot = slots.get(table);
        if(slot == null) // killed while it was out : the name is gone for good, and giving it back is a no-op
            return;

        // a slot is out from the moment it is handed out until it is given back, so it can not already be free : a second release would put a table another owner may already hold back into the
        // free set, which is the one way two owners could end up on one table
        Set<String> structFree = free.get(slot.struct);
        if(structFree != null && structFree.contains(table)) {
            ServerLoggers.assertLog(false, "RELEASE OF A POOL SLOT THAT IS ALREADY FREE : " + table);
            return;
        }

        // a slot that only ever changes hands inside transactions is only ever emptied with DELETE, so its dead rows are never reclaimed and its storage would grow for the life of the node.
        // Past the threshold it is dropped rather than handed on - the pool creates a fresh one, which is the cheap way to reset storage without a lock held until a commit
        if(slot.deadRows >= Settings.get().getDeleteFromInsteadOfTruncateForTempTablesInTransactionThreshold()) {
            kill(table);
            return;
        }

        if(!ready) { // the pool is out : a slot put into the free set now would sit there for the life of the server, taken by nobody and dropped by nothing
            kill(table);
            return;
        }

        slot.freedStamp = stamp.incrementAndGet();
        free.computeIfAbsent(slot.struct, s -> new LinkedHashSet<>()).add(table);
    }

    // the only way a slot can die : the name is forgotten and its table dropped later, outside anybody's transaction. Safe for a name whose table was never created - the drop carries IF EXISTS
    public synchronized void kill(String table) {
        Slot slot = slots.remove(table);
        if(slot == null) // already killed, and already pending : a name the pool never had has no table to drop and nowhere to drop it
            return;

        Set<String> structFree = free.get(slot.struct);
        if(structFree != null) {
            structFree.remove(table);
            if(structFree.isEmpty())
                free.remove(slot.struct);
        }
        pendingDrops.put(table, slot.server);
    }

    // a relation this server created is gone, which nothing here does : somebody outside has removed it - a hand cleaning up unlogged tables, or another server that took this node number and
    // swept it. Either way the pool has no ground left to stand on and goes out. Only a name the pool still OWNS says that : a name already killed is the ordinary way a name stops existing
    public synchronized void notExists(String table) {
        if(slots.containsKey(table) && ready) { // the pool is out for the life of this server : the reason is always something about the node rather than about one slot
            ready = false;
            ServerLoggers.serviceLogger.error("GLOBAL TEMP TABLE POOL DISABLED : TABLE " + table + " NO LONGER EXISTS - THE NODE IDENTITY WAS TAKEN OVER");
        }
        kill(table);
    }

    // on a connection of its own, from the periodic cleaner : these drops must not sit inside anybody's transaction, and a drop can wait out its lock timeout. Fired once per name - what could
    // not be dropped is left as an orphan, which is what the next start of this node sweeps away. It runs even when the pool is disabled, since every name in here is one this server minted
    public void dropPending() {
        Map<DataAdapter.Server, List<String>> dropping = new HashMap<>();
        synchronized (this) {
            if(pendingDrops.isEmpty()) // also the answer for a server that never started the pool, which is the default
                return;
            for(Map.Entry<String, DataAdapter.Server> pending : pendingDrops.entrySet()) // grouped, so that one connection does the drops of one server
                dropping.computeIfAbsent(pending.getValue(), server -> new ArrayList<>()).add(pending.getKey());
            pendingDrops.clear();
        }

        for(Map.Entry<DataAdapter.Server, List<String>> serverDropping : dropping.entrySet()) {
            try {
                adapter.runMaintenance(serverDropping.getKey(), connection -> {
                    setLockTimeout(connection);
                    try (Statement statement = connection.createStatement()) {
                        for(String table : serverDropping.getValue())
                            try {
                                statement.execute("DROP TABLE IF EXISTS " + table);
                            } catch (SQLException e) {
                                ServerLoggers.serviceLogger.info("GLOBAL TEMP TABLE POOL : left " + table + " behind : " + e.getMessage());
                            }
                    }
                });
            } catch (SQLException e) { // the names are already forgotten, so what is left behind is an orphan like any other
                ServerLoggers.serviceLogger.info("GLOBAL TEMP TABLE POOL : left " + serverDropping.getValue().size() + " tables behind on " + serverDropping.getKey().host + " : " + e.getMessage());
            }
        }
    }

    // a drop that waits for a reader gives up rather than holding up the maintenance : what it could not take is an orphan, and the next start of this node sweeps those
    private static void setLockTimeout(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET lock_timeout = '1s'");
        }
    }

    // a name of this pool, of any node : nothing else can be in that schema, so this is an answer about where the relation IS rather than about how it is spelt
    public static boolean isPoolName(String table) {
        return table.startsWith(SCHEMA + ".");
    }

    // every name of every node, as a pg_dump pattern - where * is the wildcard and _ a literal, unlike LIKE. Naming the schema alone would be shorter and is deliberately not done : nothing here
    // relies on the schema being the pool's alone - the sweep drops by node number inside it - and excluding it whole would take an application's own objects out of its backups the day one
    // of them lives in a schema of that name
    public static String getNamePattern() {
        return SCHEMA + ".n*_t_*";
    }
}
