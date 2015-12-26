package com.pandu.remotemouse;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

/**
 * Created by pandu on 26/12/15.
 */
public class SettingsActivity extends AppCompatActivity {

    private static Logger LOG = Logger.getLogger("SettingsActivity");

    Button buttonResetPrefs;
    SeekBar seekBarNumHosts;
    SharedPreferences prefs;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        prefs = getSharedPreferences(getString(R.string.app_name), 0);
        buttonResetPrefs = (Button) findViewById(R.id.buttonResetPreferences);
        buttonResetPrefs.setOnClickListener(new ResetPreferencesListener());
        seekBarNumHosts = (SeekBar) findViewById(R.id.seekBarHosts);
        seekBarNumHosts.setProgress(prefs.getInt("MAX_HOSTS", Integer.parseInt(getString(R.string.max_hosts))));
        seekBarNumHosts.setOnSeekBarChangeListener(new NumHostsSeekBarChangeListener());

        setTitle("Settings");
    }

    @Override
    public void onBackPressed() {
        Intent i = new Intent(this, MainActivity.class);
        startActivity(i);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                Intent i = new Intent(this, MainActivity.class);
                startActivity(i);
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class ResetPreferencesListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {

            SharedPreferences.Editor e = prefs.edit();
            e.clear();
            e.commit();
            onBackPressed();
        }
    }

    private class NumHostsSeekBarChangeListener implements SeekBar.OnSeekBarChangeListener {

        @Override
        public void onProgressChanged(SeekBar seekBar, final int progress, boolean fromUser) {
            LOG.info("Progress " + progress);

            SharedPreferences.Editor e = prefs.edit();
            e.putInt("MAX_HOSTS", progress);
            e.commit();


        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            Toast.makeText(SettingsActivity.this, "Value set to " + seekBar.getProgress(), Toast.LENGTH_SHORT).show();
        }
    }
}
