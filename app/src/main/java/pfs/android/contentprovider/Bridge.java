////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2023 Vladislav Trifochkin
//
// This file is part of `Android support library`.
//
// Changelog:
//      2023.03.17 Initial version.
////////////////////////////////////////////////////////////////////////////////
package pfs.android.contentprovider;

import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

public class Bridge
{
    public static final int INVALID_FILE_HANDLE = -1;

    android.content.Context _context;
    private HashMap<Integer, ParcelFileDescriptor> _parcelFds = new HashMap<Integer, ParcelFileDescriptor>();

    private Bridge (android.content.Context ctx) throws NullPointerException
    {
        if (ctx == null )
            throw new NullPointerException("Android context");
        _context = ctx;
    }

    public static Bridge create (android.content.Context ctx)
    {
        try {
            Bridge bridge = new Bridge(ctx);
            return bridge;
        } catch (NullPointerException ex) {
            return null;
        }
    }

    public int openRawReadOnly (String path)
    {
        ParcelFileDescriptor parcelFD = null;

        try {
            Say.d("Opening file: " + path);
            parcelFD = _context.getContentResolver().openFileDescriptor(Uri.parse(path), "r");
        } catch (FileNotFoundException ex) {
            Say.d(String.format("Open file failure: %s: %s", Uri.parse(path), ex.getMessage()));
            return INVALID_FILE_HANDLE;
        }

        int fd = parcelFD.getFd();
        Say.d(String.format("File opened: %s (fd=%d)", path, fd));
        _parcelFds.put(fd, parcelFD);
        return fd;
    }

    public void close (int handle)
    {
        ParcelFileDescriptor parcelFD = _parcelFds.get(handle);

        Say.d(String.format("Close file: fd=%d", handle));

        if (parcelFD != null) {
            try {
                parcelFD.close();
            } catch (IOException ex) {
            } finally {
                _parcelFds.remove(handle);
            }
        }
    }

    public ContentInfo getFileInfo (Uri uri)
    {
        ContentInfo fileInfo = new ContentInfo();
        fileInfo.uri = uri.toString();
        fileInfo.displayName = null;
        fileInfo.mimeType = null;
        fileInfo.size = -1;

        if (uri.getScheme().equals("content")) {
            Cursor cursor = _context.getContentResolver().query(uri, null, null, null, null);
            fileInfo.mimeType = _context.getContentResolver().getType(uri);

            try {
                if (cursor != null && cursor.moveToFirst()) {
                    String[] columnNames =  cursor.getColumnNames();

                    for (int i = 0; i < columnNames.length; i++) {
                        //Say.d(String.format("*** Column[%d] = %s", i, columnNames[i]));

                        if (columnNames[i].equals("last_modified")) {
                            int modTimeIndex = cursor.getColumnIndex(columnNames[i]);

                            if (modTimeIndex > 0) {
                                //Say.d(String.format("*** LAST MODIFIED STRING = %s", cursor.getString(modTimeIndex)));
                                //Say.d(String.format("*** LAST MODIFIED LONG = %d", cursor.getLong(modTimeIndex)));
                                fileInfo.modTime = cursor.getLong(modTimeIndex);
                            }
                        }
                    }

                    int displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);

                    if (displayNameIndex >= 0)
                        fileInfo.displayName = cursor.getString(displayNameIndex);

                    if (sizeIndex > 0)
                        fileInfo.size = cursor.getLong(sizeIndex);
                }
            } finally {
                if (cursor != null)
                    cursor.close();
            }
        }

        if (fileInfo.displayName == null) {
            fileInfo.displayName = uri.getPath();
            int cut = fileInfo.displayName.lastIndexOf('/');

            if (cut != -1) {
                fileInfo.displayName = fileInfo.displayName.substring(cut + 1);
            }

//             if (fileInfo.mimeType == null) {
//                 if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                     try {
//                         fileInfo.mimeType = Files.probeContentType(Paths.get(uri.getPath()));
//                     } catch (IOException ex) {}
//                 } else {
//                     ; // TODO Try other tools to detect MIME type
//                 }
//             }

            if (fileInfo.mimeType == null)
                fileInfo.mimeType = "application/octet-stream";
        }

        return fileInfo;
    }
}
