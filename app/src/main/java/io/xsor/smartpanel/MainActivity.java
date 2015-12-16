package io.xsor.smartpanel;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, View.OnLongClickListener {

    private TinyDB db;
    private ArrayList<Object> breakerList = new ArrayList<>();

    private int buttonId = 0;

    private final static String BREAKER_LIST = "BreakerList";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle("Breakers");

        db = new TinyDB(this);

        breakerList = db.getListObject(BREAKER_LIST, Breaker.class);

        for (Object b : breakerList) {
            Breaker c = (Breaker) b;
            addBreakerButton(c.getName());
            log(b.toString());

        }
    }

    private void addBreakerButton(String c) {
        Button btnTag = new Button(this);
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
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void addBreaker() {
        LinearLayout lila1= new LinearLayout(this);
        lila1.setOrientation(LinearLayout.VERTICAL); //1 is for vertical orientation
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        int margin = (int) getResources().getDimension(R.dimen.activity_horizontal_margin);

        layoutParams.setMargins(margin,margin,0,margin);
        lila1.setLayoutParams(layoutParams);

        final EditText name = new EditText(this);
        final EditText gpioPin = new EditText(this);
        name.setHint("Breaker Name");
        gpioPin.setHint("GPIO Pin");
        lila1.addView(name);
        lila1.addView(gpioPin);

        new AlertDialog.Builder(this)
                .setTitle("New Breaker")
                .setCancelable(false)
                .setView(lila1)
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Breaker b = new Breaker(name.getText().toString(),gpioPin.getText().toString());
                        addBreakerButton(b.getName());
                        breakerList.add(b);
                        db.putListObject(BREAKER_LIST,breakerList);
                    }
                })
                .setNeutralButton("Cancel",null)
                .show();

    }

    private void log(String message) {
        Log.i(MainActivity.this.getClass().getSimpleName(), message);
    }

    @Override
    public void onClick(View view) {
        Intent i = new Intent(this,OnOffActivity.class);
        Breaker b = (Breaker)breakerList.get(view.getId());
        i.putExtra("breakerName",b.getName());
        i.putExtra("gpioPin",b.getGpioPin());

        log(b.toString());

        startActivity(i);
    }

    @Override
    public boolean onLongClick(final View view) {
        new AlertDialog.Builder(this)
                .setTitle("Remove Breaker")
                .setCancelable(false)
                .setMessage("Are you sure you want to remove this breaker?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        breakerList.remove(view.getId());
                        db.putListObject(BREAKER_LIST, breakerList);
                        recreate();
                    }
                })
            .setNegativeButton("No",null)
            .show();

        return false;
    }
}
