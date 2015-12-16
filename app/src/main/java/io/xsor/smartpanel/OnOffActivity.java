package io.xsor.smartpanel;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.pubnub.api.Callback;
import com.pubnub.api.Pubnub;
import com.pubnub.api.PubnubError;
import com.pubnub.api.PubnubException;

public class OnOffActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String SEP  = "_";
    
    private Pubnub pubnub;

    private String smartPanelCh = "SmartPanelCh";
    private String publishKey;
    private String subscribeKey;

    String UUID = "Samed-Nexus6P";

    Button on, off;

    String breakerName;
    String gpioPin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_on_off);

        publishKey = getString(R.string.publish_key);
        subscribeKey = getString(R.string.subscribe_key);

        on = (Button) findViewById(R.id.on);
        off = (Button) findViewById(R.id.off);

        on.setOnClickListener(this);
        off.setOnClickListener(this);

    }

    @Override
    protected void onResume() {
        super.onResume();
        breakerName = getIntent().getStringExtra("breakerName");
        gpioPin = getIntent().getStringExtra("gpioPin");
        connectToPubnub();
    }

    @Override
    protected void onStop() {
        super.onStop();
        pubnub.unsubscribe(smartPanelCh);
    }

    private void connectToPubnub() {

        pubnub = new Pubnub(publishKey, subscribeKey);
        pubnub.setUUID(UUID);

        try {
            pubnub.subscribe(smartPanelCh, callback);
        } catch (PubnubException e) {
            System.out.println(e.toString());
        }
    }

    Callback callback = new Callback() {
        @Override
        public void connectCallback(String channel, Object message) {
            log("CONNECT on channel:" + channel
                    + " : " + message.getClass() + " : "
                    + message.toString());
            sendMessage("getStatus" + SEP + breakerName);
        }

        @Override
        public void disconnectCallback(String channel, Object message) {
            log("DISCONNECT on channel:" + channel
                    + " : " + message.getClass() + " : "
                    + message.toString());
        }

        @Override
        public void reconnectCallback(String channel, Object message) {
            log("RECONNECT on channel:" + channel
                    + " : " + message.getClass() + " : "
                    + message.toString());
        }

        @Override
        public void successCallback(String channel, Object message) {
            log(channel + " : "
                    + message.getClass() + " : " + message.toString());
        }

        @Override
        public void errorCallback(String channel, PubnubError error) {
            log("ERROR on channel " + channel
                    + " : " + error.toString());
        }
    };

    @Override
    public void onClick(View view) {

        final EditText pinText = new EditText(this);

        switch(view.getId()) {
            case R.id.on:

                new AlertDialog.Builder(this)
                        .setTitle("Confirm Pin")
                        .setCancelable(false)
                        .setMessage("Please enter the 4-digit PIN to turn on this breaker")
                        .setView(pinText)
                        .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                sendMessage(breakerName + SEP + "on");
                                sendMessage(pinText.getText().toString());
                                pinText.setText("");
                            }
                        })
                        .setNeutralButton("Cancel",null)
                        .show();
                break;
            case R.id.off:

                new AlertDialog.Builder(this)
                        .setTitle("Set Pin")
                        .setCancelable(false)
                        .setMessage("Please enter a 4-digit PIN to turn off this breaker.")
                        .setView(pinText)
                        .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                sendMessage(breakerName + SEP + "off");
                                sendMessage(pinText.getText().toString());
                                pinText.setText("");
                            }
                        })
                        .setNeutralButton("Cancel",null)
                        .show();
                break;
        }

    }

    private void sendMessage(String message) {
        log("sendMessage:" + message);
        pubnub.publish(smartPanelCh, message, new Callback() {
            @Override
            public void successCallback(String channel, Object message) {
                super.successCallback(channel, message);
                log("Success: " + message);
            }

            @Override
            public void errorCallback(String channel, PubnubError error) {
                super.errorCallback(channel, error);
                log("Error: " + error);
            }
        });
    }

    private void log(String message) {
        Log.i(OnOffActivity.this.getClass().getSimpleName(), message);
    }
}
