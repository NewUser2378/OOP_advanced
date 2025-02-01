package info.kgeorgiy.ja.kupriyanov.bank;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public class Client {

    public static void main(String[] args) {
        final Bank bank;
        String name;
        String surname;
        String id;
        String accountId ;
        int change;

        try {
            bank = (Bank) Naming.lookup("//localhost:8088/bank");
        } catch (NotBoundException e) {
            System.err.println("not bound exception");
            return;
        } catch (MalformedURLException e) {
            System.err.println("invalid URL");
            return;
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
        try {
            name = args[0];
            surname = args[1];
            id = args[2];
            accountId = args[3];
            change = Integer.parseInt(args[4]);
        } catch (Exception e) {
            System.err.println("You should have 5 args: name, surname, id, account id, change");
            return;
        }
        try {
            Person person = bank.getRemotePerson(id);
            if (person != null) {
                bank.createPerson(name, surname, id);
            } else {
                if (!bank.getPersonAccounts(person).contains(accountId)) {
                    Account account = bank.getAccount(accountId, person);
                    if (account != null) {
                        System.err.println("not new account");
                        return;
                    } else {
                        bank.createAccount(accountId, person);
                    }
                }
            }
            Account account = bank.getAccount(accountId, person);
            System.out.println("current account information: id= " + account.getId() + "balance=: " + account.getAmount());
            account.setAmount(account.getAmount() + change);
            System.out.println("balance changed: " + account.getAmount());
        } catch (RemoteException e) {
            System.err.println("RemoteException: " + e.getMessage());
        }

    }
}