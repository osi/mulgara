<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet
  version="1.0"
  xmlns="http://www.w3.org/1999/xhtml"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
  xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
  xmlns:tucanaAnswer="http://tucana.org/tql#"
  xmlns:desc="http://tucana.org/descriptor#"
  xmlns:dc="http://purl.org/dc/elements/1.1/"
  exclude-result-prefixes="lxslt tucanaAnswer xalan ns1 desc"
  xmlns:ns1="urn:Query">


  <!-- ============================================== -->
  <!-- extract the RDF                                -->
  <!-- ============================================== -->
  <xsl:template match="xsl:template[@name='usage']/rdf:RDF">
    <xsl:copy-of select="."/>
  </xsl:template>

  <!-- ============================================== -->
  <!-- gobble other tags                              -->
  <!-- ============================================== -->
  <xsl:template match="*/text()">
    <xsl:apply-templates/>
  </xsl:template>

</xsl:stylesheet>
