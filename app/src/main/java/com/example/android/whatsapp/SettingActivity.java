package com.example.android.whatsapp;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.android.whatsapp.Model.Users;
import com.example.android.whatsapp.databinding.ActivitySettingBinding;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;

public class SettingActivity extends AppCompatActivity {

    public ActivitySettingBinding binding;
    public FirebaseAuth auth;
    public FirebaseDatabase database;
    public FirebaseStorage storage;
    private static final String TAG = "MyTag";
    private static final int CAMERA_PERMISSION_CODE=101;
    private static final int READ_STORAGE_PERMISSION_CODE=102;
    private static final int WRITE_STORAGE_PERMISSION_CODE=103;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getSupportActionBar().hide();

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        storage = FirebaseStorage.getInstance();

        binding.backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SettingActivity.this,MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

        binding.saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String status = binding.aboutUs.getText().toString();
                String name = binding.userName.getText().toString();

                HashMap<String, Object> obj = new HashMap<>();
                obj.put("userName",name);
                obj.put("status",status);

                database.getReference().child("Users").child(FirebaseAuth.getInstance().getUid())
                        .updateChildren(obj);
            }
        });

        database.getReference().child("Users").child(FirebaseAuth.getInstance().getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Users users = snapshot.getValue(Users.class);
                        Picasso.get().load(users.getProfileImage()).placeholder(R.drawable.images).into(binding.profileUserImage);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

        galleryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                Intent data = result.getData();
                    if(data.getData() != null){
                        Uri uri1 = data.getData();
                        binding.profileUserImage.setImageURI(uri1);
//                        final InputStream imageStream;
//                        try {
//                            imageStream = getContentResolver().openInputStream(uri1);
//                            final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
//                            binding.profileUserImage.setImageBitmap(selectedImage);
//                        } catch (FileNotFoundException e) {
//                            e.printStackTrace();
//                        }
                        final StorageReference reference = storage.getReference().child("profile_picture")
                                .child(FirebaseAuth.getInstance().getUid());
                        reference.putFile(uri1).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                Toast.makeText(SettingActivity.this,"image uploaded",Toast.LENGTH_SHORT).show();
                                reference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        database.getReference().child("Users").child(FirebaseAuth.getInstance().getUid())
                                                .child("profilePic").setValue(uri.toString());

                                    }
                                });
                            }
                        });
                    }
            }
        });

        binding.addPic.setOnClickListener(view -> {
            Intent storageIntent = new Intent();
            storageIntent.setType("image/*");
            storageIntent.setAction(Intent.ACTION_GET_CONTENT);
            galleryLauncher.launch(storageIntent);
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPermission(Manifest.permission.CAMERA,CAMERA_PERMISSION_CODE);
    }

    private void checkPermission(String permission, int requestCode){
        if(ContextCompat.checkSelfPermission(SettingActivity.this,permission)== PackageManager.PERMISSION_GRANTED){
            Toast.makeText(this, "Permission Granted.", Toast.LENGTH_SHORT).show();
        }
        else{
            ActivityCompat.requestPermissions(SettingActivity.this,new String[]{permission},requestCode);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode== CAMERA_PERMISSION_CODE){
            if(grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED){
                checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE,READ_STORAGE_PERMISSION_CODE);
            }
            else{
                Toast.makeText(this, "Camera Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
        else if(requestCode==READ_STORAGE_PERMISSION_CODE){
            if(grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED){
                checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE,WRITE_STORAGE_PERMISSION_CODE);
            }
            else{
                Toast.makeText(this, "Read Storage Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}