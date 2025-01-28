# TON Plugin for the IntelliJ IDEs Changelog

## [2.5.1]

### Added
- Tolk 0.8 support

## [2.5.0]

### Added
- Tolk 0.7 support
- Tolk type inference & completion
- Tolk type hints for variables & functions
- TL-B schema inspections
- TL-B constructor tag generator

### Fixed
- A lot of minor bugs

## [2.4.0]

### Added
- Tolk support
- TVM Assembly support
### Fixed
- A lot of minor bugs

## [2.3.0]

### Added

- Support for the latest Tact
  1.4.0 ([#177](https://github.com/ton-blockchain/intellij-ton/issues/177), [#180](https://github.com/ton-blockchain/intellij-ton/issues/180))
- Inspections for out of range integer values in FunC and Tact
- Inspections for integer division by zero in FunC and Tact
- Inspections for integer overflow in FunC
- Constant expression evaluation in FunC and Tact (with inline
  hints!) ([#22](https://github.com/ton-blockchain/intellij-ton/issues/22))

### Fixed

- `message` and `bounced` highlight not working in Tact ([#174](https://github.com/ton-blockchain/intellij-ton/issues/174))
- `com.intellij.diagnostic.PluginException: Template not found: Fift File` ([#182](https://github.com/ton-blockchain/intellij-ton/issues/182))
- `Storage for FuncNamedElementIndex.storage is already registered` ([#181](https://github.com/ton-blockchain/intellij-ton/issues/181))

## [2.2.0]

### Added

- Full support latest Tact 1.2.0 version ([#166](https://github.com/ton-blockchain/intellij-ton/issues/166))

### Fixed

- A bug with comment/uncomment already commented
  lines ([#169](https://github.com/ton-blockchain/intellij-ton/issues/169))
- Function parameter name hints for var arguments of same
  name ([#167](https://github.com/ton-blockchain/intellij-ton/issues/167))

## [2.1.0]

### Added

- Language selection in project template ([#148](https://github.com/ton-blockchain/intellij-ton/issues/148))
- Empty and example project templates ([#147](https://github.com/ton-blockchain/intellij-ton/issues/147))
- `#include` assist ([#108](https://github.com/ton-blockchain/intellij-ton/issues/108))

### Fixed

- Negative method IDs are considered a syntax
  error ([#157](https://github.com/ton-blockchain/intellij-ton/issues/157))
- Non-ASCII characters in FunC identifiers ([#156](https://github.com/ton-blockchain/intellij-ton/issues/156))
- Invalid trailing comma in return tuple type ([#155](https://github.com/ton-blockchain/intellij-ton/issues/155))
- Unresolved reference to uninitialized
  variable ([#151](https://github.com/ton-blockchain/intellij-ton/issues/151))
- Invalid indent for multiline tuples in function signature return
  type ([#150](https://github.com/ton-blockchain/intellij-ton/issues/150))
- Auto-complete not work on `slice~` ([#149](https://github.com/ton-blockchain/intellij-ton/issues/149))
- `method_id` completion ([#126](https://github.com/ton-blockchain/intellij-ton/issues/126))
- Reference resolving with identifiers containing non-letter
  characters ([#107](https://github.com/ton-blockchain/intellij-ton/issues/107))
- Uppercase HEX in TL-B ([#100](https://github.com/ton-blockchain/intellij-ton/issues/100))
- Indent in chain calls ([#38](https://github.com/ton-blockchain/intellij-ton/issues/38))

## [2.0.5]

- fixed builtin functions resolving for `load_int`, `load_uint`

## [2.0.0]

### Complete plugin rework

- Documentation rendering
- Tact support
- Blueprint project support
- A lot of bug-fixes and small futures

## [1.0.3]

### Added

- Support try-catch statements

## [1.0.2]

### Added

- Support [FunC Update 2022.05](https://telegra.ph/FunC-Update-202205-05-17)
- Auto-formatting in FunC
- Support UTF-8 names in FunC functions
- Auto-completion for inbuilt FunC statements
- Auto-completion for all FunC functions in project scope
- Documentation in FunC stdlib.fc
- Language injection support (FunC, Fift, TL-B) for string literals in regular programming languages
- New template for FunC files creation

## [0.8.0]

### Added

- Impure functions smart-inspections ([#17](https://github.com/andreypfau/intellij-ton/issues/17))
- TON-specific words in spellchecker dictionary ([#16](https://github.com/andreypfau/intellij-ton/issues/16))

### Fixed

- Ternary expression statements ([#15](https://github.com/andreypfau/intellij-ton/issues/15))
- Code suggestions don't show after `.` (dot) ([#13](https://github.com/andreypfau/intellij-ton/issues/13))

## [0.7.0]

### Added

- Variable references
- Type checking for method calls

### Fixed

- Fixed method duplication on neighbour files
- Fixed IndexOutOfBoundsException crash
- A lot of bug fixes

## [0.6.0]

### Added

- String literals & Constants support ([https://github.com/newton-blockchain/ton/pull/75]())
- Block folding
- Parameter hints in function calls
- References in method calls (`foo.bar()`)
- Resolving references by neighbour files in directory
- New icon for TL-B Schema files

### Fixes

- Function names with `:`
- A lot of small bugs and issues

## [0.5.0]

### Added

- TL-B Schemas support
- Using new type extension `.func` for FunC files

## [0.4.0]

### Added

- New grammar model for FunC
- New syntax highlighting for FunC
- New syntax highlighting for Fift
- Auto-completions for Fift
- References in Fift

### Fixed

- A lot of small bugs and issues

## [0.3.1]

### Fixed

- [#4](https://github.com/andreypfau/intellij-ton/issues/4) `elseif` & `elseifnot` statements
- [#5](https://github.com/andreypfau/intellij-ton/issues/5) Complete revision of the Fift gramamr

## [0.3.0]

### Added

- New function highlighting in FunC
- Function name completion suggestions in FunC
- Function references by `Ctrl+Left Click` or `Ctrl+B`
- Fixed Fift path declaration (`#!/usr/bin/fift`)
- Fixed `include` declaration in Fift
- Fixed `asm` declaration in Fift
