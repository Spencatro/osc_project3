import javax.swing.*;
import java.awt.image.BufferedImage;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;

/**
 * Created by sxh112430 on 4/12/15.
 */
public class XMLParser {

    private String preparse = "";
    private String urlPrefix;
    private ArrayList<VerySimpleEntity> parseList = new ArrayList<VerySimpleEntity>();

    private Stack<HTMLEntity> parseStack = new Stack<HTMLEntity>();

    public XMLParser(String xml, String urlPrefix) {
        preparse = stripXMLComments(xml); // comments mess up everything, get rid of them right away
        this.urlPrefix = urlPrefix;
    }

    public void parse() throws ParseException {

        String[] openSplit = preparse.split("(?=<)");
        for (String line : openSplit) {
            line = line.trim();
            if (line.length() < 2) continue;
            String[] tagSplit = line.split("(?<=>)");
            String tagContent = tagSplit[0];
            tagContent = tagContent.trim();
            String bodyContent = "";
            if (tagSplit.length > 1) {
                bodyContent = tagSplit[1];
            }

            if (tagSplit.length > 2) {
                System.out.println(tagSplit[0]);
                System.out.println(tagSplit[1]);
                System.out.println(tagSplit[2]);
                throw new ParseException("Parse Error: malformed xml! Found a closing tag without a matching open tag!", 0);
            }

            boolean isOpenTag = tagContent.charAt(1) != '/';

            if (isOpenTag) {
                // separate attributes, replace <>'s
                // body content belongs to this entity on stack
                if (!(tagContent.startsWith("<") && tagContent.endsWith(">"))) {
                    // this is raw text, just shove it in the parse results
                    parseList.add(new VerySimpleEntity(tagContent, false, false, false));
                }
                tagContent = tagContent.substring(1, tagContent.length() - 1); // cut out chevrons
                tagContent = tagContent.trim();
                String[] tagContentSplit = tagContent.split(" ");
                String tagType = tagContentSplit[0].trim(); // get the first element, this is the tag type, e.g. img, or p
                String[] attributeSplit = Arrays.copyOfRange(tagContentSplit, 1, tagContentSplit.length);

                HTMLEntity thisTag = new HTMLEntity(urlPrefix, "", tagType, bodyContent, attributeSplit); //TODO: come up with a sane ID

                if(!thisTag.isClosed()) {
                    parseStack.push(thisTag);
                }

                if(thisTag.getTypeID() == HTMLEntity.IMAGE) {
                    // put the image in the list
                    parseList.add(new VerySimpleEntity(thisTag.getImageSrc(), true, false, false));
                } else {
                    // put the text in the list
                    parseList.add(new VerySimpleEntity(bodyContent, false, thisTag.getTagString().equals("script"), thisTag.getTagString().equals("style")));
                }

            } else {
                // throw an error if they don't match
                tagContent = tagContent.substring(2,tagContent.length() -1);
                String[] tagContentSplit = tagContent.split(" ");
                String tagType = tagContentSplit[0].trim();
                // body content belongs to previous entity on stack
                boolean skipThisEntity = false;
                for (String s:HTMLEntity.uncloseableTags) {
                    if(tagType.equals(s)) {
                        parseList.add(new VerySimpleEntity(bodyContent, false, false, false));
                        skipThisEntity = true;
                    }
                }
                if(skipThisEntity)
                    continue;
                HTMLEntity pop = parseStack.pop();
                if (!tagType.equals(pop.getTagString())) {

                    // about to do some weird error printing, but we'll throw an exception at the end
                    ArrayList<HTMLEntity> err = new ArrayList<>();
                    while(!parseStack.isEmpty()){
                        err.add(parseStack.pop());
                    }
                    for(HTMLEntity e:err) {
                        System.out.println(e);
                    }
                    throw new ParseException("Malformed XML: Closing tag didn't match last opened tag! Tag on stack: '"+pop.getTagString()+"'; closing tag: '"+tagType+"'",0);
                }
                // neither of the following lines are necessary, but for good practice
                pop.closeEntity();
                pop.appendToBody(bodyContent);

                parseList.add(new VerySimpleEntity(bodyContent, false, false, false));
            }


        }
    }

    public ArrayList<VerySimpleEntity> getParseList() {
        return parseList;
    }

    public static String stripXMLComments(String original) {
        String[] commentSplit = original.split("(?=<!--)");
        String result = commentSplit[0];
        for (int i = 1; i < commentSplit.length; i++) {
            int commentIdx = commentSplit[i].indexOf("-->") + 3;
            result += commentSplit[i].substring(commentIdx);
        }
        return result;
    }

}
