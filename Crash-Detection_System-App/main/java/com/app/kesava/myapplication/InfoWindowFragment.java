package com.app.kesava.myapplication;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import es.dmoral.toasty.Toasty;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link InfoWindowFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link InfoWindowFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class InfoWindowFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private DatabaseReference mFirebaseDatabase;
    private FirebaseDatabase mFirebaseInstance;

    private OnFragmentInteractionListener mListener;
    private ProgressDialog dialog;

    Button button_false_alarm;
    Button button_noted;
    TextView infowindow_title;

    public InfoWindowFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment InfoWindowFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static InfoWindowFragment newInstance(String param1, String param2) {
        InfoWindowFragment fragment = new InfoWindowFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i("infowindow_fragment","onCreate");

        mFirebaseInstance = FirebaseDatabase.getInstance();
        mFirebaseDatabase = mFirebaseInstance.getReference("Alert Event").child("event_details");


        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_info_window, container, false);

        dialog = new ProgressDialog(container.getContext());
        dialog.setCanceledOnTouchOutside(false);

        button_false_alarm = view.findViewById(R.id.false_alert_btn);
        button_noted = view.findViewById(R.id.noted_btn);
        infowindow_title = view.findViewById(R.id.infoWindowTitle);

        String message = getArguments().getString("Alert_MSG");
        infowindow_title.setText(message);

        button_false_alarm.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                mFirebaseDatabase.child("status").setValue("ignore").addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {

                        dialog.setTitle("Please wait..");
                        dialog.setMessage("Communicating with collision sensor.");
                        dialog.show();

                        //now start the timeout counter
                        new CountDownTimer(5000,1000){

                            @Override
                            public void onTick(long l) {

                            }

                            @Override
                            public void onFinish() {

                                dialog.dismiss();

                                //now check if data in db still exist. if still exist, fallback to previous state
                                mFirebaseDatabase.child("status").addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        if(dataSnapshot.exists()){
                                            //data still exist,so fallback ,means low end device did not do the deletion job.
                                            mFirebaseDatabase.child("status").setValue("uncheck");
                                            Toasty.warning(container.getContext(),"Collision sensor not responding, please make sure is on and connected to network.", Toast.LENGTH_LONG, true).show();
                                        }else{
                                            //data deletion success
                                            Log.i("InfoWindowFragment","false alarm, please ignore");
                                            Toasty.error(container.getContext(),"Is a false alarm, please ignore", Toast.LENGTH_LONG, true).show();
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {
                                    }
                                });
                            }

                        }.start();

                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toasty.error(getContext(),e.getMessage(),Toast.LENGTH_LONG,true).show();
                    }
                });

            }
        });

        button_noted.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view) {

                dialog.setTitle("Please wait..");
                dialog.setMessage("Communicating with collision sensor.");
                dialog.show();

                mFirebaseDatabase.child("status").setValue("checked").addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {


                        //now start the timeout counter
                        new CountDownTimer(5000,1000){

                            @Override
                            public void onTick(long l) {

                            }

                            @Override
                            public void onFinish() {

                                dialog.dismiss();

                                //now check if data in db still exist. if still exist, fallback to previous state
                                mFirebaseDatabase.child("status").addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        if(dataSnapshot.exists()){
                                            //data still exist,so fallback ,means low end device did not do the deletion job.
                                            mFirebaseDatabase.child("status").setValue("uncheck");
                                            Toasty.warning(container.getContext(),"Collision sensor not responding, please make sure is on and connected to network.", Toast.LENGTH_LONG, true).show();
                                        }else{
                                            //data deletion success
                                            Log.i("InfoWindowFragment","noted!");
                                            Toasty.success(container.getContext(),"Collision noted!", Toast.LENGTH_LONG, true).show();
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });
                            }

                        }.start();

                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toasty.error(getContext(),e.getMessage(),Toast.LENGTH_LONG,true).show();
                    }
                });


            }
        });


        return view;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

}
