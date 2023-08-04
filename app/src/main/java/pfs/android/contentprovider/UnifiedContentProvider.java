package pfs.android.contentprovider;

import static android.os.Build.VERSION.SDK_INT;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsProvider;
import android.provider.DocumentsContract.Document;
import android.provider.DocumentsContract.Root;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class UnifiedContentProvider extends DocumentsProvider
{
    // Use these as the default columns to return information about a root if no specific
    // columns are requested in a query.
    private static final String[] DEFAULT_ROOT_PROJECTION = new String[] {
          Root.COLUMN_ROOT_ID
        , Root.COLUMN_MIME_TYPES
        , Root.COLUMN_FLAGS
        , Root.COLUMN_ICON
        , Root.COLUMN_TITLE
        , Root.COLUMN_SUMMARY
        , Root.COLUMN_DOCUMENT_ID
        //Root.COLUMN_AVAILABLE_BYTES
    };

    // Use these as the default columns to return information about a document if no specific
    // columns are requested in a query.
    private static final String[] DEFAULT_DOCUMENT_PROJECTION = new String[]{
          Document.COLUMN_DOCUMENT_ID
        , Document.COLUMN_MIME_TYPE
        , Document.COLUMN_DISPLAY_NAME
        , Document.COLUMN_LAST_MODIFIED
        , Document.COLUMN_FLAGS
        , Document.COLUMN_SIZE
    };

    private static final String ROOT = "pfs.android.contentprovider";

    private static final String DEFAULT_PROVIDER_TITLE = "Unified Content Provider";
    private static final String DEFAULT_PROVIDER_DESCRIPTION = "Unified Content Provider";

    private static final int DEFAULT_PROVIDER_ICON = R.mipmap.ic_launcher;

    private String _providerTitle = DEFAULT_PROVIDER_TITLE;
    private String _providerDesc = DEFAULT_PROVIDER_DESCRIPTION;

    //private Drawable _providerIcon = null;
    private int _providerIcon = DEFAULT_PROVIDER_ICON;

    private ArrayList<File> _topDirs = null;

    // A file object at the root of the file hierarchy.  Depending on your implementation, the root
    // does not need to be an existing file system directory.  For example, a tag-based document
    // provider might return a directory containing all tags, represented as child directories.
    private File _baseDir;

    private Say say;

    private void determineTopDirs (int arrayResId)
    {
        _baseDir = getContext().getFilesDir();
        String[] initialTopDirs = getContext().getResources().getStringArray(arrayResId);

        try {
            _topDirs = new ArrayList<File>();

            for (String dir : initialTopDirs) {
                File folder = new File(_baseDir + File.separator + dir).getAbsoluteFile();//.getCanonicalFile();

                if (!folder.exists()) {
                    say.e(String.format("Folder not exists: %s, item ignored: %s", folder, dir));
                    continue;
                }

                if (!folder.isDirectory()) {
                    say.e(String.format("Path must be a directory: %s, item ignored: %s", folder, dir));
                    continue;
                }

                _topDirs.add(folder);

                say.d("Added top directory: " + folder.getCanonicalFile());
            }
        } catch (Resources.NotFoundException e) {
            throw new RuntimeException("Expected 'provider_top_dirs' specified in AndroidManifest.xml for UnifiedContentProvider", e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean onCreate ()
    {
        say = new Say("~~~ UCP: %s", getContext());

        Bundle bundle;

        try {
            Context ctx = getContext();
            PackageManager pm = ctx.getPackageManager();
            ComponentName providerName = new ComponentName(ctx, this.getClass());
            ProviderInfo providerInfo = pm.getProviderInfo(providerName, PackageManager.GET_META_DATA);
            bundle = providerInfo.metaData;
            _providerTitle = bundle.getString("provider_title", DEFAULT_PROVIDER_TITLE);
            _providerDesc = bundle.getString("provider_description", DEFAULT_PROVIDER_DESCRIPTION);
            //_providerIcon = getContext().getResources().getDrawable(bundle.getInt("provider_icon"));
            _providerIcon = bundle.getInt("provider_icon", DEFAULT_PROVIDER_ICON);
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }

        determineTopDirs(bundle.getInt("provider_top_dirs"));

        // Emulator output
//        say.d("getFilesDir          : " + getContext().getFilesDir());             // /data/user/0/pfs.android.contentprovider/files
//        say.d("getCacheDir()        : " + getContext().getCacheDir());             // /data/user/0/pfs.android.contentprovider/cache
//        say.d("getCodeCacheDir()    : " + getContext().getCodeCacheDir());         // /data/user/0/pfs.android.contentprovider/code_cache
//        say.d("getDataDir()         : " + getContext().getDataDir());              // /data/user/0/pfs.android.contentprovider
//        say.d("getNoBackupFilesDir  : " + getContext().getNoBackupFilesDir());     // /data/user/0/pfs.android.contentprovider/no_backup
//        say.d("getObbDir()          : " + getContext().getObbDir());               // /storage/emulated/0/Android/obb/pfs.android.contentprovider
//        say.d("getExternalCacheDir(): " + getContext().getExternalCacheDir());     // /storage/emulated/0/Android/data/pfs.android.contentprovider/cache
//        say.d("getExternalFilesDir(): " + getContext().getExternalFilesDir(null)); // /storage/emulated/0/Android/data/pfs.android.contentprovider/files

        return true;
    }

    @Override
    public Cursor queryRoots (String[] projection)
    {
        say.d("queryRoots");

        // Create a cursor with either the requested fields, or the default projection.  This
        // cursor is returned to the Android system picker UI and used to display all roots from
        // this provider.
        final MatrixCursor result = new MatrixCursor(resolveRootProjection(projection));

        // It's possible to have multiple roots (e.g. for multiple accounts in the same app) -
        // just add multiple cursor rows.
        final MatrixCursor.RowBuilder row = result.newRow();

        row.add(Root.COLUMN_ROOT_ID, ROOT);

        // COLUMN_TITLE is the root title (e.g. what will be displayed to identify your provider).
        row.add(Root.COLUMN_TITLE, _providerTitle);

        // Set summary for output as provider description in providers list in dialog
        row.add(Root.COLUMN_SUMMARY, _providerDesc);

        // FLAG_SUPPORTS_CREATE means at least one directory under the root supports creating
        // documents.  FLAG_SUPPORTS_RECENTS means your application's most recently used
        // documents will show up in the "Recents" category.  FLAG_SUPPORTS_SEARCH allows users
        // to search all documents the application shares. FLAG_SUPPORTS_IS_CHILD allows
        // testing parent child relationships, available after SDK 21 (Lollipop).
        if (SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            row.add(Root.COLUMN_FLAGS, Root.FLAG_SUPPORTS_CREATE |
                    Root.FLAG_SUPPORTS_RECENTS |
                    Root.FLAG_SUPPORTS_SEARCH);
        } else {
            row.add(Root.COLUMN_FLAGS, Root.FLAG_SUPPORTS_CREATE |
                    Root.FLAG_SUPPORTS_RECENTS |
                    Root.FLAG_SUPPORTS_SEARCH |
                    Root.FLAG_SUPPORTS_IS_CHILD);
        }

        // This document id must be unique within this provider and consistent across time.  The
        // system picker UI may save it and refer to it later.
        row.add(Root.COLUMN_DOCUMENT_ID, ROOT);

        // The child MIME types are used to filter the roots and only present to the user roots
        // that contain the desired type somewhere in their file hierarchy.
        row.add(Root.COLUMN_MIME_TYPES, getChildMimeTypes(_baseDir));
        //row.add(Root.COLUMN_AVAILABLE_BYTES, _baseDir.getFreeSpace());
        //row.add(Root.COLUMN_ICON, R.mipmap.ic_launcher);
        //row.add(Root.COLUMN_ICON, android.R.drawable.ic_delete);
        row.add(Root.COLUMN_ICON, _providerIcon);

        say.d("queryRoots: result=" + result);

        return result;
    }

    @Override
    public Cursor queryDocument (String documentId, String[] projection) throws FileNotFoundException
    {
        // Create a cursor with the requested projection, or the default projection.
        final MatrixCursor result = new MatrixCursor(resolveDocumentProjection(projection));
        includeFile(result, documentId, null);
        return result;
    }

    @Override
    public Cursor queryChildDocuments (String parentDocumentId, String[] projection
            , String sortOrder) throws FileNotFoundException
    {
        say.d(String.format("queryChildDocuments: parentDocumentId=%s, sortOrder=%s", parentDocumentId, sortOrder));

        final MatrixCursor result = new MatrixCursor(resolveDocumentProjection(projection));
        final File parent = getFileForDocId(parentDocumentId);

        //if (parentDocumentId.equals(getDocIdForFile(_baseDir))) {
        if (parentDocumentId.equals(ROOT)) {
            for (File file: _topDirs)
                includeFile(result, null, file);
        } else {
            for (File file: parent.listFiles()) {
                includeFile(result, null, file);
            }
        }

        return result;
    }

    @Override
    public ParcelFileDescriptor openDocument (final String documentId
            , final String mode, CancellationSignal signal) throws FileNotFoundException
    {
        // It's OK to do network operations in this method to download the document, as long as you
        // periodically check the CancellationSignal.  If you have an extremely large file to
        // transfer from the network, a better solution may be pipes or sockets
        // (see ParcelFileDescriptor for helper methods).

        final File file = getFileForDocId(documentId);
        final int accessMode = ParcelFileDescriptor.parseMode(mode);

        final boolean isWrite = (mode.indexOf('w') != -1);

        if (isWrite) {
            // Attach a close listener if the document is opened in write mode.
            try {
                Handler handler = new Handler(getContext().getMainLooper());
                return ParcelFileDescriptor.open(file, accessMode, handler,
                        new ParcelFileDescriptor.OnCloseListener() {
                            @Override
                            public void onClose(IOException e) {}
                        });
            } catch (IOException e) {
                throw new FileNotFoundException(String.format("Failed to open document with id %s"
                    , " and mode '%s'", documentId, mode));
            }
        } else {
            return ParcelFileDescriptor.open(file, accessMode);
        }
    }

    @Override
    public AssetFileDescriptor openDocumentThumbnail (String documentId, Point sizeHint
        , CancellationSignal signal) throws FileNotFoundException
    {
        final File file = getFileForDocId(documentId);
        final ParcelFileDescriptor pfd =
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        return new AssetFileDescriptor(pfd, 0, AssetFileDescriptor.UNKNOWN_LENGTH);
    }

    public boolean isChildFile (File parentFile, File childFile)
    {
        File realFileParent = childFile.getParentFile();
        return realFileParent == null || realFileParent.equals(parentFile);
    }

    @Override
    public boolean isChildDocument (String parentDocumentId, String documentId)
    {
        try {
            File parentFile = getFileForDocId(parentDocumentId);
            File childFile = getFileForDocId(documentId);
            return isChildFile(parentFile, childFile);
        } catch (FileNotFoundException e) {
            say.e("FileNotFound in isChildDocument: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * @param projection the requested root column projection
     * @return either the requested root column projection, or the default projection if the
     * requested projection is null.
     */
    private static String[] resolveRootProjection (String[] projection)
    {
        return projection != null ? projection : DEFAULT_ROOT_PROJECTION;
    }

    private static String[] resolveDocumentProjection (String[] projection)
    {
        return projection != null ? projection : DEFAULT_DOCUMENT_PROJECTION;
    }

    /**
     * Get a file's MIME type
     *
     * @param file the File object whose type we want
     * @return the MIME type of the file
     */
    private static String getTypeForFile (File file)
    {
        if (file.isDirectory()) {
            return Document.MIME_TYPE_DIR;
        } else {
            return getTypeForName(file.getName());
        }
    }

    /**
     * Get the MIME data type of a document, given its filename.
     *
     * @param name the filename of the document
     * @return the MIME data type of a document
     */
    private static String getTypeForName (String name)
    {
        final int lastDot = name.lastIndexOf('.');

        if (lastDot >= 0) {
            final String extension = name.substring(lastDot + 1);
            final String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);

            if (mime != null)
                return mime;
        }

        return "application/octet-stream";
    }

    /**
     * Gets a string of unique MIME data types a directory supports, separated by newlines.  This
     * should not change.
     *
     * @param parent the File for the parent directory
     * @return a string of the unique MIME data types the parent directory supports
     */
    private String getChildMimeTypes (File parent)
    {
        Set<String> mimeTypes = new HashSet<String>();
        mimeTypes.add("image/*");
        mimeTypes.add("text/*");
        mimeTypes.add("application/vnd.openxmlformats-officedocument.wordprocessingml.document");

        // Flatten the list into a string and insert newlines between the MIME type strings.
        StringBuilder mimeTypesString = new StringBuilder();

        for (String mimeType : mimeTypes)
            mimeTypesString.append(mimeType).append("\n");

        return mimeTypesString.toString();
    }

    /**
     * Get the document ID given a File.  The document id must be consistent across time.  Other
     * applications may save the ID and use it to reference documents later.
     * <p/>
     * This implementation is specific to this demo.  It assumes only one root and is built
     * directly from the file structure.  However, it is possible for a document to be a child of
     * multiple directories (for example "android" and "images"), in which case the file must have
     * the same consistent, unique document ID in both cases.
     *
     * @param file the File whose document ID you want
     * @return the corresponding document ID
     */
    private String getDocIdForFile (File file)
    {
        String path = file.getAbsolutePath();

        say.d(String.format("getDocIdForFile: file=%s, path=%s", file, path));

        // Start at first char of path under root
        final String rootPath = _baseDir.getPath();

        if (rootPath.equals(path)) {
            path = "";
        } else if (rootPath.endsWith("/")) {
            path = path.substring(rootPath.length());
        } else {
            path = path.substring(rootPath.length() + 1);
        }

        return ROOT + ':' + path;
    }

    /**
     * Add a representation of a file to a cursor.
     *
     * @param result the cursor to modify
     * @param docId  the document ID representing the desired file (may be null if given file)
     * @param file   the File object representing the desired file (may be null if given docID)
     * @throws FileNotFoundException
     */
    private void includeFile (MatrixCursor result, String docId, File file)
            throws FileNotFoundException
    {
        say.d(String.format("includeFile: docId=%s, file:%s", docId, file));

        if (docId == null) {
            docId = getDocIdForFile(file);
        } else {
            file = getFileForDocId(docId);
        }

        int flags = 0;

        if (file.isDirectory()) {
            // Request the folder to lay out as a grid rather than a list. This also allows a larger
            // thumbnail to be displayed for each image.
            //            flags |= Document.FLAG_DIR_PREFERS_GRID;

            // Add FLAG_DIR_SUPPORTS_CREATE if the file is a writable directory.
            if (file.isDirectory() && file.canWrite()) {
                flags |= Document.FLAG_DIR_SUPPORTS_CREATE;
            }
        } else if (file.canWrite()) {
            // If the file is writable set FLAG_SUPPORTS_WRITE and
            // FLAG_SUPPORTS_DELETE
            flags |= Document.FLAG_SUPPORTS_WRITE;
            flags |= Document.FLAG_SUPPORTS_DELETE;

            // Add SDK specific flags if appropriate
            if (SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                flags |= Document.FLAG_SUPPORTS_RENAME;
            }

            if (SDK_INT >= Build.VERSION_CODES.N) {
                flags |= Document.FLAG_SUPPORTS_REMOVE;
                flags |= Document.FLAG_SUPPORTS_MOVE;
                flags |= Document.FLAG_SUPPORTS_COPY;
            }
        }

        final String displayName = file.getName();
        final String mimeType = getTypeForFile(file);

        if (mimeType.startsWith("image/")) {
            // Allow the image to be represented by a thumbnail rather than an icon
            flags |= Document.FLAG_SUPPORTS_THUMBNAIL;
        }

        final MatrixCursor.RowBuilder row = result.newRow();
        row.add(Document.COLUMN_DOCUMENT_ID, docId);
        row.add(Document.COLUMN_DISPLAY_NAME, displayName);
        row.add(Document.COLUMN_SIZE, file.length());
        row.add(Document.COLUMN_MIME_TYPE, mimeType);
        row.add(Document.COLUMN_LAST_MODIFIED, file.lastModified());
        row.add(Document.COLUMN_FLAGS, flags);

        // Add a custom icon
        //row.add(Document.COLUMN_ICON, R.drawable.ic_launcher);
        row.add(Document.COLUMN_ICON, R.mipmap.ic_launcher);
    }

    /**
     * Translate your custom URI scheme into a File object.
     *
     * @param docId the document ID representing the desired file
     * @return a File represented by the given document ID
     * @throws java.io.FileNotFoundException
     */
    private File getFileForDocId (String docId) throws FileNotFoundException
    {
        say.d(String.format("getFileForDocId: docId=%s", docId));

        File target = _baseDir;

        if (docId.equals(ROOT))
            return target;

        final int splitIndex = docId.indexOf(':', 1);

        if (splitIndex < 0) {
            throw new FileNotFoundException("Missing root for " + docId);
        } else {
            final String path = docId.substring(splitIndex + 1);
            target = new File(target, path);
            if (!target.exists()) {
                throw new FileNotFoundException("Missing file for " + docId + " at " + target);
            }
            return target;
        }
    }
}

