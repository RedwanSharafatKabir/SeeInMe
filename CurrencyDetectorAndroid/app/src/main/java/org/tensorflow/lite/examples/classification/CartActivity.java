package org.tensorflow.lite.examples.classification;

import static org.tensorflow.lite.examples.classification.MyDatabaseHelper.TABLE_NAME;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import java.util.ArrayList;
import java.util.Locale;

import code.fortomorrow.easysharedpref.EasySharedPref;

public class CartActivity extends AppCompatActivity implements View.OnClickListener{

    private TextView priceTV, itemNameText, itemPriceText;
    MyDatabaseHelper myDatabaseHelper;
    int sum = 0, id = 0;
    private TextToSpeech textToSpeech;
    LinearLayout linearLayout, linearLayout2;
    Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        myDatabaseHelper = new MyDatabaseHelper(this);
        itemNameText = findViewById(R.id.itemNameId);
        itemPriceText = findViewById(R.id.itemPriceId);
        priceTV = findViewById(R.id.priceTVId);

        linearLayout = findViewById(R.id.LinearLayoutId);
        linearLayout.setOnClickListener(this);
        linearLayout2 = findViewById(R.id.firstLinearLayoutId);
        linearLayout2.setOnClickListener(this);
        toolbar = findViewById(R.id.toolBarId);
        toolbar.setOnClickListener(this);

        Cursor cursor =  myDatabaseHelper.retrieveData();

        textToSpeech = new TextToSpeech(CartActivity.this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                textToSpeech.setLanguage(Locale.US);
                textToSpeech.setSpeechRate((float) 0.5);
            }
        });

        try {
            if (cursor.getCount() == 0) {
                Toast.makeText(this, "Cart is empty", Toast.LENGTH_LONG).show();
            }

        } catch (Exception e){
            Log.i("Error_db", e.getMessage());
        }

        StringBuffer stringBuffer1 = new StringBuffer();
        StringBuffer stringBuffer2 = new StringBuffer();

        try {
            while (cursor.moveToNext()){
                stringBuffer1.append(cursor.getString(1) + "\n\n");
                stringBuffer2.append(cursor.getString(2) + " tk\n\n");

                itemNameText.setText(stringBuffer1.toString());
                itemPriceText.setText(stringBuffer2.toString());

                id = Integer.parseInt(cursor.getString(0));
                sum += Integer.parseInt(cursor.getString(2));
                if(id==cursor.getCount()){
                    priceTV.setText(sum + " tk");
                }

            }

        } catch (Exception e){
            Log.i("Error", e.getMessage());
        }
    }

    @Override
    public void onClick(View v) {
        if(v.getId()==R.id.LinearLayoutId){
            textToSpeech.speak("Total cost "+sum+"taka", TextToSpeech.QUEUE_FLUSH, null, null);
        }

        if(v.getId()==R.id.firstLinearLayoutId){
            textToSpeech.speak("Total cost "+sum+"taka", TextToSpeech.QUEUE_FLUSH, null, null);
        }

        if(v.getId()==R.id.toolBarId){
            textToSpeech.speak("Total cost "+sum+"taka", TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }
}
