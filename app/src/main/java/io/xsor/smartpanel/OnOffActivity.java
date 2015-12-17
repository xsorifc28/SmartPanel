package io.xsor.smartpanel;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.pubnub.api.Callback;
import com.pubnub.api.Pubnub;
import com.pubnub.api.PubnubError;
import com.pubnub.api.PubnubException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class OnOffActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String SEP = "_";
    private static final String SERVER_UUID = "Matt-RaspberryPi";
    private static final String STATUS_STR = "status";

    private Pubnub pubnub;

    private String smartPanelCh = "SmartPanelCh";
    private String publishKey;
    private String subscribeKey;

    private String breakerName;
    private String GPIOPin;

    private Button on, off;

    // Message structure for processing on Raspberry Pi:
    // BREAKERNAME_GPIO_ACTION_CODE

    int ON = 1;
    int OFF = 0;
    int STATUS = 2;

    private MaterialDialog noServerDialog;

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
        GPIOPin = getIntent().getStringExtra("GPIOPin");

        noServerDialog = new MaterialDialog.Builder(OnOffActivity.this)
                .title("Error")
                .content("Server not found")
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                        finish();
                    }
                })
                .positiveText("Exit")
                .cancelable(false)
                .build();

    }

    @Override
    protected void onResume() {
        super.onResume();
        connectToPubnub();
    }

    @Override
    protected void onStop() {
        super.onStop();
        pubnub.unsubscribePresence(smartPanelCh);
        pubnub.unsubscribe(smartPanelCh);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.shutdown:
                sendMessage("shutdown");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void connectToPubnub() {

        pubnub = new Pubnub(publishKey, subscribeKey);
        pubnub.setUUID("Samed-Nexus6P");

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
                    + GPIOPin + SEP
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
            //log("Message has been received: ");
            String[] splitMsg = message.toString().split(SEP);
            if(splitMsg[0].equals(STATUS_STR)) {
                if(Integer.valueOf(splitMsg[2]) == OFF) {
                    runOnUiThread(new Runnable() {
                        @SuppressLint("SetTextI18n")
                        @Override
                        public void run() {
                            off.setEnabled(false);
                            off.setText("[Off]");
                            on.setEnabled(true);
                            on.setText("On");
                        }
                    });

                } else if(Integer.valueOf(splitMsg[2]) == ON) {
                    runOnUiThread(new Runnable() {
                        @SuppressLint("SetTextI18n")
                        @Override
                        public void run() {
                            off.setEnabled(true);
                            off.setText("Off");
                            on.setEnabled(false);
                            on.setText("[On]");
                        }
                    });

                }
            }
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
            checkServerPresence();
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

    private void checkServerPresence() {
        pubnub.hereNow(smartPanelCh, new Callback() {
            @Override
            public void successCallback(String channel, Object message) {
                super.successCallback(channel, message);

                JSONObject jo = (JSONObject) message;

                try {
                    JSONArray uuids = jo.getJSONArray("uuids");
                    log("Array of uuids: " + uuids.toString());
                    if(!uuidExists(uuids,SERVER_UUID)) {
                        log("Matt not found.");
                        runOnUiThread(new Runnable() {
                            public void run() {
                                noServerDialog.show();
                            }
                        });
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private boolean uuidExists(JSONArray jsonArray, String usernameToFind){
        return jsonArray.toString().contains(usernameToFind);
    }

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
                                        + GPIOPin + SEP
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
                                        + GPIOPin + SEP
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
                log("Message Sent: " + message);
            }

            @Override
            public void errorCallback(String channel, PubnubError error) {
                super.errorCallback(channel, error);
                log("Error sending message: " + error);
            }
        });
    }

    private void log(String message) {
        Log.i(OnOffActivity.this.getClass().getSimpleName(), message);
    }
}
