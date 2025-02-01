package info.kgeorgiy.ja.kupriyanov.bank;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Set;


public interface Bank extends Remote {
    Account getAccount(final String id, final Person person) throws RemoteException;

    Person getRemotePerson(final String id) throws RemoteException;

    Set<String> getPersonAccounts(Person person) throws RemoteException;
    Person getLocalPerson(final String id) throws RemoteException;

    boolean createAccount(final String id,final  Person person) throws RemoteException;


    boolean createPerson(final String name, final String surname, final String id) throws RemoteException;

    boolean searchPerson(final String name, final String surname, final String id) throws RemoteException;

}