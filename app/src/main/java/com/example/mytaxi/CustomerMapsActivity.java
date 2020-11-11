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
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
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
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class CustomerMapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;
    private GoogleApiClient googleApiClient;
    private Location lastLocation;
    private LocationRequest locationRequest;

    private Button customerSettingsBtn, customerLogoutBtn, pickUpTaxiBtn;
    private TextView driverName, driverPhone, driverCar;
    private ImageView callToDriver;
    private CircleImageView driverPhoto;
    private RelativeLayout relativeLayout;

    private FirebaseAuth mAuth;

    private DatabaseReference customerDatabaseRef;
    private DatabaseReference driversAvailableRef;
    private DatabaseReference driversLocationRef;
    private DatabaseReference driversRef;

    private String customerId;
    private LatLng customerPosition;
    private int radiusSearch = 1;

    private Boolean driverFound = false;
    private String driverFoundId;

    Marker driverMarker, pickUpMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mAuth = FirebaseAuth.getInstance();
        customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        customerDatabaseRef = FirebaseDatabase.getInstance().getReference().child("Customer request");
        driversAvailableRef = FirebaseDatabase.getInstance().getReference().child("Driver Available");
        driversLocationRef = FirebaseDatabase.getInstance().getReference().child("Driver Working");

        driverName = findViewById(R.id.driver_name);
        driverPhone = findViewById(R.id.driver_phone);
        driverCar = findViewById(R.id.driver_car);
        driverPhoto = findViewById(R.id.driver_photo);
        relativeLayout = findViewById(R.id.driver_info);
        relativeLayout.setVisibility(View.INVISIBLE);

        customerSettingsBtn = findViewById(R.id.customerSettingsBtn);
        customerSettingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent settingsIntent = new Intent(CustomerMapsActivity.this, SettingsActivity.class);
                settingsIntent.putExtra("type", "Customers");
                startActivity(settingsIntent);
            }
        });

        pickUpTaxiBtn = findViewById(R.id.pickUpTaxi);
        pickUpTaxiBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                    GeoFire geoFire = new GeoFire(customerDatabaseRef);
                    geoFire.setLocation(customerId, new GeoLocation(lastLocation.getLatitude(), lastLocation. getLongitude()));

                    customerPosition = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                    pickUpMarker = mMap.addMarker(new MarkerOptions()
                            .position(customerPosition).title("I am here").icon(BitmapDescriptorFactory.fromResource(R.drawable.user)));

                    pickUpTaxiBtn.setText(R.string.search_drivers);

                    getNearbyDrivers();
            }
        });

        customerSettingsBtn = findViewById(R.id.customerSettingsBtn);
        customerLogoutBtn = findViewById(R.id.customerLogoutBtn);

        customerLogoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAuth.signOut();
                logoutCustomer();
            }
        });
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

    protected synchronized void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        googleApiClient.connect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);  // 1 sec
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(locationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
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
        lastLocation = location;
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private void logoutCustomer() {

        Intent welcomeIntent =  new Intent(CustomerMapsActivity.this, WelcomeActivity.class);
        welcomeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(welcomeIntent);
        finish();
    }

    private void getNearbyDrivers() {

        GeoFire geoFire = new GeoFire(driversAvailableRef);
        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(customerPosition.latitude, customerPosition.longitude), radiusSearch);

        geoQuery.removeAllListeners();
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if (!driverFound) {
                    driverFound = true;
                    driverFoundId = key;

                    driversRef = FirebaseDatabase.getInstance().getReference()
                            .child("Users").child("Drivers").child(driverFoundId);
                    HashMap driverMap = new HashMap();
                    driverMap.put("CustomerRideId", customerId);
                    driversRef.updateChildren(driverMap);

                    getDriverLocation();
                    showDriverInfo();
                }
            }

            @Override
            public void onGeoQueryReady() {
                // if drivers not found
                if (!driverFound) {
                    radiusSearch++;
                    getNearbyDrivers();
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
            @Override
            public void onKeyExited(String key) {

            }
            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }
        });
    }

    private void getDriverLocation() {

        driversLocationRef.child(driverFoundId).child("l")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            List<Object> driverLocationList = (List<Object>) snapshot.getValue();
                            double locationLat = 0;
                            double locationLng = 0;

                            pickUpTaxiBtn.setText(R.string.driver_found);

                            if (driverLocationList.get(0) != null) {
                                locationLat = Double.parseDouble(driverLocationList.get(0).toString());
                            }

                            if (driverLocationList.get(1) != null) {
                                locationLng = Double.parseDouble(driverLocationList.get(1).toString());
                            }

                            LatLng driverLatLng = new LatLng(locationLat, locationLng);

                            if (driverMarker != null) {
                                driverMarker.remove();
                            }
                            driverMarker = mMap.addMarker(new MarkerOptions()
                                    .position(driverLatLng).title("Your taxi here").icon(BitmapDescriptorFactory.fromResource(R.drawable.car)));

                            setDistance(driverLatLng);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void setDistance(LatLng driverLatLng) {

        Location locationDr = new Location("");
        locationDr.setLatitude(driverLatLng.latitude);
        locationDr.setLongitude(driverLatLng.longitude);

        Location locationCs = new Location("");
        locationCs.setLatitude(customerPosition.latitude);
        locationCs.setLongitude(customerPosition.longitude);

        double distance = locationDr.distanceTo(locationCs);

        if (distance <= 100) {
            pickUpTaxiBtn.setText(getString(R.string.your_taxi_is_near));
        } else {
            pickUpTaxiBtn.setText(getString(R.string.distance_to_taxi) + String.valueOf(distance));
        }
    }

    private void showDriverInfo() {

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundId);
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    driverName.setText(snapshot.child("Name").getValue().toString());
                    driverPhone.setText(snapshot.child("Phone").getValue().toString());
                    driverCar.setText(snapshot.child("Car").getValue().toString());
                    if (snapshot.hasChild("Image")) {
                        String url = snapshot.child("Image").getValue().toString();
                        Picasso.get().load(url).into(driverPhoto);
                    }
                    relativeLayout.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}