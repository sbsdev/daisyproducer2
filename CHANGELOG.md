# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- REST endpoint `GET /api/statistics` to read the statistics about
  unknown words, as JSON or with `format=csv` as a file for Excel (#94)

## [0.9.40] - 2026-10-19

### Added
- Record the number of words and of unknown words for both grades when
  the first version of a document is uploaded, to see how well the
  dictionaries cover new books over the years (#94)

### Changed
- Keep only the first statistics entry per document and grade of the
  old system, which recorded one every time words were confirmed.
  Back up `statistics_documentstatistic` before deploying, the deleted
  entries cannot be restored

## [0.9.39] - 2026-10-16

### Added
- Documentation of the database schema, generated from the live database

### Changed
- Store all document metadata, products, states and versions as utf8, so
  that characters outside latin1 (dashes, typographic quotes, …) are no
  longer replaced with `?`. Requires running the migrations (`migrate`)
- Stricter ISBN check in the ABACUS import and the REST API: only an
  ISBN-10 may end with the check digit `X`, and only in upper case. The
  migration upper cases the existing ISBNs

### Fixed
- Image names that differ only in umlauts or case (`kröte.jpeg` and
  `krote.jpeg`) no longer overwrite each other in the database
- Documents with statistics from the old system can be deleted again. The
  statistics are kept, without the document

## [0.9.38] - 2026-09-08

### Added
- Pipeline 2 script to convert a DTBook to eBraille

### Changed
- Pass daisyproducer2 version to dtbook2sbsform

### Fixed
- Pass the end notes setting to the volume splitter, so that volumes are split
  correctly for large print with end notes at the end of the document

## [0.9.37] - 2026-06-16

### Added
- Babashka script to compare braille output between servers, with `:document-ids` filtering option

### Fixed
- Missing whitelist export after local word deletion

## [0.9.36] - 2026-06-02

### Added
- REST endpoint to add a document

## [0.9.35] - 2025-12-24

### Fixed
- Prevent updates to `production-series` and `production-series-number` if the ABACUS import is not for a PS product

[Unreleased]: https://github.com/sbsdev/daisyproducer2/compare/0.9.40...HEAD
[0.9.40]: https://github.com/sbsdev/daisyproducer2/compare/0.9.39...0.9.40
[0.9.39]: https://github.com/sbsdev/daisyproducer2/compare/0.9.38...0.9.39
[0.9.38]: https://github.com/sbsdev/daisyproducer2/compare/0.9.37...0.9.38
[0.9.37]: https://github.com/sbsdev/daisyproducer2/compare/0.9.36...0.9.37
[0.9.36]: https://github.com/sbsdev/daisyproducer2/compare/0.9.35...0.9.36
[0.9.35]: https://github.com/sbsdev/daisyproducer2/compare/0.9.34...0.9.35
