<sch:schema xmlns:sch="http://purl.oclc.org/dsdl/schematron" queryBinding="xslt2">
    <sch:ns uri="http://www.clarin.eu/cmd/1" prefix="cmd"/>
    <sch:ns uri="http://www.w3.org/2001/XMLSchema-instance" prefix="xsi"/>

    <!-- Is there a MdProfile? -->
    <sch:pattern>
        <sch:title>Test cmd:MdProfile</sch:title>
        <sch:rule role="error" context="cmd:Header">
            <sch:assert test="string-length(cmd:MdProfile/text()) &gt; 0">
                A CMDI instance should contain a non-empty &lt;cmd:MdProfile&gt; element in &lt;cmd:Header&gt;.
            </sch:assert>
        </sch:rule>
    </sch:pattern>

    <!-- Does the schema reside in the Component Registry? -->
    <sch:pattern>
        <sch:title>Test xsi:schemaLocation</sch:title>
        <sch:rule role="error" context="/cmd:CMD">
            <sch:assert test="matches(@xsi:schemaLocation,'http(s)?://catalog.clarin.eu/ds/ComponentRegistry/rest/')">
                /cmd:CMD/@xsi:schemaLocation doesn't refer to a schema from the Component Registry! [Actual value was [<value-of select="@xsi:schemaLocation"/>]
            </sch:assert>
        </sch:rule>
    </sch:pattern>

    <!-- Can we determine the profile used? -->
    <sch:pattern>
        <sch:title>Test for known profile</sch:title>
        <sch:rule role="error" context="/cmd:CMD">
            <sch:assert test="matches(@xsi:schemaLocation,'clarin.eu:cr[0-9]+:p_[0-9]+') or matches(cmd:Header/cmd:MdProfile,'clarin.eu:cr[0-9]+:p_[0-9]+')">
                The CMD profile of this record can't be found in the /cmd:CMD/@xsi:schemaLocation or /cmd:CMD/cmd:Header/cmd:MdProfile. The profile should be known for the record to be processed properly in the CLARIN joint metadata domain!
            </sch:assert>
        </sch:rule>
    </sch:pattern>

    <!-- Do the MdProfile and the @xsi:schemaLocation refer to the same profile? -->
    <sch:pattern>
        <sch:title>Test if MdProfile and @xsi:schemaLocation are in sync</sch:title>
        <sch:rule role="error" context="/cmd:CMD[matches(@xsi:schemaLocation,'clarin.eu:cr[0-9]+:p_[0-9]+') and matches(cmd:Header/cmd:MdProfile,'clarin.eu:cr[0-9]+:p_[0-9]+')]">
            <sch:assert test="replace(@xsi:schemaLocation,'.*(clarin.eu:cr[0-9]+:p_[0-9]+).*','$1') = replace(cmd:Header/cmd:MdProfile,'.*(clarin.eu:cr[0-9]+:p_[0-9]+).*','$1')">
                The CMD profile referenced in the @xsi:schemaLocation[<sch:value-of select="replace(@xsi:schemaLocation,'.*(clarin.eu:cr[0-9]+:p_[0-9]+).*','$1')"/>] is different than the one specified in /cmd:CMD/cmd:Header/cmd:MdProfile[<sch:value-of select="replace(cmd:Header/cmd:MdProfile,'.*(clarin.eu:cr[0-9]+:p_[0-9]+).*','$1')"/>]. They should be the same!
            </sch:assert>
        </sch:rule>
    </sch:pattern>

    <!-- Is the CMD namespace bound to a schema? -->
    <sch:pattern>
        <sch:title>Test for CMD namespace schema binding</sch:title>
        <sch:rule role="warning" context="/cmd:CMD">
            <sch:assert test="matches(@xsi:schemaLocation,'http://www.clarin.eu/cmd/1 ')">
                Is the CMD 1.2 namespace bound to the envelop schema?
            </sch:assert>
        </sch:rule>
    </sch:pattern>

    <!-- Is the CMD profile namespace bound to a schema? -->
    <sch:pattern>
        <sch:title>Test for CMD profile namespace schema binding</sch:title>
        <sch:rule role="warning" context="/cmd:CMD">
            <sch:assert test="matches(@xsi:schemaLocation,'http://www.clarin.eu/cmd/1/profiles/')">
                Is the CMD 1.2 profile namespace bound to the profile schema?
            </sch:assert>
        </sch:rule>
    </sch:pattern>

    <!-- Is the cmd:CMD root there? -->
    <sch:pattern>
        <sch:title>Test for cmd:CMD root</sch:title>
        <sch:rule role="warning" context="/">
            <sch:assert test="exists(cmd:CMD)">
                Is this really a CMD record? Is the namespace properly declared, e.g., including ending slash?
            </sch:assert>
        </sch:rule>
    </sch:pattern>

    <!-- Is the profile known? -->
    <sch:pattern>
        <sch:title>Test for known profile</sch:title>
        <sch:rule role="error" context="/cmd:CMD">
            <sch:let name="profile" value="distinct-values((replace(@xsi:schemaLocation,'.*(clarin.eu:cr[0-9]+:p_[0-9]+).*','$1'),replace(cmd:Header/cmd:MdProfile,'.*(clarin.eu:cr[0-9]+:p_[0-9]+).*','$1')))"/>
            <sch:let name="allowed" value="('clarin.eu:cr1:p_1527668176011')"/>
            <sch:assert test="$profile = $allowed">
                The CMD profile[<value-of select="string-join($profile,', ')"/>] of this record is not allowed by the VRE! Allowed profiles are [<value-of select="string-join($allowed,', ')"/>].
            </sch:assert>
        </sch:rule>
    </sch:pattern>
</sch:schema>