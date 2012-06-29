package equ.srv;

import equ.srv.EquipmentServer;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface EquipmentServerHolder extends Remote {
    
    EquipmentServer getEquipmentServer() throws RemoteException;
}
