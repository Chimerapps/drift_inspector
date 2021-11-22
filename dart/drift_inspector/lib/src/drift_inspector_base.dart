import 'package:drift/drift.dart';
import 'package:drift_inspector/src/drift_inspector_driver.dart';
import 'package:uuid/uuid.dart';

///Builder for the drift inspector
class DriftInspectorBuilder {
  final _databases = <DatabaseHolder>[];

  /// The icon to use for this server, rendered in the plugin. Eg: flutter for the built-in flutter icon
  String? icon;

  /// The application id of the application, used to identify process in the plugin
  String? bundleId;

  /// The port the inspector must run on, use 0 (default) to let the server choose an open port. The server announcement manager will expose the system port
  int port = 0;

  /// Adds the drift [database] to the inspector. The [name] parameter is used in the plugin to display the [database]
  void addDatabase(String name, GeneratedDatabase database) {
    _databases.add(DatabaseHolder(name, const Uuid().v4(), database));
  }

  /// Builds the drift inspector
  DriftInspector build() {
    if (bundleId == null) {
      throw ArgumentError('bundleId must not be null!');
    }
    return DriftInspector._(port, bundleId!, icon, _databases);
  }
}

/// Drift inspector base. Use the [start] and [stop] methods to start and stop the internal server
class DriftInspector {
  final DrifteInspectorDriver _driver;

  DriftInspector._(
    int port,
    String bundleId,
    String? icon,
    List<DatabaseHolder> databases,
  ) : _driver = DrifteInspectorDriver(
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
