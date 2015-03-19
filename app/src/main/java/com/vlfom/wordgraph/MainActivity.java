package com.vlfom.wordgraph;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;


public class MainActivity extends ActionBarActivity {

    final Integer
            NUMBER_NODE_TYPE = 1,
            TEXT_NODE_TYPE = 2;
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
    private String[] mScreenTitles;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;
    private String
            mDrawerTitle,
            mTitle;
    private Pair<Point, Point> tempLine = null;
    private ArrayList<Pair<Integer, Integer>> mSegments = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ((TextView) findViewById(R.id.navbarTitle)).setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Roboto-Regular.ttf"));
        ((TextView) findViewById(R.id.navbarTitle)).setShadowLayer(2, 0, 1, Color.BLACK);
        ((TextView) findViewById(R.id.actionbarTitle)).setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Roboto-Regular.ttf"));
        ((TextView) findViewById(R.id.actionbarTitle)).setShadowLayer(2, 0, 1, Color.BLACK);

        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        getSupportActionBar().setTitle("");

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        drawBackground();

        mTitle = mDrawerTitle = "Word Graph";
        mScreenTitles = getResources().getStringArray(R.array.navbar_string_array);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

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
                        final Point touchPoint = new Point((int) event.getX(), (int) event.getY());
                        mainLayout.getLocationOnScreen(mainDisplacement);

                        final RelativeLayout relativeLayout = new RelativeLayout(MainActivity.this);
                        relativeLayout.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                        relativeLayout.setPadding(10, 5, 10, 5);
                        final EditText editText = new EditText(MainActivity.this);
                        editText.setTextColor(Color.BLACK);
                        editText.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                        relativeLayout.addView(editText);

                        new AlertDialog.Builder(MainActivity.this)
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

                        return false;
                    }
                }
        );
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
                    private final int NodeIndex = Nodes.size();
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
//                                if (event.getRawY() - dy - Nodes.get(NodeIndex).getLayoutParams().height / 2 - getSupportActionBar().getHeight() <= 0)
//                                    break;
//                                Log.d("MY", getSupportActionBar().getHeight() + " " + layoutParams.topMargin + " " + (event.getRawY() + Nodes.get(NodeIndex).getLayoutParams().height / 2) + " " + getWindowManager().getDefaultDisplay().getHeight()) ;
//                                if (event.getRawY() + Nodes.get(NodeIndex).getLayoutParams().height / 2 >= getWindowManager().getDefaultDisplay().getHeight())
//                                    break;
//                                if (event.getRawX() - dx - Nodes.get(NodeIndex).getLayoutParams().width / 2 <= 0)
//                                    break;
//                                if (event.getRawX() + dx + Nodes.get(NodeIndex).getLayoutParams().width / 2 >= getWindowManager().getDefaultDisplay().getWidth())
//                                    break;

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
                                    if( (int)(y-dy) >= getSupportActionBar().getHeight() && (int)(y-dy) + v.getHeight() <= getWindowManager().getDefaultDisplay().getHeight() )
                                        layoutParams.topMargin = (int) (y - dy);
                                    if( (int)(x-dx) >= 0 && (int)(x-dx) + v.getWidth() <= getWindowManager().getDefaultDisplay().getWidth() )
                                        layoutParams.leftMargin = (int) (x - dx);
                                    v.setLayoutParams(layoutParams);

                                    NodesPos.set(NodeIndex, new Point((int) (x - dx), (int) (y - dy)));
                                }

                                redrawCanvas();

                                checkNodesCollision(NodeIndex);
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

    private void checkNodesCollision(final int NodeIndex) {
        //Is it really necessary?
        /*
        for( FrameLayout Node : Nodes ) {
            if( Node != Nodes.get(NodeIndex) && Math.abs( Node.getX()-Nodes.get(NodeIndex).getX() ) <= node_diameter ) {

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

        return super.onOptionsItemSelected(item);
    }

    /**
     * Swaps fragments in the main content view
     */
    private void selectItem(int position) {
        // Highlight the selected item, update the title, and close the drawer
//        mDrawerList.setItemChecked(position, true);
//        setTitle(mScreenTitles[position]);
//        mDrawerLayout.closeDrawer(mDrawerList);
        if (position == 0)
            setDefault();
        else if (position == 1)
            loadInfo();
        else if (position == 2)
            saveInfo();
        else if (position == 3)
            setDefault();
        (new Handler()).postDelayed(new Runnable() {
            @Override
            public void run() {
                redrawCanvas();
            }
        }, 1);
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

    private void loadInfo() {
        setDefault();

        try {
            File newFolder = new File(Environment.getExternalStorageDirectory(), "WordGraph.files");
            if (!newFolder.exists())
                newFolder.mkdir();
            ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(newFolder + "/GraphFile.wg"));

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

    private void saveInfo() {
        try {
            File newFolder = new File(Environment.getExternalStorageDirectory(), "WordGraph.files");
            if (!newFolder.exists())
                newFolder.mkdir();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(newFolder + "/GraphFile.wg"));

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

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
        }
    }
}