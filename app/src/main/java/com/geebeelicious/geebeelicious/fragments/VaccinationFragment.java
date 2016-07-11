package com.geebeelicious.geebeelicious.fragments;


import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.geebeelicious.geebeelicious.R;
import com.geebeelicious.geebeelicious.interfaces.OnMonitoringFragmentInteractionListener;
import com.geebeelicious.geebeelicious.models.monitoring.Record;
import com.geebeelicious.geebeelicious.models.monitoring.TakePictureHelper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by MG.
 * The VaccinationFragment class serves as the fragment for
 * the taking a picture of the vaccination of a patient.
 */
public class VaccinationFragment extends Fragment {
    private final static String TAG = "VaccinationFragment";
    private static final int REQUEST_TAKE_PHOTO = 1;

    private OnMonitoringFragmentInteractionListener fragmentInteraction;
    private Activity activity;

    private TakePictureHelper vaccinationHelper;

    private Button skipButton;
    private Button pictureButton;

    private View view;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             final Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_vaccination, container, false);
        skipButton = (Button) view.findViewById(R.id.skipButton);
        pictureButton = (Button) view.findViewById(R.id.takePictureButton);
        Typeface chalkFont = Typeface.createFromAsset(getActivity().getAssets(), "fonts/DJBChalkItUp.ttf");
        skipButton.setTypeface(chalkFont);
        pictureButton.setTypeface(chalkFont);

        ImageView imageViewPlaceholder = (ImageView) view.findViewById(R.id.imagePlaceholder);

        vaccinationHelper = new TakePictureHelper(imageViewPlaceholder);

        if(savedInstanceState != null){
            vaccinationHelper.setmCurrentPhotoPath(savedInstanceState.getString("photoPath"));
            if (vaccinationHelper.getmCurrentPhotoPath() != null){
                vaccinationHelper.setPic();
            }
        }

        if(vaccinationHelper.getmCurrentPhotoPath() == null){
            fragmentInteraction.setInstructions(R.string.vaccination_instruction);
        }

        skipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (vaccinationHelper.getmCurrentPhotoPath() != null) {
                    Record record = fragmentInteraction.getRecord();
                    record.setVaccination(vaccinationHelper.getPicture());
                }
                view.setVisibility(View.GONE);
                fragmentInteraction.doneFragment();
            }
        });

        pictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
            }
        });


        return view;
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(activity.getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = vaccinationHelper.createImageFile(); // also updates mCurrentPhotoPath
            } catch (IOException ex) {
                Log.e(TAG, "Error occured while creating file");
                Toast.makeText(activity, "Please check SD card! Image shot is impossible!", Toast.LENGTH_LONG).show();
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                        Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("photoPath", vaccinationHelper.getmCurrentPhotoPath());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        InputStream stream = null;
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == Activity.RESULT_OK) {
            vaccinationHelper.setPic();
            skipButton.setText(R.string.continueWord);
            pictureButton.setText(R.string.retake);
            fragmentInteraction.setInstructions(R.string.vaccination_confirm);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = activity;

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            fragmentInteraction = (OnMonitoringFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnMonitoringFragmentInteractionListener");
        }
    }

}
