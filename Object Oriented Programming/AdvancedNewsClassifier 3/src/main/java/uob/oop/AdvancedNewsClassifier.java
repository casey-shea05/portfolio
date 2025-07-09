package uob.oop;

import org.apache.commons.lang3.time.StopWatch;
import org.deeplearning4j.datasets.iterator.utilty.ListDataSetIterator;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.WorkspaceMode;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AdvancedNewsClassifier {
    public Toolkit myTK = null;
    public static List<NewsArticles> listNews = null;
    public static List<Glove> listGlove = null;
    public List<ArticlesEmbedding> listEmbedding = null;
    public MultiLayerNetwork myNeuralNetwork = null;

    public final int BATCHSIZE = 10;

    public int embeddingSize = 0;
    private static StopWatch mySW = new StopWatch();

    public AdvancedNewsClassifier() throws IOException {
        myTK = new Toolkit();
        myTK.loadGlove();
        listNews = myTK.loadNews();
        listGlove = createGloveList();
        listEmbedding = loadData();
    }

    public static void main(String[] args) throws Exception {
        mySW.start();
        AdvancedNewsClassifier myANC = new AdvancedNewsClassifier();

        myANC.embeddingSize = myANC.calculateEmbeddingSize(myANC.listEmbedding);
        myANC.populateEmbedding();
        myANC.myNeuralNetwork = myANC.buildNeuralNetwork(2);
        myANC.predictResult(myANC.listEmbedding);
        myANC.printResults();
        mySW.stop();
        System.out.println("Total elapsed time: " + mySW.getTime());
    }

    public List<Glove> createGloveList() {
        List<Glove> listResult = new ArrayList<>();
        //TODO Task 6.1 - 5 Marks

        List<String> vocabulary = Toolkit.getListVocabulary();
        List<double[]> vectors = Toolkit.getlistVectors();

        if (vocabulary != null && vectors != null && vocabulary.size() == vectors.size()){
            for (int i = 0; i < vocabulary.size(); i++){
                String word = vocabulary.get(i);
                double[] vectorArray = vectors.get(i);

                if (!isStopWord(word)){
                    Vector vector= new Vector(vectorArray);

                    Glove glove = new Glove(word, vector);
                    listResult.add(glove);
                }
            }
        }
        return listResult;
    }

    private boolean isStopWord(String word){

        for (String stopWord : Toolkit.STOPWORDS){
            if (word.equalsIgnoreCase(stopWord)){
                return true;
            }
        }
        return false;
    }


    public static List<ArticlesEmbedding> loadData() {
        List<ArticlesEmbedding> listEmbedding = new ArrayList<>();
        for (NewsArticles news : listNews) {
            ArticlesEmbedding myAE = new ArticlesEmbedding(news.getNewsTitle(), news.getNewsContent(), news.getNewsType(), news.getNewsLabel());
            listEmbedding.add(myAE);
        }
        return listEmbedding;
    }

    public int calculateEmbeddingSize(List<ArticlesEmbedding> _listEmbedding) {
        int intMedian = -1;
        //TODO Task 6.2 - 5 Marks
        List<Integer> documentLengths = new ArrayList<>();
        List<String> vocabulary = Toolkit.getListVocabulary();

        for(ArticlesEmbedding embedding: _listEmbedding){
            int totalWords = 0;
            String content = embedding.getNewsContent();
            String[] words = content.split("\\s+");

            for (String word: words){
                if (vocabulary.contains(word.toLowerCase())){
                    totalWords++;
                }
            }
            documentLengths.add(totalWords);

        }

        sort(documentLengths);

        int[] medianIndices = findMedianIndices(documentLengths);

        if (medianIndices.length == 1){
            intMedian = documentLengths.get(medianIndices[0]);
        } else {
            intMedian = (documentLengths.get(medianIndices[0]) + documentLengths.get(medianIndices[1])) / 2;
        }
        return intMedian;
    }

    private void sort(List<Integer> list){
        list.sort(null);
    }

    public static int[] findMedianIndices(List<Integer> sortedList){
        int n = sortedList.size();

        if (n % 2 == 0){
            int middle1 = n/2 + 1;
            int middle2 = n/2;
            return new int[]{middle1, middle2};
        }
        else{
            return new int[]{(n-1)/2};
        }
    }

    public void populateEmbedding() {
        //TODO Task 6.3 - 10 Marks
        for (ArticlesEmbedding articlesEmbedding: listEmbedding){
            try {
                //Try to calculate the embedding for each article
                articlesEmbedding.getEmbedding();
            } catch (InvalidSizeException e){
                //Handle the InvalidSizeException: re-assign the intSize attribute
                articlesEmbedding.setEmbeddingSize(embeddingSize);
                //Handle the InvalidTextException: pre-process the article's contents/ text
            } catch (InvalidTextException e){
                articlesEmbedding.getNewsContent();
                //Try to calculate the article embedding again
                try {
                    articlesEmbedding.getEmbedding();
                } catch (InvalidSizeException ignored) {

                } catch (InvalidTextException ignored) {

                } catch (Exception ignored) {

                }
            } catch (Exception ignored) {

            }
        }
    }

    public DataSetIterator populateRecordReaders(int _numberOfClasses) throws Exception {
        ListDataSetIterator myDataIterator = null;
        List<DataSet> listDS = new ArrayList<>();
        INDArray inputNDArray = null;
        INDArray outputNDArray = null;

        //TODO Task 6.4 - 8 Marks
        //Iterate through listEmbedding
        for (ArticlesEmbedding articlesEmbedding: listEmbedding){
            //Check if newsType is marked as Training data
            if (articlesEmbedding.getNewsType().equals(NewsArticles.DataType.Training)){
                //Retrieve embedding for the article
                inputNDArray = articlesEmbedding.getEmbedding();

                //Create output INDArray with shape [1, _newsContent]
                outputNDArray = Nd4j.zeros(1, _numberOfClasses);

                //Determine class index (e.g. group 1 or group 2) based on newsLabel
                int newsLabel = Integer.parseInt(articlesEmbedding.getNewsLabel());
                outputNDArray.putScalar(0, newsLabel - 1, 1); //Assuming newsLabel starts from 1

                //Create DataSet Object and add it to the list
                DataSet myDataSet = new DataSet(inputNDArray, outputNDArray);
                listDS.add(myDataSet);
            }
        }

        return new ListDataSetIterator(listDS, BATCHSIZE);
    }

    public MultiLayerNetwork buildNeuralNetwork(int _numOfClasses) throws Exception {
        DataSetIterator trainIter = populateRecordReaders(_numOfClasses);
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(42)
                .trainingWorkspaceMode(WorkspaceMode.ENABLED)
                .activation(Activation.RELU)
                .weightInit(WeightInit.XAVIER)
                .updater(Adam.builder().learningRate(0.02).beta1(0.9).beta2(0.999).build())
                .l2(1e-4)
                .list()
                .layer(new DenseLayer.Builder().nIn(embeddingSize).nOut(15)
                        .build())
                .layer(new OutputLayer.Builder(LossFunctions.LossFunction.HINGE)
                        .activation(Activation.SOFTMAX)
                        .nIn(15).nOut(_numOfClasses).build())
                .build();

        MultiLayerNetwork model = new MultiLayerNetwork(conf);
        model.init();

        for (int n = 0; n < 100; n++) {
            model.fit(trainIter);
            trainIter.reset();
        }
        return model;
    }

    public List<Integer> predictResult(List<ArticlesEmbedding> _listEmbedding) throws Exception {
        List<Integer> listResult = new ArrayList<>();
        //TODO Task 6.5 - 8 Marks

        for (ArticlesEmbedding articlesEmbedding: _listEmbedding){
            if (articlesEmbedding.getNewsType().equals(NewsArticles.DataType.Testing)){
                INDArray inputNDArray = articlesEmbedding.getEmbedding();

                int predictedLabel = myNeuralNetwork.predict(inputNDArray)[0];

                listResult.add(predictedLabel);

                articlesEmbedding.setNewsLabel(String.valueOf(predictedLabel));
            }
        }

        return listResult;
    }

    public void printResults() {
        //TODO Task 6.6 - 6.5 Marks

        List<Integer> groupLabels = new ArrayList<>();

        for (ArticlesEmbedding embedding : listEmbedding) {
            if (embedding.getNewsType() == NewsArticles.DataType.Testing) {
                int predictedLabel = Integer.parseInt(embedding.getNewsLabel());

                if (!groupLabels.contains(predictedLabel)){
                    groupLabels.add(predictedLabel);
                }
            }
        }

        sort(groupLabels);

        for (Integer groupLabel: groupLabels){
            System.out.println("Group " + (groupLabel + 1));
            for (ArticlesEmbedding embedding: listEmbedding){
                if (embedding.getNewsType().equals(NewsArticles.DataType.Testing)){
                    int predictedLabel = Integer.parseInt(embedding.getNewsLabel());

                    if (predictedLabel == groupLabel){
                        System.out.println(embedding.getNewsTitle());
                    }
                }
            }
        }
    }


}
