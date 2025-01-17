package com.example.project2;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.project2.model.Route;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;

/**
 * Activity for allowing users to create a new route
 */
public class CreateRouteActivity extends AppCompatActivity {

    private static final String TAG = "CreateRouteActivity";
    private static final int REQUEST_CAMERA_PERMISSION = 100;

    /**
     * Variables for elements in the activity_create_route.xml layout.
     */
    private Uri routeImageUri;
    private Bitmap routeImageBitmap;
    private ActivityResultLauncher<Uri> takePhotoLauncher;

    private EditText titleInput, locationInput, slopeInput, difficultyInput, descriptionInput;
    private Button takePhotoButton, submitButton;
    private ImageButton backButton;
    private ImageView imagePreview;
    private RadioButton publicRadioButton, privateRadioButton;
    private byte[] photoBytes;

    /**
     * Firebase Firestore instance
     */
    private FirebaseFirestore firestore;

    /**
     * Initializes the activity and sets up the button click listeners.
     * @param savedInstanceState The saved state of the activity.
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_route);

        // Check if the app has camera permission
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // Request the camera permission
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }

        if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

        // Find UI components
        takePhotoButton = findViewById(R.id.take_photo_btn);
        imagePreview = findViewById(R.id.image_preview);
        titleInput = findViewById(R.id.title_input);
        locationInput = findViewById(R.id.location_input);
        slopeInput = findViewById(R.id.slope_input);
        difficultyInput = findViewById(R.id.difficulty_input);
        descriptionInput = findViewById(R.id.description_input);
        submitButton = findViewById(R.id.btn_submit);
        backButton = findViewById(R.id.back_button);
        publicRadioButton = findViewById(R.id.radio_public);
        privateRadioButton = findViewById(R.id.radio_private);

        ActivityResultLauncher<Intent> editImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Log.d(TAG, "In ImageEditActivity Return");
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri editedImageUri = (Uri) result.getData().getExtras().get("editedImage");
                        Log.d(TAG, "Edited Photo Uri: " + editedImageUri);
                        routeImageBitmap = copyBitmap(uriToBitmap(editedImageUri));
                        Log.d(TAG, "Edited Photo Bitmap: " + routeImageBitmap);

                        if (routeImageBitmap != null) {
                            imagePreview.setImageBitmap(routeImageBitmap);
                            Log.d(TAG, "Photo captured, edited, and loaded successfully.");
                        } else {
                            Log.e(TAG, "Edited photo URI is null.");
                        }
                    }
                }
        );

        // Initialize the launcher for camera
        takePhotoLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                result -> {
                    if (result) {
                        // Process the captured image only after the user takes a photo
                        try {
                            if (isUriValid(routeImageUri)) {
                                Intent intent = new Intent(CreateRouteActivity.this, ImageEditActivity.class);
                                intent.putExtra("photo", routeImageUri);
                                editImageLauncher.launch(intent);
                            } else {
                                Log.e(TAG, "Captured image URI is invalid.");
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing captured image.", e);
                        }
                    } else {
                        Toast.makeText(this, "Photo capture canceled or failed.", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // Set up back button
        backButton.setOnClickListener(v -> finish()); // goes back to dashboard view

        //Set up take photo button
        takePhotoButton.setOnClickListener(v -> {
            try {
                takePhoto();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        // Set up submit button
        submitButton.setOnClickListener(v -> handleSubmit());
    }

    /**
     * Creates a mutable copy of a given bitmap
     * @param original bitmap to be copied
     * @return mutable copy of original
     */
    private Bitmap copyBitmap(Bitmap original) {
        return original.copy(original.getConfig(), true); // Create a mutable copy
    }

    /**
     * Checks if given Uri is valid for the local device
     * @param uri Uri to be checked
     * @return true if valid Uri
     */
    private boolean isUriValid(Uri uri) {
        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
            return inputStream != null;
        } catch (Exception e) {
            Log.e("CreateRouteActivity", "Invalid URI: " + uri, e);
            return false;
        }
    }

    /**
     * Uses MediaStore library to launch photo taking activity
     * @throws IOException
     */
    private void takePhoto() throws IOException {
        // Create a content URI for the image and assign it to the routeImage variable
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New Route Photo");
        values.put(MediaStore.Images.Media.DESCRIPTION, "Photo taken for the route");
        ContentResolver resolver = getContentResolver();

        routeImageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);


        // Pass the URI to the camera via the intent
        if (routeImageUri != null) {
            Log.d("routeImageURI", "PICTURE URI WAS SUCCESS");
            Log.d("routeImageURI", "URI: " + routeImageUri.toString());
            Log.d("routeImageURI", "Scheme: " + routeImageUri.getScheme());
            Log.d("routeImageURI", "Authority: " + routeImageUri.getAuthority());
            Log.d("routeImageURI", "Path: " + routeImageUri.getPath());
            takePhotoLauncher.launch(routeImageUri);
        } else {
            Log.e("BitmapDetails", "PICTURE BITMAP WAS NULL");
            Toast.makeText(this, "Failed to create image file.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Converts a Uri to a Bitmap.
     *
     * @param uri The URI of the image.
     * @return The image as a Bitmap, or null if conversion fails.
     * @throws IOException If an error occurs while reading the image.
     * @throws IllegalArgumentException If the URI is {@code null}.
     */
    private Bitmap uriToBitmap(Uri uri) {
        Bitmap bitmap = null;
        try {
            // Open an InputStream from the Uri
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                // Decode the InputStream to a Bitmap
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                bitmap = BitmapFactory.decodeStream(inputStream, null, options);
                inputStream.close(); // Always close the stream to avoid memory leaks
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    /**
     * Capitalize the first letter of each word in user's input to keep text consistent among all users.
     * @param text The text that the user has entered into the create route form.
     * @return Modified text where the first letter in each word is capitalized.
     */
    private String capitalizeFirstLetter(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String[] words = text.split(" ");
        StringBuilder capitalizedText = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                capitalizedText.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1).toLowerCase())
                        .append(" ");
            }
        }

        return capitalizedText.toString().trim();
    }

    /**
     * Handles the submission of the route creation form.
     */
    private void handleSubmit() {
        Log.d("IN handleSubmit(): ", "IN METHOD handleSubmit()");
        // Get the current user
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to create a route", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get input values
        String title = capitalizeFirstLetter(titleInput.getText().toString().trim());
        String location = capitalizeFirstLetter(locationInput.getText().toString().trim());
        String slope = capitalizeFirstLetter(slopeInput.getText().toString().trim());
        String difficulty = capitalizeFirstLetter(difficultyInput.getText().toString().trim());
        String description = descriptionInput.getText().toString().trim();
        String userId = currentUser.getUid();

        // Check that the user has entered all required input fields
        if (title.isEmpty() || location.isEmpty() || slope.isEmpty() || difficulty.isEmpty() || description.isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a Route object
        Route route = new Route();
        route.setTitle(title);
        route.setCity(location);
        route.setSlope(slope);
        route.setDifficulty(difficulty);
        route.setDescription(description);
        route.setAvgRating(0.0);

        // Check the selected access type via the radio button checked by the user
        if (publicRadioButton.isChecked()) {
            // Save to both "user_routes" and "community_routes"
            saveRouteToFirestore("user_routes", route);
            saveRouteToFirestore("community_routes", route);
        } else if (privateRadioButton.isChecked()) {
            // Save only to "user_routes"
            saveRouteToFirestore("user_routes", route);
        } else {
            Toast.makeText(this, "Please select Public or Private access", Toast.LENGTH_SHORT).show();
            return;
        }

        // Finish the activity to go back to the dashboard view
        finish();
    }

    /**
     * Saves a route to the correct collection in the Firestore database. Stores the related image to Firebase Cloud Storage using the route name.
     * @param collection The collection to save the route to.
     * @param route The route to save.
     */
    private void saveRouteToFirestore(String collection, Route route) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        routeImageBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        photoBytes = (byteArray);
        FirebaseStorage storage = FirebaseStorage.getInstance();
        // Create a storage reference from our app
        StorageReference storageRef = storage.getReferenceFromUrl("gs://project-2-1d31a.firebasestorage.app");
        StorageReference riversRef = storageRef.child("RoutePhotos/" + route.getTitle() + ".jpeg");
        UploadTask uploadTask = riversRef.putBytes(photoBytes);

        route.setPhoto("");
        firestore.collection(collection)
                .add(route)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Route added to " + collection + " with ID: " + documentReference.getId());
                    if (collection.equals("user_routes")) {
                        Toast.makeText(getApplicationContext(), "Route added to your routes!", Toast.LENGTH_SHORT).show();
                    } else if (collection.equals("community_routes")) {
                        Toast.makeText(getApplicationContext(), "Route added to the community!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding route to " + collection, e);
                    Toast.makeText(getApplicationContext(), "Failed to add route to " + collection, Toast.LENGTH_SHORT).show();
                });
    }
}
