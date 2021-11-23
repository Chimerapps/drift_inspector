import 'drift_inspector_server_base.dart';

DriftInspectorServer createServer(int port) => _DummyDriftInspectorServer();

class _DummyDriftInspectorServer extends DriftInspectorServer {
  @override
  int get port => -1;

  @override
  Future<void> start() {
    return Future.value();
  }

  @override
  Future<void> stop() {
    return Future.value();
  }
}
