package com.example.cryptotrader;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Build;
import android.text.method.ScrollingMovementMethod;
import android.view.Gravity;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.binance.api.client.BinanceApiAsyncRestClient;
import com.binance.api.client.BinanceApiCallback;
import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.domain.account.Account;
import com.binance.api.client.domain.account.AssetBalance;
import com.binance.api.client.domain.market.TickerPrice;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

/**
 * profile class that shows the user all of his free assets in the Binance exchange. also allows
 * the user to log out of the app and log in with other accounts if needed.
 */
public class ProfileActivity extends AppCompatActivity implements View.OnClickListener{
    private Button logOut;
    private ProgressBar progressBar;
    private TextView textView;

    /**
     * this method initialize all of the components this screen require, listeners etc'. also sends an
     * two API request to binance to get all of the values of a the assets the current account holds and parsing them.
     * one is a snapshot of the current market prices and the other is for know the amounts the accounts of the user
     * holds in each asset. then  displaying said data on each account the user holds to the user.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        logOut = findViewById(R.id.logOut);
        progressBar = findViewById(R.id.progressBar);
        logOut.setOnClickListener(this);
        textView = findViewById(R.id.preveiw);
        DatabaseReference accounts_db = FirebaseDatabase.getInstance().getReference("Accounts");
        progressBar = findViewById(R.id.progressBar);
        textView.setMovementMethod(new ScrollingMovementMethod());
        ArrayList<String> text_preveiw = new ArrayList<>();
        text_preveiw.add("Accounts balances:\n");
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        List currencies= Arrays.asList("BTC","ETH","ADA","BNB","MANA","USDT");
        setStatusBarColor();
        accounts_db.child(user.getUid()).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if(task.isSuccessful()){
                    GenericTypeIndicator<HashMap<String, ctCredentials>> gType = new GenericTypeIndicator<HashMap<String,  ctCredentials>>() {};
                    HashMap<String, ctCredentials> map = task.getResult().getValue(gType);
                    map.forEach((clientName, credentials) ->{
                        BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance(credentials.getKey(), credentials.getSecret());
                        BinanceApiAsyncRestClient client = factory.newAsyncRestClient();
                        client.getAccount(new BinanceApiCallback<Account>() {
                            @Override
                            public void onResponse(Account account) {
                                TreeMap<String,String> cAmounts=  new TreeMap<>();
                                for(AssetBalance ass : account.getBalances()){
                                    String coin = ass.getAsset();
                                    if(currencies.contains(coin)){
                                        cAmounts.put(coin,ass.getFree());
                                    }
                                }
                                client.getAllPrices(new BinanceApiCallback<List<TickerPrice>>() {
                                    @Override
                                    public void onResponse(List<TickerPrice> tickerPrices) {
                                        double sum = 0;
                                        text_preveiw.add(clientName);
                                        text_preveiw.add("Total Amount of assets in Usd: ");
                                        for(TickerPrice tp : tickerPrices){
                                            String symbol = tp.getSymbol();
                                            if(symbol.contains("USDT")){
                                                String cName = symbol.replaceAll("USDT","");
                                                if(currencies.contains(cName)) {
                                                    double price = Double.parseDouble(tp.getPrice());
                                                    double amount = Double.parseDouble(cAmounts.get(cName));
                                                    sum += (price * amount);
                                                }
                                            }
                                        }
                                        text_preveiw.add(Double.toString(sum));
                                        text_preveiw.add("Total free USDT:");
                                        text_preveiw.add(cAmounts.get("USDT"));
                                        text_preveiw.add("\n");
                                        StringBuilder ptxt = new StringBuilder();
                                        text_preveiw.forEach((t) ->{ptxt.append(t).append("\n");});
                                        textView.setText(ptxt);
                                    }
                                    @Override
                                    public void onFailure(Throwable cause) {
                                        try {
                                            throw cause;
                                        } catch (Throwable throwable) {
                                            throwable.printStackTrace();
                                        }
                                    }
                                });
                            }
                            @Override
                            public void onFailure(Throwable cause) {
                                try {
                                    throw cause;
                                } catch (Throwable throwable) {
                                    throwable.printStackTrace();
                                }
                            }
                        });
                    });
                }else{
                    textView.setText("Please add wallets to your account.\n You can do so at the Add button in the middle of the taskbar.");
                }
            }

        });
    }
    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.logOut) {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
        }
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