# sbt-sassify releases

## 0.1.0
- Initial version

## 0.1.1
- Fix compilation on windows (and other OS's using a ` \ ` as file separator).

## 1.0.0
- Rewrite plugin settings key to have the need to prefix it with `SassKeys` (for clarity);
- Refactor the `CssStyle` from an `Enumeration` to a `sealed trait` to reduce verbosity.

## 1.1.0
- When encountering an error in a Sass file, continue compilation of other files.

## 1.1.1
- Fixes incremental compilation when included files have been modified.

## 1.2.0
- Only output source maps when it has been requested;
- Add option to embed source file contents to the source map;
- Add option to override the input style detection. Now it is possible to for an (non-)indented syntax regardless of the
 extension;
- Restructure tests;
- Package cleanup.