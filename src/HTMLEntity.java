import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by sxh112430 on 4/12/15.
 */
public class HTMLEntity {

    public static final String[] uncloseableTags = {"meta", "input","img", "body", "colgroup", "dd", "dt","head","html","li","optgroup","option","p","tbody","td","tfoot","th","thead","tr"};

    public static final int TEXT = 0;
    public static final int IMAGE = 1;
    /* potentially unused */
    public static final int ANCHOR = 2;
    public static final int ANCHOR_IMAGE = 3;

    public String getTagString() {
        return tagString;
    }

    private String urlPrefix;
    private String rootUrl;
    private String directoryUrl;
    private int typeID;
    private Map<String, String> attributes = new HashMap<String, String>();
    private String tagString;
    private String id;
    private String body;

    public String getImageSrc() {
        return imageSrc;
    }

    private String imageSrc = "null";
    private BufferedImage image;

    private boolean closed = false;

    public void closeEntity() {
        this.closed = true;
    }

    public void appendToBody(String bodyAppend) {
        this.body += bodyAppend;
    }

    public boolean isClosed() {
        return closed;
    }

    public HTMLEntity(String urlPrefix, String id, String tag, String body, String[] attributes) {
        typeID = TEXT;
        image = null;

        if (tag.equals("img")) {
            typeID = IMAGE;
        }

        if(urlPrefix.startsWith("http://") || urlPrefix.startsWith("https://")) {
            urlPrefix = urlPrefix.substring(7);
            if(urlPrefix.startsWith("/")) {
                urlPrefix = urlPrefix.substring(1);
            }
        }
        rootUrl = urlPrefix;
        if(urlPrefix.contains("/")) {
            rootUrl = urlPrefix.substring(0,rootUrl.indexOf("/"));
        }

        directoryUrl = urlPrefix;
        if(!directoryUrl.endsWith("/")) {
            if(directoryUrl.contains("/")) {
                int rIndex = directoryUrl.lastIndexOf("/");
                directoryUrl = directoryUrl.substring(0, rIndex+1);
            } else {
                directoryUrl+= "/";
            }
        }

        this.id = id;
        this.tagString = tag;
        this.body = body;
        this.urlPrefix = urlPrefix;

        for(String s:attributes) {
            String key = s;
            String value = "true";
            if(s.contains("=")) {
                int eqIdx = s.indexOf('=');
                key = s.substring(0, eqIdx);
                value = s.substring(eqIdx + 1, s.length());
            }
            key = key.trim();
            value = value.trim();

            if (key.equals("src")) {
                // make the image source usable
                value = value.replaceAll("'", "");
                value = value.replaceAll("\"","");
                if(!value.startsWith("http://") && !value.startsWith("https://")) {
                    // imgSrc is relative, make it non-relative so we can get the image
                    if(value.startsWith("/")){
                        // this is a non-rel path, but they didn't use the http:// start string b/c they suck at writing websites
                        value = rootUrl + value;
                    } else {
                        // this is relative
                        value = directoryUrl + value;
                    }
                }

                imageSrc = value;
            }

            if (key.equals("/")) {
                closed = true;
                continue; // don't add this to prop list, it's just a close e.g. "/>"
            }

            this.addAttribute(key, value);
        }

        // Special, weird rules for meta tags:
        // source: http://webdesign.about.com/od/htmltags/qt/optional-html-end-tags-when-to-include-them.htm
        for(String s:uncloseableTags) {
            if(this.getTagString().equals(s)) {
                this.closeEntity();
            }
        }
        // in case tags look like this: <br/>
        if(tag.endsWith("/")) {
            closed = true;
        }
    }

    public int getTypeID() {
        return typeID;
    }

    public void addAttribute(String key, String value) {
        attributes.put(key, value);
    }

    @Override
    public String toString() {
        String ts;
        if (typeID == IMAGE){
            ts = "image";
        } else if (typeID ==TEXT) {
            ts = "text";
        } else {
            ts = ""+ typeID;
        }
        String ret ="HTMLEntity:\n";
        ret+="\tid:"+id+",\n\ttagString:"+ tagString +",\n\ttypeID:"+ts+",\n\tcontent: "+body.substring(0,Math.min(20, body.length()))+"...,\n\tattr: {";

        for(String key:attributes.keySet()) {
            ret += "\n\t\t"+key+":"+attributes.get(key);
        }
        ret += "\n\t}";

        if (typeID ==IMAGE){
            ret+="\n\timgSrc:"+imageSrc;
        }
        return ret;
    }
}
