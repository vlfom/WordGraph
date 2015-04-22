package com.vlfom.wordgraph;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

public class Menu_FragmentList extends Fragment {
    private int
            screenHeight,
            screenWidth ;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_menulist,
                container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        screenWidth = displayMetrics.widthPixels;
        screenHeight = displayMetrics.heightPixels;

        getActivity().findViewById(R.id.btnNew).setOnTouchListener(new myTouchListener());
        getActivity().findViewById(R.id.btnOpen).setOnTouchListener(new myTouchListener());
        getActivity().findViewById(R.id.btnAbout).setOnTouchListener(new myTouchListener());

        prepareFileList();
    }

    View popupListView; PopupWindow popupWindow ;

    private void prepareFileList() {
        popupListView = ((LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).
                inflate(R.layout.fileopen_layout, ((RelativeLayout) getActivity().findViewById(R.id.fileopen_layout))) ;
        popupWindow = new PopupWindow(popupListView) ;

        Cursor cursor = getActivity().getContentResolver().query(FileList_Provider.FILELIST_URI, null, null,
                null, null);
        getActivity().startManagingCursor(cursor);

        String from[] = {
                FileList_Provider.FILE_NAME,
                FileList_Provider.FILE_FULL
        };
        int to[] = { android.R.id.text1, android.R.id.text2 };
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(getActivity(),
                android.R.layout.simple_list_item_1, cursor, from, to);

        ((ListView) popupListView.findViewById(R.id.fileopen_popuplist)).setAdapter(adapter) ;
        ((ListView) popupListView.findViewById(R.id.fileopen_popuplist)).setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> listView, View view,
                                            int position, long id) {
                        Cursor cursor = (Cursor) listView.getItemAtPosition(position);

                        String something =
                                cursor.getString(cursor.getColumnIndexOrThrow(FileList_Provider.FILE_FULL));
                        Toast.makeText(getActivity().getApplicationContext(),
                                something, Toast.LENGTH_SHORT).show();

                    }
                }
        );
    }

    boolean popupVisible = false ;
    private class myTouchListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View view, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (view.getId() == R.id.btnNew)
                    startActivity(new Intent(getActivity(), Main_Activity.class));
                else if (view.getId() == R.id.btnOpen) {
                    if( !popupVisible ) {
                        popupWindow.showAtLocation(popupListView, Gravity.TOP, 0, (int) Math.round(screenHeight * 0.025));
                        popupWindow.update((int) Math.round(screenWidth * 0.95), (int) Math.round(screenHeight * 0.95));
                    }
                    else
                        popupWindow.dismiss() ;
                    popupVisible = !popupVisible ;
                    startActivity(new Intent(getActivity(), About_Activity.class));
                }
                else if (view.getId() == R.id.btnAbout)
                    startActivity(new Intent(getActivity(), About_Activity.class));
            }
            return true;
        }
    }
}