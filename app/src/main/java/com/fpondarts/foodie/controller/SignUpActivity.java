package com.fpondarts.foodie.controller;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.fpondarts.foodie.R;
import com.fpondarts.foodie.services.CheckEmailService;
import com.fpondarts.foodie.services.RetrofitClientInstance;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignUpActivity extends AppCompatActivity {

    private static final int GOOGLE_SIGN_UP = 1;

    private Button mSignUpButton;
    private SignInButton mGoogleSignInButton;
    private Button mToSignInButton;


    GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_register);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this,gso);
        mSignUpButton = (Button) findViewById(R.id.signUpButton);
        mSignUpButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Toast.makeText(SignUpActivity.this,"SignUp",Toast.LENGTH_LONG).show();
            }
        });

        mGoogleSignInButton = findViewById(R.id.googleSignInButton);
        mGoogleSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                googleSignIn();
            }
        });
        mToSignInButton = (Button) findViewById(R.id.toSignInButton);
        mToSignInButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                finish();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if (requestCode == GOOGLE_SIGN_UP){
            if(resultCode==FirebaseAuthUI.SIGN_UP_OK){

                CheckEmailService service = RetrofitClientInstance.getRetrofitInstance().create(CheckEmailService.class);
                FirebaseUser account = FirebaseAuth.getInstance().getCurrentUser();
                final String name = account.getDisplayName();
                final String email = account.getEmail();
                final Uri photoUrl = account.getPhotoUrl();


                Call<Void> call = service.checkEmailIsAvailable(account.getEmail());
                call.enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {

                        if (response.code() == 404) {
                            Intent intent = new Intent(SignUpActivity.this, UserRegisterFirstInput.class);
                            intent.putExtra("name", name);
                            intent.putExtra("email", email);
                            if (photoUrl!=null) {
                                intent.putExtra("photoUrl", photoUrl.toString());
                            }
                            startActivity(intent);

                        } else {
                            Toast.makeText(SignUpActivity.this,"Ya existe un usuario asociado a esa cuenta\nInicie sesión.",Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t){
                        Toast.makeText(SignUpActivity.this,"Problemas de conexion con el servidor",Toast.LENGTH_LONG).show();
                    }
                });

            } else {
               Toast.makeText(SignUpActivity.this,"Hubo un error en el ingreso",Toast.LENGTH_LONG).show();
            }
        }

    }

    private void googleSignIn() {
        Intent intent = new Intent(SignUpActivity.this,FirebaseAuthUI.class);
        startActivityForResult(intent,GOOGLE_SIGN_UP);
    }
}
