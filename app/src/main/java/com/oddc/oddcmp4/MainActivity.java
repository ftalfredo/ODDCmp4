package com.oddc.oddcmp4;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PackageInfo;

import android.Manifest;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;

import android.os.Bundle;
import android.os.AsyncTask;

import android.text.Html;

import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.view.inputmethod.InputMethodManager;

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

import java.io.File;
import java.io.FileFilter;
import java.util.Locale;
import java.util.Comparator;
import java.util.ArrayList;

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
    int nMP4 = 0;
    long zMP4 = 0;
    int tValid = 0;
    int tNull = 0;
    int tZero = 0;
    final int REQ_READ_EXT_STORAGE = 1001;
    boolean reqGranted = false;
    ArrayList<FileListData> vidFiles;
    FileListAdapter adapter;

    InputMethodManager inputManager;

    public static Context mContext;

    public static final int S_VALID = 0;
    public static final int S_SIZE  = 1;
    public static final int S_NAME  = 2;
    int sortMode = S_VALID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("SEQUENCE","MainActivity.onCreate BoM");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = getApplicationContext();
        inputManager = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);

        String subT = "MP4 validation tool ";
        try {
            PackageInfo pinfo = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
            subT += pinfo.versionName;
        }
        catch (NameNotFoundException e){}

        android.support.v7.app.ActionBar ab = getSupportActionBar();
        ab.setTitle("ODDC");
        ab.setSubtitle(subT);


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

        fStat = (TextView)findViewById(R.id.fStat);
        fStat.setOnClickListener(new View.OnClickListener() {
                  @Override
                  public void onClick(View view) {
                      sortMode = ( sortMode + 1) % 3;
                      switch(sortMode){
                          case S_VALID: vidFiles.sort(new ValidListComparator()); break;
                          case S_SIZE: vidFiles.sort(new SizeListComparator()); break;
                          case S_NAME: vidFiles.sort(new NameListComparator()); break;
                      }
                      adapter = new FileListAdapter(mContext, vidFiles);
                      fsview.setAdapter(adapter);
                  }
              }
        );

        btnSwitch = (Button)findViewById(R.id.btnSwitch);
        btnSwitch.setVisibility(View.INVISIBLE);

        errMsg = (TextView)findViewById(R.id.errMsg);

        pBar = (ProgressBar)findViewById(R.id.pBar);

        fview = findViewById(R.id.fslistView);
        vview = findViewById(R.id.videoview);
        vs = (ViewSwitcher)findViewById(R.id.switcher);

        md = new File("/sdcard/oddc");

        mVideoView = (VideoView) findViewById(R.id.videoview);
        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
                    @Override
                    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
                        Log.d("VIDEOMP4","onVideoSizeChangedddddddddddddddddddddd BoM W="+width+" H="+height);
                        MediaController mc = new MediaController(MainActivity.this);
                        mVideoView.setMediaController(mc);
                        mc.setAnchorView(mVideoView);
                    }
                });
            }
        });

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
        adapter = new FileListAdapter(this, vidFiles);
        fsview.setAdapter(adapter);
        fsview.setOnItemClickListener(new OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> adapter, View v, int pos, long arg3)
            {
                errMsg.setText("");
                curVideo = vidFiles.get(pos).fpath;
                int curVideoSZ = (int)vidFiles.get(pos).fsize;
                File f = new File(curVideo);

                if (! f.exists()){
                    Log.d("VIDEOMP4",f.toString()+ " !EXISTS");
                    errMsg.setText("MediaPlayer: "+f.getName()+" not found");
                    return;
                }
                if (! f.isFile()){
                    Log.d("VIDEOMP4",f.toString()+ " !ISFILE");
                    errMsg.setText("MediaPlayer: "+f.getName()+" not a file");
                    return;
                }
                if (! f.canRead()){
                    Log.d("VIDEOMP4",f.toString()+ " !CANREAD");
                    errMsg.setText("MediaPlayer: cannot read "+f.getName());
                    return;
                }

                if (vidFiles.get(pos).fvalid.compareTo("Valid") != 0) return;

                Uri uri = Uri.fromFile(f);
                btnSwitch.setVisibility(View.VISIBLE);
                vs.showNext();
                try {
                    mVideoView.setOnErrorListener(mOnErrorListener);
                    mVideoView.setVideoURI(uri);
                    errMsg.setText(String.format("%,d  %s",curVideoSZ,curVideo));
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
        String fMaxF; int fMaxSZ = 0;
        @Override
        protected Integer doInBackground(Void... params) {
            int nProg = 0;
            File[] vFiles = md.listFiles(new FileFilter() {
                public boolean accept(File fname) {
                    return fname.getPath().endsWith(".mp4");
                }
            });
            if (vFiles != null) {
                nMP4 = vFiles.length;
                String hasVideo = "NA";
                String dur = "NA"; int vdur = 0;
                String sdur = "NA";
                String dim = "NA";
                String vw = "NA";
                String vh = "NA";
                String sbps = "NA"; int kbps = 0;
                String sfr  = "NA"; int fr = 0;

                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                vidFiles = new ArrayList<FileListData>();
                for (File f : vFiles) {
                    nProg++;
                    publishProgress(nProg);
                    FileListData fd = new FileListData();
                    fd.fpath = f.getPath();
                    fd.fname = f.getName();
                    fd.fsize = f.length();
                    zMP4 += fd.fsize;
                    if (fd.fsize > 0) {
                        if ( fd.fsize > fMaxSZ ){
                            fMaxSZ = (int)fd.fsize;
                            fMaxF = fd.fname;
                        }
                        try {
                            retriever.setDataSource(fd.fpath);
                            hasVideo = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO);
                            dur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                            sbps = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
                            //sfr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE);
                            if (dur != null){
                                vdur = (Integer.parseInt(dur))/1000;
                                long h = vdur / 3600;
                                long m = (vdur - h * 3600) / 60;
                                long s = vdur - (h * 3600 + m * 60);
                                sdur = String.format(Locale.US,"%1$02d:%2$02d:%3$02d",h,m,s);
                            }
                            vw = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
                            vh = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
                            if (sbps != null){
                                kbps = (Integer.parseInt(sbps))/1000;
                                dim =  kbps + "kbps  " +  vw + "x" + vh;
                            }
                            else {
                                dim =  "nullkbps  " +  vw + "x" + vh;
                            }
                            fd.fdur = sdur;
                            fd.fdim = dim;
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
                vidFiles.sort(new ValidListComparator());
                //vidFiles.sort(new SizeListComparator());
            }
            return new Integer(nMP4);
        }

        @Override
        protected void onProgressUpdate(Integer... p) {
            fStat.setText("Reading "+p[0].toString()+" mp4 files");
        }

        @Override
        protected void onPostExecute(Integer i) {
            pBar.setVisibility(View.GONE);
            String sMP4 = "Total mp4:";
            String sValid  = "Total Valid:";
            String sNValid = "Total NotValid:";
            int tNValid = tNull + tZero;
            float fMP4 = (float)(zMP4 / (1024 * 1024) );
            String sStatus = String.format(Locale.US,"%1$13s %2$5d %3$,12.1f MB\n%4$13s %5$5d %6$16s %7$5d",
                    sMP4,nMP4,fMP4,  sValid,tValid, sNValid, tNValid);
            fStat.setTypeface(Typeface.MONOSPACE);
            fStat.setText(sStatus);
            loadViewList();
        }
    }

    private class ValidListComparator implements Comparator<FileListData>{
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

    private class SizeListComparator implements Comparator<FileListData>{
        @Override
        public int compare(FileListData fld1, FileListData fld2) {
            if (fld1.fsize  < fld2.fsize) return -1;
            if (fld1.fsize == fld2.fsize) return  0;
            if (fld1.fsize  > fld2.fsize) return  1;
            return 0;
        }
    }

    private class NameListComparator implements Comparator<FileListData>{
        @Override
        public int compare(FileListData fld1, FileListData fld2) {
            return fld1.fname.compareTo(fld2.fname);
        }
    }

    private class FileListData {
        public long fsize;
        public String fpath;
        public String fname;
        public String fvalid;
        public String fdur;
        public String fdim;
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

            TextView fsize = (TextView) convertView.findViewById(R.id.fsize); fsize.setText( String.format("%,d",(int)flist.fsize) );
            TextView fname = (TextView) convertView.findViewById(R.id.fname); fname.setText(flist.fname);

            if(position % 2 ==1) convertView.setBackgroundColor(Color.rgb(0xd9, 0xdd, 0xf2));
            else                 convertView.setBackgroundColor(Color.rgb(0xec, 0xee, 0xf8));

            TextView fdur = (TextView) convertView.findViewById(R.id.fdur); fdur.setText(flist.fdur);
            TextView fdim = (TextView) convertView.findViewById(R.id.fdim); fdim.setText(flist.fdim);

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
