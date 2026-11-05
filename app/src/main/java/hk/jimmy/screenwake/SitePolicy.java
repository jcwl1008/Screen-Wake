package hk.jimmy.screenwake;

import java.net.URI;

final class SitePolicy {
    static String normalize(String value) throws Exception {
        value=value.trim(); if(!value.contains("://")) value="https://"+value;
        URI uri=new URI(value);
        if(!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost()==null || uri.getUserInfo()!=null
            || (uri.getPort()!=-1 && (uri.getPort()<1 || uri.getPort()>65535))) throw new Exception("Invalid HTTPS URL");
        return uri.toASCIIString();
    }
    static boolean allows(String baseUrl,String targetUrl) {
        try {
            URI base=new URI(normalize(baseUrl)), target=new URI(targetUrl);
            int basePort=base.getPort()==-1 ? 443 : base.getPort(), targetPort=target.getPort()==-1 ? 443 : target.getPort();
            return "https".equalsIgnoreCase(target.getScheme()) && target.getHost()!=null
                && base.getHost().equalsIgnoreCase(target.getHost()) && target.getUserInfo()==null && basePort==targetPort;
        } catch(Exception e) { return false; }
    }
}
