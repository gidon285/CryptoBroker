package com.example.cryptotrader;

import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.GenericTypeIndicator;

import java.util.HashMap;

/**
 * CryptoTrader Utils class will be used generally for code conventions, preproccessing and
 * data generation with general project methods.
 * Methods here are used across all classes and are no subject unique
 */
public class ctUtils {
    HashMap<String, ctCredentials> result;

    ctUtils() {
    }

    /**
     * Returns the current item choosed in a spinner as String
     * @param spinner Spinner object
     * @return String result of the current chosen object on spinner
     */
    public static String getSpinnerChosenText(Spinner spinner) {
        TextView textView = (TextView)spinner.getSelectedView();
        return textView.getText().toString();
    }

    public HashMap<String, ctCredentials> getCredentialsMap(DatabaseReference db, FirebaseUser user) {
        db.child(user.getUid()).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if(task.isSuccessful()) {
                    GenericTypeIndicator<HashMap<String, ctCredentials>> gType =
                            new GenericTypeIndicator<HashMap<String,  ctCredentials>>() {};
                    result = task.getResult().getValue(gType);
                }
            }
        });
        return result;
    }
}