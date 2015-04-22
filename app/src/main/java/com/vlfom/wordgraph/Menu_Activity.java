package com.vlfom.wordgraph;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

public class Menu_Activity extends Activity implements DataReceiver {

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        findViewById(R.id.btnNew).setOnTouchListener(new myTouchListener());
        findViewById(R.id.btnOpen).setOnTouchListener(new myTouchListener());
        findViewById(R.id.btnAbout).setOnTouchListener(new myTouchListener());

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    private class myTouchListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View view, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (view.getId() == R.id.btnNew) {
                    Intent intent = new Intent(Menu_Activity.this, Main_Activity.class) ;
                    intent.putExtra("file", "null") ;
                    startActivity(intent);
                }
                else if (view.getId() == R.id.btnOpen) {
                    FragmentManager fragmentManager = getFragmentManager();
                    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                    Fragment fragment = new Menu_FragmentList() ;
                    fragmentTransaction.add(R.id.menu_layout, fragment);

                    fragmentTransaction.commit();
                }
                else if (view.getId() == R.id.btnAbout)
                    startActivity(new Intent(Menu_Activity.this, About_Activity.class));
            }
            return true ;
        }
    }

    @Override
    public void receiveFileName( String name ) {
        if( name == null )
            return ;
        Intent intent = new Intent(Menu_Activity.this, Main_Activity.class) ;
        intent.putExtra("file", name) ;
        startActivity(intent);
    }
}
