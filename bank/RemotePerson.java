package info.kgeorgiy.ja.kupriyanov.bank;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class RemotePerson extends AbstractPerson {
    public RemotePerson(
            final String name,
            final String surname,
            final String passportId,
            final int port
    ) throws RemoteException {
        super(name, surname, passportId);
        UnicastRemoteObject.exportObject(this, port);
    }

}