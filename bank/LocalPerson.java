package info.kgeorgiy.ja.kupriyanov.bank;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

public class LocalPerson extends AbstractPerson implements Serializable {

    private final Map<String, LocalAccount> localAccs;

    public LocalPerson(final String name, final String surname, final String passportID, final Map<String, LocalAccount> localAccs) {
        super(name, surname, passportID);
        this.localAccs = localAccs;
    }

    Set<String> getAccounts() {
        return localAccs.keySet();
    }

    Account getAccountById(String id) {
        return localAccs.get(id);
    }

}