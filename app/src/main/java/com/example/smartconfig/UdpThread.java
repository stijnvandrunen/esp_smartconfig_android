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

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;


public class UdpThread extends Thread {

    public final static String TAG = "UdpThread";
    static Handler mUdpHandler;
    static DatagramSocket udpSocket = null;
    static DatagramPacket udpsendPacket = null;

    public static void send(String ssid, String pwd, String mqttHost) {
        Bundle bundle = new Bundle();

        bundle.putString("ssid", ssid);
        bundle.putString("pwd", pwd);
        bundle.putString("mqtthost", mqttHost);

        Message message = new Message();
        message.setData(bundle);

        mUdpHandler.sendMessage(message);
    }

    @Override
    public void run() {
        connect();

        Looper.prepare();

        mUdpHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Bundle data = msg.getData();
                send(data);
            }
        };

        Looper.loop();
    }

    private void connect() {
        try {
            udpSocket = new DatagramSocket(4560);
        } catch (Exception e) {
            // TODO: Exception should be handled
        }
    }

    private void send(Bundle bundle) {
        try {
            String ssid = bundle.getString("ssid");
            String pwd = bundle.getString("pwd");
            String mqtthost = bundle.getString("mqtthost");

            byte[] encodedBytes = getBytesForSmartConfig(ssid, pwd, mqtthost);
            encodedBytes = MakeCRC8(encodedBytes);
            byte[] encBuf = SmartLinkEncode(encodedBytes);
            byte[] dummybuf = new byte[18];
            int delayms = 5;
            long beginTime = System.currentTimeMillis();

            udpsendPacket = new DatagramPacket(dummybuf, dummybuf.length);
            udpsendPacket.setData(dummybuf);
            udpsendPacket.setPort(80);
            udpsendPacket.setAddress(InetAddress.getByName("255.255.255.255"));

            while (true) {
                delayms++;
                if (delayms > 27)
                    delayms = 20;
                for (byte anEncBuf : encBuf) {
                    udpsendPacket.setLength(anEncBuf);
                    udpSocket.send(udpsendPacket);
                    Thread.sleep(delayms);
                }
                Thread.sleep(200);
                if ((System.currentTimeMillis() - beginTime) / 1000 >= 25)
                    break;
            }
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    private byte[] SmartLinkEncode(byte[] src) {
        byte[] rtlval;
        int curidx = 0;
        rtlval = new byte[src.length * 10];
        byte tmp;
        for (int i = 0; i < src.length; i++) {
            rtlval[curidx++] = 0;
            rtlval[curidx++] = 0;
            tmp = (byte) (i & 0xF);
            rtlval[curidx++] = (byte) (tmp + 1);//pos_L
            rtlval[curidx++] = (byte) ((15 - tmp) + 1);//~pos_L
            tmp = (byte) ((i & 0xF0) >> 4);
            rtlval[curidx++] = (byte) (tmp + 1);//pos_H
            rtlval[curidx++] = (byte) ((15 - tmp) + 1);//~pos_H
            tmp = (byte) (src[i] & 0xF);
            rtlval[curidx++] = (byte) (tmp + 1);//val_L
            rtlval[curidx++] = (byte) ((15 - tmp) + 1);//~val_L
            tmp = (byte) ((src[i] & 0xF0) >> 4);
            rtlval[curidx++] = (byte) (tmp + 1);//val_H
            rtlval[curidx++] = (byte) ((15 - tmp) + 1);//~val_H
        }
        return rtlval;
    }

    byte[] getBytesForSmartConfig(String... values) {
        StringBuilder combinedStrings = new StringBuilder();
        for(String value : values) {
            combinedStrings.append(value);
            combinedStrings.append('\n');
        }
        return combinedStrings.toString().getBytes();
    }

    byte[] MakeCRC8(byte[] src) {
        byte crc = calcrc_bytes(src, src.length);
        byte[] rtlval = new byte[src.length + 1];
        System.arraycopy(src, 0, rtlval, 0, src.length);
        rtlval[src.length] = crc;
        return rtlval;
    }

    byte calcrc_bytes(byte[] p, int len) {
        byte crc = 0;
        int i = 0;
        while (i < len) {
            crc = (byte) calcrc_1byte(crc ^ p[i]);
            int j = crc & 0xff;
            Log.e(TAG, "crc=" + j);
            i++;
        }
        return crc;
    }

    int calcrc_1byte(int abyte) {
        int i, crc_1byte;
        crc_1byte = 0;
        for (i = 0; i < 8; i++) {
            if (((crc_1byte ^ abyte) & 0x01) > 0) {
                crc_1byte ^= 0x18;
                crc_1byte >>= 1;
                crc_1byte |= 0x80;
            } else {
                crc_1byte >>= 1;
            }
            abyte >>= 1;
        }
        return crc_1byte;
    }
}