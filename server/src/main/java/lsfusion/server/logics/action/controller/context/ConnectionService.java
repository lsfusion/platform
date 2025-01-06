package lsfusion.server.logics.action.controller.context;

import com.google.common.base.Throwables;
import lsfusion.base.Pair;
import org.xBaseJ.DBF;

import java.io.IOException;
import java.net.Socket;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class ConnectionService {
    Map<String, Connection> sqlConnectionMap;
    Map<String, DBF> dbfFilesMap;
    Map<Pair<String, Integer>, Socket> tcpSocketMap;

    public ConnectionService() {
        this.sqlConnectionMap = new HashMap<>();
        this.dbfFilesMap = new HashMap<>();
        this.tcpSocketMap = new HashMap<>();
    }

    public Connection getSQLConnection(String connectionString) {
        return sqlConnectionMap.get(connectionString);
    }

    public void putSQLConnection(String connectionString, Connection connection) {
        sqlConnectionMap.put(connectionString, connection);
    }

    public void closeSQLConnections() {
        for(Connection connection : sqlConnectionMap.values()) {
            try {
                connection.close();
            } catch (SQLException e) {
                throw Throwables.propagate(e);
            }
        }
        sqlConnectionMap.clear();
    }

    public DBF getDBFFile(String connectionString) {
        return dbfFilesMap.get(connectionString);
    }

    public void putDBFFile(String connectionString, DBF dbfFile) {
        dbfFilesMap.put(connectionString, dbfFile);
    }

    public void closeDBFFiles() {
        for(DBF dbfFile : dbfFilesMap.values()) {
            try {
                dbfFile.close();
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
        }
        dbfFilesMap.clear();
    }

    public Socket getTCPSocket(String host, Integer port) {
        return tcpSocketMap.get(Pair.create(host, port));
    }

    public void putTCPSocket(String host, Integer port, Socket socket) {
        tcpSocketMap.put(Pair.create(host, port), socket);
    }

    public void closeTCPSockets() {
        for(Socket socket : tcpSocketMap.values()) {
            try {
                socket.close();
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
        }
        tcpSocketMap.clear();
    }

    public void close() {
        closeSQLConnections();
        closeDBFFiles();
        closeTCPSockets();
    }
}
