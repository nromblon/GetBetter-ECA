package models.visualacuity;

import android.content.Context;
import android.widget.ImageView;

/**
 * Created by Kate on 02/23/2016.
 * The DistanceCalculator class determines
 * the size of the visual acuity chart to be used (maximum size depending on tablet)
 * and the required distance the user must be to perform the test.
 */
public class DistanceCalculator {

    private int displayHeight;
    private int displayWidth;

    private void getDisplaySize(ImageView imageView){
        displayHeight = imageView.getHeight();
        displayWidth = imageView.getWidth();
        System.out.println("dh: " + displayHeight + "dw: " + displayWidth);
    }

    public float getUserDistance(Context context, ImageView imageView){
        getDisplaySize(imageView);
        float height = convertPixelsToMillimeter(displayHeight, context.getResources().getDisplayMetrics().xdpi);
        float distanceMeters = (height/88) * 6;
        return distanceMeters;
    }

    private float convertPixelsToMillimeter(int pixels, float dpi){
        return pixels / dpi * 25.4f;
    }

    private float convertMetersToFeet(float meters){
        return meters * 3.28084f;
    }

}
