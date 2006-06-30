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
  <xsl:param name="_usage"/> <!-- if set return the USAGE RDF -->
  <xsl:param name="_self"/> <!-- the URL of this descriptor -->

  <!-- USER PARAMS -->
  <xsl:param name="document"/>
  <xsl:param name="model"/>

  <!-- ============================================== -->
  <!-- Match the Solution -->
  <!-- ============================================== -->
  <xsl:template match="/">
    <xsl:choose>
      <xsl:when test="$_usage">
        <xsl:call-template name="usage"/>
      </xsl:when>
      <xsl:otherwise>
        <!-- EXECUTE THE QUERY to get a citation -->
        <xsl:variable name="answer">
          <tucanaDescriptor:query model="{$model}" document="{$document}">
            <![CDATA[
            select $docNode $citationName
              count (
                select $allCitations
                from
                  <@@model@@>
                where
                  $allCitations <http://tucana.org/tucana/Document#hasStatuteCitation> $citationNode and
                  $citationNode <http://tucana.org/tucana/tool/FeatureExtractor#name> $citationName )
              subquery(
                select $address
                from
                  <@@model@@>
                where
                  $docNode <http://tucana.org/tucana/tool/FeatureExtractor#address> $address )
            from
              <@@model@@>
            where
              (<@@document@@> <http://tucana.org/tucana/Document#hasStatuteCitation> $docNode) and
              ($docNode <http://tucana.org/tucana/tool/FeatureExtractor#name> $citationName );
            ]]>
          </tucanaDescriptor:query>
        </xsl:variable>
        <!-- Now apply the templates to the answer -->
        <xsl:apply-templates select="xalan:nodeset($answer)/*"/>
        <!--
        <xsl:copy-of select="xalan:nodeset($answer)/*"/>
        -->
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>


  <!-- #################################################################### -->
  <!-- CITATIONS converts citations to nice XML format -->
  <!-- #################################################################### -->
  <xsl:template match="tucanaAnswer:answer">
    <desc:property desc:predicate="http://tucana.org/tucana/Document#hasStatuteCitation">
    <xsl:apply-templates/>
    </desc:property>
  </xsl:template>


  <!-- #################################################################### -->
  <!-- CITATIONS converts citations to nice XML format -->
  <!-- #################################################################### -->
  <xsl:template match="tucanaAnswer:solution">
    <xsl:if test="tucanaAnswer:citationName">
      <desc:citation desc:count="{tucanaAnswer:k0/text()}">
        <desc:citationName><xsl:value-of select="tucanaAnswer:citationName/text()"/></desc:citationName>
        <xsl:apply-templates/>
      </desc:citation>
    </xsl:if>
  </xsl:template>


  <!-- #################################################################### -->
  <!-- ADDRESSES converts citations ADDRESSES to nice XML format -->
  <!-- #################################################################### -->
  <xsl:template match="tucanaAnswer:k1/tucanaAnswer:solution">
    <desc:citationAddress><xsl:value-of select="tucanaAnswer:address/text()"/></desc:citationAddress>
  </xsl:template>


  <!-- #################################################################### -->
  <!-- Default catch all template                                           -->
  <!-- #################################################################### -->
  <xsl:template match="*/text()">
    <xsl:apply-templates/>
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

        <dc:title>Extracts Simple Citations from a single document in a store, not all
          citation elements are retrieved, this is suitable for a non editable view of a
          citation.
        </dc:title>

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

