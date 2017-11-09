package com.oddc.oddcmp4;

import android.content.Context;
import android.content.pm.PackageManager;
import android.Manifest;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.os.Bundle;

import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.view.inputmethod.InputMethodManager;

import android.text.Html;

import android.widget.ProgressBar;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.VideoView;
import android.widget.TextView;
import android.widget.MediaController;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ArrayAdapter;
import android.widget.AdapterView.OnItemClickListener;

import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaMetadataRetriever;

import android.graphics.Color;
import android.graphics.Typeface;

import android.net.Uri;
import android.util.Log;
import android.widget.ViewSwitcher;

import android.os.AsyncTask;
import java.util.ArrayList;

import java.io.File;
import java.io.FileFilter;
import java.util.Comparator;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    File md;
    View fview;
    View vview;
    SearchView sview;
    ListView fsview;
    ViewSwitcher vs;
    String curVideo = null;
    VideoView mVideoView;
    Button btnSwitch;
    TextView errMsg;
    TextView fStat;
    ProgressBar pBar;
    int tMP4 = 0;
    int tValid = 0;
    int tNull = 0;
    int tZero = 0;
    final int REQ_READ_EXT_STORAGE = 1001;
    boolean reqGranted = false;
    ArrayList<FileListData> vidFiles;

    InputMethodManager inputManager;

    public static Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("SEQUENCE","MainActivity.onCreate BoM");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = getApplicationContext();
        inputManager = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);

        android.support.v7.app.ActionBar ab = getSupportActionBar();
        ab.setTitle("ODDC");
        ab.setSubtitle("MP4 validation tool");

        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQ_READ_EXT_STORAGE);
        }
        else reqGranted = true;

        fsview = (ListView) findViewById(R.id.fslistView);

        sview = (SearchView) findViewById(R.id.sview);
        sview.setQueryHint("Search for mp4");
        sview.setIconified(false);
        EditText searchEditText = (EditText) sview.findViewById(android.support.v7.appcompat.R.id.search_src_text);
        searchEditText.setTextColor(Color.WHITE);
        searchEditText.setHintTextColor(Color.LTGRAY);

        sview.setOnQueryTextListener(new OnQueryListener());

        btnSwitch = (Button)findViewById(R.id.btnSwitch);
        btnSwitch.setVisibility(View.INVISIBLE);

        errMsg = (TextView)findViewById(R.id.errMsg);
        fStat = (TextView)findViewById(R.id.fStat);
        pBar = (ProgressBar)findViewById(R.id.pBar);

        fview = findViewById(R.id.fslistView);
        vview = findViewById(R.id.videoview);
        vs = (ViewSwitcher)findViewById(R.id.switcher);

        md = new File("/sdcard/oddc");

        mVideoView = (VideoView) findViewById(R.id.videoview);
        MediaController mc = new MediaController(this);
        mc.setAnchorView(mVideoView);
        mc.setPadding(0,0,0,0);
        mVideoView.setMediaController(mc);

        if (reqGranted) new ListFilesTask().execute();

        Log.d("SEQUENCE","MainActivity.onCreate EoM");
    }

    @Override
    protected void onResume() {
    Log.i("SEQUENCE", "MainActivity.onResume BoM");
    super.onResume();
    if (! reqGranted){
        sview.clearFocus();
    }
    Log.i("SEQUENCE", "MainActivity.onResume EoM");
}

    @Override
    protected void onStart() {
        Log.i("SEQUENCE", "MainActivity.onStart BoM");
        super.onStart();
        Log.i("SEQUENCE", "MainActivity.onStart EoM");
    }

    @Override
    protected void onStop() {
        Log.i("SEQUENCE", "MainActivity onStop BoM");
        super.onStop();
        Log.i("SEQUENCE", "MainActivity onStop EoM");
    }



    class OnQueryListener implements SearchView.OnQueryTextListener {
        public boolean onQueryTextChange (String newText){
            if (vidFiles != null) {
                for (int i = 0; i < vidFiles.size(); i++) {
                    FileListData sVal = vidFiles.get(i);
                    if (sVal.fname.startsWith(newText)) {
                        fsview.setSelection(i);
                        errMsg.setText(" ");
                        return true;
                    }
                }
            }
            errMsg.setText(newText + " not found");
            inputManager.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);

            return true;
        }
        public boolean onQueryTextSubmit (String query){ return true; }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQ_READ_EXT_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("VIDEOMP4","REQ_READ_EXT_STORAGE PERMISSION_GRANTED");
                    reqGranted = true;
                    new ListFilesTask().execute();
                } else {
                    Log.d("VIDEOMP4","REQ_READ_EXT_STORAGE PERMISSION_DENIED");
                    reqGranted = false;
                    pBar.setVisibility(View.GONE);
                    inputManager.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
                    errMsg.setText("PERMISSION needed to read oddc directory");
                }
                return;
            }
        }
    }

    private void loadViewList(){
        FileListAdapter adapter = new FileListAdapter(this, vidFiles);
        fsview.setAdapter(adapter);
        fsview.setOnItemClickListener(new OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> adapter, View v, int pos, long arg3)
            {
                errMsg.setText("");
                curVideo = vidFiles.get(pos).fpath;
                int curVideoSZ = (int)vidFiles.get(pos).fsize;
                Log.d("VIDEOMP4","loadViewList CLICK "+String.valueOf(pos)+" "+curVideo);
                File f = new File(curVideo);

                if (! f.exists()){
                    Log.d("VIDEOMP4",f.toString()+ " !EXISTS");
                    errMsg.setText("MediaPlayer: "+curVideo+" not found");
                    return;
                }
                if (! f.isFile()){
                    Log.d("VIDEOMP4",f.toString()+ " !ISFILE");
                    errMsg.setText("MediaPlayer: "+curVideo+" not file");
                    return;
                }
                if (! f.canRead()){
                    Log.d("VIDEOMP4",f.toString()+ " !CANREAD");
                    errMsg.setText("MediaPlayer: cannot read "+curVideo);
                    return;
                }
                if (f.length() == 0){
                    Log.d("VIDEOMP4",f.toString()+ " Zero length");
                    errMsg.setText("MediaPlayer: zero length "+curVideo);
                    return;
                }
                if (vidFiles.get(pos).fvalid.compareTo("NotValid") == 0){
                    errMsg.setText("MediaPlayer: NotValid mp4 "+ curVideo);
                }
                if (vidFiles.get(pos).fvalid.compareTo("Valid") != 0) return;

                Uri uri = Uri.fromFile(f);
                btnSwitch.setVisibility(View.VISIBLE);
                vs.showNext();
                try {
                    mVideoView.setOnErrorListener(mOnErrorListener);
                    mVideoView.setVideoURI(uri);
                    errMsg.setText(String.valueOf(curVideoSZ) + "  " + curVideo);
                    mVideoView.start();
                }
                catch (Exception e){
                    Log.e("VIDEOMP4","ERROR: unable to play "+curVideo);
                    errMsg.setText("MediaPlayer: unable to play "+curVideo);
                }
            }
        });
    }


    private class ListFilesTask extends AsyncTask<Void,Integer,Integer> {
        @Override
        protected Integer doInBackground(Void... params) {
            int fProg = 0;
            File[] vFiles = md.listFiles(new FileFilter() {
                public boolean accept(File fname) {
                    return fname.getPath().endsWith(".mp4");
                }
            });
            if (vFiles != null) {
                tMP4 = vFiles.length;
                String hasVideo = "NA";
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                vidFiles = new ArrayList<FileListData>();
                for (File f : vFiles) {
                    fProg++;
                    publishProgress(fProg);
                    FileListData fd = new FileListData();
                    fd.fpath = f.getPath();
                    fd.fname = f.getName();
                    fd.fsize = f.length();
                    if (fd.fsize > 0) {
                        try {
                            retriever.setDataSource(fd.fpath);
                            hasVideo = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO);
                            if (hasVideo == null){
                                hasVideo = "NotValid";
                                tNull++;
                            }
                            else {
                                if (hasVideo.compareTo("yes") == 0){
                                    hasVideo = "Valid";
                                    tValid++;
                                }
                            }
                        }
                        catch(Exception e){
                            Log.d("VIDEOMP4","listVideoFiles ERROR "+e.getMessage());
                        }
                    }
                    else {
                        hasVideo = "zero";
                        tZero++;
                    }
                    fd.fvalid = hasVideo;
                    vidFiles.add(fd);
                }
                vidFiles.sort(new FileListComparator());
            }
            return new Integer(tMP4);
        }

        @Override
        protected void onProgressUpdate(Integer... p) {
            fStat.setText("Reading "+p[0].toString()+" mp4 files");
        }

        @Override
        protected void onPostExecute(Integer i) {
            pBar.setVisibility(View.GONE);
            String sMP4 = "Total mp4:";
            String sOK  = "Total  Valid:";
            String sNull = "Total NotValid:";
            String sZero = "Total zero:";
            String sStatus = String.format(Locale.US,"%1$13s %2$5d %3$16s %4$5d \n%5$13s %6$5d %7$16s %8$5d",sMP4,tMP4,sNull,tNull,sOK, tValid,sZero,tZero);
            fStat.setTypeface(Typeface.MONOSPACE);
            fStat.setText(sStatus);
            loadViewList();
        }
    }

    private class FileListComparator implements Comparator<FileListData>{
        @Override
        public int compare(FileListData fld1, FileListData fld2) {
            int a1 = (int) ( fld1.fvalid.charAt(0) | 0x20 );
            int a2 = (int) ( fld2.fvalid.charAt(0) | 0x20 );
            int a12 = (a1 << 8) | a2;
            switch (a12) {
                case 0x6e76:
                case 0x7a6e:
                case 0x7a76:
                    return -1;
                case 0x6e6e:
                case 0x7676:
                case 0x7a7a:
                    return 0;
                case 0x6e7a:
                case 0x766e:
                case 0x767a:
                    return 1;
                default: return 0;
            }
        }
    }

    private class FileListData {
        public long fsize;
        public String fpath;
        public String fname;
        public String fvalid;
    }

    private class FileListAdapter extends ArrayAdapter<FileListData>
    {
        public FileListAdapter(Context context, ArrayList<FileListData> flist) {
            super(context, 0, flist);
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            String errStr;
            FileListData flist = getItem(position);
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.fslistview_layout, parent, false);
            }
            TextView fvalid = (TextView) convertView.findViewById(R.id.fvalid);
            if (flist.fvalid.compareTo("Valid") != 0) errStr = "<font color='#FF0000'>"+flist.fvalid+"</font>";
            else                                      errStr = "<font color='#000000'>"+flist.fvalid+"</font>";
            fvalid.setText(Html.fromHtml(errStr,0));

            TextView fsize = (TextView) convertView.findViewById(R.id.fsize); fsize.setText(Integer.toString((int)flist.fsize));
            TextView fname = (TextView) convertView.findViewById(R.id.fname); fname.setText(flist.fname);

            if(position % 2 ==1) convertView.setBackgroundColor(Color.rgb(0xd9, 0xdd, 0xf2));
            else                 convertView.setBackgroundColor(Color.rgb(0xec, 0xee, 0xf8));

            return convertView;
        }
    }


    private OnErrorListener mOnErrorListener = new OnErrorListener() {
        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            Log.d("VIDEOMP4","MediaPlayer ERROR for "+curVideo);
            errMsg.setText("MediaPlayer ERROR for "+curVideo);
            return true;
        }
    };

    public void onSwitch(View view){
        if (vs.getCurrentView() == fview) return; //vs.showNext();
        btnSwitch.setVisibility(View.INVISIBLE);
        errMsg.setText("");
        vs.showPrevious();
    }
}
