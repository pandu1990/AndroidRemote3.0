package com.pandu.remotemouse;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.LinkedList;

/**
 * Created by pandu on 26/12/15.
 */
public class MainActivity extends AppCompatActivity {
    String filename;
    SharedPreferences prefs;
    int MAX_HOSTS;
    LinkedList<String> ipSet = new LinkedList<String>();
    ListView listViewIPAddresses;
    EditText editTextIPAddress;

    private void addIpToList(String ipAddress) {
        if (ipSet.contains(ipAddress)) {
            ipSet.remove(ipAddress);
        } else if (ipSet.size() == MAX_HOSTS) {
            ipSet.remove(MAX_HOSTS - 1);
        }
        ipSet.add(0, ipAddress);
        SharedPreferences.Editor e = prefs.edit();
        e.clear();
        for (int i = 0; i < ipSet.size(); i++) {
            e.putString("IP" + i, ipSet.get(i));
        }
        e.commit();
    }

    private String[] getIpsFromList() {
        String[] retStringArray = new String[ipSet.size()];
        for(int i = 0; i < ipSet.size(); i++){
            retStringArray[i] = ipSet.get(i);
        }
        return retStringArray;
    }

    private void setListValues(){
        for (int i = 0; i < MAX_HOSTS; i++) {
            if (prefs.contains("IP" + i)) {
                ipSet.add(prefs.getString("IP" + i, "value not set"));
            }
        }
        if (ipSet.size() > 0) {
            String[] listIps = getIpsFromList();
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                    android.R.layout.simple_list_item_1, listIps);
            listViewIPAddresses.setAdapter(adapter);
            findViewById(R.id.textViewRecentHosts).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.textViewRecentHosts).setVisibility(View.INVISIBLE);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        filename = (String) getString(R.string.app_name);
        MAX_HOSTS = Integer.parseInt(getString(R.string.max_hosts));

        prefs = getSharedPreferences(filename, 0);
        listViewIPAddresses = (ListView) findViewById(R.id.listViewIPAddresses);
        setListValues();

        listViewIPAddresses.setOnItemClickListener(new IPAddressItemListener());

        editTextIPAddress = (EditText) findViewById(R.id.editTextIPAddress);

        Button buttonConnect = (Button) findViewById(R.id.buttonConnect);
        buttonConnect.setOnClickListener(new ConnectButtonClickListener());

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuItemHelp:
                Toast.makeText(MainActivity.this, "Help", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.menuItemSettings:
                Toast.makeText(MainActivity.this, "Settings", Toast.LENGTH_SHORT).show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        finish();
    }

    private class IPAddressItemListener implements android.widget.AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            String ipAddress = ipSet.get(position);
            editTextIPAddress.setText(ipAddress);
        }
    }

    private class ConnectButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            String ipAddress = editTextIPAddress.getText().toString();
            if(isIpValid(ipAddress)) {
                addIpToList(ipAddress);
                Bundle bundle = new Bundle();
                bundle.putString("IP", ipAddress);
                Intent i = new Intent(MainActivity.this, Touchpad.class);
                i.putExtras(bundle);
                startActivity(i);
            } else {
                Toast.makeText(MainActivity.this, "Invalid IP Address", Toast.LENGTH_SHORT).show();
            }
        }

        private boolean isIpValid(String ip) {
            if (ip.matches("[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+")) {
                String[] ipParts = ip.split(".");
                for (String s : ipParts) {
                    int i = Integer.parseInt(s);
                    if (i > 255) {
                        return false;
                    }
                }
            } else {
                return false;
            }
            return true;
        }
    }
}
