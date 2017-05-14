package com.uninorte.proyecto2;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class Dashboard extends AppCompatActivity implements View.OnClickListener,LocationListener,GoogleApiClient.OnConnectionFailedListener {

    //google
    private GoogleApiClient googleApiClient;
    private FirebaseAuth.AuthStateListener firebaseAuthListener;
    //
    private TextView txtWelcome;
    private Button btnLogout;
    private RelativeLayout activity_dashboard;

    private FirebaseAuth auth;

    //-------------------------------------------------------------------
    public static String data;
    final Messenger mMessenger = new Messenger(new IncomingHandler());
    Notification notification;
    int mNotificationId = 001;
    int REQUEST_CODE = 1;
    TelephonyManager myTelephonyManager;
    PhoneStateListener callStateListener;
    private String RecorridoId;
    private Recorrido recorrido;
    private boolean isAdmin;
    ArrayList<String> track =new ArrayList<String>();
    /**
     * Messenger for communicating with the service.
     */
    Messenger mService = null;
    /**
     * Flag indicating whether we have called bind on the service.
     */
    boolean mBound;
    private LocationUpdaterServices myService;
    //Variables
    private LocationManager mManager;
    private NotificationManager mNotificationManager;
    private boolean enableConnection;
    private boolean enableGPS;
    private DatabaseReference mDatabase;
    private Toolbar toolbar;
    private Track tracked;
   // DatabaseReference mDatabase;


    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {

            mService = new Messenger(service);

            // We want to monitor the service for as long as we are
            // connected to it.
            try {
                Message msg = Message.obtain(null,
                        LocationUpdaterServices.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);

                // Give it some value as an example.
                msg = Message.obtain(null,
                        LocationUpdaterServices.MSG_SET_VALUE, this.hashCode(), 0);
                mService.send(msg);
            } catch (RemoteException e) {

            }

           Toast.makeText(Dashboard.this, "Conectado",
                    Toast.LENGTH_SHORT).show();

        }

        public void onServiceDisconnected(ComponentName className) {
            mService = null;
           Toast.makeText(Dashboard.this, "Desconectado",
                    Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        //View
        txtWelcome = (TextView) findViewById(R.id.dashboard_welcome);

        btnLogout = (Button) findViewById(R.id.dashboard_btn_logout);
        activity_dashboard = (RelativeLayout) findViewById(R.id.activity_dash_board);
        btnLogout.setOnClickListener(this);

        //Init Firebase
        auth = FirebaseAuth.getInstance();

        //google----------------------------------------------
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        googleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        auth = FirebaseAuth.getInstance();
        firebaseAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    //setUserData(user);
                } else {
                    goLogInScreen();
                }
            }
        };
        //------------------------------------------------
        //Session check
        if (auth.getCurrentUser() != null) {
            txtWelcome.setText("Bienvenido , " + auth.getCurrentUser().getEmail());
        }
        isAdmin=false;

        mDatabase =FirebaseDatabase.getInstance().getReference("users");
        String userId=auth.getCurrentUser().getUid();

        mDatabase.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                User user=dataSnapshot.getValue(User.class);
                if (user.getRole().equals("Administrador")){
                    isAdmin=true;
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        if (!isAdmin) {
            //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            mManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            final LocationManager manager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );


            if ( !manager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
                buildAlertMessageNoGps();
            }
            myTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            callStateListener = new PhoneStateListener() {
                public void onDataConnectionStateChanged(int state) {
                    String stateString;
                    switch (state) {
                        case TelephonyManager.DATA_CONNECTED:
                            enableConnection = true;
                            break;
                        case TelephonyManager.DATA_DISCONNECTED:
                            Log.i("State: ", "Offline");
                            stateString = "Offline";
                            enableConnection = false;
                            Toast.makeText(getApplicationContext(),
                                    stateString, Toast.LENGTH_LONG).show();
                            break;
                        case TelephonyManager.DATA_SUSPENDED:
                            Log.i("State: ", "IDLE");
                            stateString = "Idle";
                            Toast.makeText(getApplicationContext(),
                                    stateString, Toast.LENGTH_LONG).show();
                            break;
                    }
                }
            };
            myTelephonyManager.listen(callStateListener,
                    PhoneStateListener.LISTEN_DATA_CONNECTION_STATE);

            //empiezo a guardar apenas inicializo

            startGPS();
        }else{

        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("GPS Desactivado");
        builder.setMessage("Su GPS se encuentra desactivado, desea activarlo?");
        builder.setCancelable(false);


        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener()
        {
            public void onClick(final DialogInterface dialog, final int id)
            {
                launchGPSOptions();
            }
        });

        builder.setNegativeButton("No", new DialogInterface.OnClickListener()
        {
            public void onClick(final DialogInterface dialog, final int id)
            {
                dialog.cancel();
            }
        });

        builder.create().show();
    }

    private void launchGPSOptions()
    {
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivityForResult(intent, REQUEST_CODE);
    }

    public void stopGps() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        //Detiene el servicio
        if (mBound) {
            mBound = false;
            Calendar c = Calendar.getInstance();
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String formattedDate = df.format(c.getTime());
            mDatabase = FirebaseDatabase.getInstance().getReference("recorridos").child(auth.getCurrentUser().getUid()).child(RecorridoId);
            recorrido.setTimeF(formattedDate);
            mDatabase.child("timeF").setValue(formattedDate);


            Intent ir = new Intent(this, LocationUpdaterServices.class);
            stopService(ir);
            unbindService(mConnection);
        }

    }
    public void startGPS() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                    1);
            return;

        }

        boolean enabled = mManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if(enabled){
            mBound = true;
            Intent ir = new Intent(this, LocationUpdaterServices.class);

            //CORREGIRRRRR PASAR KEY
            Calendar c = Calendar.getInstance();
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String formattedDate = df.format(c.getTime());
            recorrido= new Recorrido(auth.getCurrentUser().getUid(),formattedDate);
            mDatabase = FirebaseDatabase.getInstance().getReference("recorridos").child(auth.getCurrentUser().getUid());
            RecorridoId= mDatabase.push().getKey();
            mDatabase.child(RecorridoId).setValue(recorrido);
            // Bind to the service
            bindService(new Intent(this, LocationUpdaterServices.class), mConnection,
                    Context.BIND_AUTO_CREATE);
        } else {

        }

    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {

            case 1:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startGPS();
                }
                else {
                }
                return;

        }


    }



    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.dashboard_btn_logout :
                stopGps();
                logoutUser();
                break;
        }
    }

    private void logoutUser() {
        auth.signOut();
        if(auth.getCurrentUser() == null)
        {
            startActivity(new Intent(Dashboard.this,Principal.class));
            finish();
        }
    }


    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {
        Toast.makeText(getApplicationContext(), "GPS conectado", Toast.LENGTH_LONG).show();
        enableGPS = true;

    }

    @Override
    public void onProviderDisabled(String s) {
        Toast.makeText(getApplicationContext(),"GPS desconectado",Toast.LENGTH_LONG).show();
        enableGPS = false;
    }

    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case LocationUpdaterServices.MSG_SET_VALUE:
                    String str1 = msg.getData().getString("str1");
                    Log.d("VendorActivityTag", "handleMessage: " + str1);
                    int index = str1.indexOf("|");
                    String latitud = str1.substring(0, index);
                    String longitud = str1.substring(index + 1, str1.length());
                    Track tracked = new Track(latitud, longitud);
                    mDatabase=FirebaseDatabase.getInstance().getReference("tracks").child(RecorridoId);
                    String TrackId=mDatabase.push().getKey();
                    mDatabase.child(TrackId).setValue(tracked);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    //google------------------------------


    @Override
    protected void onStart() {
        super.onStart();

        auth.addAuthStateListener(firebaseAuthListener);
    }
    private void goLogInScreen() {
        Intent intent = new Intent(this, Login.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
    public void logOut(View view) {
        auth.signOut();

        Auth.GoogleSignInApi.signOut(googleApiClient).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(@NonNull Status status) {
                if (status.isSuccess()) {
                    goLogInScreen();
                } else {
                    Toast.makeText(getApplicationContext(), R.string.not_close_session, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    protected void onStop() {
        super.onStop();

        if (firebaseAuthListener != null) {
            auth.removeAuthStateListener(firebaseAuthListener);
        }
    }
}
