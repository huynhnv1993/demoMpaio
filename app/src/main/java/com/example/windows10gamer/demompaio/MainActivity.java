package com.example.windows10gamer.demompaio;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.kyanogen.signatureview.SignatureView;
import com.samilcts.media.State;
import com.samilcts.sdk.mpaio.MpaioManager;
import com.samilcts.sdk.mpaio.command.MpaioCommand;
import com.samilcts.sdk.mpaio.error.ResponseError;
import com.samilcts.sdk.mpaio.ext.dialog.RxConnectionDialog;
import com.samilcts.sdk.mpaio.message.MpaioMessage;
import com.samilcts.util.android.Converter;
import com.samilcts.util.android.Logger;
import com.samilcts.util.android.ToastUtil;

import rx.Subscriber;

import android.media.MediaScannerConnection;
import android.os.Environment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private RxConnectionDialog connectionDialog;
    private MpaioManager mpaioManager;

    private EditText etCmd;
    private EditText etParam;
    private Button btnSend;
    private CheckBox cbHex;
    private Button btnBalance,btnRecharge,btnRefund,btnPurchase, btnLog, btnLength, btnInterval;
    private AppCompatSpinner spinner;

    private Logger logger = new Logger();
    private EditText etPrepaidAmount;
    private EditText etConfig;

    private Context mContext;

    private byte[] savedata = null;
    private Dialog alertbox,alertboxsign;

    Bitmap bitmap;
    Button clear,save;
    SignatureView signatureView;
    String path;
    private static final String IMAGE_DIRECTORY = "/signdemo";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_home);
        setSupportActionBar(toolbar);
        mContext = getApplicationContext();
        mpaioManager = new MpaioManager(getApplicationContext());
        connectionDialog = new RxConnectionDialog(this, mpaioManager);
        alertbox = showDialogx();
        alertboxsign = showDialogSign();
        EventButton();

        mpaioManager.onReceived()
                .subscribe(new Subscriber<byte[]>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(byte[] bytes) {

                    }
                });

        mpaioManager.onStateChanged()
                .subscribe(new Subscriber<State>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(State state) {
                        invalidateOptionsMenu();
                        ToastUtil.show(mContext, "state changed : " + state.getValue());
                    }
                });
        mpaioManager.onReadMsCard()//Receive notify of MS card data
                .subscribe(new Subscriber<MpaioMessage>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        String msg = null == e.getMessage() ? e.toString() : e.getMessage();
                        ToastUtil.show(mContext, "ERROR : " + msg);
                    }

                    @Override
                    public void onNext(MpaioMessage mpaioMessage) {
                        byte[] data = mpaioMessage.getData();

                        logger.i(TAG, "AID : " + Converter.toInt(mpaioMessage.getAID())
                                + " CMD : " + Converter.toHexString(mpaioMessage.getCommandCode())
                                + " Data : " + Converter.toHexString(mpaioMessage.getData()));
                        if (data != null && data != savedata){
                            savedata = data;

//                            Intent intent = new Intent(getApplicationContext(),Main2Activity.class);
//                            intent.putExtra("data",data);
//                            startActivity(intent);
                        }

                    }
                });

        mpaioManager
                .onBarcodeRead()
//                .mergeWith(mpaioManager.onReadMsCard())
                .mergeWith(mpaioManager.onReadEmvCard())
                .mergeWith(mpaioManager.onReadRfidCard())
                .mergeWith(mpaioManager.onPressPinPad())
                .mergeWith(mpaioManager.onNotifyPrepaidTransaction())
                .mergeWith(mpaioManager.onReadPrepaidTransactionLog())
                .subscribe(new Subscriber<MpaioMessage>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        String msg = null == e.getMessage() ? e.toString() : e.getMessage();
                        ToastUtil.show(mContext, "ERROR : " + msg);
                    }

                    @Override
                    public void onNext(MpaioMessage mpaioMessage) {
                        byte[] data = mpaioMessage.getData();

                        logger.i(TAG, "AID : " + Converter.toInt(mpaioMessage.getAID())
                                + " CMD : " + Converter.toHexString(mpaioMessage.getCommandCode())
                                + " Data : " + Converter.toHexString(mpaioMessage.getData()));

                        ToastUtil.show(mContext, "notify. data part : " + Converter.toHexString(data) );
                        ToastUtil.show(mContext, "(string) : " + (data == null ? "" : new String(data)));
                    }
                });
    }

    private Dialog showDialogSign() {
        final Dialog aDialogSign = new Dialog(this);
        aDialogSign.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        aDialogSign.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        aDialogSign.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        aDialogSign.setContentView(R.layout.dialog_sign);
        aDialogSign.setCancelable(false);
        Button btn_close = (Button) aDialogSign.findViewById(R.id.close_button);
        btn_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mpaioManager.rxSyncRequest(mpaioManager.getNextAid(), new MpaioCommand(MpaioCommand.STOP).getCode(), new byte[0])
                        .subscribe(getMessageSubscriber());
                aDialogSign.dismiss();
            }
        });
        signatureView = (SignatureView) aDialogSign.findViewById(R.id.signature_view);
        clear = (Button) aDialogSign.findViewById(R.id.clear);
        save = (Button) aDialogSign.findViewById(R.id.save);

        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signatureView.clearCanvas();
            }
        });

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bitmap = signatureView.getSignatureBitmap();
                path = saveImage(bitmap);
            }
        });

        return aDialogSign;
    }

    public String saveImage(Bitmap myBitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        myBitmap.compress(Bitmap.CompressFormat.JPEG, 90, bytes);
        File wallpaperDirectory = new File(
                Environment.getExternalStorageDirectory() + IMAGE_DIRECTORY /*iDyme folder*/);
        // have the object build the directory structure, if needed.
        if (!wallpaperDirectory.exists()) {
            wallpaperDirectory.mkdirs();
            Log.d("hhhhh",wallpaperDirectory.toString());
        }

        try {
            File f = new File(wallpaperDirectory, Calendar.getInstance()
                    .getTimeInMillis() + ".jpg");
            f.createNewFile();
            FileOutputStream fo = new FileOutputStream(f);
            fo.write(bytes.toByteArray());
            MediaScannerConnection.scanFile(MainActivity.this,
                    new String[]{f.getPath()},
                    new String[]{"image/jpeg"}, null);
            fo.close();
            Log.d("TAG", "File Saved::--->" + f.getAbsolutePath());

            return f.getAbsolutePath();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        return "";

    }

    private Dialog showDialogx(){
        final Dialog aDialog = new Dialog(this);
        aDialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        aDialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        aDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        aDialog.setContentView(R.layout.dialog_custom);
        aDialog.setCancelable(false);
        Button btn_close = (Button) aDialog.findViewById(R.id.close_button);
        Button btn_OK = (Button) aDialog.findViewById(R.id.buttonOK);
        btn_OK.setVisibility(View.INVISIBLE);
        btn_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mpaioManager.rxSyncRequest(mpaioManager.getNextAid(), new MpaioCommand(MpaioCommand.STOP).getCode(), new byte[0])
                        .subscribe(getMessageSubscriber());
                aDialog.dismiss();
            }
        });
        btn_OK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                aDialog.dismiss();
            }
        });

        return aDialog;
    }

    public static class MyCustomObject{
        public interface MyCustomObjectListener {
            // These methods are the different events and
            // need to pass relevant arguments related to the event triggered
            public void onObjectReady(String title);
        }
    }

    private void EventButton() {
        btnSend = (Button) findViewById(R.id.buttonSend);
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mpaioManager.isConnected()) {
                    connectionDialog.show();
                    return;
                }
                String cmdString = "0003";
                try {
                    short cmdShort = Short.parseShort(cmdString, 16);
                    byte[] cmd =  Converter.toBigEndianBytes(cmdShort);

                    String paramStr = "";
                    byte[] param;
                    param = paramStr.getBytes();

                    logger.i("param", " cmd : " + Converter.toHexString(cmd));
                    logger.i("param", " param : " + Converter.toHexString(param));
                    mpaioManager.rxSyncRequest(mpaioManager.getNextAid(), cmd, param)
                            .subscribe();
                    alertbox.show();
                }catch (NumberFormatException e) {
                    ToastUtil.show(getApplicationContext(), "Input valid number");
                    e.printStackTrace();
                }
                InputMethodManager imm = (InputMethodManager) getSystemService(
                        Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
        });
    }

    private Subscriber<MpaioMessage> getMessageSubscriber() {

        return new Subscriber<MpaioMessage>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

                e.printStackTrace();
                String msg = null == e.getMessage() ? e.toString() : e.getMessage();
                ToastUtil.show(mContext, "ERROR : " + msg);
            }

            @Override
            public void onNext(MpaioMessage mpaioMessage) {

                byte[] data = mpaioMessage.getData();

                if (ResponseError.fromCode(data[0]) == ResponseError.NO_ERROR) {
                    //response ok
                }


                logger.i(TAG, "AID : " + Converter.toInt(mpaioMessage.getAID())
                        + " CMD : " + Converter.toHexString(mpaioMessage.getCommandCode())
                        + " Data : " + Converter.toHexString((byte[]) mpaioMessage.getData()));

                ToastUtil.show(mContext, "received data part : " + Converter.toHexString(data));
                ToastUtil.show(mContext, "(string) : " + (data == null ? "" : new String(data)));

            }
        };
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);


        if ( null != connectionDialog && connectionDialog.isShowing()) {
            //this is for updating connection dialog state.
            connectionDialog.onRequestPermissionResult(requestCode, permissions, grantResults);
        }

    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        if ( mpaioManager != null) {
            boolean isConnected = mpaioManager.isConnected();
            MenuItem item = menu.findItem(R.id.action_connect);
            item.setTitle(isConnected ? "disconnect" : "connect");
            return true;
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_connect:
                Log.d("code", new MpaioCommand(MpaioCommand.READ_MS_CARD).getCode().toString());
                if ( mpaioManager.isConnected()) {

                    mpaioManager.disconnect();
                    return true;

                } else {

                    connectionDialog.show();

                }
        }
        return super.onOptionsItemSelected(item);
    }
}
