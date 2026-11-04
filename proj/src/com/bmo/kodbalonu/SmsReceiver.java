package com.bmo.kodbalonu;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsMessage;

/** Gelen SMS'leri dinler, kod içerenleri yakalar. */
public class SmsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        if (!"android.provider.Telephony.SMS_RECEIVED".equals(intent.getAction())) {
            return;
        }
        Bundle bundle = intent.getExtras();
        if (bundle == null) return;
        Object[] pdus = (Object[]) bundle.get("pdus");
        if (pdus == null || pdus.length == 0) return;
        String format = bundle.getString("format");
        StringBuilder body = new StringBuilder();
        String address = "";
        for (Object pdu : pdus) {
            SmsMessage msg;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                msg = SmsMessage.createFromPdu((byte[]) pdu, format);
            } else {
                msg = SmsMessage.createFromPdu((byte[]) pdu);
            }
            if (msg == null) continue;
            if (address.isEmpty() && msg.getOriginatingAddress() != null) {
                address = msg.getOriginatingAddress();
            }
            if (msg.getMessageBody() != null) {
                body.append(msg.getMessageBody());
            }
        }
        if (isIgnoredSender(context, address)) return;
        String code = Detector.extract(context, body.toString());
        if (code != null) {
            CodeHandler.handle(context, code,
                    "SMS" + (address.isEmpty() ? "" : ": " + address),
                    body.toString(), "sender", address);
        }
    }

    private static boolean isIgnoredSender(Context context, String address) {
        if (address == null || address.isEmpty()) return false;
        String ignored = context
                .getSharedPreferences("kodbalonu", Context.MODE_PRIVATE)
                .getString("ignored_senders", "");
        for (String line : ignored.split("\n")) {
            if (!line.trim().isEmpty() && address.contains(line.trim())) {
                return true;
            }
        }
        return false;
    }
}
