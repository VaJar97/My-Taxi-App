package com.example.mytaxi;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class DriverRegLogActivity extends AppCompatActivity {

    TextView driverEnter, notAccount;
    EditText driverEmail, driverPassword;
    Button logInDriver, signInDriver;

    FirebaseAuth mAuth;
    ProgressDialog loading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_reg_log);

        driverEnter = (TextView) findViewById(R.id.driverEnter);
        notAccount = (TextView) findViewById(R.id.notAccount);
        driverEmail = (EditText) findViewById(R.id.driverEmail);
        driverPassword = (EditText) findViewById(R.id.driverPassword);
        logInDriver = (Button) findViewById(R.id.logInDriver);
        signInDriver = (Button) findViewById(R.id.signInDriver);

        signInDriver.setVisibility(View.INVISIBLE);
        signInDriver.setEnabled(false);

        notAccount.setOnClickListener( new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                logInDriver.setVisibility(View.INVISIBLE);
                notAccount.setVisibility(View.INVISIBLE);
                signInDriver.setVisibility(View.VISIBLE);
                signInDriver.setEnabled(true);
                driverEnter.setText("Sign In for drivers");
            }
        });

        signInDriver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = driverEmail.getText().toString();
                String password = driverPassword.getText().toString();

                registerDriver(email, password);
            }
        });
    }

    private void registerDriver(String email, String password) {

        loading = new ProgressDialog(this);
        loading.setTitle("Sign-in driver");
        loading.setMessage("Please, wait to complete sign-in");
        loading.show();
        mAuth = FirebaseAuth.getInstance();
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(DriverRegLogActivity.this, "Sign-in was successful", Toast.LENGTH_SHORT).show();
                    loading.dismiss();
                } else {
                    Toast.makeText(DriverRegLogActivity.this, "Error", Toast.LENGTH_SHORT).show();
                    loading.dismiss();
                }
            }
        });
    }
}