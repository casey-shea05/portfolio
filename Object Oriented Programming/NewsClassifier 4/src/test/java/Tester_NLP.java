import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import uob.oop.NLP;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Tester_NLP {

    @Test
    @Order(1)
    void textCleaning(){
        String strTest = "!\"$%&^%H).,e+ll~'/o/Wor.l,d!";
        assertEquals("helloworld", NLP.textCleaning(strTest));
    }

    @Test
    @Order(2)
    void textLemmatization(){
        String strTest = "i eat applesing on mondays";
        assertEquals("i eat apples on monday", NLP.textLemmatization(strTest));
    }

    @Test
    @Order(3)
    void removeStopWords(){
        String strTest = "harry and his cat are in their home";
        String[] _stopWords = {"and", "is", "in", "the"};
        assertEquals("harry his cat are their home", NLP.removeStopWords(strTest, _stopWords));
    }

}
