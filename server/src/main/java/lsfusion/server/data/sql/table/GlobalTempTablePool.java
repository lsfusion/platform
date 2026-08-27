package lsfusion.server.data.sql.table;

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
import java.util.HashSet;
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

    private long counter;
    // ordered against the start of a transaction, so a snapshot-based one can tell whether a free slot was already empty when it took its snapshot
    private final AtomicLong stamp = new AtomicLong();

    // everything the pool knows about one of its tables, from the moment its name is minted until it is killed
    private static class Slot {
        private final TemporaryTableStruct struct;
        private long freedStamp; // the moment its emptiness dates from - ordered against the start of a transaction, so a snapshot based one can tell whether it was already empty when it started
        private long deadRows; // what the emptyings left behind since its storage was last reset - the node's count, not any one connection's, since the slot moves between them

        private Slot(TemporaryTableStruct struct, long freedStamp) {
            this.struct = struct;
            this.freedStamp = freedStamp; // never zero, so that a slot can not reach the free set dated before every transaction there is
        }
    }
    private final Map<String, Slot> slots = new HashMap<>();
    private final Map<TemporaryTableStruct, Set<String>> free = new HashMap<>(); // by shape : the names anybody may take
    private final Set<String> pendingDrops = new HashSet<>(); // a set for the idempotence of kill, not for any order

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
                int node = adapter.getNodeId();
                if(node < 0) {
                    ServerLoggers.serviceLogger.error("GLOBAL TEMP TABLE POOL : this server has no node number, working without the pool");
                    return false;
                }

                namePrefix = SCHEMA + ".n" + node + "_t_";

                // all or nothing : anything left behind still holds the rows of the last run, so the pool stays out unless the sweep finished
                adapter.runMaintenance(connection -> dropOwnTables(connection, node));

                ready = true;
                ServerLoggers.serviceLogger.info("GLOBAL TEMP TABLE POOL READY : node " + node + ", names " + namePrefix + "*");
            } catch (Throwable t) { // the pool is optional, so nothing about it takes the server down - and the failure is logged with its stack rather than swallowed
                ServerLoggers.serviceLogger.error("GLOBAL TEMP TABLE POOL : could not start, working without it", t);
            }
            return ready;
        }
    }

    // whatever this node left behind the last time it ran : the pool creates its tables lazily, so there is nothing to keep and nothing to check the shape of
    private void dropOwnTables(Connection connection, int node) throws SQLException {
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
        for(String table : tables)
            SQLSession.dropTemporaryTableFromDB(connection, adapter.syntax, table, OperationOwner.unknown);
        if(!tables.isEmpty())
            ServerLoggers.serviceLogger.info("GLOBAL TEMP TABLE POOL : dropped " + tables.size() + " tables left by an earlier run of node " + node);
    }

    // a free slot of that shape, or null. A snapshot-based transaction may only take one that was already free when it started : its snapshot is older than a later emptying, so it would still
    // see the previous owner's rows - silently, since nothing about the table says they are there
    public synchronized String acquire(TemporaryTableStruct struct, Long startStamp) {
        if(!ready)
            return null;

        Set<String> structFree = free.get(struct);
        if(structFree == null)
            return null;

        for(Iterator<String> iterator = structFree.iterator(); iterator.hasNext(); ) {
            String table = iterator.next();
            if(startStamp == null || slots.get(table).freedStamp < startStamp) {
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
    public synchronized String reserveNew(TemporaryTableStruct struct) {
        if(!ready || slots.size() >= Settings.get().getGlobalTempTablePoolMaxTables())
            return null;

        String table = namePrefix + counter++;
        slots.put(table, new Slot(struct, stamp.incrementAndGet()));
        return table;
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
        if(slot != null) {
            Set<String> structFree = free.get(slot.struct);
            if(structFree != null) {
                structFree.remove(table);
                if(structFree.isEmpty())
                    free.remove(slot.struct);
            }
        }
        pendingDrops.add(table);
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
        List<String> dropping;
        synchronized (this) {
            if(pendingDrops.isEmpty()) // also the answer for a server that never started the pool, which is the default
                return;
            dropping = new ArrayList<>(pendingDrops);
            pendingDrops.clear();
        }

        try {
            adapter.runMaintenance(connection -> {
                setLockTimeout(connection);
                try (Statement statement = connection.createStatement()) {
                    for(String table : dropping)
                        try {
                            statement.execute("DROP TABLE IF EXISTS " + table);
                        } catch (SQLException e) {
                            ServerLoggers.serviceLogger.info("GLOBAL TEMP TABLE POOL : left " + table + " behind : " + e.getMessage());
                        }
                }
            });
        } catch (SQLException e) { // the names are already forgotten, so what is left behind is an orphan like any other
            ServerLoggers.serviceLogger.info("GLOBAL TEMP TABLE POOL : left " + dropping.size() + " tables behind : " + e.getMessage());
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
