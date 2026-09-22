package android.net;

import android.os.Parcel;

public class TestUri extends Uri {
    private final String uriString;

    public TestUri(String uriString) {
        this.uriString = uriString;
    }

    @Override
    public boolean isHierarchical() {
        return true;
    }

    @Override
    public boolean isRelative() {
        return false;
    }

    @Override
    public String getScheme() {
        return "content";
    }

    @Override
    public String getSchemeSpecificPart() {
        return uriString;
    }

    @Override
    public String getEncodedSchemeSpecificPart() {
        return uriString;
    }

    @Override
    public String getAuthority() {
        return "test";
    }

    @Override
    public String getEncodedAuthority() {
        return "test";
    }

    @Override
    public String getUserInfo() {
        return null;
    }

    @Override
    public String getEncodedUserInfo() {
        return null;
    }

    @Override
    public String getHost() {
        return "test";
    }

    @Override
    public int getPort() {
        return -1;
    }

    @Override
    public String getPath() {
        return "/test";
    }

    @Override
    public String getEncodedPath() {
        return "/test";
    }

    @Override
    public String getQuery() {
        return null;
    }

    @Override
    public String getEncodedQuery() {
        return null;
    }

    @Override
    public String getFragment() {
        return null;
    }

    @Override
    public String getEncodedFragment() {
        return null;
    }

    @Override
    public java.util.List<String> getPathSegments() {
        return java.util.Collections.singletonList("test");
    }

    @Override
    public String getLastPathSegment() {
        return "test";
    }

    @Override
    public Builder buildUpon() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {}

    @Override
    public String toString() {
        return uriString;
    }

    @Override
    public int compareTo(Uri o) {
        return 0;
    }
}
