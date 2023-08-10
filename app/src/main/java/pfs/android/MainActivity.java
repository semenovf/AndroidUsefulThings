package pfs.android;

import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity
{
    private Button _openFileButton;

    public static pfs.android.contentprovider.Bridge _contentProviderBridge = null;

    private OpenDocumentDialog _openDocumentDialog = new OpenDocumentDialog(this
        , new OpenDocumentDialog.Listener () {
                @Override
                public void onUriChosen (Uri uri)
                {
                    Say.dtoast(String.format("File selected: %s", uri.toString()));
                    pfs.android.contentprovider.ContentInfo contentInfo = _contentProviderBridge.getFileInfo(uri);
                    Say.d("Display name: " + contentInfo.displayName);
                }
        }
    );

    @Override
    protected void onCreate (Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Say.setContext(this);
        Say.resetPattern();
        _contentProviderBridge = pfs.android.contentprovider.Bridge.create(this);

        writeTestFilesToStorage();

        setContentView(R.layout.activity_main);

        _openFileButton = findViewById(R.id.open_file_button);
        _openFileButton.setOnClickListener(view -> {
            Say.dtoast(String.format("\"%s\" button clicked", getString(R.string.open_file_button)));
            _openDocumentDialog.launch("*/*");
        });

        EditText editText = findViewById(R.id.edit_text);
        // KeyboardListener keyboardListener = new KeyboardListener(this, R.id.root_view);
        // Or
        // KeyboardListener keyboardListener = new KeyboardListener(this);

        KeyboardObserver keyboardObserver = new KeyboardObserver() {
            @Override
            public void onKeyboardHeight(float height, int keyboardHeight, int keyboardY, int orientation) {
                Say.d(String.format("~~~ height=%f, keyboardHeight=%d, keyboardY=%d, orientation=%d"
                        , height, keyboardHeight, keyboardY, orientation));
            }
        };

        KeyboardProvider keyboardProvider = new KeyboardProvider(this, keyboardObserver);
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
}
