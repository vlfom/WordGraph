package com.vlfom.wordgraph;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
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

public class Main_Activity extends ActionBarActivity implements DataReceiver {
    private String fileName ;
    final Integer
            NUMBER_NODE_TYPE = 1,
            TEXT_NODE_TYPE = 2;
    final Integer
            MODE_NONE = 0,
            MODE_CREATE = 1,
            MODE_CONNECT = 2,
            MODE_DELETE = 3;
    private int currentMode = MODE_NONE;
    private final int
            node_diameter = 60,
            small_node_diameter = 40;
    private final Handler handler = new Handler();
    private Integer number_nodes_counter = 0;
    private RelativeLayout mainLayout;
    private int[] mainDisplacement = new int[2];
    private ImageView canvasLayout;
    private Bitmap surfaceBitmap;
    private Canvas surfaceCanvas;
    private Paint surfacePaint;
    private ArrayList<FrameLayout>
            Nodes = new ArrayList<>();
    private ArrayList<ArrayList<Integer>>
            Edges = new ArrayList<>();
    private ArrayList<Pair<String, Integer>>
            NodesInfo = new ArrayList<>();
    private ArrayList<Point>
            NodesPos = new ArrayList<>();
    private int
            vertexPressed = 0,
            vertexTypePressed = 0,
            vertexIndexPressed = 0,
            vertexTypeFocused = 0,
            vertexIndexFocused = 0;
    private final Runnable checkLongTouch = new Runnable() {
        public void run() {
            if (vertexPressed == -1)
                return;
            vertexPressed = 2;

            ((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(100);

            setFocusedVertex(vertexIndexPressed, vertexTypePressed, 1);
        }
    };
    private ActionBarDrawerToggle mDrawerToggle;
    private Pair<Point, Point> tempLine = null;
    private ArrayList<Pair<Integer, Integer>> mSegments = new ArrayList<>();

    DrawerLayout mDrawerLayout ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ((TextView) findViewById(R.id.navbarTitle)).setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Roboto-Regular.ttf"));
        ((TextView) findViewById(R.id.navbarTitle)).setShadowLayer(2, 0, 1, Color.BLACK);
        ((TextView) findViewById(R.id.actionbarTitle)).setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Roboto-Regular.ttf"));
        ((TextView) findViewById(R.id.actionbarTitle)).setShadowLayer(2, 0, 1, Color.BLACK);

        findViewById(R.id.onModeCreate).setOnTouchListener(
                new ChangeModeListener()
        );
        findViewById(R.id.onModeDelete).setOnTouchListener(
                new ChangeModeListener()
        );
        findViewById(R.id.onModeCancel).setOnTouchListener(
                new ChangeModeListener()
        );
        findViewById(R.id.btns_cancel).setVisibility(View.GONE);

        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        getSupportActionBar().setTitle("");

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        drawBackground();

        String[] mScreenTitles = getResources().getStringArray(R.array.navbar_string_array);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        ListView mDrawerList = (ListView) findViewById(R.id.left_drawer);

        mDrawerList.setAdapter(new ArrayAdapter<>(this,
                R.layout.navbar_list_item, mScreenTitles));
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        mDrawerToggle = new ActionBarDrawerToggle(
                this,
                mDrawerLayout,
                R.string.drawer_open,
                R.string.drawer_close
        ) {
            public void onDrawerClosed(View view) {
                supportInvalidateOptionsMenu();
            }

            public void onDrawerOpened(View drawerView) {
                supportInvalidateOptionsMenu();
            }
        };

        mDrawerLayout.setDrawerListener(mDrawerToggle);

        canvasLayout = (ImageView) findViewById(R.id.canvas_main);

        surfacePaint = new Paint();
        surfacePaint.setAntiAlias(true);
        surfacePaint.setDither(true);
        surfacePaint.setStrokeWidth(3);
        surfacePaint.setColor(Color.rgb(20, 45, 135));
        surfacePaint.setStyle(Paint.Style.STROKE);

        initCanvas();

        mainLayout = (RelativeLayout) findViewById(R.id.layout_main);

        mainLayout.setOnTouchListener(
                new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        float x = event.getRawX(), y = event.getRawY();
                        if ((int) y - node_diameter < getSupportActionBar().getHeight() ||
                                (int) y + node_diameter +
                                        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 75, getResources().getDisplayMetrics()) >
                                        getWindowManager().getDefaultDisplay().getHeight())
                            return false;
                        if ((int) x - node_diameter < 0 ||
                                (int) x + node_diameter > getWindowManager().getDefaultDisplay().getWidth())
                            return false;

                        if (currentMode == MODE_CREATE) {
                            final Point touchPoint = new Point((int) event.getX(), (int) event.getY());
                            mainLayout.getLocationOnScreen(mainDisplacement);

                            final RelativeLayout relativeLayout = new RelativeLayout(Main_Activity.this);
                            relativeLayout.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                            relativeLayout.setPadding(10, 5, 10, 5);
                            final EditText editText = new EditText(Main_Activity.this);
                            editText.setTextColor(Color.WHITE);
                            editText.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                            relativeLayout.addView(editText);

                            new AlertDialog.Builder(Main_Activity.this)
                                    .setTitle("Enter vertex value:")
                                    .setView(relativeLayout)
                                    .setNegativeButton("Create text node", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            String enteredText = editText.getText().toString();
                                            if (enteredText.equals(""))
                                                enteredText = ".......";

                                            addNewNode(touchPoint, enteredText, TEXT_NODE_TYPE, false);
                                        }
                                    })
                                    .setPositiveButton("Create empty node", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            ++number_nodes_counter;
                                            addNewNode(touchPoint, number_nodes_counter.toString(), NUMBER_NODE_TYPE, false);
                                        }
                                    })
                                    .show();
                        }
                        return false;
                    }
                }
        );

        findViewById(R.id.navigationDrawer).setAlpha(0.8f);

        String receiveName = getIntent().getStringExtra("file") ;
        if( receiveName.equals("null") )
            fileName = null ;
        else {
            fileName = receiveName;
            loadFile(fileName);
            (new Handler()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    redrawCanvas();
                }
            }, 1);
        }
    }

    private void addNewNode(Point nodePos, String nodeText, int nodeType, boolean addType) {
        LayoutInflater layoutInflater = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final FrameLayout frameLayout;

        if (nodeType == NUMBER_NODE_TYPE)
            frameLayout = (FrameLayout) layoutInflater.inflate(R.layout.number_vertex, null);
        else if (nodeType == TEXT_NODE_TYPE)
            frameLayout = (FrameLayout) layoutInflater.inflate(R.layout.text_vertex, null);
        else
            frameLayout = (FrameLayout) layoutInflater.inflate(R.layout.text_vertex, null);

        frameLayout.setOnTouchListener(
                new View.OnTouchListener() {
                    private final FrameLayout thisLayout = frameLayout ;
                    private final float BOUNDARY_MOVE = 10;
                    RelativeLayout.LayoutParams layoutParams;
                    RelativeLayout.LayoutParams mParams;
                    private float dx
                            ,
                            dy
                            ,
                            sX
                            ,
                            sY;

                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        int NodeIndex = Nodes.indexOf(thisLayout) ;
                        if( currentMode == MODE_NONE ) {
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
                                    vertexTypePressed = NodesInfo.get(NodeIndex).second;
                                }
                                break;
                                case MotionEvent.ACTION_MOVE: {
                                    if (vertexIndexFocused != -1)
                                        unsetFocusedVertex(vertexIndexFocused, vertexTypeFocused);

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
                                                new Point((int) event.getRawX() - mainDisplacement[0], (int) event.getRawY() - mainDisplacement[1])
                                        );

                                        int foundNode = -1;
                                        for (int i = 0; i < Nodes.size(); ++i)
                                            if (i != NodeIndex && !Edges.get(NodeIndex).contains(i)) {
                                                if (checkNodeIntersection(i, new Point((int) event.getRawX(), (int) event.getRawY()))) {
                                                    foundNode = i;
                                                    break;
                                                }
                                            }
                                        if (foundNode != -1) {
                                            vertexIndexFocused = foundNode;
                                            vertexTypeFocused = NodesInfo.get(foundNode).second;
                                            setFocusedVertex(vertexIndexFocused, vertexTypeFocused, 2);
                                        }
                                    } else {
                                        float x = event.getRawX(), y = event.getRawY();
                                        if ((int) (y - dy) >= getSupportActionBar().getHeight() && (int) (y - dy) + v.getHeight() + TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 75, getResources().getDisplayMetrics()) <= getWindowManager().getDefaultDisplay().getHeight())
                                            layoutParams.topMargin = (int) (y - dy);
                                        if ((int) (x - dx) >= 0 && (int) (x - dx) + v.getWidth() <= getWindowManager().getDefaultDisplay().getWidth())
                                            layoutParams.leftMargin = (int) (x - dx);
                                        v.setLayoutParams(layoutParams);

                                        NodesPos.set(NodeIndex, new Point((int) (x - dx), (int) (y - dy)));
                                    }

                                    redrawCanvas();
                                }
                                break;
                                case MotionEvent.ACTION_UP: {
                                    if (vertexIndexFocused != -1)
                                        unsetFocusedVertex(vertexIndexFocused, vertexTypeFocused);
                                    if (vertexPressed == 2) {
                                        unsetFocusedVertex(vertexIndexPressed, vertexTypePressed);

                                        int foundNode = -1;
                                        for (int i = 0; i < Nodes.size(); ++i)
                                            if (i != NodeIndex && !Edges.get(NodeIndex).contains(i)) {
                                                mParams = (RelativeLayout.LayoutParams) Nodes.get(i).getLayoutParams();
                                                if (checkNodeIntersection(i, new Point((int) event.getRawX(), (int) event.getRawY()))) {
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
                                    vertexIndexFocused = -1;
                                    handler.removeCallbacks(checkLongTouch);
                                }
                                break;
                            }
                        }
                        else if( currentMode == MODE_DELETE ) {
                            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                                NodesInfo.remove(NodeIndex) ;
                                NodesPos.remove(NodeIndex) ;
                                for( int i = 0 ; i < number_nodes_counter ; ++i )
                                    for( int j = Edges.get(i).size()-1 ; j >= 0 ; --j )
                                        if( Edges.get(i).get(j) == NodeIndex )
                                            Edges.get(i).remove(j) ;
                                        else if( Edges.get(i).get(j) > NodeIndex )
                                            Edges.get(i).set( j, Edges.get(i).get(j)-1 ) ;
                                Edges.remove(NodeIndex) ;
                                for( int i = mSegments.size()-1 ; i >= 0 ; --i ) {
                                    if (mSegments.get(i).first == NodeIndex) {
                                        mSegments.remove(i);
                                        continue;
                                    }
                                    if (mSegments.get(i).second == NodeIndex) {
                                        mSegments.remove(i);
                                        continue;
                                    }
                                    if( mSegments.get(i).first > NodeIndex )
                                        mSegments.set(i,
                                                new Pair<>(mSegments.get(i).first-1,mSegments.get(i).second)
                                        ) ;
                                    if( mSegments.get(i).second > NodeIndex )
                                        mSegments.set(i,
                                                new Pair<>(mSegments.get(i).first, mSegments.get(i).second-1)
                                        ) ;
                                }

                                --number_nodes_counter ;

                                Nodes.remove(thisLayout);
                                mainLayout.removeView(thisLayout);

                                (new Handler()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        redrawCanvas();
                                    }
                                }, 1);
                            }
                        }
                        return true;
                    }
                }
        );

        ((TextView) frameLayout.findViewById(R.id.vertex_number)).setText(nodeText);

        RelativeLayout.LayoutParams mNParams;
        if (nodeType == TEXT_NODE_TYPE) {
            mNParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, node_diameter);
            frameLayout.setMinimumWidth(node_diameter);
            if (!addType) {
                mNParams.leftMargin = nodePos.x - frameLayout.getWidth() / 2;
                mNParams.topMargin = nodePos.y - node_diameter / 2;
            } else {
                mNParams.leftMargin = nodePos.x;
                mNParams.topMargin = nodePos.y;
            }
        } else if (nodeType == NUMBER_NODE_TYPE) {
            mNParams = new RelativeLayout.LayoutParams(small_node_diameter, small_node_diameter);
            if (!addType) {
                mNParams.leftMargin = nodePos.x - small_node_diameter / 2;
                mNParams.topMargin = nodePos.y - small_node_diameter / 2;
            } else {
                mNParams.leftMargin = nodePos.x;
                mNParams.topMargin = nodePos.y;
            }
        } else
            mNParams = new RelativeLayout.LayoutParams(small_node_diameter, small_node_diameter);
        frameLayout.setLayoutParams(mNParams);

        Nodes.add(frameLayout);
        mainLayout.addView(frameLayout);

        if (!addType) {
            NodesInfo.add(new Pair<>(nodeText, nodeType));
            NodesPos.add(new Point(mNParams.leftMargin, mNParams.topMargin));
            Edges.add(new ArrayList<Integer>());
            redrawCanvas();
        }
    }

    private boolean checkNodeIntersection(int nodeIndex, Point mPoint) {
        mPoint.x -= mainDisplacement[0];
        mPoint.y -= mainDisplacement[1];

        RelativeLayout.LayoutParams mParams = (RelativeLayout.LayoutParams) Nodes.get(nodeIndex).getLayoutParams();
        Point ellipseCenter = new Point(mParams.leftMargin + Nodes.get(nodeIndex).getWidth() / 2, mParams.topMargin + Nodes.get(nodeIndex).getHeight() / 2);
        int a = Nodes.get(nodeIndex).getWidth() / 2, b = Nodes.get(nodeIndex).getHeight() / 2;
        return Math.pow((ellipseCenter.x - mPoint.x) * 1. / a, 2) + Math.pow((ellipseCenter.y - mPoint.y) * 1. / b, 2) <= 1;
    }

    private void drawBackground() {
        ImageView imageView = (ImageView) findViewById(R.id.canvas_bg);
        Bitmap tempBitmap = Bitmap.createBitmap(getWindowManager().getDefaultDisplay().getWidth(), getWindowManager().getDefaultDisplay().getHeight(), Bitmap.Config.RGB_565);

        Paint bgLinePaint = new Paint();
        bgLinePaint.setAntiAlias(true);
        bgLinePaint.setDither(true);
        bgLinePaint.setStrokeWidth(5);
        bgLinePaint.setColor(getResources().getColor(R.color.canvas_background_line));
        bgLinePaint.setStyle(Paint.Style.STROKE);

        Canvas bgCanvas = new Canvas(tempBitmap);
        bgCanvas.drawColor(getResources().getColor(R.color.canvas_background));
        for (int y = -400; y <= bgCanvas.getHeight(); y += 10)
            bgCanvas.drawLine(0, y, bgCanvas.getWidth(), y + 400, bgLinePaint);

        imageView.setImageDrawable(new BitmapDrawable(getResources(), tempBitmap));
    }

    private void initCanvas() {
        surfaceBitmap = Bitmap.createBitmap(getWindowManager().getDefaultDisplay().getWidth(), getWindowManager().getDefaultDisplay().getHeight(), Bitmap.Config.ARGB_8888);
        surfaceCanvas = new Canvas(surfaceBitmap);

        surfaceCanvas.drawColor(Color.TRANSPARENT);

        canvasLayout.setImageDrawable(new BitmapDrawable(getResources(), surfaceBitmap));
    }

    private void redrawCanvas() {
        surfaceCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        for (Pair<Integer, Integer> mSegment : mSegments) {
            RelativeLayout.LayoutParams
                    mParams1 = (RelativeLayout.LayoutParams) Nodes.get(mSegment.first).getLayoutParams(),
                    mParams2 = (RelativeLayout.LayoutParams) Nodes.get(mSegment.second).getLayoutParams();
            surfaceCanvas.drawLine(
                    mParams1.leftMargin + Nodes.get(mSegment.first).getWidth() / 2, mParams1.topMargin + Nodes.get(mSegment.first).getHeight() / 2,
                    mParams2.leftMargin + Nodes.get(mSegment.second).getWidth() / 2, mParams2.topMargin + Nodes.get(mSegment.second).getHeight() / 2,
                    surfacePaint
            );
        }
        if (tempLine != null)
            surfaceCanvas.drawLine(tempLine.first.x, tempLine.first.y, tempLine.second.x, tempLine.second.y, surfacePaint);

        canvasLayout.setImageDrawable(new BitmapDrawable(getResources(), surfaceBitmap));
    }

    private void setFocusedVertex(int vertexIndex, int vertexType, int selectType) {
        if (selectType == 1) {
            if (vertexType == 1)
                ((ImageView) Nodes.get(vertexIndex).
                        findViewById(R.id.vertex_node)).setImageResource(R.drawable.number_vertex_selected);
            else
                ((ImageView) Nodes.get(vertexIndex).
                        findViewById(R.id.vertex_node)).setImageResource(R.drawable.text_vertex_selected);
        } else if (selectType == 2) {
            if (vertexType == 1)
                ((ImageView) Nodes.get(vertexIndex).
                        findViewById(R.id.vertex_node)).setImageResource(R.drawable.number_vertex_focused);
            else
                ((ImageView) Nodes.get(vertexIndex).
                        findViewById(R.id.vertex_node)).setImageResource(R.drawable.text_vertex_focused);
        }
    }

    private void unsetFocusedVertex(int vertexIndex, int vertexType) {
        if (vertexType == 1)
            ((ImageView) Nodes.get(vertexIndex).
                    findViewById(R.id.vertex_node)).setImageResource(R.drawable.number_vertex);
        else
            ((ImageView) Nodes.get(vertexIndex).
                    findViewById(R.id.vertex_node)).setImageResource(R.drawable.text_vertex);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void selectItem(int position) {
        if (position == 0) {
            fileName = null ;
            setDefault();
        }
        else if (position == 1) {
            FragmentManager fragmentManager = getFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            Fragment fragment = new Menu_FragmentList() ;
            fragmentTransaction.add(R.id.drawer_layout, fragment);

            fragmentTransaction.commit();
        }
        else if (position == 2) {
            if( fileName == null ) {
                final RelativeLayout relativeLayout = new RelativeLayout(Main_Activity.this);
                relativeLayout.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                relativeLayout.setPadding(10, 5, 10, 5);
                final EditText editText = new EditText(Main_Activity.this);
                editText.setTextColor(Color.WHITE);
                editText.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                relativeLayout.addView(editText);
                new AlertDialog.Builder(Main_Activity.this)
                        .setTitle("Enter file name:")
                        .setView(relativeLayout)
                        .setNegativeButton("Save file", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                String enteredText = editText.getText().toString();
                                Cursor cursor = getContentResolver().query(
                                        FileList_Provider.FILELIST_URI,
                                        null,
                                        FileList_Provider.FILE_NAME + "=?",
                                        new String[]{
                                                enteredText
                                        },
                                        null);
                                if( !cursor.moveToFirst() || cursor.getCount() == 0 ) {
                                    fileName = enteredText ;
                                    ContentValues contentValues = new ContentValues();
                                    contentValues.put(FileList_Provider.FILE_NAME, fileName);
                                    contentValues.put(FileList_Provider.FILE_FULL, fileName + ".wg");
                                    getContentResolver().insert(FileList_Provider.FILELIST_URI, contentValues);
                                    saveFile(fileName + ".wg");
                                }
                                else
                                    Toast.makeText(getApplicationContext(), "File with such name already exists!", Toast.LENGTH_SHORT).show() ;
                                cursor.close() ;
                            }
                        })
                        .setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                            }
                        })
                        .show();
            }
            else {
                Cursor cursor = getContentResolver().query(
                        FileList_Provider.FILELIST_URI,
                        null,
                        FileList_Provider.FILE_NAME + "=?",
                        new String[]{
                                fileName
                        },
                        null);
                if( !cursor.moveToFirst() || cursor.getCount() == 0 ) {
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(FileList_Provider.FILE_NAME, fileName);
                    contentValues.put(FileList_Provider.FILE_FULL, fileName + ".wg");
                    getContentResolver().insert(FileList_Provider.FILELIST_URI, contentValues);
                }
                cursor.close() ;
                saveFile(fileName);
            }
        }
        else if (position == 3)
            setDefault();
        (new Handler()).postDelayed(new Runnable() {
            @Override
            public void run() {
                redrawCanvas();
            }
        }, 1);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    private void setDefault() {
        mSegments = new ArrayList<>();
        redrawCanvas();
        number_nodes_counter = 0;
        for (FrameLayout node : Nodes)
            mainLayout.removeView(node);
        NodesInfo = new ArrayList<>();
        NodesPos = new ArrayList<>();
        Nodes = new ArrayList<>();
        Edges = new ArrayList<>();
    }

    private void loadFile( String fileName ) {
        setDefault();

        try {
            File newFolder = new File(Environment.getExternalStorageDirectory(), "WordGraph.files");
            if (!newFolder.exists())
                newFolder.mkdir();
            ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(newFolder + "/" + fileName));

            int nodes_counter = objectInputStream.readInt();

            for (int i = 0; i < nodes_counter; ++i)
                NodesInfo.add(new Pair<>((String) objectInputStream.readObject(), objectInputStream.readInt()));

            for (int i = 0; i < nodes_counter; ++i)
                NodesPos.add(new Point(objectInputStream.readInt(), objectInputStream.readInt()));

            for (int i = 0; i < nodes_counter; ++i) {
                if (NodesInfo.get(i).second == 1)
                    ++number_nodes_counter;
                addNewNode(NodesPos.get(i), NodesInfo.get(i).first, NodesInfo.get(i).second, true);
            }

            for (int i = 0; i < nodes_counter; ++i) {
                Edges.add(new ArrayList<Integer>());
                int rSize = objectInputStream.readInt();
                for (int j = 0; j < rSize; ++j)
                    Edges.get(i).add(objectInputStream.readInt());
            }

            int rSize = objectInputStream.readInt();
            for (int i = 0; i < rSize; ++i)
                mSegments.add(new Pair<>(objectInputStream.readInt(), objectInputStream.readInt()));

            objectInputStream.close();
        } catch (Exception ex) {
        }
    }

    private void saveFile( String fileName ) {
        try {
            File newFolder = new File(Environment.getExternalStorageDirectory(), "WordGraph.files");
            if (!newFolder.exists())
                newFolder.mkdir();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(newFolder + "/" + fileName));

            objectOutputStream.writeInt(Nodes.size());

            for (int i = 0; i < Nodes.size(); ++i) {
                objectOutputStream.writeObject(NodesInfo.get(i).first);
                objectOutputStream.writeInt(NodesInfo.get(i).second);
            }

            for (int i = 0; i < Nodes.size(); ++i) {
                objectOutputStream.writeInt(NodesPos.get(i).x);
                objectOutputStream.writeInt(NodesPos.get(i).y);
            }

            for (int i = 0; i < Nodes.size(); ++i) {
                objectOutputStream.writeInt(Edges.get(i).size());
                for (int j = 0; j < Edges.get(i).size(); ++j)
                    objectOutputStream.writeInt(Edges.get(i).get(j));
            }

            objectOutputStream.writeInt(mSegments.size());
            for (int i = 0; i < mSegments.size(); ++i) {
                objectOutputStream.writeInt(mSegments.get(i).first);
                objectOutputStream.writeInt(mSegments.get(i).second);
            }

            objectOutputStream.flush();
            objectOutputStream.close();
        } catch (Exception ex) {
        }
    }

    private long lastActionTime = 0 ;
    @Override
    public void receiveFileName( String name ) {
        if( name == null )
            return ;
        fileName = name ;
        loadFile(fileName) ;
        (new Handler()).postDelayed(new Runnable() {
            @Override
            public void run() {
                redrawCanvas();
            }
        }, 1);
    }

    private class ChangeModeListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            long curActionTime = System.currentTimeMillis() ;
            if( curActionTime - lastActionTime < 200 )
                return false ;
            lastActionTime = curActionTime ;
            if (currentMode != MODE_NONE) {
                findViewById(R.id.btns_action).setVisibility(View.VISIBLE);
                findViewById(R.id.btns_cancel).setVisibility(View.GONE);
                currentMode = MODE_NONE ;
            } else {
                findViewById(R.id.btns_action).setVisibility(View.GONE);
                findViewById(R.id.btns_cancel).setVisibility(View.VISIBLE);
                if (v.getId() == R.id.onModeCreate) {
                    currentMode = MODE_CREATE ;
                }
                else if( v.getId() == R.id.onModeDelete ) {
                    currentMode = MODE_DELETE ;
                }
            }
            return false;
        }
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
            mDrawerLayout.closeDrawers();
        }
    }

}