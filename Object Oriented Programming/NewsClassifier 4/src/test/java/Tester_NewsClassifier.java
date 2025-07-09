import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import uob.oop.NewsClassifier;

import java.text.DecimalFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Tester_NewsClassifier {

    static NewsClassifier myNewsClassifier = new NewsClassifier();
    static String[] arrayTest = {"Hello World", "I Love This World"};

    @Test
    @Order(1)
    void loadData() {
        myNewsClassifier.loadData();
        assertEquals(51, myNewsClassifier.newsTitles[0].length());
        assertEquals(1555, myNewsClassifier.newsContents[19].length());
    }

    @Test
    @Order(2)
    void preProcessing() {
        assertEquals(20, myNewsClassifier.preProcessing().length);
    }

    @Test
    @Order(3)
    void buildVocabulary() {
        assertEquals(5, myNewsClassifier.buildVocabulary(arrayTest).length);
    }

    @Test
    @Order(4)
    void calculateTFIDF() {
        String[] corpusText = {"harry_potter is a student at hogwarts", "voldemort used to be a student at hogwarts but graduated already", "the parents of harry_potter studied at hogwarts as well"};
        myNewsClassifier.newsTFIDF = myNewsClassifier.calculateTFIDF(corpusText);
        StringBuilder mySB = new StringBuilder();
        DecimalFormat decimalFormat = new DecimalFormat("#.#####");
        for (int i = 0; i < myNewsClassifier.newsTFIDF.length; i++) {
            for (int j = 0; j < myNewsClassifier.newsTFIDF[i].length; j++) {
                mySB.append(decimalFormat.format(myNewsClassifier.newsTFIDF[i][j])).append(" ");
            }
            mySB.append("\r\n");
        }
        assertEquals("0.23424 0.34977 0.23424 0.23424 0.16667 0.16667 0 0 0 0 0 0 0 0 0 0 0 0 0 \r\n" + "0 0 0.12777 0.12777 0.09091 0.09091 0.19078 0.19078 0.19078 0.19078 0.19078 0.19078 0.19078 0 0 0 0 0 0 \r\n" + "0.15616 0 0 0 0.11111 0.11111 0 0 0 0 0 0 0 0.23318 0.23318 0.23318 0.23318 0.23318 0.23318 \r\n", mySB.toString());
    }

    @Test
    @Order(5)
    void newsSimilarity() {
        myNewsClassifier.newsCleanedContent = myNewsClassifier.preProcessing();
        myNewsClassifier.newsTFIDF = myNewsClassifier.calculateTFIDF(myNewsClassifier.newsCleanedContent);
        assertEquals("1 1 Crypto trading should be treated like a type of gambling, influential MPs say\r\n" + "7 0.20593 Bitcoin rallies slightly but still set for record losing streak after Terra 'stablecoin' collapse", myNewsClassifier.resultString(myNewsClassifier.newsSimilarity(1), 2));
    }

    @Test
    @Order(6)
    void groupingResults() {
        myNewsClassifier.newsCleanedContent = myNewsClassifier.preProcessing();
        myNewsClassifier.newsTFIDF = myNewsClassifier.calculateTFIDF(myNewsClassifier.newsCleanedContent);
        assertEquals("There are 10 news in Group 1, and 10 in Group 2.\r\n" + "=====Group 1=====\r\n" + "[1] - NASA launches Psyche mission to rare metal asteroid\r\n" + "[3] - NASA reveals 'incredible' findings from asteroid that could explain origins of life on Earth\r\n" + "[5] - NASA's Mars rover finds surprising mud cracks that hint planet once supported life\r\n" + "[7] - NASA to explore giant metal asteroid Psyche - and it could reveal secrets of solar system\r\n" + "[9] - Astronaut who accidentally broke record for longest time spent in space finally returns to Earth\r\n" + "[11] - Osiris-Rex's sample from asteroid Bennu will reveal secrets of our solar system\r\n" + "[13] - Sir Brian May 'immensely proud' to be part of Osiris-Rex asteroid sample team\r\n" + "[15] - Osiris-Rex: NASA returns sample from asteroid Bennu to Earth\r\n" + "[17] - NASA mission to return with 'pristine' samples from asteroid 'which could one day hit Earth'\r\n" + "[19] - What did we learn from NASA's first ever public meeting on UFOs?\r\n" + "=====Group 2=====\r\n" + "[2] - Crypto trading should be treated like a type of gambling, influential MPs say\r\n" + "[4] - Bitcoin tops $30,000 for first time in 10 months\r\n" + "[6] - Bitcoin suffers briefly after Tesla sells majority of its holdings\r\n" + "[8] - Bitcoin rallies slightly but still set for record losing streak after Terra 'stablecoin' collapse\r\n" + "[10] - Bitcoin and US tech stocks hammered as global flight from risk intensifies\r\n" + "[12] - Razzlekhan: Cryptocurrency worth billions seized after self-proclaimed 'Crocodile of Wall Street' arrested in connection with exchange hack\r\n" + "[14] - Bitcoin slides to five-month low amid wider sell-off\r\n" + "[16] - Bitcoin hits record high as launch of new fund opens up market to wider class of investors\r\n" + "[18] - Bitcoin faces biggest one-day slump since last year as China announces curbs\r\n" + "[20] - Bitcoin value falls below $6,000 - the lowest level since mid-November", myNewsClassifier.groupingResults("Osiris-Rex's sample from asteroid Bennu will reveal secrets of our solar system", "Bitcoin slides to five-month low amid wider sell-off"));
    }

}
