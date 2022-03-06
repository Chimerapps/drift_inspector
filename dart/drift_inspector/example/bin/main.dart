import 'package:drift/native.dart';
import 'package:drift_inspector/drift_inspector.dart';

import 'drift.dart';

Future<void> main(List<String> arguments) async {
  final database = MyDatabase(NativeDatabase.memory());

  //ignore: deprecated_member_use
  final driftInspectorBuilder = DriftInspectorBuilder()
    ..bundleId = 'com.example.text'
    ..icon = 'flutter'
    ..addDatabase('example', database);

  await _populateDatabase(database);

  final inspector = driftInspectorBuilder.build();

  print('Starting drift inspector');
  await inspector.start();

  const waitDuration = Duration(seconds: 1000000);

  print('Asking drift inspector to wait for $waitDuration');
  await Future.delayed(waitDuration);

  print('Stopping inspector');
  await inspector.stop();

  print('Inspector stopped');
}

Future<void> _populateDatabase(MyDatabase database) async {
  await database.exampleDao.replaceRecipes([
    Recipe(id: 1, title: 'Recipe 1', instructions: 'Instructions 1'),
    Recipe(id: 2, title: 'Recipe 2', instructions: 'Instructions 2'),
    Recipe(id: 3, title: 'Recipe 3', instructions: 'Instructions 3'),
    Recipe(id: 4, title: 'Recipe 4', instructions: 'Instructions 4'),
  ]);

  await database.exampleDao.replaceCategories([
    CategoriesCompanion.insert(exampleBool: true),
    CategoriesCompanion.insert(exampleBool: false),
  ]);
}
