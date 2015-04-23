package com.vlfom.wordgraph;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorAdapter;

public class Menu_FragmentList extends Fragment {
    private int
            screenHeight,
            screenWidth ;
    View popupListView; PopupWindow popupWindow ;
    Fragment thisFragment ;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        screenWidth = displayMetrics.widthPixels;
        screenHeight = displayMetrics.heightPixels;

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

        thisFragment = this ;
        ((ListView) popupListView.findViewById(R.id.fileopen_popuplist)).setAdapter(adapter) ;
        ((ListView) popupListView.findViewById(R.id.fileopen_popuplist)).setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view,
                                            int position, long id) {
                        Cursor cursor = (Cursor) parent.getItemAtPosition(position);
                        ((DataReceiver) getActivity()).receiveFileName(
                                cursor.getString(cursor.getColumnIndexOrThrow(FileList_Provider.FILE_NAME))
                        );
                        getActivity().getFragmentManager().beginTransaction().remove(thisFragment).commit();
                    }
                }
        );
        ((ListView) popupListView.findViewById(R.id.fileopen_popuplist)).setOnItemLongClickListener(
                new AdapterView.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                        final Cursor cursor = (Cursor) parent.getItemAtPosition(position);
                        new AlertDialog.Builder(getActivity())
                                .setTitle("Are you sure want to delete this file?")
                                .setNegativeButton("Delete", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        getActivity().getContentResolver().delete(
                                                FileList_Provider.FILELIST_URI,
                                                FileList_Provider.FILE_FULL + "=?",
                                                new String[]{
                                                        cursor.getString(cursor.getColumnIndexOrThrow(FileList_Provider.FILE_FULL))
                                                });
                                    }
                                })
                                .setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                    }
                                })
                                .show();
                        return true ;
                    }
                }
        );

        popupListView.findViewById(R.id.onModeCancelFile).setOnTouchListener(new myTouchListener(this));

        return popupListView ;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    private class myTouchListener implements View.OnTouchListener {
        private Fragment parentFragment ;

        public myTouchListener(Fragment fragment) {
            parentFragment = fragment ;
        }

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            ((DataReceiver) getActivity()).receiveFileName(null);
            getActivity().getFragmentManager().beginTransaction().remove(parentFragment).commit();
            return true ;
        }
    }
}