package com.example.myapplication;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final float DEFAULT_ZOOM = 15f;

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private boolean isFirstLocation = true;

    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    new ActivityResultCallback<Boolean>() {
                        @Override
                        public void onActivityResult(Boolean granted) {
                            if (granted) startLocationLogic();
                            else Toast.makeText(MainActivity.this, "Location permission required", Toast.LENGTH_LONG).show();
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

        // Buttons
        findViewById(R.id.report_button).setOnClickListener(v -> openReport());
        findViewById(R.id.dashboard_button).setOnClickListener(v -> startActivity(new android.content.Intent(this, Dashboard.class)));
        findViewById(R.id.logout_button).setOnClickListener(v -> startActivity(new android.content.Intent(this, LoginPage.class)));

        // Location updates
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) return;
                LatLng latLng = new LatLng(locationResult.getLastLocation().getLatitude(),
                        locationResult.getLastLocation().getLongitude());
                if (mMap != null && isFirstLocation) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM));
                    isFirstLocation = false;
                }
            }
        };
    }

    private void openReport() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    android.content.Intent intent = new android.content.Intent(this, Report.class);
                    intent.putExtra("LATITUDE", String.valueOf(location.getLatitude()));
                    intent.putExtra("LONGITUDE", String.valueOf(location.getLongitude()));
                    startActivity(intent);
                    Toast.makeText(this, "Lat: " + location.getLatitude() + ", Lon: " + location.getLongitude(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Enable location layer if permission granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            startLocationLogic();
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        // Load issues from Firebase
        loadIssuesFromFirebase();
    }

    private void startLocationLogic() {
        if (mMap == null) return;
        try {
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(2000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }

    private void loadIssuesFromFirebase() {
        DatabaseReference issuesRef = FirebaseDatabase.getInstance().getReference("Issues");

        issuesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<LatLng> latLngList = new ArrayList<>();
                for (DataSnapshot issueSnap : snapshot.getChildren()) {
                    String latStr = issueSnap.child("latitude").getValue(String.class);
                    String lonStr = issueSnap.child("longitude").getValue(String.class);
                    String title = issueSnap.child("title").getValue(String.class);
                    String imageUrl = issueSnap.child("imageUrl").getValue(String.class);

                    if (latStr != null && lonStr != null) {
                        LatLng latLng = new LatLng(Double.parseDouble(latStr), Double.parseDouble(lonStr));
                        latLngList.add(latLng);

                        // Load image for marker
                        if (imageUrl != null && !imageUrl.isEmpty()) {

                            Glide.with(MainActivity.this)
                                    .asBitmap()
                                    .load(imageUrl)
                                    .circleCrop()
                                    .into(new CustomTarget<Bitmap>(100, 100) {
                                        @Override
                                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                            BitmapDescriptor icon = BitmapDescriptorFactory.fromBitmap(resource);
                                            mMap.addMarker(new MarkerOptions().position(latLng).title(title).icon(icon));
                                        }

                                        // Correct signature here:
                                        @Override
                                        public void onLoadCleared(@Nullable android.graphics.drawable.Drawable placeholder) {
                                            // Required, leave empty if no cleanup needed
                                        }
                                    });


                        } else {
                            // Default marker if no image
                            mMap.addMarker(new MarkerOptions().position(latLng).title(title));
                        }
                    }
                }

                // Move camera to show all markers
                if (!latLngList.isEmpty()) {
                    LatLngBounds.Builder builder = new LatLngBounds.Builder();
                    for (LatLng ll : latLngList) builder.include(ll);
                    mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 150));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Failed to load issues: " + error.getMessage());
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mMap != null && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            startLocationLogic();
        }
    }
}
