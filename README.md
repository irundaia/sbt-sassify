# sbt-sassify: Sass for SBT
[ ![Download](https://api.bintray.com/packages/irundaia/sbt-plugins/sbt-sassify/images/download.svg) ](https://bintray.com/irundaia/sbt-plugins/sbt-sassify/_latestVersion)[![Build Status](https://travis-ci.org/irundaia/sbt-sassify.svg?branch=master)](https://travis-ci.org/irundaia/sbt-sassify)

An sbt plugin that enables you to use [Sass](http://sass-lang.com/) in your [sbt-web](https://github.com/sbt/sbt-web) project.

This plugin is a reimplementation of [sbt-sass](https://github.com/ShaggyYeti/sbt-sass). Since I wasn't allowed to install the sass command line compiler on my company's' webserver (damn you corporate IT), I decided to rewrite the plugin to use [libsass](https://github.com/sass/libsass) instead. Due to these changes, the plugin no longer resembled the old plugin, which is why I decided to host it myself.

## Sass language version
This plugin is based on [libsass](https://github.com/sass/libsass) version 3.5.5, that implements the Sass 3.4 specification.

## Compatibility
The sbt-sassify plugin supports the following operating systems:
- OS X 10.8+
- Windows (32/64 bit)
- Linux (32/64 bit)

This plugin has been tested against sbt-web and the Play framework versions 1.4.1 and 2.4.3+ respectively. Additionally, it requires Java 7.

## Usage

To use the `sbt-sassify` plugin you can include the plugin in `project/plugins.sbt` or `project/sbt-sassify.sbt` like this:

```scala
addSbtPlugin("org.irundaia.sbt" % "sbt-sassify" % "1.4.13")
```

### Directory structure

This plugin uses the same conventions as sbt-web. As such all `*.sass` and `*.scss` files in the `<source dir>/assets`directory will be compiled. Depending on the extension of the file, the plugin will decide which syntax should be used to compile the source file. `.sass` for the indented syntax and `.scss` for the css-like syntax. (Note that the input style can be forced. See the `syntaxDetection` option.)

For example, given a file structure as:

```
app
└ assets
  └ stylesheets
    └ main.scss
    └ utils
      └ _reset.scss
      └ _layout.scss
```

With the following `main.scss` source:

```scss
@import "utils/reset";
@import "utils/layout";

h1 {
  color: red;
}
```

The Sass file outlined above, will be compiled into `public/stylesheets/main.css`, and it will include all the content of the `reset` and `layout` partials.

## Mixing Sass and web-jars

[WebJars](http://www.webjars.org) enable us to depend on client side libraries without pulling all dependencies into our own code base manually.

Compass is a library containing all sorts of reusable Sass functions and mixins. Unfortunately, it is targeted towards the Ruby implementation of Sass. Luckily, there is a number of useful mixins that can be extracted from it. These mixins are wrapped in a WebJar.

Including the compass mixins in your project is as easy as including the WebJar dependency in your library dependencies. For example, within a `build.sbt` file add:

```scala
libraryDependencies += "org.webjars.bower" % "compass-mixins" % "0.12.7"
```

sbt-web will automatically extract WebJars into a `lib`` folder relative to your asset's target folder. Therefore, to use the Compass mixins you can import them by:

```scss
@import "lib/compass-mixins/lib/compass";

table.ellipsed-table {
  tr td {
    max-width: 100px;
    @include ellipsis();
  }
}
```

The same idea can be used to include other Sass libraries, for instance the [official Sass port of bootstrap](https://github.com/twbs/bootstrap-sass). To include the WebJar use:

```scala
libraryDependencies += "org.webjars.bower" % "bootstrap-sass" % "3.3.6"
```

Then to use it in your project, you can use:
```scss
@import "lib/bootstrap-sass/assets/stylesheets/bootstrap";
```

### Options

Some options can be passed to the Sass compiler. For an overview, see below:

| Setting            | Description                                                          | Supported values               | Default value |
|--------------------|----------------------------------------------------------------------|--------------------------------|---------------|
| cssStyle           | The style of the output CSS file.                                    | `Minified`/`Maxified`/`Sassy`  | `Minified`    |
| generateSourceMaps | Whether or not source files are generated.                           | `true`/`false`                 | `true`        |
| embedSources       | Whether or not the sources should be embedded in the source map file.| `true`/`false`                 | `true`        |
| syntaxDetection    | How to determine whether the sass/scss syntax is used.               | `Auto`/`ForceScss`/`ForceSass` | `Auto`        |
| assetRootURL      | The base URL used to locate the assets.                              | Any `String`                   | `/assets`     |

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

1. Issues have been known to occur when a different version of libsass has been installed on your system. This is caused by the c-API changing in libsass version 3.4.5. If your system has a different version installed, this will cause linking errors. Currently, a workaround would be to make sure that the same version of libsass is installed.

2. Only one Sass syntax style can be used at the same time. So when compiling a .scss file, one cannot include a .sass file. (Well, you can, but it won't compile.)

3. Due to a lack of testing, this plugin might not work on all 32-bit linux distributions.
