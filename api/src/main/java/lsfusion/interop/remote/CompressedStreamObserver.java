package lsfusion.interop.remote;

public interface CompressedStreamObserver {

    void bytesReaden(long in);
    
    void bytesWritten(long out);
}
