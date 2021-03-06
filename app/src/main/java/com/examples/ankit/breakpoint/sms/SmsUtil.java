package com.examples.ankit.breakpoint.sms;

import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import com.examples.ankit.breakpoint.BreakPointApplication;
import com.examples.ankit.breakpoint.models.Transaction;
import com.examples.ankit.breakpoint.models.Transactions;
import com.examples.ankit.breakpoint.prefences.MyPreferenceManager;
import com.examples.ankit.breakpoint.utils.DateUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ankit on 20/05/17.
 */

public class SmsUtil {
    private static final String TAG = "SMSUtil";
    public static final int EXPENSE = 0;
    public static final int INCOME = 1;
    public static final int UNDEFINED = -1;

    /**
     * Fetch SMS and save into local pref based on transaction type.
     */
    public static Transactions fetchInbox() {
        Transactions transactions = new Transactions();

        Uri uriSms = Uri.parse("content://sms/inbox");
        Cursor cursor = BreakPointApplication.getAppContext().getContentResolver()
                .query(uriSms, new String[]{"_id", "address", "date", "body"}, null, null, "date ASC");
        if (cursor != null && cursor.isBeforeFirst()) {
            ArrayList<Transaction> transactionsList = new ArrayList<>();
            LinkedHashMap<Integer, List<Transaction>> monthlyTransaction = new LinkedHashMap<>();
            List<Transaction> monthlyList = new ArrayList<>();
            int currentMonth = -1; // this is for mapping monthly list
            while (cursor.moveToNext()) {
                String senderAddress = cursor.getString(1);
                String messageBody = cursor.getString(3);
                if (isItTransactionalSms(senderAddress)) {
                    int transactionType = getTransactionType(messageBody);
                    if (UNDEFINED != transactionType) {
                        Transaction transaction = new Transaction();
                        try {
                            long timestamp = Long.parseLong(cursor.getString(2));
                            if (timestamp < MyPreferenceManager.getLastTransactionUpdateTime()) {
                                break;
                            }
                            transaction.setDate(new Date(timestamp));
                            transaction.setId(timestamp);
                            if (currentMonth == -1) {
                                //this is first time case.
                                currentMonth = DateUtil.getMonthFromDate(timestamp);
                            } else if (currentMonth != DateUtil.getMonthFromDate(timestamp)) {
                                //this is the case when we have reached to another month and needs tosave this list in map.
                                monthlyList = new ArrayList<>();
                                currentMonth = DateUtil.getMonthFromDate(timestamp);
                            }
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                        transaction.setAmount(getAmount(messageBody));
                        transaction.setType(transactionType);
                        transactionsList.add(transaction);
                        monthlyList.add(transaction);
                        monthlyTransaction.put(currentMonth, monthlyList);
                    }
                }
            }

            if (!transactionsList.isEmpty()) {
                transactions.setTransactions(transactionsList);
            }

            if (!monthlyList.isEmpty()) {
                transactions.setMonthlyTransactions(monthlyTransaction);
            }
            transactions.setLastChecked(System.currentTimeMillis());
        }
        return transactions;
    }

    public static Transaction addSingleSmsToTransaction(String address, String messageBody, long timestamp) {
        Transaction transaction = null;
        if (TextUtils.isEmpty(address) || TextUtils.isEmpty(messageBody)) {
            return transaction;
        }
        if (isItTransactionalSms(address)) {
            int transactionType = getTransactionType(messageBody);
            if (UNDEFINED != transactionType) {
                transaction = new Transaction();
                try {
                    transaction.setDate(new Date(timestamp));

                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
                transaction.setAmount(getAmount(messageBody));
                transaction.setType(transactionType);
                Transactions transactions = MyPreferenceManager.getTransactions();
                transactions.getTransactions().add(0, transaction);
                transactions.setLastChecked(System.currentTimeMillis());

                int month = DateUtil.getMonthFromDate(timestamp);
                LinkedHashMap<Integer, List<Transaction>> existingMonthlyTransactions = transactions.getMonthlyTransactions();
                List<Transaction> expensesOfMonth = existingMonthlyTransactions.get(month);
                expensesOfMonth.add(transaction);
                existingMonthlyTransactions.put(month, expensesOfMonth);
                transactions.setMonthlyTransactions(existingMonthlyTransactions);

                MyPreferenceManager.setTransactions(transactions);
            }
            return transaction;
        }
        return transaction;
    }

    /**
     * Checks and return if a given SMS is from Special Number
     */
    private static boolean isItTransactionalSms(String senderAddress) {
        if (TextUtils.isEmpty(senderAddress)) {
            return false;
        }

        boolean isFromBank = false;
        Pattern regEx =
                Pattern.compile("[a-zA-Z0-9]{2}-[a-zA-Z0-9]{6}");
        Matcher matcher = regEx.matcher(senderAddress);
        if (matcher.find()) {
            isFromBank = true;
        }
        return isFromBank;
    }

    /**
     * Returns amount from an SMS.
     */
    private static double getAmount(String messageBody) {
        Pattern regEx
                = Pattern.compile("(?i)(?:(?:RS|INR|MRP)\\.?\\s?)(\\d+(:?\\,\\d+)?(\\,\\d+)?(\\.\\d{1,2})?)");
        Matcher matcher = regEx.matcher(messageBody);
        if (matcher.find()) {
            String amount = (matcher.group(0));
            //(?i) will search for case insensitive
            amount = amount.replaceAll("(?i)rs.", "");
            amount = amount.replaceAll("(?i)inr.", "");
            amount = amount.replaceAll(" ", "");
            amount = amount.replaceAll(",", "");
            return Double.parseDouble(amount);
        }

        return 0;
    }

    /**
     * It reads sms body and based on keywords, return transaction type as Expense or Income
     */
    private static int getTransactionType(String messageBody) {
        int transactionType = UNDEFINED;
        if (messageBody.contains("paid") || messageBody.contains("debited")
                || messageBody.contains("spent") || messageBody.contains("purchase")) {
            transactionType = EXPENSE;
        } else if (messageBody.contains("credited") || messageBody.contains("received")
                || messageBody.contains("receive")) {
            transactionType = INCOME;
        }

        return transactionType;
    }
}
