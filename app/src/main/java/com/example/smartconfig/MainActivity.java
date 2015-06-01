/*
    The MIT License (MIT)

    Copyright (c) 2015
        Stijn van Drunen (www.stijnvandrunen.nl),
        younger (https://github.com/youngBuger/esp8266-smartconfig)

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in
    all copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
    THE SOFTWARE.
*/

package com.example.smartconfig;

import android.app.Activity;
import android.app.AlertDialog;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.example.smartlink.R;

public class MainActivity extends Activity {

    private static String BTN_START = "Start Smartlink";
    private static String BTN_STOP = "Stop Smartlink";
    private EditText txt_ssid;
    private EditText txt_password;
    private EditText txt_mqtthost;
    private Button btn_smartlink;
    private WifiManager wm = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        wm = (WifiManager) getSystemService(WIFI_SERVICE);
        new UdpThread().start();

        txt_ssid = (EditText) findViewById(R.id.txt_ssid);
        txt_password = (EditText) findViewById(R.id.txt_password);
        txt_mqtthost = (EditText) findViewById(R.id.txt_mqtthost);

        btn_smartlink = (Button) findViewById(R.id.btn_smartlink);
        btn_smartlink.setText(BTN_START);

        txt_ssid.setText(getSSid());

        btn_smartlink.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btn_smartlink.getText().toString().equals(BTN_START)) {
                    if (txt_ssid.getText().toString().equals("")) {
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("Error")
                                .setMessage("SSID field cannot be empty.")
                                .show();
                        return;
                    }

                    UdpThread.send(txt_ssid.getText().toString(), txt_password.getText().toString(), txt_mqtthost.getText().toString());

                    btn_smartlink.setText(BTN_STOP);
                } else {
                    btn_smartlink.setText(BTN_START);
                }
            }
        });
    }

    private String getSSid() {
        if (wm != null) {
            WifiInfo wi = wm.getConnectionInfo();
            if (wi != null) {
                String s = wi.getSSID();
                Log.d("SmartlinkActivity", wi.getBSSID() + s.substring(1, s.length() - 1));
                if (s.length() > 2 && s.charAt(0) == '"' && s.charAt(s.length() - 1) == '"') {
                    return s.substring(1, s.length() - 1);
                }
            }
        }

        return "";
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
}
