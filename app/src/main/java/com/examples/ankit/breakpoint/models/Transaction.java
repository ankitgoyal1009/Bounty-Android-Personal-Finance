package com.examples.ankit.breakpoint.models;

import java.util.Date;

/**
 * Created by ankit on 21/05/17.
 */

public class Transaction {
    private String name;
    private Date date;
    private double amount;
    private int type; //debit or credit
    private int expenseOrIncomeType; // 0=Grocery, 1=mobile recharge, 2=utility bill payment etc
    private int mode; // 0=debit card, 1=credit card, 2= wallets

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getExpenseOrIncomeType() {
        return expenseOrIncomeType;
    }

    public void setExpenseOrIncomeType(int expenseOrIncomeType) {
        this.expenseOrIncomeType = expenseOrIncomeType;
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }
}