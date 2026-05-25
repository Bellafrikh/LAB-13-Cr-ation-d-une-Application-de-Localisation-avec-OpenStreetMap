package com.example.mapapplication_lab13;

import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class MapsActivity extends AppCompatActivity {

    private static final String TAG = "GoogleMapActivity";

    private MapView map;
    private EditText etSearch;
    private FloatingActionButton fabMyLocation;

    private RequestQueue requestQueue;
    private final String showUrl = "http://10.0.2.2/map_project/getPosition.php";

    // Coordonnées par défaut au centre de la carte au démarrage (Ex: Marrakech)
    private final GeoPoint defaultPoint = new GeoPoint(31.6295, -7.9811);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialisation d'OSMDroid avant de charger le layout
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid_prefs", MODE_PRIVATE));

        // CORRECTION : Utilisation du bon fichier layout personnalisé créé précédemment
        setContentView(R.layout.activity_maps);

        // Liaison des composants de l'interface
        map = findViewById(R.id.map);
        etSearch = findViewById(R.id.etSearch);
        fabMyLocation = findViewById(R.id.fabMyLocation);

        // Configuration de la carte
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setBuiltInZoomControls(false);
        map.setMultiTouchControls(true);

        // Animation de focus initiale
        map.getController().setZoom(14.0);
        map.getController().setCenter(defaultPoint);

        // Bouton flottant de recentrage
        fabMyLocation.setOnClickListener(v -> {
            map.getController().animateTo(defaultPoint);
            Toast.makeText(this, "Recentrage sur la position principale", Toast.LENGTH_SHORT).show();
        });

        // Initialisation de Volley
        requestQueue = Volley.newRequestQueue(this);

        // Chargement des données
        loadPositions();
    }

    /**
     * Interroge l'API PHP pour récupérer les coordonnées et dessine les marqueurs
     */
    private void loadPositions() {
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.POST,
                showUrl,
                null,
                response -> {
                    try {
                        JSONArray positions = response.getJSONArray("positions");

                        if (positions.length() == 0) {
                            Toast.makeText(this, "Aucun marqueur trouvé sur le serveur", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        for (int i = 0; i < positions.length(); i++) {
                            JSONObject position = positions.getJSONObject(i);
                            double lat = position.getDouble("latitude");
                            double lng = position.getDouble("longitude");

                            GeoPoint point = new GeoPoint(lat, lng);
                            Marker marker = new Marker(map);
                            marker.setPosition(point);
                            marker.setTitle("Position N°" + (i + 1));
                            marker.setSnippet(String.format("Lat: %.4f\nLng: %.4f", lat, lng));

                            // SOLUTION CODE : On supprime le bloc de redimensionnement de R.drawable.marker
                            // et on laisse OSMDroid appliquer automatiquement son icône de pin rouge par défaut.
                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

                            map.getOverlays().add(marker);
                        }

                        // Rafraîchissement de la carte
                        map.invalidate();
                        Toast.makeText(this, positions.length() + " repères affichés", Toast.LENGTH_SHORT).show();

                    } catch (JSONException e) {
                        Log.e(TAG, "Erreur de parsing JSON : " + e.getMessage());
                        Toast.makeText(this, "Données reçues corrompues", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e(TAG, "Impossible de joindre l'API : " + error.getMessage());
                    Toast.makeText(this, "Erreur de connexion (Serveur introuvable)", Toast.LENGTH_LONG).show();
                }
        );

        requestQueue.add(jsonObjectRequest);
    }

    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
    }
}