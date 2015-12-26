package com.pandu.remotemouse;

import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;


public class Touchpad extends AppCompatActivity {

    private static Logger LOG = Logger.getLogger("Android Remote");

    private float x, y, oldX, oldY;
    private float dispX, dispY;

    private float x1, y1, x2, y2;
    private float oldx1, oldx2, oldy1, oldy2;
    private float olddistx, distx;
    private float dy1, dy2, dx1, dx2;

    private static final int TAP_NONE = 0;
    private static final int TAP_FIRST = 1;
    private static final int TAP_SECOND = 2;
    private static final int TAP_RIGHT = 5;

    private long lastTap = 0;
    private int tapState = TAP_NONE;
    private Timer tapTimer;

    private boolean isScrolling = false;
    private boolean valuesSet = false;

    private boolean oneFingerKeptBefore = false;
    private int pointerCount = 0;
    private int lastCount = 0;
    private RelativeLayout touchPad;
    private ImageButton showKeyboard;
    private EditText editTextContent;
    private InputMethodManager inputMethodManager;

    private DatagramSocket ds;
    private InetAddress addr;
    private int port;

    protected PowerManager.WakeLock mWakeLock;

    private void initializeVars(final String ip) {
        new Thread() {
            public void run() {
                try {
                    addr = InetAddress.getByName(ip);
                    port = getDefaultPort();
                } catch (UnknownHostException e) {
                    String problem = "There was a problem establishing connection";
                    Toast.makeText(Touchpad.this, problem, Toast.LENGTH_LONG).show();
                }
            }
        }.start();
    }

    private int getDefaultPort() {
        return Integer.parseInt(getString(R.string.port));
    }

    private void initializeControls() {
        LOG.info("In initializeControls");
        touchPad = (RelativeLayout) findViewById(R.id.relativeLayoutTouchpad);
        touchPad.setOnTouchListener(new MyOnTouchListener());

        editTextContent = (EditText) findViewById(R.id.editTextContent);
        editTextContent.setFocusable(true);
        editTextContent.setFocusableInTouchMode(true);
        editTextContent.setOnKeyListener(new MyKeyListener());
        editTextContent.addTextChangedListener(new MyKeyListener());
        editTextContent.setSelection(editTextContent.getText().length());
        showKeyboard = (ImageButton) findViewById(R.id.buttonShowKeyboard);
        showKeyboard.setOnClickListener(new ShowKeyboardListener());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle gotBundle = getIntent().getExtras();
        String ip = gotBundle.getString("IP");
        LOG.info("Got IP: " + ip);
        initializeVars(ip);
        setContentView(R.layout.activity_touchpad);
        initializeControls();
        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        this.mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "Android Remote");
        this.mWakeLock.acquire();
    }

    @Override
    protected void onPause() {
        super.onPause();
        new Thread() {
            public void run() {
                try {
                    ds.close();
                    mWakeLock.release();
                } catch (Exception e) {
                    String problem = "There was a problem closing the connection";
                    Toast.makeText(Touchpad.this, problem,
                            Toast.LENGTH_LONG).show();
                }

                try {
                    finalize();
                } catch (Throwable e) {
                    // do nothing
                }
            }
        }.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        new Thread() {
            public void run() {
                try {
                    ds = new DatagramSocket();
                    mWakeLock.acquire();
                } catch (SocketException e) {
                    String problem = "There was a problem establishing connection";
                    Toast.makeText(Touchpad.this, problem, Toast.LENGTH_LONG).show();
                }
            }
        }.start();
    }

    public void sendData(final String dataString) {
        LOG.info("Sending " + dataString);
        byte[] data = dataString.getBytes();
        try {
            ds.send(new DatagramPacket(data, data.length, addr, port));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        this.mWakeLock.release();
        super.onDestroy();
    }

    private class MyOnTouchListener implements View.OnTouchListener {

        public boolean onTouch(View v, final MotionEvent event) {
            LOG.info("Inside MyOnTouchListener.onTouch");
            new Thread() {
                public void run() {
                    pointerCount = event.getPointerCount();
                    LOG.info("Pointer Count: " + pointerCount);
                    LOG.info("Action Masked: " + event.getActionMasked());
                    LOG.info("Tap State: " +tapState);
                    switch (event.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN:
                        case MotionEvent.ACTION_POINTER_DOWN:
                            if (pointerCount == 1) {
                                if (tapState == TAP_NONE) {
                                    lastTap = System.currentTimeMillis();
                                    tapState = TAP_FIRST;
                                } else if (tapState == TAP_FIRST) {
                                    long now = System.currentTimeMillis();
                                    long elapsed = now - lastTap;
                                    if (elapsed < 300) {
                                        tapState = TAP_SECOND;
                                        tapTimer.cancel();
                                        sendData("clickhold");
                                    } else {
                                        tapState = TAP_NONE;
                                    }
                                    lastTap = System.currentTimeMillis();
                                }
                                oldX = event.getX();
                                oldY = event.getY();
                            } else if(pointerCount == 2) {
                                long now = System.currentTimeMillis();
                                long elapsed = now - lastTap;
                                if (tapState == TAP_NONE || elapsed < 300) {
                                    lastTap = System.currentTimeMillis();
                                    tapState = TAP_RIGHT;
                                }
                                oldX = event.getX();
                                oldY = event.getY();
                            }


                            break;

                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_POINTER_UP:
//                            if (pointerCount == 1) {
                            if (tapState == TAP_FIRST) {
                                long now = System.currentTimeMillis();
                                long elapsed = now - lastTap;
                                if (elapsed < 300) {
                                    tapTimer = new Timer();
                                    tapTimer.schedule(new TimerTask() {
                                        @Override
                                        public void run() {
                                            tapState = TAP_NONE;
                                            sendData("click");
                                        }
                                    }, 300);

                                    lastTap = System.currentTimeMillis();
                                } else {
                                    tapState = TAP_NONE;
                                    lastTap = 0;
                                }
                            } else if (tapState == TAP_SECOND) {
                                sendData("clickrelease");
                                tapState = TAP_NONE;
                                lastTap = 0;
                            }

                            else if (tapState == TAP_RIGHT) {
                                long now = System.currentTimeMillis();
                                long elapsed = now - lastTap;
                                if (elapsed < 300) {
                                    sendData("rightclick");
                                }
                                tapState = TAP_NONE;
                            }
                            // set to false when no finger on screen
                            oneFingerKeptBefore = false;
                            isScrolling = false;
                            valuesSet = false;
//                            }
                            break;

                        case MotionEvent.ACTION_MOVE:
                            if (pointerCount == 1 && tapState != TAP_RIGHT
                                    && !isScrolling) {
                                if (tapState == TAP_SECOND) {
                                    long now = System.currentTimeMillis();
                                    long elapsed = now - lastTap;
                                    if (elapsed > 50) {
                                        tapTimer.cancel();
                                        lastTap = 0;
                                    }
                                }

                                if (lastCount != 1) {
                                    oldX = event.getX();
                                    oldY = event.getY();
                                }

                                x = event.getX();
                                y = event.getY();
                                dispX = x - oldX;
                                dispY = y - oldY;
                                oldX = x;
                                oldY = y;
                                oneFingerKeptBefore = true;
                                sendData("moved " + dispX + " " + dispY);
                            } else if (pointerCount == 2 && tapState != TAP_RIGHT
                                    && !oneFingerKeptBefore) {
                                // oneFingerKeptBefore variable used to see that one finger is not kept previously and second kept later
                                tapState = TAP_RIGHT;
                                lastTap = System.currentTimeMillis();
                            } else if (pointerCount == 2 && !oneFingerKeptBefore) {
                                if (!valuesSet) {
                                    oldx1 = event.getX(0);
                                    oldy1 = event.getY(0);
                                    oldx2 = event.getX(1);
                                    oldy2 = event.getY(1);
                                    olddistx = Math.abs(oldx1 - oldx2);
                                    valuesSet = true;
                                    break;
                                }

                                x1 = event.getX(0);
                                y1 = event.getY(0);
                                x2 = event.getX(1);
                                y2 = event.getY(1);
                                distx = Math.abs(x1 - x2);

                                dx1 = oldx1 - x1;
                                dx2 = oldx2 - x2;
                                dy1 = oldy1 - y1;
                                dy2 = oldy2 - y2;
                                // ensure dx1, dx2, dy1, dy2 are not 0
                                if (dx1 == 0) {
                                    dx1 = 0.01f;
                                }
                                if (dx2 == 0) {
                                    dx2 = 0.01f;
                                }

                                float slope1 = Math.abs(dy1 / dx1);
                                float slope2 = Math.abs(dy2 / dx2);

                                // scrolling
                                if (Math.abs(olddistx - distx) < 10 && slope1 > 1
                                        && slope2 > 1) {
                                    isScrolling = true;
                                    if (dy1 > 0 && dy2 > 0)
                                        sendData("scroll " + (dy1 + dy2) / 2);
                                    else if (dy1 < 0 && dy2 < 0)
                                        sendData("scroll " + (dy1 + dy2) / 2);
                                }

                                oldx1 = x1;
                                oldy1 = y1;
                                oldx2 = x2;
                                oldy2 = y2;
                                olddistx = Math.abs(oldx1 - oldx2);

                            } else if (isScrolling && pointerCount == 1) {
                                x1 = event.getX();
                                y1 = event.getY();
                                dx1 = oldx1 - x1;
                                dy1 = oldy1 - y1;
                                if (dx1 == 0) {
                                    dx1 = 0.01f;
                                }
                                float slope1 = Math.abs(dy1 / dx1);
                                if (slope1 > 1) {
                                    if (dy1 > 00)
                                        sendData("scroll " + dy1);
                                    else if (dy1 < 0)
                                        sendData("scroll " + dy1);
                                }

                                oldx1 = x1;
                                oldy1 = y1;

                            }
                            break;
                    }
                    // sending the state for debugging purpose
                    // sendData(Integer.toString(tapState));

                    lastCount = pointerCount;

                }
            }.start();
            return true;
        }

    }

    public class MyKeyListener implements View.OnKeyListener, TextWatcher {

        public boolean onKey(View v, final int keyCode, final KeyEvent event) {
            LOG.info("Inside MyKeyListener.onKey");
            LOG.info("KeyCode: " + keyCode);
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                switch (keyCode) {
                    case KeyEvent.KEYCODE_VOLUME_UP:
                        sendData("keyin VK_RIGHT");
                        return true;
                    case KeyEvent.KEYCODE_VOLUME_DOWN:
                        sendData("keyin VK_LEFT");
                        return true;
                }
            } else if (event.getAction() == KeyEvent.ACTION_UP) {
                switch (keyCode) {
                    case KeyEvent.KEYCODE_BACK:
                        Intent i = new Intent(Touchpad.this,
                                MainActivity.class);
                        startActivity(i);
                        finish();
                        return true;
                }
            }
            return false;

        }

        public void afterTextChanged(Editable s) {

        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(final CharSequence s, final int start,
                                  final int before, final int count) {
            LOG.info("Inside MyKeyListener.onTextChanged");
            new Thread() {
                public void run() {
                    try {
                        String str = s.subSequence(start, start + count).toString();
                        if(count == 0) {
                            //Backspace
                            editTextContent.append("VK_BACKSPACE");
                            editTextContent.setSelection(editTextContent.getText().length());
                        }
                        char character = str.charAt(0);
                        int ascii = (int) character;

                        if(ascii == 10) {
                            str = "VK_ENTER";
                        }
                        if (str.equals(" ")) {
                            str = "VK_SPACE";
                        }

                        sendData("keyin " + str);
                    } catch (Exception e) {
                        // Toast.makeText(Main.this, e.toString() + " " + count,
                        // Toast.LENGTH_LONG).show();
                    }

                }
            }.start();
        }
    }

    private class ShowKeyboardListener implements View.OnClickListener {
        public void onClick(View v) {
            LOG.info("Inside ShowKeyboardListener.onClick");
            editTextContent.requestFocus();
            inputMethodManager = (InputMethodManager) Touchpad.this
                    .getApplicationContext().getSystemService(
                            Context.INPUT_METHOD_SERVICE);
            inputMethodManager.showSoftInput(editTextContent, 0);
        }

    }
}
