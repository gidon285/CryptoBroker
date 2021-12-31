package com.example.cryptotrader;

import androidx.annotation.NonNull;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.account.Order;
import com.binance.api.client.domain.account.request.OrderRequest;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.GenericTypeIndicator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

public class ctAccount {
    private final Executor executor;
    ctCredentials credentials;
    String clientName;
    private ArrayList<String> resultSync = new ArrayList<>();
    private List<Order> resultOrdersSync = new ArrayList<>();
    private ArrayList<Order> result = new ArrayList<>();

    public ctAccount(Executor executor, ctCredentials otherCredentials, String name) {
        this.executor = executor;
        credentials = otherCredentials;
        clientName = name;
    }

    /**
     * -= Async Task May cause inconsistent performance when used incorrectly =-
     * Returns a list of current client names as String.
     * This method is for use on spinners and other methods that require a String representation
     * of client name.
     *
     * @param db FireBase DB object of the current connected user
     * @param user FireBase user object of the current connected user
     * @return ArrayList of name representing the current user clients
     */
    public static ArrayList<String> getClientNamesListAsync(DatabaseReference db, FirebaseUser user, String firstObject) {
        ArrayList<String> result = new ArrayList<>();
        result.add(firstObject);
        db.child(user.getUid()).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if(task.isSuccessful()) {
                    GenericTypeIndicator<HashMap<String, ctCredentials>> gType =
                            new GenericTypeIndicator<HashMap<String,  ctCredentials>>() {};
                    HashMap<String, ctCredentials> map = task.getResult().getValue(gType);
                    try {
                        map.forEach((clientName, clientTokens) -> result.add(clientName));
                    } catch (Exception e) {
                        System.out.println(e.toString());
                        result.clear();
                    }
                }
            }
        });
        return result;
    }

    /**
     * -= Sync Task Requires thread management =-
     * Returns a list of current client names as String.
     * This method is for use on spinners and other methods that require a String representation
     * of client name.
     *
     * @param db FireBase DB object of the current connected user
     * @param user FireBase user object of the current connected user
     * @return ArrayList of name representing the current user clients
     */
    public ArrayList<String> getClientNamesList(DatabaseReference db, FirebaseUser user) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                resultSync.add("All");
                db.child(user.getUid()).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DataSnapshot> task) {
                        if(task.isSuccessful()) {
                            GenericTypeIndicator<HashMap<String, ctCredentials>> gType =
                                    new GenericTypeIndicator<HashMap<String,  ctCredentials>>() {};
                            HashMap<String,  ctCredentials> map = task.getResult().getValue(gType);
                            map.forEach((clientName, clientTokens) -> resultSync.add(clientName));
                        }
                    }
                });
            }
        });
        return resultSync;
    }

    /**
     * -= Async Task May cause inconsistent performance when used incorrectly =-
     * Returns a List of BinanceAPI Order objects the are currently open for a specific requested
     * user (via username).
     *
     * @param clientName The current client name as shown on a spinner for example
     * @param accountsDB FireBase DB object of the current connected user
     * @param user FireBase user object of the current connected user
     * @return List of BinanceAPI Order objects
     */
    public List<Order> getAllOpenOrdersList(String clientName, DatabaseReference accountsDB, FirebaseUser user) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                accountsDB.child(user.getUid()).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DataSnapshot> task) {
                        if(task.isSuccessful()) {
                            GenericTypeIndicator<HashMap<String, ctCredentials>> gType =
                                    new GenericTypeIndicator<HashMap<String,  ctCredentials>>() {};
                            HashMap<String,  ctCredentials> map = task.getResult().getValue(gType);
                            map.forEach((clientName, clientTokens) ->
                            {
                                if (clientName.equals(clientName)) {
                                    BinanceApiClientFactory factory = BinanceApiClientFactory
                                            .newInstance(clientTokens.getKey(), clientTokens.getSecret());
                                    BinanceApiRestClient client = factory.newRestClient();
                                    resultOrdersSync = client.getOpenOrders(new OrderRequest(null));
                                }
                            });
                        }
                    }
                });
            }
        });
        return resultOrdersSync;
    }
}
