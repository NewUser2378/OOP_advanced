package info.kgeorgiy.ja.kupriyanov.bank;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class BankTest {
    private static Bank bank;
    private static String personId = "test_person";
    private static final  String surname="surname";

    @BeforeClass
    public static void setUp() throws RemoteException, NotBoundException {
        final Registry registry = LocateRegistry.createRegistry(8088);
        registry.rebind("//localhost/bank", new RemoteBank(8889));
        bank = (Bank) registry.lookup("//localhost/bank");
    }



    @Test
    public void testGettingAccounts() throws RemoteException {
        int personIndex = 0;
        int accountIndex = 0;

        while (personIndex < 10) {
            String personId = "getAccounts" + personIndex;
            Assert.assertTrue(bank.createPerson(personId, Integer.toString(personIndex), personId));

            int numAccountsToCreate = personIndex + 1;
            int accountsCreated = 0;
            Person remotePerson = bank.getRemotePerson(personId);

            while (accountsCreated < numAccountsToCreate) {
                String accountId = "Account" + personIndex + "_" + accountIndex;
                if (bank.createAccount(accountId, remotePerson)) {
                    accountsCreated++;
                }
                accountIndex++;
            }
            Set<String> personAccounts = bank.getPersonAccounts(remotePerson);
            Assert.assertNotNull(personAccounts);
            Assert.assertEquals(numAccountsToCreate, personAccounts.size());
            personIndex++;
        }
    }


    @Test
    public void testSearch() throws RemoteException {
        personId += "new";
        Assert.assertFalse(bank.searchPerson(personId, surname, personId));
        Assert.assertTrue(bank.createPerson(personId, surname, personId));
        Assert.assertTrue(bank.searchPerson(personId, surname, personId));
    }

    @Test
    public void testGettingPerson() throws RemoteException {
        int personIndex = 0;
        while ( personIndex < 10 ) {
            String personId = "person" + personIndex;
            bank.createPerson(personId, surname, personId);

            Person remotePerson = bank.getRemotePerson(personId);
            Assert.assertEquals(personId, remotePerson.getName());
            Assert.assertEquals(surname, remotePerson.getSurname());
            Assert.assertEquals(personId, remotePerson.getPassportID());

            Person localPerson = bank.getLocalPerson(personId);
            Assert.assertEquals(personId, localPerson.getName());
            Assert.assertEquals(surname, localPerson.getSurname());
            Assert.assertEquals(personId, localPerson.getPassportID());
            ++personIndex;
        }
    }

    @Test
    public void testCreatingLocalAccount() throws RemoteException {
        String personId = "local" ;
        String accountId = "2";
        //испарвил, теперь не обращаемся в банк
        Map<String, LocalAccount> localAccs = new HashMap<>();
        LocalPerson localPerson = new LocalPerson(personId, surname, personId, localAccs);

        Assert.assertTrue(localPerson.getAccounts().isEmpty());
        LocalAccount newAccount = new LocalAccount(accountId, 0);
        localAccs.put(accountId, newAccount);

        Assert.assertTrue(localPerson.getAccounts().contains(accountId));
        Assert.assertEquals(newAccount, localPerson.getAccountById(accountId));
        Assert.assertEquals(0, newAccount.getAmount());
    }

    @Test
    public void testCreatingRemoteAccount() throws RemoteException {
        bank.createPerson(personId, surname , personId);
        Person remotePerson = bank.getRemotePerson(personId);
        bank.createAccount("2", remotePerson);
        Assert.assertEquals(1, bank.getPersonAccounts(remotePerson).size());
        Assert.assertNotNull(bank.getAccount("2", remotePerson));
    }


    @Test
    public void testAccountBalanceOperations() throws RemoteException {
        String personId = "balance_test";
        bank.createPerson(personId, surname , personId);
        Person remotePerson = bank.getRemotePerson(personId);
        String accountId = "account_test";

        bank.createAccount(accountId, remotePerson);
        Account account = bank.getAccount(accountId, remotePerson);
        Assert.assertNotNull(account);

        account.setAmount(1000);
        Assert.assertEquals(1000, account.getAmount());

        account.setAmount(account.getAmount() - 500);
        Assert.assertEquals(500, account.getAmount());
    }

    //добавил тест с неверными данными
    @Test
    public void testGettingPersonWithInvalidData() throws RemoteException {
        String invalidPersonId = personId + " invalid";
        bank.createPerson(invalidPersonId, "invalid " + surname, "invalidPassportId");
        Assert.assertNull(bank.getRemotePerson(invalidPersonId));
        Person nonExistentPerson = bank.getRemotePerson(invalidPersonId);
        Assert.assertNull(nonExistentPerson);
        Person localNonExistentPerson = bank.getLocalPerson(invalidPersonId);
        Assert.assertNull(localNonExistentPerson);
    }


    @AfterClass
    public static void tearDown() throws RemoteException, NotBoundException {
        Registry registry = LocateRegistry.getRegistry(8088);
        registry.unbind("//localhost/bank");
    }

    // :NOTE: исправил
    // 1) Fix test: add incorrect data test
    // 2) localPerson не ходит в банк (создавать аккаунты локально)
    // 3) push libs

    @Test
    public void testConcurrentAccountOperations() throws RemoteException, InterruptedException {
        String personId = "concurrent_test";
        bank.createPerson(personId, surname, personId);
        Person remotePerson = bank.getRemotePerson(personId);
        String accountId = "concurrent_account";
        bank.createAccount(accountId, remotePerson);
        Account account = bank.getAccount(accountId, remotePerson);
        Assert.assertNotNull(account);

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.execute(() -> {
                try {
                    for (int j = 0; j < 100; j++) {
                        synchronized (account) {
                            account.setAmount(account.getAmount() + 10);
                            account.setAmount(account.getAmount() - 10);
                        }
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(1, TimeUnit.MINUTES);
        executor.shutdown();
        Assert.assertEquals(0, account.getAmount());
    }

}