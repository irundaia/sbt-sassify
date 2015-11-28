# SASS plugin for sbt

An sbt plugin that enables you to use [SASS](http://sass-lang.com/) in your sbt-web
project.

This plugin is a reimplementation of [sbt-sass](https://github.com/ShaggyYeti/sbt-sass).
Since I wasn't allowed to install the sass command line compiler on my company's' webserver (damn you corporate IT),
I decided to rewrite the plugin to use [jsass](https://github.com/bit3/jsass) instead. Due to these changes, the plugin
no longer resembled the old plugin, which is why I decided to host it myself.

## SASS language version
This plugin is based on [libsass](https://github.com/sass/libsass) version 3.2, that implements the SASS 3.3 specification.

## sbt-web and Play framework support

This plugin has been tested against sbt-web and the Play framework versions 1.2.2 and 2.4.3 respectively.

## Usage

Add the irundaia sbt-plugins repository at bintray to your resolvers in `build.sbt`:

    resolvers += "irundaia sbt-plugins" at "https://dl.bintray.com/irundaia/sbt-plugins/"

Now you can include the plugin in `project/plugins.sbt` or `project/sbt-sassify.sbt` like this:

    addSbtPlugin("org.irundaia.sbt" % "sbt-sassify" % "1.2.2")

### Directory structure

This plugin uses the same conventions as sbt-web. As such all `*.sass` and `*.scss` files in the `<source dir>/assets`
directory will be compiled. Depending on the extension of the file, the plugin will decide which syntax should be used
to compile the source file. `.sass` for the indented syntax and `.scss` for the css-like syntax. (Note that the input
style can be forced. See the `syntaxDetection` option.)

### Options

Some options can be passed to the SASS compiler. For an overview, see below:

| Setting            | Description                                                          | Supported values               | Default value |
|--------------------|----------------------------------------------------------------------|--------------------------------|---------------|
| cssStyle           | The style of the output CSS file.                                    | `Minified`/`Maxified`/`Sassy`  | `Minified`    |
| generateSourceMaps | Whether or not source files are generated.                           | `true`/`false`                 | `true`        |
| embedSources       | Whether or not the sources should be embedded in the source map file | `true`/`false`                 | `true`        |
| syntaxDetection    | How to determine whether the sass/scss syntax is used                | `Auto`/`ForceScss`/`ForceSass` | `Auto`        |

Changing the settings can be done by including the following settings in your build.sbt file:

```scala
import org.irundaia.sbt.sass._

SassKeys.cssStyle := Maxified

SassKeys.generateSourceMaps := true

SassKeys.syntaxDetection := ForceScss
```

## Versioning
sbt-sassify uses [semantic versioning](http://semver.org). Given a version number `major.minor.patch`, an increment in

- `major` signifies a non-backwards-compatible API change;
- `minor` signifies a backwards-compatible functionality implementation;
- `patch` signifies a backwards-compatible bug fix/refactoring.

## Known limitations

1. Source maps can only reflect files in the assets folder.

2. Only one SASS syntax style can be used at the same time. So when compile a .scss file, one cannot include a .sass
  file. (Well, you can, but it won't compile.)