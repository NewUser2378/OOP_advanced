package info.kgeorgiy.ja.kupriyanov.bank;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class RemoteBank extends UnicastRemoteObject implements Bank {
    private final int port;
    private final ConcurrentMap<String, Person> persons = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Account> accounts = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Set<String>> accountsByPassportID = new ConcurrentHashMap<>();

    RemoteBank(int port) throws RemoteException {
        super(port);
        this.port = port;
    }
    private boolean isValidPassportId(String passportId) {
        return passportId != null && persons.containsKey(passportId);
    }
    private boolean isValidInput(String accId, Person person) {
        return accId != null && person != null;
    }

    private String generateAccountId(Person person, String accId) throws RemoteException {
        return person.getPassportID() + ":" + accId;
    }

    private boolean accountExists(String accountId) {
        return accounts.containsKey(accountId);
    }

    private void createAndStoreAccount(String accId, String accountId) throws RemoteException {
        Account account = new RemoteAccount(accId, port);
        accounts.put(accountId, account);

    }

    private void addToPassportIDMap(Person person, String accId) throws RemoteException {
        accountsByPassportID.computeIfAbsent(person.getPassportID(), k -> new ConcurrentSkipListSet<>()).add(accId);
    }

    private boolean isValidInput(String name, String surname, String passportId) {
        return name != null && surname != null && passportId != null;
    }

    private boolean isNewPassport(String passportId) {
        return !persons.containsKey(passportId);
    }

    private void initializeAccountSet(String passportId) {
        accountsByPassportID.put(passportId, new ConcurrentSkipListSet<>());
    }

    private boolean personMatchesCriteria(Person person, String name, String surname) throws RemoteException {
        return person.getName().equals(name) && person.getSurname().equals(surname);
    }

    private boolean isNullPerson(Person person) {
        return person==null;
    }




    @Override
    public Account getAccount(String accId, Person person) throws RemoteException {
        if (isValidInput(accId, person)) {
            String accountId = generateAccountId(person, accId);
            Account account = accounts.get(accountId);

            if (account != null) {
                if (person instanceof LocalPerson) {
                    return ((LocalPerson) person).getAccountById(accId);
                } else {
                    return account;
                }
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    @Override
    public boolean createPerson(String name, String surname, String passportId) throws RemoteException {
        if (isValidInput(name, surname, passportId) && isNewPassport(passportId)) {
            RemotePerson newPerson = new RemotePerson(name, surname, passportId, port);
            persons.put(passportId, newPerson);
            initializeAccountSet(passportId);
            return true;
        } else {
            return false;
        }
    }


    @Override
    public boolean searchPerson(String name, String surname, String passportId) throws RemoteException {
        if (isValidInput(name, surname, passportId)) {
            Person person = persons.get(passportId);
            return person != null && personMatchesCriteria(person, name, surname);
        } else {
            return false;
        }
    }
    @Override
    public Person getLocalPerson(String passportId) throws RemoteException {
        if (isValidPassportId(passportId)) {
            Person person = persons.get(passportId);
            if (isNullPerson(person)) {
                return null;
            } else {
                Map<String, LocalAccount> localAccounts = new ConcurrentHashMap<>();
                Set<String> personAccountIds = getPersonAccounts(person);
                personAccountIds.forEach((accountId) -> {
                    try {
                        Account remoteAccount = getAccount(accountId, person);
                        if (remoteAccount != null) {
                            LocalAccount localAccount = new LocalAccount(remoteAccount.getId(), remoteAccount.getAmount());
                            localAccounts.put(accountId, localAccount);
                        } else {
                            System.err.println("Not found account with id:  " + accountId);
                        }
                    } catch (RemoteException e) {
                        System.err.println("error with creating account " + e.getMessage());
                    }
                });

                return new LocalPerson(person.getName(), person.getSurname(), person.getPassportID(), localAccounts);

            }
        } else {
            return null;
        }
    }

    public boolean createAccount(String accId, Person person) throws RemoteException {
        if (isValidInput(accId, person)) {
            String accountId = generateAccountId(person, accId);
            if (!accountExists(accountId)) {
                createAndStoreAccount(accId, accountId);
                addToPassportIDMap(person, accId);
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
    @Override
    public Person getRemotePerson(String passportId) throws RemoteException {
        if (isValidPassportId(passportId)) {
            return persons.get(passportId);
        } else {
            return null;
        }
    }

    @Override
    public Set<String> getPersonAccounts(Person person) throws RemoteException {
        if (isNullPerson(person )) {
            return null;
        } else {
            if (person instanceof LocalPerson) {
                return ((LocalPerson) person).getAccounts();
            } else {
                String passportId = person.getPassportID();
                return accountsByPassportID.get(passportId);
            }
        }
    }
}