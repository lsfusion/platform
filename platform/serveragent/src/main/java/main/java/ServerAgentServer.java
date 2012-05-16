package main.java;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.sql.SQLException;

public class ServerAgentServer {

    private Thread thread;

    protected final static Logger logger = Logger.getLogger(ServerAgentServer.class);

    private static Registry registry;

    public ServerAgentServer(final int port) {

        thread = new Thread(new Runnable() {

            @Override
            public void run() {

                int millis = 10000;

                try {
                    initRMIRegistry(port);
                } catch (Exception e) {
                    logger.error("Unhandled exception : ", e);
                }

                while (true) {

                    try {
                        logger.info("Server Agent is working");

                    } catch (Exception e) {
                        logger.error("Unhandled exception : ", e);
                    }

                    try {
                        Thread.sleep(millis);
                    } catch (InterruptedException e) {
                        logger.info("Thread has been interrupted : ", e);
                        break;
                    }
                }
            }
        });

        thread.start();
    }

    private static void initRMIRegistry(int port) throws IOException, ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
        registry = LocateRegistry.getRegistry(port);
        try {
            registry.list();
        } catch (RemoteException e) {
            registry = LocateRegistry.createRegistry(port);
        }
        try {
            registry.bind("ServerAgentLoader", new ServerAgentLoader());
        } catch (AlreadyBoundException e2) {
            throw new RuntimeException("The service is already started");
        }
    }

    public void stop() {
        thread.interrupt();
    }
}