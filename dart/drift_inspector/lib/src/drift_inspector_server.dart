import 'dart:convert';
import 'dart:io';

import 'package:synchronized/synchronized.dart';

import 'drift_inspector_server_base.dart';

DriftInspectorServer createServer(int port) => _DrifteInspectorServerImpl(port);

class _DrifteInspectorServerImpl extends DriftInspectorServer {
  HttpServer? _server;
  final int _port;
  final _connections = <DrifteInspectorConnection>[];
  final _lock = Lock();

  @override
  int get port => _server?.port ?? -1;

  _DrifteInspectorServerImpl(this._port);

  /// Starts the server
  @override
  Future<void> start() async {
    _server = await HttpServer.bind(InternetAddress.loopbackIPv4, _port)
      ..transform(WebSocketTransformer()).listen(_onNewConnection);
    print('Server running on ${_server!.port}');
  }

  /// Stops the server
  @override
  Future<void> stop() async {
    await _server?.close(force: true);
    _server = null;
    await _lock.synchronized(() async {
      for (final socket in _connections) {
        try {
          socket.close();
        } catch (_) {}
      }
    });
  }

  void _onNewConnection(WebSocket socket) {
    final connection =
        _DrifteInspectorConnectionImpl(socket, connectionListener);
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

  void _onSocketClosed(DrifteInspectorConnection socket) {
    _lock.synchronized(() async {
      _connections.remove(socket);
    });
  }
}

class _DrifteInspectorConnectionImpl extends DrifteInspectorConnection {
  static const _messageFilter = 'filter';
  static const _messageUpdate = 'update';
  static const _messageBatch = 'batch';
  static const _messageExport = 'export';

  final WebSocket _socket;
  final ConnectionListener _listener;

  _DrifteInspectorConnectionImpl(this._socket, this._listener);

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
    final body = parsedJson['body'] as Map<String, dynamic>;
    _handleType(type, body, sendResponse: true);
  }

  @override
  void close() {
    _socket.close();
  }

  Future<void> _handleType(String type, Map<String, dynamic> body,
      {required bool sendResponse}) {
    switch (type) {
      case _messageFilter:
        return _handleFilterRequest(body, sendResponse);
      case _messageUpdate:
        return _handleUpdateRequest(body, sendResponse);
      case _messageBatch:
        return _handleBulkRequest(body, sendResponse);
      case _messageExport:
        return _handleExport(body);
    }
    return Future.value();
  }

  Future<void> _handleFilterRequest(
      Map<String, dynamic> parsedJson, bool sendResponse) async {
    final id = parsedJson['databaseId'] as String;
    final requestId = parsedJson['requestId'] as String;
    final query = parsedJson['query'] as String;
    final variables = parsedJson.containsKey('variables')
        ? (parsedJson['variables'] as List).map(_mapVariable).toList()
        : List<InspectorVariable>.empty();

    if (sendResponse) {
      await _sendOrError(
          requestId,
          _listener.filterTable(id, requestId, query, variables,
              sendResponse: sendResponse));
    } else {
      await _listener.filterTable(id, requestId, query, variables,
          sendResponse: sendResponse);
    }
  }

  Future<void> _handleUpdateRequest(
      Map<String, dynamic> parsedJson, bool sendResponse) async {
    final id = parsedJson['databaseId'] as String;
    final query = parsedJson['query'] as String;
    final requestId = parsedJson['requestId'] as String;
    final affectedTables = (parsedJson['affectedTables'] as List<dynamic>)
        .map((it) => it.toString())
        .toList();
    final variables = parsedJson.containsKey('variables')
        ? (parsedJson['variables'] as List).map(_mapVariable).toList()
        : List<InspectorVariable>.empty();

    if (sendResponse) {
      await _sendOrError(
          requestId,
          _listener.update(id, requestId, query, affectedTables, variables,
              sendResponse: sendResponse));
    } else {
      await _listener.update(id, requestId, query, affectedTables, variables,
          sendResponse: sendResponse);
    }
  }

  Future<void> _handleBulkRequest(
      Map<String, dynamic> parsedJson, bool sendResponse) async {
    final actions = parsedJson['actions'] as List;
    final requestId = parsedJson['requestId'] as String;

    Exception? lastException;
    for (final action in actions) {
      final actionInternal = action as Map<String, dynamic>;
      final type = actionInternal['type'] as String;
      final body = actionInternal['body'] as Map<String, dynamic>;
      body['requestId'] = requestId;
      try {
        await _handleType(type, body, sendResponse: false);
      } on Exception catch (e) {
        lastException = e;
      }
    }
    if (sendResponse) {
      if (lastException != null) {
        final wrapper = {
          'type': 'error',
          'body': {
            'requestId': requestId,
            'message': lastException.toString(),
          }
        };
        sendMessageUTF8(utf8.encode(json.encode(wrapper)));
      } else {
        final wrapper = {
          'type': 'bulkResponse',
          'body': {
            'requestId': requestId,
          }
        };
        sendMessageUTF8(utf8.encode(json.encode(wrapper)));
      }
    }
  }

  Future<void> _handleExport(Map<String, dynamic> parsedJson) async {
    final id = parsedJson['databaseId'] as String;
    final tables = parsedJson.containsKey('tables')
        ? (parsedJson['tables'] as List<dynamic>)
            .map((it) => it.toString())
            .toList()
        : null;
    final requestId = parsedJson['requestId'] as String;

    await _sendOrError(
      requestId,
      _listener.export(id, requestId, tables),
    );
  }

  Future<void> _sendOrError(
      String requestId, Future<List<int>> dataProvider) async {
    try {
      final data = await dataProvider;
      sendMessageUTF8(data);
    } on Exception catch (e) {
      final wrapper = {
        'type': 'error',
        'body': {
          'requestId': requestId,
          'message': e.toString(),
        }
      };
      sendMessageUTF8(utf8.encode(json.encode(wrapper)));
    }
  }

  InspectorVariable _mapVariable(data) {
    final json = data as Map<String, dynamic>;

    return InspectorVariable(json['type'] as String, json['data']);
  }
}
