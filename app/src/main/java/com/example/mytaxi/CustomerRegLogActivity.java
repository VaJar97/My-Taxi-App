package com.example.mytaxi;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

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

public class CustomerRegLogActivity extends AppCompatActivity {

    TextView customerEnter, notAccount;
    EditText customerEmail, customerPassword;
    Button logInCustomer, signInCustomer;

    FirebaseAuth mAuth;
    ProgressDialog loading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_reg_log);

        customerEnter = (TextView) findViewById(R.id.customerEnter);
        notAccount = (TextView) findViewById(R.id.notAccount);
        customerEmail = (EditText) findViewById(R.id.customerEmail);
        customerPassword = (EditText) findViewById(R.id.customerPassword);
        logInCustomer = (Button) findViewById(R.id.logInCustomer);
        signInCustomer = (Button) findViewById(R.id.signInCustomer);

        signInCustomer.setVisibility(View.INVISIBLE);
        signInCustomer.setEnabled(false);

        notAccount.setOnClickListener( new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                logInCustomer.setVisibility(View.INVISIBLE);
                notAccount.setVisibility(View.INVISIBLE);
                signInCustomer.setVisibility(View.VISIBLE);
                signInCustomer.setEnabled(true);
                customerEnter.setText("Sign In for customers");
            }
        });

        signInCustomer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = customerEmail.getText().toString();
                String password = customerPassword.getText().toString();

                registerCustomer(email, password);
            }
        });
    }

    private void registerCustomer(String email, String password) {

        loading = new ProgressDialog(this);
        loading.setTitle("Sign-in customer");
        loading.setMessage("Please, wait to complete sign-in");
        loading.show();
        mAuth = FirebaseAuth.getInstance();
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(CustomerRegLogActivity.this, "Sign-in was successful", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(CustomerRegLogActivity.this, "Error", Toast.LENGTH_SHORT).show();
                }
                loading.dismiss();
            }
        });
    }
}