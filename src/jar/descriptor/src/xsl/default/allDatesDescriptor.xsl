<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet
  version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
  xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
  xmlns:dc="http://purl.org/dc/elements/1.1/"
  xmlns:lxslt="http://xml.apache.org/xslt"
  xmlns:xalan="http://xml.apache.org/xalan"
  xmlns:tucanaDescriptor="tucanaDescriptor"
  extension-element-prefixes="tucanaDescriptor"
  exclude-result-prefixes="xsl rdf rdfs lxslt tucanaAnswer xalan ns1"
  xmlns:tucanaAnswer="http://tucana.org/tql#"
  xmlns:desc="http://tucana.org/descriptor#"
  xmlns:ns1="urn:Query">

  <!-- ============================================== -->
  <!-- Assemble the parameters -->
  <!-- ============================================== -->

  <!-- DESCRIPTOR PARAMS -->
  <xsl:param name="_usage"/> <!-- is set return the USAGE RDF -->
  <xsl:param name="_self"/> <!-- the URL of this descriptor -->

  <!-- USER PARAMS -->
  <xsl:param name="document"/>
  <xsl:param name="model"/>
  <xsl:param name="predicate"/>

  <!-- ============================================== -->
  <!-- Match the Solution -->
  <!-- ============================================== -->
  <xsl:template match="/">
    <xsl:choose>
      <xsl:when test="$_usage">
        <xsl:call-template name="usage"/>
      </xsl:when>
      <xsl:otherwise>
        <!--
          <debug>Searching node...<xsl:value-of select="$node"/> in model <xsl:value-of select="$model"/></debug>
          -->
        <!-- EXECUTE THE QUERY to get a date -->
        <xsl:variable name="answer">
          <tucanaDescriptor:query model="{$model}" document="{$document}" predicate="{$predicate}">
            <![CDATA[
              select $day $month $year $score
              count( select $allDates
              from <@@model@@>
              where (
              ( $allDates <@@predicate@@>  $dateNode ) and
              ( $dateNode <http://tucana.org/tucana/tool/DateExtractor#day> $day ) and
              ( $dateNode  <http://tucana.org/tucana/tool/DateExtractor#month> $month ) and
              ( $dateNode  <http://tucana.org/tucana/tool/DateExtractor#year> $year ) ) )
              from <@@model@@> 
              where ( <@@document@@> <@@predicate@@>  $node ) and
              ( $node <http://tucana.org/tucana/tool/DateExtractor#day> $day ) and
              ( $node  <http://tucana.org/tucana/tool/DateExtractor#month> $month ) and
              ( $node  <http://tucana.org/tucana/tool/DateExtractor#year> $year ) and
              ( $id <http://www.w3.org/1999/02/22-rdf-syntax-ns#object>  $node  ) and
              ( $id <http://tucana.org/tucana/Document#score> $score );
            ]]>
            <!--[CDATA[
            select $day $month $year $score from <@@model@@> where
            ( <@@document@@> <@@predicate@@>  $node ) and
            ( $node <http://tucana.org/tucana/tool/DateExtractor#day> $day ) and
            ( $node  <http://tucana.org/tucana/tool/DateExtractor#month> $month ) and
            ( $node  <http://tucana.org/tucana/tool/DateExtractor#year> $year ) and
            ( $id <http://www.w3.org/1999/02/22-rdf-syntax-ns#object>  $node  ) and
            ( $id <http://tucana.org/tucana/Document#score> $score );
            ]]-->
          </tucanaDescriptor:query>
        </xsl:variable>
        <!-- Now apply the templates to the answer -->
        <xsl:apply-templates select="xalan:nodeset($answer)/*"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>


  <!-- #################################################################### -->
  <!-- DATES grouping element -->
  <!-- #################################################################### -->
  <xsl:template match="tucanaAnswer:answer">
    <desc:property desc:predicate="{$predicate}">
    <xsl:apply-templates/>
    </desc:property>
  </xsl:template>


  <!-- #################################################################### -->
  <!-- converts dates to nice XML format -->
  <!-- #################################################################### -->
  <xsl:template match="tucanaAnswer:solution">
      <xsl:if test="tucanaAnswer:month|tucanaAnswer:day|tucanaAnswer:year|tucanaAnswer:hour|tucanaAnswer:minute|tucanaAnswer:second">
        <desc:date desc:count="{tucanaAnswer:k0/text()}">
        <xsl:if test="tucanaAnswer:second">
          <desc:second><xsl:value-of select="tucanaAnswer:second/text()"/></desc:second>
        </xsl:if>
        <xsl:if test="tucanaAnswer:minute">
          <desc:minute><xsl:value-of select="tucanaAnswer:minute/text()"/></desc:minute>
        </xsl:if>
        <xsl:if test="tucanaAnswer:hour">
          <desc:hour><xsl:value-of select="tucanaAnswer:hour/text()"/></desc:hour>
        </xsl:if>
        <xsl:if test="tucanaAnswer:day">
          <desc:day><xsl:value-of select="tucanaAnswer:day/text()"/></desc:day>
        </xsl:if>
        <xsl:if test="tucanaAnswer:month">
          <desc:month><xsl:value-of select="tucanaAnswer:month/text()"/></desc:month>
        </xsl:if>
        <xsl:if test="tucanaAnswer:year">
          <desc:year><xsl:value-of select="tucanaAnswer:year/text()"/></desc:year>
        </xsl:if>
        </desc:date>
      </xsl:if>
  </xsl:template>

  <!-- #################################################################### -->
  <!-- Calls a java class for queries -->
  <!-- #################################################################### -->
  <lxslt:component prefix="tucanaDescriptor" elements="descriptor query debug" functions="test">
    <lxslt:script lang="javaclass" src="xalan://org.kowari.descriptor.DescriptorElement"/>
  </lxslt:component>


  <!-- #################################################################### -->
  <!-- USAGE of this descriptor                                             -->
  <!-- #################################################################### -->
  <xsl:template name="usage">
    <rdf:RDF
      xml:lang="en"
      xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
      xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
      xmlns:dc="http://purl.org/dc/elements/1.1/"
      xmlns:desc="http://tucana.org/descriptor#">

      <desc:Descriptor rdf:about="">

        <dc:title>Extracts Dates from store</dc:title>

        <desc:hasParam>
          <desc:Param>
            <desc:name>_self</desc:name>
            <desc:type>String</desc:type>
            <desc:description>The URL of this Descriptor</desc:description>
            <desc:required>Yes</desc:required>
          </desc:Param>
        </desc:hasParam>

        <desc:hasParam>
          <desc:Param>
            <desc:name>document</desc:name>
            <desc:type>String</desc:type>
            <desc:description>The URI of the document</desc:description>
            <desc:required>Yes</desc:required>
          </desc:Param>
        </desc:hasParam>

        <desc:hasParam>
          <desc:Param>
            <desc:name>model</desc:name>
            <desc:type>String</desc:type>
            <desc:description>The URI of the model to query</desc:description>
            <desc:required>Yes</desc:required>
          </desc:Param>
        </desc:hasParam>

        <desc:hasParam>
          <desc:Param>
            <desc:name>predicate</desc:name>
            <desc:type>String</desc:type>
            <desc:description>The predicate to query on the document to find Date Types</desc:description>
            <desc:required>Yes</desc:required>
          </desc:Param>
        </desc:hasParam>

        <!-- MIME TYPE -->
        <desc:hasMimetype>
          <desc:Mimetype>
            <desc:mime-major>text</desc:mime-major>
            <desc:mime-minor>xml</desc:mime-minor>
          </desc:Mimetype>
        </desc:hasMimetype>
      </desc:Descriptor>

    </rdf:RDF>
  </xsl:template>

</xsl:stylesheet>
