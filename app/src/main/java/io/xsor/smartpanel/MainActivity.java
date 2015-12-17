package io.xsor.smartpanel;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.pubnub.api.Callback;
import com.pubnub.api.Pubnub;
import com.pubnub.api.PubnubError;
import com.pubnub.api.PubnubException;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, View.OnLongClickListener {

    private TinyDB db;
    private ArrayList<Object> breakerList = new ArrayList<>();

    private Pubnub pubnub;

    private String smartPanelCh = "SmartPanelCh";
    private String publishKey;
    private String subscribeKey;

    private int buttonId = 0;

    private final static String BREAKER_LIST = "BreakerList";

    private MaterialDialog GPIODialog;
    private MaterialDialog breakerNameDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Breakers");
        setSupportActionBar(toolbar);

        publishKey = getString(R.string.publish_key);
        subscribeKey = getString(R.string.subscribe_key);

        db = new TinyDB(this);

        breakerList = db.getListObject(BREAKER_LIST, Breaker.class);

        for (Object b : breakerList) {
            Breaker c = (Breaker) b;
            addBreakerButton(c.getName());
            log(b.toString());
        }
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

    private void connectToPubnub() {

        pubnub = new Pubnub(publishKey, subscribeKey);
        pubnub.setUUID("Samed-Nexus6P");

        try {
            pubnub.subscribe(smartPanelCh, subscribeCallback);
        } catch (PubnubException e) {
            System.out.println(e.toString());
        }
    }

    Callback subscribeCallback = new Callback() {
        @Override
        public void connectCallback(String channel, Object message) {
            log("subscribeCallback, CONNECT to:" + channel + " : " + message.toString());
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
        }
    };



    private void addBreakerButton(String c) {
        ContextThemeWrapper newContext = new ContextThemeWrapper(this, R.style.BrandButtonStyle);
        Button btnTag = new Button(newContext);
        btnTag.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        btnTag.setText(c);
        ((LinearLayout)findViewById(R.id.contentMain)).addView(btnTag);

        btnTag.setId(buttonId++);

        btnTag.setOnClickListener(this);
        btnTag.setOnLongClickListener(this);
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
            case R.id.add_breaker:
                addBreaker();
                return true;
            case R.id.shutdown:
                new MaterialDialog.Builder(this)
                        .title("Shutdown Server")
                        .content("Are you sure you want to shutdown the server?")
                        .positiveText("Yes")
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                                sendMessage("shutdown");
                            }
                        })
                        .neutralText("Cancel")
                        .show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
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

    private void addBreaker() {
        new MaterialDialog.Builder(this)
                .title("New Breaker")
                .content("Are you sure you want to remove this breaker?")
                .positiveText("Yes")
                .input("Breaker Name", null, new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(@NonNull MaterialDialog materialDialog, CharSequence input) {
                        final Breaker tempBreaker = new Breaker(input.toString());
                        new MaterialDialog.Builder(MainActivity.this)
                                .title("Set GPIO")
                                .content("Please enter GPIO number:")
                                .inputType(InputType.TYPE_CLASS_NUMBER)
                                .inputRangeRes(1, 2, R.color.md_red_500)
                                .input("3", null, new MaterialDialog.InputCallback() {
                                    @Override
                                    public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                                        tempBreaker.setGpioPin(input.toString());
                                        addBreakerButton(tempBreaker.getName());
                                        breakerList.add(tempBreaker);
                                        db.putListObject(BREAKER_LIST, breakerList);
                                    }
                                })
                                .positiveText("Confirm")
                                .neutralText("Cancel")
                                .show();
                    }
                })
                .positiveText("Next")
                .neutralText("Cancel")
                .show();
    }

    private void log(String message) {
        Log.i(MainActivity.this.getClass().getSimpleName(), message);
    }

    @Override
    public void onClick(View view) {
        Intent i = new Intent(this,OnOffActivity.class);
        Breaker b = (Breaker)breakerList.get(view.getId());
        i.putExtra("breakerName", b.getName());
        i.putExtra("GPIOPin", b.getGpioPin());

        log(b.toString());

        startActivity(i);
    }

    @Override
    public boolean onLongClick(final View view) {

        new MaterialDialog.Builder(this)
                .title("Remove Breaker")
                .content("Are you sure you want to remove this breaker?")
                .positiveText("Yes")
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                        breakerList.remove(view.getId());
                        db.putListObject(BREAKER_LIST, breakerList);
                        recreate();
                    }
                })
                .neutralText("Cancel")
                .show();

        return false;
    }
}
