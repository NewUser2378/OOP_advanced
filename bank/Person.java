package info.kgeorgiy.ja.kupriyanov.bank;

import java.rmi.Remote;
import java.rmi.RemoteException;


public interface Person extends Remote {
    String getName() throws RemoteException;

    String getSurname() throws RemoteException;

    String getPassportID() throws RemoteException;

}