import 'package:moor/moor.dart';
import 'package:moor_inspector/src/moor_insepctor_driver.dart';
import 'package:uuid/uuid.dart';

class MoorInspectorBuilder {
  final _databases = List<DatabaseHolder>();
  String icon;
  String bundleId;
  int port = 0;

  void addDatabase(String name, GeneratedDatabase database) {
    _databases.add(DatabaseHolder(name, Uuid().v4().toString(), database));
  }

  MoorInspector build() {
    return MoorInspector._(port, bundleId, icon, _databases);
  }
}

class MoorInspector {
  final MooreInspectorDriver _driver;

  MoorInspector._(
    int port,
    String bundleId,
    String icon,
    List<DatabaseHolder> databases,
  ) : _driver = MooreInspectorDriver(
          databases,
          bundleId,
          icon,
          port,
        );

  Future<void> start() {
    return _driver.start();
  }

  Future<void> stop() {
    return _driver.stop();
  }
}
