package com.example.cryptotrader;

import static com.example.cryptotrader.App.CHANNEL_1_ID;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.app.Dialog;
import android.app.Notification;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.binance.api.client.BinanceApiAsyncRestClient;
import com.binance.api.client.BinanceApiCallback;
import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.domain.account.Order;
import com.binance.api.client.domain.account.request.CancelOrderRequest;
import com.binance.api.client.domain.account.request.CancelOrderResponse;
import com.binance.api.client.domain.account.request.OrderRequest;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Cancel order Activity is used to close part or all of the orders that are open on the binance
 * exchange in realtime. with a toggleable button the user can chose which Account to search in and
 * then look through the active orders, then cancel them one by one, or all at the same time.
 */
public class CancelOrderActivity extends AppCompatActivity implements View.OnClickListener {
    private NotificationManagerCompat notificationManager;
    private DatabaseReference accountsDB;
    private FirebaseUser user;
    private Button cancelOrderButton, emergencyButton;
    private Spinner clientSpinner, openOrdersSpinner;
    private ArrayList<String> clientsList = new ArrayList<>();
    private ArrayList<String> normalizedOrderList;
    private ProgressBar progressBar;
    private Dialog myDialog;
    private TextView inputMessage, popupTopic;
    private ImageView popupImage,profile;

    /**
     * on create to initialize all of the components this screen require, listeners etc'. setting the default option in
     * the spinner to None for future uses.
     * @param savedInstanceState all of the open orders that are represented as a bundle object
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        notificationManager = NotificationManagerCompat.from(this);
        setContentView(R.layout.activity_cancel_order);
        clientSpinner = findViewById(R.id.clientSpinner);
        openOrdersSpinner = findViewById(R.id.openOrdersSpinner);
        user = FirebaseAuth.getInstance().getCurrentUser();
        accountsDB = FirebaseDatabase.getInstance().getReference("Accounts");
        ArrayList<String> clientsList = ctAccount.getClientNamesListAsync(accountsDB, user, "None");
        ArrayAdapter<String> clientsAdapter = new ArrayAdapter<>(this, android.R.layout
                .simple_spinner_dropdown_item, clientsList);
        clientSpinner.setAdapter(clientsAdapter);
        createClientsSpinner();
        cancelOrderButton = findViewById(R.id.sendCancelOderButton);
        emergencyButton = findViewById(R.id.emergencyButton);
        progressBar = findViewById(R.id.progressBar);
        myDialog = new Dialog(this);
        profile = findViewById(R.id.profile);
        profile.setOnClickListener(this);
        setStatusBarColor();
    }

    /**
     * on click used for passing to the Profile screen, mainly for Menu purposes.
     * @param view
     */
    @Override
    public void onClick(View view) {
        if(view.getId() == R.id.profile){
            startActivity(new Intent(CancelOrderActivity.this, ProfileActivity.class));
        }
    }

    public ArrayList<String> normalizeOrderListForSpinner(List<Order> beforeList) {
        ArrayList<String> result = new ArrayList<>();
        for (Order order : beforeList) {
            String tmpOrder = order.getSymbol() + " " + order.getType() + " "  + order.getPrice();
            result.add(tmpOrder);
        }
        if (result.size() < 1) {
            result.add("None");
        }
        return result;
    }

    /**
     * Utilize the main purpose of the screen. first we set a listener for the spinner that is represents the Account
     * in which the user chooses to cancel an order. each method is backed by firebase, meaning we check to get the users
     * accounts from firebase. then we loop on all of the data and find the credentials that are need for the account.
     * with those we make an API request to Binance to get all open orders, when the users chooses the order he desires
     * we send another API request to Binance to cancel the order the user has chose, notify the user via notification and
     * a popup screen for success or failure(via showOrderPopup).
     */
    void createClientsSpinner() {
        clientSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String chosenUser = clientSpinner.getSelectedItem().toString();
                accountsDB.child(user.getUid()).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DataSnapshot> task) {
                        if(task.isSuccessful()) {
                            GenericTypeIndicator<HashMap<String, ctCredentials>> gType =
                                    new GenericTypeIndicator<HashMap<String,  ctCredentials>>() {};
                            HashMap<String,  ctCredentials> map = task.getResult().getValue(gType);
                            map.forEach((clientNameItr, clientTokens) ->
                            {
                                if (clientNameItr.equals(chosenUser)) {
                                    BinanceApiClientFactory factory = BinanceApiClientFactory
                                            .newInstance(clientTokens.getKey(), clientTokens.getSecret());
                                    BinanceApiAsyncRestClient client = factory.newAsyncRestClient();
                                    client.getOpenOrders(new OrderRequest(null), new BinanceApiCallback<List<Order>>() {
                                        @Override
                                        public void onResponse(List<Order> orders) {
                                            ArrayList<Order> orderList = new ArrayList(orders);
                                            ArrayAdapter<String> ordersAdapter = new ArrayAdapter(CancelOrderActivity.this, android.R.layout
                                                    .simple_spinner_dropdown_item, normalizeOrderListForSpinner(orderList));
                                            openOrdersSpinner.setAdapter(ordersAdapter);
                                            cancelOrderButton.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View view) {
                                                    if(!openOrdersSpinner.getSelectedItem().toString().equals("None"))
                                                    {
                                                        String[] order = openOrdersSpinner.getSelectedItem().toString().trim().split(" ");
                                                        Long oid = Long.parseLong(order[0].substring(1, order[0].length() - 1).trim());
                                                        String symbol = order[1].trim();
                                                        client.cancelOrder(new CancelOrderRequest(symbol, oid), new BinanceApiCallback<CancelOrderResponse>() {
                                                            @Override
                                                            public void onResponse(CancelOrderResponse cancelOrderResponse) {
                                                                showOrderPopup("Success"
                                                                        , "Order: \n " + cancelOrderResponse.getOrderId() + "\n"
                                                                                + cancelOrderResponse.getSymbol() + "\nCancelled");
                                                                Notification notification = new NotificationCompat.Builder(CancelOrderActivity.this, CHANNEL_1_ID)
                                                                        .setSmallIcon(R.drawable.notification_icon)
                                                                        .setContentTitle("Order Sent!")
                                                                        .setContentText("Order: \n " + cancelOrderResponse.getOrderId() + "\n"
                                                                                + cancelOrderResponse.getSymbol() + "\nCancelled")
                                                                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                                                        .setCategory(NotificationCompat.CATEGORY_EVENT)
                                                                        .build();

                                                                notificationManager.notify(1, notification);
                                                            }

                                                            @Override
                                                            public void onFailure(Throwable cause) {
                                                                try {
                                                                    showOrderPopup("Error", cause.toString().split(":")[1]);
                                                                    throw cause;
                                                                } catch (Throwable throwable) {
                                                                    throwable.printStackTrace();
                                                                }
                                                            }
                                                        });
                                                    }
                                                    else{
                                                        Toast.makeText(CancelOrderActivity.this, "No Orders to cancel!", Toast.LENGTH_LONG).show();
                                                    }
                                                }
                                            });
                                        }

                                        @Override
                                        public void onFailure(Throwable cause) {
                                            try {
                                                Toast.makeText(CancelOrderActivity.this, "Couldn't retrieve Open Orders. Please try again.", Toast.LENGTH_LONG).show();
                                                throw cause;
                                            } catch (Throwable throwable) {
                                                throwable.printStackTrace();
                                            }
                                        }
                                    });
                                }
                            });
                        }
                    }
                });


            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        if (normalizedOrderList != null) {
            showOpenOrders();
        }
    }

    /**
     * A simple parsing method for Strings, to not clutter other methods.
     * @param beforeList given Arraylist of strings.
     * @return result, a parsed Arraylist of strings.
     */
    public ArrayList<String> normalizeOrderListForSpinner(ArrayList<Order> beforeList) {
        ArrayList<String> result = new ArrayList<>();
        for (Order order : beforeList) {
            String tmpOrder = "[" + order.getOrderId()+ "] " + order.getSymbol() + " " + order.getType() + " "  + order.getPrice();
            result.add(tmpOrder);
        }
        if (result.size() < 1) {
            result.add("None");
        }
        return result;
    }

    /**
     * A simple method for controlling the content of the spinner, so to not clutter other methods.
     */
    void showOpenOrders() {
        ArrayAdapter<String> ordersAdapter = new ArrayAdapter<>(this, android.R.layout
                .simple_spinner_dropdown_item, normalizedOrderList);
        openOrdersSpinner.setAdapter(ordersAdapter);
    }

    /**
     * same method mentioned above for alerting the user of success and failure,
     * controlling the visual content of the page.
     */
    void showOrderPopup(String topic, String msg) {
        myDialog.setContentView(R.layout.popup_invalid_order_warning);
        Window window = myDialog.getWindow();
        window.setGravity(Gravity.CENTER);
        window.getAttributes().windowAnimations = R.style.DialogAnimator;
        inputMessage = myDialog.findViewById(R.id.orderInputErrorText);
        popupTopic = myDialog.findViewById(R.id.orderInputTopic);
        popupImage = myDialog.findViewById(R.id.orderPopupImage);
        if (topic.equals("Error")) popupImage.setImageResource(R.drawable.error_icon);
        else popupImage.setImageResource(R.drawable.success_icon);
        popupTopic.setText(topic);
        inputMessage.setText(msg);
        myDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        myDialog.show();
    }

    /**
     * Sets the status bar color to #121212, The apps main background color
     * This is used mainly for cosmetics in order to create an immersive feel while browsing the app
     */
    void setStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.status_bar, this.getTheme()));
        }
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.status_bar));
        }
    }
}