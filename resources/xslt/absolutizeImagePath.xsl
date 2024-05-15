<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="2.0"
                xmlns="http://www.daisy.org/z3986/2005/dtbook/"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:dtb="http://www.daisy.org/z3986/2005/dtbook/"
                exclude-result-prefixes="dtb">

  <xsl:param name="absolute-image-path"/>

  <xsl:output method="xml" encoding="utf-8" indent="no"
              doctype-public="-//NISO//DTD dtbook 2005-3//EN"
              doctype-system="http://www.daisy.org/z3986/2005/dtbook-2005-3.dtd" />

  <xsl:template match="dtb:img/@src">
    <xsl:attribute name="src">
      <xsl:choose>
	<!-- Only make the url absolute if it isn't already -->
	<xsl:when test="matches(., '^([a-z+]+:)?//', 'i')">
	  <!-- we are looking at an absolute url, no need to make it absolute -->
	  <xsl:value-of select="."/>
	</xsl:when>
	<xsl:when test="starts-with(.,'/')">
	  <!-- we are looking at a relative url starting with '/' -->
	  <xsl:value-of select="concat($absolute-image-path, .)"/>
	</xsl:when>
	<xsl:otherwise>
	  <!-- we are looking at a relative url -->
	  <xsl:value-of select="concat($absolute-image-path, '/', .)"/>
	</xsl:otherwise>
      </xsl:choose>
    </xsl:attribute>
  </xsl:template>

  <!-- Copy all other elements and attributes -->
  <xsl:template match="node()|@*">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>

</xsl:stylesheet>
