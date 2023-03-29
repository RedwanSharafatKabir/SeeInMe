package org.tensorflow.lite.examples.classification;

import static org.tensorflow.lite.examples.classification.MyDatabaseHelper.TABLE_NAME;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.speech.RecognizerIntent;
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
    RelativeLayout relativeLayout;
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
        relativeLayout = findViewById(R.id.relativeLayoutId);
        relativeLayout.setOnClickListener(this);

        Cursor cursor =  myDatabaseHelper.retrieveData();

        try {
            if (cursor.getCount() == 0) {
                Toast.makeText(this, "Cart is empty", Toast.LENGTH_LONG).show();

                textToSpeech = new TextToSpeech(CartActivity.this, new TextToSpeech.OnInitListener() {
                    @Override
                    public void onInit(int status) {
                        textToSpeech.setLanguage(Locale.US);
                        textToSpeech.setSpeechRate((float) 0.5);
                        textToSpeech.speak("Total cost "+sum+"taka", TextToSpeech.QUEUE_FLUSH, null, null);
                    }
                });
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

                    textToSpeech = new TextToSpeech(CartActivity.this, new TextToSpeech.OnInitListener() {
                        @Override
                        public void onInit(int status) {
                            textToSpeech.setLanguage(Locale.US);
                            textToSpeech.setSpeechRate((float) 0.5);
                            textToSpeech.speak("Total cost "+sum+"taka", TextToSpeech.QUEUE_FLUSH, null, null);
                        }
                    });
                }

            }

        } catch (Exception e){
            Log.i("Error", e.getMessage());
        }
    }

    @Override
    public void onClick(View v) {
        if(v.getId()==R.id.LinearLayoutId || v.getId()==R.id.firstLinearLayoutId ||
                v.getId()==R.id.toolBarId || v.getId()==R.id.relativeLayoutId){

            voiceOn();
        }
    }

    private void voiceOn(){
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        try {
            startActivityForResult(intent, 10);

        } catch (Exception e){
            Log.d("Error_Device ", e.getMessage());
            Toast.makeText(getApplicationContext(), "Your Device Doesn't Support Voice Command", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){
            case 10:
                if(resultCode==RESULT_OK && data!=null){
                    ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                    if(results.get(0).equals("delete product") || results.get(0).equals("delete cart") ||
                        results.get(0).equals("Delete product") || results.get(0).equals("Delete cart") ||
                        results.get(0).equals("Clear") || results.get(0).equals("clear") ||
                        results.get(0).equals("Clear all") || results.get(0).equals("clear all") ||
                        results.get(0).equals("Clear product") || results.get(0).equals("clear product") ||
                        results.get(0).equals("Clear products") || results.get(0).equals("clear products") ||
                        results.get(0).equals("Clear cart") || results.get(0).equals("clear cart") ||
                        results.get(0).equals("Remove cart") || results.get(0).equals("remove cart") ||
                        results.get(0).equals("Remove product") || results.get(0).equals("remove product") ||
                        results.get(0).equals("Remove products") || results.get(0).equals("remove products") ||
                        results.get(0).equals("Product delete korun") || results.get(0).equals("product delete korun") ||
                        results.get(0).equals("Delete korun") || results.get(0).equals("delete korun")){

                        myDatabaseHelper.deleteData();

                        itemNameText.setText("");
                        itemPriceText.setText("");
                        priceTV.setText("0");

                        textToSpeech.speak("items deleted", TextToSpeech.QUEUE_FLUSH, null, null);

                    }

                    else if(results.get(0).equals("exit") || results.get(0).equals("Exit") || results.get(0).equals("exit app") ||
                            results.get(0).equals("Exit app") || results.get(0).equals("Close") || results.get(0).equals("close") ||
                            results.get(0).equals("Close app") || results.get(0).equals("close app") ||
                            results.get(0).equals("App bondho korun") || results.get(0).equals("app bondho korun")){

                        myDatabaseHelper.deleteData();
                        finish();
                        System.exit(0);
                    }
                }

                break;
        }
    }
}
