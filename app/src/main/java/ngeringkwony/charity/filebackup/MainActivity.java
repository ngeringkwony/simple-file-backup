package ngeringkwony.charity.filebackup;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button connect;
    private Button disconnect;
    private Button upload;
    private Button download;
    private EditText serverAddress;
    private EditText userName;
    private EditText userPassword;
    private ProgressDialog progressDialog;

    private final int NETWORK_STATE_PERMISSION = 1;
    private final int CHOOSE_FILE_CODE = 1;

    ListView listView;

    String fileToDownload;
    String selectedFilePath;
    String fileName;

    private FTPFunctions ftpClient;

    private String[] fileList = new String[8];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        upload = (Button) findViewById(R.id.b_upload);
        download = (Button) findViewById(R.id.b_download);
        connect = (Button) findViewById(R.id.b_connect);
        disconnect = (Button) findViewById(R.id.b_disconnect);
        //getFileList = (Button) findViewById(R.id.b_filelist);
        serverAddress = (EditText) findViewById(R.id.et_server_address);
        userName = (EditText) findViewById(R.id.et_server_username);
        userPassword = (EditText) findViewById(R.id.et_server_password);

        ftpClient = new FTPFunctions();

        connect.setOnClickListener(this);
        upload.setOnClickListener(this);
        disconnect.setOnClickListener(this);
        download.setOnClickListener(this);

    }//end onCreate

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.b_connect:
                if (isOnline(MainActivity.this)) {
                    connectToServer();
                } else {
                    Toast.makeText(MainActivity.this, "Check your internet connection!", Toast.LENGTH_LONG);
                }
                break;
            case R.id.b_upload:
                launchFileChooser();
                break;
            case R.id.b_disconnect:
                progressDialog = ProgressDialog.show(MainActivity.this, "", "Disconnecting...", true, false);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        ftpClient.disconnect();
                        handler.sendEmptyMessage(4);
                    }
                }).start();
                break;
            case R.id.b_download:
                getFileList();
                break;
        }

    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            // super.handleMessage(msg);
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            if (msg.what == 0) {
                Toast.makeText(MainActivity.this, "Connected!", Toast.LENGTH_LONG).show();
            } else if (msg.what == 1) {
                showFileListDialog(fileList);
            } else if (msg.what == 2) {
                Toast.makeText(MainActivity.this, "Uploaded Successfully", Toast.LENGTH_SHORT).show();
            } else if (msg.what == 3) {
                Toast.makeText(MainActivity.this, "Download Successful", Toast.LENGTH_SHORT).show();
            } else if(msg.what == 4){
                Toast.makeText(MainActivity.this, "Disconnected", Toast.LENGTH_SHORT).show();
            }else {
                Toast.makeText(MainActivity.this, "Unable To Complete Action", Toast.LENGTH_SHORT).show();
            }
        }
    };

    private void showFileListDialog(String[] fileList) {
        final Dialog dialog = new Dialog(MainActivity.this);
        dialog.setContentView(R.layout.file_list_dialog);
        dialog.setTitle("Dir/File List");

        TextView heading = (TextView) dialog.findViewById(R.id.tv_list_heading);
        heading.setText("File List");

        if (fileList != null && fileList.length > 0) {
            listView = (ListView) dialog.findViewById(R.id.lv_item_list);
            ArrayAdapter<String> fileListAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, fileList);
            listView.setAdapter(fileListAdapter);

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    fileToDownload = (String)adapterView.getItemAtPosition(i);
                    Log.i("FILETODOWNLOAD: ", fileToDownload);
                    progressDialog = ProgressDialog.show(MainActivity.this, "","Downloading "+fileToDownload, true, true);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                           final boolean downloadStatus = ftpClient.download("/"+fileToDownload, fileToDownload);
                            if(downloadStatus == true){
                                handler.sendEmptyMessage(3);
                            }
                        }
                    }).start();
                }
            });
        } else {
            heading.setText("NO FILES");
        }

        Button dialogButton = (Button) dialog.findViewById(R.id.b_ok);
        dialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    private void connectToServer() {
        final String server = serverAddress.getText().toString().trim();
        final String user = userName.getText().toString().trim();
        final String password = userPassword.getText().toString().trim();

        if (server.length() < 1) {
            Toast.makeText(MainActivity.this, "Enter the Server Address", Toast.LENGTH_LONG).show();
        } else if (user.length() < 1) {
            Toast.makeText(MainActivity.this, "Enter the User Name", Toast.LENGTH_LONG).show();
        } else if (password.length() < 1) {
            Toast.makeText(MainActivity.this, "Enter the Password", Toast.LENGTH_LONG).show();
        } else {
            progressDialog = ProgressDialog.show(MainActivity.this, "", "Connecting...", true, true);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    boolean status = false;
                    status = ftpClient.connect(server, user, password, 21);
                    if (status == true) {
                        handler.sendEmptyMessage(0);
                    } else {
                        handler.sendEmptyMessage(-1);
                    }
                }
            }).start();
        }
    }

    private boolean isOnline(Context context) {
        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_NETWORK_STATE}, NETWORK_STATE_PERMISSION);
        }

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            return true;
        }
        return false;
    }

    private void launchFileChooser(){
        Intent intent = new Intent();
        intent.setType("File/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Choose a file to upload..."), CHOOSE_FILE_CODE);
    }

    private void getFileList() {
        progressDialog = ProgressDialog.show(MainActivity.this, "", "Getting Files...", true, true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                fileList = ftpClient.printFilesList("/");
                handler.sendEmptyMessage(1);
            }
        }).start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        progressDialog = ProgressDialog.show(this, "", "Uploading file...", true, true);
        if(resultCode == Activity.RESULT_OK){
            if(requestCode == CHOOSE_FILE_CODE){
                if(data == null){
                    return;
                }

                Uri selectedFileUri = data.getData();
                selectedFilePath = FilePath.getPath(this, selectedFileUri);
                Log.i("SELECTED FILE PATH: ", ""+ selectedFilePath);

                int index = selectedFilePath.lastIndexOf("/");
                fileName = selectedFilePath.substring(index+1);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        boolean status;
                        Log.i("FILENAME: "," "+fileName);
                        status = ftpClient.upload(selectedFilePath, fileName,"/", getApplicationContext());

                        if(status == true){
                            handler.sendEmptyMessage(2);
                        }else{
                            handler.sendEmptyMessage(-1);
                        }
                    }
                }).start();
            }
        }
    }
}




