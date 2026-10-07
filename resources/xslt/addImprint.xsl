<?xml version="1.0" encoding="utf-8"?>

<!--
    Add the imprint of the large print book to a DTBook.

    The conversion in Pipeline 2 has no SBS specific code, so the imprint is added to the
    document before it is converted, as a level1 with class "colophon".

    The conversion starts a page for that level and sets its last block at the foot of the
    page, which is why the lines of the imprint are a single linegroup.

    The imprint goes in front of the rest of the front matter, where it was printed before. A
    document that already has a colophon is left alone.
-->

<xsl:stylesheet version="2.0"
		xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:xs="http://www.w3.org/2001/XMLSchema"
		xmlns:dtb="http://www.daisy.org/z3986/2005/dtbook/"
		xmlns="http://www.daisy.org/z3986/2005/dtbook/"
		exclude-result-prefixes="#all">

  <xsl:output method="xml" encoding="utf-8" indent="no"/>

  <xsl:param name="notice">
    Dieses Grossdruckbuch ist die ausschliesslich für die Nutzung durch seh- und lesebehinderte
    Menschen bestimmte zugängliche Version eines urheberrechtlich geschützten Werks. Sie können es
    im Rahmen des Urheberrechts persönlich nutzen, dürfen es aber nicht weiter verbreiten oder
    öffentlich zugänglich machen.
  </xsl:param>

  <!-- only printed when the publisher provided the text of the book in electronic form -->
  <xsl:param name="thanks">
    Wir danken dem Verlag für die freundliche Bereitstellung der elektronischen Textdaten.
  </xsl:param>

  <xsl:param name="imprint-heading">Verlag, Satz und Druck:</xsl:param>

  <!-- A line break in the name is kept, both here and on the cover: the name is too long for one
       line and reads better broken after "Bibliothek" than wherever it happens to fit. -->
  <xsl:param name="publisher" as="xs:string"
	     select="'SBS Schweizerische Bibliothek&#10;für Blinde, Seh- und Lesebehinderte, Zürich'"/>

  <!-- the word in the name of the publisher in the metadata after which the cover breaks it -->
  <xsl:param name="break-publisher-after" as="xs:string" select="'Bibliothek'"/>
  <xsl:param name="url">www.sbs.ch</xsl:param>
  <xsl:param name="copyright-holder">SBS</xsl:param>

  <!-- the year the book was produced, from the metadata, or else the current year -->
  <xsl:variable name="year"
		select="(tokenize(//dtb:meta[lower-case(@name)='dc:date']/@content,'-')[1][normalize-space(.)],
			 string(year-from-date(current-date())))[1]"/>

  <xsl:variable name="electronic-data" as="xs:boolean"
		select="//dtb:meta[lower-case(@name)='prod:source']/@content = 'electronicData'"/>

  <xsl:variable name="has-colophon" as="xs:boolean"
		select="exists(//dtb:level1[tokenize(@class,'\s+')='colophon'])"/>

  <xsl:variable name="imprint" as="element()">
    <level1 class="colophon">
      <p><xsl:value-of select="normalize-space($notice)"/></p>
      <xsl:if test="$electronic-data">
	<!-- the Pipeline 1 conversion set this paragraph off from the notice with a \bigskip, which
	     is not something a DTBook can say. The class says what was meant, an empty line in
	     front of the paragraph, and the conversion decides how much space that is. -->
	<p class="precedingemptyline"><xsl:value-of select="normalize-space($thanks)"/></p>
      </xsl:if>
      <linegroup>
	<line><xsl:value-of select="normalize-space($imprint-heading)"/></line>
	<!-- One line, because it is one name: the break inside it is a br, which the conversion
	     sets tight, while separate lines are separate items and are set with air between them. -->
	<line>
	  <xsl:for-each select="tokenize($publisher,'\n')">
	    <xsl:if test="position() gt 1"><br/></xsl:if>
	    <xsl:value-of select="normalize-space(.)"/>
	  </xsl:for-each>
	</line>
	<line><xsl:value-of select="normalize-space($url)"/></line>
	<line><xsl:value-of select="concat(normalize-space($copyright-holder),' ',$year)"/></line>
      </linegroup>
    </level1>
  </xsl:variable>

  <!-- the imprint comes before the rest of the front matter, but after the title and the author of
       the document, which have to be the first children of frontmatter -->
  <xsl:template match="dtb:frontmatter">
    <xsl:copy>
      <xsl:apply-templates select="@*"/>
      <xsl:apply-templates select="dtb:doctitle|dtb:docauthor"/>
      <xsl:if test="not($has-colophon)">
	<xsl:copy-of select="$imprint"/>
      </xsl:if>
      <xsl:apply-templates select="node() except (dtb:doctitle|dtb:docauthor)"/>
    </xsl:copy>
  </xsl:template>

  <!-- a book without a front matter gets one -->
  <xsl:template match="dtb:book[not(dtb:frontmatter)]">
    <xsl:copy>
      <xsl:apply-templates select="@*"/>
      <xsl:if test="not($has-colophon)">
	<frontmatter>
	  <xsl:copy-of select="$imprint"/>
	</frontmatter>
      </xsl:if>
      <xsl:apply-templates/>
    </xsl:copy>
  </xsl:template>

  <!-- The cover takes the publisher from the metadata, where a line break cannot be marked up, so
       it is put there as a character. -->
  <xsl:template match="dtb:meta[@name=('dc:Publisher','dc:publisher')]/@content
		       [contains(.,concat($break-publisher-after,' '))]">
    <xsl:attribute name="content"
		   select="replace(., concat('(',$break-publisher-after,') '), '$1&#10;')"/>
  </xsl:template>

  <xsl:template match="node()|@*">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>

</xsl:stylesheet>
