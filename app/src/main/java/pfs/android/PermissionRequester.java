////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2023 Vladislav Trifochkin
//
// This file is part of `Android support library`.
//
// Changelog:
//      2023.12.25 Initial version.
////////////////////////////////////////////////////////////////////////////////
package pfs.android;

import android.app.Activity;
import android.content.pm.PackageManager;
import pfs.android.NonNull;
import pfs.android.Say;

public class PermissionRequester
{
    private Activity _activity = null;
    private String _permission = null;
    private int _requestCode = 0;

    private static final int STATUS_INITIAL = -2;
    private static final int STATUS_DENIED = -1;
    private static final int STATUS_REQUESTED = 0;
    private static final int STATUS_GRANTED = 1;

    private int _status = STATUS_INITIAL;

    /**
     *
     * @param activity Activity.
     * @param permission Requested permission.
     * @param requestCode Request code.
     */
    public PermissionRequester (@NonNull Activity activity, String permission, int requestCode)
    {
        _activity = activity;
        _permission = permission;
        _requestCode = requestCode;
    }

    public boolean isGranted ()
    {
        return _status == STATUS_GRANTED;
    }

    /**
     * Checks if permission is granted or requests the permission otherwise.
     *
     * @return Status code of the request operation (see
     */
    public void request ()
    {
        boolean alreadyGranted = _activity.checkSelfPermission(_permission) == PackageManager.PERMISSION_GRANTED;

        if (!alreadyGranted) {
            // When permission is not granted by user, show them message why this permission is needed.
            if (_activity.shouldShowRequestPermissionRationale(_permission)) {
                Say.d(String.format("Please grant permission: %s", _permission));

                // Give user option to still opt-in the permissions
                _activity.requestPermissions(new String[]{_permission}, _requestCode);
            } else {
                // Show user dialog to grant permission
                _activity.requestPermissions(new String[]{_permission}, _requestCode);
            }

            _status = STATUS_REQUESTED;
        } else {
            _status = STATUS_GRANTED;
            Say.d(String.format("Permission already granted: %s", _permission));
        }
    }

    /**
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     * @return @c true if arguments contains data for requested permission, @c false otherwise.
     */
    public boolean resultCallback (int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == _requestCode) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                _status = STATUS_GRANTED;
                Say.d(String.format("Permission granted: %s", _permission));
            } else {
                _status = STATUS_DENIED;
                Say.e(String.format("Permission denied: %s", _permission));
            }

            return true;
        }

        return false;
    }
}
