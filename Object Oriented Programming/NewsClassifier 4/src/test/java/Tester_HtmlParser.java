import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import uob.oop.HtmlParser;
import uob.oop.Toolkit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Tester_HtmlParser {
    String strHTML2 = "This is a test string";

    @Test
    @Order(1)
    void getNewsTitle_0() {
        Toolkit myTK = new Toolkit();
        String strHTML[] = myTK.loadHTML();
        assertEquals("NASA launches Psyche mission to rare metal asteroid", HtmlParser.getNewsTitle(strHTML[0]));
    }

    @Test
    @Order(2)
    void getNewsTitle_19() {
        Toolkit myTK = new Toolkit();
        String strHTML[] = myTK.loadHTML();
        assertEquals("Bitcoin value falls below $6,000 - the lowest level since mid-November", HtmlParser.getNewsTitle(strHTML[19]));
    }

    @Test
    @Order(3)
    void getNewsTitle_HTML2() {
        assertEquals("Title not found!", HtmlParser.getNewsTitle(strHTML2));
    }

    @Test
    @Order(4)
    void getNewsContent_1() {
        Toolkit myTK = new Toolkit();
        String strHTML[] = myTK.loadHTML();
        assertEquals("an influential panel of mps has called on the government to regulate consumer crypto trading and speculation as a type of gambling. the cross-party treasury committee claimed digital currencies such as bitcoin and ether have \\\"no intrinsic value and no useful social purpose\\\" - and as well as consuming large amounts of energy, they are often used by criminals for scams. it comes after the government announced proposals in february to regulate the crypto industry by bringing it under financial services law. but mps said a better approach would be to recognise how speculation in unbacked cryptoassets - like bitcoin - \\\"more closely resembles gambling than a financial service\\\". it recommended that safeguarding rules which oversee the likes of lotteries, betting firms and casinos should apply instead. around 10% of uk adults have speculated in cryptoassets, according to hm revenue and customs. the committee's new report warned digital currencies are a \\\"significant risk\\\" due to \\\"huge\\\" price volatility, with the potential for customers to lose everything they invest. it said there was evidence that addictions to cryptocurrency speculation were on the rise - and warned there are limited controls currently in place to protect vulnerable consumers. mps said they were concerned that bringing the industry under financial service regulation \\\"will create a 'halo' effect that leads consumers to believe that this activity is safer than it is, or protected when it is not\\\". \\\"we therefore strongly recommend that the government regulates retail trading and investment activity in unbacked cryptoassets as gambling rather than as a financial service, consistent with its stated principle of 'same risk, same regulatory outcome,'\\\" the report added. a 'wild west' industry it comes after a 2018 report by the committee described the cryptocurrency industry as a \\\"wild west\\\" - with mps saying nothing in their subsequent enquiries had moved them to alter that verdict. following the new report, committee chair, conservative mp harriett baldwin, said: \\\"effective regulation is clearly needed to protect consumers from harm, as well as to support productive innovation in the uk's financial services industry. \\\"however, with no intrinsic value, huge price volatility and no discernible social good, consumer trading of cryptocurrencies like bitcoin more closely resembles gambling than a financial service, and should be regulated as such.\\\" the mps said they still felt there was potential in the technology - such as by improving the efficiency and costs of making payments - and advised the government to take a \\\"balanced approach\\\" in supporting innovation. the committee added it was separately considering the potential role of digital currencies backed by central banks. meanwhile its report also criticised the government's attempt in april 2022 to launch a non-fungible token (nft) - a type of cryptocurrency asset - through the royal mint. the plan was dropped earlier this year following a review. mps said the government \\\"should seek to avoid expending public resources on supporting cryptoasset activities without a clear, beneficial use case\\\".  crypto 'offers opportunities' it comes as the government considers responses to a consultation into its regulation proposals. a treasury spokesperson indicated ministers would likely reject the committee's recommendation. they told sky news: \\\"risks posed by crypto are typical of those that exist in traditional financial services and it's financial services regulation - rather than gambling regulation - that has the track record in mitigating them. \\\"crypto offers opportunities but we are taking an agile approach to robustly regulating the market, addressing the most pressing risks first in a way that promotes innovation.\\\"  the report comes amid growing pressure on governments around the world to better regulate the industry, heightened by the sudden bankruptcy of crypto platform ftx in november. some 80,000 uk-based customers were impacted by the collapse, and one british investor was left with a &#163;1m hole in his finances. the european union this week approved tougher cryptoasset rules - including new powers to ban exchanges that fail to protect consumers. the international organisation of securities commissions (iosco), whose members include regulators in the us and uk, said it will also soon announce proposals for the first ever set of global rules covering crypto trading.", HtmlParser.getNewsContent(strHTML[1]));
    }

}
