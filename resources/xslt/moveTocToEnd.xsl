<?xml version="1.0" encoding="utf-8"?>

<!--
    Move the list of contents to the end of the front matter.

    The DTBook to LaTeX conversion replaces the list of contents in the front matter by a generated
    table of contents, and prints it where that list is. The conversion in Pipeline 1 printed it at
    the end of the front matter instead, whatever the order of the document, which is where our
    books have it.

    In most of our books the list of contents is the last thing in the front matter anyway and this
    changes nothing. In the ones where it sits right after the title page, with the jacket copy and
    the like behind it, this puts it back where it belongs.

    A document without a list of contents is left alone.
-->

<xsl:stylesheet version="2.0"
		xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:dtb="http://www.daisy.org/z3986/2005/dtbook/"
		exclude-result-prefixes="#all">

  <xsl:output method="xml" encoding="utf-8" indent="no"/>

  <xsl:template match="dtb:frontmatter[dtb:level1[dtb:list[descendant::dtb:lic]]]">
    <!-- the same shape that the conversion looks for -->
    <xsl:variable name="toc" select="dtb:level1[dtb:list[descendant::dtb:lic]]"/>
    <xsl:copy>
      <xsl:apply-templates select="@*"/>
      <xsl:apply-templates select="node() except $toc"/>
      <xsl:apply-templates select="$toc"/>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="node()|@*">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>

</xsl:stylesheet>
