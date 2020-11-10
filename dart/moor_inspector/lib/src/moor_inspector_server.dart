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
    final connection =
        _MooreInspectorConnectionImpl(socket, connectionListener);
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
  static const _MESSAGE_BATCH = 'batch';

  final WebSocket _socket;
  final ConnectionListener _listener;

  _MooreInspectorConnectionImpl(this._socket, this._listener);

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
      {bool sendResponse}) {
    switch (type) {
      case _MESSAGE_FILTER:
        return _handleFilterRequest(body, sendResponse);
      case _MESSAGE_UPDATE:
        return _handleUpdateRequest(body, sendResponse);
      case _MESSAGE_BATCH:
        return _handleBulkRequest(body, sendResponse);
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

    Exception lastException;
    actions.forEach((action) async {
      final actionInternal = action as Map<String, dynamic>;
      final type = actionInternal['type'] as String;
      final body = actionInternal['body'] as Map<String, dynamic>;
      body['requestId'] = requestId;
      try {
        await _handleType(type, body, sendResponse: false);
      } on Exception catch (e) {
        lastException = e;
      }
    });
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
