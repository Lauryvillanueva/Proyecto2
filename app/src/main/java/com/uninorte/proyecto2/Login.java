package com.uninorte.proyecto2;


import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;

public class Login extends AppCompatActivity implements View.OnClickListener{


    private static final String TAG = "LoginActivity";
    private Button btnLogin;
    private EditText input_email,input_password;
    private TextView btnForgotPass;
    private Context context;
    private DatabaseReference mDatabase;
    //Intent intent;
    private ScrollView activity_main;
    private ProgressDialog progressDialog;
    private FirebaseAuth auth;

    //base de datos Firebase
    private DatabaseReference ref = FirebaseDatabase.getInstance().getReference();
    private DatabaseReference user = ref.child("users");

    SignInButton loginbtn;
    private static final int RC_SIGN_IN = 0;
    private GoogleApiClient mGoogleApiClient;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //-------------------------------------------------------------------
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Login.this,Principal.class));
                finish();
                overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
            }
        });
        //----------------------------------------------------------------------
        context=getApplicationContext();
        FirebaseApp.initializeApp(context);

        //FirebaseApp.initializeApp(this);


        //progress
        progressDialog = new ProgressDialog(this, R.style.AppTheme_Dark_Dialog);

        //View
        btnLogin = (Button)findViewById(R.id.login_btn_login);
        input_email = (EditText)findViewById(R.id.login_email);
        input_password = (EditText)findViewById(R.id.login_password);
        btnForgotPass = (TextView)findViewById(R.id.login_btn_forgot_password);
        String email = input_email.getText().toString();
        String password = input_password.getText().toString();
        btnForgotPass.setOnClickListener(this);
        btnLogin.setOnClickListener(this);

        //Init Firebase Auth
        auth = FirebaseAuth.getInstance();

        //Check already session , if ok-> DashBoard
        if(auth.getCurrentUser() != null) {
            startActivity(new Intent(Login.this, Dashboard.class));
        }

        // Google
        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener(){

            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {

                FirebaseUser user = firebaseAuth.getCurrentUser();





                if (firebaseAuth.getCurrentUser()!=null){
                   // Toast.makeText(getApplicationContext(), "usuario "+user.getPhotoUrl(),Toast.LENGTH_SHORT).show();
                    User userG=new User("Vendedor",firebaseAuth.getCurrentUser().getEmail());
                    FirebaseDatabase.getInstance().getReference().child("users").child(firebaseAuth.getCurrentUser().getUid()).setValue(userG);
                    Intent intent = new Intent(Login.this, Dashboard.class);
                    startActivity(intent);
                }

            }
        };

        loginbtn = (SignInButton) findViewById(R.id.loginbtn);

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

                        Toast.makeText(getApplicationContext(), connectionResult.getErrorMessage(), Toast.LENGTH_SHORT)
                                .show();

                    }
                }).addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        loginbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                signIn();

            }
        });



    }
    //google
    @Override
    protected void onStart() {
        super.onStart();

        mAuth.addAuthStateListener(mAuthListener);
    }

    private void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = result.getSignInAccount();

                firebaseAuthWithGoogle(account);
            } else {
                // Google Sign In failed, update UI appropriately
                // ...
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.getId());

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "signInWithCredential:onComplete:" + task.isSuccessful());

                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "signInWithCredential", task.getException());
                            Toast.makeText(Login.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                        // ...
                    }
                });
    }
    //----------------------------------

    @Override
    public void onClick(View view) {
        if(view.getId() == R.id.login_btn_forgot_password)
        {
            startActivity(new Intent(Login.this,ForgotPassword.class));
            finish();
            overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
        }
        else if(view.getId() == R.id.login_btn_login )
        {
            loginUser(input_email.getText().toString(),input_password.getText().toString());
            overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
        }
    }

    private void loginUser(final String email, final String password) {



        if(TextUtils.isEmpty(email)|| TextUtils.isEmpty(password)){
            Toast.makeText(Login.this,"Hay campos vacios",Toast.LENGTH_SHORT).show();
            //  input_email.setError("No puede estar vacio");
            //input_password.setError("No Puede Estar Vacio");

        }else{

            progressDialog.setIndeterminate(true);
            progressDialog.setMessage("Autenticando...");
            progressDialog.show();

                auth.signInWithEmailAndPassword(email,password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        progressDialog.dismiss();
                        if(!task.isSuccessful())
                        {
                            if(password.length() < 6)
                            {
                                Toast.makeText(Login.this,"La contraseña debe tener mas de 6 caracteres",Toast.LENGTH_SHORT).show();


                            }else  if (!isValidEmail(email)) {
                                Toast.makeText(Login.this,"Email no valido",Toast.LENGTH_SHORT).show();

                            }else {
                                Toast.makeText(Login.this,"Email o contraseña incorrecta! Verifiquelo e intente de nuevo",Toast.LENGTH_SHORT).show();

                            }
                        }
                        else{

                            Intent intent = new Intent(Login.this,Dashboard.class);
                            startActivity(intent);
                            finish();



                        }
                    }
                });
    }}

    // validating email iD
    private boolean isValidEmail(String email) {
        String EMAIL_PATTERN = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
                + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

        Pattern pattern = Pattern.compile(EMAIL_PATTERN);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }



}
