package com.wb.apducommander;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.simalliance.openmobileapi.Channel;
import org.simalliance.openmobileapi.Reader;
import org.simalliance.openmobileapi.SEService;
import org.simalliance.openmobileapi.Session;

/*
 *
 * This app uses SIMalliance Open Mobile API.
 */

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";

    private TelephonyManager telephonyManager;

    private SEService _service = null;
    private Session _session = null;

    private TextView _textview = null;
    private ScrollView _scrollview = null;

    /**
     *   https://osmocom.org/projects/security/wiki/A5_GSM_AT_tricks
     *
     *  (byte) 0xA0, (byte) 0xA4, (byte) 0x00 (byte) 0x00, (byte) 0x02, (byte) 0x6F, (byte) 0x07
     */

    /**
     *     Original:
     *     private static final byte[] ISD_AID = new byte[]{(byte) 0xA0, 0x00, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00};
     */


    /**
     * Experimented:
     */
    // private static final byte[] ISD_AID = new byte[]{(byte) 0xA0, (byte) 0xA4, (byte) 0x00, (byte) 0x00, (byte) 0x02, (byte) 0x6F, (byte) 0x07};
    private static final byte[] CMD_IMSI = new byte[]{(byte) 0xA0, (byte) 0xA4, (byte) 0x00, (byte) 0x00, (byte) 0x02, (byte) 0x6F, (byte) 0x07};
    private static final byte[] ISD_AID = new byte[]{(byte) 0x00, (byte) 0xCA, (byte) 0x00, (byte) 0x5A};

    private static String bytesToString(byte[] bytes) {
        StringBuffer sb = new StringBuffer();
        for (byte b : bytes)
            sb.append(String.format("%02x ", b & 0xFF));

        String stringOfByte = sb.toString();
        Log.e(TAG, "----------- stringOfByte " + stringOfByte);
        return stringOfByte;
    }

    private void logText(String message) {

        Log.d(TAG, "------------ message " + message);
        _scrollview.post(new Runnable() {
            public void run() {
                _scrollview.fullScroll(ScrollView.FOCUS_DOWN);
            }

        });

        _textview.append(message);
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        _scrollview = new ScrollView(this);
        _textview = new TextView(this);
        _textview.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        _scrollview.addView(_textview);
        layout.addView(_scrollview);

        setContentView(layout);


        telephonyManager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);

        /**
         * sending APDU using telephony.
         */

        /*       if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                 try {
                 logText(telephonyManager.iccTransmitApduBasicChannel(00, 00, 00, 00, 00, "Hello"));
                 } catch (Exception e) {
                 e.printStackTrace();
                 }
                 }*/


        /**
         * sending APDU using seek library.
         */

        SEServiceCallback callback = new SEServiceCallback();
        new SEService(this, callback);


    }

    /**
     * Callback interface if informs that this SEService is connected to the SmartCardService
     */
    public class SEServiceCallback implements SEService.CallBack {

        public void serviceConnected(SEService service) {
            _service = service;
            performTest();
        }
    }

    private void performTest() {
        Reader[] readers = _service.getReaders();
        logText("Available readers:  \n");
        for (Reader reader : readers)
            logText("	" + reader.getName() + "   - " + ((reader.isSecureElementPresent()) ? "present" : "absent") + "\n");

        if (readers.length == 0) {
            logText("No reader available \n");
            return;
        }

        for (Reader reader : readers) {
            if (!reader.isSecureElementPresent())
                continue;

            logText("\n--------------------------------\nSelected reader: \"" + reader.getName() + "\"\n");

            try {
                _session = reader.openSession();
            } catch (Exception e) {
                logText(e.getMessage());
            }
            if (_session == null)
                continue;

            try {
                byte[] atr = _session.getATR();
                logText("ATR: " + ((atr == null) ? "unavailable" : bytesToString(atr)) + "\n\n");
            } catch (Exception e) {
                logText("Exception on getATR(): " + e.getMessage() + "\n\n");
            }

            testBasicChannel(null);
            testBasicChannel(ISD_AID);

            testLogicalChannel(null);
            testLogicalChannel(ISD_AID);

            _session.close();
        }
    }

    void testBasicChannel(byte[] aid) {
        try {
            logText("BasicChannel test: " + ((aid == null) ? "default applet" : bytesToString(aid)) + "\n");
            Channel channel = _session.openBasicChannel(aid);

            // (byte)0xA0, (byte)0xA4, (byte)0x00, (byte)0x00, (byte)0x02, (byte)0x6F, (byte)0x7E
            // (byte)0xA0, (byte)0xA4, (byte)0x00, (byte)0x00, (byte)0x02, (byte)0x7F, (byte)0x20
            // (byte)0xA0, (byte)0xA4, (byte)0x00, (byte)0x00, (byte)0x02, (byte)0x7F, (byte)0x20
            byte[] cmd = new byte[]{(byte) 0x00, (byte) 0xCA, (byte) 0x00, (byte) 0x5A};
            //            byte[] cmd = new byte[]{(byte)0xA0, (byte)0xA4, (byte)0x00, (byte)0x00, (byte)0x02, (byte)0x6F, (byte)0x7E};
            //            byte[] cmd = new byte[]{(byte) 0xA0, (byte) 0xA4, (byte) 0x00, (byte) 0x00, (byte) 0x02, (byte) 0x6F, (byte) 0x07};
            // original           byte[] cmd = new byte[]{(byte) 0x80, (byte) 0xCA, (byte) 0x9F, 0x7F, 0x00};

            logText(" -> " + bytesToString(cmd) + "\n");
            byte[] rsp = channel.transmit(cmd);
            logText(" <- " + bytesToString(rsp) + "\n\n");

            channel.close();
        } catch (Exception e) {
            logText("Exception on BasicChannel: " + e.getMessage() + "\n\n");
        }
    }

    void testLogicalChannel(byte[] aid) {
        try {
            logText("LogicalChannel test: " + ((aid == null) ? "default applet" : bytesToString(aid)) + "\n");
            Channel channel = _session.openLogicalChannel(aid);

            byte[] cmd = new byte[]{(byte) 0x00, (byte) 0xCA, (byte) 0x00, (byte) 0x5A};
            //            byte[] cmd = new byte[]{(byte)0xA0, (byte)0xA4, (byte)0x00, (byte)0x00, (byte)0x02, (byte)0x6F, (byte)0x7E};
            // original            byte[] cmd = new byte[]{(byte) 0x80, (byte) 0xCA, (byte) 0x9F, 0x7F, 0x00};
            logText(" -> " + bytesToString(cmd) + "\n");
            byte[] rsp = channel.transmit(cmd);
            logText(" <- " + bytesToString(rsp) + "\n\n");

            channel.close();
        } catch (Exception e) {
            logText("Exception on LogicalChannel: " + e.getMessage() + "\n\n");
        }
    }

    @Override
    protected void onDestroy() {
        if (_service != null) {
            _service.shutdown();
        }
        super.onDestroy();
    }

}//MainActivity
