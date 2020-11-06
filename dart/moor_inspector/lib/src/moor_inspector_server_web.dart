import 'moor_inspector_server_base.dart';

MoorInspectorServer createServer(int port) => _DummyMoorInspectorServer();

class _DummyMoorInspectorServer extends MoorInspectorServer {
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
