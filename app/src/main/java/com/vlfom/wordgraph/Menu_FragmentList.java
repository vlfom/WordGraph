package com.vlfom.wordgraph;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

public class Menu_FragmentList extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_menulist,
                container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getActivity().findViewById(R.id.btnNew).setOnTouchListener(new myTouchListener());
        getActivity().findViewById(R.id.btnOpen).setOnTouchListener(new myTouchListener());
        getActivity().findViewById(R.id.btnAbout).setOnTouchListener(new myTouchListener());
    }

    private class myTouchListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View view, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (view.getId() == R.id.btnNew)
                    startActivity(new Intent(getActivity(), Main_Activity.class));
                else if (view.getId() == R.id.btnOpen)
                    startActivity(new Intent(getActivity(), About_Activity.class));
                else if (view.getId() == R.id.btnAbout)
                    startActivity(new Intent(getActivity(), About_Activity.class));
            }
            return true;
        }
    }
}