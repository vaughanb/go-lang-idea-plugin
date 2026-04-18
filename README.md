# Go plugin for IntelliJ

Community-maintained Go language support plugin for JetBrains IDEs.

## Status

This plugin has been modernized from its original 2016.3-era codebase to work with **IntelliJ IDEA 2026.1+** (build 261+). It requires **Java 21** or later.

### What's supported

- Go syntax highlighting, parsing, and code completion
- Go modules (`go.mod`, `go.sum`) file type recognition
- Go generics (type parameters, constraints, `~` operator) - parser support
- Modern numeric literals (binary `0b`, explicit octal `0o`, digit separators `_`)
- Type aliases
- Run configurations for Go applications and tests
- Test framework support (including fuzz tests)
- Delve debugger integration
- Code inspections and quick-fixes
- Code formatting with `gofmt`/`goimports`
- Structure view, folding, navigation
- Coverage support (optional, requires Coverage plugin)

### Building

```bash
./gradlew buildPlugin
```

The plugin ZIP will be in `build/distributions/`.

### Requirements

- Gradle 9.0+ (included via wrapper)
- Java 21+
- IntelliJ IDEA 2026.1+ or compatible JetBrains IDE

### Configuration

Edit `gradle.properties` to customize:
- `platformVersion` - target IntelliJ Platform version (default: `2026.1`)
- `localIdePath` - path to a local IDE installation (for offline development)

## FAQ

See the [FAQ](https://github.com/go-lang-plugin-org/go-lang-idea-plugin/wiki/FAQ).

## Bugs

If you've found a bug, [report it](http://github.com/go-lang-plugin-org/go-lang-idea-plugin/issues).

When reporting, please include:
- IDE and plugin version
- OS and JDK version
- Steps to reproduce with sample code

## Contributing

See the [contribution guide](CONTRIBUTING.md).

## License

The Gopher icons are based on the Go mascot designed by [Renee French](http://reneefrench.blogspot.com/) and copyrighted under the [Creative Commons Attribution 3.0 license](http://creativecommons.org/licenses/by/3.0/us/).

The plugin is distributed under Apache License, version 2.0. See [LICENCE](https://github.com/go-lang-plugin-org/go-lang-idea-plugin/blob/master/LICENCE).
