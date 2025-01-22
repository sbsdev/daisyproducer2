<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="2.0"
		xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
		xmlns:dtb="http://www.daisy.org/z3986/2005/dtbook/"	
		xmlns:brl="http://www.daisy.org/z3986/2009/braille/"
		exclude-result-prefixes="dtb brl">

  <xsl:output method="text" encoding="utf-8" indent="no" />

  <!-- Inline elements do not need any spacing -->
  <xsl:template match="dtb:em|dtb:strong|dtb:span|dtb:sub|dtb:sup">
    <xsl:apply-templates/>
  </xsl:template>

  <!-- Add some spacing to (block) elements to avoid words getting glued together -->
  <xsl:template match="element()">
    <xsl:value-of select="' '"/>
    <xsl:apply-templates/>
    <xsl:value-of select="' '"/>
  </xsl:template>

  <xsl:template match="text()">
    <xsl:value-of select="string(.)"/>
  </xsl:template>
  
</xsl:stylesheet>
