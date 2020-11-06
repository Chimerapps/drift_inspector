import 'package:moor/moor.dart';
import 'package:moor_inspector/src/moor_insepctor_driver.dart';
import 'package:tuple/tuple.dart';

class MoorInspectorBuilder {
  final _databases = List<Tuple2<String, GeneratedDatabase>>();
  String icon;
  String bundleId;
  int port = 0;

  void addDatabase(String name, GeneratedDatabase database) {
    _databases.add(Tuple2(name, database));
  }

  MoorInspector build() {
    return MoorInspector(port, bundleId, icon, _databases);
  }
}

class MoorInspector {
  final MooreInspectorDriver _driver;

  MoorInspector(
    int port,
    String bundleId,
    String icon,
    List<Tuple2<String, GeneratedDatabase>> databases,
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
