package com.siddhilabs.locationaddress;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.ResultReceiver;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import java.util.logging.Handler;


public class MainActivity extends ActionBarActivity
        implements GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks{

    protected  static final String TAG = "main-activity";
    protected static final String ADDRESS_REQUESTED_KEY = "address-request-pending";
    protected static final String LOCATION_ADDRESS_KEY = "location-address";

    protected GoogleApiClient mGoogleApiClient;
    protected Location mLastLocation;

    protected boolean mAddressRequested;
    protected String mAddressOutput;

    private AddressResultReceiver mResultReceiver;

    //UI
    private TextView mLocationAddressTextView;
    private ProgressBar mProgressBar;
    private Button mFetchAddressButton;

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mResultReceiver = new AddressResultReceiver(new android.os.Handler());

        //UI initialize
        mLocationAddressTextView = (TextView) findViewById(R.id.location_address_view);
        mFetchAddressButton = (Button) findViewById(R.id.fetch_address_button);
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);

        //set defaults
        mAddressRequested = false;
        mAddressOutput = "";
        updateValuesFromBundle(savedInstanceState);

        updateUIWidgets();
        buildGoogleApiClient();
    }

    private void updateValuesFromBundle(Bundle savedInstanceState){
        if(savedInstanceState != null){
            if(savedInstanceState.keySet().contains(ADDRESS_REQUESTED_KEY)){
                mAddressRequested = savedInstanceState.getBoolean(ADDRESS_REQUESTED_KEY);
            }

            if(savedInstanceState.keySet().contains(LOCATION_ADDRESS_KEY)){
                mAddressOutput = savedInstanceState.getString(LOCATION_ADDRESS_KEY);
                displayAddressOutput();
            }
        }
    }

    private void displayAddressOutput(){
        mLocationAddressTextView.setText(mAddressOutput);
    }

    private void updateUIWidgets(){
        if(mAddressRequested){
            mProgressBar.setVisibility(ProgressBar.VISIBLE);
            mFetchAddressButton.setEnabled(false);
        } else {
            mProgressBar.setVisibility(ProgressBar.GONE);
            mFetchAddressButton.setEnabled(true);
        }
    }

    private void buildGoogleApiClient(){
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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

    @Override
    public void onConnected(Bundle bundle) {
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if(mLastLocation != null){
            if(!Geocoder.isPresent()) {
                Toast.makeText(this, R.string.service_not_available, Toast.LENGTH_LONG).show();
            }

            if(mAddressRequested) {
                startIntentService();
            }
        }
    }

    private void startIntentService() {
        Intent intent = new Intent(this, FetchAddressIntentService.class);
        intent.putExtra(AppConstants.RECEIVER, mResultReceiver);
        intent.putExtra(AppConstants.LOCATION_DATA_EXTRA, mLastLocation);

        startService(intent);
    }

    public void fetchAddressButtonHandler(View view) {
        if(mGoogleApiClient.isConnected() && mLastLocation != null) {
            startIntentService();
        }
        mAddressRequested = true;
        updateUIWidgets();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, getString(R.string.googleapiclient_conn_susp));
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG, getString(R.string.googleapiclient_conn_failed));
    }

    class AddressResultReceiver extends ResultReceiver {

        public AddressResultReceiver(android.os.Handler handler) {
            super(handler);
        }
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            //super.onReceiveResult(resultCode, resultData);
            if(resultCode == AppConstants.SUCCESS_RESULT) {
                Toast.makeText(getBaseContext(), getString(R.string.address_found),
                        Toast.LENGTH_LONG).show();

                mAddressOutput = resultData.getString(AppConstants.RESULT_DATA_KEY);
                displayAddressOutput();

                mAddressRequested = false;
                updateUIWidgets();
            }
        }
    }
}
