package hk.jimmy.screenwake;
import android.content.SharedPreferences;
import java.util.HashMap;
final class PasswordStoreTest {
 static class Memory implements SharedPreferences,SharedPreferences.Editor {
  final HashMap<String,Object> data=new HashMap<>();
  public boolean contains(String k){return data.containsKey(k);}
  public String getString(String k,String d){return (String)data.getOrDefault(k,d);}
  public int getInt(String k,int d){return (Integer)data.getOrDefault(k,d);}
  public long getLong(String k,long d){return (Long)data.getOrDefault(k,d);}
  public SharedPreferences.Editor edit(){return this;}
  public SharedPreferences.Editor putString(String k,String v){data.put(k,v);return this;}
  public SharedPreferences.Editor putInt(String k,int v){data.put(k,v);return this;}
  public SharedPreferences.Editor putLong(String k,long v){data.put(k,v);return this;}
  public boolean commit(){return true;}
 }
 static void check(boolean b){if(!b)throw new AssertionError();}
 public static void main(String[] args)throws Exception {
  Memory m=new Memory(); PasswordStore store=new PasswordStore(m); check(!store.exists());
  char[] secret="TestingOnly938!".toCharArray(); store.save(secret); check(store.exists());
  check(!m.getString("hash","").contains("TestingOnly938!"));
  check(new PasswordStore(m).check(secret));
  for(int i=0;i<5;i++)check(!store.check("incorrect".toCharArray()));
  check(store.waitSeconds()>0); check(!new PasswordStore(m).check(secret));
  m.putLong("blockedUntil",0); check(store.check(secret)); check(m.getInt("failures",-1)==0);
  String salt=m.getString("salt",""),hash=m.getString("hash",""); store.save(secret);
  check(!salt.equals(m.getString("salt",""))); check(!hash.equals(m.getString("hash","")));
  check(!store.check("incorrect".toCharArray())); check(store.check(secret));
  System.out.println("PASS: password persistence, salted hashing, failures, cooldown and recovery");
 }
}
