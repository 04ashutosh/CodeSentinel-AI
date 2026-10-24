package com.example.billing;

import java.util.List;

public class PaymentService implements AutoCloseable {
    private String merchantId;
    private double balance;

    public boolean processTransaction(String account, double amount) {
        return amount > 0;
    }

    @Override
    public void close() throws Exception {
    }
}