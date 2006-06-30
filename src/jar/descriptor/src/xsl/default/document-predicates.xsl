<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet
  version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:dc="http://purl.org/dc/elements/1.1/"
  xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
  xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
  xmlns:lxslt="http://xml.apache.org/xslt"
  xmlns:xalan="http://xml.apache.org/xalan"
  xmlns:tucanaDescriptor="tucanaDescriptor"
  extension-element-prefixes="tucanaDescriptor"
  exclude-result-prefixes="xsl rdf rdfs lxslt tucana xalan ns1"
  xmlns:tucana="http://tucana.org/tql#"
  xmlns:desc="http://tucana.org/descriptor#"
  xmlns:doc="http://tucana.org/tucana/Document#"
  xmlns:ns1="urn:Query">


  <!-- ============================================== -->
  <!-- Assemble the parameters -->
  <!-- ============================================== -->
  <!-- DESCRIPTOR PARAMS -->
  <xsl:param name="_usage"/> <!-- if set return the USAGE RDF -->
  <xsl:param name="_self"/> <!-- the URL of this descriptor -->

  <!-- USER PARAMS -->
  <xsl:param name="node"/>
  <xsl:param name="model"/>

  <!-- #################################################################### -->
  <!-- Top level template                                                   -->
  <!-- #################################################################### -->
  <xsl:template match="/">
    <xsl:choose>
      <xsl:when test="$_usage">
        <xsl:call-template name="usage"/>
      </xsl:when>
      <xsl:otherwise>
        <desc:Document>

         <!-- get the concepts (concept-based) -->
         <tucanaDescriptor:descriptor
           _target="concept-based-concepts-descriptor.xsl"
           _source="{$_self}"
           document="{$node}"
           model="{$model}"/>

          <!--TODO score generator quantity -->
          <xsl:variable name="answer">
          <tucanaDescriptor:query model="{$model}" node="{$node}">
            <![CDATA[
            select $predicate from <@@model@@> where <@@node@@> $predicate $object;
            ]]>
          </tucanaDescriptor:query>
          </xsl:variable>
          <!-- uncomment to see the raw XML response-->
          <!--
          <xsl:copy-of select="xalan:nodeset($answer)/*"/>
          -->
          <!-- Now apply the templates to the answer -->
          <xsl:apply-templates select="xalan:nodeset($answer)/*"/>
        </desc:Document>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>


  <!-- #################################################################### -->
  <!-- Pass title to other descriptor                                       -->
  <!-- #################################################################### -->
  <xsl:template match="tucana:solution/tucana:predicate[@resource='http://tucana.org/tucana/Document#title']">
    <tucanaDescriptor:descriptor
      _target="titles-descriptor.xsl"
      _source="{$_self}"
      document="{$node}"
      model="{$model}"/>
    <xsl:apply-templates/>
  </xsl:template>


  <!-- #################################################################### -->
  <!-- Pass subject to other descriptor                                       -->
  <!-- #################################################################### -->
  <xsl:template match="tucana:solution/tucana:predicate[@resource='http://tucana.org/tucana/Document#subject']">
    <tucanaDescriptor:descriptor
      _target="subjects-descriptor.xsl"
      _source="{$_self}"
      document="{$node}"
      model="{$model}"/>
    <xsl:apply-templates/>
  </xsl:template>


  <!-- #################################################################### -->
  <!-- Pass message ID to other descriptor                                  -->
  <!-- #################################################################### -->
  <xsl:template match="tucana:solution/tucana:predicate[@resource='http://tucana.org/tucana/Document#messageID']">
    <tucanaDescriptor:descriptor
      _target="messageID-descriptor.xsl"
      _source="{$_self}"
      document="{$node}"
      model="{$model}"/>
    <xsl:apply-templates/>
  </xsl:template>


  <!-- #################################################################### -->
  <!-- Pass folder to other descriptor                                      -->
  <!-- #################################################################### -->
  <xsl:template match="tucana:solution/tucana:predicate[@resource='http://tucana.org/tucana/Document#folder']">
    <tucanaDescriptor:descriptor
      _target="folders-descriptor.xsl"
      _source="{$_self}"
      document="{$node}"
      model="{$model}"/>
    <xsl:apply-templates/>
  </xsl:template>


  <!-- #################################################################### -->
  <!-- Pass parent to other descriptor                                      -->
  <!-- #################################################################### -->
  <xsl:template match="tucana:solution/tucana:predicate[@resource='http://tucana.org/tucana/Document#parent']">
    <tucanaDescriptor:descriptor
      _target="parents-descriptor.xsl"
      _source="{$_self}"
      document="{$node}"
      model="{$model}"/>
    <xsl:apply-templates/>
  </xsl:template>


  <!-- #################################################################### -->
  <!-- Pass child to other descriptor                                      -->
  <!-- #################################################################### -->
  <xsl:template match="tucana:solution/tucana:predicate[@resource='http://tucana.org/tucana/Document#child']">
    <tucanaDescriptor:descriptor
      _target="children-descriptor.xsl"
      _source="{$_self}"
      document="{$node}"
      model="{$model}"/>
    <xsl:apply-templates/>
  </xsl:template>


  <!-- #################################################################### -->
  <!-- Pass hasEmailAddress to other descriptor                              -->
  <!-- #################################################################### -->
  <xsl:template match="tucana:solution/tucana:predicate[@resource='http://tucana.org/tucana/Document#hasEmailAddress']">
    <!--
    Got Address - calling address descriptor with node <xsl:value-of select="{@resource}"/> and
    mode <xsl:value-of select="{$model}"/>
    -->
    <tucanaDescriptor:descriptor
      _target="allAddressesDescriptor.xsl"
      _source="{$_self}"
      document="{$node}"
      model="{$model}"
      predicate="{@resource}"/>
    <xsl:apply-templates/>
  </xsl:template>


  <!-- #################################################################### -->
  <!-- Pass toAddress to other descriptor                                   -->
  <!-- #################################################################### -->
  <xsl:template match="tucana:solution/tucana:predicate[@resource='http://tucana.org/tucana/Document#toAddress']">
    <!--
    Got Address - calling address descriptor with node <xsl:value-of select="{@resource}"/> and
    mode <xsl:value-of select="{$model}"/>
    -->
    <tucanaDescriptor:descriptor
      _target="allAddressesDescriptor.xsl"
      _source="{$_self}"
      document="{$node}"
      model="{$model}"
      predicate="{@resource}"/>
    <xsl:apply-templates/>
  </xsl:template>


  <!-- #################################################################### -->
  <!-- Pass fromAddress to other descriptor                                   -->
  <!-- #################################################################### -->
  <xsl:template match="tucana:solution/tucana:predicate[@resource='http://tucana.org/tucana/Document#fromAddress']">
    <!--
    Got Address - calling address descriptor with node <xsl:value-of select="{@resource}"/> and
    mode <xsl:value-of select="{$model}"/>
    -->
    <tucanaDescriptor:descriptor
      _target="allAddressesDescriptor.xsl"
      _source="{$_self}"
      document="{$node}"
      model="{$model}"
      predicate="{@resource}"/>
    <xsl:apply-templates/>
  </xsl:template>



  <!-- #################################################################### -->
  <!-- Pass ccAddress to other descriptor                                   -->
  <!-- #################################################################### -->
  <xsl:template match="tucana:solution/tucana:predicate[@resource='http://tucana.org/tucana/Document#ccAddress']">
    <!--
    Got Address - calling address descriptor with node <xsl:value-of select="{@resource}"/> and
    mode <xsl:value-of select="{$model}"/>
    -->
    <tucanaDescriptor:descriptor
      _target="allAddressesDescriptor.xsl"
      _source="{$_self}"
      document="{$node}"
      model="{$model}"
      predicate="{@resource}"/>
    <xsl:apply-templates/>
  </xsl:template>


  <!-- #################################################################### -->
  <!-- Pass replyToAddress to other descriptor                              -->
  <!-- #################################################################### -->
  <xsl:template match="tucana:solution/tucana:predicate[@resource='http://tucana.org/tucana/Document#replyToAddress']">
    <!--
    Got Address - calling address descriptor with node <xsl:value-of select="{@resource}"/> and
    mode <xsl:value-of select="{$model}"/>
    -->
    <tucanaDescriptor:descriptor
      _target="allAddressesDescriptor.xsl"
      _source="{$_self}"
      document="{$node}"
      model="{$model}"
      predicate="{@resource}"/>
    <xsl:apply-templates/>
  </xsl:template>


  <!-- #################################################################### -->
  <!-- Pass containsDate to other descriptor                                -->
  <!-- #################################################################### -->
  <xsl:template match="tucana:solution/tucana:predicate[@resource='http://tucana.org/tucana/Document#containsDate']">
    <!--
    Got Date - calling date descriptor with node <xsl:value-of select="{@resource}"/> and
    mode <xsl:value-of select="{$model}"/>
    -->
    <tucanaDescriptor:descriptor
      _target="allDatesDescriptor.xsl"
      _source="{$_self}"
      document="{$node}"
      model="{$model}"
      predicate="{@resource}"/>
    <xsl:apply-templates/>
  </xsl:template>


  <!-- #################################################################### -->
  <!-- Pass publishedDate to other descriptor (special kind of date)        -->
  <!-- #################################################################### -->
  <xsl:template match="tucana:solution/tucana:predicate[@resource='http://tucana.org/tucana/Document#publishedDate']">
    <!--
    Got Date - calling date descriptor with node <xsl:value-of select="{@resource}"/> and
    mode <xsl:value-of select="{$model}"/>
    -->
    <tucanaDescriptor:descriptor
      _target="allDatesGenericDescriptor.xsl"
      _source="{$_self}"
      document="{$node}"
      model="{$model}"
      predicate="{@resource}"/>
    <xsl:apply-templates/>
  </xsl:template>


  <!-- #################################################################### -->
  <!-- Pass postingDate to other descriptor (special kind of date)          -->
  <!-- #################################################################### -->
  <xsl:template match="tucana:solution/tucana:predicate[@resource='http://tucana.org/tucana/Document#postingDate']">
    <!--
    Got Date - calling date descriptor with node <xsl:value-of select="{@resource}"/> and
    mode <xsl:value-of select="{$model}"/>
    -->
    <tucanaDescriptor:descriptor
      _target="allDatesGenericDescriptor.xsl"
      _source="{$_self}"
      document="{$node}"
      model="{$model}"
      predicate="{@resource}"/>
    <xsl:apply-templates/>
  </xsl:template>


  <!-- #################################################################### -->
  <!-- Pass category to other descriptor                                       -->
  <!-- #################################################################### -->
  <xsl:template match="tucana:solution/tucana:predicate[@resource='http://tucana.org/tucana/Document#category']">
    <tucanaDescriptor:descriptor
      _target="categories-descriptor.xsl"
      _source="{$_self}"
      document="{$node}"
      model="{$model}"/>
    <xsl:apply-templates/>
  </xsl:template>

  
  <!-- #################################################################### -->
  <!-- Pass case names to other descriptor                              -->
  <!-- #################################################################### -->
  <xsl:template match="tucana:solution/tucana:predicate[@resource='http://tucana.org/tucana/Document#hasCaseName']">
    <tucanaDescriptor:descriptor
      _target="caseNames-descriptor.xsl"
      _source="{$_self}"
      document="{$node}"
      model="{$model}"/>
    <xsl:apply-templates/>
  </xsl:template>


  <!-- #################################################################### -->
  <!-- Pass case citations to other descriptor                              -->
  <!-- #################################################################### -->
  <xsl:template match="tucana:solution/tucana:predicate[@resource='http://tucana.org/tucana/Document#hasCaseCitation']">
    <tucanaDescriptor:descriptor
      _target="caseCitations-descriptor.xsl"
      _source="{$_self}"
      document="{$node}"
      model="{$model}"/>
    <xsl:apply-templates/>
  </xsl:template>


  <!-- #################################################################### -->
  <!-- Pass statute citations to other descriptor                           -->
  <!-- #################################################################### -->
  <xsl:template match="tucana:solution/tucana:predicate[@resource='http://tucana.org/tucana/Document#hasStatuteCitation']">
    <tucanaDescriptor:descriptor
      _target="statuteCitations-descriptor.xsl"
      _source="{$_self}"
      document="{$node}"
      model="{$model}"/>
    <xsl:apply-templates/>
  </xsl:template>


  <!-- #################################################################### -->
  <!-- Pass genericFeatures to other descriptor                                       -->
  <!-- #################################################################### -->
  <xsl:template match="tucana:solution/tucana:predicate[@resource='http://tucana.org/tucana/Document#hasGenericFeature']">
    <tucanaDescriptor:descriptor
      _target="genericFeature-descriptor.xsl"
      _source="{$_self}"
      document="{$node}"
      model="{$model}"/>
    <xsl:apply-templates/>
  </xsl:template>


  <!-- #################################################################### -->
  <!-- Pass personalNames to other descriptor                                       -->
  <!-- #################################################################### -->
  <xsl:template match="tucana:solution/tucana:predicate[@resource='http://tucana.org/tucana/Document#hasPersonalName']">
    <tucanaDescriptor:descriptor
      _target="allPersonsDescriptor.xsl"
      _source="{$_self}"
      document="{$node}"
      model="{$model}"
      predicate="{@resource}"/>
    <xsl:apply-templates/>
  </xsl:template>


  <!-- #################################################################### -->
  <!-- Pass companyNames to other descriptor                                       -->
  <!-- #################################################################### -->
  <xsl:template match="tucana:solution/tucana:predicate[@resource='http://tucana.org/tucana/Document#hasCompanyName']|tucana:solution/tucana:predicate[@resource='http://tucana.org/tucana/Document#companyAuthor']">
    <tucanaDescriptor:descriptor
      _target="allCompaniesDescriptor.xsl"
      _source="{$_self}"
      document="{$node}"
      model="{$model}"
      predicate="{@resource}"/>
    <xsl:apply-templates/>
  </xsl:template>


  <!-- #################################################################### -->
  <!-- Pass geographicFeature to other descriptor                           -->
  <!-- #################################################################### -->
  <xsl:template match="tucana:solution/tucana:predicate[@resource='http://tucana.org/tucana/Document#hasGeographicFeature']">
    <tucanaDescriptor:descriptor
      _target="geographicFeature-descriptor.xsl"
      _source="{$_self}"
      document="{$node}"
      model="{$model}"
      predicate="{@resource}"/>
    <xsl:apply-templates/>
  </xsl:template>

  
  <!-- #################################################################### -->
  <!-- Pass concept to other descriptor                                     -->
  <!-- #################################################################### -->
  <xsl:template match="tucana:solution/tucana:predicate[@resource='http://tucana.org/tucana/Document#hasConcept']">
    <tucanaDescriptor:descriptor
      _target="document-based-concepts-descriptor.xsl"
      _source="{$_self}"
      document="{$node}"
      model="{$model}"
      predicate="{@resource}"/>
    <xsl:apply-templates/>
  </xsl:template>


  <!-- #################################################################### -->
  <!-- Calls a java class for queries -->
  <!-- #################################################################### -->
  <lxslt:component prefix="tucanaDescriptor" elements="descriptor query" functions="test">
    <lxslt:script lang="javaclass" src="xalan://org.kowari.descriptor.DescriptorElement"/>
  </lxslt:component>

  <!-- #################################################################### -->
  <!-- Match on no metadata for a document                                  -->
  <!-- #################################################################### -->
  <xsl:template match="ns1:executeResponse/return[not(tucana:answer)]">
    <p>No metadata exists for the given document.</p>
    <xsl:apply-templates/>
  </xsl:template>


  <!-- #################################################################### -->
  <!-- Match on the solutions (document metadata)                           -->
  <!-- #################################################################### -->
  <xsl:template match="tucana:title">
    <desc:title><xsl:value-of select="./text()"/></desc:title>
    <xsl:apply-templates/>
  </xsl:template>

  <!-- #################################################################### -->
  <!-- Match on the solutions (document metadata)                           -->
  <!-- #################################################################### -->
  <xsl:template match="tucana:solution">
    <!--Got solution applying templates -->
    <xsl:apply-templates/>
  </xsl:template>


  <!-- #################################################################### -->
  <!-- Default catch all template                                           -->
  <!-- #################################################################### -->
  <xsl:template match="*/text()">
    <xsl:apply-templates/>
  </xsl:template>

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

    <!-- TODO define parameters with XML Schemas -->
    <desc:Descriptor rdf:about="">

      <dc:title>Extracts Documents from store</dc:title>

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
          <desc:name>node</desc:name>
          <desc:description>The URL of the document</desc:description>
          <desc:type>String</desc:type>
          <desc:required>Yes</desc:required>
        </desc:Param>
      </desc:hasParam>

      <desc:hasParam>
        <desc:Param>
          <desc:name>model</desc:name>
          <desc:description>The URI of the model to query</desc:description>
          <desc:type>String</desc:type>
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
