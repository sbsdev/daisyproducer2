CREATE TABLE documents_document2 (
    id INT PRIMARY KEY AUTO_INCREMENT,
    -- The title of the DTB, including any subtitles
    title VARCHAR(255) NOT NULL,
    -- Names of primary author or creator of the intellectual content of the publication
    author VARCHAR(255),
    -- The topic of the content of the publication
    subject VARCHAR(255),
    -- Plain text describing the publication's content
    description LONGTEXT,
    -- The agency responsible for making the DTB available
    publisher VARCHAR(255),
    -- Date of publication of the DTB
    date DATE NOT NULL,
--    date DATE NOT NULL DEFAULT (CURRENT_DATE), -- FIXME: this doesn't seem to work
    -- A string or number identifying the DTB
    identifier VARCHAR(40) NOT NULL UNIQUE,
    -- A reference to a resource (e.g., a print original, ebook, etc.) from which the DTB is derived. Best practice is to use the ISBN when available
    source VARCHAR(20) NOT NULL,
    -- Language of the content of the publication
    language VARCHAR(10) NOT NULL
      DEFAULT 'de'
      CHECK (state IN ('de', 'de-1901', 'en', 'es', 'it', 'rm-sursilv')),
    -- Information about rights held in and over the DTB
    rights VARCHAR(255),
    -- Date of publication of the resource (e.g., a print original, ebook, etc.) from which the DTB is derived
    source_date DATE,
    -- A string describing the edition of the resource (e.g., a print original, ebook, etc.) from which the DTB is derived
    source_edition VARCHAR(255),
    -- The agency responsible for making available the resource (e.g., a print original, ebook, etc.) from which the DTB is derived
    source_publisher VARCHAR(255),
    -- Information about rights held in and over the resource (e.g., a print original, ebook, etc.) from which the DTB is derived
    source_rights VARCHAR(255),
    state CHAR(6) NOT NULL
      DEFAULT 'open'
      CHECK (state IN ('open', 'closed')),
    created_at TIMESTAMP
      DEFAULT CURRENT_TIMESTAMP,
    modified_at TIMESTAMP
      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    production_series CHAR(3)
      CHECK (production_series IN ('PPP', 'SJW')),
    production_series_number INT,
    production_source VARCHAR(25)
      CHECK (production_source IN ('electronicData'))
);
