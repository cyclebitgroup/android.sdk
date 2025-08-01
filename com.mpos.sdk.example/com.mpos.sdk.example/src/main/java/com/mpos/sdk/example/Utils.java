package com.mpos.sdk.example;

import android.app.Activity;
import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mpos.sdk.entities.Account;
import com.mpos.sdk.entities.PaymentProductItemField;
import com.mpos.sdk.entities.Purchase;
import com.mpos.sdk.entities.Tax;
import com.mpos.sdk.entities.TaxContribution;
import com.mpos.sdk.entities.TransactionItem;

public class Utils {
    public static String getString(Activity activity, String key) {
        return activity.getSharedPreferences(activity.getApplicationContext().getPackageName(), Context.MODE_PRIVATE).getString(key, "");
    }

    public static void setString(Activity activity,String key, String value) {
        activity.getSharedPreferences(activity.getApplicationContext().getPackageName(), Context.MODE_PRIVATE).edit().putString(key, value).commit();
    }

    private static final int S_DECIMALS = MainActivity.CURRENCY.getE();
    private static final int Q_DECIMALS = 3;
    private static final HashMap<String, BigDecimal> TAX_RATES = new HashMap<String, BigDecimal>();
    static {
        TAX_RATES.put(Purchase.TaxCode.VAT_NA, BigDecimal.ZERO.setScale(S_DECIMALS, RoundingMode.HALF_UP));
        TAX_RATES.put(Purchase.TaxCode.VAT_0, BigDecimal.ZERO.setScale(S_DECIMALS, RoundingMode.HALF_UP));
        TAX_RATES.put(Purchase.TaxCode.VAT_10, BigDecimal.valueOf(0.1d).setScale(S_DECIMALS, RoundingMode.HALF_UP));
        TAX_RATES.put(Purchase.TaxCode.VAT_18, BigDecimal.valueOf(0.18d).setScale(S_DECIMALS, RoundingMode.HALF_UP));
        TAX_RATES.put(Purchase.TaxCode.VAT_20, BigDecimal.valueOf(0.2d).setScale(S_DECIMALS, RoundingMode.HALF_UP));
        TAX_RATES.put(Purchase.TaxCode.VAT_110, BigDecimal.valueOf(0.1d).setScale(S_DECIMALS, RoundingMode.HALF_UP));
        TAX_RATES.put(Purchase.TaxCode.VAT_120, BigDecimal.valueOf(0.2d).setScale(S_DECIMALS, RoundingMode.HALF_UP));
    }

    private static HashMap<String, BigDecimal> CalculateTaxes(TransactionItem.TaxMode taxMode, BigDecimal total, List<String> appliedTaxes) {
        HashMap<String, BigDecimal> result = new HashMap<String, BigDecimal>();
        if (appliedTaxes != null && appliedTaxes.size() > 0 && taxMode != null) {
            BigDecimal taxAmount = total.setScale(S_DECIMALS, RoundingMode.HALF_UP);
            for (String taxCode : appliedTaxes) {
                if (taxMode == TransactionItem.TaxMode.FOR_EACH) {
                    if (!taxCode.equals(Purchase.TaxCode.VAT_NA) && !taxCode.equals(Purchase.TaxCode.VAT_0)) {
                        if (!TAX_RATES.containsKey(taxCode))
                            throw new IllegalArgumentException("invalid tax code :" + taxCode);
                        BigDecimal taxRate = TAX_RATES.get(taxCode);
                        taxAmount = taxAmount.subtract(total.divide(taxRate.add(BigDecimal.ONE), RoundingMode.HALF_UP).setScale(S_DECIMALS, RoundingMode.HALF_UP));
                    }
                }

                result.put(taxCode, result.containsKey(taxCode) ? (result.get(taxCode).add(taxAmount)) : taxAmount);
            }
        } else
            result.put(Purchase.TaxCode.VAT_NA, result.containsKey(Purchase.TaxCode.VAT_NA) ? (result.get(Purchase.TaxCode.VAT_NA).add(total)) : total);

        if (result.size() == 0)
            throw new IllegalArgumentException("failed to calculate taxes");
        return result;
    }

    private static List<String> SplitString(String str, int size) {
        List<String> result = new ArrayList<String>();
        for (int i = 0; i < str.length(); i += size)
            result.add(str.substring(i, i + Math.min(size, str.length() - i)));
        return result;
    }

    private static void AppendKeyValue(StringBuilder builder, int lineWidth, String key, String value) {
        boolean multiline = key.length() + 1 + value.length() > lineWidth;
        if (multiline) {
            for (String keyChunk : SplitString(key, lineWidth))
                builder.append(keyChunk).append("\n");
            for (String valueChunk : SplitString(value, lineWidth))
                builder.append(valueChunk.length() == lineWidth ? valueChunk : String.format("%1$-" + valueChunk.length() + "s", valueChunk)).append("\n");
        } else
            builder.append(String.format("%1$-" + (key.length() + 1) + "s%2$" + (lineWidth - (key.length() + 1)) + "s", key, value)).append("\n");
    }

    public static List<Purchase> GetPurchasesFromJson(String json) {
        try {
            List<Purchase> result = new ArrayList<Purchase>();
            JSONObject o = new JSONObject(json);
            JSONArray purchases = o.getJSONArray("Purchases");
            if (purchases != null)
                for (int i = 0; i < purchases.length(); i++)
                    result.add(new Purchase(purchases.getJSONObject(i)));
            return result;
        } catch (JSONException e) {
            return null;
        }
    }


}
