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
import android.view.View;

import java.util.HashMap;
import java.util.Vector;

public class PermissionsRequester
{
    private static final int STATUS_INITIAL = -2;
    private static final int STATUS_DENIED = -1;
    private static final int STATUS_REQUESTED = 0;
    private static final int STATUS_GRANTED = 1;

    private class Permission
    {
        public String name;
        public int status = STATUS_INITIAL;
    }

    private Activity _activity;
    private Vector<Permission> _permissions;
    private HashMap<String, Integer> _permissionsMap;
    private int _requestCode = 0;

    public interface OnPermissionResult
    {
        public void on (String name, boolean isGranted);
    };

    public OnPermissionResult onPermissionResult;

    /**
     *
     * @param activity Activity.
     * @param permissions Requested permissions.
     * @param requestCode Request code.
     */
    public PermissionsRequester(@NonNull Activity activity, int requestCode, String[] permissions)
    {
        _activity = activity;
        _requestCode = requestCode;
        _permissions = new Vector<Permission>();
        _permissionsMap = new HashMap<String, Integer>();

        for (int i = 0; i < permissions.length; i++) {
            Permission perm = new Permission();
            perm.name = permissions[i];
            _permissions.add(perm);
            _permissionsMap.put(perm.name, i);
        }
    }

    public boolean isGranted (String permName)
    {
        for (Permission perm: _permissions) {
            if (perm.name.equals(permName)) {
                return perm.status == STATUS_GRANTED;
            }
        }

        throw new IllegalArgumentException(String.format("Permission not found: %s", permName));
    }

    /**
     * Checks if permission is granted or requests the permission otherwise.
     *
     * @return Status code of the request operation (see
     */
    public void request ()
    {
        Vector<String> requestedPermissions = new Vector<String>();

        for (Permission perm: _permissions) {
            boolean alreadyGranted = _activity.checkSelfPermission(perm.name) == PackageManager.PERMISSION_GRANTED;

            if (!alreadyGranted) {
                // When permission is not granted by user, show them message why this permission is needed.
                if (_activity.shouldShowRequestPermissionRationale(perm.name)) {
                    Say.d(String.format("Please grant permission: %s", perm.name));
                    requestedPermissions.add(perm.name);
                } else {
                    // TODO: Show user dialog to grant permission
                    Say.d(String.format("Show user dialog for grant permission: %s", perm.name));

                    requestedPermissions.add(perm.name);
                }

                perm.status = STATUS_REQUESTED;
            } else {
                perm.status = STATUS_GRANTED;

                if (onPermissionResult != null)
                    onPermissionResult.on(perm.name, true);
            }
        }
        if (!requestedPermissions.isEmpty()) {
            String[] permissions = new String[requestedPermissions.size()];
            requestedPermissions.toArray(permissions);
            _activity.requestPermissions(permissions, _requestCode);
        }
    }

    /**
     *
     * @param permissions
     * @param grantResults
     * @return @c true if arguments contains data for requested permission, @c false otherwise.
     */
    public void resultCallback (String permissions[], int[] grantResults) {
        for (int i = 0, count = permissions.length; i < count; i++) {
            String name = permissions[i];
            Integer index = _permissionsMap.getOrDefault(permissions[i], -1);
            boolean isGranted = false;

            if (index < 0) {
                Say.e(String.format("Permission not found in requester: %s (request code=%d), ignored"
                    , name, _requestCode));
                continue;
            }

            if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                _permissions.get(index).status = STATUS_GRANTED;
                isGranted = true;
            } else {
                _permissions.get(index).status = STATUS_DENIED;
            }

            if (onPermissionResult != null)
                onPermissionResult.on(name, isGranted);
        }
    }
}
