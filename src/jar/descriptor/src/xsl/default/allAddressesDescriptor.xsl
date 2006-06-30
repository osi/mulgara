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
              select $address $addressNumber $userId $node
                subquery(
                  select $personal
                    from <@@model@@>
                    where ( $node <http://tucana.org/tucana/EmailAddress#personal> $personal ) )
                subquery(
                  select $domain
                    from <@@model@@>
                    where ( $node <http://tucana.org/tucana/EmailAddress#domain> $domain ) )
                count( select $docURI
                  from <@@model@@>
                  where (
                    ( $docURI <@@predicate@@> $addressNode ) and
                    ( $addressNode <http://tucana.org/tucana/EmailAddress#address> $address ) and
                    ( $addressNode <http://tucana.org/tucana/EmailAddress#addressNumber> $addressNumber ) and
                    ( $addressNode <http://tucana.org/tucana/EmailAddress#userId> $userId ) ) )
              from <@@model@@>
              where
                ( <@@document@@> <@@predicate@@> $node ) and
                ( $node <http://tucana.org/tucana/EmailAddress#address> $address ) and
                ( $node <http://tucana.org/tucana/EmailAddress#addressNumber> $addressNumber ) and
                ( $node <http://tucana.org/tucana/EmailAddress#userId> $userId ) ;
            ]]>
          </tucanaDescriptor:query>
        </xsl:variable>
        <!-- Now apply the templates to the answer -->
        <xsl:apply-templates select="xalan:nodeset($answer)/*"/>
        <!--<xsl:copy-of select="xalan:nodeset($answer)/*"/>-->
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>


  <!-- #################################################################### -->
  <!-- Addresses grouping element -->
  <!-- #################################################################### -->
  <xsl:template match="tucanaAnswer:answer">
    <desc:property desc:predicate="{$predicate}">
    <xsl:apply-templates/>
    </desc:property>
  </xsl:template>


  <!-- #################################################################### -->
  <!-- Converts dates to nice XML format -->
  <!-- #################################################################### -->
  <xsl:template match="tucanaAnswer:solution">
    <xsl:if test="tucanaAnswer:address|tucanaAnswer:personal|tucanaAnswer:addressNumber|tucanaAnswer:domain|tucanaAnswer:userId">
      <desc:emailAddress desc:count="{tucanaAnswer:k2/text()}">
      <xsl:if test="tucanaAnswer:address">
        <desc:address><xsl:value-of select="tucanaAnswer:address/text()"/></desc:address>
      </xsl:if>
      <xsl:if test="tucanaAnswer:k0/tucanaAnswer:solution/tucanaAnswer:personal">
        <desc:personal><xsl:value-of select="tucanaAnswer:k0/tucanaAnswer:solution/tucanaAnswer:personal/text()"/></desc:personal>
      </xsl:if>
      <xsl:if test="tucanaAnswer:addressNumber">
        <desc:addressNumber><xsl:value-of select="tucanaAnswer:addressNumber/text()"/></desc:addressNumber>
      </xsl:if>
      <xsl:if test="tucanaAnswer:k1/tucanaAnswer:solution/tucanaAnswer:domain">
        <desc:domain><xsl:value-of select="tucanaAnswer:domain/text()"/></desc:domain>
      </xsl:if>
      <xsl:if test="tucanaAnswer:userId">
        <desc:userId><xsl:value-of select="tucanaAnswer:userId/text()"/></desc:userId>
      </xsl:if>
      </desc:emailAddress>
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

        <dc:title>Extracts email Addresses from store</dc:title>

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
            <desc:description>The predicate to query on the document to find Address Types</desc:description>
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
