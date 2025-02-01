package info.kgeorgiy.ja.kupriyanov.bank;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;


public class Server {
    private final static int PORT = 8088;

    public static void main(String[] args) {
        try {
            final Bank bank = new RemoteBank(PORT);
            Naming.rebind("//localhost:8088/bank", bank);
        } catch (RemoteException e) {
            System.err.println("Cannot export object: " + e.getMessage());
        } catch (MalformedURLException e) {
            System.err.println("Malformed URL");
        }
    }
}