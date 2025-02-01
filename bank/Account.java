package info.kgeorgiy.ja.kupriyanov.bank;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Account extends Remote {

    String getId() throws RemoteException;


    int getAmount() throws RemoteException;

    void setAmount(int amount) throws RemoteException;


}