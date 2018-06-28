INSERT INTO service
  ("name","recipe","semantics")
SELECT
  'TEST',
  'nl.knaw.meertens.deployment.lib.Test',
  '<?xml version="1.0" encoding="UTF-8"?>
<cmd:CMD xmlns:cmd="http://www.clarin.eu/cmd/1" xmlns:cmdp="http://www.clarin.eu/cmd/1/profiles/clarin.eu:cr1:p_1505397653795" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="
  http://www.clarin.eu/cmd/1 https://infra.clarin.eu/CMDI/1.x/xsd/cmd-envelop.xsd
  http://www.clarin.eu/cmd/1/profiles/clarin.eu:cr1:p_1505397653795 https://catalog.clarin.eu/ds/ComponentRegistry/rest/registry/1.x/profiles/clarin.eu:cr1:p_1505397653795/xsd" CMDVersion="1.2">
  <cmd:Header>
    <cmd:MdCreationDate>2018-05-28</cmd:MdCreationDate>
    <cmd:MdProfile>clarin.eu:cr1:p_1505397653795</cmd:MdProfile><!-- profile is fixed -->
  </cmd:Header>
  <cmd:Resources>
    <cmd:ResourceProxyList/>
    <cmd:JournalFileProxyList/>
    <cmd:ResourceRelationList/>
  </cmd:Resources>
  <cmd:Components>
    <cmdp:CLARINWebService>
      <cmdp:Service CoreVersion="1.0">
        <cmdp:Name>Test</cmdp:Name>
        <cmdp:Description>Service to test deployment mechanism of VRE</cmdp:Description>
        <cmdp:ServiceDescriptionLocation/> <!-- test doesn''t really run remote -->
        <cmdp:Operations>
          <cmdp:Operation>
            <cmdp:Name>main</cmdp:Name><!-- main is our default endpoint -->
            <cmdp:Input>
              <cmdp:Parameter>
                  <cmdp:Name>input</cmdp:Name>
                  <cmdp:MIMEType>text/plain</cmdp:MIMEType>
                  <cmdp:MinimumCardinality>1</cmdp:MinimumCardinality>
                  <cmdp:MaximumCardinality>*</cmdp:MaximumCardinality>
                  <cmdp:Label xml:lang="dk">Indtastningstekst</cmdp:Label>
                  <cmdp:Label xml:lang="en">Input text</cmdp:Label>
                  <cmdp:Label xml:lang="nl">Invoer tekst</cmdp:Label>
                  <cmdp:DataType>integer</cmdp:DataType>
              </cmdp:Parameter>
              <cmdp:Parameter>
                  <cmdp:Name>red-pill-and-blue-pill</cmdp:Name>
                  <cmdp:Label xml:lang="en">Red pill and blue pill</cmdp:Label>
                  <cmdp:Description>This is your last chance. After this, there is no turning back. You take the blue pill—the story ends, you wake up in your bed and believe whatever you want to believe. You take the red pill—you stay in Wonderland, and I show you how deep the rabbit hole goes. Remember: all I''m offering is the truth. Nothing more.</cmdp:Description>
                  <cmdp:DataType>string</cmdp:DataType>
                  <cmdp:MinimumCardinality>1</cmdp:MinimumCardinality>
                  <cmdp:MaximumCardinality>1</cmdp:MaximumCardinality>
                  <cmdp:Values>
                      <cmdp:ParameterValue>
                          <cmdp:Value>red</cmdp:Value>
                          <cmdp:Label xml:lang="dk">Rød</cmdp:Label>
                          <cmdp:Label xml:lang="en">Red</cmdp:Label>
                          <cmdp:Label xml:lang="nl">Rood</cmdp:Label>
                          <cmdp:Description>Knowledge, freedom, and the brutal truths of reality</cmdp:Description>
                      </cmdp:ParameterValue>
                      <cmdp:ParameterValue>
                          <cmdp:Value>blue</cmdp:Value>
                          <cmdp:Label xml:lang="dk">Blå</cmdp:Label>
                          <cmdp:Label xml:lang="en">Blue</cmdp:Label>
                          <cmdp:Label xml:lang="nl">Blauw</cmdp:Label>
                          <cmdp:Description>Security, happiness and the blissful ignorance of illusion</cmdp:Description>
                      </cmdp:ParameterValue>
                  </cmdp:Values>
              </cmdp:Parameter>
              <cmdp:ParameterGroup>
                  <cmdp:Name>untokinput</cmdp:Name>
                  <cmdp:Label xml:lang="dk">Untok Indtastningstekst</cmdp:Label>
                  <cmdp:Label xml:lang="en">Untok Input text</cmdp:Label>
                  <cmdp:Label xml:lang="nl">Untok Invoer tekst</cmdp:Label>
                  <cmdp:MIMEType>text/plain</cmdp:MIMEType>
                  <cmdp:MinimumCardinality>1</cmdp:MinimumCardinality>
                  <cmdp:MaximumCardinality>1</cmdp:MaximumCardinality>
                  <cmdp:Parameters>
                      <cmdp:Parameter>
                          <cmdp:Name>language</cmdp:Name>
                          <cmdp:Description>The language this text is in</cmdp:Description>
                          <cmdp:DataType>string</cmdp:DataType>
                          <cmdp:MinimumCardinality>1</cmdp:MinimumCardinality>
                          <cmdp:MaximumCardinality>1</cmdp:MaximumCardinality>
                          <cmdp:Values>
                              <cmdp:ParameterValue>
                                  <cmdp:Value>nld</cmdp:Value>
                                  <cmdp:Label xml:lang="en">Dutch</cmdp:Label>
                              </cmdp:ParameterValue>
                              <cmdp:ParameterValue>
                                  <cmdp:Value>eng</cmdp:Value>
                                  <cmdp:Label xml:lang="en">English</cmdp:Label>
                              </cmdp:ParameterValue>
                              <cmdp:ParameterValue>
                                  <cmdp:Value>nld-twitter</cmdp:Value>
                                  <cmdp:Label xml:lang="en">Dutch on Twitter</cmdp:Label>
                              </cmdp:ParameterValue>
                          </cmdp:Values>
                      </cmdp:Parameter>
                      <cmdp:Parameter>
                          <cmdp:Name>author</cmdp:Name>
                          <cmdp:DataType>string</cmdp:DataType>
                          <cmdp:MinimumCardinality>0</cmdp:MinimumCardinality>
                          <cmdp:MaximumCardinality>*</cmdp:MaximumCardinality>
                      </cmdp:Parameter>
                  </cmdp:Parameters>
              </cmdp:ParameterGroup>
            </cmdp:Input>
            <cmdp:Output>
              <cmdp:Parameter>
                <cmdp:Name>output</cmdp:Name>
                <cmdp:Description>Surprise</cmdp:Description>
                <cmdp:MIMEType>text/plain</cmdp:MIMEType>
              </cmdp:Parameter>
            </cmdp:Output>
          </cmdp:Operation>
        </cmdp:Operations>
      </cmdp:Service>
    </cmdp:CLARINWebService>
  </cmd:Components>
</cmd:CMD>'
WHERE NOT EXISTS (
  SELECT "name" FROM service WHERE "name" = 'TEST'
);
