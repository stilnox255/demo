package de.ingoschindler.demo.application.port.in;

/** Published in-port: store a file and attach it to an item. */
public interface AttachFileToDemoItemUseCase {

    DemoItemResult attach(AttachFileToDemoItemCommand command);
}
