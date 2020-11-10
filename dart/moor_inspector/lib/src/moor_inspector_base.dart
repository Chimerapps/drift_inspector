import 'package:moor/moor.dart';
import 'package:moor_inspector/src/moor_inspector_driver.dart';
import 'package:uuid/uuid.dart';

///Builder for the moor inspector
class MoorInspectorBuilder {
  final _databases = List<DatabaseHolder>();

  /// The icon to use for this server, rendered in the plugin. Eg: flutter for the built-in flutteer icon
  String icon;

  /// The application id of the application, used to identify process in the plugin
  String bundleId;

  /// The port the inspector must run on, use 0 (default) to let the server choose an open port. The server announcement manager will expose the system port
  int port = 0;

  /// Adds the moor [database] to the inspector. The [name] parameter is used in the plugin to display the [database]
  void addDatabase(String name, GeneratedDatabase database) {
    _databases.add(DatabaseHolder(name, Uuid().v4().toString(), database));
  }

  /// Builds the moor inspector
  MoorInspector build() {
    return MoorInspector._(port, bundleId, icon, _databases);
  }
}

/// Moor inspector base. Use the [start] and [stop] methods to start and stop the internal server
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

  /// Starts the inspector server and starts announcing for the plugin
  Future<void> start() {
    return _driver.start();
  }

  /// Stops the inspector server
  Future<void> stop() {
    return _driver.stop();
  }
}
