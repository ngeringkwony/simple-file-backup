package ngeringkwony.charity.filebackup;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by ngeringkwony on 30/05/16.
 */
public class FTPFunctions {
    FTPClient mFTPClient;
    //connecting to ftp server
    public boolean connect(String host, String user, String pass, int port) {
        try {
            mFTPClient = new FTPClient();
            mFTPClient.connect(host, port);

            //if connection is successful, log in
            if (FTPReply.isPositiveCompletion(mFTPClient.getReplyCode())) {
                mFTPClient.enterLocalPassiveMode();
                boolean status = mFTPClient.login(user, pass);

                //file transfer mode BINARY_FILE_TYPE for images, text and compressed files
                mFTPClient.setFileType(FTP.BINARY_FILE_TYPE);
                //mFTPClient.enterRemotePassiveMode();
                return status;
            }

        } catch (IOException e) {
            e.printStackTrace();
            Log.e("CONNECTION", "Could not connect to host");
        }
        return false;
    }

    //disconnecting from server
    public boolean disconnect() {
        try {
            mFTPClient.logout();
            mFTPClient.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("DIsCONNECTING", "diconnection error");
        }
        return false;
    }

    //upload
    public boolean upload(String filePath, String destFileName, String destDirectory, Context context) {
        boolean status = false;
        try {
            FileInputStream fileInputStream = new FileInputStream(filePath);
            status = mFTPClient.storeFile(destFileName, fileInputStream);

            fileInputStream.close();

            return status;
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("UPLOADING", "uploading file failed");
        }
        return status;
    }

    //download
    public boolean download(String srcFilePath, String destFile) {
        boolean status = false;
        try {
            //save downloaded files to a folder
            String state = Environment.getExternalStorageState();
            if(Environment.MEDIA_MOUNTED.equals(state)){
                Log.i("IS MEDIA MOUNTED? ", "TRUE");
                File mDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/FileBackup");
                mDir.mkdir();
                FileOutputStream fos = new FileOutputStream(new File(mDir, destFile));
                status = mFTPClient.retrieveFile(srcFilePath, fos);

            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return status;
    }

    //list all files in the dir
    public String[] printFilesList(String dirPath) {
        String[] fileNames = null;
        try {
            FTPFile[] ftpFiles = mFTPClient.listFiles(dirPath);
            fileNames = new String[ftpFiles.length];

            for (int i = 0; i < ftpFiles.length; i++) {
                if (ftpFiles[i].isFile()) {
                    fileNames[i] = ftpFiles[i].getName();
                } else {
                    fileNames[i] = "Dir:" + ftpFiles[i].getName();
                }
            }
            return fileNames;
        } catch (IOException e) {
            e.printStackTrace();
            return fileNames;
        }
    }
}
