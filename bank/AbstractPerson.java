package info.kgeorgiy.ja.kupriyanov.bank;

import java.rmi.RemoteException;

public abstract class AbstractPerson implements Person {
    protected String name;
    protected String surname;
    protected String passportId;

    public AbstractPerson(final String name, final String surname, final String passportId) {
        this.name = name;
        this.surname = surname;
        this.passportId = passportId;
    }

    @Override
    public String getName() throws RemoteException {
        return this.name;
    }

    @Override
    public String getSurname() throws RemoteException {
        return this.surname;
    }

    @Override
    public String getPassportID() throws RemoteException {
        return this.passportId;
    }
}