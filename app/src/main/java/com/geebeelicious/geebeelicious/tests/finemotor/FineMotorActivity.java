package com.geebeelicious.geebeelicious.tests.finemotor;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.geebeelicious.geebeelicious.MonitoringConsultationChoice;
import com.geebeelicious.geebeelicious.R;

import models.consultation.Patient;
import models.consultation.Record;
import models.finemotor.FineMotorHelper;


/*
* Activity for the Fine Motors Test
*/
public class FineMotorActivity extends Activity {

    Bundle record;

    private static final String TAG = "FineMotorActivity";

    private ImageView imageViewPathToTrace;
    private Button buttonYes;
    private Button buttonNo;

    //Set the color for the start and end of the path
    private final int START_COLOR = Color.parseColor("#09BCD4");//update the instruction if you change this
    private final int END_COLOR = Color.parseColor("#E71E63");  //update the instruction if you change this

    private int currentTest; //0 for non dominant, dominant, ask assistance

    private boolean isTestOngoing = true;
    private boolean hasStarted = false; //has user started
    private FineMotorHelper fineMotorHelper;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fine_motor);

        TextView ECAtext = (TextView) findViewById(R.id.placeholderECAText);

        record = this.getIntent().getExtras();

        imageViewPathToTrace = (ImageView) findViewById(R.id.imageViewPathToTrace);
        imageViewPathToTrace.setOnTouchListener(image_Listener);

        buttonYes = (Button) findViewById(R.id.YesButton);
        buttonNo = (Button) findViewById(R.id.NoButton);
        currentTest = 0;
        fineMotorHelper = new FineMotorHelper(getApplicationContext(), imageViewPathToTrace, ECAtext);
        initializeButtons();
    }

    public View.OnTouchListener image_Listener = new View.OnTouchListener(){
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            Bitmap bitmap = ((BitmapDrawable) imageViewPathToTrace.getDrawable()).getBitmap();
            float eventX = event.getX();
            float eventY = event.getY();
            int[] xY = fineMotorHelper.getBitMapCoordinates(bitmap, event.getX(), event.getY());
            int pixel = bitmap.getPixel(xY[0], xY[1]);

            if(hasStarted){ //if user have pressed start_color
                switch(event.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        break;
                    case MotionEvent.ACTION_UP: //go back to start if finger is lifted
                        hasStarted = false;
                        fineMotorHelper.doIfTouchIsUp();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if(pixel == END_COLOR) { //if user done
                            if(hasStarted){
                                hasStarted = false;
                                if(currentTest == 0){
                                    fineMotorHelper.doTestWithPen();
                                    currentTest = 1;
                                } else if(currentTest == 1) {
                                    fineMotorHelper.askAssistantOfPen();
                                    currentTest = 2;
                                    showAnswerButtons();
                                }
                            }
                        } else if(pixel == 0){ //if touch is outside path
                            fineMotorHelper.doIfOutSideThePath();
                        } else {
                            fineMotorHelper.doIfWithinPath();
                        }
                        break;
                    default:
                        return false;
                }
                Log.d(TAG, "Touch event position: " + eventX + ", " + eventY + "\n" +
                        "Pixel: " + pixel);
                return true;
            } else {
                if(pixel == START_COLOR && (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE)){
                    hasStarted = true;
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_DOWN){
                    fineMotorHelper.refreshInstructions(currentTest);
                    return true;
                }
                return false;
            }
        }
    };

    private void initializeButtons(){
        buttonYes.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                fineMotorHelper.setResult(2, true);
                sendResults();
            }
        });

        buttonNo.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                fineMotorHelper.setResult(2, false);
                sendResults();
            }
        });
    }

    private synchronized void sendResults(){
        String resultString = "";
        String[] testString = {"nonDominantHand", "dominantHand", "usePen"};
        boolean[] result = fineMotorHelper.getResults();

        if(isTestOngoing){ //this is too avoid double clicking
            for(int i = 0; i < 3; i++){
                String temp =  (result[i] ? "Pass" : "Fail");
                resultString += testString[i] + ": " + temp + "\n";
                record.putString(testString[i], temp);
            }
            isTestOngoing = false;
        }
        TextView resultView = (TextView)findViewById(R.id.fineMotorResultsTV);
        resultView.setText(resultString);

        imageViewPathToTrace.setBackgroundColor(Color.WHITE);
        imageViewPathToTrace.setImageResource(R.drawable.wait_for_next_test);
        hideAnswerButtons();


        CountDownTimer timer = new CountDownTimer(10000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            private int getIntResults(String result){
                switch(result){
                    case "Pass":
                        return 0;
                    case "Fail":
                        return 1;
                    default:
                        return 2;
                }
            }

            @Override
            public void onFinish() {
                Patient patient = record.getParcelable("patient");
                double height = 127;
                double weight =  25;

                int grossMotor = getIntResults(record.getString("grossMotor"));
                int nonDominantHand = getIntResults(record.getString("nonDominantHand"));
                int dominantHand = getIntResults(record.getString("dominantHand"));
                int usePen = getIntResults(record.getString("usePen"));

                fineMotorHelper.saveToDatabase(new Record(patient.getPatientID(),
                        record.getString("currentDate"), height, weight, record.getString("visualAcuityLeft"),
                        record.getString("visualAcuityRight"), record.getString("colorVision"),
                        record.getString("hearingLeft"), record.getString("hearingRight"),
                        grossMotor, nonDominantHand,
                        dominantHand, usePen));

                //TODO: [NOT URGENT] Add activity asking for height and weight. change the placeholders

                Intent intent = new Intent(FineMotorActivity.this, MonitoringConsultationChoice.class);
                finish();
                startActivity(intent);
            }
        };
        timer.start();
    }

    private void hideAnswerButtons(){
        LinearLayout answers = (LinearLayout)findViewById(R.id.linearLayoutAnswers);
        for (int j = 0; j<answers.getChildCount(); j++){
            View view = answers.getChildAt(j);
            view.setEnabled(false);
            view.setVisibility(View.INVISIBLE);
        }
        answers.setVisibility(View.INVISIBLE);

    }

    private void showAnswerButtons(){
        LinearLayout answers = (LinearLayout)findViewById(R.id.linearLayoutAnswers);
        for (int j = 0; j<answers.getChildCount(); j++){
            View view = answers.getChildAt(j);
            view.setEnabled(true);
            view.setVisibility(View.VISIBLE);
        }
        answers.setVisibility(View.VISIBLE);

    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(FineMotorActivity.this, MonitoringConsultationChoice.class);
        finish();
        startActivity(intent);
    }
}