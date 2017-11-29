/**
 * A message handler is notified when a message is received.
 */
interface MessageHandler {
	public void handleMessage(Envelope msg);
}
