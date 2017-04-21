package com.example.ss;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /***ON BUTTON CLICK -- TELEPHONY MANAGER***/
        Button Tel = (Button) findViewById(R.id.Tel_Loc_Mngr);
        Tel.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent Tel_Intent = new Intent(MainActivity.this, Telephony_Manager_screen.class);
                startActivity(Tel_Intent);
            }
        });
    }
}