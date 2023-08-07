package pfs.android.contentprovider;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

public class OpenDocumentDialog
{
    private ActivityResultLauncher<Intent> _openDocumentResult = null;

    public interface Listener {
        public abstract void onUriChosen (Uri uri);
    }

    public OpenDocumentDialog (ComponentActivity activity, Listener listener)
    {
        // Need to call `registerForActivityResult()` before the fragment or activity is created.
        // You cannot launch the ActivityResultLauncher until the fragment or activity's Lifecycle
        // has reached CREATED.
        _openDocumentResult = activity.registerForActivityResult(
              new ActivityResultContracts.StartActivityForResult()
            , result -> {
                if (result.getResultCode() == Activity.RESULT_CANCELED)
                    return;

                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent intent = result.getData();
                    Uri uri = intent.getData();

                    if (listener != null)
                        listener.onUriChosen(uri);
                }
            });
    }

    public void launch (String mimeTypePattern)
    {
        Intent openDocumentIntent = new Intent();
        openDocumentIntent.addCategory(Intent.CATEGORY_OPENABLE);
        openDocumentIntent.setAction(Intent.ACTION_OPEN_DOCUMENT);
        openDocumentIntent.setType(mimeTypePattern);
        _openDocumentResult.launch(openDocumentIntent);
    }
}
