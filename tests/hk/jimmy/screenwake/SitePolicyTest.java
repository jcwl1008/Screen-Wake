package hk.jimmy.screenwake;
public final class SitePolicyTest {
 public static void main(String[] args) throws Exception {
  String base="https://example.com/start";
  for(String u:new String[]{"https://example.com/other","https://example.com:443/path#2","HTTPS://EXAMPLE.COM/?q=test"}) {
   if(!SitePolicy.allows(base,u)) throw new AssertionError("Should allow "+u);
  }
  for(String u:new String[]{"https://evil.com/","https://example.com.evil.com/","https://www.example.com/","http://example.com/","https://example.com:444/","https://a@example.com/","file:///etc/passwd","intent://example.com/","javascript:alert(1)","https://example.com\\@evil.com/"}) {
   if(SitePolicy.allows(base,u)) throw new AssertionError("Must block "+u);
  }
  if(!SitePolicy.normalize(" example.com/path ").equals("https://example.com/path")) throw new AssertionError();
  for(String u:new String[]{"","http://example.com/","https://a@example.com/","https://example.com:0/","https://example.com:65536/","https://ex ample.com/"}) {
   boolean rejected=false; try { SitePolicy.normalize(u); } catch(Exception e) { rejected=true; }
   if(!rejected) throw new AssertionError("Accepted invalid URL "+u);
  }
  System.out.println("PASS: same-origin navigation and URL validation (20 cases)");
 }
}
