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
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import fr.ganfra.materialspinner.MaterialSpinner;

public class Dashboard extends AppCompatActivity implements View.OnClickListener,LocationListener{

    //google
    private GoogleApiClient googleApiClient;
    private FirebaseAuth.AuthStateListener firebaseAuthListener;
    //
    private TextView txtWelcome,tipousua,tvFecha;
    private Button btnLogout,btnMapa;
    private LinearLayout opc;
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
    private DatabaseReference refvend;
    private Toolbar toolbar;
    private Track tracked;
   // DatabaseReference mDatabase;

    private List<Vendedor> vendedorList;
    private MaterialSpinner spVen,spFecha;
    private String vendKey;
    private SpinnerAdapterVend spinnerAdapterVend;

    private List<RecowithKey> recorridoList;
    private String recoKey;
    private SpinnerAdapterReco spinnerAdapterReco;


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
        tipousua = (TextView) findViewById(R.id.tipousuario);
        opc = (LinearLayout) findViewById(R.id.OpcAdm);

        btnLogout = (Button) findViewById(R.id.dashboard_btn_logout);
        activity_dashboard = (RelativeLayout) findViewById(R.id.activity_dash_board);
        btnLogout.setOnClickListener(this);

        btnMapa = (Button) findViewById(R.id.dashboard_btn_mapa);
        btnMapa.setOnClickListener(this);

        final MaterialSpinner spVen = (MaterialSpinner) findViewById(R.id.spinnerVend);
        final MaterialSpinner spFecha = (MaterialSpinner) findViewById(R.id.spinnerFecha);
        tvFecha =(TextView) findViewById(R.id.textViewFecha);
        tvFecha.setVisibility(View.INVISIBLE);
        spFecha.setVisibility(View.INVISIBLE);
        recorridoList=new ArrayList<>();
        vendedorList=new ArrayList<>();

        //Init Firebase
        auth = FirebaseAuth.getInstance();


        //Session check
        if (auth.getCurrentUser() != null) {
            txtWelcome.setText("Bienvenido , " + auth.getCurrentUser().getEmail());

        }
        isAdmin=true;


        String userId=auth.getCurrentUser().getUid();
        mDatabase =FirebaseDatabase.getInstance().getReference("users").child(userId);


        mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                User user=dataSnapshot.getValue(User.class);
                Log.i("Tipo Usuario: ",user.getRole() );
                if (!user.getRole().equals("Administrador")){
                    isAdmin=false;
                }

                tipousua.setText(user.getRole());

                if (!isAdmin) {
                    //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
                    btnMapa.setVisibility(View.GONE);
                    opc.setVisibility(View.GONE);
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
                    opc.setVisibility(View.VISIBLE);

                    // Toma los usuarios---------------------------------------------

                    final FirebaseDatabase database = FirebaseDatabase.getInstance();
                    DatabaseReference ref = database.getReference("users");
                    ref.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            for (DataSnapshot snap: dataSnapshot.getChildren()) {
                                User usuario= snap.getValue(User.class);
                                if(usuario.getRole().equals("Vendedor")){
                                    vendedorList.add(new Vendedor(snap.getKey(),usuario.getEmail()));
                                }

                            }

                            //CORREGIR ACA... DEBE CONFIRMAR SI ES VEND Y PASAR AL SPINNER
                            if(!vendedorList.isEmpty()) {
                                //spinner

                                spinnerAdapterVend = new SpinnerAdapterVend(Dashboard.this, android.R.layout.simple_spinner_item, vendedorList);
                                spinnerAdapterVend.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                spVen.setAdapter(spinnerAdapterVend);
                                spVen.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                    @Override
                                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                                        if(i!=-1){
                                            vendKey=spinnerAdapterVend.getItem(i).getKey();
                                            recorridoList=new ArrayList<RecowithKey>();
                                            recoKey=null;

                                            DatabaseReference recoRef=database.getReference("recorridos");
                                            recoRef.child(vendKey).addValueEventListener(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(DataSnapshot dataSnapshot) {
                                                    for (DataSnapshot snap: dataSnapshot.getChildren()) {
                                                        Recorrido recorrido= snap.getValue(Recorrido.class);
                                                        recorridoList.add(new RecowithKey(snap.getKey(),recorrido));
                                                    }
                                                    if (!recorridoList.isEmpty()){
                                                        spFecha.setVisibility(View.VISIBLE);
                                                        tvFecha.setVisibility(View.VISIBLE);
                                                        spinnerAdapterReco= new SpinnerAdapterReco(Dashboard.this, android.R.layout.simple_spinner_item, recorridoList);
                                                        spinnerAdapterReco.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                                        spFecha.setAdapter(spinnerAdapterReco);
                                                        spFecha.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                                            @Override
                                                            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                                                                if(i!=-1){
                                                                    recoKey=spinnerAdapterReco.getItem(i).getKey();
                                                                }else{
                                                                    recoKey=null;
                                                                }
                                                            }

                                                            @Override
                                                            public void onNothingSelected(AdapterView<?> adapterView) {

                                                            }
                                                        });

                                                    }else{
                                                        spFecha.setVisibility(View.INVISIBLE);
                                                        tvFecha.setVisibility(View.INVISIBLE);
                                                        Toast.makeText(Dashboard.this,"No Hay Recorridos",Toast.LENGTH_SHORT);
                                                    }
                                                }

                                                @Override
                                                public void onCancelled(DatabaseError databaseError) {

                                                }
                                            });


                                        }else{
                                            spFecha.setVisibility(View.INVISIBLE);
                                            tvFecha.setVisibility(View.INVISIBLE);
                                            vendKey=null;
                                            recoKey=null;
                                        }

                                    }

                                    @Override
                                    public void onNothingSelected(AdapterView<?> adapterView) {
                                        Log.d("SpinnerVend", "onNothingSelected");
                                    }
                                });


                            }else{
                                Toast.makeText(Dashboard.this,"No Hay Vendedores",Toast.LENGTH_SHORT);
                            }

                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            System.out.println("The read failed: " + databaseError.getCode());
                        }
                    });
                    //--------------------------------------------------------------

                }


            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void buildAlertMessageNoGps() {

        final AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(Dashboard.this, R.style.AppTheme_Dialog));
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

            // Bind to the service
            bindService(new Intent(this, LocationUpdaterServices.class), mConnection,
                    Context.BIND_AUTO_CREATE);

            Calendar c = Calendar.getInstance();
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String formattedDate = df.format(c.getTime());
            recorrido= new Recorrido(auth.getCurrentUser().getUid(),formattedDate);
            mDatabase = FirebaseDatabase.getInstance().getReference("recorridos").child(auth.getCurrentUser().getUid());
            RecorridoId= mDatabase.push().getKey();
            mDatabase.child(RecorridoId).setValue(recorrido);
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
            case R.id.dashboard_btn_mapa :
                if(vendKey!=null & recoKey!=null){
                    Intent i = new Intent(Dashboard.this, MapsActivity.class);
                    i.putExtra("VendedorId",vendKey);
                    i.putExtra("RecorridoId",recoKey);
                    startActivity(i);
                    break;
                }else{
                    Toast.makeText(Dashboard.this,"Seleccionar Vendedor y Recorrido",Toast.LENGTH_SHORT);
                }

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

    public void onClickMap(View view) {
        Intent i = new Intent(Dashboard.this,MapsActivity.class);
        startActivity(i);
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


}
