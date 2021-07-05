import 'package:moor/ffi.dart';
import 'package:moor_inspector/moor_inspector.dart';

import 'moor.dart';

Future<void> main(List<String> arguments) async {
  final database = Database(VmDatabase.memory());

  final moorInspectorBuilder = MoorInspectorBuilder()
    ..bundleId = 'com.example.text'
    ..icon = 'flutter'
    ..addDatabase('example', database);

  await _populateDatabase(database);

  final inspector = moorInspectorBuilder.build();

  print('Starting moor inspector');
  await inspector.start();

  const waitDuration = Duration(seconds: 1000000);

  print('Asking moor inspector to wait for $waitDuration');
  await Future.delayed(waitDuration);

  print('Stopping inspector');
  await inspector.stop();

  print('Inspector stopped');
}

Future<void> _populateDatabase(Database database) async {
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
