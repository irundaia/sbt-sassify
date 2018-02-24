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

## 1.2.1
- Make sure that there are no jsass related entries in the sourceContent field in the source map
- Remove unused baseDirectory parameter

## 1.2.2
- Make sure that sass files are recompiled if the settings are modified (no longer needs a clean)

## 1.3.0
- Upgrade to jsass v4.0.0 (libsass v3.3.2)
- Expand tests
- Additional refactoring

## 1.3.1
- Fix source map paths

## 1.3.2
- Allow including web-jarred Sass files

## 1.4.0
- Fix caret position on exceptions
- Add support for generic compilation issues (rather than line based ones)
- Reuse the same compiler instance within an assets run
- Use the libsass error code to determine whether the compilation succeeded or not
- Make sure that assets can be found (now that absolute URL's are used in the source map)
- Update the source mappings to exclude filtered files.

## 1.4.1
- Move from `jsass` to a custom `libsass` wrapper
- Rewrite `sbt-sassify` to use `Path`s rather than `File`s. (Why implement things yourself when it's already done in `Java`)
- Fix bug for projects with spaces in their paths (see #5)

## 1.4.2
- Update to `libsass` 3.3.3 (#10)
- Remove modifications to source maps (#7)
- Write SBT scripted tests for including files (#6)
- Log compile duration (#8)
- Update copyright headers

## 1.4.3
- Update to `libsass` 3.3.4 (#12)
- Update to `play-json` 2.4.6
- Update to `scalatest` 2.2.6

## 1.4.4
- Fix character encoding when outputting CSS on Windows. (#13)

## 1.4.5
- Small refactorings
- Upgrade to `libsass` 3.3.5 (#14)

## 1.4.6
- Revert to plain SBT project layout. The root directory was becoming too cluttered.
- Upgrade to `libsass` 3.3.6 (#15)
- Fix nested `@extend` (#11)

## 1.4.7
- Upgrade to `libsass` to 3.4.0
- Upgrade `scalatest` to 3.0.1
- Make sure errors are reported on the correct line/col combo
- Remove dependency on `play-json`

## 1.4.8
- Upgrade to `libsass` 3.4.2

## 1.4.9
- Upgrade to `libsass` 3.4.5
- Upgrade to `sbt` 0.13.15
- Upgrade to `sbt-web` to 1.4.1

## 1.4.10
- Cross build for scala 2.12
- Cross build for sbt 1.*

## 1.4.11
- Force JDK 8 support

## 1.4.12
- Upgrade to `libsass` 3.4.9
- Upgrade to `sbt` 1.1.1
- Fix string encoding issue with SBT 1.*