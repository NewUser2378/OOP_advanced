package info.kgeorgiy.ja.kupriyanov.bank;

public abstract class AbstractAccount implements Account {
    protected final String id;
    protected int amount;

    public AbstractAccount(final String id, final int amount) {
        this.id = id;
        this.amount = amount;
    }

    public String getId() {
        return id;
    }

    public synchronized int getAmount() {
        return amount;
    }

    public synchronized void setAmount(final int amount) {
        this.amount = amount;
    }
}