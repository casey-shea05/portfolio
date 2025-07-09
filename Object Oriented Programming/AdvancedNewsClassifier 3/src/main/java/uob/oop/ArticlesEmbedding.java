package uob.oop;

import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.pipeline.*;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import java.util.Properties;


public class ArticlesEmbedding extends NewsArticles {
    private int intSize = -1;
    private String processedText = "";

    private INDArray newsEmbedding = Nd4j.create(0);

    public ArticlesEmbedding(String _title, String _content, NewsArticles.DataType _type, String _label) {
        //TODO Task 5.1 - 1 Mark
        super(_title,_content,_type, _label);
    }

    public void setEmbeddingSize(int _size) {
        //TODO Task 5.2 - 0.5 Marks
        intSize = _size;
    }

    public int getEmbeddingSize(){
        return intSize;
    }

    private boolean isPreprocessed = false;

    @Override
    public String getNewsContent() {
        //TODO Task 5.3 - 10 Marks

        if (!isPreprocessed){
            preprocessedText();
            isPreprocessed = true;
        }

        return processedText.trim();
    }

    private void preprocessedText(){
        String content = super.getNewsContent();
        String cleanContent = textCleaning(content);

        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,pos,lemma");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        CoreDocument document = pipeline.processToCoreDocument(cleanContent);
        pipeline.annotate(document);

        StringBuilder lemContent = new StringBuilder();
        for (CoreLabel token : document.tokens()){
            String lemma = token.lemma();
            if (!isStopWord(lemma)) {
                lemContent.append(lemma).append(" ");
            }
        }

        processedText =lemContent.toString().toLowerCase();
    }

    private boolean isStopWord(String word){

        for (String stopWord : Toolkit.STOPWORDS){
            if (word.equalsIgnoreCase(stopWord)){
                return true;
            }
        }
        return false;
    }

    private boolean isEmbeddingComputed = false;

    public INDArray getEmbedding() throws Exception {
        //TODO Task 5.4 - 20 Marks

        if (intSize == -1){
            throw new InvalidSizeException("Invalid size");
        }

        if (processedText.isEmpty()){
            throw new InvalidTextException("Invalid text");
        }

        if (isEmbeddingComputed){
            return Nd4j.vstack(newsEmbedding.mean(1));
        }

        String[] words = processedText.split("\\s+");

        int wordVectorSize = AdvancedNewsClassifier.listGlove.get(0).getVector().getVectorSize();
        newsEmbedding = Nd4j.zeros(intSize, wordVectorSize);

        int index = 0;

        for (String word: words) {
            Glove glove = findGloveObject(word);
            if (glove != null){
                double[] vectorArray = new double[wordVectorSize];

                for (int i = 0; i < vectorArray.length; i++) {
                        vectorArray[i] = glove.getVector().getElementatIndex(i);
                }

                INDArray wordEmbedding = Nd4j.create(vectorArray);
                newsEmbedding.putRow(index++, wordEmbedding);

                if (index == intSize){
                    break;
                }
            }
        }

        isEmbeddingComputed = true;

        return Nd4j.vstack(newsEmbedding.mean(1));

    }

    private Glove findGloveObject(String word) {
        for (Glove glove: AdvancedNewsClassifier.listGlove) {
            if (glove.getVocabulary().equals(word)) {
                return glove;
            }
        }
        return null;
    }


    /***
     * Clean the given (_content) text by removing all the characters that are not 'a'-'z', '0'-'9' and white space.
     * @param _content Text that need to be cleaned.
     * @return The cleaned text.
     */
    private static String textCleaning(String _content) {
        StringBuilder sbContent = new StringBuilder();

        for (char c : _content.toLowerCase().toCharArray()) {
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || Character.isWhitespace(c)) {
                sbContent.append(c);
            }
        }

        return sbContent.toString().trim();
    }
}
