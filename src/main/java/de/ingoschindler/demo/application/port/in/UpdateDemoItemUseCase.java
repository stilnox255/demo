package de.ingoschindler.demo.application.port.in;

/** Published in-port: change name, description and status of an existing item. */
public interface UpdateDemoItemUseCase {

    DemoItemResult update(UpdateDemoItemCommand command);
}
