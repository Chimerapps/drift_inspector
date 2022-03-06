# DEPRECATION NOTICE

This package is being replaced with a more powerful inspector system. Please migrate to: https://pub.dev/packages/drift_local_storage_inspector
Follow the migration instructions outlined in: https://github.com/Chimerapps/drift_inspector/wiki/Deprecation

## Old instructions

Drift inspector dart library

Helper library for drift databases, enables the drift inspector intellij plugin to inspect and edit your drift databases during development

Requires the database inspector plugin from the intellij marketplace to work.

Download plugin [here](https://plugins.jetbrains.com/plugin/15364-database-inspector/)
 
## Usage

A simple usage example:

```dart
import 'package:drift_inspector/drift_inspector.dart';
import 'drift.dart';

main() async {
  final database = Database(...); //Generated drift database
  
  final driftInspectorBuilder = DriftInspectorBuilder()
      ..bundleId = 'com.example.text'
      ..icon = 'flutter'
      ..addDatabase('example', database);
  final inspector = driftInspectorBuilder.build();
  
  //Start server and announcement server
  await inspector.start();

  //Application code here

  //Optionally shut down server, will stop at end of program anyway
  await inspector.stop();
}
```

## Web support
Web is only supported as a target to ensure you can build your code.
Due to some architectural differences (most notably lack of dart:io support), discovering and communication with a web app is currently not possible.

## Features and bugs

Please file feature requests and bugs at the [issue tracker][tracker].

[tracker]: https://github.com/Chimerapps/drift_inspector
