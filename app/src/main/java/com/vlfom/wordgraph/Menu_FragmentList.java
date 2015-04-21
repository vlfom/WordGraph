package com.vlfom.wordgraph;

import android.app.Fragment;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class Menu_FragmentList extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        getActivity().findViewById(R.id.btnNew).setOnTouchListener(new myTouchListener()) ;
        getActivity().findViewById(R.id.btnOpen).setOnTouchListener( new myTouchListener() ) ;
        getActivity().findViewById(R.id.btnAbout).setOnTouchListener( new myTouchListener() ) ;
        return inflater.inflate(R.layout.fragment_menulist,
                container, false);
    }

    private class myTouchListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View view, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (view.getId() == R.id.btnNew)
                    startActivity(new Intent(getActivity(), MainActivity.class)) ;
                else if (view.getId() == R.id.btnOpen)
                    startActivity(new Intent(getActivity(), AboutActivity.class)) ;
                else if (view.getId() == R.id.btnAbout)
                    startActivity(new Intent(getActivity(), AboutActivity.class)) ;
            }
            return true ;
        }
    }
}