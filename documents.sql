create table documents_document (
  id    	    int(11)	PRIMARY KEY AUTO_INCREMENT,
  title 	    varchar(255) 	NOT NULL,
  author 	    varchar(255),
  subject 	    varchar(255),
  description       longtext,
  publisher         varchar(255)	NOT NULL  DEFAULT "SBS Schweizerische Bibliothek für Blinde, Seh- und Lesebehinderte",
  date 	     	    date 	      	NOT NULL,
  identifier        varchar(255)	NOT NULL  UNIQUE,
  source 	    varchar(20)  	NOT NULL,
  language          enum('de', 'de-1901', 'en', 'es', 'fr', 'it', 'rm-sursilv')  	NOT NULL  DEFAULT "de",
  rights 	    varchar(255),
  source_date       date,
  source_edition    varchar(255),
  source_publisher  varchar(255),
  source_rights     varchar(255),
  state      	    enum('open', 'closed')	NOT NULL  DEFAULT 'open',
--  state_id        int(11)	NOT NULL  DEFAULT 1,
  created_at        timestamp 	NOT NULL  DEFAULT CURRENT_TIMESTAMP,
  modified_at       datetime 	NOT NULL  DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  production_series enum('PPP', 'SJW'),
  production_series_number varchar(25),
  production_source enum('electronicData'),
);

create table documents_document (
  id    	    int(11)	PRIMARY KEY AUTO_INCREMENT,
  title 	    varchar(255) 	NOT NULL,
  author 	    varchar(255),
  subject 	    varchar(255),
  description       longtext,
  publisher         varchar(255)	NOT NULL  DEFAULT "SBS Schweizerische Bibliothek für Blinde, Seh- und Lesebehinderte",
  date 	     	    date 	      	NOT NULL,
  identifier        varchar(255)	NOT NULL  UNIQUE,
  source 	    varchar(20)  	NOT NULL,
  language          char(10)  		NOT NULL  DEFAULT "de",
  rights 	    varchar(255),
  source_date       date,
  source_edition    varchar(255),
  source_publisher  varchar(255),
  source_rights     varchar(255),
  state      	    char(8)	NOT NULL  DEFAULT 'open',
  created_at        timestamp 	NOT NULL  DEFAULT CURRENT_TIMESTAMP,
  modified_at       datetime 	NOT NULL  DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  production_series char(3),
  production_series_number varchar(25),
  production_source char(14),
  
  FOREIGN KEY (language) REFERENCES documents_document_language(name)
  FOREIGN KEY (state) REFERENCES documents_document_state(name)
  FOREIGN KEY (production_series) REFERENCES documents_document_production_series(name)
  FOREIGN KEY (production_source) REFERENCES documents_document_production_source(name)
);

create table documents_document_language (
  name char(10) PRIMARY KEY CHARACTER SET ascii
)
create table documents_document_state (
  name char(8) PRIMARY KEY CHARACTER SET ascii
)
create table documents_document_production_series (
  name char(3) PRIMARY KEY CHARACTER SET ascii
)
create table documents_document_production_source (
  name char(14) PRIMARY KEY CHARACTER SET ascii
)
