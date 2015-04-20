package com.vlfom.wordgraph;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;

public class MenuActivity extends Activity {

    private class myTouchListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View view, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (view.getId() == R.id.btnNew)
                    startActivity(new Intent(MenuActivity.this, AboutActivity.class)) ;
                else if (view.getId() == R.id.btnOpen)
                    startActivity(new Intent(MenuActivity.this, AboutActivity.class)) ;
                else if (view.getId() == R.id.btnAbout)
                    startActivity(new Intent(MenuActivity.this, AboutActivity.class)) ;
            }
            return true ;
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState ) ;
        setContentView( R.layout.activity_main_menu ) ;

        ((Button) findViewById(R.id.btnNew)).setOnTouchListener( new myTouchListener() ) ;
        ((Button) findViewById(R.id.btnOpen)).setOnTouchListener( new myTouchListener() ) ;
        ((Button) findViewById(R.id.btnAbout)).setOnTouchListener( new myTouchListener() ) ;

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

}
