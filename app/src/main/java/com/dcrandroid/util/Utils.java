package com.dcrandroid.util;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

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

    public static String getRemoteCertificate(Context context) {
        try {
            File path = new File(context.getFilesDir() + "/savedata");
            if (!path.exists()) {
                path.mkdirs();
            }
            File file = new File(path, "remote rpc.cert");
            if (file.exists()) {
                FileInputStream fin = new FileInputStream(file);
                StringBuilder sb = new StringBuilder();
                BufferedReader reader = new BufferedReader(new InputStreamReader(fin));
                String s;
                while ((s = reader.readLine()) != null) {
                    sb.append(s);
                    sb.append("\n");
                }
                fin.close();
                //System.out.println("Cert: "+sb.toString());
                return sb.toString();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static void setRemoteCetificate(Context context, String certificate) {
        try {
            File path = new File(context.getFilesDir() + "/savedata");
            if (!path.exists()) {
                path.mkdirs();
            }
            File file = new File(path, "remote rpc.cert");
            if (file.exists()) {
                file.delete();
            }
            FileOutputStream fout = new FileOutputStream(file);
            byte[] buff = certificate.getBytes();
            fout.write(buff, 0, buff.length);
            fout.flush();
            fout.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getNetworkAddress(Context context) {
        PreferenceUtil util = new PreferenceUtil(context);
        String addr = util.get(Constants.REMOTE_NODE_ADDRESS);
        System.out.println("Util is using remote server: " + addr);
        return addr;
    }

    public static String calculateDays(long seconds, Context context){
        long days = TimeUnit.SECONDS.toDays(seconds);
        if (days == 0){
            return context.getString(R.string.less_than_one_day);
        }else if(days == 1){
            return context.getString(R.string.one_day);
        }

        return context.getString(R.string.multiple_days, days);
    }

    public static String calculateDaysAgo(long seconds, Context context){
        long days = TimeUnit.SECONDS.toDays(seconds);
        if (days == 0){
            return context.getString(R.string.less_than_one_day_ago);
        }else if(days == 1){
            return context.getString(R.string.one_day_ago);
        }

        return context.getString(R.string.multiple_days_ago, days);
    }

    public static String calculateTime(long seconds, Context context) {
        String ago = context.getString(R.string.ago);
        if (seconds > 59) {

            // convert to minutes
            seconds /= 60;

            if (seconds > 59) {

                // convert to hours
                seconds /= 60;

                if (seconds > 23) {

                    // convert to days
                    seconds /= 24;

                    if (seconds > 6) {

                        // convert to weeks
                        seconds /= 7;
                        if (seconds > 3) {

                            // convert to month
                            seconds /= 4;

                            if (seconds > 11) {

                                // convert to year
                                seconds /= 12;

                                return seconds + "y " + ago;
                            }

                            //months
                            return seconds + "mo " + ago;
                        }
                        //weeks
                        return seconds + "w " + ago;
                    }
                    //days
                    return seconds + "d " + ago;
                }
                //hour
                return seconds + "h " + ago;
            }

            //minutes
            return seconds + "m " + ago;
        }

        if (seconds < 0) {
            return context.getString(R.string.now);
        }
        //seconds
        return seconds + "s " + ago;
    }

    public static String getTimeRemaining(long millis, int percentageCompleted, boolean useLeft, Context ctx){
        if (millis > 1000){
            long seconds = millis / 1000;

            if(seconds > 60){
                long minutes = seconds / 60;
                if (useLeft){
                    return ctx.getString(R.string.left_minute_sync_eta, percentageCompleted, minutes);
                }

                return ctx.getString(R.string.remaining_minute_sync_eta, percentageCompleted, minutes);
            }

            if (useLeft){
                return ctx.getString(R.string.left_seconds_sync_eta, percentageCompleted, seconds);
            }

            return ctx.getString(R.string.remaining_seconds_sync_eta, percentageCompleted, seconds);
        }

        if (useLeft){
            return ctx.getString(R.string.left_sync_eta_less_than_seconds, percentageCompleted);
        }

        return ctx.getString(R.string.remaining_sync_eta_less_than_seconds, percentageCompleted);
    }

    public static String formatDecred(long dcr) {
        BigDecimal satoshi = BigDecimal.valueOf(dcr);
        BigDecimal amount = satoshi.divide(BigDecimal.valueOf(1e8), new MathContext(100));
        DecimalFormat format = new DecimalFormat();
        format.applyPattern("#,###,###,##0.########");
        return format.format(amount);
    }

    public static String removeTrailingZeros(double dcr) {
        DecimalFormat format = new DecimalFormat();
        format.applyPattern("#,###,###,##0.########");
        return format.format(dcr);
    }

    public static String formatDecredWithComma(long dcr) {
        double convertedDcr = Dcrlibwallet.amountCoin(dcr);
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        DecimalFormat df = (DecimalFormat) nf;
        df.applyPattern("#,###,###,##0.########");
        return df.format(convertedDcr);
    }

    public static String formatDecredWithoutComma(long dcr) {
        BigDecimal atom = new BigDecimal(dcr);
        BigDecimal amount = atom.divide(BigDecimal.valueOf(1e8), new MathContext(100));
        DecimalFormat format = new DecimalFormat();
        format.applyPattern("#########0.########");
        return format.format(amount);
    }

    public static long signedSizeToAtom(long signedSize) {
        BigDecimal signed = new BigDecimal(signedSize);
        signed = signed.setScale(9, RoundingMode.HALF_UP);

        BigDecimal feePerKb = new BigDecimal(0.01);
        feePerKb = feePerKb.setScale(9, RoundingMode.HALF_UP);

        signed = signed.divide(feePerKb, MathContext.DECIMAL128);

        return signed.longValue();
    }

    public static void copyToClipboard(Context ctx, String copyText, String successMessage) {
        Resources r = ctx.getResources();
        int margin25dp = Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 25, r.getDisplayMetrics()));

        int sdk = android.os.Build.VERSION.SDK_INT;
        if (sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
            android.text.ClipboardManager clipboard = (android.text.ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                clipboard.setText(copyText);
            }
        } else {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager)
                    ctx.getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData
                    .newPlainText(ctx.getString(R.string.your_address), copyText);
            if (clipboard != null)
                clipboard.setPrimaryClip(clip);
        }

        LayoutInflater inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View vi = inflater.inflate(R.layout.toast, null);
        TextView tv = vi.findViewById(android.R.id.message);
        tv.setText(successMessage);
        Toast toast = new Toast(ctx);
        toast.setGravity(Gravity.TOP | Gravity.CENTER, 0, 50);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(vi);
        toast.show();
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

    public static void backupWalletDB(final Context context) {
        try {
            long startTime = System.currentTimeMillis();
            //TODO: Mainnet support
            File walletDb = new File(context.getFilesDir() + "/dcrwallet/testnet2/wallet.db");
            File backup = new File(context.getFilesDir() + "/dcrwallet/testnet2/wallet.db.bak");
            if (backup.exists()) {
                backup.delete();
            }
            if (walletDb.exists() && walletDb.isFile()) {
                FileOutputStream out = new FileOutputStream(backup);
                FileInputStream in = new FileInputStream(walletDb);

                byte[] buff = new byte[8192];
                int len;

                while ((len = in.read(buff)) != -1) {
                    out.write(buff, 0, len);
                    out.flush();
                }
                out.close();
                in.close();
                System.out.println("Backup took " + (System.currentTimeMillis() - startTime) + " ms");
            }
        } catch (IOException e) {
            System.out.println("Backup Failed");
            e.printStackTrace();
        }
    }

    public static void restoreWalletDB(final Context context) {
        try {
            long startTime = System.currentTimeMillis();
            //TODO: Mainnet support
            File walletDb = new File(context.getFilesDir() + "/dcrwallet/testnet2/wallet.db");
            File backup = new File(context.getFilesDir() + "/dcrwallet/testnet2/wallet.db.bak");
            if (walletDb.exists()) {
                walletDb.delete();
            }
            if (walletDb.exists()) {
                FileOutputStream out = new FileOutputStream(walletDb);
                FileInputStream in = new FileInputStream(backup);

                byte[] buff = new byte[8192];
                int len;

                while ((len = in.read(buff)) != -1) {
                    out.write(buff, 0, len);
                    out.flush();
                }
                out.close();
                in.close();
                System.out.println("Restore took " + (System.currentTimeMillis() - startTime) + " ms");
            }
        } catch (IOException e) {
            System.out.println("Restore Failed");
            e.printStackTrace();
        }
    }

    public static String translateError(Context ctx, Exception e) {
        switch (e.getMessage()) {
            case Dcrlibwallet.ErrInsufficientBalance:
                if (!WalletData.getInstance().synced) {
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

    public static void clearApplicationData(Context context) {
        File cache = context.getCacheDir();
        File appDir = new File(cache.getParent());
        if (appDir.exists()) {
            String[] children = appDir.list();
            for (String s : children) {
                if (!s.equals("lib")) {
                    deleteDir(new File(appDir, s));
                }
            }
        }
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().clear().commit();
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
}