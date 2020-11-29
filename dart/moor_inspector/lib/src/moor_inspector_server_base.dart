abstract class MoorInspectorServer {
  ///The port the tooling server is running on
  int get port;

  ConnectionListener connectionListener;

  Future<void> start();

  Future<void> stop();
}

// ignore: one_member_abstracts
abstract class ConnectionListener {
  void onNewConnection(MooreInspectorConnection connection);

  Future<List<int>> filterTable(
    String databaseId,
    String requestId,
    String query,
    List<InspectorVariable> variables, {
    bool sendResponse = true,
  });

  Future<List<int>> update(
    String databaseId,
    String requestId,
    String query,
    List<String> affectedTables,
    List<InspectorVariable> variables, {
    bool sendResponse = true,
  });

  Future<List<int>> export(
    String databaseId,
    String requestId,
    List<String> tables,
  );
}

abstract class MooreInspectorConnection {
  void sendMessageUTF8(List<int> data);

  void close();
}

class InspectorVariable {
  final String type;
  final dynamic data;

  InspectorVariable(this.type, this.data);
}
