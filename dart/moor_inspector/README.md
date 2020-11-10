Moor inspector dart library

Helper library for moor databases, enables the moor inspector intellij plugin to inspect and edit your moor databases during development

Requires the moor inspector plugin from the intellij marketplace to work.

Download plugin [here](https://plugins.jetbrains.com/plugin/15364-moor-inspector)
 
## Usage

A simple usage example:

```dart
import 'package:moor_inspector/moor_inspector.dart';
import 'moor.dart';

main() async {
  final database = Database(...); //Generated moor database
  
  final moorInspectorBuilder = MoorInspectorBuilder()
      ..bundleId = 'com.example.text'
      ..icon = 'flutter'
      ..addDatabase('example', database);
  final inspector = moorInspectorBuilder.build();
  
  //Start server and announcement server
  await inspector.start();

  //Application code here

  //Optionally shut down server, will stop at end of program anyway
  await inspector.stop();
}
```

## Features and bugs

Please file feature requests and bugs at the [issue tracker][tracker].

[tracker]: https://github.com/Chimerapps/moor_inspector
