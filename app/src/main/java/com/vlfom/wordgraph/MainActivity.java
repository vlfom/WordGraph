package com.vlfom.wordgraph;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.internal.widget.TintImageView;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.TypefaceSpan;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashSet;

import com.vlfom.wordgraph.CustomTypefaceSpan ;


public class MainActivity extends ActionBarActivity {

    private Integer nodes_counter = 0 ;
    private final int
            node_height = 80,
            small_node_height = 60 ;
    private RelativeLayout mainLayout ;
    private int[] mainDisplacement = new int[2] ;
    private SurfaceView canvasLayout ;
    private SurfaceHolder surfaceHolder ;
    private Canvas surfaceCanvas ;
    private Paint surfacePaint ;
    private ArrayList < FrameLayout >
            Nodes = new ArrayList < FrameLayout > () ;
    private ArrayList < HashSet < Integer > >
            Edges = new ArrayList < HashSet < Integer > > () ;
    private int
            vertexPressed = 0,
            vertexIndexPressed = 0,
            vertexIndexFocused = -1 ;

    private String[] mScreenTitles;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;
    private String
            mDrawerTitle,
            mTitle ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar) ;
        setSupportActionBar(mToolbar) ;

        getSupportActionBar().setTitle(buildStyledString("Word Graph")) ;

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        drawBackground() ;

        mTitle = mDrawerTitle = "Word Graph" ;
        mScreenTitles = getResources().getStringArray(R.array.navbar_string_array);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        mDrawerList.setAdapter(new ArrayAdapter<String>(this,
                R.layout.navbar_list_item, mScreenTitles));
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener()) ;

        getSupportActionBar().setDisplayHomeAsUpEnabled(true) ;
        getSupportActionBar().setHomeButtonEnabled(true) ;

        mDrawerToggle = new ActionBarDrawerToggle(
                this,
                mDrawerLayout,
                R.string.drawer_open,
                R.string.drawer_close
        ) {
            public void onDrawerClosed(View view) {
                getSupportActionBar().setTitle( buildStyledString(mTitle) ) ;
                supportInvalidateOptionsMenu();
            }
            public void onDrawerOpened(View drawerView) {
                getSupportActionBar().setTitle( buildStyledString(mDrawerTitle) ) ;
                supportInvalidateOptionsMenu();
            }
        } ;

        mDrawerLayout.setDrawerListener(mDrawerToggle) ;

        canvasLayout = (SurfaceView) findViewById(R.id.canvas_main) ;
        canvasLayout.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                // Do some drawing when surface is ready
                surfaceHolder = holder ;
                surfaceCanvas = surfaceHolder.lockCanvas() ;
                surfaceCanvas.drawColor(getResources().getColor(R.color.canvas_background)) ;

                surfacePaint = new Paint() ;
                surfacePaint.setAntiAlias(true);
                surfacePaint.setDither(true);
                surfacePaint.setStrokeWidth(5);
                surfacePaint.setColor(Color.rgb(20,45,135)) ;
                surfacePaint.setStyle(Paint.Style.STROKE) ;

                surfaceHolder.unlockCanvasAndPost(surfaceCanvas);
                redrawCanvas() ;
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }
        });

        mainLayout = (RelativeLayout) findViewById(R.id.layout_main) ;

        mainLayout.setOnTouchListener(
                new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        LayoutInflater layoutInflater = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                        final FrameLayout frameLayout = (FrameLayout) layoutInflater.inflate(R.layout.simple_number_vertex, null);

                        final RelativeLayout.LayoutParams mParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, node_height);
                        final Point touchPoint = new Point( (int)event.getX(), (int)event.getY() ) ;
                        mParams.topMargin = (int) event.getY() - node_height + 20;
                        mParams.leftMargin = (int) event.getX() - node_height + 20;

                        frameLayout.setLayoutParams(mParams);

                        mainLayout.getLocationOnScreen(mainDisplacement) ;

                        frameLayout.setOnTouchListener(
                                new View.OnTouchListener() {
                                    private float dx, dy, sX, sY;
                                    private final int NodeIndex = Nodes.size();
                                    private final float BOUNDARY_MOVE = 10 ;
                                    RelativeLayout.LayoutParams layoutParams;
                                    RelativeLayout.LayoutParams mParams ;

                                    @Override
                                    public boolean onTouch(View v, MotionEvent event) {
                                        switch (event.getAction()) {
                                            case MotionEvent.ACTION_DOWN: {
                                                layoutParams = (RelativeLayout.LayoutParams) v.getLayoutParams();
                                                dx = event.getRawX() - layoutParams.leftMargin;
                                                dy = event.getRawY() - layoutParams.topMargin;

                                                if (vertexPressed == 0) {
                                                    sX = event.getRawX();
                                                    sY = event.getRawY();
                                                    handler.postDelayed(checkLongTouch, 500);
                                                    vertexPressed = 1;
                                                }

                                                vertexIndexPressed = NodeIndex;
                                            }
                                            break;
                                            case MotionEvent.ACTION_MOVE: {
                                                if( vertexIndexFocused != -1 )
                                                    unsetFocusedVertex( vertexIndexFocused ) ;

                                                if (vertexPressed == 1 && Math.sqrt(
                                                        Math.pow(event.getRawX() - sX, 2) +
                                                                Math.pow(event.getRawY() - sY, 2)) > BOUNDARY_MOVE) {
                                                    vertexPressed = 0;
                                                    handler.removeCallbacks(checkLongTouch);
                                                } else if (vertexPressed == 2) {
                                                    RelativeLayout.LayoutParams
                                                            mParams = (RelativeLayout.LayoutParams) Nodes.get(NodeIndex).getLayoutParams();
                                                    tempLine = new Pair<>(
                                                            new Point(mParams.leftMargin + Nodes.get(NodeIndex).getWidth() / 2, mParams.topMargin + Nodes.get(NodeIndex).getHeight() / 2),
                                                            new Point((int)event.getRawX()-mainDisplacement[0], (int)event.getRawY()-mainDisplacement[1])
                                                    );

                                                    int foundNode = -1;
                                                    for (int i = 0; i < Nodes.size(); ++i)
                                                        if ( i != NodeIndex && !Edges.get(NodeIndex).contains(i) ) {
                                                            mParams = (RelativeLayout.LayoutParams) Nodes.get(i).getLayoutParams();
                                                            if ( checkNodeIntersection( i, new Point( (int)event.getRawX(), (int)event.getRawY() ) ) ) {
                                                                foundNode = i;
                                                                break;
                                                            }
                                                        }
                                                    if (foundNode != -1) {
                                                        vertexIndexFocused = foundNode ;
                                                        setFocusedVertex( vertexIndexFocused, 2 ) ;
                                                    }
                                                } else {
                                                    float x = event.getRawX(), y = event.getRawY();
                                                    layoutParams.leftMargin = (int) (x - dx);
                                                    layoutParams.topMargin = (int) (y - dy);
                                                    v.setLayoutParams(layoutParams);
                                                }

                                                redrawCanvas();

                                                checkNodesCollision(NodeIndex);
                                            }
                                            break;
                                            case MotionEvent.ACTION_UP: {
                                                if( vertexIndexFocused != -1 )
                                                    unsetFocusedVertex( vertexIndexFocused ) ;

                                                if (vertexPressed == 2) {
                                                    unsetFocusedVertex(vertexIndexPressed);

                                                    int foundNode = -1;
                                                    for (int i = 0; i < Nodes.size(); ++i)
                                                        if (i != NodeIndex && !Edges.get(NodeIndex).contains(i)) {
                                                            mParams = (RelativeLayout.LayoutParams) Nodes.get(i).getLayoutParams();
                                                            if ( checkNodeIntersection( i, new Point( (int)event.getRawX(), (int)event.getRawY() ) ) ) {
                                                                foundNode = i;
                                                                break;
                                                            }
                                                        }
                                                    if (foundNode != -1) {
                                                        mSegments.add(new Pair<>(NodeIndex, foundNode));
                                                        Edges.get(NodeIndex).add(foundNode);
                                                        Edges.get(foundNode).add(NodeIndex);
                                                    }
                                                    tempLine = null;

                                                    redrawCanvas();
                                                }

                                                vertexPressed = 0;
                                                vertexIndexFocused = -1 ;
                                                handler.removeCallbacks(checkLongTouch);
                                            }
                                            break;
                                        }
                                        return true;
                                    }
                                }
                        );

                        final RelativeLayout relativeLayout = new RelativeLayout(MainActivity.this) ;
                        relativeLayout.setLayoutParams( new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT) ) ;
                        relativeLayout.setPadding(10,5,10,5) ;
                        final EditText editText = new EditText(MainActivity.this);
                        editText.setTextColor(Color.BLACK);
                        editText.setLayoutParams( new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT) );
                        relativeLayout.addView(editText) ;

                        final FrameLayout copiedFrameLayout = frameLayout ;
                        final AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this)
                                .setTitle("Enter vertex value:")
                                .setView(relativeLayout)
                                .setNegativeButton("Create text node", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        String enteredText = editText.getText().toString() ;
                                        if( enteredText.equals("") )
                                            enteredText = "......." ;
                                        ((TextView) copiedFrameLayout.findViewById(R.id.vertex_number)).setText(enteredText);

                                        RelativeLayout.LayoutParams mNParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, node_height);
                                        mNParams.topMargin = touchPoint.y - node_height/2;
                                        mNParams.leftMargin = touchPoint.x - (int) copiedFrameLayout.getWidth()/2;

                                        copiedFrameLayout.setLayoutParams(mNParams);

                                        Nodes.add(copiedFrameLayout);
                                        Edges.add(new HashSet<Integer>());

                                        mainLayout.addView(copiedFrameLayout);
                                        redrawCanvas();
                                    }
                                })
                                .setPositiveButton("Create empty node", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        ++nodes_counter ;

                                        ((TextView) copiedFrameLayout.findViewById(R.id.vertex_number)).setText(""+nodes_counter);

                                        RelativeLayout.LayoutParams mNParams = new RelativeLayout.LayoutParams(small_node_height, small_node_height);
                                        mNParams.topMargin = touchPoint.y - small_node_height/2;
                                        mNParams.leftMargin = touchPoint.x - small_node_height/2;

                                        copiedFrameLayout.setLayoutParams(mNParams);

                                        Nodes.add(copiedFrameLayout);
                                        Edges.add(new HashSet<Integer>());

                                        mainLayout.addView(copiedFrameLayout);
                                        redrawCanvas();
                                    }
                                })
                                .show();

                        editText.setFocusableInTouchMode(true);
                        editText.requestFocus() ;
                        editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                            @Override
                            public void onFocusChange(View v, boolean hasFocus) {
                                if (hasFocus) {
                                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                                }
                            }
                        });

                        return false;
                    }
                }
        );

        loadInfo() ;
        saveInfo() ;
    }

    private SpannableStringBuilder buildStyledString( String textValue ) {
        Typeface robotoRegular = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Regular.ttf");
        ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(getResources().getColor(R.color.white));
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(textValue);
        spannableStringBuilder.setSpan(new CustomTypefaceSpan("", robotoRegular), 0, 10, Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
        spannableStringBuilder.setSpan(foregroundColorSpan, 0, 10, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        return spannableStringBuilder ;
    }

    private boolean checkNodeIntersection( int nodeIndex, Point mPoint ) {
        mPoint.x -= mainDisplacement[0] ;
        mPoint.y -= mainDisplacement[1] ;

        RelativeLayout.LayoutParams mParams = (RelativeLayout.LayoutParams) Nodes.get(nodeIndex).getLayoutParams() ;
        Point ellipseCenter = new Point (mParams.leftMargin + Nodes.get(nodeIndex).getWidth()/2, mParams.topMargin + Nodes.get(nodeIndex).getHeight()/2) ;
        int a = Nodes.get(nodeIndex).getWidth()/2, b = Nodes.get(nodeIndex).getHeight()/2 ;
        return Math.pow( (ellipseCenter.x-mPoint.x) * 1. / a, 2 ) + Math.pow( (ellipseCenter.y-mPoint.y) * 1. / b, 2 ) <= 1 ;
    }

    private final Handler handler = new Handler();
    private final Runnable checkLongTouch = new Runnable() {
        public void run() {
            if( vertexPressed == -1 )
                return ;
            vertexPressed = 2 ;

            ((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(100);

            setFocusedVertex( vertexIndexPressed, 1 ) ;
        }
    };

    private void drawBackground() {
        ((SurfaceView) findViewById(R.id.canvas_bg)).getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                // Do some drawing when surface is ready
                Canvas surfaceBgCanvas = holder.lockCanvas() ;
                surfaceBgCanvas.drawColor(getResources().getColor(R.color.canvas_background)) ;

                Paint bgLinePaint = new Paint() ;
                bgLinePaint.setAntiAlias(true);
                bgLinePaint.setDither(true);
                bgLinePaint.setStrokeWidth(5);
                bgLinePaint.setColor(getResources().getColor(R.color.canvas_background_line)) ;
                bgLinePaint.setStyle(Paint.Style.STROKE) ;

                surfaceBgCanvas.drawColor(getResources().getColor(R.color.canvas_background)) ;
                for( int y = -200 ; y <= surfaceBgCanvas.getHeight() ; y += 10 )
                    surfaceBgCanvas.drawLine( 0, y, surfaceBgCanvas.getWidth(), y+200, bgLinePaint ) ;

                holder.unlockCanvasAndPost(surfaceBgCanvas);
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }
        });
    }

    private Pair < Point, Point > tempLine = null ;
    private ArrayList < Pair < Integer, Integer > > mSegments = new ArrayList < Pair < Integer, Integer > > () ;
    private void redrawCanvas() {
        Canvas surfaceCanvas = surfaceHolder.lockCanvas();
        surfaceCanvas.drawColor( 0, PorterDuff.Mode.CLEAR );
        for( Pair < Integer, Integer > mSegment : mSegments ) {
            RelativeLayout.LayoutParams
                    mParams1 = (RelativeLayout.LayoutParams) Nodes.get(mSegment.first).getLayoutParams(),
                    mParams2 = (RelativeLayout.LayoutParams) Nodes.get(mSegment.second).getLayoutParams() ;
            surfaceCanvas.drawLine(
                    mParams1.leftMargin+Nodes.get(mSegment.first).getWidth()/2, mParams1.topMargin+Nodes.get(mSegment.first).getHeight()/2,
                    mParams2.leftMargin+Nodes.get(mSegment.second).getWidth()/2, mParams2.topMargin+Nodes.get(mSegment.second).getHeight()/2,
                    surfacePaint
            );
        }
        if( tempLine != null ) {
            surfaceCanvas.drawLine(tempLine.first.x, tempLine.first.y, tempLine.second.x, tempLine.second.y, surfacePaint) ;
                    Toast.makeText(getApplicationContext(), "" + tempLine.first.x + " " + tempLine.first.y, Toast.LENGTH_SHORT).show();
        }
        surfaceHolder.unlockCanvasAndPost(surfaceCanvas);
    }

    private void setFocusedVertex( int vertexIndex, int type ) {
        if( type == 1 )
            ((ImageView) Nodes.get(vertexIndex).
                    findViewById(R.id.vertex_node)).setImageResource(R.drawable.simple_number_vertex_selected) ;
        else
            ((ImageView) Nodes.get(vertexIndex).
                    findViewById(R.id.vertex_node)).setImageResource(R.drawable.simple_number_vertex_focused) ;
    }
    private void unsetFocusedVertex( int vertexIndex ) {
        ((ImageView) Nodes.get(vertexIndex).
                findViewById(R.id.vertex_node)).setImageResource(R.drawable.simple_number_vertex) ;
    }

    private void checkNodesCollision( final int NodeIndex ) {
        //Is it really necessary?
        /*
        for( FrameLayout Node : Nodes ) {
            if( Node != Nodes.get(NodeIndex) && Math.abs( Node.getX()-Nodes.get(NodeIndex).getX() ) <= node_height ) {

            }
        }
        */
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }


        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_clear) {
            nodes_counter = 0 ;
            for( FrameLayout Node : Nodes )
                mainLayout.removeView( Node ) ;
            Nodes.clear() ;
            Edges.clear() ;
            mSegments.clear() ;
            redrawCanvas() ;
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
        }
    }

    /** Swaps fragments in the main content view */
    private void selectItem(int position) {
        // Highlight the selected item, update the title, and close the drawer
//        mDrawerList.setItemChecked(position, true);
//        setTitle(mScreenTitles[position]);
//        mDrawerLayout.closeDrawer(mDrawerList);
        if( position == 0 ) {
            setDefault() ;
            redrawCanvas() ;
        }
        else if( position == 1 ) {
            loadInfo() ;
            redrawCanvas() ;
        }
        else if( position == 2 ) {
            saveInfo() ;
        }
        else if( position == 3 ) {
            setDefault() ;
            redrawCanvas() ;
        }
    }

    /**
     * When using the ActionBarDrawerToggle, you must call it during
     * onPostCreate() and onConfigurationChanged()...
     */

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggles
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    private void setDefault() {
        nodes_counter = 0 ;
        for( FrameLayout curNode : Nodes )
            mainLayout.removeView( curNode ) ;
        Nodes.clear() ;
        Edges.clear() ;
        mSegments.clear() ;
    }

    private void loadInfo() {
        try {
            File newFolder = new File(Environment.getExternalStorageDirectory(), "WordGraph.files");
            if (!newFolder.exists())
                newFolder.mkdir();
            ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream( newFolder + "/GraphFile.wg" )) ;
            nodes_counter = (Integer) objectInputStream.readObject() ;
            Toast.makeText(getApplicationContext(),"" + nodes_counter, Toast.LENGTH_SHORT).show() ;
            Nodes = (ArrayList) objectInputStream.readObject() ;
            Toast.makeText(getApplicationContext(),"OK0", Toast.LENGTH_SHORT).show() ;
            Edges = (ArrayList) objectInputStream.readObject() ;
            Toast.makeText(getApplicationContext(),"OK1", Toast.LENGTH_SHORT).show() ;
            mSegments = (ArrayList) objectInputStream.readObject() ;
            Toast.makeText(getApplicationContext(),"OK2", Toast.LENGTH_SHORT).show() ;
        } catch (Exception ex) {
            setDefault() ;
        }

        for( FrameLayout Node : Nodes )
            mainLayout.addView( Node ) ;
    }

    private void saveInfo() {
        try {
            File newFolder = new File(Environment.getExternalStorageDirectory(), "WordGraph.files");
            if (!newFolder.exists())
                newFolder.mkdir();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream( newFolder + "/GraphFile.wg" )) ;
            objectOutputStream.writeObject( nodes_counter ) ;
            Toast.makeText(getApplicationContext(),"" + nodes_counter, Toast.LENGTH_SHORT).show() ;
            objectOutputStream.writeObject( Nodes ) ;
            Toast.makeText(getApplicationContext(),"OK0", Toast.LENGTH_SHORT).show() ;
            objectOutputStream.writeObject( Edges ) ;
            Toast.makeText(getApplicationContext(),"OK1", Toast.LENGTH_SHORT).show() ;
            objectOutputStream.writeObject( mSegments ) ;
            Toast.makeText(getApplicationContext(),"OK2", Toast.LENGTH_SHORT).show() ;
            objectOutputStream.flush();
        } catch (Exception ex) {
        }
    }
}
