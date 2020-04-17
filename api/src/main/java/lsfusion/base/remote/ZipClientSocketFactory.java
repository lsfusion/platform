package lsfusion.base.remote;

import lsfusion.base.BaseUtils;

import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.rmi.server.RMIClientSocketFactory;

public class ZipClientSocketFactory extends ZipSocketFactory implements RMIClientSocketFactory, Serializable, CompressedStreamObserver {

    // the problem is that RMI architecture assumes that hostname comes from server (from RMI registry) with remote object in TCPEndpoint
    // but in real life server address is client specific (depending on network architecture)
    // so we will override this behaviour assuming that every call that can return RemoteObject should be wrapped with setting realHostName in thread local
    // (i.e.: rmiLookup + every remote call that might create another remote object, see RemoteObjectProxy.RealHostNameAspect)
    public static ThreadLocal<String> threadRealHostName = new ThreadLocal<>();

    public ZipClientSocketFactory(String realHostName) {
        this.realHostName = realHostName;
    }

    // socket factory for object exporting (including rmi registry)
    public final static ZipClientSocketFactory export = new ZipClientSocketFactory();

    // for deserialization
    public ZipClientSocketFactory() {
    }

    private String realHostName;

    public CountZipSocket createSocket(String host, int port) throws IOException {
        if(realHostName != null)
            host = realHostName;
        CountZipSocket socket = new CountZipSocket(host, port);
        socket.setObserver(this);
        if(timeout != null)
            socket.setSoTimeout(timeout);
        return socket;
    }

    private void writeObject(java.io.ObjectOutputStream out)
            throws IOException {
        assert this == export;
        out.defaultWriteObject();
    }
    private void readObject(java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        assert this != export;
        in.defaultReadObject();

        String hostName = threadRealHostName.get();
        if (hostName == null) //fails on login getAndCheckServerSettings
            hostName = "localhost";//throw new RuntimeException("Real host name should be provided in ZipClientSocketFactory.threadRealHostName");
        this.realHostName = hostName;
    }
    private void readObjectNoData() {
        throw new UnsupportedOperationException();
    }

    public static volatile transient long inSum;
    public static volatile transient long outSum;
    public static volatile transient Integer timeout;

    public void bytesReaden(long in) {
        inSum += in;
    }

    public void bytesWritten(long out) {
        outSum += out;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof ZipClientSocketFactory && BaseUtils.nullEquals(realHostName, ((ZipClientSocketFactory) o).realHostName);
    }

    @Override
    public int hashCode() {
        return realHostName != null ? realHostName.hashCode() : 0;
    }
}
