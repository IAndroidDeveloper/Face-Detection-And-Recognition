package com.anviam.faceregister;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.anviam.faceregister.face_recognition.FaceClassifier;
import com.anviam.faceregister.face_recognition.TFLiteFaceRecognition;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.List;

public class RecognizeActivity extends AppCompatActivity {
    ImageView imageView;
    View camera, gallery;
    int SELECT_PICTURE = 100;
    Uri image_uri;

    //face Detector Configurations
    FaceDetectorOptions highAccuracyOpts =
            new FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                    .build();
    FaceDetector detector;
    FaceClassifier faceClassifier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recognize);
        imageView = findViewById(R.id.imageView);
        camera = findViewById(R.id.camera);
        gallery = findViewById(R.id.Gallery);
        camera.setOnClickListener(view -> {
            permission();
            //openCamera();
        });
        gallery.setOnClickListener(view -> imageChooser());
        detector = FaceDetection.getClient(highAccuracyOpts);
        try {
            faceClassifier = TFLiteFaceRecognition.create(getAssets(), "facenet.tflite", 160, false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void imageChooser() {
        Intent i = new Intent();      //Intent throws the user to the Gallery Page
        i.setType("image/*");         // Represents the image Format that user has to select these kind of images
        i.setAction(Intent.ACTION_GET_CONTENT);      //After Selecting the image then Get ACTION CONTENT throws the selected image to the parent Activity and pass to the image URI.
        galleryActivityResultLauncher.launch(i); //launches the launcher Activity

    }

    //Permissions Method
    private void permission() {
        if (checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED || checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            String[] permissions = {android.Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
            requestPermissions(permissions, 121);
        } else openCamera();
    }

    private void openCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New Picture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From The Camera");
        image_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        cameraActivityResultLauncher.launch(cameraIntent);
    }

    ActivityResultLauncher<Intent> galleryActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        image_uri = result.getData().getData();
                        Bitmap inputimage = uriToBitmap(image_uri);
                        Bitmap rotated = rotateBitmap(inputimage);
                        imageView.setImageBitmap(rotated);
                        performFaceDetection(rotated);
                    }
                }
            }
    );

    ActivityResultLauncher<Intent> cameraActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Bitmap inputimage = uriToBitmap(image_uri);
                        Bitmap rotated = rotateBitmap(inputimage);
                        imageView.setImageBitmap(rotated);
                        performFaceDetection(rotated);
                    }
                }
            }
    );


    @SuppressLint("Range")
    private Bitmap rotateBitmap(Bitmap inputimage) {
        String[] orientationColumn = {MediaStore.Images.Media.ORIENTATION};
        Cursor cur = getContentResolver().query(image_uri, orientationColumn, null, null, null);
        int orientation = -1;
        if (cur != null && cur.moveToFirst()) {
            orientation = cur.getInt(cur.getColumnIndex(orientationColumn[0]));
        }
        Log.d("tryOrientation", orientation + "");
        Matrix rotationMatrix = new Matrix();
        rotationMatrix.setRotate(orientation);
        Bitmap cropped = Bitmap.createBitmap(inputimage, 0, 0, inputimage.getWidth(), inputimage.getHeight(), rotationMatrix, true);
        return cropped;
    }

    Canvas canvas;
    public void performFaceDetection(Bitmap input) {
        Bitmap mutablebmp = input.copy(Bitmap.Config.ARGB_8888, true);
         canvas = new Canvas(mutablebmp);
        InputImage image = InputImage.fromBitmap(input, 0);
        Task<List<Face>> result =
                detector.process(image)
                        .addOnSuccessListener(
                                new OnSuccessListener<List<Face>>() {
                                    @Override
                                    public void onSuccess(List<Face> faces) {
                                        // Task completed successfully
                                        // ...
                                        Log.d("tryFaces", "LenOfFace" + faces.size());
                                        for (Face face : faces) {
                                            Rect bounds = face.getBoundingBox();
                                            Paint paint = new Paint();
                                            paint.setColor(Color.WHITE);
                                            paint.setStyle(Paint.Style.STROKE);
                                            paint.setStrokeWidth(5);
                                            performFaceRecognition(bounds, input);
                                            canvas.drawRect(bounds, paint);
                                        }
                                        imageView.setImageBitmap(mutablebmp);
                                    }
                                })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Task failed with an exception
                                        // ...
                                    }
                                });
    }

    public void performFaceRecognition(Rect bound, Bitmap input) {
        if (bound.top < 0)
            bound.top = 0;
        if (bound.left < 0)
            bound.left = 0;
        if (bound.right > input.getWidth())
            bound.right = input.getWidth();
        if (bound.bottom > input.getHeight())
            bound.bottom = input.getHeight();
        Bitmap croppedFace = Bitmap.createBitmap(input, bound.left, bound.top, bound.width(), bound.height());
        imageView.setImageBitmap(croppedFace);
        croppedFace = Bitmap.createScaledBitmap(croppedFace, 160, 160, false);
        //if want to register the face pass true here else pass false here
        FaceClassifier.Recognition recognition = faceClassifier.recognizeImage(croppedFace, false);
        Log.d("tryf",recognition.getTitle()+recognition.getDistance());
        //recognition.getTitle();
        //   recognition.getEmbeeding();
       // showRegisterDialouge(croppedFace, recognition);

    }

    /*public void showRegisterDialouge(Bitmap face, FaceClassifier.Recognition recognition) {
        //Dialog dialog = new Dialog(this);
        setContentView(R.layout.register_face_dialouge);
        TextView textView = findViewById(R.id.registertheUser);
        TextView Name = findViewById(R.id.enterTheName);
        EditText mEntertheName = findViewById(R.id.username);
        ImageView imageView = findViewById(R.id.image);
        Button registerButton = findViewById(R.id.register);
        imageView.setImageBitmap(face);
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mEntertheName.getText().toString().equals(""))
                    mEntertheName.setError("Enter the Name");
                else {
                    faceClassifier.register(mEntertheName.getText().toString(), recognition);
                    Toast.makeText(RecognizeActivity.this, "Face is Registered Successfully", Toast.LENGTH_LONG).show();
                }
            }
        });
        //dialog.show();
    }*/

    private Bitmap uriToBitmap(Uri selectedFileUri) {
        try {
            ParcelFileDescriptor parcelFileDescriptor =
                    getContentResolver().openFileDescriptor(selectedFileUri, "r");
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
            parcelFileDescriptor.close();
            return image;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    //In this case Activity For Result is depreciated
    //startActivityForResult(Intent.createChooser(i, "Select Picture"), SELECT_PICTURE);
    /*ActivityResultLauncher<Intent> launcher = registerForActivityResult(new
                    ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Bitmap inputimage = uriToBitmap(image_uri);
                    Bitmap rotated = rotateBitmap(inputimage);
                    imageView.setImageBitmap(rotated);
                    PerformFaceDetection(rotated);
                    Intent data = result.getData();
                    if (data != null && data.getData() != null) {
                        Uri selectedImageUri = data.getData();
                        Bitmap selectedImageBitmap = null;
                        try {
                            selectedImageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        imageView.setImageBitmap(selectedImageBitmap);
                    }
                }
            });*/


    //This Activity cannot be Private
  /*  public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);//After adding data to this function it immediatelly calls the super method which is the quick reference to the parent activity
        if (requestCode == RESULT_OK) {
            if (requestCode == SELECT_PICTURE) {
                Uri selectedImageUri = data.getData();
                if (null != selectedImageUri) {
                    imageView.setImageURI(selectedImageUri);
                }
            }
        }
    }*/

}