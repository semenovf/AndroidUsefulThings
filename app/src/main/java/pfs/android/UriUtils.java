////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2023 Vladislav Trifochkin
//
// This file is part of `Android support library`.
//
// Changelog:
//      2023.08.31 Initial version.
////////////////////////////////////////////////////////////////////////////////
package pfs.android;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.util.List;

public class UriUtils
{
    // http://www.java2s.com/example/java-utility-method/uri-to-parent-uri/getparent-uri-uri-1f938.html
    public static android.net.Uri getParentUri (android.net.Uri uri)
    {
        if (uri.toString().startsWith("jar:")) {
//            JarURLConnection jarConn = (JarURLConnection) (uri.toURL().openConnection());
//            String entryName = jarConn.getEntryName();
//            String parentPath = "";
//
//            if (entryName != null) {
//                parentPath = (new File(entryName)).getParent();
//            }
//            String jarUrlStr = "jar:" + jarConn.getJarFileURL().toExternalForm() + "!/";
//            URL jarUrl = new URL(jarUrlStr);
//            URL parentUrl = null;
//            if (parentPath != null)
//                parentUrl = new URL(jarUrl, parentPath + "/");
//            else
//                parentUrl = jarUrl;
//            return parentUrl.toURI();
            return null; // TODO Unsupported yet
        } else {
            String parentPath = null;
            File baseFile = null;
            String path = uri.getPath();

            if (path != null) {
                baseFile = new File(path);
            } else {
                throw new RuntimeException("getPath() on URI \"" + uri + "\" returned null");
            }
            parentPath = baseFile.getParent();

            if (parentPath == null)
                return uri.buildUpon().path("../").build();

            if (!"/".equals(parentPath))
                return uri.buildUpon().path(parentPath + "/").build();

            return uri.buildUpon().path(parentPath.replace('\\', '/')).build();
        }
    }

    private static String getExtension (android.net.Uri uri)
    {
        String path = uri.getPath();
        int pos = path.lastIndexOf('.');

        if (pos >= 0)
            return path.substring(pos + 1, path.length());

        return new String("");
    }

    public static String getType (Context context, android.net.Uri uri)
    {
        String mimeType = null;
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            ContentResolver cr = context.getContentResolver();
            mimeType = cr.getType(uri);
        } else {
            String fileExtension = getExtension(uri).toLowerCase();

            if (!fileExtension.isEmpty())
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);
        }

        return mimeType == null ? "*/*" : mimeType;
    }

    // 1. [Sending the user to another app - Show an app chooser]
    //    (https://developer.android.com/training/basics/intents/sending#AppChooser)
    // 2. [Common intents]
    //    (https://developer.android.com/guide/components/intents-common)
    //
    private static boolean chooseFileViewer (Context context, CharSequence title, Intent intent)
    {
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> apps = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL);

        if (apps.isEmpty())
            return false;

        int i = 1;

        Say.d("Available activities for ACTION_VIEW:");

        for (ResolveInfo info: apps) {
            Say.d(String.format("\t%d. %s", i, info.activityInfo.packageName));
            i++;
        }

        context.startActivity(Intent.createChooser(intent, title));
        return true;
    }

    public static void chooseFileViewer (Context context, CharSequence title, android.net.Uri uri)
    {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        String mimeType = getType(context, uri);

        Say.d("VIEW URI: " + uri + "; MIME=" + mimeType);

        // Work right in this order.
        // Or use intent.setDataAndType(uri, mimeType);
        intent.setData(uri);
        intent.setType(mimeType);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        if (!chooseFileViewer(context, title, intent)) {
            Say.w(String.format("No any activities found for view '%s' file type"
                + ", will try default ('*/*') instead", mimeType));

            intent.setType("*/*");

            if (!chooseFileViewer(context, title, intent))
                Say.w("No any activities found for view document: " + uri);
        }
    }

    // NOTE! Required API >= 26
    // Not work properly on emulator.
    // TODO Not checked on the real device yet.
//    public static void openDocument (Context context, CharSequence title, android.net.Uri uri)
//    {
//        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
//        Uri folderUri = uri.getPath().endsWith("/") ? uri : UriUtils.getParentUri(uri);
//
//        intent.setData(uri);
//        //intent.setType("resource/folder"); // No viewer found
//        //intent.setType(DocumentsContract.Document.MIME_TYPE_DIR); // Open File manager
//        //intent.setType("vnd.android.cursor.dir/*"); // No viewer found
//        intent.setType("*/*");
//        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, folderUri);
//
//        Say.d("INITIAL FOLDER: " + folderUri);
//
//        PackageManager pm = context.getPackageManager();
//        List<ResolveInfo> apps = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL);
//
//        if (apps.size() > 0) {
//            int i = 1;
//            Say.d("Available activities for ACTION_OPEN_DOCUMENT:");
//            for (ResolveInfo info: apps) {
//                Say.d(String.format("\t%d. %s", i, info.activityInfo.packageName));
//                i++;
//            }
//
//            context.startActivity(Intent.createChooser(intent, title));
//        } else {
//            Say.w("No any activities found for ACTION_OPEN_DOCUMENT: " + uri);
//        }
//    }
}
