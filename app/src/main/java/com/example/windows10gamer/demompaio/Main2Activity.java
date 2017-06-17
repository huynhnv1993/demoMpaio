package com.example.windows10gamer.demompaio;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.samilcts.util.android.Converter;

public class Main2Activity extends AppCompatActivity {
    private TextView txtdatahex,txtdatastring;
    byte[] data = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        txtdatahex = (TextView) findViewById(R.id.textviewdatahex);
        txtdatastring = (TextView) findViewById(R.id.textviewdatastring);

        data = getIntent().getByteArrayExtra("data");
        txtdatahex.setText("Data hexa: " + Converter.toHexString(data));
        txtdatastring.setText((data == null ? "" : new String(data)));
    }
}
