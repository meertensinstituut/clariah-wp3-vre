INSERT INTO service
  ("name","recipe","semantics","kind")
SELECT
  'TEST',
  'nl.knaw.meertens.deployment.lib.Test',
  '<?xml version="1.0" encoding="UTF-8"?>
<cmd:CMD xmlns:cmd="http://www.clarin.eu/cmd/1" xmlns:cmdp="http://www.clarin.eu/cmd/1/profiles/clarin.eu:cr1:p_1527668176011" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="
  http://www.clarin.eu/cmd/1 https://infra.clarin.eu/CMDI/1.x/xsd/cmd-envelop.xsd
  http://www.clarin.eu/cmd/1/profiles/clarin.eu:cr1:p_1527668176011 https://catalog.clarin.eu/ds/ComponentRegistry/rest/registry/1.x/profiles/clarin.eu:cr1:p_1505397653795/xsd" CMDVersion="1.2">
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
              <cmdp:Parameter><!-- use Parameter instead of ParameterGroup, if there are no nested parameters -->
                <cmdp:Name>input</cmdp:Name>
                <cmdp:Label xml:lang="en">Test input file</cmdp:Label>
                <cmdp:MIMEType>text/plain</cmdp:MIMEType>
                <cmdp:MinimumCardinality>1</cmdp:MinimumCardinality>
                <cmdp:MaximumCardinality>1</cmdp:MaximumCardinality>
              </cmdp:Parameter>
              <cmdp:Parameter>
                <cmdp:Name>author</cmdp:Name>
                <cmdp:Label xml:lang="en">Author</cmdp:Label>
                <cmdp:DataType>string</cmdp:DataType>
                <cmdp:MinimumCardinality>0</cmdp:MinimumCardinality>
                <cmdp:MaximumCardinality>*</cmdp:MaximumCardinality>
              </cmdp:Parameter>
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
</cmd:CMD>', 
  'service'
WHERE NOT EXISTS (
  SELECT "name" FROM service WHERE "name" = 'TEST'
);
