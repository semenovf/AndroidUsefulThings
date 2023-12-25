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
    private boolean _granted = false;

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
        return _granted;
    }
    /**
     * Checks if permission is granted or requests the permission otherwise.
     *
     * @return @c true if requested permission already granted or @c false if need to request the
     *         permission
     */
    public boolean request ()
    {
        boolean alreadyGranted = _activity.checkSelfPermission(_permission) == PackageManager.PERMISSION_GRANTED;

        if (!alreadyGranted) {
            //When permission is not granted by user, show them message why this permission is needed.
            if (_activity.shouldShowRequestPermissionRationale(_permission)) {
                Say.d(String.format("Please grant permission: %s", _permission));

                // Give user option to still opt-in the permissions
                _activity.requestPermissions(new String[]{_permission}, _requestCode);
            } else {
                // Show user dialog to grant permission
                _activity.requestPermissions(new String[]{_permission}, _requestCode);
            }

            return false;
        }

        _granted = true;
        Say.d(String.format("Permission already granted: %s", _permission));

        return true;
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
                _granted = true;
                Say.d(String.format("Permission granted: %s", _permission));
            } else {
                _granted = false;
                Say.e(String.format("Permission denied: %s", _permission));
            }

            return true;
        }

        return false;
    }
}
