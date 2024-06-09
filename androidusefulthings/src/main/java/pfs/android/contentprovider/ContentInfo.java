////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2023 Vladislav Trifochkin
//
// This file is part of `Android support library`.
//
// Changelog:
//      2023.03.15 Initial version.
////////////////////////////////////////////////////////////////////////////////
package pfs.android.contentprovider;

public class ContentInfo
{
    public String uri;
    public String displayName;

    // Value returned by `getContentResolver().getType(uri)`
    // Examples:
    //      application/vnd.android.package-archive
    //      image/jpeg
    //      etc
    public String mimeType;
    public long size;
    public long modTime;
}
