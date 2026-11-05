package android.content;
public interface SharedPreferences {
 boolean contains(String k); String getString(String k,String d); int getInt(String k,int d); long getLong(String k,long d); Editor edit();
 interface Editor { Editor putString(String k,String v); Editor putInt(String k,int v); Editor putLong(String k,long v); boolean commit(); }
}
