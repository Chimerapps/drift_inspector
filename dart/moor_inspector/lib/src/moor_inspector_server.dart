import 'dart:convert';
import 'dart:io';

import 'package:synchronized/synchronized.dart';

import 'moor_inspector_server_base.dart';

MoorInspectorServer createServer(int port) => _MooreInspectorServerImpl(port);

class _MooreInspectorServerImpl extends MoorInspectorServer {
  HttpServer _server;
  final int _port;
  final _connections = List<MooreInspectorConnection>();
  final _lock = Lock();

  @override
  int get port => _server.port;

  _MooreInspectorServerImpl(this._port);

  /// Starts the server
  @override
  Future<void> start() async {
    _server = await HttpServer.bind(InternetAddress.loopbackIPv4, _port)
      ..transform(WebSocketTransformer()).listen(_onNewConnection);
    print('Server running on ${_server.port}');
  }

  /// Stops the server
  @override
  Future<void> stop() async {
    await _server.close(force: true);
    _server = null;
    await _lock.synchronized(() async {
      _connections.forEach((socket) => socket.close());
    });
  }

  void _onNewConnection(WebSocket socket) {
    final connection = _MooreInspectorConnectionImpl(socket, this, connectionListener);
    _lock.synchronized(() async {
      _connections.add(connection);
      socket.listen(
        (data) => connection.onMessage(data as String),
        onDone: () => _onSocketClosed(connection),
        onError: (_) => _onSocketClosed(connection),
        cancelOnError: true,
      );
    });

    connection.onConnectionReady();
  }

  void _onSocketClosed(MooreInspectorConnection socket) {
    _lock.synchronized(() async {
      _connections.remove(socket);
    });
  }
}

class _MooreInspectorConnectionImpl extends MooreInspectorConnection {
  static const _MESSAGE_FILTER = 'filter';
  static const _MESSAGE_UPDATE = 'update';

  final WebSocket _socket;
  final MoorInspectorServer _server;
  final ConnectionListener _listener;

  _MooreInspectorConnectionImpl(this._socket, this._server, this._listener);

  void onConnectionReady() {
    _listener.onNewConnection(this);
  }

  @override
  void sendMessageUTF8(List<int> data) {
    _socket.addUtf8Text(data);
  }

  void onMessage(String data) {
    final parsedJson = jsonDecode(data);
    final type = parsedJson['type'] as String;
    switch (type) {
      case _MESSAGE_FILTER:
        _handleFilterRequest(parsedJson['body'] as Map<String, dynamic>);
        break;
      case _MESSAGE_UPDATE:
        _handleUpdateRequest(parsedJson['body'] as Map<String, dynamic>);
        break;
    }
  }

  @override
  void close() {
    _socket.close();
  }

  Future<void> _handleFilterRequest(Map<String, dynamic> parsedJson) async {
    final id = parsedJson['databaseId'] as String;
    final requestId = parsedJson['requestId'] as String;
    final query = parsedJson['query'] as String;

    sendMessageUTF8(await _listener.filterTable(id, requestId, query));
  }

  Future<void> _handleUpdateRequest(Map<String, dynamic> parsedJson) async {
    final id = parsedJson['databaseId'] as String;
    final query = parsedJson['query'] as String;
    final requestId = parsedJson['requestId'] as String;
    final affectedTables = (parsedJson['affectedTables'] as List<dynamic>).map((it) => it.toString()).toList();

    print('Executing: $query which affects $affectedTables on $id');
    sendMessageUTF8(await _listener.update(id, requestId, query, affectedTables));
  }
}
