package com.example.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.util.UUID;

public class Report extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private ImageView issueImage;
    private ImageButton btnCapture;
    private EditText titleET, descET;
    private TextView locationTV;
    private Button submitBtn;
    private Bitmap capturedImage;
    private String latitude = "12.9716"; // Replace with dynamic location
    private String longitude = "77.5946"; // Replace with dynamic location

    private DatabaseReference database;
    private StorageReference storage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        issueImage = findViewById(R.id.issueImage);
        btnCapture = findViewById(R.id.bttncapture);
        titleET = findViewById(R.id.issueTitleET);
        descET = findViewById(R.id.issueDescET);
        locationTV = findViewById(R.id.locationofreport);
        submitBtn = findViewById(R.id.submitbtn);

        locationTV.setText(latitude + "," + longitude);

        database = FirebaseDatabase.getInstance().getReference("Issues");
        storage = FirebaseStorage.getInstance().getReference("IssueImages");

        btnCapture.setOnClickListener(v -> {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        });

        submitBtn.setOnClickListener(v -> uploadIssue());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            capturedImage = (Bitmap) data.getExtras().get("data");
            issueImage.setImageBitmap(capturedImage);
        }
    }

    private void uploadIssue() {
        String title = titleET.getText().toString();
        String desc = descET.getText().toString();
        if (title.isEmpty() || desc.isEmpty() || capturedImage == null) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        String issueId = UUID.randomUUID().toString();
        StorageReference imgRef = storage.child(issueId + ".jpg");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        capturedImage.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        byte[] data = baos.toByteArray();

        imgRef.putBytes(data).addOnSuccessListener(taskSnapshot -> {
            imgRef.getDownloadUrl().addOnSuccessListener(uri -> {
                IssueModel issue = new IssueModel(issueId, title, desc, uri.toString(),
                        latitude, longitude, "unresolved");
                database.child(issueId).setValue(issue)
                        .addOnSuccessListener(aVoid -> Toast.makeText(Report.this,
                                "Issue submitted!", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(Report.this,
                                "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            });
        }).addOnFailureListener(e -> Toast.makeText(this, "Upload failed", Toast.LENGTH_SHORT).show());
    }
}
