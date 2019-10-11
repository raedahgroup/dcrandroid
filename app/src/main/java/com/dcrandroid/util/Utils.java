/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.util;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.StringRes;
import androidx.core.app.NotificationCompat;

import com.dcrandroid.HomeActivity;
import com.dcrandroid.R;
import com.dcrandroid.data.Constants;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import dcrlibwallet.Dcrlibwallet;

public class Utils {
    public static ProgressDialog getProgressDialog(Context context, boolean cancelable, boolean cancelOnTouchOutside,
                                                   String message) {
        ProgressDialog pd = new ProgressDialog(context);
        pd.setCancelable(cancelable);
        pd.setCanceledOnTouchOutside(cancelOnTouchOutside);
        pd.setMessage(message);
        return pd;
    }

    public static ProgressDialog getProgressDialog(Context context, boolean cancelable, boolean cancelOnTouchOutside, @StringRes int message) {
        return Utils.getProgressDialog(context, cancelable, cancelOnTouchOutside, context.getString(message));
    }

    public static String getWordList(Context context) {
        try {
            InputStream fin = context.getAssets().open("wordlist.txt");
            StringBuilder wordsList = new StringBuilder();
            BufferedReader br = new BufferedReader(new InputStreamReader(fin));
            String line;
            while ((line = br.readLine()) != null) {
                wordsList.append(" ");
                wordsList.append(line);
            }
            fin.close();
            return wordsList.toString().trim();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] getHash(String hash) {
        List<String> hashList = new ArrayList<>();
        String[] split = hash.split("");
        if ((split.length - 1) % 2 == 0) {
            String d = "";
            for (int i = 0; i < split.length - 1; i += 2) {
                d = d.concat(split[(split.length - 1) - (i + 1)]
                        + split[(split.length - 1) - i]);
                hashList.add(split[(split.length - 1) - (i + 1)]
                        + split[(split.length - 1) - i]);
            }
            return hexStringToByteArray(d);
        } else {
            System.err.println("Invalid Hash");
        }
        return null;
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public static String getDaysBehind(long seconds, Context context) {
        long days = TimeUnit.SECONDS.toDays(seconds);
        if (days == 1) {
            return context.getString(R.string.one_days_behind);
        }

        return context.getString(R.string.days_behind, days);
    }

    public static String calculateTime(long seconds, Context context) {
        String ago = "";
        if (seconds > 59) {

            // convert to minutes
            seconds /= 60;

            if (seconds > 59) {

                // convert to hours
                seconds /= 60;

                if (seconds > 23) {

                    // convert to days
                    seconds /= 24;

                    //days
                    return seconds + " days " + ago;
                }
                //hour
                return seconds + " hours " + ago;
            }

            //minutes
            return seconds + " minutes " + ago;
        }

        if (seconds < 0) {
            seconds = 0;
        }
        //seconds
        return seconds + " seconds " + ago;
    }

    public static String getSyncTimeRemaining(long seconds, int percentageCompleted, boolean useLeft, Context ctx) {
        if (seconds > 1) {

            if (seconds > 60) {
                long minutes = seconds / 60;
                if (useLeft) {
                    return ctx.getString(R.string.left_minute_sync_eta, percentageCompleted, minutes);
                }

                return ctx.getString(R.string.remaining_minute_sync_eta, percentageCompleted, minutes);
            }

            if (useLeft) {
                return ctx.getString(R.string.left_seconds_sync_eta, percentageCompleted, seconds);
            }

            return ctx.getString(R.string.remaining_seconds_sync_eta, percentageCompleted, seconds);
        }

        if (useLeft) {
            return ctx.getString(R.string.left_sync_eta_less_than_seconds, percentageCompleted);
        }

        return ctx.getString(R.string.remaining_sync_eta_less_than_seconds, percentageCompleted);
    }

    public static String getSyncTimeRemaining(long seconds, Context ctx) {
        if (seconds > 60) {
            long minutes = seconds / 60;

            return ctx.getString(R.string.time_left_minutes, minutes);
        }

        return ctx.getString(R.string.time_left_seconds, seconds);
    }

    public static String getTime(long seconds) {
        if (seconds > 60) {
            long minutes = seconds / 60;
            seconds = seconds % 60;
            return minutes + "m" + seconds + "s";
        }

        return seconds + "s";
    }

    public static String removeTrailingZeros(double dcr) {
        DecimalFormat format = (DecimalFormat) NumberFormat.getNumberInstance(Locale.ENGLISH);
        format.applyPattern("#,###,###,##0.########");
        return format.format(dcr);
    }

    public static String formatDecredWithComma(long dcr) {
        double convertedDcr = Dcrlibwallet.amountCoin(dcr);
        DecimalFormat df = (DecimalFormat) NumberFormat.getNumberInstance(Locale.ENGLISH);
        df.applyPattern("#,###,###,##0.########");
        return df.format(convertedDcr);
    }

    public static String formatDecredWithoutComma(long dcr) {
        BigDecimal atom = new BigDecimal(dcr);
        BigDecimal amount = atom.divide(BigDecimal.valueOf(1e8), new MathContext(100));
        DecimalFormat format = (DecimalFormat) NumberFormat.getNumberInstance(Locale.ENGLISH);
        format.applyPattern("#########0.########");
        return format.format(amount);
    }

    private static void saveToClipboard(Context context, String text){

        int sdk = android.os.Build.VERSION.SDK_INT;
        if (sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
            android.text.ClipboardManager clipboard = (android.text.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                clipboard.setText(text);
            }
        } else {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager)
                    context.getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData
                    .newPlainText(context.getString(R.string.your_address), text);
            if (clipboard != null)
                clipboard.setPrimaryClip(clip);
        }
    }

    public static void copyToClipboard(View v, String text, @StringRes int successMessage) {
        saveToClipboard(v.getContext(), text);
        SnackBar.Companion.showText(v, successMessage, Toast.LENGTH_SHORT);
    }

    public static void copyToClipboard(Context context, String text, @StringRes int successMessage) {
        saveToClipboard(context, text);
        SnackBar.Companion.showText(context, successMessage, Toast.LENGTH_SHORT);
    }

    public static String readFromClipboard(Context context) {
        int sdk = android.os.Build.VERSION.SDK_INT;
        if (sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
            android.text.ClipboardManager clipboard = (android.text.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) return clipboard.getText().toString();
        } else {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager)
                    context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null && clipboard.hasPrimaryClip())
                return clipboard.getPrimaryClip().getItemAt(0).getText().toString();
        }
        return "";
    }

    public static String translateError(Context ctx, Exception e) {
        switch (e.getMessage()) {
            case Dcrlibwallet.ErrInsufficientBalance:
                if (!WalletData.Companion.getInstance().getSynced()) {
                    return ctx.getString(R.string.not_enought_funds_synced);
                }
                return ctx.getString(R.string.not_enough_funds);
            case Dcrlibwallet.ErrEmptySeed:
                return ctx.getString(R.string.empty_seed);
            case Dcrlibwallet.ErrNotConnected:
                return ctx.getString(R.string.not_connected);
            case Dcrlibwallet.ErrPassphraseRequired:
                return ctx.getString(R.string.passphrase_required);
            case Dcrlibwallet.ErrWalletNotLoaded:
                return ctx.getString(R.string.wallet_not_loaded);
            case Dcrlibwallet.ErrInvalidPassphrase:
                return ctx.getString(R.string.invalid_passphrase);
            case Dcrlibwallet.ErrNoPeers:
                return ctx.getString(R.string.err_no_peers);
            default:
                return e.getMessage();
        }
    }

    //Shannon Entropy
    public static double getShannonEntropy(String s) {
        int n = 0;
        Map<Character, Integer> occ = new HashMap<>();

        for (int c_ = 0; c_ < s.length(); ++c_) {
            char cx = s.charAt(c_);
            if (occ.containsKey(cx)) {
                occ.put(cx, occ.get(cx) + 1);
            } else {
                occ.put(cx, 1);
            }
            ++n;
        }

        double e = 0.0;
        for (Map.Entry<Character, Integer> entry : occ.entrySet()) {
            double p = (double) entry.getValue() / n;
            e += p * log2(p);
        }
        return -e;
    }

    private static double log2(double a) {
        return Math.log(a) / Math.log(2);
    }

    static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        assert dir != null;
        return dir.delete();
    }

    public static void restartApp(Context context) {
        PackageManager packageManager = context.getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage(context.getPackageName());
        if (intent != null) {
            ComponentName componentName = intent.getComponent();
            Intent mainIntent = Intent.makeRestartActivityTask(componentName);
            context.startActivity(mainIntent);
            Runtime.getRuntime().exit(0);
        }
    }

    public static void sendTransactionNotification(Context context, NotificationManager manager, String amount,
                                                   int nonce, boolean multiWallet, String walletName) {


        String title;
        if(multiWallet) {
            title = context.getString(R.string.wallet_new_transaction, walletName);
        }else{
            title = context.getString(R.string.new_transaction);
        }

        Intent launchIntent = new Intent(context, HomeActivity.class);
        launchIntent.setAction(Constants.NEW_TRANSACTION_NOTIFICATION);
        PendingIntent launchPendingIntent = PendingIntent.getActivity(context, 1, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new NotificationCompat.Builder(context, "new transaction")
                .setContentTitle(title)
                .setContentText(amount)
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setOngoing(false)
                .setAutoCancel(true)
                .setGroup(Constants.TRANSACTION_NOTIFICATION_GROUP)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(launchPendingIntent)
                .build();

        Notification groupSummary = new NotificationCompat.Builder(context, "new transaction")
                .setContentTitle(context.getString(R.string.new_transaction))
                .setContentText(context.getString(R.string.new_transaction))
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setGroup(Constants.TRANSACTION_NOTIFICATION_GROUP)
                .setGroupSummary(true)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build();

        manager.notify(nonce, notification);
        manager.notify(Constants.TRANSACTION_SUMMARY_ID, groupSummary);
    }
}