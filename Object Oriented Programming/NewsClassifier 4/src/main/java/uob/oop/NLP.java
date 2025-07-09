package uob.oop;

public class NLP {
    /***
     * Clean the given (_content) text by removing all the characters that are not 'a'-'z', '0'-'9' and white space.
     * @param _content Text that need to be cleaned.
     * @return The cleaned text.
     */
    public static String textCleaning(String _content) {
        StringBuilder sbContent = new StringBuilder();
        //TODO Task 2.1 - 3 marks
        String contentLowerCase = _content.toLowerCase();
        char[] characterArray = contentLowerCase.toCharArray();
        char[] letters = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};
        char[] numbers = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
        char whitespace = ' ';

        for (int elementToCompare : characterArray) {
            for (char letter : letters) {

                if (elementToCompare == letter) {
                    sbContent.append(letter);
                }
            }
            for (char number : numbers) {
                if (elementToCompare == number) {
                    sbContent.append(number);
                }
            }
            if (elementToCompare == whitespace) {
                sbContent.append(whitespace);
            }
        }
        return sbContent.toString().trim();
    }

    /***
     * Text lemmatization. Delete 'ing', 'ed', 'es' and 's' from the end of the word.
     * @param _content Text that need to be lemmatized.
     * @return Lemmatized text.
     */
    public static String textLemmatization(String _content) {
        StringBuilder sbContent = new StringBuilder();
        //TODO Task 2.2 - 3 marks
        String suffixIng = "ing";
        String suffixEd = "ed";
        String suffixEs = "es";
        String suffixS = "s";
        String[] textToLemmatize = _content.split(" ");

        for (String wordToLemmatize : textToLemmatize) {
            if (wordToLemmatize.endsWith(suffixIng)) {
                wordToLemmatize = wordToLemmatize.substring(0, wordToLemmatize.length() - suffixIng.length());
                sbContent.append(wordToLemmatize).append(" ");
            } else if (wordToLemmatize.endsWith(suffixEd)) {
                wordToLemmatize = wordToLemmatize.substring(0, wordToLemmatize.length() - suffixEd.length());
                sbContent.append(wordToLemmatize).append(" ");
            } else if (wordToLemmatize.endsWith(suffixEs)) {
                wordToLemmatize = wordToLemmatize.substring(0, wordToLemmatize.length() - suffixEs.length());
                sbContent.append(wordToLemmatize).append(" ");
            } else if (wordToLemmatize.endsWith(suffixS)) {
                wordToLemmatize = wordToLemmatize.substring(0, wordToLemmatize.length() - suffixS.length());
                sbContent.append(wordToLemmatize).append(" ");
            } else {
                sbContent.append(wordToLemmatize).append(" ");
            }
        }
        return sbContent.toString().trim();
    }

    /***
     * Remove stop-words from the text.
     * @param _content The original text.
     * @param _stopWords An array that contains stop-words.
     * @return Modified text.
     */
    public static String removeStopWords(String _content, String[] _stopWords) {
        StringBuilder sbConent = new StringBuilder();
        //TODO Task 2.3 - 3 marks
        String[] contentArray = _content.split(" ");

        for (String wordInContentArray: contentArray){
            boolean isStopWord = false;

            for (String stopWord: _stopWords) {
                if (wordInContentArray.equals(stopWord)) {
                    isStopWord = true;
                    break;
                }
            }
            if (!isStopWord){
                sbConent.append(wordInContentArray).append(" ");
            }
        }
        return sbConent.toString().trim();
    }

}
