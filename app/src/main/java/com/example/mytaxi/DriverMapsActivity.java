package com.example.mytaxi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApi;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class DriverMapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;
    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;

    private Button driverSettingsBtn, driverLogoutBtn;
    private FirebaseAuth mAuth;

    DatabaseReference driversAvailabilityRef;
    DatabaseReference driversWorkingRef;
    String userID;

    private Boolean driverLogoutStatus = false;
    private String driverId, customerId = "";

    private DatabaseReference customerRef, customerPositionRef;
    Marker pickUpMarker;
    ValueEventListener customerPositionRefListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mAuth = FirebaseAuth.getInstance();
        driverId = mAuth.getCurrentUser().getUid();

        driverSettingsBtn = (Button) findViewById(R.id.driverSettingsBtn);
        driverSettingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent settingsIntent = new Intent(DriverMapsActivity.this, SettingsActivity.class);
                settingsIntent.putExtra("type", "Drivers");
                startActivity(settingsIntent);
            }
        });

        driverLogoutBtn = findViewById(R.id.driverLogoutBtn);
        driverLogoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                driverLogoutStatus = true;
                mAuth.signOut();
                logoutDriver();
                disconnectDriver();
            }
        });

        getAssignedCustomerRequest();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        buildGoogleApiClient();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        mMap.setMyLocationEnabled(true);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);  // 1 sec
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(locationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {

        if (getApplicationContext() != null) {

            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(17));

            userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
            driversAvailabilityRef = FirebaseDatabase.getInstance().getReference().child("Driver Available");
            driversWorkingRef = FirebaseDatabase.getInstance().getReference().child("Driver Working");

            GeoFire geoFireAvailable = new GeoFire(driversAvailabilityRef);
            GeoFire geoFireWorking = new GeoFire(driversWorkingRef);

            switch (customerId) {
                case "":
                    geoFireWorking.removeLocation(userID);
                    geoFireAvailable.setLocation(userID, new GeoLocation(location.getLatitude(), location.getLongitude()));
                    break;
                default:
                    geoFireAvailable.removeLocation(userID);
                    geoFireWorking.setLocation(userID, new GeoLocation(location.getLatitude(), location.getLongitude()));
                    break;
            }
        }
    }

    protected synchronized void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
        .addConnectionCallbacks(this)
        .addOnConnectionFailedListener(this)
        .addApi(LocationServices.API)
        .build();

        googleApiClient.connect();
    }

    @Override
    protected void onStop() {

        super.onStop();

        if(!driverLogoutStatus) {
            disconnectDriver();
        }
    }

    private void disconnectDriver() {

        driverLogoutStatus = false;
        GeoFire geofire = new GeoFire(driversAvailabilityRef);
        geofire.removeLocation(userID);
    }

    private void logoutDriver() {
        Intent welcomeIntent =  new Intent(DriverMapsActivity.this, WelcomeActivity.class);
        welcomeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(welcomeIntent);
        finish();
    }

    private void getAssignedCustomerRequest() {
       
        customerRef = FirebaseDatabase.getInstance().getReference()
                .child("Users").child("Drivers").child(driverId).child("CustomerRideId");

        customerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    customerId = snapshot.getValue().toString();
                    getCustomerPosition();

                } else {
                    customerId = "";
                    if (pickUpMarker != null) {
                        pickUpMarker.remove();
                    }

                    if (customerPositionRefListener != null) {
                        customerPositionRef.removeEventListener(customerPositionRefListener);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void getCustomerPosition() {

        customerPositionRef = FirebaseDatabase.getInstance().getReference().child("Customer Request")
                .child(customerId).child("l");

        customerPositionRefListener = customerPositionRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    List<Object> customerPositionList = (List<Object>) snapshot.getValue();
                    double locationLat = 0;
                    double locationLng = 0;

                    if (customerPositionList.get(0) != null) {
                        locationLat = Double.parseDouble(customerPositionList.get(0).toString());
                    }

                    if (customerPositionList.get(1) != null) {
                        locationLng = Double.parseDouble(customerPositionList.get(1).toString());
                    }

                    LatLng customerLatLng = new LatLng(locationLat, locationLng);

                    if (pickUpMarker != null) {
                        pickUpMarker.remove();
                    }
                    pickUpMarker = mMap.addMarker(new MarkerOptions().position(customerLatLng)
                            .title("Pick Up here").icon(BitmapDescriptorFactory.fromResource(R.drawable.user)));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}