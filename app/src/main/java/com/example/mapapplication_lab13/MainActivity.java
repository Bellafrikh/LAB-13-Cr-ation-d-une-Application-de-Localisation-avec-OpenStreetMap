package com.example.mapapplication_lab13;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 100;

    private Button btnMap;
    private double latitude;
    private double longitude;
    private double altitude;
    private float accuracy;

    private RequestQueue requestQueue;
    private LocationManager locationManager;

    // URL locale de votre script PHP (via l'adresse spéciale de l'émulateur Android)
    private final String insertUrl = "http://10.0.2.2/map_project/createPosition.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialisation des services essentiels
        requestQueue = Volley.newRequestQueue(this);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Configuration du bouton d'affichage de la carte (Syntaxe Lambda moderne)
        btnMap = findViewById(R.id.btnMap);
        btnMap.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MapsActivity.class);
            startActivity(intent);
        });

        // Demande et vérification des permissions au démarrage
        checkAndRequestPermissions();
    }

    /**
     * Centralise la logique de vérification des permissions requises pour l'application
     */
    private void checkAndRequestPermissions() {
        boolean hasLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean hasPhoneState = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;

        if (!hasLocation || !hasPhoneState) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.READ_PHONE_STATE
                    }, PERMISSION_REQUEST_CODE);
        } else {
            startLocationUpdates();
        }
    }

    /**
     * Active l'écoute active des changements de coordonnées GPS
     */
    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Configuration : Fournisseur GPS, rafraîchissement toutes les 60s ou tous les 150 mètres
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 60000, 150, new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
                altitude = location.getAltitude();
                accuracy = location.getAccuracy();

                // Création d'un feedback visuel propre pour l'utilisateur
                String feedbackMessage = String.format(Locale.getDefault(),
                        "Position actualisée\nLat : %.4f | Lon : %.4f\nPrécision : %.1fm",
                        latitude, longitude, accuracy);

                Toast.makeText(getApplicationContext(), feedbackMessage, Toast.LENGTH_SHORT).show();

                // Envoi des nouvelles données de suivi vers l'API
                addPosition(latitude, longitude);
            }

            @Override
            public void onProviderEnabled(@NonNull String provider) {
                Log.d(TAG, "Le fournisseur de localisation a été activé : " + provider);
            }

            @Override
            public void onProviderDisabled(@NonNull String provider) {
                Toast.makeText(MainActivity.this, "Veuillez activer votre GPS pour le suivi.", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                Toast.makeText(this, "Permissions requises pour localiser et enregistrer vos parcours.", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Transmet les données de positionnement récoltées vers la base de données distante
     */
    private void addPosition(final double lat, final double lon) {
        StringRequest request = new StringRequest(Request.Method.POST, insertUrl,
                response -> {
                    // Ajout d'un suivi clair pour savoir si le script PHP a bien fonctionné
                    Log.i(TAG, "Succès de la synchronisation : " + response);
                    Toast.makeText(getApplicationContext(), "Position synchronisée avec le serveur", Toast.LENGTH_SHORT).show();
                },
                error -> {
                    // Notification humaine en cas de coupure réseau ou serveur inaccessible
                    Log.e(TAG, "Erreur réseau Volley : " + error.getMessage());
                    Toast.makeText(getApplicationContext(), "Échec de synchronisation (Serveur déconnecté)", Toast.LENGTH_SHORT).show();
                }) {

            @Override
            protected Map<String, String> getParams() {
                HashMap<String, String> params = new HashMap<>();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

                params.put("latitude", String.valueOf(lat));
                params.put("longitude", String.valueOf(lon));
                params.put("date", sdf.format(new Date()));

                // Utilisation de l'ANDROID_ID (solution fiable sans faille de confidentialité)
                String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
                params.put("imei", androidId);

                return params;
            }
        };

        requestQueue.add(request);
    }
}