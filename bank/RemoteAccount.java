package info.kgeorgiy.ja.kupriyanov.bank;

import java.rmi.RemoteException;

public class RemoteAccount extends AbstractAccount {
    public RemoteAccount(final String id, int port) throws RemoteException {
        super(id, 0);
    }

}