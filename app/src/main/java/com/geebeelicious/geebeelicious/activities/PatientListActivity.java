package com.geebeelicious.geebeelicious.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.geebeelicious.geebeelicious.R;
import com.geebeelicious.geebeelicious.adapters.PatientsAdapter;
import com.geebeelicious.geebeelicious.database.DatabaseAdapter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.SQLException;
import java.util.ArrayList;

import com.geebeelicious.geebeelicious.interfaces.ECAActivity;
import com.geebeelicious.geebeelicious.models.consultation.Patient;

/**
 * Created by Kate.
 * The PatientsListActivity serves as the activity allowing
 * the user to view the patient list from a given school.
 * The user can view the list, search for patients, and
 * is given the option to select the patient or be redirected
 * to the module allowing for new patients to be added.
 */

public class PatientListActivity extends ECAActivity{
    private boolean hasSpoken;
    private ArrayList<Patient> patients = null;
    private Patient chosenPatient = null;
    private PatientsAdapter patientsAdapter;
    private EditText inputSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_list);

        DatabaseAdapter getBetterDb = new DatabaseAdapter(PatientListActivity.this);
        try {
            getBetterDb.openDatabaseForRead();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        patients = new ArrayList<>();
        patients = getBetterDb.getPatientsFromSchool(getSchoolPreferences());
        getBetterDb.closeDatabase();

        inputSearch = (EditText)findViewById(R.id.search_input);

        patientsAdapter = new PatientsAdapter(PatientListActivity.this, patients);
        ListView patientListView = (ListView)findViewById(R.id.patientListView);
        patientListView.setAdapter(patientsAdapter);

        integrateECA();

        if(savedInstanceState == null){
            hasSpoken = false;
        } else {
            hasSpoken = savedInstanceState.getBoolean("hasSpoken");
        }

        patientListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                chosenPatient = patients.get(position);
                String patientInfo = "First Name: " + chosenPatient.getFirstName() +
                                    "\nLast Name: " + chosenPatient.getLastName() +
                                    "\nBirthdate: " + chosenPatient.getBirthday() +
                                    "\nGender: " + chosenPatient.getGenderString();
                TextView patientInfoView = (TextView) findViewById(R.id.patientDetailsTV);
                patientInfoView.setText(patientInfo);

                InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                mgr.hideSoftInputFromWindow(inputSearch.getWindowToken(), 0);

                ecaFragment.sendToECAToSpeak("Are you " + chosenPatient.getFirstName() +
                        " " + chosenPatient.getLastName() + "?");

            }
        });

        TextView patientDetailsView = (TextView)findViewById(R.id.patientDetailsTV);
        patientDetailsView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                mgr.hideSoftInputFromWindow(inputSearch.getWindowToken(), 0);
            }
        });

        Button selectPatientButton = (Button)findViewById(R.id.selectPatientButton);
        selectPatientButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PatientListActivity.this, MonitoringConsultationChoice.class);
                intent.putExtra("patient", chosenPatient);
                startActivity(intent);
                finish();
            }
        });

        Button addNewPatientButton = (Button)findViewById(R.id.addPatientButton);
        addNewPatientButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PatientListActivity.this, AddPatientActivity.class);
                intent.putExtra("schoolID", getSchoolPreferences());
                startActivity(intent);
                finish();
            }
        });

        inputSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                patientsAdapter.getFilter().filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                patientsAdapter.filter(s.toString());
            }
        });
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if(hasFocus){
            if(!hasSpoken){
                ecaFragment.sendToECAToSpeak("Is your name in the list?");
                hasSpoken = true;
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("hasSpoken", hasSpoken);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(PatientListActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    //Returns the schoolID fo the preferred school stored in device storage via Settings
    private int getSchoolPreferences(){
        int schoolID = 1; //default schoolID
        byte[] byteArray = new byte[4];
        try{
            FileInputStream fis = openFileInput("SchoolIDPreferences");
            fis.read(byteArray, 0, 4);
            fis.close();
        } catch(IOException e){
            return 1;
        }

        ByteBuffer b = ByteBuffer.wrap(byteArray);
        schoolID = b.getInt();

        return schoolID;
    }
}
