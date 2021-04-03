package com.example.tipcalculator_tr;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethod;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.NumberFormat;

public class TipCalculatorActivity extends AppCompatActivity {

    private float tipPercent=.15f;

    private final int ROUND_NONE = 0;
    private final int ROUND_TIP = 1;
    private final int ROUND_TOTAL = 2;
    private int round = ROUND_NONE;
    private boolean divide_total = false;

    private EditText billAmountEditText;
    private TextView percentTextView;
    private Button plusButton;
    private Button minusButton;
    private TextView tipTextView;
    private TextView totalTextView;
    private Spinner divide_spinner;
    private TextView perPersonTextView;
    private TextView perPersonLabelTextView;
    private TextView divideLabelTextView;
    private RadioGroup colorTotalRadioGroup;
    private TextView progressTextView;
    private SeekBar progressSeekBar;

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tip_calculator);

        billAmountEditText = (EditText) findViewById(R.id.billAmountEditText);
        percentTextView = (TextView) findViewById(R.id.percentTextView);
        plusButton = (Button) findViewById(R.id.plus_button);
        minusButton = (Button) findViewById(R.id.minus_button);
        tipTextView = (TextView) findViewById(R.id.tipTextView);
        totalTextView = (TextView) findViewById(R.id.totalTextView);
        divide_spinner = (Spinner) findViewById(R.id.divideSpinner);
        perPersonTextView = (TextView) findViewById(R.id.perPersonTextView);
        perPersonLabelTextView = (TextView) findViewById(R.id.perPersonLabelTextView);
        divideLabelTextView = (TextView) findViewById(R.id.divideLabelTextView);
        colorTotalRadioGroup = (RadioGroup) findViewById(R.id.colorTotalRadioGroup);
        progressTextView = (TextView) findViewById(R.id.progressTextView);
        progressSeekBar = (SeekBar) findViewById(R.id.progressSeekBar);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,R.array.spinner_keys, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        divide_spinner.setAdapter(adapter);

        billAmountEditText.setOnEditorActionListener(editorActionListener);
        billAmountEditText.setOnKeyListener(keyListener);
        plusButton.setOnClickListener(clickListener);
        minusButton.setOnClickListener(clickListener);
        divide_spinner.setOnItemSelectedListener(itemSelectedListener);
        colorTotalRadioGroup.setOnCheckedChangeListener(checkedChangeListener);
        progressSeekBar.setOnSeekBarChangeListener(seekBarChangeListener);

        //prefs = getSharedPreferences("SavedValues", MODE_PRIVATE);

        PreferenceManager.setDefaultValues(this, R.xml.preferences,false);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        calculateAndDisplay();
    }

    @Override
    public void onPause(){
        String billAmountString = billAmountEditText.getText().toString();
        Editor editor = prefs.edit();
        editor.putString("billAmountString",billAmountString);
        editor.putFloat("tipPercent",tipPercent);
        editor.putInt("progressSeekBar",progressSeekBar.getProgress());
        editor.commit();
        //Log.d("TipCalculatorActivity",billAmountString);
        super.onPause();
    }

    @Override
    public void onResume(){
        super.onResume();
        String billAmountString = prefs.getString("billAmountString","0");
        billAmountEditText.setText(billAmountString);
        boolean remember_tip_percent = prefs.getBoolean("pref_remember_tip_percent",true);
        if(remember_tip_percent){
            tipPercent = prefs.getFloat("tipPercent",0.15f);
        }else{
            float default_tip_percent = Float.parseFloat(prefs.getString("pref_default_tip_percent","0.15f"));
            tipPercent=default_tip_percent;
        }
        round = Integer.parseInt(prefs.getString("pref_rounding", String.valueOf(ROUND_NONE)));

        divide_total = prefs.getBoolean("pref_divide_total",false);
        if(divide_total){
            divideLabelTextView.setVisibility(View.VISIBLE);
            divide_spinner.setVisibility(View.VISIBLE);
            perPersonLabelTextView.setVisibility(View.VISIBLE);
            perPersonTextView.setVisibility(View.VISIBLE);
        }else{
            divideLabelTextView.setVisibility(View.GONE);
            divide_spinner.setVisibility(View.GONE);
            perPersonLabelTextView.setVisibility(View.GONE);
            perPersonTextView.setVisibility(View.GONE);
        }

        int progress = prefs.getInt("progressSeekBar",50);
        progressTextView.setText(Integer.toString(progress));
        progressSeekBar.setProgress(progress);

        calculateAndDisplay();
        //Log.d("TipCalculatorActivity", String.valueOf(tipPercent));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.menu_tip_calculator,menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getItemId()){
            case R.id.menu_settings:
                startActivity(new Intent(getApplicationContext(),SettingsActivity.class));
                return true;
            case R.id.menu_about:
                startActivity(new Intent(getApplicationContext(), AboutActivity.class));
                return true;
            case R.id.menu_help:
                Toast.makeText(this,"Help function has not been implemented", Toast.LENGTH_SHORT).show();
                return true;
            default: return super.onOptionsItemSelected(item);
        }
    }

    private void calculateAndDisplay(){
        String billAmountString = billAmountEditText.getText().toString();
        float billAmount;
        if(billAmountString.equals("")){
            billAmount=0;
        }else{
            billAmount=Float.parseFloat(billAmountString);
        }

        float tip = billAmount*tipPercent;
        if(round==ROUND_TIP){
            tip = Math.round(tip);
        }
        float total=billAmount+tip;
        if(round==ROUND_TOTAL){
            total=Math.round(total);
            tip=(1*(total-billAmount))/1;
        }

        NumberFormat currency = NumberFormat.getCurrencyInstance();
        NumberFormat percent = NumberFormat.getPercentInstance();

        if(divide_total){
            int pos = divide_spinner.getSelectedItemPosition()+2;
            float perPersonTotal = total/pos;
            if(round==ROUND_TOTAL){
                perPersonTotal=Math.round(perPersonTotal);
            }
            perPersonTextView.setText(currency.format(perPersonTotal));
        }

        tipTextView.setText(currency.format(tip));
        totalTextView.setText(currency.format(total));
        percentTextView.setText(percent.format(tipPercent));
    }

    private TextView.OnEditorActionListener editorActionListener = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if(actionId== EditorInfo.IME_ACTION_DONE || actionId==EditorInfo.IME_ACTION_UNSPECIFIED){
                calculateAndDisplay();
            }
            return false;
        }
    };

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch(v.getId()){
                case R.id.plus_button:
                    tipPercent=tipPercent+0.01f;
                    calculateAndDisplay();
                    break;
                case R.id.minus_button:
                    tipPercent=tipPercent-0.01f;
                    calculateAndDisplay();
            }
        }
    };

    private AdapterView.OnItemSelectedListener itemSelectedListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            calculateAndDisplay();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            Toast.makeText(getApplicationContext(),"You don't have selected a voice", Toast.LENGTH_SHORT).show();
        }
    };

    private RadioGroup.OnCheckedChangeListener checkedChangeListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            switch (checkedId){
                case R.id.blackColoRadioButton:
                    totalTextView.setTextColor(Color.BLACK);
                    break;
                case R.id.blueColorRadioButton:
                    totalTextView.setTextColor(Color.BLUE);
                    break;
                case R.id.redColorRadioButton:
                    totalTextView.setTextColor(Color.RED);
                    break;
            }
        }
    };

    private View.OnKeyListener keyListener = new View.OnKeyListener() {
        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            switch (keyCode){
                case KeyEvent.KEYCODE_ENTER:
                case KeyEvent.KEYCODE_DPAD_CENTER:
                    calculateAndDisplay();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(billAmountEditText.getWindowToken(),0);
                    return true;
            }
            return false;
        }
    };

    private SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            progressTextView.setText(Integer.toString(progress));
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };

}