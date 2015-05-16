package com.siddhilabs.locationaddress;

import android.app.IntentService;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 */
public class FetchAddressIntentService extends IntentService {
    public static final String TAG = "fetchaddrintentservice";

    protected ResultReceiver mResultReceiver;

    public FetchAddressIntentService() {
        super("FetchAddressIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String errorMessage = "";

        mResultReceiver = intent.getParcelableExtra(AppConstants.RECEIVER);

        if(mResultReceiver == null){
            Log.wtf(TAG, "No receiver received. There is nowhere to send the results !!");
            return;
        }

        //get location
        Location location = intent.getParcelableExtra(AppConstants.LOCATION_DATA_EXTRA);
        if(location == null){
            errorMessage = getString(R.string.no_location_data_provided);
            Log.wtf(TAG, errorMessage);
            deliverResultsToReceiver(AppConstants.FAILURE_RESULT, errorMessage);
            return;
        }

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        List<Address> addresses = null;

        try {
            addresses = geocoder.getFromLocation(location.getLatitude(),
                    location.getLongitude(),
                    1);
        } catch (IOException ioException) {
            errorMessage = getString(R.string.service_not_available);
            Log.e(TAG, errorMessage, ioException);
        } catch (IllegalArgumentException illegalArgumentException){
            errorMessage = getString(R.string.invalid_lat_long_specified);
            Log.e(TAG,
                    errorMessage+"."+"Latitude="+location.getLatitude()+","+"Longitude="+location.getLongitude()
                    ,illegalArgumentException);
        }

        if(addresses == null || addresses.size() < 1){
            errorMessage = getString(R.string.no_address_found);
            Log.e(TAG, errorMessage);
            deliverResultsToReceiver(AppConstants.FAILURE_RESULT, errorMessage);
        } else {
            Address address = addresses.get(0);
            ArrayList<String> addressFragments = new ArrayList<>();

            for(int i = 0; i < address.getMaxAddressLineIndex(); i++){
                addressFragments.add(address.getAddressLine(i));
            }

            Log.i(TAG, getString(R.string.address_found));

            deliverResultsToReceiver(AppConstants.SUCCESS_RESULT,
                    TextUtils.join(System.getProperty("line.separator"), addressFragments));
        }
    }

    private void deliverResultsToReceiver(int resultCode, String message){
        Bundle bundle = new Bundle();
        bundle.putString(AppConstants.RESULT_DATA_KEY, message);

        mResultReceiver.send(resultCode, bundle);
    }
}