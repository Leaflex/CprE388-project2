package com.example.project2;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.project2.model.Route;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

/**
 * Activity for displaying information about a route when clicked on in the dashboard view
 */
public class RouteDetailActivity extends AppCompatActivity {

    private static final String TAG = "RouteDetailActivity";
    public static final String KEY_ROUTE_ID = "key_route_id";
    public static final String KEY_ROUTE_COLLECTION = "key_route_collection";

    /**
     * Variables for UI elements in the activity_route_details.xml layout.
     */
    private TextView routeTitle, location, difficulty, slope, routeDescription;
    private RatingBar communityRatingBar;
    private ImageView imageOne, imageTwo;

    /**
     * Variables for Firestore.
     */
    private FirebaseFirestore firestore;
    private DocumentReference routeRef;
    private String routeCollection;

    /**
     * Initializes the activity.
     * @param savedInstanceState The saved state of the activity.
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_details);

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

        // Get Route ID and Collection from Intent Extras
        String routeId = getIntent().getStringExtra(KEY_ROUTE_ID);
        routeCollection = getIntent().getStringExtra(KEY_ROUTE_COLLECTION);
        if (routeId == null || routeCollection == null) {
            throw new IllegalArgumentException("Must pass extras " + KEY_ROUTE_ID + " and " + KEY_ROUTE_COLLECTION);
        }

        // Get reference to the route collection
        routeRef = firestore.collection(routeCollection).document(routeId);

        // Initialize UI Components
        routeTitle = findViewById(R.id.route_title);
        location = findViewById(R.id.location);
        difficulty = findViewById(R.id.difficulty);
        slope = findViewById(R.id.slope);
        routeDescription = findViewById(R.id.route_description);
        communityRatingBar = findViewById(R.id.community_rating_bar);
        imageOne = findViewById(R.id.image_one);
        imageTwo = findViewById(R.id.image_two);

        // Load route details
        loadRouteDetails();

        // When back button is pressed, go back to the dashboard view
        findViewById(R.id.back_button).setOnClickListener(v -> {
            Intent intent = new Intent(RouteDetailActivity.this, MainActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.image_one).setOnClickListener(v -> {
        });

        // Set up BottomNavigationView
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(this::onNavigationItemSelected);
    }

    /**
     * Handles navigation item selection in the BottomNavigationView.
     * @param item The selected navigation item.
     * @return True if the selection is handled, false otherwise.
     */
    private boolean onNavigationItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.nav_reviews){
            switchToReviewsView();
            return true;
        }
        else if(item.getItemId() == R.id.nav_delete) {
            deleteRoute();
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Go to the activity_route_reviews.xml layout when the "Reviews" button is pressed.
     */
    private void switchToReviewsView() {
        // Create an Intent to open the RouteReviewsActivity
        Intent intent = new Intent(RouteDetailActivity.this, RouteReviewsActivity.class);

        // Pass the route ID to the RouteReviewsActivity
        intent.putExtra(RouteReviewsActivity.KEY_ROUTE_ID, routeRef.getId());

        // Pass the collection the route is in (community_routes or user_routes)
        intent.putExtra(RouteDetailActivity.KEY_ROUTE_COLLECTION, routeCollection);

        // Pass the title of the route
        intent.putExtra(RouteReviewsActivity.KEY_ROUTE_TITLE, routeTitle.getText());

        // Start the RouteReviewsActivity
        startActivity(intent);
    }

    /**
     * Delete the route from the database (and all collections if the route is public).
     */
    private void deleteRoute() {
        // Check if the route exists in the "user_routes" collection, aka if the route belongs to the user
        firestore.collection("user_routes")
                .whereEqualTo("title", routeTitle.getText()) // Use the title to identify the route
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        // Route exists in "user_routes", proceed with deletion
                        routeRef.delete().addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Route deleted from " + routeCollection);
                            Toast.makeText(this, "Route deleted successfully", Toast.LENGTH_SHORT).show();

                            // If the route is public, delete it from the other collection
                            String otherCollection = routeCollection.equals("community_routes") ? "user_routes" : "community_routes";
                            deleteFromOtherCollection(otherCollection, (String) routeTitle.getText());

                            // Navigate back to the dashboard after deletion
                            Intent intent = new Intent(RouteDetailActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();

                        }).addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to delete route from " + routeCollection, e);
                            Toast.makeText(this, "Failed to delete route", Toast.LENGTH_SHORT).show();
                        });
                    } else {
                        // Route does not exist in "user_routes", aka the route was not created by the user
                        Toast.makeText(this, "Route cannot be deleted as you didn't create it", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Route does not exist in user_routes");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to check route in user_routes", e);
                    Toast.makeText(this, "Failed to validate route existence", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Helper function to delete route from the another collection if route was posted publicly.
     * @param otherCollection The other collection to delete the route from.
     * @param routeTitle The title of the route in the collection to delete.
     */
    private void deleteFromOtherCollection(String otherCollection, String routeTitle) {
        firestore.collection(otherCollection)
                .whereEqualTo("title", routeTitle)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                            document.getReference().delete()
                                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Route deleted from " + otherCollection))
                                    .addOnFailureListener(e -> Log.e(TAG, "Failed to delete route from " + otherCollection, e));
                        }
                    } else {
                        Log.d(TAG, "No matching route found in " + otherCollection);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to query " + otherCollection, e));
    }

    /**
     * Fetch route information from the database
     */
    private void loadRouteDetails() {
        routeRef.get().addOnSuccessListener(snapshot -> {
            if (!snapshot.exists()) {
                Toast.makeText(this, "Route not found", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            Route route = snapshot.toObject(Route.class);
            if (route != null) {
                displayRouteDetails(route);
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to load route", e);
            Toast.makeText(this, "Failed to load route", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Display route information in the UI.
     * @param route The route to display.
     */
    private void displayRouteDetails(Route route) {
        Log.d(TAG, "IN METHOD displayRouteDetails()");
        // Set UI elements with route data
        routeTitle.setText(route.getTitle());
        location.setText(String.format("Location: %s", route.getCity()));
        difficulty.setText(String.format("Difficulty: %s", route.getDifficulty()));
        slope.setText(String.format("Slope: %s", route.getSlope()));
        routeDescription.setText(route.getDescription());
        communityRatingBar.setRating((float) route.getAvgRating());

        // Load images
        Glide.with(this)
                .load(route.getPhotoAsBitmap())
                .into(imageOne);

//        Glide.with(this)
//                .load(route.getPhotoAsBitmap())
//                .into(imageTwo);
    }
}
