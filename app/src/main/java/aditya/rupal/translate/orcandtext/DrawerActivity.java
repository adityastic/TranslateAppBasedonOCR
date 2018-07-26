package aditya.rupal.translate.orcandtext;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.Text;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextDetector;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import aditya.rupal.translate.orcandtext.adapter.TranslationRecyclerAdapter;
import aditya.rupal.translate.orcandtext.data.SortText;
import aditya.rupal.translate.orcandtext.data.TranslationData;
import aditya.rupal.translate.orcandtext.utils.ImageUtils;

import static android.view.View.GONE;

public class DrawerActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, ImageUtils.ImageAttachmentListener {

    ImageUtils imageutils;
    ImageView sq;

    RecyclerView mRecyclerView;
    TranslationRecyclerAdapter mAdapter;

    File file;

    int com = 0;
    RequestQueue requestQueue;
    ArrayList<TranslationData> result;

    LinearLayout fragOCR, fragTrans;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        imageutils.request_permission_result(requestCode, permissions, grantResults);
    }

    @Override
    public void image_attachment(int from, String filename, Bitmap file, Uri uri) {
        String path = Environment.getExternalStorageDirectory() + File.separator + "PersonalOCR" + File.separator;
        imageutils.createImage(file, filename, path, false);

        Bitmap imageBitmap = file;

        if (imageBitmap != null) {

            ((ImageView) findViewById(R.id.img)).setImageBitmap(imageBitmap);

            FirebaseVisionTextDetector textRecognizer = FirebaseVision.getInstance().getVisionTextDetector();
            FirebaseVisionImage fn = FirebaseVisionImage.fromBitmap(imageBitmap);
            Task<FirebaseVisionText> fvresult = textRecognizer.detectInImage(fn).addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                @Override
                public void onSuccess(FirebaseVisionText results) {
                    List<FirebaseVisionText.Block> block = results.getBlocks();

                    Log.e("Request Recieved", "YES");
                    SortText sortText = new SortText();

                    for (int i = 0; i < block.size(); i++) {
                        List<FirebaseVisionText.Line> lines = block.get(i).getLines();
                        for (int j = 0; j < lines.size(); j++) {
                            sortText.add(lines.get(j));
                        }
                    }

                    sortText.sort();

                    result.clear();
                    ((LinearLayout) findViewById(R.id.resultLayout)).setVisibility(View.VISIBLE);
                    ((TextView) findViewById(R.id.imgselect)).setVisibility(GONE);
                    for (aditya.rupal.translate.orcandtext.data.Text t : sortText.getAllText()) {
                        String str = "";
                        for (FirebaseVisionText.Line e : t.getText()) {
                            str += e.getText() + " ";
                        }
                        TranslationData td = new TranslationData(str, "");
                        result.add(td);

                        JsonObjectRequest request = null;
                        try {
                            String origlink = String.format("https://script.google.com/macros/s/AKfycbwb8tHeko9lBq3l7xkm8Aa0qQVtXg9FJGH7tSDrDtyLDE98zAY/exec?q=%s&target=%s&id=%s", URLEncoder.encode(str, "UTF-8"), languages.get(spinner.getSelectedItem().toString()), result.indexOf(td));

                            request = new JsonObjectRequest(Request.Method.GET, origlink, null, new Response.Listener<JSONObject>() {
                                @Override
                                public void onResponse(JSONObject response) {
                                    try {
                                        int id = Integer.parseInt(response.get("id").toString());
                                        result.set(id, new TranslationData(result.get(id).original, response.get("translatedText").toString()));
                                        mAdapter.notifyDataSetChanged();
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }, new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    Log.e("VolleyError", error.toString());
                                }
                            });
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                        requestQueue.add(request);
                    }
                    mAdapter.notifyDataSetChanged();
                }
            }).addOnFailureListener(
                    new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e("Failed", "Yes");
                        }
                    });
        }
    }


    Toolbar mToolbar;

    @SuppressLint("HandlerLeak")
    @Override
    protected void onActivityResult(int requestCode, final int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        imageutils.onActivityResult(requestCode, resultCode, data);
    }

    Spinner spinner, firstSpinner, lastSpinner;
    Button translateTextButton;
    EditText transQuery;
    TextView transAns;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drawer);

        sq = findViewById(R.id.img);
        mToolbar = findViewById(R.id.toolbar);
        spinner = findViewById(R.id.spinnerTrans);
        firstSpinner = findViewById(R.id.spinnerFirst);
        translateTextButton = findViewById(R.id.translateButton);
        lastSpinner = findViewById(R.id.spinnerLast);
        transQuery = findViewById(R.id.translateQuery);
        transAns = findViewById(R.id.translateAns);
        fragOCR = findViewById(R.id.OCRFragment);
        fragTrans = findViewById(R.id.TranslateFragment);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);

        mToolbar.setNavigationIcon(R.drawable.ic_toolbar_navigation_4);
        setSupportActionBar(mToolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, mToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        toggle.setDrawerIndicatorEnabled(false);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        navigationView.setCheckedItem(R.id.nav_ocr);
        navigationView.getMenu().performIdentifierAction(R.id.nav_ocr, 0);

        setUpToolbar();
        setUpMap();
        setUpSpinner();

        result = new ArrayList<>();
        mRecyclerView = findViewById(R.id.resultRecyc);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new TranslationRecyclerAdapter(this, result);
        mRecyclerView.setAdapter(mAdapter);

        ((LinearLayout) findViewById(R.id.resultLayout)).setVisibility(View.GONE);
        ((TextView) findViewById(R.id.imgselect)).setVisibility(View.VISIBLE);

        requestQueue = Volley.newRequestQueue(this);

        imageutils = new ImageUtils(this);

        sq.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("HandlerLeak")
            @Override
            public void onClick(View v) {
                imageutils.imagepicker(1);
            }
        });
        translateTextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                JsonObjectRequest request = null;
                try {
                    request = new JsonObjectRequest(Request.Method.GET, String.format("https://script.google.com/macros/s/AKfycbwb8tHeko9lBq3l7xkm8Aa0qQVtXg9FJGH7tSDrDtyLDE98zAY/exec?q=%s&target=%s&id=%s&source=%s", URLEncoder.encode(transQuery.getText().toString(), "UTF-8"), languages.get(lastSpinner.getSelectedItem().toString()), -1 + "", languages.get(firstSpinner.getSelectedItem().toString())), null, new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                transAns.setText(response.get("translatedText").toString());
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.e("VolleyError", error.toString());
                        }
                    });
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                requestQueue.add(request);
            }
        });
    }


    private void setUpSpinner() {
        ArrayList<String> trannames = new ArrayList<>(languages.keySet());
        Collections.sort(trannames);
        trannames.remove("Automatic");
        trannames.add(0, "Automatic");
        ArrayAdapter<String> spinad = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, trannames);
        spinner.setAdapter(spinad);

        ArrayList<String> firstSpinnerAdapter = new ArrayList<>(languages.keySet());
        Collections.sort(firstSpinnerAdapter);
        firstSpinnerAdapter.remove("Automatic");
        firstSpinnerAdapter.add(0, "Automatic");
        ArrayAdapter<String> firstspinad = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, firstSpinnerAdapter);

        firstSpinner.setAdapter(firstspinad);

        ArrayList<String> lastSpinnerAdapter = new ArrayList<>(languages.keySet());
        Collections.sort(lastSpinnerAdapter);
        lastSpinnerAdapter.remove("Spanish");
        lastSpinnerAdapter.add(0, "Spanish");
        ArrayAdapter<String> secondSpinAdap = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, lastSpinnerAdapter);

        lastSpinner.setAdapter(secondSpinAdap);
    }

    private void setUpToolbar() {
        setTitle("Translate");
        setSupportActionBar(mToolbar);
    }

    Map<String, String> languages = new HashMap<>();

    public void setUpMap() {
        languages.put("Automatic", "auto");
        languages.put("Afrikaans", "af");
        languages.put("Albanian", "sq");
        languages.put("Amharic", "am");
        languages.put("Arabic", "ar");
        languages.put("Armenian", "hy");
        languages.put("Azerbaijani", "az");
        languages.put("Basque", "eu");
        languages.put("Belarusian", "be");
        languages.put("Bengali", "bn");
        languages.put("Bosnian", "bs");
        languages.put("Bulgarian", "bg");
        languages.put("Catalan", "ca");
        languages.put("Cebuano", "ceb");
        languages.put("Chinese Simplified", "zh-cn");
        languages.put("Chinese Traditional", "zh-tw");
        languages.put("Corsican", "co");
        languages.put("Croatian", "hr");
        languages.put("Czech", "cs");
        languages.put("Danish", "da");
        languages.put("Dutch", "nl");
        languages.put("English", "en");
        languages.put("Esperanto", "eo");
        languages.put("Estonian", "et");
        languages.put("Filipino", "tl");
        languages.put("Finnish", "fi");
        languages.put("French", "fr");
        languages.put("Frisian", "fy");
        languages.put("Galician", "gl");
        languages.put("Georgian", "ka");
        languages.put("German", "de");
        languages.put("Greek", "el");
        languages.put("Gujarati", "gu");
        languages.put("Haitian Creole", "ht");
        languages.put("Hausa", "ha");
        languages.put("Hawaiian", "haw");
        languages.put("Hebrew", "iw");
        languages.put("Hindi", "hi");
        languages.put("Hmong", "hmn");
        languages.put("Hungarian", "hu");
        languages.put("Icelandic", "is");
        languages.put("Igbo", "ig");
        languages.put("Indonesian", "id");
        languages.put("Irish", "ga");
        languages.put("Italian", "it");
        languages.put("Japanese", "ja");
        languages.put("Javanese", "jw");
        languages.put("Kannada", "kn");
        languages.put("Kazakh", "kk");
        languages.put("Khmer", "km");
        languages.put("Korean", "ko");
        languages.put("Kurdish (Kurmanji)", "ku");
        languages.put("Kyrgyz", "ky");
        languages.put("Lao", "lo");
        languages.put("Latin", "la");
        languages.put("Latvian", "lv");
        languages.put("Lithuanian", "lt");
        languages.put("Luxembourgish", "lb");
        languages.put("Macedonian", "mk");
        languages.put("Malagasy", "mg");
        languages.put("Malay", "ms");
        languages.put("Malayalam", "ml");
        languages.put("Maltese", "mt");
        languages.put("Maori", "mi");
        languages.put("Marathi", "mr");
        languages.put("Mongolian", "mn");
        languages.put("Myanmar (Burmese)", "my");
        languages.put("Nepali", "ne");
        languages.put("Norwegian", "no");
        languages.put("Pashto", "ps");
        languages.put("Persian", "fa");
        languages.put("Polish", "pl");
        languages.put("Portuguese", "pt");
        languages.put("Punjabi", "ma");
        languages.put("Romanian", "ro");
        languages.put("Russian", "ru");
        languages.put("Samoan", "sm");
        languages.put("Scots Gaelic", "gd");
        languages.put("Serbian", "sr");
        languages.put("Sesotho", "st");
        languages.put("Shona", "sn");
        languages.put("Sindhi", "sd");
        languages.put("Sinhala", "si");
        languages.put("Slovak", "sk");
        languages.put("Slovenian", "sl");
        languages.put("Somali", "so");
        languages.put("Spanish", "es");
        languages.put("Sundanese", "su");
        languages.put("Swedish", "sv");
        languages.put("Tajik", "tg");
        languages.put("Tamil", "ta");
        languages.put("Telugu", "te");
        languages.put("Thai", "th");
        languages.put("Turkish", "tr");
        languages.put("Ukrainian", "uk");
        languages.put("Urdu", "ur");
        languages.put("Uzbek", "uz");
        languages.put("Vietnamese", "vi");
        languages.put("Welsh", "cy");
        languages.put("Xhosa", "xh");
        languages.put("Yiddish", "yi");
        languages.put("Yoruba", "yo");
        languages.put("Zulu", "zu");

    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.drawer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_ocr) {
            fragOCR.setVisibility(View.VISIBLE);
            fragTrans.setVisibility(View.GONE);
        } else if (id == R.id.nav_text) {
            fragOCR.setVisibility(View.GONE);
            fragTrans.setVisibility(View.VISIBLE);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
