package uob.oop;

import java.text.DecimalFormat;

public class NewsClassifier {
    public String[] myHTMLs;
    public String[] myStopWords = new String[127];
    public String[] newsTitles;
    public String[] newsContents;
    public String[] newsCleanedContent;
    public double[][] newsTFIDF;

    private final String TITLE_GROUP1 = "Osiris-Rex's sample from asteroid Bennu will reveal secrets of our solar system";
    private final String TITLE_GROUP2 = "Bitcoin slides to five-month low amid wider sell-off";

    public Toolkit myTK;

    public NewsClassifier() { //Our constructor
        myTK = new Toolkit();
        myHTMLs = myTK.loadHTML();
        myStopWords = myTK.loadStopWords();

        loadData();
    }

    public static void main(String[] args) {
        NewsClassifier myNewsClassifier = new NewsClassifier();

        myNewsClassifier.newsCleanedContent = myNewsClassifier.preProcessing();

        myNewsClassifier.newsTFIDF = myNewsClassifier.calculateTFIDF(myNewsClassifier.newsCleanedContent);

        //Change the _index value to calculate similar based on a different news article.
        double[][] doubSimilarity = myNewsClassifier.newsSimilarity(0);

        System.out.println(myNewsClassifier.resultString(doubSimilarity, 10));

        String strGroupingResults = myNewsClassifier.groupingResults(myNewsClassifier.TITLE_GROUP1, myNewsClassifier.TITLE_GROUP2);
        System.out.println(strGroupingResults);
    }

    public void loadData() {
        //TODO 4.1 - 2 marks
        newsTitles = new String[myHTMLs.length];
        newsContents = new String[myHTMLs.length];

        for (int i = 0; i < myHTMLs.length; i++) {
            newsTitles[i] = HtmlParser.getNewsTitle(myHTMLs[i]);
            newsContents[i] = HtmlParser.getNewsContent(myHTMLs[i]);
        }
    }

    public String[] preProcessing() {
        String[] myCleanedContent = new String[newsContents.length];
        //TODO 4.2 - 5 marks
        for (int i = 0; i < newsContents.length; i++) {
            String cleanedText = NLP.textCleaning(newsContents[i]);
            String lemmatizedText = NLP.textLemmatization(cleanedText);
            String stopWordsRemoved = NLP.removeStopWords(lemmatizedText, myStopWords);

            myCleanedContent[i] = stopWordsRemoved;
        }
        return myCleanedContent;
    }

    public double[][] calculateTFIDF(String[] _cleanedContents) {
        String[] vocabularyList = buildVocabulary(_cleanedContents);
        double[][] myTFIDF = new double[_cleanedContents.length][vocabularyList.length];
        //TODO 4.3 - 10 marks

        //Calculate TF Value
        double[][] tfArray = new double[_cleanedContents.length][vocabularyList.length];

        for (int i = 0; i < _cleanedContents.length; i++){
            String currentDoc = _cleanedContents[i];
            String[] wordContentArray = currentDoc.split(" ");

            //Calculate the total number of words in current html document
            int totalSumOfWordsInDoc = wordContentArray.length;

            for (int j = 0; j < vocabularyList.length; j++) {
                String uniqueWord = vocabularyList[j];
                int wordFrequency = 0;

                //Count how many times the unique word appears in each html document in our corpus
                for (String word: wordContentArray){
                    if (uniqueWord.equals(word)){
                        wordFrequency++;
                    }
                }

                //Calculate the TF Value for the unique/ specific word in the current document
                tfArray[i][j] = (double) wordFrequency / (double)totalSumOfWordsInDoc;
            }
        }

        //Calculate IDF Value
        double[] idfArray = new double[vocabularyList.length];
        int totalNumberOfDocs = _cleanedContents.length;

        for (int i = 0; i < vocabularyList.length; i++) {
            //Counter for how many documents word appears in
            int wordAppearsInDocCount = 0;

            for (int j = 0; j < _cleanedContents.length; j++) {
                String currentDoc = _cleanedContents[j];
                String[] wordContentArray = currentDoc.split(" ");

                boolean wordAppearsInDoc = false;

                for (String word : wordContentArray) {
                    if (vocabularyList[i].equals(word)) {
                        wordAppearsInDoc = true;
                        break;
                    }
                }

                if (wordAppearsInDoc) {
                    wordAppearsInDocCount++;
                }
            }

            //Calculate IDF value for the word in each vocab list
            idfArray[i] = Math.log((double) totalNumberOfDocs / wordAppearsInDocCount) + 1;
        }
        //Calculate TFIDF
        for (int i = 0; i < _cleanedContents.length; i++){
            for (int j = 0; j < vocabularyList.length; j++){
                myTFIDF[i][j] = tfArray[i][j] * idfArray[j];
            }
        }
        return myTFIDF;
    }

    public String[] buildVocabulary(String[] _cleanedContents) {
        String[] arrayVocabulary = new String[0];
        //TODO 4.4 - 10 marks
        for (String eachContents : _cleanedContents) {
            String[] textToCheck = eachContents.split(" ");

            for (String word : textToCheck) {
                boolean isUnique = true;

                for (String uniqueWord : arrayVocabulary) {
                    if (word.equals(uniqueWord)) {
                        isUnique = false;
                        break;
                    }
                }

                if (isUnique) {
                    String[] tempArray = new String[arrayVocabulary.length + 1];
                    System.arraycopy(arrayVocabulary, 0, tempArray, 0, arrayVocabulary.length);
                    tempArray[arrayVocabulary.length] = word;
                    arrayVocabulary = tempArray;
                }
            }
        }
        return arrayVocabulary;
    }

    public double[][] newsSimilarity(int _newsIndex) {
        double[][] mySimilarity = new double[newsTFIDF.length][2];
        //TODO 4.5 - 15 marks

        double[] vector1 = newsTFIDF[_newsIndex];

        Vector selectedVector = new Vector(vector1);

        for (int i = 0; i < newsTFIDF.length; i++){

            double[] vector2 = newsTFIDF[i];

            Vector otherVector = new Vector(vector2);

            double similarity = selectedVector.cosineSimilarity(otherVector);

            mySimilarity[i][0] = i;
            mySimilarity[i][1] = similarity;
        }

        //Using simple bubble sort
        for (int i = 0; i < mySimilarity.length - 1; i++){
            for (int j = 0; j < mySimilarity.length - i - 1; j++){
                if (mySimilarity[j][1] < mySimilarity[j + 1][1]){
                    double[] temp = mySimilarity[j];
                    mySimilarity[j] = mySimilarity[j+1];
                    mySimilarity[j+1] = temp;
                }
            }
        }

        return mySimilarity;
    }

    private int getNewsTitleByIndex(String targetTitle){
        for (int i = 0; i < newsTitles.length; i++){
            if (newsTitles[i].equals(targetTitle)){
                return i;
            }
        }
        return -1;
    }

    public String groupingResults(String _firstTitle, String _secondTitle) {
        int[] arrayGroup1 = null, arrayGroup2 = null;
        //TODO 4.6 - 15 marks

        int indexFirstTitle = getNewsTitleByIndex(_firstTitle);
        int indexSecondTitle = getNewsTitleByIndex(_secondTitle);

        Vector firstVectorTitle = new Vector(newsTFIDF[indexFirstTitle]);
        Vector secondVectorTitle = new Vector(newsTFIDF[indexSecondTitle]);
        double cosineSimilarity = firstVectorTitle.cosineSimilarity(secondVectorTitle);

        double[][] similarityArray = newsSimilarity(indexFirstTitle);

        for (int i = 0; i < similarityArray.length - 1; i++){
            for (int j = 0; j < similarityArray.length - 1; j++){
                if (similarityArray[j][1] < similarityArray[j + 1][1]){
                    double[] temp = similarityArray [j];
                    similarityArray[j] = similarityArray[j + 1];
                    similarityArray[j + 1] = temp;
                }
            }
        }

        int separate = similarityArray.length / 2;
        arrayGroup1 = new int[separate];
        arrayGroup2 = new int[similarityArray.length - separate];

        for (int i = 0; i < similarityArray.length; i++){
            if (i < separate){
                arrayGroup1[i] = (int) similarityArray[i][0];
            }
            else {
                arrayGroup2[i - separate] = (int) similarityArray[i][0];
            }
        }
        orderElementsByIndex(arrayGroup1);
        orderElementsByIndex(arrayGroup2);
        return resultString(arrayGroup1, arrayGroup2);
    }

    private int[] orderElementsByIndex(int[] array){
        for(int i = 0; i < array.length - 1; i++){
            for (int j = 0; j < array.length - i - 1; j++){
                if (array[j] > array[j+1]) {
                    int temp = array[j];
                    array[j] = array[j+1];
                    array[j + 1] = temp;
                }
            }
        }
        return array;
    }

    public String resultString(double[][] _similarityArray, int _groupNumber) {
        StringBuilder mySB = new StringBuilder();
        DecimalFormat decimalFormat = new DecimalFormat("#.#####");
        for (int j = 0; j < _groupNumber; j++) {
            for (int k = 0; k < _similarityArray[j].length; k++) {
                if (k == 0) {
                    mySB.append((int) _similarityArray[j][k]).append(" ");
                } else {
                    String formattedCS = decimalFormat.format(_similarityArray[j][k]);
                    mySB.append(formattedCS).append(" ");
                }
            }
            mySB.append(newsTitles[(int) _similarityArray[j][0]]).append("\r\n");
        }
        mySB.delete(mySB.length() - 2, mySB.length());
        return mySB.toString();
    }

    public String resultString(int[] _firstGroup, int[] _secondGroup) {
        StringBuilder mySB = new StringBuilder();
        mySB.append("There are ").append(_firstGroup.length).append(" news in Group 1, and ").append(_secondGroup.length).append(" in Group 2.\r\n").append("=====Group 1=====\r\n");

        for (int i : _firstGroup) {
            mySB.append("[").append(i + 1).append("] - ").append(newsTitles[i]).append("\r\n");
        }
        mySB.append("=====Group 2=====\r\n");
        for (int i : _secondGroup) {
            mySB.append("[").append(i + 1).append("] - ").append(newsTitles[i]).append("\r\n");
        }

        mySB.delete(mySB.length() - 2, mySB.length());
        return mySB.toString();
    }

}
