package uob.oop;

public class HtmlParser {
    /***
     * Extract the title of the news from the _htmlCode.
     * @param _htmlCode Contains the full HTML string from a specific news. E.g. 01.htm.
     * @return Return the title if it's been found. Otherwise, return "Title not found!".
     */
    public static String getNewsTitle(String _htmlCode) {
        //TODO Task 1.1 - 5 marks
        if (_htmlCode.contains("<title>")) {
            int fullTitleBegin = _htmlCode.indexOf("<title>");
            int fullTitleEnd = _htmlCode.indexOf("</title>");
            String title = _htmlCode.substring(fullTitleBegin, fullTitleEnd);
            int titleBegin = title.indexOf("<title>");
            int cutOff = title.indexOf("|");
            return title.substring(titleBegin + 7, cutOff).trim();
        }
        else {
            return "Title not found!";
        }
    }

    /***
     * Extract the content of the news from the _htmlCode.
     * @param _htmlCode Contains the full HTML string from a specific news. E.g. 01.htm.
     * @return Return the content if it's been found. Otherwise, return "Content not found!".
     */
    public static String getNewsContent(String _htmlCode) {
        //TODO Task 1.2 - 5 marks
        if (_htmlCode.contains("\"articleBody\"")){
            int contentBegin = _htmlCode.indexOf("\"articleBody\"");
            int contentEnd = _htmlCode.indexOf("\"mainEntityOfPage\"");
            String article = _htmlCode.substring(contentBegin + 16, contentEnd - 3);
            return article.toLowerCase();
        }
        else {
            return  "Content not found!";
        }
    }
}
