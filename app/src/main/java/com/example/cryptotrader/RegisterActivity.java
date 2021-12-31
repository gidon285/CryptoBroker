package com.example.cryptotrader;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
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
import com.google.firebase.database.FirebaseDatabase;

public class RegisterActivity extends AppCompatActivity implements View.OnClickListener{
    private TextView registered;
    private Button registerButton;
    private ProgressBar progressBar;
    private EditText editTextEmail, editTextPassword, editTextPasswordValidation;
    private FirebaseAuth mAuth;
    private static final int PASS_LENGTH = 6 ;

    /**
     * this method initialize all of the components this screen require, listeners etc'.
     * @param savedInstanceState Components bundle
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        mAuth = FirebaseAuth.getInstance();
        editTextEmail = findViewById(R.id.email);
        editTextPassword = findViewById(R.id.password);
        editTextPasswordValidation = findViewById(R.id.passwordvalidation);
        registered = findViewById(R.id.registered);
        registered.setOnClickListener(this);
        registerButton = findViewById(R.id.registerButton);
        registerButton.setOnClickListener(this);
        progressBar = findViewById(R.id.progressBar);
        setStatusBarColor();
    }

    /**
     * this method is for navigation purposes.
     * @param view current screen view
     */
    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.registered:
                startActivity(new Intent(this, LoginActivity.class));
                break;
            case R.id.registerButton:
                String[] emailAndPass = validateEmailAndPass();
                if(emailAndPass == null){
                    break;
                }
                showProgression(true);
                registerUser(emailAndPass[0],emailAndPass[1]);
        }
    }

    /**
     * this method sets our rules for registering to our app, checking the length of the password,
     * of both entries match and if the email address is valid.
     * @return String[] email pass validation
     */
    private String[] validateEmailAndPass(){
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String passwordValidation = editTextPasswordValidation.getText().toString().trim();
        if(email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            editTextEmail.setError("Invalid E-Mail address! Please provide a valid address");
            editTextEmail.requestFocus();
        }
        if(password.length() < PASS_LENGTH){
            editTextPassword.setError("Invalid Password! Please provide at least 6 characters");
            editTextPassword.requestFocus();
            return null;
        }
        if(!password.equals(passwordValidation)){
            editTextPasswordValidation.setError("Re-Entered Password does not match the above");
            editTextPasswordValidation.requestFocus();
            return null;
        }
        return new String[] {email,password};
    }


    private void showProgression(boolean show){
        if (show){
            progressBar.setVisibility(View.VISIBLE);
        }
        if(!show){
            progressBar.setVisibility(View.GONE);
        }
    }


    private void announceSuccess(boolean success){
        showProgression(false);
        if(!success){
            Toast.makeText(RegisterActivity.this, "Registration Failed!", Toast.LENGTH_LONG).show();
        }
        if(success){
            Toast.makeText(RegisterActivity.this, "User Registered Successfully! Please verify your e-mail address",
                    Toast.LENGTH_LONG).show();
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
        }


    }

    /**
     * uses firebase to register a new user the the realtime database.
     * @param email
     * @param password
     */
    private void registerUser(String email,String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        if(task.isSuccessful()){
                            ctUser ctUser = new ctUser(email);

                            FirebaseDatabase.getInstance().getReference("Users")
                                    .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                    .setValue(ctUser).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if(task.isSuccessful()){
                                        announceSuccess(true);
                                        FirebaseUser u = mAuth.getCurrentUser();
                                        u.sendEmailVerification();
                                        startActivity(new Intent(RegisterActivity.this, LoginActivity.class));

                                    }
                                    else{
                                        announceSuccess(false);

                                    }
                                }
                            });
                        }
                        else{
                            announceSuccess(false);
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