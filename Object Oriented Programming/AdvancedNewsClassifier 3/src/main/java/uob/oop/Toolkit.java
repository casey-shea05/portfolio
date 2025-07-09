package uob.oop;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class Toolkit {
    public static List<String> listVocabulary = null;
    public static List<double[]> listVectors = null;
    private static final String FILENAME_GLOVE = "glove.6B.50d_Reduced.csv";

    public static final String[] STOPWORDS = {"a", "able", "about", "across", "after", "all", "almost", "also", "am", "among", "an", "and", "any", "are", "as", "at", "be", "because", "been", "but", "by", "can", "cannot", "could", "dear", "did", "do", "does", "either", "else", "ever", "every", "for", "from", "get", "got", "had", "has", "have", "he", "her", "hers", "him", "his", "how", "however", "i", "if", "in", "into", "is", "it", "its", "just", "least", "let", "like", "likely", "may", "me", "might", "most", "must", "my", "neither", "no", "nor", "not", "of", "off", "often", "on", "only", "or", "other", "our", "own", "rather", "said", "say", "says", "she", "should", "since", "so", "some", "than", "that", "the", "their", "them", "then", "there", "these", "they", "this", "tis", "to", "too", "twas", "us", "wants", "was", "we", "were", "what", "when", "where", "which", "while", "who", "whom", "why", "will", "with", "would", "yet", "you", "your"};

    public void loadGlove() throws IOException {
        BufferedReader myReader = null;
        //TODO Task 4.1 - 5 marks

        File gloveFile;
        try {
            gloveFile = getFileFromResource(FILENAME_GLOVE);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        listVocabulary = new ArrayList<>();
        listVectors = new ArrayList<>();

        try {
            myReader = new BufferedReader(new FileReader(gloveFile));
            String line;

            while ((line = myReader.readLine()) != null){
                String[] parts = line.split(",");

                String vocab = parts[0];
                double[] vector = new double[parts.length - 1];

                for (int i = 1; i < parts.length; i++){
                    vector[i - 1] = Double.parseDouble(parts[i]);
                }

                listVocabulary.add(vocab);
                listVectors.add(vector);
            }
        } catch (IOException e) {
            throw new IOException(e.getMessage());
        }
    }

    private static File getFileFromResource(String fileName) throws URISyntaxException {
        ClassLoader classLoader = Toolkit.class.getClassLoader();
        URL resource = classLoader.getResource(fileName);
        if (resource == null) {
            throw new IllegalArgumentException(fileName);
        } else {
            return new File(resource.toURI());
        }
    }

    public List<NewsArticles> loadNews() {
        List<NewsArticles> listNews = new ArrayList<>();
        //TODO Task 4.2 - 5 Marks

        File newsFolder;
        try {
            newsFolder = getFileFromResource("News");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        if (newsFolder.isDirectory()){
            File[] files = newsFolder.listFiles();
            insertionSort(files);

            if (files != null){

                for (File file: files){
                    if (file.getName().contains(".htm")){

                        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))){
                            StringBuilder fileContentBuilder = new StringBuilder();
                            String line;
                            while ((line = bufferedReader.readLine()) != null){
                                fileContentBuilder.append(line).append("\n");
                            }
                            String fileContent = fileContentBuilder.toString();

                            String title = HtmlParser.getNewsTitle(fileContent);
                            String content = HtmlParser.getNewsContent(fileContent);
                            NewsArticles.DataType dataType = HtmlParser.getDataType(fileContent);
                            String label = HtmlParser.getLabel(fileContent);

                            NewsArticles newsArticle = new NewsArticles(title, content, dataType, label);
                            listNews.add(newsArticle);

                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }
        return listNews;
    }

    public static List<String> getListVocabulary() {
        return listVocabulary;
    }

    public static List<double[]> getlistVectors() {
        return listVectors;
    }

    public static void insertionSort(File[] files){
        int n = files.length;

        for (int i = 1; i < n; ++i){
            File key = files[i];
            int j = i - 1;

            while (j >= 0 && files[j].getName().compareTo(key.getName()) > 0){
                files[j + 1] = files[j];
                j = j - 1;
            }
            files[j + 1] = key;
        }
    }
}