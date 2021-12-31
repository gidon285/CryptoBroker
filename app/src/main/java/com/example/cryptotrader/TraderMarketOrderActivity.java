package com.example.cryptotrader;

import static com.binance.api.client.domain.account.NewOrder.marketBuy;
import static com.binance.api.client.domain.account.NewOrder.marketSell;
import static com.example.cryptotrader.App.CHANNEL_1_ID;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.annotation.SuppressLint;
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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.binance.api.client.BinanceApiAsyncRestClient;
import com.binance.api.client.BinanceApiCallback;
import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.domain.account.NewOrderResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;

import java.util.ArrayList;
import java.util.HashMap;

public class TraderMarketOrderActivity extends AppCompatActivity implements View.OnClickListener{
    private NotificationManagerCompat notificationManager;
    private DatabaseReference accountsDB;
    private FirebaseUser user;
    private Button sendOrderButton;
    private ImageView popupImage,profile;
    private TextView fundText, inputMessage, popupTopic;
    private final String[] tradeOptions = {"Buy", "Sell"};
    private final String[] symbolFundOptions = {"USDT", "BUSD", "BNB"};
    private final String[] symbolTargetOptions = {"BTC", "ETH", "ADA", "MANA", "BNB"};
    private Spinner clientSpinner;
    private Spinner tradeOptionsSpinner;
    private Spinner symbolFundSpinner;
    private Spinner symbolTargetSpinner;
    private String chosenSymbol;
    private String chosenOrderType;
    private String chosenClient;
    private String chosenFundCoin;
    private String chosenTargetCoin;
    private String chosenCoinAmount;
    private Dialog myDialog;
    ArrayList<String> clientsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        notificationManager = NotificationManagerCompat.from(this);
        setContentView(R.layout.activity_trader_buy);
        sendOrderButton = findViewById(R.id.initiateOrderButton);
        user = FirebaseAuth.getInstance().getCurrentUser();
        accountsDB = FirebaseDatabase.getInstance().getReference("Accounts");
        clientSpinner = findViewById(R.id.clientSpinner);
        tradeOptionsSpinner = findViewById(R.id.optionsSpinner);
        symbolFundSpinner = findViewById(R.id.symbolFundSpinner);
        symbolTargetSpinner = findViewById(R.id.symbolTargetSpinner);
        clientsList = ctAccount.getClientNamesListAsync(accountsDB, user, "All");
        ArrayAdapter<String> clientsAdapter = new ArrayAdapter<>
                (this, android.R.layout.simple_spinner_dropdown_item, clientsList);
        ArrayAdapter<String> tradeOptionsAdapter = new ArrayAdapter<>
                (this, android.R.layout.simple_spinner_dropdown_item, tradeOptions);
        ArrayAdapter<String> symbolFundsAdapter = new ArrayAdapter<>
                (this, android.R.layout.simple_spinner_dropdown_item, symbolFundOptions);
        ArrayAdapter<String> symbolTargetAdapter = new ArrayAdapter<>
                (this, android.R.layout.simple_spinner_dropdown_item, symbolTargetOptions);
        clientSpinner.setAdapter(clientsAdapter);
        tradeOptionsSpinner.setAdapter(tradeOptionsAdapter);
        symbolFundSpinner.setAdapter(symbolFundsAdapter);
        symbolTargetSpinner.setAdapter(symbolTargetAdapter);
        fundText = findViewById(R.id.fundsAmountText);
        profile = findViewById(R.id.profile);
        profile.setOnClickListener(this);
        myDialog = new Dialog(this);
        sendOrderButton.setOnClickListener(this);
        setStatusBarColor();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.initiateOrderButton) {
            chosenClient = ctUtils.getSpinnerChosenText(clientSpinner);
            chosenOrderType = ctUtils.getSpinnerChosenText(tradeOptionsSpinner);
            chosenFundCoin = ctUtils.getSpinnerChosenText(symbolFundSpinner);
            chosenTargetCoin = ctUtils.getSpinnerChosenText(symbolTargetSpinner);
            chosenCoinAmount = fundText.getText().toString();
            chosenSymbol = chosenTargetCoin + chosenFundCoin;
            if (checkOrderValidity()) return;
            if (chosenOrderType.equals("Buy") || chosenOrderType.equals("Sell")) {
                initiateMarketOrder();
            }
        }else if (v.getId() == R.id.profile) {
            startActivity(new Intent(TraderMarketOrderActivity.this, ProfileActivity.class));
        }
    }

    /**
     * Checks if the order information is not valid for processing
     *
     * @return True if the order information is not valid, False otherwise
     */
    @SuppressLint("SetTextI18n")
    boolean checkOrderValidity() {
        if (chosenCoinAmount == null) {
            showOrderPopup("Error", "Any order must have a coin amount (Null)");
            return true;
        }
        else if (chosenCoinAmount.isEmpty()) {
            showOrderPopup("Error", "Any order must have a coin amount (isEmpty)");
            return true;
        }
        return false;
    }

    /**
     * Generates a Market order according to information filled by the user
     * Will also mitigate errors and promp the customer accordingly
     */
    void initiateMarketOrder() {
        accountsDB.child(user.getUid()).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (task.isSuccessful()) {
                    GenericTypeIndicator<HashMap<String, ctCredentials>> gType =
                            new GenericTypeIndicator<HashMap<String, ctCredentials>>() {};
                    HashMap<String, ctCredentials> map = task.getResult().getValue(gType);
                    assert map != null;
                    map.forEach((clientName, clientTokens) -> {
                        if (clientName.equals(chosenClient) && !chosenClient.equals("All")) {
                            BinanceApiClientFactory factory = BinanceApiClientFactory
                                    .newInstance(clientTokens.getKey(), clientTokens.getSecret());
                            BinanceApiAsyncRestClient client = factory.newAsyncRestClient();
                            if (chosenOrderType.equals("Buy")) {
                                client.newOrder(marketBuy(chosenSymbol, chosenCoinAmount)
                                        , createOrderCallback());
                            }
                            else if (chosenOrderType.equals("Sell")) {
                                client.newOrder(marketSell(chosenSymbol, chosenCoinAmount)
                                        , createOrderCallback());
                            }
                        }
                        else if (chosenClient.equals("All")) {
                            BinanceApiClientFactory factory = BinanceApiClientFactory
                                    .newInstance(clientTokens.getKey(), clientTokens.getSecret());
                            BinanceApiAsyncRestClient client = factory.newAsyncRestClient();
                            if (chosenOrderType.equals("Buy")) {
                                client.newOrder(marketBuy(chosenSymbol, chosenCoinAmount)
                                        , createOrderCallback());
                            }
                            else if (chosenOrderType.equals("Sell")) {
                                client.newOrder(marketSell(chosenSymbol, chosenCoinAmount)
                                        , createOrderCallback());
                            }
                        }
                    });
                }
            }
        });
    }

    /**
     * Creates a BinanceAPI callback object for validity check of each order
     * will catch errors and return the cause for later processing and popup generation
     * This method will also call the popup generation view avvordingly
     *
     * @return BinanceApiCallback<NewOrderResponse> with the order status and information
     */
    private BinanceApiCallback<NewOrderResponse> createOrderCallback() {
        BinanceApiCallback<NewOrderResponse> result = new BinanceApiCallback<NewOrderResponse>() {
            @Override
            public void onResponse(NewOrderResponse newOrderResponse) {
                showOrderPopup("Success"
                        , "Order: \n " + newOrderResponse.getOrderId() + "\n"
                                + newOrderResponse.getSymbol() + "\nCreated");
                Notification notification = new NotificationCompat.Builder(TraderMarketOrderActivity.this, CHANNEL_1_ID)
                        .setSmallIcon(R.drawable.notification_icon)
                        .setContentTitle("Order Sent!")
                        .setContentText("Order: \n " + newOrderResponse.getOrderId() + "\n"
                                + newOrderResponse.getSymbol() + "\nCreated")
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
        };
        return result;
    }

    /**
     * Creates a popup with information about success or fail of the current order
     * contains information about the cause of failure and reason of success
     *
     * @param topic The topic of the popup, either Error or Success
     * @param msg The popup message
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