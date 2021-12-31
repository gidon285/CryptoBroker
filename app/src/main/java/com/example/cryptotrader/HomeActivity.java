package com.example.cryptotrader;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.binance.api.client.BinanceApiAsyncRestClient;
import com.binance.api.client.BinanceApiCallback;
import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.domain.account.Account;
import com.binance.api.client.domain.account.AssetBalance;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.litesoftwares.coingecko.CoinGeckoApiClient;
import com.litesoftwares.coingecko.domain.Coins.MarketChart;
import com.litesoftwares.coingecko.impl.CoinGeckoApiClientImpl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * home screen of our app, from here you can navigate to all other pages of the app,
 * popup, user stored data, information about the current market including specific
 * crypto currencies and their status, and our own implantation of visualization of the data.
 */
public class HomeActivity extends AppCompatActivity implements View.OnClickListener{
    private ArrayList<BarEntry> chartEntries;
    private BarChart barChart;
    private BarDataSet barDataSet;
    private int chartCounter = 0;
    private Button btcVal;
    private Button ethVal;
    private Button adaVal;
    private Button manaVal;
    private Button bnbVal;
    private Dialog myDialog;
    private EditText accountName, keyInput, secretInput;
    private DatabaseReference accounts_db;
    private FirebaseUser user;
    private ProgressBar progressBar;
    private Spinner barChartSpinner;
    private final String[] coinNames= {"bitcoin", "ethereum", "cardano", "decentraland", "binancecoin"};

    /**
     *  on create to initialize all of the components this screen require, listeners etc'. setting the visualization
     *  of a wanted given coin chosen by the user, defaulting in BTC when no coin in given.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myDialog = new Dialog(this);
        setContentView(R.layout.activity_home);
        ImageButton add = findViewById(R.id.add);
        ImageButton profile = findViewById(R.id.profile);
        ImageButton wallet = findViewById(R.id.wallet);
        ImageButton chart = findViewById(R.id.chart);
        barChartSpinner = findViewById(R.id.barChartSpinner);
        ArrayAdapter<String> coinChartAdapter = new ArrayAdapter<>
                (this, android.R.layout.simple_spinner_dropdown_item, coinNames);
        barChartSpinner.setAdapter(coinChartAdapter);
        wallet.setOnClickListener(this);
        chart.setOnClickListener(this);
        add.setOnClickListener(this);
        profile.setOnClickListener(this);
        user = FirebaseAuth.getInstance().getCurrentUser();
        accounts_db = FirebaseDatabase.getInstance().getReference("Accounts");
        progressBar = findViewById(R.id.progressBar);
        setStatusBarColor();
        chartEntries = new ArrayList<>();
        barChart = findViewById(R.id.barChart);
        try {
            new barChartTask("bitcoin").execute().get();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        barChart.animateXY(2000, 2000);
        barChartSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String chosenCoin = barChartSpinner.getSelectedItem().toString();
                chartEntries.clear();
                barDataSet.clear();
                try {
                    new barChartTask(chosenCoin).execute().get();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                barChart.animateXY(2000, 2000);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    /**
     * for navigation purposes, this on click method what represents the Menu of the application.
     * can be accessed for every page.
     * @param view This view
     */
    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        switch(view.getId()){
            case R.id.wallet:
                showWallet(view);
                break;
            case R.id.chart:
                showPopupTradeChooser(view);
                break;
            case R.id.add:
                showPopupAdd(view);
                break;
            case R.id.profile:
                progressBar.setVisibility(View.VISIBLE);
                startActivity(new Intent(HomeActivity.this, ProfileActivity.class));
                progressBar.setVisibility(View.GONE);
                break;
        }
    }

    /**
     * this method shows the a popup that contains the values of a chosen account from the users accounts.
     * defaulting in a select account message, then sends an API request to firebase to get the available accounts.
     * on completion of the request we add then to the an auto complete text view, then sending two API
     * request to Binance- one to get all of the assets in the account and the other the the price
     * of all coins in the market, then parsing them to get how much is free in USDT(stable-coin)
     * and how much is in assets in USD at the account.
     * @param view This view
     */
    private void showWallet(View view) {
        ArrayList<String> nList = new ArrayList<>();
        ArrayList<ctCredentials> ctCredentialsArrayList = new ArrayList<>();
        myDialog.setContentView(R.layout.popup_mywallets);
        btcVal = myDialog.findViewById(R.id.bit_value);
        ethVal = myDialog.findViewById(R.id.eth_value);
        adaVal = myDialog.findViewById(R.id.ada_value);
        bnbVal = myDialog.findViewById(R.id.bnb_value);
        manaVal = myDialog.findViewById(R.id.mana_value);
        accounts_db.child(user.getUid()).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if(task.isSuccessful()){
                    GenericTypeIndicator<HashMap<String, ctCredentials>> gType = new GenericTypeIndicator<HashMap<String,  ctCredentials>>() {};
                    HashMap<String, ctCredentials> map = task.getResult().getValue(gType);
                    map.forEach((clientName, clientTokens) -> {nList.add(clientName); ctCredentialsArrayList.add(clientTokens); });
                }
            }
        });
        AutoCompleteTextView textView =  myDialog.findViewById(R.id.autoCompleteTextView);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, nList);
        textView.setAdapter(adapter);
        ArrayList<String> cname = new ArrayList<>();
        textView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                cname.add(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                ctCredentials credentials = ctCredentialsArrayList.get(nList.indexOf(s.toString()));
                BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance(credentials.getKey(), credentials.getSecret());
                BinanceApiAsyncRestClient client = factory.newAsyncRestClient();
                client.getAccount(new BinanceApiCallback<Account>() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onResponse(Account account) {
                        for(AssetBalance ass : account.getBalances()){
                            String coin = ass.getAsset();
                            BigDecimal biggy = new BigDecimal(0);
                            switch (coin) {
                                case "BTC":
                                    if (Double.parseDouble(ass.getFree()) > 0) {
                                        biggy = new BigDecimal(Double.parseDouble(ass.getFree()))
                                                .setScale(9, BigDecimal.ROUND_HALF_EVEN);
                                    }
                                    btcVal.setText("BTC:  " + biggy.toPlainString());
                                    break;
                                case "ETH":
                                    if (Double.parseDouble(ass.getFree()) > 0) {
                                        biggy = new BigDecimal(Double.parseDouble(ass.getFree()))
                                                .setScale(6, BigDecimal.ROUND_HALF_EVEN);
                                    }
                                    ethVal.setText("ETH:  " + biggy.toPlainString());
                                    break;
                                case "ADA":
                                    if (Double.parseDouble(ass.getFree()) > 0) {
                                        biggy = new BigDecimal(Double.parseDouble(ass.getFree()))
                                                .setScale(3, BigDecimal.ROUND_HALF_EVEN);
                                    }
                                    adaVal.setText("ADA:  " + biggy.toPlainString());
                                    break;
                                case "BNB":
                                    if (Double.parseDouble(ass.getFree()) > 0) {
                                        biggy = new BigDecimal(Double.parseDouble(ass.getFree()))
                                                .setScale(6, BigDecimal.ROUND_HALF_EVEN);
                                    }
                                    bnbVal.setText("BNB:  " + biggy.toPlainString());
                                    break;
                                case "MANA":
                                    if (Double.parseDouble(ass.getFree()) > 0) {
                                        biggy = new BigDecimal(Double.parseDouble(ass.getFree()))
                                                .setScale(3, BigDecimal.ROUND_HALF_EVEN);
                                    }
                                    manaVal.setText("MANA:  " + biggy.toPlainString());
                                    break;
                            }
                        }
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
        });
        TextView close_pop = myDialog.findViewById(R.id.txtclose);
        close_pop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myDialog.dismiss();
            }
        });
        myDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        myDialog.show();
    }
  
    /**
     * this method shows a poppup containing a veiw that helps user to add an binance API key to the app.
     * all values are added to firebase realtime storage so each account needs to be added once. showing
     * a toast message of the progress.
     * @param view
     */

  private void showPopupAdd(View view){
        myDialog.setContentView(R.layout.popup_add);
      Button addButton = myDialog.findViewById(R.id.btnadd);
        accountName = myDialog.findViewById(R.id.account_name_input);
        keyInput = myDialog.findViewById(R.id.key_input);
        secretInput = myDialog.findViewById(R.id.secret_input);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String a_name = accountName.getText().toString().trim();
                String key = keyInput.getText().toString().trim();
                String secret = secretInput.getText().toString().trim();
                accounts_db.child(user.getUid()).child(a_name)
                        .setValue(new ctCredentials(key,secret))
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    Toast.makeText(HomeActivity.this,
                                            "Account added successfully",
                                            Toast.LENGTH_LONG).show();
                                    myDialog.dismiss();
                                }
                                else {
                                    Toast.makeText(HomeActivity.this, "Something went wrong. Please try again", Toast.LENGTH_LONG).show();
                                }
                            }
                        });
            }
        });
        TextView closePopupText = myDialog.findViewById(R.id.txtclose);
        closePopupText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myDialog.dismiss();
            }
        });
        myDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        myDialog.show();
    }

    /**
     * this method shows the main popup for the trading platform at the app. from here the user
     * can navigate to cancel orders, order limit, and plain buy and sell screens, using buttons
     * and button onClicklisteners.
     * @param view This view
     */
    void showPopupTradeChooser(View view) {
        myDialog.setContentView(R.layout.popup_trader_chooser);
        Window window = myDialog.getWindow();
        window.setGravity(Gravity.CENTER);
        window.getAttributes().windowAnimations = R.style.DialogAnimator;
        TextView closePopupText = myDialog.findViewById(R.id.txtclose);
        Button simpleOrderButton = myDialog.findViewById(R.id.simpleOrderButton);
        Button limitOrderButton = myDialog.findViewById(R.id.limitOrderButton);
        limitOrderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HomeActivity.this, LimitTraderActivity.class));
            }
        });
        simpleOrderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HomeActivity.this, TraderMarketOrderActivity.class));
            }
        });
        Button cancelTradeButton = myDialog.findViewById(R.id.cancelTradeButton);
        cancelTradeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HomeActivity.this, CancelOrderActivity.class));
            }
        });
        closePopupText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myDialog.dismiss();
            }
        });
        myDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        myDialog.show();
    }
  
    /**
     * this method is for using the Coingeko API and MPAndroidChart library, using an Asynctask so
     * this method will run in the background so to not clutter main thread and main methods.
     */

    @SuppressWarnings("rawtypes")
    @SuppressLint("StaticFieldLeak")
    private class barChartTask extends AsyncTask {
        private String symbol;
        @SuppressWarnings("deprecation")
        private barChartTask(String symbol) {
            this.symbol = symbol;
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            CoinGeckoApiClient client = new CoinGeckoApiClientImpl();
            int barColor = 0xFF6200ee;
            MarketChart resultMarketChart = client.getCoinMarketChartById(symbol, "usd", 1);
            for (List<String> list : resultMarketChart.getPrices()) {
                BarEntry tempEntry = new BarEntry(chartCounter, Float.parseFloat(list.get(1)));
                chartCounter++;
                chartEntries.add(tempEntry);
            }
            String symbolViewText = symbol.substring(0, 1).toUpperCase() + symbol.substring(1);
            barDataSet = new BarDataSet(chartEntries, symbolViewText + " Daily Candles");
            switch(symbol) {
                case("bitcoin"): {
                    barColor = 0xFFFFAB01;
                    break;
                }
                case("ethereum"): {
                    barColor = 0xFFF5F5F5;
                    break;
                }
                case("cardano"): {
                    barColor = 0xFF2962FF;
                    break;
                }
                case("decentraland"): {
                    barColor = 0xFFFA1302;
                    break;
                }
                case("binancecoin"): {
                    barColor = 0xFFFFD600;
                    break;
                }
            }
            barDataSet.setColor(barColor);
            barDataSet.setValueTextColor(Color.WHITE);
            barDataSet.setValueTextSize(16f);
            client.shutdown();
            BarData barData = new BarData(barDataSet);
            barChart.setNoDataTextColor(Color.WHITE);
            barChart.setFitBars(true);
            barChart.setData(barData);
            barChart.getAxisLeft().setTextColor(0xFFffffff);
            barChart.getXAxis().setTextColor(0xFFffffff);
            barChart.getLegend().setTextColor(0xFFffffff);
            barChart.getDescription().setTextColor(0xFFffffff);
            barChart.getDescription().setText("© GGAS Team");
            return null;
        }
    }

    /**
     * Sets the status bar color to #121212, The apps main background color
     * This is used mainly for cosmetics in order to create an immersive feel while browsing the app
     */
    @SuppressLint("ObsoleteSdkInt")
    void setStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.status_bar, this.getTheme()));
        }
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.status_bar));
        }
    }
}