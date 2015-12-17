package io.xsor.smartpanel;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.pubnub.api.Callback;
import com.pubnub.api.Pubnub;
import com.pubnub.api.PubnubError;
import com.pubnub.api.PubnubException;

import org.json.JSONException;
import org.json.JSONObject;

public class OnOffActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String SEP = "_";
    private static final String SERVER_UUID = "Matt-RaspberryPi";
    
    private Pubnub pubnub;

    private String smartPanelCh = "SmartPanelCh";
    private String publishKey;
    private String subscribeKey;

    String UUID = "Samed-Nexus6P";

    Button on, off;

    String breakerName;
    String gpioPin;

    // Message structure for processing on Raspberry Pi:
    // BREAKERNAME_GPIO_ACTION_CODE

    int ON = 1;
    int OFF = 0;
    int STATUS = 2;

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

        breakerName = getIntent().getStringExtra("breakerName");
        gpioPin = getIntent().getStringExtra("gpioPin");
        connectToPubnub();

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
        pubnub.unsubscribePresence(smartPanelCh);
        pubnub.unsubscribe(smartPanelCh);
    }

    private void connectToPubnub() {

        pubnub = new Pubnub(publishKey, subscribeKey);
        pubnub.setUUID(UUID);

        try {
            pubnub.subscribe(smartPanelCh, subscribeCallback);
            pubnub.presence(smartPanelCh, presenceCallback);
        } catch (PubnubException e) {
            System.out.println(e.toString());
        }
    }

    Callback subscribeCallback = new Callback() {
        @Override
        public void connectCallback(String channel, Object message) {
            log("subscribeCallback, CONNECT to:" + channel + " : " + message.toString());
            sendMessage(breakerName + SEP
                    + gpioPin + SEP
                    + STATUS);
        }

        @Override
        public void disconnectCallback(String channel, Object message) {
            log("subscribeCallback, DISCONNECT from:" + channel
                    + " : " + message.toString());
        }

        @Override
        public void reconnectCallback(String channel, Object message) {
            log("subscribeCallback, RECONNECT to:" + channel
                    + " : " + message.toString());
        }

        @Override
        public void successCallback(String channel, Object message) {
            log("subscribeCallback, SUCCESS on " + channel + " : " + message.toString());
        }

        @Override
        public void errorCallback(String channel, PubnubError error) {
            log("subscribeCallback, ERROR on " + channel + " : " + error.toString());
        }
    };

    Callback presenceCallback = new Callback() {
        @Override
        public void connectCallback(String channel, Object message) {
            super.connectCallback(channel, message);

            log("presenceCallback, CONNECT to " + channel + " : " + message.toString());
            pubnub.hereNow(smartPanelCh, new Callback() {
                @Override
                public void successCallback(String channel, Object message) {
                    super.successCallback(channel, message);

                    JSONObject jo = (JSONObject) message;

                    try {
                        jo.getJSONArray("uuids");
                        log("Array of uuids: " + jo.getJSONArray("uuids").toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public void successCallback(String channel, Object message) {
            log("presenceCallback, SUCCESS on " + channel + " : " + message.toString());
        }

        @Override
        public void errorCallback(String channel, PubnubError error) {
            log("presenceCallback, ERROR on " + channel + " : " + error.toString());
        }
    };

    @Override
    public void onClick(View view) {

        switch(view.getId()) {
            case R.id.on:

                new MaterialDialog.Builder(this)
                        .title("Confirm PIN")
                        .content("Please enter the PIN for this breaker.")
                        .inputType(InputType.TYPE_CLASS_NUMBER)
                        .inputRangeRes(4, 4, R.color.md_red_500)
                        .input("0000", null, new MaterialDialog.InputCallback() {
                            @Override
                            public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                                sendMessage(breakerName + SEP
                                        + gpioPin + SEP
                                        + ON + SEP
                                        + input);
                            }
                        })
                        .positiveText("Confirm")
                        .neutralText("Cancel")
                        .show();
                break;
            case R.id.off:

                new MaterialDialog.Builder(this)
                        .title("Set PIN")
                        .content("Please set a PIN for this breaker.")
                        .inputType(InputType.TYPE_CLASS_NUMBER)
                        .inputRangeRes(4, 4, R.color.md_red_500)
                        .input("0000", null, new MaterialDialog.InputCallback() {
                            @Override
                            public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                                sendMessage(breakerName + SEP
                                        + gpioPin + SEP
                                        + OFF + SEP
                                        + input);
                            }
                        })
                        .positiveText("Confirm")
                        .neutralText("Cancel")
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
