package com.example.cryptotrader;

import static com.example.cryptotrader.App.CHANNEL_1_ID;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.app.Activity;
import android.app.Notification;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.litesoftwares.coingecko.CoinGeckoApiClient;
import com.litesoftwares.coingecko.constant.Currency;
import com.litesoftwares.coingecko.domain.Coins.CoinData.DeveloperData;
import com.litesoftwares.coingecko.domain.Coins.CoinData.IcoData;
import com.litesoftwares.coingecko.domain.Coins.CoinFullData;
import com.litesoftwares.coingecko.domain.Coins.CoinList;
import com.litesoftwares.coingecko.domain.Coins.CoinMarkets;
import com.litesoftwares.coingecko.domain.Coins.CoinTickerById;
import com.litesoftwares.coingecko.impl.CoinGeckoApiClientImpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 *  this class is the second screen the the user are presented with, using firebase to authenticate
 *  users, directing users to forgot password section or register section.
 */
public class LoginActivity extends AppCompatActivity implements View.OnClickListener {
    private EditText editTextEmail, editTextPassword;
    private TextView register, forgotPassword;
    private Button login;
    private NotificationManagerCompat notificationManager;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;

    /**
     *  on create to initialize all of the components this screen require, listeners etc'. using a
     *  separate thread to run custom notifications for users, using real time data from Coingeko using
     *  Coingeko API request.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        notificationManager = NotificationManagerCompat.from(this);
        setContentView(R.layout.activity_login);
        editTextEmail = findViewById(R.id.email);
        editTextPassword = findViewById(R.id.password);
        forgotPassword = findViewById(R.id.forgotpassword);
        forgotPassword.setOnClickListener(this);
        register = findViewById(R.id.register);
        register.setOnClickListener(this);
        login = findViewById(R.id.loginButton);
        login.setOnClickListener(this);
        progressBar = findViewById(R.id.progressBar);
        mAuth = FirebaseAuth.getInstance();
        setStatusBarColor();
        new Thread(new Runnable() {
            boolean flag = false;
            @Override
            public void run() {
                try {
                    while(true){
                        int time =300000;
                        if( ! flag ) {time = 60000;flag = true;}
                        Thread.sleep(time);
                        int coinIndex =(int)(Math.random()*5);
                        int action =(int)(Math.random());
                        int titleIndex = (int)(Math.random()*3);
                        CoinGeckoApiClient client = new CoinGeckoApiClientImpl();
                        String[] coinNames= {"bitcoin", "ethereum", "cardano", "decentraland", "binancecoin"};
                        String[] title = {"Have you noticed?","Hold on and look!","Maybe you didn't know but..","We just found out that..."};
                        String[] actions= {"buying?","selling?"};
                        String coin = coinNames[coinIndex];
                        Map<String, Map<String, Double>> coinPrices = client.getPrice(coin,Currency.USD);
                        double price= coinPrices.get(coin).get(Currency.USD);
                        Notification notification = new NotificationCompat.Builder(LoginActivity.this, CHANNEL_1_ID)
                                .setSmallIcon(R.drawable.notification_icon)
                                .setContentTitle(title[titleIndex])
                                .setContentText(coinNames[coinIndex]+" value is " +price+" please consider "+actions[action])
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                .setCategory(NotificationCompat.CATEGORY_EVENT)
                                .build();
                        notificationManager.notify(1, notification);

                    }

                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();


    }

    /**
     * method for navigation based on user status (remember password, nor registered etc')
     * @param view
     */
    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.loginButton:
                String[] emailAndPass = validateEmailAndPass();
                if (emailAndPass == null){
                    break;
                }
                showProgression(true);
                userLogin(emailAndPass[0],emailAndPass[1]);

                break;
            case R.id.register:
                startActivity(new Intent(this, RegisterActivity.class));
                break;
            case R.id.forgotpassword:
                startActivity(new Intent(this, ForgotPasswordActivity.class));
                break;

        }
    }
    private void showProgression(boolean show){
        if (show){
            progressBar.setVisibility(View.VISIBLE);
        }
        if(!show){
            progressBar.setVisibility(View.GONE);
        }
    }

    /**
     * using firebase to validate the information sent to the users email adress.
     * @return
     */
    private String[] validateEmailAndPass(){
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            editTextEmail.setError("Invalid E-Mail address! Please provide a valid address");
            editTextEmail.requestFocus();
        }
        if (password.length() < 6){
            editTextPassword.setError("Invalid Password! Please provide at least 6 characters");
            editTextPassword.requestFocus();
            return null;
        }
        return new String[] {email,password};
    }
    /*
    * 0  success
    * 1 need to verify email
    * 2 failure
    * */
    private void announceSuccess(int event){
        showProgression(false);
        if (event == 0){
            return;
        }
        notifyFailure(event);

    }
    private void notifyFailure(int event){
        String txt = "";
        if (event == 1){
            txt = "Verification mail sent again. Please check your e-mail";
        }
        if (event == 2){
            txt = "Failed to Login! Please try again";
        }
        Toast.makeText(LoginActivity.this
                , txt
                ,Toast.LENGTH_LONG).show();
    }

    private void userLogin(String email,String password) {
        mAuth.signInWithEmailAndPassword(email,password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()){
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user.isEmailVerified()) {
                        announceSuccess(0);
                        startActivity(new Intent(LoginActivity.this, HomeActivity.class));

                    }
                    else {
                        user.sendEmailVerification();
                        announceSuccess(1);

                    }
                }
                else {
                    announceSuccess(2);

                }
            }
        });
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
