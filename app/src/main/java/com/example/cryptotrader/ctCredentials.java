package com.example.cryptotrader;
import java.util.LinkedList;
import java.util.List;

/**
 * Main credentials class for each client, this object holds the authentication tokes for
 * BinanceAPI using the API library
 */
public class ctCredentials {
    private String key;
    private String secret;

    public ctCredentials(){
    }

    public ctCredentials(String key, String secret){
        this.key = key;
        this.secret = secret;
    }

    public String getKey() {
        return this.key;
    }

    public String getSecret() {
        return this.secret;
    }
}
