package com.app.kesava.myapplication;

import androidx.annotation.NonNull;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.widget.Toast;

import com.appolica.interactiveinfowindow.InfoWindow;
import com.appolica.interactiveinfowindow.InfoWindowManager;
import com.appolica.interactiveinfowindow.fragment.MapInfoWindowFragment;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;
import es.dmoral.toasty.Toasty;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, InfoWindowFragment.OnFragmentInteractionListener, NetworkStateReceiver.NetworkStateReceiverListener {

    private GoogleMap mMap;
    private DatabaseReference mFirebaseDatabase;
    private FirebaseDatabase mFirebaseInstance;
    private MapInfoWindowFragment mapInfoWindowFragment;
    private ValueEventListener geolocation_listener;
    private NetworkStateReceiver networkStateReceiver;

    private boolean infowindow_flag_avaliable = false;
    private boolean mapReady_flag = false;
    private boolean user_turn_off_internet = false;
    private boolean checking_alert_marker_exist = false;

    InfoWindow.MarkerSpecification spec;
    Marker marker;
    Fragment infowindowfragment;
    InfoWindow infoWindow;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        Log.i("MapsActivity","onCreate");


        //attach a network state listener

        networkStateReceiver = new NetworkStateReceiver();
        networkStateReceiver.addListener(this);
        this.registerReceiver(networkStateReceiver, new IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION));


            //remove all the notification, if user open through launcher icon

            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancelAll();

            mapInfoWindowFragment = (MapInfoWindowFragment) getSupportFragmentManager().findFragmentById(R.id.map);
            mapInfoWindowFragment.getMapAsync(this);


            mFirebaseInstance = FirebaseDatabase.getInstance();
            mFirebaseDatabase = mFirebaseInstance.getReference("Alert Event");

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        //remove firebase callback listener
        Log.i("MapsActivity","onDestroy, listener removed");
        mFirebaseInstance.getReference("Alert Event").child("event_details").child("geolocation").removeEventListener(geolocation_listener);

        //remove network listener
        networkStateReceiver.removeListener(this);
        this.unregisterReceiver(networkStateReceiver);

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mapReady_flag = true;

        // move map position to dataran as default
        LatLng dataran = new LatLng(3.139633, 101.700471);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(dataran,15));

        Log.i("MapsActivity","On map ready.");


        mFirebaseInstance.getReference("Alert Event").child("event_details").child("geolocation").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    //alert found, do nothing.
                }else{
                    Toasty.warning(getApplicationContext(),"No collision alert found",Toasty.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


        geolocation_listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                Log.i("MapsActivity","geolocation data changed!");

                if(dataSnapshot.exists() && infowindow_flag_avaliable == false) {

                    // this part will get executed when alert data first being written in db

                    checking_alert_marker_exist = true;

                    Log.i("MapsActivity","database content still exists");

                    setAlertOnMap(dataSnapshot);

                }else if(!dataSnapshot.exists() && infowindow_flag_avaliable == true){

                    // this part will get executed when data is being erase from db

                    Log.i("MapsActivity","database content already deleted, now removing marker ");
                    mMap.clear();
                    mapInfoWindowFragment.infoWindowManager().hide(infoWindow);
                    infowindow_flag_avaliable = false;

                }else{

                    // this part will get executed when no data in db or there is data in db but had changes made.
                    Log.i("MapsActivity","onDataChange callback executed, do nothing.");

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };

        mFirebaseInstance.getReference("Alert Event").child("event_details").child("geolocation").addValueEventListener(geolocation_listener);

    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    public  Drawable tintDrawable(Drawable drawable, ColorStateList colors) {
        final Drawable wrappedDrawable = DrawableCompat.wrap(drawable);
        DrawableCompat.setTintList(wrappedDrawable, colors);
        return wrappedDrawable;
    }

    @Override
    public void networkAvailable() {
        Log.i("MapsActivity","network available");

        if(user_turn_off_internet){
            Toasty.success(getApplicationContext(),"Connected to Internet.", Toast.LENGTH_SHORT, true).show();
            user_turn_off_internet = false;
        }


        mFirebaseInstance.getReference("Alert Event").child("event_details").child("geolocation").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && infowindow_flag_avaliable == false) {
                    setAlertOnMap(dataSnapshot);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toasty.error(getApplicationContext(),databaseError.getMessage());
            }
        });

    }

    @Override
    public void networkUnavailable() {
        Log.i("MapsActivity","network unavailable");

        user_turn_off_internet = true;

        if(mapReady_flag && infowindow_flag_avaliable == true) { //make sure infowindow is avaviable, then proceed to delete

            mMap.clear();
            mapInfoWindowFragment.infoWindowManager().hide(infoWindow);
            infowindow_flag_avaliable = false;

        }

        Toasty.warning(getApplicationContext(),"No Internet available, please enable Internet to continue.", Toast.LENGTH_SHORT, true).show();
    }


    public void setAlertOnMap(DataSnapshot dataSnapshot){

        String lat = "0.0";
        String lon = "0.0";
        String timestamp = "N/A";

        try {

            JSONObject coordinates = new JSONObject(dataSnapshot.getValue().toString());

            lat = coordinates.getString("latitude");
            lon = coordinates.getString("longitude");
            timestamp = coordinates.getString("timestamp");


        } catch (JSONException e) {
            e.printStackTrace();

            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();

        }


        LatLng newlocation = new LatLng(Double.valueOf(lat), Double.valueOf(lon));

        mMap.clear();

        spec = new InfoWindow.MarkerSpecification(0, 110);
        marker = mMap.addMarker(new MarkerOptions().position(newlocation));
        infowindowfragment = new InfoWindowFragment();


        Bundle data = new Bundle();
        data.putString("Alert_MSG", "Collision occurred here at: " + timestamp);
        infowindowfragment.setArguments(data);

        infoWindow = new InfoWindow(marker, spec, infowindowfragment);

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newlocation, 15));

        Drawable drawable_bg = getResources().getDrawable(R.drawable.infowindow_background);

        Drawable alert_drawable = tintDrawable(drawable_bg, ColorStateList.valueOf(Color.rgb(255, 193, 21)));

        InfoWindowManager.ContainerSpecification container_spec = new InfoWindowManager.ContainerSpecification(alert_drawable);

        mapInfoWindowFragment.infoWindowManager().setContainerSpec(container_spec);

        mapInfoWindowFragment.infoWindowManager().show(infoWindow, true); //display the info window fragment

        infowindow_flag_avaliable = true;

    }

}
