package pfs.android.app;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import pfs.android.app.databinding.ActivityMainBinding;

import pfs.android.IntentDumper;
import pfs.android.KeyboardProvider;
import pfs.android.MessageBox;
import pfs.android.OpenDocumentDialog;
import pfs.android.PermissionsRequester;
import pfs.android.Say;
import pfs.android.UriUtils;
import pfs.android.contentprovider.UnifiedContentProvider;
import pfs.android.daemon.Daemon;

public class MainActivity extends AppCompatActivity
{
    KeyboardProvider _keyboardProvider;
    private Button _openFileButton;
    private Button _openFolderButton;
    private Uri _lastUriChosen;

    // Set to true if need to force full screen mode
    private boolean _fullscreenMode = false;

    public static pfs.android.contentprovider.Bridge _contentProviderBridge = null;

    private static final int GRANT_REQUIRED_PERMISSIONS = 1001;
    private PermissionsRequester _permissionsRequester;

    private OpenDocumentDialog _openDocumentDialog = new OpenDocumentDialog(this
        , new OpenDocumentDialog.Listener () {
                @Override
                public void onUriChosen (Uri uri)
                {
                    Say.dtoast(String.format("File selected: %s", uri.toString()));
                    pfs.android.contentprovider.ContentInfo contentInfo = _contentProviderBridge.getFileInfo(uri);
                    Say.d("Display name: " + contentInfo.displayName);

                    MessageBox.showInfo(getFragmentManager(), "MESSAGE_BOX"
                        , "File chosen", uri.toString());

                    _lastUriChosen = uri;

                    if (_openFolderButton != null) {
                        _openFolderButton.setEnabled(true);
                        _openFolderButton.setText(contentInfo.displayName);
                    }
                }
        });

    private boolean startDaemon ()
    {
        int rc = Daemon.start(this, MainActivity.class);

        if (rc != Daemon.NO_ERROR) {
            String message = Daemon.errorString(rc);

            switch (rc) {
                case Daemon.ERROR_NOTIFICATIONS_DISABLED:
                    message = message + ".\nEnable them using Settings for this application.";
                    break;
                default:
                    break;
            }

            MessageBox.showAlert(getFragmentManager(), "MESSAGE_BOX"
                , getResources().getString(R.string.startDaemonFailure), message);

            return false;
        }

        return true;
    }

    private void stopDaemon ()
    {
        Daemon.stop(this);
    }

    private void finishActivity ()
    {
        this.finish();
    }

    @Override
    protected void onCreate (Bundle savedInstanceState)
    {
        Say.d("Create DaemonActivator: " + IntentDumper.toString(savedInstanceState));

        super.onCreate(savedInstanceState);

        String[] permissions = new String[] {
              android.Manifest.permission.ACCESS_NETWORK_STATE
            , android.Manifest.permission.RECORD_AUDIO
            , android.Manifest.permission.ACCESS_FINE_LOCATION
            , android.Manifest.permission.ACCESS_COARSE_LOCATION
            , android.Manifest.permission.INTERNET
            , android.Manifest.permission.RECORD_AUDIO
        };

        _permissionsRequester = new PermissionsRequester(this, GRANT_REQUIRED_PERMISSIONS, permissions);
        _permissionsRequester.onPermissionResult = new PermissionsRequester.OnPermissionResult() {
            @Override
            public void on (String name, boolean isGranted) {
                Say.d(String.format("Permission %s: %s", name, isGranted ? "GRANTED" : "DENIED"));
            }
        };
        _permissionsRequester.request();

        Say.setContext(this);
        Say.resetPattern();
        Say.setTraceLevel(3);

        _contentProviderBridge = pfs.android.contentprovider.Bridge.create(this);

        writeTestFilesToStorage();

        // Set fullscreen mode
        if (_fullscreenMode) {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                   WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        //setContentView(R.layout.activity_main);
        setContentView(binding.getRoot());

        //_openFolderButton = findViewById(R.id.open_folder_button);
        _openFolderButton = binding.openFolderButton;

        _openFolderButton.setEnabled(false);
        _openFolderButton.setOnClickListener(view -> {
            if (_lastUriChosen != null && _lastUriChosen != Uri.EMPTY) {
                switch (_lastUriChosen.getScheme()) {
                    case ContentResolver.SCHEME_CONTENT:
                        UriUtils.chooseFileViewer(this, getString(R.string.open_file), _lastUriChosen);
                        break;
                    case ContentResolver.SCHEME_FILE:
                        File file;
                        String providerAuthority;
                        try {
                            file = new File(_lastUriChosen.getPath());
                            PackageManager pm = this.getPackageManager();
                            ProviderInfo providerInfo = pm.getProviderInfo(new ComponentName(this, UnifiedContentProvider.class), 0);
                            providerAuthority = providerInfo.authority;
                        } catch (PackageManager.NameNotFoundException e) {
                            throw new RuntimeException(e);
//                        } catch (IOException e) {
//                            throw new RuntimeException(e);
                        }

                        UnifiedContentProvider.Caller caller = new UnifiedContentProvider.Caller(this.getContentResolver());
                        Uri uri = caller.getUriFromFile(providerAuthority, file);

                        if (uri != null && !uri.equals(Uri.EMPTY)) {
                            UriUtils.chooseFileViewer(this, getString(R.string.open_file), uri);
                        } else {
                            Say.e("Unable to handle file: " + _lastUriChosen);
                        }

                        break;
                    default:
                        Say.e("Unsupported URI scheme: " + _lastUriChosen.getScheme());
                        break;
                }
            } else {
                Say.dtoast(String.format("\"%s\" button clicked", getString(R.string.no_file_chosen)));
            }
        });

        //_openFileButton = findViewById(R.id.open_file_button);
        _openFileButton = binding.openFileButton;
        _openFileButton.setOnClickListener(view -> {
            Say.dtoast(String.format("\"%s\" button clicked", getString(R.string.open_file_button)));
            _openDocumentDialog.launch("*/*");
        });

        //ViewGroup rootViewGroup = findViewById(R.id.root_view);
        ViewGroup rootViewGroup = binding.rootView;

        //EditText editText = findViewById(R.id.edit_text);
        EditText editText = binding.editText;

        Button startDaemonButton = binding.startDaemonButton;
        startDaemonButton.setText(R.string.startDaemon);
        startDaemonButton.setOnClickListener(new View.OnClickListener () {
            @Override
            public void onClick (View v) {
                boolean startRequired = startDaemonButton.getText().toString()
                    .equals(getString(R.string.startDaemon));

                if (startRequired) {
                    boolean success = startDaemon();

                    if (success) {
                        startDaemonButton.setText(R.string.stopDaemon);
                        //finishActivity();
                    }
                } else {
                    stopDaemon();
                    startDaemonButton.setText(R.string.startDaemon);
                }
            }
        });

        KeyboardProvider.KeyboardObserver keyboardObserver = new KeyboardProvider.KeyboardObserver() {
            @SuppressLint("DefaultLocale")
            @Override
            public void onKeyboardGeometry(KeyboardProvider.KeyboardGeometry geom)
            {
                Say.d(String.format("GEOMETRY READY:\n"
                    + "\tdisplay resolution=%d x %d\n"
                    + "\tviewAreaTop=%d\n"
                    + "\tviewAreaBottom=%d\n"
                    + "\tkeyboardHeight=%d\n"
                    + "\tkeyboardY=%d\n"
                    + "\torientation=%d\n"
                    + "\tnav bar height=%d\n"
                    + "\tstatus bar height=%d\n"
                    + "\tdensity=%f\n"
                    , geom.displayResolution.x, geom.displayResolution.y
                    , geom.viewAreaTop, geom.viewAreaBottom
                    , geom.keyboardHeight, geom.keyboardY, geom.orientation
                    , geom.navBarHeight
                    , geom.statusBarHeight
                    , geom.density));

                ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) editText.getLayoutParams();
                p.setMargins(p.leftMargin, p.topMargin, p.rightMargin
                    //, geom.displayResolution.y - geom.keyboardY - geom.navBarHeight/* - geom.statusBarHeight*/);
                    , geom.keyboardHeight);
                editText.requestLayout();
            }
        };

        _keyboardProvider = new KeyboardProvider(this, keyboardObserver);
    }

    @Override
    protected void onNewIntent (Intent intent)
    {
        Say.d("------ New intent -------");
        super.onNewIntent(intent);
    }

    @Override
    protected void onDestroy ()
    {
        Say.d("Destroy DaemonActivator");
        super.onDestroy();

        // Avoid exception:
        // Activity pfs.android.MainActivity has leaked window ...
        if (_keyboardProvider != null)
            _keyboardProvider.dismiss();
    }

    /**
     * Preload sample files packaged in the apk into the internal storage directory.  This is a
     * test function specific to this demo.  The MyCloud mock cloud service doesn't actually
     * have a backend, so it simulates by reading content from the device's internal storage.
     */
    private void writeTestFilesToStorage ()
    {
        File baseDir = this.getFilesDir();

        if (baseDir.list().length > 0)
            return;

        int[] imageResIds = getResourceIdArray(R.array.image_res_ids);
        String[] topSubDirs = new String[] {".", "Folder1", "Folder2", "Folder3", "../OutsideFolder"};

        for (String subdir: topSubDirs) {
            File folder = new File(baseDir, subdir);
            folder.mkdirs();

            for (int resId : imageResIds)
                writeFileToInternalStorage(folder, resId, ".jpeg");

            int[] textResIds = getResourceIdArray(R.array.text_res_ids);

            for (int resId : textResIds)
                writeFileToInternalStorage(folder, resId, ".txt");

            int[] docxResIds = getResourceIdArray(R.array.docx_res_ids);

            for (int resId : docxResIds)
                writeFileToInternalStorage(folder, resId, ".docx");

            int[] pdfResIds = getResourceIdArray(R.array.pdf_res_ids);

            for (int resId : pdfResIds)
                writeFileToInternalStorage(folder, resId, ".pdf");
        }
    }

    /**
     * Write a file to internal storage.  Used to set up our simple "cloud server".
     *
     * @param resId     the resource ID of the file to write to internal storage
     * @param extension the file extension (ex. .png, .mp3)
     */
    private void writeFileToInternalStorage (File parentFolder, int resId, String extension)
    {
        InputStream ins = this.getResources().openRawResource(resId);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int size;
        byte[] buffer = new byte[1024];
        try {
            while ((size = ins.read(buffer, 0, 1024)) >= 0) {
                outputStream.write(buffer, 0, size);
            }
            ins.close();
            buffer = outputStream.toByteArray();
            String filename = parentFolder + File.separator + this.getResources().getResourceEntryName(resId) + extension;
            //FileOutputStream fos = this.openFileOutput(filename, Context.MODE_PRIVATE);
            FileOutputStream fos = new FileOutputStream(filename);
            fos.write(buffer);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int[] getResourceIdArray (int arrayResId)
    {
        TypedArray ar = this.getResources().obtainTypedArray(arrayResId);
        int len = ar.length();
        int[] resIds = new int[len];
        for (int i = 0; i < len; i++) {
            resIds[i] = ar.getResourceId(i, 0);
        }
        ar.recycle();
        return resIds;
    }

//    @Override
//    public void onRequestPermissionsResult (int requestCode, String permissions[], int[] grantResults) {
//        switch (requestCode) {
//            case GRANT_RECORD_AUDIO: {
//                if (_audioRecordRequester.resultCallback(requestCode, permissions, grantResults)) {
//                    // Processed by this callback
//                    if (_audioRecordRequester.isGranted()) {
//                        Say.d("RECORD_AUDIO is granted");
//                    } else {
//                        Say.d("RECORD_AUDIO denied");
//                    }
//                }
//            }
//        }
//    }
    @Override
    public void onRequestPermissionsResult (int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult (requestCode, permissions, grantResults);

        switch (requestCode) {
            case GRANT_REQUIRED_PERMISSIONS:
                _permissionsRequester.resultCallback(permissions, grantResults);
                break;
            default:
                Say.w(String.format("Unhandled permission request code: %d, ignored", requestCode));
                break;
        }
    }
}
