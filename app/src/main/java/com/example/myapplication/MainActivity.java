package com.example.myapplication;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

import com.google.android.gms.maps.model.LatLng;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final float DEFAULT_ZOOM = 17f;

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private boolean isFirstLocation = true; // center camera only on first fix if you want

    // permission launcher for the modern API
    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    new ActivityResultCallback<Boolean>() {
                        @Override
                        public void onActivityResult(Boolean granted) {
                            if (granted) {
                                startLocationLogic();
                            } else {
                                Toast.makeText(MainActivity.this, "Location permission required", Toast.LENGTH_LONG).show();
                            }
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);


       /* Button reportBtn = findViewById(R.id.report_button);
        reportBtn.setOnClickListener(v -> {
            // your report action
            Intent intent = new Intent(this, Report.class);
            intent.putExtra("LOCATION","00.0,00.00");
            startActivity(intent);

        });*/


        Button reportBtn = findViewById(R.id.report_button);
        reportBtn.setOnClickListener(v -> {
            // Check permission first
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.getLastLocation()
                        .addOnSuccessListener(location -> {
                            if (location != null) {
                                double lat = location.getLatitude();
                                double lon = location.getLongitude();
                                Intent intent = new Intent(this, Report.class);


                                intent.putExtra("LATITUDE",String.valueOf(lat));
                                intent.putExtra("LONGITUDE",String.valueOf(lon));
                                startActivity(intent);

                                Toast.makeText(MainActivity.this,
                                        "Latitude: " + lat + "\nLongitude: " + lon,
                                        Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(MainActivity.this,
                                        "Permission Not granted", Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                // Ask permission if not granted
                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        });



        Button dashboardBtn = findViewById(R.id.dashboard_button);
        dashboardBtn.setOnClickListener(v -> {
            // your report action
            Intent intent = new Intent(this, Dashboard.class);
            startActivity(intent);
        });

        Button logoutBtn = findViewById(R.id.logout_button);
        logoutBtn.setOnClickListener(v -> {
            // your report action
            Toast.makeText(this, "Report tapped", Toast.LENGTH_SHORT).show();
        });


        // create locationCallback once
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location loc = locationResult.getLastLocation();
                if (loc == null) return;
                LatLng latLng = new LatLng(loc.getLatitude(), loc.getLongitude());

                // Option A: rely on the My Location blue dot to show user marker (we still move camera)
                // Move or animate camera to follow user
                if (mMap != null) {
                    if (isFirstLocation) {
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM));
                        isFirstLocation = false;
                    } else {
                        // smooth follow; you can alter to only update when user moved more than X meters
                        mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
                    }
                }
            }
        };
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Check permissions and start location logic (enable my-location layer + updates)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            startLocationLogic();
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void startLocationLogic() {
        if (mMap == null) return;

        // Enable the My Location layer (blue dot / chevron) — requires runtime permission already granted
        try {
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
        } catch (SecurityException e) {
            // Shouldn't happen because we checked permission, but catch just in case
            e.printStackTrace();
        }

        // Build location request
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(5000);            // 5 seconds
        locationRequest.setFastestInterval(2000);     // 2 seconds
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // Request location updates
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
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop updates to save battery (or keep running if you want background)
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // restart updates if permission still granted and map ready
        if (mMap != null && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            startLocationLogic();
        }
    }
}
