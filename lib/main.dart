import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Flutter Demo',
      theme: ThemeData(
        primarySwatch: Colors.blue,
      ),
      home: const MyHomePage(title: 'Flutter Demo Home Page'),
    );
  }
}

class MyHomePage extends StatefulWidget {
  const MyHomePage({super.key, required this.title});

  final String title;

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  static const methodChannel = MethodChannel("com.example.cop_flutter_biometry");
  static const keyName = "random-key-name";
  static const payload = "random-payload";
  String? _publicKey;
  String? _signature;

  void _generateKey() async {
    final publicKey = await methodChannel.invokeMethod("generate_public_key", {
      'keyName': keyName,
    });
    setState(() {
      _publicKey = publicKey;
    });
  }

  void _generateSignature() async {
    final signature = await methodChannel.invokeMethod("generate_signature_for_key", {
      'keyName': keyName,
      'payload': payload,
    });
    setState(() {
      _signature = signature;
    });
  }


  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.title),
      ),
      body: Padding(
        padding: const EdgeInsets.all(32.0),
        child: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.start,
            children: <Widget>[
              ElevatedButton(
                onPressed: _generateKey,
                child: const Text('Gerar chave'),
              ),
              const SizedBox(height: 32.0),
              ElevatedButton(
                onPressed: _generateSignature,
                child: const Text('Gerar assinatura'),
              ),
              const SizedBox(height: 32.0),
              SelectableText(_publicKey ?? ""),
              const SizedBox(height: 32.0),
              SelectableText(_signature ?? ""),
            ],
          ),
        ),
      ),
    );
  }
}
