package com.example.mytaxi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;

import java.io.File;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class SettingsActivity extends AppCompatActivity {

    private String getType;

    private EditText userName, userPhone, userCar;
    private ImageView iconClose, iconSave;
    private CircleImageView circleImageView;
    private TextView userPhoto;

    private Boolean checker = false;
    private Uri imageUri;
    private String imageUrl;

    private StorageTask uploadTask;
    private StorageReference profileImageStorageRef;
    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        getType = getIntent().getStringExtra("type");

        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference().child("Users").child(getType);

        profileImageStorageRef = FirebaseStorage.getInstance().getReference().child("Profile Picture");

        userPhoto = findViewById(R.id.user_photo);
        userName = findViewById(R.id.user_name);
        userPhone = findViewById(R.id.user_phone);
        userCar = findViewById(R.id.user_car);
        circleImageView = findViewById(R.id.profile_image);
        iconClose = findViewById(R.id.icon_close);
        iconSave = findViewById(R.id.icon_save);

        if (getType.equals("Drivers")) {
            userCar.setVisibility(View.VISIBLE);
        }

        userPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checker = true;
                CropImage.activity().setAspectRatio(1, 1).start(SettingsActivity.this);
            }
        });

        iconClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getType.equals("Customers")) {
                    startActivity(new Intent(SettingsActivity.this, CustomerMapsActivity.class));
                } else {
                    startActivity(new Intent(SettingsActivity.this, DriverMapsActivity.class));
                }
            }
        });

        iconSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checker) {
                    saveDataWithImage();
                } else {
                    saveDataOnly();
                }
            }
        });

        getUserData();
    }

    private void getUserData() {
        databaseReference.child(mAuth.getCurrentUser().getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.getChildrenCount() > 0) {
                    userName.setText(snapshot.child("Name").getValue().toString());
                    userPhone.setText(snapshot.child("Phone").getValue().toString());
                    if (getType.equals("Drivers")) userCar.setText(snapshot.child("Car").getValue().toString());
                    if (snapshot.hasChild("Image")) {
                        String url = snapshot.child("Image").getValue().toString();
                        Picasso.get().load(url).into(circleImageView);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE
                && resultCode == RESULT_OK && data != null) {

            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            imageUri = result.getUri();
            circleImageView.setImageURI(imageUri);
        } else {
            if (getType.equals("Drivers")) {
                startActivity(new Intent(SettingsActivity.this, DriverMapsActivity.class));
            } else {
                startActivity(new Intent(SettingsActivity.this, CustomerMapsActivity.class));
            }
            Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveDataOnly() {
        if (TextUtils.isEmpty(userName.getText().toString())) {
            Toast.makeText(this, "Please, enter name", Toast.LENGTH_SHORT).show();
        }

        else if (TextUtils.isEmpty(userPhone.getText().toString())) {
            Toast.makeText(this, "Please, enter phone", Toast.LENGTH_SHORT).show();
        }

        else if (getType.equals("Drivers") && TextUtils.isEmpty(userCar.getText().toString())) {
            Toast.makeText(this, "Please, enter car description", Toast.LENGTH_SHORT).show();
        } else {
            HashMap<String, Object> userMap = new HashMap<>();
            userMap.put("Uid", mAuth.getCurrentUser().getUid());
            userMap.put("Name", userName.getText().toString());
            userMap.put("Phone", userPhone.getText().toString());

            if (getType.equals("Drivers")) {
                userMap.put("Car", userCar.getText().toString());
            }

            databaseReference.child(mAuth.getCurrentUser().getUid()).updateChildren(userMap);

            Toast.makeText(SettingsActivity.this, "Profile successful upload", Toast.LENGTH_SHORT).show();

            if (getType.equals("Drivers")) {
                startActivity(new Intent(SettingsActivity.this, DriverMapsActivity.class));
            } else {
                startActivity(new Intent(SettingsActivity.this, CustomerMapsActivity.class));
            }
        }



    }

    private void saveDataWithImage() {
        if (TextUtils.isEmpty(userName.getText().toString())) {
            Toast.makeText(this, "Please, enter name", Toast.LENGTH_SHORT).show();
        }

        else if (TextUtils.isEmpty(userPhone.getText().toString())) {
            Toast.makeText(this, "Please, enter phone", Toast.LENGTH_SHORT).show();
        }

        else if (getType.equals("Drivers") && TextUtils.isEmpty(userCar.getText().toString())) {
            Toast.makeText(this, "Please, enter car description", Toast.LENGTH_SHORT).show();
        }

        else if (checker) {
            uploadProfileImage();
        }
    }

    private void uploadProfileImage() {

        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Upload Image");
        progressDialog.setMessage("Please, wait");
        progressDialog.show();

        if (imageUri != null) {
            final StorageReference FileRef = profileImageStorageRef.child(mAuth.getCurrentUser().getUid() + ".jpg");
            uploadTask = FileRef.putFile(imageUri);

            uploadTask.continueWithTask(new Continuation() {
                @Override
                public Object then(@NonNull Task task) throws Exception {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return FileRef.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()) {

                        Uri downloadUrl = task.getResult();
                        imageUrl = downloadUrl.toString();

                        HashMap<String, Object> userMap = new HashMap<>();
                        userMap.put("Uid", mAuth.getCurrentUser().getUid());
                        userMap.put("Name", userName.getText().toString());
                        userMap.put("Phone", userPhone.getText().toString());
                        userMap.put("Image", imageUrl);

                        if (getType.equals("Drivers")) {
                            userMap.put("Car", userCar.getText().toString());
                        }

                        databaseReference.child(mAuth.getCurrentUser().getUid()).updateChildren(userMap);

                        progressDialog.dismiss();

                        Toast.makeText(SettingsActivity.this, "Profile successful upload", Toast.LENGTH_SHORT).show();

                        if (getType.equals("Drivers")) {
                            startActivity(new Intent(SettingsActivity.this, DriverMapsActivity.class));
                        } else {
                            startActivity(new Intent(SettingsActivity.this, CustomerMapsActivity.class));
                        }
                    }
                }
            });
        } else {
            Toast.makeText(this, "Image Error", Toast.LENGTH_SHORT).show();
        }
    }
}