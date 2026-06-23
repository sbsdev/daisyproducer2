# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed
- Pass daisyproducer2 version to dtbook2sbsform

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

[Unreleased]: https://github.com/sbsdev/daisyproducer2/compare/0.9.37...HEAD
[0.9.37]: https://github.com/sbsdev/daisyproducer2/compare/0.9.36...0.9.37
[0.9.36]: https://github.com/sbsdev/daisyproducer2/compare/0.9.35...0.9.36
[0.9.35]: https://github.com/sbsdev/daisyproducer2/compare/0.9.34...0.9.35
