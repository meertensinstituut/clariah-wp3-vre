package nl.knaw.meertens.deployment.lib;

import org.junit.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class DemoTest {

  @Test
  public void getResultString_multiline() {
    Demo demo = new Demo();
    String response = "<!DOCTYPE HTML>\n" +
      "<html>\n" +
      "    <head>\n" +
      "        <meta charset=\"UTF-8\">\n" +
      "        <META NAME=\"ROBOTS\" CONTENT=\"NOINDEX, NOFOLLOW\">\n" +
      "        <title>Deduplicate</title>\n" +
      "        <link rel=\"icon\" type=\"image/x-icon\" href=\"/img/favicon.ico\">\n" +
      "        <link rel='stylesheet' type='text/css' href='/style/toolstyle.css'>\n" +
      "        <script type='text/javascript' src='/script/jquery.js'></script>\n" +
      "        <script type='text/javascript' src='/script/dmi.js'></script>\n" +
      "        <script type='text/javascript' src='/script/new_elements.js'></script>\n" +
      "        <script type='text/javascript' src='/script/ajax.js'></script>\n" +
      "        <script type='text/javascript' src='/script/utilities.js'></script>\n" +
      "        <!-- customhead -->\n" +
      "    </head>\n" +
      "<body>\n" +
      "    <div id='container'>\n" +
      "        <div id='head'>\n" +
      "            <div id='menu'></div>\n" +
      "            <div id='line'>Deduplicate</div>\n" +
      "        </div>\n" +
      "        <div id='main'>\n" +
      "<div id='column_left'>\n" +
      "    <fieldset id='input'>\n" +
      "        <legend class='heading'>Input</legend>\n" +
      "        <form name='' action='' method='POST'>Input tag cloud in the format tag1 (13) tag2 (2) .." +
      ".<br/><textarea rows=\"10\" cols=\"60\" name=\"text\" class=\"\"><result>(1) test(10) </result>(1)" +
      "</textarea>\n" +
      "<br /><br /><input type='submit' class='' value='Deduplicate'></form>\n" +
      "    </fieldset>\n" +
      "</div>\n" +
      "<div id='column_right'>\n" +
      "    <fieldset id='howto'>\n" +
      "        <legend class='heading'>Deduplicate, an Introduction</legend>\n" +
      "        <span class='text'>Replicates the tags in a tag cloud by their value<br/><br/>Insert a tag cloud, e.g." +
      " war (5) peace (6) and the tool will write ouput 'war' five times and 'peace' six times.<br/>\n" +
      "<br/>\n" +
      "Can be used to input preformatted tag clouds into services like <a href='http://wordle.net'>wordle</a>" +
      ".<br/></span>\n" +
      "    </fieldset>\n" +
      "</div>\n" +
      "<div style='clear:both'></div>\n" +
      "</div><result>  test  test  test  test  test  test  test  test  test  test  </result> \t</div>\n" +
      "</div>\n" +
      "</body>\n" +
      "</html>";
    String result = demo.getResultString(response);

    assertThat(result).isEqualToIgnoringWhitespace("  test  test  test  test  test  test  test  test  test  test  ");
  }

  @Test
  public void testCreateRequestCookie() {
    String expectedCookie =
      "PHPSESSID=3b26uu7dki19j3fqv15ihv1bdf; " +
        "gcosvcauth=a00b6b173c5be71f1aa37bd2cf9d5ac642605b17bf826bd0de2a3a0bef4df3ac";

    String setCookieValue = "[PHPSESSID=3b26uu7dki19j3fqv15ihv1bdf; " +
      "path=/, gcosvcauth=a00b6b173c5be71f1aa37bd2cf9d5ac642605b17bf826bd0de2a3a0bef4df3ac; " +
      "expires=Wed, 05-Dec-2018 12:48:12 GMT; Max-Age=3600; path=/]";

    String headerCookie = Demo.createRequestCookie(setCookieValue);

    assertThat(headerCookie).isEqualTo(expectedCookie);
  }

}
